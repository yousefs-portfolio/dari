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
class BudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val createBudgetUseCase: CreateBudgetUseCase = mockk(relaxed = true)
    private val updateBudgetUseCase: UpdateBudgetUseCase = mockk(relaxed = true)
    private val deleteBudgetUseCase: DeleteBudgetUseCase = mockk(relaxed = true)
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase = mockk(relaxed = true)
    private val getBudgetRecommendationsUseCase: GetBudgetRecommendationsUseCase = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)

    private lateinit var viewModel: BudgetViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        every { savedStateHandle.get<String>("accountId") } returns "test-account-id"
        
        // Create sample budgets
        val sampleBudgets = createSampleBudgets()
        every { budgetRepository.getBudgets() } returns flowOf(sampleBudgets)
        
        viewModel = BudgetViewModel(
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            createBudgetUseCase = createBudgetUseCase,
            updateBudgetUseCase = updateBudgetUseCase,
            deleteBudgetUseCase = deleteBudgetUseCase,
            calculateBudgetStatusUseCase = calculateBudgetStatusUseCase,
            getBudgetRecommendationsUseCase = getBudgetRecommendationsUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading with empty budgets`() {
        // Given - ViewModel is created in setup()
        
        // Then
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertEquals(emptyList(), initialState.budgets)
        assertEquals(emptyList(), initialState.budgetStatuses)
        assertEquals(BudgetPeriod.MONTHLY, initialState.selectedPeriod)
    }

    @Test
    fun `loadBudgets should update state with budgets and calculate statuses`() = runTest {
        // Given
        val sampleBudgets = createSampleBudgets()
        val budgetStatuses = sampleBudgets.map { budget ->
            BudgetStatus(
                budgetId = budget.id,
                spent = Money.fromDouble(500.0, "SAR"),
                remaining = Money.fromDouble(500.0, "SAR"),
                percentageUsed = 50.0,
                isOverBudget = false,
                daysRemaining = 15
            )
        }
        
        every { budgetRepository.getBudgets() } returns flowOf(sampleBudgets)
        coEvery { calculateBudgetStatusUseCase(any()) } returns Result.success(budgetStatuses[0])

        // When
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(sampleBudgets, finalState.budgets)
        assertTrue(finalState.budgetStatuses.isNotEmpty())
    }

    @Test
    fun `createBudget should add new budget successfully`() = runTest {
        // Given
        val newBudget = Budget(
            id = "new-budget",
            accountId = "test-account-id",
            name = "New Budget",
            category = "Food & Dining",
            amount = Money.fromDouble(1000.0, "SAR"),
            period = BudgetPeriod.MONTHLY,
            startDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            endDate = null,
            isActive = true,
            alertThreshold = 80.0,
            metadata = emptyMap()
        )
        
        coEvery { createBudgetUseCase(any()) } returns Result.success(newBudget)

        // When
        viewModel.createBudget(
            name = "New Budget",
            category = "Food & Dining",
            amount = Money.fromDouble(1000.0, "SAR"),
            period = BudgetPeriod.MONTHLY
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCreating)
        coVerify {
            createBudgetUseCase(
                match { budget ->
                    budget.name == "New Budget" &&
                    budget.category == "Food & Dining" &&
                    budget.amount.amount == 1000.0 &&
                    budget.period == BudgetPeriod.MONTHLY
                }
            )
        }
    }

    @Test
    fun `updateBudget should modify existing budget`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val budgetId = "budget1"
        val updatedBudget = createSampleBudgets()[0].copy(
            amount = Money.fromDouble(1500.0, "SAR"),
            alertThreshold = 85.0
        )
        
        coEvery { updateBudgetUseCase(any()) } returns Result.success(updatedBudget)

        // When
        viewModel.updateBudget(
            budgetId = budgetId,
            amount = Money.fromDouble(1500.0, "SAR"),
            alertThreshold = 85.0
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isUpdating)
        coVerify { updateBudgetUseCase(match { it.id == budgetId && it.amount.amount == 1500.0 }) }
    }

    @Test
    fun `deleteBudget should remove budget successfully`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val budgetId = "budget1"
        coEvery { deleteBudgetUseCase(budgetId) } returns Result.success(Unit)

        // When
        viewModel.deleteBudget(budgetId)
        advanceUntilIdle()

        // Then
        coVerify { deleteBudgetUseCase(budgetId) }
    }

    @Test
    fun `changePeriod should update selected period and reload budgets`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val newPeriod = BudgetPeriod.WEEKLY
        val weeklyBudgets = listOf(
            createSampleBudgets()[0].copy(period = BudgetPeriod.WEEKLY)
        )
        
        every { budgetRepository.getBudgetsByPeriod(newPeriod) } returns flowOf(weeklyBudgets)

        // When
        viewModel.changePeriod(newPeriod)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(newPeriod, finalState.selectedPeriod)
        assertEquals(weeklyBudgets, finalState.budgets)
    }

    @Test
    fun `filterByCategory should show budgets for selected category`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val category = "Food & Dining"

        // When
        viewModel.filterByCategory(category)

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(category, finalState.selectedCategory)
        assertTrue(finalState.filteredBudgets.all { it.category == category })
    }

    @Test
    fun `clearCategoryFilter should show all budgets`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        viewModel.filterByCategory("Food & Dining") // Apply filter first

        // When
        viewModel.clearCategoryFilter()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(null, finalState.selectedCategory)
        assertEquals(finalState.budgets, finalState.filteredBudgets)
    }

    @Test
    fun `calculateOverallBudgetHealth should return correct metrics`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val budgetStatuses = listOf(
            BudgetStatus(
                budgetId = "budget1",
                spent = Money.fromDouble(800.0, "SAR"),
                remaining = Money.fromDouble(200.0, "SAR"),
                percentageUsed = 80.0,
                isOverBudget = false,
                daysRemaining = 10
            ),
            BudgetStatus(
                budgetId = "budget2",
                spent = Money.fromDouble(1200.0, "SAR"),
                remaining = Money.fromDouble(-200.0, "SAR"),
                percentageUsed = 120.0,
                isOverBudget = true,
                daysRemaining = 10
            )
        )
        
        // Mock the budget statuses
        budgetStatuses.forEach { status ->
            coEvery { calculateBudgetStatusUseCase(match { it.id == status.budgetId }) } returns Result.success(status)
        }
        
        // Trigger recalculation
        viewModel.refreshBudgetStatuses()
        advanceUntilIdle()

        // When
        val health = viewModel.calculateOverallBudgetHealth()

        // Then
        assertTrue(health.totalBudgeted.amount > 0)
        assertTrue(health.totalSpent.amount > 0)
        assertEquals(1, health.overBudgetCount)
        assertEquals(2, health.totalBudgetCount)
    }

    @Test
    fun `getBudgetRecommendations should provide spending insights`() = runTest {
        // Given
        val recommendations = listOf(
            BudgetRecommendation(
                category = "Food & Dining",
                suggestedAmount = Money.fromDouble(800.0, "SAR"),
                reasoning = "Based on last 3 months average",
                confidenceScore = 0.85
            ),
            BudgetRecommendation(
                category = "Transportation",
                suggestedAmount = Money.fromDouble(300.0, "SAR"),
                reasoning = "Seasonal adjustment",
                confidenceScore = 0.75
            )
        )
        
        coEvery { getBudgetRecommendationsUseCase() } returns Result.success(recommendations)

        // When
        viewModel.getBudgetRecommendations()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(recommendations, finalState.budgetRecommendations)
        assertFalse(finalState.isLoadingRecommendations)
    }

    @Test
    fun `toggleBudgetAlert should update alert threshold`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val budgetId = "budget1"
        val newThreshold = 90.0
        val updatedBudget = createSampleBudgets()[0].copy(alertThreshold = newThreshold)
        
        coEvery { updateBudgetUseCase(any()) } returns Result.success(updatedBudget)

        // When
        viewModel.updateAlertThreshold(budgetId, newThreshold)
        advanceUntilIdle()

        // Then
        coVerify { 
            updateBudgetUseCase(match { 
                it.id == budgetId && it.alertThreshold == newThreshold 
            }) 
        }
    }

    @Test
    fun `budget creation validation should prevent invalid budgets`() = runTest {
        // Given
        val invalidAmount = Money.fromDouble(0.0, "SAR")

        // When
        viewModel.createBudget(
            name = "",
            category = "",
            amount = invalidAmount,
            period = BudgetPeriod.MONTHLY
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertNotNull(finalState.error)
        assertFalse(finalState.isCreating)
        
        // Verify repository was not called
        coVerify(exactly = 0) { createBudgetUseCase(any()) }
    }

    @Test
    fun `error handling should update error state correctly`() = runTest {
        // Given
        val errorMessage = "Budget creation failed"
        coEvery { createBudgetUseCase(any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.createBudget(
            name = "Valid Budget",
            category = "Food & Dining",
            amount = Money.fromDouble(1000.0, "SAR"),
            period = BudgetPeriod.MONTHLY
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(errorMessage, finalState.error)
        assertFalse(finalState.isCreating)
    }

    @Test
    fun `refreshBudgetStatuses should recalculate all budget statuses`() = runTest {
        // Given
        advanceUntilIdle() // Load initial budgets
        
        val budgetStatus = BudgetStatus(
            budgetId = "budget1",
            spent = Money.fromDouble(600.0, "SAR"),
            remaining = Money.fromDouble(400.0, "SAR"),
            percentageUsed = 60.0,
            isOverBudget = false,
            daysRemaining = 20
        )
        
        coEvery { calculateBudgetStatusUseCase(any()) } returns Result.success(budgetStatus)

        // When
        viewModel.refreshBudgetStatuses()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.budgetStatuses.isNotEmpty())
        coVerify { calculateBudgetStatusUseCase(any()) }
    }

    @Test
    fun `budget templates should be available for quick setup`() {
        // When
        val templates = viewModel.getBudgetTemplates()

        // Then
        assertTrue(templates.isNotEmpty())
        assertTrue(templates.any { it.name == "Essential Expenses" })
        assertTrue(templates.any { it.name == "Entertainment & Dining" })
        assertTrue(templates.any { it.name == "Savings & Investment" })
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
                isActive = false,
                alertThreshold = 85.0,
                metadata = emptyMap()
            )
        )
    }
}