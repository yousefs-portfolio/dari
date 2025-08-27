package code.yousef.dari.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// Local composition for financial colors
val LocalFinancialColors = staticCompositionLocalOf { FinancialColors }
val LocalFinancialTextStyles = staticCompositionLocalOf { FinancialTextStyles }
val LocalFinancialShapes = staticCompositionLocalOf { FinancialShapes }

@Composable
fun DariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && isDynamicColorSupported() -> getDynamicColorScheme(darkTheme)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalFinancialColors provides FinancialColors,
        LocalFinancialTextStyles provides FinancialTextStyles,
        LocalFinancialShapes provides FinancialShapes,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

// Extension properties for easy access to custom design tokens
object DariTheme {
    val financialColors: FinancialColors
        @Composable
        get() = LocalFinancialColors.current
    
    val financialTextStyles: FinancialTextStyles
        @Composable
        get() = LocalFinancialTextStyles.current
    
    val financialShapes: FinancialShapes
        @Composable
        get() = LocalFinancialShapes.current
}