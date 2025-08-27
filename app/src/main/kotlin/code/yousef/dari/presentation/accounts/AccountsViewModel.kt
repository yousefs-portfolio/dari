package code.yousef.dari.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.SyncAccountsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Accounts ViewModel
 * Manages account listing, grouping, filtering, and synchronization
 */
class AccountsViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val syncAccountsUseCase: SyncAccountsUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                getAccountsUseCase().collect { accounts ->
                    val balanceSummary = calculateBalanceSummary(accounts)
                    val groupedAccounts = groupAccountsByType(accounts)
                    val filteredAccounts = applyCurrentFilters(accounts)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = accounts,
                        groupedAccounts = groupedAccounts,
                        filteredAccounts = filteredAccounts,
                        balanceSummary = balanceSummary,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load accounts"
                )
            }
        }
    }

    fun syncAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                val result = syncAccountsUseCase()
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            lastSyncTime = kotlinx.datetime.Clock.System.now()
                        )
                        // Accounts will be automatically updated through the flow
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = exception.message ?: "Failed to sync accounts"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "An error occurred during sync"
                )
            }
        }
    }

    fun filterByAccountType(accountType: AccountType?) {
        val currentState = _uiState.value
        val filteredAccounts = if (accountType == null) {
            applySearchFilter(currentState.accounts, currentState.searchQuery)
        } else {
            currentState.accounts.filter { it.type == accountType }
                .let { applySearchFilter(it, currentState.searchQuery) }
        }

        _uiState.value = currentState.copy(
            selectedAccountType = accountType,
            filteredAccounts = filteredAccounts
        )
    }

    fun searchAccounts(query: String) {
        val currentState = _uiState.value
        val filteredByType = if (currentState.selectedAccountType != null) {
            currentState.accounts.filter { it.type == currentState.selectedAccountType }
        } else {
            currentState.accounts
        }
        
        val filteredAccounts = applySearchFilter(filteredByType, query)

        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredAccounts = filteredAccounts
        )
    }

    fun clearFilters() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            searchQuery = "",
            selectedAccountType = null,
            filteredAccounts = currentState.accounts
        )
    }

    fun showAccountDetails(accountId: String) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
    }

    fun hideAccountDetails() {
        _uiState.value = _uiState.value.copy(selectedAccountId = null)
    }

    fun disconnectAccount(accountId: String) {
        viewModelScope.launch {
            try {
                val result = accountRepository.disconnectAccount(accountId)
                result.fold(
                    onSuccess = {
                        // Account will be removed automatically through the flow
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to disconnect account"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An error occurred while disconnecting account"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadAccounts()
    }

    private fun calculateBalanceSummary(accounts: List<FinancialAccount>): AccountBalanceSummary {
        var totalAssets = 0.0
        var totalLiabilities = 0.0

        accounts.forEach { account ->
            val balance = account.currentBalance?.toDouble() ?: 0.0
            if (balance >= 0) {
                totalAssets += balance
            } else {
                totalLiabilities += balance
            }
        }

        val totalBalance = totalAssets + totalLiabilities

        return AccountBalanceSummary(
            totalBalance = Money.fromDouble(totalBalance, "SAR"),
            totalAssets = Money.fromDouble(totalAssets, "SAR"),
            totalLiabilities = Money.fromDouble(totalLiabilities, "SAR"),
            accountCount = accounts.size
        )
    }

    private fun groupAccountsByType(accounts: List<FinancialAccount>): Map<AccountType, List<FinancialAccount>> {
        return accounts.groupBy { it.type }
    }

    private fun applyCurrentFilters(accounts: List<FinancialAccount>): List<FinancialAccount> {
        val currentState = _uiState.value
        
        var filtered = accounts
        
        // Apply account type filter
        currentState.selectedAccountType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }
        
        // Apply search filter
        filtered = applySearchFilter(filtered, currentState.searchQuery)
        
        return filtered
    }

    private fun applySearchFilter(accounts: List<FinancialAccount>, query: String): List<FinancialAccount> {
        return if (query.isBlank()) {
            accounts
        } else {
            accounts.filter { account ->
                account.name.contains(query, ignoreCase = true) ||
                account.accountNumber.contains(query, ignoreCase = true) ||
                account.metadata["bankName"]?.toString()?.contains(query, ignoreCase = true) == true
            }
        }
    }
}

/**
 * Accounts UI State
 */
data class AccountsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val accounts: List<FinancialAccount> = emptyList(),
    val filteredAccounts: List<FinancialAccount> = emptyList(),
    val groupedAccounts: Map<AccountType, List<FinancialAccount>> = emptyMap(),
    val balanceSummary: AccountBalanceSummary = AccountBalanceSummary.empty(),
    val searchQuery: String = "",
    val selectedAccountType: AccountType? = null,
    val selectedAccountId: String? = null,
    val lastSyncTime: kotlinx.datetime.Instant? = null
)

/**
 * Account balance summary for display
 */
data class AccountBalanceSummary(
    val totalBalance: Money,
    val totalAssets: Money,
    val totalLiabilities: Money,
    val accountCount: Int
) {
    companion object {
        fun empty() = AccountBalanceSummary(
            totalBalance = Money.fromDouble(0.0, "SAR"),
            totalAssets = Money.fromDouble(0.0, "SAR"),
            totalLiabilities = Money.fromDouble(0.0, "SAR"),
            accountCount = 0
        )
    }
}