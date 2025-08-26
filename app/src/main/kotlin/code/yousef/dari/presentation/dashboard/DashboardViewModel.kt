package code.yousef.dari.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.GoalRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit

/**
 * Dashboard ViewModel
 * Manages dashboard state including accounts, transactions, budgets, and goals
 */
class DashboardViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load data in parallel
                launch { loadAccountSummary() }
                launch { loadRecentTransactions() }
                launch { loadBudgetSummary() }
                launch { loadGoalSummary() }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    private suspend fun loadAccountSummary() {
        try {
            getAccountsUseCase().collect { accounts ->
                val totalBalance = accounts.sumOf { it.currentBalance?.toDouble() ?: 0.0 }
                val availableBalance = accounts.sumOf { it.availableBalance?.toDouble() ?: 0.0 }
                
                val summary = AccountSummaryData(
                    totalBalance = Money.fromDouble(totalBalance, "SAR"),
                    availableBalance = Money.fromDouble(availableBalance, "SAR"),
                    accountCount = accounts.size,
                    accounts = accounts.take(3) // Show top 3 accounts
                )
                
                _uiState.value = _uiState.value.copy(
                    accountSummary = summary,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load accounts: ${e.message}",
                isLoading = false
            )
        }
    }

    private suspend fun loadRecentTransactions() {
        try {
            // Get recent transactions (last 10)
            getTransactionsUseCase("", limit = 10).collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    recentTransactions = transactions
                )
            }
        } catch (e: Exception) {
            // Handle error - don't update the error state as other data might load successfully
        }
    }

    private suspend fun loadBudgetSummary() {
        try {
            val budgetsResult = budgetRepository.getActiveBudgets()
            budgetsResult.onSuccess { budgets ->
                if (budgets.isNotEmpty()) {
                    val currentBudget = budgets.first()
                    val spentPercentage = if (currentBudget.totalAmount.toDouble() > 0) {
                        (currentBudget.spentAmount.toDouble() / currentBudget.totalAmount.toDouble()) * 100.0
                    } else 0.0
                    
                    val summary = BudgetSummaryData(
                        currentBudget = currentBudget,
                        spentPercentage = spentPercentage,
                        remainingAmount = currentBudget.remainingAmount,
                        status = when {
                            spentPercentage >= 100.0 -> BudgetStatus.OVER_BUDGET
                            spentPercentage >= currentBudget.alertThreshold -> BudgetStatus.NEAR_LIMIT
                            else -> BudgetStatus.ON_TRACK
                        }
                    )
                    
                    _uiState.value = _uiState.value.copy(budgetSummary = summary)
                }
            }
        } catch (e: Exception) {
            // Handle error silently for dashboard
        }
    }

    private suspend fun loadGoalSummary() {
        try {
            val goalsResult = goalRepository.getActiveGoals()
            goalsResult.onSuccess { goals ->
                if (goals.isNotEmpty()) {
                    val totalTargetAmount = goals.sumOf { it.targetAmount.toDouble() }
                    val totalCurrentAmount = goals.sumOf { it.currentAmount.toDouble() }
                    val progressPercentage = if (totalTargetAmount > 0) {
                        (totalCurrentAmount / totalTargetAmount) * 100.0
                    } else 0.0
                    
                    val summary = GoalSummaryData(
                        totalGoals = goals.size,
                        completedGoals = goals.count { it.isCompleted },
                        totalTargetAmount = Money.fromDouble(totalTargetAmount, "SAR"),
                        totalCurrentAmount = Money.fromDouble(totalCurrentAmount, "SAR"),
                        progressPercentage = progressPercentage,
                        topGoals = goals.take(2) // Show top 2 goals
                    )
                    
                    _uiState.value = _uiState.value.copy(goalSummary = summary)
                }
            }
        } catch (e: Exception) {
            // Handle error silently for dashboard
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Dashboard UI State
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountSummary: AccountSummaryData? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val budgetSummary: BudgetSummaryData? = null,
    val goalSummary: GoalSummaryData? = null
)

/**
 * Account summary data for dashboard
 */
data class AccountSummaryData(
    val totalBalance: Money,
    val availableBalance: Money,
    val accountCount: Int,
    val accounts: List<FinancialAccount>
)

/**
 * Budget summary data for dashboard
 */
data class BudgetSummaryData(
    val currentBudget: Budget,
    val spentPercentage: Double,
    val remainingAmount: Money,
    val status: BudgetStatus
)

/**
 * Goal summary data for dashboard
 */
data class GoalSummaryData(
    val totalGoals: Int,
    val completedGoals: Int,
    val totalTargetAmount: Money,
    val totalCurrentAmount: Money,
    val progressPercentage: Double,
    val topGoals: List<Goal>
)

/**
 * Budget status for dashboard
 */
enum class BudgetStatus {
    ON_TRACK,
    NEAR_LIMIT,
    OVER_BUDGET
}