package code.yousef.dari.shared.domain.repository

import code.yousef.dari.shared.domain.model.Subscription
import code.yousef.dari.shared.domain.model.SubscriptionAlert
import code.yousef.dari.shared.domain.model.SubscriptionRenewalHistory
import code.yousef.dari.shared.domain.model.SubscriptionStatus
import code.yousef.dari.shared.domain.model.SubscriptionUsage
import code.yousef.dari.shared.domain.usecase.subscription.SubscriptionCategory
import code.yousef.dari.shared.domain.usecase.subscription.SubscriptionFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface SubscriptionRepository {

    // Basic CRUD operations
    suspend fun create(subscription: Subscription)
    suspend fun update(subscription: Subscription)
    suspend fun delete(id: String)
    suspend fun getById(id: String): Subscription?
    suspend fun getAll(): List<Subscription>
    suspend fun getAllActive(): List<Subscription>
    suspend fun getByStatus(status: SubscriptionStatus): List<Subscription>
    suspend fun getByCategory(category: SubscriptionCategory): List<Subscription>
    suspend fun getByFrequency(frequency: SubscriptionFrequency): List<Subscription>

    // Observable queries
    fun observeAll(): Flow<List<Subscription>>
    fun observeActive(): Flow<List<Subscription>>
    fun observeByCategory(category: SubscriptionCategory): Flow<List<Subscription>>
    fun observeUpcomingRenewals(days: Int): Flow<List<Subscription>>

    // Renewal and payment tracking
    suspend fun recordRenewal(renewalHistory: SubscriptionRenewalHistory)
    suspend fun getRenewalHistory(subscriptionId: String): List<SubscriptionRenewalHistory>
    suspend fun updateRenewalDate(subscriptionId: String, nextRenewalDate: Instant)
    suspend fun incrementRenewalCount(subscriptionId: String)

    // Usage tracking
    suspend fun recordUsage(usage: SubscriptionUsage)
    suspend fun getUsageHistory(subscriptionId: String, fromDate: Instant? = null): List<SubscriptionUsage>
    suspend fun getLastUsageDate(subscriptionId: String): Instant?
    suspend fun getUsageStats(subscriptionId: String, days: Int): Map<String, Int>

    // Alerts and notifications
    suspend fun createAlert(alert: SubscriptionAlert)
    suspend fun getAlerts(subscriptionId: String): List<SubscriptionAlert>
    suspend fun getAllUnreadAlerts(): List<SubscriptionAlert>
    suspend fun markAlertAsRead(alertId: String)
    suspend fun deleteAlert(alertId: String)

    // Analytics and insights
    suspend fun getTotalMonthlyCost(): Double
    suspend fun getCostByCategory(): Map<SubscriptionCategory, Double>
    suspend fun getCostByFrequency(): Map<SubscriptionFrequency, Double>
    suspend fun getUnusedSubscriptions(daysThreshold: Int): List<Subscription>
    suspend fun getSubscriptionsForRenewal(fromDate: Instant, toDate: Instant): List<Subscription>
    suspend fun getAverageMonthlySpend(): Double
    suspend fun getYearlySpendingTrend(): Map<String, Double>

    // Search and filtering
    suspend fun searchByName(query: String): List<Subscription>
    suspend fun getByMerchant(merchantName: String): List<Subscription>
    suspend fun getExpiring(days: Int): List<Subscription>
    suspend fun getTrialSubscriptions(): List<Subscription>

    // Bulk operations
    suspend fun updateStatus(subscriptionIds: List<String>, status: SubscriptionStatus)
    suspend fun updateCategory(subscriptionIds: List<String>, category: SubscriptionCategory)
    suspend fun bulkDelete(subscriptionIds: List<String>)

    // Backup and restore
    suspend fun exportSubscriptions(): List<Subscription>
    suspend fun importSubscriptions(subscriptions: List<Subscription>)
    suspend fun clearAll()
}
