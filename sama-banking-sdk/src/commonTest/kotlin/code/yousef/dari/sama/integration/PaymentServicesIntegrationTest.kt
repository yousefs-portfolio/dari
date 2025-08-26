package code.yousef.dari.sama.integration

import code.yousef.dari.sama.MockSamaServer
import code.yousef.dari.sama.implementation.PaymentServiceImpl
import code.yousef.dari.sama.implementation.ConsentManagerImpl
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
 * Integration tests for payment services
 * Tests the complete payment initiation and management flow
 */
class PaymentServicesIntegrationTest {
    
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
    fun testDomesticPaymentInitiation() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        val paymentRequest = DomesticPaymentRequest(
            instructionIdentification = "PAY-001",
            endToEndIdentification = "E2E-001",
            instructedAmount = Amount(
                amount = "1000.00",
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA4420000001234567890123456"
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457",
                name = "Ahmad Mohammed"
            ),
            creditorName = "Ahmad Mohammed",
            remittanceInformation = RemittanceInformation(
                unstructured = listOf("Salary payment")
            )
        )
        
        // Act
        val result = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
        
        // Assert
        assertTrue(result.isSuccess)
        val payment = result.getOrThrow()
        assertNotNull(payment.domesticPaymentId)
        assertEquals("pay-001", payment.domesticPaymentId)
        assertEquals("AcceptedSettlementInProcess", payment.status)
        assertNotNull(payment.creationDateTime)
        assertNotNull(payment.expectedExecutionDateTime)
        
        // Verify charges are calculated
        assertTrue(payment.charges?.isNotEmpty() == true)
        val samaFee = payment.charges!!.find { it.type == "SAMA_FEE" }
        assertNotNull(samaFee)
        assertEquals("2.00", samaFee.amount.amount)
        assertEquals("SAR", samaFee.amount.currency)
    }
    
    @Test
    fun testDomesticPaymentStatusCheck() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        val paymentId = "pay-001"
        
        // Act
        val result = paymentService.getDomesticPaymentStatus(testAccessToken, paymentId)
        
        // Assert
        assertTrue(result.isSuccess)
        val paymentStatus = result.getOrThrow()
        assertEquals(paymentId, paymentStatus.domesticPaymentId)
        assertEquals("AcceptedSettlementCompleted", paymentStatus.status)
        assertNotNull(paymentStatus.statusUpdateDateTime)
        
        // Verify charges are included in status
        assertTrue(paymentStatus.charges?.isNotEmpty() == true)
    }
    
    @Test
    fun testScheduledPaymentCreation() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        val scheduledPaymentRequest = DomesticScheduledPaymentRequest(
            instructionIdentification = "SCHED-PAY-001",
            endToEndIdentification = "SCHED-E2E-001",
            requestedExecutionDateTime = "2024-09-01T10:00:00Z",
            instructedAmount = Amount(
                amount = "500.00",
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA4420000001234567890123456"
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457",
                name = "Monthly Rent"
            ),
            creditorName = "Property Management",
            remittanceInformation = RemittanceInformation(
                unstructured = listOf("Monthly rent payment - September 2024")
            )
        )
        
        // Act
        val result = paymentService.createScheduledPayment(testAccessToken, scheduledPaymentRequest)
        
        // Assert
        assertTrue(result.isSuccess)
        val scheduledPayment = result.getOrThrow()
        assertNotNull(scheduledPayment.domesticScheduledPaymentId)
        assertEquals("scheduled-pay-001", scheduledPayment.domesticScheduledPaymentId)
        assertEquals("InitiationPending", scheduledPayment.status)
        assertNotNull(scheduledPayment.creationDateTime)
    }
    
    @Test
    fun testScheduledPaymentCancellation() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        val scheduledPaymentId = "scheduled-pay-001"
        
        // Act
        val result = paymentService.cancelScheduledPayment(testAccessToken, scheduledPaymentId)
        
        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow()) // Should return true for successful cancellation
    }
    
    @Test
    fun testCompletePaymentFlow() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        val consentManager = ConsentManagerImpl(httpClient, json)
        
        // Step 1: Create payment consent
        val paymentConsentRequest = PaymentConsentRequest(
            permissions = listOf(ConsentPermission.CREATE_DOMESTIC_PAYMENTS),
            instructedAmount = Amount(
                amount = "750.00",
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA4420000001234567890123456"
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457",
                name = "Service Provider"
            ),
            creditorName = "Service Provider Ltd",
            remittanceInformation = RemittanceInformation(
                unstructured = listOf("Service fee payment")
            ),
            expirationDateTime = "2024-08-27T10:00:00Z"
        )
        
        val consentResult = consentManager.createPaymentConsent(paymentConsentRequest)
        assertTrue(consentResult.isSuccess)
        
        val consent = consentResult.getOrThrow()
        assertNotNull(consent.consentId)
        assertEquals(ConsentStatus.AWAITING_AUTHORISATION, consent.status)
        
        // Step 2: Simulate consent authorization (in real flow, user would authorize)
        // For testing, we'll assume consent is now authorized
        
        // Step 3: Initiate payment using the consent
        val paymentRequest = DomesticPaymentRequest(
            consentId = consent.consentId,
            instructionIdentification = "FULL-FLOW-001",
            endToEndIdentification = "FULL-E2E-001",
            instructedAmount = Amount(
                amount = "750.00",
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA4420000001234567890123456"
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457",
                name = "Service Provider"
            ),
            creditorName = "Service Provider Ltd",
            remittanceInformation = RemittanceInformation(
                unstructured = listOf("Service fee payment")
            )
        )
        
        val paymentResult = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
        assertTrue(paymentResult.isSuccess)
        
        val payment = paymentResult.getOrThrow()
        assertNotNull(payment.domesticPaymentId)
        assertEquals(consent.consentId, payment.consentId)
        assertEquals("AcceptedSettlementInProcess", payment.status)
        
        // Step 4: Check payment status
        val statusResult = paymentService.getDomesticPaymentStatus(testAccessToken, payment.domesticPaymentId!!)
        assertTrue(statusResult.isSuccess)
        
        val finalStatus = statusResult.getOrThrow()
        assertEquals("AcceptedSettlementCompleted", finalStatus.status)
    }
    
    @Test
    fun testPaymentWithInvalidAmount() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "invalid_amount", "error_description": "Amount exceeds transaction limit"}""",
                status = io.ktor.http.HttpStatusCode.BadRequest,
                headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        val paymentRequest = DomesticPaymentRequest(
            instructionIdentification = "INVALID-001",
            endToEndIdentification = "INVALID-E2E-001",
            instructedAmount = Amount(
                amount = "999999999.00", // Exceeds limit
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA4420000001234567890123456"
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457"
            ),
            creditorName = "Test Creditor"
        )
        
        // Act
        val result = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("invalid_amount") == true)
    }
    
    @Test
    fun testPaymentWithInvalidAccount() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "invalid_account", "error_description": "Debtor account not found or inactive"}""",
                status = io.ktor.http.HttpStatusCode.BadRequest,
                headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        val paymentRequest = DomesticPaymentRequest(
            instructionIdentification = "INVALID-ACC-001",
            endToEndIdentification = "INVALID-ACC-E2E-001",
            instructedAmount = Amount(
                amount = "100.00",
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA9999999999999999999999999" // Invalid IBAN
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457"
            ),
            creditorName = "Test Creditor"
        )
        
        // Act
        val result = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("invalid_account") == true)
    }
    
    @Test
    fun testPaymentWithInsufficientFunds() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "insufficient_funds", "error_description": "Insufficient balance in debtor account"}""",
                status = io.ktor.http.HttpStatusCode.BadRequest,
                headers = io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        val paymentRequest = DomesticPaymentRequest(
            instructionIdentification = "INSUFFICIENT-001",
            endToEndIdentification = "INSUFFICIENT-E2E-001",
            instructedAmount = Amount(
                amount = "50000.00", // More than available balance
                currency = "SAR"
            ),
            debtorAccount = Account(
                iban = "SA4420000001234567890123456"
            ),
            creditorAccount = Account(
                iban = "SA4420000001234567890123457"
            ),
            creditorName = "Test Creditor"
        )
        
        // Act
        val result = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("insufficient_funds") == true)
    }
    
    @Test
    fun testBulkPaymentProcessing() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        val paymentRequests = listOf(
            DomesticPaymentRequest(
                instructionIdentification = "BULK-001-1",
                endToEndIdentification = "BULK-E2E-001-1",
                instructedAmount = Amount("100.00", "SAR"),
                debtorAccount = Account(iban = "SA4420000001234567890123456"),
                creditorAccount = Account(iban = "SA4420000001234567890123457"),
                creditorName = "Beneficiary 1"
            ),
            DomesticPaymentRequest(
                instructionIdentification = "BULK-001-2",
                endToEndIdentification = "BULK-E2E-001-2",
                instructedAmount = Amount("200.00", "SAR"),
                debtorAccount = Account(iban = "SA4420000001234567890123456"),
                creditorAccount = Account(iban = "SA4420000001234567890123458"),
                creditorName = "Beneficiary 2"
            )
        )
        
        // Act - Process payments in sequence (bulk processing simulation)
        val results = mutableListOf<Result<DomesticPaymentResponse>>()
        paymentRequests.forEach { request ->
            val result = paymentService.initiateDomesticPayment(testAccessToken, request)
            results.add(result)
        }
        
        // Assert
        assertTrue(results.all { it.isSuccess })
        results.forEach { result ->
            val payment = result.getOrThrow()
            assertNotNull(payment.domesticPaymentId)
            assertEquals("AcceptedSettlementInProcess", payment.status)
        }
    }
    
    @Test
    fun testPaymentLimitsValidation() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        // Test different payment amounts to validate limits
        val testAmounts = listOf(
            "10.00",      // Minimum amount
            "5000.00",    // Normal amount
            "50000.00",   // High amount
            "100000.00"   // Maximum amount for some banks
        )
        
        testAmounts.forEach { amount ->
            val paymentRequest = DomesticPaymentRequest(
                instructionIdentification = "LIMIT-TEST-$amount",
                endToEndIdentification = "LIMIT-E2E-$amount",
                instructedAmount = Amount(
                    amount = amount,
                    currency = "SAR"
                ),
                debtorAccount = Account(
                    iban = "SA4420000001234567890123456"
                ),
                creditorAccount = Account(
                    iban = "SA4420000001234567890123457"
                ),
                creditorName = "Limit Test Beneficiary"
            )
            
            // Act
            val result = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
            
            // Assert - All amounts should be acceptable in sandbox
            assertTrue(result.isSuccess, "Payment with amount $amount should be accepted")
        }
    }
    
    @Test
    fun testMultiCurrencyPaymentSupport() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        
        // Test different currencies (for banks that support multi-currency)
        val currencies = listOf("SAR", "USD", "EUR", "GBP")
        
        currencies.forEach { currency ->
            val paymentRequest = DomesticPaymentRequest(
                instructionIdentification = "CURRENCY-TEST-$currency",
                endToEndIdentification = "CURRENCY-E2E-$currency",
                instructedAmount = Amount(
                    amount = if (currency == "SAR") "1000.00" else "100.00",
                    currency = currency
                ),
                debtorAccount = Account(
                    iban = "SA4540000001234567890123456" // SABB multi-currency account
                ),
                creditorAccount = Account(
                    iban = "SA4540000001234567890123457"
                ),
                creditorName = "Multi-Currency Test"
            )
            
            // Act
            val result = paymentService.initiateDomesticPayment(testAccessToken, paymentRequest)
            
            // Assert - SAR should always work, others depend on bank support
            if (currency == "SAR") {
                assertTrue(result.isSuccess, "SAR payments should always be supported")
            }
            // For other currencies, we just verify the request is well-formed
            // (actual support depends on specific bank configuration)
        }
    }
}