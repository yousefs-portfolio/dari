package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*

/**
 * Use case for forecasting monthly spending based on historical data and current budgets
 */
class ForecastMonthlySpendingUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    
    suspend operator fun invoke(): Result<BudgetScenario> {
        return try {
            // Get current active budgets
            val budgetsResult = budgetRepository.getActiveBudgets()
            if (budgetsResult.isFailure) {
                return Result.failure(budgetsResult.exceptionOrNull() ?: Exception("Failed to get budgets"))
            }
            val budgets = budgetsResult.getOrThrow()
            
            // Get historical spending data (last 90 days)
            val transactions = transactionRepository.getRecentTransactionsByDays(90)
            
            // Calculate baseline scenario
            val totalBudget = budgets.sumOf { it.amount.amount }
            val categoryBreakdown = calculateCategoryBreakdown(budgets, transactions)
            val totalSpent = categoryBreakdown.values.sumOf { it.amount }
            
            val projectedSavings = maxOf(0.0, totalBudget - totalSpent)
            
            val baselineScenario = BudgetScenario(
                id = "baseline",
                name = "Current Spending Pattern",
                type = ScenarioType.BASELINE,
                totalBudget = Money.fromDouble(totalBudget, "SAR"),
                totalSpent = Money.fromDouble(totalSpent, "SAR"),
                categoryBreakdown = categoryBreakdown,
                projectedSavings = Money.fromDouble(projectedSavings, "SAR"),
                cashFlowImpact = Money.fromDouble(0.0, "SAR"),
                modifications = emptyList(),
                description = "Based on your current budgets and spending patterns from the last 3 months"
            )
            
            Result.success(baselineScenario)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun calculateCategoryBreakdown(
        budgets: List<Budget>,
        transactions: List<Transaction>
    ): Map<String, Money> {
        val categorySpending = mutableMapOf<String, Double>()
        
        // Initialize with budget categories
        budgets.forEach { budget ->
            categorySpending[budget.category] = 0.0
        }
        
        // Calculate actual spending by category (average over last 3 months)
        val monthlySpending = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, categoryTransactions) ->
                categoryTransactions.sumOf { it.amount.amount } / 3.0 // Average over 3 months
            }
        
        // Combine budget categories with actual spending
        budgets.forEach { budget ->
            val actualSpending = monthlySpending[budget.category] ?: 0.0
            
            // Use the lower of budget amount or actual spending for conservative estimate
            categorySpending[budget.category] = minOf(budget.amount.amount, actualSpending)
        }
        
        return categorySpending.mapValues { (_, amount) ->
            Money.fromDouble(amount, "SAR")
        }
    }
}