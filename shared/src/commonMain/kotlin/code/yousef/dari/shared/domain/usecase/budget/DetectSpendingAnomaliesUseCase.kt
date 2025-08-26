package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.TransactionRepository
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Use case for detecting spending anomalies and unusual transaction patterns
 */
class DetectSpendingAnomaliesUseCase(
    private val transactionRepository: TransactionRepository
) {

    /**
     * Detect spending anomalies within a given date range
     * 
     * @param startDate Start of the analysis period
     * @param endDate End of the analysis period
     * @return Result containing list of detected anomalies
     */
    suspend operator fun invoke(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Result<List<DetectedAnomaly>> = runCatching {
        // Get transactions for the analysis period
        val periodTransactions = transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .filter { it.type == Transaction.Type.EXPENSE }
        
        if (periodTransactions.isEmpty()) {
            return@runCatching emptyList()
        }
        
        // Get historical data for baseline comparison (3 months before start date)
        val historicalStartDate = LocalDateTime(
            startDate.year,
            startDate.monthNumber - 3,
            startDate.dayOfMonth,
            startDate.hour,
            startDate.minute
        )
        val historicalTransactions = transactionRepository.getTransactionsByDateRange(
            historicalStartDate, 
            startDate
        ).filter { it.type == Transaction.Type.EXPENSE }
        
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // Detect different types of anomalies
        anomalies.addAll(detectHighAmountAnomalies(periodTransactions, historicalTransactions))
        anomalies.addAll(detectCategorySpendingAnomalies(periodTransactions, historicalTransactions))
        anomalies.addAll(detectFrequentMerchantAnomalies(periodTransactions))
        anomalies.addAll(detectTimePatternAnomalies(periodTransactions, historicalTransactions))
        anomalies.addAll(detectDuplicateTransactions(periodTransactions))
        anomalies.addAll(detectLocationAnomalies(periodTransactions, historicalTransactions))
        
        // Sort by severity and return
        anomalies.sortedByDescending { it.severity.priority }
    }

    private fun detectHighAmountAnomalies(
        periodTransactions: List<Transaction>,
        historicalTransactions: List<Transaction>
    ): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // Group transactions by category
        val categoryGroups = (periodTransactions + historicalTransactions).groupBy { it.categoryId }
        
        categoryGroups.forEach { (categoryId, transactions) ->
            if (transactions.size < 3) return@forEach // Need minimum transactions for analysis
            
            val amounts = transactions.map { it.amount.amount }
            val mean = amounts.average()
            val standardDeviation = calculateStandardDeviation(amounts, mean)
            
            if (standardDeviation == 0.0) return@forEach // All amounts are the same
            
            // Check transactions in current period
            val currentPeriodTransactions = transactions.filter { transaction ->
                transaction.date >= periodTransactions.firstOrNull()?.date ?: LocalDateTime(2024, 1, 1, 0, 0) &&
                transaction.date <= periodTransactions.lastOrNull()?.date ?: LocalDateTime(2024, 12, 31, 23, 59)
            }
            
            currentPeriodTransactions.forEach { transaction ->
                val zScore = (transaction.amount.amount - mean) / standardDeviation
                
                when {
                    zScore > 3.0 -> { // More than 3 standard deviations
                        anomalies.add(
                            DetectedAnomaly(
                                transactionId = transaction.id,
                                type = DetectedAnomaly.Type.UNUSUALLY_HIGH_AMOUNT,
                                severity = DetectedAnomaly.Severity.HIGH,
                                description = "Transaction of ${transaction.amount.amount.let { "%.2f".format(it) }} SAR in '$categoryId' is unusually high (${zScore.let { "%.1f".format(it) }}x above average)",
                                affectedTransaction = transaction,
                                detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                                confidence = minOf(1.0, zScore / 5.0) // Cap at 100%
                            )
                        )
                    }
                    zScore > 2.0 -> { // More than 2 standard deviations
                        anomalies.add(
                            DetectedAnomaly(
                                transactionId = transaction.id,
                                type = DetectedAnomaly.Type.UNUSUALLY_HIGH_AMOUNT,
                                severity = DetectedAnomaly.Severity.MEDIUM,
                                description = "Transaction of ${transaction.amount.amount.let { "%.2f".format(it) }} SAR in '$categoryId' is higher than usual",
                                affectedTransaction = transaction,
                                detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                                confidence = zScore / 3.0
                            )
                        )
                    }
                }
            }
        }
        
        return anomalies
    }
    
    private fun detectCategorySpendingAnomalies(
        periodTransactions: List<Transaction>,
        historicalTransactions: List<Transaction>
    ): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // Calculate monthly spending by category for historical data
        val historicalMonthlySpendings = historicalTransactions
            .groupBy { "${it.date.year}-${it.date.monthNumber.toString().padStart(2, '0')}-${it.categoryId}" }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount.amount } }
            .groupBy { it.key.split("-")[2] } // Group by category
            .mapValues { (_, monthlyTotals) -> monthlyTotals.map { it.value } }
        
        // Calculate current period spending by category
        val currentCategorySpendings = periodTransactions.groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount.amount } }
        
        currentCategorySpendings.forEach { (categoryId, currentSpending) ->
            val historicalSpendings = historicalMonthlySpendings[categoryId]
            if (historicalSpendings == null || historicalSpendings.isEmpty()) return@forEach
            
            val historicalMean = historicalSpendings.average()
            val historicalStdDev = calculateStandardDeviation(historicalSpendings, historicalMean)
            
            if (historicalStdDev == 0.0) return@forEach
            
            val zScore = (currentSpending - historicalMean) / historicalStdDev
            
            if (zScore > 2.5) {
                anomalies.add(
                    DetectedAnomaly(
                        transactionId = "", // Category-level anomaly
                        type = DetectedAnomaly.Type.UNUSUALLY_HIGH_CATEGORY_SPENDING,
                        severity = if (zScore > 3.5) DetectedAnomaly.Severity.HIGH else DetectedAnomaly.Severity.MEDIUM,
                        description = "Spending in '$categoryId' category (${currentSpending.let { "%.2f".format(it) }} SAR) is significantly higher than historical average (${historicalMean.let { "%.2f".format(it) }} SAR)",
                        affectedTransaction = null,
                        detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                        confidence = minOf(1.0, zScore / 4.0)
                    )
                )
            }
        }
        
        return anomalies
    }
    
    private fun detectFrequentMerchantAnomalies(
        periodTransactions: List<Transaction>
    ): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // Group by merchant and date to detect unusual frequency
        val merchantTransactions = periodTransactions.groupBy { it.merchantName }
        
        merchantTransactions.forEach { (merchantName, transactions) ->
            if (transactions.size < 3) return@forEach
            
            // Check for multiple transactions on the same day
            val dailyTransactions = transactions.groupBy { 
                "${it.date.year}-${it.date.monthNumber}-${it.date.dayOfMonth}" 
            }
            
            dailyTransactions.forEach { (date, dayTransactions) ->
                if (dayTransactions.size >= 4) {
                    // Multiple transactions at same merchant on same day
                    val timeWindow = dayTransactions.maxOf { it.date.hour * 60 + it.date.minute } - 
                                   dayTransactions.minOf { it.date.hour * 60 + it.date.minute }
                    
                    if (timeWindow <= 60) { // Within 1 hour
                        anomalies.add(
                            DetectedAnomaly(
                                transactionId = dayTransactions.last().id,
                                type = DetectedAnomaly.Type.FREQUENT_MERCHANT_TRANSACTIONS,
                                severity = DetectedAnomaly.Severity.MEDIUM,
                                description = "Unusual pattern: ${dayTransactions.size} transactions at '$merchantName' within ${timeWindow} minutes on $date",
                                affectedTransaction = dayTransactions.last(),
                                detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                                confidence = if (timeWindow <= 30) 0.9 else 0.7
                            )
                        )
                    }
                }
            }
        }
        
        return anomalies
    }
    
    private fun detectTimePatternAnomalies(
        periodTransactions: List<Transaction>,
        historicalTransactions: List<Transaction>
    ): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // Analyze historical time patterns
        val historicalHours = historicalTransactions.map { it.date.hour }
        val unusualTimeThreshold = if (historicalHours.isNotEmpty()) {
            // Consider hours outside of typical pattern as unusual
            val hourCounts = historicalHours.groupingBy { it }.eachCount()
            val lowActivityHours = hourCounts.filter { it.value < historicalHours.size * 0.02 }.keys
            lowActivityHours
        } else {
            // Default unusual hours if no historical data
            setOf(0, 1, 2, 3, 4, 5, 23)
        }
        
        // Check current transactions for unusual times
        periodTransactions.forEach { transaction ->
            val hour = transaction.date.hour
            
            if (hour in unusualTimeThreshold && transaction.amount.amount > 100.0) {
                val severity = when {
                    hour in 1..4 -> DetectedAnomaly.Severity.HIGH // Very late night
                    transaction.amount.amount > 500.0 -> DetectedAnomaly.Severity.HIGH // High amount at unusual time
                    else -> DetectedAnomaly.Severity.MEDIUM
                }
                
                anomalies.add(
                    DetectedAnomaly(
                        transactionId = transaction.id,
                        type = DetectedAnomaly.Type.UNUSUAL_TIME_PATTERN,
                        severity = severity,
                        description = "Transaction of ${transaction.amount.amount.let { "%.2f".format(it) }} SAR at unusual time (${hour}:${transaction.date.minute.toString().padStart(2, '0')})",
                        affectedTransaction = transaction,
                        detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                        confidence = if (hour in 1..4) 0.9 else 0.6
                    )
                )
            }
        }
        
        // Analyze weekend vs weekday patterns
        val weekdayTransactions = periodTransactions.filter { 
            it.date.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) 
        }
        val weekendTransactions = periodTransactions.filter { 
            it.date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) 
        }
        
        if (weekdayTransactions.isNotEmpty() && weekendTransactions.isNotEmpty()) {
            val weekdayAvg = weekdayTransactions.map { it.amount.amount }.average()
            val weekendAvg = weekendTransactions.map { it.amount.amount }.average()
            
            if (weekendAvg > weekdayAvg * 3) { // Weekend spending 3x higher
                anomalies.add(
                    DetectedAnomaly(
                        transactionId = "",
                        type = DetectedAnomaly.Type.UNUSUAL_TIME_PATTERN,
                        severity = DetectedAnomaly.Severity.MEDIUM,
                        description = "Weekend spending (${weekendAvg.let { "%.2f".format(it) }} SAR avg) significantly higher than weekday spending (${weekdayAvg.let { "%.2f".format(it) }} SAR avg)",
                        affectedTransaction = null,
                        detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                        confidence = 0.8
                    )
                )
            }
        }
        
        return anomalies
    }
    
    private fun detectDuplicateTransactions(
        periodTransactions: List<Transaction>
    ): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // Sort by date to check adjacent transactions
        val sortedTransactions = periodTransactions.sortedBy { it.date }
        
        for (i in 0 until sortedTransactions.size - 1) {
            val current = sortedTransactions[i]
            val next = sortedTransactions[i + 1]
            
            // Check for potential duplicates
            val amountMatch = abs(current.amount.amount - next.amount.amount) < 0.01
            val merchantMatch = current.merchantName == next.merchantName
            val categoryMatch = current.categoryId == next.categoryId
            
            val timeDifferenceMinutes = abs(
                (next.date.hour * 60 + next.date.minute) - 
                (current.date.hour * 60 + current.date.minute)
            )
            
            if (amountMatch && merchantMatch && categoryMatch && timeDifferenceMinutes <= 5) {
                anomalies.add(
                    DetectedAnomaly(
                        transactionId = next.id,
                        type = DetectedAnomaly.Type.POTENTIAL_DUPLICATE,
                        severity = DetectedAnomaly.Severity.HIGH,
                        description = "Potential duplicate transaction: ${next.amount.amount.let { "%.2f".format(it) }} SAR at '${next.merchantName}' ${timeDifferenceMinutes} minutes after similar transaction",
                        affectedTransaction = next,
                        detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                        confidence = if (timeDifferenceMinutes <= 2) 0.95 else 0.8
                    )
                )
            }
        }
        
        return anomalies
    }
    
    private fun detectLocationAnomalies(
        periodTransactions: List<Transaction>,
        historicalTransactions: List<Transaction>
    ): List<DetectedAnomaly> {
        val anomalies = mutableListOf<DetectedAnomaly>()
        
        // This is a simplified location anomaly detection
        // In a real implementation, you would use actual location data
        
        val historicalMerchants = historicalTransactions.map { it.merchantName }.toSet()
        val newMerchants = periodTransactions.filter { it.merchantName !in historicalMerchants }
        
        newMerchants.forEach { transaction ->
            if (transaction.amount.amount > 200.0) { // High amount at new location
                anomalies.add(
                    DetectedAnomaly(
                        transactionId = transaction.id,
                        type = DetectedAnomaly.Type.UNUSUAL_LOCATION,
                        severity = if (transaction.amount.amount > 500.0) 
                            DetectedAnomaly.Severity.HIGH else DetectedAnomaly.Severity.MEDIUM,
                        description = "High-value transaction (${transaction.amount.amount.let { "%.2f".format(it) }} SAR) at new merchant '${transaction.merchantName}'",
                        affectedTransaction = transaction,
                        detectedAt = LocalDateTime(2024, 3, 15, 12, 0),
                        confidence = 0.7
                    )
                )
            }
        }
        
        return anomalies
    }
    
    private fun calculateStandardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
}

/**
 * Data class representing a detected spending anomaly
 */
data class DetectedAnomaly(
    val transactionId: String, // Empty for category-level anomalies
    val type: Type,
    val severity: Severity,
    val description: String,
    val affectedTransaction: Transaction?,
    val detectedAt: LocalDateTime,
    val confidence: Double // 0.0 to 1.0
) {
    enum class Type {
        UNUSUALLY_HIGH_AMOUNT,
        UNUSUALLY_HIGH_CATEGORY_SPENDING,
        FREQUENT_MERCHANT_TRANSACTIONS,
        UNUSUAL_TIME_PATTERN,
        POTENTIAL_DUPLICATE,
        UNUSUAL_LOCATION
    }
    
    enum class Severity(val priority: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3)
    }
}