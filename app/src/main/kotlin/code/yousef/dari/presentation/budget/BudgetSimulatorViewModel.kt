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

/**
 * Budget Simulator ViewModel
 * Handles what-if budget scenarios, impact analysis, and cash flow projections
 */
class BudgetSimulatorViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase,
    private val forecastMonthlySpendingUseCase: ForecastMonthlySpendingUseCase,
    private val simulateExpenseAdditionUseCase: SimulateExpenseAdditionUseCase,
    private val simulateExpenseRemovalUseCase: SimulateExpenseRemovalUseCase,
    private val simulateIncomeChangeUseCase: SimulateIncomeChangeUseCase,
    private val calculateCashFlowUseCase: CalculateCashFlowUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetSimulatorUiState())
    val uiState: StateFlow<BudgetSimulatorUiState> = _uiState.asStateFlow()

    private val accountId: String = savedStateHandle.get<String>("accountId") ?: ""

    init {
        loadBaselineScenario()
    }

    private fun loadBaselineScenario() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = forecastMonthlySpendingUseCase()
                result.fold(
                    onSuccess = { baselineScenario ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            baselineScenario = baselineScenario
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load baseline scenario"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }

    fun addExpenseScenario(
        name: String,
        category: String,
        amount: Money,
        frequency: ExpenseFrequency
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val baselineScenario = currentState.baselineScenario ?: return@launch
            
            _uiState.value = currentState.copy(isCreating = true, error = null)
            
            try {
                val expenseAddition = ExpenseAddition(
                    name = name,
                    category = category,
                    amount = amount,
                    frequency = frequency
                )
                
                val result = simulateExpenseAdditionUseCase(baselineScenario, expenseAddition)
                result.fold(
                    onSuccess = { newScenario ->
                        val updatedScenarios = currentState.scenarios + newScenario
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            scenarios = updatedScenarios
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            error = exception.message ?: "Failed to create expense scenario"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isCreating = false,
                    error = e.message ?: "An error occurred while creating scenario"
                )
            }
        }
    }

    fun removeExpenseScenario(
        category: String,
        reductionAmount: Money
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val baselineScenario = currentState.baselineScenario ?: return@launch
            
            _uiState.value = currentState.copy(isCreating = true, error = null)
            
            try {
                val result = simulateExpenseRemovalUseCase(baselineScenario, category, reductionAmount)
                result.fold(
                    onSuccess = { newScenario ->
                        val updatedScenarios = currentState.scenarios + newScenario
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            scenarios = updatedScenarios
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            error = exception.message ?: "Failed to create expense reduction scenario"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isCreating = false,
                    error = e.message ?: "An error occurred while creating scenario"
                )
            }
        }
    }

    fun adjustIncomeScenario(newIncome: Money) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val baselineScenario = currentState.baselineScenario ?: return@launch
            
            _uiState.value = currentState.copy(isCreating = true, error = null)
            
            try {
                val result = simulateIncomeChangeUseCase(baselineScenario, newIncome)
                result.fold(
                    onSuccess = { newScenario ->
                        val updatedScenarios = currentState.scenarios + newScenario
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            scenarios = updatedScenarios
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            isCreating = false,
                            error = exception.message ?: "Failed to create income change scenario"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isCreating = false,
                    error = e.message ?: "An error occurred while creating scenario"
                )
            }
        }
    }

    fun compareScenarios(scenario1Id: String, scenario2Id: String): ScenarioComparison? {
        val currentState = _uiState.value
        val scenario1 = currentState.scenarios.find { it.id == scenario1Id }
        val scenario2 = currentState.scenarios.find { it.id == scenario2Id }
        
        if (scenario1 == null || scenario2 == null) return null
        
        val impactDifference = Money.fromDouble(
            scenario2.cashFlowImpact.amount - scenario1.cashFlowImpact.amount,
            scenario1.totalBudget.currency
        )
        
        val savingsDifference = Money.fromDouble(
            scenario2.projectedSavings.amount - scenario1.projectedSavings.amount,
            scenario1.projectedSavings.currency
        )
        
        val betterScenarioId = if (scenario2.projectedSavings.amount > scenario1.projectedSavings.amount) {
            scenario2.id
        } else {
            scenario1.id
        }
        
        val recommendation = when {
            savingsDifference.amount > 100 -> "Scenario 2 provides significantly better savings potential"
            savingsDifference.amount > 0 -> "Scenario 2 offers modest savings improvement"
            savingsDifference.amount < -100 -> "Scenario 1 provides significantly better savings potential"
            savingsDifference.amount < 0 -> "Scenario 1 offers modest savings improvement"
            else -> "Both scenarios have similar financial impact"
        }
        
        return ScenarioComparison(
            scenario1Id = scenario1.id,
            scenario2Id = scenario2.id,
            scenario1Name = scenario1.name,
            scenario2Name = scenario2.name,
            impactDifference = impactDifference,
            savingsDifference = savingsDifference,
            recommendation = recommendation,
            betterScenarioId = betterScenarioId,
            confidenceScore = 0.8 // Simple confidence score
        )
    }

    fun calculateCashFlowProjection(scenario: BudgetScenario, months: Int) {
        viewModelScope.launch {
            try {
                val result = calculateCashFlowUseCase(scenario, months)
                result.fold(
                    onSuccess = { projection ->
                        _uiState.value = _uiState.value.copy(cashFlowProjection = projection)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to calculate cash flow projection"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An error occurred while calculating projection"
                )
            }
        }
    }

    fun saveScenario(scenarioId: String, customName: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedScenarios = currentState.scenarios.map { scenario ->
                if (scenario.id == scenarioId) {
                    scenario.copy(name = customName, isSaved = true)
                } else {
                    scenario
                }
            }
            
            _uiState.value = currentState.copy(scenarios = updatedScenarios)
        }
    }

    fun deleteScenario(scenarioId: String) {
        val currentState = _uiState.value
        val updatedScenarios = currentState.scenarios.filter { it.id != scenarioId }
        
        _uiState.value = currentState.copy(
            scenarios = updatedScenarios,
            selectedScenarioId = if (currentState.selectedScenarioId == scenarioId) null else currentState.selectedScenarioId
        )
    }

    fun selectScenario(scenarioId: String) {
        _uiState.value = _uiState.value.copy(selectedScenarioId = scenarioId)
    }

    fun addScenario(scenario: BudgetScenario) {
        val currentState = _uiState.value
        val updatedScenarios = currentState.scenarios + scenario
        _uiState.value = currentState.copy(scenarios = updatedScenarios)
    }

    fun getScenarioTemplates(): List<ScenarioTemplate> {
        return listOf(
            ScenarioTemplate(
                name = "Emergency Fund Builder",
                description = "Reduce discretionary spending to build emergency fund faster",
                type = ScenarioType.EXPENSE_REMOVAL,
                modifications = listOf(
                    ExpenseRemoval("Entertainment", Money.fromDouble(200.0, "SAR")),
                    ExpenseRemoval("Dining Out", Money.fromDouble(300.0, "SAR"))
                ),
                tags = listOf("savings", "emergency"),
                category = "Financial Security"
            ),
            ScenarioTemplate(
                name = "Salary Increase Impact",
                description = "See how a salary increase affects your savings potential",
                type = ScenarioType.INCOME_CHANGE,
                modifications = listOf(
                    IncomeAdjustment(
                        currentIncome = Money.fromDouble(8000.0, "SAR"),
                        newIncome = Money.fromDouble(9000.0, "SAR"),
                        changeType = IncomeChangeType.INCREASE
                    )
                ),
                tags = listOf("career", "income"),
                category = "Career Growth"
            ),
            ScenarioTemplate(
                name = "New Car Purchase",
                description = "Impact of adding a car loan to your monthly expenses",
                type = ScenarioType.EXPENSE_ADDITION,
                modifications = listOf(
                    ExpenseAddition(
                        name = "Car Loan",
                        category = "Transportation",
                        amount = Money.fromDouble(800.0, "SAR"),
                        frequency = ExpenseFrequency.MONTHLY
                    )
                ),
                tags = listOf("transportation", "loan"),
                category = "Major Purchase"
            )
        )
    }

    fun applyScenarioTemplate(template: ScenarioTemplate) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val baselineScenario = currentState.baselineScenario ?: return@launch
            
            try {
                // Apply first modification from template
                val modification = template.modifications.firstOrNull() ?: return@launch
                
                val result = when (modification) {
                    is ExpenseAddition -> simulateExpenseAdditionUseCase(baselineScenario, modification)
                    is ExpenseRemoval -> simulateExpenseRemovalUseCase(baselineScenario, modification.category, modification.amount)
                    is IncomeAdjustment -> simulateIncomeChangeUseCase(baselineScenario, modification.newIncome)
                }
                
                result.fold(
                    onSuccess = { newScenario ->
                        val templatedScenario = newScenario.copy(
                            name = template.name,
                            description = template.description
                        )
                        addScenario(templatedScenario)
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            error = exception.message ?: "Failed to apply template"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    error = e.message ?: "An error occurred while applying template"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(scenarios = emptyList())
        loadBaselineScenario()
    }
}

/**
 * Budget Simulator UI State
 */
data class BudgetSimulatorUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val baselineScenario: BudgetScenario? = null,
    val scenarios: List<BudgetScenario> = emptyList(),
    val selectedScenarioId: String? = null,
    val cashFlowProjection: List<CashFlowProjection>? = null,
    val lastRefreshTime: kotlinx.datetime.Instant? = null
)