package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic back press handling interface
 */
@Composable
expect fun HandleBackPress(
    enabled: Boolean = true,
    onBackPressed: () -> Unit
)

/**
 * Common back press handling for financial app screens
 */
@Composable
fun FinancialScreenBackHandler(
    canGoBack: Boolean = true,
    hasUnsavedData: Boolean = false,
    onConfirmExit: (() -> Unit)? = null,
    onBackPressed: () -> Unit
) {
    HandleBackPress(enabled = canGoBack) {
        if (hasUnsavedData && onConfirmExit != null) {
            onConfirmExit()
        } else {
            onBackPressed()
        }
    }
}