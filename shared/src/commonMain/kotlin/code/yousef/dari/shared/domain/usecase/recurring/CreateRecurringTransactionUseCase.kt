package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.domain.repository.RecurringTransactionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import java.math.BigDecimal

data class CreateRecurringTransactionRequest(
    val accountId: String,
    val merchantName: String,
    val amount: Money,
    val frequency: RecurringFrequency,
    val startDate: Instant,
    val categoryId: String,
    val description: String,
    val endDate: Instant? = null,
    val isActive: Boolean = true,
    val maxOccurrences: Int? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val reminderDaysBefore: Int = 1,
    val isReminderEnabled: Boolean = true
)

class CreateRecurringTransactionUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend fun execute(request: CreateRecurringTransactionRequest): Result<RecurringTransaction> {
        return try {
            validateRequest(request)

            val now = Clock.System.now()
            val nextDueDate = calculateNextDueDate(request.startDate, request.frequency)

            val recurringTransaction = RecurringTransaction(
                id = generateId(),
                accountId = request.accountId,
                merchantName = request.merchantName,
                amount = request.amount,
                frequency = request.frequency,
                startDate = request.startDate,
                endDate = request.endDate,
                categoryId = request.categoryId,
                description = request.description,
                status = if (request.isActive) RecurringTransactionStatus.ACTIVE else RecurringTransactionStatus.PAUSED,
                nextDueDate = nextDueDate,
                lastProcessedDate = null,
                totalOccurrences = 0,
                maxOccurrences = request.maxOccurrences,
                tags = request.tags,
                notes = request.notes,
                reminderDaysBefore = request.reminderDaysBefore,
                isReminderEnabled = request.isReminderEnabled,
                createdAt = now,
                updatedAt = now
            )

            recurringTransactionRepository.create(recurringTransaction)
            Result.success(recurringTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateRequest(request: CreateRecurringTransactionRequest) {
        if (request.amount.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }

        if (request.merchantName.isBlank()) {
            throw IllegalArgumentException("Merchant name cannot be blank")
        }

        if (request.accountId.isBlank()) {
            throw IllegalArgumentException("Account ID cannot be blank")
        }

        if (request.categoryId.isBlank()) {
            throw IllegalArgumentException("Category ID cannot be blank")
        }

        request.endDate?.let { endDate ->
            if (endDate <= request.startDate) {
                throw IllegalArgumentException("End date must be after start date")
            }
        }

        if (request.reminderDaysBefore < 0) {
            throw IllegalArgumentException("Reminder days before must be non-negative")
        }

        request.maxOccurrences?.let { maxOccurrences ->
            if (maxOccurrences <= 0) {
                throw IllegalArgumentException("Max occurrences must be greater than zero")
            }
        }
    }

    private fun calculateNextDueDate(startDate: Instant, frequency: RecurringFrequency): Instant {
        val now = Clock.System.now()

        return if (startDate > now) {
            startDate
        } else {
            // Calculate the next occurrence from the start date
            var nextDate = startDate
            while (nextDate <= now) {
                nextDate = nextDate.plus(frequency.days, DateTimeUnit.DAY)
            }
            nextDate
        }
    }

    private fun generateId(): String {
        return "rec_" + Clock.System.now().epochSeconds.toString() + "_" + (1000..9999).random()
    }
}
