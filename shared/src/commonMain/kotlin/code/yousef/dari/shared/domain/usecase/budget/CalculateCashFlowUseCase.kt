package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*

class CalculateCashFlowUseCase {
    suspend operator fun invoke(
        scenario: BudgetScenario,
        monthsToProject: Int
    ): Result<List<CashFlowProjection>> {
        return try {
            val projections = mutableListOf<CashFlowProjection>()
            val currentIncome = scenario.projectedSavings.amount + scenario.totalSpent.amount
            val monthlyExpenses = scenario.totalSpent.amount
            val monthlyNetCashFlow = currentIncome - monthlyExpenses
            
            var cumulativeSavings = 0.0
            val monthlyExpensesForEmergency = monthlyExpenses
            
            for (month in 1..monthsToProject) {
                cumulativeSavings += monthlyNetCashFlow
                val emergencyFundMonths = if (monthlyExpensesForEmergency > 0) {
                    cumulativeSavings / monthlyExpensesForEmergency
                } else {
                    0.0
                }
                
                projections.add(
                    CashFlowProjection(
                        month = month,
                        income = Money.fromDouble(currentIncome, scenario.totalBudget.currency),
                        expenses = Money.fromDouble(monthlyExpenses, scenario.totalBudget.currency),
                        netCashFlow = Money.fromDouble(monthlyNetCashFlow, scenario.totalBudget.currency),
                        cumulativeSavings = Money.fromDouble(cumulativeSavings, scenario.totalBudget.currency),
                        emergencyFundMonths = emergencyFundMonths
                    )
                )
            }
            
            Result.success(projections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}