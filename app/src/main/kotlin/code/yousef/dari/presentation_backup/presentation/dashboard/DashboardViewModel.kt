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
import kotlinx.datetime.minus
import kotlin.math.abs

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
                launch { loadSpendingVelocity() }
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

    private suspend fun loadSpendingVelocity() {
        try {
            // Get transactions for the last 14 days to calculate velocity
            getTransactionsUseCase("", limit = 100).collect { transactions ->
                val spendingVelocity = calculateSpendingVelocity(transactions)
                _uiState.value = _uiState.value.copy(spendingVelocity = spendingVelocity)
            }
        } catch (e: Exception) {
            // Handle error silently for dashboard
        }
    }

    private fun calculateSpendingVelocity(transactions: List<Transaction>): SpendingVelocityData {
        val now = Clock.System.now()
        val currentWeekStart = now.minus(7, DateTimeUnit.DAY)
        val previousWeekStart = now.minus(14, DateTimeUnit.DAY)

        // Filter expense transactions only
        val expenseTransactions = transactions.filter { 
            it.type == TransactionType.EXPENSE && it.amount.toDouble() < 0
        }

        // Calculate current week spending (last 7 days)
        val currentWeekTransactions = expenseTransactions.filter { 
            it.timestamp >= currentWeekStart 
        }
        val currentWeekSpending = abs(currentWeekTransactions.sumOf { it.amount.toDouble() })

        // Calculate previous week spending (days 8-14)
        val previousWeekTransactions = expenseTransactions.filter {
            it.timestamp >= previousWeekStart && it.timestamp < currentWeekStart
        }
        val previousWeekSpending = abs(previousWeekTransactions.sumOf { it.amount.toDouble() })

        // Calculate weekly trend percentage
        val weeklyTrendPercentage = if (previousWeekSpending > 0) {
            ((currentWeekSpending - previousWeekSpending) / previousWeekSpending) * 100.0
        } else if (currentWeekSpending > 0) {
            100.0 // If no previous spending but current spending exists
        } else {
            0.0 // No spending in either period
        }

        // Determine trend direction
        val trend = when {
            weeklyTrendPercentage > 5.0 -> SpendingTrend.INCREASING
            weeklyTrendPercentage < -5.0 -> SpendingTrend.DECREASING
            else -> SpendingTrend.STABLE
        }

        // Calculate daily average from last 7 days
        val dailyAverage = if (currentWeekTransactions.isNotEmpty()) {
            currentWeekSpending / 7.0
        } else {
            0.0
        }

        // Project monthly spending based on current velocity
        val projectedMonthlySpending = dailyAverage * 30.0

        return SpendingVelocityData(
            currentWeekSpending = Money.fromDouble(-currentWeekSpending, "SAR"),
            previousWeekSpending = Money.fromDouble(-previousWeekSpending, "SAR"),
            weeklyTrendPercentage = weeklyTrendPercentage,
            trend = trend,
            dailyAverageSpending = Money.fromDouble(-dailyAverage, "SAR"),
            projectedMonthlySpending = Money.fromDouble(-projectedMonthlySpending, "SAR")
        )
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
    val goalSummary: GoalSummaryData? = null,
    val spendingVelocity: SpendingVelocityData? = null
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

/**
 * Spending velocity data for dashboard
 */
data class SpendingVelocityData(
    val currentWeekSpending: Money,
    val previousWeekSpending: Money,
    val weeklyTrendPercentage: Double,
    val trend: SpendingTrend,
    val dailyAverageSpending: Money,
    val projectedMonthlySpending: Money
)

/**
 * Spending trend direction
 */
enum class SpendingTrend {
    INCREASING,
    DECREASING,
    STABLE
}