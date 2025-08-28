package code.yousef.dari.presentation.budget

import androidx.lifecycle.SavedStateHandle
import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.budget.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetSimulatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase = mockk(relaxed = true)
    private val forecastMonthlySpendingUseCase: ForecastMonthlySpendingUseCase = mockk(relaxed = true)
    private val simulateExpenseAdditionUseCase: SimulateExpenseAdditionUseCase = mockk(relaxed = true)
    private val simulateExpenseRemovalUseCase: SimulateExpenseRemovalUseCase = mockk(relaxed = true)
    private val simulateIncomeChangeUseCase: SimulateIncomeChangeUseCase = mockk(relaxed = true)
    private val calculateCashFlowUseCase: CalculateCashFlowUseCase = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)

    private lateinit var viewModel: BudgetSimulatorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        every { savedStateHandle.get<String>("accountId") } returns "test-account-id"
        
        // Create sample budgets and baseline scenario
        val sampleBudgets = createSampleBudgets()
        every { budgetRepository.getBudgets() } returns flowOf(sampleBudgets)
        
        val baselineScenario = createBaselineScenario()
        every { forecastMonthlySpendingUseCase() } returns Result.success(baselineScenario)
        
        viewModel = BudgetSimulatorViewModel(
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            calculateBudgetStatusUseCase = calculateBudgetStatusUseCase,
            forecastMonthlySpendingUseCase = forecastMonthlySpendingUseCase,
            simulateExpenseAdditionUseCase = simulateExpenseAdditionUseCase,
            simulateExpenseRemovalUseCase = simulateExpenseRemovalUseCase,
            simulateIncomeChangeUseCase = simulateIncomeChangeUseCase,
            calculateCashFlowUseCase = calculateCashFlowUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading with empty scenarios`() {
        // Given - ViewModel is created in setup()
        
        // Then
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertEquals(emptyList(), initialState.scenarios)
        assertNotNull(initialState.baselineScenario)
    }

    @Test
    fun `loadBaselineScenario should update state with current spending forecast`() = runTest {
        // Given
        val baselineScenario = createBaselineScenario()
        coEvery { forecastMonthlySpendingUseCase() } returns Result.success(baselineScenario)

        // When
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(baselineScenario, finalState.baselineScenario)
        assertTrue(finalState.scenarios.isEmpty())
    }

    @Test
    fun `addExpenseScenario should create new scenario with additional expense`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val expenseAddition = ExpenseAddition(
            name = "Gym Membership",
            category = "Health & Fitness",
            amount = Money.fromDouble(200.0, "SAR"),
            frequency = ExpenseFrequency.MONTHLY
        )
        
        val resultScenario = BudgetScenario(
            id = "scenario_1",
            name = "Add Gym Membership",
            type = ScenarioType.EXPENSE_ADDITION,
            totalBudget = Money.fromDouble(5200.0, "SAR"), // 5000 + 200
            totalSpent = Money.fromDouble(3200.0, "SAR"),
            categoryBreakdown = mapOf(
                "Food & Dining" to Money.fromDouble(1000.0, "SAR"),
                "Transportation" to Money.fromDouble(500.0, "SAR"),
                "Entertainment" to Money.fromDouble(300.0, "SAR"),
                "Health & Fitness" to Money.fromDouble(200.0, "SAR")
            ),
            projectedSavings = Money.fromDouble(2000.0, "SAR"),
            cashFlowImpact = Money.fromDouble(-200.0, "SAR"),
            modifications = listOf(expenseAddition)
        )
        
        coEvery { simulateExpenseAdditionUseCase(any(), any()) } returns Result.success(resultScenario)

        // When
        viewModel.addExpenseScenario(
            name = "Gym Membership",
            category = "Health & Fitness",
            amount = Money.fromDouble(200.0, "SAR"),
            frequency = ExpenseFrequency.MONTHLY
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCreating)
        assertEquals(1, finalState.scenarios.size)
        assertEquals("Add Gym Membership", finalState.scenarios[0].name)
        assertEquals(ScenarioType.EXPENSE_ADDITION, finalState.scenarios[0].type)
        assertEquals(Money.fromDouble(-200.0, "SAR"), finalState.scenarios[0].cashFlowImpact)
    }

    @Test
    fun `removeExpenseScenario should create scenario without specified expense`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val expenseRemoval = ExpenseRemoval(
            category = "Entertainment", 
            amount = Money.fromDouble(150.0, "SAR")
        )
        
        val resultScenario = BudgetScenario(
            id = "scenario_2",
            name = "Reduce Entertainment",
            type = ScenarioType.EXPENSE_REMOVAL,
            totalBudget = Money.fromDouble(4850.0, "SAR"), // 5000 - 150
            totalSpent = Money.fromDouble(2850.0, "SAR"),
            categoryBreakdown = mapOf(
                "Food & Dining" to Money.fromDouble(1000.0, "SAR"),
                "Transportation" to Money.fromDouble(500.0, "SAR"),
                "Entertainment" to Money.fromDouble(150.0, "SAR") // Reduced from 300
            ),
            projectedSavings = Money.fromDouble(2000.0, "SAR"),
            cashFlowImpact = Money.fromDouble(150.0, "SAR"),
            modifications = listOf(expenseRemoval)
        )
        
        coEvery { simulateExpenseRemovalUseCase(any(), any(), any()) } returns Result.success(resultScenario)

        // When
        viewModel.removeExpenseScenario(
            category = "Entertainment",
            reductionAmount = Money.fromDouble(150.0, "SAR")
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCreating)
        assertEquals(1, finalState.scenarios.size)
        assertEquals("Reduce Entertainment", finalState.scenarios[0].name)
        assertEquals(ScenarioType.EXPENSE_REMOVAL, finalState.scenarios[0].type)
        assertEquals(Money.fromDouble(150.0, "SAR"), finalState.scenarios[0].cashFlowImpact)
    }

    @Test
    fun `adjustIncomeScenario should create scenario with income change`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val incomeChange = IncomeAdjustment(
            currentIncome = Money.fromDouble(8000.0, "SAR"),
            newIncome = Money.fromDouble(9000.0, "SAR"),
            changeType = IncomeChangeType.INCREASE
        )
        
        val resultScenario = BudgetScenario(
            id = "scenario_3",
            name = "Income Increase",
            type = ScenarioType.INCOME_CHANGE,
            totalBudget = Money.fromDouble(5000.0, "SAR"), // Same budget
            totalSpent = Money.fromDouble(3000.0, "SAR"),
            categoryBreakdown = mapOf(
                "Food & Dining" to Money.fromDouble(1000.0, "SAR"),
                "Transportation" to Money.fromDouble(500.0, "SAR"),
                "Entertainment" to Money.fromDouble(300.0, "SAR")
            ),
            projectedSavings = Money.fromDouble(3000.0, "SAR"), // Increased from 2000
            cashFlowImpact = Money.fromDouble(1000.0, "SAR"),
            modifications = listOf(incomeChange)
        )
        
        coEvery { simulateIncomeChangeUseCase(any(), any()) } returns Result.success(resultScenario)

        // When
        viewModel.adjustIncomeScenario(
            newIncome = Money.fromDouble(9000.0, "SAR")
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCreating)
        assertEquals(1, finalState.scenarios.size)
        assertEquals("Income Increase", finalState.scenarios[0].name)
        assertEquals(ScenarioType.INCOME_CHANGE, finalState.scenarios[0].type)
        assertEquals(Money.fromDouble(1000.0, "SAR"), finalState.scenarios[0].cashFlowImpact)
    }

    @Test
    fun `compareScenarios should provide impact analysis between scenarios`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        // Create two scenarios
        val scenario1 = createSampleScenario("scenario_1", ScenarioType.EXPENSE_ADDITION)
        val scenario2 = createSampleScenario("scenario_2", ScenarioType.EXPENSE_REMOVAL)
        
        viewModel.addScenario(scenario1)
        viewModel.addScenario(scenario2)

        // When
        val comparison = viewModel.compareScenarios(scenario1.id, scenario2.id)

        // Then
        assertNotNull(comparison)
        assertEquals(scenario1.id, comparison.scenario1Id)
        assertEquals(scenario2.id, comparison.scenario2Id)
        assertTrue(comparison.impactDifference.amount != 0.0)
        assertNotNull(comparison.recommendation)
    }

    @Test
    fun `calculateCashFlowProjection should project future cash flow`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val scenario = createSampleScenario("scenario_1", ScenarioType.EXPENSE_ADDITION)
        val cashFlowProjection = listOf(
            CashFlowProjection(
                month = 1,
                income = Money.fromDouble(8000.0, "SAR"),
                expenses = Money.fromDouble(5200.0, "SAR"),
                netCashFlow = Money.fromDouble(2800.0, "SAR"),
                cumulativeSavings = Money.fromDouble(2800.0, "SAR")
            ),
            CashFlowProjection(
                month = 2,
                income = Money.fromDouble(8000.0, "SAR"),
                expenses = Money.fromDouble(5200.0, "SAR"),
                netCashFlow = Money.fromDouble(2800.0, "SAR"),
                cumulativeSavings = Money.fromDouble(5600.0, "SAR")
            )
        )
        
        coEvery { calculateCashFlowUseCase(any(), any()) } returns Result.success(cashFlowProjection)

        // When
        viewModel.calculateCashFlowProjection(scenario, 6)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertNotNull(finalState.cashFlowProjection)
        assertEquals(2, finalState.cashFlowProjection!!.size)
        assertEquals(Money.fromDouble(2800.0, "SAR"), finalState.cashFlowProjection!![0].netCashFlow)
        assertEquals(Money.fromDouble(5600.0, "SAR"), finalState.cashFlowProjection!![1].cumulativeSavings)
    }

    @Test
    fun `saveScenario should persist scenario with custom name`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val scenario = createSampleScenario("temp_scenario", ScenarioType.EXPENSE_ADDITION)
        viewModel.addScenario(scenario)
        
        val customName = "My Savings Plan"

        // When
        viewModel.saveScenario(scenario.id, customName)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        val savedScenario = finalState.scenarios.find { it.id == scenario.id }
        assertNotNull(savedScenario)
        assertEquals(customName, savedScenario.name)
        assertTrue(savedScenario.isSaved)
    }

    @Test
    fun `deleteScenario should remove scenario from list`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val scenario1 = createSampleScenario("scenario_1", ScenarioType.EXPENSE_ADDITION)
        val scenario2 = createSampleScenario("scenario_2", ScenarioType.EXPENSE_REMOVAL)
        
        viewModel.addScenario(scenario1)
        viewModel.addScenario(scenario2)
        
        assertEquals(2, viewModel.uiState.value.scenarios.size)

        // When
        viewModel.deleteScenario(scenario1.id)

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(1, finalState.scenarios.size)
        assertEquals(scenario2.id, finalState.scenarios[0].id)
    }

    @Test
    fun `selectScenario should update current selected scenario`() {
        // Given
        val scenario = createSampleScenario("scenario_1", ScenarioType.EXPENSE_ADDITION)
        viewModel.addScenario(scenario)

        // When
        viewModel.selectScenario(scenario.id)

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(scenario.id, finalState.selectedScenarioId)
    }

    @Test
    fun `error handling should update error state correctly`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val errorMessage = "Simulation failed"
        coEvery { simulateExpenseAdditionUseCase(any(), any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.addExpenseScenario(
            name = "Test Expense",
            category = "Test Category",
            amount = Money.fromDouble(100.0, "SAR"),
            frequency = ExpenseFrequency.MONTHLY
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(errorMessage, finalState.error)
        assertFalse(finalState.isCreating)
    }

    @Test
    fun `refreshData should reload baseline and clear scenarios`() = runTest {
        // Given
        advanceUntilIdle() // Load baseline
        
        val scenario = createSampleScenario("scenario_1", ScenarioType.EXPENSE_ADDITION)
        viewModel.addScenario(scenario)
        assertEquals(1, viewModel.uiState.value.scenarios.size)

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(0, finalState.scenarios.size) // Scenarios cleared on refresh
        assertNotNull(finalState.baselineScenario)
    }

    // Helper methods
    private fun createSampleBudgets(): List<Budget> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            Budget(
                id = "budget1",
                accountId = "test-account-id",
                name = "Food & Dining",
                category = "Food & Dining",
                amount = Money.fromDouble(1000.0, "SAR"),
                period = BudgetPeriod.MONTHLY,
                startDate = today,
                endDate = null,
                isActive = true,
                alertThreshold = 80.0,
                metadata = emptyMap()
            ),
            Budget(
                id = "budget2",
                accountId = "test-account-id",
                name = "Transportation",
                category = "Transportation",
                amount = Money.fromDouble(500.0, "SAR"),
                period = BudgetPeriod.MONTHLY,
                startDate = today,
                endDate = null,
                isActive = true,
                alertThreshold = 75.0,
                metadata = emptyMap()
            ),
            Budget(
                id = "budget3",
                accountId = "test-account-id",
                name = "Entertainment",
                category = "Entertainment",
                amount = Money.fromDouble(300.0, "SAR"),
                period = BudgetPeriod.MONTHLY,
                startDate = today,
                endDate = null,
                isActive = true,
                alertThreshold = 85.0,
                metadata = emptyMap()
            )
        )
    }

    private fun createBaselineScenario(): BudgetScenario {
        return BudgetScenario(
            id = "baseline",
            name = "Current Spending",
            type = ScenarioType.BASELINE,
            totalBudget = Money.fromDouble(5000.0, "SAR"),
            totalSpent = Money.fromDouble(3000.0, "SAR"),
            categoryBreakdown = mapOf(
                "Food & Dining" to Money.fromDouble(1000.0, "SAR"),
                "Transportation" to Money.fromDouble(500.0, "SAR"),
                "Entertainment" to Money.fromDouble(300.0, "SAR")
            ),
            projectedSavings = Money.fromDouble(2000.0, "SAR"),
            cashFlowImpact = Money.fromDouble(0.0, "SAR"),
            modifications = emptyList(),
            isSaved = false
        )
    }

    private fun createSampleScenario(id: String, type: ScenarioType): BudgetScenario {
        return BudgetScenario(
            id = id,
            name = when (type) {
                ScenarioType.EXPENSE_ADDITION -> "Add New Expense"
                ScenarioType.EXPENSE_REMOVAL -> "Reduce Expense"
                ScenarioType.INCOME_CHANGE -> "Income Change"
                ScenarioType.BASELINE -> "Baseline"
            },
            type = type,
            totalBudget = Money.fromDouble(5000.0, "SAR"),
            totalSpent = Money.fromDouble(3000.0, "SAR"),
            categoryBreakdown = mapOf(
                "Food & Dining" to Money.fromDouble(1000.0, "SAR"),
                "Transportation" to Money.fromDouble(500.0, "SAR"),
                "Entertainment" to Money.fromDouble(300.0, "SAR")
            ),
            projectedSavings = Money.fromDouble(2000.0, "SAR"),
            cashFlowImpact = when (type) {
                ScenarioType.EXPENSE_ADDITION -> Money.fromDouble(-200.0, "SAR")
                ScenarioType.EXPENSE_REMOVAL -> Money.fromDouble(200.0, "SAR")
                ScenarioType.INCOME_CHANGE -> Money.fromDouble(1000.0, "SAR")
                ScenarioType.BASELINE -> Money.fromDouble(0.0, "SAR")
            },
            modifications = emptyList(),
            isSaved = false
        )
    }
}