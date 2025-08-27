package code.yousef.dari.shared.domain.usecase.recurring

import code.yousef.dari.shared.domain.model.OccurrenceStatus
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionOccurrence
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class PredictNextOccurrenceUseCase {

    fun execute(recurringTransaction: RecurringTransaction): RecurringTransactionOccurrence? {
        if (!canGenerateNextOccurrence(recurringTransaction)) {
            return null
        }

        val nextDate = recurringTransaction.nextDueDate.plus(
            recurringTransaction.frequency.days,
            DateTimeUnit.DAY
        )

        // Check if the next occurrence would exceed end date
        recurringTransaction.endDate?.let { endDate ->
            if (nextDate > endDate) {
                return null
            }
        }

        // Check if the next occurrence would exceed max occurrences
        recurringTransaction.maxOccurrences?.let { maxOccurrences ->
            if (recurringTransaction.totalOccurrences >= maxOccurrences) {
                return null
            }
        }

        return RecurringTransactionOccurrence(
            id = generateOccurrenceId(recurringTransaction.id),
            recurringTransactionId = recurringTransaction.id,
            scheduledDate = nextDate,
            amount = recurringTransaction.amount,
            status = OccurrenceStatus.SCHEDULED,
            notes = null
        )
    }

    fun executeMultiple(
        recurringTransaction: RecurringTransaction,
        count: Int
    ): List<RecurringTransactionOccurrence> {
        if (!canGenerateNextOccurrence(recurringTransaction)) {
            return emptyList()
        }

        val occurrences = mutableListOf<RecurringTransactionOccurrence>()
        var currentDate = recurringTransaction.nextDueDate
        var currentOccurrenceCount = recurringTransaction.totalOccurrences

        repeat(count) {
            val nextDate = currentDate.plus(recurringTransaction.frequency.days, DateTimeUnit.DAY)

            // Check end date constraint
            recurringTransaction.endDate?.let { endDate ->
                if (nextDate > endDate) {
                    return occurrences
                }
            }

            // Check max occurrences constraint
            recurringTransaction.maxOccurrences?.let { maxOccurrences ->
                if (currentOccurrenceCount >= maxOccurrences) {
                    return occurrences
                }
            }

            occurrences.add(
                RecurringTransactionOccurrence(
                    id = generateOccurrenceId(recurringTransaction.id, it + 1),
                    recurringTransactionId = recurringTransaction.id,
                    scheduledDate = nextDate,
                    amount = recurringTransaction.amount,
                    status = OccurrenceStatus.SCHEDULED,
                    notes = null
                )
            )

            currentDate = nextDate
            currentOccurrenceCount++
        }

        return occurrences
    }

    fun predictUntilDate(
        recurringTransaction: RecurringTransaction,
        untilDate: kotlinx.datetime.Instant
    ): List<RecurringTransactionOccurrence> {
        if (!canGenerateNextOccurrence(recurringTransaction)) {
            return emptyList()
        }

        val occurrences = mutableListOf<RecurringTransactionOccurrence>()
        var currentDate = recurringTransaction.nextDueDate
        var currentOccurrenceCount = recurringTransaction.totalOccurrences
        var occurrenceIndex = 1

        while (true) {
            val nextDate = currentDate.plus(recurringTransaction.frequency.days, DateTimeUnit.DAY)

            // Check if we've reached the target date
            if (nextDate > untilDate) {
                break
            }

            // Check end date constraint
            recurringTransaction.endDate?.let { endDate ->
                if (nextDate > endDate) {
                    break
                }
            }

            // Check max occurrences constraint
            recurringTransaction.maxOccurrences?.let { maxOccurrences ->
                if (currentOccurrenceCount >= maxOccurrences) {
                    break
                }
            }

            occurrences.add(
                RecurringTransactionOccurrence(
                    id = generateOccurrenceId(recurringTransaction.id, occurrenceIndex),
                    recurringTransactionId = recurringTransaction.id,
                    scheduledDate = nextDate,
                    amount = recurringTransaction.amount,
                    status = OccurrenceStatus.SCHEDULED,
                    notes = null
                )
            )

            currentDate = nextDate
            currentOccurrenceCount++
            occurrenceIndex++
        }

        return occurrences
    }

    fun calculateTotalProjectedAmount(
        recurringTransaction: RecurringTransaction,
        months: Int = 12
    ): Money {
        val occurrences = executeMultiple(
            recurringTransaction,
            calculateMaxOccurrencesForMonths(recurringTransaction.frequency, months)
        )
        val totalAmount = occurrences.sumOf { it.amount.amount }
        return Money(totalAmount, recurringTransaction.amount.currency)
    }

    private fun canGenerateNextOccurrence(recurringTransaction: RecurringTransaction): Boolean {
        return when (recurringTransaction.status) {
            RecurringTransactionStatus.ACTIVE -> true
            RecurringTransactionStatus.PAUSED -> false
            RecurringTransactionStatus.COMPLETED -> false
            RecurringTransactionStatus.CANCELLED -> false
        }
    }

    private fun generateOccurrenceId(recurringTransactionId: String, index: Int = 1): String {
        return "${recurringTransactionId}_occ_${Clock.System.now().epochSeconds}_${index}"
    }

    private fun calculateMaxOccurrencesForMonths(frequency: RecurringFrequency, months: Int): Int {
        return when (frequency) {
            RecurringFrequency.WEEKLY -> months * 4 + 2 // Adding buffer for month variations
            RecurringFrequency.BIWEEKLY -> months * 2 + 1
            RecurringFrequency.MONTHLY -> months
            RecurringFrequency.QUARTERLY -> (months / 3) + 1
            RecurringFrequency.YEARLY -> (months / 12) + 1
        }
    }
}
