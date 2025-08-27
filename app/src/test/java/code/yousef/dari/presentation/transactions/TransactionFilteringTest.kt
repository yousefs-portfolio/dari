package code.yousef.dari.presentation.transactions

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.*
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFilteringTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val getTransactionsUseCase: GetTransactionsUseCase = mockk(relaxed = true)
    private val searchTransactionsUseCase: SearchTransactionsUseCase = mockk(relaxed = true)
    private val syncTransactionsUseCase: SyncTransactionsUseCase = mockk(relaxed = true)
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)

    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val sampleTransactions = createDiverseTransactions()
        every { getTransactionsUseCase(any()) } returns flowOf(sampleTransactions)
        
        viewModel = TransactionsViewModel(
            getTransactionsUseCase = getTransactionsUseCase,
            searchTransactionsUseCase = searchTransactionsUseCase,
            syncTransactionsUseCase = syncTransactionsUseCase,
            categorizeTransactionUseCase = categorizeTransactionUseCase,
            transactionRepository = transactionRepository,
            savedStateHandle = mockk(relaxed = true) {
                every { get<String>("accountId") } returns "test-account"
            }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filter by single category should show only matching transactions`() = runTest {
        // Given
        advanceUntilIdle() // Load initial transactions
        
        // When
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.selectedCategories.contains("Food & Dining"))
        assertTrue(state.filteredTransactions.all { it.category == "Food & Dining" })
        assertEquals(2, state.filteredTransactions.size) // 2 food transactions
    }

    @Test
    fun `filter by multiple categories should show transactions from all selected categories`() = runTest {
        // Given
        advanceUntilIdle() // Load initial transactions
        
        // When
        viewModel.filterByCategory("Food & Dining")
        viewModel.filterByCategory("Transportation")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.selectedCategories.contains("Food & Dining"))
        assertTrue(state.selectedCategories.contains("Transportation"))
        assertTrue(
            state.filteredTransactions.all { 
                it.category == "Food & Dining" || it.category == "Transportation" 
            }
        )
        assertEquals(3, state.filteredTransactions.size) // 2 food + 1 transportation
    }

    @Test
    fun `toggle category filter should add and remove category`() = runTest {
        // Given
        advanceUntilIdle()
        
        // When - Add category
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()
        
        // Then
        var state = viewModel.uiState.value
        assertTrue(state.selectedCategories.contains("Food & Dining"))
        
        // When - Toggle same category (should remove)
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()
        
        // Then
        state = viewModel.uiState.value
        assertTrue(!state.selectedCategories.contains("Food & Dining"))
        assertEquals(state.transactions.size, state.filteredTransactions.size) // All transactions shown
    }

    @Test
    fun `date range filter should only show transactions within range`() = runTest {
        // Given
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startDate = today.minus(kotlinx.datetime.DatePeriod(days = 2))
        val endDate = today
        
        val filteredResults = createDiverseTransactions().filter { transaction ->
            transaction.date >= startDate && transaction.date <= endDate
        }
        
        coEvery { 
            searchTransactionsUseCase.searchByDateRange(any(), startDate, endDate, any(), any())
        } returns Result.success(filteredResults)

        // When
        viewModel.filterByDateRange(startDate, endDate)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(Pair(startDate, endDate), state.dateRange)
        assertTrue(state.filteredTransactions.all { 
            it.date >= startDate && it.date <= endDate 
        })
        coVerify { 
            searchTransactionsUseCase.searchByDateRange("test-account", startDate, endDate, 100, 0)
        }
    }

    @Test
    fun `amount range filter should only show transactions within amount range`() = runTest {
        // Given
        val minAmount = Money.fromDouble(50.0, "SAR")
        val maxAmount = Money.fromDouble(200.0, "SAR")
        
        val filteredResults = listOf(
            createDiverseTransactions()[2] // Gas station (100 SAR)
        )
        
        coEvery { 
            searchTransactionsUseCase.searchByAmountRange(any(), minAmount, maxAmount, any(), any())
        } returns Result.success(filteredResults)

        // When
        viewModel.filterByAmount(minAmount, maxAmount)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(Pair(minAmount, maxAmount), state.amountRange)
        assertEquals(filteredResults, state.filteredTransactions)
        coVerify { 
            searchTransactionsUseCase.searchByAmountRange("test-account", minAmount, maxAmount, 100, 0)
        }
    }

    @Test
    fun `search with active category filter should apply both filters`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.filterByCategory("Food & Dining") // Apply category filter first
        
        val searchResults = listOf(createDiverseTransactions()[0]) // Starbucks
        coEvery { 
            searchTransactionsUseCase(any(), "Starbucks", any(), any())
        } returns Result.success(searchResults)

        // When
        viewModel.searchTransactions("Starbucks")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Starbucks", state.searchQuery)
        assertTrue(state.selectedCategories.contains("Food & Dining"))
        assertEquals(searchResults, state.filteredTransactions)
    }

    @Test
    fun `clear all filters should reset to original transaction list`() = runTest {
        // Given - apply various filters
        advanceUntilIdle()
        viewModel.filterByCategory("Food & Dining")
        viewModel.searchTransactions("test")
        advanceUntilIdle()
        
        // Verify filters are applied
        var state = viewModel.uiState.value
        assertTrue(state.selectedCategories.isNotEmpty())
        assertTrue(state.searchQuery.isNotEmpty())

        // When
        viewModel.clearAllFilters()
        advanceUntilIdle()

        // Then
        state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(emptySet<String>(), state.selectedCategories)
        assertEquals(null, state.dateRange)
        assertEquals(null, state.amountRange)
        assertEquals(state.transactions, state.filteredTransactions)
    }

    @Test
    fun `transaction grouping should maintain groups after filtering`() = runTest {
        // Given
        advanceUntilIdle()

        // When
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.groupedTransactions.isNotEmpty())
        
        // Verify all grouped transactions match the filter
        state.groupedTransactions.values.flatten().forEach { transaction ->
            assertEquals("Food & Dining", transaction.category)
        }
        
        // Verify total count matches filtered transactions
        val totalGroupedCount = state.groupedTransactions.values.sumOf { it.size }
        assertEquals(state.filteredTransactions.size, totalGroupedCount)
    }

    @Test
    fun `filter with no matching results should show empty list`() = runTest {
        // Given
        coEvery { 
            searchTransactionsUseCase(any(), "NonExistentMerchant", any(), any())
        } returns Result.success(emptyList())

        // When
        viewModel.searchTransactions("NonExistentMerchant")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("NonExistentMerchant", state.searchQuery)
        assertTrue(state.filteredTransactions.isEmpty())
        assertTrue(state.groupedTransactions.isEmpty())
    }

    // Helper method to create diverse transaction data for testing
    private fun createDiverseTransactions(): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            Transaction(
                id = "tx1",
                accountId = "test-account",
                amount = Money.fromDouble(-25.50, "SAR"),
                description = "Starbucks",
                date = today,
                type = TransactionType.DEBIT,
                category = "Food & Dining",
                merchant = Merchant(name = "Starbucks", category = "Food & Dining"),
                reference = "REFtx1",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx2",
                accountId = "test-account",
                amount = Money.fromDouble(-45.0, "SAR"),
                description = "McDonald's",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 1)),
                type = TransactionType.DEBIT,
                category = "Food & Dining",
                merchant = Merchant(name = "McDonald's", category = "Food & Dining"),
                reference = "REFtx2",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx3",
                accountId = "test-account",
                amount = Money.fromDouble(-100.0, "SAR"),
                description = "Gas Station",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 1)),
                type = TransactionType.DEBIT,
                category = "Transportation",
                merchant = Merchant(name = "Shell", category = "Transportation"),
                reference = "REFtx3",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx4",
                accountId = "test-account",
                amount = Money.fromDouble(5000.0, "SAR"),
                description = "Salary",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 3)),
                type = TransactionType.CREDIT,
                category = "Income",
                merchant = Merchant(name = "Company Inc", category = "Income"),
                reference = "REFtx4",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx5",
                accountId = "test-account",
                amount = Money.fromDouble(-300.0, "SAR"),
                description = "Amazon Purchase",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 4)),
                type = TransactionType.DEBIT,
                category = "Shopping",
                merchant = Merchant(name = "Amazon", category = "Shopping"),
                reference = "REFtx5",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            )
        )
    }
}