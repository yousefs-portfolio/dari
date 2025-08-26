package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Category
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

class ForecastMonthlySpendingUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: ForecastMonthlySpendingUseCase

    @BeforeTest
    fun setup() {
        transactionRepository = mockk()
        useCase = ForecastMonthlySpendingUseCase(transactionRepository)
    }

    @Test
    fun `forecast monthly spending with sufficient historical data`() = runTest {
        // Given
        val year = 2024
        val month = 3
        val categoryId = "groceries"

        val historicalTransactions = listOf(
            createTransaction("1", LocalDateTime(2024, 1, 5, 10, 0), Money(400.0, "SAR"), categoryId),
            createTransaction("2", LocalDateTime(2024, 1, 15, 14, 0), Money(300.0, "SAR"), categoryId),
            createTransaction("3", LocalDateTime(2024, 1, 25, 16, 0), Money(200.0, "SAR"), categoryId),
            createTransaction("4", LocalDateTime(2024, 2, 3, 9, 0), Money(450.0, "SAR"), categoryId),
            createTransaction("5", LocalDateTime(2024, 2, 18, 12, 0), Money(350.0, "SAR"), categoryId),
            createTransaction("6", LocalDateTime(2024, 2, 28, 18, 0), Money(250.0, "SAR"), categoryId)
        )

        val currentMonthTransactions = listOf(
            createTransaction("7", LocalDateTime(2024, 3, 2, 11, 0), Money(380.0, "SAR"), categoryId),
            createTransaction("8", LocalDateTime(2024, 3, 12, 15, 0), Money(280.0, "SAR"), categoryId)
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } returns historicalTransactions

        coEvery { 
            transactionRepository.getTransactionsByMonth(year, month) 
        } returns currentMonthTransactions

        // When
        val result = useCase(year, month, categoryId)

        // Then
        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        
        assertEquals(categoryId, forecast.categoryId)
        assertEquals(year, forecast.year)
        assertEquals(month, forecast.month)
        
        // Historical averages: Jan = 900 SAR, Feb = 1050 SAR, Average = 975 SAR
        assertEquals(975.0, forecast.historicalAverage.amount, 0.01)
        
        // Current spending: 380 + 280 = 660 SAR
        assertEquals(660.0, forecast.currentSpending.amount)
        
        // Should be around day 12 of 31 days, so roughly 39% through month
        assertTrue(forecast.daysElapsed > 0)
        assertTrue(forecast.daysInMonth > 0)
        
        // Projected spending should be extrapolated based on current pace
        assertTrue(forecast.projectedSpending.amount > forecast.currentSpending.amount)
        
        // Confidence should be high with sufficient data
        assertTrue(forecast.confidence >= 0.7)

        coVerify { transactionRepository.getTransactionsByDateRange(any(), any()) }
        coVerify { transactionRepository.getTransactionsByMonth(year, month) }
    }

    @Test
    fun `forecast monthly spending with limited historical data`() = runTest {
        // Given
        val year = 2024
        val month = 2
        val categoryId = "entertainment"

        val historicalTransactions = listOf(
            createTransaction("1", LocalDateTime(2024, 1, 15, 20, 0), Money(150.0, "SAR"), categoryId)
        )

        val currentMonthTransactions = listOf(
            createTransaction("2", LocalDateTime(2024, 2, 10, 19, 0), Money(200.0, "SAR"), categoryId)
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } returns historicalTransactions

        coEvery { 
            transactionRepository.getTransactionsByMonth(year, month) 
        } returns currentMonthTransactions

        // When
        val result = useCase(year, month, categoryId)

        // Then
        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        
        assertEquals(categoryId, forecast.categoryId)
        assertEquals(150.0, forecast.historicalAverage.amount)
        assertEquals(200.0, forecast.currentSpending.amount)
        
        // Confidence should be lower with limited data
        assertTrue(forecast.confidence < 0.7)

        coVerify { transactionRepository.getTransactionsByDateRange(any(), any()) }
        coVerify { transactionRepository.getTransactionsByMonth(year, month) }
    }

    @Test
    fun `forecast monthly spending with no historical data`() = runTest {
        // Given
        val year = 2024
        val month = 1
        val categoryId = "new_category"

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } returns emptyList()

        coEvery { 
            transactionRepository.getTransactionsByMonth(year, month) 
        } returns emptyList()

        // When
        val result = useCase(year, month, categoryId)

        // Then
        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        
        assertEquals(categoryId, forecast.categoryId)
        assertEquals(0.0, forecast.historicalAverage.amount)
        assertEquals(0.0, forecast.currentSpending.amount)
        assertEquals(0.0, forecast.projectedSpending.amount)
        
        // Confidence should be very low with no data
        assertEquals(0.0, forecast.confidence)
    }

    @Test
    fun `forecast monthly spending at end of month`() = runTest {
        // Given
        val year = 2024
        val month = 1 // January has 31 days
        val categoryId = "groceries"

        val historicalTransactions = listOf(
            createTransaction("1", LocalDateTime(2023, 12, 5, 10, 0), Money(500.0, "SAR"), categoryId),
            createTransaction("2", LocalDateTime(2023, 11, 15, 14, 0), Money(600.0, "SAR"), categoryId)
        )

        val currentMonthTransactions = listOf(
            createTransaction("3", LocalDateTime(2024, 1, 15, 12, 0), Money(550.0, "SAR"), categoryId),
            createTransaction("4", LocalDateTime(2024, 1, 30, 16, 0), Money(100.0, "SAR"), categoryId) // Near end of month
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } returns historicalTransactions

        coEvery { 
            transactionRepository.getTransactionsByMonth(year, month) 
        } returns currentMonthTransactions

        // When
        val result = useCase(year, month, categoryId)

        // Then
        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        
        assertEquals(650.0, forecast.currentSpending.amount)
        
        // Since we're near the end of the month, projection should be close to current spending
        assertTrue(
            kotlin.math.abs(forecast.projectedSpending.amount - forecast.currentSpending.amount) < 100.0
        )
        
        // Confidence should be high when month is nearly complete
        assertTrue(forecast.confidence >= 0.8)
    }

    @Test
    fun `forecast monthly spending handles repository error`() = runTest {
        // Given
        val error = Exception("Database error")
        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } throws error

        // When
        val result = useCase(2024, 1, "category")

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `forecast monthly spending with seasonal patterns`() = runTest {
        // Given - January and December typically have higher spending (holidays)
        val year = 2024
        val month = 12
        val categoryId = "gifts"

        val historicalTransactions = listOf(
            // December 2023 - high spending
            createTransaction("1", LocalDateTime(2023, 12, 15, 14, 0), Money(800.0, "SAR"), categoryId),
            createTransaction("2", LocalDateTime(2023, 12, 25, 16, 0), Money(600.0, "SAR"), categoryId),
            // June 2023 - low spending
            createTransaction("3", LocalDateTime(2023, 6, 10, 12, 0), Money(50.0, "SAR"), categoryId),
            // December 2022 - high spending
            createTransaction("4", LocalDateTime(2022, 12, 20, 18, 0), Money(900.0, "SAR"), categoryId)
        )

        val currentMonthTransactions = listOf(
            createTransaction("5", LocalDateTime(2024, 12, 10, 15, 0), Money(400.0, "SAR"), categoryId)
        )

        coEvery { 
            transactionRepository.getTransactionsByDateRange(any(), any()) 
        } returns historicalTransactions

        coEvery { 
            transactionRepository.getTransactionsByMonth(year, month) 
        } returns currentMonthTransactions

        // When
        val result = useCase(year, month, categoryId)

        // Then
        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        
        // Should weight recent December data more heavily
        assertTrue(forecast.historicalAverage.amount > 600.0) // Should be influenced by high Dec spending
        assertEquals(400.0, forecast.currentSpending.amount)
        
        // Projection should account for typical December spending patterns
        assertTrue(forecast.projectedSpending.amount > forecast.currentSpending.amount)
    }

    private fun createTransaction(
        id: String,
        date: LocalDateTime,
        amount: Money,
        categoryId: String
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount,
            description = "Test transaction",
            date = date,
            categoryId = categoryId,
            category = null,
            merchantName = "Test Merchant",
            type = Transaction.Type.EXPENSE,
            tags = emptyList(),
            receiptImagePath = null
        )
    }
}