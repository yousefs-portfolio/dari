package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.repository.BudgetRepository
import kotlinx.datetime.LocalDateTime
import kotlin.math.max

/**
 * Use case for simulating the impact of removing an expense from a budget
 */
class SimulateExpenseRemovalUseCase(
    private val budgetRepository: BudgetRepository
) {

    /**
     * Simulate removing an expense from a budget and analyze the impact
     * 
     * @param budgetId ID of the budget to modify
     * @param expenseRemoval Details of the expense to remove
     * @return Result containing simulation results
     */
    suspend operator fun invoke(
        budgetId: String,
        expenseRemoval: ExpenseRemoval
    ): Result<ExpenseRemovalSimulation> = runCatching {
        val budget = budgetRepository.getBudget(budgetId)
            ?: throw IllegalArgumentException("Budget not found: $budgetId")
        
        simulateExpenseRemoval(budget, expenseRemoval)
    }

    private fun simulateExpenseRemoval(
        budget: Budget,
        expenseRemoval: ExpenseRemoval
    ): ExpenseRemovalSimulation {
        // Calculate original totals
        val originalTotalBudget = budget.categories.values.fold(Money(0.0, "SAR")) { acc, money ->
            Money(acc.amount + money.amount, acc.currency)
        }
        
        val originalSpending = budget.spent.values.fold(Money(0.0, "SAR")) { acc, money ->
            Money(acc.amount + money.amount, acc.currency)
        }
        
        // Calculate monthly expense reduction
        val monthlyExpenseReduction = calculateMonthlyExpenseReduction(expenseRemoval)
        
        // Calculate prorated savings for current budget period
        val proratedSavings = calculateProratedSavings(
            expenseRemoval,
            budget.startDate,
            budget.endDate
        )
        
        // Calculate new totals
        val budgetDecrease = when (expenseRemoval.frequency) {
            ExpenseRemoval.Frequency.ONE_TIME -> Money(0.0, "SAR") // One-time doesn't affect future budget allocation
            else -> monthlyExpenseReduction
        }
        
        val newTotalBudget = Money(
            (originalTotalBudget.amount - budgetDecrease.amount).coerceAtLeast(0.0),
            originalTotalBudget.currency
        )
        
        val newSpending = Money(
            (originalSpending.amount - proratedSavings.amount).coerceAtLeast(0.0),
            originalSpending.currency
        )
        
        // Calculate percentages
        val budgetDecreasePercentage = if (originalTotalBudget.amount > 0) {
            (budgetDecrease.amount / originalTotalBudget.amount) * 100
        } else 0.0
        
        val spendingDecreasePercentage = if (originalSpending.amount > 0) {
            (proratedSavings.amount / originalSpending.amount) * 100
        } else 0.0
        
        // Analyze category impact
        val categoryImpact = analyzeCategoryImpact(budget, expenseRemoval, proratedSavings)
        
        // Calculate actual savings realized
        val savingsRealized = if (isRemovalEffectiveInCurrentPeriod(expenseRemoval, budget)) {
            proratedSavings
        } else {
            Money(0.0, "SAR") // Future removal
        }
        
        // Generate recommendations
        val recommendations = generateRecommendations(
            budget,
            expenseRemoval,
            monthlyExpenseReduction,
            categoryImpact,
            savingsRealized
        )
        
        return ExpenseRemovalSimulation(
            budgetId = budget.id,
            expenseRemoval = expenseRemoval,
            originalTotalBudget = originalTotalBudget,
            newTotalBudget = newTotalBudget,
            budgetDecrease = budgetDecrease,
            budgetDecreasePercentage = budgetDecreasePercentage,
            originalSpending = originalSpending,
            newSpending = newSpending,
            spendingDecrease = proratedSavings,
            spendingDecreasePercentage = spendingDecreasePercentage,
            savingsRealized = savingsRealized,
            categoryImpact = categoryImpact,
            recommendations = recommendations
        )
    }
    
    private fun calculateMonthlyExpenseReduction(expenseRemoval: ExpenseRemoval): Money {
        return when (expenseRemoval.frequency) {
            ExpenseRemoval.Frequency.MONTHLY -> expenseRemoval.monthlyAmount
            ExpenseRemoval.Frequency.WEEKLY -> {
                // Approximate 4.4 weeks per month
                Money(expenseRemoval.monthlyAmount.amount * 4.4, expenseRemoval.monthlyAmount.currency)
            }
            ExpenseRemoval.Frequency.DAILY -> {
                // Approximate 30.4 days per month
                Money(expenseRemoval.monthlyAmount.amount * 30.4, expenseRemoval.monthlyAmount.currency)
            }
            ExpenseRemoval.Frequency.ONE_TIME -> Money(0.0, expenseRemoval.monthlyAmount.currency)
        }
    }
    
    private fun calculateProratedSavings(
        expenseRemoval: ExpenseRemoval,
        budgetStart: LocalDateTime,
        budgetEnd: LocalDateTime
    ): Money {
        // If removal is in the future, no current savings
        if (expenseRemoval.removalDate > budgetEnd) {
            return Money(0.0, expenseRemoval.monthlyAmount.currency)
        }
        
        val effectiveRemovalDate = maxOf(expenseRemoval.removalDate, budgetStart)
        
        return when (expenseRemoval.frequency) {
            ExpenseRemoval.Frequency.ONE_TIME -> expenseRemoval.monthlyAmount
            ExpenseRemoval.Frequency.MONTHLY -> {
                // Calculate what portion of the month this removal saves
                val totalDays = daysBetween(budgetStart, budgetEnd)
                val savedDays = daysBetween(effectiveRemovalDate, budgetEnd)
                val prorateRatio = savedDays.toDouble() / totalDays.toDouble()
                
                Money(
                    expenseRemoval.monthlyAmount.amount * prorateRatio,
                    expenseRemoval.monthlyAmount.currency
                )
            }
            ExpenseRemoval.Frequency.WEEKLY -> {
                val savedWeeks = daysBetween(effectiveRemovalDate, budgetEnd) / 7.0
                Money(
                    expenseRemoval.monthlyAmount.amount * savedWeeks,
                    expenseRemoval.monthlyAmount.currency
                )
            }
            ExpenseRemoval.Frequency.DAILY -> {
                val savedDays = daysBetween(effectiveRemovalDate, budgetEnd)
                Money(
                    expenseRemoval.monthlyAmount.amount * savedDays,
                    expenseRemoval.monthlyAmount.currency
                )
            }
        }
    }
    
    private fun analyzeCategoryImpact(
        budget: Budget,
        expenseRemoval: ExpenseRemoval,
        proratedSavings: Money
    ): CategoryRemovalImpact {
        val currentBudgetAmount = budget.categories[expenseRemoval.categoryId] ?: Money(0.0, "SAR")
        val currentSpentAmount = budget.spent[expenseRemoval.categoryId] ?: Money(0.0, "SAR")
        val newSpentAmount = Money(
            (currentSpentAmount.amount - proratedSavings.amount).coerceAtLeast(0.0),
            currentSpentAmount.currency
        )
        
        val utilizationBefore = if (currentBudgetAmount.amount > 0) {
            (currentSpentAmount.amount / currentBudgetAmount.amount) * 100
        } else 0.0
        
        val utilizationAfter = if (currentBudgetAmount.amount > 0) {
            (newSpentAmount.amount / currentBudgetAmount.amount) * 100
        } else 0.0
        
        val monthlyReduction = calculateMonthlyExpenseReduction(expenseRemoval)
        val willEliminateCategory = monthlyReduction.amount >= currentBudgetAmount.amount
        
        val surplusCreated = Money(
            currentBudgetAmount.amount - newSpentAmount.amount,
            currentBudgetAmount.currency
        ).takeIf { it.amount > 0 } ?: Money(0.0, "SAR")
        
        return CategoryRemovalImpact(
            categoryId = expenseRemoval.categoryId,
            currentBudgetAmount = currentBudgetAmount,
            currentSpentAmount = currentSpentAmount,
            newSpentAmount = newSpentAmount,
            spendingReduction = proratedSavings,
            utilizationBefore = utilizationBefore,
            utilizationAfter = utilizationAfter,
            willEliminateCategory = willEliminateCategory,
            surplusCreated = surplusCreated
        )
    }
    
    private fun isRemovalEffectiveInCurrentPeriod(
        expenseRemoval: ExpenseRemoval,
        budget: Budget
    ): Boolean {
        return expenseRemoval.removalDate <= budget.endDate
    }
    
    private fun generateRecommendations(
        budget: Budget,
        expenseRemoval: ExpenseRemoval,
        monthlyReduction: Money,
        categoryImpact: CategoryRemovalImpact,
        savingsRealized: Money
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Primary savings message
        when (expenseRemoval.frequency) {
            ExpenseRemoval.Frequency.ONE_TIME -> {
                if (savingsRealized.amount > 0) {
                    recommendations.add(
                        "Removing this one-time expense saves ${savingsRealized.amount.let { "%.2f".format(it) }} SAR immediately"
                    )
                } else {
                    recommendations.add("This one-time expense removal will not affect the current budget period")
                }
            }
            else -> {
                recommendations.add(
                    "Removing this expense will save ${monthlyReduction.amount.let { "%.2f".format(it) }} SAR per month"
                )
                
                if (savingsRealized.amount > 0) {
                    recommendations.add(
                        "For the current period, you'll save ${savingsRealized.amount.let { "%.2f".format(it) }} SAR"
                    )
                }
            }
        }
        
        // Category-specific recommendations
        if (categoryImpact.willEliminateCategory) {
            recommendations.add(
                "This removal will eliminate the '${expenseRemoval.categoryId}' category entirely. Consider reallocating the budget to other categories."
            )
        } else if (categoryImpact.surplusCreated.amount > 50) {
            recommendations.add(
                "This creates a surplus of ${categoryImpact.surplusCreated.amount.let { "%.2f".format(it) }} SAR in the '${expenseRemoval.categoryId}' category"
            )
        }
        
        // Future vs current removal
        if (expenseRemoval.removalDate > LocalDateTime(2024, 12, 31, 0, 0)) { // Future date check
            recommendations.add("This is a future removal starting from ${expenseRemoval.removalDate.date}")
        }
        
        // Utilization insights
        if (categoryImpact.utilizationAfter < 50 && categoryImpact.surplusCreated.amount > 0) {
            recommendations.add(
                "The '${expenseRemoval.categoryId}' category will be under-utilized. Consider reducing the budget allocation or reallocating funds."
            )
        }
        
        // Savings opportunities
        val annualSavings = monthlyReduction.amount * 12
        if (annualSavings >= 1000) {
            recommendations.add(
                "This change will save ${annualSavings.let { "%.0f".format(it) }} SAR annually - consider redirecting these funds to your savings goals"
            )
        }
        
        // Reallocation suggestions
        val underUtilizedCategories = budget.categories.entries.filter { (categoryId, budgetAmount) ->
            val spent = budget.spent[categoryId] ?: Money(0.0, "SAR")
            val utilization = if (budgetAmount.amount > 0) spent.amount / budgetAmount.amount else 0.0
            utilization < 0.3 && categoryId != expenseRemoval.categoryId
        }
        
        if (underUtilizedCategories.isNotEmpty() && categoryImpact.surplusCreated.amount > 0) {
            val suggestedCategory = underUtilizedCategories.first().key
            recommendations.add(
                "Consider reallocating savings to the '$suggestedCategory' category which appears to need more budget"
            )
        }
        
        return recommendations
    }
    
    private fun daysBetween(start: LocalDateTime, end: LocalDateTime): Int {
        // Simplified calculation - in real implementation, use proper date library
        val startDay = start.dayOfYear + (start.year * 365)
        val endDay = end.dayOfYear + (end.year * 365)
        return maxOf(0, endDay - startDay)
    }
    
    private fun maxOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a.year > b.year || (a.year == b.year && a.monthNumber > b.monthNumber) ||
            (a.year == b.year && a.monthNumber == b.monthNumber && a.dayOfMonth > b.dayOfMonth)) a else b
    }
}

/**
 * Data class representing an expense to be removed from a budget
 */
data class ExpenseRemoval(
    val categoryId: String,
    val monthlyAmount: Money,
    val description: String,
    val frequency: Frequency,
    val removalDate: LocalDateTime,
    val wasRecurring: Boolean
) {
    enum class Frequency {
        ONE_TIME,
        DAILY,
        WEEKLY,
        MONTHLY
    }
}

/**
 * Data class representing the simulation results of removing an expense
 */
data class ExpenseRemovalSimulation(
    val budgetId: String,
    val expenseRemoval: ExpenseRemoval,
    val originalTotalBudget: Money,
    val newTotalBudget: Money,
    val budgetDecrease: Money,
    val budgetDecreasePercentage: Double,
    val originalSpending: Money,
    val newSpending: Money,
    val spendingDecrease: Money,
    val spendingDecreasePercentage: Double,
    val savingsRealized: Money,
    val categoryImpact: CategoryRemovalImpact,
    val recommendations: List<String>
)

/**
 * Data class representing the impact on a specific category when removing an expense
 */
data class CategoryRemovalImpact(
    val categoryId: String,
    val currentBudgetAmount: Money,
    val currentSpentAmount: Money,
    val newSpentAmount: Money,
    val spendingReduction: Money,
    val utilizationBefore: Double, // Percentage
    val utilizationAfter: Double, // Percentage
    val willEliminateCategory: Boolean,
    val surplusCreated: Money
)