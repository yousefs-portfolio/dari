package code.yousef.dari.sama.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Account summary information
 */
@Serializable
data class Account(
    val accountId: String,
    val accountType: AccountType,
    val accountSubType: String,
    val currency: String, // ISO 4217 currency code
    val nickname: String? = null,
    val accountNumber: String? = null,
    val iban: String? = null,
    val status: AccountStatus,
    val openingDate: Instant? = null,
    val maturityDate: Instant? = null
)

/**
 * Detailed account information
 */
@Serializable
data class AccountDetails(
    val accountId: String,
    val accountType: AccountType,
    val accountSubType: AccountSubType,
    val currency: String,
    val nickname: String? = null,
    val account: List<AccountIdentifier>,
    val maturityDate: String? = null, // ISO 8601 date
    val openingDate: String? = null, // ISO 8601 date
    val status: AccountStatus
)

/**
 * Account balance information
 */
@Serializable
data class AccountBalances(
    val accountId: String,
    val balances: List<Balance>
)

/**
 * Individual balance entry
 */
@Serializable
data class Balance(
    val accountId: String,
    val amount: Amount,
    val creditDebitIndicator: CreditDebitIndicator,
    val type: BalanceType,
    val dateTime: Instant
)

/**
 * Transaction response with pagination
 */
@Serializable
data class TransactionResponse(
    val data: List<Transaction>,
    val pagination: PaginationInfo
)

/**
 * Individual transaction
 */
@Serializable
data class Transaction(
    val transactionId: String,
    val accountId: String,
    val amount: Amount,
    val creditDebitIndicator: CreditDebitIndicator,
    val status: TransactionStatus,
    val bookingDateTime: Instant,
    val valueDateTime: Instant? = null,
    val transactionInformation: String? = null,
    val merchantName: String? = null,
    val merchantCategoryCode: String? = null,
    val proprietaryBankTransactionCode: String? = null,
    val balance: Balance? = null
)

/**
 * Standing order information
 */
@Serializable
data class StandingOrder(
    val standingOrderId: String,
    val accountId: String,
    val frequency: String,
    val reference: String? = null,
    val firstPaymentDateTime: Instant,
    val nextPaymentDateTime: Instant? = null,
    val lastPaymentDateTime: Instant? = null,
    val finalPaymentDateTime: Instant? = null,
    val instructedAmount: Amount,
    val status: String,
    val creditorAccount: CreditorAccount
)

/**
 * Direct debit information
 */
@Serializable
data class DirectDebit(
    val directDebitId: String,
    val accountId: String,
    val mandateId: String,
    val directDebitStatusCode: String,
    val name: String,
    val previousPaymentDateTime: Instant? = null,
    val previousPaymentAmount: Amount? = null
)

/**
 * Account statement
 */
@Serializable
data class Statement(
    val statementId: String,
    val accountId: String,
    val statementReference: String? = null,
    val type: String,
    val startDateTime: Instant,
    val endDateTime: Instant,
    val creationDateTime: Instant,
    val description: String? = null
)

// Supporting data classes and enums
@Serializable
data class AccountIdentifier(
    val schemeName: String,
    val identification: String,
    val name: String? = null,
    val secondaryIdentification: String? = null
)

@Serializable
data class Amount(
    val amount: String, // Decimal string with 2 decimal places
    val currency: String // ISO 4217 currency code
)

@Serializable
data class PaginationInfo(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalRecords: Int
)

@Serializable
data class CreditorAccount(
    val iban: String,
    val name: String
)

@Serializable
data class BankTransactionCode(
    val code: String,
    val subCode: String? = null
)

@Serializable
data class ProprietaryBankTransactionCode(
    val code: String,
    val issuer: String? = null
)

@Serializable
data class MerchantDetails(
    val merchantName: String? = null,
    val merchantCategoryCode: String? = null
)

@Serializable
data class FinancialAgent(
    val schemeName: String,
    val identification: String,
    val name: String? = null
)

@Serializable
data class StatementBenefit(
    val type: String,
    val amount: Amount
)

@Serializable
data class StatementFee(
    val type: String,
    val amount: Amount
)

@Serializable
data class StatementInterest(
    val type: String,
    val rate: String? = null,
    val rateType: String? = null,
    val amount: Amount? = null
)

@Serializable
data class StatementAmount(
    val type: String,
    val amount: Amount
)

@Serializable
data class StatementDateTime(
    val type: String,
    val dateTime: String
)

@Serializable
data class StatementRate(
    val type: String,
    val rate: String,
    val rateType: String
)

@Serializable
data class StatementValue(
    val type: String,
    val value: String
)

// Enums
@Serializable
enum class AccountType {
    BUSINESS,
    PERSONAL,
    CURRENT,
    SAVINGS,
    CREDIT,
    LOAN
}

@Serializable
enum class AccountSubType {
    ChargeCard,
    CreditCard,
    CurrentAccount,
    EMoney,
    Loan,
    Mortgage,
    PrePaidCard,
    Savings
}

@Serializable
enum class AccountStatus {
    Deleted,
    Disabled,
    Enabled,
    Pending,
    ProForma
}

@Serializable
enum class BalanceType {
    AVAILABLE,
    CURRENT,
    CLOSING_AVAILABLE,
    CLOSING_BOOKED,
    CLOSING_CLEARED,
    EXPECTED,
    FORWARD_AVAILABLE,
    INFORMATION,
    INTERIM_AVAILABLE,
    INTERIM_BOOKED,
    INTERIM_CLEARED,
    OPENING_AVAILABLE,
    OPENING_BOOKED,
    OPENING_CLEARED,
    PREVIOUSLY_CLOSED_BOOKED
}

@Serializable
enum class CreditDebitIndicator {
    CREDIT,
    DEBIT
}

@Serializable
enum class TransactionStatus {
    BOOKED,
    PENDING
}

@Serializable
enum class StatementType {
    AccountClosure,
    AccountOpening,
    Annual,
    Interim,
    RegularPeriodic
}