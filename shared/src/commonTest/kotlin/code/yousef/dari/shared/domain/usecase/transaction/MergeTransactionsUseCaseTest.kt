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

class MergeTransactionsUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val mergeTransactionsUseCase = MergeTransactionsUseCase(transactionRepository)

    private val duplicateTransaction1 = Transaction(
        id = "tx1",
        accountId = "acc1",
        amount = Money(100.0, Currency.SAR),
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        description = "McDonald's",
        date = LocalDateTime(2024, 1, 15, 12, 30),
        merchant = Merchant("merchant1", "McDonald's", "Restaurant"),
        location = TransactionLocation(24.7136, 46.6753, "Riyadh, Saudi Arabia"),
        isRecurring = false,
        tags = listOf("lunch", "fast-food"),
        paymentMethod = PaymentMethod.CARD,
        status = TransactionStatus.COMPLETED
    )

    private val duplicateTransaction2 = Transaction(
        id = "tx2",
        accountId = "acc1",
        amount = Money(100.0, Currency.SAR),
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        description = "McDonalds", // Slightly different spelling
        date = LocalDateTime(2024, 1, 15, 12, 32), // 2 minutes later
        merchant = Merchant("merchant2", "McDonald's Restaurant", "Food"), // Slightly different name
        location = TransactionLocation(24.7136, 46.6753, "Riyadh, Saudi Arabia"),
        isRecurring = false,
        tags = listOf("lunch"),
        paymentMethod = PaymentMethod.CARD,
        status = TransactionStatus.COMPLETED
    )

    private val duplicateTransaction3 = Transaction(
        id = "tx3",
        accountId = "acc1",
        amount = Money(100.0, Currency.SAR),
        type = TransactionType.EXPENSE,
        category = TransactionCategory.FOOD_DINING,
        description = "MCDONALDS RESTAURANT",
        date = LocalDateTime(2024, 1, 15, 12, 35), // 5 minutes later
        merchant = null,
        location = null,
        isRecurring = false,
        tags = emptyList(),
        paymentMethod = PaymentMethod.CARD,
        status = TransactionStatus.COMPLETED
    )

    private val similarTransaction = Transaction(
        id = "tx4",
        accountId = "acc1",
        amount = Money(250.0, Currency.SAR),
        type = TransactionType.EXPENSE,
        category = TransactionCategory.SHOPPING,
        description = "Carrefour Hypermarket",
        date = LocalDateTime(2024, 1, 15, 10, 0),
        merchant = Merchant("merchant3", "Carrefour", "Supermarket"),
        location = TransactionLocation(24.7000, 46.6800, "Riyadh, Saudi Arabia"),
        isRecurring = false,
        tags = listOf("grocery", "weekly"),
        paymentMethod = PaymentMethod.CARD,
        status = TransactionStatus.COMPLETED
    )

    @Test
    fun `should merge duplicate transactions successfully`() = runTest {
        // Given
        val transactionsToMerge = listOf(duplicateTransaction1, duplicateTransaction2, duplicateTransaction3)
        val request = MergeTransactionsRequest(
            transactionIds = transactionsToMerge.map { it.id },
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = false
        )

        val expectedMergedTransaction = duplicateTransaction1.copy(
            id = "merged_${System.currentTimeMillis()}",
            description = "McDonald's", // Most detailed description
            merchant = duplicateTransaction1.merchant, // Most detailed merchant info
            tags = listOf("lunch", "fast-food"), // Combined unique tags
            mergedTransactionIds = transactionsToMerge.map { it.id }
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(transactionsToMerge)
        coEvery { transactionRepository.createTransaction(any()) } returns Result.Success(expectedMergedTransaction)
        coEvery { transactionRepository.deleteTransactions(any()) } returns Result.Success(Unit)

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val mergedResult = result.data
        assertEquals(expectedMergedTransaction.id, mergedResult.mergedTransaction.id)
        assertEquals(3, mergedResult.originalTransactions.size)
        assertEquals("McDonald's", mergedResult.mergedTransaction.description)
        assertTrue(mergedResult.mergedTransaction.tags.contains("lunch"))
        assertTrue(mergedResult.mergedTransaction.tags.contains("fast-food"))
        
        coVerify {
            transactionRepository.getTransactionsByIds(transactionsToMerge.map { it.id })
            transactionRepository.createTransaction(any())
            transactionRepository.deleteTransactions(transactionsToMerge.map { it.id })
        }
    }

    @Test
    fun `should merge transactions using earliest date strategy`() = runTest {
        // Given
        val transactionsToMerge = listOf(duplicateTransaction1, duplicateTransaction2, duplicateTransaction3)
        val request = MergeTransactionsRequest(
            transactionIds = transactionsToMerge.map { it.id },
            mergeStrategy = MergeStrategy.KEEP_EARLIEST,
            keepOriginals = false
        )

        val expectedMergedTransaction = duplicateTransaction1.copy(
            id = "merged_${System.currentTimeMillis()}",
            date = duplicateTransaction1.date, // Earliest date (12:30)
            mergedTransactionIds = transactionsToMerge.map { it.id }
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(transactionsToMerge)
        coEvery { transactionRepository.createTransaction(any()) } returns Result.Success(expectedMergedTransaction)
        coEvery { transactionRepository.deleteTransactions(any()) } returns Result.Success(Unit)

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val mergedResult = result.data
        assertEquals(duplicateTransaction1.date, mergedResult.mergedTransaction.date)
    }

    @Test
    fun `should merge transactions using latest date strategy`() = runTest {
        // Given
        val transactionsToMerge = listOf(duplicateTransaction1, duplicateTransaction2, duplicateTransaction3)
        val request = MergeTransactionsRequest(
            transactionIds = transactionsToMerge.map { it.id },
            mergeStrategy = MergeStrategy.KEEP_LATEST,
            keepOriginals = false
        )

        val expectedMergedTransaction = duplicateTransaction3.copy(
            id = "merged_${System.currentTimeMillis()}",
            date = duplicateTransaction3.date, // Latest date (12:35)
            mergedTransactionIds = transactionsToMerge.map { it.id }
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(transactionsToMerge)
        coEvery { transactionRepository.createTransaction(any()) } returns Result.Success(expectedMergedTransaction)
        coEvery { transactionRepository.deleteTransactions(any()) } returns Result.Success(Unit)

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val mergedResult = result.data
        assertEquals(duplicateTransaction3.date, mergedResult.mergedTransaction.date)
    }

    @Test
    fun `should detect duplicate transactions automatically`() = runTest {
        // Given
        val allTransactions = listOf(
            duplicateTransaction1,
            duplicateTransaction2,
            duplicateTransaction3,
            similarTransaction
        )

        coEvery { transactionRepository.getTransactionsByAccountId("acc1") } returns Result.Success(allTransactions)

        // When
        val result = mergeTransactionsUseCase.findDuplicateTransactions("acc1")

        // Then
        assertTrue(result is Result.Success)
        val duplicateGroups = result.data
        assertEquals(1, duplicateGroups.size)
        assertEquals(3, duplicateGroups.first().transactions.size) // The 3 McDonald's transactions
        assertTrue(duplicateGroups.first().transactions.none { it.id == "tx4" }) // Carrefour should not be included
    }

    @Test
    fun `should validate transactions are mergeable`() = runTest {
        // Given - different currency transactions
        val transaction1 = duplicateTransaction1
        val transaction2 = duplicateTransaction2.copy(amount = Money(100.0, Currency.USD))
        
        val request = MergeTransactionsRequest(
            transactionIds = listOf(transaction1.id, transaction2.id),
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = false
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(listOf(transaction1, transaction2))

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("different currencies") == true)
    }

    @Test
    fun `should validate transactions are from same account`() = runTest {
        // Given - different account transactions
        val transaction1 = duplicateTransaction1
        val transaction2 = duplicateTransaction2.copy(accountId = "acc2")
        
        val request = MergeTransactionsRequest(
            transactionIds = listOf(transaction1.id, transaction2.id),
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = false
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(listOf(transaction1, transaction2))

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("same account") == true)
    }

    @Test
    fun `should handle Saudi-specific duplicate scenarios`() = runTest {
        // Given - Saudi bank transactions with Arabic merchant names
        val arabicTransaction1 = Transaction(
            id = "tx_ar1",
            accountId = "acc1",
            amount = Money(50.0, Currency.SAR),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.TRANSPORTATION,
            description = "أوبر - الرياض",
            date = LocalDateTime(2024, 1, 15, 8, 0),
            merchant = Merchant("uber_ar", "أوبر", "Transportation"),
            location = TransactionLocation(24.7136, 46.6753, "الرياض، السعودية"),
            isRecurring = false,
            tags = listOf("uber", "transport"),
            paymentMethod = PaymentMethod.CARD,
            status = TransactionStatus.COMPLETED
        )

        val englishTransaction = Transaction(
            id = "tx_en1",
            accountId = "acc1",
            amount = Money(50.0, Currency.SAR),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.TRANSPORTATION,
            description = "Uber - Riyadh",
            date = LocalDateTime(2024, 1, 15, 8, 2),
            merchant = Merchant("uber_en", "Uber Technologies", "Transportation"),
            location = TransactionLocation(24.7136, 46.6753, "Riyadh, Saudi Arabia"),
            isRecurring = false,
            tags = listOf("uber", "ride"),
            paymentMethod = PaymentMethod.CARD,
            status = TransactionStatus.COMPLETED
        )

        val request = MergeTransactionsRequest(
            transactionIds = listOf(arabicTransaction1.id, englishTransaction.id),
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = false
        )

        val expectedMergedTransaction = arabicTransaction1.copy(
            id = "merged_${System.currentTimeMillis()}",
            description = "أوبر - الرياض / Uber - Riyadh", // Combined Arabic and English
            merchant = Merchant("uber_merged", "أوبر / Uber Technologies", "Transportation"),
            tags = listOf("uber", "transport", "ride"), // Combined unique tags
            mergedTransactionIds = listOf(arabicTransaction1.id, englishTransaction.id)
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(listOf(arabicTransaction1, englishTransaction))
        coEvery { transactionRepository.createTransaction(any()) } returns Result.Success(expectedMergedTransaction)
        coEvery { transactionRepository.deleteTransactions(any()) } returns Result.Success(Unit)

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val mergedResult = result.data
        assertTrue(mergedResult.mergedTransaction.description.contains("أوبر"))
        assertTrue(mergedResult.mergedTransaction.description.contains("Uber"))
    }

    @Test
    fun `should handle duplicate bank transfer transactions`() = runTest {
        // Given - Same salary appearing twice due to bank sync issues
        val salaryTransaction1 = Transaction(
            id = "salary1",
            accountId = "acc1",
            amount = Money(8000.0, Currency.SAR),
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "SALARY TRANSFER",
            date = LocalDateTime(2024, 1, 1, 0, 0),
            merchant = null,
            location = null,
            isRecurring = true,
            tags = listOf("salary", "monthly"),
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            status = TransactionStatus.COMPLETED
        )

        val salaryTransaction2 = Transaction(
            id = "salary2",
            accountId = "acc1",
            amount = Money(8000.0, Currency.SAR),
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            description = "Monthly Salary",
            date = LocalDateTime(2024, 1, 1, 0, 1), // 1 minute later
            merchant = null,
            location = null,
            isRecurring = true,
            tags = listOf("salary"),
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            status = TransactionStatus.COMPLETED
        )

        val request = MergeTransactionsRequest(
            transactionIds = listOf(salaryTransaction1.id, salaryTransaction2.id),
            mergeStrategy = MergeStrategy.KEEP_EARLIEST,
            keepOriginals = false
        )

        val expectedMergedTransaction = salaryTransaction1.copy(
            id = "merged_${System.currentTimeMillis()}",
            description = "SALARY TRANSFER / Monthly Salary",
            tags = listOf("salary", "monthly"), // Combined unique tags
            mergedTransactionIds = listOf(salaryTransaction1.id, salaryTransaction2.id)
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(listOf(salaryTransaction1, salaryTransaction2))
        coEvery { transactionRepository.createTransaction(any()) } returns Result.Success(expectedMergedTransaction)
        coEvery { transactionRepository.deleteTransactions(any()) } returns Result.Success(Unit)

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val mergedResult = result.data
        assertEquals(8000.0, mergedResult.mergedTransaction.amount.value)
        assertEquals(TransactionType.INCOME, mergedResult.mergedTransaction.type)
    }

    @Test
    fun `should keep originals when requested`() = runTest {
        // Given
        val transactionsToMerge = listOf(duplicateTransaction1, duplicateTransaction2)
        val request = MergeTransactionsRequest(
            transactionIds = transactionsToMerge.map { it.id },
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = true
        )

        val expectedMergedTransaction = duplicateTransaction1.copy(
            id = "merged_${System.currentTimeMillis()}",
            mergedTransactionIds = transactionsToMerge.map { it.id }
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Success(transactionsToMerge)
        coEvery { transactionRepository.createTransaction(any()) } returns Result.Success(expectedMergedTransaction)

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val mergedResult = result.data
        assertEquals(2, mergedResult.originalTransactions.size) // Originals kept
        
        coVerify {
            transactionRepository.createTransaction(any())
        }
        coVerify(exactly = 0) {
            transactionRepository.deleteTransactions(any())
        }
    }

    @Test
    fun `should require at least two transactions for merging`() = runTest {
        // Given
        val request = MergeTransactionsRequest(
            transactionIds = listOf("tx1"),
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = false
        )

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("At least two transactions") == true)
    }

    @Test
    fun `should handle transactions not found error`() = runTest {
        // Given
        val request = MergeTransactionsRequest(
            transactionIds = listOf("nonexistent1", "nonexistent2"),
            mergeStrategy = MergeStrategy.KEEP_MOST_DETAILED,
            keepOriginals = false
        )

        coEvery { transactionRepository.getTransactionsByIds(any()) } returns Result.Error(
            Exception("Transactions not found")
        )

        // When
        val result = mergeTransactionsUseCase(request)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exception.message?.contains("Transactions not found") == true)
    }
}