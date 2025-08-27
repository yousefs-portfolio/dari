package code.yousef.dari.shared.ui.platform

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.*

/**
 * Android-specific safe area handling using WindowInsets
 */
@Composable
actual fun SafeAreaInsets(): PlatformInsets {
    val view = LocalView.current
    val density = LocalDensity.current
    
    val insets = remember {
        val windowInsets = ViewCompat.getRootWindowInsets(view)
        val systemBars = windowInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
        val displayCutout = windowInsets?.getInsets(WindowInsetsCompat.Type.displayCutout())
        
        // Combine system bars and display cutout insets
        val top = maxOf(systemBars?.top ?: 0, displayCutout?.top ?: 0)
        val bottom = maxOf(systemBars?.bottom ?: 0, displayCutout?.bottom ?: 0)
        val left = maxOf(systemBars?.left ?: 0, displayCutout?.left ?: 0)
        val right = maxOf(systemBars?.right ?: 0, displayCutout?.right ?: 0)
        
        with(density) {
            PlatformInsets(
                top = top.toDp(),
                bottom = bottom.toDp(),
                left = left.toDp(),
                right = right.toDp()
            )
        }
    }
    
    return insets
}

/**
 * Apply safe area padding using Android WindowInsets
 */
@Composable
actual fun Modifier.safeAreaPadding(): Modifier {
    return this.systemBarsPadding()
}

/**
 * Apply only top safe area padding
 */
@Composable
actual fun Modifier.safeAreaTopPadding(): Modifier {
    return this.statusBarsPadding()
}

/**
 * Apply only bottom safe area padding
 */
@Composable
actual fun Modifier.safeAreaBottomPadding(): Modifier {
    return this.navigationBarsPadding()
}

/**
 * Android-specific WindowInsets extensions
 */
@Composable
fun Modifier.statusBarsPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.statusBars)
}

@Composable
fun Modifier.navigationBarsPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.navigationBars)
}

@Composable
fun Modifier.systemBarsPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.systemBars)
}

@Composable
fun Modifier.imePadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.ime)
}

/**
 * Android-specific cutout handling
 */
@Composable
fun Modifier.displayCutoutPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.displayCutout)
}

/**
 * Combined system bars and display cutout padding
 */
@Composable
fun Modifier.safeDrawingPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.safeDrawing)
}

/**
 * Android-specific keyboard (IME) awareness
 */
@Composable
fun KeyboardAwareSafeArea(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding()
    ) {
        content()
    }
}

/**
 * Android navigation gesture area handling
 */
@Composable
fun NavigationGestureSafeArea(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemGesturesPadding()
    ) {
        content()
    }
}

@Composable
fun Modifier.systemGesturesPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.systemGestures)
}

/**
 * Android-specific edge-to-edge support
 */
@Composable
fun EdgeToEdgeSafeArea(
    includeStatusBar: Boolean = true,
    includeNavigationBar: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .let { modifier ->
                when {
                    includeStatusBar && includeNavigationBar -> modifier.systemBarsPadding()
                    includeStatusBar -> modifier.statusBarsPadding()
                    includeNavigationBar -> modifier.navigationBarsPadding()
                    else -> modifier
                }
            }
    ) {
        content()
    }
}

/**
 * Handle different Android device types
 */
@Composable
fun isAndroidTablet(): Boolean {
    // This would typically be determined by screen size or configuration
    // For now, return false as a placeholder
    return false
}

@Composable
fun hasNavigationBar(): Boolean {
    val insets = SafeAreaInsets()
    return insets.bottom > 0.dp
}

@Composable
fun hasDisplayCutout(): Boolean {
    val view = LocalView.current
    val windowInsets = ViewCompat.getRootWindowInsets(view)
    val cutout = windowInsets?.getInsets(WindowInsetsCompat.Type.displayCutout())
    return cutout != null && (cutout.top > 0 || cutout.left > 0 || cutout.right > 0 || cutout.bottom > 0)
}

/**
 * Android-specific banking security safe area
 */
@Composable
fun AndroidBankingSafeArea(
    enforceSecureDisplay: Boolean = true,
    content: @Composable () -> Unit
) {
    if (enforceSecureDisplay) {
        // In a real app, you would set FLAG_SECURE here
        // window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        content()
    }
}

/**
 * Android-specific multi-window support
 */
@Composable
fun MultiWindowSafeArea(
    content: @Composable () -> Unit
) {
    // Handle multi-window mode adjustments
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        content()
    }
}