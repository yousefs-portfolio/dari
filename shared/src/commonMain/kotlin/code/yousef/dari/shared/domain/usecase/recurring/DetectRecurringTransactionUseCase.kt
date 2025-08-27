package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.TransactionRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.until
import kotlin.math.abs

enum class RecurringFrequency(val days: Int, val displayName: String) {
    WEEKLY(7, "Weekly"),
    BIWEEKLY(14, "Bi-weekly"),
    MONTHLY(30, "Monthly"),
    QUARTERLY(90, "Quarterly"),
    YEARLY(365, "Yearly")
}

data class RecurringPattern(
    val merchantName: String,
    val amount: Money,
    val frequency: RecurringFrequency,
    val detectedTransactions: List<Transaction>,
    val confidence: Double,
    val hasVariableAmount: Boolean = false,
    val averageAmount: Money? = null,
    val nextPredictedDate: Instant? = null
)

class DetectRecurringTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    companion object {
        private const val MIN_OCCURRENCES = 3
        private const val AMOUNT_TOLERANCE_PERCENT = 0.15 // 15% tolerance for amount variation
        private const val DATE_TOLERANCE_DAYS = 3 // 3 days tolerance for date variation
        private const val MIN_CONFIDENCE_THRESHOLD = 0.7
    }

    suspend fun execute(): List<RecurringPattern> {
        val allTransactions = transactionRepository.getAllTransactions()
        return detectRecurringPatterns(allTransactions)
    }

    private fun detectRecurringPatterns(transactions: List<Transaction>): List<RecurringPattern> {
        val patterns = mutableListOf<RecurringPattern>()

        // Group transactions by merchant
        val transactionsByMerchant = transactions
            .filter { it.merchantName.isNotBlank() }
            .groupBy { it.merchantName }

        for ((merchantName, merchantTransactions) in transactionsByMerchant) {
            if (merchantTransactions.size < MIN_OCCURRENCES) continue

            // Sort transactions by date
            val sortedTransactions = merchantTransactions.sortedBy { it.date }

            // Try different frequencies
            RecurringFrequency.values().forEach { frequency ->
                val pattern = analyzeFrequencyPattern(merchantName, sortedTransactions, frequency)
                if (pattern != null && pattern.confidence >= MIN_CONFIDENCE_THRESHOLD) {
                    patterns.add(pattern)
                }
            }
        }

        // Return patterns sorted by confidence
        return patterns.sortedByDescending { it.confidence }
    }

    private fun analyzeFrequencyPattern(
        merchantName: String,
        transactions: List<Transaction>,
        frequency: RecurringFrequency
    ): RecurringPattern? {
        if (transactions.size < MIN_OCCURRENCES) return null

        val patternTransactions = mutableListOf<Transaction>()
        val intervals = mutableListOf<Long>()
        val amounts = mutableListOf<Money>()

        // Start with the first transaction
        patternTransactions.add(transactions.first())
        amounts.add(transactions.first().amount)

        var previousTransaction = transactions.first()

        for (i in 1 until transactions.size) {
            val currentTransaction = transactions[i]
            val daysDiff = previousTransaction.date.until(currentTransaction.date, DateTimeUnit.DAY)

            // Check if this transaction fits the pattern
            if (isWithinDateTolerance(daysDiff, frequency.days) &&
                isWithinAmountTolerance(currentTransaction.amount, amounts)
            ) {

                patternTransactions.add(currentTransaction)
                amounts.add(currentTransaction.amount)
                intervals.add(daysDiff)
                previousTransaction = currentTransaction
            }
        }

        // Need at least MIN_OCCURRENCES to consider it a pattern
        if (patternTransactions.size < MIN_OCCURRENCES) return null

        val confidence = calculateConfidence(intervals, frequency, amounts)
        if (confidence < MIN_CONFIDENCE_THRESHOLD) return null

        val hasVariableAmount = hasSignificantAmountVariation(amounts)
        val averageAmount = if (hasVariableAmount) calculateAverageAmount(amounts) else amounts.first()
        val nextPredictedDate = predictNextDate(patternTransactions.last().date, frequency)

        return RecurringPattern(
            merchantName = merchantName,
            amount = averageAmount,
            frequency = frequency,
            detectedTransactions = patternTransactions,
            confidence = confidence,
            hasVariableAmount = hasVariableAmount,
            averageAmount = if (hasVariableAmount) averageAmount else null,
            nextPredictedDate = nextPredictedDate
        )
    }

    private fun isWithinDateTolerance(actualDays: Long, expectedDays: Int): Boolean {
        return abs(actualDays - expectedDays) <= DATE_TOLERANCE_DAYS
    }

    private fun isWithinAmountTolerance(amount: Money, existingAmounts: List<Money>): Boolean {
        if (existingAmounts.isEmpty()) return true

        val averageAmount = calculateAverageAmount(existingAmounts)
        val tolerance = averageAmount.amount * AMOUNT_TOLERANCE_PERCENT.toBigDecimal()
        val difference = abs(amount.amount.toDouble() - averageAmount.amount.toDouble())

        return difference <= tolerance.toDouble()
    }

    private fun calculateConfidence(
        intervals: List<Long>,
        frequency: RecurringFrequency,
        amounts: List<Money>
    ): Double {
        if (intervals.isEmpty()) return 0.0

        // Date consistency score (0.0 to 0.6)
        val averageInterval = intervals.average()
        val intervalVariance = intervals.map { abs(it - averageInterval) }.average()
        val dateScore = maxOf(0.0, 0.6 - (intervalVariance / frequency.days) * 0.6)

        // Amount consistency score (0.0 to 0.3)
        val amountVariance = calculateAmountVariance(amounts)
        val amountScore = maxOf(0.0, 0.3 - amountVariance * 0.3)

        // Frequency match score (0.0 to 0.1)
        val frequencyScore = maxOf(0.0, 0.1 - abs(averageInterval - frequency.days) / frequency.days * 0.1)

        return dateScore + amountScore + frequencyScore
    }

    private fun calculateAmountVariance(amounts: List<Money>): Double {
        if (amounts.isEmpty()) return 0.0

        val average = calculateAverageAmount(amounts)
        val variance = amounts.map {
            abs(it.amount.toDouble() - average.amount.toDouble()) / average.amount.toDouble()
        }.average()

        return variance
    }

    private fun hasSignificantAmountVariation(amounts: List<Money>): Boolean {
        if (amounts.size < 2) return false

        val variance = calculateAmountVariance(amounts)
        return variance > AMOUNT_TOLERANCE_PERCENT / 2
    }

    private fun calculateAverageAmount(amounts: List<Money>): Money {
        if (amounts.isEmpty()) return Money(0.toBigDecimal(), "SAR")

        val currency = amounts.first().currency
        val average = amounts.map { it.amount }.reduce { acc, amount -> acc + amount } / amounts.size.toBigDecimal()

        return Money(average, currency)
    }

    private fun predictNextDate(lastDate: Instant, frequency: RecurringFrequency): Instant {
        return lastDate.plus(frequency.days, DateTimeUnit.DAY)
    }
}
