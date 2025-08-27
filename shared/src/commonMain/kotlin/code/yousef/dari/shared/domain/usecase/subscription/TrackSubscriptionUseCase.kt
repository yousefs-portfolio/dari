package code.yousef.dari.shared.domain.usecase.subscription

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Subscription
import code.yousef.dari.shared.domain.model.SubscriptionAlert
import code.yousef.dari.shared.domain.model.SubscriptionAlertType
import code.yousef.dari.shared.domain.model.SubscriptionRenewalHistory
import code.yousef.dari.shared.domain.model.SubscriptionStatus
import code.yousef.dari.shared.domain.model.SubscriptionUsage
import code.yousef.dari.shared.domain.repository.SubscriptionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class TrackSubscriptionUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {

    suspend fun createFromDetected(detectedSubscription: DetectedSubscription): Result<Subscription> {
        return try {
            val subscription = Subscription(
                id = generateSubscriptionId(),
                serviceName = detectedSubscription.serviceName,
                merchantName = detectedSubscription.merchantName,
                category = detectedSubscription.category,
                frequency = detectedSubscription.frequency,
                monthlyAmount = detectedSubscription.monthlyAmount,
                actualAmount = detectedSubscription.actualAmount,
                status = if (detectedSubscription.isActive) SubscriptionStatus.ACTIVE else SubscriptionStatus.EXPIRED,
                startDate = detectedSubscription.firstPaymentDate,
                nextRenewalDate = detectedSubscription.nextExpectedDate ?: predictNextRenewal(detectedSubscription),
                lastRenewalDate = detectedSubscription.lastPaymentDate,
                renewalCount = detectedSubscription.transactionHistory.size,
                totalPaid = detectedSubscription.totalPaid,
                tags = detectedSubscription.tags,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            subscriptionRepository.create(subscription)
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStatus(
        subscriptionId: String,
        status: SubscriptionStatus,
        reason: String? = null
    ): Result<Subscription> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            val updatedSubscription = subscription.copy(
                status = status,
                cancellationReason = if (status == SubscriptionStatus.CANCELLED) reason else subscription.cancellationReason,
                endDate = if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
                    Clock.System.now()
                } else subscription.endDate,
                updatedAt = Clock.System.now()
            )

            subscriptionRepository.update(updatedSubscription)

            // Create alert for status change
            createStatusChangeAlert(updatedSubscription, status, reason)

            Result.success(updatedSubscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordRenewal(
        subscriptionId: String,
        amount: Money,
        transactionId: String? = null
    ): Result<Subscription> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            val now = Clock.System.now()
            val nextRenewal = calculateNextRenewalDate(now, subscription.frequency)

            // Record renewal history
            val renewalHistory = SubscriptionRenewalHistory(
                id = generateRenewalHistoryId(),
                subscriptionId = subscriptionId,
                renewalDate = now,
                amount = amount,
                transactionId = transactionId,
                isSuccessful = true
            )

            subscriptionRepository.recordRenewal(renewalHistory)

            // Update subscription
            val updatedSubscription = subscription.copy(
                monthlyAmount = normalizeToMonthlyAmount(amount, subscription.frequency),
                actualAmount = amount,
                nextRenewalDate = nextRenewal,
                lastRenewalDate = now,
                renewalCount = subscription.renewalCount + 1,
                totalPaid = Money(
                    subscription.totalPaid.amount + amount.amount,
                    subscription.totalPaid.currency
                ),
                updatedAt = now
            )

            subscriptionRepository.update(updatedSubscription)
            Result.success(updatedSubscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordUsage(subscriptionId: String, usageType: String, duration: Long? = null): Result<Unit> {
        return try {
            val usage = SubscriptionUsage(
                subscriptionId = subscriptionId,
                date = Clock.System.now(),
                usageType = usageType,
                duration = duration
            )

            subscriptionRepository.recordUsage(usage)

            // Update last used date in subscription
            val subscription = subscriptionRepository.getById(subscriptionId)
            subscription?.let {
                val updatedSubscription = it.copy(
                    lastUsedDate = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                subscriptionRepository.update(updatedSubscription)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateMonthlyCost(): Money {
        val activeSubscriptions = subscriptionRepository.getAllActive()
        val totalAmount = activeSubscriptions.sumOf { it.monthlyAmount.amount }
        val currency = activeSubscriptions.firstOrNull()?.monthlyAmount?.currency ?: "SAR"

        return Money(totalAmount, currency)
    }

    suspend fun getByCategory(category: SubscriptionCategory): List<Subscription> {
        return subscriptionRepository.getByCategory(category)
    }

    suspend fun getUpcomingRenewals(days: Int): List<Subscription> {
        val endDate = Clock.System.now().plus(days, DateTimeUnit.DAY)
        return subscriptionRepository.getSubscriptionsForRenewal(Clock.System.now(), endDate)
    }

    suspend fun identifyUnusedSubscriptions(daysThreshold: Int): List<Subscription> {
        return subscriptionRepository.getUnusedSubscriptions(daysThreshold)
    }

    suspend fun calculatePotentialSavings(unusedSubscriptions: List<Subscription>): Money {
        val annualSavings = unusedSubscriptions.sumOf { subscription ->
            subscription.monthlyAmount.amount * 12.toBigDecimal()
        }
        val currency = unusedSubscriptions.firstOrNull()?.monthlyAmount?.currency ?: "SAR"

        return Money(annualSavings, currency)
    }

    suspend fun generateRenewalReminders(): List<SubscriptionAlert> {
        val alerts = mutableListOf<SubscriptionAlert>()
        val upcomingRenewals = getUpcomingRenewals(7) // Next 7 days

        for (subscription in upcomingRenewals) {
            if (subscription.isReminderEnabled) {
                val daysUntilRenewal =
                    (subscription.nextRenewalDate.epochSeconds - Clock.System.now().epochSeconds) / (24 * 60 * 60)

                if (daysUntilRenewal <= subscription.reminderDaysBefore) {
                    val alert = SubscriptionAlert(
                        id = generateAlertId(),
                        subscriptionId = subscription.id,
                        alertType = SubscriptionAlertType.RENEWAL_REMINDER,
                        title = "${subscription.serviceName} Renewal Reminder",
                        message = "Your ${subscription.serviceName} subscription will renew in ${daysUntilRenewal} day(s) for ${subscription.actualAmount}",
                        createdAt = Clock.System.now()
                    )

                    alerts.add(alert)
                    subscriptionRepository.createAlert(alert)
                }
            }
        }

        return alerts
    }

    suspend fun generateUsageAlerts(): List<SubscriptionAlert> {
        val alerts = mutableListOf<SubscriptionAlert>()
        val unusedSubscriptions = identifyUnusedSubscriptions(30) // 30 days

        for (subscription in unusedSubscriptions) {
            val alert = SubscriptionAlert(
                id = generateAlertId(),
                subscriptionId = subscription.id,
                alertType = SubscriptionAlertType.UNUSED_WARNING,
                title = "Unused Subscription Detected",
                message = "You haven't used ${subscription.serviceName} in the last 30 days. Consider cancelling to save ${subscription.monthlyAmount} per month.",
                createdAt = Clock.System.now()
            )

            alerts.add(alert)
            subscriptionRepository.createAlert(alert)
        }

        return alerts
    }

    suspend fun generateSavingsOpportunities(): List<SubscriptionAlert> {
        val alerts = mutableListOf<SubscriptionAlert>()
        val potentialSavings = calculatePotentialSavings(identifyUnusedSubscriptions(30))

        if (potentialSavings.amount > 0.toBigDecimal()) {
            val alert = SubscriptionAlert(
                id = generateAlertId(),
                subscriptionId = "", // General alert
                alertType = SubscriptionAlertType.SAVINGS_OPPORTUNITY,
                title = "Potential Savings Identified",
                message = "You could save ${potentialSavings} annually by cancelling unused subscriptions.",
                createdAt = Clock.System.now()
            )

            alerts.add(alert)
            subscriptionRepository.createAlert(alert)
        }

        return alerts
    }

    private fun predictNextRenewal(detectedSubscription: DetectedSubscription): Instant {
        val lastPayment = detectedSubscription.lastPaymentDate ?: Clock.System.now()
        return calculateNextRenewalDate(lastPayment, detectedSubscription.frequency)
    }

    private fun calculateNextRenewalDate(fromDate: Instant, frequency: SubscriptionFrequency): Instant {
        return fromDate.plus(frequency.days, DateTimeUnit.DAY)
    }

    private fun normalizeToMonthlyAmount(amount: Money, frequency: SubscriptionFrequency): Money {
        val monthlyAmount = when (frequency) {
            SubscriptionFrequency.WEEKLY -> amount.amount * 4.toBigDecimal()
            SubscriptionFrequency.MONTHLY -> amount.amount
            SubscriptionFrequency.QUARTERLY -> amount.amount / 3.toBigDecimal()
            SubscriptionFrequency.YEARLY -> amount.amount / 12.toBigDecimal()
        }

        return Money(monthlyAmount, amount.currency)
    }

    private suspend fun createStatusChangeAlert(
        subscription: Subscription,
        newStatus: SubscriptionStatus,
        reason: String?
    ) {
        val alertType = when (newStatus) {
            SubscriptionStatus.CANCELLED -> SubscriptionAlertType.CANCELLATION_CONFIRMED
            SubscriptionStatus.EXPIRED -> SubscriptionAlertType.TRIAL_EXPIRING
            else -> return
        }

        val alert = SubscriptionAlert(
            id = generateAlertId(),
            subscriptionId = subscription.id,
            alertType = alertType,
            title = "${subscription.serviceName} ${newStatus.name.lowercase().replaceFirstChar { it.uppercase() }}",
            message = "${subscription.serviceName} has been ${newStatus.name.lowercase()}${reason?.let { " - $it" } ?: ""}",
            createdAt = Clock.System.now()
        )

        subscriptionRepository.createAlert(alert)
    }

    private fun generateSubscriptionId(): String = "sub_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
    private fun generateRenewalHistoryId(): String = "ren_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
    private fun generateAlertId(): String = "alert_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
}
