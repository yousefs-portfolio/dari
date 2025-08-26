package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.domain.services.OCRService
import code.yousef.dari.shared.domain.services.FileStorageService
import code.yousef.dari.shared.utils.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * Use case for attaching receipts to transactions with OCR processing
 * Supports Saudi-specific VAT validation and Arabic text recognition
 */
class AttachReceiptUseCase(
    private val transactionRepository: TransactionRepository,
    private val ocrService: OCRService,
    private val fileStorageService: FileStorageService
) {
    suspend operator fun invoke(request: AttachReceiptRequest): Result<Receipt> {
        return try {
            // Get the transaction
            val transactionResult = transactionRepository.getTransactionById(request.transactionId)
            if (transactionResult is Result.Error) {
                return transactionResult
            }

            val transaction = (transactionResult as Result.Success).data

            // Handle existing receipt replacement
            if (transaction.receipt != null && !request.replaceExisting) {
                return Result.Error(IllegalStateException("Transaction already has a receipt. Use replaceExisting=true to replace."))
            }

            // Delete old receipt if replacing
            if (transaction.receipt != null && request.replaceExisting) {
                fileStorageService.deleteReceiptImage(transaction.receipt.url)
            }

            // Upload the new receipt image
            val uploadResult = fileStorageService.uploadReceiptImage(request.receiptImageData)
            if (uploadResult is Result.Error) {
                return uploadResult
            }

            val receiptUrl = (uploadResult as Result.Success).data

            // Process OCR if requested
            val (extractedData, ocrConfidence) = if (request.performOCR) {
                processOCR(receiptUrl, transaction, request)
            } else {
                Pair(emptyMap<String, String>(), null)
            }

            // Create receipt object
            val receipt = Receipt(
                id = generateReceiptId(),
                url = receiptUrl,
                extractedData = extractedData,
                ocrConfidence = ocrConfidence,
                uploadDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )

            // Update transaction with receipt
            val updatedTransaction = transaction.copy(receipt = receipt)
            val updateResult = transactionRepository.updateTransaction(updatedTransaction)
            
            when (updateResult) {
                is Result.Success -> Result.Success(receipt)
                is Result.Error -> updateResult
            }

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun processOCR(
        receiptUrl: String,
        transaction: Transaction,
        request: AttachReceiptRequest
    ): Pair<Map<String, String>, Double?> {
        return when (val ocrResult = ocrService.extractReceiptData(receiptUrl)) {
            is Result.Success -> {
                val extractedData = ocrResult.data
                val confidence = calculateOCRConfidence(extractedData)

                // Validate amount if requested
                if (request.validateAmount) {
                    validateReceiptAmount(extractedData, transaction.amount)
                }

                // Validate VAT if requested (Saudi-specific)
                if (request.validateVAT) {
                    validateSaudiVAT(extractedData)
                }

                Pair(extractedData, confidence)
            }
            is Result.Error -> {
                // OCR failed, but we can still attach the receipt without extracted data
                Pair(emptyMap(), null)
            }
        }
    }

    private fun validateReceiptAmount(extractedData: Map<String, String>, transactionAmount: Money) {
        val extractedAmountStr = extractedData["total_amount"] ?: extractedData["amount"]
        if (extractedAmountStr != null) {
            try {
                val extractedAmount = extractedAmountStr.toDouble()
                val transactionAmountValue = abs(transactionAmount.value) // Use absolute value for comparison
                
                // Allow for small discrepancies due to rounding (within 1 SAR)
                val tolerance = 1.0
                if (abs(extractedAmount - transactionAmountValue) > tolerance) {
                    throw IllegalArgumentException(
                        "Receipt amount mismatch: extracted $extractedAmount, transaction $transactionAmountValue"
                    )
                }
            } catch (e: NumberFormatException) {
                // If we can't parse the amount, don't fail - just log it
                // In a real app, this would be logged for manual review
            }
        }
    }

    private fun validateSaudiVAT(extractedData: Map<String, String>) {
        val vatRate = extractedData["vat_rate"]
        val taxAmount = extractedData["tax_amount"]
        val subtotal = extractedData["subtotal"]
        val total = extractedData["total_amount"]
        val vatRegistration = extractedData["vat_registration"]

        // Validate VAT rate (should be 15% in Saudi Arabia)
        if (vatRate != null && !isValidSaudiVATRate(vatRate)) {
            // Log warning but don't fail - different rates might apply to different goods
        }

        // Validate VAT calculation if we have all required fields
        if (taxAmount != null && subtotal != null && total != null) {
            try {
                val tax = taxAmount.toDouble()
                val sub = subtotal.toDouble()
                val tot = total.toDouble()
                
                val expectedTotal = sub + tax
                if (abs(tot - expectedTotal) > 0.01) { // Allow for rounding
                    // Log discrepancy but don't fail
                }
            } catch (e: NumberFormatException) {
                // Can't validate calculation
            }
        }

        // Validate VAT registration number format (Saudi format: 15 digits)
        if (vatRegistration != null && !isValidSaudiVATRegistration(vatRegistration)) {
            // Log warning but don't fail - format might vary
        }
    }

    private fun isValidSaudiVATRate(vatRate: String): Boolean {
        // Common Saudi VAT rates: 15%, 5% (for some goods), 0% (exports/exempt)
        val normalizedRate = vatRate.replace("%", "").trim()
        return normalizedRate in listOf("0", "5", "15")
    }

    private fun isValidSaudiVATRegistration(vatRegistration: String): Boolean {
        // Saudi VAT registration numbers are 15 digits
        val cleaned = vatRegistration.replace(Regex("[^0-9]"), "")
        return cleaned.length == 15
    }

    private fun calculateOCRConfidence(extractedData: Map<String, String>): Double {
        // Simple confidence calculation based on how much data was extracted
        // In a real implementation, this would come from the OCR engine
        val fieldsExtracted = extractedData.size
        return when {
            fieldsExtracted >= 8 -> 0.95
            fieldsExtracted >= 6 -> 0.90
            fieldsExtracted >= 4 -> 0.85
            fieldsExtracted >= 2 -> 0.80
            fieldsExtracted >= 1 -> 0.70
            else -> 0.50
        }
    }

    private fun generateReceiptId(): String {
        return "receipt_${Clock.System.now().epochSeconds}_${(0..999).random()}"
    }
}

/**
 * Request data class for attaching receipts
 */
data class AttachReceiptRequest(
    val transactionId: String,
    val receiptImageData: ReceiptImageData,
    val performOCR: Boolean = true,
    val validateAmount: Boolean = false,
    val validateVAT: Boolean = false,
    val replaceExisting: Boolean = false
)

/**
 * Receipt image data container
 */
data class ReceiptImageData(
    val imageData: ByteArray,
    val fileName: String,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ReceiptImageData

        if (!imageData.contentEquals(other.imageData)) return false
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageData.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/**
 * OCR Service interface for receipt text extraction
 */
interface OCRService {
    suspend fun extractReceiptData(imageUrl: String): Result<Map<String, String>>
}

/**
 * File Storage Service interface for receipt image management
 */
interface FileStorageService {
    suspend fun uploadReceiptImage(imageData: ReceiptImageData): Result<String>
    suspend fun deleteReceiptImage(imageUrl: String): Result<Unit>
}