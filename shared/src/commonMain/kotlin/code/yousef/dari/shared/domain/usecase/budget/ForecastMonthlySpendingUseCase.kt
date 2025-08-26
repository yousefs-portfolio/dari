package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlin.math.max
import kotlin.math.min

/**
 * Use case for forecasting monthly spending based on historical patterns and current spending pace
 */
class ForecastMonthlySpendingUseCase(
    private val transactionRepository: TransactionRepository
) {

    /**
     * Forecast spending for a specific month and category
     * 
     * @param year The year to forecast
     * @param month The month to forecast (1-12)
     * @param categoryId The category to forecast for (null for all categories)
     * @return Result containing spending forecast
     */
    suspend operator fun invoke(
        year: Int,
        month: Int,
        categoryId: String? = null
    ): Result<SpendingForecast> = runCatching {
        val currentDate = LocalDateTime(year, month, 15, 12, 0) // Use middle of month as reference
        
        // Get historical data (last 12 months, excluding current month)
        val historicalStartDate = LocalDateTime(year - 1, month, 1, 0, 0)
        val historicalEndDate = LocalDateTime(year, month, 1, 0, 0)
        val historicalTransactions = transactionRepository.getTransactionsByDateRange(
            historicalStartDate, 
            historicalEndDate
        ).filter { transaction ->
            categoryId == null || transaction.categoryId == categoryId
        }.filter { it.type == Transaction.Type.EXPENSE }

        // Get current month transactions
        val currentMonthTransactions = transactionRepository.getTransactionsByMonth(year, month)
            .filter { transaction ->
                categoryId == null || transaction.categoryId == categoryId
            }.filter { it.type == Transaction.Type.EXPENSE }

        generateForecast(
            year = year,
            month = month,
            categoryId = categoryId,
            historicalTransactions = historicalTransactions,
            currentMonthTransactions = currentMonthTransactions
        )
    }

    private fun generateForecast(
        year: Int,
        month: Int,
        categoryId: String?,
        historicalTransactions: List<Transaction>,
        currentMonthTransactions: List<Transaction>
    ): SpendingForecast {
        val daysInMonth = getDaysInMonth(year, month)
        val today = kotlin.math.min(15, daysInMonth) // Use day 15 as reference point
        val daysElapsed = today
        
        // Calculate historical average
        val historicalMonthlyTotals = groupTransactionsByMonth(historicalTransactions)
        val historicalAverage = if (historicalMonthlyTotals.isNotEmpty()) {
            val totalAmount = historicalMonthlyTotals.values.sumOf { it.amount }
            Money(totalAmount / historicalMonthlyTotals.size, "SAR")
        } else {
            Money(0.0, "SAR")
        }
        
        // Calculate current spending
        val currentSpending = currentMonthTransactions.fold(Money(0.0, "SAR")) { acc, transaction ->
            Money(acc.amount + transaction.amount.amount, acc.currency)
        }
        
        // Calculate projected spending based on current pace
        val projectedSpending = if (daysElapsed > 0 && currentSpending.amount > 0) {
            val dailyAverage = currentSpending.amount / daysElapsed
            val projectedTotal = dailyAverage * daysInMonth
            
            // Apply seasonal adjustment based on historical data
            val seasonalAdjustment = calculateSeasonalAdjustment(
                month, 
                historicalTransactions, 
                historicalAverage.amount
            )
            
            Money((projectedTotal * seasonalAdjustment).coerceAtLeast(currentSpending.amount), "SAR")
        } else if (historicalAverage.amount > 0) {
            // If no current spending, use historical average as projection
            historicalAverage
        } else {
            Money(0.0, "SAR")
        }
        
        // Calculate confidence score
        val confidence = calculateConfidence(
            historicalTransactions.size,
            historicalMonthlyTotals.size,
            daysElapsed,
            daysInMonth
        )
        
        // Generate insights
        val insights = generateInsights(
            historicalAverage,
            currentSpending,
            projectedSpending,
            daysElapsed,
            daysInMonth
        )
        
        return SpendingForecast(
            categoryId = categoryId ?: "all",
            year = year,
            month = month,
            historicalAverage = historicalAverage,
            currentSpending = currentSpending,
            projectedSpending = projectedSpending,
            daysElapsed = daysElapsed,
            daysInMonth = daysInMonth,
            confidence = confidence,
            insights = insights,
            lastUpdated = LocalDateTime(year, month, today, 12, 0)
        )
    }
    
    private fun groupTransactionsByMonth(transactions: List<Transaction>): Map<String, Money> {
        return transactions.groupBy { transaction ->
            "${transaction.date.year}-${transaction.date.monthNumber.toString().padStart(2, '0')}"
        }.mapValues { (_, monthTransactions) ->
            monthTransactions.fold(Money(0.0, "SAR")) { acc, transaction ->
                Money(acc.amount + transaction.amount.amount, acc.currency)
            }
        }
    }
    
    private fun calculateSeasonalAdjustment(
        month: Int,
        historicalTransactions: List<Transaction>,
        historicalAverage: Double
    ): Double {
        if (historicalAverage == 0.0 || historicalTransactions.isEmpty()) {
            return 1.0
        }
        
        // Group by same month in previous years
        val sameMonthTransactions = historicalTransactions.filter { 
            it.date.monthNumber == month 
        }
        
        if (sameMonthTransactions.isEmpty()) {
            return 1.0
        }
        
        val sameMonthTotals = groupTransactionsByMonth(sameMonthTransactions)
        val sameMonthAverage = if (sameMonthTotals.isNotEmpty()) {
            sameMonthTotals.values.sumOf { it.amount } / sameMonthTotals.size
        } else {
            return 1.0
        }
        
        // Return seasonal adjustment factor (how this month typically compares to overall average)
        return (sameMonthAverage / historicalAverage).coerceIn(0.5, 2.0)
    }
    
    private fun calculateConfidence(
        totalHistoricalTransactions: Int,
        historicalMonths: Int,
        daysElapsed: Int,
        daysInMonth: Int
    ): Double {
        // Base confidence on amount of historical data
        val dataConfidence = when {
            historicalMonths >= 6 -> 0.8
            historicalMonths >= 3 -> 0.6
            historicalMonths >= 1 -> 0.4
            totalHistoricalTransactions > 0 -> 0.2
            else -> 0.0
        }
        
        // Adjust confidence based on how much of current month has elapsed
        val progressConfidence = when {
            daysElapsed >= (daysInMonth * 0.8) -> 1.0 // 80% through month
            daysElapsed >= (daysInMonth * 0.5) -> 0.8 // 50% through month
            daysElapsed >= (daysInMonth * 0.3) -> 0.6 // 30% through month
            else -> 0.4
        }
        
        return (dataConfidence * 0.7 + progressConfidence * 0.3).coerceIn(0.0, 1.0)
    }
    
    private fun generateInsights(
        historicalAverage: Money,
        currentSpending: Money,
        projectedSpending: Money,
        daysElapsed: Int,
        daysInMonth: Int
    ): List<String> {
        val insights = mutableListOf<String>()
        
        if (historicalAverage.amount > 0) {
            val comparisonToHistorical = projectedSpending.amount / historicalAverage.amount
            
            when {
                comparisonToHistorical > 1.2 -> {
                    insights.add("Projected spending is ${((comparisonToHistorical - 1) * 100).toInt()}% higher than historical average")
                }
                comparisonToHistorical < 0.8 -> {
                    insights.add("Projected spending is ${((1 - comparisonToHistorical) * 100).toInt()}% lower than historical average")
                }
                else -> {
                    insights.add("Projected spending is in line with historical patterns")
                }
            }
        }
        
        val progressPercentage = (daysElapsed.toDouble() / daysInMonth) * 100
        val spendingPercentage = if (projectedSpending.amount > 0) {
            (currentSpending.amount / projectedSpending.amount) * 100
        } else 0.0
        
        when {
            spendingPercentage > progressPercentage + 10 -> {
                insights.add("You're spending faster than projected pace")
            }
            spendingPercentage < progressPercentage - 10 -> {
                insights.add("You're spending slower than projected pace")
            }
        }
        
        if (daysElapsed < daysInMonth * 0.5 && currentSpending.amount == 0.0) {
            insights.add("No spending recorded yet this month")
        }
        
        return insights
    }
    
    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
    
    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}

/**
 * Data class representing a spending forecast for a month
 */
data class SpendingForecast(
    val categoryId: String,
    val year: Int,
    val month: Int,
    val historicalAverage: Money,
    val currentSpending: Money,
    val projectedSpending: Money,
    val daysElapsed: Int,
    val daysInMonth: Int,
    val confidence: Double, // 0.0 to 1.0
    val insights: List<String>,
    val lastUpdated: LocalDateTime
)