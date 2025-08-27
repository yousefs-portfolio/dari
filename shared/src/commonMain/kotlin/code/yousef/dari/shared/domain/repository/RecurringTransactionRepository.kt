package code.yousef.dari.shared.domain.repository

import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionOccurrence
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.domain.usecase.recurring.RecurringFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface RecurringTransactionRepository {
    suspend fun create(recurringTransaction: RecurringTransaction)
    suspend fun update(recurringTransaction: RecurringTransaction)
    suspend fun delete(id: String)
    suspend fun getById(id: String): RecurringTransaction?
    suspend fun getByAccountId(accountId: String): List<RecurringTransaction>
    suspend fun getAllActive(): List<RecurringTransaction>
    suspend fun getAllByStatus(status: RecurringTransactionStatus): List<RecurringTransaction>
    fun observeByAccountId(accountId: String): Flow<List<RecurringTransaction>>
    fun observeActive(): Flow<List<RecurringTransaction>>

    // Occurrence management
    suspend fun createOccurrence(occurrence: RecurringTransactionOccurrence)
    suspend fun updateOccurrence(occurrence: RecurringTransactionOccurrence)
    suspend fun getOccurrencesByRecurringId(recurringId: String): List<RecurringTransactionOccurrence>
    suspend fun getUpcomingOccurrences(beforeDate: Instant): List<RecurringTransactionOccurrence>
    suspend fun getPendingOccurrences(): List<RecurringTransactionOccurrence>

    // Bulk operations
    suspend fun pauseAll(accountId: String)
    suspend fun resumeAll(accountId: String)
    suspend fun deleteByAccountId(accountId: String)

    // Analytics
    suspend fun getTotalByFrequency(frequency: RecurringFrequency): Int
    suspend fun getTotalAmountByPeriod(startDate: Instant, endDate: Instant): Map<String, Double>
    suspend fun getMostCommonMerchants(limit: Int = 10): List<Pair<String, Int>>
}
