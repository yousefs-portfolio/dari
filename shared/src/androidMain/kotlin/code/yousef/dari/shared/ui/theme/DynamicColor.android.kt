package code.yousef.dari.shared.ui.theme

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android-specific dynamic color implementation for Material You
 * Available on Android 12+ (API 31+)
 */
@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean) = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
    else -> {
        // Fallback to static color scheme for older Android versions
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
}

/**
 * Check if dynamic color is supported on this device
 */
@Composable
actual fun isDynamicColorSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}