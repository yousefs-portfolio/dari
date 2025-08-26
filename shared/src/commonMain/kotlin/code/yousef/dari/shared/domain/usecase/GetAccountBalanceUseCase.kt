package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.Money

/**
 * Get Account Balance Use Case
 * Handles retrieving account balances with various options for fresh data,
 * historical data, and multiple account aggregation
 */
class GetAccountBalanceUseCase(
    private val accountRepository: AccountRepository
) {

    /**
     * Get current cached balance for an account
     * 
     * @param accountId The account ID to get balance for
     * @return Result containing the account balance or error
     */
    suspend operator fun invoke(accountId: String): Result<Money> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            if (!account.isActive) {
                return Result.failure(Exception("Account is inactive: $accountId"))
            }

            Result.success(account.balance)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get fresh balance from the bank (forces remote update)
     * 
     * @param accountId The account ID to refresh balance for
     * @return Result containing the fresh account balance or error
     */
    suspend fun getFreshBalance(accountId: String): Result<Money> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            if (!account.isActive) {
                return Result.failure(Exception("Account is inactive: $accountId"))
            }

            // Refresh balance from remote source
            val refreshResult = accountRepository.refreshAccountBalance(accountId)
            if (refreshResult.isFailure) {
                // Fall back to cached balance if refresh fails
                return Result.success(account.balance)
            }

            refreshResult

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available balance (current balance minus holds and pending transactions)
     * 
     * @param accountId The account ID to get available balance for
     * @return Result containing the available balance or error
     */
    suspend fun getAvailableBalance(accountId: String): Result<Money> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            if (!account.isActive) {
                return Result.failure(Exception("Account is inactive: $accountId"))
            }

            // Get available balance (which considers holds and pending transactions)
            val availableResult = accountRepository.getAvailableBalance(accountId)
            if (availableResult.isFailure) {
                // Fall back to current balance if available balance cannot be determined
                return Result.success(account.balance)
            }

            availableResult

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get balances for multiple accounts
     * 
     * @param accountIds List of account IDs to get balances for
     * @return Result containing map of account ID to balance
     */
    suspend fun getMultipleBalances(accountIds: List<String>): Result<Map<String, Money>> {
        return try {
            if (accountIds.isEmpty()) {
                return Result.success(emptyMap())
            }

            val balances = mutableMapOf<String, Money>()
            
            accountIds.forEach { accountId ->
                try {
                    validateAccountId(accountId)
                    
                    val account = accountRepository.getAccountById(accountId)
                    if (account != null && account.isActive) {
                        balances[accountId] = account.balance
                    }
                    // Skip inactive or non-existent accounts without failing the whole operation
                } catch (e: Exception) {
                    // Log error but continue processing other accounts
                    println("Warning: Failed to get balance for account $accountId: ${e.message}")
                }
            }

            Result.success(balances)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total balance across multiple accounts (same currency only)
     * 
     * @param accountIds List of account IDs to sum balances for
     * @param targetCurrency Currency to convert balances to (optional)
     * @return Result containing the total balance
     */
    suspend fun getTotalBalance(
        accountIds: List<String>,
        targetCurrency: String = "SAR"
    ): Result<Money> {
        return try {
            if (accountIds.isEmpty()) {
                return Result.success(Money(0, targetCurrency))
            }

            val balancesResult = getMultipleBalances(accountIds)
            if (balancesResult.isFailure) {
                return Result.failure(
                    balancesResult.exceptionOrNull() ?: Exception("Failed to get multiple balances")
                )
            }

            val balances = balancesResult.getOrNull() ?: emptyMap()
            
            // Sum all balances in the target currency
            var totalAmount = 0L
            balances.values.forEach { balance ->
                if (balance.currency == targetCurrency) {
                    totalAmount += balance.amount
                } else {
                    // In a real implementation, this would convert currencies
                    // For now, we'll skip balances in different currencies
                    println("Warning: Skipping balance in ${balance.currency}, target currency is $targetCurrency")
                }
            }

            Result.success(Money(totalAmount, targetCurrency))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get balance with historical data
     * 
     * @param accountId The account ID to get balance history for
     * @param days Number of days of history to retrieve
     * @return Result containing balance history
     */
    suspend fun getBalanceWithHistory(
        accountId: String,
        days: Int = 30
    ): Result<BalanceWithHistory> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            if (!account.isActive) {
                return Result.failure(Exception("Account is inactive: $accountId"))
            }

            val history = accountRepository.getBalanceHistory(accountId, days)
            
            Result.success(
                BalanceWithHistory(
                    currentBalance = account.balance,
                    history = history,
                    accountId = accountId,
                    days = days
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get balance summary for all user accounts
     * 
     * @param userId The user ID to get balance summary for
     * @return Result containing comprehensive balance summary
     */
    suspend fun getBalanceSummary(userId: String): Result<BalanceSummary> {
        return try {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be empty")
            }

            val accounts = accountRepository.getAccountsByUserId(userId)
            val activeAccounts = accounts.filter { it.isActive }
            
            if (activeAccounts.isEmpty()) {
                return Result.success(
                    BalanceSummary(
                        totalBalance = Money.sar(0),
                        accountBalances = emptyMap(),
                        currencyBreakdown = emptyMap(),
                        accountCount = 0
                    )
                )
            }

            val accountBalances = activeAccounts.associate { account ->
                account.accountId to account.balance
            }

            // Calculate totals by currency
            val currencyTotals = mutableMapOf<String, Money>()
            activeAccounts.forEach { account ->
                val currency = account.currency
                val existing = currencyTotals[currency] ?: Money(0, currency)
                currencyTotals[currency] = Money(existing.amount + account.balance.amount, currency)
            }

            // Primary total (in SAR or main currency)
            val primaryTotal = currencyTotals["SAR"] ?: currencyTotals.values.firstOrNull() ?: Money.sar(0)

            Result.success(
                BalanceSummary(
                    totalBalance = primaryTotal,
                    accountBalances = accountBalances,
                    currencyBreakdown = currencyTotals,
                    accountCount = activeAccounts.size
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if account has sufficient balance for a transaction
     * 
     * @param accountId The account ID to check
     * @param amount The amount to check against
     * @return Result indicating if sufficient balance is available
     */
    suspend fun hasSufficientBalance(accountId: String, amount: Money): Result<Boolean> {
        return try {
            val availableBalanceResult = getAvailableBalance(accountId)
            if (availableBalanceResult.isFailure) {
                return availableBalanceResult.map { false }
            }

            val availableBalance = availableBalanceResult.getOrNull()
            if (availableBalance == null) {
                return Result.success(false)
            }

            // Check if currencies match
            if (availableBalance.currency != amount.currency) {
                return Result.failure(Exception("Currency mismatch: ${availableBalance.currency} vs ${amount.currency}"))
            }

            Result.success(availableBalance.amount >= amount.amount)

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
}

/**
 * Balance with historical data
 */
data class BalanceWithHistory(
    val currentBalance: Money,
    val history: List<Money>,
    val accountId: String,
    val days: Int
) {
    /**
     * Calculate balance change over the period
     */
    fun getBalanceChange(): Money? {
        return if (history.isNotEmpty()) {
            Money(
                currentBalance.amount - history.first().amount,
                currentBalance.currency
            )
        } else null
    }

    /**
     * Get percentage change over the period
     */
    fun getPercentageChange(): Double? {
        return if (history.isNotEmpty()) {
            val oldBalance = history.first().amount.toDouble()
            val newBalance = currentBalance.amount.toDouble()
            if (oldBalance != 0.0) {
                ((newBalance - oldBalance) / oldBalance) * 100
            } else null
        } else null
    }
}

/**
 * Comprehensive balance summary for a user
 */
data class BalanceSummary(
    val totalBalance: Money,
    val accountBalances: Map<String, Money>,
    val currencyBreakdown: Map<String, Money>,
    val accountCount: Int
) {
    /**
     * Get formatted total balance string
     */
    fun getFormattedTotal(): String {
        return totalBalance.toString()
    }

    /**
     * Check if user has multiple currencies
     */
    fun hasMultipleCurrencies(): Boolean {
        return currencyBreakdown.size > 1
    }
}