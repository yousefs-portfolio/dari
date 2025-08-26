package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Use case for splitting transactions into multiple parts
 * Supports various splitting strategies: fixed amounts, percentages, equal splits
 * Includes Saudi-specific splitting scenarios (Ramadan, charity, etc.)
 */
class SplitTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Split transaction by fixed amounts
     */
    suspend operator fun invoke(request: SplitTransactionRequest): Result<SplitTransactionResult> {
        return try {
            // Validate request
            if (request.splits.isEmpty()) {
                return Result.Error(IllegalArgumentException("At least one split is required"))
            }

            // Get original transaction
            val transactionResult = transactionRepository.getTransactionById(request.originalTransactionId)
            if (transactionResult is Result.Error) {
                return transactionResult
            }

            val originalTransaction = (transactionResult as Result.Success).data

            // Validate splits
            validateFixedAmountSplits(request.splits, originalTransaction.amount)

            // Create split transactions
            val splitTransactions = createSplitTransactions(
                originalTransaction = originalTransaction,
                splits = request.splits.map { split ->
                    SplitTransactionData(
                        amount = split.amount,
                        category = split.category,
                        description = split.description,
                        tags = split.tags
                    )
                }
            )

            // Save split transactions
            val createResult = transactionRepository.createTransactions(splitTransactions)
            if (createResult is Result.Error) {
                return createResult
            }

            val createdTransactions = (createResult as Result.Success).data

            // Delete original transaction if not keeping it
            if (!request.keepOriginal) {
                val deleteResult = transactionRepository.deleteTransaction(request.originalTransactionId)
                if (deleteResult is Result.Error) {
                    return deleteResult
                }
            }

            Result.Success(
                SplitTransactionResult(
                    originalTransaction = if (request.keepOriginal) originalTransaction else null,
                    splitTransactions = createdTransactions
                )
            )

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Split transaction by percentages
     */
    suspend fun splitByPercentage(request: SplitTransactionByPercentageRequest): Result<SplitTransactionResult> {
        return try {
            // Validate percentages
            val totalPercentage = request.percentageSplits.sumOf { it.percentage }
            if (abs(totalPercentage - 100.0) > 0.01) {
                return Result.Error(IllegalArgumentException("Percentages must sum to 100, got $totalPercentage"))
            }

            // Get original transaction
            val transactionResult = transactionRepository.getTransactionById(request.originalTransactionId)
            if (transactionResult is Result.Error) {
                return transactionResult
            }

            val originalTransaction = (transactionResult as Result.Success).data

            // Convert percentages to amounts
            val splits = request.percentageSplits.map { percentageSplit ->
                val splitAmount = (originalTransaction.amount.value * percentageSplit.percentage / 100.0)
                SplitTransactionData(
                    amount = Money(splitAmount, originalTransaction.amount.currency),
                    category = percentageSplit.category,
                    description = percentageSplit.description,
                    tags = percentageSplit.tags
                )
            }

            // Adjust for rounding errors to ensure total matches exactly
            val adjustedSplits = adjustForRoundingErrors(splits, originalTransaction.amount)

            // Create split transactions
            val splitTransactions = createSplitTransactions(originalTransaction, adjustedSplits)

            // Save split transactions
            val createResult = transactionRepository.createTransactions(splitTransactions)
            if (createResult is Result.Error) {
                return createResult
            }

            val createdTransactions = (createResult as Result.Success).data

            // Delete original transaction if not keeping it
            if (!request.keepOriginal) {
                val deleteResult = transactionRepository.deleteTransaction(request.originalTransactionId)
                if (deleteResult is Result.Error) {
                    return deleteResult
                }
            }

            Result.Success(
                SplitTransactionResult(
                    originalTransaction = if (request.keepOriginal) originalTransaction else null,
                    splitTransactions = createdTransactions
                )
            )

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Split transaction equally
     */
    suspend fun splitEqually(request: SplitTransactionEquallyRequest): Result<SplitTransactionResult> {
        return try {
            if (request.numberOfSplits <= 0) {
                return Result.Error(IllegalArgumentException("Number of splits must be greater than 0"))
            }

            // Get original transaction
            val transactionResult = transactionRepository.getTransactionById(request.originalTransactionId)
            if (transactionResult is Result.Error) {
                return transactionResult
            }

            val originalTransaction = (transactionResult as Result.Success).data

            // Calculate equal amount per split
            val amountPerSplit = originalTransaction.amount.value / request.numberOfSplits

            // Create splits
            val splits = (0 until request.numberOfSplits).map { index ->
                SplitTransactionData(
                    amount = Money(amountPerSplit, originalTransaction.amount.currency),
                    category = request.categories.getOrNull(index) ?: originalTransaction.category,
                    description = request.descriptions.getOrNull(index) ?: "${originalTransaction.description} (Split ${index + 1})",
                    tags = request.tags.getOrNull(index) ?: originalTransaction.tags
                )
            }

            // Adjust for rounding errors
            val adjustedSplits = adjustForRoundingErrors(splits, originalTransaction.amount)

            // Create split transactions
            val splitTransactions = createSplitTransactions(originalTransaction, adjustedSplits)

            // Save split transactions
            val createResult = transactionRepository.createTransactions(splitTransactions)
            if (createResult is Result.Error) {
                return createResult
            }

            val createdTransactions = (createResult as Result.Success).data

            // Delete original transaction if not keeping it
            if (!request.keepOriginal) {
                val deleteResult = transactionRepository.deleteTransaction(request.originalTransactionId)
                if (deleteResult is Result.Error) {
                    return deleteResult
                }
            }

            Result.Success(
                SplitTransactionResult(
                    originalTransaction = if (request.keepOriginal) originalTransaction else null,
                    splitTransactions = createdTransactions
                )
            )

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun validateFixedAmountSplits(splits: List<TransactionSplit>, originalAmount: Money) {
        // Check all splits have the same currency
        val currencies = splits.map { it.amount.currency }.distinct()
        if (currencies.size > 1) {
            throw IllegalArgumentException("All split amounts must have the same currency as the original transaction")
        }

        if (currencies.isNotEmpty() && currencies.first() != originalAmount.currency) {
            throw IllegalArgumentException("All split amounts must have the same currency as the original transaction")
        }

        // Check total amount matches
        val totalSplitAmount = splits.sumOf { it.amount.value }
        if (abs(totalSplitAmount - originalAmount.value) > 0.01) {
            throw IllegalArgumentException(
                "Split amounts must equal original amount. Expected: ${originalAmount.value}, Got: $totalSplitAmount"
            )
        }
    }

    private fun adjustForRoundingErrors(splits: List<SplitTransactionData>, originalAmount: Money): List<SplitTransactionData> {
        val totalSplitAmount = splits.sumOf { it.amount.value }
        val difference = originalAmount.value - totalSplitAmount

        if (abs(difference) <= 0.01) {
            // Add the difference to the largest split
            val largestSplitIndex = splits.withIndex().maxByOrNull { it.value.amount.value }?.index ?: 0
            return splits.mapIndexed { index, split ->
                if (index == largestSplitIndex) {
                    split.copy(
                        amount = Money(split.amount.value + difference, split.amount.currency)
                    )
                } else {
                    split
                }
            }
        }

        return splits
    }

    private fun createSplitTransactions(
        originalTransaction: Transaction,
        splits: List<SplitTransactionData>
    ): List<Transaction> {
        return splits.mapIndexed { index, split ->
            originalTransaction.copy(
                id = "${originalTransaction.id}_split_${index + 1}",
                amount = split.amount,
                category = split.category,
                description = split.description,
                tags = split.tags,
                parentTransactionId = originalTransaction.id
            )
        }
    }
}

/**
 * Request for splitting transaction by fixed amounts
 */
data class SplitTransactionRequest(
    val originalTransactionId: String,
    val splits: List<TransactionSplit>,
    val keepOriginal: Boolean = false
)

/**
 * Request for splitting transaction by percentages
 */
data class SplitTransactionByPercentageRequest(
    val originalTransactionId: String,
    val percentageSplits: List<TransactionSplitPercentage>,
    val keepOriginal: Boolean = false
)

/**
 * Request for splitting transaction equally
 */
data class SplitTransactionEquallyRequest(
    val originalTransactionId: String,
    val numberOfSplits: Int,
    val categories: List<TransactionCategory> = emptyList(),
    val descriptions: List<String> = emptyList(),
    val tags: List<List<String>> = emptyList(),
    val keepOriginal: Boolean = false
)

/**
 * Split definition with fixed amount
 */
data class TransactionSplit(
    val amount: Money,
    val category: TransactionCategory,
    val description: String,
    val tags: List<String> = emptyList()
)

/**
 * Split definition with percentage
 */
data class TransactionSplitPercentage(
    val percentage: Double,
    val category: TransactionCategory,
    val description: String,
    val tags: List<String> = emptyList()
)

/**
 * Internal data class for split processing
 */
private data class SplitTransactionData(
    val amount: Money,
    val category: TransactionCategory,
    val description: String,
    val tags: List<String>
)

/**
 * Result of transaction splitting
 */
data class SplitTransactionResult(
    val originalTransaction: Transaction?, // null if original was deleted
    val splitTransactions: List<Transaction>
)