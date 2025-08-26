package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.CategoryRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.CategoryMatch

/**
 * Categorize Transaction Use Case
 * Automatically categorizes transactions using ML and rules
 * Handles manual categorization and learning
 */
class CategorizeTransactionUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Automatically categorize a transaction
     */
    suspend fun categorizeTransaction(transaction: Transaction): Result<CategoryMatch?> {
        return try {
            val match = categoryRepository.findBestMatch(
                description = transaction.description,
                merchantName = transaction.merchantName,
                amount = transaction.amount,
                accountId = transaction.accountId
            )
            
            if (match.isSuccess && match.getOrNull() != null) {
                val categoryMatch = match.getOrNull()!!
                
                // Update transaction with suggested category if confidence is high
                if (categoryMatch.confidence >= 80) {
                    val updatedTransaction = transaction.copy(
                        categoryId = categoryMatch.category.categoryId,
                        categoryName = categoryMatch.category.name
                    )
                    transactionRepository.upsertTransaction(updatedTransaction)
                }
            }
            
            match
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get categorization suggestions for a transaction
     */
    suspend fun getSuggestions(
        transaction: Transaction,
        limit: Int = 5
    ): Result<List<CategoryMatch>> {
        return try {
            categoryRepository.getMatchingSuggestions(
                description = transaction.description,
                merchantName = transaction.merchantName,
                amount = transaction.amount,
                limit = limit
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Manually categorize a transaction
     */
    suspend fun manuallyCategorizTransaction(
        transactionId: String,
        categoryId: String,
        categoryName: String,
        subcategoryName: String? = null,
        confidence: Int = 100
    ): Result<Unit> {
        return try {
            // Update transaction category
            transactionRepository.updateTransactionCategory(
                transactionId = transactionId,
                categoryId = categoryId,
                categoryName = categoryName,
                subcategoryName = subcategoryName
            )
            
            // Get transaction for learning
            val transaction = transactionRepository.getTransactionById(transactionId)
            transaction.getOrNull()?.let { txn ->
                // Learn from manual categorization
                categoryRepository.learnFromCategorization(
                    transactionDescription = txn.description,
                    merchantName = txn.merchantName,
                    amount = txn.amount,
                    selectedCategoryId = categoryId,
                    confidence = confidence
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk categorize transactions
     */
    suspend fun bulkCategorize(transactionIds: List<String>, categoryId: String): Result<Unit> {
        return try {
            // Get category details
            val categoryResult = categoryRepository.getCategoryById(categoryId)
            val category = categoryResult.getOrNull() 
                ?: return Result.failure(Exception("Category not found"))
            
            // Update all transactions
            transactionIds.forEach { transactionId ->
                transactionRepository.updateTransactionCategory(
                    transactionId = transactionId,
                    categoryId = categoryId,
                    categoryName = category.name,
                    subcategoryName = null
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Auto-categorize uncategorized transactions
     */
    suspend fun autoCategorizeUncategorized(): Result<Int> {
        return try {
            var categorizedCount = 0
            
            // This would typically get uncategorized transactions from repository
            // For now, we'll assume there's a method to get them
            // val uncategorizedTransactions = transactionRepository.getUncategorizedTransactions()
            
            // uncategorizedTransactions.forEach { transaction ->
            //     val match = categorizeTransaction(transaction)
            //     if (match.isSuccess && match.getOrNull()?.confidence ?: 0 >= 70) {
            //         categorizedCount++
            //     }
            // }
            
            Result.success(categorizedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}