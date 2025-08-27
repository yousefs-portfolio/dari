package code.yousef.dari.presentation.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.GoalRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.GetTransactionsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getAccountsUseCase = mockk<GetAccountsUseCase>()
    private val getTransactionsUseCase = mockk<GetTransactionsUseCase>()
    private val budgetRepository = mockk<BudgetRepository>()
    private val goalRepository = mockk<GoalRepository>()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock responses
        coEvery { getAccountsUseCase() } returns flowOf(emptyList())
        coEvery { getTransactionsUseCase(any(), any()) } returns flowOf(emptyList())
        coEvery { budgetRepository.getActiveBudgets() } returns Result.success(emptyList())
        coEvery { goalRepository.getActiveGoals() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should calculate spending velocity indicator correctly`() = runTest {
        // Given
        val now = Clock.System.now()
        val recentTransactions = listOf(
            createMockTransaction(-50.0, now.minus(1, DateTimeUnit.DAY)),
            createMockTransaction(-30.0, now.minus(2, DateTimeUnit.DAY)),
            createMockTransaction(-20.0, now.minus(3, DateTimeUnit.DAY)),
            createMockTransaction(-100.0, now.minus(7, DateTimeUnit.DAY)),
            createMockTransaction(500.0, now.minus(8, DateTimeUnit.DAY)) // Income
        )

        coEvery { getTransactionsUseCase(any(), any()) } returns flowOf(recentTransactions)

        // When
        viewModel = DashboardViewModel(
            getAccountsUseCase = getAccountsUseCase,
            getTransactionsUseCase = getTransactionsUseCase,
            budgetRepository = budgetRepository,
            goalRepository = goalRepository
        )

        advanceUntilIdle()

        // Then
        val spendingVelocity = viewModel.uiState.value.spendingVelocity
        assertNotNull(spendingVelocity)
        
        // Should show spending trend (last 7 days vs previous 7 days)
        // Last 7 days: -50 - 30 - 20 = -100 SAR
        // Previous 7 days: -100 SAR
        // Trend should be 0% (same spending)
        assertEquals(0.0, spendingVelocity!!.weeklyTrendPercentage, 0.1)
        assertEquals(-100.0, spendingVelocity.currentWeekSpending.toDouble(), 0.1)
        assertEquals(-100.0, spendingVelocity.previousWeekSpending.toDouble(), 0.1)
    }

    @Test
    fun `should handle increasing spending velocity`() = runTest {
        // Given
        val now = Clock.System.now()
        val recentTransactions = listOf(
            createMockTransaction(-100.0, now.minus(1, DateTimeUnit.DAY)), // Current week
            createMockTransaction(-100.0, now.minus(2, DateTimeUnit.DAY)),
            createMockTransaction(-50.0, now.minus(10, DateTimeUnit.DAY)), // Previous week
        )

        coEvery { getTransactionsUseCase(any(), any()) } returns flowOf(recentTransactions)

        // When
        viewModel = DashboardViewModel(
            getAccountsUseCase = getAccountsUseCase,
            getTransactionsUseCase = getTransactionsUseCase,
            budgetRepository = budgetRepository,
            goalRepository = goalRepository
        )

        advanceUntilIdle()

        // Then
        val spendingVelocity = viewModel.uiState.value.spendingVelocity
        assertNotNull(spendingVelocity)
        
        // Current week: -200, Previous week: -50
        // Trend: ((200 - 50) / 50) * 100 = 300% increase
        assertEquals(300.0, spendingVelocity!!.weeklyTrendPercentage, 0.1)
        assertEquals(SpendingTrend.INCREASING, spendingVelocity.trend)
    }

    @Test
    fun `should handle decreasing spending velocity`() = runTest {
        // Given
        val now = Clock.System.now()
        val recentTransactions = listOf(
            createMockTransaction(-50.0, now.minus(1, DateTimeUnit.DAY)), // Current week
            createMockTransaction(-100.0, now.minus(10, DateTimeUnit.DAY)), // Previous week
            createMockTransaction(-100.0, now.minus(11, DateTimeUnit.DAY)),
        )

        coEvery { getTransactionsUseCase(any(), any()) } returns flowOf(recentTransactions)

        // When
        viewModel = DashboardViewModel(
            getAccountsUseCase = getAccountsUseCase,
            getTransactionsUseCase = getTransactionsUseCase,
            budgetRepository = budgetRepository,
            goalRepository = goalRepository
        )

        advanceUntilIdle()

        // Then
        val spendingVelocity = viewModel.uiState.value.spendingVelocity
        assertNotNull(spendingVelocity)
        
        // Current week: -50, Previous week: -200
        // Trend: ((50 - 200) / 200) * 100 = -75% decrease
        assertEquals(-75.0, spendingVelocity!!.weeklyTrendPercentage, 0.1)
        assertEquals(SpendingTrend.DECREASING, spendingVelocity.trend)
    }

    @Test
    fun `should predict monthly spending based on current velocity`() = runTest {
        // Given
        val now = Clock.System.now()
        val recentTransactions = listOf(
            createMockTransaction(-100.0, now.minus(1, DateTimeUnit.DAY)),
            createMockTransaction(-100.0, now.minus(2, DateTimeUnit.DAY)),
            createMockTransaction(-100.0, now.minus(3, DateTimeUnit.DAY))
        )

        coEvery { getTransactionsUseCase(any(), any()) } returns flowOf(recentTransactions)

        // When
        viewModel = DashboardViewModel(
            getAccountsUseCase = getAccountsUseCase,
            getTransactionsUseCase = getTransactionsUseCase,
            budgetRepository = budgetRepository,
            goalRepository = goalRepository
        )

        advanceUntilIdle()

        // Then
        val spendingVelocity = viewModel.uiState.value.spendingVelocity
        assertNotNull(spendingVelocity)
        
        // Daily average: 300 / 3 = 100 SAR per day
        // Monthly prediction: 100 * 30 = 3000 SAR
        assertEquals(3000.0, spendingVelocity!!.projectedMonthlySpending.toDouble(), 0.1)
    }

    private fun createMockTransaction(amount: Double, timestamp: kotlinx.datetime.Instant) = Transaction(
        id = "test-${timestamp}",
        accountId = "test-account",
        amount = Money.fromDouble(amount, "SAR"),
        description = "Test transaction",
        timestamp = timestamp,
        category = null,
        merchant = null,
        type = if (amount < 0) TransactionType.EXPENSE else TransactionType.INCOME,
        tags = emptyList(),
        receiptId = null
    )
}