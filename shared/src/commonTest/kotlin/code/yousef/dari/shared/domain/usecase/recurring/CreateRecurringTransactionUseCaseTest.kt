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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecurringTransactionUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val recurringTransactionRepository = mockk<RecurringTransactionRepository>()
    private lateinit var createRecurringTransactionUseCase: CreateRecurringTransactionUseCase

    @BeforeTest
    fun setup() {
        createRecurringTransactionUseCase = CreateRecurringTransactionUseCase(recurringTransactionRepository)
    }

    @Test
    fun `create monthly recurring transaction successfully`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "Netflix",
            amount = Money(BigDecimal("50.00"), "SAR"),
            frequency = RecurringFrequency.MONTHLY,
            startDate = startDate,
            categoryId = "entertainment",
            description = "Netflix Subscription",
            endDate = startDate.plus(365, DateTimeUnit.DAY),
            isActive = true
        )

        coEvery {
            recurringTransactionRepository.create(any())
        } returns Unit

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isSuccess)
        val recurringTransaction = result.getOrNull()
        assertNotNull(recurringTransaction)
        assertEquals("Netflix", recurringTransaction.merchantName)
        assertEquals(Money(BigDecimal("50.00"), "SAR"), recurringTransaction.amount)
        assertEquals(RecurringFrequency.MONTHLY, recurringTransaction.frequency)
        assertEquals(RecurringTransactionStatus.ACTIVE, recurringTransaction.status)

        coVerify { recurringTransactionRepository.create(any()) }
    }

    @Test
    fun `create weekly recurring transaction with no end date`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "Coffee Shop",
            amount = Money(BigDecimal("25.00"), "SAR"),
            frequency = RecurringFrequency.WEEKLY,
            startDate = startDate,
            categoryId = "food",
            description = "Weekly Coffee",
            endDate = null,
            isActive = true
        )

        coEvery {
            recurringTransactionRepository.create(any())
        } returns Unit

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isSuccess)
        val recurringTransaction = result.getOrNull()
        assertNotNull(recurringTransaction)
        assertEquals(null, recurringTransaction.endDate)
        assertEquals(RecurringTransactionStatus.ACTIVE, recurringTransaction.status)
    }

    @Test
    fun `create biweekly payroll recurring transaction`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "ACME Corp Payroll",
            amount = Money(BigDecimal("5000.00"), "SAR"),
            frequency = RecurringFrequency.BIWEEKLY,
            startDate = startDate,
            categoryId = "income",
            description = "Salary Deposit",
            endDate = null,
            isActive = true
        )

        coEvery {
            recurringTransactionRepository.create(any())
        } returns Unit

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isSuccess)
        val recurringTransaction = result.getOrNull()
        assertNotNull(recurringTransaction)
        assertEquals(Money(BigDecimal("5000.00"), "SAR"), recurringTransaction.amount)
        assertEquals(RecurringFrequency.BIWEEKLY, recurringTransaction.frequency)
    }

    @Test
    fun `fail to create recurring transaction with invalid amount`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "Invalid Transaction",
            amount = Money(BigDecimal("0.00"), "SAR"),
            frequency = RecurringFrequency.MONTHLY,
            startDate = startDate,
            categoryId = "misc",
            description = "Invalid Amount",
            endDate = null,
            isActive = true
        )

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Amount must be greater than zero", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fail to create recurring transaction with end date before start date`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val endDate = startDate.minus(30, DateTimeUnit.DAY)

        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "Invalid Transaction",
            amount = Money(BigDecimal("100.00"), "SAR"),
            frequency = RecurringFrequency.MONTHLY,
            startDate = startDate,
            categoryId = "misc",
            description = "Invalid Dates",
            endDate = endDate,
            isActive = true
        )

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isFailure)
        assertEquals("End date must be after start date", result.exceptionOrNull()?.message)
    }

    @Test
    fun `create inactive recurring transaction`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "Paused Subscription",
            amount = Money(BigDecimal("30.00"), "SAR"),
            frequency = RecurringFrequency.MONTHLY,
            startDate = startDate,
            categoryId = "entertainment",
            description = "Paused Service",
            endDate = null,
            isActive = false
        )

        coEvery {
            recurringTransactionRepository.create(any())
        } returns Unit

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isSuccess)
        val recurringTransaction = result.getOrNull()
        assertNotNull(recurringTransaction)
        assertEquals(RecurringTransactionStatus.PAUSED, recurringTransaction.status)
    }

    @Test
    fun `handle repository error during creation`() = runTest {
        // Given
        val startDate = Clock.System.now()
        val request = CreateRecurringTransactionRequest(
            accountId = "account1",
            merchantName = "Netflix",
            amount = Money(BigDecimal("50.00"), "SAR"),
            frequency = RecurringFrequency.MONTHLY,
            startDate = startDate,
            categoryId = "entertainment",
            description = "Netflix Subscription",
            endDate = null,
            isActive = true
        )

        coEvery {
            recurringTransactionRepository.create(any())
        } throws RuntimeException("Database error")

        // When
        val result = createRecurringTransactionUseCase.execute(request)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
