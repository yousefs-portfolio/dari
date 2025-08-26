package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.test.util.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulateExpenseRemovalUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var useCase: SimulateExpenseRemovalUseCase

    @BeforeTest
    fun setup() {
        budgetRepository = mockk()
        useCase = SimulateExpenseRemovalUseCase(budgetRepository)
    }

    @Test
    fun `simulate removing recurring monthly expense`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(1000.0, "SAR"),
                "subscriptions" to Money(300.0, "SAR"),
                "transport" to Money(400.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(650.0, "SAR"),
                "subscriptions" to Money(250.0, "SAR"),
                "transport" to Money(200.0, "SAR")
            )
        )

        val expenseRemoval = ExpenseRemoval(
            categoryId = "subscriptions",
            monthlyAmount = Money(99.0, "SAR"),
            description = "Netflix subscription",
            frequency = ExpenseRemoval.Frequency.MONTHLY,
            removalDate = LocalDateTime(2024, 3, 15, 0, 0),
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", expenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        assertEquals("budget1", simulation.budgetId)
        assertEquals(expenseRemoval, simulation.expenseRemoval)
        
        // Original budget: 1000 + 300 + 400 = 1700 SAR
        assertEquals(Money(1700.0, "SAR"), simulation.originalTotalBudget)
        
        // New total should exclude the removed expense
        assertEquals(Money(1601.0, "SAR"), simulation.newTotalBudget)
        
        // Budget decrease
        assertEquals(Money(99.0, "SAR"), simulation.budgetDecrease)
        assertEquals(5.82, simulation.budgetDecreasePercentage, 0.01)
        
        // Original spending: 650 + 250 + 200 = 1100 SAR
        assertEquals(Money(1100.0, "SAR"), simulation.originalSpending)
        
        // New spending should exclude prorated amount
        // 17 days remaining in March (15-31) = ~54.8% of month saved
        assertTrue(simulation.newSpending.amount < 1100.0)
        assertTrue(simulation.newSpending.amount > 1000.0) // Should be prorated
        
        // Should identify savings opportunity
        assertTrue(simulation.savingsRealized.amount > 0)
        assertEquals(
            "Removing this expense will save 99.00 SAR per month",
            simulation.recommendations.first()
        )

        coVerify { budgetRepository.getBudget("budget1") }
    }

    @Test
    fun `simulate removing one-time expense that already occurred`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("misc" to Money(500.0, "SAR")),
            spent = mapOf("misc" to Money(400.0, "SAR"))
        )

        val oneTimeExpenseRemoval = ExpenseRemoval(
            categoryId = "misc",
            monthlyAmount = Money(300.0, "SAR"),
            description = "Car repair",
            frequency = ExpenseRemoval.Frequency.ONE_TIME,
            removalDate = LocalDateTime(2024, 3, 5, 0, 0),
            wasRecurring = false
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", oneTimeExpenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // One-time expense removal doesn't change monthly budget allocation for future
        assertEquals(Money(500.0, "SAR"), simulation.originalTotalBudget)
        assertEquals(Money(500.0, "SAR"), simulation.newTotalBudget)
        assertEquals(Money(0.0, "SAR"), simulation.budgetDecrease)
        
        // But it affects current spending
        assertEquals(Money(400.0, "SAR"), simulation.originalSpending)
        assertEquals(Money(100.0, "SAR"), simulation.newSpending)
        assertEquals(Money(300.0, "SAR"), simulation.savingsRealized)
        
        // Should explain the one-time nature
        assertTrue(simulation.recommendations.any { it.contains("one-time") })
    }

    @Test
    fun `simulate removing weekly expense`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("food" to Money(800.0, "SAR")),
            spent = mapOf("food" to Money(600.0, "SAR"))
        )

        val weeklyExpenseRemoval = ExpenseRemoval(
            categoryId = "food",
            monthlyAmount = Money(50.0, "SAR"), // Per week
            description = "Weekly restaurant visit",
            frequency = ExpenseRemoval.Frequency.WEEKLY,
            removalDate = LocalDateTime(2024, 3, 10, 0, 0),
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", weeklyExpenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // March has roughly 4.4 weeks, so 50 * 4.4 = 220 SAR monthly
        val expectedMonthlyDecrease = 220.0 // 50 * 4.4 weeks
        assertEquals(expectedMonthlyDecrease, simulation.budgetDecrease.amount, 10.0) // Allow small variance
        
        // New budget should be reduced by weekly expense
        assertTrue(simulation.newTotalBudget.amount < 800.0)
        
        // Should mention weekly savings
        assertTrue(simulation.recommendations.any { 
            it.contains("weekly") || it.contains("220")
        })
    }

    @Test
    fun `simulate removing partial month expense`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("entertainment" to Money(400.0, "SAR")),
            spent = mapOf("entertainment" to Money(300.0, "SAR"))
        )

        val expenseRemoval = ExpenseRemoval(
            categoryId = "entertainment",
            monthlyAmount = Money(100.0, "SAR"),
            description = "Gym membership",
            frequency = ExpenseRemoval.Frequency.MONTHLY,
            removalDate = LocalDateTime(2024, 3, 20, 0, 0), // Remove mid-month
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", expenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        assertEquals(Money(400.0, "SAR"), simulation.originalTotalBudget)
        assertEquals(Money(300.0, "SAR"), simulation.newTotalBudget) // Full monthly reduction
        
        // Current month savings should be prorated (11 days remaining)
        assertTrue(simulation.savingsRealized.amount < 100.0) // Prorated for partial month
        assertTrue(simulation.savingsRealized.amount > 0.0)
        
        // New spending should reflect prorated savings
        assertTrue(simulation.newSpending.amount < 300.0)
        assertTrue(simulation.newSpending.amount > 200.0)
    }

    @Test
    fun `simulate removing entire category budget`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "subscriptions" to Money(150.0, "SAR"),
                "food" to Money(800.0, "SAR")
            ),
            spent = mapOf(
                "subscriptions" to Money(150.0, "SAR"),
                "food" to Money(600.0, "SAR")
            )
        )

        val expenseRemoval = ExpenseRemoval(
            categoryId = "subscriptions",
            monthlyAmount = Money(150.0, "SAR"), // Entire category
            description = "All subscriptions",
            frequency = ExpenseRemoval.Frequency.MONTHLY,
            removalDate = LocalDateTime(2024, 3, 1, 0, 0),
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", expenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Category should be effectively eliminated
        assertTrue(simulation.categoryImpact.willEliminateCategory)
        assertEquals(0.0, simulation.categoryImpact.utilizationAfter)
        
        // Should suggest reallocating the entire category
        assertTrue(simulation.recommendations.any { 
            it.contains("eliminate") || it.contains("reallocate")
        })
    }

    @Test
    fun `simulate removing expense with future impact`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("education" to Money(300.0, "SAR")),
            spent = mapOf("education" to Money(150.0, "SAR"))
        )

        val futureExpenseRemoval = ExpenseRemoval(
            categoryId = "education",
            monthlyAmount = Money(100.0, "SAR"),
            description = "Online course subscription",
            frequency = ExpenseRemoval.Frequency.MONTHLY,
            removalDate = LocalDateTime(2024, 4, 1, 0, 0), // Future removal
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", futureExpenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Current month should not be affected
        assertEquals(simulation.originalSpending, simulation.newSpending)
        assertEquals(Money(0.0, "SAR"), simulation.savingsRealized)
        
        // But future budget should be reduced
        assertEquals(Money(200.0, "SAR"), simulation.newTotalBudget)
        
        // Should mention future savings
        assertTrue(simulation.recommendations.any { it.contains("future") || it.contains("starting") })
    }

    @Test
    fun `simulate handles repository error`() = runTest {
        // Given
        val error = Exception("Budget not found")
        val expenseRemoval = ExpenseRemoval(
            categoryId = "test",
            monthlyAmount = Money(100.0, "SAR"),
            description = "Test",
            frequency = ExpenseRemoval.Frequency.MONTHLY,
            removalDate = LocalDateTime(2024, 3, 1, 0, 0),
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } throws error

        // When
        val result = useCase("budget1", expenseRemoval)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `simulate removing expense creates budget surplus`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("transport" to Money(500.0, "SAR")),
            spent = mapOf("transport" to Money(400.0, "SAR"))
        )

        val expenseRemoval = ExpenseRemoval(
            categoryId = "transport",
            monthlyAmount = Money(200.0, "SAR"),
            description = "Car payment",
            frequency = ExpenseRemoval.Frequency.MONTHLY,
            removalDate = LocalDateTime(2024, 3, 1, 0, 0),
            wasRecurring = true
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", expenseRemoval)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Should create significant surplus
        assertTrue(simulation.categoryImpact.utilizationAfter < simulation.categoryImpact.utilizationBefore)
        
        // Should suggest reallocating surplus
        assertTrue(simulation.recommendations.any { 
            it.contains("surplus") || it.contains("reallocate") || it.contains("save")
        })
    }
}