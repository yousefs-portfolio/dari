package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.domain.models.Budget
import code.yousef.dari.shared.domain.models.Money

/**
 * Update Budget Use Case
 * Handles budget updates and spending tracking
 */
class UpdateBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    
    /**
     * Update budget details
     */
    suspend fun updateBudget(budget: Budget): Result<Budget> {
        return try {
            budgetRepository.updateBudget(budget)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update budget spending amounts
     */
    suspend fun updateBudgetSpending(
        budgetId: String,
        categorySpending: Map<String, Money>
    ): Result<Unit> {
        return try {
            budgetRepository.updateBudgetSpending(budgetId, categorySpending)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add spending to budget category
     */
    suspend fun addSpending(
        budgetId: String,
        categoryId: String,
        amount: Money
    ): Result<Unit> {
        return try {
            // Get current budget
            val budgetResult = budgetRepository.getBudgetById(budgetId)
            val budget = budgetResult.getOrNull() 
                ?: return Result.failure(Exception("Budget not found"))
            
            // Update spending for the category
            val currentSpending = budget.categories
                .find { it.categoryId == categoryId }
                ?.spentAmount ?: Money.fromInt(0, amount.currency)
            
            val newSpending = currentSpending + amount
            val categorySpending = mapOf(categoryId to newSpending)
            
            budgetRepository.updateBudgetSpending(budgetId, categorySpending)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Recalculate budget spending from transactions
     */
    suspend fun recalculateBudgetSpending(budgetId: String): Result<Unit> {
        return try {
            // This would typically query transactions in the budget period
            // and recalculate spending by category
            
            // For now, return success - would implement with transaction queries
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check budget status and update alerts
     */
    suspend fun checkBudgetStatus(budgetId: String): Result<BudgetStatus> {
        return try {
            val budgetResult = budgetRepository.getBudgetById(budgetId)
            val budget = budgetResult.getOrNull()
                ?: return Result.failure(Exception("Budget not found"))
            
            val spentPercentage = if (budget.totalAmount.toDouble() > 0) {
                (budget.spentAmount.toDouble() / budget.totalAmount.toDouble()) * 100.0
            } else 0.0
            
            val status = when {
                spentPercentage >= 100.0 -> BudgetStatus.OVER_BUDGET
                spentPercentage >= budget.alertThreshold -> BudgetStatus.NEAR_LIMIT
                else -> BudgetStatus.ON_TRACK
            }
            
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Budget status enumeration
 */
enum class BudgetStatus {
    ON_TRACK,
    NEAR_LIMIT,
    OVER_BUDGET
}