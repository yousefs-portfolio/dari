package code.yousef.dari.shared.domain.usecase.categorization

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.model.TransactionType
import code.yousef.dari.shared.domain.repository.CategoryRepository
import code.yousef.dari.shared.testutil.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AutoCategorizationRulesUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val categoryRepository = mockk<CategoryRepository>()
    private lateinit var autoCategorizationRulesUseCase: AutoCategorizationRulesUseCase

    @BeforeTest
    fun setup() {
        autoCategorizationRulesUseCase = AutoCategorizationRulesUseCase(categoryRepository)
    }

    @Test
    fun `categorize grocery transaction by merchant name`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "PANDA HYPERMARKET",
            amount = Money(BigDecimal("-150.50"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Groceries") } returns
            createCategory("cat_groceries", "Groceries")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_groceries", result?.categoryId)
        assertEquals("Groceries", result?.categoryName)
        assertEquals(0.95, result?.confidence)
        assertTrue(result?.rules?.contains("merchant_keyword_match") == true)
    }

    @Test
    fun `categorize gas station transaction`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "ARAMCO Gas Station",
            amount = Money(BigDecimal("-80.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Transportation") } returns
            createCategory("cat_transport", "Transportation")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_transport", result?.categoryId)
        assertEquals("Transportation", result?.categoryName)
        assertTrue(result?.confidence!! > 0.9)
    }

    @Test
    fun `categorize restaurant transaction by description`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "McDonald's Restaurant",
            description = "Fast food dinner",
            amount = Money(BigDecimal("-45.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Dining") } returns
            createCategory("cat_dining", "Dining")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_dining", result?.categoryId)
        assertEquals("Dining", result?.categoryName)
    }

    @Test
    fun `categorize ATM cash withdrawal`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "NCB ATM",
            description = "Cash withdrawal",
            amount = Money(BigDecimal("-500.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Cash & ATM") } returns
            createCategory("cat_cash", "Cash & ATM")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_cash", result?.categoryId)
        assertEquals("Cash & ATM", result?.categoryName)
    }

    @Test
    fun `categorize salary deposit`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "ACME CORP PAYROLL",
            description = "Monthly salary",
            amount = Money(BigDecimal("8000.00"), "SAR"),
            type = TransactionType.CREDIT
        )

        coEvery { categoryRepository.getCategoryByName("Salary") } returns
            createCategory("cat_salary", "Salary")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_salary", result?.categoryId)
        assertEquals("Salary", result?.categoryName)
        assertTrue(result?.confidence!! > 0.9)
    }

    @Test
    fun `categorize subscription service`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "NETFLIX.COM",
            description = "Monthly subscription",
            amount = Money(BigDecimal("-56.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Entertainment") } returns
            createCategory("cat_entertainment", "Entertainment")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_entertainment", result?.categoryId)
        assertTrue(result?.rules?.contains("subscription_pattern") == true)
    }

    @Test
    fun `categorize pharmacy transaction`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "Al Dawaa Pharmacy",
            amount = Money(BigDecimal("-125.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Healthcare") } returns
            createCategory("cat_healthcare", "Healthcare")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_healthcare", result?.categoryId)
        assertEquals("Healthcare", result?.categoryName)
    }

    @Test
    fun `categorize utility bill payment`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "SEC - Saudi Electricity",
            description = "Electricity bill payment",
            amount = Money(BigDecimal("-280.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Utilities") } returns
            createCategory("cat_utilities", "Utilities")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_utilities", result?.categoryId)
        assertEquals("Utilities", result?.categoryName)
        assertTrue(result?.confidence!! > 0.9)
    }

    @Test
    fun `return null for unknown transaction type`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "Unknown Merchant XYZ",
            description = "Unknown transaction",
            amount = Money(BigDecimal("-100.00"), "SAR")
        )

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `prioritize multiple matching rules`() = runTest {
        // Given - A transaction that could match multiple categories
        val transaction = createTransaction(
            merchantName = "Starbucks Coffee Shop", // Could be dining or coffee
            description = "Coffee and food",
            amount = Money(BigDecimal("-35.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Dining") } returns
            createCategory("cat_dining", "Dining")
        coEvery { categoryRepository.getCategoryByName("Coffee") } returns
            createCategory("cat_coffee", "Coffee")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_dining", result?.categoryId) // Dining should have higher priority
        assertTrue(result?.confidence!! > 0.8)
    }

    @Test
    fun `handle special Saudi merchants`() = runTest {
        // Given
        val transaction = createTransaction(
            merchantName = "Jarir Bookstore",
            amount = Money(BigDecimal("-89.50"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Shopping") } returns
            createCategory("cat_shopping", "Shopping")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_shopping", result?.categoryId)
        assertTrue(result?.rules?.contains("saudi_merchant_match") == true)
    }

    @Test
    fun `categorize based on amount patterns`() = runTest {
        // Given - Large round amount typically indicates rent or major expense
        val transaction = createTransaction(
            merchantName = "Property Management Co",
            description = "Monthly rent",
            amount = Money(BigDecimal("-3000.00"), "SAR")
        )

        coEvery { categoryRepository.getCategoryByName("Housing") } returns
            createCategory("cat_housing", "Housing")

        // When
        val result = autoCategorizationRulesUseCase.categorize(transaction)

        // Then
        assertEquals("cat_housing", result?.categoryId)
        assertTrue(result?.rules?.contains("amount_pattern") == true)
    }

    private fun createTransaction(
        merchantName: String,
        description: String = merchantName,
        amount: Money,
        type: TransactionType = if (amount.amount >= BigDecimal.ZERO) TransactionType.CREDIT else TransactionType.DEBIT
    ) = Transaction(
        id = "txn_${(1000..9999).random()}",
        accountId = "account1",
        amount = amount,
        type = type,
        description = description,
        merchantName = merchantName,
        date = Clock.System.now(),
        categoryId = "uncategorized"
    )

    private fun createCategory(id: String, name: String) =
        code.yousef.dari.shared.domain.model.Category(
            id = id,
            name = name,
            icon = "default",
            color = "#000000",
            parentId = null
        )
}
