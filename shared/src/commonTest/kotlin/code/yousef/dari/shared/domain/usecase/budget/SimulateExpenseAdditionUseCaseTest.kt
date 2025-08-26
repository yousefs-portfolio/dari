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
import kotlin.test.assertTrue

class SimulateExpenseAdditionUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var useCase: SimulateExpenseAdditionUseCase

    @BeforeTest
    fun setup() {
        budgetRepository = mockk()
        useCase = SimulateExpenseAdditionUseCase(budgetRepository)
    }

    @Test
    fun `simulate adding recurring expense to budget`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(1000.0, "SAR"),
                "entertainment" to Money(300.0, "SAR"),
                "transport" to Money(400.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(650.0, "SAR"),
                "entertainment" to Money(150.0, "SAR"),
                "transport" to Money(200.0, "SAR")
            )
        )

        val newExpense = ExpenseAddition(
            categoryId = "subscriptions",
            monthlyAmount = Money(99.0, "SAR"),
            description = "Netflix subscription",
            frequency = ExpenseAddition.Frequency.MONTHLY,
            startDate = LocalDateTime(2024, 3, 15, 0, 0),
            endDate = null // Ongoing
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", newExpense)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        assertEquals("budget1", simulation.budgetId)
        assertEquals(newExpense, simulation.expenseAddition)
        
        // Original budget: 1000 + 300 + 400 = 1700 SAR
        assertEquals(Money(1700.0, "SAR"), simulation.originalTotalBudget)
        
        // New total should include the new expense
        assertEquals(Money(1799.0, "SAR"), simulation.newTotalBudget)
        
        // Budget impact
        assertEquals(Money(99.0, "SAR"), simulation.budgetIncrease)
        assertEquals(5.82, simulation.budgetIncreasePercentage, 0.01)
        
        // Original spending: 650 + 150 + 200 = 1000 SAR
        assertEquals(Money(1000.0, "SAR"), simulation.originalSpending)
        
        // New spending should include prorated amount (started mid-month)
        // 17 days remaining in March (15-31) = ~54.8% of month
        assertTrue(simulation.newSpending.amount > 1000.0)
        assertTrue(simulation.newSpending.amount < 1099.0) // Should be prorated
        
        // Should identify budget adjustment needed
        assertTrue(simulation.budgetAdjustmentNeeded)
        assertEquals(
            "Adding this expense requires increasing the 'subscriptions' budget by 99.00 SAR per month",
            simulation.recommendations.first()
        )

        coVerify { budgetRepository.getBudget("budget1") }
    }

    @Test
    fun `simulate adding one-time expense`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("misc" to Money(200.0, "SAR")),
            spent = mapOf("misc" to Money(100.0, "SAR"))
        )

        val oneTimeExpense = ExpenseAddition(
            categoryId = "misc",
            monthlyAmount = Money(300.0, "SAR"),
            description = "Car repair",
            frequency = ExpenseAddition.Frequency.ONE_TIME,
            startDate = LocalDateTime(2024, 3, 20, 0, 0),
            endDate = null
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", oneTimeExpense)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // One-time expense doesn't change monthly budget allocation
        assertEquals(Money(200.0, "SAR"), simulation.originalTotalBudget)
        assertEquals(Money(200.0, "SAR"), simulation.newTotalBudget)
        assertEquals(Money(0.0, "SAR"), simulation.budgetIncrease)
        
        // But it affects spending
        assertEquals(Money(100.0, "SAR"), simulation.originalSpending)
        assertEquals(Money(400.0, "SAR"), simulation.newSpending)
        
        // Should identify overspend
        assertTrue(simulation.budgetAdjustmentNeeded)
        assertTrue(simulation.recommendations.any { it.contains("overspend") })
    }

    @Test
    fun `simulate adding weekly expense`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("food" to Money(800.0, "SAR")),
            spent = mapOf("food" to Money(300.0, "SAR"))
        )

        val weeklyExpense = ExpenseAddition(
            categoryId = "food",
            monthlyAmount = Money(50.0, "SAR"), // Per week
            description = "Weekly restaurant visit",
            frequency = ExpenseAddition.Frequency.WEEKLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = null
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", weeklyExpense)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // March has roughly 4.4 weeks, so 50 * 4.4 = 220 SAR monthly
        val expectedMonthlyIncrease = 220.0 // 50 * 4.4 weeks
        assertEquals(expectedMonthlyIncrease, simulation.budgetIncrease.amount, 10.0) // Allow small variance
        
        // New budget should accommodate weekly expense
        assertTrue(simulation.newTotalBudget.amount > 800.0)
        
        // Should suggest budget adjustment
        assertTrue(simulation.recommendations.any { 
            it.contains("weekly") || it.contains("220") 
        })
    }

    @Test
    fun `simulate adding expense to existing category within budget`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("entertainment" to Money(500.0, "SAR")),
            spent = mapOf("entertainment" to Money(200.0, "SAR"))
        )

        val newExpense = ExpenseAddition(
            categoryId = "entertainment",
            monthlyAmount = Money(100.0, "SAR"),
            description = "Gym membership",
            frequency = ExpenseAddition.Frequency.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = null
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", newExpense)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        assertEquals(Money(500.0, "SAR"), simulation.originalTotalBudget)
        assertEquals(Money(600.0, "SAR"), simulation.newTotalBudget)
        
        // New spending: 200 (existing) + 100 (new) = 300 SAR
        assertEquals(Money(300.0, "SAR"), simulation.newSpending)
        
        // Still within budget (300 < 600), so no adjustment needed
        assertTrue(simulation.budgetAdjustmentNeeded) // But budget still increased
        
        // Should suggest that expense fits within increased budget
        assertTrue(simulation.recommendations.any { 
            it.contains("increase") && it.contains("entertainment")
        })
    }

    @Test
    fun `simulate adding expense with end date`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("education" to Money(200.0, "SAR")),
            spent = mapOf("education" to Money(50.0, "SAR"))
        )

        val temporaryExpense = ExpenseAddition(
            categoryId = "education",
            monthlyAmount = Money(150.0, "SAR"),
            description = "Online course",
            frequency = ExpenseAddition.Frequency.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 5, 31, 23, 59) // 3 months
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", temporaryExpense)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        assertEquals(Money(350.0, "SAR"), simulation.newTotalBudget)
        
        // Should mention temporary nature
        assertTrue(simulation.recommendations.any { 
            it.contains("temporary") || it.contains("3 months")
        })
    }

    @Test
    fun `simulate handles repository error`() = runTest {
        // Given
        val error = Exception("Budget not found")
        val expense = ExpenseAddition(
            categoryId = "test",
            monthlyAmount = Money(100.0, "SAR"),
            description = "Test",
            frequency = ExpenseAddition.Frequency.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = null
        )

        coEvery { budgetRepository.getBudget("budget1") } throws error

        // When
        val result = useCase("budget1", expense)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}