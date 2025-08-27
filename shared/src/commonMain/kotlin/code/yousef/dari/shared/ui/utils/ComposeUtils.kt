package code.yousef.dari.shared.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Common UI utilities and extension functions to reduce code duplication
 */

/**
 * Clickable modifier without ripple effect
 */
fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/**
 * Common date formatting utilities
 */
object DateFormatter {
    fun formatShort(date: LocalDate): String {
        return "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
    }
    
    fun formatMedium(date: LocalDate): String {
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        return "${date.dayOfMonth} ${months[date.monthNumber - 1]} ${date.year}"
    }
    
    fun formatLong(date: LocalDate): String {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return "${date.dayOfMonth} ${months[date.monthNumber - 1]} ${date.year}"
    }
}

/**
 * Common amount formatting utilities
 */
object AmountFormatter {
    fun format(amount: Double, currency: String = "SAR"): String {
        return "$currency ${String.format("%.2f", abs(amount))}"
    }
    
    fun formatWithSign(amount: Double, currency: String = "SAR"): String {
        val sign = if (amount >= 0) "+" else "-"
        return "$sign$currency ${String.format("%.2f", abs(amount))}"
    }
    
    fun formatCompact(amount: Double, currency: String = "SAR"): String {
        return when {
            abs(amount) >= 1_000_000 -> "$currency ${String.format("%.1fM", amount / 1_000_000)}"
            abs(amount) >= 1_000 -> "$currency ${String.format("%.1fK", amount / 1_000)}"
            else -> format(amount, currency)
        }
    }
    
    /**
     * Format amount input (remove invalid characters, limit decimal places)
     */
    fun formatAmountInput(input: String): String {
        // Remove all non-digit and non-decimal characters
        val cleaned = input.filter { it.isDigit() || it == '.' }
        
        // Ensure only one decimal point
        val parts = cleaned.split(".")
        return if (parts.size > 2) {
            "${parts[0]}.${parts.drop(1).joinToString("")}"
        } else if (parts.size == 2) {
            // Limit to 2 decimal places
            val decimal = parts[1].take(2)
            "${parts[0]}.$decimal"
        } else {
            cleaned
        }
    }
    
    /**
     * Format amount for display
     */
    fun formatDisplayAmount(amount: String): String {
        if (amount.isEmpty()) return "0.00"
        
        val number = amount.toDoubleOrNull() ?: return amount
        
        return String.format("%.2f", number)
    }
}

/**
 * Common color utilities for financial data
 */
object FinancialColors {
    @Composable
    fun getAmountColor(amount: Double): Color {
        return when {
            amount > 0 -> MaterialTheme.colorScheme.primary // Positive/Income
            amount < 0 -> MaterialTheme.colorScheme.error   // Negative/Expense
            else -> MaterialTheme.colorScheme.onSurface     // Zero
        }
    }
    
    @Composable
    fun getBudgetProgressColor(percentage: Double, alertThreshold: Double = 80.0): Color {
        return when {
            percentage >= 100 -> MaterialTheme.colorScheme.error
            percentage >= alertThreshold -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
    }
    
    @Composable
    fun getTransactionTypeColor(type: TransactionType): Color {
        return when (type) {
            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
            TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
        }
    }
}

/**
 * Common text style utilities
 */
object FinancialTextStyles {
    @Composable
    fun currencyLarge(): TextStyle {
        return MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold
        )
    }
    
    @Composable
    fun currencyMedium(): TextStyle {
        return MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Medium
        )
    }
    
    @Composable
    fun currencySmall(): TextStyle {
        return MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium
        )
    }
    
    @Composable
    fun percentage(): TextStyle {
        return MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Common validation utilities
 */
object ValidationUtils {
    fun isValidAmount(amount: String): Boolean {
        if (amount.isBlank()) return false
        val numericValue = amount.toDoubleOrNull()
        return numericValue != null && numericValue > 0
    }
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && email.contains("@") && email.contains(".")
    }
    
    fun isValidPhoneNumber(phone: String): Boolean {
        // Basic Saudi phone number validation
        val cleaned = phone.replace(Regex("[^\\d]"), "")
        return cleaned.length >= 9 && (cleaned.startsWith("05") || cleaned.startsWith("9665"))
    }
    
    fun getAmountError(amount: String): String? {
        return when {
            amount.isBlank() -> "Amount is required"
            amount.toDoubleOrNull() == null -> "Invalid amount format"
            amount.toDoubleOrNull()!! <= 0 -> "Amount must be greater than 0"
            else -> null
        }
    }
}

/**
 * Common state management patterns
 */
@Composable
fun <T> rememberMutableState(initial: T): MutableState<T> {
    return remember { mutableStateOf(initial) }
}

@Composable
fun <T> rememberMutableStateOf(value: T): MutableState<T> {
    return remember(value) { mutableStateOf(value) }
}

/**
 * Common progress calculation utilities
 */
object ProgressUtils {
    fun calculatePercentage(current: Double, target: Double): Double {
        if (target <= 0) return 0.0
        return (current / target * 100).coerceIn(0.0, 100.0)
    }
    
    fun calculateBudgetProgress(spent: Money, budget: Money): Double {
        if (budget.amount <= 0) return 0.0
        return (spent.amount / budget.amount * 100).coerceIn(0.0, 100.0)
    }
    
    fun calculateGoalProgress(current: Money, target: Money): Double {
        if (target.amount <= 0) return 0.0
        return (current.amount / target.amount * 100).coerceIn(0.0, 100.0)
    }
}