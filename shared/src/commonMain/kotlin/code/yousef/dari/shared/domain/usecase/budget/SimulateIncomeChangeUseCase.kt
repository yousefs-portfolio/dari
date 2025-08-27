package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*

class SimulateIncomeChangeUseCase {
    suspend operator fun invoke(
        baselineScenario: BudgetScenario,
        newIncome: Money
    ): Result<BudgetScenario> {
        return try {
            // Assume current income is projected savings + total spent
            val currentIncome = baselineScenario.projectedSavings.amount + baselineScenario.totalSpent.amount
            val incomeChange = newIncome.amount - currentIncome
            
            val changeType = if (incomeChange > 0) IncomeChangeType.INCREASE else IncomeChangeType.DECREASE
            
            val incomeAdjustment = IncomeAdjustment(
                currentIncome = Money.fromDouble(currentIncome, baselineScenario.totalBudget.currency),
                newIncome = newIncome,
                changeType = changeType
            )
            
            // New projected savings = old savings + income change
            val newProjectedSavings = baselineScenario.projectedSavings.amount + incomeChange
            
            val scenarioName = if (changeType == IncomeChangeType.INCREASE) {
                "Income Increase (+${incomeChange.toInt()} SAR)"
            } else {
                "Income Decrease (${incomeChange.toInt()} SAR)"
            }
            
            val newScenario = BudgetScenario(
                id = "income_change_${System.currentTimeMillis()}",
                name = scenarioName,
                type = ScenarioType.INCOME_CHANGE,
                totalBudget = baselineScenario.totalBudget,
                totalSpent = baselineScenario.totalSpent,
                categoryBreakdown = baselineScenario.categoryBreakdown,
                projectedSavings = Money.fromDouble(newProjectedSavings, baselineScenario.projectedSavings.currency),
                cashFlowImpact = Money.fromDouble(incomeChange, baselineScenario.totalBudget.currency),
                modifications = listOf(incomeAdjustment),
                description = "Changing monthly income by ${incomeChange.toInt()} SAR affects your savings capacity"
            )
            
            Result.success(newScenario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}