package com.dari.finance.ocr

/**
 * OCR Scanner interface for text recognition from images
 * Provides platform-agnostic text extraction capabilities
 */
interface OcrScanner {
    
    /**
     * Scans text from image data
     * @param imageData Image data as byte array
     * @return OCR result with recognized text and confidence score
     */
    suspend fun scanText(imageData: ByteArray): OcrResult<String>
    
    /**
     * Extracts structured receipt data from image
     * @param imageData Image data as byte array
     * @return OCR result with parsed receipt data and confidence score
     */
    suspend fun extractReceiptData(imageData: ByteArray): OcrResult<ReceiptData>
    
    /**
     * Checks if OCR is supported on the current platform
     * @return True if OCR is available, false otherwise
     */
    fun isSupported(): Boolean
}

/**
 * OCR result sealed class
 */
sealed class OcrResult<out T> {
    data class Success<T>(
        val data: T,
        val confidence: Float
    ) : OcrResult<T>()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : OcrResult<Nothing>()
}

/**
 * Receipt data structure
 */
data class ReceiptData(
    val merchantName: String? = null,
    val date: String? = null,
    val total: String? = null,
    val currency: String = "SAR",
    val items: List<ReceiptItem> = emptyList(),
    val tax: String? = null,
    val subtotal: String? = null,
    val rawText: String = ""
)

/**
 * Receipt item structure
 */
data class ReceiptItem(
    val name: String,
    val price: String,
    val quantity: Int = 1,
    val category: String? = null
)

/**
 * OCR confidence levels
 */
enum class ConfidenceLevel {
    LOW,     // 0.0 - 0.5
    MEDIUM,  // 0.5 - 0.8
    HIGH;    // 0.8 - 1.0
    
    companion object {
        fun fromFloat(confidence: Float): ConfidenceLevel = when {
            confidence >= 0.8f -> HIGH
            confidence >= 0.5f -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * OCR processing options
 */
data class OcrOptions(
    val language: String = "ar", // Arabic for Saudi market
    val enableItemExtraction: Boolean = true,
    val minConfidence: Float = 0.5f,
    val enhanceImage: Boolean = true
)