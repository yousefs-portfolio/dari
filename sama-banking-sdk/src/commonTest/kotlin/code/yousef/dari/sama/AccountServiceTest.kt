package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.AccountService
import code.yousef.dari.sama.models.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for AccountService implementation - TDD approach
 * Tests account listing, details, balances, transactions, and statements
 */
class AccountServiceTest {

    private class MockAccountService : AccountService {
        override suspend fun getAccounts(accessToken: String): Result<List<Account>> {
            return if (accessToken.isNotBlank()) {
                Result.success(
                    listOf(
                        Account(
                            accountId = "acc-001",
                            accountType = AccountType.CURRENT,
                            accountSubType = "Personal",
                            currency = "SAR",
                            nickname = "Main Account",
                            accountNumber = "1234567890",
                            iban = "SA0310000001234567890",
                            status = AccountStatus.ENABLED,
                            openingDate = Clock.System.now(),
                            maturityDate = null
                        ),
                        Account(
                            accountId = "acc-002",
                            accountType = AccountType.SAVINGS,
                            accountSubType = "High Yield",
                            currency = "SAR",
                            nickname = "Savings Account",
                            accountNumber = "1234567891",
                            iban = "SA0310000001234567891",
                            status = AccountStatus.ENABLED,
                            openingDate = Clock.System.now(),
                            maturityDate = null
                        )
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid access token"))
            }
        }

        override suspend fun getAccountDetails(
            accessToken: String,
            accountId: String
        ): Result<Account> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                Result.success(
                    Account(
                        accountId = accountId,
                        accountType = AccountType.CURRENT,
                        accountSubType = "Personal",
                        currency = "SAR",
                        nickname = "Main Account",
                        accountNumber = "1234567890",
                        iban = "SA0310000001234567890",
                        status = AccountStatus.ENABLED,
                        openingDate = Clock.System.now(),
                        maturityDate = null
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun getAccountBalances(
            accessToken: String,
            accountId: String
        ): Result<List<Balance>> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                Result.success(
                    listOf(
                        Balance(
                            accountId = accountId,
                            amount = Amount("5000.00", "SAR"),
                            creditDebitIndicator = CreditDebitIndicator.CREDIT,
                            type = BalanceType.AVAILABLE,
                            dateTime = Clock.System.now()
                        ),
                        Balance(
                            accountId = accountId,
                            amount = Amount("5000.00", "SAR"),
                            creditDebitIndicator = CreditDebitIndicator.CREDIT,
                            type = BalanceType.CURRENT,
                            dateTime = Clock.System.now()
                        )
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun getAccountTransactions(
            accessToken: String,
            accountId: String,
            fromBookingDateTime: String?,
            toBookingDateTime: String?,
            page: Int?,
            pageSize: Int?
        ): Result<TransactionResponse> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                Result.success(
                    TransactionResponse(
                        data = listOf(
                            Transaction(
                                transactionId = "txn-001",
                                accountId = accountId,
                                amount = Amount("150.00", "SAR"),
                                creditDebitIndicator = CreditDebitIndicator.DEBIT,
                                status = TransactionStatus.BOOKED,
                                bookingDateTime = Clock.System.now(),
                                valueDateTime = Clock.System.now(),
                                transactionInformation = "Coffee Shop Payment",
                                merchantName = "Starbucks",
                                merchantCategoryCode = "5814",
                                proprietaryBankTransactionCode = "POS",
                                balance = Balance(
                                    accountId = accountId,
                                    amount = Amount("4850.00", "SAR"),
                                    creditDebitIndicator = CreditDebitIndicator.CREDIT,
                                    type = BalanceType.CURRENT,
                                    dateTime = Clock.System.now()
                                )
                            )
                        ),
                        pagination = PaginationInfo(
                            page = page ?: 1,
                            pageSize = pageSize ?: 100,
                            totalPages = 1,
                            totalRecords = 1
                        )
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun getStandingOrders(
            accessToken: String,
            accountId: String
        ): Result<List<StandingOrder>> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                Result.success(
                    listOf(
                        StandingOrder(
                            standingOrderId = "so-001",
                            accountId = accountId,
                            frequency = "Monthly",
                            reference = "Rent Payment",
                            firstPaymentDateTime = Clock.System.now(),
                            nextPaymentDateTime = Clock.System.now(),
                            lastPaymentDateTime = null,
                            finalPaymentDateTime = null,
                            status = "Active",
                            creditorAccount = CreditorAccount(
                                iban = "SA0320000001234567890",
                                name = "Landlord"
                            ),
                            instructedAmount = Amount("2000.00", "SAR")
                        )
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun getDirectDebits(
            accessToken: String,
            accountId: String
        ): Result<List<DirectDebit>> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                Result.success(
                    listOf(
                        DirectDebit(
                            directDebitId = "dd-001",
                            accountId = accountId,
                            mandateId = "mandate-001",
                            directDebitStatusCode = "Active",
                            name = "Utility Bill",
                            previousPaymentDateTime = Clock.System.now(),
                            previousPaymentAmount = Amount("300.00", "SAR")
                        )
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun getStatements(
            accessToken: String,
            accountId: String,
            fromStatementDateTime: String?,
            toStatementDateTime: String?
        ): Result<List<Statement>> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                Result.success(
                    listOf(
                        Statement(
                            statementId = "stmt-001",
                            accountId = accountId,
                            statementReference = "202312-001",
                            type = "Regular",
                            startDateTime = Clock.System.now(),
                            endDateTime = Clock.System.now(),
                            creationDateTime = Clock.System.now(),
                            description = "Monthly Statement - December 2023"
                        )
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }
    }

    private val accountService = MockAccountService()

    @Test
    fun `should get accounts list successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"

        // When
        val result = accountService.getAccounts(accessToken)

        // Then
        assertTrue(result.isSuccess, "Should get accounts successfully")
        val accounts = result.getOrNull()
        assertNotNull(accounts, "Accounts list should not be null")
        assertEquals(2, accounts.size, "Should return 2 accounts")
        
        val firstAccount = accounts.first()
        assertEquals("acc-001", firstAccount.accountId)
        assertEquals(AccountType.CURRENT, firstAccount.accountType)
        assertEquals("SAR", firstAccount.currency)
        assertEquals("SA0310000001234567890", firstAccount.iban)
        assertEquals(AccountStatus.ENABLED, firstAccount.status)
    }

    @Test
    fun `should fail to get accounts with invalid token`() = runTest {
        // Given
        val invalidToken = ""

        // When
        val result = accountService.getAccounts(invalidToken)

        // Then
        assertTrue(result.isFailure, "Should fail with invalid token")
    }

    @Test
    fun `should get account details successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"

        // When
        val result = accountService.getAccountDetails(accessToken, accountId)

        // Then
        assertTrue(result.isSuccess, "Should get account details successfully")
        val account = result.getOrNull()
        assertNotNull(account, "Account details should not be null")
        assertEquals(accountId, account.accountId)
        assertEquals(AccountType.CURRENT, account.accountType)
        assertEquals("Main Account", account.nickname)
    }

    @Test
    fun `should get account balances successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"

        // When
        val result = accountService.getAccountBalances(accessToken, accountId)

        // Then
        assertTrue(result.isSuccess, "Should get account balances successfully")
        val balances = result.getOrNull()
        assertNotNull(balances, "Balances should not be null")
        assertEquals(2, balances.size, "Should return 2 balances")
        
        val availableBalance = balances.find { it.type == BalanceType.AVAILABLE }
        assertNotNull(availableBalance, "Available balance should exist")
        assertEquals("5000.00", availableBalance.amount.amount)
        assertEquals("SAR", availableBalance.amount.currency)
        assertEquals(CreditDebitIndicator.CREDIT, availableBalance.creditDebitIndicator)
    }

    @Test
    fun `should get account transactions successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"

        // When
        val result = accountService.getAccountTransactions(accessToken, accountId)

        // Then
        assertTrue(result.isSuccess, "Should get transactions successfully")
        val response = result.getOrNull()
        assertNotNull(response, "Transaction response should not be null")
        assertEquals(1, response.data.size, "Should return 1 transaction")
        assertEquals(1, response.pagination.totalRecords, "Should have correct pagination")
        
        val transaction = response.data.first()
        assertEquals("txn-001", transaction.transactionId)
        assertEquals(accountId, transaction.accountId)
        assertEquals("150.00", transaction.amount.amount)
        assertEquals(CreditDebitIndicator.DEBIT, transaction.creditDebitIndicator)
        assertEquals(TransactionStatus.BOOKED, transaction.status)
        assertEquals("Coffee Shop Payment", transaction.transactionInformation)
        assertEquals("Starbucks", transaction.merchantName)
    }

    @Test
    fun `should get standing orders successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"

        // When
        val result = accountService.getStandingOrders(accessToken, accountId)

        // Then
        assertTrue(result.isSuccess, "Should get standing orders successfully")
        val standingOrders = result.getOrNull()
        assertNotNull(standingOrders, "Standing orders should not be null")
        assertEquals(1, standingOrders.size, "Should return 1 standing order")
        
        val standingOrder = standingOrders.first()
        assertEquals("so-001", standingOrder.standingOrderId)
        assertEquals(accountId, standingOrder.accountId)
        assertEquals("Monthly", standingOrder.frequency)
        assertEquals("Rent Payment", standingOrder.reference)
        assertEquals("Active", standingOrder.status)
        assertEquals("2000.00", standingOrder.instructedAmount.amount)
    }

    @Test
    fun `should get direct debits successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"

        // When
        val result = accountService.getDirectDebits(accessToken, accountId)

        // Then
        assertTrue(result.isSuccess, "Should get direct debits successfully")
        val directDebits = result.getOrNull()
        assertNotNull(directDebits, "Direct debits should not be null")
        assertEquals(1, directDebits.size, "Should return 1 direct debit")
        
        val directDebit = directDebits.first()
        assertEquals("dd-001", directDebit.directDebitId)
        assertEquals(accountId, directDebit.accountId)
        assertEquals("mandate-001", directDebit.mandateId)
        assertEquals("Active", directDebit.directDebitStatusCode)
        assertEquals("Utility Bill", directDebit.name)
        assertEquals("300.00", directDebit.previousPaymentAmount.amount)
    }

    @Test
    fun `should get statements successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"

        // When
        val result = accountService.getStatements(accessToken, accountId)

        // Then
        assertTrue(result.isSuccess, "Should get statements successfully")
        val statements = result.getOrNull()
        assertNotNull(statements, "Statements should not be null")
        assertEquals(1, statements.size, "Should return 1 statement")
        
        val statement = statements.first()
        assertEquals("stmt-001", statement.statementId)
        assertEquals(accountId, statement.accountId)
        assertEquals("202312-001", statement.statementReference)
        assertEquals("Regular", statement.type)
        assertEquals("Monthly Statement - December 2023", statement.description)
    }

    @Test
    fun `should handle pagination parameters correctly`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"
        val page = 2
        val pageSize = 50

        // When
        val result = accountService.getAccountTransactions(
            accessToken, accountId, page = page, pageSize = pageSize
        )

        // Then
        assertTrue(result.isSuccess, "Should handle pagination successfully")
        val response = result.getOrNull()
        assertNotNull(response, "Response should not be null")
        assertEquals(page, response.pagination.page, "Should return correct page number")
        assertEquals(pageSize, response.pagination.pageSize, "Should return correct page size")
    }

    @Test
    fun `should handle date range parameters for transactions`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"
        val fromDate = "2023-12-01T00:00:00Z"
        val toDate = "2023-12-31T23:59:59Z"

        // When
        val result = accountService.getAccountTransactions(
            accessToken, accountId, fromDate, toDate
        )

        // Then
        assertTrue(result.isSuccess, "Should handle date range successfully")
        val response = result.getOrNull()
        assertNotNull(response, "Response should not be null")
        assertTrue(response.data.isNotEmpty(), "Should return transactions")
    }

    @Test
    fun `should handle date range parameters for statements`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val accountId = "acc-001"
        val fromDate = "2023-12-01T00:00:00Z"
        val toDate = "2023-12-31T23:59:59Z"

        // When
        val result = accountService.getStatements(
            accessToken, accountId, fromDate, toDate
        )

        // Then
        assertTrue(result.isSuccess, "Should handle date range successfully")
        val statements = result.getOrNull()
        assertNotNull(statements, "Statements should not be null")
        assertTrue(statements.isNotEmpty(), "Should return statements")
    }
}