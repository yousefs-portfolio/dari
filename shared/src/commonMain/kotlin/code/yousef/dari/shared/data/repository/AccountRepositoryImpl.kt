package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.AccountSummary
import code.yousef.dari.shared.domain.models.AccountType
import code.yousef.dari.shared.domain.models.Money
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers

/**
 * Account Repository Implementation
 * Implements offline-first data access with SQLDelight
 * Handles account synchronization and caching
 */
class AccountRepositoryImpl(
    private val database: DariDatabase
) : AccountRepository {

    private val accountDao = database.accountDao()

    override fun getAccountsFlow(): Flow<List<FinancialAccount>> {
        return accountDao.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { accounts -> accounts.map { it.toDomainModel() } }
    }

    override fun getActiveAccountsFlow(): Flow<List<FinancialAccount>> {
        return accountDao.selectActiveAccounts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { accounts -> accounts.map { it.toDomainModel() } }
    }

    override fun getAccountSummariesFlow(): Flow<List<AccountSummary>> {
        return accountDao.selectAccountSummaries()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { summaries -> summaries.map { it.toAccountSummary() } }
    }

    override suspend fun getAccountById(accountId: String): FinancialAccount? {
        return accountDao.selectById(accountId).executeAsOneOrNull()?.toDomainModel()
    }

    override suspend fun getAccountsByBank(bankCode: String): List<FinancialAccount> {
        return accountDao.selectByBankCode(bankCode)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun getLowBalanceAccounts(): List<FinancialAccount> {
        return accountDao.selectLowBalanceAccounts()
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun upsertAccount(account: FinancialAccount) {
        database.withTransaction {
            val existing = accountDao.selectById(account.accountId).executeAsOneOrNull()
            
            if (existing != null) {
                accountDao.updateAccount(
                    accountName = account.accountName,
                    accountType = account.accountType.name,
                    isActive = if (account.isActive) 1 else 0,
                    updatedAt = account.updatedAt.epochSeconds,
                    currentBalance = account.currentBalance?.amount,
                    availableBalance = account.availableBalance?.amount,
                    creditLimit = account.creditLimit?.amount,
                    lowBalanceThreshold = account.lowBalanceThreshold?.amount,
                    iban = account.iban,
                    accountHolderName = account.accountHolderName,
                    branchCode = account.branchCode,
                    productName = account.productName,
                    interestRate = account.interestRate,
                    maturityDate = account.maturityDate?.epochSeconds,
                    lastTransactionDate = account.lastTransactionDate?.epochSeconds,
                    lastSyncDate = Clock.System.now().epochSeconds,
                    metadata = serializeMetadata(account.metadata),
                    accountId = account.accountId
                )
            } else {
                accountDao.insertAccount(
                    accountId = account.accountId,
                    accountNumber = account.accountNumber,
                    bankCode = account.bankCode,
                    accountName = account.accountName,
                    accountType = account.accountType.name,
                    currency = account.currency,
                    isActive = if (account.isActive) 1 else 0,
                    createdAt = account.createdAt.epochSeconds,
                    updatedAt = account.updatedAt.epochSeconds,
                    currentBalance = account.currentBalance?.amount,
                    availableBalance = account.availableBalance?.amount,
                    creditLimit = account.creditLimit?.amount,
                    lowBalanceThreshold = account.lowBalanceThreshold?.amount,
                    iban = account.iban,
                    accountHolderName = account.accountHolderName,
                    branchCode = account.branchCode,
                    productName = account.productName,
                    interestRate = account.interestRate,
                    maturityDate = account.maturityDate?.epochSeconds,
                    lastTransactionDate = account.lastTransactionDate?.epochSeconds,
                    lastSyncDate = Clock.System.now().epochSeconds,
                    metadata = serializeMetadata(account.metadata)
                )
            }
        }
    }

    override suspend fun upsertAccounts(accounts: List<FinancialAccount>) {
        database.withTransaction {
            accounts.forEach { account ->
                upsertAccount(account)
            }
        }
    }

    override suspend fun updateAccountBalance(
        accountId: String,
        currentBalance: String,
        availableBalance: String
    ) {
        accountDao.updateBalance(
            currentBalance = currentBalance,
            availableBalance = availableBalance,
            updatedAt = Clock.System.now().epochSeconds,
            lastSyncDate = Clock.System.now().epochSeconds,
            accountId = accountId
        )
    }

    override suspend fun deleteAccount(accountId: String) {
        accountDao.deleteAccount(accountId)
    }

    override suspend fun deleteAccountsByBank(bankCode: String) {
        accountDao.deleteByBankCode(bankCode)
    }

    override suspend fun syncAccounts(bankCode: String): Result<List<FinancialAccount>> {
        // TODO: Implement API synchronization with SAMA Banking SDK
        return try {
            // This would use the OpenBankingClient to fetch accounts
            // For now, return empty success
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAccountsNeedingSync(): List<FinancialAccount> {
        val cutoffTime = Clock.System.now().minus(kotlinx.datetime.DateTimeUnit.HOUR * 1).epochSeconds
        return accountDao.getAccountsNeedingSync(cutoffTime)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun markAccountsSynced(accountIds: List<String>) {
        database.withTransaction {
            val now = Clock.System.now().epochSeconds
            accountIds.forEach { accountId ->
                accountDao.updateSyncStatus(
                    syncedAt = now,
                    lastSyncDate = now,
                    updatedAt = now,
                    accountId = accountId
                )
            }
        }
    }

    private fun serializeMetadata(metadata: Map<String, String>): String {
        // Simple JSON serialization - in production would use kotlinx.serialization
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

    private fun deserializeMetadata(json: String): Map<String, String> {
        // Simple JSON deserialization - in production would use kotlinx.serialization
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

// Extension functions to convert between database and domain models
private fun code.yousef.dari.shared.database.Account.toDomainModel(): FinancialAccount {
    return FinancialAccount(
        accountId = accountId,
        accountNumber = accountNumber,
        bankCode = bankCode,
        accountName = accountName,
        accountType = AccountType.valueOf(accountType),
        currency = currency,
        isActive = isActive == 1L,
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        currentBalance = currentBalance?.let { Money(it, currency) },
        availableBalance = availableBalance?.let { Money(it, currency) },
        creditLimit = creditLimit?.let { Money(it, currency) },
        lowBalanceThreshold = lowBalanceThreshold?.let { Money(it, currency) },
        lastTransactionDate = lastTransactionDate?.let { Instant.fromEpochSeconds(it) },
        iban = iban,
        accountHolderName = accountHolderName,
        branchCode = branchCode,
        productName = productName,
        interestRate = interestRate,
        maturityDate = maturityDate?.let { Instant.fromEpochSeconds(it) },
        metadata = deserializeMetadata(metadata ?: "{}")
    )
}

private fun code.yousef.dari.shared.database.SelectAccountSummaries.toAccountSummary(): AccountSummary {
    return AccountSummary(
        accountId = accountId,
        accountName = accountName,
        accountType = AccountType.valueOf(accountType),
        bankCode = bankCode,
        currentBalance = currentBalance?.let { Money(it, currency) } ?: Money.fromInt(0, currency),
        availableBalance = availableBalance?.let { Money(it, currency) } ?: Money.fromInt(0, currency),
        reservedAmount = run {
            val current = currentBalance?.let { Money(it, currency) } ?: Money.fromInt(0, currency)
            val available = availableBalance?.let { Money(it, currency) } ?: current
            current - available
        },
        currency = currency,
        isActive = isActive == 1L,
        isLowBalance = false, // Would be calculated based on threshold
        lastTransactionDate = lastTransactionDate?.let { Instant.fromEpochSeconds(it) }
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