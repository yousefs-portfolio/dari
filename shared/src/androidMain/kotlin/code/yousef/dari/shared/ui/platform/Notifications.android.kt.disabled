package code.yousef.dari.shared.ui.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Android-specific notification channel setup for financial app
 */
class AndroidNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_TRANSACTIONS = "transactions"
        const val CHANNEL_BUDGETS = "budgets"
        const val CHANNEL_GOALS = "goals"
        const val CHANNEL_BILLS = "bills"
        const val CHANNEL_SECURITY = "security"
        const val CHANNEL_GENERAL = "general"
    }
    
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createTransactionChannel()
            createBudgetChannel()
            createGoalChannel()
            createBillsChannel()
            createSecurityChannel()
            createGeneralChannel()
        }
    }
    
    private fun createTransactionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new transactions and transaction updates"
                enableLights(true)
                enableVibration(true)
                setSound(null, null) // Use default sound for financial privacy
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createBudgetChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_BUDGETS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget limits and spending alerts"
                enableLights(true)
                enableVibration(true)
                lightColor = android.graphics.Color.YELLOW
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createGoalChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_GOALS,
                "Goal Progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for goal milestones and achievements"
                enableLights(true)
                enableVibration(false)
                lightColor = android.graphics.Color.GREEN
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createBillsChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_BILLS,
                "Bill Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming bills and payment reminders"
                enableLights(true)
                enableVibration(true)
                lightColor = android.graphics.Color.RED
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createSecurityChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SECURITY,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for security alerts and account access"
                enableLights(true)
                enableVibration(true)
                lightColor = android.graphics.Color.RED
                setBypassDnd(true) // Security alerts bypass Do Not Disturb
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createGeneralChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General app notifications and updates"
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    fun showTransactionNotification(
        title: String,
        message: String,
        amount: String,
        isIncome: Boolean = false
    ) {
        if (!hasNotificationPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(if (isIncome) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(amount)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    fun showBudgetAlertNotification(
        categoryName: String,
        percentage: Int,
        remainingAmount: String
    ) {
        if (!hasNotificationPermission()) return
        
        val title = when {
            percentage >= 100 -> "Budget Exceeded!"
            percentage >= 90 -> "Budget Almost Exhausted"
            percentage >= 80 -> "Budget Alert"
            else -> "Budget Update"
        }
        
        val message = when {
            percentage >= 100 -> "You've exceeded your $categoryName budget"
            else -> "$categoryName budget is $percentage% used. $remainingAmount remaining"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGETS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (percentage >= 90) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notificationManager.notify("budget_$categoryName".hashCode(), notification)
    }
    
    fun showGoalAchievementNotification(
        goalName: String,
        isCompleted: Boolean = false,
        progress: Int = 0
    ) {
        if (!hasNotificationPermission()) return
        
        val (title, message) = if (isCompleted) {
            "Goal Achieved! 🎉" to "Congratulations! You've completed your $goalName goal"
        } else {
            "Goal Progress Update" to "$goalName is $progress% complete"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_GOALS)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        
        notificationManager.notify("goal_$goalName".hashCode(), notification)
    }
    
    fun showBillReminderNotification(
        billName: String,
        amount: String,
        daysUntilDue: Int
    ) {
        if (!hasNotificationPermission()) return
        
        val title = when {
            daysUntilDue <= 0 -> "Bill Overdue!"
            daysUntilDue == 1 -> "Bill Due Tomorrow"
            daysUntilDue <= 3 -> "Bill Due Soon"
            else -> "Upcoming Bill"
        }
        
        val message = if (daysUntilDue <= 0) {
            "$billName ($amount) is overdue"
        } else {
            "$billName ($amount) is due in $daysUntilDue day${if (daysUntilDue > 1) "s" else ""}"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (daysUntilDue <= 1) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notificationManager.notify("bill_$billName".hashCode(), notification)
    }
    
    fun showSecurityAlertNotification(
        alertType: SecurityAlertType,
        details: String
    ) {
        if (!hasNotificationPermission()) return
        
        val (title, icon) = when (alertType) {
            SecurityAlertType.SUSPICIOUS_LOGIN -> "Suspicious Login Detected" to android.R.drawable.ic_dialog_alert
            SecurityAlertType.ACCOUNT_ACCESSED -> "Account Access Alert" to android.R.drawable.ic_dialog_info
            SecurityAlertType.PASSWORD_CHANGE -> "Password Changed" to android.R.drawable.ic_dialog_info
            SecurityAlertType.ACCOUNT_LOCKED -> "Account Locked" to android.R.drawable.ic_dialog_alert
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(details)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // Hide on lock screen for security
            .build()
        
        notificationManager.notify("security_${alertType.name}".hashCode(), notification)
    }
}

enum class SecurityAlertType {
    SUSPICIOUS_LOGIN,
    ACCOUNT_ACCESSED,
    PASSWORD_CHANGE,
    ACCOUNT_LOCKED
}

/**
 * Composable function to initialize notification channels
 */
@Composable
actual fun SetupNotificationChannels() {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val notificationManager = AndroidNotificationManager(context)
        notificationManager.createNotificationChannels()
    }
}

/**
 * Check notification permission
 */
@Composable
actual fun hasNotificationPermission(): Boolean {
    val context = LocalContext.current
    return AndroidNotificationManager(context).hasNotificationPermission()
}

/**
 * Show a financial notification
 */
@Composable
fun ShowFinancialNotification(
    type: NotificationType,
    title: String,
    message: String,
    data: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current
    val notificationManager = remember { AndroidNotificationManager(context) }
    
    LaunchedEffect(type, title, message) {
        when (type) {
            NotificationType.TRANSACTION -> {
                notificationManager.showTransactionNotification(
                    title = title,
                    message = message,
                    amount = data["amount"] ?: "",
                    isIncome = data["type"] == "income"
                )
            }
            NotificationType.BUDGET_ALERT -> {
                notificationManager.showBudgetAlertNotification(
                    categoryName = data["category"] ?: "",
                    percentage = data["percentage"]?.toIntOrNull() ?: 0,
                    remainingAmount = data["remaining"] ?: ""
                )
            }
            NotificationType.GOAL_PROGRESS -> {
                notificationManager.showGoalAchievementNotification(
                    goalName = data["goal"] ?: "",
                    isCompleted = data["completed"] == "true",
                    progress = data["progress"]?.toIntOrNull() ?: 0
                )
            }
            NotificationType.BILL_REMINDER -> {
                notificationManager.showBillReminderNotification(
                    billName = data["bill"] ?: "",
                    amount = data["amount"] ?: "",
                    daysUntilDue = data["days"]?.toIntOrNull() ?: 0
                )
            }
        }
    }
}

enum class NotificationType {
    TRANSACTION,
    BUDGET_ALERT,
    GOAL_PROGRESS,
    BILL_REMINDER
}