package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.model.TransactionType
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.testutil.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetectRecurringTransactionUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private lateinit var detectRecurringTransactionUseCase: DetectRecurringTransactionUseCase

    @BeforeTest
    fun setup() {
        detectRecurringTransactionUseCase = DetectRecurringTransactionUseCase(transactionRepository)
    }

    @Test
    fun `detect monthly recurring transaction with same amount and merchant`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val baseAmount = Money(BigDecimal("1200.00"), "SAR")
        val merchant = "Netflix Saudi"

        val transactions = listOf(
            createTransaction("1", merchant, baseAmount, baseDate.minus(30, DateTimeUnit.DAY)),
            createTransaction("2", merchant, baseAmount, baseDate.minus(60, DateTimeUnit.DAY)),
            createTransaction("3", merchant, baseAmount, baseDate.minus(90, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectRecurringTransactionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val pattern = result.first()
        assertEquals("Netflix Saudi", pattern.merchantName)
        assertEquals(Money(BigDecimal("1200.00"), "SAR"), pattern.amount)
        assertEquals(RecurringFrequency.MONTHLY, pattern.frequency)
        assertEquals(3, pattern.detectedTransactions.size)
        assertTrue(pattern.confidence > 0.8)
    }

    @Test
    fun `detect weekly recurring transaction pattern`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val baseAmount = Money(BigDecimal("50.00"), "SAR")
        val merchant = "Starbucks"

        val transactions = listOf(
            createTransaction("1", merchant, baseAmount, baseDate.minus(7, DateTimeUnit.DAY)),
            createTransaction("2", merchant, baseAmount, baseDate.minus(14, DateTimeUnit.DAY)),
            createTransaction("3", merchant, baseAmount, baseDate.minus(21, DateTimeUnit.DAY)),
            createTransaction("4", merchant, baseAmount, baseDate.minus(28, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectRecurringTransactionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val pattern = result.first()
        assertEquals(RecurringFrequency.WEEKLY, pattern.frequency)
        assertEquals(4, pattern.detectedTransactions.size)
        assertTrue(pattern.confidence > 0.85)
    }

    @Test
    fun `detect biweekly recurring transaction pattern`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val baseAmount = Money(BigDecimal("3500.00"), "SAR")
        val merchant = "ACME Corp Payroll"

        val transactions = listOf(
            createTransaction("1", merchant, baseAmount, baseDate.minus(14, DateTimeUnit.DAY)),
            createTransaction("2", merchant, baseAmount, baseDate.minus(28, DateTimeUnit.DAY)),
            createTransaction("3", merchant, baseAmount, baseDate.minus(42, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectRecurringTransactionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val pattern = result.first()
        assertEquals(RecurringFrequency.BIWEEKLY, pattern.frequency)
        assertEquals(3, pattern.detectedTransactions.size)
    }

    @Test
    fun `not detect pattern with insufficient occurrences`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val baseAmount = Money(BigDecimal("100.00"), "SAR")
        val merchant = "Single Purchase"

        val transactions = listOf(
            createTransaction("1", merchant, baseAmount, baseDate.minus(30, DateTimeUnit.DAY)),
            createTransaction("2", merchant, baseAmount, baseDate.minus(60, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectRecurringTransactionUseCase.execute()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `handle varying amounts within tolerance`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val merchant = "Utility Company"

        val transactions = listOf(
            createTransaction("1", merchant, Money(BigDecimal("150.00"), "SAR"), baseDate.minus(30, DateTimeUnit.DAY)),
            createTransaction("2", merchant, Money(BigDecimal("155.50"), "SAR"), baseDate.minus(60, DateTimeUnit.DAY)),
            createTransaction("3", merchant, Money(BigDecimal("148.75"), "SAR"), baseDate.minus(90, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectRecurringTransactionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val pattern = result.first()
        assertEquals("Utility Company", pattern.merchantName)
        assertEquals(RecurringFrequency.MONTHLY, pattern.frequency)
        assertTrue(pattern.hasVariableAmount)
        assertTrue(pattern.confidence > 0.7)
    }

    @Test
    fun `detect multiple recurring patterns`() = runTest {
        // Given
        val baseDate = Clock.System.now()

        val transactions = listOf(
            // Netflix monthly
            createTransaction("1", "Netflix", Money(BigDecimal("50.00"), "SAR"), baseDate.minus(30, DateTimeUnit.DAY)),
            createTransaction("2", "Netflix", Money(BigDecimal("50.00"), "SAR"), baseDate.minus(60, DateTimeUnit.DAY)),
            createTransaction("3", "Netflix", Money(BigDecimal("50.00"), "SAR"), baseDate.minus(90, DateTimeUnit.DAY)),

            // Salary biweekly
            createTransaction(
                "4",
                "ACME Payroll",
                Money(BigDecimal("5000.00"), "SAR"),
                baseDate.minus(14, DateTimeUnit.DAY)
            ),
            createTransaction(
                "5",
                "ACME Payroll",
                Money(BigDecimal("5000.00"), "SAR"),
                baseDate.minus(28, DateTimeUnit.DAY)
            ),
            createTransaction(
                "6",
                "ACME Payroll",
                Money(BigDecimal("5000.00"), "SAR"),
                baseDate.minus(42, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectRecurringTransactionUseCase.execute()

        // Then
        assertEquals(2, result.size)

        val netflixPattern = result.find { it.merchantName == "Netflix" }
        val salaryPattern = result.find { it.merchantName == "ACME Payroll" }

        assertTrue(netflixPattern != null)
        assertTrue(salaryPattern != null)
        assertEquals(RecurringFrequency.MONTHLY, netflixPattern!!.frequency)
        assertEquals(RecurringFrequency.BIWEEKLY, salaryPattern!!.frequency)
    }

    private fun createTransaction(
        id: String,
        merchantName: String,
        amount: Money,
        date: kotlinx.datetime.Instant
    ) = Transaction(
        id = id,
        accountId = "account1",
        amount = amount,
        type = if (amount.amount > BigDecimal.ZERO) TransactionType.CREDIT else TransactionType.DEBIT,
        description = merchantName,
        merchantName = merchantName,
        date = date,
        categoryId = "category1"
    )
}
