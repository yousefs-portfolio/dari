package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.domain.models.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Transaction Repository Interface
 * Defines contract for transaction data operations
 * Supports offline-first architecture with sync capabilities
 */
interface TransactionRepository {
    
    /**
     * Get all transactions for an account as a flow
     */
    fun getTransactionsFlow(accountId: String): Flow<List<Transaction>>
    
    /**
     * Get transactions for account with pagination
     */
    suspend fun getTransactionsPaginated(
        accountId: String,
        limit: Int,
        offset: Int
    ): List<Transaction>
    
    /**
     * Get transactions by date range
     */
    suspend fun getTransactionsByDateRange(
        accountId: String,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction>
    
    /**
     * Get transaction by ID
     */
    suspend fun getTransactionById(transactionId: String): Transaction?
    
    /**
     * Get recent transactions across all accounts
     */
    suspend fun getRecentTransactions(limit: Int): List<Transaction>
    
    /**
     * Get recent transactions for last N days across all accounts
     */
    suspend fun getRecentTransactionsByDays(days: Int): List<Transaction>
    
    /**
     * Search transactions by text
     */
    suspend fun searchTransactions(query: String): List<Transaction>
    
    /**
     * Get transactions by category
     */
    suspend fun getTransactionsByCategory(categoryId: String): List<Transaction>
    
    /**
     * Get transactions by merchant
     */
    suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction>
    
    /**
     * Insert or update transaction
     */
    suspend fun upsertTransaction(transaction: Transaction)
    
    /**
     * Insert or update multiple transactions
     */
    suspend fun upsertTransactions(transactions: List<Transaction>)
    
    /**
     * Update transaction category
     */
    suspend fun updateTransactionCategory(
        transactionId: String,
        categoryId: String,
        categoryName: String,
        subcategoryName: String?
    )
    
    /**
     * Delete transaction by ID
     */
    suspend fun deleteTransaction(transactionId: String)
    
    /**
     * Delete transactions by account
     */
    suspend fun deleteTransactionsByAccount(accountId: String)
    
    /**
     * Sync transactions with remote API
     */
    suspend fun syncTransactions(accountId: String): Result<List<Transaction>>
    
    /**
     * Get transactions that need syncing
     */
    suspend fun getTransactionsNeedingSync(): List<Transaction>
    
    /**
     * Mark transactions as synced
     */
    suspend fun markTransactionsSynced(transactionIds: List<String>)
}