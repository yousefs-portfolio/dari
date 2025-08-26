package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.repository.BudgetRepository
import kotlinx.datetime.LocalDateTime
import kotlin.math.ceil
import kotlin.math.max

/**
 * Use case for simulating the impact of adding a new expense to a budget
 */
class SimulateExpenseAdditionUseCase(
    private val budgetRepository: BudgetRepository
) {

    /**
     * Simulate adding a new expense to a budget and analyze the impact
     * 
     * @param budgetId ID of the budget to modify
     * @param expenseAddition Details of the expense to add
     * @return Result containing simulation results
     */
    suspend operator fun invoke(
        budgetId: String,
        expenseAddition: ExpenseAddition
    ): Result<ExpenseAdditionSimulation> = runCatching {
        val budget = budgetRepository.getBudget(budgetId)
            ?: throw IllegalArgumentException("Budget not found: $budgetId")
        
        simulateExpenseAddition(budget, expenseAddition)
    }

    private fun simulateExpenseAddition(
        budget: Budget,
        expenseAddition: ExpenseAddition
    ): ExpenseAdditionSimulation {
        // Calculate original totals
        val originalTotalBudget = budget.categories.values.fold(Money(0.0, "SAR")) { acc, money ->
            Money(acc.amount + money.amount, acc.currency)
        }
        
        val originalSpending = budget.spent.values.fold(Money(0.0, "SAR")) { acc, money ->
            Money(acc.amount + money.amount, acc.currency)
        }
        
        // Calculate monthly expense amount
        val monthlyExpenseAmount = calculateMonthlyExpenseAmount(
            expenseAddition, 
            budget.startDate, 
            budget.endDate
        )
        
        // Calculate prorated expense for current budget period
        val proratedExpenseAmount = calculateProratedExpenseAmount(
            expenseAddition,
            budget.startDate,
            budget.endDate
        )
        
        // Calculate new totals
        val budgetIncrease = when (expenseAddition.frequency) {
            ExpenseAddition.Frequency.ONE_TIME -> Money(0.0, "SAR") // One-time doesn't affect budget allocation
            else -> monthlyExpenseAmount
        }
        
        val newTotalBudget = Money(
            originalTotalBudget.amount + budgetIncrease.amount,
            originalTotalBudget.currency
        )
        
        val newSpending = Money(
            originalSpending.amount + proratedExpenseAmount.amount,
            originalSpending.currency
        )
        
        // Calculate percentages
        val budgetIncreasePercentage = if (originalTotalBudget.amount > 0) {
            (budgetIncrease.amount / originalTotalBudget.amount) * 100
        } else 0.0
        
        val spendingIncreasePercentage = if (originalSpending.amount > 0) {
            (proratedExpenseAmount.amount / originalSpending.amount) * 100
        } else 0.0
        
        // Analyze category impact
        val categoryImpact = analyzeCategoryImpact(budget, expenseAddition, proratedExpenseAmount)
        
        // Determine if budget adjustment is needed
        val budgetAdjustmentNeeded = determineBudgetAdjustmentNeeded(
            budget, 
            expenseAddition, 
            proratedExpenseAmount
        )
        
        // Generate recommendations
        val recommendations = generateRecommendations(
            budget,
            expenseAddition,
            monthlyExpenseAmount,
            categoryImpact,
            budgetAdjustmentNeeded
        )
        
        return ExpenseAdditionSimulation(
            budgetId = budget.id,
            expenseAddition = expenseAddition,
            originalTotalBudget = originalTotalBudget,
            newTotalBudget = newTotalBudget,
            budgetIncrease = budgetIncrease,
            budgetIncreasePercentage = budgetIncreasePercentage,
            originalSpending = originalSpending,
            newSpending = newSpending,
            spendingIncrease = proratedExpenseAmount,
            spendingIncreasePercentage = spendingIncreasePercentage,
            categoryImpact = categoryImpact,
            budgetAdjustmentNeeded = budgetAdjustmentNeeded,
            recommendations = recommendations
        )
    }
    
    private fun calculateMonthlyExpenseAmount(
        expenseAddition: ExpenseAddition,
        budgetStart: LocalDateTime,
        budgetEnd: LocalDateTime
    ): Money {
        return when (expenseAddition.frequency) {
            ExpenseAddition.Frequency.MONTHLY -> expenseAddition.monthlyAmount
            ExpenseAddition.Frequency.WEEKLY -> {
                // Approximate 4.4 weeks per month
                Money(expenseAddition.monthlyAmount.amount * 4.4, expenseAddition.monthlyAmount.currency)
            }
            ExpenseAddition.Frequency.DAILY -> {
                // Approximate 30.4 days per month
                Money(expenseAddition.monthlyAmount.amount * 30.4, expenseAddition.monthlyAmount.currency)
            }
            ExpenseAddition.Frequency.ONE_TIME -> Money(0.0, expenseAddition.monthlyAmount.currency)
        }
    }
    
    private fun calculateProratedExpenseAmount(
        expenseAddition: ExpenseAddition,
        budgetStart: LocalDateTime,
        budgetEnd: LocalDateTime
    ): Money {
        val effectiveStartDate = maxOf(expenseAddition.startDate, budgetStart)
        val effectiveEndDate = expenseAddition.endDate?.let { minOf(it, budgetEnd) } ?: budgetEnd
        
        return when (expenseAddition.frequency) {
            ExpenseAddition.Frequency.ONE_TIME -> expenseAddition.monthlyAmount
            ExpenseAddition.Frequency.MONTHLY -> {
                // Calculate what portion of the month this expense applies to
                val totalDays = daysBetween(budgetStart, budgetEnd)
                val applicableDays = daysBetween(effectiveStartDate, effectiveEndDate)
                val prorateRatio = applicableDays.toDouble() / totalDays.toDouble()
                
                Money(
                    expenseAddition.monthlyAmount.amount * prorateRatio,
                    expenseAddition.monthlyAmount.currency
                )
            }
            ExpenseAddition.Frequency.WEEKLY -> {
                val weeksInPeriod = daysBetween(effectiveStartDate, effectiveEndDate) / 7.0
                Money(
                    expenseAddition.monthlyAmount.amount * weeksInPeriod,
                    expenseAddition.monthlyAmount.currency
                )
            }
            ExpenseAddition.Frequency.DAILY -> {
                val daysInPeriod = daysBetween(effectiveStartDate, effectiveEndDate)
                Money(
                    expenseAddition.monthlyAmount.amount * daysInPeriod,
                    expenseAddition.monthlyAmount.currency
                )
            }
        }
    }
    
    private fun analyzeCategoryImpact(
        budget: Budget,
        expenseAddition: ExpenseAddition,
        proratedAmount: Money
    ): CategoryImpact {
        val currentBudgetAmount = budget.categories[expenseAddition.categoryId] ?: Money(0.0, "SAR")
        val currentSpentAmount = budget.spent[expenseAddition.categoryId] ?: Money(0.0, "SAR")
        val newSpentAmount = Money(
            currentSpentAmount.amount + proratedAmount.amount,
            currentSpentAmount.currency
        )
        
        val utilizationBefore = if (currentBudgetAmount.amount > 0) {
            (currentSpentAmount.amount / currentBudgetAmount.amount) * 100
        } else 0.0
        
        val utilizationAfter = if (currentBudgetAmount.amount > 0) {
            (newSpentAmount.amount / currentBudgetAmount.amount) * 100
        } else Double.MAX_VALUE
        
        val willExceedBudget = newSpentAmount.amount > currentBudgetAmount.amount
        val isNewCategory = expenseAddition.categoryId !in budget.categories
        
        return CategoryImpact(
            categoryId = expenseAddition.categoryId,
            currentBudgetAmount = currentBudgetAmount,
            currentSpentAmount = currentSpentAmount,
            newSpentAmount = newSpentAmount,
            spendingIncrease = proratedAmount,
            utilizationBefore = utilizationBefore,
            utilizationAfter = utilizationAfter,
            willExceedBudget = willExceedBudget,
            isNewCategory = isNewCategory
        )
    }
    
    private fun determineBudgetAdjustmentNeeded(
        budget: Budget,
        expenseAddition: ExpenseAddition,
        proratedAmount: Money
    ): Boolean {
        val currentBudgetAmount = budget.categories[expenseAddition.categoryId] ?: Money(0.0, "SAR")
        val currentSpentAmount = budget.spent[expenseAddition.categoryId] ?: Money(0.0, "SAR")
        val newSpentAmount = Money(
            currentSpentAmount.amount + proratedAmount.amount,
            currentSpentAmount.currency
        )
        
        // Need adjustment if:
        // 1. New category (need to add budget)
        // 2. Will exceed current budget
        // 3. Recurring expense (need to plan for future months)
        
        return expenseAddition.categoryId !in budget.categories ||
               newSpentAmount.amount > currentBudgetAmount.amount ||
               expenseAddition.frequency != ExpenseAddition.Frequency.ONE_TIME
    }
    
    private fun generateRecommendations(
        budget: Budget,
        expenseAddition: ExpenseAddition,
        monthlyAmount: Money,
        categoryImpact: CategoryImpact,
        budgetAdjustmentNeeded: Boolean
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (expenseAddition.frequency) {
            ExpenseAddition.Frequency.ONE_TIME -> {
                if (categoryImpact.willExceedBudget) {
                    recommendations.add(
                        "This one-time expense will cause an overspend of ${(categoryImpact.newSpentAmount.amount - categoryImpact.currentBudgetAmount.amount).let { "%.2f".format(it) }} SAR in the '${expenseAddition.categoryId}' category"
                    )
                }
            }
            else -> {
                if (categoryImpact.isNewCategory) {
                    recommendations.add(
                        "Adding this expense requires creating a new '${expenseAddition.categoryId}' budget of ${monthlyAmount.amount.let { "%.2f".format(it) }} SAR per month"
                    )
                } else {
                    recommendations.add(
                        "Adding this expense requires increasing the '${expenseAddition.categoryId}' budget by ${monthlyAmount.amount.let { "%.2f".format(it) }} SAR per month"
                    )
                }
        }
        
        if (expenseAddition.endDate != null) {
            val months = calculateMonthsBetween(expenseAddition.startDate, expenseAddition.endDate!!)
            recommendations.add("This is a temporary expense lasting approximately $months months")
        }
        
        if (categoryImpact.utilizationAfter > 80 && !categoryImpact.willExceedBudget) {
            recommendations.add("This expense will bring the '${expenseAddition.categoryId}' category to ${categoryImpact.utilizationAfter.let { "%.1f".format(it) }}% utilization")
        }
        
        // Suggest reallocation if other categories have room
        val categoryWithRoom = budget.categories.entries.find { (categoryId, budgetAmount) ->
            val spent = budget.spent[categoryId] ?: Money(0.0, "SAR")
            val utilization = if (budgetAmount.amount > 0) spent.amount / budgetAmount.amount else 1.0
            utilization < 0.5 && categoryId != expenseAddition.categoryId
        }
        
        if (categoryWithRoom != null && budgetAdjustmentNeeded) {
            recommendations.add("Consider reallocating from '${categoryWithRoom.key}' category which is currently underutilized")
        }
        
        return recommendations
    }
    
    private fun daysBetween(start: LocalDateTime, end: LocalDateTime): Int {
        // Simplified calculation - in real implementation, use proper date library
        val startDay = start.dayOfYear + (start.year * 365)
        val endDay = end.dayOfYear + (end.year * 365)
        return maxOf(1, endDay - startDay)
    }
    
    private fun calculateMonthsBetween(start: LocalDateTime, end: LocalDateTime): Int {
        val yearDiff = end.year - start.year
        val monthDiff = end.monthNumber - start.monthNumber
        return maxOf(1, yearDiff * 12 + monthDiff)
    }
    
    private fun maxOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a.year > b.year || (a.year == b.year && a.monthNumber > b.monthNumber) ||
            (a.year == b.year && a.monthNumber == b.monthNumber && a.dayOfMonth > b.dayOfMonth)) a else b
    }
    
    private fun minOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a.year < b.year || (a.year == b.year && a.monthNumber < b.monthNumber) ||
            (a.year == b.year && a.monthNumber == b.monthNumber && a.dayOfMonth < b.dayOfMonth)) a else b
    }
}

/**
 * Data class representing an expense to be added to a budget
 */
data class ExpenseAddition(
    val categoryId: String,
    val monthlyAmount: Money,
    val description: String,
    val frequency: Frequency,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime? = null
) {
    enum class Frequency {
        ONE_TIME,
        DAILY,
        WEEKLY,
        MONTHLY
    }
}

/**
 * Data class representing the simulation results of adding an expense
 */
data class ExpenseAdditionSimulation(
    val budgetId: String,
    val expenseAddition: ExpenseAddition,
    val originalTotalBudget: Money,
    val newTotalBudget: Money,
    val budgetIncrease: Money,
    val budgetIncreasePercentage: Double,
    val originalSpending: Money,
    val newSpending: Money,
    val spendingIncrease: Money,
    val spendingIncreasePercentage: Double,
    val categoryImpact: CategoryImpact,
    val budgetAdjustmentNeeded: Boolean,
    val recommendations: List<String>
)

/**
 * Data class representing the impact on a specific category
 */
data class CategoryImpact(
    val categoryId: String,
    val currentBudgetAmount: Money,
    val currentSpentAmount: Money,
    val newSpentAmount: Money,
    val spendingIncrease: Money,
    val utilizationBefore: Double, // Percentage
    val utilizationAfter: Double, // Percentage
    val willExceedBudget: Boolean,
    val isNewCategory: Boolean
)