package code.yousef.dari.shared.domain.model

import code.yousef.dari.shared.domain.usecase.subscription.SubscriptionCategory
import code.yousef.dari.shared.domain.usecase.subscription.SubscriptionFrequency
import kotlinx.datetime.Instant

enum class SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
    EXPIRED,
    TRIAL
}

data class Subscription(
    val id: String,
    val serviceName: String,
    val merchantName: String,
    val category: SubscriptionCategory,
    val frequency: SubscriptionFrequency,
    val monthlyAmount: Money,
    val actualAmount: Money = monthlyAmount,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val startDate: Instant,
    val endDate: Instant? = null,
    val nextRenewalDate: Instant,
    val lastRenewalDate: Instant? = null,
    val lastUsedDate: Instant? = null,
    val trialEndDate: Instant? = null,
    val renewalCount: Int = 0,
    val totalPaid: Money = Money(0.toBigDecimal(), monthlyAmount.currency),
    val cancellationReason: String? = null,
    val isAutoRenewal: Boolean = true,
    val reminderDaysBefore: Int = 3,
    val isReminderEnabled: Boolean = true,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val website: String? = null,
    val supportContact: String? = null,
    val loginEmail: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SubscriptionUsage(
    val subscriptionId: String,
    val date: Instant,
    val usageType: String, // "login", "streaming", "download", etc.
    val duration: Long? = null, // Duration in minutes if applicable
    val metadata: Map<String, String> = emptyMap()
)

data class SubscriptionRenewalHistory(
    val id: String,
    val subscriptionId: String,
    val renewalDate: Instant,
    val amount: Money,
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val isSuccessful: Boolean,
    val failureReason: String? = null
)

data class SubscriptionAlert(
    val id: String,
    val subscriptionId: String,
    val alertType: SubscriptionAlertType,
    val title: String,
    val message: String,
    val createdAt: Instant,
    val isRead: Boolean = false,
    val actionUrl: String? = null
)

enum class SubscriptionAlertType {
    RENEWAL_REMINDER,
    PRICE_INCREASE,
    UNUSED_WARNING,
    TRIAL_EXPIRING,
    PAYMENT_FAILED,
    CANCELLATION_CONFIRMED,
    SAVINGS_OPPORTUNITY
}