package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.domain.services.AIBudgetService
import code.yousef.dari.shared.utils.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for generating AI-powered budget recommendations
 * Supports Saudi-specific budgeting, Islamic financial principles, and cultural considerations
 */
class GetBudgetRecommendationsUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val aiBudgetService: AIBudgetService
) {
    suspend operator fun invoke(request: GetBudgetRecommendationsRequest): Result<BudgetRecommendationsResponse> {
        return try {
            // Gather user financial data
            val financialDataResult = gatherFinancialData(request.userId)
            if (financialDataResult is Result.Error) {
                return financialDataResult
            }

            val financialData = (financialDataResult as Result.Success).data

            // Try AI-powered recommendations first
            val aiRecommendations = if (request.useAI) {
                generateAIRecommendations(request, financialData)
            } else {
                null
            }

            // Generate final recommendations (AI + rule-based)
            val recommendations = when {
                aiRecommendations is Result.Success -> aiRecommendations.data
                else -> generateRuleBasedRecommendations(request, financialData)
            }

            // Apply filters and prioritization
            val filteredRecommendations = filterAndPrioritizeRecommendations(
                recommendations = recommendations,
                request = request,
                financialData = financialData
            )

            Result.Success(
                BudgetRecommendationsResponse(
                    recommendations = filteredRecommendations,
                    totalRecommendedMonthlyBudget = filteredRecommendations.sumOf { it.recommendedAmount.value },
                    availableIncome = calculateAvailableIncome(request.userProfile, financialData),
                    budgetUtilization = calculateBudgetUtilization(filteredRecommendations, request.userProfile),
                    fallbackUsed = aiRecommendations !is Result.Success,
                    generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            )

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun gatherFinancialData(userId: String): Result<UserFinancialData> {
        return try {
            // Get recent transactions (last 3 months)
            val transactionsResult = transactionRepository.getRecentTransactions(userId, months = 3)
            if (transactionsResult is Result.Error) {
                return transactionsResult
            }

            // Get existing budgets
            val budgetsResult = budgetRepository.getBudgetsByUserId(userId)
            if (budgetsResult is Result.Error) {
                return budgetsResult
            }

            val transactions = (transactionsResult as Result.Success).data
            val budgets = (budgetsResult as Result.Success).data

            // Calculate spending patterns
            val spendingByCategory = calculateSpendingByCategory(transactions)
            val monthlyAverages = calculateMonthlyAverages(transactions)
            val budgetPerformance = calculateBudgetPerformance(budgets, transactions)

            Result.Success(
                UserFinancialData(
                    recentTransactions = transactions,
                    existingBudgets = budgets,
                    spendingByCategory = spendingByCategory,
                    monthlyAverages = monthlyAverages,
                    budgetPerformance = budgetPerformance
                )
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun generateAIRecommendations(
        request: GetBudgetRecommendationsRequest,
        financialData: UserFinancialData
    ): Result<List<BudgetRecommendation>> {
        val aiRequest = AIBudgetRequest(
            userProfile = request.userProfile,
            financialData = financialData,
            includeIslamicBudgets = request.includeIslamicBudgets,
            includeSaudiSpecific = request.includeSaudiSpecific,
            includeSeasonalBudgets = request.includeSeasonalBudgets
        )

        return aiBudgetService.generateRecommendations(aiRequest)
    }

    private fun generateRuleBasedRecommendations(
        request: GetBudgetRecommendationsRequest,
        financialData: UserFinancialData
    ): List<BudgetRecommendation> {
        val recommendations = mutableListOf<BudgetRecommendation>()
        val monthlyIncome = request.userProfile.monthlyIncome.value

        // Essential Islamic budgets
        if (request.includeIslamicBudgets && request.userProfile.isMuslim) {
            recommendations.addAll(generateIslamicRecommendations(request.userProfile))
        }

        // Saudi-specific budgets
        if (request.includeSaudiSpecific) {
            recommendations.addAll(generateSaudiSpecificRecommendations(request.userProfile, financialData))
        }

        // Core living expenses
        recommendations.addAll(generateCoreExpenseRecommendations(request.userProfile, financialData))

        // Seasonal budgets
        if (request.includeSeasonalBudgets) {
            recommendations.addAll(generateSeasonalRecommendations(request.userProfile))
        }

        // Emergency fund
        if (request.includeEmergencyFund) {
            recommendations.add(generateEmergencyFundRecommendation(request.userProfile, financialData))
        }

        return recommendations
    }

    private fun generateIslamicRecommendations(userProfile: UserProfile): List<BudgetRecommendation> {
        val monthlyIncome = userProfile.monthlyIncome.value
        val recommendations = mutableListOf<BudgetRecommendation>()

        // Zakat calculation (2.5% of wealth, simplified to income-based)
        val zakatAmount = monthlyIncome * 0.025
        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.CHARITY,
                recommendedAmount = Money(zakatAmount, userProfile.monthlyIncome.currency),
                confidence = 0.95,
                reasoning = "Zakat obligation: 2.5% of income (simplified calculation)",
                priority = BudgetPriority.ESSENTIAL,
                period = BudgetPeriod.MONTHLY,
                isIslamic = true,
                tags = listOf("zakat", "islamic", "obligation")
            )
        )

        // Sadaqah (additional charity)
        val sadaqahAmount = monthlyIncome * 0.01 // 1% recommended
        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.CHARITY,
                recommendedAmount = Money(sadaqahAmount, userProfile.monthlyIncome.currency),
                confidence = 0.8,
                reasoning = "Additional voluntary charity (Sadaqah) - recommended 1% of income",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                isIslamic = true,
                tags = listOf("sadaqah", "charity", "voluntary")
            )
        )

        return recommendations
    }

    private fun generateSaudiSpecificRecommendations(
        userProfile: UserProfile,
        financialData: UserFinancialData
    ): List<BudgetRecommendation> {
        val monthlyIncome = userProfile.monthlyIncome.value
        val recommendations = mutableListOf<BudgetRecommendation>()

        // Housing (30-40% for Saudi market)
        val housingPercentage = when {
            userProfile.hasMortgage -> 0.35 // 35% for mortgage holders
            userProfile.location.contains("Riyadh") || userProfile.location.contains("Jeddah") -> 0.40 // Higher for major cities
            else -> 0.30
        }
        
        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.HOUSING,
                recommendedAmount = Money(monthlyIncome * housingPercentage, userProfile.monthlyIncome.currency),
                confidence = 0.9,
                reasoning = "Housing costs in Saudi Arabia: ${(housingPercentage * 100).toInt()}% of income recommended",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("housing", "saudi", "rent")
            )
        )

        // Transportation (Saudi-specific, considering high fuel subsidies)
        val transportationAmount = monthlyIncome * 0.15 // 15% for transportation
        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.TRANSPORTATION,
                recommendedAmount = Money(transportationAmount, userProfile.monthlyIncome.currency),
                confidence = 0.85,
                reasoning = "Transportation in Saudi Arabia including fuel (subsidized) and maintenance",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("transportation", "fuel", "saudi")
            )
        )

        // VAT and government fees
        val avgMonthlyExpenses = monthlyIncome * 0.7 // Assuming 70% of income is spent
        val vatAmount = avgMonthlyExpenses * 0.15 // 15% VAT
        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.TAXES,
                recommendedAmount = Money(vatAmount, userProfile.monthlyIncome.currency),
                confidence = 0.9,
                reasoning = "VAT (15%) and government fees budget",
                priority = BudgetPriority.MEDIUM,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("vat", "tax", "government")
            )
        )

        return recommendations
    }

    private fun generateCoreExpenseRecommendations(
        userProfile: UserProfile,
        financialData: UserFinancialData
    ): List<BudgetRecommendation> {
        val monthlyIncome = userProfile.monthlyIncome.value
        val recommendations = mutableListOf<BudgetRecommendation>()

        // Food and dining (adjusted for family size)
        val baseFoodAmount = monthlyIncome * 0.15
        val familyMultiplier = 1.0 + (userProfile.dependents * 0.3)
        val foodAmount = baseFoodAmount * familyMultiplier

        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.FOOD_DINING,
                recommendedAmount = Money(foodAmount, userProfile.monthlyIncome.currency),
                confidence = 0.85,
                reasoning = "Food budget for family of ${1 + userProfile.dependents} people",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                basedOnSpending = financialData.spendingByCategory.containsKey(TransactionCategory.FOOD_DINING)
            )
        )

        // Healthcare
        val healthcareAmount = monthlyIncome * 0.08
        recommendations.add(
            BudgetRecommendation(
                category = TransactionCategory.HEALTHCARE,
                recommendedAmount = Money(healthcareAmount, userProfile.monthlyIncome.currency),
                confidence = 0.8,
                reasoning = "Healthcare expenses including insurance premiums and medical costs",
                priority = BudgetPriority.MEDIUM,
                period = BudgetPeriod.MONTHLY
            )
        )

        return recommendations
    }

    private fun generateSeasonalRecommendations(userProfile: UserProfile): List<BudgetRecommendation> {
        val recommendations = mutableListOf<BudgetRecommendation>()
        val monthlyIncome = userProfile.monthlyIncome.value

        // Ramadan budget (if Muslim)
        if (userProfile.isMuslim) {
            recommendations.add(
                BudgetRecommendation(
                    category = TransactionCategory.FOOD_DINING,
                    recommendedAmount = Money(monthlyIncome * 0.25, userProfile.monthlyIncome.currency),
                    confidence = 0.9,
                    reasoning = "Ramadan iftar and suhoor expenses (40% increase from normal food budget)",
                    priority = BudgetPriority.HIGH,
                    period = BudgetPeriod.MONTHLY,
                    seasonalBudget = true,
                    tags = listOf("ramadan", "iftar", "seasonal")
                )
            )

            // Hajj savings (long-term goal)
            recommendations.add(
                BudgetRecommendation(
                    category = TransactionCategory.TRAVEL,
                    recommendedAmount = Money(monthlyIncome * 0.05, userProfile.monthlyIncome.currency),
                    confidence = 0.8,
                    reasoning = "Hajj pilgrimage savings (typical cost: 25,000-40,000 SAR)",
                    priority = BudgetPriority.MEDIUM,
                    period = BudgetPeriod.GOAL_BASED,
                    targetAmount = Money(30000.0, userProfile.monthlyIncome.currency),
                    monthsToTarget = 60, // 5 years
                    isIslamic = true,
                    tags = listOf("hajj", "pilgrimage", "islamic", "savings")
                )
            )
        }

        return recommendations
    }

    private fun generateEmergencyFundRecommendation(
        userProfile: UserProfile,
        financialData: UserFinancialData
    ): BudgetRecommendation {
        val monthlyExpenses = financialData.monthlyAverages.values.sum()
        val emergencyTarget = monthlyExpenses * 6 // 6 months of expenses
        val monthlyContribution = userProfile.monthlyIncome.value * 0.10 // 10% of income

        return BudgetRecommendation(
            category = TransactionCategory.EMERGENCY_FUND,
            recommendedAmount = Money(monthlyContribution, userProfile.monthlyIncome.currency),
            confidence = 0.95,
            reasoning = "Emergency fund should cover 6 months of expenses (${emergencyTarget.toInt()} SAR)",
            priority = BudgetPriority.ESSENTIAL,
            period = BudgetPeriod.GOAL_BASED,
            targetAmount = Money(emergencyTarget, userProfile.monthlyIncome.currency),
            monthsToTarget = (emergencyTarget / monthlyContribution).toInt().coerceAtLeast(1)
        )
    }

    private fun filterAndPrioritizeRecommendations(
        recommendations: List<BudgetRecommendation>,
        request: GetBudgetRecommendationsRequest,
        financialData: UserFinancialData
    ): List<BudgetRecommendation> {
        return recommendations
            .filter { recommendation ->
                when {
                    // Always include essential recommendations
                    recommendation.priority == BudgetPriority.ESSENTIAL -> true
                    
                    // Include Islamic recommendations if requested
                    recommendation.isIslamic && request.includeIslamicBudgets -> true
                    
                    // Include seasonal recommendations if requested
                    recommendation.seasonalBudget && request.includeSeasonalBudgets -> true
                    
                    // Include based on confidence threshold
                    recommendation.confidence >= (request.confidenceThreshold ?: 0.7) -> true
                    
                    else -> false
                }
            }
            .sortedWith(compareByDescending<BudgetRecommendation> { it.priority.ordinal }
                .thenByDescending { if (it.isIslamic) 1 else 0 }
                .thenByDescending { it.confidence })
    }

    // Helper functions for calculations
    private fun calculateSpendingByCategory(transactions: List<Transaction>): Map<TransactionCategory, Double> {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount.value } }
    }

    private fun calculateMonthlyAverages(transactions: List<Transaction>): Map<TransactionCategory, Double> {
        val months = 3.0 // Assuming 3 months of data
        return calculateSpendingByCategory(transactions)
            .mapValues { (_, total) -> total / months }
    }

    private fun calculateBudgetPerformance(budgets: List<Budget>, transactions: List<Transaction>): Map<String, Double> {
        return budgets.associate { budget ->
            val spent = transactions
                .filter { it.budgetId == budget.id }
                .sumOf { it.amount.value }
            
            budget.id to (spent / budget.amount.value)
        }
    }

    private fun calculateAvailableIncome(userProfile: UserProfile, financialData: UserFinancialData): Double {
        val totalMonthlyExpenses = financialData.monthlyAverages.values.sum()
        return userProfile.monthlyIncome.value - totalMonthlyExpenses
    }

    private fun calculateBudgetUtilization(
        recommendations: List<BudgetRecommendation>,
        userProfile: UserProfile
    ): Double {
        val totalRecommended = recommendations.sumOf { it.recommendedAmount.value }
        return (totalRecommended / userProfile.monthlyIncome.value) * 100
    }
}

// Supporting data classes and interfaces

/**
 * AI Budget Service interface
 */
interface AIBudgetService {
    suspend fun generateRecommendations(request: AIBudgetRequest): Result<List<BudgetRecommendation>>
}

/**
 * Request for budget recommendations
 */
data class GetBudgetRecommendationsRequest(
    val userId: String,
    val userProfile: UserProfile,
    val includeIslamicBudgets: Boolean = false,
    val includeSaudiSpecific: Boolean = true,
    val includeSeasonalBudgets: Boolean = false,
    val includeEmergencyFund: Boolean = true,
    val useAI: Boolean = true,
    val confidenceThreshold: Double? = null
)

/**
 * User profile for budget recommendations
 */
data class UserProfile(
    val id: String,
    val age: Int,
    val monthlyIncome: Money,
    val dependents: Int = 0,
    val maritalStatus: String,
    val location: String,
    val isMuslim: Boolean = false,
    val hasMortgage: Boolean = false,
    val hasVehicleLoan: Boolean = false,
    val employmentType: String = "full_time",
    val riskTolerance: RiskTolerance = RiskTolerance.MODERATE,
    val preferences: Map<String, String> = emptyMap()
)

/**
 * Risk tolerance levels
 */
enum class RiskTolerance {
    CONSERVATIVE, MODERATE, AGGRESSIVE
}

/**
 * User financial data aggregation
 */
data class UserFinancialData(
    val recentTransactions: List<Transaction>,
    val existingBudgets: List<Budget>,
    val spendingByCategory: Map<TransactionCategory, Double>,
    val monthlyAverages: Map<TransactionCategory, Double>,
    val budgetPerformance: Map<String, Double>
)

/**
 * AI budget request
 */
data class AIBudgetRequest(
    val userProfile: UserProfile,
    val financialData: UserFinancialData,
    val includeIslamicBudgets: Boolean,
    val includeSaudiSpecific: Boolean,
    val includeSeasonalBudgets: Boolean
)

/**
 * Budget recommendation
 */
data class BudgetRecommendation(
    val category: TransactionCategory,
    val recommendedAmount: Money,
    val confidence: Double, // 0.0 to 1.0
    val reasoning: String,
    val priority: BudgetPriority,
    val period: BudgetPeriod,
    val adjustmentType: BudgetAdjustmentType? = null,
    val currentAmount: Money? = null,
    val targetAmount: Money? = null,
    val monthsToTarget: Int? = null,
    val isIslamic: Boolean = false,
    val seasonalBudget: Boolean = false,
    val basedOnSpending: Boolean = false,
    val tags: List<String> = emptyList()
)

/**
 * Budget priority levels
 */
enum class BudgetPriority {
    ESSENTIAL, HIGH, MEDIUM, LOW
}

/**
 * Budget adjustment types
 */
enum class BudgetAdjustmentType {
    INCREASE, DECREASE, MAINTAIN
}

/**
 * Response containing budget recommendations
 */
data class BudgetRecommendationsResponse(
    val recommendations: List<BudgetRecommendation>,
    val totalRecommendedMonthlyBudget: Double,
    val availableIncome: Double,
    val budgetUtilization: Double, // Percentage
    val fallbackUsed: Boolean = false,
    val generatedAt: kotlinx.datetime.LocalDateTime
)