package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*

/**
 * Use case for simulating the addition of a new expense to the budget
 */
class SimulateExpenseAdditionUseCase {
    
    suspend operator fun invoke(
        baselineScenario: BudgetScenario,
        expenseAddition: ExpenseAddition
    ): Result<BudgetScenario> {
        return try {
            // Calculate monthly impact of the new expense
            val monthlyImpact = expenseAddition.amount.amount * expenseAddition.frequency.toMonthlyMultiplier()
            
            // Create updated category breakdown
            val updatedCategoryBreakdown = baselineScenario.categoryBreakdown.toMutableMap()
            val currentCategoryAmount = updatedCategoryBreakdown[expenseAddition.category]?.amount ?: 0.0
            updatedCategoryBreakdown[expenseAddition.category] = Money.fromDouble(
                currentCategoryAmount + monthlyImpact,
                baselineScenario.totalBudget.currency
            )
            
            // Calculate new totals
            val newTotalSpent = baselineScenario.totalSpent.amount + monthlyImpact
            val newTotalBudget = baselineScenario.totalBudget.amount + monthlyImpact
            val newProjectedSavings = maxOf(0.0, newTotalBudget - newTotalSpent)
            
            val newScenario = BudgetScenario(
                id = "expense_addition_${System.currentTimeMillis()}",
                name = "Add ${expenseAddition.name}",
                type = ScenarioType.EXPENSE_ADDITION,
                totalBudget = Money.fromDouble(newTotalBudget, baselineScenario.totalBudget.currency),
                totalSpent = Money.fromDouble(newTotalSpent, baselineScenario.totalSpent.currency),
                categoryBreakdown = updatedCategoryBreakdown,
                projectedSavings = Money.fromDouble(newProjectedSavings, baselineScenario.projectedSavings.currency),
                cashFlowImpact = Money.fromDouble(-monthlyImpact, baselineScenario.totalBudget.currency),
                modifications = listOf(expenseAddition),
                description = "Adding ${expenseAddition.name} (${expenseAddition.frequency.name.lowercase()}) " +
                             "will cost ${monthlyImpact.toInt()} SAR per month"
            )
            
            Result.success(newScenario)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}