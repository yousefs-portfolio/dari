package code.yousef.dari.presentation.addtransaction

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.CategorizeTransactionUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
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
class ManualEntryValidationTest {

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
    fun `empty description should fail validation`() {
        // Given
        viewModel.updateDescription("")
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Description cannot be empty", validation.errorMessage)
    }

    @Test
    fun `whitespace only description should fail validation`() {
        // Given
        viewModel.updateDescription("   ")
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Description cannot be empty", validation.errorMessage)
    }

    @Test
    fun `zero amount should fail validation`() {
        // Given
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
    fun `negative amount should fail validation`() {
        // Given
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
    fun `very small positive amount should pass validation`() {
        // Given
        viewModel.updateDescription("Small transaction")
        viewModel.updateAmount(0.01)
        viewModel.updateCategory("Other")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `very large amount should pass validation`() {
        // Given
        viewModel.updateDescription("Large transaction")
        viewModel.updateAmount(1000000.0)
        viewModel.updateCategory("Other")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `empty category should fail validation`() {
        // Given
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
    fun `whitespace only category should fail validation`() {
        // Given
        viewModel.updateDescription("Test transaction")
        viewModel.updateAmount(25.0)
        viewModel.updateCategory("   ")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Category cannot be empty", validation.errorMessage)
    }

    @Test
    fun `valid basic transaction should pass validation`() {
        // Given
        viewModel.updateDescription("Coffee purchase")
        viewModel.updateAmount(25.50)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `transaction with merchant name should pass validation`() {
        // Given
        viewModel.updateDescription("Coffee purchase")
        viewModel.updateAmount(25.50)
        viewModel.updateCategory("Food & Dining")
        viewModel.updateMerchantName("Starbucks")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `transaction with past date should pass validation`() {
        // Given
        val pastDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minus(kotlinx.datetime.DatePeriod(days = 7))
        
        viewModel.updateDescription("Past transaction")
        viewModel.updateAmount(100.0)
        viewModel.updateCategory("Shopping")
        viewModel.updateDate(pastDate)

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `transaction with future date should pass validation`() {
        // Given
        val futureDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .plus(kotlinx.datetime.DatePeriod(days = 7))
        
        viewModel.updateDescription("Future transaction")
        viewModel.updateAmount(100.0)
        viewModel.updateCategory("Bills & Utilities")
        viewModel.updateDate(futureDate)

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `recurring transaction should pass validation`() {
        // Given
        viewModel.updateDescription("Monthly subscription")
        viewModel.updateAmount(29.99)
        viewModel.updateCategory("Entertainment")
        viewModel.toggleRecurring()

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `split transaction with equal amounts should pass validation`() {
        // Given
        viewModel.updateDescription("Split meal")
        viewModel.updateAmount(100.0)
        viewModel.updateCategory("Food & Dining")
        viewModel.toggleSplitTransaction()
        
        // Clear default splits and add custom ones that equal total
        viewModel.removeSplitAmount(0)
        viewModel.removeSplitAmount(0)
        viewModel.addSplitAmount(SplitAmount("Food", Money.fromDouble(60.0, "SAR")))
        viewModel.addSplitAmount(SplitAmount("Drinks", Money.fromDouble(40.0, "SAR")))

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `split transaction with unequal amounts should fail validation`() {
        // Given
        viewModel.updateDescription("Split meal")
        viewModel.updateAmount(100.0)
        viewModel.updateCategory("Food & Dining")
        viewModel.toggleSplitTransaction()
        
        // Clear default splits and add custom ones that don't equal total
        viewModel.removeSplitAmount(0)
        viewModel.removeSplitAmount(0)
        viewModel.addSplitAmount(SplitAmount("Food", Money.fromDouble(60.0, "SAR")))
        viewModel.addSplitAmount(SplitAmount("Drinks", Money.fromDouble(30.0, "SAR")))
        // Total: 90.0, but main amount is 100.0

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("Split amounts must equal total amount", validation.errorMessage)
    }

    @Test
    fun `split transaction with empty split description should fail validation`() {
        // Given
        viewModel.updateDescription("Split meal")
        viewModel.updateAmount(100.0)
        viewModel.updateCategory("Food & Dining")
        viewModel.toggleSplitTransaction()
        
        // Clear default splits and add one with empty description
        viewModel.removeSplitAmount(0)
        viewModel.removeSplitAmount(0)
        viewModel.addSplitAmount(SplitAmount("", Money.fromDouble(60.0, "SAR")))
        viewModel.addSplitAmount(SplitAmount("Drinks", Money.fromDouble(40.0, "SAR")))

        // When
        val validation = viewModel.validateInput()

        // Then
        assertFalse(validation.isValid)
        assertEquals("All split descriptions must be filled", validation.errorMessage)
    }

    @Test
    fun `validation should handle floating point precision for split amounts`() {
        // Given - amounts that might have floating point precision issues
        viewModel.updateDescription("Precise split")
        viewModel.updateAmount(10.0)
        viewModel.updateCategory("Food & Dining")
        viewModel.toggleSplitTransaction()
        
        // Clear default splits
        viewModel.removeSplitAmount(0)
        viewModel.removeSplitAmount(0)
        
        // Add splits that should equal 10.0 but might have precision issues
        viewModel.addSplitAmount(SplitAmount("Split 1", Money.fromDouble(3.33, "SAR")))
        viewModel.addSplitAmount(SplitAmount("Split 2", Money.fromDouble(3.33, "SAR")))
        viewModel.addSplitAmount(SplitAmount("Split 3", Money.fromDouble(3.34, "SAR")))
        // Total: 9.99999... should be close enough to 10.0

        // When
        val validation = viewModel.validateInput()

        // Then - should pass due to tolerance in validation (0.01)
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `arabic text in description should pass validation`() {
        // Given
        viewModel.updateDescription("مطعم الرياض")
        viewModel.updateAmount(75.0)
        viewModel.updateCategory("Food & Dining")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `special characters in description should pass validation`() {
        // Given
        viewModel.updateDescription("AT&T Bill - Account #123456")
        viewModel.updateAmount(99.99)
        viewModel.updateCategory("Bills & Utilities")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `very long description should pass validation`() {
        // Given
        val longDescription = "This is a very long transaction description that might be used " +
                "for detailed record keeping and should still pass validation even though " +
                "it contains many words and characters and might be longer than typical descriptions"
        
        viewModel.updateDescription(longDescription)
        viewModel.updateAmount(50.0)
        viewModel.updateCategory("Other")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `credit transaction should pass validation with positive amount`() {
        // Given
        viewModel.updateDescription("Salary deposit")
        viewModel.updateAmount(5000.0)
        viewModel.updateType(TransactionType.CREDIT)
        viewModel.updateCategory("Income")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }

    @Test
    fun `transaction with receipt URLs should pass validation`() {
        // Given
        viewModel.updateDescription("Business expense")
        viewModel.updateAmount(150.0)
        viewModel.updateCategory("Business")
        viewModel.addReceiptUrl("https://example.com/receipt1.jpg")
        viewModel.addReceiptUrl("https://example.com/receipt2.jpg")

        // When
        val validation = viewModel.validateInput()

        // Then
        assertTrue(validation.isValid)
        assertNull(validation.errorMessage)
    }
}