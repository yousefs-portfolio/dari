package code.yousef.dari.presentation.addtransaction

import androidx.lifecycle.SavedStateHandle
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)

    private lateinit var viewModel: AddTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        every { savedStateHandle.get<String>("accountId") } returns "test-account-id"
        
        viewModel = AddTransactionViewModel(
            transactionRepository = transactionRepository,
            categorizeTransactionUseCase = categorizeTransactionUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() {
        // Then
        val initialState = viewModel.uiState.value
        
        assertEquals("", initialState.description)
        assertEquals(Money.fromDouble(0.0, "SAR"), initialState.amount)
        assertEquals(TransactionType.DEBIT, initialState.type)
        assertEquals("Other", initialState.category)
        assertEquals(Clock.System.todayIn(TimeZone.currentSystemDefault()), initialState.date)
        assertEquals("", initialState.merchantName)
        assertFalse(initialState.isRecurring)
        assertFalse(initialState.isSplitTransaction)
        assertTrue(initialState.splitAmounts.isEmpty())
        assertNull(initialState.error)
        assertFalse(initialState.isSaving)
    }

    @Test
    fun `updateDescription should update description in state`() {
        // Given
        val description = "Starbucks Coffee"

        // When
        viewModel.updateDescription(description)

        // Then
        val state = viewModel.uiState.value
        assertEquals(description, state.description)
    }

    @Test
    fun `updateAmount should update amount in state`() {
        // Given
        val amount = 25.50

        // When
        viewModel.updateAmount(amount)

        // Then
        val state = viewModel.uiState.value
        assertEquals(Money.fromDouble(amount, "SAR"), state.amount)
    }

    @Test
    fun `updateType should update transaction type in state`() {
        // Given
        val type = TransactionType.CREDIT

        // When
        viewModel.updateType(type)

        // Then
        val state = viewModel.uiState.value
        assertEquals(type, state.type)
    }

    @Test
    fun `updateCategory should update category in state`() {
        // Given
        val category = "Food & Dining"

        // When
        viewModel.updateCategory(category)

        // Then
        val state = viewModel.uiState.value
        assertEquals(category, state.category)
    }

    @Test
    fun `updateDate should update date in state`() {
        // Given
        val newDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minus(kotlinx.datetime.DatePeriod(days = 1))

        // When
        viewModel.updateDate(newDate)

        // Then
        val state = viewModel.uiState.value
        assertEquals(newDate, state.date)
    }

    @Test
    fun `updateMerchantName should update merchant name in state`() {
        // Given
        val merchantName = "Starbucks"

        // When
        viewModel.updateMerchantName(merchantName)

        // Then
        val state = viewModel.uiState.value
        assertEquals(merchantName, state.merchantName)
    }

    @Test
    fun `toggleRecurring should toggle recurring transaction state`() {
        // Given
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isRecurring)

        // When
        viewModel.toggleRecurring()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isRecurring)

        // When - toggle again
        viewModel.toggleRecurring()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isRecurring)
    }

    @Test
    fun `toggleSplitTransaction should toggle split transaction state`() {
        // Given
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isSplitTransaction)

        // When
        viewModel.toggleSplitTransaction()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isSplitTransaction)
        assertTrue(state.splitAmounts.isNotEmpty()) // Should initialize with default splits

        // When - toggle off
        viewModel.toggleSplitTransaction()

        // Then
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isSplitTransaction)
        assertTrue(finalState.splitAmounts.isEmpty())
    }

    @Test
    fun `addSplitAmount should add new split amount`() {
        // Given
        viewModel.toggleSplitTransaction() // Enable split mode first
        val splitAmount = SplitAmount("Food", Money.fromDouble(15.0, "SAR"))

        // When
        viewModel.addSplitAmount(splitAmount)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.splitAmounts.contains(splitAmount))
    }

    @Test
    fun `removeSplitAmount should remove split amount by index`() {
        // Given
        viewModel.toggleSplitTransaction() // Enable split mode first
        val splitAmount1 = SplitAmount("Food", Money.fromDouble(15.0, "SAR"))
        val splitAmount2 = SplitAmount("Coffee", Money.fromDouble(10.0, "SAR"))
        
        viewModel.addSplitAmount(splitAmount1)
        viewModel.addSplitAmount(splitAmount2)

        // When
        viewModel.removeSplitAmount(0) // Remove first split

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.splitAmounts.contains(splitAmount1))
        assertTrue(state.splitAmounts.contains(splitAmount2))
    }

    @Test
    fun `validateInput should return error for empty description`() {
        // Given - empty description
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Description cannot be empty", validation.errorMessage)
    }

    @Test
    fun `validateInput should return error for zero amount`() {
        // Given - zero amount
        viewModel.updateDescription("Test transaction")
        viewModel.updateAmount(0.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Amount must be greater than zero", validation.errorMessage)
    }

    @Test
    fun `validateInput should return error for negative amount`() {
        // Given - negative amount
        viewModel.updateDescription("Test transaction")
        viewModel.updateAmount(-25.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Amount must be greater than zero", validation.errorMessage)
    }

    @Test
    fun `validateInput should return error for empty category`() {
        // Given - empty category
        viewModel.updateDescription("Test transaction")
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Category cannot be empty", validation.errorMessage)
    }

    @Test
    fun `validateInput should return success for valid input`() {
        // Given - valid input
        viewModel.updateDescription("Test transaction")
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `validateInput should validate split amounts when split transaction is enabled`() {
        // Given - split transaction with unequal amounts
        viewModel.updateDescription("Split meal")
        viewModel.updateAmount(100.0)
        viewModel.updateCategory("Food & Dining")
        viewModel.toggleSplitTransaction()
        
        viewModel.addSplitAmount(SplitAmount("Food", Money.fromDouble(30.0, "SAR")))
        viewModel.addSplitAmount(SplitAmount("Drinks", Money.fromDouble(40.0, "SAR")))
        // Total split amounts = 70.0, but main amount = 100.0

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Split amounts must equal total amount", validation.errorMessage)
    }

    @Test
    fun `saveTransaction should create transaction successfully`() = runTest {
        // Given - valid input
        viewModel.updateDescription("Coffee purchase")
        viewModel.updateAmount(25.50)
        viewModel.updateType(TransactionType.DEBIT)
        viewModel.updateCategory("Food & Dining")
        viewModel.updateMerchantName("Starbucks")
        
        val expectedTransaction = Transaction(
            id = "generated-id",
            accountId = "test-account-id",
            amount = Money.fromDouble(-25.50, "SAR"), // Negative for debit
            description = "Coffee purchase",
            date = viewModel.uiState.value.date,
            type = TransactionType.DEBIT,
            category = "Food & Dining",
            merchant = Merchant(name = "Starbucks", category = "Food & Dining"),
            reference = "manual-entry",
            status = TransactionStatus.COMPLETED,
            metadata = emptyMap()
        )

        coEvery { transactionRepository.createTransaction(any()) } returns Result.success(expectedTransaction)

        // When
        viewModel.saveTransaction()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.error)
        
        coVerify {
            transactionRepository.createTransaction(
                match { transaction ->
                    transaction.description == "Coffee purchase" &&
                    transaction.amount.amount == -25.50 &&
                    transaction.type == TransactionType.DEBIT &&
                    transaction.category == "Food & Dining" &&
                    transaction.merchant?.name == "Starbucks"
                }
            )
        }
    }

    @Test
    fun `saveTransaction should handle repository error`() = runTest {
        // Given - valid input but repository error
        viewModel.updateDescription("Coffee purchase")
        viewModel.updateAmount(25.50)
        
        val errorMessage = "Network error"
        coEvery { transactionRepository.createTransaction(any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.saveTransaction()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `saveTransaction should not save if validation fails`() = runTest {
        // Given - invalid input (empty description)
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("Food & Dining")

        // When
        viewModel.saveTransaction()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Description cannot be empty", state.error)
        
        // Verify repository was not called
        coVerify(exactly = 0) { transactionRepository.createTransaction(any()) }
    }

    @Test
    fun `smartCategorization should suggest category based on merchant`() = runTest {
        // Given
        val merchantName = "Starbucks"
        val suggestedCategory = "Food & Dining"
        
        viewModel.updateMerchantName(merchantName)
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(suggestedCategory)

        // When
        viewModel.applyCategorySuggestion()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(suggestedCategory, state.category)
        
        coVerify { categorizeTransactionUseCase.suggestCategory(merchantName, any()) }
    }

    @Test
    fun `smartCategorization should handle suggestion failure gracefully`() = runTest {
        // Given
        val merchantName = "Unknown Merchant"
        
        viewModel.updateMerchantName(merchantName)
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.failure(Exception("No suggestion available"))

        // When
        viewModel.applyCategorySuggestion()
        advanceUntilIdle()

        // Then - category should remain unchanged
        val state = viewModel.uiState.value
        assertEquals("Other", state.category) // Default category
    }

    @Test
    fun `credit transaction should have positive amount`() = runTest {
        // Given
        viewModel.updateDescription("Salary")
        viewModel.updateAmount(5000.0)
        viewModel.updateType(TransactionType.CREDIT)
        viewModel.updateCategory("Income")
        
        val expectedTransaction = Transaction(
            id = "generated-id",
            accountId = "test-account-id",
            amount = Money.fromDouble(5000.0, "SAR"), // Positive for credit
            description = "Salary",
            date = viewModel.uiState.value.date,
            type = TransactionType.CREDIT,
            category = "Income",
            merchant = null,
            reference = "manual-entry",
            status = TransactionStatus.COMPLETED,
            metadata = emptyMap()
        )

        coEvery { transactionRepository.createTransaction(any()) } returns Result.success(expectedTransaction)

        // When
        viewModel.saveTransaction()
        advanceUntilIdle()

        // Then
        coVerify {
            transactionRepository.createTransaction(
                match { transaction ->
                    transaction.amount.amount == 5000.0 && // Positive for credit
                    transaction.type == TransactionType.CREDIT
                }
            )
        }
    }

    @Test
    fun `clearForm should reset all fields to default values`() {
        // Given - set some values
        viewModel.updateDescription("Test transaction")
        viewModel.updateAmount(100.0)
        viewModel.updateType(TransactionType.CREDIT)
        viewModel.updateCategory("Food & Dining")
        viewModel.updateMerchantName("Test Merchant")
        viewModel.toggleRecurring()
        viewModel.toggleSplitTransaction()

        // When
        viewModel.clearForm()

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.description)
        assertEquals(Money.fromDouble(0.0, "SAR"), state.amount)
        assertEquals(TransactionType.DEBIT, state.type)
        assertEquals("Other", state.category)
        assertEquals("", state.merchantName)
        assertFalse(state.isRecurring)
        assertFalse(state.isSplitTransaction)
        assertTrue(state.splitAmounts.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `dismissError should clear error state`() {
        // Given - set error state
        viewModel.updateDescription("") // This will cause validation error
        viewModel.saveTransaction()
        runTest { advanceUntilIdle() }
        
        val stateWithError = viewModel.uiState.value
        assertEquals("Description cannot be empty", stateWithError.error)

        // When
        viewModel.dismissError()

        // Then
        val finalState = viewModel.uiState.value
        assertNull(finalState.error)
    }
}