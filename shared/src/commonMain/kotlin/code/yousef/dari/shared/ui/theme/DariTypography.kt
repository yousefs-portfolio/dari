package code.yousef.dari.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Dari App Typography
 * Material 3 compliant typography with support for Arabic and English text
 * Optimized for financial data display and readability
 */

// Font weights
object FontWeights {
    val Thin = FontWeight.W100
    val Light = FontWeight.W300
    val Regular = FontWeight.W400
    val Medium = FontWeight.W500
    val SemiBold = FontWeight.W600
    val Bold = FontWeight.W700
    val ExtraBold = FontWeight.W800
    val Black = FontWeight.W900
}

// Line heights for better readability
object LineHeights {
    val Tight = 1.1
    val Normal = 1.4
    val Relaxed = 1.6
    val Loose = 1.8
}

// Base Typography following Material 3 guidelines
val DariTypography = Typography(
    // Display styles - Large headlines and hero text
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles - Section headers and important text
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles - Card headers and important labels
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Label styles - Form labels and captions
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    
    // Body styles - Regular text content
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

/**
 * Custom typography extensions for financial app needs
 */
object FinancialTypography {
    
    // Money amount styles - Optimized for displaying financial data
    val MoneyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
    
    val MoneyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    val MoneySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    val MoneyCaption = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Regular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    // Account information styles
    val AccountName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    )
    
    val AccountNumber = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Regular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val BankName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    // Transaction styles
    val TransactionAmount = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    val TransactionDescription = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val TransactionMerchant = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val TransactionDate = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    val TransactionCategory = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    // Budget and goal styles
    val BudgetAmount = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    val BudgetCategory = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val GoalProgress = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    val GoalTarget = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    // Chart and analytics styles
    val ChartLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    val ChartValue = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    val StatisticValue = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    val StatisticLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    // Status and indicator styles
    val StatusText = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    val PercentageText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    val CurrencySymbol = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
}

/**
 * Arabic typography support
 */
object ArabicTypography {
    
    // Arabic text requires different line heights and letter spacing
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 57.sp,
        lineHeight = 68.sp, // Increased for Arabic
        letterSpacing = 0.sp
    )
    
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 32.sp,
        lineHeight = 44.sp, // Increased for Arabic
        letterSpacing = 0.sp
    )
    
    val TitleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Medium,
        fontSize = 16.sp,
        lineHeight = 28.sp, // Increased for Arabic
        letterSpacing = 0.sp
    )
    
    val BodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeights.Regular,
        fontSize = 16.sp,
        lineHeight = 28.sp, // Increased for Arabic
        letterSpacing = 0.sp
    )
    
    val MoneyAmount = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeights.Bold,
        fontSize = 24.sp,
        lineHeight = 36.sp, // Increased for Arabic numerals
        letterSpacing = 0.sp
    )
}

/**
 * Responsive typography that adapts to screen size
 */
object ResponsiveTypography {
    
    fun getDisplayLarge(isTablet: Boolean = false): TextStyle {
        return if (isTablet) {
            DariTypography.displayLarge.copy(fontSize = 64.sp)
        } else {
            DariTypography.displayLarge
        }
    }
    
    fun getMoneyLarge(isTablet: Boolean = false): TextStyle {
        return if (isTablet) {
            FinancialTypography.MoneyLarge.copy(fontSize = 40.sp)
        } else {
            FinancialTypography.MoneyLarge
        }
    }
    
    fun getBodyLarge(isTablet: Boolean = false): TextStyle {
        return if (isTablet) {
            DariTypography.bodyLarge.copy(fontSize = 18.sp)
        } else {
            DariTypography.bodyLarge
        }
    }
}

/**
 * Typography utilities
 */
object TypographyUtils {
    
    /**
     * Get appropriate text style for money amounts based on value
     */
    fun getMoneyStyle(amount: Double): TextStyle {
        return when {
            amount >= 100000 -> FinancialTypography.MoneyLarge
            amount >= 1000 -> FinancialTypography.MoneyMedium
            else -> FinancialTypography.MoneySmall
        }
    }
    
    /**
     * Get text style with emphasis
     */
    fun emphasized(style: TextStyle): TextStyle {
        return style.copy(fontWeight = FontWeights.Bold)
    }
    
    /**
     * Get text style with reduced opacity for secondary text
     */
    fun secondary(style: TextStyle): TextStyle {
        return style.copy(color = style.color.copy(alpha = 0.7f))
    }
    
    /**
     * Get text style for disabled state
     */
    fun disabled(style: TextStyle): TextStyle {
        return style.copy(color = style.color.copy(alpha = 0.38f))
    }
}