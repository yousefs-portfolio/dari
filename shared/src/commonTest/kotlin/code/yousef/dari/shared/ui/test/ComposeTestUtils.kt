package code.yousef.dari.shared.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import code.yousef.dari.shared.ui.theme.DariTheme

/**
 * Common utilities for Compose UI testing
 */

/**
 * Sets up the UI test with the Dari theme
 */
fun ComposeUiTest.setContentWithTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    setContent {
        DariTheme(darkTheme = darkTheme) {
            content()
        }
    }
}

/**
 * Tests a composable in both light and dark themes
 */
fun ComposeUiTest.testBothThemes(
    testName: String,
    content: @Composable () -> Unit,
    test: ComposeUiTest.() -> Unit
) {
    // Test light theme
    setContentWithTheme(darkTheme = false, content = content)
    test()
    
    // Test dark theme
    setContentWithTheme(darkTheme = true, content = content)
    test()
}

/**
 * Common assertions for financial UI components
 */
object FinancialTestAssertions {
    
    /**
     * Asserts that a currency amount is displayed correctly
     */
    fun ComposeUiTest.assertCurrencyDisplayed(amount: String) {
        // Implementation will check for proper SAR formatting
        // Example: "1,234.56 SAR" or "١٢٣٤.٥٦ ريال" in Arabic
    }
    
    /**
     * Asserts that percentage is displayed correctly
     */
    fun ComposeUiTest.assertPercentageDisplayed(percentage: String) {
        // Implementation will check for percentage formatting
        // Example: "15.5%" or "١٥.٥٪" in Arabic
    }
    
    /**
     * Asserts accessibility properties are set correctly
     */
    fun ComposeUiTest.assertAccessibilityCompliance() {
        // Implementation will check:
        // - Content descriptions
        // - Semantic properties
        // - Touch target sizes
        // - Color contrast (if possible)
    }
}