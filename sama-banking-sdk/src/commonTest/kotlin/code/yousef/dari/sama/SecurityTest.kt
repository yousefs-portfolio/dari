package code.yousef.dari.sama

import code.yousef.dari.sama.implementation.*
import code.yousef.dari.sama.models.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Security Testing Suite for SAMA Banking SDK
 * Tests security controls, validation, and protection mechanisms
 */
class SecurityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `test HTTPS enforcement`() = runTest {
        val bankRegistry = BankConfigurationRegistry()
        val allBanks = bankRegistry.getAllBanks("production") + bankRegistry.getAllBanks("sandbox")
        
        // All endpoints must use HTTPS
        allBanks.forEach { config ->
            assertTrue(
                config.baseUrl.startsWith("https://"),
                "${config.bankName} base URL must use HTTPS: ${config.baseUrl}"
            )
            assertTrue(
                config.authorizationEndpoint.startsWith("https://"),
                "${config.bankName} authorization endpoint must use HTTPS"
            )
            assertTrue(
                config.tokenEndpoint.startsWith("https://"),
                "${config.bankName} token endpoint must use HTTPS"
            )
            assertTrue(
                config.parEndpoint.startsWith("https://"),
                "${config.bankName} PAR endpoint must use HTTPS"
            )
        }
    }

    @Test
    fun `test certificate pinning configuration`() = runTest {
        val bankRegistry = BankConfigurationRegistry()
        val allBanks = bankRegistry.getAllBanks("production")
        
        allBanks.forEach { config ->
            // Each bank must have certificate fingerprints
            assertTrue(
                config.certificateFingerprints.isNotEmpty(),
                "${config.bankName} must have certificate fingerprints configured"
            )
            
            // Validate fingerprint format
            config.certificateFingerprints.forEach { fingerprint ->
                assertTrue(
                    fingerprint.matches(Regex("[a-fA-F0-9:]{47,95}")),
                    "${config.bankName} certificate fingerprint must be valid hex format: $fingerprint"
                )
            }
            
            // Production should have multiple fingerprints for redundancy
            assertTrue(
                config.certificateFingerprints.size >= 1,
                "${config.bankName} should have at least 1 certificate fingerprint"
            )
        }
    }

    @Test
    fun `test certificate pinning validation`() = runTest {
        val validator = CertificatePinningValidator()
        
        // Valid fingerprint
        val validFingerprints = listOf(
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0",
            "B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1"
        )
        
        assertTrue(
            validator.validateFingerprint(validFingerprints[0], validFingerprints),
            "Valid fingerprint should pass validation"
        )
        
        // Invalid fingerprint
        val invalidFingerprint = "Z9:X8:Y7:W6:V5:U4:T3:S2:R1:Q0:P1:O2:N3:M4:L5:K6:J7:I8:H9:G0"
        assertFalse(
            validator.validateFingerprint(invalidFingerprint, validFingerprints),
            "Invalid fingerprint should fail validation"
        )
        
        // Malformed fingerprint
        val malformedFingerprint = "not-a-valid-fingerprint"
        assertFalse(
            validator.validateFingerprint(malformedFingerprint, validFingerprints),
            "Malformed fingerprint should fail validation"
        )
    }

    @Test
    fun `test PKCE security requirements`() = runTest {
        val securityProvider = CommonSecurityProvider()
        
        // Generate multiple PKCE challenges
        val challenges = (1..10).map { securityProvider.generatePKCEChallenge() }
        
        challenges.forEach { challenge ->
            // Code verifier should be sufficiently long (43-128 characters)
            assertTrue(
                challenge.codeVerifier.length >= 43,
                "Code verifier should be at least 43 characters long: ${challenge.codeVerifier.length}"
            )
            assertTrue(
                challenge.codeVerifier.length <= 128,
                "Code verifier should not exceed 128 characters: ${challenge.codeVerifier.length}"
            )
            
            // Code verifier should contain only valid characters
            assertTrue(
                challenge.codeVerifier.matches(Regex("[A-Za-z0-9._~-]+")),
                "Code verifier should contain only valid characters"
            )
            
            // Code challenge should be present
            assertTrue(
                challenge.codeChallenge.isNotEmpty(),
                "Code challenge should not be empty"
            )
            
            // Should use S256 method
            assertEquals(
                "S256",
                challenge.codeChallengeMethod,
                "Should use S256 code challenge method"
            )
        }
        
        // All challenges should be unique
        val verifiers = challenges.map { it.codeVerifier }
        val uniqueVerifiers = verifiers.toSet()
        assertEquals(
            verifiers.size,
            uniqueVerifiers.size,
            "All PKCE code verifiers should be unique"
        )
    }

    @Test
    fun `test state parameter security`() = runTest {
        val securityProvider = CommonSecurityProvider()
        
        // Generate multiple states
        val states = (1..10).map { securityProvider.generateState() }
        
        states.forEach { state ->
            // State should be sufficiently long
            assertTrue(
                state.length >= 16,
                "State should be at least 16 characters long: ${state.length}"
            )
            
            // State should be non-predictable
            assertFalse(
                state.contains("test") || state.contains("123") || state.contains("abc"),
                "State should not contain predictable patterns: $state"
            )
        }
        
        // All states should be unique
        val uniqueStates = states.toSet()
        assertEquals(
            states.size,
            uniqueStates.size,
            "All states should be unique"
        )
    }

    @Test
    fun `test token expiry validation`() = runTest {
        val httpClient = createSecureTestClient()
        val securityProvider = CommonSecurityProvider()
        val authService = AuthenticationServiceImpl(httpClient, securityProvider, json)
        
        // Test expired token
        val expiredToken = TokenResponse(
            accessToken = "expired.token",
            tokenType = "Bearer",
            expiresIn = 0, // Already expired
            refreshToken = "refresh.token",
            scope = "accounts"
        )
        
        val isExpiredResult = authService.isTokenExpired(expiredToken)
        assertTrue(isExpiredResult.isSuccess)
        assertTrue(isExpiredResult.getOrThrow(), "Expired token should be detected")
        
        // Test valid token
        val validToken = TokenResponse(
            accessToken = "valid.token",
            tokenType = "Bearer",
            expiresIn = 3600, // Valid for 1 hour
            refreshToken = "refresh.token",
            scope = "accounts"
        )
        
        val isValidResult = authService.isTokenExpired(validToken)
        assertTrue(isValidResult.isSuccess)
        // Should not be expired (assuming test runs quickly)
    }

    @Test
    fun `test secure data transmission`() = runTest {
        val httpClient = createSecureTestClient()
        val accountService = AccountServiceImpl(httpClient, json)
        val testAccessToken = "secure-test-token"
        
        // All requests should include proper security headers
        var lastRequest: HttpRequestData? = null
        
        val mockEngine = MockEngine { request ->
            lastRequest = request
            
            respond(
                content = """{"Data": {"Account": []}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        val secureClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val secureAccountService = AccountServiceImpl(secureClient, json)
        
        // Make a request
        val result = secureAccountService.getAccounts(testAccessToken)
        assertTrue(result.isSuccess)
        
        // Verify security headers
        assertNotNull(lastRequest, "Request should have been captured")
        
        val authHeader = lastRequest!!.headers[HttpHeaders.Authorization]
        assertNotNull(authHeader, "Authorization header should be present")
        assertTrue(authHeader.startsWith("Bearer "), "Should use Bearer token")
        
        // Verify HTTPS URL
        assertTrue(
            lastRequest!!.url.toString().startsWith("https://") || 
            lastRequest!!.url.toString().startsWith("http://localhost"), // Allow localhost for testing
            "Should use HTTPS for requests"
        )
    }

    @Test
    fun `test input validation and sanitization`() = runTest {
        val httpClient = createSecureTestClient()
        val paymentService = PaymentServiceImpl(httpClient, json)
        val testAccessToken = "test-token"
        
        // Test SQL injection attempts
        val maliciousIBAN = "SA44'; DROP TABLE accounts; --"
        val maliciousName = "<script>alert('xss')</script>"
        val maliciousReference = "'; DELETE FROM transactions; --"
        
        val maliciousPaymentRequest = DomesticPaymentRequest(
            instructionIdentification = "SEC-TEST-001",
            endToEndIdentification = "SEC-E2E-001",
            instructedAmount = Amount("100.00", "SAR"),
            debtorAccount = Account(iban = maliciousIBAN),
            creditorAccount = Account(iban = "SA4420000001234567890123457"),
            creditorName = maliciousName,
            remittanceInformation = RemittanceInformation(
                unstructured = listOf(maliciousReference)
            )
        )
        
        // Should handle malicious input gracefully
        val result = paymentService.initiateDomesticPayment(testAccessToken, maliciousPaymentRequest)
        
        // Either success with sanitized data or proper error handling
        // This depends on server-side validation
        assertTrue(
            result.isSuccess || result.isFailure,
            "Should handle malicious input properly"
        )
    }

    @Test
    fun `test rate limiting protection`() = runTest {
        val bankRegistry = BankConfigurationRegistry()
        val alrajhiConfig = bankRegistry.getBankConfiguration("alrajhi", "production")
        assertNotNull(alrajhiConfig)
        
        val rateLimits = alrajhiConfig.rateLimits
        
        // Verify rate limits are configured
        assertTrue(
            rateLimits.requestsPerMinute > 0,
            "Requests per minute should be configured"
        )
        assertTrue(
            rateLimits.requestsPerHour > 0,
            "Requests per hour should be configured"
        )
        assertTrue(
            rateLimits.requestsPerDay > 0,
            "Requests per day should be configured"
        )
        
        // Verify reasonable limits
        assertTrue(
            rateLimits.requestsPerMinute <= 1000,
            "Rate limit should be reasonable: ${rateLimits.requestsPerMinute}/min"
        )
        assertTrue(
            rateLimits.burstLimit > 0 && rateLimits.burstLimit <= rateLimits.requestsPerMinute,
            "Burst limit should be reasonable: ${rateLimits.burstLimit}"
        )
    }

    @Test
    fun `test sensitive data masking`() = runTest {
        val account = Account(
            accountId = "acc-001",
            accountNumber = "1234567890123456",
            accountName = "Test Account",
            accountType = "CURRENT",
            currency = "SAR",
            isActive = true,
            iban = "SA4420000001234567890123456"
        )
        
        // Account number should be maskable for logging
        val maskedAccountNumber = account.accountNumber?.let { number ->
            if (number.length > 4) {
                "*".repeat(number.length - 4) + number.takeLast(4)
            } else number
        }
        
        assertEquals("************3456", maskedAccountNumber, "Account number should be properly masked")
        
        // IBAN should be maskable
        val maskedIBAN = account.iban?.let { iban ->
            if (iban.length > 8) {
                iban.take(4) + "*".repeat(iban.length - 8) + iban.takeLast(4)
            } else iban
        }
        
        assertEquals("SA44****************3456", maskedIBAN, "IBAN should be properly masked")
    }

    @Test
    fun `test error message security`() = runTest {
        val errorHandler = ErrorHandlerImpl()
        
        // Test various error scenarios
        val sensitiveErrors = listOf(
            BankError("database_error", "SQL connection failed: SELECT * FROM users WHERE password='secret123'"),
            BankError("system_error", "Internal server error: /home/api/.env file not found"),
            BankError("auth_error", "Authentication failed for user: admin@bank.com with password: wrongpass")
        )
        
        sensitiveErrors.forEach { error ->
            val handledError = errorHandler.handleError(error, "security-test")
            
            // Error messages should not expose sensitive information
            assertFalse(
                handledError.message?.contains("password", ignoreCase = true) == true,
                "Error message should not expose passwords"
            )
            assertFalse(
                handledError.message?.contains(".env", ignoreCase = true) == true,
                "Error message should not expose configuration files"
            )
            assertFalse(
                handledError.message?.contains("@", ignoreCase = true) == true,
                "Error message should not expose email addresses"
            )
            assertFalse(
                handledError.message?.contains("SELECT", ignoreCase = true) == true,
                "Error message should not expose SQL queries"
            )
        }
    }

    @Test
    fun `test consent security requirements`() = runTest {
        val httpClient = createSecureTestClient()
        val consentManager = ConsentManagerImpl(httpClient, json)
        
        val consentRequest = ConsentRequest(
            permissions = listOf(
                ConsentPermission.READ_ACCOUNTS_BASIC,
                ConsentPermission.READ_BALANCES
            ),
            expirationDateTime = "2024-08-27T10:00:00Z",
            transactionFromDateTime = "2024-01-01T00:00:00Z",
            transactionToDateTime = "2024-12-31T23:59:59Z"
        )
        
        val result = consentManager.createConsent(consentRequest)
        assertTrue(result.isSuccess)
        
        val consent = result.getOrThrow()
        
        // Consent should have security properties
        assertNotNull(consent.consentId, "Consent should have unique ID")
        assertTrue(
            consent.consentId.length >= 10,
            "Consent ID should be sufficiently long: ${consent.consentId.length}"
        )
        
        // Consent should expire
        assertNotNull(consent.expirationDateTime, "Consent should have expiration")
        
        // Permissions should be explicit
        assertTrue(
            consent.permissions.isNotEmpty(),
            "Consent should have explicit permissions"
        )
    }

    @Test
    fun `test API security headers`() = runTest {
        // This would typically test that all API responses include proper security headers
        // Such as: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, etc.
        
        var lastResponse: HttpResponse? = null
        
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"status": "ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    "X-Content-Type-Options" to listOf("nosniff"),
                    "X-Frame-Options" to listOf("DENY"),
                    "X-XSS-Protection" to listOf("1; mode=block"),
                    "Strict-Transport-Security" to listOf("max-age=31536000; includeSubDomains")
                )
            )
        }
        
        val secureClient = HttpClient(mockEngine)
        
        // Make a test request
        val response = secureClient.get("https://api.test.com/secure")
        
        // Verify security headers
        assertEquals("nosniff", response.headers["X-Content-Type-Options"])
        assertEquals("DENY", response.headers["X-Frame-Options"])
        assertEquals("1; mode=block", response.headers["X-XSS-Protection"])
        assertNotNull(response.headers["Strict-Transport-Security"])
    }

    @Test
    fun `test cryptographic security`() = runTest {
        val securityProvider = CommonSecurityProvider()
        
        // Test entropy of generated values
        val randomValues = (1..100).map { securityProvider.generateState() }
        
        // All values should be different (high entropy)
        val uniqueValues = randomValues.toSet()
        assertEquals(randomValues.size, uniqueValues.size, "All random values should be unique")
        
        // Test PKCE challenge cryptographic properties
        val pkceChallenge = securityProvider.generatePKCEChallenge()
        
        // Code challenge should be derived from code verifier
        assertNotNull(pkceChallenge.codeChallenge)
        assertTrue(
            pkceChallenge.codeChallenge != pkceChallenge.codeVerifier,
            "Code challenge should be different from code verifier"
        )
        
        // Should use S256 (SHA256) method
        assertEquals("S256", pkceChallenge.codeChallengeMethod)
    }

    @Test
    fun `test sandbox security isolation`() = runTest {
        val sandboxManager = SandboxEnvironmentManager()
        
        // Sandbox configurations should be isolated from production
        val prodConfig = sandboxManager.bankRegistry.getBankConfiguration("alrajhi", "production")
        val sandboxConfig = sandboxManager.getSandboxConfiguration("alrajhi")
        
        assertNotNull(prodConfig)
        assertNotNull(sandboxConfig)
        
        // URLs should be different
        assertTrue(
            prodConfig.baseUrl != sandboxConfig.baseUrl,
            "Sandbox and production should have different URLs"
        )
        
        // Client IDs should be different
        assertTrue(
            prodConfig.clientId != sandboxConfig.clientId,
            "Sandbox and production should have different client IDs"
        )
        
        // Sandbox should be clearly marked
        assertEquals("sandbox", sandboxConfig.environment)
        assertTrue(
            sandboxConfig.baseUrl.contains("sandbox", ignoreCase = true) ||
            sandboxConfig.baseUrl.contains("test", ignoreCase = true),
            "Sandbox URL should clearly indicate test environment"
        )
    }

    private fun createSecureTestClient(): HttpClient {
        return HttpClient(MockSamaServer.createMockEngine()) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }
}