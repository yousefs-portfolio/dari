package code.yousef.dari.shared.domain.models

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Financial Account Domain Model
 * Represents a bank account with balances, metadata, and business logic
 * Follows SAMA Open Banking account structure and Saudi banking practices
 */
@Serializable
data class FinancialAccount(
    val accountId: String,
    val accountNumber: String,
    val bankCode: String,
    val accountName: String,
    val accountType: AccountType,
    val currency: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant = Clock.System.now(),
    val currentBalance: Money? = null,
    val availableBalance: Money? = null,
    val creditLimit: Money? = null,
    val lowBalanceThreshold: Money? = null,
    val lastTransactionDate: Instant? = null,
    val iban: String? = null,
    val accountHolderName: String? = null,
    val branchCode: String? = null,
    val productName: String? = null,
    val interestRate: String? = null,
    val maturityDate: Instant? = null,
    val metadata: Map<String, String> = emptyMap()
) {

    /**
     * Check if account has low balance based on threshold
     */
    fun isLowBalance(): Boolean {
        val threshold = lowBalanceThreshold ?: return false
        val balance = currentBalance ?: return false
        return balance < threshold
    }

    /**
     * Get display name for UI (account name + masked number)
     */
    fun getDisplayName(): String {
        val maskedNumber = if (accountNumber.length <= 6) {
            "($accountNumber)"
        } else {
            "(..." + accountNumber.takeLast(4) + ")"
        }
        return "$accountName $maskedNumber"
    }

    /**
     * Get masked account number for security
     */
    fun getMaskedAccountNumber(): String {
        return if (accountNumber.length <= 6) {
            accountNumber
        } else {
            "*".repeat(accountNumber.length - 4) + accountNumber.takeLast(4)
        }
    }

    /**
     * Calculate reserved amount (difference between current and available balance)
     */
    fun getReservedAmount(): Money {
        val current = currentBalance ?: Money.fromInt(0, currency)
        val available = availableBalance ?: current
        return if (current.currency == available.currency) {
            current - available
        } else {
            Money.fromInt(0, currency)
        }
    }

    /**
     * Get account summary for dashboard display
     */
    fun getAccountSummary(): AccountSummary {
        val current = currentBalance ?: Money.fromInt(0, currency)
        val available = availableBalance ?: current
        val reserved = getReservedAmount()
        
        return AccountSummary(
            accountId = accountId,
            accountName = accountName,
            accountType = accountType,
            bankCode = bankCode,
            currentBalance = current,
            availableBalance = available,
            reservedAmount = reserved,
            currency = currency,
            isActive = isActive,
            isLowBalance = isLowBalance(),
            lastTransactionDate = lastTransactionDate
        )
    }

    /**
     * Calculate days since last transaction
     */
    fun getDaysSinceLastTransaction(): Int {
        val lastTxn = lastTransactionDate ?: return Int.MAX_VALUE
        val now = Clock.System.now()
        val duration = now - lastTxn
        return duration.inWholeDays.toInt()
    }

    /**
     * Check if account is dormant (no transactions for extended period)
     */
    fun isDormant(dormancyPeriodDays: Int = 365): Boolean {
        return getDaysSinceLastTransaction() > dormancyPeriodDays
    }

    /**
     * Check if account supports specific features
     */
    fun supportsFeature(feature: AccountFeature): Boolean {
        return when (feature) {
            AccountFeature.TRANSFERS -> accountType in listOf(AccountType.CURRENT, AccountType.SAVINGS)
            AccountFeature.PAYMENTS -> accountType in listOf(AccountType.CURRENT, AccountType.SAVINGS, AccountType.CREDIT)
            AccountFeature.OVERDRAFT -> accountType == AccountType.CURRENT && creditLimit != null
            AccountFeature.INTEREST_EARNING -> accountType in listOf(AccountType.SAVINGS, AccountType.INVESTMENT)
            AccountFeature.STATEMENTS -> isActive
            AccountFeature.STANDING_ORDERS -> accountType in listOf(AccountType.CURRENT, AccountType.SAVINGS)
        }
    }

    /**
     * Get account health status based on various factors
     */
    fun getHealthStatus(): AccountHealthStatus {
        return when {
            !isActive -> AccountHealthStatus.INACTIVE
            isLowBalance() -> AccountHealthStatus.LOW_BALANCE
            isDormant(90) -> AccountHealthStatus.DORMANT
            currentBalance?.isNegative() == true -> AccountHealthStatus.OVERDRAWN
            else -> AccountHealthStatus.HEALTHY
        }
    }

    /**
     * Format account for display in different contexts
     */
    fun formatForDisplay(context: DisplayContext): String {
        return when (context) {
            DisplayContext.LIST -> "${accountName} • ${getMaskedAccountNumber()}"
            DisplayContext.CARD -> "${accountName}\n${getMaskedAccountNumber()}\n${currentBalance?.format() ?: "Balance unavailable"}"
            DisplayContext.TRANSACTION -> "${accountName} (...${accountNumber.takeLast(4)})"
            DisplayContext.STATEMENT -> "${accountName} - ${accountNumber}"
        }
    }

    /**
     * Calculate available credit (for credit accounts)
     */
    fun getAvailableCredit(): Money? {
        if (accountType != AccountType.CREDIT || creditLimit == null) return null
        val balance = currentBalance ?: Money.fromInt(0, currency)
        return creditLimit + balance.abs() // Credit balance is usually negative
    }

    /**
     * Check if account can perform a transaction of given amount
     */
    fun canTransact(amount: Money): Boolean {
        if (!isActive) return false
        if (amount.currency != currency) return false
        
        val available = when (accountType) {
            AccountType.CREDIT -> getAvailableCredit() ?: Money.fromInt(0, currency)
            else -> availableBalance ?: currentBalance ?: Money.fromInt(0, currency)
        }
        
        return available >= amount
    }
}

/**
 * Account Type enumeration following Saudi banking standards
 */
@Serializable
enum class AccountType(val displayName: String, val displayNameAr: String) {
    CURRENT("Current Account", "حساب جاري"),
    SAVINGS("Savings Account", "حساب توفير"),
    CREDIT("Credit Account", "حساب ائتماني"),
    LOAN("Loan Account", "حساب قرض"),
    INVESTMENT("Investment Account", "حساب استثماري"),
    PREPAID("Prepaid Account", "حساب مدفوع مسبقاً");

    fun isDebitAccount(): Boolean = this in listOf(CURRENT, SAVINGS, PREPAID)
    fun isCreditAccount(): Boolean = this in listOf(CREDIT, LOAN)
    fun isInvestmentAccount(): Boolean = this == INVESTMENT
}

/**
 * Account features that can be supported
 */
@Serializable
enum class AccountFeature {
    TRANSFERS,
    PAYMENTS,
    OVERDRAFT,
    INTEREST_EARNING,
    STATEMENTS,
    STANDING_ORDERS
}

/**
 * Account health status indicators
 */
@Serializable
enum class AccountHealthStatus(val displayName: String, val displayNameAr: String) {
    HEALTHY("Healthy", "جيد"),
    LOW_BALANCE("Low Balance", "رصيد منخفض"),
    OVERDRAWN("Overdrawn", "مسحوب على المكشوف"),
    DORMANT("Dormant", "خامل"),
    INACTIVE("Inactive", "غير نشط")
}

/**
 * Display context for account formatting
 */
@Serializable
enum class DisplayContext {
    LIST,        // Account selection list
    CARD,        // Account card view
    TRANSACTION, // Transaction details
    STATEMENT    // Account statement
}

/**
 * Account Summary for dashboard and overview screens
 */
@Serializable
data class AccountSummary(
    val accountId: String,
    val accountName: String,
    val accountType: AccountType,
    val bankCode: String,
    val currentBalance: Money,
    val availableBalance: Money,
    val reservedAmount: Money,
    val currency: String,
    val isActive: Boolean,
    val isLowBalance: Boolean,
    val lastTransactionDate: Instant?
) {
    fun getBalanceChangeIndicator(): String {
        return when {
            reservedAmount.isPositive() -> "⏳" // Pending transactions
            isLowBalance -> "⚠️" // Low balance warning
            currentBalance.isPositive() -> "✅" // Healthy balance
            else -> "❌" // Zero or negative balance
        }
    }
}