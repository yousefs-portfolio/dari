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
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionReminderUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private lateinit var subscriptionReminderUseCase: SubscriptionReminderUseCase

    @BeforeTest
    fun setup() {
        subscriptionReminderUseCase = SubscriptionReminderUseCase(subscriptionRepository)
    }

    @Test
    fun `schedule reminder for upcoming renewal`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscription = createSubscription(
            nextRenewalDate = now.plus(2, DateTimeUnit.DAY),
            reminderDaysBefore = 3,
            isReminderEnabled = true
        )

        coEvery { subscriptionRepository.getAllActive() } returns listOf(subscription)
        coEvery { subscriptionRepository.createAlert(any()) } returns Unit

        // When
        val result = subscriptionReminderUseCase.scheduleReminders()

        // Then
        assertEquals(1, result.size)
        val reminder = result.first()
        assertEquals("Netflix Renewal Reminder", reminder.title)
        assertTrue(reminder.message.contains("will renew in 2 day(s)"))

        coVerify { subscriptionRepository.createAlert(any()) }
    }

    @Test
    fun `do not schedule reminder when disabled`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscription = createSubscription(
            nextRenewalDate = now.plus(1, DateTimeUnit.DAY),
            reminderDaysBefore = 3,
            isReminderEnabled = false
        )

        coEvery { subscriptionRepository.getAllActive() } returns listOf(subscription)

        // When
        val result = subscriptionReminderUseCase.scheduleReminders()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `do not schedule reminder when renewal is too far`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscription = createSubscription(
            nextRenewalDate = now.plus(10, DateTimeUnit.DAY),
            reminderDaysBefore = 3,
            isReminderEnabled = true
        )

        coEvery { subscriptionRepository.getAllActive() } returns listOf(subscription)

        // When
        val result = subscriptionReminderUseCase.scheduleReminders()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `schedule urgent reminder for today's renewal`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscription = createSubscription(
            nextRenewalDate = now,
            reminderDaysBefore = 1,
            isReminderEnabled = true
        )

        coEvery { subscriptionRepository.getAllActive() } returns listOf(subscription)
        coEvery { subscriptionRepository.createAlert(any()) } returns Unit

        // When
        val result = subscriptionReminderUseCase.scheduleReminders()

        // Then
        assertEquals(1, result.size)
        val reminder = result.first()
        assertTrue(reminder.message.contains("will renew in 0 day(s)"))
        assertTrue(reminder.title.contains("Netflix"))
    }

    @Test
    fun `schedule multiple reminders for different subscriptions`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscriptions = listOf(
            createSubscription(
                id = "sub_1",
                serviceName = "Netflix",
                nextRenewalDate = now.plus(1, DateTimeUnit.DAY),
                reminderDaysBefore = 2,
                isReminderEnabled = true
            ),
            createSubscription(
                id = "sub_2",
                serviceName = "Spotify",
                nextRenewalDate = now.plus(2, DateTimeUnit.DAY),
                reminderDaysBefore = 3,
                isReminderEnabled = true
            ),
            createSubscription(
                id = "sub_3",
                serviceName = "Adobe",
                nextRenewalDate = now.plus(10, DateTimeUnit.DAY), // Too far
                reminderDaysBefore = 3,
                isReminderEnabled = true
            )
        )

        coEvery { subscriptionRepository.getAllActive() } returns subscriptions
        coEvery { subscriptionRepository.createAlert(any()) } returns Unit

        // When
        val result = subscriptionReminderUseCase.scheduleReminders()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.title.contains("Netflix") })
        assertTrue(result.any { it.title.contains("Spotify") })
    }

    @Test
    fun `create price increase notification`() = runTest {
        // Given
        val subscription = createSubscription()
        val newAmount = Money(BigDecimal("65.00"), "SAR")
        val oldAmount = subscription.actualAmount

        coEvery { subscriptionRepository.getById("sub_1") } returns subscription
        coEvery { subscriptionRepository.createAlert(any()) } returns Unit

        // When
        val result = subscriptionReminderUseCase.notifyPriceIncrease("sub_1", newAmount, oldAmount)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            subscriptionRepository.createAlert(
                match {
                    it.title.contains("Price Increase") &&
                        it.message.contains("increased from") &&
                        it.message.contains("56.00") &&
                        it.message.contains("65.00")
                }
            )
        }
    }

    @Test
    fun `create trial expiring notification`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscription = createSubscription(
            status = SubscriptionStatus.TRIAL,
            nextRenewalDate = now.plus(2, DateTimeUnit.DAY)
        )

        coEvery { subscriptionRepository.getById("sub_1") } returns subscription
        coEvery { subscriptionRepository.createAlert(any()) } returns Unit

        // When
        val result = subscriptionReminderUseCase.notifyTrialExpiring("sub_1")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            subscriptionRepository.createAlert(
                match {
                    it.title.contains("Trial Expiring") &&
                        it.message.contains("trial will expire")
                }
            )
        }
    }

    @Test
    fun `create payment failed notification`() = runTest {
        // Given
        val subscription = createSubscription()
        val failureReason = "Insufficient funds"

        coEvery { subscriptionRepository.getById("sub_1") } returns subscription
        coEvery { subscriptionRepository.createAlert(any()) } returns Unit

        // When
        val result = subscriptionReminderUseCase.notifyPaymentFailed("sub_1", failureReason)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            subscriptionRepository.createAlert(
                match {
                    it.title.contains("Payment Failed") &&
                        it.message.contains("Insufficient funds")
                }
            )
        }
    }

    @Test
    fun `get upcoming renewals within specified days`() = runTest {
        // Given
        val now = Clock.System.now()
        val subscriptions = listOf(
            createSubscription(nextRenewalDate = now.plus(1, DateTimeUnit.DAY)),
            createSubscription(nextRenewalDate = now.plus(5, DateTimeUnit.DAY)),
            createSubscription(nextRenewalDate = now.plus(10, DateTimeUnit.DAY))
        )

        coEvery { subscriptionRepository.getAllActive() } returns subscriptions

        // When
        val result = subscriptionReminderUseCase.getUpcomingRenewals(7)

        // Then
        assertEquals(2, result.size) // Only renewals within 7 days
    }

    private fun createSubscription(
        id: String = "sub_1",
        serviceName: String = "Netflix",
        nextRenewalDate: kotlinx.datetime.Instant = Clock.System.now().plus(30, DateTimeUnit.DAY),
        reminderDaysBefore: Int = 3,
        isReminderEnabled: Boolean = true,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ) = Subscription(
        id = id,
        serviceName = serviceName,
        merchantName = "$serviceName.com",
        category = SubscriptionCategory.STREAMING,
        frequency = SubscriptionFrequency.MONTHLY,
        monthlyAmount = Money(BigDecimal("56.00"), "SAR"),
        actualAmount = Money(BigDecimal("56.00"), "SAR"),
        status = status,
        startDate = Clock.System.now(),
        nextRenewalDate = nextRenewalDate,
        reminderDaysBefore = reminderDaysBefore,
        isReminderEnabled = isReminderEnabled,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
