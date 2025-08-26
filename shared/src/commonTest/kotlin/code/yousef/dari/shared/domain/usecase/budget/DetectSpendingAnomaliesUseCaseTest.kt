package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.test.util.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetectSpendingAnomaliesUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: DetectSpendingAnomaliesUseCase

    @BeforeTest
    fun setup() {
        transactionRepository = mockk()
        useCase = DetectSpendingAnomaliesUseCase(transactionRepository)
    }

    @Test
    fun `detect unusually high single transaction amount`() = runTest {
        // Given
        val transactions = listOf(
            createTransaction("1", LocalDateTime(2024, 3, 1, 10, 0), Money(50.0, "SAR"), "groceries"),
            createTransaction("2", LocalDateTime(2024, 3, 3, 15, 0), Money(75.0, "SAR"), "groceries"),
            createTransaction("3", LocalDateTime(2024, 3, 5, 12, 0), Money(60.0, "SAR"), "groceries"),
            createTransaction("4", LocalDateTime(2024, 3, 7, 14, 0), Money(80.0, "SAR"), "groceries"),
            createTransaction("5", LocalDateTime(2024, 3, 10, 11, 0), Money(500.0, "SAR"), "groceries"), // Anomaly!
            createTransaction("6", LocalDateTime(2024, 3, 12, 16, 0), Money(65.0, "SAR"), "groceries"),
            createTransaction("7", LocalDateTime(2024, 3, 15, 13, 0), Money(70.0, "SAR"), "groceries")
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        assertEquals(1, anomalies.size)
        val anomaly = anomalies.first()
        
        assertEquals("5", anomaly.transactionId)
        assertEquals(DetectedAnomaly.Type.UNUSUALLY_HIGH_AMOUNT, anomaly.type)
        assertEquals(DetectedAnomaly.Severity.HIGH, anomaly.severity)
        assertTrue(anomaly.description.contains("500.00 SAR"))
        assertTrue(anomaly.description.contains("groceries"))

        coVerify { transactionRepository.getTransactionsByDateRange(startDate, endDate) }
    }

    @Test
    fun `detect unusually high category spending for period`() = runTest {
        // Given - Normal entertainment spending is ~200 SAR/month
        val transactions = listOf(
            // January - normal
            createTransaction("1", LocalDateTime(2024, 1, 5, 20, 0), Money(50.0, "SAR"), "entertainment"),
            createTransaction("2", LocalDateTime(2024, 1, 15, 19, 0), Money(80.0, "SAR"), "entertainment"),
            createTransaction("3", LocalDateTime(2024, 1, 25, 21, 0), Money(70.0, "SAR"), "entertainment"),
            
            // February - normal
            createTransaction("4", LocalDateTime(2024, 2, 8, 18, 0), Money(60.0, "SAR"), "entertainment"),
            createTransaction("5", LocalDateTime(2024, 2, 18, 20, 0), Money(90.0, "SAR"), "entertainment"),
            createTransaction("6", LocalDateTime(2024, 2, 28, 19, 0), Money(55.0, "SAR"), "entertainment"),
            
            // March - anomalously high spending
            createTransaction("7", LocalDateTime(2024, 3, 2, 20, 0), Money(150.0, "SAR"), "entertainment"),
            createTransaction("8", LocalDateTime(2024, 3, 10, 21, 0), Money(200.0, "SAR"), "entertainment"),
            createTransaction("9", LocalDateTime(2024, 3, 15, 19, 0), Money(180.0, "SAR"), "entertainment"),
            createTransaction("10", LocalDateTime(2024, 3, 22, 20, 0), Money(170.0, "SAR"), "entertainment"),
            createTransaction("11", LocalDateTime(2024, 3, 28, 18, 0), Money(300.0, "SAR"), "entertainment") // Big concert
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } returns transactions

        coEvery { 
            transactionRepository.getTransactionsByDateRange(startDate, endDate) 
        } returns transactions.filter { it.date >= startDate && it.date <= endDate }

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        // Should detect high category spending and/or individual high transactions
        assertTrue(anomalies.isNotEmpty())
        
        // Should detect the 300 SAR concert ticket as individual anomaly
        val highAmountAnomaly = anomalies.find { it.transactionId == "11" }
        assertTrue(highAmountAnomaly != null)
        assertEquals(DetectedAnomaly.Type.UNUSUALLY_HIGH_AMOUNT, highAmountAnomaly.type)
        
        // Might also detect high category spending
        val categoryAnomaly = anomalies.find { it.type == DetectedAnomaly.Type.UNUSUALLY_HIGH_CATEGORY_SPENDING }
        if (categoryAnomaly != null) {
            assertTrue(categoryAnomaly.description.contains("entertainment"))
            assertTrue(categoryAnomaly.description.contains("1000"))
        }
    }

    @Test
    fun `detect frequent transactions at same merchant`() = runTest {
        // Given - Multiple transactions at same merchant in short time
        val transactions = listOf(
            createTransaction("1", LocalDateTime(2024, 3, 1, 14, 30), Money(25.0, "SAR"), "food", "Coffee Shop"),
            createTransaction("2", LocalDateTime(2024, 3, 1, 14, 35), Money(15.0, "SAR"), "food", "Coffee Shop"),
            createTransaction("3", LocalDateTime(2024, 3, 1, 14, 40), Money(30.0, "SAR"), "food", "Coffee Shop"),
            createTransaction("4", LocalDateTime(2024, 3, 1, 14, 45), Money(20.0, "SAR"), "food", "Coffee Shop"),
            createTransaction("5", LocalDateTime(2024, 3, 2, 12, 0), Money(50.0, "SAR"), "groceries", "Supermarket"),
            createTransaction("6", LocalDateTime(2024, 3, 3, 18, 0), Money(80.0, "SAR"), "transport", "Gas Station")
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        val frequentMerchantAnomaly = anomalies.find { it.type == DetectedAnomaly.Type.FREQUENT_MERCHANT_TRANSACTIONS }
        assertTrue(frequentMerchantAnomaly != null)
        assertTrue(frequentMerchantAnomaly!!.description.contains("Coffee Shop"))
        assertTrue(frequentMerchantAnomaly.description.contains("4 transactions"))
    }

    @Test
    fun `detect unusual spending time pattern`() = runTest {
        // Given - Transactions at unusual hours (3 AM)
        val transactions = listOf(
            createTransaction("1", LocalDateTime(2024, 3, 5, 3, 15), Money(200.0, "SAR"), "entertainment", "Online Store"),
            createTransaction("2", LocalDateTime(2024, 3, 10, 3, 30), Money(150.0, "SAR"), "shopping", "Online Store"),
            createTransaction("3", LocalDateTime(2024, 3, 12, 14, 0), Money(50.0, "SAR"), "food", "Restaurant"), // Normal time
            createTransaction("4", LocalDateTime(2024, 3, 15, 2, 45), Money(300.0, "SAR"), "electronics", "Tech Store"),
            createTransaction("5", LocalDateTime(2024, 3, 20, 12, 30), Money(80.0, "SAR"), "groceries", "Supermarket") // Normal time
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        val lateNightAnomalies = anomalies.filter { it.type == DetectedAnomaly.Type.UNUSUAL_TIME_PATTERN }
        assertTrue(lateNightAnomalies.isNotEmpty())
        
        // Should detect at least one late-night transaction
        val lateNightAnomaly = lateNightAnomalies.first()
        assertTrue(lateNightAnomaly.description.contains("late night") || lateNightAnomaly.description.contains("unusual time"))
    }

    @Test
    fun `detect duplicate transactions`() = runTest {
        // Given - Potential duplicate transactions
        val transactions = listOf(
            createTransaction("1", LocalDateTime(2024, 3, 5, 14, 30), Money(89.99, "SAR"), "shopping", "Store ABC"),
            createTransaction("2", LocalDateTime(2024, 3, 5, 14, 31), Money(89.99, "SAR"), "shopping", "Store ABC"), // Potential duplicate
            createTransaction("3", LocalDateTime(2024, 3, 10, 12, 0), Money(50.0, "SAR"), "food", "Restaurant"),
            createTransaction("4", LocalDateTime(2024, 3, 15, 16, 45), Money(125.50, "SAR"), "groceries", "Supermarket"),
            createTransaction("5", LocalDateTime(2024, 3, 15, 16, 47), Money(125.50, "SAR"), "groceries", "Supermarket") // Potential duplicate
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        val duplicateAnomalies = anomalies.filter { it.type == DetectedAnomaly.Type.POTENTIAL_DUPLICATE }
        assertTrue(duplicateAnomalies.size >= 2) // Should detect both pairs
        
        // Check that it found the duplicate pairs
        val duplicateTransactionIds = duplicateAnomalies.map { it.transactionId }
        assertTrue(duplicateTransactionIds.contains("2")) // Second transaction in first pair
        assertTrue(duplicateTransactionIds.contains("5")) // Second transaction in second pair
    }

    @Test
    fun `detect weekend vs weekday spending anomaly`() = runTest {
        // Given - Much higher weekend spending than weekdays
        val transactions = listOf(
            // Weekdays - modest spending
            createTransaction("1", LocalDateTime(2024, 3, 4, 12, 0), Money(25.0, "SAR"), "food"), // Monday
            createTransaction("2", LocalDateTime(2024, 3, 5, 13, 0), Money(30.0, "SAR"), "food"), // Tuesday
            createTransaction("3", LocalDateTime(2024, 3, 6, 14, 0), Money(28.0, "SAR"), "food"), // Wednesday
            createTransaction("4", LocalDateTime(2024, 3, 7, 11, 0), Money(35.0, "SAR"), "food"), // Thursday
            createTransaction("5", LocalDateTime(2024, 3, 8, 12, 30), Money(32.0, "SAR"), "food"), // Friday
            
            // Weekends - high spending
            createTransaction("6", LocalDateTime(2024, 3, 9, 20, 0), Money(200.0, "SAR"), "entertainment"), // Saturday
            createTransaction("7", LocalDateTime(2024, 3, 9, 21, 30), Money(150.0, "SAR"), "food"), // Saturday
            createTransaction("8", LocalDateTime(2024, 3, 10, 14, 0), Money(180.0, "SAR"), "shopping"), // Sunday
            createTransaction("9", LocalDateTime(2024, 3, 10, 16, 0), Money(120.0, "SAR"), "entertainment") // Sunday
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        // Should detect pattern anomaly or high individual transactions
        assertTrue(anomalies.isNotEmpty())
        
        // May detect high weekend spending pattern
        val patternAnomaly = anomalies.find { it.type == DetectedAnomaly.Type.UNUSUAL_TIME_PATTERN }
        if (patternAnomaly != null) {
            assertTrue(patternAnomaly.description.contains("weekend") || patternAnomaly.description.contains("pattern"))
        }
    }

    @Test
    fun `no anomalies detected with normal spending patterns`() = runTest {
        // Given - Normal, consistent spending
        val transactions = listOf(
            createTransaction("1", LocalDateTime(2024, 3, 1, 12, 0), Money(50.0, "SAR"), "groceries"),
            createTransaction("2", LocalDateTime(2024, 3, 3, 14, 0), Money(75.0, "SAR"), "groceries"),
            createTransaction("3", LocalDateTime(2024, 3, 5, 13, 30), Money(60.0, "SAR"), "groceries"),
            createTransaction("4", LocalDateTime(2024, 3, 8, 11, 15), Money(80.0, "SAR"), "groceries"),
            createTransaction("5", LocalDateTime(2024, 3, 10, 15, 45), Money(65.0, "SAR"), "groceries"),
            createTransaction("6", LocalDateTime(2024, 3, 12, 14, 20), Money(70.0, "SAR"), "groceries"),
            createTransaction("7", LocalDateTime(2024, 3, 15, 12, 30), Money(55.0, "SAR"), "groceries")
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        // Should detect no anomalies with consistent spending
        assertTrue(anomalies.isEmpty() || anomalies.all { it.severity == DetectedAnomaly.Severity.LOW })
    }

    @Test
    fun `handles repository error`() = runTest {
        // Given
        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)
        val error = Exception("Database error")

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } throws error

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `handles empty transaction list`() = runTest {
        // Given
        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns emptyList()

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        assertTrue(anomalies.isEmpty())
    }

    @Test
    fun `detect multiple types of anomalies in same dataset`() = runTest {
        // Given - Dataset with multiple anomaly types
        val transactions = listOf(
            // Normal transactions
            createTransaction("1", LocalDateTime(2024, 3, 1, 12, 0), Money(50.0, "SAR"), "groceries"),
            createTransaction("2", LocalDateTime(2024, 3, 3, 14, 0), Money(55.0, "SAR"), "groceries"),
            
            // High amount anomaly
            createTransaction("3", LocalDateTime(2024, 3, 5, 13, 0), Money(500.0, "SAR"), "groceries"), 
            
            // Duplicate anomaly
            createTransaction("4", LocalDateTime(2024, 3, 8, 16, 30), Money(99.99, "SAR"), "shopping", "Store XYZ"),
            createTransaction("5", LocalDateTime(2024, 3, 8, 16, 31), Money(99.99, "SAR"), "shopping", "Store XYZ"),
            
            // Late night anomaly
            createTransaction("6", LocalDateTime(2024, 3, 10, 2, 45), Money(150.0, "SAR"), "entertainment"),
            
            // Frequent merchant
            createTransaction("7", LocalDateTime(2024, 3, 12, 10, 0), Money(20.0, "SAR"), "food", "Coffee Place"),
            createTransaction("8", LocalDateTime(2024, 3, 12, 10, 15), Money(25.0, "SAR"), "food", "Coffee Place"),
            createTransaction("9", LocalDateTime(2024, 3, 12, 10, 30), Money(18.0, "SAR"), "food", "Coffee Place"),
            createTransaction("10", LocalDateTime(2024, 3, 12, 10, 45), Money(22.0, "SAR"), "food", "Coffee Place")
        )

        val startDate = LocalDateTime(2024, 3, 1, 0, 0)
        val endDate = LocalDateTime(2024, 3, 31, 23, 59)

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions
        coEvery { transactionRepository.getTransactionsByDateRange(startDate, endDate) } returns transactions

        // When
        val result = useCase(startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        val anomalies = result.getOrThrow()
        
        // Should detect multiple types
        assertTrue(anomalies.size >= 3)
        
        val anomalyTypes = anomalies.map { it.type }.distinct()
        assertTrue(anomalyTypes.contains(DetectedAnomaly.Type.UNUSUALLY_HIGH_AMOUNT))
        assertTrue(
            anomalyTypes.contains(DetectedAnomaly.Type.POTENTIAL_DUPLICATE) ||
            anomalyTypes.contains(DetectedAnomaly.Type.FREQUENT_MERCHANT_TRANSACTIONS) ||
            anomalyTypes.contains(DetectedAnomaly.Type.UNUSUAL_TIME_PATTERN)
        )
    }

    private fun createTransaction(
        id: String,
        date: LocalDateTime,
        amount: Money,
        categoryId: String,
        merchantName: String = "Test Merchant"
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount,
            description = "Test transaction",
            date = date,
            categoryId = categoryId,
            category = null,
            merchantName = merchantName,
            type = Transaction.Type.EXPENSE,
            tags = emptyList(),
            receiptImagePath = null
        )
    }
}