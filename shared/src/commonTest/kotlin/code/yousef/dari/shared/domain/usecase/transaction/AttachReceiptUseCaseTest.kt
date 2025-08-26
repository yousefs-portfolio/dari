package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.domain.services.OCRService
import code.yousef.dari.shared.domain.services.FileStorageService
import code.yousef.dari.shared.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttachReceiptUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val ocrService = mockk<OCRService>()
    private val fileStorageService = mockk<FileStorageService>()
    private val attachReceiptUseCase = AttachReceiptUseCase(
        transactionRepository = transactionRepository,
        ocrService = ocrService,
        fileStorageService = fileStorageService
    )

    private val sampleTransaction = Transaction(
        id = "tx1",
        accountId = "acc1",
        amount = Money(100.0, Currency.SAR),
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        description = "Restaurant expense",
        date = LocalDateTime(2024, 1, 15, 12, 30),
        merchant = Merchant("merchant1", "Al Baik", "Restaurant"),
        location = TransactionLocation(24.7136, 46.6753, "Riyadh, Saudi Arabia"),
        isRecurring = false,
        tags = listOf("lunch", "work"),
        paymentMethod = PaymentMethod.CARD,
        status = TransactionStatus.COMPLETED,
        receipt = null
    )

    private val sampleReceiptData = ReceiptImageData(
        imageData = "base64_encoded_image_data".toByteArray(),
        fileName = "receipt.jpg",
        mimeType = "image/jpeg"
    )

    private val ocrExtractedData = mapOf(
        "total_amount" to "100.00",
        "merchant_name" to "Al Baik Restaurant",
        "date" to "2024-01-15",
        "tax_amount" to "15.00",
        "vat_registration" to "123456789001",
        "items" to listOf("Chicken Meal", "Pepsi").toString()
    )

    @Test
    fun `should attach receipt with OCR processing successfully`() = runTest {
        // Given
        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = true,
            validateAmount = true
        )

        val uploadedReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt.jpg"
        val receipt = Receipt(
            id = "receipt1",
            url = uploadedReceiptUrl,
            extractedData = ocrExtractedData,
            ocrConfidence = 0.95,
            uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(uploadedReceiptUrl)
        coEvery { ocrService.extractReceiptData(uploadedReceiptUrl) } returns Result.Success(ocrExtractedData)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(sampleTransaction.copy(receipt = receipt))

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val attachedReceipt = result.data
        assertEquals("receipt1", attachedReceipt.id)
        assertEquals(uploadedReceiptUrl, attachedReceipt.url)
        assertEquals(ocrExtractedData, attachedReceipt.extractedData)
        assertEquals(0.95, attachedReceipt.ocrConfidence)
        
        coVerify {
            transactionRepository.getTransactionById("tx1")
            fileStorageService.uploadReceiptImage(sampleReceiptData)
            ocrService.extractReceiptData(uploadedReceiptUrl)
            transactionRepository.updateTransaction(any())
        }
    }

    @Test
    fun `should attach receipt without OCR when not requested`() = runTest {
        // Given
        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = false,
            validateAmount = false
        )

        val uploadedReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt.jpg"
        val receipt = Receipt(
            id = "receipt1",
            url = uploadedReceiptUrl,
            extractedData = emptyMap(),
            ocrConfidence = null,
            uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(uploadedReceiptUrl)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(sampleTransaction.copy(receipt = receipt))

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val attachedReceipt = result.data
        assertEquals(uploadedReceiptUrl, attachedReceipt.url)
        assertTrue(attachedReceipt.extractedData.isEmpty())
        assertEquals(null, attachedReceipt.ocrConfidence)
        
        coVerify {
            transactionRepository.getTransactionById("tx1")
            fileStorageService.uploadReceiptImage(sampleReceiptData)
            transactionRepository.updateTransaction(any())
        }
        coVerify(exactly = 0) {
            ocrService.extractReceiptData(any())
        }
    }

    @Test
    fun `should validate amount against OCR extracted data`() = runTest {
        // Given
        val ocrDataWithDifferentAmount = mapOf(
            "total_amount" to "150.00", // Different from transaction amount (100.00)
            "merchant_name" to "Al Baik Restaurant"
        )

        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = true,
            validateAmount = true
        )

        val uploadedReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt.jpg"

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(uploadedReceiptUrl)
        coEvery { ocrService.extractReceiptData(uploadedReceiptUrl) } returns Result.Success(ocrDataWithDifferentAmount)

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("amount mismatch") == true)
    }

    @Test
    fun `should handle Saudi VAT validation in OCR data`() = runTest {
        // Given
        val ocrDataWithVAT = mapOf(
            "total_amount" to "115.00",
            "subtotal" to "100.00",
            "tax_amount" to "15.00",
            "vat_rate" to "15%",
            "vat_registration" to "123456789001",
            "merchant_name" to "Al Baik Restaurant"
        )

        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = true,
            validateAmount = false, // Don't validate amount as total includes VAT
            validateVAT = true
        )

        val uploadedReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt.jpg"
        val receipt = Receipt(
            id = "receipt1",
            url = uploadedReceiptUrl,
            extractedData = ocrDataWithVAT,
            ocrConfidence = 0.92,
            uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(uploadedReceiptUrl)
        coEvery { ocrService.extractReceiptData(uploadedReceiptUrl) } returns Result.Success(ocrDataWithVAT)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(sampleTransaction.copy(receipt = receipt))

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val attachedReceipt = result.data
        assertEquals("15%", attachedReceipt.extractedData["vat_rate"])
        assertEquals("123456789001", attachedReceipt.extractedData["vat_registration"])
    }

    @Test
    fun `should handle receipt replacement for existing receipt`() = runTest {
        // Given
        val existingReceipt = Receipt(
            id = "old_receipt",
            url = "https://storage.dari.com/receipts/old_receipt.jpg",
            extractedData = emptyMap(),
            ocrConfidence = 0.8,
            uploadDate = LocalDateTime(2024, 1, 10, 10, 0)
        )

        val transactionWithReceipt = sampleTransaction.copy(receipt = existingReceipt)

        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = true,
            validateAmount = true,
            replaceExisting = true
        )

        val newReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt_new.jpg"
        val newReceipt = Receipt(
            id = "new_receipt",
            url = newReceiptUrl,
            extractedData = ocrExtractedData,
            ocrConfidence = 0.95,
            uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(transactionWithReceipt)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(newReceiptUrl)
        coEvery { ocrService.extractReceiptData(newReceiptUrl) } returns Result.Success(ocrExtractedData)
        coEvery { fileStorageService.deleteReceiptImage(existingReceipt.url) } returns Result.Success(Unit)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(transactionWithReceipt.copy(receipt = newReceipt))

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        
        coVerify {
            fileStorageService.deleteReceiptImage(existingReceipt.url)
            fileStorageService.uploadReceiptImage(sampleReceiptData)
        }
    }

    @Test
    fun `should handle transaction not found error`() = runTest {
        // Given
        val request = AttachReceiptRequest(
            transactionId = "nonexistent_tx",
            receiptImageData = sampleReceiptData,
            performOCR = false
        )

        coEvery { transactionRepository.getTransactionById("nonexistent_tx") } returns Result.Error(
            Exception("Transaction not found")
        )

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Transaction not found") == true)
    }

    @Test
    fun `should handle file upload error`() = runTest {
        // Given
        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = false
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Error(
            Exception("Upload failed")
        )

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Upload failed") == true)
    }

    @Test
    fun `should handle OCR processing error gracefully`() = runTest {
        // Given
        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = true,
            validateAmount = false
        )

        val uploadedReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt.jpg"
        val receiptWithoutOCR = Receipt(
            id = "receipt1",
            url = uploadedReceiptUrl,
            extractedData = emptyMap(),
            ocrConfidence = null,
            uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(uploadedReceiptUrl)
        coEvery { ocrService.extractReceiptData(uploadedReceiptUrl) } returns Result.Error(
            Exception("OCR processing failed")
        )
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(sampleTransaction.copy(receipt = receiptWithoutOCR))

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val attachedReceipt = result.data
        assertEquals(uploadedReceiptUrl, attachedReceipt.url)
        assertTrue(attachedReceipt.extractedData.isEmpty())
        assertEquals(null, attachedReceipt.ocrConfidence)
    }

    @Test
    fun `should handle Arabic text in OCR data`() = runTest {
        // Given
        val arabicOCRData = mapOf(
            "total_amount" to "100.00",
            "merchant_name" to "البيك",
            "merchant_name_ar" to "البيك",
            "merchant_name_en" to "Al Baik",
            "items" to "وجبة دجاج، بيبسي",
            "location" to "الرياض، السعودية"
        )

        val request = AttachReceiptRequest(
            transactionId = "tx1",
            receiptImageData = sampleReceiptData,
            performOCR = true,
            validateAmount = true
        )

        val uploadedReceiptUrl = "https://storage.dari.com/receipts/tx1_receipt.jpg"
        val receipt = Receipt(
            id = "receipt1",
            url = uploadedReceiptUrl,
            extractedData = arabicOCRData,
            ocrConfidence = 0.88,
            uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(sampleTransaction)
        coEvery { fileStorageService.uploadReceiptImage(any()) } returns Result.Success(uploadedReceiptUrl)
        coEvery { ocrService.extractReceiptData(uploadedReceiptUrl) } returns Result.Success(arabicOCRData)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(sampleTransaction.copy(receipt = receipt))

        // When
        val result = attachReceiptUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val attachedReceipt = result.data
        assertEquals("البيك", attachedReceipt.extractedData["merchant_name"])
        assertEquals("وجبة دجاج، بيبسي", attachedReceipt.extractedData["items"])
    }
}