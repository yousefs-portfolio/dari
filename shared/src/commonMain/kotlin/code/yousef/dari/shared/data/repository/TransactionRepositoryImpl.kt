package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionType
import code.yousef.dari.shared.domain.models.TransactionStatus
import code.yousef.dari.shared.domain.models.TransactionLocation
import code.yousef.dari.shared.domain.models.TransactionReceipt
import code.yousef.dari.shared.domain.models.Money
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers

/**
 * Transaction Repository Implementation
 * Implements offline-first data access for transactions with SQLDelight
 */
class TransactionRepositoryImpl(
    private val database: DariDatabase
) : TransactionRepository {

    private val transactionDao = database.transactionDao()

    override fun getTransactionsFlow(accountId: String): Flow<List<Transaction>> {
        return transactionDao.selectByAccount(accountId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { transactions -> transactions.map { it.toDomainModel() } }
    }

    override suspend fun getTransactionsPaginated(
        accountId: String,
        limit: Int,
        offset: Int
    ): List<Transaction> {
        return transactionDao.selectByAccountPaginated(accountId, limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun getTransactionsByDateRange(
        accountId: String,
        startDate: Instant,
        endDate: Instant
    ): List<Transaction> {
        return transactionDao.selectByAccountAndDateRange(
            accountId,
            startDate.epochSeconds,
            endDate.epochSeconds
        ).executeAsList().map { it.toDomainModel() }
    }

    override suspend fun getTransactionById(transactionId: String): Transaction? {
        return transactionDao.selectById(transactionId).executeAsOneOrNull()?.toDomainModel()
    }

    override suspend fun getRecentTransactions(limit: Int): List<Transaction> {
        val cutoffTime = Clock.System.now().minus(kotlinx.datetime.DateTimeUnit.DAY * 30).epochSeconds
        return transactionDao.selectRecentTransactions(cutoffTime, limit.toLong())
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun searchTransactions(query: String): List<Transaction> {
        return transactionDao.searchTransactions(query, query, query)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun getTransactionsByCategory(categoryId: String): List<Transaction> {
        return transactionDao.selectByCategory(categoryId)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction> {
        return transactionDao.selectByMerchant(merchantName)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun upsertTransaction(transaction: Transaction) {
        database.withTransaction {
            val existing = transactionDao.selectById(transaction.transactionId).executeAsOneOrNull()
            
            if (existing != null) {
                transactionDao.updateTransaction(
                    amount = transaction.amount.amount,
                    currency = transaction.amount.currency,
                    transactionType = transaction.transactionType.name,
                    status = transaction.status.name,
                    description = transaction.description,
                    merchantName = transaction.merchantName,
                    merchantCategory = transaction.merchantCategory,
                    reference = transaction.reference,
                    bookingDate = transaction.bookingDate?.epochSeconds,
                    valueDate = transaction.valueDate?.epochSeconds,
                    updatedAt = transaction.updatedAt.epochSeconds,
                    categoryId = transaction.categoryId,
                    categoryName = transaction.categoryName,
                    subcategoryName = transaction.subcategoryName,
                    isRecurring = if (transaction.isRecurring) 1 else 0,
                    recurringGroupId = transaction.recurringGroupId,
                    locationLatitude = transaction.location?.latitude,
                    locationLongitude = transaction.location?.longitude,
                    locationName = transaction.location?.name,
                    receiptImagePath = transaction.receipt?.imagePath,
                    receiptData = transaction.receipt?.let { serializeReceiptData(it) },
                    hasReceipt = if (transaction.hasReceipt()) 1 else 0,
                    runningBalance = transaction.runningBalance?.amount,
                    isProcessed = 1,
                    metadata = serializeMetadata(transaction.metadata),
                    transactionId = transaction.transactionId
                )
            } else {
                transactionDao.insertTransaction(
                    transactionId = transaction.transactionId,
                    accountId = transaction.accountId,
                    bankCode = transaction.bankCode,
                    amount = transaction.amount.amount,
                    currency = transaction.amount.currency,
                    transactionType = transaction.transactionType.name,
                    status = transaction.status.name,
                    description = transaction.description,
                    merchantName = transaction.merchantName,
                    merchantCategory = transaction.merchantCategory,
                    reference = transaction.reference,
                    transactionDate = transaction.transactionDate.epochSeconds,
                    bookingDate = transaction.bookingDate?.epochSeconds,
                    valueDate = transaction.valueDate?.epochSeconds,
                    createdAt = transaction.createdAt.epochSeconds,
                    updatedAt = transaction.updatedAt.epochSeconds,
                    categoryId = transaction.categoryId,
                    categoryName = transaction.categoryName,
                    subcategoryName = transaction.subcategoryName,
                    isRecurring = if (transaction.isRecurring) 1 else 0,
                    recurringGroupId = transaction.recurringGroupId,
                    locationLatitude = transaction.location?.latitude,
                    locationLongitude = transaction.location?.longitude,
                    locationName = transaction.location?.name,
                    receiptImagePath = transaction.receipt?.imagePath,
                    receiptData = transaction.receipt?.let { serializeReceiptData(it) },
                    hasReceipt = if (transaction.hasReceipt()) 1 else 0,
                    originalAmount = transaction.originalAmount?.amount,
                    originalCurrency = transaction.originalAmount?.currency,
                    exchangeRate = transaction.exchangeRate,
                    bankTransactionCode = transaction.bankTransactionCode,
                    purposeCode = transaction.purposeCode,
                    proprietaryBankTransactionCode = null,
                    runningBalance = transaction.runningBalance?.amount,
                    isProcessed = 1,
                    isSynced = 0,
                    syncedAt = null,
                    metadata = serializeMetadata(transaction.metadata)
                )
            }
        }
    }

    override suspend fun upsertTransactions(transactions: List<Transaction>) {
        database.withTransaction {
            transactions.forEach { transaction ->
                upsertTransaction(transaction)
            }
        }
    }

    override suspend fun updateTransactionCategory(
        transactionId: String,
        categoryId: String,
        categoryName: String,
        subcategoryName: String?
    ) {
        transactionDao.updateCategory(
            categoryId = categoryId,
            categoryName = categoryName,
            subcategoryName = subcategoryName,
            updatedAt = Clock.System.now().epochSeconds,
            transactionId = transactionId
        )
    }

    override suspend fun deleteTransaction(transactionId: String) {
        transactionDao.deleteTransaction(transactionId)
    }

    override suspend fun deleteTransactionsByAccount(accountId: String) {
        transactionDao.deleteByAccount(accountId)
    }

    override suspend fun syncTransactions(accountId: String): Result<List<Transaction>> {
        // TODO: Implement API synchronization with SAMA Banking SDK
        return try {
            // This would use the OpenBankingClient to fetch transactions
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactionsNeedingSync(): List<Transaction> {
        return transactionDao.selectUnsyncedTransactions()
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun markTransactionsSynced(transactionIds: List<String>) {
        database.withTransaction {
            val now = Clock.System.now().epochSeconds
            transactionIds.forEach { transactionId ->
                transactionDao.updateSyncStatus(
                    isSynced = 1,
                    syncedAt = now,
                    updatedAt = now,
                    transactionId = transactionId
                )
            }
        }
    }

    private fun serializeMetadata(metadata: Map<String, String>): String {
        return if (metadata.isEmpty()) {
            "{}"
        } else {
            metadata.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { "\"${it.key}\":\"${it.value}\"" }
        }
    }

    private fun serializeReceiptData(receipt: TransactionReceipt): String {
        return if (receipt.extractedData.isEmpty()) {
            "{}"
        } else {
            receipt.extractedData.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { "\"${it.key}\":\"${it.value}\"" }
        }
    }

    private fun deserializeReceiptData(json: String): Map<String, String> {
        if (json == "{}") return emptyMap()
        
        return try {
            json.removeSurrounding("{", "}")
                .split(",")
                .associate { pair ->
                    val (key, value) = pair.split(":")
                    key.removeSurrounding("\"") to value.removeSurrounding("\"")
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// Extension function to convert database model to domain model
private fun code.yousef.dari.shared.database.Transaction.toDomainModel(): Transaction {
    return Transaction(
        transactionId = transactionId,
        accountId = accountId,
        bankCode = bankCode,
        amount = Money(amount, currency),
        transactionType = TransactionType.valueOf(transactionType),
        status = TransactionStatus.valueOf(status),
        description = description,
        merchantName = merchantName,
        merchantCategory = merchantCategory,
        reference = reference,
        transactionDate = Instant.fromEpochSeconds(transactionDate),
        bookingDate = bookingDate?.let { Instant.fromEpochSeconds(it) },
        valueDate = valueDate?.let { Instant.fromEpochSeconds(it) },
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        categoryId = categoryId,
        categoryName = categoryName,
        subcategoryName = subcategoryName,
        isRecurring = isRecurring == 1L,
        recurringGroupId = recurringGroupId,
        location = if (locationLatitude != null && locationLongitude != null) {
            TransactionLocation(locationLatitude, locationLongitude, locationName)
        } else null,
        receipt = receiptImagePath?.let { path ->
            TransactionReceipt(
                imagePath = path,
                extractedData = receiptData?.let { deserializeReceiptData(it) } ?: emptyMap()
            )
        },
        originalAmount = originalAmount?.let { Money(it, originalCurrency ?: currency) },
        exchangeRate = exchangeRate,
        bankTransactionCode = bankTransactionCode,
        purposeCode = purposeCode,
        runningBalance = runningBalance?.let { Money(it, currency) },
        metadata = deserializeMetadata(metadata ?: "{}")
    )
}

private fun deserializeMetadata(json: String): Map<String, String> {
    if (json == "{}") return emptyMap()
    
    return try {
        json.removeSurrounding("{", "}")
            .split(",")
            .associate { pair ->
                val (key, value) = pair.split(":")
                key.removeSurrounding("\"") to value.removeSurrounding("\"")
            }
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun deserializeReceiptData(json: String): Map<String, String> {
    if (json == "{}") return emptyMap()
    
    return try {
        json.removeSurrounding("{", "}")
            .split(",")
            .associate { pair ->
                val (key, value) = pair.split(":")
                key.removeSurrounding("\"") to value.removeSurrounding("\"")
            }
    } catch (e: Exception) {
        emptyMap()
    }
}