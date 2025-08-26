package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.model.Budget
import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.domain.repository.TransactionRepository
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

class CalculateCashFlowUseCaseTest {

    private val testCoroutineRule = TestCoroutineRule()

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: CalculateCashFlowUseCase

    @BeforeTest
    fun setup() {
        budgetRepository = mockk()
        transactionRepository = mockk()
        useCase = CalculateCashFlowUseCase(budgetRepository, transactionRepository)
    }

    @Test
    fun `calculate cash flow projection for 6 months`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "Current Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "groceries" to Money(1000.0, "SAR"),
                "rent" to Money(2000.0, "SAR"),
                "entertainment" to Money(300.0, "SAR")
            ),
            spent = mapOf(
                "groceries" to Money(800.0, "SAR"),
                "rent" to Money(2000.0, "SAR"),
                "entertainment" to Money(250.0, "SAR")
            ),
            totalIncome = Money(5000.0, "SAR")
        )

        val historicalTransactions = listOf(
            createIncomeTransaction("1", LocalDateTime(2024, 2, 1, 0, 0), Money(5000.0, "SAR")),
            createExpenseTransaction("2", LocalDateTime(2024, 2, 5, 0, 0), Money(900.0, "SAR"), "groceries"),
            createExpenseTransaction("3", LocalDateTime(2024, 2, 1, 0, 0), Money(2000.0, "SAR"), "rent"),
            createIncomeTransaction("4", LocalDateTime(2024, 1, 1, 0, 0), Money(5000.0, "SAR")),
            createExpenseTransaction("5", LocalDateTime(2024, 1, 15, 0, 0), Money(850.0, "SAR"), "groceries")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns historicalTransactions

        // When
        val result = useCase("budget1", 6)

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        assertEquals("budget1", cashFlow.budgetId)
        assertEquals(6, cashFlow.projectionMonths)
        assertEquals(6, cashFlow.monthlyProjections.size)
        
        // Should project consistent monthly income of 5000 SAR
        cashFlow.monthlyProjections.forEach { monthly ->
            assertEquals(Money(5000.0, "SAR"), monthly.projectedIncome)
            assertTrue(monthly.projectedExpenses.amount > 0)
            assertTrue(monthly.netCashFlow.amount != 0.0)
        }
        
        // Running balance should accumulate over time
        assertTrue(cashFlow.monthlyProjections.last().runningBalance.amount != cashFlow.monthlyProjections.first().runningBalance.amount)
        
        // Should identify trends
        assertTrue(cashFlow.insights.isNotEmpty())

        coVerify { budgetRepository.getBudget("budget1") }
        coVerify { transactionRepository.getTransactionsByDateRange(any(), any()) }
    }

    @Test
    fun `calculate cash flow with positive net flow`() = runTest {
        // Given - Income exceeds expenses
        val budget = Budget(
            id = "budget1",
            name = "High Income Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("basics" to Money(2000.0, "SAR")),
            spent = mapOf("basics" to Money(1800.0, "SAR")),
            totalIncome = Money(4000.0, "SAR")
        )

        val historicalTransactions = listOf(
            createIncomeTransaction("1", LocalDateTime(2024, 2, 1, 0, 0), Money(4000.0, "SAR")),
            createExpenseTransaction("2", LocalDateTime(2024, 2, 15, 0, 0), Money(1900.0, "SAR"), "basics")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns historicalTransactions

        // When
        val result = useCase("budget1", 3)

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        // All months should have positive cash flow
        cashFlow.monthlyProjections.forEach { monthly ->
            assertTrue(monthly.netCashFlow.amount > 0, "Month ${monthly.month} should have positive cash flow")
        }
        
        // Running balance should increase each month
        val firstBalance = cashFlow.monthlyProjections.first().runningBalance.amount
        val lastBalance = cashFlow.monthlyProjections.last().runningBalance.amount
        assertTrue(lastBalance > firstBalance, "Running balance should increase")
        
        // Should suggest savings opportunities
        assertTrue(cashFlow.insights.any { it.contains("positive") || it.contains("surplus") })
    }

    @Test
    fun `calculate cash flow with negative net flow`() = runTest {
        // Given - Expenses exceed income
        val budget = Budget(
            id = "budget1",
            name = "Tight Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "rent" to Money(2500.0, "SAR"),
                "food" to Money(1200.0, "SAR"),
                "utilities" to Money(400.0, "SAR")
            ),
            spent = mapOf(
                "rent" to Money(2500.0, "SAR"),
                "food" to Money(1100.0, "SAR"),
                "utilities" to Money(380.0, "SAR")
            ),
            totalIncome = Money(3500.0, "SAR") // Less than total spending
        )

        val historicalTransactions = listOf(
            createIncomeTransaction("1", LocalDateTime(2024, 2, 1, 0, 0), Money(3500.0, "SAR")),
            createExpenseTransaction("2", LocalDateTime(2024, 2, 1, 0, 0), Money(2500.0, "SAR"), "rent"),
            createExpenseTransaction("3", LocalDateTime(2024, 2, 15, 0, 0), Money(1200.0, "SAR"), "food")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns historicalTransactions

        // When
        val result = useCase("budget1", 3)

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        // Should have negative cash flow
        cashFlow.monthlyProjections.forEach { monthly ->
            assertTrue(monthly.netCashFlow.amount < 0, "Month ${monthly.month} should have negative cash flow")
        }
        
        // Running balance should decrease
        val firstBalance = cashFlow.monthlyProjections.first().runningBalance.amount
        val lastBalance = cashFlow.monthlyProjections.last().runningBalance.amount
        assertTrue(lastBalance < firstBalance, "Running balance should decrease")
        
        // Should warn about deficit
        assertTrue(cashFlow.insights.any { it.contains("deficit") || it.contains("negative") })
    }

    @Test
    fun `calculate cash flow with seasonal variations`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "Seasonal Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("variable" to Money(1000.0, "SAR")),
            spent = mapOf("variable" to Money(800.0, "SAR")),
            totalIncome = Money(3000.0, "SAR")
        )

        val historicalTransactions = listOf(
            // January - high expenses (holidays)
            createIncomeTransaction("1", LocalDateTime(2024, 1, 1, 0, 0), Money(3000.0, "SAR")),
            createExpenseTransaction("2", LocalDateTime(2024, 1, 15, 0, 0), Money(1500.0, "SAR"), "variable"),
            // February - normal expenses
            createIncomeTransaction("3", LocalDateTime(2024, 2, 1, 0, 0), Money(3000.0, "SAR")),
            createExpenseTransaction("4", LocalDateTime(2024, 2, 15, 0, 0), Money(900.0, "SAR"), "variable"),
            // December previous year - high expenses
            createIncomeTransaction("5", LocalDateTime(2023, 12, 1, 0, 0), Money(3000.0, "SAR")),
            createExpenseTransaction("6", LocalDateTime(2023, 12, 20, 0, 0), Money(1600.0, "SAR"), "variable")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns historicalTransactions

        // When - Project through December to capture seasonal pattern
        val result = useCase("budget1", 10) // March to December

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        // Should have different projections for different months
        val decemberProjection = cashFlow.monthlyProjections.find { it.month == 12 }
        val regularMonthProjection = cashFlow.monthlyProjections.find { it.month in 4..11 }
        
        if (decemberProjection != null && regularMonthProjection != null) {
            // December should have higher expenses due to seasonal pattern
            assertTrue(
                decemberProjection.projectedExpenses.amount > regularMonthProjection.projectedExpenses.amount,
                "December should have higher projected expenses due to seasonal pattern"
            )
        }
        
        // Should mention seasonal variations
        assertTrue(cashFlow.insights.any { it.contains("seasonal") || it.contains("December") || it.contains("holiday") })
    }

    @Test
    fun `calculate cash flow with irregular income`() = runTest {
        // Given - Freelancer with variable income
        val budget = Budget(
            id = "budget1",
            name = "Freelancer Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("expenses" to Money(2000.0, "SAR")),
            spent = mapOf("expenses" to Money(1800.0, "SAR")),
            totalIncome = Money(3000.0, "SAR") // Current month
        )

        val historicalTransactions = listOf(
            // Variable income pattern
            createIncomeTransaction("1", LocalDateTime(2024, 2, 1, 0, 0), Money(4500.0, "SAR")), // Good month
            createIncomeTransaction("2", LocalDateTime(2024, 1, 1, 0, 0), Money(2000.0, "SAR")), // Poor month
            createIncomeTransaction("3", LocalDateTime(2023, 12, 1, 0, 0), Money(5000.0, "SAR")), // Excellent month
            createIncomeTransaction("4", LocalDateTime(2023, 11, 1, 0, 0), Money(1500.0, "SAR")), // Bad month
            createExpenseTransaction("5", LocalDateTime(2024, 2, 15, 0, 0), Money(1900.0, "SAR"), "expenses")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns historicalTransactions

        // When
        val result = useCase("budget1", 6)

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        // Should use average income for projections due to variability
        val avgHistoricalIncome = (4500.0 + 2000.0 + 5000.0 + 1500.0) / 4 // 3250 SAR average
        
        cashFlow.monthlyProjections.forEach { monthly ->
            assertTrue(
                monthly.projectedIncome.amount >= 2500.0 && monthly.projectedIncome.amount <= 4000.0,
                "Projected income should be within reasonable range of historical average"
            )
        }
        
        // Should warn about income variability
        assertTrue(cashFlow.insights.any { it.contains("variable") || it.contains("irregular") })
    }

    @Test
    fun `calculate cash flow with existing debt payments`() = runTest {
        // Given - Budget with recurring debt payments
        val budget = Budget(
            id = "budget1",
            name = "Debt Repayment Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf(
                "living" to Money(2500.0, "SAR"),
                "debt_payment" to Money(800.0, "SAR")
            ),
            spent = mapOf(
                "living" to Money(2400.0, "SAR"),
                "debt_payment" to Money(800.0, "SAR")
            ),
            totalIncome = Money(4000.0, "SAR")
        )

        val historicalTransactions = listOf(
            createIncomeTransaction("1", LocalDateTime(2024, 2, 1, 0, 0), Money(4000.0, "SAR")),
            createExpenseTransaction("2", LocalDateTime(2024, 2, 15, 0, 0), Money(2450.0, "SAR"), "living"),
            createExpenseTransaction("3", LocalDateTime(2024, 2, 5, 0, 0), Money(800.0, "SAR"), "debt_payment")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns historicalTransactions

        // When
        val result = useCase("budget1", 12)

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        // Should consistently project debt payments
        cashFlow.monthlyProjections.forEach { monthly ->
            assertTrue(
                monthly.projectedExpenses.amount >= 3200.0, // Living + debt payment
                "Should include recurring debt payments in projections"
            )
        }
        
        // Should mention debt service
        assertTrue(cashFlow.insights.any { it.contains("debt") })
    }

    @Test
    fun `calculate cash flow handles repository errors`() = runTest {
        // Given
        val error = Exception("Budget not found")
        coEvery { budgetRepository.getBudget("budget1") } throws error

        // When
        val result = useCase("budget1", 6)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `calculate cash flow with zero historical data uses budget as baseline`() = runTest {
        // Given
        val budget = Budget(
            id = "budget1",
            name = "New Budget",
            period = Budget.Period.MONTHLY,
            startDate = LocalDateTime(2024, 3, 1, 0, 0),
            endDate = LocalDateTime(2024, 3, 31, 23, 59),
            categories = mapOf("basics" to Money(1500.0, "SAR")),
            spent = mapOf("basics" to Money(1200.0, "SAR")),
            totalIncome = Money(2500.0, "SAR")
        )

        coEvery { budgetRepository.getBudget("budget1") } returns budget
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns emptyList()

        // When
        val result = useCase("budget1", 3)

        // Then
        assertTrue(result.isSuccess)
        val cashFlow = result.getOrThrow()
        
        // Should use budget values as baseline
        cashFlow.monthlyProjections.forEach { monthly ->
            assertEquals(Money(2500.0, "SAR"), monthly.projectedIncome)
            assertTrue(monthly.projectedExpenses.amount > 0)
        }
        
        // Should mention limited historical data
        assertTrue(cashFlow.insights.any { it.contains("limited") || it.contains("budget") })
    }

    private fun createIncomeTransaction(
        id: String,
        date: LocalDateTime,
        amount: Money
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount,
            description = "Income",
            date = date,
            categoryId = "income",
            category = null,
            merchantName = "Employer",
            type = Transaction.Type.INCOME,
            tags = emptyList(),
            receiptImagePath = null
        )
    }

    private fun createExpenseTransaction(
        id: String,
        date: LocalDateTime,
        amount: Money,
        categoryId: String
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount,
            description = "Expense",
            date = date,
            categoryId = categoryId,
            category = null,
            merchantName = "Store",
            type = Transaction.Type.EXPENSE,
            tags = emptyList(),
            receiptImagePath = null
        )
    }
}