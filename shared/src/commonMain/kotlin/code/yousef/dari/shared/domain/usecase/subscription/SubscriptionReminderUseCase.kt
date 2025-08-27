package code.yousef.dari.shared.domain.usecase.subscription

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Subscription
import code.yousef.dari.shared.domain.model.SubscriptionAlert
import code.yousef.dari.shared.domain.model.SubscriptionAlertType
import code.yousef.dari.shared.domain.model.SubscriptionStatus
import code.yousef.dari.shared.domain.repository.SubscriptionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant

class SubscriptionReminderUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {

    suspend fun scheduleReminders(): List<SubscriptionAlert> {
        val now = Clock.System.now()
        val activeSubscriptions = subscriptionRepository.getAllActive()
        val reminders = mutableListOf<SubscriptionAlert>()

        for (subscription in activeSubscriptions) {
            if (!subscription.isReminderEnabled) continue

            val daysUntilRenewal = calculateDaysUntilRenewal(now, subscription.nextRenewalDate)

            if (daysUntilRenewal <= subscription.reminderDaysBefore && daysUntilRenewal >= 0) {
                val reminder = createRenewalReminder(subscription, daysUntilRenewal)
                subscriptionRepository.createAlert(reminder)
                reminders.add(reminder)
            }
        }

        return reminders
    }

    suspend fun notifyPriceIncrease(
        subscriptionId: String,
        newAmount: Money,
        oldAmount: Money
    ): Result<SubscriptionAlert> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            val increaseAmount = Money(newAmount.amount - oldAmount.amount, newAmount.currency)
            val increasePercentage =
                ((newAmount.amount - oldAmount.amount) / oldAmount.amount * 100.toBigDecimal()).toInt()

            val alert = SubscriptionAlert(
                id = generateAlertId(),
                subscriptionId = subscriptionId,
                alertType = SubscriptionAlertType.PRICE_INCREASE,
                title = "${subscription.serviceName} Price Increase",
                message = "The price for ${subscription.serviceName} has increased from ${oldAmount} to ${newAmount} (${increaseAmount} more, ${increasePercentage}% increase). Your next bill will reflect this change.",
                createdAt = Clock.System.now(),
                actionUrl = subscription.website
            )

            subscriptionRepository.createAlert(alert)
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun notifyTrialExpiring(subscriptionId: String): Result<SubscriptionAlert> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            val daysUntilExpiry = calculateDaysUntilRenewal(Clock.System.now(), subscription.nextRenewalDate)

            val alert = SubscriptionAlert(
                id = generateAlertId(),
                subscriptionId = subscriptionId,
                alertType = SubscriptionAlertType.TRIAL_EXPIRING,
                title = "${subscription.serviceName} Trial Expiring",
                message = "Your ${subscription.serviceName} trial will expire in ${daysUntilExpiry} day(s). " +
                    "You'll be charged ${subscription.actualAmount} unless you cancel before then.",
                createdAt = Clock.System.now(),
                actionUrl = subscription.website
            )

            subscriptionRepository.createAlert(alert)
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun notifyPaymentFailed(subscriptionId: String, failureReason: String): Result<SubscriptionAlert> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            val alert = SubscriptionAlert(
                id = generateAlertId(),
                subscriptionId = subscriptionId,
                alertType = SubscriptionAlertType.PAYMENT_FAILED,
                title = "${subscription.serviceName} Payment Failed",
                message = "Payment for ${subscription.serviceName} failed: $failureReason. " +
                    "Please update your payment method to avoid service interruption.",
                createdAt = Clock.System.now(),
                actionUrl = subscription.website
            )

            subscriptionRepository.createAlert(alert)
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun notifyUnusedSubscription(subscriptionId: String, daysUnused: Int): Result<SubscriptionAlert> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            val monthlySavings = subscription.monthlyAmount
            val yearlySavings = Money(monthlySavings.amount * 12.toBigDecimal(), monthlySavings.currency)

            val alert = SubscriptionAlert(
                id = generateAlertId(),
                subscriptionId = subscriptionId,
                alertType = SubscriptionAlertType.UNUSED_WARNING,
                title = "Unused Subscription: ${subscription.serviceName}",
                message = "You haven't used ${subscription.serviceName} in $daysUnused days. " +
                    "Consider cancelling to save ${monthlySavings}/month (${yearlySavings}/year).",
                createdAt = Clock.System.now(),
                actionUrl = subscription.website
            )

            subscriptionRepository.createAlert(alert)
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpcomingRenewals(days: Int): List<Subscription> {
        val now = Clock.System.now()
        val endDate = now.plus(days, DateTimeUnit.DAY)

        return subscriptionRepository.getAllActive().filter { subscription ->
            subscription.nextRenewalDate <= endDate && subscription.nextRenewalDate >= now
        }.sortedBy { it.nextRenewalDate }
    }

    suspend fun getOverdueRenewals(): List<Subscription> {
        val now = Clock.System.now()

        return subscriptionRepository.getAllActive().filter { subscription ->
            subscription.nextRenewalDate < now
        }.sortedBy { it.nextRenewalDate }
    }

    suspend fun updateReminderSettings(
        subscriptionId: String,
        isEnabled: Boolean,
        daysBefore: Int
    ): Result<Subscription> {
        return try {
            val subscription = subscriptionRepository.getById(subscriptionId)
                ?: throw IllegalArgumentException("Subscription not found")

            if (daysBefore < 0) {
                throw IllegalArgumentException("Days before must be non-negative")
            }

            val updatedSubscription = subscription.copy(
                isReminderEnabled = isEnabled,
                reminderDaysBefore = daysBefore,
                updatedAt = Clock.System.now()
            )

            subscriptionRepository.update(updatedSubscription)
            Result.success(updatedSubscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun snoozeReminder(alertId: String, snoozeHours: Int): Result<Unit> {
        return try {
            // For now, just mark as read. In a full implementation,
            // we'd reschedule the reminder
            subscriptionRepository.markAlertAsRead(alertId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dismissAlert(alertId: String): Result<Unit> {
        return try {
            subscriptionRepository.deleteAlert(alertId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateDailySummary(): DailySubscriptionSummary {
        val now = Clock.System.now()
        val allSubscriptions = subscriptionRepository.getAllActive()

        val renewingToday = allSubscriptions.filter {
            calculateDaysUntilRenewal(now, it.nextRenewalDate) == 0L
        }

        val renewingThisWeek = allSubscriptions.filter {
            val days = calculateDaysUntilRenewal(now, it.nextRenewalDate)
            days in 1..7
        }

        val unusedSubscriptions = subscriptionRepository.getUnusedSubscriptions(30)
        val totalMonthlyCost = subscriptionRepository.getTotalMonthlyCost()

        return DailySubscriptionSummary(
            date = now,
            totalActiveSubscriptions = allSubscriptions.size,
            renewingToday = renewingToday.size,
            renewingThisWeek = renewingThisWeek.size,
            unusedSubscriptions = unusedSubscriptions.size,
            totalMonthlyCost = totalMonthlyCost,
            recommendations = generateRecommendations(allSubscriptions, unusedSubscriptions)
        )
    }

    private fun createRenewalReminder(subscription: Subscription, daysUntilRenewal: Long): SubscriptionAlert {
        val urgencyMessage = when (daysUntilRenewal) {
            0L -> "today"
            1L -> "tomorrow"
            else -> "in $daysUntilRenewal day(s)"
        }

        return SubscriptionAlert(
            id = generateAlertId(),
            subscriptionId = subscription.id,
            alertType = SubscriptionAlertType.RENEWAL_REMINDER,
            title = "${subscription.serviceName} Renewal Reminder",
            message = "Your ${subscription.serviceName} subscription will renew $urgencyMessage for ${subscription.actualAmount}",
            createdAt = Clock.System.now(),
            actionUrl = subscription.website
        )
    }

    private fun calculateDaysUntilRenewal(now: Instant, renewalDate: Instant): Long {
        return (renewalDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
    }

    private fun generateRecommendations(
        allSubscriptions: List<Subscription>,
        unusedSubscriptions: List<Subscription>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (unusedSubscriptions.isNotEmpty()) {
            val savings = unusedSubscriptions.sumOf { it.monthlyAmount.amount }
            recommendations.add(
                "Cancel ${unusedSubscriptions.size} unused subscription(s) to save ${
                    Money(
                        savings,
                        "SAR"
                    )
                }/month"
            )
        }

        val streamingServices = allSubscriptions.filter { it.category == SubscriptionCategory.STREAMING }
        if (streamingServices.size > 3) {
            recommendations.add("You have ${streamingServices.size} streaming services. Consider keeping only your most-used ones.")
        }

        val totalCost = allSubscriptions.sumOf { it.monthlyAmount.amount }
        if (totalCost > 500.toBigDecimal()) {
            recommendations.add(
                "Your monthly subscription cost is high at ${
                    Money(
                        totalCost,
                        "SAR"
                    )
                }. Review for potential savings."
            )
        }

        return recommendations
    }

    private fun generateAlertId(): String {
        return "alert_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
    }
}

data class DailySubscriptionSummary(
    val date: Instant,
    val totalActiveSubscriptions: Int,
    val renewingToday: Int,
    val renewingThisWeek: Int,
    val unusedSubscriptions: Int,
    val totalMonthlyCost: Double,
    val recommendations: List<String>
)
