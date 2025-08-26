package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteBudgetUseCaseTest {

    private val budgetRepository = mockk<BudgetRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val deleteBudgetUseCase = DeleteBudgetUseCase(
        budgetRepository = budgetRepository,
        transactionRepository = transactionRepository
    )

    private val sampleBudget = Budget(
        id = "budget1",
        name = "Monthly Food Budget",
        nameArabic = "ميزانية الطعام الشهرية",
        amount = Money(1500.0, Currency.SAR),
        period = BudgetPeriod.MONTHLY,
        category = TransactionCategory.FOOD_DINING,
        startDate = LocalDateTime(2024, 1, 1, 0, 0),
        endDate = LocalDateTime(2024, 1, 31, 23, 59),
        accountIds = listOf("acc1"),
        alertThresholds = listOf(0.8, 0.9),
        isActive = true,
        includeSubcategories = true,
        budgetType = BudgetType.EXPENSE,
        rolloverUnused = false,
        tags = listOf("food", "monthly"),
        notes = "Budget for dining and groceries",
        notesArabic = "ميزانية للطعام والتسوق"
    )

    private val childBudget = Budget(
        id = "child_budget1",
        name = "Fast Food Budget",
        nameArabic = "ميزانية الوجبات السريعة",
        amount = Money(300.0, Currency.SAR),
        period = BudgetPeriod.MONTHLY,
        category = TransactionCategory.FOOD_DINING,
        startDate = LocalDateTime(2024, 1, 1, 0, 0),
        endDate = LocalDateTime(2024, 1, 31, 23, 59),
        accountIds = listOf("acc1"),
        alertThresholds = listOf(0.9),
        isActive = true,
        includeSubcategories = false,
        budgetType = BudgetType.EXPENSE,
        rolloverUnused = false,
        parentBudgetId = "budget1", // Child of sample budget
        tags = listOf("fastfood", "monthly")
    )

    @Test
    fun `should delete budget successfully when no dependencies exist`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = false
        )

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(emptyList())
        coEvery { budgetRepository.deleteBudget("budget1") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals("budget1", deleteResult.deletedBudgetId)
        assertEquals(emptyList(), deleteResult.affectedChildBudgets)
        
        coVerify {
            budgetRepository.getBudgetById("budget1")
            budgetRepository.getChildBudgets("budget1")
            budgetRepository.deleteBudget("budget1")
        }
    }

    @Test
    fun `should delete budget and reassign child budgets when cascade is true`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = false,
            cascadeToChildren = true
        )

        val childBudgets = listOf(childBudget)

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(childBudgets)
        coEvery { budgetRepository.updateBudget(any()) } returns Result.Success(childBudgets[0].copy(parentBudgetId = null))
        coEvery { budgetRepository.deleteBudget("budget1") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals("budget1", deleteResult.deletedBudgetId)
        assertEquals(1, deleteResult.affectedChildBudgets.size)
        assertEquals(null, deleteResult.affectedChildBudgets[0].parentBudgetId)
        
        coVerify {
            budgetRepository.updateBudget(match { it.parentBudgetId == null })
            budgetRepository.deleteBudget("budget1")
        }
    }

    @Test
    fun `should delete budget and all children when cascade delete is true`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = false,
            cascadeDelete = true
        )

        val childBudgets = listOf(childBudget)

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(childBudgets)
        coEvery { budgetRepository.deleteBudget("budget1") } returns Result.Success(Unit)
        coEvery { budgetRepository.deleteBudget("child_budget1") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals("budget1", deleteResult.deletedBudgetId)
        assertEquals(1, deleteResult.deletedChildBudgets.size)
        assertEquals("child_budget1", deleteResult.deletedChildBudgets[0])
        
        coVerify {
            budgetRepository.deleteBudget("budget1")
            budgetRepository.deleteBudget("child_budget1")
        }
    }

    @Test
    fun `should prevent deletion when child budgets exist and no cascade option`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = false,
            cascadeToChildren = false,
            cascadeDelete = false
        )

        val childBudgets = listOf(childBudget)

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(childBudgets)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("child budgets") == true)
        
        coVerify(exactly = 0) {
            budgetRepository.deleteBudget(any())
        }
    }

    @Test
    fun `should force delete budget with active transactions when force is true`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = true
        )

        val activeTransactions = listOf(
            Transaction(
                id = "tx1",
                accountId = "acc1",
                amount = Money(50.0, Currency.SAR),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FOOD_DINING,
                description = "Restaurant",
                date = LocalDateTime(2024, 1, 15, 12, 0),
                status = TransactionStatus.COMPLETED,
                budgetId = "budget1"
            )
        )

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(emptyList())
        coEvery { transactionRepository.getTransactionsByBudgetId("budget1") } returns Result.Success(activeTransactions)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(activeTransactions[0].copy(budgetId = null))
        coEvery { budgetRepository.deleteBudget("budget1") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals("budget1", deleteResult.deletedBudgetId)
        assertEquals(1, deleteResult.unlinkedTransactions)
        
        coVerify {
            transactionRepository.updateTransaction(match { it.budgetId == null })
            budgetRepository.deleteBudget("budget1")
        }
    }

    @Test
    fun `should prevent deletion when active transactions exist and force is false`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = false
        )

        val activeTransactions = listOf(
            Transaction(
                id = "tx1",
                accountId = "acc1",
                amount = Money(50.0, Currency.SAR),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FOOD_DINING,
                description = "Restaurant",
                date = LocalDateTime(2024, 1, 15, 12, 0),
                status = TransactionStatus.COMPLETED,
                budgetId = "budget1"
            )
        )

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(emptyList())
        coEvery { transactionRepository.getTransactionsByBudgetId("budget1") } returns Result.Success(activeTransactions)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("active transactions") == true)
        
        coVerify(exactly = 0) {
            budgetRepository.deleteBudget(any())
        }
    }

    @Test
    fun `should handle Ramadan budget deletion with special considerations`() = runTest {
        // Given
        val ramadanBudget = sampleBudget.copy(
            id = "ramadan_budget",
            name = "Ramadan Budget",
            nameArabic = "ميزانية رمضان",
            category = TransactionCategory.CHARITY,
            tags = listOf("ramadan", "charity", "zakat"),
            notes = "Special budget for Ramadan activities",
            notesArabic = "ميزانية خاصة لأنشطة رمضان"
        )

        val request = DeleteBudgetRequest(
            budgetId = "ramadan_budget",
            force = false,
            preserveTransactionHistory = true
        )

        coEvery { budgetRepository.getBudgetById("ramadan_budget") } returns Result.Success(ramadanBudget)
        coEvery { budgetRepository.getChildBudgets("ramadan_budget") } returns Result.Success(emptyList())
        coEvery { transactionRepository.getTransactionsByBudgetId("ramadan_budget") } returns Result.Success(emptyList())
        coEvery { budgetRepository.archiveBudget("ramadan_budget") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals("ramadan_budget", deleteResult.deletedBudgetId)
        assertTrue(deleteResult.archived)
        
        coVerify {
            budgetRepository.archiveBudget("ramadan_budget")
        }
        coVerify(exactly = 0) {
            budgetRepository.deleteBudget("ramadan_budget")
        }
    }

    @Test
    fun `should validate budget exists before deletion`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "nonexistent_budget",
            force = false
        )

        coEvery { budgetRepository.getBudgetById("nonexistent_budget") } returns Result.Error(
            Exception("Budget not found")
        )

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Budget not found") == true)
    }

    @Test
    fun `should handle deletion of Islamic budgets with special rules`() = runTest {
        // Given
        val zakatBudget = sampleBudget.copy(
            id = "zakat_budget",
            name = "Zakat Budget",
            nameArabic = "ميزانية الزكاة",
            category = TransactionCategory.CHARITY,
            budgetType = BudgetType.CHARITY,
            tags = listOf("zakat", "islamic", "obligation"),
            amount = Money(2000.0, Currency.SAR)
        )

        val request = DeleteBudgetRequest(
            budgetId = "zakat_budget",
            force = false,
            preserveTransactionHistory = true
        )

        coEvery { budgetRepository.getBudgetById("zakat_budget") } returns Result.Success(zakatBudget)
        coEvery { budgetRepository.getChildBudgets("zakat_budget") } returns Result.Success(emptyList())
        coEvery { transactionRepository.getTransactionsByBudgetId("zakat_budget") } returns Result.Success(emptyList())
        coEvery { budgetRepository.archiveBudget("zakat_budget") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertTrue(deleteResult.archived) // Zakat budgets should be archived, not deleted
        
        coVerify {
            budgetRepository.archiveBudget("zakat_budget")
        }
    }

    @Test
    fun `should handle budget with recurring transactions`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = false,
            cancelRecurringTransactions = true
        )

        val recurringTransaction = Transaction(
            id = "recurring_tx1",
            accountId = "acc1",
            amount = Money(100.0, Currency.SAR),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FOOD_DINING,
            description = "Monthly grocery budget",
            date = LocalDateTime(2024, 1, 1, 0, 0),
            status = TransactionStatus.PENDING,
            isRecurring = true,
            budgetId = "budget1"
        )

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(emptyList())
        coEvery { transactionRepository.getTransactionsByBudgetId("budget1") } returns Result.Success(listOf(recurringTransaction))
        coEvery { transactionRepository.cancelRecurringTransaction("recurring_tx1") } returns Result.Success(Unit)
        coEvery { budgetRepository.deleteBudget("budget1") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals(1, deleteResult.cancelledRecurringTransactions)
        
        coVerify {
            transactionRepository.cancelRecurringTransaction("recurring_tx1")
            budgetRepository.deleteBudget("budget1")
        }
    }

    @Test
    fun `should provide comprehensive deletion summary`() = runTest {
        // Given
        val request = DeleteBudgetRequest(
            budgetId = "budget1",
            force = true,
            cascadeDelete = true
        )

        val childBudgets = listOf(childBudget)
        val transactions = listOf(
            Transaction(
                id = "tx1",
                accountId = "acc1",
                amount = Money(50.0, Currency.SAR),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FOOD_DINING,
                description = "Restaurant",
                date = LocalDateTime(2024, 1, 15, 12, 0),
                status = TransactionStatus.COMPLETED,
                budgetId = "budget1"
            )
        )

        coEvery { budgetRepository.getBudgetById("budget1") } returns Result.Success(sampleBudget)
        coEvery { budgetRepository.getChildBudgets("budget1") } returns Result.Success(childBudgets)
        coEvery { transactionRepository.getTransactionsByBudgetId("budget1") } returns Result.Success(transactions)
        coEvery { transactionRepository.updateTransaction(any()) } returns Result.Success(transactions[0].copy(budgetId = null))
        coEvery { budgetRepository.deleteBudget("budget1") } returns Result.Success(Unit)
        coEvery { budgetRepository.deleteBudget("child_budget1") } returns Result.Success(Unit)

        // When
        val result = deleteBudgetUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val deleteResult = result.data
        assertEquals("budget1", deleteResult.deletedBudgetId)
        assertEquals(1, deleteResult.deletedChildBudgets.size)
        assertEquals(1, deleteResult.unlinkedTransactions)
        assertEquals(Money(1500.0, Currency.SAR), deleteResult.originalBudgetAmount)
        assertEquals("Monthly Food Budget", deleteResult.originalBudgetName)
    }
}