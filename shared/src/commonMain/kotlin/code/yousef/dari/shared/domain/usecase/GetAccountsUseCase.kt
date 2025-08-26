package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import kotlinx.coroutines.flow.Flow

/**
 * Get Accounts Use Case
 * Retrieves user's financial accounts with proper business logic
 * Implements domain rules and filtering
 */
class GetAccountsUseCase(
    private val accountRepository: AccountRepository
) {
    
    /**
     * Get all active accounts as a flow
     */
    operator fun invoke(): Flow<List<FinancialAccount>> {
        return accountRepository.getActiveAccountsFlow()
    }
    
    /**
     * Get accounts by bank code
     */
    suspend fun getByBank(bankCode: String): List<FinancialAccount> {
        return accountRepository.getAccountsByBank(bankCode)
    }
    
    /**
     * Get accounts with low balance warnings
     */
    suspend fun getLowBalanceAccounts(): List<FinancialAccount> {
        return accountRepository.getLowBalanceAccounts()
    }
}