package code.yousef.dari.shared.ui.platform

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Platform-agnostic safe area insets data class
 */
data class PlatformInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp,
    val right: Dp = 0.dp
)

/**
 * Get platform-specific safe area insets
 */
@Composable
expect fun SafeAreaInsets(): PlatformInsets

/**
 * Apply safe area padding
 */
@Composable
expect fun Modifier.safeAreaPadding(): Modifier

/**
 * Apply only top safe area padding
 */
@Composable
expect fun Modifier.safeAreaTopPadding(): Modifier

/**
 * Apply only bottom safe area padding
 */
@Composable
expect fun Modifier.safeAreaBottomPadding(): Modifier

/**
 * Safe area wrapper composable for financial app screens
 */
@Composable
fun FinancialScreenSafeArea(
    includeTopPadding: Boolean = true,
    includeBottomPadding: Boolean = true,
    content: @Composable () -> Unit
) {
    val insets = SafeAreaInsets()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = if (includeTopPadding) insets.top else 0.dp,
                bottom = if (includeBottomPadding) insets.bottom else 0.dp,
                start = insets.left,
                end = insets.right
            )
    ) {
        content()
    }
}

/**
 * Safe area for dashboard with transparent status bar
 */
@Composable
fun DashboardSafeArea(
    content: @Composable () -> Unit
) {
    FinancialScreenSafeArea(
        includeTopPadding = false, // Dashboard handles its own top padding
        includeBottomPadding = true,
        content = content
    )
}

/**
 * Safe area for modal sheets
 */
@Composable
fun ModalSheetSafeArea(
    content: @Composable () -> Unit
) {
    FinancialScreenSafeArea(
        includeTopPadding = false, // Modal sheets don't need top padding
        includeBottomPadding = true,
        content = content
    )
}

/**
 * Safe area for fullscreen experiences (like camera or charts)
 */
@Composable
fun FullscreenSafeArea(
    includeAllInsets: Boolean = false,
    content: @Composable () -> Unit
) {
    if (includeAllInsets) {
        FinancialScreenSafeArea(
            includeTopPadding = true,
            includeBottomPadding = true,
            content = content
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * Navigation-aware safe area that accounts for navigation bars
 */
@Composable
fun NavigationAwareSafeArea(
    hasBottomNavigation: Boolean = true,
    content: @Composable () -> Unit
) {
    FinancialScreenSafeArea(
        includeTopPadding = true,
        includeBottomPadding = !hasBottomNavigation, // Don't double-pad if bottom nav handles it
        content = content
    )
}

/**
 * Utility functions for common safe area measurements
 */
@Composable
fun getStatusBarHeight(): Dp {
    return SafeAreaInsets().top
}

@Composable
fun getNavigationBarHeight(): Dp {
    return SafeAreaInsets().bottom
}

@Composable
fun hasNotchOrCutout(): Boolean {
    val insets = SafeAreaInsets()
    return insets.top > 24.dp // Standard status bar height
}

/**
 * Safe area modifiers for specific UI elements
 */
@Composable
fun Modifier.topBarSafeArea(): Modifier {
    return this.safeAreaTopPadding()
}

@Composable
fun Modifier.bottomBarSafeArea(): Modifier {
    return this.safeAreaBottomPadding()
}

@Composable
fun Modifier.horizontalSafeArea(): Modifier {
    val insets = SafeAreaInsets()
    return this.padding(
        start = insets.left,
        end = insets.right
    )
}

/**
 * Financial app specific safe area configurations
 */
@Composable
fun BankingScreenSafeArea(
    showSecurityIndicator: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (showSecurityIndicator) {
            // Add visual security indicator at the top
            Spacer(modifier = Modifier.safeAreaTopPadding())
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalSafeArea()
        ) {
            content()
        }
        
        Spacer(modifier = Modifier.safeAreaBottomPadding())
    }
}