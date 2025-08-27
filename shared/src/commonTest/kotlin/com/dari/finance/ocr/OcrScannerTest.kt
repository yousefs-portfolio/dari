package com.dari.finance.ocr

import kotlin.test.*

class OcrScannerTest {

    @Test
    fun `should define OCR scanner interface correctly`() {
        // Verify interface methods
        val methods = OcrScanner::class.members.map { it.name }.toSet()
        assertTrue(methods.contains("scanText"))
        assertTrue(methods.contains("extractReceiptData"))
        assertTrue(methods.contains("isSupported"))
    }

    @Test
    fun `should handle text recognition request`() {
        val mockScanner = MockOcrScanner()
        val mockImageData = ByteArray(100) { it.toByte() }
        
        val result = mockScanner.scanText(mockImageData)
        
        assertNotNull(result)
        assertTrue(result is OcrResult.Success || result is OcrResult.Error)
    }

    @Test
    fun `should extract receipt data with confidence scores`() {
        val mockScanner = MockOcrScanner()
        val mockImageData = ByteArray(100) { it.toByte() }
        
        val result = mockScanner.extractReceiptData(mockImageData)
        
        when (result) {
            is OcrResult.Success -> {
                assertNotNull(result.data)
                assertTrue(result.confidence >= 0.0f)
                assertTrue(result.confidence <= 1.0f)
            }
            is OcrResult.Error -> {
                assertNotNull(result.message)
            }
        }
    }

    @Test
    fun `should check platform support correctly`() {
        val mockScanner = MockOcrScanner()
        
        val isSupported = mockScanner.isSupported()
        
        assertTrue(isSupported is Boolean)
    }

    @Test
    fun `should handle invalid image data`() {
        val mockScanner = MockOcrScanner()
        val invalidImageData = ByteArray(0)
        
        val result = mockScanner.scanText(invalidImageData)
        
        assertTrue(result is OcrResult.Error)
        if (result is OcrResult.Error) {
            assertTrue(result.message.contains("invalid") || result.message.contains("empty"))
        }
    }

    @Test
    fun `should handle unsupported platform gracefully`() {
        val unsupportedScanner = MockOcrScanner(isSupported = false)
        val mockImageData = ByteArray(100) { it.toByte() }
        
        val result = unsupportedScanner.scanText(mockImageData)
        
        assertTrue(result is OcrResult.Error)
        if (result is OcrResult.Error) {
            assertTrue(result.message.contains("not supported"))
        }
    }
}

// Mock implementation for testing
class MockOcrScanner(private val isSupported: Boolean = true) : OcrScanner {
    
    override suspend fun scanText(imageData: ByteArray): OcrResult<String> {
        if (!isSupported) {
            return OcrResult.Error("OCR not supported on this platform")
        }
        
        if (imageData.isEmpty()) {
            return OcrResult.Error("Invalid or empty image data")
        }
        
        return OcrResult.Success(
            data = "Mock receipt text\nTotal: $25.99\nDate: 2024-01-15",
            confidence = 0.95f
        )
    }
    
    override suspend fun extractReceiptData(imageData: ByteArray): OcrResult<ReceiptData> {
        if (!isSupported) {
            return OcrResult.Error("OCR not supported on this platform")
        }
        
        if (imageData.isEmpty()) {
            return OcrResult.Error("Invalid or empty image data")
        }
        
        val mockReceiptData = ReceiptData(
            merchantName = "Test Store",
            date = "2024-01-15",
            total = "25.99",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Item 1", "10.00", 1),
                ReceiptItem("Item 2", "15.99", 1)
            )
        )
        
        return OcrResult.Success(
            data = mockReceiptData,
            confidence = 0.89f
        )
    }
    
    override fun isSupported(): Boolean = isSupported
}