package code.yousef.dari.shared.domain.usecase.subscription

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Subscription
import code.yousef.dari.shared.domain.model.SubscriptionStatus
import code.yousef.dari.shared.domain.repository.SubscriptionRepository
import code.yousef.dari.shared.testutil.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
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
class TrackSubscriptionUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private lateinit var trackSubscriptionUseCase: TrackSubscriptionUseCase

    @BeforeTest
    fun setup() {
        trackSubscriptionUseCase = TrackSubscriptionUseCase(subscriptionRepository)
    }

    @Test
    fun `create new subscription from detected subscription`() = runTest {
        // Given
        val detectedSubscription = createDetectedNetflixSubscription()
        coEvery { subscriptionRepository.create(any()) } returns Unit

        // When
        val result = trackSubscriptionUseCase.createFromDetected(detectedSubscription)

        // Then
        assertTrue(result.isSuccess)
        val subscription = result.getOrNull()!!
        assertEquals("Netflix", subscription.serviceName)
        assertEquals(SubscriptionCategory.STREAMING, subscription.category)
        assertEquals(Money(BigDecimal("56.00"), "SAR"), subscription.monthlyAmount)
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)

        coVerify { subscriptionRepository.create(any()) }
    }

    @Test
    fun `update subscription status to cancelled`() = runTest {
        // Given
        val subscription = createActiveSubscription()
        coEvery { subscriptionRepository.getById("sub_1") } returns subscription
        coEvery { subscriptionRepository.update(any()) } returns Unit

        // When
        val result = trackSubscriptionUseCase.updateStatus("sub_1", SubscriptionStatus.CANCELLED, "User cancelled")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            subscriptionRepository.update(
                match { it.status == SubscriptionStatus.CANCELLED && it.cancellationReason == "User cancelled" }
            )
        }
    }

    @Test
    fun `update subscription status to paused`() = runTest {
        // Given
        val subscription = createActiveSubscription()
        coEvery { subscriptionRepository.getById("sub_1") } returns subscription
        coEvery { subscriptionRepository.update(any()) } returns Unit

        // When
        val result = trackSubscriptionUseCase.updateStatus("sub_1", SubscriptionStatus.PAUSED, "Temporary pause")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            subscriptionRepository.update(
                match { it.status == SubscriptionStatus.PAUSED }
            )
        }
    }

    @Test
    fun `track subscription renewal`() = runTest {
        // Given
        val subscription = createActiveSubscription()
        coEvery { subscriptionRepository.getById("sub_1") } returns subscription
        coEvery { subscriptionRepository.update(any()) } returns Unit

        // When
        val result = trackSubscriptionUseCase.recordRenewal("sub_1", Money(BigDecimal("60.00"), "SAR"))

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            subscriptionRepository.update(
                match {
                    it.lastRenewalDate != null &&
                        it.renewalCount == subscription.renewalCount + 1 &&
                        it.monthlyAmount == Money(BigDecimal("60.00"), "SAR")
                }
            )
        }
    }

    @Test
    fun `calculate total monthly subscription cost`() = runTest {
        // Given
        val subscriptions = listOf(
            createActiveSubscription("Netflix", Money(BigDecimal("56.00"), "SAR")),
            createActiveSubscription("Spotify", Money(BigDecimal("20.00"), "SAR")),
            createActiveSubscription("Adobe", Money(BigDecimal("90.00"), "SAR"))
        )
        coEvery { subscriptionRepository.getAllActive() } returns subscriptions

        // When
        val result = trackSubscriptionUseCase.calculateMonthlyCost()

        // Then
        assertEquals(Money(BigDecimal("166.00"), "SAR"), result)
    }

    @Test
    fun `get subscriptions by category`() = runTest {
        // Given
        val subscriptions = listOf(
            createActiveSubscription("Netflix", Money(BigDecimal("56.00"), "SAR"), SubscriptionCategory.STREAMING),
            createActiveSubscription("Spotify", Money(BigDecimal("20.00"), "SAR"), SubscriptionCategory.MUSIC),
            createActiveSubscription("Disney+", Money(BigDecimal("30.00"), "SAR"), SubscriptionCategory.STREAMING)
        )
        coEvery { subscriptionRepository.getByCategory(SubscriptionCategory.STREAMING) } returns
            subscriptions.filter { it.category == SubscriptionCategory.STREAMING }

        // When
        val result = trackSubscriptionUseCase.getByCategory(SubscriptionCategory.STREAMING)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == SubscriptionCategory.STREAMING })
    }

    @Test
    fun `detect upcoming renewals within next 7 days`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscriptions = listOf(
            createSubscriptionWithRenewalDate("Netflix", now.plus(3, DateTimeUnit.DAY)),
            createSubscriptionWithRenewalDate("Spotify", now.plus(10, DateTimeUnit.DAY)),
            createSubscriptionWithRenewalDate("Adobe", now.plus(5, DateTimeUnit.DAY))
        )
        coEvery { subscriptionRepository.getAllActive() } returns subscriptions

        // When
        val result = trackSubscriptionUseCase.getUpcomingRenewals(7)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.serviceName == "Netflix" })
        assertTrue(result.any { it.serviceName == "Adobe" })
    }

    @Test
    fun `identify potential savings from unused subscriptions`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscriptions = listOf(
            createSubscriptionWithLastUsed(
                "Netflix",
                now.minus(5, DateTimeUnit.DAY),
                Money(BigDecimal("56.00"), "SAR")
            ),
            createSubscriptionWithLastUsed(
                "Unused App",
                now.minus(60, DateTimeUnit.DAY),
                Money(BigDecimal("30.00"), "SAR")
            ),
            createSubscriptionWithLastUsed("Spotify", now.minus(2, DateTimeUnit.DAY), Money(BigDecimal("20.00"), "SAR"))
        )
        coEvery { subscriptionRepository.getAllActive() } returns subscriptions

        // When
        val result = trackSubscriptionUseCase.identifyUnusedSubscriptions(30) // 30 days threshold

        // Then
        assertEquals(1, result.size)
        assertEquals("Unused App", result.first().serviceName)
    }

    @Test
    fun `calculate potential annual savings`() = runTest {
        // Given
        val unusedSubscriptions = listOf(
            createActiveSubscription("Unused Service 1", Money(BigDecimal("25.00"), "SAR")),
            createActiveSubscription("Unused Service 2", Money(BigDecimal("15.00"), "SAR"))
        )

        // When
        val result = trackSubscriptionUseCase.calculatePotentialSavings(unusedSubscriptions)

        // Then
        assertEquals(Money(BigDecimal("480.00"), "SAR"), result) // (25 + 15) * 12
    }

    private fun createDetectedNetflixSubscription() = DetectedSubscription(
        serviceName = "Netflix",
        merchantName = "NETFLIX.COM",
        category = SubscriptionCategory.STREAMING,
        frequency = SubscriptionFrequency.MONTHLY,
        monthlyAmount = Money(BigDecimal("56.00"), "SAR"),
        actualAmount = Money(BigDecimal("56.00"), "SAR"),
        isActive = true,
        confidence = 0.95,
        hasVariableAmount = false,
        transactionHistory = emptyList(),
        firstPaymentDate = Clock.System.now().minus(90, DateTimeUnit.DAY),
        lastPaymentDate = Clock.System.now().minus(30, DateTimeUnit.DAY),
        nextExpectedDate = Clock.System.now().plus(1, DateTimeUnit.DAY),
        totalPaid = Money(BigDecimal("168.00"), "SAR"),
        averageInterval = 30.0
    )

    private fun createActiveSubscription(
        serviceName: String = "Netflix",
        monthlyAmount: Money = Money(BigDecimal("56.00"), "SAR"),
        category: SubscriptionCategory = SubscriptionCategory.STREAMING
    ) = Subscription(
        id = "sub_1",
        serviceName = serviceName,
        merchantName = "$serviceName.com",
        category = category,
        frequency = SubscriptionFrequency.MONTHLY,
        monthlyAmount = monthlyAmount,
        status = SubscriptionStatus.ACTIVE,
        startDate = Clock.System.now().minus(90, DateTimeUnit.DAY),
        nextRenewalDate = Clock.System.now().plus(30, DateTimeUnit.DAY),
        renewalCount = 3,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private fun createSubscriptionWithRenewalDate(
        serviceName: String,
        renewalDate: kotlinx.datetime.Instant
    ) = createActiveSubscription(serviceName).copy(nextRenewalDate = renewalDate)

    private fun createSubscriptionWithLastUsed(
        serviceName: String,
        lastUsedDate: kotlinx.datetime.Instant,
        monthlyAmount: Money
    ) = createActiveSubscription(serviceName, monthlyAmount).copy(lastUsedDate = lastUsedDate)
}
