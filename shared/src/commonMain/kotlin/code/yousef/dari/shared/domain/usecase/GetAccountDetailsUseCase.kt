package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import kotlinx.datetime.LocalDateTime

/**
 * Get Account Details Use Case
 * Retrieves comprehensive account information including balance, transactions, and statistics
 */
class GetAccountDetailsUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Get complete account details including recent transactions and statistics
     * 
     * @param accountId The account ID to get details for
     * @param transactionLimit Number of recent transactions to include
     * @return Result containing comprehensive account details
     */
    suspend operator fun invoke(
        accountId: String,
        transactionLimit: Int = 10
    ): Result<AccountDetails> {
        return try {
            validateAccountId(accountId)

            // Get the account information
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            // Get recent transactions (don't fail if this fails)
            val recentTransactions = try {
                transactionRepository.getRecentTransactions(accountId, transactionLimit)
            } catch (e: Exception) {
                println("Warning: Failed to get recent transactions for $accountId: ${e.message}")
                emptyList()
            }

            // Get account statistics (don't fail if this fails)
            val stats = try {
                accountRepository.getAccountStats(accountId)
            } catch (e: Exception) {
                println("Warning: Failed to get account stats for $accountId: ${e.message}")
                AccountStats()
            }

            Result.success(
                AccountDetails(
                    account = account,
                    recentTransactions = recentTransactions,
                    stats = stats
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get detailed account information with custom transaction limit
     * 
     * @param accountId The account ID to get details for
     * @param transactionLimit Number of recent transactions to include
     * @return Result containing detailed account information
     */
    suspend fun getDetailedInfo(
        accountId: String,
        transactionLimit: Int = 25
    ): Result<AccountDetails> {
        return invoke(accountId, transactionLimit)
    }

    /**
     * Get account summary without detailed statistics (lighter weight)
     * 
     * @param accountId The account ID to get summary for
     * @return Result containing basic account summary
     */
    suspend fun getAccountSummary(accountId: String): Result<AccountSummary> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            Result.success(
                AccountSummary(
                    accountId = account.accountId,
                    accountName = account.accountName,
                    accountType = account.accountType,
                    balance = account.balance,
                    currency = account.currency,
                    bankCode = account.bankCode,
                    accountNumber = account.accountNumber,
                    iban = account.iban,
                    isActive = account.isActive,
                    lastUpdated = account.lastUpdated
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get account details with spending analysis
     * 
     * @param accountId The account ID to analyze
     * @param analysisPeriodDays Number of days to analyze spending for
     * @return Result containing account details with spending analysis
     */
    suspend fun getDetailedInfoWithAnalysis(
        accountId: String,
        analysisPeriodDays: Int = 30
    ): Result<DetailedAccountInfo> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            // Get recent transactions
            val recentTransactions = try {
                transactionRepository.getRecentTransactions(accountId, 10)
            } catch (e: Exception) {
                emptyList()
            }

            // Get account statistics
            val stats = try {
                accountRepository.getAccountStats(accountId)
            } catch (e: Exception) {
                AccountStats()
            }

            // Get spending analysis
            val spendingAnalysis = try {
                transactionRepository.getSpendingAnalysis(accountId, analysisPeriodDays)
            } catch (e: Exception) {
                println("Warning: Failed to get spending analysis for $accountId: ${e.message}")
                SpendingAnalysis()
            }

            Result.success(
                DetailedAccountInfo(
                    account = account,
                    recentTransactions = recentTransactions,
                    stats = stats,
                    spendingAnalysis = spendingAnalysis
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get account details for multiple accounts
     * 
     * @param accountIds List of account IDs to get details for
     * @return Result containing map of account ID to account details
     */
    suspend fun getMultipleAccountDetails(
        accountIds: List<String>
    ): Result<Map<String, AccountDetails>> {
        return try {
            if (accountIds.isEmpty()) {
                return Result.success(emptyMap())
            }

            val accountDetails = mutableMapOf<String, AccountDetails>()

            accountIds.forEach { accountId ->
                try {
                    val result = invoke(accountId)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { details ->
                            accountDetails[accountId] = details
                        }
                    }
                } catch (e: Exception) {
                    println("Warning: Failed to get details for account $accountId: ${e.message}")
                }
            }

            Result.success(accountDetails)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get account connection information
     * 
     * @param accountId The account ID to get connection info for
     * @return Result containing account connection details
     */
    suspend fun getConnectionInfo(accountId: String): Result<AccountConnectionInfo> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            val connectionInfo = accountRepository.getConnectionInfo(accountId)

            Result.success(
                AccountConnectionInfo(
                    accountId = account.accountId,
                    bankCode = account.bankCode,
                    isActive = account.isActive,
                    lastSyncTime = account.lastUpdated,
                    connectionStatus = if (account.isActive) "Connected" else "Disconnected",
                    consentExpiryDate = connectionInfo.consentExpiryDate,
                    permissions = connectionInfo.permissions,
                    lastErrorMessage = connectionInfo.lastErrorMessage
                )
            )

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
 * Complete account details including transactions and statistics
 */
data class AccountDetails(
    val account: FinancialAccount,
    val recentTransactions: List<Transaction>,
    val stats: AccountStats
) {
    /**
     * Get formatted account display name
     */
    fun getDisplayName(): String {
        return "${account.accountName} (${account.accountNumber})"
    }

    /**
     * Get account health score based on activity and balance
     */
    fun getHealthScore(): Int {
        return when {
            !account.isActive -> 0
            recentTransactions.isEmpty() -> 30
            account.balance.amount <= 0 -> 40
            recentTransactions.size >= 5 -> 100
            else -> 70
        }
    }
}

/**
 * Basic account summary for lightweight operations
 */
data class AccountSummary(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val balance: Money,
    val currency: String,
    val bankCode: String,
    val accountNumber: String,
    val iban: String,
    val isActive: Boolean,
    val lastUpdated: LocalDateTime
) {
    /**
     * Get masked account number for display
     */
    fun getMaskedAccountNumber(): String {
        return if (accountNumber.startsWith("****")) {
            accountNumber
        } else {
            "****${accountNumber.takeLast(4)}"
        }
    }

    /**
     * Get formatted IBAN for display
     */
    fun getFormattedIban(): String {
        return if (iban.length >= 8) {
            "${iban.take(4)} **** **** ${iban.takeLast(4)}"
        } else {
            iban
        }
    }
}

/**
 * Detailed account information with spending analysis
 */
data class DetailedAccountInfo(
    val account: FinancialAccount,
    val recentTransactions: List<Transaction>,
    val stats: AccountStats,
    val spendingAnalysis: SpendingAnalysis
)

/**
 * Account statistics
 */
data class AccountStats(
    val totalTransactions: Int = 0,
    val totalSpent: Money = Money.sar(0),
    val totalReceived: Money = Money.sar(0),
    val averageTransaction: Money = Money.sar(0),
    val mostFrequentCategory: String = "",
    val lastTransactionDate: LocalDateTime? = null
) {
    /**
     * Calculate net balance change
     */
    fun getNetChange(): Money {
        return Money(
            totalReceived.amount - totalSpent.amount,
            totalReceived.currency
        )
    }
}

/**
 * Spending analysis data
 */
data class SpendingAnalysis(
    val categoryBreakdown: Map<String, Money> = emptyMap(),
    val monthlyTrend: List<Money> = emptyList(),
    val averageDailySpending: Money = Money.sar(0),
    val topMerchants: List<String> = emptyList(),
    val spendingPattern: String = "Unknown"
) {
    /**
     * Get top spending category
     */
    fun getTopCategory(): String? {
        return categoryBreakdown.maxByOrNull { it.value.amount }?.key
    }

    /**
     * Get spending trend (increasing, decreasing, stable)
     */
    fun getTrend(): String {
        return when {
            monthlyTrend.size < 2 -> "Unknown"
            monthlyTrend.last().amount > monthlyTrend[monthlyTrend.size - 2].amount -> "Increasing"
            monthlyTrend.last().amount < monthlyTrend[monthlyTrend.size - 2].amount -> "Decreasing"
            else -> "Stable"
        }
    }
}

/**
 * Account connection information
 */
data class AccountConnectionInfo(
    val accountId: String,
    val bankCode: String,
    val isActive: Boolean,
    val lastSyncTime: LocalDateTime,
    val connectionStatus: String,
    val consentExpiryDate: LocalDateTime? = null,
    val permissions: List<String> = emptyList(),
    val lastErrorMessage: String? = null
) {
    /**
     * Check if consent is near expiry (within 30 days)
     */
    fun isConsentNearExpiry(): Boolean {
        // TODO: Implement proper date comparison
        return false
    }

    /**
     * Get connection health status
     */
    fun getHealthStatus(): String {
        return when {
            !isActive -> "Disconnected"
            lastErrorMessage != null -> "Error"
            isConsentNearExpiry() -> "Expiring Soon"
            else -> "Healthy"
        }
    }
}