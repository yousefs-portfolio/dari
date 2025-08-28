package code.yousef.dari.presentation.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.budget.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Budget ViewModel
 * Manages budget creation, tracking, analysis, and recommendations
 * Supports multiple budget periods, categories, and alert thresholds
 */
class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase,
    private val getBudgetRecommendationsUseCase: GetBudgetRecommendationsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val accountId: String = savedStateHandle.get<String>("accountId") ?: ""

    init {
        loadBudgets()
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                budgetRepository.getBudgets().collect { budgets ->
                    val filteredBudgets = applyCurrentFilters(budgets)
                    val budgetStatuses = calculateBudgetStatuses(budgets)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        budgets = budgets,
                        filteredBudgets = filteredBudgets,
                        budgetStatuses = budgetStatuses,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load budgets"
                )
            }
        }
    }

    fun createBudget(
        name: String,
        category: String,
        amount: Money,
        period: BudgetPeriod,
        alertThreshold: Double = 80.0,
        startDate: LocalDate? = null
    ) {
        viewModelScope.launch {
            // Validation
            val validation = validateBudgetInput(name, category, amount, alertThreshold)
            if (!validation.isValid) {
                _uiState.value = _uiState.value.copy(error = validation.errorMessage)
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            
            try {
                val budget = Budget(
                    id = generateBudgetId(),
                    accountId = accountId,
                    name = name,
                    category = category,
                    amount = amount,
                    period = period,
                    startDate = startDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    endDate = calculateEndDate(period, startDate),
                    isActive = true,
                    alertThreshold = alertThreshold,
                    metadata = emptyMap()
                )
                
                val result = createBudgetUseCase(budget)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isCreating = false)
                        // Budget will be updated automatically through the repository flow
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isCreating = false,
                            error = exception.message ?: "Failed to create budget"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = e.message ?: "An error occurred while creating budget"
                )
            }
        }
    }

    fun updateBudget(
        budgetId: String,
        name: String? = null,
        amount: Money? = null,
        alertThreshold: Double? = null,
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)
            
            try {
                val currentBudget = _uiState.value.budgets.find { it.id == budgetId }
                if (currentBudget == null) {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = "Budget not found"
                    )
                    return@launch
                }
                
                val updatedBudget = currentBudget.copy(
                    name = name ?: currentBudget.name,
                    amount = amount ?: currentBudget.amount,
                    alertThreshold = alertThreshold ?: currentBudget.alertThreshold,
                    isActive = isActive ?: currentBudget.isActive
                )
                
                val result = updateBudgetUseCase(updatedBudget)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isUpdating = false)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = exception.message ?: "Failed to update budget"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = e.message ?: "An error occurred while updating budget"
                )
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            
            try {
                val result = deleteBudgetUseCase(budgetId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isDeleting = false)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            error = exception.message ?: "Failed to delete budget"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = e.message ?: "An error occurred while deleting budget"
                )
            }
        }
    }

    fun changePeriod(period: BudgetPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedPeriod = period, isLoading = true)
            
            try {
                budgetRepository.getBudgetsByPeriod(period).collect { budgets ->
                    val filteredBudgets = applyCurrentFilters(budgets)
                    val budgetStatuses = calculateBudgetStatuses(budgets)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        budgets = budgets,
                        filteredBudgets = filteredBudgets,
                        budgetStatuses = budgetStatuses
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load budgets for period"
                )
            }
        }
    }

    fun filterByCategory(category: String) {
        val currentState = _uiState.value
        val filteredBudgets = currentState.budgets.filter { it.category == category }
        
        _uiState.value = currentState.copy(
            selectedCategory = category,
            filteredBudgets = filteredBudgets
        )
    }

    fun clearCategoryFilter() {
        val currentState = _uiState.value
        
        _uiState.value = currentState.copy(
            selectedCategory = null,
            filteredBudgets = currentState.budgets
        )
    }

    fun refreshBudgetStatuses() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val budgetStatuses = calculateBudgetStatuses(currentState.budgets)
            
            _uiState.value = currentState.copy(budgetStatuses = budgetStatuses)
        }
    }

    fun getBudgetRecommendations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRecommendations = true)
            
            try {
                val result = getBudgetRecommendationsUseCase()
                result.fold(
                    onSuccess = { recommendations ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingRecommendations = false,
                            budgetRecommendations = recommendations
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingRecommendations = false,
                            error = exception.message ?: "Failed to get recommendations"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingRecommendations = false,
                    error = e.message ?: "An error occurred while getting recommendations"
                )
            }
        }
    }

    fun updateAlertThreshold(budgetId: String, threshold: Double) {
        updateBudget(budgetId = budgetId, alertThreshold = threshold)
    }

    fun toggleBudgetActive(budgetId: String, isActive: Boolean) {
        updateBudget(budgetId = budgetId, isActive = isActive)
    }

    fun calculateOverallBudgetHealth(): BudgetHealth {
        val currentState = _uiState.value
        val totalBudgeted = currentState.budgets
            .filter { it.isActive }
            .sumOf { it.amount.amount }
            
        val totalSpent = currentState.budgetStatuses.sumOf { it.spent.amount }
        val overBudgetCount = currentState.budgetStatuses.count { it.isOverBudget }
        val totalBudgetCount = currentState.budgets.count { it.isActive }
        
        return BudgetHealth(
            totalBudgeted = Money.fromDouble(totalBudgeted, "SAR"),
            totalSpent = Money.fromDouble(totalSpent, "SAR"),
            overBudgetCount = overBudgetCount,
            totalBudgetCount = totalBudgetCount,
            healthScore = calculateHealthScore(totalBudgeted, totalSpent, overBudgetCount, totalBudgetCount)
        )
    }

    fun getBudgetTemplates(): List<BudgetTemplate> {
        return listOf(
            BudgetTemplate(
                name = "Essential Expenses",
                categories = listOf(
                    "Groceries" to Money.fromDouble(800.0, "SAR"),
                    "Bills & Utilities" to Money.fromDouble(500.0, "SAR"),
                    "Transportation" to Money.fromDouble(400.0, "SAR")
                )
            ),
            BudgetTemplate(
                name = "Entertainment & Dining",
                categories = listOf(
                    "Food & Dining" to Money.fromDouble(600.0, "SAR"),
                    "Entertainment" to Money.fromDouble(300.0, "SAR"),
                    "Shopping" to Money.fromDouble(400.0, "SAR")
                )
            ),
            BudgetTemplate(
                name = "Savings & Investment",
                categories = listOf(
                    "Savings" to Money.fromDouble(1000.0, "SAR"),
                    "Investment" to Money.fromDouble(500.0, "SAR"),
                    "Emergency Fund" to Money.fromDouble(300.0, "SAR")
                )
            ),
            BudgetTemplate(
                name = "Healthcare & Education",
                categories = listOf(
                    "Healthcare" to Money.fromDouble(300.0, "SAR"),
                    "Education" to Money.fromDouble(200.0, "SAR"),
                    "Insurance" to Money.fromDouble(250.0, "SAR")
                )
            )
        )
    }

    fun applyBudgetTemplate(template: BudgetTemplate, period: BudgetPeriod) {
        viewModelScope.launch {
            template.categories.forEach { (category, amount) ->
                createBudget(
                    name = category,
                    category = category,
                    amount = amount,
                    period = period
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadBudgets()
    }

    private suspend fun calculateBudgetStatuses(budgets: List<Budget>): List<BudgetStatus> {
        return budgets.mapNotNull { budget ->
            try {
                val result = calculateBudgetStatusUseCase(budget)
                result.getOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun applyCurrentFilters(budgets: List<Budget>): List<Budget> {
        val currentState = _uiState.value
        
        return if (currentState.selectedCategory != null) {
            budgets.filter { it.category == currentState.selectedCategory }
        } else {
            budgets
        }
    }

    private fun validateBudgetInput(
        name: String,
        category: String,
        amount: Money,
        alertThreshold: Double
    ): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Budget name cannot be empty")
            category.isBlank() -> ValidationResult(false, "Category cannot be empty")
            amount.amount <= 0 -> ValidationResult(false, "Amount must be greater than zero")
            alertThreshold < 0 || alertThreshold > 100 -> ValidationResult(false, "Alert threshold must be between 0 and 100")
            else -> ValidationResult(true, null)
        }
    }

    private fun calculateEndDate(period: BudgetPeriod, startDate: LocalDate?): LocalDate? {
        val start = startDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (period) {
            BudgetPeriod.WEEKLY -> start.plus(kotlinx.datetime.DatePeriod(days = 7))
            BudgetPeriod.MONTHLY -> start.plus(kotlinx.datetime.DatePeriod(months = 1))
            BudgetPeriod.QUARTERLY -> start.plus(kotlinx.datetime.DatePeriod(months = 3))
            BudgetPeriod.YEARLY -> start.plus(kotlinx.datetime.DatePeriod(years = 1))
            BudgetPeriod.CUSTOM -> null // End date should be set manually for custom periods
        }
    }

    private fun calculateHealthScore(
        totalBudgeted: Double,
        totalSpent: Double,
        overBudgetCount: Int,
        totalBudgetCount: Int
    ): Double {
        if (totalBudgetCount == 0) return 100.0
        
        val spentRatio = if (totalBudgeted > 0) (totalSpent / totalBudgeted) else 0.0
        val overBudgetRatio = overBudgetCount.toDouble() / totalBudgetCount
        
        // Health score decreases with overspending and over-budget items
        val baseScore = 100.0
        val spentPenalty = if (spentRatio > 1.0) (spentRatio - 1.0) * 50 else 0.0
        val overBudgetPenalty = overBudgetRatio * 30
        
        return maxOf(0.0, baseScore - spentPenalty - overBudgetPenalty)
    }

    private fun generateBudgetId(): String {
        return "budget_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Budget UI State
 */
data class BudgetUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoadingRecommendations: Boolean = false,
    val error: String? = null,
    val budgets: List<Budget> = emptyList(),
    val filteredBudgets: List<Budget> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val budgetRecommendations: List<BudgetRecommendation> = emptyList(),
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val selectedCategory: String? = null,
    val lastRefreshTime: kotlinx.datetime.Instant? = null
)

/**
 * Budget Health Overview
 */
data class BudgetHealth(
    val totalBudgeted: Money,
    val totalSpent: Money,
    val overBudgetCount: Int,
    val totalBudgetCount: Int,
    val healthScore: Double
) {
    val remainingAmount: Money
        get() = Money.fromDouble(totalBudgeted.amount - totalSpent.amount, totalBudgeted.currency)
    
    val isHealthy: Boolean
        get() = healthScore >= 70.0
    
    val healthLevel: HealthLevel
        get() = when {
            healthScore >= 80 -> HealthLevel.EXCELLENT
            healthScore >= 60 -> HealthLevel.GOOD
            healthScore >= 40 -> HealthLevel.FAIR
            else -> HealthLevel.POOR
        }
}

/**
 * Budget Health Level
 */
enum class HealthLevel {
    EXCELLENT, GOOD, FAIR, POOR
}

/**
 * Budget Template for quick setup
 */
data class BudgetTemplate(
    val name: String,
    val categories: List<Pair<String, Money>>
)

/**
 * Budget Recommendation from analysis
 */
data class BudgetRecommendation(
    val category: String,
    val suggestedAmount: Money,
    val reasoning: String,
    val confidenceScore: Double
)

/**
 * Budget Status with spending analysis
 */
data class BudgetStatus(
    val budgetId: String,
    val spent: Money,
    val remaining: Money,
    val percentageUsed: Double,
    val isOverBudget: Boolean,
    val daysRemaining: Int
)

/**
 * Validation result for budget input
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)