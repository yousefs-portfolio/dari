package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.AccountSummary
import kotlinx.coroutines.flow.Flow

/**
 * Account Repository Interface
 * Defines contract for account data operations
 * Supports offline-first architecture with sync capabilities
 */
interface AccountRepository {
    
    /**
     * Get all accounts as a flow for reactive UI updates
     */
    fun getAccountsFlow(): Flow<List<FinancialAccount>>
    
    /**
     * Get active accounts only
     */
    fun getActiveAccountsFlow(): Flow<List<FinancialAccount>>
    
    /**
     * Get account summaries for dashboard
     */
    fun getAccountSummariesFlow(): Flow<List<AccountSummary>>
    
    /**
     * Get account by ID
     */
    suspend fun getAccountById(accountId: String): FinancialAccount?
    
    /**
     * Get accounts by bank code
     */
    suspend fun getAccountsByBank(bankCode: String): List<FinancialAccount>
    
    /**
     * Get accounts that have low balance
     */
    suspend fun getLowBalanceAccounts(): List<FinancialAccount>
    
    /**
     * Insert or update account
     */
    suspend fun upsertAccount(account: FinancialAccount)
    
    /**
     * Insert or update multiple accounts
     */
    suspend fun upsertAccounts(accounts: List<FinancialAccount>)
    
    /**
     * Update account balance
     */
    suspend fun updateAccountBalance(
        accountId: String,
        currentBalance: String,
        availableBalance: String
    )
    
    /**
     * Delete account by ID
     */
    suspend fun deleteAccount(accountId: String)
    
    /**
     * Delete all accounts for a bank
     */
    suspend fun deleteAccountsByBank(bankCode: String)
    
    /**
     * Sync accounts with remote API
     */
    suspend fun syncAccounts(bankCode: String): Result<List<FinancialAccount>>
    
    /**
     * Get accounts that need syncing
     */
    suspend fun getAccountsNeedingSync(): List<FinancialAccount>
    
    /**
     * Mark accounts as synced
     */
    suspend fun markAccountsSynced(accountIds: List<String>)
}