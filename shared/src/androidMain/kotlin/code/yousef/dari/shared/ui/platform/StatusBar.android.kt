package code.yousef.dari.shared.ui.platform

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Android-specific status bar configuration
 */
@Composable
actual fun ConfigureStatusBar(
    statusBarColor: Color,
    isLightStatusBar: Boolean,
    isEdgeToEdge: Boolean
) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    
    SideEffect {
        window?.let { w ->
            // Configure status bar color
            w.statusBarColor = statusBarColor.toArgb()
            
            // Configure system bars appearance
            WindowCompat.setDecorFitsSystemWindows(w, !isEdgeToEdge)
            
            val controller = WindowCompat.getInsetsController(w, view)
            controller.isAppearanceLightStatusBars = isLightStatusBar
            
            // Enable edge-to-edge if requested
            if (isEdgeToEdge) {
                enableEdgeToEdge(w, view)
            }
        }
    }
}

/**
 * Hide the status bar completely
 */
@Composable
actual fun HideStatusBar() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    
    SideEffect {
        window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, view)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

/**
 * Show the status bar
 */
@Composable
actual fun ShowStatusBar() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    
    SideEffect {
        window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, view)
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}

/**
 * Configure immersive mode for fullscreen experience
 */
@Composable
fun ConfigureImmersiveMode(
    hideSystemBars: Boolean = false,
    hideNavigationBar: Boolean = false,
    hideStatusBar: Boolean = false
) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    
    SideEffect {
        window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, view)
            
            if (hideSystemBars) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                if (hideStatusBar) {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
                if (hideNavigationBar) {
                    controller.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }
        }
    }
}

/**
 * Configure status bar for different screen types
 */
@Composable
fun ConfigureStatusBarForScreen(screenType: ScreenType) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    when (screenType) {
        ScreenType.DASHBOARD -> {
            ConfigureStatusBar(
                statusBarColor = Color.Transparent,
                isLightStatusBar = !isLightTheme,
                isEdgeToEdge = true
            )
        }
        ScreenType.TRANSACTION_DETAILS -> {
            ConfigureStatusBar(
                statusBarColor = surfaceColor,
                isLightStatusBar = isLightTheme,
                isEdgeToEdge = false
            )
        }
        ScreenType.FULLSCREEN_CHART -> {
            HideStatusBar()
        }
        ScreenType.MODAL_SHEET -> {
            ConfigureStatusBar(
                statusBarColor = Color.Black.copy(alpha = 0.3f),
                isLightStatusBar = false,
                isEdgeToEdge = true
            )
        }
        else -> {
            ConfigureStatusBar(
                statusBarColor = surfaceColor,
                isLightStatusBar = isLightTheme,
                isEdgeToEdge = false
            )
        }
    }
}

/**
 * Enable edge-to-edge display
 */
private fun enableEdgeToEdge(window: Window, view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ (API 30+)
        window.setDecorFitsSystemWindows(false)
    } else {
        // Android 10 and below
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
}

/**
 * Configure translucent status bar
 */
@Composable
fun ConfigureTranslucentStatusBar() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    
    SideEffect {
        window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }
        }
    }
}

/**
 * Set status bar to match app theme
 */
@Composable
fun ConfigureThemeStatusBar() {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    
    ConfigureStatusBar(
        statusBarColor = backgroundColor,
        isLightStatusBar = isLightTheme,
        isEdgeToEdge = false
    )
}

/**
 * Configure status bar for financial app screens
 */
@Composable
fun ConfigureFinancialStatusBar(
    isIncomeScreen: Boolean = false,
    isExpenseScreen: Boolean = false,
    isDashboard: Boolean = false
) {
    val statusBarColor = when {
        isDashboard -> Color.Transparent
        isIncomeScreen -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        isExpenseScreen -> Color(0xFFE53935).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val isLightStatusBar = when {
        isDashboard -> false
        else -> MaterialTheme.colorScheme.background.luminance() > 0.5f
    }
    
    ConfigureStatusBar(
        statusBarColor = statusBarColor,
        isLightStatusBar = isLightStatusBar,
        isEdgeToEdge = isDashboard
    )
}

/**
 * Screen type enum for status bar configuration
 */
enum class ScreenType {
    DASHBOARD,
    TRANSACTION_LIST,
    TRANSACTION_DETAILS,
    BUDGET_SCREEN,
    GOALS_SCREEN,
    ANALYTICS_SCREEN,
    SETTINGS_SCREEN,
    FULLSCREEN_CHART,
    MODAL_SHEET,
    CAMERA_SCREEN
}

/**
 * Extension function to get Color luminance
 */
private fun Color.luminance(): Float {
    val sRGB = listOf(red, green, blue).map { component ->
        if (component <= 0.03928f) {
            component / 12.92f
        } else {
            kotlin.math.pow((component + 0.055f) / 1.055f, 2.4f).toFloat()
        }
    }
    return 0.2126f * sRGB[0] + 0.7152f * sRGB[1] + 0.0722f * sRGB[2]
}