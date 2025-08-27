package com.dari.finance.ocr

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.Vision.*
import kotlin.coroutines.resume

/**
 * iOS Vision framework implementation of OCR Scanner
 * Uses Apple's Vision framework for text recognition on iOS devices
 */
class IOSVisionOcrScanner : OcrScanner {

    private val receiptParser = ReceiptParser()

    override suspend fun scanText(imageData: ByteArray): OcrResult<String> {
        return try {
            if (imageData.isEmpty()) {
                return OcrResult.Error("Image data is empty")
            }

            val nsData = imageData.toNSData()
            val uiImage = UIImage.imageWithData(nsData)
                ?: return OcrResult.Error("Failed to create UIImage from data")

            val recognizedText = performTextRecognition(uiImage)

            if (recognizedText.isBlank()) {
                OcrResult.Error("No text found in image")
            } else {
                OcrResult.Success(
                    data = recognizedText,
                    confidence = calculateOverallConfidence(recognizedText)
                )
            }
        } catch (e: Exception) {
            OcrResult.Error("Text recognition failed: ${e.message}", e)
        }
    }

    override suspend fun extractReceiptData(imageData: ByteArray): OcrResult<ReceiptData> {
        return try {
            when (val textResult = scanText(imageData)) {
                is OcrResult.Success -> {
                    val receiptData = receiptParser.parseReceiptText(textResult.data)
                    OcrResult.Success(
                        data = receiptData.copy(rawText = textResult.data),
                        confidence = textResult.confidence * receiptParser.getParsingConfidence(receiptData)
                    )
                }
                is OcrResult.Error -> textResult
            }
        } catch (e: Exception) {
            OcrResult.Error("Receipt extraction failed: ${e.message}", e)
        }
    }

    /**
     * Enhanced scanning with custom options
     */
    suspend fun scanTextWithOptions(imageData: ByteArray, options: OcrOptions): OcrResult<String> {
        return try {
            if (imageData.isEmpty()) {
                return OcrResult.Error("Image data is empty")
            }

            val nsData = imageData.toNSData()
            var uiImage = UIImage.imageWithData(nsData)
                ?: return OcrResult.Error("Failed to create UIImage from data")

            // Apply image enhancement if requested
            if (options.enhanceImage) {
                uiImage = enhanceImage(uiImage)
            }

            val recognizedText = performTextRecognition(uiImage, options)

            val confidence = calculateOverallConfidence(recognizedText)
            
            if (confidence < options.minConfidence) {
                return OcrResult.Error("Recognition confidence $confidence below threshold ${options.minConfidence}")
            }

            if (recognizedText.isBlank()) {
                OcrResult.Error("No text found in image")
            } else {
                OcrResult.Success(
                    data = recognizedText,
                    confidence = confidence
                )
            }
        } catch (e: Exception) {
            OcrResult.Error("Text recognition failed: ${e.message}", e)
        }
    }

    override fun isSupported(): Boolean {
        // Vision framework is available on iOS 13+
        return NSProcessInfo.processInfo.operatingSystemVersion.majorVersion >= 13
    }

    private suspend fun performTextRecognition(
        image: UIImage, 
        options: OcrOptions = OcrOptions()
    ): String = suspendCancellableCoroutine { continuation ->
        
        val request = VNRecognizeTextRequest { request, error ->
            if (error != null) {
                continuation.resume("")
                return@VNRecognizeTextRequest
            }

            val observations = request.results as? List<VNRecognizedTextObservation> ?: emptyList()
            val recognizedStrings = observations.mapNotNull { observation ->
                observation.topCandidates(1).firstOrNull()?.string
            }

            continuation.resume(recognizedStrings.joinToString("\n"))
        }

        // Configure recognition level based on options
        request.recognitionLevel = if (options.language == "ar") {
            VNRequestTextRecognitionLevelAccurate
        } else {
            VNRequestTextRecognitionLevelFast
        }

        // Set language preferences if specified
        if (options.language.isNotEmpty()) {
            request.recognitionLanguages = listOf(options.language)
        }

        // Create request handler with the image
        val cgImage = image.CGImage ?: run {
            continuation.resume("")
            return@suspendCancellableCoroutine
        }

        val requestHandler = VNImageRequestHandler(cgImage, mapOf<Any?, Any?>())

        try {
            requestHandler.performRequests(listOf(request), null)
        } catch (e: Exception) {
            continuation.resume("")
        }
    }

    private fun calculateOverallConfidence(text: String): Float {
        // Simple confidence calculation based on text characteristics
        if (text.isBlank()) return 0.0f
        
        var confidence = 0.5f
        
        // Boost confidence for receipt-like patterns
        if (text.contains(Regex("\\d+\\.\\d{2}"))) confidence += 0.2f // Price patterns
        if (text.contains(Regex("total|Total|TOTAL|المجموع", RegexOption.IGNORE_CASE))) confidence += 0.1f
        if (text.contains(Regex("\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2}"))) confidence += 0.1f // Date patterns
        
        // Arabic receipt patterns
        if (text.contains(Regex("ريال|درهم|دينار"))) confidence += 0.1f // Currency
        if (text.contains(Regex("فاتورة|إيصال"))) confidence += 0.1f // Receipt/invoice
        
        return minOf(confidence, 1.0f)
    }

    private fun enhanceImage(image: UIImage): UIImage {
        // Create enhanced version using Core Image filters
        val ciImage = CIImage.imageWithCGImage(image.CGImage!!)
        
        // Apply contrast and brightness adjustments
        val filter = CIFilter.filterWithName("CIColorControls")!!
        filter.setValue(ciImage, forKey = "inputImage")
        filter.setValue(1.2, forKey = "inputContrast") // Increase contrast
        filter.setValue(0.1, forKey = "inputBrightness") // Slight brightness boost
        
        val outputImage = filter.outputImage ?: ciImage
        val context = CIContext()
        val cgImage = context.createCGImage(outputImage, fromRect = outputImage.extent)
        
        return UIImage.imageWithCGImage(cgImage!!)
    }

    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
        }
    }
}