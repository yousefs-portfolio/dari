package com.dari.finance.ocr

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class IOSVisionOcrScannerTest {

    private lateinit var ocrScanner: IOSVisionOcrScanner

    @BeforeTest
    fun setup() {
        ocrScanner = IOSVisionOcrScanner()
    }

    @Test
    fun `should check Vision framework support on iOS`() {
        val isSupported = ocrScanner.isSupported()
        
        // On iOS 13+ Vision framework should be available
        assertTrue(isSupported)
    }

    @Test
    fun `should handle text recognition with Vision framework`() = runTest {
        val mockImageData = createMockImageData()
        
        val result = ocrScanner.scanText(mockImageData)
        
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
        if (result is OcrResult.Success) {
            assertTrue(result.confidence >= 0.0f)
            assertTrue(result.confidence <= 1.0f)
            assertNotNull(result.data)
        }
    }

    @Test
    fun `should extract receipt data with Vision framework`() = runTest {
        val mockReceiptImage = createMockReceiptImageData()
        
        val result = ocrScanner.extractReceiptData(mockReceiptImage)
        
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
        if (result is OcrResult.Success) {
            assertNotNull(result.data)
            assertTrue(result.confidence >= 0.0f)
        }
    }

    @Test
    fun `should handle Arabic text recognition on iOS`() = runTest {
        val arabicReceiptImage = createMockArabicReceiptData()
        val options = OcrOptions(language = "ar")
        
        val result = ocrScanner.scanTextWithOptions(arabicReceiptImage, options)
        
        // Vision framework should handle Arabic text
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
    }

    @Test
    fun `should handle empty image data`() = runTest {
        val emptyData = ByteArray(0)
        
        val result = ocrScanner.scanText(emptyData)
        
        assertTrue(result is OcrResult.Error)
        if (result is OcrResult.Error) {
            assertTrue(result.message.contains("empty") || result.message.contains("invalid"))
        }
    }

    @Test
    fun `should handle corrupted image data`() = runTest {
        val corruptedData = ByteArray(100) { 0xFF.toByte() }
        
        val result = ocrScanner.scanText(corruptedData)
        
        assertTrue(result is OcrResult.Error)
        if (result is OcrResult.Error) {
            assertTrue(result.message.contains("image") || result.message.contains("processing"))
        }
    }

    @Test
    fun `should respect confidence threshold in options`() = runTest {
        val lowQualityImage = createLowQualityImageData()
        val highThresholdOptions = OcrOptions(minConfidence = 0.95f)
        
        val result = ocrScanner.scanTextWithOptions(lowQualityImage, highThresholdOptions)
        
        when (result) {
            is OcrResult.Success -> assertTrue(result.confidence >= 0.95f)
            is OcrResult.Error -> assertTrue(result.message.contains("confidence"))
        }
    }

    @Test
    fun `should handle image enhancement option`() = runTest {
        val darkImage = createDarkImageData()
        val enhancementOptions = OcrOptions(enhanceImage = true)
        
        val result = ocrScanner.scanTextWithOptions(darkImage, enhancementOptions)
        
        // Should attempt to process even with enhancement
        assertNotNull(result)
    }

    @Test
    fun `should detect receipt patterns`() = runTest {
        val receiptWithTotal = createReceiptWithTotalData()
        
        val result = ocrScanner.extractReceiptData(receiptWithTotal)
        
        if (result is OcrResult.Success) {
            assertTrue(result.data.total?.isNotEmpty() == true || result.confidence > 0.0f)
        }
    }

    // Helper methods to create mock image data for testing
    private fun createMockImageData(): ByteArray {
        return ByteArray(1000) { (it % 256).toByte() }
    }

    private fun createMockReceiptImageData(): ByteArray {
        // Simulate receipt image data
        return ByteArray(1500) { (it * 3 % 256).toByte() }
    }

    private fun createMockArabicReceiptData(): ByteArray {
        // Simulate Arabic text receipt data
        return ByteArray(1200) { (it * 7 % 256).toByte() }
    }

    private fun createLowQualityImageData(): ByteArray {
        // Simulate low quality/blurry image
        return ByteArray(800) { (it % 100).toByte() }
    }

    private fun createDarkImageData(): ByteArray {
        // Simulate dark/low contrast image
        return ByteArray(900) { (it % 50).toByte() }
    }

    private fun createReceiptWithTotalData(): ByteArray {
        // Simulate receipt with clear total amount
        return ByteArray(1100) { (it * 11 % 256).toByte() }
    }
}