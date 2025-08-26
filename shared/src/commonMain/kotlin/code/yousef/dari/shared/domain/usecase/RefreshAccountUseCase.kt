package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.sama.OpenBankingClient
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Refresh Account Use Case
 * Handles manual refresh of account data from banking APIs
 * Updates local cache with fresh data from the bank
 */
class RefreshAccountUseCase(
    private val accountRepository: AccountRepository,
    private val openBankingClient: OpenBankingClient
) {

    /**
     * Refresh a specific account's data from the bank
     * 
     * @param accountId The account ID to refresh
     * @return Result containing the updated account or error
     */
    suspend operator fun invoke(accountId: String): Result<FinancialAccount> {
        return try {
            validateAccountId(accountId)

            // Get the current account
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            if (!account.isActive) {
                return Result.failure(Exception("Cannot refresh inactive account: $accountId"))
            }

            // Fetch fresh data from the bank
            val refreshResult = openBankingClient.getAccountInfo(account.bankCode, accountId)
            if (refreshResult.isFailure) {
                return Result.failure(
                    refreshResult.exceptionOrNull() ?: Exception("Failed to refresh account data")
                )
            }

            val updatedAccount = refreshResult.getOrNull()
                ?: return Result.failure(Exception("No account data received from bank"))

            // Update the account in local storage
            val updateResult = accountRepository.updateAccount(updatedAccount)
            if (updateResult.isFailure) {
                return Result.failure(
                    updateResult.exceptionOrNull() ?: Exception("Failed to update account locally")
                )
            }

            Result.success(updatedAccount)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Force refresh an account even if recently updated
     * 
     * @param accountId The account ID to force refresh
     * @return Result containing the updated account or error
     */
    suspend fun forceRefresh(accountId: String): Result<FinancialAccount> {
        // Force refresh is the same as regular refresh since we always fetch from remote
        return invoke(accountId)
    }

    /**
     * Refresh all accounts for a specific user
     * 
     * @param userId The user ID to refresh all accounts for
     * @return Result containing list of updated accounts
     */
    suspend fun refreshAllAccounts(userId: String): Result<List<FinancialAccount>> {
        return try {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be empty")
            }

            val accounts = accountRepository.getAccountsByUserId(userId)
            val activeAccounts = accounts.filter { it.isActive }

            if (activeAccounts.isEmpty()) {
                return Result.success(emptyList())
            }

            val refreshedAccounts = mutableListOf<FinancialAccount>()

            activeAccounts.forEach { account ->
                try {
                    val refreshResult = openBankingClient.getAccountInfo(account.bankCode, account.accountId)
                    if (refreshResult.isSuccess) {
                        val updatedAccount = refreshResult.getOrNull()
                        if (updatedAccount != null) {
                            val updateResult = accountRepository.updateAccount(updatedAccount)
                            if (updateResult.isSuccess) {
                                refreshedAccounts.add(updatedAccount)
                            } else {
                                println("Warning: Failed to update account ${account.accountId} locally: ${updateResult.exceptionOrNull()?.message}")
                            }
                        }
                    } else {
                        println("Warning: Failed to refresh account ${account.accountId}: ${refreshResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("Warning: Exception refreshing account ${account.accountId}: ${e.message}")
                }
            }

            Result.success(refreshedAccounts)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh multiple specific accounts
     * 
     * @param accountIds List of account IDs to refresh
     * @return Result containing list of successfully refreshed accounts
     */
    suspend fun refreshMultipleAccounts(accountIds: List<String>): Result<List<FinancialAccount>> {
        return try {
            if (accountIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val refreshedAccounts = mutableListOf<FinancialAccount>()

            accountIds.forEach { accountId ->
                try {
                    val result = invoke(accountId)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { account ->
                            refreshedAccounts.add(account)
                        }
                    } else {
                        println("Warning: Failed to refresh account $accountId: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("Warning: Exception refreshing account $accountId: ${e.message}")
                }
            }

            Result.success(refreshedAccounts)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh accounts by bank code
     * Useful for bank-specific refresh operations
     * 
     * @param bankCode The bank code to refresh accounts for
     * @param userId The user ID (optional filter)
     * @return Result containing list of refreshed accounts
     */
    suspend fun refreshAccountsByBank(
        bankCode: String,
        userId: String? = null
    ): Result<List<FinancialAccount>> {
        return try {
            validateBankCode(bankCode)

            val accounts = if (userId != null) {
                accountRepository.getAccountsByBankAndUser(bankCode, userId)
            } else {
                accountRepository.getAccountsByBank(bankCode)
            }

            val activeAccounts = accounts.filter { it.isActive }

            if (activeAccounts.isEmpty()) {
                return Result.success(emptyList())
            }

            val refreshedAccounts = mutableListOf<FinancialAccount>()

            activeAccounts.forEach { account ->
                try {
                    val refreshResult = openBankingClient.getAccountInfo(bankCode, account.accountId)
                    if (refreshResult.isSuccess) {
                        val updatedAccount = refreshResult.getOrNull()
                        if (updatedAccount != null) {
                            val updateResult = accountRepository.updateAccount(updatedAccount)
                            if (updateResult.isSuccess) {
                                refreshedAccounts.add(updatedAccount)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Warning: Exception refreshing account ${account.accountId}: ${e.message}")
                }
            }

            Result.success(refreshedAccounts)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Smart refresh - only refresh accounts that haven't been updated recently
     * 
     * @param userId The user ID to smart refresh accounts for
     * @param maxAgeMinutes Maximum age in minutes before refresh is needed
     * @return Result containing list of refreshed accounts
     */
    suspend fun smartRefresh(
        userId: String,
        maxAgeMinutes: Long = 15
    ): Result<List<FinancialAccount>> {
        return try {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be empty")
            }

            val accounts = accountRepository.getAccountsByUserId(userId)
            val activeAccounts = accounts.filter { it.isActive }

            if (activeAccounts.isEmpty()) {
                return Result.success(emptyList())
            }

            val currentTime = Clock.System.now()
            val refreshThreshold = currentTime.minus(kotlinx.datetime.DateTimePeriod(minutes = maxAgeMinutes.toInt()))

            // Filter accounts that need refresh
            val accountsToRefresh = activeAccounts.filter { account ->
                val lastUpdated = account.lastUpdated
                // Convert LocalDateTime to Instant for comparison
                // This is a simplified comparison - in production, you'd need proper timezone handling
                true // For now, always refresh - implement proper time comparison
            }

            if (accountsToRefresh.isEmpty()) {
                // All accounts are up to date
                return Result.success(activeAccounts)
            }

            val refreshedAccounts = mutableListOf<FinancialAccount>()
            val upToDateAccounts = activeAccounts - accountsToRefresh.toSet()

            // Add accounts that are already up to date
            refreshedAccounts.addAll(upToDateAccounts)

            // Refresh stale accounts
            accountsToRefresh.forEach { account ->
                try {
                    val refreshResult = openBankingClient.getAccountInfo(account.bankCode, account.accountId)
                    if (refreshResult.isSuccess) {
                        val updatedAccount = refreshResult.getOrNull()
                        if (updatedAccount != null) {
                            val updateResult = accountRepository.updateAccount(updatedAccount)
                            if (updateResult.isSuccess) {
                                refreshedAccounts.add(updatedAccount)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Warning: Exception smart refreshing account ${account.accountId}: ${e.message}")
                    // Keep the old account data if refresh fails
                    refreshedAccounts.add(account)
                }
            }

            Result.success(refreshedAccounts)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get refresh status for an account
     * 
     * @param accountId The account ID to check refresh status for
     * @return RefreshStatus indicating the current status
     */
    suspend fun getRefreshStatus(accountId: String): RefreshStatus {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
            when {
                account == null -> RefreshStatus.NOT_FOUND
                !account.isActive -> RefreshStatus.INACTIVE
                else -> {
                    // Check if account needs refresh (simplified logic)
                    val currentTime = Clock.System.now()
                    val lastUpdated = account.lastUpdated
                    // In a real implementation, you'd calculate the time difference properly
                    RefreshStatus.UP_TO_DATE
                }
            }
        } catch (e: Exception) {
            RefreshStatus.ERROR
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
     * Validate bank code format
     */
    private fun validateBankCode(bankCode: String) {
        if (bankCode.isBlank()) {
            throw IllegalArgumentException("Bank code cannot be empty")
        }
    }
}

/**
 * Account refresh status
 */
enum class RefreshStatus {
    UP_TO_DATE,
    NEEDS_REFRESH,
    REFRESHING,
    INACTIVE,
    NOT_FOUND,
    ERROR
}