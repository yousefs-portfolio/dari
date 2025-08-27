package code.yousef.dari.shared.domain.model

import code.yousef.dari.shared.domain.usecase.recurring.RecurringFrequency
import kotlinx.datetime.Instant

enum class RecurringTransactionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED
}

data class RecurringTransaction(
    val id: String,
    val accountId: String,
    val merchantName: String,
    val amount: Money,
    val frequency: RecurringFrequency,
    val startDate: Instant,
    val endDate: Instant? = null,
    val categoryId: String,
    val description: String,
    val status: RecurringTransactionStatus = RecurringTransactionStatus.ACTIVE,
    val nextDueDate: Instant,
    val lastProcessedDate: Instant? = null,
    val totalOccurrences: Int = 0,
    val maxOccurrences: Int? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val reminderDaysBefore: Int = 1,
    val isReminderEnabled: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RecurringTransactionOccurrence(
    val id: String,
    val recurringTransactionId: String,
    val transactionId: String? = null, // Linked to actual transaction when processed
    val scheduledDate: Instant,
    val processedDate: Instant? = null,
    val amount: Money,
    val status: OccurrenceStatus,
    val notes: String? = null
)

enum class OccurrenceStatus {
    SCHEDULED,
    PROCESSED,
    SKIPPED,
    FAILED
}