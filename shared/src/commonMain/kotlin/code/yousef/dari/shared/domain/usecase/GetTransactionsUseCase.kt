package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Get Transactions Use Case
 * Retrieves account transactions with proper business logic and filtering
 */
class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * Get transactions for an account as a flow
     */
    operator fun invoke(accountId: String): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsFlow(accountId)
    }
    
    /**
     * Get transactions with pagination
     */
    suspend fun getPaginated(
        accountId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<Transaction> {
        return transactionRepository.getTransactionsPaginated(accountId, limit, offset)
    }
    
    /**
     * Get transactions by date range
     */
    suspend fun getByDateRange(
        accountId: String,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> {
        return transactionRepository.getTransactionsByDateRange(accountId, startDate, endDate)
    }
    
    /**
     * Search transactions
     */
    suspend fun search(query: String): List<Transaction> {
        return transactionRepository.searchTransactions(query)
    }
}