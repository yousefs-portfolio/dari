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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BulkSelectionTest {

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
        
        val sampleTransactions = createBulkTestTransactions()
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
    fun `toggleBulkSelectionMode should enable bulk selection mode`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
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
    fun `toggleBulkSelectionMode should disable bulk selection mode and clear selections`() = runTest {
        // Given - enable bulk mode and select some transactions
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()
        viewModel.toggleTransactionSelection("tx1")
        viewModel.toggleTransactionSelection("tx2")
        
        val bulkModeState = viewModel.uiState.value
        assertTrue(bulkModeState.isBulkSelectionMode)
        assertEquals(2, bulkModeState.selectedTransactionIds.size)

        // When - toggle bulk mode off
        viewModel.toggleBulkSelectionMode()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isBulkSelectionMode)
        assertTrue(finalState.selectedTransactionIds.isEmpty())
    }

    @Test
    fun `toggleTransactionSelection should add transaction to selection`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()
        
        // When
        viewModel.toggleTransactionSelection("tx1")

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.selectedTransactionIds.contains("tx1"))
        assertEquals(1, state.selectedTransactionIds.size)
    }

    @Test
    fun `toggleTransactionSelection should remove transaction from selection`() = runTest {
        // Given - select a transaction first
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()
        viewModel.toggleTransactionSelection("tx1")
        
        val selectedState = viewModel.uiState.value
        assertTrue(selectedState.selectedTransactionIds.contains("tx1"))

        // When - toggle same transaction again
        viewModel.toggleTransactionSelection("tx1")

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.selectedTransactionIds.contains("tx1"))
        assertEquals(0, finalState.selectedTransactionIds.size)
    }

    @Test
    fun `multiple transaction selection should work correctly`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()

        // When - select multiple transactions
        viewModel.toggleTransactionSelection("tx1")
        viewModel.toggleTransactionSelection("tx2")
        viewModel.toggleTransactionSelection("tx3")

        // Then
        val state = viewModel.uiState.value
        assertEquals(3, state.selectedTransactionIds.size)
        assertTrue(state.selectedTransactionIds.contains("tx1"))
        assertTrue(state.selectedTransactionIds.contains("tx2"))
        assertTrue(state.selectedTransactionIds.contains("tx3"))
    }

    @Test
    fun `selectAllVisibleTransactions should select all filtered transactions`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()
        
        val state = viewModel.uiState.value
        val visibleTransactionIds = state.filteredTransactions.map { it.id }

        // When
        viewModel.selectAllVisibleTransactions()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(visibleTransactionIds.size, finalState.selectedTransactionIds.size)
        assertTrue(finalState.selectedTransactionIds.containsAll(visibleTransactionIds))
    }

    @Test
    fun `selectAllVisibleTransactions with filters should only select filtered transactions`() = runTest {
        // Given - apply filter first
        advanceUntilIdle()
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()
        
        viewModel.toggleBulkSelectionMode()
        
        val stateWithFilter = viewModel.uiState.value
        val filteredTransactionIds = stateWithFilter.filteredTransactions.map { it.id }

        // When
        viewModel.selectAllVisibleTransactions()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(filteredTransactionIds.size, finalState.selectedTransactionIds.size)
        assertTrue(finalState.selectedTransactionIds.containsAll(filteredTransactionIds))
        
        // Verify no transactions outside the filter are selected
        val allTransactionIds = finalState.transactions.map { it.id }
        val unfiltered = allTransactionIds - filteredTransactionIds.toSet()
        assertTrue(finalState.selectedTransactionIds.intersect(unfiltered).isEmpty())
    }

    @Test
    fun `clearAllSelections should remove all selected transactions`() = runTest {
        // Given - select multiple transactions
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()
        viewModel.toggleTransactionSelection("tx1")
        viewModel.toggleTransactionSelection("tx2")
        viewModel.toggleTransactionSelection("tx3")
        
        val selectedState = viewModel.uiState.value
        assertEquals(3, selectedState.selectedTransactionIds.size)

        // When
        viewModel.clearAllSelections()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(0, finalState.selectedTransactionIds.size)
        assertTrue(finalState.isBulkSelectionMode) // Should remain in bulk mode
    }

    @Test
    fun `bulk selection should work with search`() = runTest {
        // Given - perform search first
        val searchResults = createBulkTestTransactions().filter { 
            it.description.contains("Coffee", ignoreCase = true) 
        }
        coEvery { 
            searchTransactionsUseCase(any(), "Coffee", any(), any())
        } returns Result.success(searchResults)
        
        viewModel.searchTransactions("Coffee")
        advanceUntilIdle()
        
        viewModel.toggleBulkSelectionMode()

        // When - select all from search results
        viewModel.selectAllVisibleTransactions()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(searchResults.size, finalState.selectedTransactionIds.size)
        
        // Verify only search result IDs are selected
        val searchResultIds = searchResults.map { it.id }
        assertTrue(finalState.selectedTransactionIds.containsAll(searchResultIds))
    }

    @Test
    fun `bulk selection should persist during filtering`() = runTest {
        // Given - select some transactions first
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()
        viewModel.toggleTransactionSelection("tx1")
        viewModel.toggleTransactionSelection("tx2")
        
        val selectedIds = viewModel.uiState.value.selectedTransactionIds

        // When - apply filter that includes selected transactions
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()

        // Then - selections should persist if transactions are still visible
        val finalState = viewModel.uiState.value
        val visibleSelectedIds = finalState.filteredTransactions
            .filter { selectedIds.contains(it.id) }
            .map { it.id }
        
        assertTrue(finalState.selectedTransactionIds.containsAll(visibleSelectedIds))
    }

    @Test
    fun `bulk selection count should be accurate`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.toggleBulkSelectionMode()

        // When - select transactions one by one
        viewModel.toggleTransactionSelection("tx1")
        assertEquals(1, viewModel.uiState.value.selectedTransactionIds.size)
        
        viewModel.toggleTransactionSelection("tx2")
        assertEquals(2, viewModel.uiState.value.selectedTransactionIds.size)
        
        viewModel.toggleTransactionSelection("tx3")
        assertEquals(3, viewModel.uiState.value.selectedTransactionIds.size)
        
        // Remove one selection
        viewModel.toggleTransactionSelection("tx2")
        assertEquals(2, viewModel.uiState.value.selectedTransactionIds.size)
    }

    @Test
    fun `single transaction selection should work outside bulk mode`() = runTest {
        // Given
        advanceUntilIdle()
        
        // When - select transaction outside bulk mode
        viewModel.selectTransaction("tx1")

        // Then
        val state = viewModel.uiState.value
        assertEquals("tx1", state.selectedTransactionId)
        assertFalse(state.isBulkSelectionMode)
        
        // Should not affect bulk selections
        assertTrue(state.selectedTransactionIds.isEmpty())
    }

    @Test
    fun `clearSelection should clear single transaction selection`() = runTest {
        // Given - select a transaction
        advanceUntilIdle()
        viewModel.selectTransaction("tx1")
        
        val selectedState = viewModel.uiState.value
        assertEquals("tx1", selectedState.selectedTransactionId)

        // When
        viewModel.clearSelection()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(null, finalState.selectedTransactionId)
    }

    @Test
    fun `bulk selection should not interfere with single selection`() = runTest {
        // Given - select single transaction first
        advanceUntilIdle()
        viewModel.selectTransaction("tx1")
        
        // When - enable bulk mode and make bulk selections
        viewModel.toggleBulkSelectionMode()
        viewModel.toggleTransactionSelection("tx2")
        viewModel.toggleTransactionSelection("tx3")

        // Then
        val finalState = viewModel.uiState.value
        assertEquals("tx1", finalState.selectedTransactionId) // Single selection preserved
        assertEquals(2, finalState.selectedTransactionIds.size) // Bulk selections separate
        assertFalse(finalState.selectedTransactionIds.contains("tx1")) // tx1 not in bulk selection
    }

    // Helper method to create test transactions for bulk operations
    private fun createBulkTestTransactions(): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            Transaction(
                id = "tx1",
                accountId = "test-account",
                amount = Money.fromDouble(-25.50, "SAR"),
                description = "Morning Coffee",
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
                amount = Money.fromDouble(-15.75, "SAR"),
                description = "Afternoon Coffee",
                date = today,
                type = TransactionType.DEBIT,
                category = "Food & Dining",
                merchant = Merchant(name = "Local Cafe", category = "Food & Dining"),
                reference = "REFtx2",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx3",
                accountId = "test-account",
                amount = Money.fromDouble(-100.00, "SAR"),
                description = "Gas Fill-up",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 1)),
                type = TransactionType.DEBIT,
                category = "Transportation",
                merchant = Merchant(name = "Shell", category = "Gas Stations"),
                reference = "REFtx3",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx4",
                accountId = "test-account",
                amount = Money.fromDouble(-50.00, "SAR"),
                description = "Grocery Shopping",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 1)),
                type = TransactionType.DEBIT,
                category = "Groceries",
                merchant = Merchant(name = "Carrefour", category = "Supermarkets"),
                reference = "REFtx4",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx5",
                accountId = "test-account",
                amount = Money.fromDouble(5000.00, "SAR"),
                description = "Monthly Salary",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 2)),
                type = TransactionType.CREDIT,
                category = "Income",
                merchant = Merchant(name = "Company Inc", category = "Employer"),
                reference = "REFtx5",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            )
        )
    }
}