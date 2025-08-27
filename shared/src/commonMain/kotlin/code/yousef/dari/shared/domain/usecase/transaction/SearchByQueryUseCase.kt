package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.usecase.base.BaseUseCase

/**
 * Search transactions by query string use case
 * Extracted from SearchTransactionsUseCase to follow Single Responsibility Principle
 */
class SearchByQueryUseCase(
    private val transactionRepository: TransactionRepository
) : BaseUseCase() {

    /**
     * Search transactions by query string
     * 
     * @param accountId The account ID to search within
     * @param query The search query (supports Arabic and English)
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend operator fun invoke(
        accountId: String,
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return execute {
            validateAccountId(accountId)
            validateQuery(query)
            validatePagination(limit, offset)

            transactionRepository.searchTransactions(
                accountId = accountId,
                query = query,
                limit = limit,
                offset = offset,
                sortBy = "date_desc"
            )
        }
    }
}