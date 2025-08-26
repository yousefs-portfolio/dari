package code.yousef.dari.sama

import code.yousef.dari.sama.implementation.*
import code.yousef.dari.sama.models.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Performance Tests for SAMA Banking SDK
 * Benchmarks critical operations to ensure they meet performance requirements
 */
class PerformanceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createTestClient(): HttpClient {
        return HttpClient(MockSamaServer.createMockEngine()) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    companion object {
        // Performance thresholds (in milliseconds)
        const val FAST_OPERATION_THRESHOLD = 100L
        const val MEDIUM_OPERATION_THRESHOLD = 500L
        const val SLOW_OPERATION_THRESHOLD = 2000L
        const val BULK_OPERATION_THRESHOLD = 5000L
        
        // Test data sizes
        const val SMALL_DATASET = 10
        const val MEDIUM_DATASET = 100
        const val LARGE_DATASET = 1000
    }

    @Test
    fun `benchmark bank configuration loading`() = runTest {
        val bankRegistry = BankConfigurationRegistry()
        val iterations = 1000
        
        val time = measureTimeMillis {
            repeat(iterations) {
                bankRegistry.getBankConfiguration("alrajhi")
                bankRegistry.getBankConfiguration("snb")
                bankRegistry.getBankConfiguration("riyadbank")
                bankRegistry.getAllBanks()
            }
        }
        
        val avgTime = time / iterations
        assertTrue(avgTime < FAST_OPERATION_THRESHOLD, 
            "Bank configuration loading should be fast: ${avgTime}ms > ${FAST_OPERATION_THRESHOLD}ms")
        
        println("Bank configuration loading benchmark: ${avgTime}ms average over $iterations iterations")
    }

    @Test
    fun `benchmark authentication flow performance`() = runTest {
        val httpClient = createTestClient()
        val securityProvider = CommonSecurityProvider()
        val authService = AuthenticationServiceImpl(httpClient, securityProvider, json)
        
        val time = measureTimeMillis {
            // Complete auth flow
            val parRequest = PARRequest(
                clientId = "test-client",
                redirectUri = "https://test.app.com/callback",
                scope = "accounts payments",
                consentId = "test-consent"
            )
            
            val parResult = authService.initiatePAR(parRequest)
            assertTrue(parResult.isSuccess)
            
            val authUrl = authService.generateAuthorizationUrl(
                clientId = "test-client",
                requestUri = parResult.getOrThrow().requestUri,
                state = "test-state"
            )
            assertTrue(authUrl.isSuccess)
            
            val tokenRequest = TokenRequest(
                grantType = GrantType.AUTHORIZATION_CODE,
                code = "test-code",
                redirectUri = "https://test.app.com/callback",
                clientId = "test-client",
                clientSecret = "test-secret",
                codeVerifier = "test-verifier"
            )
            
            val tokenResult = authService.exchangeAuthorizationCode(tokenRequest)
            assertTrue(tokenResult.isSuccess)
        }
        
        assertTrue(time < MEDIUM_OPERATION_THRESHOLD,
            "Complete auth flow should complete within ${MEDIUM_OPERATION_THRESHOLD}ms: actual ${time}ms")
        
        println("Authentication flow benchmark: ${time}ms")
    }

    @Test
    fun `benchmark account data retrieval performance`() = runTest {
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val testAccessToken = "test-token"
        
        val time = measureTimeMillis {
            // Get accounts
            val accountsResult = accountService.getAccounts(testAccessToken)
            assertTrue(accountsResult.isSuccess)
            
            val accounts = accountsResult.getOrThrow()
            assertNotNull(accounts)
            
            // Get details for first account
            if (accounts.isNotEmpty()) {
                val accountId = accounts.first().accountId
                
                val detailsResult = accountService.getAccountDetails(testAccessToken, accountId)
                assertTrue(detailsResult.isSuccess)
                
                val balancesResult = accountService.getAccountBalances(testAccessToken, accountId)
                assertTrue(balancesResult.isSuccess)
                
                val transactionsResult = accountService.getAccountTransactions(testAccessToken, accountId)
                assertTrue(transactionsResult.isSuccess)
            }
        }
        
        assertTrue(time < MEDIUM_OPERATION_THRESHOLD,
            "Account data retrieval should complete within ${MEDIUM_OPERATION_THRESHOLD}ms: actual ${time}ms")
        
        println("Account data retrieval benchmark: ${time}ms")
    }

    @Test
    fun `benchmark payment processing performance`() = runTest {
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        val testAccessToken = "test-token"
        
        val paymentRequest = DomesticPaymentRequest(
            instructionIdentification = "PERF-TEST-001",
            endToEndIdentification = "PERF-E2E-001",
            instructedAmount = Amount("1000.00", "SAR"),
            debtorAccount = Account(iban = "SA4420000001234567890123456"),
            creditorAccount = Account(iban = "SA4420000001234567890123457"),
            creditorName = "Performance Test Beneficiary"
        )
        
        val time = measureTimeMillis {
            val paymentResult = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
            assertTrue(paymentResult.isSuccess)
            
            val payment = paymentResult.getOrThrow()
            assertNotNull(payment.domesticPaymentId)
            
            // Check payment status
            val statusResult = paymentService.getDomesticPaymentStatus(testAccessToken, payment.domesticPaymentId!!)
            assertTrue(statusResult.isSuccess)
        }
        
        assertTrue(time < MEDIUM_OPERATION_THRESHOLD,
            "Payment processing should complete within ${MEDIUM_OPERATION_THRESHOLD}ms: actual ${time}ms")
        
        println("Payment processing benchmark: ${time}ms")
    }

    @Test
    fun `benchmark bulk payment processing`() = runTest {
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        val testAccessToken = "test-token"
        
        // Generate bulk payments
        val payments = (1..SMALL_DATASET).map { index ->
            DomesticPaymentRequest(
                instructionIdentification = "BULK-$index",
                endToEndIdentification = "BULK-E2E-$index",
                instructedAmount = Amount("${100 + index}.00", "SAR"),
                debtorAccount = Account(iban = "SA4420000001234567890123456"),
                creditorAccount = Account(iban = "SA4420000001234567890123457"),
                creditorName = "Bulk Payment Beneficiary $index"
            )
        }
        
        val time = measureTimeMillis {
            payments.forEach { payment ->
                val result = paymentService.initiateDomesticPayment(testAccessToken, payment)
                assertTrue(result.isSuccess)
            }
        }
        
        val avgTimePerPayment = time / payments.size
        assertTrue(time < BULK_OPERATION_THRESHOLD,
            "Bulk payment processing (${payments.size} payments) should complete within ${BULK_OPERATION_THRESHOLD}ms: actual ${time}ms")
        
        println("Bulk payment processing benchmark: ${time}ms total, ${avgTimePerPayment}ms per payment")
    }

    @Test
    fun `benchmark transaction history loading`() = runTest {
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val testAccessToken = "test-token"
        val accountId = "acc-001"
        
        // Test different pagination sizes
        val pageSizes = listOf(10, 50, 100, 500)
        
        pageSizes.forEach { pageSize ->
            val time = measureTimeMillis {
                val result = accountService.getAccountTransactions(
                    accessToken = testAccessToken,
                    accountId = accountId,
                    pagination = PaginationParams(pageSize = pageSize, offset = 0)
                )
                assertTrue(result.isSuccess)
            }
            
            assertTrue(time < MEDIUM_OPERATION_THRESHOLD,
                "Transaction loading (page size: $pageSize) should complete within ${MEDIUM_OPERATION_THRESHOLD}ms: actual ${time}ms")
            
            println("Transaction loading benchmark (page size $pageSize): ${time}ms")
        }
    }

    @Test
    fun `benchmark PKCE challenge generation`() = runTest {
        val securityProvider = CommonSecurityProvider()
        val iterations = 1000
        
        val time = measureTimeMillis {
            repeat(iterations) {
                val challenge = securityProvider.generatePKCEChallenge()
                assertNotNull(challenge.codeVerifier)
                assertNotNull(challenge.codeChallenge)
            }
        }
        
        val avgTime = time / iterations
        assertTrue(avgTime < FAST_OPERATION_THRESHOLD / 10, // Should be very fast
            "PKCE challenge generation should be very fast: ${avgTime}ms")
        
        println("PKCE challenge generation benchmark: ${avgTime}ms average over $iterations iterations")
    }

    @Test
    fun `benchmark certificate pinning validation`() = runTest {
        val validator = CertificatePinningValidator()
        val testFingerprints = listOf(
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0",
            "B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1",
            "C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2"
        )
        val iterations = 1000
        
        val time = measureTimeMillis {
            repeat(iterations) {
                testFingerprints.forEach { fingerprint ->
                    val isValid = validator.validateFingerprint(fingerprint, testFingerprints)
                    assertTrue(isValid)
                }
            }
        }
        
        val avgTime = time / (iterations * testFingerprints.size)
        assertTrue(avgTime < FAST_OPERATION_THRESHOLD / 100, // Should be extremely fast
            "Certificate pinning validation should be extremely fast: ${avgTime}ms")
        
        println("Certificate pinning validation benchmark: ${avgTime}ms average")
    }

    @Test
    fun `benchmark error handling performance`() = runTest {
        val errorHandler = ErrorHandlerImpl()
        val testErrors = listOf(
            BankError("invalid_request", "The request is malformed"),
            BankError("invalid_client", "Client authentication failed"),
            BankError("invalid_grant", "The provided authorization grant is invalid"),
            BankError("insufficient_funds", "Account has insufficient funds"),
            BankError("limit_exceeded", "Transaction limit exceeded")
        )
        val iterations = 1000
        
        val time = measureTimeMillis {
            repeat(iterations) {
                testErrors.forEach { error ->
                    val handled = errorHandler.handleError(error, "test-context")
                    assertNotNull(handled)
                }
            }
        }
        
        val avgTime = time / (iterations * testErrors.size)
        assertTrue(avgTime < FAST_OPERATION_THRESHOLD / 10,
            "Error handling should be very fast: ${avgTime}ms")
        
        println("Error handling benchmark: ${avgTime}ms average")
    }

    @Test
    fun `benchmark concurrent operations`() = runTest {
        val httpClient = createTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val paymentService = PaymentServiceImpl(httpClient, json)
        val testAccessToken = "test-token"
        
        val time = measureTimeMillis {
            // Simulate concurrent operations
            val accountsDeferred = async { accountService.getAccounts(testAccessToken) }
            val balancesDeferred = async { accountService.getAccountBalances(testAccessToken, "acc-001") }
            val transactionsDeferred = async { accountService.getAccountTransactions(testAccessToken, "acc-001") }
            
            val paymentRequest = DomesticPaymentRequest(
                instructionIdentification = "CONCURRENT-001",
                endToEndIdentification = "CONCURRENT-E2E-001",
                instructedAmount = Amount("500.00", "SAR"),
                debtorAccount = Account(iban = "SA4420000001234567890123456"),
                creditorAccount = Account(iban = "SA4420000001234567890123457"),
                creditorName = "Concurrent Test"
            )
            val paymentDeferred = async { paymentService.initiateDomesticPayment(testAccessToken, paymentRequest) }
            
            // Wait for all operations
            val accountsResult = accountsDeferred.await()
            val balancesResult = balancesDeferred.await()
            val transactionsResult = transactionsDeferred.await()
            val paymentResult = paymentDeferred.await()
            
            assertTrue(accountsResult.isSuccess)
            assertTrue(balancesResult.isSuccess)
            assertTrue(transactionsResult.isSuccess)
            assertTrue(paymentResult.isSuccess)
        }
        
        assertTrue(time < SLOW_OPERATION_THRESHOLD,
            "Concurrent operations should complete within ${SLOW_OPERATION_THRESHOLD}ms: actual ${time}ms")
        
        println("Concurrent operations benchmark: ${time}ms")
    }

    @Test
    fun `benchmark memory usage during large operations`() = runTest {
        val runtime = Runtime.getRuntime()
        val testData = TestDataFactories.TransactionFactory.createTransactionHistory("acc-001", MEDIUM_DATASET)
        
        // Measure memory before
        runtime.gc()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform operations
        val time = measureTimeMillis {
            // Process large dataset
            val processed = testData.map { transaction ->
                // Simulate processing
                transaction.copy(
                    transactionInformation = transaction.transactionInformation?.uppercase(),
                    amount = transaction.amount.copy(
                        amount = (transaction.amount.amount.toDouble() * 1.1).toString()
                    )
                )
            }
            
            // Filter operations
            val credits = processed.filter { it.creditDebitIndicator == "Credit" }
            val debits = processed.filter { it.creditDebitIndicator == "Debit" }
            
            assertTrue(credits.isNotEmpty())
            assertTrue(debits.isNotEmpty())
        }
        
        // Measure memory after
        runtime.gc()
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / 1024 / 1024 // MB
        
        println("Large operations benchmark: ${time}ms, Memory used: ${memoryUsed}MB for $MEDIUM_DATASET items")
        
        // Memory usage should be reasonable (less than 50MB for this test)
        assertTrue(memoryUsed < 50, "Memory usage should be reasonable: ${memoryUsed}MB")
    }

    // Helper function for async operations (simplified)
    private suspend fun <T> async(block: suspend () -> T): AsyncResult<T> {
        return AsyncResult(block())
    }

    private class AsyncResult<T>(private val value: T) {
        suspend fun await(): T = value
    }
    
    @Test
    fun `benchmark API response parsing`() = runTest {
        val largeAccountResponse = """
        {
            "Data": {
                "Account": [
                    ${(1..LARGE_DATASET).map { index ->
                        """
                        {
                            "AccountId": "acc-$index",
                            "AccountNumber": "123456789012345$index",
                            "AccountName": "Test Account $index",
                            "AccountType": "${if (index % 2 == 0) "CURRENT" else "SAVINGS"}",
                            "Currency": "SAR",
                            "IsActive": true,
                            "OpeningDate": "2023-01-${(index % 28 + 1).toString().padStart(2, '0')}T00:00:00Z",
                            "IBAN": "SA44200000012345678901234$index"
                        }
                        """.trimIndent()
                    }.joinToString(",")}
                ]
            }
        }
        """.trimIndent()
        
        val time = measureTimeMillis {
            val parsed = json.decodeFromString<Map<String, Any>>(largeAccountResponse)
            assertNotNull(parsed)
            
            val data = parsed["Data"] as Map<String, Any>
            val accounts = data["Account"] as List<Any>
            assertTrue(accounts.size == LARGE_DATASET)
        }
        
        assertTrue(time < SLOW_OPERATION_THRESHOLD,
            "Large JSON parsing should complete within ${SLOW_OPERATION_THRESHOLD}ms: actual ${time}ms")
        
        println("Large JSON parsing benchmark (${LARGE_DATASET} accounts): ${time}ms")
    }
}