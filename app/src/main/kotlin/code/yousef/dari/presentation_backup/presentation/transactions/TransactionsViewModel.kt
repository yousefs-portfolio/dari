package code.yousef.dari.presentation.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Transactions ViewModel
 * Manages transaction listing, filtering, searching, and grouping with TDD approach
 * Supports infinite scrolling, bulk operations, and real-time updates
 */
class TransactionsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val syncTransactionsUseCase: SyncTransactionsUseCase,
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val accountId: String = savedStateHandle.get<String>("accountId") ?: ""

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                getTransactionsUseCase(accountId).collect { transactions ->
                    val groupedTransactions = groupTransactionsByDate(transactions)
                    val filteredTransactions = applyCurrentFilters(transactions)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactions = transactions,
                        filteredTransactions = filteredTransactions,
                        groupedTransactions = groupedTransactions,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load transactions"
                )
            }
        }
    }

    fun searchTransactions(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                isSearching = true,
                error = null
            )
            
            try {
                if (query.isBlank()) {
                    clearSearch()
                    return@launch
                }
                
                val result = searchTransactionsUseCase(accountId, query, limit = 100, offset = 0)
                result.fold(
                    onSuccess = { searchResults ->
                        val groupedResults = groupTransactionsByDate(searchResults)
                        _uiState.value = _uiState.value.copy(
                            filteredTransactions = searchResults,
                            groupedTransactions = groupedResults,
                            isSearching = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error = exception.message ?: "Search failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.message ?: "An error occurred during search"
                )
            }
        }
    }

    fun clearSearch() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val filteredTransactions = applyCurrentFilters(currentState.transactions)
            val groupedTransactions = groupTransactionsByDate(filteredTransactions)
            
            _uiState.value = currentState.copy(
                searchQuery = "",
                isSearching = false,
                filteredTransactions = filteredTransactions,
                groupedTransactions = groupedTransactions
            )
        }
    }

    fun filterByCategory(category: String) {
        val currentState = _uiState.value
        val updatedCategories = if (currentState.selectedCategories.contains(category)) {
            currentState.selectedCategories - category
        } else {
            currentState.selectedCategories + category
        }
        
        _uiState.value = currentState.copy(selectedCategories = updatedCategories)
        applyFilters()
    }

    fun filterByDateRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                dateRange = Pair(startDate, endDate),
                isLoading = true
            )
            
            try {
                val result = searchTransactionsUseCase.searchByDateRange(
                    accountId, startDate, endDate, limit = 100, offset = 0
                )
                
                result.fold(
                    onSuccess = { filteredTransactions ->
                        val groupedTransactions = groupTransactionsByDate(filteredTransactions)
                        _uiState.value = _uiState.value.copy(
                            filteredTransactions = filteredTransactions,
                            groupedTransactions = groupedTransactions,
                            isLoading = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Date filter failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred while filtering by date"
                )
            }
        }
    }

    fun filterByAmount(minAmount: Money, maxAmount: Money) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                amountRange = Pair(minAmount, maxAmount),
                isLoading = true
            )
            
            try {
                val result = searchTransactionsUseCase.searchByAmountRange(
                    accountId, minAmount, maxAmount, limit = 100, offset = 0
                )
                
                result.fold(
                    onSuccess = { filteredTransactions ->
                        val groupedTransactions = groupTransactionsByDate(filteredTransactions)
                        _uiState.value = _uiState.value.copy(
                            filteredTransactions = filteredTransactions,
                            groupedTransactions = groupedTransactions,
                            isLoading = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Amount filter failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred while filtering by amount"
                )
            }
        }
    }

    fun syncTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                val result = syncTransactionsUseCase(accountId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            lastSyncTime = kotlinx.datetime.Clock.System.now()
                        )
                        // Transactions will be automatically updated through the flow
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = exception.message ?: "Failed to sync transactions"
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

    fun loadMoreTransactions() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || currentState.hasReachedEnd) return
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMore = true)
            
            try {
                val nextPage = currentState.currentPage + 1
                val offset = nextPage * 50
                
                val nextPageTransactions = getTransactionsUseCase.getPaginated(
                    accountId, 
                    limit = 50, 
                    offset = offset
                )
                
                if (nextPageTransactions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        hasReachedEnd = true
                    )
                } else {
                    val updatedTransactions = currentState.transactions + nextPageTransactions
                    val filteredTransactions = applyCurrentFilters(updatedTransactions)
                    val groupedTransactions = groupTransactionsByDate(filteredTransactions)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        currentPage = nextPage,
                        transactions = updatedTransactions,
                        filteredTransactions = filteredTransactions,
                        groupedTransactions = groupedTransactions
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load more transactions"
                )
            }
        }
    }

    fun categorizeTransaction(transactionId: String, category: String) {
        viewModelScope.launch {
            try {
                val result = categorizeTransactionUseCase(transactionId, category)
                result.fold(
                    onSuccess = {
                        // Transaction will be updated automatically through the repository flow
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to categorize transaction"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An error occurred while categorizing transaction"
                )
            }
        }
    }

    fun selectTransaction(transactionId: String) {
        _uiState.value = _uiState.value.copy(selectedTransactionId = transactionId)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedTransactionId = null)
    }

    fun toggleBulkSelectionMode() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isBulkSelectionMode = !currentState.isBulkSelectionMode,
            selectedTransactionIds = emptySet()
        )
    }

    fun toggleTransactionSelection(transactionId: String) {
        val currentState = _uiState.value
        val updatedSelection = if (currentState.selectedTransactionIds.contains(transactionId)) {
            currentState.selectedTransactionIds - transactionId
        } else {
            currentState.selectedTransactionIds + transactionId
        }
        
        _uiState.value = currentState.copy(selectedTransactionIds = updatedSelection)
    }

    fun selectAllVisibleTransactions() {
        val currentState = _uiState.value
        val allVisibleIds = currentState.filteredTransactions.map { it.id }.toSet()
        _uiState.value = currentState.copy(selectedTransactionIds = allVisibleIds)
    }

    fun clearAllSelections() {
        _uiState.value = _uiState.value.copy(selectedTransactionIds = emptySet())
    }

    fun clearAllFilters() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val groupedTransactions = groupTransactionsByDate(currentState.transactions)
            
            _uiState.value = currentState.copy(
                searchQuery = "",
                selectedCategories = emptySet(),
                dateRange = null,
                amountRange = null,
                isSearching = false,
                filteredTransactions = currentState.transactions,
                groupedTransactions = groupedTransactions
            )
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadTransactions()
    }

    private fun applyCurrentFilters(transactions: List<Transaction>): List<Transaction> {
        val currentState = _uiState.value
        var filtered = transactions

        // Apply category filters
        if (currentState.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { transaction ->
                currentState.selectedCategories.contains(transaction.category)
            }
        }

        return filtered
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val filteredTransactions = applyCurrentFilters(currentState.transactions)
        val groupedTransactions = groupTransactionsByDate(filteredTransactions)
        
        _uiState.value = currentState.copy(
            filteredTransactions = filteredTransactions,
            groupedTransactions = groupedTransactions
        )
    }

    private fun groupTransactionsByDate(transactions: List<Transaction>): Map<String, List<Transaction>> {
        return transactions
            .sortedByDescending { it.date }
            .groupBy { transaction ->
                // Format date as readable string for grouping
                val date = transaction.date
                "${date.month.name} ${date.dayOfMonth}, ${date.year}"
            }
    }
}

/**
 * Transactions UI State
 */
data class TransactionsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val error: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val groupedTransactions: Map<String, List<Transaction>> = emptyMap(),
    val searchQuery: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val dateRange: Pair<LocalDate, LocalDate>? = null,
    val amountRange: Pair<Money, Money>? = null,
    val selectedTransactionId: String? = null,
    val isBulkSelectionMode: Boolean = false,
    val selectedTransactionIds: Set<String> = emptySet(),
    val currentPage: Int = 0,
    val lastSyncTime: kotlinx.datetime.Instant? = null
)