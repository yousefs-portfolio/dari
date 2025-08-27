package code.yousef.dari.presentation.addtransaction

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.usecase.CategorizeTransactionUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SmartCategorizationTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase = mockk(relaxed = true)

    private lateinit var viewModel: AddTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        viewModel = AddTransactionViewModel(
            transactionRepository = transactionRepository,
            categorizeTransactionUseCase = categorizeTransactionUseCase,
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
    fun `updating merchant name should trigger category suggestion for known merchant`() = runTest {
        // Given
        val merchantName = "Starbucks"
        val expectedCategory = "Food & Dining"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(merchantName, state.merchantName)
        assertEquals(expectedCategory, state.category)
        
        coVerify { categorizeTransactionUseCase.suggestCategory(merchantName, any()) }
    }

    @Test
    fun `updating merchant name should not trigger suggestion for short names`() = runTest {
        // Given
        val shortMerchantName = "AB"
        
        // When
        viewModel.updateMerchantName(shortMerchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(shortMerchantName, state.merchantName)
        assertEquals("Other", state.category) // Should remain default
        
        // Verify no suggestion call was made
        coVerify(exactly = 0) { categorizeTransactionUseCase.suggestCategory(any(), any()) }
    }

    @Test
    fun `fast food merchant should be categorized as Food & Dining`() = runTest {
        // Given
        val merchantName = "McDonald's"
        val expectedCategory = "Food & Dining"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `gas station merchant should be categorized as Transportation`() = runTest {
        // Given
        val merchantName = "Shell Gas Station"
        val expectedCategory = "Transportation"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `online shopping merchant should be categorized as Shopping`() = runTest {
        // Given
        val merchantName = "Amazon"
        val expectedCategory = "Shopping"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `pharmacy merchant should be categorized as Healthcare`() = runTest {
        // Given
        val merchantName = "CVS Pharmacy"
        val expectedCategory = "Healthcare"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `utility company merchant should be categorized as Bills & Utilities`() = runTest {
        // Given
        val merchantName = "Saudi Electric Company"
        val expectedCategory = "Bills & Utilities"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `cinema merchant should be categorized as Entertainment`() = runTest {
        // Given
        val merchantName = "AMC Theaters"
        val expectedCategory = "Entertainment"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `supermarket merchant should be categorized as Groceries`() = runTest {
        // Given
        val merchantName = "Carrefour"
        val expectedCategory = "Groceries"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `category suggestion failure should not change category`() = runTest {
        // Given
        val merchantName = "Unknown Merchant"
        val errorMessage = "No category suggestion available"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(merchantName, state.merchantName)
        assertEquals("Other", state.category) // Should remain default
    }

    @Test
    fun `manual category change should not be overridden by suggestion`() = runTest {
        // Given
        val manualCategory = "Business"
        viewModel.updateCategory(manualCategory)
        
        val merchantName = "Starbucks"
        val suggestedCategory = "Food & Dining"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } returns Result.success(suggestedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(merchantName, state.merchantName)
        assertEquals(manualCategory, state.category) // Should keep manual category
    }

    @Test
    fun `category suggestion should work with Arabic merchant names`() = runTest {
        // Given
        val arabicMerchantName = "مطعم الرياض"
        val expectedCategory = "Food & Dining"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(arabicMerchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(arabicMerchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(arabicMerchantName, state.merchantName)
        assertEquals(expectedCategory, state.category)
    }

    @Test
    fun `applying category suggestion manually should update category`() = runTest {
        // Given
        val merchantName = "Target"
        val suggestedCategory = "Shopping"
        
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
    }

    @Test
    fun `category suggestion should use both merchant name and description`() = runTest {
        // Given
        val merchantName = "Local Store"
        val description = "Coffee and pastries"
        val expectedCategory = "Food & Dining"
        
        viewModel.updateDescription(description)
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, description)
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
        
        coVerify { categorizeTransactionUseCase.suggestCategory(merchantName, description) }
    }

    @Test
    fun `updating description should update merchant name if merchant is empty`() = runTest {
        // Given
        val description = "Starbucks Coffee Purchase"
        
        // When
        viewModel.updateDescription(description)

        // Then
        val state = viewModel.uiState.value
        assertEquals(description, state.description)
        assertEquals(description, state.merchantName) // Should auto-fill merchant name
    }

    @Test
    fun `updating description should not update merchant name if merchant is already set`() = runTest {
        // Given
        val existingMerchantName = "Existing Merchant"
        val description = "New Description"
        
        viewModel.updateMerchantName(existingMerchantName)

        // When
        viewModel.updateDescription(description)

        // Then
        val state = viewModel.uiState.value
        assertEquals(description, state.description)
        assertEquals(existingMerchantName, state.merchantName) // Should keep existing merchant
    }

    @Test
    fun `category suggestion should handle network errors gracefully`() = runTest {
        // Given
        val merchantName = "Network Error Merchant"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(merchantName, any())
        } throws Exception("Network connection failed")

        // When
        viewModel.updateMerchantName(merchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(merchantName, state.merchantName)
        assertEquals("Other", state.category) // Should remain default
        // Should not show error to user for categorization failures
        assertEquals(null, state.error)
    }

    @Test
    fun `category suggestion should work for compound merchant names`() = runTest {
        // Given
        val compoundMerchantName = "Starbucks Coffee Company Store #1234"
        val expectedCategory = "Food & Dining"
        
        coEvery { 
            categorizeTransactionUseCase.suggestCategory(compoundMerchantName, any())
        } returns Result.success(expectedCategory)

        // When
        viewModel.updateMerchantName(compoundMerchantName)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedCategory, state.category)
    }
}