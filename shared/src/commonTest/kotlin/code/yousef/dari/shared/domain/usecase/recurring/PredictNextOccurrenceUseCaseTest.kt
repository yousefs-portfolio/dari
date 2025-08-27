package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.testutil.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PredictNextOccurrenceUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var predictNextOccurrenceUseCase: PredictNextOccurrenceUseCase

    @BeforeTest
    fun setup() {
        predictNextOccurrenceUseCase = PredictNextOccurrenceUseCase()
    }

    @Test
    fun `predict next monthly occurrence from current date`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.MONTHLY,
            startDate = now.minus(30, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY)
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNotNull(result)
        val expectedDate = recurringTransaction.nextDueDate.plus(30, DateTimeUnit.DAY)
        assertEquals(expectedDate, result.scheduledDate)
        assertEquals(recurringTransaction.amount, result.amount)
    }

    @Test
    fun `predict next weekly occurrence`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.WEEKLY,
            startDate = now.minus(7, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY)
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNotNull(result)
        val expectedDate = recurringTransaction.nextDueDate.plus(7, DateTimeUnit.DAY)
        assertEquals(expectedDate, result.scheduledDate)
    }

    @Test
    fun `predict next biweekly occurrence`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.BIWEEKLY,
            startDate = now.minus(14, DateTimeUnit.DAY),
            nextDueDate = now.plus(2, DateTimeUnit.DAY)
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNotNull(result)
        val expectedDate = recurringTransaction.nextDueDate.plus(14, DateTimeUnit.DAY)
        assertEquals(expectedDate, result.scheduledDate)
    }

    @Test
    fun `predict next quarterly occurrence`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.QUARTERLY,
            startDate = now.minus(90, DateTimeUnit.DAY),
            nextDueDate = now.plus(3, DateTimeUnit.DAY)
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNotNull(result)
        val expectedDate = recurringTransaction.nextDueDate.plus(90, DateTimeUnit.DAY)
        assertEquals(expectedDate, result.scheduledDate)
    }

    @Test
    fun `predict next yearly occurrence`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.YEARLY,
            startDate = now.minus(365, DateTimeUnit.DAY),
            nextDueDate = now.plus(5, DateTimeUnit.DAY)
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNotNull(result)
        val expectedDate = recurringTransaction.nextDueDate.plus(365, DateTimeUnit.DAY)
        assertEquals(expectedDate, result.scheduledDate)
    }

    @Test
    fun `do not predict next occurrence when end date is reached`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.MONTHLY,
            startDate = now.minus(90, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY),
            endDate = now.plus(15, DateTimeUnit.DAY) // End date before next occurrence
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNull(result)
    }

    @Test
    fun `do not predict next occurrence when max occurrences reached`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.MONTHLY,
            startDate = now.minus(90, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY),
            totalOccurrences = 5,
            maxOccurrences = 5
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNull(result)
    }

    @Test
    fun `do not predict next occurrence when transaction is paused`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.MONTHLY,
            startDate = now.minus(30, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY),
            status = RecurringTransactionStatus.PAUSED
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNull(result)
    }

    @Test
    fun `do not predict next occurrence when transaction is cancelled`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.MONTHLY,
            startDate = now.minus(30, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY),
            status = RecurringTransactionStatus.CANCELLED
        )

        // When
        val result = predictNextOccurrenceUseCase.execute(recurringTransaction)

        // Then
        assertNull(result)
    }

    @Test
    fun `predict multiple future occurrences`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.WEEKLY,
            startDate = now.minus(7, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY)
        )

        // When
        val result = predictNextOccurrenceUseCase.executeMultiple(recurringTransaction, 4)

        // Then
        assertEquals(4, result.size)

        val firstOccurrence = result[0]
        val secondOccurrence = result[1]
        val thirdOccurrence = result[2]
        val fourthOccurrence = result[3]

        assertEquals(recurringTransaction.nextDueDate.plus(7, DateTimeUnit.DAY), firstOccurrence.scheduledDate)
        assertEquals(recurringTransaction.nextDueDate.plus(14, DateTimeUnit.DAY), secondOccurrence.scheduledDate)
        assertEquals(recurringTransaction.nextDueDate.plus(21, DateTimeUnit.DAY), thirdOccurrence.scheduledDate)
        assertEquals(recurringTransaction.nextDueDate.plus(28, DateTimeUnit.DAY), fourthOccurrence.scheduledDate)
    }

    @Test
    fun `stop prediction when end date is reached in multiple occurrences`() = runTest {
        // Given
        val now = Clock.System.now()
        val recurringTransaction = createRecurringTransaction(
            frequency = RecurringFrequency.WEEKLY,
            startDate = now.minus(7, DateTimeUnit.DAY),
            nextDueDate = now.plus(1, DateTimeUnit.DAY),
            endDate = now.plus(15, DateTimeUnit.DAY) // Should allow only 2 more occurrences
        )

        // When
        val result = predictNextOccurrenceUseCase.executeMultiple(recurringTransaction, 5)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.scheduledDate <= recurringTransaction.endDate!! })
    }

    private fun createRecurringTransaction(
        frequency: RecurringFrequency,
        startDate: kotlinx.datetime.Instant,
        nextDueDate: kotlinx.datetime.Instant,
        endDate: kotlinx.datetime.Instant? = null,
        status: RecurringTransactionStatus = RecurringTransactionStatus.ACTIVE,
        totalOccurrences: Int = 0,
        maxOccurrences: Int? = null
    ) = RecurringTransaction(
        id = "rec_1",
        accountId = "account1",
        merchantName = "Test Merchant",
        amount = Money(BigDecimal("100.00"), "SAR"),
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        categoryId = "category1",
        description = "Test Recurring Transaction",
        status = status,
        nextDueDate = nextDueDate,
        totalOccurrences = totalOccurrences,
        maxOccurrences = maxOccurrences,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
