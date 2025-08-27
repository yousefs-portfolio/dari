package code.yousef.dari.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Expect declaration for dynamic color support across platforms
 */
@Composable
expect fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme

/**
 * Expect declaration to check if dynamic color is supported
 */
@Composable
expect fun isDynamicColorSupported(): Boolean