package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.domain.repository.RecurringTransactionRepository
import code.yousef.dari.shared.testutil.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ManageRecurringTransactionUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val recurringTransactionRepository = mockk<RecurringTransactionRepository>()
    private lateinit var manageRecurringTransactionUseCase: ManageRecurringTransactionUseCase

    @BeforeTest
    fun setup() {
        manageRecurringTransactionUseCase = ManageRecurringTransactionUseCase(recurringTransactionRepository)
    }

    @Test
    fun `pause active recurring transaction`() = runTest {
        // Given
        val activeTransaction = createActiveRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns activeTransaction
        coEvery { recurringTransactionRepository.update(any()) } returns Unit

        // When
        val result = manageRecurringTransactionUseCase.pause("rec_1")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            recurringTransactionRepository.update(
                match { it.id == "rec_1" && it.status == RecurringTransactionStatus.PAUSED }
            )
        }
    }

    @Test
    fun `resume paused recurring transaction`() = runTest {
        // Given
        val pausedTransaction = createPausedRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns pausedTransaction
        coEvery { recurringTransactionRepository.update(any()) } returns Unit

        // When
        val result = manageRecurringTransactionUseCase.resume("rec_1")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            recurringTransactionRepository.update(
                match { it.id == "rec_1" && it.status == RecurringTransactionStatus.ACTIVE }
            )
        }
    }

    @Test
    fun `cancel recurring transaction`() = runTest {
        // Given
        val activeTransaction = createActiveRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns activeTransaction
        coEvery { recurringTransactionRepository.update(any()) } returns Unit

        // When
        val result = manageRecurringTransactionUseCase.cancel("rec_1")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            recurringTransactionRepository.update(
                match { it.id == "rec_1" && it.status == RecurringTransactionStatus.CANCELLED }
            )
        }
    }

    @Test
    fun `skip single occurrence of recurring transaction`() = runTest {
        // Given
        val activeTransaction = createActiveRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns activeTransaction
        coEvery { recurringTransactionRepository.update(any()) } returns Unit
        coEvery { recurringTransactionRepository.createOccurrence(any()) } returns Unit

        // When
        val result = manageRecurringTransactionUseCase.skipNext("rec_1", "Vacation - no payment needed")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            // Verify the next due date was updated
            recurringTransactionRepository.update(
                match { it.nextDueDate > activeTransaction.nextDueDate }
            )
            // Verify a skipped occurrence was created
            recurringTransactionRepository.createOccurrence(any())
        }
    }

    @Test
    fun `complete recurring transaction when max occurrences reached`() = runTest {
        // Given
        val nearCompletionTransaction = createNearCompletionRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns nearCompletionTransaction
        coEvery { recurringTransactionRepository.update(any()) } returns Unit

        // When
        val result = manageRecurringTransactionUseCase.processOccurrence("rec_1", "txn_123")

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            recurringTransactionRepository.update(
                match {
                    it.id == "rec_1" &&
                        it.status == RecurringTransactionStatus.COMPLETED &&
                        it.totalOccurrences == 12
                }
            )
        }
    }

    @Test
    fun `fail to pause already paused transaction`() = runTest {
        // Given
        val pausedTransaction = createPausedRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns pausedTransaction

        // When
        val result = manageRecurringTransactionUseCase.pause("rec_1")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Transaction is already paused", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fail to resume active transaction`() = runTest {
        // Given
        val activeTransaction = createActiveRecurringTransaction()
        coEvery { recurringTransactionRepository.getById("rec_1") } returns activeTransaction

        // When
        val result = manageRecurringTransactionUseCase.resume("rec_1")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Transaction is already active", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fail to manage non-existent transaction`() = runTest {
        // Given
        coEvery { recurringTransactionRepository.getById("nonexistent") } returns null

        // When
        val result = manageRecurringTransactionUseCase.pause("nonexistent")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Recurring transaction not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `update recurring transaction amount and frequency`() = runTest {
        // Given
        val existingTransaction = createActiveRecurringTransaction()
        val updateRequest = UpdateRecurringTransactionRequest(
            id = "rec_1",
            amount = Money(BigDecimal("75.00"), "SAR"),
            frequency = RecurringFrequency.BIWEEKLY,
            description = "Updated Netflix Subscription"
        )

        coEvery { recurringTransactionRepository.getById("rec_1") } returns existingTransaction
        coEvery { recurringTransactionRepository.update(any()) } returns Unit

        // When
        val result = manageRecurringTransactionUseCase.update(updateRequest)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            recurringTransactionRepository.update(
                match {
                    it.amount == Money(BigDecimal("75.00"), "SAR") &&
                        it.frequency == RecurringFrequency.BIWEEKLY &&
                        it.description == "Updated Netflix Subscription"
                }
            )
        }
    }

    private fun createActiveRecurringTransaction() = RecurringTransaction(
        id = "rec_1",
        accountId = "account1",
        merchantName = "Netflix",
        amount = Money(BigDecimal("50.00"), "SAR"),
        frequency = RecurringFrequency.MONTHLY,
        startDate = Clock.System.now(),
        categoryId = "entertainment",
        description = "Netflix Subscription",
        status = RecurringTransactionStatus.ACTIVE,
        nextDueDate = Clock.System.now(),
        totalOccurrences = 5,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private fun createPausedRecurringTransaction() = RecurringTransaction(
        id = "rec_1",
        accountId = "account1",
        merchantName = "Netflix",
        amount = Money(BigDecimal("50.00"), "SAR"),
        frequency = RecurringFrequency.MONTHLY,
        startDate = Clock.System.now(),
        categoryId = "entertainment",
        description = "Netflix Subscription",
        status = RecurringTransactionStatus.PAUSED,
        nextDueDate = Clock.System.now(),
        totalOccurrences = 3,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private fun createNearCompletionRecurringTransaction() = RecurringTransaction(
        id = "rec_1",
        accountId = "account1",
        merchantName = "Gym Membership",
        amount = Money(BigDecimal("200.00"), "SAR"),
        frequency = RecurringFrequency.MONTHLY,
        startDate = Clock.System.now(),
        categoryId = "fitness",
        description = "Annual Gym Membership",
        status = RecurringTransactionStatus.ACTIVE,
        nextDueDate = Clock.System.now(),
        totalOccurrences = 11,
        maxOccurrences = 12,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
