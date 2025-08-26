package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.repository.BudgetRepository
import kotlinx.datetime.LocalDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Use case for simulating the impact of income changes on budget allocation
 */
class SimulateIncomeChangeUseCase(
    private val budgetRepository: BudgetRepository
) {

    /**
     * Simulate how a change in income affects budget allocation
     * 
     * @param budgetId ID of the budget to modify
     * @param incomeChange Details of the income change
     * @return Result containing simulation results
     */
    suspend operator fun invoke(
        budgetId: String,
        incomeChange: IncomeChange
    ): Result<IncomeChangeSimulation> = runCatching {
        val budget = budgetRepository.getBudget(budgetId)
            ?: throw IllegalArgumentException("Budget not found: $budgetId")
        
        simulateIncomeChange(budget, incomeChange)
    }

    private fun simulateIncomeChange(
        budget: Budget,
        incomeChange: IncomeChange
    ): IncomeChangeSimulation {
        // Calculate income change metrics
        val originalIncome = budget.totalIncome ?: Money(0.0, "SAR")
        val newIncome = incomeChange.newMonthlyIncome
        val incomeIncrease = Money(
            newIncome.amount - originalIncome.amount,
            originalIncome.currency
        )
        val incomeIncreasePercentage = if (originalIncome.amount > 0) {
            (incomeIncrease.amount / originalIncome.amount) * 100
        } else 0.0
        
        // Calculate original budget metrics
        val originalTotalBudget = budget.categories.values.fold(Money(0.0, "SAR")) { acc, money ->
            Money(acc.amount + money.amount, acc.currency)
        }
        
        val originalSpending = budget.spent.values.fold(Money(0.0, "SAR")) { acc, money ->
            Money(acc.amount + money.amount, acc.currency)
        }
        
        val originalBudgetUtilization = if (originalTotalBudget.amount > 0) {
            (originalSpending.amount / originalTotalBudget.amount) * 100
        } else 0.0
        
        // Calculate new budget allocation based on strategy
        val categoryAdjustments = calculateCategoryAdjustments(
            budget,
            incomeChange,
            incomeIncrease
        )
        
        val newTotalBudget = Money(
            originalTotalBudget.amount + categoryAdjustments.sumOf { it.adjustmentAmount.amount },
            originalTotalBudget.currency
        )
        
        val budgetIncrease = Money(
            newTotalBudget.amount - originalTotalBudget.amount,
            originalTotalBudget.currency
        )
        
        val newBudgetUtilization = if (newTotalBudget.amount > 0) {
            (originalSpending.amount / newTotalBudget.amount) * 100
        } else 0.0
        
        // Calculate surplus/deficit
        val surplusAmount = Money(
            newIncome.amount - newTotalBudget.amount,
            newIncome.currency
        )
        
        // Generate recommendations
        val recommendations = generateRecommendations(
            budget,
            incomeChange,
            incomeIncrease,
            categoryAdjustments,
            surplusAmount
        )
        
        return IncomeChangeSimulation(
            budgetId = budget.id,
            incomeChange = incomeChange,
            originalIncome = originalIncome,
            newIncome = newIncome,
            incomeIncrease = incomeIncrease,
            incomeIncreasePercentage = incomeIncreasePercentage,
            originalTotalBudget = originalTotalBudget,
            newTotalBudget = newTotalBudget,
            budgetIncrease = budgetIncrease,
            budgetUtilizationBefore = originalBudgetUtilization,
            budgetUtilizationAfter = newBudgetUtilization,
            categoryAdjustments = categoryAdjustments,
            surplusAmount = surplusAmount,
            recommendations = recommendations
        )
    }
    
    private fun calculateCategoryAdjustments(
        budget: Budget,
        incomeChange: IncomeChange,
        incomeIncrease: Money
    ): List<CategoryAdjustment> {
        return when (incomeChange.budgetScalingStrategy) {
            IncomeChange.BudgetScalingStrategy.PROPORTIONAL -> {
                calculateProportionalAdjustments(budget, incomeIncrease)
            }
            IncomeChange.BudgetScalingStrategy.FIXED_SAVINGS_ALLOCATION -> {
                calculateFixedSavingsAdjustments(budget, incomeChange, incomeIncrease)
            }
            IncomeChange.BudgetScalingStrategy.CONSERVATIVE_REDUCTION -> {
                calculateConservativeReductionAdjustments(budget, incomeIncrease)
            }
            IncomeChange.BudgetScalingStrategy.CUSTOM_ALLOCATION -> {
                calculateCustomAdjustments(budget, incomeChange)
            }
            IncomeChange.BudgetScalingStrategy.MINIMAL_CHANGE -> {
                calculateMinimalAdjustments(budget, incomeIncrease)
            }
        }
    }
    
    private fun calculateProportionalAdjustments(
        budget: Budget,
        incomeIncrease: Money
    ): List<CategoryAdjustment> {
        val totalBudget = budget.categories.values.sumOf { it.amount }
        
        return budget.categories.map { (categoryId, currentAmount) ->
            val proportion = if (totalBudget > 0) currentAmount.amount / totalBudget else 0.0
            val adjustmentAmount = Money(
                incomeIncrease.amount * proportion,
                incomeIncrease.currency
            )
            val newBudgetAmount = Money(
                currentAmount.amount + adjustmentAmount.amount,
                currentAmount.currency
            )
            
            CategoryAdjustment(
                categoryId = categoryId,
                originalAmount = currentAmount,
                adjustmentAmount = adjustmentAmount,
                newBudgetAmount = newBudgetAmount,
                adjustmentReason = "Proportional scaling based on income change",
                isNewCategory = false
            )
        }
    }
    
    private fun calculateFixedSavingsAdjustments(
        budget: Budget,
        incomeChange: IncomeChange,
        incomeIncrease: Money
    ): List<CategoryAdjustment> {
        val targetSavingsRate = incomeChange.targetSavingsRate ?: 20.0 // Default to 20%
        val newTotalSavings = (incomeChange.newMonthlyIncome.amount * targetSavingsRate) / 100
        val currentSavingsAmount = budget.categories["savings"]?.amount ?: 0.0
        val savingsAdjustment = Money(
            newTotalSavings - currentSavingsAmount,
            incomeIncrease.currency
        )
        
        val remainingIncrease = Money(
            incomeIncrease.amount - savingsAdjustment.amount,
            incomeIncrease.currency
        )
        
        val adjustments = mutableListOf<CategoryAdjustment>()
        
        // Handle savings category
        adjustments.add(
            CategoryAdjustment(
                categoryId = "savings",
                originalAmount = budget.categories["savings"] ?: Money(0.0, "SAR"),
                adjustmentAmount = savingsAdjustment,
                newBudgetAmount = Money(newTotalSavings, "SAR"),
                adjustmentReason = "Fixed ${targetSavingsRate}% savings allocation",
                isNewCategory = "savings" !in budget.categories
            )
        )
        
        // Distribute remaining increase proportionally among other categories
        val nonSavingsCategories = budget.categories.filterKeys { it != "savings" }
        val totalNonSavings = nonSavingsCategories.values.sumOf { it.amount }
        
        nonSavingsCategories.forEach { (categoryId, currentAmount) ->
            val proportion = if (totalNonSavings > 0) currentAmount.amount / totalNonSavings else 0.0
            val adjustmentAmount = Money(
                remainingIncrease.amount * proportion,
                remainingIncrease.currency
            )
            
            adjustments.add(
                CategoryAdjustment(
                    categoryId = categoryId,
                    originalAmount = currentAmount,
                    adjustmentAmount = adjustmentAmount,
                    newBudgetAmount = Money(currentAmount.amount + adjustmentAmount.amount, currentAmount.currency),
                    adjustmentReason = "Proportional allocation of remaining income increase",
                    isNewCategory = false
                )
            )
        }
        
        return adjustments
    }
    
    private fun calculateConservativeReductionAdjustments(
        budget: Budget,
        incomeDecrease: Money
    ): List<CategoryAdjustment> {
        // Prioritize essential categories, reduce discretionary spending first
        val essentialCategories = setOf("groceries", "rent", "utilities", "transport", "healthcare")
        val discretionaryCategories = budget.categories.filterKeys { it !in essentialCategories }
        val reductionNeeded = abs(incomeDecrease.amount)
        
        val adjustments = mutableListOf<CategoryAdjustment>()
        var remainingReduction = reductionNeeded
        
        // First, reduce discretionary categories
        val totalDiscretionary = discretionaryCategories.values.sumOf { it.amount }
        discretionaryCategories.forEach { (categoryId, currentAmount) ->
            val reductionRatio = if (totalDiscretionary > 0) {
                min(currentAmount.amount / totalDiscretionary, remainingReduction / totalDiscretionary)
            } else 0.0
            
            val reduction = min(currentAmount.amount * 0.5, remainingReduction * reductionRatio)
            val adjustmentAmount = Money(-reduction, incomeDecrease.currency)
            
            adjustments.add(
                CategoryAdjustment(
                    categoryId = categoryId,
                    originalAmount = currentAmount,
                    adjustmentAmount = adjustmentAmount,
                    newBudgetAmount = Money(currentAmount.amount - reduction, currentAmount.currency),
                    adjustmentReason = "Discretionary spending reduction due to income decrease",
                    isNewCategory = false
                )
            )
            
            remainingReduction -= reduction
        }
        
        // If still need reductions, reduce essential categories modestly
        if (remainingReduction > 0) {
            val essentialBudgetCategories = budget.categories.filterKeys { it in essentialCategories }
            val totalEssential = essentialBudgetCategories.values.sumOf { it.amount }
            
            essentialBudgetCategories.forEach { (categoryId, currentAmount) ->
                val reductionRatio = if (totalEssential > 0) currentAmount.amount / totalEssential else 0.0
                val reduction = min(currentAmount.amount * 0.2, remainingReduction * reductionRatio) // Max 20% reduction
                val adjustmentAmount = Money(-reduction, incomeDecrease.currency)
                
                adjustments.add(
                    CategoryAdjustment(
                        categoryId = categoryId,
                        originalAmount = currentAmount,
                        adjustmentAmount = adjustmentAmount,
                        newBudgetAmount = Money(currentAmount.amount - reduction, currentAmount.currency),
                        adjustmentReason = "Conservative reduction in essential category",
                        isNewCategory = false
                    )
                )
            }
        }
        
        return adjustments
    }
    
    private fun calculateCustomAdjustments(
        budget: Budget,
        incomeChange: IncomeChange
    ): List<CategoryAdjustment> {
        val customAllocations = incomeChange.customCategoryAllocations ?: emptyMap()
        
        return customAllocations.map { (categoryId, allocationAmount) ->
            val originalAmount = budget.categories[categoryId] ?: Money(0.0, "SAR")
            val newBudgetAmount = Money(
                originalAmount.amount + allocationAmount.amount,
                originalAmount.currency
            )
            
            CategoryAdjustment(
                categoryId = categoryId,
                originalAmount = originalAmount,
                adjustmentAmount = allocationAmount,
                newBudgetAmount = newBudgetAmount,
                adjustmentReason = "Custom allocation specified by user",
                isNewCategory = categoryId !in budget.categories
            )
        }
    }
    
    private fun calculateMinimalAdjustments(
        budget: Budget,
        incomeChange: Money
    ): List<CategoryAdjustment> {
        // Make minimal adjustments, primarily to avoid overspending
        val adjustments = mutableListOf<CategoryAdjustment>()
        
        budget.categories.forEach { (categoryId, currentAmount) ->
            val currentSpent = budget.spent[categoryId] ?: Money(0.0, "SAR")
            
            // Only adjust if currently overspending
            if (currentSpent.amount > currentAmount.amount) {
                val overspendAmount = currentSpent.amount - currentAmount.amount
                val adjustmentAmount = Money(
                    min(overspendAmount, abs(incomeChange.amount) * 0.1), // Use up to 10% of income change
                    incomeChange.currency
                )
                
                adjustments.add(
                    CategoryAdjustment(
                        categoryId = categoryId,
                        originalAmount = currentAmount,
                        adjustmentAmount = adjustmentAmount,
                        newBudgetAmount = Money(currentAmount.amount + adjustmentAmount.amount, currentAmount.currency),
                        adjustmentReason = "Minimal adjustment to address overspending",
                        isNewCategory = false
                    )
                )
            }
        }
        
        return adjustments
    }
    
    private fun generateRecommendations(
        budget: Budget,
        incomeChange: IncomeChange,
        incomeIncrease: Money,
        categoryAdjustments: List<CategoryAdjustment>,
        surplusAmount: Money
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Income change context
        val changeDirection = if (incomeIncrease.amount >= 0) "increase" else "decrease"
        val changeAmount = abs(incomeIncrease.amount)
        
        recommendations.add(
            "Income ${changeDirection} of ${changeAmount.let { "%.2f".format(it) }} SAR (${incomeChange.changeReason})"
        )
        
        if (incomeChange.isTemporary) {
            recommendations.add("This is a temporary income change - consider conservative budget adjustments")
        }
        
        // Strategy-specific recommendations
        when (incomeChange.budgetScalingStrategy) {
            IncomeChange.BudgetScalingStrategy.PROPORTIONAL -> {
                recommendations.add("Budget categories scaled proportionally to maintain current spending patterns")
            }
            IncomeChange.BudgetScalingStrategy.FIXED_SAVINGS_ALLOCATION -> {
                val savingsRate = incomeChange.targetSavingsRate ?: 20.0
                recommendations.add("Budget adjusted to maintain ${savingsRate}% savings rate")
            }
            IncomeChange.BudgetScalingStrategy.CONSERVATIVE_REDUCTION -> {
                recommendations.add("Conservative reduction prioritizing essential expenses over discretionary spending")
            }
            IncomeChange.BudgetScalingStrategy.CUSTOM_ALLOCATION -> {
                recommendations.add("Budget adjusted according to your specific allocation preferences")
            }
            IncomeChange.BudgetScalingStrategy.MINIMAL_CHANGE -> {
                recommendations.add("Minimal budget changes applied to maintain current structure")
            }
        }
        
        // Surplus/deficit recommendations
        when {
            surplusAmount.amount > 500 -> {
                recommendations.add("Significant surplus of ${surplusAmount.amount.let { "%.2f".format(it) }} SAR available for emergency fund or new goals")
            }
            surplusAmount.amount > 100 -> {
                recommendations.add("Small surplus available - consider allocating to savings or debt repayment")
            }
            surplusAmount.amount < -100 -> {
                recommendations.add("Budget deficit of ${abs(surplusAmount.amount).let { "%.2f".format(it) }} SAR - additional reductions may be needed")
            }
        }
        
        // Category-specific recommendations
        val newCategories = categoryAdjustments.filter { it.isNewCategory }
        if (newCategories.isNotEmpty()) {
            recommendations.add("New budget categories suggested: ${newCategories.joinToString(", ") { it.categoryId }}")
        }
        
        val largeAdjustments = categoryAdjustments.filter { abs(it.adjustmentAmount.amount) > 200 }
        if (largeAdjustments.isNotEmpty()) {
            recommendations.add("Significant adjustments in: ${largeAdjustments.joinToString(", ") { it.categoryId }}")
        }
        
        // Check for existing overspending
        val overspentCategories = budget.categories.filter { (categoryId, budgetAmount) ->
            val spent = budget.spent[categoryId] ?: Money(0.0, "SAR")
            spent.amount > budgetAmount.amount
        }
        
        if (overspentCategories.isNotEmpty()) {
            recommendations.add("Address existing overspending in: ${overspentCategories.keys.joinToString(", ")}")
        }
        
        return recommendations
    }
}

/**
 * Data class representing a change in income
 */
data class IncomeChange(
    val newMonthlyIncome: Money,
    val changeReason: String,
    val effectiveDate: LocalDateTime,
    val isTemporary: Boolean,
    val budgetScalingStrategy: BudgetScalingStrategy,
    val targetSavingsRate: Double? = null, // For FIXED_SAVINGS_ALLOCATION
    val customCategoryAllocations: Map<String, Money>? = null // For CUSTOM_ALLOCATION
) {
    enum class BudgetScalingStrategy {
        PROPORTIONAL, // Scale all categories proportionally
        FIXED_SAVINGS_ALLOCATION, // Maintain specific savings rate
        CONSERVATIVE_REDUCTION, // Reduce discretionary first
        CUSTOM_ALLOCATION, // User-specified allocations
        MINIMAL_CHANGE // Make minimal adjustments
    }
}

/**
 * Data class representing the simulation results of an income change
 */
data class IncomeChangeSimulation(
    val budgetId: String,
    val incomeChange: IncomeChange,
    val originalIncome: Money,
    val newIncome: Money,
    val incomeIncrease: Money, // Can be negative for decrease
    val incomeIncreasePercentage: Double,
    val originalTotalBudget: Money,
    val newTotalBudget: Money,
    val budgetIncrease: Money,
    val budgetUtilizationBefore: Double, // Percentage
    val budgetUtilizationAfter: Double, // Percentage
    val categoryAdjustments: List<CategoryAdjustment>,
    val surplusAmount: Money, // Positive = surplus, negative = deficit
    val recommendations: List<String>
)

/**
 * Data class representing adjustment to a budget category
 */
data class CategoryAdjustment(
    val categoryId: String,
    val originalAmount: Money,
    val adjustmentAmount: Money, // Can be negative
    val newBudgetAmount: Money,
    val adjustmentReason: String,
    val isNewCategory: Boolean
)