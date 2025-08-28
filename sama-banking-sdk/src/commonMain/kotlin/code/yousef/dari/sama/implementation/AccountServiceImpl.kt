package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.AccountService
import code.yousef.dari.sama.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * SAMA-compliant Account Service Implementation
 * Implements Account Information Service endpoints according to SAMA specifications
 */
class AccountServiceImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AccountService {

    @Serializable
    private data class AccountsResponse(
        val data: List<AccountData>
    )

    @Serializable
    private data class AccountData(
        val accountId: String,
        val accountType: String,
        val accountSubType: String,
        val currency: String,
        val nickname: String? = null,
        val account: List<AccountInfo>
    )

    @Serializable
    private data class AccountInfo(
        val schemeName: String,
        val identification: String,
        val name: String? = null
    )

    @Serializable
    private data class BalancesResponse(
        val data: List<BalanceData>
    )

    @Serializable
    private data class BalanceData(
        val accountId: String,
        val amount: AmountData,
        val creditDebitIndicator: String,
        val type: String,
        val dateTime: String
    )

    @Serializable
    private data class AmountData(
        val amount: String,
        val currency: String
    )

    @Serializable
    private data class TransactionsResponse(
        val data: List<TransactionData>,
        val links: Links? = null,
        val meta: Meta? = null
    )

    @Serializable
    private data class TransactionData(
        val transactionId: String,
        val accountId: String,
        val amount: AmountData,
        val creditDebitIndicator: String,
        val status: String,
        val bookingDateTime: String,
        val valueDateTime: String? = null,
        val transactionInformation: String? = null,
        val merchantName: String? = null,
        val merchantCategoryCode: String? = null,
        val proprietaryBankTransactionCode: String? = null,
        val balance: BalanceData? = null
    )

    @Serializable
    private data class Links(
        val self: String? = null,
        val first: String? = null,
        val prev: String? = null,
        val next: String? = null,
        val last: String? = null
    )

    @Serializable
    private data class Meta(
        val totalPages: Int? = null,
        val totalRecords: Int? = null
    )

    override suspend fun getAccounts(accessToken: String): Result<List<Account>> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                val accountsResponse = response.body<AccountsResponse>()
                val accounts = accountsResponse.data.map { accountData ->
                    Account(
                        accountId = accountData.accountId,
                        accountType = mapAccountType(accountData.accountType),
                        accountSubType = accountData.accountSubType,
                        currency = accountData.currency,
                        nickname = accountData.nickname,
                        accountNumber = accountData.account.find { it.schemeName == "UK.OBIE.IBAN" }?.identification,
                        iban = accountData.account.find { it.schemeName == "UK.OBIE.IBAN" }?.identification,
                        status = AccountStatus.Enabled, // Default status
                        openingDate = null, // Not available in list endpoint
                        maturityDate = null
                    )
                }
                Result.success(accounts)
            } else {
                Result.failure(Exception("Failed to get accounts: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAccountDetails(
        accessToken: String,
        accountId: String
    ): Result<AccountDetails> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts/$accountId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                val accountData = response.body<AccountData>()
                val account = AccountDetails(
                    accountId = accountData.accountId,
                    accountType = mapAccountType(accountData.accountType),
                    accountSubType = mapAccountSubType(accountData.accountSubType),
                    currency = accountData.currency,
                    nickname = accountData.nickname,
                    account = accountData.account.map { info ->
                        AccountIdentifier(
                            schemeName = info.schemeName,
                            identification = info.identification,
                            name = info.name
                        )
                    },
                    status = AccountStatus.Enabled,
                    openingDate = null,
                    maturityDate = null
                )
                Result.success(account)
            } else {
                Result.failure(Exception("Failed to get account details: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAccountBalances(
        accessToken: String,
        accountId: String
    ): Result<AccountBalances> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts/$accountId/balances") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                val balancesResponse = response.body<BalancesResponse>()
                val balances = balancesResponse.data.map { balanceData ->
                    Balance(
                        accountId = balanceData.accountId,
                        amount = Amount(
                            amount = balanceData.amount.amount,
                            currency = balanceData.amount.currency
                        ),
                        creditDebitIndicator = mapCreditDebitIndicator(balanceData.creditDebitIndicator),
                        type = mapBalanceType(balanceData.type),
                        dateTime = parseDateTime(balanceData.dateTime)
                    )
                }
                Result.success(AccountBalances(accountId = accountId, balances = balances))
            } else {
                Result.failure(Exception("Failed to get balances: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactions(
        accessToken: String,
        accountId: String,
        fromDate: String?,
        toDate: String?,
        limit: Int,
        offset: Int
    ): Result<TransactionResponse> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts/$accountId/transactions") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                
                fromDate?.let { parameter("fromBookingDateTime", it) }
                toDate?.let { parameter("toBookingDateTime", it) }
                parameter("limit", limit.toString())
                parameter("offset", offset.toString())
            }

            if (response.status == HttpStatusCode.OK) {
                val transactionsResponse = response.body<TransactionsResponse>()
                val transactions = transactionsResponse.data.map { txnData ->
                    Transaction(
                        transactionId = txnData.transactionId,
                        accountId = txnData.accountId,
                        amount = Amount(
                            amount = txnData.amount.amount,
                            currency = txnData.amount.currency
                        ),
                        creditDebitIndicator = mapCreditDebitIndicator(txnData.creditDebitIndicator),
                        status = mapTransactionStatus(txnData.status),
                        bookingDateTime = parseDateTime(txnData.bookingDateTime),
                        valueDateTime = txnData.valueDateTime?.let { parseDateTime(it) },
                        transactionInformation = txnData.transactionInformation,
                        merchantName = txnData.merchantName,
                        merchantCategoryCode = txnData.merchantCategoryCode,
                        proprietaryBankTransactionCode = txnData.proprietaryBankTransactionCode,
                        balance = txnData.balance?.let { balanceData ->
                            Balance(
                                accountId = balanceData.accountId,
                                amount = Amount(
                                    amount = balanceData.amount.amount,
                                    currency = balanceData.amount.currency
                                ),
                                creditDebitIndicator = mapCreditDebitIndicator(balanceData.creditDebitIndicator),
                                type = mapBalanceType(balanceData.type),
                                dateTime = parseDateTime(balanceData.dateTime)
                            )
                        }
                    )
                }
                
                val pagination = PaginationInfo(
                    page = (offset / limit) + 1,
                    pageSize = limit,
                    totalPages = transactionsResponse.meta?.totalPages ?: 1,
                    totalRecords = transactionsResponse.meta?.totalRecords ?: transactions.size
                )

                Result.success(TransactionResponse(data = transactions, pagination = pagination))
            } else {
                Result.failure(Exception("Failed to get transactions: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStandingOrders(
        accessToken: String,
        accountId: String
    ): Result<List<StandingOrder>> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts/$accountId/standing-orders") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                // For now, return empty list as implementation would depend on specific bank API
                Result.success(emptyList())
            } else {
                Result.failure(Exception("Failed to get standing orders: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDirectDebits(
        accessToken: String,
        accountId: String
    ): Result<List<DirectDebit>> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts/$accountId/direct-debits") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                // For now, return empty list as implementation would depend on specific bank API
                Result.success(emptyList())
            } else {
                Result.failure(Exception("Failed to get direct debits: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatements(
        accessToken: String,
        accountId: String,
        fromDate: String,
        toDate: String
    ): Result<List<Statement>> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/accounts/$accountId/statements") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                
                parameter("fromStatementDateTime", fromDate)
                parameter("toStatementDateTime", toDate)
            }

            if (response.status == HttpStatusCode.OK) {
                // For now, return empty list as implementation would depend on specific bank API
                Result.success(emptyList())
            } else {
                Result.failure(Exception("Failed to get statements: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapAccountType(type: String): AccountType {
        return when (type.uppercase()) {
            "BUSINESS" -> AccountType.BUSINESS
            "PERSONAL" -> AccountType.PERSONAL
            "CURRENT" -> AccountType.CURRENT
            "SAVINGS" -> AccountType.SAVINGS
            "CREDIT" -> AccountType.CREDIT
            "LOAN" -> AccountType.LOAN
            else -> AccountType.PERSONAL
        }
    }

    private fun mapAccountSubType(subType: String): AccountSubType {
        return when (subType.uppercase()) {
            "CHARGECARD" -> AccountSubType.ChargeCard
            "CREDITCARD" -> AccountSubType.CreditCard
            "CURRENTACCOUNT" -> AccountSubType.CurrentAccount
            "EMONEY" -> AccountSubType.EMoney
            "LOAN" -> AccountSubType.Loan
            "MORTGAGE" -> AccountSubType.Mortgage
            "PREPAIDCARD" -> AccountSubType.PrePaidCard
            "SAVINGS" -> AccountSubType.Savings
            else -> AccountSubType.CurrentAccount
        }
    }

    private fun mapCreditDebitIndicator(indicator: String): CreditDebitIndicator {
        return when (indicator.uppercase()) {
            "CREDIT" -> CreditDebitIndicator.CREDIT
            "DEBIT" -> CreditDebitIndicator.DEBIT
            else -> CreditDebitIndicator.DEBIT
        }
    }

    private fun mapBalanceType(type: String): BalanceType {
        return when (type.uppercase()) {
            "AVAILABLE" -> BalanceType.AVAILABLE
            "CURRENT" -> BalanceType.CURRENT
            "CLOSINGAVAILABLE" -> BalanceType.CLOSING_AVAILABLE
            "CLOSINGBOOKED" -> BalanceType.CLOSING_BOOKED
            else -> BalanceType.CURRENT
        }
    }

    private fun mapTransactionStatus(status: String): TransactionStatus {
        return when (status.uppercase()) {
            "BOOKED" -> TransactionStatus.BOOKED
            "PENDING" -> TransactionStatus.PENDING
            else -> TransactionStatus.BOOKED
        }
    }

    private fun parseDateTime(dateTime: String): kotlinx.datetime.Instant {
        return try {
            kotlinx.datetime.Instant.parse(dateTime)
        } catch (e: Exception) {
            kotlinx.datetime.Clock.System.now()
        }
    }

    private fun generateInteractionId(): String {
        // Simple UUID-like generation for common code
        return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".map { char ->
            when (char) {
                'x' -> (0..15).random().toString(16)
                'y' -> (8..11).random().toString(16)
                else -> char.toString()
            }
        }.joinToString("")
    }
}