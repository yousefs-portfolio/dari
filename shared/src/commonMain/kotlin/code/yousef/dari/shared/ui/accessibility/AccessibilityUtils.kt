package code.yousef.dari.shared.ui.accessibility

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for the Dari app
 */
object AccessibilityConfig {
    
    // Minimum touch target size as per accessibility guidelines
    val MIN_TOUCH_TARGET_SIZE = 48.dp
    
    // Recommended touch target size for better usability
    val RECOMMENDED_TOUCH_TARGET_SIZE = 56.dp
}

/**
 * Ensures minimum touch target size for accessibility
 */
fun Modifier.minimumTouchTarget(): Modifier = this.size(AccessibilityConfig.MIN_TOUCH_TARGET_SIZE)

/**
 * Ensures recommended touch target size for better usability
 */
fun Modifier.recommendedTouchTarget(): Modifier = this.size(AccessibilityConfig.RECOMMENDED_TOUCH_TARGET_SIZE)

/**
 * Adds content description for screen readers
 */
fun Modifier.accessibilityDescription(description: String): Modifier = this.semantics {
    contentDescription = description
}

/**
 * Creates accessible interaction source with proper state management
 */
@Composable
fun rememberAccessibleInteractionSource(): MutableInteractionSource {
    return remember { MutableInteractionSource() }
}

/**
 * Financial-specific accessibility descriptions
 */
object FinancialAccessibility {
    
    fun currencyDescription(amount: Double, currency: String = "SAR"): String {
        return when {
            amount > 0 -> "Income of $amount $currency"
            amount < 0 -> "Expense of ${kotlin.math.abs(amount)} $currency"
            else -> "Zero balance"
        }
    }
    
    fun percentageDescription(percentage: Double): String {
        return "$percentage percent"
    }
    
    fun budgetStatusDescription(spent: Double, total: Double): String {
        val remaining = total - spent
        val percentageUsed = (spent / total * 100).toInt()
        
        return when {
            spent > total -> "Budget exceeded by ${spent - total} SAR, $percentageUsed percent used"
            remaining < total * 0.1 -> "Budget almost reached, $remaining SAR remaining, $percentageUsed percent used"
            else -> "Budget on track, $remaining SAR remaining, $percentageUsed percent used"
        }
    }
    
    fun goalProgressDescription(current: Double, target: Double): String {
        val percentage = (current / target * 100).toInt()
        val remaining = target - current
        
        return "Goal progress: $current of $target SAR completed, $percentage percent achieved, $remaining SAR remaining"
    }
}