package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Category
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

class CompareBudgetsUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var useCase: CompareBudgetsUseCase

    @BeforeTest
    fun setup() {
        budgetRepository = mockk()
        useCase = CompareBudgetsUseCase(budgetRepository)
    }

    @Test
    fun `compare budgets returns detailed comparison`() = runTest {
        // Given
        val category1 = Category(
            id = "category1",
            name = "Groceries",
            icon = "grocery",
            color = 0xFF4CAF50,
            type = Category.Type.EXPENSE
        )
        
        val category2 = Category(
            id = "category2",
            name = "Entertainment",
            icon = "entertainment",
            color = 0xFFFF9800,
            type = Category.Type.EXPENSE
        )

        val budget1 = Budget(
            id = "budget1",
            name = "January 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            categories = mapOf(
                "category1" to Money(500.0, "SAR"),
                "category2" to Money(200.0, "SAR")
            ),
            spent = mapOf(
                "category1" to Money(450.0, "SAR"),
                "category2" to Money(180.0, "SAR")
            )
        )

        val budget2 = Budget(
            id = "budget2",
            name = "February 2024",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 2, 1, 0, 0),
            endDate = LocalDateTime(2024, 2, 29, 23, 59),
            categories = mapOf(
                "category1" to Money(600.0, "SAR"),
                "category2" to Money(150.0, "SAR")
            ),
            spent = mapOf(
                "category1" to Money(520.0, "SAR"),
                "category2" to Money(120.0, "SAR")
            )
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget1
        coEvery { budgetRepository.getBudget("budget2") } returns budget2

        // When
        val result = useCase("budget1", "budget2")

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrThrow()
        
        assertEquals(budget1, comparison.budget1)
        assertEquals(budget2, comparison.budget2)
        
        // Check total budget comparison
        assertEquals(Money(700.0, "SAR"), comparison.budget1Total)
        assertEquals(Money(750.0, "SAR"), comparison.budget2Total)
        assertEquals(Money(50.0, "SAR"), comparison.budgetDifference)
        assertEquals(7.14, comparison.budgetChangePercentage, 0.01)
        
        // Check total spending comparison
        assertEquals(Money(630.0, "SAR"), comparison.budget1Spending)
        assertEquals(Money(640.0, "SAR"), comparison.budget2Spending)
        assertEquals(Money(10.0, "SAR"), comparison.spendingDifference)
        assertEquals(1.59, comparison.spendingChangePercentage, 0.01)
        
        // Check category comparisons
        assertEquals(2, comparison.categoryComparisons.size)
        
        val groceryComparison = comparison.categoryComparisons.find { it.categoryId == "category1" }!!
        assertEquals(Money(500.0, "SAR"), groceryComparison.budget1Amount)
        assertEquals(Money(600.0, "SAR"), groceryComparison.budget2Amount)
        assertEquals(Money(100.0, "SAR"), groceryComparison.budgetDifference)
        assertEquals(20.0, groceryComparison.budgetChangePercentage)
        assertEquals(Money(450.0, "SAR"), groceryComparison.budget1Spent)
        assertEquals(Money(520.0, "SAR"), groceryComparison.budget2Spent)
        assertEquals(Money(70.0, "SAR"), groceryComparison.spendingDifference)
        assertEquals(15.56, groceryComparison.spendingChangePercentage, 0.01)

        coVerify { budgetRepository.getBudget("budget1") }
        coVerify { budgetRepository.getBudget("budget2") }
    }

    @Test
    fun `compare budgets with different categories shows missing categories`() = runTest {
        // Given
        val budget1 = Budget(
            id = "budget1",
            name = "Budget 1",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            categories = mapOf(
                "category1" to Money(500.0, "SAR"),
                "category2" to Money(200.0, "SAR")
            ),
            spent = mapOf(
                "category1" to Money(400.0, "SAR"),
                "category2" to Money(150.0, "SAR")
            )
        )

        val budget2 = Budget(
            id = "budget2",
            name = "Budget 2",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 2, 1, 0, 0),
            endDate = LocalDateTime(2024, 2, 29, 23, 59),
            categories = mapOf(
                "category1" to Money(600.0, "SAR"),
                "category3" to Money(300.0, "SAR")
            ),
            spent = mapOf(
                "category1" to Money(500.0, "SAR"),
                "category3" to Money(250.0, "SAR")
            )
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget1
        coEvery { budgetRepository.getBudget("budget2") } returns budget2

        // When
        val result = useCase("budget1", "budget2")

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrThrow()
        
        // Should include all categories from both budgets
        assertEquals(3, comparison.categoryComparisons.size)
        
        // category1 exists in both
        val category1Comparison = comparison.categoryComparisons.find { it.categoryId == "category1" }!!
        assertEquals(Money(500.0, "SAR"), category1Comparison.budget1Amount)
        assertEquals(Money(600.0, "SAR"), category1Comparison.budget2Amount)
        
        // category2 only in budget1
        val category2Comparison = comparison.categoryComparisons.find { it.categoryId == "category2" }!!
        assertEquals(Money(200.0, "SAR"), category2Comparison.budget1Amount)
        assertEquals(Money(0.0, "SAR"), category2Comparison.budget2Amount)
        
        // category3 only in budget2
        val category3Comparison = comparison.categoryComparisons.find { it.categoryId == "category3" }!!
        assertEquals(Money(0.0, "SAR"), category3Comparison.budget1Amount)
        assertEquals(Money(300.0, "SAR"), category3Comparison.budget2Amount)
    }

    @Test
    fun `compare budgets handles repository error`() = runTest {
        // Given
        val error = Exception("Budget not found")
        coEvery { budgetRepository.getBudget("budget1") } throws error

        // When
        val result = useCase("budget1", "budget2")

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `compare budgets with zero amounts handles division by zero`() = runTest {
        // Given
        val budget1 = Budget(
            id = "budget1",
            name = "Budget 1",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            categories = mapOf("category1" to Money(0.0, "SAR")),
            spent = mapOf("category1" to Money(0.0, "SAR"))
        )

        val budget2 = Budget(
            id = "budget2",
            name = "Budget 2",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 2, 1, 0, 0),
            endDate = LocalDateTime(2024, 2, 29, 23, 59),
            categories = mapOf("category1" to Money(100.0, "SAR")),
            spent = mapOf("category1" to Money(50.0, "SAR"))
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget1
        coEvery { budgetRepository.getBudget("budget2") } returns budget2

        // When
        val result = useCase("budget1", "budget2")

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrThrow()
        
        // Should handle percentage calculation when base is zero
        assertEquals(0.0, comparison.budgetChangePercentage) // Special case: 0 to positive
        assertEquals(0.0, comparison.spendingChangePercentage)
        
        val categoryComparison = comparison.categoryComparisons.first()
        assertEquals(0.0, categoryComparison.budgetChangePercentage)
        assertEquals(0.0, categoryComparison.spendingChangePercentage)
    }
}