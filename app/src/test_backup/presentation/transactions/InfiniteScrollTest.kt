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
class InfiniteScrollTest {

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
        
        // Setup initial page of transactions
        val initialTransactions = createTransactionPage(0, 50)
        every { getTransactionsUseCase(any()) } returns flowOf(initialTransactions)
        
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
    fun `loadMoreTransactions should append new transactions to existing list`() = runTest {
        // Given - initial transactions are loaded
        advanceUntilIdle()
        val initialState = viewModel.uiState.value
        val initialCount = initialState.transactions.size
        assertEquals(0, initialState.currentPage)
        
        // Setup next page data
        val nextPageTransactions = createTransactionPage(1, 50)
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns nextPageTransactions

        // When
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(1, finalState.currentPage)
        assertEquals(initialCount + nextPageTransactions.size, finalState.transactions.size)
        assertFalse(finalState.isLoadingMore)
        assertFalse(finalState.hasReachedEnd)
    }

    @Test
    fun `loadMoreTransactions should set loading state correctly`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
        
        val nextPageTransactions = createTransactionPage(1, 50)
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns nextPageTransactions

        // When - start loading more
        viewModel.loadMoreTransactions()

        // Then - should show loading state immediately
        val loadingState = viewModel.uiState.value
        assertTrue(loadingState.isLoadingMore)

        // Wait for completion
        advanceUntilIdle()

        // Then - should clear loading state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoadingMore)
    }

    @Test
    fun `loadMoreTransactions should not load if already loading`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
        
        val nextPageTransactions = createTransactionPage(1, 50)
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } coAnswers {
            // Simulate slow network
            kotlinx.coroutines.delay(1000)
            nextPageTransactions
        }

        // When - trigger multiple load requests rapidly
        viewModel.loadMoreTransactions()
        viewModel.loadMoreTransactions() // Should be ignored
        viewModel.loadMoreTransactions() // Should be ignored

        // Then - only one call should be made
        advanceTimeBy(1100)
        advanceUntilIdle()
        
        coVerify(exactly = 1) { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        }
    }

    @Test
    fun `loadMoreTransactions should not load if reached end`() = runTest {
        // Given - set has reached end state
        advanceUntilIdle()
        
        // Simulate reaching end by returning empty list
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns emptyList()
        
        viewModel.loadMoreTransactions()
        advanceUntilIdle()
        
        val endReachedState = viewModel.uiState.value
        assertTrue(endReachedState.hasReachedEnd)

        // When - try to load more after reaching end
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then - no additional API call should be made
        coVerify(exactly = 1) { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        }
    }

    @Test
    fun `empty page should set hasReachedEnd flag`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
        
        // Setup empty page response
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns emptyList()

        // When
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.hasReachedEnd)
        assertFalse(finalState.isLoadingMore)
        assertEquals(0, finalState.currentPage) // Should not increment page
    }

    @Test
    fun `partial page should still increment page and allow further loading`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
        
        // Setup partial page (less than full page size)
        val partialPageTransactions = createTransactionPage(1, 25) // Only 25 instead of 50
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns partialPageTransactions

        // When
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertEquals(1, finalState.currentPage)
        assertFalse(finalState.hasReachedEnd) // Should still allow more loading
        assertEquals(75, finalState.transactions.size) // 50 initial + 25 new
    }

    @Test
    fun `loadMoreTransactions with error should handle gracefully`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
        
        val errorMessage = "Network error"
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } throws Exception(errorMessage)

        // When
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoadingMore)
        assertEquals(errorMessage, finalState.error)
        assertEquals(0, finalState.currentPage) // Page should not increment on error
    }

    @Test
    fun `loadMoreTransactions should update filtered transactions and grouping`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data
        
        // Apply a category filter first
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()
        
        val stateWithFilter = viewModel.uiState.value
        val initialFilteredCount = stateWithFilter.filteredTransactions.size
        
        // Setup next page with more food transactions
        val nextPageTransactions = createTransactionPage(1, 50).map { transaction ->
            if (transaction.id.endsWith("1") || transaction.id.endsWith("2")) {
                transaction.copy(category = "Food & Dining")
            } else {
                transaction
            }
        }
        
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns nextPageTransactions

        // When
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState.filteredTransactions.size >= initialFilteredCount)
        assertTrue(finalState.groupedTransactions.isNotEmpty())
        
        // Verify all filtered transactions still match the category
        assertTrue(finalState.filteredTransactions.all { it.category == "Food & Dining" })
    }

    @Test
    fun `pagination parameters should be calculated correctly`() = runTest {
        // Given
        advanceUntilIdle() // Load initial data (page 0)
        
        val page1Transactions = createTransactionPage(1, 50)
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50)
        } returns page1Transactions

        // When - load page 1
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then - verify correct pagination parameters for page 1
        coVerify { 
            getTransactionsUseCase.getPaginated("test-account", 50, 50) // offset = page * limit = 1 * 50
        }
        
        // Setup for page 2
        val page2Transactions = createTransactionPage(2, 50)
        coEvery { 
            getTransactionsUseCase.getPaginated("test-account", 50, 100)
        } returns page2Transactions

        // When - load page 2
        viewModel.loadMoreTransactions()
        advanceUntilIdle()

        // Then - verify correct pagination parameters for page 2
        coVerify { 
            getTransactionsUseCase.getPaginated("test-account", 50, 100) // offset = page * limit = 2 * 50
        }
    }

    // Helper method to create a page of transactions
    private fun createTransactionPage(page: Int, size: Int): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return (0 until size).map { index ->
            val transactionId = "tx_page${page}_$index"
            Transaction(
                id = transactionId,
                accountId = "test-account",
                amount = Money.fromDouble(-(10.0 + index * 5), "SAR"),
                description = "Transaction $page-$index",
                date = today.minus(kotlinx.datetime.DatePeriod(days = page * size + index)),
                type = TransactionType.DEBIT,
                category = when (index % 4) {
                    0 -> "Food & Dining"
                    1 -> "Transportation"
                    2 -> "Shopping"
                    else -> "Other"
                },
                merchant = Merchant(
                    name = "Merchant $page-$index",
                    category = "Merchant"
                ),
                reference = "REF$transactionId",
                status = TransactionStatus.COMPLETED,
                metadata = emptyMap()
            )
        }
    }
}