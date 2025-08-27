package code.yousef.dari.presentation.goals

import androidx.lifecycle.SavedStateHandle
import code.yousef.dari.shared.data.repository.GoalRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.goal.*
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
class GoalsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val createGoalUseCase: CreateGoalUseCase = mockk(relaxed = true)
    private val updateGoalProgressUseCase: UpdateGoalProgressUseCase = mockk(relaxed = true)
    private val calculateGoalProjectionUseCase: CalculateGoalProjectionUseCase = mockk(relaxed = true)
    private val adjustGoalParametersUseCase: AdjustGoalParametersUseCase = mockk(relaxed = true)
    private val pauseGoalUseCase: PauseGoalUseCase = mockk(relaxed = true)
    private val resumeGoalUseCase: ResumeGoalUseCase = mockk(relaxed = true)
    private val completeGoalUseCase: CompleteGoalUseCase = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)

    private lateinit var viewModel: GoalsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        every { savedStateHandle.get<String>("accountId") } returns "test-account-id"
        
        // Create sample goals
        val sampleGoals = createSampleGoals()
        every { goalRepository.getGoals() } returns flowOf(sampleGoals)
        
        viewModel = GoalsViewModel(
            goalRepository = goalRepository,
            transactionRepository = transactionRepository,
            createGoalUseCase = createGoalUseCase,
            updateGoalProgressUseCase = updateGoalProgressUseCase,
            calculateGoalProjectionUseCase = calculateGoalProjectionUseCase,
            adjustGoalParametersUseCase = adjustGoalParametersUseCase,
            pauseGoalUseCase = pauseGoalUseCase,
            resumeGoalUseCase = resumeGoalUseCase,
            completeGoalUseCase = completeGoalUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading with empty goals`() {
        // Given - ViewModel is created in setup()
        
        // Then
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertEquals(emptyList(), initialState.goals)
        assertEquals(emptyList(), initialState.goalProjections)
        assertEquals(GoalFilter.ALL, initialState.selectedFilter)
    }

    @Test
    fun `loadGoals should update state with goals and projections`() = runTest {
        // Given
        val sampleGoals = createSampleGoals()
        val goalProjections = sampleGoals.map { goal ->
            GoalProjection(
                goalId = goal.id,
                projectedCompletionDate = goal.targetDate,
                monthlyContributionNeeded = Money.fromDouble(100.0, "SAR"),
                totalContributionsNeeded = Money.fromDouble(1000.0, "SAR"),
                isOnTrack = true,
                daysToCompletion = 30
            )
        }
        
        every { goalRepository.getGoals() } returns flowOf(sampleGoals)
        goalProjections.forEach { projection ->
            coEvery { calculateGoalProjectionUseCase(any()) } returns Result.success(projection)
        }

        // When
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(sampleGoals, finalState.goals)
        assertTrue(finalState.goalProjections.isNotEmpty())
    }

    @Test
    fun `createGoal should add new goal successfully`() = runTest {
        // Given
        val newGoal = Goal(
            id = "new-goal",
            accountId = "test-account-id",
            name = "Emergency Fund",
            description = "Build 6-month emergency fund",
            type = GoalType.SAVING,
            targetAmount = Money.fromDouble(10000.0, "SAR"),
            currentAmount = Money.fromDouble(0.0, "SAR"),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(kotlinx.datetime.DatePeriod(months = 12)),
            monthlyContribution = Money.fromDouble(800.0, "SAR"),
            isActive = true,
            priority = GoalPriority.HIGH,
            category = "Emergency",
            metadata = emptyMap()
        )
        
        coEvery { createGoalUseCase(any()) } returns Result.success(newGoal)

        // When
        viewModel.createGoal(
            name = "Emergency Fund",
            description = "Build 6-month emergency fund",
            type = GoalType.SAVING,
            targetAmount = Money.fromDouble(10000.0, "SAR"),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(kotlinx.datetime.DatePeriod(months = 12)),
            monthlyContribution = Money.fromDouble(800.0, "SAR"),
            priority = GoalPriority.HIGH,
            category = "Emergency"
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCreating)
        coVerify {
            createGoalUseCase(
                match { goal ->
                    goal.name == "Emergency Fund" &&
                    goal.type == GoalType.SAVING &&
                    goal.targetAmount.amount == 10000.0 &&
                    goal.priority == GoalPriority.HIGH
                }
            )
        }
    }

    @Test
    fun `updateGoalProgress should modify goal current amount`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        
        val goalId = "goal1"
        val contributionAmount = Money.fromDouble(500.0, "SAR")
        
        val updatedGoal = createSampleGoals()[0].copy(
            currentAmount = Money.fromDouble(1500.0, "SAR") // 1000 + 500
        )
        
        coEvery { updateGoalProgressUseCase(goalId, contributionAmount) } returns Result.success(updatedGoal)

        // When
        viewModel.addContribution(goalId, contributionAmount)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isUpdating)
        coVerify { updateGoalProgressUseCase(goalId, contributionAmount) }
    }

    @Test
    fun `pauseGoal should pause active goal`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        
        val goalId = "goal1"
        val pausedGoal = createSampleGoals()[0].copy(isActive = false)
        
        coEvery { pauseGoalUseCase(goalId) } returns Result.success(pausedGoal)

        // When
        viewModel.pauseGoal(goalId)
        advanceUntilIdle()

        // Then
        coVerify { pauseGoalUseCase(goalId) }
    }

    @Test
    fun `resumeGoal should reactivate paused goal`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        
        val goalId = "goal1"
        val resumedGoal = createSampleGoals()[0].copy(isActive = true)
        
        coEvery { resumeGoalUseCase(goalId) } returns Result.success(resumedGoal)

        // When
        viewModel.resumeGoal(goalId)
        advanceUntilIdle()

        // Then
        coVerify { resumeGoalUseCase(goalId) }
    }

    @Test
    fun `completeGoal should mark goal as completed`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        
        val goalId = "goal1"
        val completedGoal = createSampleGoals()[0].copy(
            currentAmount = Money.fromDouble(5000.0, "SAR"), // Equals target
            isActive = false
        )
        
        coEvery { completeGoalUseCase(goalId) } returns Result.success(completedGoal)

        // When
        viewModel.completeGoal(goalId)
        advanceUntilIdle()

        // Then
        coVerify { completeGoalUseCase(goalId) }
    }

    @Test
    fun `filterGoals should update filtered goals list`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        
        val filter = GoalFilter.SAVING

        // When
        viewModel.filterGoals(filter)

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(filter, finalState.selectedFilter)
        assertTrue(finalState.filteredGoals.all { it.type == GoalType.SAVING })
    }

    @Test
    fun `adjustGoalParameters should update goal settings`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        
        val goalId = "goal1"
        val newTargetAmount = Money.fromDouble(8000.0, "SAR")
        val newTargetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(kotlinx.datetime.DatePeriod(months = 18))
        val newMonthlyContribution = Money.fromDouble(450.0, "SAR")
        
        val adjustedGoal = createSampleGoals()[0].copy(
            targetAmount = newTargetAmount,
            targetDate = newTargetDate,
            monthlyContribution = newMonthlyContribution
        )
        
        coEvery { adjustGoalParametersUseCase(any()) } returns Result.success(adjustedGoal)

        // When
        viewModel.adjustGoalParameters(
            goalId = goalId,
            targetAmount = newTargetAmount,
            targetDate = newTargetDate,
            monthlyContribution = newMonthlyContribution
        )
        advanceUntilIdle()

        // Then
        coVerify { 
            adjustGoalParametersUseCase(
                match { goal ->
                    goal.id == goalId &&
                    goal.targetAmount.amount == 8000.0 &&
                    goal.monthlyContribution.amount == 450.0
                }
            )
        }
    }

    @Test
    fun `calculateGoalProjection should provide timeline estimation`() = runTest {
        // Given
        val goal = createSampleGoals()[0]
        val projection = GoalProjection(
            goalId = goal.id,
            projectedCompletionDate = goal.targetDate,
            monthlyContributionNeeded = Money.fromDouble(400.0, "SAR"),
            totalContributionsNeeded = Money.fromDouble(4000.0, "SAR"),
            isOnTrack = true,
            daysToCompletion = 120
        )
        
        coEvery { calculateGoalProjectionUseCase(goal) } returns Result.success(projection)

        // When
        viewModel.calculateGoalProjection(goal)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        val goalProjection = finalState.goalProjections.find { it.goalId == goal.id }
        assertNotNull(goalProjection)
        assertEquals(Money.fromDouble(400.0, "SAR"), goalProjection.monthlyContributionNeeded)
        assertTrue(goalProjection.isOnTrack)
    }

    @Test
    fun `getGoalCategories should return available categories`() {
        // When
        val categories = viewModel.getGoalCategories()

        // Then
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.contains("Emergency"))
        assertTrue(categories.contains("Vacation"))
        assertTrue(categories.contains("Investment"))
        assertTrue(categories.contains("Education"))
    }

    @Test
    fun `getGoalTemplates should provide quick setup options`() {
        // When
        val templates = viewModel.getGoalTemplates()

        // Then
        assertTrue(templates.isNotEmpty())
        assertTrue(templates.any { it.name == "Emergency Fund" })
        assertTrue(templates.any { it.name == "Dream Vacation" })
        assertTrue(templates.any { it.name == "House Down Payment" })
    }

    @Test
    fun `error handling should update error state correctly`() = runTest {
        // Given
        val errorMessage = "Goal creation failed"
        coEvery { createGoalUseCase(any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.createGoal(
            name = "Test Goal",
            description = "Test Description",
            type = GoalType.SAVING,
            targetAmount = Money.fromDouble(1000.0, "SAR"),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(kotlinx.datetime.DatePeriod(months = 6)),
            monthlyContribution = Money.fromDouble(100.0, "SAR"),
            priority = GoalPriority.MEDIUM,
            category = "Test"
        )
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(errorMessage, finalState.error)
        assertFalse(finalState.isCreating)
    }

    @Test
    fun `refreshGoals should reload goals and clear projections`() = runTest {
        // Given
        advanceUntilIdle() // Load initial goals
        assertTrue(viewModel.uiState.value.goals.isNotEmpty())

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertTrue(finalState.goals.isNotEmpty())
    }

    // Helper methods
    private fun createSampleGoals(): List<Goal> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            Goal(
                id = "goal1",
                accountId = "test-account-id",
                name = "Emergency Fund",
                description = "6-month emergency fund",
                type = GoalType.SAVING,
                targetAmount = Money.fromDouble(5000.0, "SAR"),
                currentAmount = Money.fromDouble(1000.0, "SAR"),
                targetDate = today.plus(kotlinx.datetime.DatePeriod(months = 12)),
                monthlyContribution = Money.fromDouble(400.0, "SAR"),
                isActive = true,
                priority = GoalPriority.HIGH,
                category = "Emergency",
                metadata = emptyMap()
            ),
            Goal(
                id = "goal2",
                accountId = "test-account-id",
                name = "Vacation Fund",
                description = "Summer vacation to Europe",
                type = GoalType.SAVING,
                targetAmount = Money.fromDouble(8000.0, "SAR"),
                currentAmount = Money.fromDouble(2000.0, "SAR"),
                targetDate = today.plus(kotlinx.datetime.DatePeriod(months = 8)),
                monthlyContribution = Money.fromDouble(750.0, "SAR"),
                isActive = true,
                priority = GoalPriority.MEDIUM,
                category = "Vacation",
                metadata = emptyMap()
            ),
            Goal(
                id = "goal3",
                accountId = "test-account-id",
                name = "Investment Portfolio",
                description = "Build investment portfolio",
                type = GoalType.INVESTMENT,
                targetAmount = Money.fromDouble(20000.0, "SAR"),
                currentAmount = Money.fromDouble(5000.0, "SAR"),
                targetDate = today.plus(kotlinx.datetime.DatePeriod(years = 2)),
                monthlyContribution = Money.fromDouble(600.0, "SAR"),
                isActive = false,
                priority = GoalPriority.LOW,
                category = "Investment",
                metadata = emptyMap()
            )
        )
    }
}