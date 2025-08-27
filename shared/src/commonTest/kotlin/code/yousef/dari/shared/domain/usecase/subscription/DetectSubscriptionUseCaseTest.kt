package code.yousef.dari.shared.domain.usecase.subscription

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
class DetectSubscriptionUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private lateinit var detectSubscriptionUseCase: DetectSubscriptionUseCase

    @BeforeTest
    fun setup() {
        detectSubscriptionUseCase = DetectSubscriptionUseCase(transactionRepository)
    }

    @Test
    fun `detect Netflix streaming subscription`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction(
                "1",
                "NETFLIX.COM",
                Money(BigDecimal("56.00"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            ),
            createTransaction(
                "2",
                "NETFLIX.COM",
                Money(BigDecimal("56.00"), "SAR"),
                baseDate.minus(60, DateTimeUnit.DAY)
            ),
            createTransaction(
                "3",
                "NETFLIX.COM",
                Money(BigDecimal("56.00"), "SAR"),
                baseDate.minus(90, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("Netflix", subscription.serviceName)
        assertEquals(SubscriptionCategory.STREAMING, subscription.category)
        assertEquals(Money(BigDecimal("56.00"), "SAR"), subscription.monthlyAmount)
        assertTrue(subscription.isActive)
        assertEquals(3, subscription.transactionHistory.size)
    }

    @Test
    fun `detect Spotify music subscription`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction(
                "1",
                "Spotify Premium",
                Money(BigDecimal("19.99"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            ),
            createTransaction(
                "2",
                "Spotify Premium",
                Money(BigDecimal("19.99"), "SAR"),
                baseDate.minus(60, DateTimeUnit.DAY)
            ),
            createTransaction(
                "3",
                "Spotify Premium",
                Money(BigDecimal("19.99"), "SAR"),
                baseDate.minus(90, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("Spotify", subscription.serviceName)
        assertEquals(SubscriptionCategory.MUSIC, subscription.category)
        assertEquals(Money(BigDecimal("19.99"), "SAR"), subscription.monthlyAmount)
    }

    @Test
    fun `detect STC Pay mobile subscription`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction("1", "STC PAY", Money(BigDecimal("150.00"), "SAR"), baseDate.minus(30, DateTimeUnit.DAY)),
            createTransaction("2", "STC PAY", Money(BigDecimal("150.00"), "SAR"), baseDate.minus(60, DateTimeUnit.DAY)),
            createTransaction("3", "STC PAY", Money(BigDecimal("150.00"), "SAR"), baseDate.minus(90, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("STC Pay", subscription.serviceName)
        assertEquals(SubscriptionCategory.TELECOM, subscription.category)
        assertEquals(Money(BigDecimal("150.00"), "SAR"), subscription.monthlyAmount)
    }

    @Test
    fun `detect gym membership subscription`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction(
                "1",
                "Fitness First",
                Money(BigDecimal("200.00"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            ),
            createTransaction(
                "2",
                "Fitness First",
                Money(BigDecimal("200.00"), "SAR"),
                baseDate.minus(60, DateTimeUnit.DAY)
            ),
            createTransaction(
                "3",
                "Fitness First",
                Money(BigDecimal("200.00"), "SAR"),
                baseDate.minus(90, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("Fitness First", subscription.serviceName)
        assertEquals(SubscriptionCategory.FITNESS, subscription.category)
    }

    @Test
    fun `detect multiple subscriptions`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            // Netflix
            createTransaction(
                "1",
                "NETFLIX.COM",
                Money(BigDecimal("56.00"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            ),
            createTransaction(
                "2",
                "NETFLIX.COM",
                Money(BigDecimal("56.00"), "SAR"),
                baseDate.minus(60, DateTimeUnit.DAY)
            ),
            createTransaction(
                "3",
                "NETFLIX.COM",
                Money(BigDecimal("56.00"), "SAR"),
                baseDate.minus(90, DateTimeUnit.DAY)
            ),

            // Adobe Creative Cloud
            createTransaction(
                "4",
                "ADOBE SYSTEMS",
                Money(BigDecimal("89.99"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            ),
            createTransaction(
                "5",
                "ADOBE SYSTEMS",
                Money(BigDecimal("89.99"), "SAR"),
                baseDate.minus(60, DateTimeUnit.DAY)
            ),
            createTransaction(
                "6",
                "ADOBE SYSTEMS",
                Money(BigDecimal("89.99"), "SAR"),
                baseDate.minus(90, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.serviceName == "Netflix" })
        assertTrue(result.any { it.serviceName == "Adobe Systems" })
    }

    @Test
    fun `detect inactive subscription when payments stopped`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction("1", "HBO Max", Money(BigDecimal("29.99"), "SAR"), baseDate.minus(90, DateTimeUnit.DAY)),
            createTransaction("2", "HBO Max", Money(BigDecimal("29.99"), "SAR"), baseDate.minus(120, DateTimeUnit.DAY)),
            createTransaction("3", "HBO Max", Money(BigDecimal("29.99"), "SAR"), baseDate.minus(150, DateTimeUnit.DAY))
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("HBO Max", subscription.serviceName)
        assertEquals(false, subscription.isActive)
        assertTrue(subscription.lastPaymentDate!! < baseDate.minus(60, DateTimeUnit.DAY))
    }

    @Test
    fun `handle varying subscription amounts within tolerance`() = runTest {
        // Given - Some subscriptions may have slight price variations due to taxes or promotions
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction(
                "1",
                "Microsoft 365",
                Money(BigDecimal("25.00"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            ),
            createTransaction(
                "2",
                "Microsoft 365",
                Money(BigDecimal("26.25"), "SAR"),
                baseDate.minus(60, DateTimeUnit.DAY)
            ), // 5% tax
            createTransaction(
                "3",
                "Microsoft 365",
                Money(BigDecimal("25.00"), "SAR"),
                baseDate.minus(90, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("Microsoft 365", subscription.serviceName)
        assertEquals(SubscriptionCategory.PRODUCTIVITY, subscription.category)
        assertTrue(subscription.hasVariableAmount)
    }

    @Test
    fun `not detect single payment as subscription`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction(
                "1",
                "Single Purchase",
                Money(BigDecimal("99.99"), "SAR"),
                baseDate.minus(30, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `detect annual subscription with yearly payments`() = runTest {
        // Given
        val baseDate = Clock.System.now()
        val transactions = listOf(
            createTransaction(
                "1",
                "Adobe Creative Cloud",
                Money(BigDecimal("1200.00"), "SAR"),
                baseDate.minus(365, DateTimeUnit.DAY)
            ),
            createTransaction(
                "2",
                "Adobe Creative Cloud",
                Money(BigDecimal("1200.00"), "SAR"),
                baseDate.minus(730, DateTimeUnit.DAY)
            )
        )

        coEvery { transactionRepository.getAllTransactions() } returns transactions

        // When
        val result = detectSubscriptionUseCase.execute()

        // Then
        assertEquals(1, result.size)
        val subscription = result.first()
        assertEquals("Adobe Creative Cloud", subscription.serviceName)
        assertEquals(SubscriptionFrequency.YEARLY, subscription.frequency)
        assertEquals(Money(BigDecimal("100.00"), "SAR"), subscription.monthlyAmount) // 1200/12
    }

    private fun createTransaction(
        id: String,
        merchantName: String,
        amount: Money,
        date: kotlinx.datetime.Instant
    ) = Transaction(
        id = id,
        accountId = "account1",
        amount = -amount.amount, // Subscriptions are expenses (negative amounts)
        type = TransactionType.DEBIT,
        description = merchantName,
        merchantName = merchantName,
        date = date,
        categoryId = "subscription"
    )
}
