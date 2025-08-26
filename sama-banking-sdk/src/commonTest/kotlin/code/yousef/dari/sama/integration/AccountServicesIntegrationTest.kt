package code.yousef.dari.sama.integration

import code.yousef.dari.sama.MockSamaServer
import code.yousef.dari.sama.implementation.AccountServiceImpl
import code.yousef.dari.sama.models.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for account services
 * Tests the complete account information retrieval flow
 */
class AccountServicesIntegrationTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private fun createTestClient(): HttpClient {
        return HttpClient(MockSamaServer.createMockEngine()) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }
    
    private val testAccessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.mock.token"
    
    @Test
    fun testGetAccountsList() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        
        // Act
        val result = accountService.getAccounts(testAccessToken)
        
        // Assert
        assertTrue(result.isSuccess)
        val accounts = result.getOrThrow()
        assertTrue(accounts.isNotEmpty())
        assertEquals(2, accounts.size)
        
        val currentAccount = accounts.find { it.accountType == "CURRENT" }
        assertNotNull(currentAccount)
        assertEquals("acc-001", currentAccount.accountId)
        assertEquals("1234567890123456", currentAccount.accountNumber)
        assertEquals("Current Account", currentAccount.accountName)
        assertEquals("SAR", currentAccount.currency)
        assertTrue(currentAccount.isActive)
        assertEquals("SA4420000001234567890123456", currentAccount.iban)
        
        val savingsAccount = accounts.find { it.accountType == "SAVINGS" }
        assertNotNull(savingsAccount)
        assertEquals("acc-002", savingsAccount.accountId)
        assertEquals("Savings Account", savingsAccount.accountName)
    }
    
    @Test
    fun testGetAccountDetails() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountDetails(testAccessToken, accountId)
        
        // Assert
        assertTrue(result.isSuccess)
        val account = result.getOrThrow()
        assertEquals(accountId, account.accountId)
        assertEquals("Ahmad Mohammed", account.accountHolderName)
        assertEquals("001", account.branchCode)
        assertEquals("Premium Current Account", account.productName)
        assertEquals("0.50", account.interestRate)
    }
    
    @Test
    fun testGetAccountBalances() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountBalances(testAccessToken, accountId)
        
        // Assert
        assertTrue(result.isSuccess)
        val balances = result.getOrThrow()
        assertTrue(balances.isNotEmpty())
        assertEquals(2, balances.size)
        
        val closingBooked = balances.find { it.type == "ClosingBooked" }
        assertNotNull(closingBooked)
        assertEquals("15000.00", closingBooked.amount.amount)
        assertEquals("SAR", closingBooked.amount.currency)
        assertEquals("Credit", closingBooked.creditDebitIndicator)
        
        val closingAvailable = balances.find { it.type == "ClosingAvailable" }
        assertNotNull(closingAvailable)
        assertEquals("14500.00", closingAvailable.amount.amount)
    }
    
    @Test
    fun testGetAccountTransactions() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountTransactions(testAccessToken, accountId)
        
        // Assert
        assertTrue(result.isSuccess)
        val transactions = result.getOrThrow()
        assertTrue(transactions.isNotEmpty())
        assertEquals(2, transactions.size)
        
        val withdrawal = transactions.find { it.transactionId == "txn-001" }
        assertNotNull(withdrawal)
        assertEquals("250.00", withdrawal.amount.amount)
        assertEquals("SAR", withdrawal.amount.currency)
        assertEquals("Debit", withdrawal.creditDebitIndicator)
        assertEquals("Booked", withdrawal.status)
        assertEquals("ATM Withdrawal", withdrawal.transactionInformation)
        assertNotNull(withdrawal.merchantDetails)
        assertEquals("Samba ATM", withdrawal.merchantDetails!!.merchantName)
        assertEquals("6011", withdrawal.merchantDetails!!.merchantCategoryCode)
        
        val salary = transactions.find { it.transactionId == "txn-002" }
        assertNotNull(salary)
        assertEquals("3000.00", salary.amount.amount)
        assertEquals("Credit", salary.creditDebitIndicator)
        assertEquals("Salary Transfer", salary.transactionInformation)
        assertEquals("Tech Solutions LLC", salary.merchantDetails!!.merchantName)
    }
    
    @Test
    fun testGetAccountTransactionsWithPagination() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        val paginationParams = PaginationParams(
            pageSize = 10,
            offset = 0
        )
        
        // Act
        val result = accountService.getAccountTransactions(
            accessToken = testAccessToken,
            accountId = accountId,
            fromBookingDate = null,
            toBookingDate = null,
            pagination = paginationParams
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val transactions = result.getOrThrow()
        assertTrue(transactions.size <= 10) // Should respect page size
    }
    
    @Test
    fun testGetAccountTransactionsWithDateRange() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountTransactions(
            accessToken = testAccessToken,
            accountId = accountId,
            fromBookingDate = "2024-08-01T00:00:00Z",
            toBookingDate = "2024-08-31T23:59:59Z"
        )
        
        // Assert
        assertTrue(result.isSuccess)
        val transactions = result.getOrThrow()
        // All transactions should be within the date range
        transactions.forEach { transaction ->
            assertNotNull(transaction.bookingDateTime)
            assertTrue(transaction.bookingDateTime!!.startsWith("2024-08"))
        }
    }
    
    @Test
    fun testGetAccountStandingOrders() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountStandingOrders(testAccessToken, accountId)
        
        // Assert
        assertTrue(result.isSuccess)
        val standingOrders = result.getOrThrow()
        assertTrue(standingOrders.isNotEmpty())
        
        val rentPayment = standingOrders.first()
        assertEquals("so-001", rentPayment.standingOrderId)
        assertEquals(accountId, rentPayment.accountId)
        assertEquals("500.00", rentPayment.amount.amount)
        assertEquals("SAR", rentPayment.amount.currency)
        assertEquals("EveryMonth", rentPayment.frequency)
        assertEquals("Monthly Rent Payment", rentPayment.reference)
        assertEquals("Active", rentPayment.standingOrderStatusCode)
    }
    
    @Test
    fun testGetAccountDirectDebits() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountDirectDebits(testAccessToken, accountId)
        
        // Assert
        assertTrue(result.isSuccess)
        val directDebits = result.getOrThrow()
        assertTrue(directDebits.isNotEmpty())
        
        val electricityDD = directDebits.first()
        assertEquals("dd-001", electricityDD.directDebitId)
        assertEquals(accountId, electricityDD.accountId)
        assertEquals("MANDATE-001", electricityDD.mandateIdentification)
        assertEquals("Active", electricityDD.directDebitStatusCode)
        assertEquals("Saudi Electricity Company", electricityDD.name)
        assertNotNull(electricityDD.previousPaymentAmount)
        assertEquals("180.50", electricityDD.previousPaymentAmount!!.amount)
    }
    
    @Test
    fun testGetAccountStatements() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val accountId = "acc-001"
        
        // Act
        val result = accountService.getAccountStatements(testAccessToken, accountId)
        
        // Assert
        assertTrue(result.isSuccess)
        val statements = result.getOrThrow()
        assertTrue(statements.isNotEmpty())
        assertEquals(2, statements.size)
        
        val julyStatement = statements.find { it.statementId == "stmt-202407" }
        assertNotNull(julyStatement)
        assertEquals(accountId, julyStatement.accountId)
        assertEquals("RegularPeriodic", julyStatement.type)
        assertEquals("2024-07-01T00:00:00Z", julyStatement.startDateTime)
        assertEquals("2024-07-31T23:59:59Z", julyStatement.endDateTime)
        
        val juneStatement = statements.find { it.statementId == "stmt-202406" }
        assertNotNull(juneStatement)
        assertEquals("2024-06-01T00:00:00Z", juneStatement.startDateTime)
        assertEquals("2024-06-30T23:59:59Z", juneStatement.endDateTime)
    }
    
    @Test
    fun testCompleteAccountDataRetrieval() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        
        // Act & Assert - Complete flow for getting all account data
        
        // Step 1: Get accounts list
        val accountsResult = accountService.getAccounts(testAccessToken)
        assertTrue(accountsResult.isSuccess)
        val accounts = accountsResult.getOrThrow()
        assertTrue(accounts.isNotEmpty())
        
        val testAccount = accounts.first()
        val accountId = testAccount.accountId
        
        // Step 2: Get account details
        val detailsResult = accountService.getAccountDetails(testAccessToken, accountId)
        assertTrue(detailsResult.isSuccess)
        val accountDetails = detailsResult.getOrThrow()
        assertEquals(accountId, accountDetails.accountId)
        
        // Step 3: Get account balances
        val balancesResult = accountService.getAccountBalances(testAccessToken, accountId)
        assertTrue(balancesResult.isSuccess)
        val balances = balancesResult.getOrThrow()
        assertTrue(balances.isNotEmpty())
        
        // Step 4: Get transactions
        val transactionsResult = accountService.getAccountTransactions(testAccessToken, accountId)
        assertTrue(transactionsResult.isSuccess)
        val transactions = transactionsResult.getOrThrow()
        assertTrue(transactions.isNotEmpty())
        
        // Step 5: Get standing orders
        val standingOrdersResult = accountService.getAccountStandingOrders(testAccessToken, accountId)
        assertTrue(standingOrdersResult.isSuccess)
        
        // Step 6: Get direct debits
        val directDebitsResult = accountService.getAccountDirectDebits(testAccessToken, accountId)
        assertTrue(directDebitsResult.isSuccess)
        
        // Step 7: Get statements
        val statementsResult = accountService.getAccountStatements(testAccessToken, accountId)
        assertTrue(statementsResult.isSuccess)
        
        // Verify all data is consistent
        assertEquals(testAccount.accountId, accountDetails.accountId)
        assertEquals(testAccount.accountNumber, accountDetails.accountNumber)
        assertEquals(testAccount.accountName, accountDetails.accountName)
        
        // All transactions should belong to this account
        transactions.forEach { transaction ->
            assertEquals(accountId, transaction.accountId)
        }
        
        // All balances should be for this account
        balances.forEach { balance ->
            assertEquals(accountId, balance.accountId)
        }
    }
    
    @Test
    fun testInvalidAccountIdError() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "invalid_account", "error_description": "Account not found"}""",
                status = io.ktor.http.HttpStatusCode.NotFound,
                headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val accountService = AccountServiceImpl(httpClient, json)
        
        // Act
        val result = accountService.getAccountDetails(testAccessToken, "invalid-account-id")
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("Account not found") == true)
    }
    
    @Test
    fun testUnauthorizedAccessError() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "unauthorized", "error_description": "Invalid or expired access token"}""",
                status = io.ktor.http.HttpStatusCode.Unauthorized,
                headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val accountService = AccountServiceImpl(httpClient, json)
        
        // Act
        val result = accountService.getAccounts("invalid-token")
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("unauthorized") == true)
    }
}