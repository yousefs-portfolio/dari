package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*

class SimulateExpenseRemovalUseCase {
    suspend operator fun invoke(
        baselineScenario: BudgetScenario,
        category: String,
        reductionAmount: Money
    ): Result<BudgetScenario> {
        return try {
            val updatedCategoryBreakdown = baselineScenario.categoryBreakdown.toMutableMap()
            val currentAmount = updatedCategoryBreakdown[category]?.amount ?: 0.0
            val newAmount = maxOf(0.0, currentAmount - reductionAmount.amount)
            
            updatedCategoryBreakdown[category] = Money.fromDouble(newAmount, baselineScenario.totalBudget.currency)
            
            val newTotalSpent = baselineScenario.totalSpent.amount - reductionAmount.amount
            val newProjectedSavings = baselineScenario.projectedSavings.amount + reductionAmount.amount
            
            val expenseRemoval = ExpenseRemoval(
                category = category,
                amount = reductionAmount,
                name = "Reduce $category by ${reductionAmount.amount.toInt()} SAR"
            )
            
            val newScenario = BudgetScenario(
                id = "expense_removal_${System.currentTimeMillis()}",
                name = "Reduce $category",
                type = ScenarioType.EXPENSE_REMOVAL,
                totalBudget = baselineScenario.totalBudget,
                totalSpent = Money.fromDouble(newTotalSpent, baselineScenario.totalSpent.currency),
                categoryBreakdown = updatedCategoryBreakdown,
                projectedSavings = Money.fromDouble(newProjectedSavings, baselineScenario.projectedSavings.currency),
                cashFlowImpact = Money.fromDouble(reductionAmount.amount, baselineScenario.totalBudget.currency),
                modifications = listOf(expenseRemoval),
                description = "Reducing $category spending by ${reductionAmount.amount.toInt()} SAR per month"
            )
            
            Result.success(newScenario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}