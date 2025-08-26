package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Transaction

/**
 * Sync Transactions Use Case
 * Synchronizes transaction data from SAMA Open Banking APIs
 * Handles offline-first synchronization with conflict resolution
 */
class SyncTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Sync transactions for a specific account
     */
    suspend operator fun invoke(accountId: String): Result<List<Transaction>> {
        return try {
            val result = transactionRepository.syncTransactions(accountId)
            if (result.isSuccess) {
                val transactions = result.getOrNull() ?: emptyList()
                // Mark transactions as synced
                val transactionIds = transactions.map { it.transactionId }
                transactionRepository.markTransactionsSynced(transactionIds)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync all pending transactions
     */
    suspend fun syncPending(): Result<List<Transaction>> {
        return try {
            val transactionsNeedingSync = transactionRepository.getTransactionsNeedingSync()
            val allSyncedTransactions = mutableListOf<Transaction>()
            
            // Group by account ID and sync each account
            transactionsNeedingSync.groupBy { it.accountId }.forEach { (accountId, transactions) ->
                val result = transactionRepository.syncTransactions(accountId)
                if (result.isSuccess) {
                    val syncedTransactions = result.getOrNull() ?: emptyList()
                    allSyncedTransactions.addAll(syncedTransactions)
                    
                    // Mark as synced
                    val transactionIds = transactions.map { it.transactionId }
                    transactionRepository.markTransactionsSynced(transactionIds)
                }
            }
            
            Result.success(allSyncedTransactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}