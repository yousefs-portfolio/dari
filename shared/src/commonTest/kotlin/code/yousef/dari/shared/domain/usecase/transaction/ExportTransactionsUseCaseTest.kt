package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportTransactionsUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val exportTransactionsUseCase = ExportTransactionsUseCase(transactionRepository)

    private val sampleTransactions = listOf(
        Transaction(
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
            status = TransactionStatus.COMPLETED
        ),
        Transaction(
            id = "tx2",
            accountId = "acc1",
            amount = Money(1500.0, Currency.SAR),
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "Monthly salary",
            date = LocalDateTime(2024, 1, 1, 9, 0),
            merchant = null,
            location = null,
            isRecurring = true,
            tags = listOf("salary"),
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            status = TransactionStatus.COMPLETED
        )
    )

    @Test
    fun `should export transactions to CSV format successfully`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            includeCategories = listOf(TransactionCategory.FOOD_DINING, TransactionCategory.SALARY),
            includeHeaders = true,
            includeReceiptUrls = false
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(
                accountId = "acc1",
                startDate = request.startDate,
                endDate = request.endDate
            ) 
        } returns Result.Success(sampleTransactions)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        assertEquals(ExportFormat.CSV, exportData.format)
        assertTrue(exportData.content.contains("Date,Description,Amount,Category,Type"))
        assertTrue(exportData.content.contains("Restaurant expense,100.0,FOOD_DINING,EXPENSE"))
        assertTrue(exportData.content.contains("Monthly salary,1500.0,SALARY,INCOME"))
        assertEquals("transactions_2024-01-01_2024-01-31.csv", exportData.fileName)
        
        coVerify {
            transactionRepository.getTransactionsByDateRange(
                accountId = "acc1",
                startDate = request.startDate,
                endDate = request.endDate
            )
        }
    }

    @Test
    fun `should export transactions to PDF format successfully`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.PDF,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            includeCategories = listOf(),
            includeHeaders = true,
            includeReceiptUrls = true
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Success(sampleTransactions)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        assertEquals(ExportFormat.PDF, exportData.format)
        assertTrue(exportData.content.isNotEmpty())
        assertEquals("transactions_2024-01-01_2024-01-31.pdf", exportData.fileName)
    }

    @Test
    fun `should export transactions to Excel format successfully`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.EXCEL,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59)
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Success(sampleTransactions)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        assertEquals(ExportFormat.EXCEL, exportData.format)
        assertEquals("transactions_2024-01-01_2024-01-31.xlsx", exportData.fileName)
    }

    @Test
    fun `should filter transactions by categories when specified`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            includeCategories = listOf(TransactionCategory.FOOD_DINING)
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Success(sampleTransactions)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        assertTrue(exportData.content.contains("Restaurant expense"))
        assertTrue(!exportData.content.contains("Monthly salary"))
    }

    @Test
    fun `should include receipt URLs when requested for CSV format`() = runTest {
        // Given
        val transactionsWithReceipts = listOf(
            sampleTransactions[0].copy(
                receipt = Receipt(
                    id = "receipt1",
                    url = "https://example.com/receipt1.jpg",
                    extractedData = emptyMap(),
                    ocrConfidence = 0.95,
                    uploadDate = LocalDateTime(2024, 1, 15, 12, 35)
                )
            )
        )

        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            includeReceiptUrls = true
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Success(transactionsWithReceipts)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        assertTrue(exportData.content.contains("Receipt URL"))
        assertTrue(exportData.content.contains("https://example.com/receipt1.jpg"))
    }

    @Test
    fun `should handle empty transaction list`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59)
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Success(emptyList())

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        assertTrue(exportData.content.contains("Date,Description,Amount"))
        assertTrue(!exportData.content.contains("Restaurant expense"))
    }

    @Test
    fun `should handle repository error`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59)
        )

        val error = Exception("Database connection failed")
        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Error(error)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(error, result.exception)
    }

    @Test
    fun `should validate date range`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 31, 0, 0),
            endDate = LocalDateTime(2024, 1, 1, 23, 59) // End date before start date
        )

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Start date must be before end date") == true)
    }

    @Test
    fun `should include Saudi-specific formatting for amounts`() = runTest {
        // Given
        val request = ExportTransactionsRequest(
            accountId = "acc1",
            format = ExportFormat.CSV,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            locale = "ar-SA"
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any(), any()) 
        } returns Result.Success(sampleTransactions)

        // When
        val result = exportTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val exportData = result.data
        // Should include Saudi currency formatting
        assertTrue(exportData.content.contains("SAR"))
    }
}