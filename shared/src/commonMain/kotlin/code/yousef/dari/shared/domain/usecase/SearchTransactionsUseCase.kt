package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionType
import code.yousef.dari.shared.domain.usecase.base.BaseUseCase
import code.yousef.dari.shared.domain.usecase.transaction.SearchByQueryUseCase
import code.yousef.dari.shared.domain.usecase.transaction.SearchByCategoryUseCase
import code.yousef.dari.shared.domain.usecase.transaction.SearchByAmountRangeUseCase
import kotlinx.datetime.LocalDate

/**
 * Search Transactions Use Case
 * Handles comprehensive transaction search functionality with various filters and criteria
 * Supports Arabic language search and Saudi-specific transaction patterns
 * 
 * Refactored to use extracted smaller use cases following SRP
 */
class SearchTransactionsUseCase(
    private val searchByQueryUseCase: SearchByQueryUseCase,
    private val searchByCategoryUseCase: SearchByCategoryUseCase,
    private val searchByAmountRangeUseCase: SearchByAmountRangeUseCase
) : BaseUseCase() {

    /**
     * Search transactions by query string
     */
    suspend operator fun invoke(
        accountId: String,
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return searchByQueryUseCase(accountId, query, limit, offset)
    }

    /**
     * Search transactions by category
     */
    suspend fun searchByCategory(
        accountId: String,
        category: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return searchByCategoryUseCase(accountId, category, limit, offset)
    }

    /**
     * Search transactions by amount range
     */
    suspend fun searchByAmountRange(
        accountId: String,
        minAmount: Money,
        maxAmount: Money,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return searchByAmountRangeUseCase(accountId, minAmount, maxAmount, limit, offset)
    }

    /**
     * Search transactions by date range
     * 
     * @param accountId The account ID to search within
     * @param startDate Start date for the search range
     * @param endDate End date for the search range
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend fun searchByDateRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return try {
            validateAccountId(accountId)
            
            if (startDate > endDate) {
                throw IllegalArgumentException("Start date cannot be after end date")
            }

            val transactions = transactionRepository.searchByDateRange(
                accountId = accountId,
                startDate = startDate,
                endDate = endDate,
                limit = limit,
                offset = offset
            )

            Result.success(transactions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search transactions by transaction type
     * 
     * @param accountId The account ID to search within
     * @param type The transaction type to filter by
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend fun searchByType(
        accountId: String,
        type: TransactionType,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return try {
            validateAccountId(accountId)

            val transactions = transactionRepository.searchByType(
                accountId = accountId,
                type = type,
                limit = limit,
                offset = offset
            )

            Result.success(transactions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search transactions by merchant name
     * 
     * @param accountId The account ID to search within
     * @param merchantName The merchant name to search for
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend fun searchByMerchant(
        accountId: String,
        merchantName: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return try {
            validateAccountId(accountId)
            
            if (merchantName.isBlank()) {
                throw IllegalArgumentException("Merchant name cannot be empty")
            }

            val transactions = transactionRepository.searchByMerchant(
                accountId = accountId,
                merchantName = merchantName,
                limit = limit,
                offset = offset
            )

            Result.success(transactions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Advanced search with multiple filters
     * 
     * @param accountId The account ID to search within
     * @param filters Search filters to apply
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip (for pagination)
     * @return Result containing list of matching transactions
     */
    suspend fun advancedSearch(
        accountId: String,
        filters: SearchFilters,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> {
        return try {
            validateAccountId(accountId)
            validateSearchFilters(filters)

            val transactions = transactionRepository.advancedSearch(
                accountId = accountId,
                filters = filters,
                limit = limit,
                offset = offset
            )

            Result.success(transactions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search transactions with pagination support
     * 
     * @param accountId The account ID to search within
     * @param query The search query
     * @param limit Maximum number of results per page
     * @param offset Number of results to skip
     * @return Result containing list of matching transactions
     */
    suspend fun searchWithPagination(
        accountId: String,
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<Transaction>> {
        return invoke(accountId, query, limit, offset)
    }

    /**
     * Search across multiple accounts
     * 
     * @param accountIds List of account IDs to search within
     * @param query The search query
     * @param limit Maximum number of results per account
     * @return Result containing map of account ID to transactions
     */
    suspend fun searchMultipleAccounts(
        accountIds: List<String>,
        query: String,
        limit: Int = 50
    ): Result<Map<String, List<Transaction>>> {
        return try {
            if (accountIds.isEmpty()) {
                return Result.success(emptyMap())
            }

            validateQuery(query)

            val results = mutableMapOf<String, List<Transaction>>()

            accountIds.forEach { accountId ->
                try {
                    validateAccountId(accountId)
                    val transactions = transactionRepository.searchTransactions(
                        accountId = accountId,
                        query = query,
                        limit = limit,
                        offset = 0,
                        sortBy = "date_desc"
                    )
                    results[accountId] = transactions
                } catch (e: Exception) {
                    // Log error but continue with other accounts
                    println("Warning: Failed to search in account $accountId: ${e.message}")
                    results[accountId] = emptyList()
                }
            }

            Result.success(results)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search transactions by reference number
     * 
     * @param accountId The account ID to search within
     * @param reference The transaction reference number
     * @return Result containing list of matching transactions
     */
    suspend fun searchByReference(
        accountId: String,
        reference: String
    ): Result<List<Transaction>> {
        return try {
            validateAccountId(accountId)
            
            if (reference.isBlank()) {
                throw IllegalArgumentException("Reference cannot be empty")
            }

            val transactions = transactionRepository.searchByReference(accountId, reference)
            Result.success(transactions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find similar transactions based on patterns
     * 
     * @param baseTransactionId The transaction ID to find similar transactions for
     * @param similarityThreshold Similarity threshold (0.0 to 1.0)
     * @return Result containing list of similar transactions
     */
    suspend fun findSimilarTransactions(
        baseTransactionId: String,
        similarityThreshold: Double = 0.8
    ): Result<List<Transaction>> {
        return try {
            if (baseTransactionId.isBlank()) {
                throw IllegalArgumentException("Base transaction ID cannot be empty")
            }
            
            if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
                throw IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0")
            }

            val transactions = transactionRepository.findSimilarTransactions(
                baseTransactionId,
                similarityThreshold
            )
            Result.success(transactions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search recent transactions (last 30 days by default)
     * 
     * @param accountId The account ID to search within
     * @param query The search query
     * @param days Number of recent days to search within
     * @return Result containing list of recent matching transactions
     */
    suspend fun searchRecent(
        accountId: String,
        query: String,
        days: Int = 30
    ): Result<List<Transaction>> {
        return try {
            validateAccountId(accountId)
            validateQuery(query)
            
            if (days <= 0) {
                throw IllegalArgumentException("Days must be positive")
            }

            // Calculate date range for recent search
            val endDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            val startDate = endDate.minus(kotlinx.datetime.DatePeriod(days = days))

            searchByDateRange(accountId, startDate, endDate)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get search suggestions based on query
     * 
     * @param accountId The account ID to get suggestions for
     * @param partialQuery Partial search query
     * @param maxSuggestions Maximum number of suggestions to return
     * @return Result containing list of search suggestions
     */
    suspend fun getSearchSuggestions(
        accountId: String,
        partialQuery: String,
        maxSuggestions: Int = 10
    ): Result<List<String>> {
        return try {
            validateAccountId(accountId)
            
            if (partialQuery.length < 2) {
                return Result.success(emptyList())
            }

            val suggestions = transactionRepository.getSearchSuggestions(
                accountId,
                partialQuery,
                maxSuggestions
            )
            Result.success(suggestions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate account ID format
     */
    private fun validateAccountId(accountId: String) {
        if (accountId.isBlank()) {
            throw IllegalArgumentException("Account ID cannot be empty")
        }
    }

    /**
     * Validate search query
     */
    private fun validateQuery(query: String) {
        if (query.isBlank()) {
            throw IllegalArgumentException("Search query cannot be empty")
        }
    }

    /**
     * Validate search filters
     */
    private fun validateSearchFilters(filters: SearchFilters) {
        filters.startDate?.let { startDate ->
            filters.endDate?.let { endDate ->
                if (startDate > endDate) {
                    throw IllegalArgumentException("Start date cannot be after end date")
                }
            }
        }
        
        filters.minAmount?.let { minAmount ->
            filters.maxAmount?.let { maxAmount ->
                if (minAmount.amount > maxAmount.amount) {
                    throw IllegalArgumentException("Minimum amount cannot be greater than maximum amount")
                }
            }
        }
    }
}

/**
 * Search filters for advanced transaction search
 */
data class SearchFilters(
    val query: String? = null,
    val categories: List<String> = emptyList(),
    val types: List<TransactionType> = emptyList(),
    val merchants: List<String> = emptyList(),
    val minAmount: Money? = null,
    val maxAmount: Money? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val tags: List<String> = emptyList(),
    val hasReceipt: Boolean? = null,
    val isRecurring: Boolean? = null,
    val currency: String? = null
) {
    /**
     * Check if any filters are applied
     */
    fun hasFilters(): Boolean {
        return query != null ||
                categories.isNotEmpty() ||
                types.isNotEmpty() ||
                merchants.isNotEmpty() ||
                minAmount != null ||
                maxAmount != null ||
                startDate != null ||
                endDate != null ||
                tags.isNotEmpty() ||
                hasReceipt != null ||
                isRecurring != null ||
                currency != null
    }

    /**
     * Get a human-readable description of active filters
     */
    fun getDescription(): String {
        val descriptions = mutableListOf<String>()
        
        query?.let { descriptions.add("Query: '$it'") }
        if (categories.isNotEmpty()) descriptions.add("Categories: ${categories.joinToString(", ")}")
        if (types.isNotEmpty()) descriptions.add("Types: ${types.joinToString(", ")}")
        if (merchants.isNotEmpty()) descriptions.add("Merchants: ${merchants.joinToString(", ")}")
        minAmount?.let { descriptions.add("Min Amount: $it") }
        maxAmount?.let { descriptions.add("Max Amount: $it") }
        startDate?.let { descriptions.add("From: $it") }
        endDate?.let { descriptions.add("To: $it") }
        if (tags.isNotEmpty()) descriptions.add("Tags: ${tags.joinToString(", ")}")
        hasReceipt?.let { descriptions.add("Has Receipt: $it") }
        isRecurring?.let { descriptions.add("Recurring: $it") }
        currency?.let { descriptions.add("Currency: $it") }
        
        return descriptions.joinToString(", ")
    }
}