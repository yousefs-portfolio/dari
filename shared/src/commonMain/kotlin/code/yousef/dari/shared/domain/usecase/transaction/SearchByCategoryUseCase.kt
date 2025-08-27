package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.usecase.base.BaseUseCase

/**
 * Search transactions by category use case
 * Extracted from SearchTransactionsUseCase to follow Single Responsibility Principle
 */
class SearchByCategoryUseCase(
    private val transactionRepository: TransactionRepository
) : BaseUseCase() {

    /**
     * Search transactions by category
     * 
     * @param accountId The account ID to search within
     * @param categoryId The category ID to search for
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend operator fun invoke(
        accountId: String,
        categoryId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return execute {
            validateAccountId(accountId)
            validateCategoryId(categoryId)
            validatePagination(limit, offset)

            transactionRepository.searchByCategory(
                accountId = accountId,
                category = categoryId,
                limit = limit,
                offset = offset
            )
        }
    }
}