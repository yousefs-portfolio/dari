package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.OccurrenceStatus
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionOccurrence
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.domain.repository.RecurringTransactionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus

data class UpdateRecurringTransactionRequest(
    val id: String,
    val merchantName: String? = null,
    val amount: Money? = null,
    val frequency: RecurringFrequency? = null,
    val categoryId: String? = null,
    val description: String? = null,
    val endDate: Instant? = null,
    val maxOccurrences: Int? = null,
    val reminderDaysBefore: Int? = null,
    val isReminderEnabled: Boolean? = null,
    val tags: List<String>? = null,
    val notes: String? = null
)

class ManageRecurringTransactionUseCase(
    private val recurringTransactionRepository: RecurringTransactionRepository
) {
    suspend fun pause(id: String): Result<RecurringTransaction> {
        return try {
            val transaction = getTransactionOrThrow(id)

            if (transaction.status == RecurringTransactionStatus.PAUSED) {
                throw IllegalStateException("Transaction is already paused")
            }

            if (transaction.status != RecurringTransactionStatus.ACTIVE) {
                throw IllegalStateException("Only active transactions can be paused")
            }

            val updatedTransaction = transaction.copy(
                status = RecurringTransactionStatus.PAUSED,
                updatedAt = Clock.System.now()
            )

            recurringTransactionRepository.update(updatedTransaction)
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resume(id: String): Result<RecurringTransaction> {
        return try {
            val transaction = getTransactionOrThrow(id)

            if (transaction.status == RecurringTransactionStatus.ACTIVE) {
                throw IllegalStateException("Transaction is already active")
            }

            if (transaction.status != RecurringTransactionStatus.PAUSED) {
                throw IllegalStateException("Only paused transactions can be resumed")
            }

            val updatedTransaction = transaction.copy(
                status = RecurringTransactionStatus.ACTIVE,
                updatedAt = Clock.System.now()
            )

            recurringTransactionRepository.update(updatedTransaction)
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancel(id: String): Result<RecurringTransaction> {
        return try {
            val transaction = getTransactionOrThrow(id)

            if (transaction.status == RecurringTransactionStatus.CANCELLED) {
                throw IllegalStateException("Transaction is already cancelled")
            }

            if (transaction.status == RecurringTransactionStatus.COMPLETED) {
                throw IllegalStateException("Completed transactions cannot be cancelled")
            }

            val updatedTransaction = transaction.copy(
                status = RecurringTransactionStatus.CANCELLED,
                updatedAt = Clock.System.now()
            )

            recurringTransactionRepository.update(updatedTransaction)
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun skipNext(id: String, reason: String? = null): Result<RecurringTransaction> {
        return try {
            val transaction = getTransactionOrThrow(id)

            if (transaction.status != RecurringTransactionStatus.ACTIVE) {
                throw IllegalStateException("Only active transactions can have occurrences skipped")
            }

            // Create a skipped occurrence
            val skippedOccurrence = RecurringTransactionOccurrence(
                id = generateOccurrenceId(id),
                recurringTransactionId = id,
                scheduledDate = transaction.nextDueDate,
                amount = transaction.amount,
                status = OccurrenceStatus.SKIPPED,
                notes = reason
            )

            recurringTransactionRepository.createOccurrence(skippedOccurrence)

            // Update next due date
            val nextDueDate = transaction.nextDueDate.plus(transaction.frequency.days, DateTimeUnit.DAY)
            val updatedTransaction = transaction.copy(
                nextDueDate = nextDueDate,
                updatedAt = Clock.System.now()
            )

            recurringTransactionRepository.update(updatedTransaction)
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processOccurrence(id: String, transactionId: String): Result<RecurringTransaction> {
        return try {
            val transaction = getTransactionOrThrow(id)

            if (transaction.status != RecurringTransactionStatus.ACTIVE) {
                throw IllegalStateException("Only active transactions can have occurrences processed")
            }

            // Create processed occurrence
            val processedOccurrence = RecurringTransactionOccurrence(
                id = generateOccurrenceId(id),
                recurringTransactionId = id,
                transactionId = transactionId,
                scheduledDate = transaction.nextDueDate,
                processedDate = Clock.System.now(),
                amount = transaction.amount,
                status = OccurrenceStatus.PROCESSED
            )

            recurringTransactionRepository.createOccurrence(processedOccurrence)

            // Update transaction
            val newTotalOccurrences = transaction.totalOccurrences + 1
            val isCompleted = transaction.maxOccurrences?.let { max -> newTotalOccurrences >= max } ?: false
            val nextDueDate = if (!isCompleted) {
                transaction.nextDueDate.plus(transaction.frequency.days, DateTimeUnit.DAY)
            } else {
                transaction.nextDueDate
            }

            val updatedTransaction = transaction.copy(
                nextDueDate = nextDueDate,
                lastProcessedDate = Clock.System.now(),
                totalOccurrences = newTotalOccurrences,
                status = if (isCompleted) RecurringTransactionStatus.COMPLETED else transaction.status,
                updatedAt = Clock.System.now()
            )

            recurringTransactionRepository.update(updatedTransaction)
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun update(request: UpdateRecurringTransactionRequest): Result<RecurringTransaction> {
        return try {
            val transaction = getTransactionOrThrow(request.id)

            // Validate updates
            request.amount?.let { amount ->
                if (amount.amount <= 0.toBigDecimal()) {
                    throw IllegalArgumentException("Amount must be greater than zero")
                }
            }

            request.maxOccurrences?.let { maxOccurrences ->
                if (maxOccurrences <= 0) {
                    throw IllegalArgumentException("Max occurrences must be greater than zero")
                }
                if (maxOccurrences < transaction.totalOccurrences) {
                    throw IllegalArgumentException("Max occurrences cannot be less than current total occurrences")
                }
            }

            request.reminderDaysBefore?.let { reminderDays ->
                if (reminderDays < 0) {
                    throw IllegalArgumentException("Reminder days before must be non-negative")
                }
            }

            // Recalculate next due date if frequency changed
            val newNextDueDate = if (request.frequency != null && request.frequency != transaction.frequency) {
                calculateNewNextDueDate(transaction, request.frequency)
            } else {
                transaction.nextDueDate
            }

            val updatedTransaction = transaction.copy(
                merchantName = request.merchantName ?: transaction.merchantName,
                amount = request.amount ?: transaction.amount,
                frequency = request.frequency ?: transaction.frequency,
                categoryId = request.categoryId ?: transaction.categoryId,
                description = request.description ?: transaction.description,
                endDate = request.endDate ?: transaction.endDate,
                maxOccurrences = request.maxOccurrences ?: transaction.maxOccurrences,
                reminderDaysBefore = request.reminderDaysBefore ?: transaction.reminderDaysBefore,
                isReminderEnabled = request.isReminderEnabled ?: transaction.isReminderEnabled,
                tags = request.tags ?: transaction.tags,
                notes = request.notes ?: transaction.notes,
                nextDueDate = newNextDueDate,
                updatedAt = Clock.System.now()
            )

            recurringTransactionRepository.update(updatedTransaction)
            Result.success(updatedTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            val transaction = getTransactionOrThrow(id)

            // Optionally, you might want to prevent deletion of completed transactions
            // or require confirmation for active transactions

            recurringTransactionRepository.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getTransactionOrThrow(id: String): RecurringTransaction {
        return recurringTransactionRepository.getById(id)
            ?: throw IllegalArgumentException("Recurring transaction not found")
    }

    private fun calculateNewNextDueDate(
        transaction: RecurringTransaction,
        newFrequency: RecurringFrequency
    ): Instant {
        val now = Clock.System.now()
        val currentNextDue = transaction.nextDueDate

        // If the current next due date is in the future, recalculate based on the new frequency
        return if (currentNextDue > now) {
            // Find the appropriate next date with the new frequency
            var nextDate = transaction.lastProcessedDate ?: transaction.startDate
            while (nextDate <= now) {
                nextDate = nextDate.plus(newFrequency.days, DateTimeUnit.DAY)
            }
            nextDate
        } else {
            currentNextDue
        }
    }

    private fun generateOccurrenceId(recurringTransactionId: String): String {
        return "${recurringTransactionId}_occ_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
    }
}
