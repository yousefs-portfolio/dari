package code.yousef.dari.shared.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Saudi-inspired brand colors
private val PrimaryGreen = Color(0xFF1B5E20)      // Saudi flag green
private val PrimaryGreenLight = Color(0xFF4CAF50)
private val PrimaryGreenDark = Color(0xFF0D3818)

private val SecondaryGold = Color(0xFFFFC107)     // Gold accent
private val SecondaryGoldLight = Color(0xFFFFD54F)
private val SecondaryGoldDark = Color(0xFFF57C00)

// Financial colors
private val IncomeGreen = Color(0xFF2E7D32)
private val ExpenseRed = Color(0xFFD32F2F)
private val SavingsBlue = Color(0xFF1976D2)
private val WarningOrange = Color(0xFFFF9800)

// Neutral colors
private val SurfaceLight = Color(0xFFFAFAFA)
private val SurfaceDark = Color(0xFF121212)
private val OnSurfaceLight = Color(0xFF1A1A1A)
private val OnSurfaceDark = Color(0xFFE1E1E1)

val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = Color.White,
    
    secondary = SecondaryGold,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryGoldLight,
    onSecondaryContainer = Color.Black,
    
    tertiary = SavingsBlue,
    onTertiary = Color.White,
    
    error = ExpenseRed,
    onError = Color.White,
    
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    
    background = Color.White,
    onBackground = OnSurfaceLight,
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenLight,
    onPrimary = Color.Black,
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = PrimaryGreenLight,
    
    secondary = SecondaryGoldLight,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryGoldDark,
    onSecondaryContainer = SecondaryGoldLight,
    
    tertiary = Color(0xFF42A5F5),
    onTertiary = Color.Black,
    
    error = Color(0xFFEF5350),
    onError = Color.Black,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    
    background = Color(0xFF000000),
    onBackground = OnSurfaceDark,
    
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242)
)

// Financial-specific colors
object FinancialColors {
    val Income = IncomeGreen
    val IncomeLight = Color(0xFF4CAF50)
    val IncomeDark = Color(0xFF1B5E20)
    
    val Expense = ExpenseRed
    val ExpenseLight = Color(0xFFEF5350)
    val ExpenseDark = Color(0xFFC62828)
    
    val Savings = SavingsBlue
    val SavingsLight = Color(0xFF42A5F5)
    val SavingsDark = Color(0xFF1565C0)
    
    val Warning = WarningOrange
    val WarningLight = Color(0xFFFFB74D)
    val WarningDark = Color(0xFFF57C00)
    
    val Goal = Color(0xFF7B1FA2)
    val GoalLight = Color(0xFFBA68C8)
    val GoalDark = Color(0xFF4A148C)
    
    val Budget = Color(0xFF388E3C)
    val BudgetLight = Color(0xFF66BB6A)
    val BudgetDark = Color(0xFF2E7D32)
}