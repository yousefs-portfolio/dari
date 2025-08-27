package code.yousef.dari.shared.ui.platform

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import platform.UIKit.*
import platform.Foundation.*

/**
 * iOS-specific safe area handling using UIKit safe area insets
 */
@Composable
actual fun SafeAreaInsets(): PlatformInsets {
    // Get safe area insets from UIKit
    val safeAreaInsets = remember {
        val window = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        val safeArea = window?.safeAreaInsets ?: UIEdgeInsetsZero.readValue()
        
        PlatformInsets(
            top = safeArea.top.dp,
            bottom = safeArea.bottom.dp,
            left = safeArea.left.dp,
            right = safeArea.right.dp
        )
    }
    
    return safeAreaInsets
}

/**
 * Apply safe area padding for iOS screens
 */
@Composable
actual fun Modifier.safeAreaPadding(): Modifier {
    val insets = SafeAreaInsets()
    return this.padding(
        top = insets.top,
        bottom = insets.bottom,
        start = insets.left,
        end = insets.right
    )
}

/**
 * Apply only top safe area padding (for status bar area)
 */
@Composable
actual fun Modifier.safeAreaTopPadding(): Modifier {
    val insets = SafeAreaInsets()
    return this.padding(top = insets.top)
}

/**
 * Apply only bottom safe area padding (for home indicator area)
 */
@Composable
actual fun Modifier.safeAreaBottomPadding(): Modifier {
    val insets = SafeAreaInsets()
    return this.padding(bottom = insets.bottom)
}

/**
 * iOS-specific safe area handling for financial screens
 */
@Composable
fun IOSSafeAreaWrapper(
    includeStatusBar: Boolean = true,
    includeHomeIndicator: Boolean = true,
    content: @Composable () -> Unit
) {
    val insets = SafeAreaInsets()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = if (includeStatusBar) insets.top else 0.dp,
                bottom = if (includeHomeIndicator) insets.bottom else 0.dp
            )
    ) {
        content()
    }
}

/**
 * iOS-specific navigation bar safe area
 */
@Composable
fun IOSNavigationBarSafeArea(
    content: @Composable () -> Unit
) {
    val insets = SafeAreaInsets()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = insets.top)
    ) {
        content()
    }
}

/**
 * iOS-specific tab bar safe area
 */
@Composable
fun IOSTabBarSafeArea(
    content: @Composable () -> Unit
) {
    val insets = SafeAreaInsets()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = insets.bottom)
    ) {
        content()
    }
}

/**
 * Check if device has notch or Dynamic Island
 */
@Composable
fun hasNotchOrDynamicIsland(): Boolean {
    val insets = SafeAreaInsets()
    return insets.top > 20.dp // Standard status bar height is 20pt
}

/**
 * Get the safe area height for the top area (status bar + notch/Dynamic Island)
 */
@Composable
fun getTopSafeAreaHeight(): androidx.compose.ui.unit.Dp {
    val insets = SafeAreaInsets()
    return insets.top
}

/**
 * Get the safe area height for the bottom area (home indicator)
 */
@Composable
fun getBottomSafeAreaHeight(): androidx.compose.ui.unit.Dp {
    val insets = SafeAreaInsets()
    return insets.bottom
}

/**
 * iOS-specific screen edge detection
 */
@Composable
fun isIPhoneWithNotch(): Boolean {
    return hasNotchOrDynamicIsland()
}

/**
 * iOS device type detection for UI adaptation
 */
enum class IOSDeviceType {
    IPHONE_SE,
    IPHONE_REGULAR,
    IPHONE_PLUS,
    IPHONE_X_SERIES, // iPhone X, XS, 11 Pro, 12 mini, 13 mini
    IPHONE_XR_SERIES, // iPhone XR, 11, 12, 13, 14
    IPHONE_PLUS_SERIES, // iPhone 12 Pro Max, 13 Pro Max, 14 Plus, 14 Pro Max
    IPAD
}

/**
 * Get current iOS device type
 */
@Composable
fun getCurrentIOSDeviceType(): IOSDeviceType {
    val window = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val screenBounds = window?.screen?.bounds ?: CGRectZero.readValue()
    val screenWidth = screenBounds.size.width
    val screenHeight = screenBounds.size.height
    val hasNotch = hasNotchOrDynamicIsland()
    
    return when {
        screenWidth >= 1024 || screenHeight >= 1024 -> IOSDeviceType.IPAD
        hasNotch && (screenWidth == 375.0 || screenHeight == 375.0) -> IOSDeviceType.IPHONE_X_SERIES
        hasNotch && (screenWidth == 414.0 || screenHeight == 414.0) -> IOSDeviceType.IPHONE_XR_SERIES
        hasNotch && (screenWidth >= 428.0 || screenHeight >= 428.0) -> IOSDeviceType.IPHONE_PLUS_SERIES
        screenWidth == 414.0 || screenHeight == 414.0 -> IOSDeviceType.IPHONE_PLUS
        screenWidth == 320.0 || screenHeight == 320.0 -> IOSDeviceType.IPHONE_SE
        else -> IOSDeviceType.IPHONE_REGULAR
    }
}