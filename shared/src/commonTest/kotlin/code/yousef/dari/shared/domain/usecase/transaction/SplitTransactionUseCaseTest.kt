package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplitTransactionUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val splitTransactionUseCase = SplitTransactionUseCase(transactionRepository)

    private val originalTransaction = Transaction(
        id = "tx1",
        accountId = "acc1",
        amount = Money(300.0, Currency.SAR),
        type = TransactionType.EXPENSE,
        category = TransactionCategory.SHOPPING,
        description = "Grocery shopping",
        date = LocalDateTime(2024, 1, 15, 12, 30),
        merchant = Merchant("merchant1", "Carrefour", "Supermarket"),
        location = TransactionLocation(24.7136, 46.6753, "Riyadh, Saudi Arabia"),
        isRecurring = false,
        tags = listOf("grocery", "weekly"),
        paymentMethod = PaymentMethod.CARD,
        status = TransactionStatus.COMPLETED
    )

    @Test
    fun `should split transaction into multiple parts successfully`() = runTest {
        // Given
        val splitParts = listOf(
            TransactionSplit(
                amount = Money(150.0, Currency.SAR),
                category = TransactionCategory.FOOD_DINING,
                description = "Food items",
                tags = listOf("food", "grocery")
            ),
            TransactionSplit(
                amount = Money(80.0, Currency.SAR),
                category = TransactionCategory.PERSONAL_CARE,
                description = "Personal care items",
                tags = listOf("health", "grocery")
            ),
            TransactionSplit(
                amount = Money(70.0, Currency.SAR),
                category = TransactionCategory.HOUSEHOLD,
                description = "Household items",
                tags = listOf("home", "cleaning")
            )
        )

        val request = SplitTransactionRequest(
            originalTransactionId = "tx1",
            splits = splitParts,
            keepOriginal = false
        )

        val expectedSplitTransactions = splitParts.mapIndexed { index, split ->
            originalTransaction.copy(
                id = "tx1_split_${index + 1}",
                amount = split.amount,
                category = split.category,
                description = split.description,
                tags = split.tags,
                parentTransactionId = "tx1"
            )
        }

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)
        coEvery { transactionRepository.createTransactions(any()) } returns Result.Success(expectedSplitTransactions)
        coEvery { transactionRepository.deleteTransaction("tx1") } returns Result.Success(Unit)

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val splitResult = result.data
        assertEquals(3, splitResult.splitTransactions.size)
        assertEquals(300.0, splitResult.splitTransactions.sumOf { it.amount.value })
        
        coVerify {
            transactionRepository.getTransactionById("tx1")
            transactionRepository.createTransactions(any())
            transactionRepository.deleteTransaction("tx1")
        }
    }

    @Test
    fun `should split transaction while keeping original`() = runTest {
        // Given
        val splitParts = listOf(
            TransactionSplit(
                amount = Money(200.0, Currency.SAR),
                category = TransactionCategory.FOOD_DINING,
                description = "Food portion",
                tags = listOf("food")
            ),
            TransactionSplit(
                amount = Money(100.0, Currency.SAR),
                category = TransactionCategory.HOUSEHOLD,
                description = "Household portion",
                tags = listOf("home")
            )
        )

        val request = SplitTransactionRequest(
            originalTransactionId = "tx1",
            splits = splitParts,
            keepOriginal = true
        )

        val expectedSplitTransactions = splitParts.mapIndexed { index, split ->
            originalTransaction.copy(
                id = "tx1_split_${index + 1}",
                amount = split.amount,
                category = split.category,
                description = split.description,
                tags = split.tags,
                parentTransactionId = "tx1"
            )
        }

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)
        coEvery { transactionRepository.createTransactions(any()) } returns Result.Success(expectedSplitTransactions)

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val splitResult = result.data
        assertEquals(2, splitResult.splitTransactions.size)
        assertEquals(originalTransaction, splitResult.originalTransaction)
        
        coVerify {
            transactionRepository.getTransactionById("tx1")
            transactionRepository.createTransactions(any())
        }
        coVerify(exactly = 0) {
            transactionRepository.deleteTransaction("tx1")
        }
    }

    @Test
    fun `should validate split amounts equal original amount`() = runTest {
        // Given - splits don't add up to original amount
        val splitParts = listOf(
            TransactionSplit(
                amount = Money(150.0, Currency.SAR),
                category = TransactionCategory.FOOD_DINING,
                description = "Food items"
            ),
            TransactionSplit(
                amount = Money(100.0, Currency.SAR), // Total: 250, Original: 300
                category = TransactionCategory.HOUSEHOLD,
                description = "Household items"
            )
        )

        val request = SplitTransactionRequest(
            originalTransactionId = "tx1",
            splits = splitParts,
            keepOriginal = false
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Split amounts must equal original amount") == true)
    }

    @Test
    fun `should validate currency consistency`() = runTest {
        // Given - different currency in split
        val splitParts = listOf(
            TransactionSplit(
                amount = Money(200.0, Currency.USD), // Different currency
                category = TransactionCategory.FOOD_DINING,
                description = "Food items"
            ),
            TransactionSplit(
                amount = Money(100.0, Currency.SAR),
                category = TransactionCategory.HOUSEHOLD,
                description = "Household items"
            )
        )

        val request = SplitTransactionRequest(
            originalTransactionId = "tx1",
            splits = splitParts,
            keepOriginal = false
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("All split amounts must have the same currency") == true)
    }

    @Test
    fun `should handle percentage-based splitting`() = runTest {
        // Given
        val percentageSplits = listOf(
            TransactionSplitPercentage(
                percentage = 50.0,
                category = TransactionCategory.FOOD_DINING,
                description = "Food items (50%)"
            ),
            TransactionSplitPercentage(
                percentage = 30.0,
                category = TransactionCategory.HOUSEHOLD,
                description = "Household items (30%)"
            ),
            TransactionSplitPercentage(
                percentage = 20.0,
                category = TransactionCategory.PERSONAL_CARE,
                description = "Personal care (20%)"
            )
        )

        val request = SplitTransactionByPercentageRequest(
            originalTransactionId = "tx1",
            percentageSplits = percentageSplits,
            keepOriginal = false
        )

        val expectedSplitTransactions = listOf(
            originalTransaction.copy(
                id = "tx1_split_1",
                amount = Money(150.0, Currency.SAR), // 50% of 300
                category = TransactionCategory.FOOD_DINING,
                description = "Food items (50%)",
                parentTransactionId = "tx1"
            ),
            originalTransaction.copy(
                id = "tx1_split_2",
                amount = Money(90.0, Currency.SAR), // 30% of 300
                category = TransactionCategory.HOUSEHOLD,
                description = "Household items (30%)",
                parentTransactionId = "tx1"
            ),
            originalTransaction.copy(
                id = "tx1_split_3",
                amount = Money(60.0, Currency.SAR), // 20% of 300
                category = TransactionCategory.PERSONAL_CARE,
                description = "Personal care (20%)",
                parentTransactionId = "tx1"
            )
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)
        coEvery { transactionRepository.createTransactions(any()) } returns Result.Success(expectedSplitTransactions)
        coEvery { transactionRepository.deleteTransaction("tx1") } returns Result.Success(Unit)

        // When
        val result = splitTransactionUseCase.splitByPercentage(request)

        // Then
        assertTrue(result is Result.Success)
        val splitResult = result.data
        assertEquals(3, splitResult.splitTransactions.size)
        assertEquals(150.0, splitResult.splitTransactions[0].amount.value)
        assertEquals(90.0, splitResult.splitTransactions[1].amount.value)
        assertEquals(60.0, splitResult.splitTransactions[2].amount.value)
    }

    @Test
    fun `should validate percentages sum to 100`() = runTest {
        // Given - percentages don't add up to 100%
        val percentageSplits = listOf(
            TransactionSplitPercentage(
                percentage = 50.0,
                category = TransactionCategory.FOOD_DINING,
                description = "Food items"
            ),
            TransactionSplitPercentage(
                percentage = 30.0, // Total: 80%, not 100%
                category = TransactionCategory.HOUSEHOLD,
                description = "Household items"
            )
        )

        val request = SplitTransactionByPercentageRequest(
            originalTransactionId = "tx1",
            percentageSplits = percentageSplits,
            keepOriginal = false
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)

        // When
        val result = splitTransactionUseCase.splitByPercentage(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Percentages must sum to 100") == true)
    }

    @Test
    fun `should handle equal splitting`() = runTest {
        // Given
        val request = SplitTransactionEquallyRequest(
            originalTransactionId = "tx1",
            numberOfSplits = 3,
            categories = listOf(
                TransactionCategory.FOOD_DINING,
                TransactionCategory.HOUSEHOLD,
                TransactionCategory.PERSONAL_CARE
            ),
            descriptions = listOf(
                "Food portion",
                "Household portion",
                "Personal care portion"
            ),
            keepOriginal = false
        )

        val expectedSplitTransactions = listOf(
            originalTransaction.copy(
                id = "tx1_split_1",
                amount = Money(100.0, Currency.SAR), // 300 / 3
                category = TransactionCategory.FOOD_DINING,
                description = "Food portion",
                parentTransactionId = "tx1"
            ),
            originalTransaction.copy(
                id = "tx1_split_2",
                amount = Money(100.0, Currency.SAR),
                category = TransactionCategory.HOUSEHOLD,
                description = "Household portion",
                parentTransactionId = "tx1"
            ),
            originalTransaction.copy(
                id = "tx1_split_3",
                amount = Money(100.0, Currency.SAR),
                category = TransactionCategory.PERSONAL_CARE,
                description = "Personal care portion",
                parentTransactionId = "tx1"
            )
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)
        coEvery { transactionRepository.createTransactions(any()) } returns Result.Success(expectedSplitTransactions)
        coEvery { transactionRepository.deleteTransaction("tx1") } returns Result.Success(Unit)

        // When
        val result = splitTransactionUseCase.splitEqually(request)

        // Then
        assertTrue(result is Result.Success)
        val splitResult = result.data
        assertEquals(3, splitResult.splitTransactions.size)
        splitResult.splitTransactions.forEach { 
            assertEquals(100.0, it.amount.value)
        }
    }

    @Test
    fun `should handle Saudi-specific splitting scenarios`() = runTest {
        // Given - Ramadan iftar shopping split
        val splitParts = listOf(
            TransactionSplit(
                amount = Money(180.0, Currency.SAR),
                category = TransactionCategory.FOOD_DINING,
                description = "Iftar food items",
                tags = listOf("ramadan", "iftar", "dates", "juice")
            ),
            TransactionSplit(
                amount = Money(70.0, Currency.SAR),
                category = TransactionCategory.CHARITY,
                description = "Dates for mosque distribution",
                tags = listOf("charity", "ramadan", "dates")
            ),
            TransactionSplit(
                amount = Money(50.0, Currency.SAR),
                category = TransactionCategory.HOUSEHOLD,
                description = "Disposable plates and cups",
                tags = listOf("ramadan", "tableware")
            )
        )

        val request = SplitTransactionRequest(
            originalTransactionId = "tx1",
            splits = splitParts,
            keepOriginal = false
        )

        val expectedSplitTransactions = splitParts.mapIndexed { index, split ->
            originalTransaction.copy(
                id = "tx1_split_${index + 1}",
                amount = split.amount,
                category = split.category,
                description = split.description,
                tags = split.tags,
                parentTransactionId = "tx1"
            )
        }

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)
        coEvery { transactionRepository.createTransactions(any()) } returns Result.Success(expectedSplitTransactions)
        coEvery { transactionRepository.deleteTransaction("tx1") } returns Result.Success(Unit)

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val splitResult = result.data
        
        // Verify Ramadan-specific categorization
        assertTrue(splitResult.splitTransactions.any { it.category == TransactionCategory.CHARITY })
        assertTrue(splitResult.splitTransactions.any { it.tags.contains("ramadan") })
        assertTrue(splitResult.splitTransactions.any { it.tags.contains("iftar") })
    }

    @Test
    fun `should handle transaction not found error`() = runTest {
        // Given
        val request = SplitTransactionRequest(
            originalTransactionId = "nonexistent_tx",
            splits = listOf(),
            keepOriginal = false
        )

        coEvery { transactionRepository.getTransactionById("nonexistent_tx") } returns Result.Error(
            Exception("Transaction not found")
        )

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Transaction not found") == true)
    }

    @Test
    fun `should validate minimum split requirements`() = runTest {
        // Given - empty splits
        val request = SplitTransactionRequest(
            originalTransactionId = "tx1",
            splits = emptyList(),
            keepOriginal = false
        )

        coEvery { transactionRepository.getTransactionById("tx1") } returns Result.Success(originalTransaction)

        // When
        val result = splitTransactionUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("At least one split is required") == true)
    }
}