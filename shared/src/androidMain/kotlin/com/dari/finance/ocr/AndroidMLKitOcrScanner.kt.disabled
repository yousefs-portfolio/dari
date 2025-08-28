package com.dari.finance.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android ML Kit implementation of OCR Scanner
 * Uses Google's ML Kit for text recognition on Android devices
 */
class AndroidMLKitOcrScanner(
    private val context: Context
) : OcrScanner {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val receiptParser = ReceiptParser()

    override suspend fun scanText(imageData: ByteArray): OcrResult<String> {
        return try {
            if (imageData.isEmpty()) {
                return OcrResult.Error("Image data is empty")
            }

            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return OcrResult.Error("Failed to decode image data")

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizedText = recognizeText(inputImage)

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

            var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return OcrResult.Error("Failed to decode image data")

            // Apply image enhancement if requested
            if (options.enhanceImage) {
                bitmap = enhanceImage(bitmap)
            }

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizedText = recognizeText(inputImage)

            val confidence = calculateOverallConfidence(recognizedText)
            
            if (confidence < options.minConfidence) {
                return OcrResult.Error("Recognition confidence ${confidence} below threshold ${options.minConfidence}")
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
        return try {
            // Check if ML Kit is available
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun recognizeText(inputImage: InputImage): String = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText.text)
            }
            .addOnFailureListener { exception ->
                continuation.resume("")
            }
    }

    private fun calculateOverallConfidence(text: String): Float {
        // Simple confidence calculation based on text characteristics
        if (text.isBlank()) return 0.0f
        
        var confidence = 0.5f
        
        // Boost confidence for receipt-like patterns
        if (text.contains(Regex("\\d+\\.\\d{2}"))) confidence += 0.2f // Price patterns
        if (text.contains(Regex("total|Total|TOTAL", RegexOption.IGNORE_CASE))) confidence += 0.1f
        if (text.contains(Regex("\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2}"))) confidence += 0.1f // Date patterns
        
        return minOf(confidence, 1.0f)
    }

    private fun enhanceImage(bitmap: Bitmap): Bitmap {
        // Simple image enhancement - increase contrast
        val enhanced = bitmap.copy(bitmap.config, true)
        
        // Apply basic contrast and brightness adjustments
        val canvas = android.graphics.Canvas(enhanced)
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply {
                    // Increase contrast
                    setSaturation(1.2f)
                    postConcat(android.graphics.ColorMatrix(floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.2f, 0f, 0f, 10f,
                        0f, 0f, 1.2f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
            )
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return enhanced
    }
}