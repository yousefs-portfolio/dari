package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.usecase.base.BaseUseCase

/**
 * Search transactions by amount range use case
 * Extracted from SearchTransactionsUseCase to follow Single Responsibility Principle
 */
class SearchByAmountRangeUseCase(
    private val transactionRepository: TransactionRepository
) : BaseUseCase() {

    /**
     * Search transactions by amount range
     * 
     * @param accountId The account ID to search within
     * @param minAmount Minimum transaction amount (absolute value)
     * @param maxAmount Maximum transaction amount (absolute value)
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend operator fun invoke(
        accountId: String,
        minAmount: Money,
        maxAmount: Money,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return execute {
            validateAccountId(accountId)
            validateAmountRange(minAmount, maxAmount)
            validatePagination(limit, offset)

            // Get all transactions for the account and filter by amount
            val allTransactions = transactionRepository.getTransactionsByAccount(
                accountId = accountId,
                limit = limit * 2, // Get more to account for filtering
                offset = offset
            )
            
            // Filter by amount range
            val filteredTransactions = allTransactions.filter { transaction ->
                val amount = transaction.amount.toDouble()
                amount >= minAmount.toDouble() && amount <= maxAmount.toDouble()
            }.take(limit)
            
            filteredTransactions
        }
    }

    private fun validateAmountRange(minAmount: Money, maxAmount: Money) {
        require(minAmount.currency == maxAmount.currency) {
            "Currency mismatch: ${minAmount.currency} vs ${maxAmount.currency}"
        }
        
        require(minAmount.toDouble() >= 0) {
            "Minimum amount must be non-negative"
        }
        
        require(maxAmount.toDouble() >= minAmount.toDouble()) {
            "Maximum amount must be greater than or equal to minimum amount"
        }
        
        require(maxAmount.toDouble() <= 1_000_000) {
            "Maximum amount cannot exceed 1,000,000"
        }
    }
}