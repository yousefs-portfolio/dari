package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.domain.models.Budget
import code.yousef.dari.shared.domain.models.BudgetPeriod
import code.yousef.dari.shared.domain.models.BudgetPerformanceSummary
import code.yousef.dari.shared.domain.models.BudgetType
import code.yousef.dari.shared.domain.models.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for Budget data operations
 * Handles CRUD operations and budget-specific queries
 */
interface BudgetRepository {
    
    /**
     * Create a new budget
     */
    suspend fun createBudget(budget: Budget): Result<Budget>
    
    /**
     * Update an existing budget
     */
    suspend fun updateBudget(budget: Budget): Result<Budget>
    
    /**
     * Delete a budget
     */
    suspend fun deleteBudget(budgetId: String): Result<Unit>
    
    /**
     * Get budget by ID
     */
    suspend fun getBudgetById(budgetId: String): Result<Budget?>
    
    /**
     * Get all active budgets
     */
    suspend fun getActiveBudgets(): Result<List<Budget>>
    
    /**
     * Get all budgets (active and inactive)
     */
    suspend fun getAllBudgets(): Result<List<Budget>>
    
    /**
     * Get budgets by type
     */
    suspend fun getBudgetsByType(type: BudgetType): Result<List<Budget>>
    
    /**
     * Get budgets for a date range
     */
    suspend fun getBudgetsForDateRange(
        startDate: Instant,
        endDate: Instant
    ): Result<List<Budget>>
    
    /**
     * Get current active budget (most recent)
     */
    suspend fun getCurrentBudget(): Result<Budget?>
    
    /**
     * Get budget performance summaries for dashboard
     */
    suspend fun getBudgetPerformanceSummaries(): Result<List<BudgetPerformanceSummary>>
    
    /**
     * Get overspent budgets
     */
    suspend fun getOverspentBudgets(): Result<List<Budget>>
    
    /**
     * Get budgets approaching limits (within threshold percentage)
     */
    suspend fun getBudgetsNearLimit(thresholdPercentage: Double = 80.0): Result<List<Budget>>
    
    /**
     * Update budget spent amounts based on transactions
     */
    suspend fun updateBudgetSpending(
        budgetId: String,
        categorySpending: Map<String, Money>
    ): Result<Unit>
    
    /**
     * Get budget by category ID
     */
    suspend fun getBudgetByCategory(categoryId: String): Result<Budget?>
    
    /**
     * Search budgets by name
     */
    suspend fun searchBudgets(query: String): Result<List<Budget>>
    
    /**
     * Get budget history (past budgets)
     */
    suspend fun getBudgetHistory(limit: Int = 10): Result<List<Budget>>
    
    /**
     * Check if budget name already exists
     */
    suspend fun isBudgetNameExists(name: String, excludeBudgetId: String? = null): Result<Boolean>
    
    /**
     * Get total budget amount for period
     */
    suspend fun getTotalBudgetAmount(
        startDate: Instant,
        endDate: Instant
    ): Result<Money?>
    
    /**
     * Get total spent amount across all budgets for period
     */
    suspend fun getTotalSpentAmount(
        startDate: Instant,
        endDate: Instant
    ): Result<Money?>
    
    /**
     * Get budgets as Flow for reactive updates
     */
    fun getBudgets(): Flow<List<Budget>>
    
    /**
     * Get budgets by period
     */
    fun getBudgetsByPeriod(period: BudgetPeriod): Flow<List<Budget>>
}