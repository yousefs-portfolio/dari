package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic share sheet interface
 */
@Composable
expect fun ShareSheet(
    content: ShareContent,
    onShared: () -> Unit,
    onCancelled: () -> Unit
)

/**
 * Share content types
 */
sealed class ShareContent {
    data class Text(val text: String, val subject: String? = null) : ShareContent()
    data class File(val filePath: String, val mimeType: String, val subject: String? = null) : ShareContent()
    data class Image(val imagePath: String, val caption: String? = null) : ShareContent()
    data class Url(val url: String, val title: String? = null) : ShareContent()
    data class Multiple(val items: List<ShareContent>) : ShareContent()
}

/**
 * Financial app specific share functions
 */
@Composable
fun ShareFinancialReport(
    reportPath: String,
    reportType: FinancialReportType,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    val (mimeType, subject) = when (reportType) {
        FinancialReportType.MONTHLY_STATEMENT -> "application/pdf" to "Monthly Financial Statement"
        FinancialReportType.EXPENSE_REPORT -> "application/pdf" to "Expense Report"
        FinancialReportType.BUDGET_SUMMARY -> "application/pdf" to "Budget Summary"
        FinancialReportType.TAX_REPORT -> "application/pdf" to "Tax Report"
        FinancialReportType.TRANSACTION_EXPORT -> "text/csv" to "Transaction Export"
    }
    
    ShareSheet(
        content = ShareContent.File(
            filePath = reportPath,
            mimeType = mimeType,
            subject = subject
        ),
        onShared = onShared,
        onCancelled = onCancelled
    )
}

@Composable
fun ShareBudgetInsights(
    insightsText: String,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    ShareSheet(
        content = ShareContent.Text(
            text = insightsText,
            subject = "Budget Insights from Dari Finance"
        ),
        onShared = onShared,
        onCancelled = onCancelled
    )
}

@Composable
fun ShareGoalAchievement(
    goalName: String,
    achievementText: String,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    ShareSheet(
        content = ShareContent.Text(
            text = achievementText,
            subject = "Goal Achievement: $goalName"
        ),
        onShared = onShared,
        onCancelled = onCancelled
    )
}

@Composable
fun ShareReceipt(
    receiptImagePath: String,
    merchantName: String,
    amount: String,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    ShareSheet(
        content = ShareContent.Image(
            imagePath = receiptImagePath,
            caption = "Receipt from $merchantName - $amount"
        ),
        onShared = onShared,
        onCancelled = onCancelled
    )
}

@Composable
fun ShareAppInvite(
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    ShareSheet(
        content = ShareContent.Text(
            text = "Check out Dari Finance - the best app for managing your finances! Download it now from the app store.",
            subject = "Try Dari Finance App"
        ),
        onShared = onShared,
        onCancelled = onCancelled
    )
}

/**
 * Financial report types
 */
enum class FinancialReportType {
    MONTHLY_STATEMENT,
    EXPENSE_REPORT,
    BUDGET_SUMMARY,
    TAX_REPORT,
    TRANSACTION_EXPORT
}

/**
 * Share options for different financial contexts
 */
enum class FinancialShareContext {
    TRANSACTION_DETAILS,
    BUDGET_PROGRESS,
    GOAL_ACHIEVEMENT,
    SPENDING_INSIGHTS,
    ACCOUNT_SUMMARY,
    RECEIPT_BACKUP
}