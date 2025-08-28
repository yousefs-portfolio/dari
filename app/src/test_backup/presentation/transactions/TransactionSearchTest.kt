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
class TransactionSearchTest {

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
        
        val sampleTransactions = createSearchableTransactions()
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
    fun `search by merchant name should return matching transactions`() = runTest {
        // Given
        val query = "Starbucks"
        val searchResults = listOf(createSearchableTransactions()[0])
        
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(searchResults)

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        assertFalse(state.isSearching) // Should complete
        assertEquals(searchResults, state.filteredTransactions)
        coVerify { searchTransactionsUseCase("test-account", query, 100, 0) }
    }

    @Test
    fun `search by description should return matching transactions`() = runTest {
        // Given
        val query = "Coffee"
        val coffeeTransactions = createSearchableTransactions().filter { 
            it.description.contains("Coffee", ignoreCase = true) 
        }
        
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(coffeeTransactions)

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        assertEquals(coffeeTransactions, state.filteredTransactions)
        assertTrue(state.filteredTransactions.all { 
            it.description.contains("Coffee", ignoreCase = true) 
        })
    }

    @Test
    fun `search with Arabic text should work correctly`() = runTest {
        // Given
        val arabicQuery = "مطعم"
        val arabicResults = listOf(createArabicTransaction())
        
        coEvery { 
            searchTransactionsUseCase(any(), arabicQuery, any(), any())
        } returns Result.success(arabicResults)

        // When
        viewModel.searchTransactions(arabicQuery)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(arabicQuery, state.searchQuery)
        assertEquals(arabicResults, state.filteredTransactions)
    }

    @Test
    fun `search by amount should return transactions with matching amounts`() = runTest {
        // Given
        val query = "25.50"
        val amountMatches = createSearchableTransactions().filter { 
            it.amount.amount.toString().contains(query)
        }
        
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(amountMatches)

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        assertEquals(amountMatches, state.filteredTransactions)
    }

    @Test
    fun `search by category should return transactions in matching category`() = runTest {
        // Given
        val query = "Food"
        val foodTransactions = createSearchableTransactions().filter { 
            it.category.contains("Food", ignoreCase = true) 
        }
        
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(foodTransactions)

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        assertEquals(foodTransactions, state.filteredTransactions)
    }

    @Test
    fun `empty search query should clear search results`() = runTest {
        // Given - first perform a search
        coEvery { 
            searchTransactionsUseCase(any(), "Starbucks", any(), any())
        } returns Result.success(listOf(createSearchableTransactions()[0]))
        
        viewModel.searchTransactions("Starbucks")
        advanceUntilIdle()

        // When - search with empty query
        viewModel.searchTransactions("")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearching)
        assertEquals(state.transactions, state.filteredTransactions) // Should show all transactions
    }

    @Test
    fun `clear search should reset to all transactions`() = runTest {
        // Given - perform a search first
        coEvery { 
            searchTransactionsUseCase(any(), "test", any(), any())
        } returns Result.success(listOf(createSearchableTransactions()[0]))
        
        viewModel.searchTransactions("test")
        advanceUntilIdle()

        // When
        viewModel.clearSearch()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearching)
        assertEquals(state.transactions, state.filteredTransactions)
    }

    @Test
    fun `search with no results should show empty list`() = runTest {
        // Given
        val query = "NonExistentMerchant"
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(emptyList())

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        assertTrue(state.filteredTransactions.isEmpty())
        assertTrue(state.groupedTransactions.isEmpty())
    }

    @Test
    fun `search error should update error state`() = runTest {
        // Given
        val query = "ErrorQuery"
        val errorMessage = "Search service unavailable"
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(query, state.searchQuery)
        assertFalse(state.isSearching)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `search should update grouped transactions correctly`() = runTest {
        // Given
        val query = "Starbucks"
        val searchResults = listOf(createSearchableTransactions()[0], createSearchableTransactions()[1])
        
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(searchResults)

        // When
        viewModel.searchTransactions(query)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.groupedTransactions.isNotEmpty())
        
        // Verify grouping contains only search results
        val groupedTotal = state.groupedTransactions.values.sumOf { it.size }
        assertEquals(searchResults.size, groupedTotal)
        
        // Verify all grouped transactions match search results
        val allGroupedTransactions = state.groupedTransactions.values.flatten()
        assertTrue(allGroupedTransactions.all { transaction ->
            searchResults.any { it.id == transaction.id }
        })
    }

    @Test
    fun `search while loading should not interfere with loading state`() = runTest {
        // Given - ViewModel starts loading
        assertTrue(viewModel.uiState.value.isLoading)

        val query = "test"
        coEvery { 
            searchTransactionsUseCase(any(), query, any(), any())
        } returns Result.success(emptyList())

        // When - search while loading
        viewModel.searchTransactions(query)

        // Then - should set searching state
        val searchingState = viewModel.uiState.value
        assertTrue(searchingState.isSearching)
        assertEquals(query, searchingState.searchQuery)
    }

    @Test
    fun `multiple rapid searches should handle the latest query`() = runTest {
        // Given
        val query1 = "first"
        val query2 = "second"
        val query3 = "third"
        
        coEvery { 
            searchTransactionsUseCase(any(), any(), any(), any())
        } returns Result.success(emptyList())

        // When - fire multiple searches rapidly
        viewModel.searchTransactions(query1)
        viewModel.searchTransactions(query2)
        viewModel.searchTransactions(query3)
        advanceUntilIdle()

        // Then - should reflect the latest query
        val state = viewModel.uiState.value
        assertEquals(query3, state.searchQuery)
    }

    // Helper methods
    private fun createSearchableTransactions(): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            Transaction(
                id = "tx1",
                accountId = "test-account",
                amount = Money.fromDouble(-25.50, "SAR"),
                description = "Starbucks Coffee",
                date = today,
                type = TransactionType.DEBIT,
                category = "Food & Dining",
                merchant = Merchant(name = "Starbucks", category = "Food & Dining"),
                reference = "REFtx1",
                status = TransactionStatus.COMPLETED,
                metadata = mapOf("location" to "Riyadh Mall")
            ),
            Transaction(
                id = "tx2",
                accountId = "test-account",
                amount = Money.fromDouble(-15.75, "SAR"),
                description = "Local Coffee Shop",
                date = today.minus(kotlinx.datetime.DatePeriod(days = 1)),
                type = TransactionType.DEBIT,
                category = "Food & Dining",
                merchant = Merchant(name = "Bean There Coffee", category = "Food & Dining"),
                reference = "REFtx2",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            ),
            Transaction(
                id = "tx3",
                accountId = "test-account",
                amount = Money.fromDouble(-100.00, "SAR"),
                description = "Gas Station Fill-up",
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
                date = today.minus(kotlinx.datetime.DatePeriod(days = 2)),
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
                date = today.minus(kotlinx.datetime.DatePeriod(days = 3)),
                type = TransactionType.CREDIT,
                category = "Income",
                merchant = Merchant(name = "Tech Company Ltd", category = "Employer"),
                reference = "REFtx5",
                status = TransactionStatus.COMPLETED,
                metadata = mapOf("payroll_id" to "PAY123456")
            )
        )
    }

    private fun createArabicTransaction(): Transaction {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return Transaction(
            id = "tx_ar1",
            accountId = "test-account",
            amount = Money.fromDouble(-75.00, "SAR"),
            description = "مطعم الرياض",
            date = today,
            type = TransactionType.DEBIT,
            category = "Food & Dining",
            merchant = Merchant(name = "مطعم الرياض", category = "Food & Dining"),
            reference = "REFtx_ar1",
            status = TransactionStatus.COMPLETED,
            metadata = mapOf("location" to "الرياض")
        )
    }
}