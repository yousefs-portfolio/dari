package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount

/**
 * Sync Accounts Use Case
 * Synchronizes account data from SAMA Open Banking APIs
 * Handles offline-first synchronization with conflict resolution
 */
class SyncAccountsUseCase(
    private val accountRepository: AccountRepository
) {
    
    /**
     * Sync all accounts for a specific bank
     */
    suspend operator fun invoke(bankCode: String): Result<List<FinancialAccount>> {
        return try {
            val result = accountRepository.syncAccounts(bankCode)
            if (result.isSuccess) {
                val accounts = result.getOrNull() ?: emptyList()
                // Mark accounts as synced
                val accountIds = accounts.map { it.accountId }
                accountRepository.markAccountsSynced(accountIds)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync accounts that need updating
     */
    suspend fun syncPending(): Result<List<FinancialAccount>> {
        return try {
            val accountsNeedingSync = accountRepository.getAccountsNeedingSync()
            val allSyncedAccounts = mutableListOf<FinancialAccount>()
            
            // Group by bank code and sync each bank
            accountsNeedingSync.groupBy { it.bankCode }.forEach { (bankCode, accounts) ->
                val result = accountRepository.syncAccounts(bankCode)
                if (result.isSuccess) {
                    val syncedAccounts = result.getOrNull() ?: emptyList()
                    allSyncedAccounts.addAll(syncedAccounts)
                    
                    // Mark as synced
                    val accountIds = accounts.map { it.accountId }
                    accountRepository.markAccountsSynced(accountIds)
                }
            }
            
            Result.success(allSyncedAccounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}