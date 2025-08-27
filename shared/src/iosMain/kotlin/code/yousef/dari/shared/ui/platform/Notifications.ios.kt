package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UserNotifications.*
import platform.Foundation.*

/**
 * iOS-specific notification setup and permission handling
 */
@Composable
actual fun SetupNotificationChannels() {
    LaunchedEffect(Unit) {
        // iOS doesn't use channels like Android, but we can request permission here
        requestNotificationPermission()
    }
}

/**
 * Check notification permission on iOS
 */
@Composable
actual fun hasNotificationPermission(): Boolean {
    // This would need to be implemented with platform-specific code
    // In a real implementation, this would check UNUserNotificationCenter authorization status
    return true // Placeholder
}

/**
 * Request notification permission for iOS
 */
suspend fun requestNotificationPermission() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    
    center.requestAuthorizationWithOptions(
        options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
    ) { granted, error ->
        if (granted) {
            // Permission granted, can send notifications
        } else {
            // Permission denied or error occurred
            error?.let {
                // Handle error
            }
        }
    }
}

/**
 * iOS-specific notification manager
 */
class IOSNotificationManager {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    
    fun scheduleTransactionNotification(
        title: String,
        body: String,
        amount: String,
        isIncome: Boolean = false
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSubtitle(amount)
            setBadge(NSNumber.numberWithInt(1))
            
            // Set category for actionable notifications
            setCategoryIdentifier("TRANSACTION_CATEGORY")
        }
        
        // Create trigger for immediate delivery
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "transaction_${System.currentTimeMillis()}",
            content = content,
            trigger = trigger
        )
        
        center.addNotificationRequest(request) { error ->
            error?.let {
                // Handle error
            }
        }
    }
    
    fun scheduleBudgetAlert(
        categoryName: String,
        percentage: Int,
        remainingAmount: String
    ) {
        val title = when {
            percentage >= 100 -> "Budget Exceeded!"
            percentage >= 90 -> "Budget Almost Exhausted"
            percentage >= 80 -> "Budget Alert"
            else -> "Budget Update"
        }
        
        val body = when {
            percentage >= 100 -> "You've exceeded your $categoryName budget"
            else -> "$categoryName budget is $percentage% used. $remainingAmount remaining"
        }
        
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setBadge(NSNumber.numberWithInt(1))
            setCategoryIdentifier("BUDGET_CATEGORY")
        }
        
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "budget_${categoryName}_${System.currentTimeMillis()}",
            content = content,
            trigger = trigger
        )
        
        center.addNotificationRequest(request) { error ->
            error?.let {
                // Handle error
            }
        }
    }
    
    fun scheduleGoalNotification(
        goalName: String,
        isCompleted: Boolean = false,
        progress: Int = 0
    ) {
        val (title, body) = if (isCompleted) {
            "Goal Achieved! 🎉" to "Congratulations! You've completed your $goalName goal"
        } else {
            "Goal Progress Update" to "$goalName is $progress% complete"
        }
        
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setBadge(NSNumber.numberWithInt(1))
            setCategoryIdentifier("GOAL_CATEGORY")
        }
        
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "goal_${goalName}_${System.currentTimeMillis()}",
            content = content,
            trigger = trigger
        )
        
        center.addNotificationRequest(request) { error ->
            error?.let {
                // Handle error
            }
        }
    }
    
    fun scheduleBillReminder(
        billName: String,
        amount: String,
        daysUntilDue: Int
    ) {
        val title = when {
            daysUntilDue <= 0 -> "Bill Overdue!"
            daysUntilDue == 1 -> "Bill Due Tomorrow"
            daysUntilDue <= 3 -> "Bill Due Soon"
            else -> "Upcoming Bill"
        }
        
        val body = if (daysUntilDue <= 0) {
            "$billName ($amount) is overdue"
        } else {
            "$billName ($amount) is due in $daysUntilDue day${if (daysUntilDue > 1) "s" else ""}"
        }
        
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setBadge(NSNumber.numberWithInt(1))
            setCategoryIdentifier("BILL_CATEGORY")
        }
        
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "bill_${billName}_${System.currentTimeMillis()}",
            content = content,
            trigger = trigger
        )
        
        center.addNotificationRequest(request) { error ->
            error?.let {
                // Handle error
            }
        }
    }
    
    fun scheduleSecurityAlert(
        alertType: String,
        details: String
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Security Alert")
            setBody(details)
            setBadge(NSNumber.numberWithInt(1))
            setCategoryIdentifier("SECURITY_CATEGORY")
            
            // Set as critical alert for security notifications
            // This requires special entitlement from Apple
            // setInterruptionLevel(UNNotificationInterruptionLevelCritical)
        }
        
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "security_${alertType}_${System.currentTimeMillis()}",
            content = content,
            trigger = trigger
        )
        
        center.addNotificationRequest(request) { error ->
            error?.let {
                // Handle error
            }
        }
    }
    
    fun setupNotificationCategories() {
        // Setup actionable notification categories
        val transactionActions = listOf(
            UNNotificationAction.actionWithIdentifier(
                identifier = "CATEGORIZE_ACTION",
                title = "Categorize",
                options = UNNotificationActionOptionNone
            ),
            UNNotificationAction.actionWithIdentifier(
                identifier = "VIEW_ACTION",
                title = "View Details",
                options = UNNotificationActionOptionForeground
            )
        )
        
        val transactionCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "TRANSACTION_CATEGORY",
            actions = transactionActions,
            intentIdentifiers = emptyList(),
            options = UNNotificationCategoryOptionNone
        )
        
        val budgetActions = listOf(
            UNNotificationAction.actionWithIdentifier(
                identifier = "VIEW_BUDGET_ACTION",
                title = "View Budget",
                options = UNNotificationActionOptionForeground
            ),
            UNNotificationAction.actionWithIdentifier(
                identifier = "ADJUST_BUDGET_ACTION",
                title = "Adjust Budget",
                options = UNNotificationActionOptionForeground
            )
        )
        
        val budgetCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "BUDGET_CATEGORY",
            actions = budgetActions,
            intentIdentifiers = emptyList(),
            options = UNNotificationCategoryOptionNone
        )
        
        val goalActions = listOf(
            UNNotificationAction.actionWithIdentifier(
                identifier = "CONTRIBUTE_ACTION",
                title = "Contribute",
                options = UNNotificationActionOptionForeground
            ),
            UNNotificationAction.actionWithIdentifier(
                identifier = "VIEW_GOAL_ACTION",
                title = "View Goal",
                options = UNNotificationActionOptionForeground
            )
        )
        
        val goalCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "GOAL_CATEGORY",
            actions = goalActions,
            intentIdentifiers = emptyList(),
            options = UNNotificationCategoryOptionNone
        )
        
        val billActions = listOf(
            UNNotificationAction.actionWithIdentifier(
                identifier = "PAY_BILL_ACTION",
                title = "Pay Now",
                options = UNNotificationActionOptionForeground
            ),
            UNNotificationAction.actionWithIdentifier(
                identifier = "REMIND_LATER_ACTION",
                title = "Remind Later",
                options = UNNotificationActionOptionNone
            )
        )
        
        val billCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "BILL_CATEGORY",
            actions = billActions,
            intentIdentifiers = emptyList(),
            options = UNNotificationCategoryOptionNone
        )
        
        val securityActions = listOf(
            UNNotificationAction.actionWithIdentifier(
                identifier = "SECURE_ACCOUNT_ACTION",
                title = "Secure Account",
                options = UNNotificationActionOptionForeground
            ),
            UNNotificationAction.actionWithIdentifier(
                identifier = "REVIEW_ACTION",
                title = "Review Activity",
                options = UNNotificationActionOptionForeground
            )
        )
        
        val securityCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "SECURITY_CATEGORY",
            actions = securityActions,
            intentIdentifiers = emptyList(),
            options = UNNotificationCategoryOptionNone
        )
        
        val categories = setOf(
            transactionCategory,
            budgetCategory,
            goalCategory,
            billCategory,
            securityCategory
        )
        
        center.setNotificationCategories(categories)
    }
}

/**
 * iOS notification scheduling functions
 */
@Composable
fun IOSFinancialNotifications() {
    LaunchedEffect(Unit) {
        val notificationManager = IOSNotificationManager()
        notificationManager.setupNotificationCategories()
    }
}