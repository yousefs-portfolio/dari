package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.repository.BudgetRepository
import kotlin.math.abs

/**
 * Use case for comparing two budgets to analyze differences in allocation and spending
 */
class CompareBudgetsUseCase(
    private val budgetRepository: BudgetRepository
) {

    /**
     * Compare two budgets and return detailed comparison analysis
     * 
     * @param budget1Id ID of the first budget to compare
     * @param budget2Id ID of the second budget to compare
     * @return Result containing budget comparison data
     */
    suspend operator fun invoke(
        budget1Id: String,
        budget2Id: String
    ): Result<BudgetComparison> = runCatching {
        val budget1 = budgetRepository.getBudget(budget1Id)
        val budget2 = budgetRepository.getBudget(budget2Id)
        
        if (budget1 == null || budget2 == null) {
            throw IllegalArgumentException("One or both budgets not found")
        }
        
        compareBudgets(budget1, budget2)
    }

    private fun compareBudgets(budget1: Budget, budget2: Budget): BudgetComparison {
        // Calculate total budgets
        val budget1Total = budget1.categories.values.fold(Money(0.0, "SAR")) { acc, amount ->
            Money(acc.amount + amount.amount, acc.currency)
        }
        
        val budget2Total = budget2.categories.values.fold(Money(0.0, "SAR")) { acc, amount ->
            Money(acc.amount + amount.amount, acc.currency)
        }
        
        // Calculate total spending
        val budget1Spending = budget1.spent.values.fold(Money(0.0, "SAR")) { acc, amount ->
            Money(acc.amount + amount.amount, acc.currency)
        }
        
        val budget2Spending = budget2.spent.values.fold(Money(0.0, "SAR")) { acc, amount ->
            Money(acc.amount + amount.amount, acc.currency)
        }
        
        // Calculate differences
        val budgetDifference = Money(
            budget2Total.amount - budget1Total.amount,
            budget1Total.currency
        )
        
        val spendingDifference = Money(
            budget2Spending.amount - budget1Spending.amount,
            budget1Spending.currency
        )
        
        // Calculate percentage changes
        val budgetChangePercentage = if (budget1Total.amount == 0.0) {
            0.0
        } else {
            (budgetDifference.amount / budget1Total.amount) * 100
        }
        
        val spendingChangePercentage = if (budget1Spending.amount == 0.0) {
            0.0
        } else {
            (spendingDifference.amount / budget1Spending.amount) * 100
        }
        
        // Get all unique category IDs from both budgets
        val allCategoryIds = (budget1.categories.keys + budget2.categories.keys).distinct()
        
        // Compare each category
        val categoryComparisons = allCategoryIds.map { categoryId ->
            compareBudgetCategory(budget1, budget2, categoryId)
        }
        
        return BudgetComparison(
            budget1 = budget1,
            budget2 = budget2,
            budget1Total = budget1Total,
            budget2Total = budget2Total,
            budgetDifference = budgetDifference,
            budgetChangePercentage = budgetChangePercentage,
            budget1Spending = budget1Spending,
            budget2Spending = budget2Spending,
            spendingDifference = spendingDifference,
            spendingChangePercentage = spendingChangePercentage,
            categoryComparisons = categoryComparisons
        )
    }
    
    private fun compareBudgetCategory(
        budget1: Budget,
        budget2: Budget,
        categoryId: String
    ): CategoryComparison {
        val budget1Amount = budget1.categories[categoryId] ?: Money(0.0, "SAR")
        val budget2Amount = budget2.categories[categoryId] ?: Money(0.0, "SAR")
        val budget1Spent = budget1.spent[categoryId] ?: Money(0.0, "SAR")
        val budget2Spent = budget2.spent[categoryId] ?: Money(0.0, "SAR")
        
        val budgetDifference = Money(
            budget2Amount.amount - budget1Amount.amount,
            budget1Amount.currency
        )
        
        val spendingDifference = Money(
            budget2Spent.amount - budget1Spent.amount,
            budget1Spent.currency
        )
        
        val budgetChangePercentage = if (budget1Amount.amount == 0.0) {
            0.0
        } else {
            (budgetDifference.amount / budget1Amount.amount) * 100
        }
        
        val spendingChangePercentage = if (budget1Spent.amount == 0.0) {
            0.0
        } else {
            (spendingDifference.amount / budget1Spent.amount) * 100
        }
        
        return CategoryComparison(
            categoryId = categoryId,
            budget1Amount = budget1Amount,
            budget2Amount = budget2Amount,
            budgetDifference = budgetDifference,
            budgetChangePercentage = budgetChangePercentage,
            budget1Spent = budget1Spent,
            budget2Spent = budget2Spent,
            spendingDifference = spendingDifference,
            spendingChangePercentage = spendingChangePercentage
        )
    }
}

/**
 * Data class representing the comparison between two budgets
 */
data class BudgetComparison(
    val budget1: Budget,
    val budget2: Budget,
    val budget1Total: Money,
    val budget2Total: Money,
    val budgetDifference: Money,
    val budgetChangePercentage: Double,
    val budget1Spending: Money,
    val budget2Spending: Money,
    val spendingDifference: Money,
    val spendingChangePercentage: Double,
    val categoryComparisons: List<CategoryComparison>
)

/**
 * Data class representing the comparison of a specific category between two budgets
 */
data class CategoryComparison(
    val categoryId: String,
    val budget1Amount: Money,
    val budget2Amount: Money,
    val budgetDifference: Money,
    val budgetChangePercentage: Double,
    val budget1Spent: Money,
    val budget2Spent: Money,
    val spendingDifference: Money,
    val spendingChangePercentage: Double
)