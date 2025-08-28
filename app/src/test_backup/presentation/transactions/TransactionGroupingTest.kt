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
class TransactionGroupingTest {

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
        
        val transactionsAcrossMultipleDates = createTransactionsAcrossDates()
        every { getTransactionsUseCase(any()) } returns flowOf(transactionsAcrossMultipleDates)
        
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
    fun `transactions should be grouped by date correctly`() = runTest {
        // Given
        advanceUntilIdle() // Load transactions

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Should have multiple date groups
        assertTrue(groupedTransactions.size >= 3)
        
        // Verify grouping structure
        groupedTransactions.forEach { (dateKey, transactions) ->
            assertTrue(dateKey.isNotEmpty())
            assertTrue(transactions.isNotEmpty())
            
            // All transactions in the same group should have the same date
            val firstTransactionDate = transactions.first().date
            assertTrue(transactions.all { it.date == firstTransactionDate })
        }
    }

    @Test
    fun `transactions should be sorted by date descending within groups`() = runTest {
        // Given
        advanceUntilIdle() // Load transactions

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Verify each group is sorted by date descending
        groupedTransactions.values.forEach { transactions ->
            if (transactions.size > 1) {
                val sortedTransactions = transactions.sortedByDescending { it.date }
                assertEquals(sortedTransactions, transactions)
            }
        }
    }

    @Test
    fun `date groups should be ordered from newest to oldest`() = runTest {
        // Given
        advanceUntilIdle() // Load transactions

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Get the first transaction from each group (they should be ordered)
        val firstTransactionFromEachGroup = groupedTransactions.values.map { it.first() }
        val dates = firstTransactionFromEachGroup.map { it.date }
        
        // Dates should be in descending order (newest first)
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `same date transactions should be in same group`() = runTest {
        // Given
        val sameDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val transactionsOnSameDate = listOf(
            createTransactionOnDate("tx1", "Morning Coffee", -15.50, sameDate),
            createTransactionOnDate("tx2", "Lunch", -35.00, sameDate),
            createTransactionOnDate("tx3", "Evening Snack", -8.25, sameDate)
        )
        
        every { getTransactionsUseCase(any()) } returns flowOf(transactionsOnSameDate)

        // When - recreate viewModel to get fresh data
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
        
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Should have only one group
        assertEquals(1, groupedTransactions.size)
        
        // All three transactions should be in the same group
        val singleGroup = groupedTransactions.values.first()
        assertEquals(3, singleGroup.size)
        assertTrue(singleGroup.all { it.date == sameDate })
    }

    @Test
    fun `different dates should create separate groups`() = runTest {
        // Given
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(kotlinx.datetime.DatePeriod(days = 1))
        val dayBefore = today.minus(kotlinx.datetime.DatePeriod(days = 2))
        
        val transactionsOnDifferentDates = listOf(
            createTransactionOnDate("tx1", "Today Transaction", -15.50, today),
            createTransactionOnDate("tx2", "Yesterday Transaction", -35.00, yesterday),
            createTransactionOnDate("tx3", "Day Before Transaction", -8.25, dayBefore)
        )
        
        every { getTransactionsUseCase(any()) } returns flowOf(transactionsOnDifferentDates)

        // When
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
        
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Should have three separate groups
        assertEquals(3, groupedTransactions.size)
        
        // Each group should have exactly one transaction
        groupedTransactions.values.forEach { transactions ->
            assertEquals(1, transactions.size)
        }
    }

    @Test
    fun `date group keys should be human readable`() = runTest {
        // Given
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Verify date keys are human readable format: "Month Day, Year"
        groupedTransactions.keys.forEach { dateKey ->
            assertTrue(dateKey.matches(Regex("\\w+ \\d{1,2}, \\d{4}")))
            // Example: "January 15, 2024" or "December 3, 2024"
        }
    }

    @Test
    fun `empty transactions should result in empty groups`() = runTest {
        // Given
        every { getTransactionsUseCase(any()) } returns flowOf(emptyList())

        // When
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
        
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.groupedTransactions.isEmpty())
    }

    @Test
    fun `filtered transactions should maintain proper grouping`() = runTest {
        // Given
        advanceUntilIdle() // Load initial transactions
        
        // When - apply category filter
        viewModel.filterByCategory("Food & Dining")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Verify all grouped transactions match the filter
        groupedTransactions.values.flatten().forEach { transaction ->
            assertEquals("Food & Dining", transaction.category)
        }
        
        // Verify grouping integrity
        val totalFiltered = state.filteredTransactions.size
        val totalGrouped = groupedTransactions.values.sumOf { it.size }
        assertEquals(totalFiltered, totalGrouped)
    }

    @Test
    fun `search results should maintain proper grouping`() = runTest {
        // Given
        val searchResults = createTransactionsAcrossDates().filter { 
            it.description.contains("Coffee", ignoreCase = true)
        }
        
        coEvery { 
            searchTransactionsUseCase(any(), "Coffee", any(), any())
        } returns Result.success(searchResults)

        // When
        viewModel.searchTransactions("Coffee")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        val groupedTransactions = state.groupedTransactions
        
        // Verify grouping of search results
        val totalSearchResults = state.filteredTransactions.size
        val totalGrouped = groupedTransactions.values.sumOf { it.size }
        assertEquals(totalSearchResults, totalGrouped)
        
        // Verify all grouped transactions are from search results
        val allGroupedIds = groupedTransactions.values.flatten().map { it.id }.toSet()
        val searchResultIds = searchResults.map { it.id }.toSet()
        assertEquals(searchResultIds, allGroupedIds)
    }

    // Helper methods
    private fun createTransactionsAcrossDates(): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(kotlinx.datetime.DatePeriod(days = 1))
        val dayBefore = today.minus(kotlinx.datetime.DatePeriod(days = 2))
        val threeDaysAgo = today.minus(kotlinx.datetime.DatePeriod(days = 3))

        return listOf(
            // Today's transactions
            createTransactionOnDate("tx1", "Morning Coffee", -15.50, today),
            createTransactionOnDate("tx2", "Lunch", -35.00, today),
            
            // Yesterday's transactions
            createTransactionOnDate("tx3", "Gas Station", -100.00, yesterday),
            createTransactionOnDate("tx4", "Evening Coffee", -12.25, yesterday),
            createTransactionOnDate("tx5", "Groceries", -75.50, yesterday),
            
            // Day before transactions
            createTransactionOnDate("tx6", "Dinner", -45.00, dayBefore),
            
            // Three days ago
            createTransactionOnDate("tx7", "Salary Deposit", 5000.00, threeDaysAgo),
            createTransactionOnDate("tx8", "Online Shopping", -200.00, threeDaysAgo)
        )
    }

    private fun createTransactionOnDate(
        id: String, 
        description: String, 
        amount: Double, 
        date: LocalDate
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "test-account",
            amount = Money.fromDouble(amount, "SAR"),
            description = description,
            date = date,
            type = if (amount < 0) TransactionType.DEBIT else TransactionType.CREDIT,
            category = when {
                description.contains("Coffee", ignoreCase = true) -> "Food & Dining"
                description.contains("Gas", ignoreCase = true) -> "Transportation"
                description.contains("Groceries", ignoreCase = true) -> "Groceries"
                description.contains("Salary", ignoreCase = true) -> "Income"
                description.contains("Shopping", ignoreCase = true) -> "Shopping"
                else -> "Other"
            },
            merchant = Merchant(
                name = description,
                category = "Merchant"
            ),
            reference = "REF$id",
            status = TransactionStatus.COMPLETED,
            metadata = emptyMap()
        )
    }
}