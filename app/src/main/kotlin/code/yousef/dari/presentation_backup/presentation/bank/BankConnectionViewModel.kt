package code.yousef.dari.presentation.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.sama.sdk.OpenBankingClient
import code.yousef.dari.sama.sdk.models.Bank
import code.yousef.dari.sama.sdk.models.ConsentRequest
import code.yousef.dari.sama.sdk.models.PermissionType
import code.yousef.dari.shared.data.repository.AccountRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * Bank Connection ViewModel
 * Manages bank selection, OAuth flow initiation, and consent management
 */
class BankConnectionViewModel(
    private val openBankingClient: OpenBankingClient,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BankConnectionUiState())
    val uiState: StateFlow<BankConnectionUiState> = _uiState.asStateFlow()

    init {
        loadSupportedBanks()
    }

    private fun loadSupportedBanks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = openBankingClient.getSupportedBanks()
                result.fold(
                    onSuccess = { banks ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            supportedBanks = banks,
                            filteredBanks = banks,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load banks"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred while loading banks"
                )
            }
        }
    }

    fun searchBanks(query: String) {
        val currentState = _uiState.value
        val filteredBanks = if (query.isBlank()) {
            currentState.supportedBanks
        } else {
            currentState.supportedBanks.filter { bank ->
                bank.name.contains(query, ignoreCase = true) ||
                bank.displayName.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredBanks = filteredBanks
        )
    }

    fun connectToBank(bank: Bank) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)
            
            try {
                // Create consent request
                val consentRequest = ConsentRequest(
                    bankId = bank.id,
                    permissions = listOf(
                        PermissionType.READ_ACCOUNTS,
                        PermissionType.READ_BALANCES,
                        PermissionType.READ_TRANSACTIONS
                    ),
                    expirationDateTime = Clock.System.now().plus(90, DateTimeUnit.DAY),
                    transactionFromDateTime = Clock.System.now().plus(-12, DateTimeUnit.MONTH),
                    transactionToDateTime = Clock.System.now()
                )
                
                val result = openBankingClient.createConsent(consentRequest)
                result.fold(
                    onSuccess = { consentResponse ->
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            selectedBank = bank,
                            currentConsentId = consentResponse.consentId,
                            authorizationUrl = consentResponse.authorizationUrl,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            error = exception.message ?: "Failed to initiate bank connection"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message ?: "An error occurred while connecting to bank"
                )
            }
        }
    }

    fun completeAuthorization(authorizationCode: String, consentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)
            
            try {
                // Exchange authorization code for tokens
                val tokenResult = openBankingClient.exchangeAuthorizationCode(authorizationCode, consentId)
                tokenResult.fold(
                    onSuccess = {
                        // Sync accounts after successful authorization
                        syncAccounts(consentId)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            error = exception.message ?: "Failed to complete authorization"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message ?: "An error occurred during authorization"
                )
            }
        }
    }

    private suspend fun syncAccounts(consentId: String) {
        try {
            val syncResult = accountRepository.syncAccountsForConsent(consentId)
            syncResult.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        connectionSuccessful = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = exception.message ?: "Failed to sync accounts"
                    )
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                error = e.message ?: "An error occurred while syncing accounts"
            )
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetConnectionState() {
        _uiState.value = _uiState.value.copy(
            selectedBank = null,
            currentConsentId = null,
            authorizationUrl = null,
            connectionSuccessful = false,
            isConnecting = false,
            error = null
        )
    }

    fun refresh() {
        loadSupportedBanks()
    }
}

/**
 * Bank Connection UI State
 */
data class BankConnectionUiState(
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    val supportedBanks: List<Bank> = emptyList(),
    val filteredBanks: List<Bank> = emptyList(),
    val searchQuery: String = "",
    val selectedBank: Bank? = null,
    val currentConsentId: String? = null,
    val authorizationUrl: String? = null,
    val connectionSuccessful: Boolean = false
)