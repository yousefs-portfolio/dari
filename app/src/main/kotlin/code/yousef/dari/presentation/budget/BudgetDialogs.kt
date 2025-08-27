package code.yousef.dari.presentation.budget

import androidx.compose.runtime.Composable
import code.yousef.dari.presentation.budget.dialogs.*
import code.yousef.dari.shared.domain.models.*

/**
 * Consolidated budget dialogs - Refactored for better maintainability
 * All dialog components are now extracted into separate files for better organization
 */

// Re-export all dialog functions for backward compatibility
@Composable
fun CreateBudgetDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Money, BudgetPeriod, Double) -> Unit
) = CreateBudgetDialog(onDismiss = onDismiss, onCreate = onCreate)

@Composable
fun EditBudgetDialog(
    budget: Budget,
    onDismiss: () -> Unit,
    onUpdate: (Budget) -> Unit
) = EditBudgetDialog(budget = budget, onDismiss = onDismiss, onUpdate = onUpdate)

@Composable
fun BudgetDetailsDialog(
    budget: Budget,
    recentTransactions: List<Transaction>,
    onDismiss: () -> Unit,
    onEditBudget: () -> Unit,
    onViewAllTransactions: () -> Unit
) = BudgetDetailsDialog(
    budget = budget,
    recentTransactions = recentTransactions,
    onDismiss = onDismiss,
    onEditBudget = onEditBudget,
    onViewAllTransactions = onViewAllTransactions
)

/**
 * All budget-related dialogs in one place for easy access
 */
object BudgetDialogs {
    @Composable
    fun CreateBudget(
        onDismiss: () -> Unit,
        onCreate: (String, String, Money, BudgetPeriod, Double) -> Unit
    ) = CreateBudgetDialog(onDismiss = onDismiss, onCreate = onCreate)
    
    @Composable
    fun EditBudget(
        budget: Budget,
        onDismiss: () -> Unit,
        onUpdate: (Budget) -> Unit
    ) = EditBudgetDialog(budget = budget, onDismiss = onDismiss, onUpdate = onUpdate)
    
    @Composable
    fun BudgetDetails(
        budget: Budget,
        recentTransactions: List<Transaction>,
        onDismiss: () -> Unit,
        onEditBudget: () -> Unit,
        onViewAllTransactions: () -> Unit
    ) = BudgetDetailsDialog(
        budget = budget,
        recentTransactions = recentTransactions,
        onDismiss = onDismiss,
        onEditBudget = onEditBudget,
        onViewAllTransactions = onViewAllTransactions
    )
}