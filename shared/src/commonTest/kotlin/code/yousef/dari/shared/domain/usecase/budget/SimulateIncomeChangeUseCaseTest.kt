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

class SimulateIncomeChangeUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var useCase: SimulateIncomeChangeUseCase

    @BeforeTest
    fun setup() {
        budgetRepository = mockk()
        useCase = SimulateIncomeChangeUseCase(budgetRepository)
    }

    @Test
    fun `simulate income increase with proportional budget scaling`() = runTest {
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
                "transport" to Money(400.0, "SAR"),
                "savings" to Money(500.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(750.0, "SAR"),
                "entertainment" to Money(200.0, "SAR"),
                "transport" to Money(350.0, "SAR"),
                "savings" to Money(500.0, "SAR")
            ),
            totalIncome = Money(3000.0, "SAR")
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(4000.0, "SAR"), // 33% increase
            changeReason = "Promotion",
            effectiveDate = LocalDateTime(2024, 3, 15, 0, 0),
            isTemporary = false,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.PROPORTIONAL
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        assertEquals("budget1", simulation.budgetId)
        assertEquals(incomeChange, simulation.incomeChange)
        
        // Income analysis
        assertEquals(Money(3000.0, "SAR"), simulation.originalIncome)
        assertEquals(Money(4000.0, "SAR"), simulation.newIncome)
        assertEquals(Money(1000.0, "SAR"), simulation.incomeIncrease)
        assertEquals(33.33, simulation.incomeIncreasePercentage, 0.01)
        
        // Original budget total: 1000 + 300 + 400 + 500 = 2200 SAR
        assertEquals(Money(2200.0, "SAR"), simulation.originalTotalBudget)
        
        // With proportional scaling, budget should increase by ~33%
        // 2200 * 1.33 = ~2926 SAR
        assertTrue(simulation.newTotalBudget.amount > 2800.0)
        assertTrue(simulation.newTotalBudget.amount < 3000.0)
        
        // Budget utilization should improve
        assertTrue(simulation.budgetUtilizationAfter < simulation.budgetUtilizationBefore)
        
        // Should suggest optimal allocation
        assertTrue(simulation.recommendations.any { it.contains("proportionally") })

        coVerify { budgetRepository.getBudget("budget1") }
    }

    @Test
    fun `simulate income decrease with conservative budget reduction`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(1200.0, "SAR"),
                "entertainment" to Money(400.0, "SAR"),
                "transport" to Money(500.0, "SAR"),
                "savings" to Money(600.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(1100.0, "SAR"),
                "entertainment" to Money(350.0, "SAR"),
                "transport" to Money(450.0, "SAR"),
                "savings" to Money(600.0, "SAR")
            ),
            totalIncome = Money(4000.0, "SAR")
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(3000.0, "SAR"), // 25% decrease
            changeReason = "Job loss",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = true,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.CONSERVATIVE_REDUCTION
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Income analysis
        assertEquals(Money(4000.0, "SAR"), simulation.originalIncome)
        assertEquals(Money(3000.0, "SAR"), simulation.newIncome)
        assertEquals(Money(-1000.0, "SAR"), simulation.incomeIncrease) // Actually decrease
        assertEquals(-25.0, simulation.incomeIncreasePercentage)
        
        // Original budget total: 1200 + 400 + 500 + 600 = 2700 SAR
        assertEquals(Money(2700.0, "SAR"), simulation.originalTotalBudget)
        
        // Conservative reduction should prioritize essential categories
        assertTrue(simulation.newTotalBudget.amount <= 3000.0) // Should not exceed new income
        assertTrue(simulation.newTotalBudget.amount < simulation.originalTotalBudget.amount)
        
        // Should identify categories for reduction
        assertTrue(simulation.categoryAdjustments.any { it.adjustmentAmount.amount < 0 })
        
        // Budget utilization might increase due to tighter budget
        assertTrue(simulation.recommendations.any { it.contains("reduce") || it.contains("essential") })
    }

    @Test
    fun `simulate fixed savings allocation strategy`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(800.0, "SAR"),
                "entertainment" to Money(200.0, "SAR"),
                "savings" to Money(300.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(600.0, "SAR"),
                "entertainment" to Money(150.0, "SAR"),
                "savings" to Money(300.0, "SAR")
            ),
            totalIncome = Money(2000.0, "SAR")
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(2500.0, "SAR"), // 25% increase
            changeReason = "Bonus",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = false,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.FIXED_SAVINGS_ALLOCATION,
            targetSavingsRate = 30.0 // 30% to savings
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Should allocate 30% of new income (2500) to savings = 750 SAR
        val savingsAdjustment = simulation.categoryAdjustments.find { it.categoryId == "savings" }
        assertTrue(savingsAdjustment != null)
        assertTrue(savingsAdjustment!!.newBudgetAmount.amount >= 700.0) // Around 750 SAR
        
        // Remaining increase should be distributed among other categories
        val nonSavingsIncrease = simulation.categoryAdjustments
            .filter { it.categoryId != "savings" }
            .sumOf { it.adjustmentAmount.amount }
        
        assertTrue(nonSavingsIncrease > 0) // Should get some increase too
        
        assertTrue(simulation.recommendations.any { it.contains("savings") && it.contains("30%") })
    }

    @Test
    fun `simulate custom allocation strategy`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(600.0, "SAR"),
                "transport" to Money(300.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(500.0, "SAR"),
                "transport" to Money(250.0, "SAR")
            ),
            totalIncome = Money(1500.0, "SAR")
        )

        val customAllocations = mapOf(
            "groceries" to Money(200.0, "SAR"), // Specific increase
            "entertainment" to Money(300.0, "SAR") // New category
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(2000.0, "SAR"), // 33% increase
            changeReason = "Side business",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = false,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.CUSTOM_ALLOCATION,
            customCategoryAllocations = customAllocations
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Should use custom allocations
        val groceriesAdjustment = simulation.categoryAdjustments.find { it.categoryId == "groceries" }
        assertEquals(Money(200.0, "SAR"), groceriesAdjustment?.adjustmentAmount)
        assertEquals(Money(800.0, "SAR"), groceriesAdjustment?.newBudgetAmount) // 600 + 200
        
        val entertainmentAdjustment = simulation.categoryAdjustments.find { it.categoryId == "entertainment" }
        assertEquals(Money(300.0, "SAR"), entertainmentAdjustment?.adjustmentAmount)
        assertEquals(Money(300.0, "SAR"), entertainmentAdjustment?.newBudgetAmount) // New category
        assertTrue(entertainmentAdjustment?.isNewCategory == true)
        
        assertTrue(simulation.recommendations.any { it.contains("custom") || it.contains("specific") })
    }

    @Test
    fun `simulate temporary income change`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("groceries" to Money(1000.0, "SAR")),
            spent = mapOf("groceries" to Money(800.0, "SAR")),
            totalIncome = Money(2000.0, "SAR")
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(1500.0, "SAR"), // Temporary reduction
            changeReason = "Temporary layoff",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = true,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.MINIMAL_CHANGE
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Should acknowledge temporary nature
        assertTrue(simulation.recommendations.any { it.contains("temporary") })
        
        // Minimal change strategy should make small adjustments
        val totalAdjustment = simulation.categoryAdjustments.sumOf { kotlin.math.abs(it.adjustmentAmount.amount) }
        assertTrue(totalAdjustment < 300.0) // Should be conservative changes
    }

    @Test
    fun `simulate income change creating budget surplus`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("basics" to Money(1500.0, "SAR")),
            spent = mapOf("basics" to Money(1200.0, "SAR")),
            totalIncome = Money(2000.0, "SAR")
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(3500.0, "SAR"), // Large increase
            changeReason = "New job",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = false,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.PROPORTIONAL
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Should identify significant surplus
        assertEquals(Money(1500.0, "SAR"), simulation.incomeIncrease)
        assertTrue(simulation.surplusAmount.amount > 1000.0)
        
        // Should suggest new budget categories
        assertTrue(simulation.recommendations.any { 
            it.contains("surplus") || it.contains("new categories") || it.contains("emergency fund")
        })
    }

    @Test
    fun `simulate handles repository error`() = runTest {
        // Given
        val error = Exception("Budget not found")
        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(2000.0, "SAR"),
            changeReason = "Test",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = false,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.PROPORTIONAL
        )

        coEvery { budgetRepository.getBudget("budget1") } throws error

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `simulate income change with existing overspending`() = runTest {
        // Given - Budget with overspending
        val budget = Budget(
            id = "budget1",
            name = "March 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(800.0, "SAR"),
                "entertainment" to Money(200.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(900.0, "SAR"), // Overspent
                "entertainment" to Money(250.0, "SAR") // Overspent
            ),
            totalIncome = Money(1500.0, "SAR")
        )

        val incomeChange = IncomeChange(
            newMonthlyIncome = Money(2000.0, "SAR"),
            changeReason = "Raise",
            effectiveDate = LocalDateTime(2024, 3, 1, 0, 0),
            isTemporary = false,
            budgetScalingStrategy = IncomeChange.BudgetScalingStrategy.PROPORTIONAL
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget

        // When
        val result = useCase("budget1", incomeChange)

        // Then
        assertTrue(result.isSuccess)
        val simulation = result.getOrThrow()
        
        // Should acknowledge existing overspending
        assertTrue(simulation.recommendations.any { it.contains("overspending") || it.contains("overspent") })
        
        // Should suggest addressing the deficit first
        assertTrue(simulation.categoryAdjustments.any { 
            it.categoryId == "groceries" && it.adjustmentAmount.amount > 100.0 
        })
    }
}