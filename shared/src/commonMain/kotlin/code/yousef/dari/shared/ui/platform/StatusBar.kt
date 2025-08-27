package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Platform-agnostic status bar configuration interface
 */
@Composable
expect fun ConfigureStatusBar(
    statusBarColor: Color = Color.Transparent,
    isLightStatusBar: Boolean = false,
    isEdgeToEdge: Boolean = false
)

/**
 * Hide the status bar completely
 */
@Composable
expect fun HideStatusBar()

/**
 * Show the status bar
 */
@Composable
expect fun ShowStatusBar()

/**
 * Common status bar configuration for financial apps
 */
@Composable
fun ConfigureFinancialThemeStatusBar(
    isDarkTheme: Boolean = false,
    isTransparent: Boolean = false
) {
    ConfigureStatusBar(
        statusBarColor = if (isTransparent) Color.Transparent else Color.Black,
        isLightStatusBar = !isDarkTheme,
        isEdgeToEdge = isTransparent
    )
}