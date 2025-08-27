package code.yousef.dari.shared.domain.usecase.subscription

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.TransactionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlin.math.abs

enum class SubscriptionCategory {
    STREAMING,
    MUSIC,
    GAMING,
    PRODUCTIVITY,
    FITNESS,
    NEWS,
    TELECOM,
    CLOUD_STORAGE,
    EDUCATION,
    FINANCE,
    FOOD_DELIVERY,
    TRANSPORTATION,
    UTILITIES,
    OTHER
}

enum class SubscriptionFrequency(val days: Int, val displayName: String) {
    WEEKLY(7, "Weekly"),
    MONTHLY(30, "Monthly"),
    QUARTERLY(90, "Quarterly"),
    YEARLY(365, "Yearly")
}

data class DetectedSubscription(
    val serviceName: String,
    val merchantName: String,
    val category: SubscriptionCategory,
    val frequency: SubscriptionFrequency,
    val monthlyAmount: Money,
    val actualAmount: Money,
    val isActive: Boolean,
    val confidence: Double,
    val hasVariableAmount: Boolean,
    val transactionHistory: List<Transaction>,
    val firstPaymentDate: Instant,
    val lastPaymentDate: Instant?,
    val nextExpectedDate: Instant?,
    val totalPaid: Money,
    val averageInterval: Double,
    val tags: List<String> = emptyList()
)

class DetectSubscriptionUseCase(
    private val transactionRepository: TransactionRepository
) {
    companion object {
        private const val MIN_OCCURRENCES = 2 // Minimum for annual, 3+ for others
        private const val AMOUNT_TOLERANCE_PERCENT = 0.20 // 20% tolerance for amount variation
        private const val DATE_TOLERANCE_DAYS = 7 // 7 days tolerance for payment dates
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6
        private const val ACTIVE_SUBSCRIPTION_DAYS = 45 // Consider active if payment within 45 days
    }

    private val subscriptionPatterns = mapOf(
        // Streaming services
        "netflix" to Pair("Netflix", SubscriptionCategory.STREAMING),
        "amazon prime" to Pair("Amazon Prime", SubscriptionCategory.STREAMING),
        "disney" to Pair("Disney+", SubscriptionCategory.STREAMING),
        "hbo" to Pair("HBO Max", SubscriptionCategory.STREAMING),
        "hulu" to Pair("Hulu", SubscriptionCategory.STREAMING),
        "shahid" to Pair("Shahid VIP", SubscriptionCategory.STREAMING),

        // Music services
        "spotify" to Pair("Spotify", SubscriptionCategory.MUSIC),
        "apple music" to Pair("Apple Music", SubscriptionCategory.MUSIC),
        "anghami" to Pair("Anghami", SubscriptionCategory.MUSIC),
        "youtube music" to Pair("YouTube Music", SubscriptionCategory.MUSIC),

        // Productivity
        "microsoft" to Pair("Microsoft 365", SubscriptionCategory.PRODUCTIVITY),
        "adobe" to Pair("Adobe Creative Cloud", SubscriptionCategory.PRODUCTIVITY),
        "dropbox" to Pair("Dropbox", SubscriptionCategory.CLOUD_STORAGE),
        "google one" to Pair("Google One", SubscriptionCategory.CLOUD_STORAGE),

        // Telecom
        "stc" to Pair("STC Pay", SubscriptionCategory.TELECOM),
        "mobily" to Pair("Mobily", SubscriptionCategory.TELECOM),
        "zain" to Pair("Zain", SubscriptionCategory.TELECOM),

        // Fitness
        "fitness" to Pair("Fitness First", SubscriptionCategory.FITNESS),
        "gym" to Pair("Gym Membership", SubscriptionCategory.FITNESS),

        // Food delivery
        "hungerstation" to Pair("HungerStation Plus", SubscriptionCategory.FOOD_DELIVERY),
        "careem" to Pair("Careem Plus", SubscriptionCategory.TRANSPORTATION),

        // Gaming
        "playstation" to Pair("PlayStation Plus", SubscriptionCategory.GAMING),
        "xbox" to Pair("Xbox Game Pass", SubscriptionCategory.GAMING),
        "steam" to Pair("Steam", SubscriptionCategory.GAMING)
    )

    suspend fun execute(): List<DetectedSubscription> {
        val allTransactions = transactionRepository.getAllTransactions()
        return detectSubscriptions(allTransactions.filter { it.amount.amount < 0.toBigDecimal() }) // Only expenses
    }

    private fun detectSubscriptions(transactions: List<Transaction>): List<DetectedSubscription> {
        val subscriptions = mutableListOf<DetectedSubscription>()

        // Group transactions by merchant (case-insensitive)
        val transactionsByMerchant = transactions
            .filter { it.merchantName.isNotBlank() }
            .groupBy { it.merchantName.lowercase().trim() }

        for ((merchantKey, merchantTransactions) in transactionsByMerchant) {
            if (merchantTransactions.size < MIN_OCCURRENCES) continue

            // Sort transactions by date
            val sortedTransactions = merchantTransactions.sortedBy { it.date }

            // Try different frequencies
            SubscriptionFrequency.values().forEach { frequency ->
                val subscription = analyzeSubscriptionPattern(merchantKey, sortedTransactions, frequency)
                if (subscription != null && subscription.confidence >= MIN_CONFIDENCE_THRESHOLD) {
                    subscriptions.add(subscription)
                }
            }
        }

        // Return subscriptions sorted by confidence and activity
        return subscriptions
            .sortedWith(compareByDescending<DetectedSubscription> { it.isActive }
                .thenByDescending { it.confidence })
    }

    private fun analyzeSubscriptionPattern(
        merchantKey: String,
        transactions: List<Transaction>,
        frequency: SubscriptionFrequency
    ): DetectedSubscription? {
        if (transactions.size < getMinOccurrencesForFrequency(frequency)) return null

        val intervals = mutableListOf<Long>()
        val amounts = mutableListOf<Money>()
        val validTransactions = mutableListOf<Transaction>()

        // Analyze transaction patterns
        for (i in 0 until transactions.size - 1) {
            val current = transactions[i]
            val next = transactions[i + 1]

            val daysDiff = current.date.until(next.date, DateTimeUnit.DAY)
            val amountDiff = abs(current.amount.amount.toDouble() - next.amount.amount.toDouble())

            // Check if this interval fits the expected frequency
            if (isWithinDateTolerance(daysDiff, frequency.days) &&
                isWithinAmountTolerance(current.amount, next.amount)
            ) {

                intervals.add(daysDiff)
                amounts.add(current.amount)

                if (validTransactions.isEmpty()) {
                    validTransactions.add(current)
                }
                validTransactions.add(next)
            }
        }

        if (validTransactions.size < getMinOccurrencesForFrequency(frequency)) return null

        val confidence = calculateSubscriptionConfidence(intervals, frequency, amounts)
        if (confidence < MIN_CONFIDENCE_THRESHOLD) return null

        // Determine service details
        val serviceName = determineServiceName(merchantKey, transactions.first().merchantName)
        val category = determineSubscriptionCategory(serviceName, merchantKey)

        val averageAmount = calculateAverageAmount(amounts)
        val monthlyAmount = normalizeToMonthlyAmount(averageAmount, frequency)
        val hasVariableAmount = hasSignificantAmountVariation(amounts)

        val now = Clock.System.now()
        val lastPayment = validTransactions.maxByOrNull { it.date }
        val isActive = lastPayment?.date?.let { lastDate ->
            now.minus(ACTIVE_SUBSCRIPTION_DAYS, DateTimeUnit.DAY) <= lastDate
        } ?: false

        val nextExpectedDate = lastPayment?.date?.let { lastDate ->
            if (isActive) lastDate.plus(frequency.days, DateTimeUnit.DAY) else null
        }

        val totalPaid = Money(
            amounts.sumOf { it.amount.abs() },
            amounts.first().currency
        )

        return DetectedSubscription(
            serviceName = serviceName,
            merchantName = transactions.first().merchantName,
            category = category,
            frequency = frequency,
            monthlyAmount = monthlyAmount,
            actualAmount = averageAmount,
            isActive = isActive,
            confidence = confidence,
            hasVariableAmount = hasVariableAmount,
            transactionHistory = validTransactions,
            firstPaymentDate = validTransactions.minOf { it.date },
            lastPaymentDate = validTransactions.maxOfOrNull { it.date },
            nextExpectedDate = nextExpectedDate,
            totalPaid = totalPaid,
            averageInterval = intervals.average(),
            tags = generateSubscriptionTags(serviceName, category, isActive)
        )
    }

    private fun determineServiceName(merchantKey: String, originalMerchantName: String): String {
        subscriptionPatterns.forEach { (pattern, serviceInfo) ->
            if (merchantKey.contains(pattern)) {
                return serviceInfo.first
            }
        }
        return originalMerchantName.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun determineSubscriptionCategory(serviceName: String, merchantKey: String): SubscriptionCategory {
        subscriptionPatterns.forEach { (pattern, serviceInfo) ->
            if (merchantKey.contains(pattern)) {
                return serviceInfo.second
            }
        }

        // Fallback category detection based on keywords
        return when {
            merchantKey.contains("tv") || merchantKey.contains("video") -> SubscriptionCategory.STREAMING
            merchantKey.contains("music") || merchantKey.contains("audio") -> SubscriptionCategory.MUSIC
            merchantKey.contains("office") || merchantKey.contains("productivity") -> SubscriptionCategory.PRODUCTIVITY
            merchantKey.contains("cloud") || merchantKey.contains("storage") -> SubscriptionCategory.CLOUD_STORAGE
            merchantKey.contains("mobile") || merchantKey.contains("telecom") -> SubscriptionCategory.TELECOM
            merchantKey.contains("food") || merchantKey.contains("delivery") -> SubscriptionCategory.FOOD_DELIVERY
            merchantKey.contains("transport") || merchantKey.contains("ride") -> SubscriptionCategory.TRANSPORTATION
            merchantKey.contains("game") || merchantKey.contains("gaming") -> SubscriptionCategory.GAMING
            merchantKey.contains("news") || merchantKey.contains("magazine") -> SubscriptionCategory.NEWS
            else -> SubscriptionCategory.OTHER
        }
    }

    private fun isWithinDateTolerance(actualDays: Long, expectedDays: Int): Boolean {
        return abs(actualDays - expectedDays) <= DATE_TOLERANCE_DAYS
    }

    private fun isWithinAmountTolerance(amount1: Money, amount2: Money): Boolean {
        val avg = (amount1.amount.abs() + amount2.amount.abs()) / 2.toBigDecimal()
        val tolerance = avg * AMOUNT_TOLERANCE_PERCENT.toBigDecimal()
        val difference = (amount1.amount.abs() - amount2.amount.abs()).abs()
        return difference <= tolerance
    }

    private fun calculateSubscriptionConfidence(
        intervals: List<Long>,
        frequency: SubscriptionFrequency,
        amounts: List<Money>
    ): Double {
        if (intervals.isEmpty()) return 0.0

        // Date regularity score (0.0 to 0.5)
        val averageInterval = intervals.average()
        val intervalVariance = intervals.map { abs(it - averageInterval) }.average()
        val dateScore = maxOf(0.0, 0.5 - (intervalVariance / frequency.days) * 0.5)

        // Amount consistency score (0.0 to 0.3)
        val amountVariance = calculateAmountVariance(amounts)
        val amountScore = maxOf(0.0, 0.3 - amountVariance * 0.3)

        // Frequency match score (0.0 to 0.2)
        val frequencyScore = maxOf(0.0, 0.2 - abs(averageInterval - frequency.days) / frequency.days * 0.2)

        return dateScore + amountScore + frequencyScore
    }

    private fun calculateAmountVariance(amounts: List<Money>): Double {
        if (amounts.isEmpty()) return 0.0

        val average = calculateAverageAmount(amounts)
        return amounts.map {
            abs(it.amount.toDouble() - average.amount.toDouble()) / average.amount.toDouble()
        }.average()
    }

    private fun hasSignificantAmountVariation(amounts: List<Money>): Boolean {
        if (amounts.size < 2) return false
        return calculateAmountVariance(amounts) > AMOUNT_TOLERANCE_PERCENT / 2
    }

    private fun calculateAverageAmount(amounts: List<Money>): Money {
        if (amounts.isEmpty()) return Money(0.toBigDecimal(), "SAR")

        val currency = amounts.first().currency
        val average =
            amounts.map { it.amount.abs() }.reduce { acc, amount -> acc + amount } / amounts.size.toBigDecimal()

        return Money(average, currency)
    }

    private fun normalizeToMonthlyAmount(amount: Money, frequency: SubscriptionFrequency): Money {
        val monthlyAmount = when (frequency) {
            SubscriptionFrequency.WEEKLY -> amount.amount * 4.toBigDecimal()
            SubscriptionFrequency.MONTHLY -> amount.amount
            SubscriptionFrequency.QUARTERLY -> amount.amount / 3.toBigDecimal()
            SubscriptionFrequency.YEARLY -> amount.amount / 12.toBigDecimal()
        }

        return Money(monthlyAmount, amount.currency)
    }

    private fun getMinOccurrencesForFrequency(frequency: SubscriptionFrequency): Int {
        return when (frequency) {
            SubscriptionFrequency.WEEKLY -> 4 // At least 4 weeks
            SubscriptionFrequency.MONTHLY -> 3 // At least 3 months
            SubscriptionFrequency.QUARTERLY -> 2 // At least 2 quarters
            SubscriptionFrequency.YEARLY -> 2 // At least 2 years
        }
    }

    private fun generateSubscriptionTags(
        serviceName: String,
        category: SubscriptionCategory,
        isActive: Boolean
    ): List<String> {
        val tags = mutableListOf<String>()

        tags.add("subscription")
        tags.add(category.name.lowercase())

        if (isActive) {
            tags.add("active")
        } else {
            tags.add("inactive")
        }

        // Add service-specific tags
        when {
            serviceName.contains("Netflix", ignoreCase = true) -> tags.add("entertainment")
            serviceName.contains("Spotify", ignoreCase = true) -> tags.add("music")
            serviceName.contains("Microsoft", ignoreCase = true) -> tags.add("business")
            serviceName.contains("Adobe", ignoreCase = true) -> tags.add("creative")
        }

        return tags.distinct()
    }
}
