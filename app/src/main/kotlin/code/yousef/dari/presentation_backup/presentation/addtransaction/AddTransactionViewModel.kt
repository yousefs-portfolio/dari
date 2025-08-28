package code.yousef.dari.presentation.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.CategorizeTransactionUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.UUID

/**
 * Add Transaction ViewModel
 * Manages transaction creation with validation, smart categorization, and split transactions
 * Supports manual entry, recurring transactions, and receipt attachments
 */
class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val accountId: String = savedStateHandle.get<String>("accountId") ?: ""

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        
        // Auto-suggest category if merchant name is detected
        if (description.isNotBlank() && _uiState.value.merchantName.isBlank()) {
            updateMerchantName(description)
        }
    }

    fun updateAmount(amount: Double) {
        _uiState.value = _uiState.value.copy(amount = Money.fromDouble(amount, "SAR"))
        
        // Update split amounts if in split mode
        if (_uiState.value.isSplitTransaction) {
            updateSplitAmountsForTotalChange()
        }
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateMerchantName(merchantName: String) {
        _uiState.value = _uiState.value.copy(merchantName = merchantName)
        
        // Trigger smart categorization if merchant name is meaningful
        if (merchantName.trim().length >= 3) {
            applyCategorySuggestionDebounced(merchantName)
        }
    }

    fun toggleRecurring() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(isRecurring = !currentState.isRecurring)
    }

    fun updateRecurrenceFrequency(frequency: RecurrenceFrequency) {
        _uiState.value = _uiState.value.copy(recurrenceFrequency = frequency)
    }

    fun updateRecurrenceEndDate(endDate: LocalDate?) {
        _uiState.value = _uiState.value.copy(recurrenceEndDate = endDate)
    }

    fun toggleSplitTransaction() {
        val currentState = _uiState.value
        val newIsSplit = !currentState.isSplitTransaction
        
        if (newIsSplit) {
            // Initialize with two default split amounts
            val halfAmount = currentState.amount.amount / 2
            val defaultSplits = listOf(
                SplitAmount("Split 1", Money.fromDouble(halfAmount, "SAR")),
                SplitAmount("Split 2", Money.fromDouble(halfAmount, "SAR"))
            )
            _uiState.value = currentState.copy(
                isSplitTransaction = true,
                splitAmounts = defaultSplits
            )
        } else {
            _uiState.value = currentState.copy(
                isSplitTransaction = false,
                splitAmounts = emptyList()
            )
        }
    }

    fun addSplitAmount(splitAmount: SplitAmount) {
        val currentState = _uiState.value
        val updatedSplits = currentState.splitAmounts + splitAmount
        _uiState.value = currentState.copy(splitAmounts = updatedSplits)
    }

    fun updateSplitAmount(index: Int, splitAmount: SplitAmount) {
        val currentState = _uiState.value
        val updatedSplits = currentState.splitAmounts.toMutableList()
        
        if (index in updatedSplits.indices) {
            updatedSplits[index] = splitAmount
            _uiState.value = currentState.copy(splitAmounts = updatedSplits)
        }
    }

    fun removeSplitAmount(index: Int) {
        val currentState = _uiState.value
        val updatedSplits = currentState.splitAmounts.toMutableList()
        
        if (index in updatedSplits.indices && updatedSplits.size > 1) {
            updatedSplits.removeAt(index)
            _uiState.value = currentState.copy(splitAmounts = updatedSplits)
        }
    }

    fun addReceiptUrl(receiptUrl: String) {
        val currentState = _uiState.value
        val updatedReceipts = currentState.receiptUrls + receiptUrl
        _uiState.value = currentState.copy(receiptUrls = updatedReceipts)
    }

    fun removeReceiptUrl(receiptUrl: String) {
        val currentState = _uiState.value
        val updatedReceipts = currentState.receiptUrls - receiptUrl
        _uiState.value = currentState.copy(receiptUrls = updatedReceipts)
    }

    fun applyCategorySuggestion() {
        val merchantName = _uiState.value.merchantName
        if (merchantName.isNotBlank()) {
            applyCategorySuggestionDebounced(merchantName)
        }
    }

    fun saveTransaction() {
        viewModelScope.launch {
            val validation = validateInput()
            
            if (!validation.isValid) {
                _uiState.value = _uiState.value.copy(error = validation.errorMessage)
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            try {
                val currentState = _uiState.value
                val transaction = buildTransaction(currentState)
                
                val result = transactionRepository.createTransaction(transaction)
                
                result.fold(
                    onSuccess = { savedTransaction ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            isTransactionSaved = true
                        )
                        
                        // Handle recurring transaction setup if enabled
                        if (currentState.isRecurring) {
                            setupRecurringTransaction(savedTransaction)
                        }
                        
                        // Handle split transactions if enabled
                        if (currentState.isSplitTransaction) {
                            createSplitTransactions(savedTransaction, currentState.splitAmounts)
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = exception.message ?: "Failed to save transaction"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "An error occurred while saving"
                )
            }
        }
    }

    fun validateInput(): ValidationResult {
        val currentState = _uiState.value
        
        when {
            currentState.description.isBlank() -> {
                return ValidationResult(false, "Description cannot be empty")
            }
            currentState.amount.amount <= 0.0 -> {
                return ValidationResult(false, "Amount must be greater than zero")
            }
            currentState.category.isBlank() -> {
                return ValidationResult(false, "Category cannot be empty")
            }
            currentState.isSplitTransaction -> {
                val totalSplitAmount = currentState.splitAmounts.sumOf { it.amount.amount }
                if (kotlin.math.abs(totalSplitAmount - currentState.amount.amount) > 0.01) {
                    return ValidationResult(false, "Split amounts must equal total amount")
                }
                if (currentState.splitAmounts.any { it.description.isBlank() }) {
                    return ValidationResult(false, "All split descriptions must be filled")
                }
            }
        }
        
        return ValidationResult(true, null)
    }

    fun clearForm() {
        _uiState.value = AddTransactionUiState()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun applyCategorySuggestionDebounced(merchantName: String) {
        viewModelScope.launch {
            try {
                val result = categorizeTransactionUseCase.suggestCategory(merchantName, _uiState.value.description)
                
                result.fold(
                    onSuccess = { suggestedCategory ->
                        // Only update if category is still "Other" (hasn't been manually changed)
                        if (_uiState.value.category == "Other") {
                            _uiState.value = _uiState.value.copy(category = suggestedCategory)
                        }
                    },
                    onFailure = {
                        // Silently ignore categorization failures
                    }
                )
            } catch (e: Exception) {
                // Silently ignore categorization errors
            }
        }
    }

    private fun buildTransaction(state: AddTransactionUiState): Transaction {
        val finalAmount = if (state.type == TransactionType.DEBIT) {
            Money.fromDouble(-kotlin.math.abs(state.amount.amount), state.amount.currency)
        } else {
            Money.fromDouble(kotlin.math.abs(state.amount.amount), state.amount.currency)
        }
        
        val merchant = if (state.merchantName.isNotBlank()) {
            Merchant(name = state.merchantName, category = state.category)
        } else null
        
        val metadata = mutableMapOf<String, Any>()
        if (state.receiptUrls.isNotEmpty()) {
            metadata["receipt_urls"] = state.receiptUrls
        }
        if (state.isRecurring) {
            metadata["is_recurring"] = true
            metadata["recurrence_frequency"] = state.recurrenceFrequency.name
            state.recurrenceEndDate?.let { metadata["recurrence_end_date"] = it.toString() }
        }
        if (state.isSplitTransaction) {
            metadata["is_split"] = true
            metadata["split_count"] = state.splitAmounts.size
        }
        
        return Transaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            amount = finalAmount,
            description = state.description,
            date = state.date,
            type = state.type,
            category = state.category,
            merchant = merchant,
            reference = "manual-entry",
            status = TransactionStatus.COMPLETED,
            metadata = metadata
        )
    }

    private suspend fun setupRecurringTransaction(transaction: Transaction) {
        // TODO: Implement recurring transaction setup
        // This would create a recurring transaction rule/schedule
    }

    private suspend fun createSplitTransactions(parentTransaction: Transaction, splitAmounts: List<SplitAmount>) {
        // TODO: Implement split transaction creation
        // This would create child transactions for each split amount
    }

    private fun updateSplitAmountsForTotalChange() {
        val currentState = _uiState.value
        val newTotalAmount = currentState.amount.amount
        val currentSplits = currentState.splitAmounts
        
        if (currentSplits.isNotEmpty() && newTotalAmount > 0) {
            val amountPerSplit = newTotalAmount / currentSplits.size
            val updatedSplits = currentSplits.mapIndexed { index, split ->
                // Give any remainder to the last split
                val splitAmount = if (index == currentSplits.size - 1) {
                    newTotalAmount - (amountPerSplit * (currentSplits.size - 1))
                } else {
                    amountPerSplit
                }
                split.copy(amount = Money.fromDouble(splitAmount, "SAR"))
            }
            
            _uiState.value = currentState.copy(splitAmounts = updatedSplits)
        }
    }
}

/**
 * Add Transaction UI State
 */
data class AddTransactionUiState(
    val description: String = "",
    val amount: Money = Money.fromDouble(0.0, "SAR"),
    val type: TransactionType = TransactionType.DEBIT,
    val category: String = "Other",
    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val merchantName: String = "",
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val recurrenceEndDate: LocalDate? = null,
    val isSplitTransaction: Boolean = false,
    val splitAmounts: List<SplitAmount> = emptyList(),
    val receiptUrls: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isTransactionSaved: Boolean = false,
    val error: String? = null,
    val categorySuggestions: List<String> = emptyList(),
    val merchantSuggestions: List<String> = emptyList()
)

/**
 * Split Amount data class for split transactions
 */
data class SplitAmount(
    val description: String,
    val amount: Money,
    val category: String? = null
)

/**
 * Recurrence frequency for recurring transactions
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Input validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)