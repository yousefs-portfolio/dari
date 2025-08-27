package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic notification setup interface
 */
@Composable
expect fun SetupNotificationChannels()

/**
 * Check if the app has notification permission
 */
@Composable
expect fun hasNotificationPermission(): Boolean

/**
 * Common notification configuration for financial apps
 */
@Composable
fun ConfigureFinancialNotifications() {
    SetupNotificationChannels()
}

/**
 * Financial app notification categories
 */
enum class FinancialNotificationCategory {
    TRANSACTION_ALERT,
    BUDGET_WARNING,
    GOAL_MILESTONE,
    BILL_REMINDER,
    SECURITY_ALERT,
    ACCOUNT_UPDATE,
    INVESTMENT_UPDATE,
    MARKET_ALERT
}

/**
 * Financial notification priority levels
 */
enum class FinancialNotificationPriority {
    LOW,        // General updates, tips
    DEFAULT,    // Transaction notifications, goal progress
    HIGH,       // Budget alerts, bill reminders
    CRITICAL    // Security alerts, account breaches
}

/**
 * Common financial notification data structure
 */
data class FinancialNotification(
    val id: String,
    val category: FinancialNotificationCategory,
    val priority: FinancialNotificationPriority,
    val title: String,
    val message: String,
    val amount: String? = null,
    val currency: String? = null,
    val accountName: String? = null,
    val categoryName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actionRequired: Boolean = false,
    val securitySensitive: Boolean = false
)