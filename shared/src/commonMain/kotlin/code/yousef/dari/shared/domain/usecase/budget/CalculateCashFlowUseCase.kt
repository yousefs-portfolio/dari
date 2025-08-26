package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Use case for calculating cash flow projections based on historical data and budget information
 */
class CalculateCashFlowUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Calculate cash flow projection for specified number of months
     * 
     * @param budgetId ID of the budget to base projections on
     * @param projectionMonths Number of months to project (default 6)
     * @return Result containing cash flow projection
     */
    suspend operator fun invoke(
        budgetId: String,
        projectionMonths: Int = 6
    ): Result<CashFlowProjection> = runCatching {
        val budget = budgetRepository.getBudget(budgetId)
            ?: throw IllegalArgumentException("Budget not found: $budgetId")
        
        // Get historical data for analysis (last 12 months)
        val currentDate = LocalDateTime(2024, 3, 15, 12, 0) // Reference date
        val historicalStartDate = currentDate.let { 
            LocalDateTime(it.year - 1, it.monthNumber, it.dayOfMonth, it.hour, it.minute)
        }
        val historicalTransactions = transactionRepository.getTransactionsByDateRange(
            historicalStartDate,
            currentDate
        )
        
        calculateCashFlowProjection(budget, historicalTransactions, projectionMonths)
    }

    private fun calculateCashFlowProjection(
        budget: Budget,
        historicalTransactions: List<Transaction>,
        projectionMonths: Int
    ): CashFlowProjection {
        // Analyze historical patterns
        val historicalAnalysis = analyzeHistoricalTransactions(historicalTransactions)
        
        // Calculate baseline projections
        val baselineIncome = calculateBaselineIncome(budget, historicalAnalysis)
        val baselineExpenses = calculateBaselineExpenses(budget, historicalAnalysis)
        
        // Generate monthly projections
        val startDate = LocalDateTime(2024, 3, 1, 0, 0) // Starting from March 2024
        val monthlyProjections = generateMonthlyProjections(
            startDate,
            projectionMonths,
            baselineIncome,
            baselineExpenses,
            historicalAnalysis
        )
        
        // Calculate running balances
        val projectionsWithBalance = calculateRunningBalances(monthlyProjections, Money(0.0, "SAR"))
        
        // Generate insights
        val insights = generateCashFlowInsights(projectionsWithBalance, historicalAnalysis)
        
        return CashFlowProjection(
            budgetId = budget.id,
            projectionMonths = projectionMonths,
            monthlyProjections = projectionsWithBalance,
            totalProjectedIncome = Money(
                projectionsWithBalance.sumOf { it.projectedIncome.amount },
                "SAR"
            ),
            totalProjectedExpenses = Money(
                projectionsWithBalance.sumOf { it.projectedExpenses.amount },
                "SAR"
            ),
            netCashFlow = Money(
                projectionsWithBalance.sumOf { it.netCashFlow.amount },
                "SAR"
            ),
            finalBalance = projectionsWithBalance.lastOrNull()?.runningBalance ?: Money(0.0, "SAR"),
            insights = insights,
            lastUpdated = LocalDateTime(2024, 3, 15, 12, 0)
        )
    }
    
    private fun analyzeHistoricalTransactions(
        transactions: List<Transaction>
    ): HistoricalAnalysis {
        val incomeTransactions = transactions.filter { it.type == Transaction.Type.INCOME }
        val expenseTransactions = transactions.filter { it.type == Transaction.Type.EXPENSE }
        
        // Group by month
        val monthlyIncome = incomeTransactions.groupBy { 
            "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}"
        }.mapValues { (_, monthTransactions) ->
            monthTransactions.sumOf { it.amount.amount }
        }
        
        val monthlyExpenses = expenseTransactions.groupBy { 
            "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}"
        }.mapValues { (_, monthTransactions) ->
            monthTransactions.sumOf { it.amount.amount }
        }
        
        // Calculate statistics
        val avgMonthlyIncome = if (monthlyIncome.isNotEmpty()) {
            monthlyIncome.values.average()
        } else 0.0
        
        val avgMonthlyExpenses = if (monthlyExpenses.isNotEmpty()) {
            monthlyExpenses.values.average()
        } else 0.0
        
        val incomeVariability = if (monthlyIncome.values.size > 1) {
            calculateVariability(monthlyIncome.values.toList())
        } else 0.0
        
        val expenseVariability = if (monthlyExpenses.values.size > 1) {
            calculateVariability(monthlyExpenses.values.toList())
        } else 0.0
        
        // Analyze seasonal patterns
        val seasonalFactors = calculateSeasonalFactors(expenseTransactions)
        
        return HistoricalAnalysis(
            averageMonthlyIncome = avgMonthlyIncome,
            averageMonthlyExpenses = avgMonthlyExpenses,
            incomeVariability = incomeVariability,
            expenseVariability = expenseVariability,
            seasonalFactors = seasonalFactors,
            monthsOfData = maxOf(monthlyIncome.keys.size, monthlyExpenses.keys.size)
        )
    }
    
    private fun calculateBaselineIncome(
        budget: Budget,
        historicalAnalysis: HistoricalAnalysis
    ): Money {
        return when {
            historicalAnalysis.averageMonthlyIncome > 0 -> {
                // Use historical average if available
                Money(historicalAnalysis.averageMonthlyIncome, "SAR")
            }
            budget.totalIncome != null -> {
                // Fall back to budget income
                budget.totalIncome!!
            }
            else -> {
                // Last resort: estimate from budget allocations
                val totalBudgetAllocations = budget.categories.values.sumOf { it.amount }
                Money(totalBudgetAllocations * 1.1, "SAR") // Add 10% buffer
            }
        }
    }
    
    private fun calculateBaselineExpenses(
        budget: Budget,
        historicalAnalysis: HistoricalAnalysis
    ): Money {
        return when {
            historicalAnalysis.averageMonthlyExpenses > 0 -> {
                // Use historical average
                Money(historicalAnalysis.averageMonthlyExpenses, "SAR")
            }
            else -> {
                // Use current spending pattern from budget
                val currentSpending = budget.spent.values.sumOf { it.amount }
                if (currentSpending > 0) {
                    Money(currentSpending, "SAR")
                } else {
                    // Estimate 80% of budget allocations
                    val totalBudgetAllocations = budget.categories.values.sumOf { it.amount }
                    Money(totalBudgetAllocations * 0.8, "SAR")
                }
            }
        }
    }
    
    private fun generateMonthlyProjections(
        startDate: LocalDateTime,
        projectionMonths: Int,
        baselineIncome: Money,
        baselineExpenses: Money,
        historicalAnalysis: HistoricalAnalysis
    ): List<MonthlyCashFlow> {
        return (0 until projectionMonths).map { monthOffset ->
            val projectionDate = LocalDateTime(
                startDate.year,
                startDate.monthNumber + monthOffset,
                startDate.dayOfMonth,
                startDate.hour,
                startDate.minute
            )
            
            val month = projectionDate.monthNumber
            val year = projectionDate.year
            
            // Apply seasonal adjustments
            val seasonalFactor = historicalAnalysis.seasonalFactors[month] ?: 1.0
            val adjustedExpenses = Money(
                baselineExpenses.amount * seasonalFactor,
                baselineExpenses.currency
            )
            
            // Apply income variability if high
            val adjustedIncome = if (historicalAnalysis.incomeVariability > 0.3) {
                // Add conservative buffer for variable income
                Money(baselineIncome.amount * 0.9, baselineIncome.currency)
            } else {
                baselineIncome
            }
            
            val netCashFlow = Money(
                adjustedIncome.amount - adjustedExpenses.amount,
                adjustedIncome.currency
            )
            
            MonthlyCashFlow(
                year = year,
                month = month,
                projectedIncome = adjustedIncome,
                projectedExpenses = adjustedExpenses,
                netCashFlow = netCashFlow,
                runningBalance = Money(0.0, "SAR") // Will be calculated later
            )
        }
    }
    
    private fun calculateRunningBalances(
        monthlyProjections: List<MonthlyCashFlow>,
        startingBalance: Money
    ): List<MonthlyCashFlow> {
        var runningBalance = startingBalance.amount
        
        return monthlyProjections.map { monthly ->
            runningBalance += monthly.netCashFlow.amount
            monthly.copy(
                runningBalance = Money(runningBalance, startingBalance.currency)
            )
        }
    }
    
    private fun calculateSeasonalFactors(expenseTransactions: List<Transaction>): Map<Int, Double> {
        val monthlyExpenses = expenseTransactions.groupBy { it.date.monthNumber }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount.amount } }
        
        if (monthlyExpenses.isEmpty()) return emptyMap()
        
        val averageExpense = monthlyExpenses.values.average()
        
        return monthlyExpenses.mapValues { (_, monthTotal) ->
            if (averageExpense > 0) monthTotal / averageExpense else 1.0
        }
    }
    
    private fun calculateVariability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return if (mean > 0) standardDeviation / mean else 0.0
    }
    
    private fun generateCashFlowInsights(
        monthlyProjections: List<MonthlyCashFlow>,
        historicalAnalysis: HistoricalAnalysis
    ): List<String> {
        val insights = mutableListOf<String>()
        
        // Overall cash flow trend
        val positiveMonths = monthlyProjections.count { it.netCashFlow.amount > 0 }
        val negativeMonths = monthlyProjections.count { it.netCashFlow.amount < 0 }
        
        when {
            positiveMonths == monthlyProjections.size -> {
                insights.add("Consistent positive cash flow projected for all ${monthlyProjections.size} months")
                val totalSurplus = monthlyProjections.sumOf { it.netCashFlow.amount }
                insights.add("Total projected surplus: ${totalSurplus.let { "%.2f".format(it) }} SAR")
            }
            negativeMonths == monthlyProjections.size -> {
                insights.add("Cash flow deficit projected for all months - immediate action required")
                val totalDeficit = abs(monthlyProjections.sumOf { it.netCashFlow.amount })
                insights.add("Total projected deficit: ${totalDeficit.let { "%.2f".format(it) }} SAR")
            }
            negativeMonths > positiveMonths -> {
                insights.add("More deficit months ($negativeMonths) than surplus months ($positiveMonths)")
            }
            else -> {
                insights.add("Mixed cash flow with $positiveMonths positive and $negativeMonths negative months")
            }
        }
        
        // Running balance analysis
        val finalBalance = monthlyProjections.lastOrNull()?.runningBalance?.amount ?: 0.0
        val lowestBalance = monthlyProjections.minOfOrNull { it.runningBalance.amount } ?: 0.0
        
        when {
            lowestBalance < -1000 -> {
                insights.add("Cash balance may drop below -1,000 SAR - consider building emergency fund")
            }
            lowestBalance < 0 -> {
                insights.add("Cash balance may go negative - monitor spending closely")
            }
            finalBalance > 5000 -> {
                insights.add("Strong final balance of ${finalBalance.let { "%.2f".format(it) }} SAR projected")
            }
        }
        
        // Income variability insights
        when {
            historicalAnalysis.incomeVariability > 0.4 -> {
                insights.add("High income variability detected - projections are conservative")
                insights.add("Consider building larger emergency fund due to irregular income")
            }
            historicalAnalysis.incomeVariability > 0.2 -> {
                insights.add("Moderate income variability - some fluctuation expected")
            }
        }
        
        // Seasonal patterns
        val seasonalMonths = historicalAnalysis.seasonalFactors.filter { (_, factor) ->
            factor > 1.2 || factor < 0.8
        }
        
        if (seasonalMonths.isNotEmpty()) {
            val highSpendingMonths = seasonalMonths.filter { it.value > 1.2 }.keys
            if (highSpendingMonths.isNotEmpty()) {
                val monthNames = highSpendingMonths.joinToString(", ") { getMonthName(it) }
                insights.add("Higher spending typically occurs in: $monthNames")
            }
        }
        
        // Data quality insights
        when {
            historicalAnalysis.monthsOfData < 3 -> {
                insights.add("Limited historical data (${historicalAnalysis.monthsOfData} months) - projections based on budget")
            }
            historicalAnalysis.monthsOfData >= 12 -> {
                insights.add("Projections based on ${historicalAnalysis.monthsOfData} months of transaction history")
            }
        }
        
        // Emergency fund recommendations
        val avgMonthlyExpenses = historicalAnalysis.averageMonthlyExpenses
        if (avgMonthlyExpenses > 0 && finalBalance < avgMonthlyExpenses * 3) {
            val recommendedEmergencyFund = avgMonthlyExpenses * 3
            insights.add("Consider building emergency fund to ${recommendedEmergencyFund.let { "%.0f".format(it) }} SAR (3 months expenses)")
        }
        
        return insights
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Month $month"
        }
    }
}

/**
 * Data class representing historical transaction analysis
 */
private data class HistoricalAnalysis(
    val averageMonthlyIncome: Double,
    val averageMonthlyExpenses: Double,
    val incomeVariability: Double, // Standard deviation / mean
    val expenseVariability: Double,
    val seasonalFactors: Map<Int, Double>, // Month -> factor (1.0 = average)
    val monthsOfData: Int
)

/**
 * Data class representing cash flow projection results
 */
data class CashFlowProjection(
    val budgetId: String,
    val projectionMonths: Int,
    val monthlyProjections: List<MonthlyCashFlow>,
    val totalProjectedIncome: Money,
    val totalProjectedExpenses: Money,
    val netCashFlow: Money,
    val finalBalance: Money,
    val insights: List<String>,
    val lastUpdated: LocalDateTime
)

/**
 * Data class representing monthly cash flow projection
 */
data class MonthlyCashFlow(
    val year: Int,
    val month: Int,
    val projectedIncome: Money,
    val projectedExpenses: Money,
    val netCashFlow: Money,
    val runningBalance: Money
)