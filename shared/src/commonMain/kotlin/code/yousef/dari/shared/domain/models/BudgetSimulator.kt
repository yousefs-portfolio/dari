package code.yousef.dari.shared.domain.models

import kotlinx.serialization.Serializable

/**
 * Budget Scenario for what-if analysis
 */
@Serializable
data class BudgetScenario(
    val id: String,
    val name: String,
    val type: ScenarioType,
    val totalBudget: Money,
    val totalSpent: Money,
    val categoryBreakdown: Map<String, Money>,
    val projectedSavings: Money,
    val cashFlowImpact: Money,
    val modifications: List<ScenarioModification>,
    val isSaved: Boolean = false,
    val createdAt: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now(),
    val description: String? = null
)

/**
 * Types of budget scenarios
 */
enum class ScenarioType {
    BASELINE,           // Current spending baseline
    EXPENSE_ADDITION,   // Adding new expenses
    EXPENSE_REMOVAL,    // Reducing/removing expenses
    INCOME_CHANGE      // Income increase/decrease
}

/**
 * Base class for scenario modifications
 */
@Serializable
sealed class ScenarioModification {
    abstract val name: String
    abstract val amount: Money
}

/**
 * Expense Addition Modification
 */
@Serializable
data class ExpenseAddition(
    override val name: String,
    val category: String,
    override val amount: Money,
    val frequency: ExpenseFrequency,
    val startDate: kotlinx.datetime.LocalDate? = null,
    val endDate: kotlinx.datetime.LocalDate? = null,
    val description: String? = null
) : ScenarioModification()

/**
 * Expense Removal Modification
 */
@Serializable
data class ExpenseRemoval(
    val category: String,
    override val amount: Money,
    override val name: String = "Reduce $category",
    val description: String? = null
) : ScenarioModification()

/**
 * Income Adjustment Modification
 */
@Serializable
data class IncomeAdjustment(
    val currentIncome: Money,
    val newIncome: Money,
    val changeType: IncomeChangeType,
    override val name: String = "${changeType.name.lowercase().replaceFirstChar { it.uppercase() }} Income",
    val description: String? = null
) : ScenarioModification() {
    override val amount: Money = Money.fromDouble(
        newIncome.amount - currentIncome.amount,
        currentIncome.currency
    )
}

/**
 * Expense Frequency for recurring expenses
 */
enum class ExpenseFrequency {
    DAILY,
    WEEKLY, 
    MONTHLY,
    QUARTERLY,
    YEARLY,
    ONE_TIME;
    
    /**
     * Convert frequency to monthly multiplier
     */
    fun toMonthlyMultiplier(): Double = when (this) {
        DAILY -> 30.0
        WEEKLY -> 4.33 // Approximate weeks per month
        MONTHLY -> 1.0
        QUARTERLY -> 0.33
        YEARLY -> 0.083 // 1/12
        ONE_TIME -> 1.0 // Assume it's for current month
    }
}

/**
 * Income Change Type
 */
enum class IncomeChangeType {
    INCREASE,
    DECREASE
}

/**
 * Cash Flow Projection for scenario analysis
 */
@Serializable
data class CashFlowProjection(
    val month: Int,
    val income: Money,
    val expenses: Money,
    val netCashFlow: Money,
    val cumulativeSavings: Money,
    val emergencyFundMonths: Double = 0.0
)

/**
 * Scenario Comparison Result
 */
data class ScenarioComparison(
    val scenario1Id: String,
    val scenario2Id: String,
    val scenario1Name: String,
    val scenario2Name: String,
    val impactDifference: Money,
    val savingsDifference: Money,
    val recommendation: String,
    val betterScenarioId: String,
    val confidenceScore: Double
)

/**
 * Budget Impact Analysis
 */
data class BudgetImpactAnalysis(
    val totalImpact: Money,
    val categoryImpacts: Map<String, Money>,
    val monthlyImpact: Money,
    val yearlyImpact: Money,
    val breakEvenMonths: Int?,
    val riskLevel: RiskLevel,
    val recommendations: List<String>
)

/**
 * Risk Level for budget scenarios
 */
enum class RiskLevel {
    LOW,     // Minimal impact on budget
    MEDIUM,  // Moderate impact, manageable
    HIGH,    // Significant impact, requires careful planning
    CRITICAL // Major impact, might require budget restructuring
}

/**
 * Simulation Parameters
 */
data class SimulationParameters(
    val timeHorizonMonths: Int = 12,
    val inflationRate: Double = 0.03, // 3% annual inflation
    val emergencyFundTarget: Double = 6.0, // 6 months of expenses
    val includeInflation: Boolean = true,
    val includeTaxes: Boolean = false,
    val taxRate: Double = 0.0
)

/**
 * Budget Optimization Suggestion
 */
data class OptimizationSuggestion(
    val type: OptimizationType,
    val category: String,
    val currentAmount: Money,
    val suggestedAmount: Money,
    val potentialSavings: Money,
    val reasoning: String,
    val difficultyLevel: DifficultyLevel,
    val estimatedTimeToImplement: String
)

/**
 * Optimization Type
 */
enum class OptimizationType {
    REDUCE_EXPENSE,
    ELIMINATE_EXPENSE,
    NEGOTIATE_BETTER_RATE,
    SWITCH_PROVIDER,
    CONSOLIDATE_SUBSCRIPTIONS,
    BULK_PURCHASE,
    SEASONAL_ADJUSTMENT
}

/**
 * Difficulty Level for implementing optimization
 */
enum class DifficultyLevel {
    EASY,      // Can be done immediately
    MODERATE,  // Requires some effort/research
    HARD,      // Significant lifestyle change required
    VERY_HARD  // Major restructuring needed
}

/**
 * Scenario Template for common what-if scenarios
 */
data class ScenarioTemplate(
    val name: String,
    val description: String,
    val type: ScenarioType,
    val modifications: List<ScenarioModification>,
    val tags: List<String> = emptyList(),
    val category: String = "General"
)

/**
 * Simulation Result Summary
 */
data class SimulationResultSummary(
    val scenarioId: String,
    val scenarioName: String,
    val totalImpact: Money,
    val monthlyImpact: Money,
    val projectedSavings: Money,
    val riskLevel: RiskLevel,
    val feasibilityScore: Double, // 0.0 - 1.0
    val recommendationScore: Double, // 0.0 - 1.0
    val keyInsights: List<String>
)