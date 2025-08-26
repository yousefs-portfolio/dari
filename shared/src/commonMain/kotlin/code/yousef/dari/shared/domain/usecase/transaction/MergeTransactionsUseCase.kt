package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result
import kotlinx.datetime.Clock
import kotlin.math.abs

/**
 * Use case for merging duplicate transactions
 * Supports Saudi-specific duplicate detection and Arabic/English text merging
 */
class MergeTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Merge specified transactions
     */
    suspend operator fun invoke(request: MergeTransactionsRequest): Result<MergeTransactionsResult> {
        return try {
            // Validate request
            if (request.transactionIds.size < 2) {
                return Result.Error(IllegalArgumentException("At least two transactions are required for merging"))
            }

            // Get transactions to merge
            val transactionsResult = transactionRepository.getTransactionsByIds(request.transactionIds)
            if (transactionsResult is Result.Error) {
                return transactionsResult
            }

            val transactions = (transactionsResult as Result.Success).data

            // Validate transactions can be merged
            validateTransactionsCanBeMerged(transactions)

            // Create merged transaction based on strategy
            val mergedTransaction = createMergedTransaction(transactions, request.mergeStrategy)

            // Save merged transaction
            val createResult = transactionRepository.createTransaction(mergedTransaction)
            if (createResult is Result.Error) {
                return createResult
            }

            val savedMergedTransaction = (createResult as Result.Success).data

            // Delete original transactions if not keeping them
            if (!request.keepOriginals) {
                val deleteResult = transactionRepository.deleteTransactions(request.transactionIds)
                if (deleteResult is Result.Error) {
                    return deleteResult
                }
            }

            Result.Success(
                MergeTransactionsResult(
                    mergedTransaction = savedMergedTransaction,
                    originalTransactions = transactions
                )
            )

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Find duplicate transactions in an account
     */
    suspend fun findDuplicateTransactions(accountId: String): Result<List<DuplicateTransactionGroup>> {
        return try {
            val transactionsResult = transactionRepository.getTransactionsByAccountId(accountId)
            if (transactionsResult is Result.Error) {
                return transactionsResult
            }

            val transactions = (transactionsResult as Result.Success).data
            val duplicateGroups = detectDuplicateGroups(transactions)

            Result.Success(duplicateGroups)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun validateTransactionsCanBeMerged(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            throw IllegalArgumentException("No transactions provided for merging")
        }

        // All transactions must be from the same account
        val accounts = transactions.map { it.accountId }.distinct()
        if (accounts.size > 1) {
            throw IllegalArgumentException("Transactions must be from the same account")
        }

        // All transactions must have the same currency
        val currencies = transactions.map { it.amount.currency }.distinct()
        if (currencies.size > 1) {
            throw IllegalArgumentException("Transactions must have the same currency, found different currencies: $currencies")
        }

        // All transactions must be the same type (income/expense)
        val types = transactions.map { it.type }.distinct()
        if (types.size > 1) {
            throw IllegalArgumentException("Transactions must be the same type (all income or all expense)")
        }
    }

    private fun createMergedTransaction(transactions: List<Transaction>, strategy: MergeStrategy): Transaction {
        val baseTransaction = when (strategy) {
            MergeStrategy.KEEP_EARLIEST -> transactions.minByOrNull { it.date }!!
            MergeStrategy.KEEP_LATEST -> transactions.maxByOrNull { it.date }!!
            MergeStrategy.KEEP_MOST_DETAILED -> selectMostDetailedTransaction(transactions)
        }

        return baseTransaction.copy(
            id = "merged_${Clock.System.now().epochSeconds}_${(0..999).random()}",
            description = mergeDescriptions(transactions),
            merchant = mergeMerchants(transactions),
            location = mergeLocations(transactions),
            tags = mergeTags(transactions),
            receipt = mergeReceipts(transactions),
            mergedTransactionIds = transactions.map { it.id }
        )
    }

    private fun selectMostDetailedTransaction(transactions: List<Transaction>): Transaction {
        return transactions.maxByOrNull { transaction ->
            var score = 0
            if (transaction.merchant != null) score += 2
            if (transaction.location != null) score += 2
            if (transaction.receipt != null) score += 3
            if (transaction.tags.isNotEmpty()) score += 1
            if (transaction.description.length > 10) score += 1
            score
        } ?: transactions.first()
    }

    private fun mergeDescriptions(transactions: List<Transaction>): String {
        val descriptions = transactions.map { it.description.trim() }.distinct().filter { it.isNotEmpty() }
        return when {
            descriptions.size == 1 -> descriptions.first()
            descriptions.size <= 3 -> descriptions.joinToString(" / ")
            else -> "${descriptions.take(2).joinToString(" / ")} and ${descriptions.size - 2} others"
        }
    }

    private fun mergeMerchants(transactions: List<Transaction>): Merchant? {
        val merchants = transactions.mapNotNull { it.merchant }.distinctBy { it.name }
        return when {
            merchants.isEmpty() -> null
            merchants.size == 1 -> merchants.first()
            else -> {
                // Merge merchant names
                val names = merchants.map { it.name }.distinct()
                val categories = merchants.map { it.category }.distinct()
                Merchant(
                    id = "merged_${merchants.first().id}",
                    name = if (names.size <= 2) names.joinToString(" / ") else "${names.first()} (+${names.size - 1} variants)",
                    category = categories.firstOrNull() ?: merchants.first().category
                )
            }
        }
    }

    private fun mergeLocations(transactions: List<Transaction>): TransactionLocation? {
        val locations = transactions.mapNotNull { it.location }.distinctBy { "${it.latitude},${it.longitude}" }
        return when {
            locations.isEmpty() -> null
            locations.size == 1 -> locations.first()
            else -> {
                // Use the most detailed location (longest address)
                locations.maxByOrNull { it.address?.length ?: 0 }
            }
        }
    }

    private fun mergeTags(transactions: List<Transaction>): List<String> {
        return transactions.flatMap { it.tags }.distinct().sorted()
    }

    private fun mergeReceipts(transactions: List<Transaction>): Receipt? {
        val receipts = transactions.mapNotNull { it.receipt }
        return when {
            receipts.isEmpty() -> null
            receipts.size == 1 -> receipts.first()
            else -> {
                // Keep the receipt with highest OCR confidence or most extracted data
                receipts.maxByOrNull { receipt ->
                    (receipt.ocrConfidence ?: 0.0) + (receipt.extractedData.size * 0.1)
                }
            }
        }
    }

    private fun detectDuplicateGroups(transactions: List<Transaction>): List<DuplicateTransactionGroup> {
        val duplicateGroups = mutableListOf<DuplicateTransactionGroup>()
        val processed = mutableSetOf<String>()

        for (transaction in transactions) {
            if (transaction.id in processed) continue

            val duplicates = findDuplicatesForTransaction(transaction, transactions)
            if (duplicates.size > 1) {
                val confidence = calculateDuplicateConfidence(duplicates)
                duplicateGroups.add(
                    DuplicateTransactionGroup(
                        transactions = duplicates,
                        confidence = confidence
                    )
                )
                processed.addAll(duplicates.map { it.id })
            }
        }

        return duplicateGroups.sortedByDescending { it.confidence }
    }

    private fun findDuplicatesForTransaction(target: Transaction, allTransactions: List<Transaction>): List<Transaction> {
        return allTransactions.filter { candidate ->
            candidate.accountId == target.accountId &&
            areDuplicateTransactions(target, candidate)
        }
    }

    private fun areDuplicateTransactions(transaction1: Transaction, transaction2: Transaction): Boolean {
        if (transaction1.id == transaction2.id) return true

        // Same amount and currency
        if (transaction1.amount != transaction2.amount) return false

        // Same type
        if (transaction1.type != transaction2.type) return false

        // Similar time (within 10 minutes)
        val timeDifferenceMinutes = abs(transaction1.date.toEpochSecond() - transaction2.date.toEpochSecond()) / 60
        if (timeDifferenceMinutes > 10) return false

        // Similar description or merchant
        val descriptionSimilarity = calculateTextSimilarity(transaction1.description, transaction2.description)
        val merchantSimilarity = if (transaction1.merchant != null && transaction2.merchant != null) {
            calculateTextSimilarity(transaction1.merchant.name, transaction2.merchant.name)
        } else {
            0.0
        }

        return descriptionSimilarity > 0.7 || merchantSimilarity > 0.7
    }

    private fun calculateTextSimilarity(text1: String, text2: String): Double {
        val normalized1 = normalizeText(text1)
        val normalized2 = normalizeText(text2)

        if (normalized1 == normalized2) return 1.0

        // Simple similarity based on common words
        val words1 = normalized1.split(" ").filter { it.length > 2 }
        val words2 = normalized2.split(" ").filter { it.length > 2 }

        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val commonWords = words1.intersect(words2.toSet()).size
        val totalWords = (words1.size + words2.size) / 2.0

        return commonWords / totalWords
    }

    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF\\s]"), "") // Keep Arabic, English, numbers, spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun calculateDuplicateConfidence(transactions: List<Transaction>): Double {
        if (transactions.size < 2) return 0.0

        var totalConfidence = 0.0
        var comparisons = 0

        for (i in transactions.indices) {
            for (j in i + 1 until transactions.size) {
                val confidence = calculatePairConfidence(transactions[i], transactions[j])
                totalConfidence += confidence
                comparisons++
            }
        }

        return if (comparisons > 0) totalConfidence / comparisons else 0.0
    }

    private fun calculatePairConfidence(t1: Transaction, t2: Transaction): Double {
        var confidence = 0.0

        // Exact amount match
        if (t1.amount == t2.amount) confidence += 0.3

        // Time proximity (closer = higher confidence)
        val timeDiffMinutes = abs(t1.date.toEpochSecond() - t2.date.toEpochSecond()) / 60
        confidence += when {
            timeDiffMinutes <= 1 -> 0.3
            timeDiffMinutes <= 5 -> 0.2
            timeDiffMinutes <= 10 -> 0.1
            else -> 0.0
        }

        // Description similarity
        val descSimilarity = calculateTextSimilarity(t1.description, t2.description)
        confidence += descSimilarity * 0.2

        // Merchant similarity
        if (t1.merchant != null && t2.merchant != null) {
            val merchantSimilarity = calculateTextSimilarity(t1.merchant.name, t2.merchant.name)
            confidence += merchantSimilarity * 0.2
        }

        return confidence.coerceIn(0.0, 1.0)
    }
}

/**
 * Extension function to convert LocalDateTime to epoch seconds (simplified)
 */
private fun kotlinx.datetime.LocalDateTime.toEpochSecond(): Long {
    // Simplified conversion - in real implementation use proper timezone conversion
    return this.year * 365L * 24 * 3600 +
           this.monthNumber * 30L * 24 * 3600 +
           this.dayOfMonth * 24L * 3600 +
           this.hour * 3600L +
           this.minute * 60L +
           this.second
}

/**
 * Request for merging transactions
 */
data class MergeTransactionsRequest(
    val transactionIds: List<String>,
    val mergeStrategy: MergeStrategy,
    val keepOriginals: Boolean = false
)

/**
 * Merge strategy enumeration
 */
enum class MergeStrategy {
    KEEP_EARLIEST,      // Keep the earliest transaction's details
    KEEP_LATEST,        // Keep the latest transaction's details
    KEEP_MOST_DETAILED  // Keep the transaction with most complete information
}

/**
 * Result of merging transactions
 */
data class MergeTransactionsResult(
    val mergedTransaction: Transaction,
    val originalTransactions: List<Transaction>
)

/**
 * Group of duplicate transactions
 */
data class DuplicateTransactionGroup(
    val transactions: List<Transaction>,
    val confidence: Double
)