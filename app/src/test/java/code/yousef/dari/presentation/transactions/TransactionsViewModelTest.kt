package code.yousef.dari.presentation.transactions

import androidx.lifecycle.SavedStateHandle
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val getTransactionsUseCase: GetTransactionsUseCase = mockk(relaxed = true)
    private val searchTransactionsUseCase: SearchTransactionsUseCase = mockk(relaxed = true)
    private val syncTransactionsUseCase: SyncTransactionsUseCase = mockk(relaxed = true)
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)

    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        every { savedStateHandle.get<String>("accountId") } returns "test-account-id"
        
        // Create sample transactions
        val sampleTransactions = createSampleTransactions()
        every { getTransactionsUseCase(any()) } returns flowOf(sampleTransactions)
        
        viewModel = TransactionsViewModel(
            getTransactionsUseCase = getTransactionsUseCase,
            searchTransactionsUseCase = searchTransactionsUseCase,
            syncTransactionsUseCase = syncTransactionsUseCase,
            categorizeTransactionUseCase = categorizeTransactionUseCase,
            transactionRepository = transactionRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading with empty transactions`() {
        // Given - ViewModel is created in setup()
        
        // Then
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertEquals(emptyList(), initialState.transactions)
        assertEquals(emptyList(), initialState.filteredTransactions)
        assertEquals("", initialState.searchQuery)
        assertEquals(emptyMap<String, List<Transaction>>(), initialState.groupedTransactions)
    }

    @Test
    fun `loadTransactions should update state with transactions`() = runTest {
        // Given
        val sampleTransactions = createSampleTransactions()
        every { getTransactionsUseCase(any()) } returns flowOf(sampleTransactions)

        // When
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(sampleTransactions, finalState.transactions)
        assertEquals(sampleTransactions, finalState.filteredTransactions)
        assertEquals(3, finalState.groupedTransactions.size) // 3 different dates
    }

    @Test
    fun `searchTransactions should filter transactions by query`() = runTest {
        // Given
        val query = "Starbucks"
        val searchResults = listOf(createSampleTransactions()[0]) // Only Starbucks transaction
        coEvery { searchTransactionsUseCase(any(), query, any(), any()) } returns Result.success(searchResults)

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(query, finalState.searchQuery)
        assertEquals(searchResults, finalState.filteredTransactions)
        assertTrue(finalState.isSearching)
        coVerify { searchTransactionsUseCase("test-account-id", query, 100, 0) }
    }

    @Test
    fun `clearSearch should reset search state`() = runTest {
        // Given - set search first
        viewModel.searchTransactions("test query")
        advanceUntilIdle()

        // When
        viewModel.clearSearch()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals("", finalState.searchQuery)
        assertFalse(finalState.isSearching)
        assertEquals(finalState.transactions, finalState.filteredTransactions)
    }

    @Test
    fun `filterByCategory should apply category filter`() = runTest {
        // Given
        val category = "Food & Dining"
        val filteredTransactions = listOf(createSampleTransactions()[0]) // Only food transaction
        
        // When
        viewModel.filterByCategory(category)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(setOf(category), finalState.selectedCategories)
        assertEquals(filteredTransactions.size, finalState.filteredTransactions.count { it.category == category })
    }

    @Test
    fun `filterByDateRange should apply date filter`() = runTest {
        // Given
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startDate = today.minus(kotlinx.datetime.DatePeriod(days = 7))
        val endDate = today
        
        coEvery { 
            searchTransactionsUseCase.searchByDateRange(any(), startDate, endDate, any(), any())
        } returns Result.success(listOf(createSampleTransactions()[0]))

        // When
        viewModel.filterByDateRange(startDate, endDate)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(startDate, finalState.dateRange?.first)
        assertEquals(endDate, finalState.dateRange?.second)
        coVerify { searchTransactionsUseCase.searchByDateRange("test-account-id", startDate, endDate, 100, 0) }
    }

    @Test
    fun `filterByAmount should apply amount range filter`() = runTest {
        // Given
        val minAmount = Money.fromDouble(10.0, "SAR")
        val maxAmount = Money.fromDouble(100.0, "SAR")
        
        coEvery { 
            searchTransactionsUseCase.searchByAmountRange(any(), minAmount, maxAmount, any(), any())
        } returns Result.success(listOf(createSampleTransactions()[1]))

        // When
        viewModel.filterByAmount(minAmount, maxAmount)
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(Pair(minAmount, maxAmount), finalState.amountRange)
        coVerify { searchTransactionsUseCase.searchByAmountRange("test-account-id", minAmount, maxAmount, 100, 0) }
    }

    @Test
    fun `syncTransactions should refresh transaction data`() = runTest {
        // Given
        val syncResult = Result.success(Unit)
        coEvery { syncTransactionsUseCase(any()) } returns syncResult

        // When
        viewModel.syncTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isRefreshing)
        coVerify { syncTransactionsUseCase("test-account-id") }
    }

    @Test
    fun `loadMoreTransactions should load next page`() = runTest {
        // Given
        val nextPageTransactions = listOf(createSampleTransaction("tx4", "Amazon", 200.0, TransactionType.DEBIT))
        coEvery { 
            getTransactionsUseCase.getPaginated(any(), any(), any())
        } returns nextPageTransactions

        // When
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoadingMore)
        assertEquals(1, finalState.currentPage) // Should increment
        coVerify { getTransactionsUseCase.getPaginated("test-account-id", 50, 50) }
    }

    @Test
    fun `groupTransactionsByDate should group transactions correctly`() = runTest {
        // Given
        val sampleTransactions = createSampleTransactions()
        every { getTransactionsUseCase(any()) } returns flowOf(sampleTransactions)

        // When
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        val groupedTransactions = finalState.groupedTransactions
        
        // Should have groups for different dates
        assertTrue(groupedTransactions.isNotEmpty())
        groupedTransactions.values.forEach { transactions ->
            assertTrue(transactions.isNotEmpty())
        }
    }

    @Test
    fun `categorizeTransaction should update transaction category`() = runTest {
        // Given
        val transactionId = "tx1"
        val newCategory = "Entertainment"
        val updatedTransaction = createSampleTransactions()[0].copy(category = newCategory)
        
        coEvery { 
            categorizeTransactionUseCase(transactionId, newCategory)
        } returns Result.success(updatedTransaction)

        // When
        viewModel.categorizeTransaction(transactionId, newCategory)
        advanceUntilIdle()

        // Then
        coVerify { categorizeTransactionUseCase(transactionId, newCategory) }
    }

    @Test
    fun `selectTransaction should update selected transaction`() {
        // Given
        val transactionId = "tx1"

        // When
        viewModel.selectTransaction(transactionId)

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(transactionId, finalState.selectedTransactionId)
    }

    @Test
    fun `clearSelection should clear selected transaction`() {
        // Given
        viewModel.selectTransaction("tx1")

        // When
        viewModel.clearSelection()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(null, finalState.selectedTransactionId)
    }

    @Test
    fun `toggleBulkSelectionMode should toggle bulk selection`() {
        // Given
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isBulkSelectionMode)

        // When
        viewModel.toggleBulkSelectionMode()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.isBulkSelectionMode)
        assertTrue(finalState.selectedTransactionIds.isEmpty())
    }

    @Test
    fun `clearAllFilters should reset all filters`() = runTest {
        // Given - apply some filters first
        viewModel.searchTransactions("test")
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()

        // When
        viewModel.clearAllFilters()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals("", finalState.searchQuery)
        assertEquals(emptySet<String>(), finalState.selectedCategories)
        assertEquals(null, finalState.dateRange)
        assertEquals(null, finalState.amountRange)
        assertFalse(finalState.isSearching)
    }

    // Helper methods
    private fun createSampleTransactions(): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            createSampleTransaction("tx1", "Starbucks", 25.50, TransactionType.DEBIT, "Food & Dining", today),
            createSampleTransaction("tx2", "Salary", 5000.0, TransactionType.CREDIT, "Income", today.minus(kotlinx.datetime.DatePeriod(days = 1))),
            createSampleTransaction("tx3", "Gas Station", 80.0, TransactionType.DEBIT, "Transportation", today.minus(kotlinx.datetime.DatePeriod(days = 2)))
        )
    }

    private fun createSampleTransaction(
        id: String,
        description: String,
        amount: Double,
        type: TransactionType,
        category: String = "Other",
        date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "test-account-id",
            amount = Money.fromDouble(if (type == TransactionType.DEBIT) -amount else amount, "SAR"),
            description = description,
            date = date,
            type = type,
            category = category,
            merchant = Merchant(
                name = description,
                category = category
            ),
            reference = "REF$id",
            status = TransactionStatus.COMPLETED,
            metadata = emptyMap()
        )
    }
}