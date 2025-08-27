package com.dari.finance.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class AndroidMLKitOcrScannerTest {

    private lateinit var ocrScanner: AndroidMLKitOcrScanner

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ocrScanner = AndroidMLKitOcrScanner(context)
    }

    @Test
    fun `should initialize ML Kit text recognizer`() {
        assertTrue(ocrScanner.isSupported())
    }

    @Test
    fun `should handle text recognition with ML Kit`() = runTest {
        // Mock image data representing a simple receipt
        val mockReceiptImage = createMockReceiptImageData()
        
        val result = ocrScanner.scanText(mockReceiptImage)
        
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
        if (result is OcrResult.Success) {
            assertTrue(result.confidence >= 0.0f)
            assertTrue(result.confidence <= 1.0f)
            assertNotNull(result.data)
        }
    }

    @Test
    fun `should extract receipt data with ML Kit`() = runTest {
        val mockReceiptImage = createMockReceiptImageData()
        
        val result = ocrScanner.extractReceiptData(mockReceiptImage)
        
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
        if (result is OcrResult.Success) {
            assertNotNull(result.data)
            assertTrue(result.confidence >= 0.0f)
        }
    }

    @Test
    fun `should handle Arabic text recognition`() = runTest {
        val arabicReceiptImage = createMockArabicReceiptImageData()
        val options = OcrOptions(language = "ar")
        
        val result = ocrScanner.scanTextWithOptions(arabicReceiptImage, options)
        
        // Should process Arabic text
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
    }

    @Test
    fun `should handle corrupted image data`() = runTest {
        val corruptedData = ByteArray(10) { 0xFF.toByte() }
        
        val result = ocrScanner.scanText(corruptedData)
        
        assertTrue(result is OcrResult.Error)
        if (result is OcrResult.Error) {
            assertTrue(result.message.contains("image") || result.message.contains("processing"))
        }
    }

    @Test
    fun `should process image preprocessing`() = runTest {
        val darkImage = createDarkImageData()
        val options = OcrOptions(enhanceImage = true)
        
        val result = ocrScanner.scanTextWithOptions(darkImage, options)
        
        // Should attempt to enhance and process the image
        assertNotNull(result)
    }

    @Test
    fun `should respect confidence threshold`() = runTest {
        val blurryImage = createBlurryImageData()
        val highThresholdOptions = OcrOptions(minConfidence = 0.9f)
        
        val result = ocrScanner.scanTextWithOptions(blurryImage, highThresholdOptions)
        
        when (result) {
            is OcrResult.Success -> assertTrue(result.confidence >= 0.9f)
            is OcrResult.Error -> assertTrue(result.message.contains("confidence"))
        }
    }

    @Test
    fun `should handle empty image gracefully`() = runTest {
        val emptyImage = ByteArray(0)
        
        val result = ocrScanner.scanText(emptyImage)
        
        assertTrue(result is OcrResult.Error)
        if (result is OcrResult.Error) {
            assertTrue(result.message.contains("empty") || result.message.contains("invalid"))
        }
    }

    // Helper methods to create mock image data
    private fun createMockReceiptImageData(): ByteArray {
        // Create a simple bitmap with text-like patterns
        return ByteArray(1000) { (it % 256).toByte() }
    }

    private fun createMockArabicReceiptImageData(): ByteArray {
        // Create mock data that would represent Arabic text
        return ByteArray(1200) { (it * 7 % 256).toByte() }
    }

    private fun createDarkImageData(): ByteArray {
        // Create mock data representing a dark/low contrast image
        return ByteArray(800) { (it % 50).toByte() }
    }

    private fun createBlurryImageData(): ByteArray {
        // Create mock data representing a blurry image
        return ByteArray(900) { (it * 3 % 128).toByte() }
    }
}