package code.yousef.dari.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dari App Color Schemes
 * Material 3 compliant colors with Saudi-inspired touches
 * Supports light, dark, and dynamic theming
 */

// Primary Colors - Saudi Green inspired
val DariGreen10 = Color(0xFF0D2818)
val DariGreen20 = Color(0xFF1A4F30)
val DariGreen30 = Color(0xFF267648)
val DariGreen40 = Color(0xFF339E60)
val DariGreen50 = Color(0xFF40C578)
val DariGreen60 = Color(0xFF66D394)
val DariGreen70 = Color(0xFF8CE0B0)
val DariGreen80 = Color(0xFFB3EDCC)
val DariGreen90 = Color(0xFFD9F7E8)
val DariGreen95 = Color(0xFFECFBF4)
val DariGreen99 = Color(0xFFF8FDF9)

// Secondary Colors - Desert Sand inspired
val DariSand10 = Color(0xFF2B1A0F)
val DariSand20 = Color(0xFF56341E)
val DariSand30 = Color(0xFF814F2D)
val DariSand40 = Color(0xFFAC693C)
val DariSand50 = Color(0xFFD7844B)
val DariSand60 = Color(0xFFDF9F6F)
val DariSand70 = Color(0xFFE7BA93)
val DariSand80 = Color(0xFFEFD5B7)
val DariSand90 = Color(0xFFF7F0DB)
val DariSand95 = Color(0xFFFBF8ED)
val DariSand99 = Color(0xFFFEFDFA)

// Tertiary Colors - Saudi Blue inspired
val DariBlue10 = Color(0xFF0A1B2E)
val DariBlue20 = Color(0xFF14365C)
val DariBlue30 = Color(0xFF1E518A)
val DariBlue40 = Color(0xFF286CB8)
val DariBlue50 = Color(0xFF3287E6)
val DariBlue60 = Color(0xFF5B9FEB)
val DariBlue70 = Color(0xFF84B7F0)
val DariBlue80 = Color(0xFFADCFF5)
val DariBlue90 = Color(0xFFD6E7FA)
val DariBlue95 = Color(0xFFEBF3FD)
val DariBlue99 = Color(0xFFF8FBFE)

// Neutral Colors
val DariNeutral10 = Color(0xFF1C1C1C)
val DariNeutral20 = Color(0xFF313131)
val DariNeutral30 = Color(0xFF484848)
val DariNeutral40 = Color(0xFF606060)
val DariNeutral50 = Color(0xFF787878)
val DariNeutral60 = Color(0xFF919191)
val DariNeutral70 = Color(0xFFABABAB)
val DariNeutral80 = Color(0xFFC7C7C7)
val DariNeutral90 = Color(0xFFE3E3E3)
val DariNeutral95 = Color(0xFFF1F1F1)
val DariNeutral99 = Color(0xFFFBFBFB)

// Error Colors - Red palette
val DariError10 = Color(0xFF410002)
val DariError20 = Color(0xFF690005)
val DariError30 = Color(0xFF93000A)
val DariError40 = Color(0xFFBA1A1A)
val DariError50 = Color(0xFFDE3730)
val DariError60 = Color(0xFFFF5449)
val DariError70 = Color(0xFFFF897D)
val DariError80 = Color(0xFFFFB4AB)
val DariError90 = Color(0xFFFFDAD6)
val DariError95 = Color(0xFFFFEDEA)
val DariError99 = Color(0xFFFFFBFF)

// Financial Colors - Semantic colors for financial data
val DariIncomeGreen = Color(0xFF0D7337)
val DariExpenseRed = Color(0xFFD32F2F)
val DariWarningOrange = Color(0xFFFF8F00)
val DariSavingsBlue = Color(0xFF1976D2)
val DariInvestmentPurple = Color(0xFF7B1FA2)
val DariDebtRed = Color(0xFFC62828)

// Light Theme Color Scheme
val DariLightColorScheme = lightColorScheme(
    primary = DariGreen40,
    onPrimary = Color.White,
    primaryContainer = DariGreen90,
    onPrimaryContainer = DariGreen10,
    
    secondary = DariSand40,
    onSecondary = Color.White,
    secondaryContainer = DariSand90,
    onSecondaryContainer = DariSand10,
    
    tertiary = DariBlue40,
    onTertiary = Color.White,
    tertiaryContainer = DariBlue90,
    onTertiaryContainer = DariBlue10,
    
    error = DariError40,
    onError = Color.White,
    errorContainer = DariError90,
    onErrorContainer = DariError10,
    
    background = DariNeutral99,
    onBackground = DariNeutral10,
    surface = DariNeutral99,
    onSurface = DariNeutral10,
    
    surfaceVariant = DariNeutral90,
    onSurfaceVariant = DariNeutral30,
    outline = DariNeutral50,
    outlineVariant = DariNeutral80,
    
    scrim = Color.Black,
    inverseSurface = DariNeutral20,
    inverseOnSurface = DariNeutral95,
    inversePrimary = DariGreen80,
    
    surfaceDim = DariNeutral95,
    surfaceBright = DariNeutral99,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = DariNeutral95,
    surfaceContainer = DariNeutral90,
    surfaceContainerHigh = DariNeutral80,
    surfaceContainerHighest = DariNeutral70
)

// Dark Theme Color Scheme
val DariDarkColorScheme = darkColorScheme(
    primary = DariGreen80,
    onPrimary = DariGreen20,
    primaryContainer = DariGreen30,
    onPrimaryContainer = DariGreen90,
    
    secondary = DariSand80,
    onSecondary = DariSand20,
    secondaryContainer = DariSand30,
    onSecondaryContainer = DariSand90,
    
    tertiary = DariBlue80,
    onTertiary = DariBlue20,
    tertiaryContainer = DariBlue30,
    onTertiaryContainer = DariBlue90,
    
    error = DariError80,
    onError = DariError20,
    errorContainer = DariError30,
    onErrorContainer = DariError90,
    
    background = DariNeutral10,
    onBackground = DariNeutral90,
    surface = DariNeutral10,
    onSurface = DariNeutral90,
    
    surfaceVariant = DariNeutral30,
    onSurfaceVariant = DariNeutral80,
    outline = DariNeutral60,
    outlineVariant = DariNeutral30,
    
    scrim = Color.Black,
    inverseSurface = DariNeutral90,
    inverseOnSurface = DariNeutral20,
    inversePrimary = DariGreen40,
    
    surfaceDim = DariNeutral10,
    surfaceBright = DariNeutral30,
    surfaceContainerLowest = DariNeutral10,
    surfaceContainerLow = DariNeutral20,
    surfaceContainer = DariNeutral30,
    surfaceContainerHigh = DariNeutral40,
    surfaceContainerHighest = DariNeutral50
)

/**
 * Custom colors for financial data visualization
 */
data class FinancialColors(
    val income: Color,
    val expense: Color,
    val savings: Color,
    val investment: Color,
    val debt: Color,
    val warning: Color,
    val success: Color,
    val neutral: Color
)

val LightFinancialColors = FinancialColors(
    income = DariIncomeGreen,
    expense = DariExpenseRed,
    savings = DariSavingsBlue,
    investment = DariInvestmentPurple,
    debt = DariDebtRed,
    warning = DariWarningOrange,
    success = DariGreen40,
    neutral = DariNeutral50
)

val DarkFinancialColors = FinancialColors(
    income = Color(0xFF4CAF50),
    expense = Color(0xFFFF6B6B),
    savings = Color(0xFF42A5F5),
    investment = Color(0xFFAB47BC),
    debt = Color(0xFFEF5350),
    warning = Color(0xFFFFB74D),
    success = DariGreen80,
    neutral = DariNeutral70
)

/**
 * Get appropriate color scheme based on system theme
 */
@Composable
fun getDariColorScheme(darkTheme: Boolean = isSystemInDarkTheme()): ColorScheme {
    return if (darkTheme) DariDarkColorScheme else DariLightColorScheme
}

/**
 * Get appropriate financial colors based on system theme
 */
@Composable
fun getFinancialColors(darkTheme: Boolean = isSystemInDarkTheme()): FinancialColors {
    return if (darkTheme) DarkFinancialColors else LightFinancialColors
}

/**
 * Category-specific colors for consistent UI
 */
object CategoryColors {
    val Food = Color(0xFFF44336)
    val Transport = Color(0xFF9C27B0)
    val Shopping = Color(0xFFE91E63)
    val Healthcare = Color(0xFF009688)
    val Utilities = Color(0xFF607D8B)
    val Entertainment = Color(0xFFFF5722)
    val Education = Color(0xFF3F51B5)
    val Income = Color(0xFF4CAF50)
    val Savings = Color(0xFF2196F3)
    val Investment = Color(0xFF7B1FA2)
    val Default = Color(0xFF757575)
    
    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName.lowercase()) {
            "food", "dining", "restaurant" -> Food
            "transport", "fuel", "car" -> Transport
            "shopping", "retail", "store" -> Shopping
            "healthcare", "medical", "pharmacy" -> Healthcare
            "utilities", "electricity", "water", "internet" -> Utilities
            "entertainment", "movie", "game" -> Entertainment
            "education", "school", "course" -> Education
            "income", "salary", "bonus" -> Income
            "savings", "deposit" -> Savings
            "investment", "stocks", "bonds" -> Investment
            else -> Default
        }
    }
}

/**
 * Account type specific colors
 */
object AccountTypeColors {
    val Current = DariBlue40
    val Savings = DariGreen40
    val Credit = DariError40
    val Investment = DariInvestmentPurple
    val Loan = DariDebtRed
    val Default = DariNeutral50
    
    fun getAccountTypeColor(accountType: String): Color {
        return when (accountType.uppercase()) {
            "CURRENT" -> Current
            "SAVINGS" -> Savings
            "CREDIT" -> Credit
            "INVESTMENT" -> Investment
            "LOAN" -> Loan
            else -> Default
        }
    }
}

/**
 * Status colors for various UI states
 */
object StatusColors {
    val Success = DariGreen40
    val Warning = DariWarningOrange
    val Error = DariError40
    val Info = DariBlue40
    val Pending = DariSand40
    val Inactive = DariNeutral50
}

/**
 * Chart colors for data visualization
 */
object ChartColors {
    val Primary = listOf(
        DariGreen40, DariBlue40, DariSand40, DariError40,
        DariGreen60, DariBlue60, DariSand60, DariError60,
        DariGreen80, DariBlue80, DariSand80, DariError80
    )
    
    val Gradient = listOf(
        DariGreen40, DariGreen60, DariGreen80,
        DariBlue40, DariBlue60, DariBlue80,
        DariSand40, DariSand60, DariSand80
    )
    
    val Monochromatic = listOf(
        DariGreen30, DariGreen40, DariGreen50, DariGreen60, DariGreen70
    )
}