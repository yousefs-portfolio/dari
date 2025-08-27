package code.yousef.dari.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * iOS-specific dynamic color implementation
 * iOS doesn't support Material You dynamic colors, so we adapt to system appearance
 */
@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme {
    // iOS fallback to static color scheme with system appearance adaptation
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

/**
 * Dynamic color is not supported on iOS in the same way as Android Material You
 */
@Composable
actual fun isDynamicColorSupported(): Boolean = false