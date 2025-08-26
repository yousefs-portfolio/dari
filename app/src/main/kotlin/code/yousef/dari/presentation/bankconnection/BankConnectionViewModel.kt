package code.yousef.dari.presentation.bankconnection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

data class BankInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val logoUrl: String? = null,
    val isSupported: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONNECTED
)

enum class ConnectionStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
    EXPIRED
}

data class BankConnectionUiState(
    val banks: List<BankInfo> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filteredBanks: List<BankInfo> = emptyList(),
    val selectedBank: BankInfo? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONNECTED,
    val error: String? = null,
    val connectedBanks: List<BankInfo> = emptyList()
)

class BankConnectionViewModel : ViewModel(), KoinComponent {
    
    private val _uiState = MutableStateFlow(BankConnectionUiState())
    val uiState: StateFlow<BankConnectionUiState> = _uiState.asStateFlow()
    
    private val supportedBanks = listOf(
        BankInfo(
            id = "al_rajhi",
            name = "Al Rajhi Bank",
            displayName = "Al Rajhi Bank",
            isSupported = true
        ),
        BankInfo(
            id = "snb",
            name = "Saudi National Bank",
            displayName = "Saudi National Bank (SNB)",
            isSupported = true
        ),
        BankInfo(
            id = "riyad_bank",
            name = "Riyad Bank",
            displayName = "Riyad Bank",
            isSupported = true
        ),
        BankInfo(
            id = "sabb",
            name = "SABB",
            displayName = "Saudi British Bank (SABB)",
            isSupported = true
        ),
        BankInfo(
            id = "alinma",
            name = "Alinma Bank",
            displayName = "Alinma Bank",
            isSupported = true
        ),
        BankInfo(
            id = "albilad",
            name = "Bank Albilad",
            displayName = "Bank Albilad",
            isSupported = true
        ),
        BankInfo(
            id = "stc_pay",
            name = "STC Pay",
            displayName = "STC Pay",
            isSupported = true
        )
    )
    
    init {
        loadSupportedBanks()
    }
    
    fun searchBanks(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterBanks(query)
    }
    
    fun selectBank(bank: BankInfo) {
        _uiState.value = _uiState.value.copy(selectedBank = bank)
    }
    
    fun connectToBank(bank: BankInfo) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    connectionStatus = ConnectionStatus.CONNECTING,
                    error = null
                )
                
                // Simulate OAuth flow initiation
                initiateOAuthFlow(bank)
                
                // TODO: Replace with actual SAMA SDK integration
                kotlinx.coroutines.delay(2000) // Simulate connection delay
                
                val updatedBank = bank.copy(connectionStatus = ConnectionStatus.CONNECTED)
                val updatedConnectedBanks = _uiState.value.connectedBanks + updatedBank
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    connectedBanks = updatedConnectedBanks
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = ConnectionStatus.FAILED,
                    error = "Failed to connect to ${bank.displayName}. Please try again."
                )
            }
        }
    }
    
    fun disconnectFromBank(bank: BankInfo) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // TODO: Implement actual disconnection logic
                kotlinx.coroutines.delay(1000)
                
                val updatedConnectedBanks = _uiState.value.connectedBanks.filter { it.id != bank.id }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectedBanks = updatedConnectedBanks
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to disconnect from ${bank.displayName}"
                )
            }
        }
    }
    
    fun retryConnection() {
        val selectedBank = _uiState.value.selectedBank
        if (selectedBank != null) {
            connectToBank(selectedBank)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun loadSupportedBanks() {
        _uiState.value = _uiState.value.copy(
            banks = supportedBanks,
            filteredBanks = supportedBanks,
            isLoading = false
        )
    }
    
    private fun filterBanks(query: String) {
        val filtered = if (query.isBlank()) {
            supportedBanks
        } else {
            supportedBanks.filter { bank ->
                bank.displayName.contains(query, ignoreCase = true) ||
                bank.name.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.value = _uiState.value.copy(filteredBanks = filtered)
    }
    
    private suspend fun initiateOAuthFlow(bank: BankInfo) {
        // TODO: Implement actual SAMA OAuth flow
        // 1. Create PAR request
        // 2. Generate authorization URL
        // 3. Launch browser/custom tab for user consent
        // 4. Handle redirect and exchange code for tokens
        // 5. Store tokens securely
        
        println("Initiating OAuth flow for ${bank.name}")
        
        // For now, simulate the flow
        kotlinx.coroutines.delay(1500)
    }
    
    fun handleOAuthCallback(authorizationCode: String, state: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement token exchange
                // 1. Validate state parameter
                // 2. Exchange authorization code for access token
                // 3. Store tokens securely
                // 4. Fetch initial account data
                
                println("Handling OAuth callback: code=$authorizationCode, state=$state")
                
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.FAILED,
                    isLoading = false,
                    error = "Authentication failed. Please try again."
                )
            }
        }
    }
    
    fun refreshConnectedBanks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // TODO: Implement actual refresh logic
                // 1. Check token validity for each connected bank
                // 2. Refresh expired tokens
                // 3. Update connection status
                
                kotlinx.coroutines.delay(1000)
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh bank connections"
                )
            }
        }
    }
}