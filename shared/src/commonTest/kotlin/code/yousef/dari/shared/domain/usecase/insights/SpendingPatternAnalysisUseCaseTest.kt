package code.yousef.dari.shared.domain.usecase.insights

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
class SpendingPatternAnalysisUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private lateinit var spendingPatternAnalysisUseCase: SpendingPatternAnalysisUseCase

    @BeforeTest
    fun setup() {
        spendingPatternAnalysisUseCase = SpendingPatternAnalysisUseCase(transactionRepository)
    }

    @Test
    fun `analyze weekly spending pattern shows increased weekend spending`() = runTest {
        // Given - More spending on weekends
        val baseDate = Clock.System.now()
        val transactions = listOf(
            // Weekend transactions (higher amounts)
            createTransaction("1", Money(BigDecimal("-200.00"), "SAR"), baseDate.minus(0, DateTimeUnit.DAY)), // Sunday
            createTransaction(
                "2",
                Money(BigDecimal("-150.00"), "SAR"),
                baseDate.minus(1, DateTimeUnit.DAY)
            ), // Saturday
            createTransaction("3", Money(BigDecimal("-180.00"), "SAR"), baseDate.minus(7, DateTimeUnit.DAY)), // Sunday

            // Weekday transactions (lower amounts)
            createTransaction("4", Money(BigDecimal("-50.00"), "SAR"), baseDate.minus(2, DateTimeUnit.DAY)), // Friday
            createTransaction("5", Money(BigDecimal("-40.00"), "SAR"), baseDate.minus(3, DateTimeUnit.DAY)), // Thursday
            createTransaction(
                "6",
                Money(BigDecimal("-60.00"), "SAR"),
                baseDate.minus(4, DateTimeUnit.DAY)
            )  // Wednesday
        )

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.analyzeWeeklyPattern(30)

        // Then
        assertTrue(result.isSuccess)
        val pattern = result.getOrNull()!!

        // Weekend spending should be higher
        val weekendSpending = pattern.weekendAverageAmount
        val weekdaySpending = pattern.weekdayAverageAmount

        assertTrue(weekendSpending > weekdaySpending)
        assertEquals("Weekend Spender", pattern.patternType)
        assertTrue(pattern.insights.any { it.contains("weekend") })
    }

    @Test
    fun `analyze monthly spending trend shows increasing pattern`() = runTest {
        // Given - Increasing spending over months
        val baseDate = Clock.System.now()
        val transactions = listOf(
            // Current month (highest)
            createTransaction("1", Money(BigDecimal("-3000.00"), "SAR"), baseDate.minus(5, DateTimeUnit.DAY)),
            createTransaction("2", Money(BigDecimal("-2500.00"), "SAR"), baseDate.minus(10, DateTimeUnit.DAY)),

            // Last month (medium)
            createTransaction("3", Money(BigDecimal("-2000.00"), "SAR"), baseDate.minus(35, DateTimeUnit.DAY)),
            createTransaction("4", Money(BigDecimal("-1800.00"), "SAR"), baseDate.minus(40, DateTimeUnit.DAY)),

            // Two months ago (lowest)
            createTransaction("5", Money(BigDecimal("-1200.00"), "SAR"), baseDate.minus(65, DateTimeUnit.DAY)),
            createTransaction("6", Money(BigDecimal("-1000.00"), "SAR"), baseDate.minus(70, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.analyzeMonthlyTrend(90)

        // Then
        assertTrue(result.isSuccess)
        val trend = result.getOrNull()!!

        assertEquals(SpendingTrend.INCREASING, trend.trend)
        assertTrue(trend.growthRate > 0)
        assertTrue(trend.monthlyAmounts.size == 3)
        assertTrue(trend.insights.any { it.contains("increasing") })
    }

    @Test
    fun `analyze spending by category identifies top categories`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction("1", Money(BigDecimal("-800.00"), "SAR"), baseDate, "Groceries"),
            createTransaction("2", Money(BigDecimal("-600.00"), "SAR"), baseDate, "Groceries"),
            createTransaction("3", Money(BigDecimal("-400.00"), "SAR"), baseDate, "Dining"),
            createTransaction("4", Money(BigDecimal("-300.00"), "SAR"), baseDate, "Dining"),
            createTransaction("5", Money(BigDecimal("-200.00"), "SAR"), baseDate, "Transportation"),
            createTransaction("6", Money(BigDecimal("-100.00"), "SAR"), baseDate, "Entertainment")
        )

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.analyzeCategorySpending(30)

        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!

        assertEquals("Groceries", analysis.topCategory.categoryName)
        assertEquals(Money(BigDecimal("1400.00"), "SAR"), analysis.topCategory.totalSpent)
        assertEquals(4, analysis.categoryBreakdown.size)
        assertTrue(analysis.categoryBreakdown[0].percentage > 50.0) // Groceries should be >50%
    }

    @Test
    fun `detect merchant loyalty patterns`() = runTest {
        // Given - Frequent visits to same merchants
        val baseDate = Clock.System.now()
        val transactions = listOf(
            // Starbucks - 8 visits
            createTransaction("1", Money(BigDecimal("-25.00"), "SAR"), baseDate, "Dining", "Starbucks"),
            createTransaction(
                "2",
                Money(BigDecimal("-30.00"), "SAR"),
                baseDate.minus(3, DateTimeUnit.DAY),
                "Dining",
                "Starbucks"
            ),
            createTransaction(
                "3",
                Money(BigDecimal("-28.00"), "SAR"),
                baseDate.minus(7, DateTimeUnit.DAY),
                "Dining",
                "Starbucks"
            ),

            // Panda - 5 visits
            createTransaction(
                "4",
                Money(BigDecimal("-150.00"), "SAR"),
                baseDate.minus(2, DateTimeUnit.DAY),
                "Groceries",
                "Panda"
            ),
            createTransaction(
                "5",
                Money(BigDecimal("-180.00"), "SAR"),
                baseDate.minus(10, DateTimeUnit.DAY),
                "Groceries",
                "Panda"
            ),

            // Other merchants - single visits
            createTransaction(
                "6",
                Money(BigDecimal("-45.00"), "SAR"),
                baseDate.minus(1, DateTimeUnit.DAY),
                "Dining",
                "McDonald's"
            )
        )

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.analyzeMerchantLoyalty(30)

        // Then
        assertTrue(result.isSuccess)
        val loyalty = result.getOrNull()!!

        assertTrue(loyalty.loyalMerchants.isNotEmpty())
        val topMerchant = loyalty.loyalMerchants.first()
        assertEquals("Starbucks", topMerchant.merchantName)
        assertTrue(topMerchant.visitFrequency > 2.0) // More than 2 visits per month
    }

    @Test
    fun `identify seasonal spending patterns`() = runTest {
        // Given - Higher spending in certain months (like Ramadan/Eid)
        val baseDate = Clock.System.now()
        val transactions = mutableListOf<Transaction>()

        // Simulate Ramadan period with higher food spending
        for (i in 1..30) {
            transactions.add(
                createTransaction(
                    "ramadan_$i",
                    Money(BigDecimal("-200.00"), "SAR"),
                    baseDate.minus(i, DateTimeUnit.DAY),
                    "Groceries"
                )
            )
        }

        // Normal period with lower spending
        for (i in 31..60) {
            transactions.add(
                createTransaction(
                    "normal_$i",
                    Money(BigDecimal("-100.00"), "SAR"),
                    baseDate.minus(i, DateTimeUnit.DAY),
                    "Groceries"
                )
            )
        }

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.analyzeSeasonalPatterns(90)

        // Then
        assertTrue(result.isSuccess)
        val patterns = result.getOrNull()!!

        assertTrue(patterns.isNotEmpty())
        val recentPattern = patterns.first()
        assertTrue(recentPattern.averageAmount > Money(BigDecimal("150.00"), "SAR"))
    }

    @Test
    fun `analyze spending velocity shows daily vs weekly patterns`() = runTest {
        // Given - Consistent daily small spends vs weekly large spends
        val baseDate = Clock.System.now()
        val transactions = listOf(
            // Daily small transactions
            createTransaction("1", Money(BigDecimal("-20.00"), "SAR"), baseDate),
            createTransaction("2", Money(BigDecimal("-25.00"), "SAR"), baseDate.minus(1, DateTimeUnit.DAY)),
            createTransaction("3", Money(BigDecimal("-22.00"), "SAR"), baseDate.minus(2, DateTimeUnit.DAY)),
            createTransaction("4", Money(BigDecimal("-18.00"), "SAR"), baseDate.minus(3, DateTimeUnit.DAY)),

            // Weekly large transaction
            createTransaction("5", Money(BigDecimal("-300.00"), "SAR"), baseDate.minus(7, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.analyzeSpendingVelocity(30)

        // Then
        assertTrue(result.isSuccess)
        val velocity = result.getOrNull()!!

        assertTrue(velocity.dailyTransactionCount > 0)
        assertTrue(velocity.averageTransactionAmount < Money(BigDecimal("100.00"), "SAR"))
        assertEquals(SpendingVelocityType.FREQUENT_SMALL, velocity.velocityType)
    }

    @Test
    fun `provide spending insights and recommendations`() = runTest {
        // Given - Mix of spending patterns
        val baseDate = Clock.System.now()
        val transactions = listOf(
            // High dining spending
            createTransaction("1", Money(BigDecimal("-500.00"), "SAR"), baseDate, "Dining"),
            createTransaction("2", Money(BigDecimal("-300.00"), "SAR"), baseDate.minus(5, DateTimeUnit.DAY), "Dining"),

            // Subscription spending
            createTransaction("3", Money(BigDecimal("-60.00"), "SAR"), baseDate, "Entertainment", "Netflix"),
            createTransaction("4", Money(BigDecimal("-30.00"), "SAR"), baseDate, "Entertainment", "Spotify"),

            // Essential spending
            createTransaction("5", Money(BigDecimal("-200.00"), "SAR"), baseDate, "Groceries"),
            createTransaction("6", Money(BigDecimal("-150.00"), "SAR"), baseDate, "Utilities")
        )

        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions

        // When
        val result = spendingPatternAnalysisUseCase.generateInsights(30)

        // Then
        assertTrue(result.isSuccess)
        val insights = result.getOrNull()!!

        assertTrue(insights.insights.isNotEmpty())
        assertTrue(insights.recommendations.isNotEmpty())

        // Should identify high dining spending
        assertTrue(insights.insights.any { it.contains("dining") || it.contains("restaurant") })

        // Should suggest ways to reduce dining costs
        assertTrue(insights.recommendations.any { it.contains("cook") || it.contains("meal") })
    }

    private fun createTransaction(
        id: String,
        amount: Money,
        date: kotlinx.datetime.Instant,
        categoryName: String = "Other",
        merchantName: String = "Unknown Merchant"
    ) = Transaction(
        id = id,
        accountId = "account1",
        amount = amount,
        type = TransactionType.DEBIT,
        description = merchantName,
        merchantName = merchantName,
        date = date,
        categoryId = "cat_${categoryName.lowercase()}"
    )
}
