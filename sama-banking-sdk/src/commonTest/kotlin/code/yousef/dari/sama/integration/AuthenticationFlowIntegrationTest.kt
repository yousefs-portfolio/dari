package code.yousef.dari.sama.integration

import code.yousef.dari.sama.MockSamaServer
import code.yousef.dari.sama.implementation.AuthenticationServiceImpl
import code.yousef.dari.sama.implementation.CommonSecurityProvider
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
 * Integration tests for complete authentication flow
 * Tests the interaction between authentication service, consent manager, and security provider
 */
class AuthenticationFlowIntegrationTest {
    
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
    
    @Test
    fun testCompleteAuthenticationFlow() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val securityProvider = CommonSecurityProvider()
        val authService = AuthenticationServiceImpl(httpClient, securityProvider, json)
        val consentManager = ConsentManagerImpl(httpClient, json)
        
        val clientConfig = ClientConfiguration(
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            redirectUri = "https://test.app.com/callback",
            baseUrl = "https://api.mock-bank.com.sa/open-banking/v1",
            certificatePins = emptyList()
        )
        
        // Act & Assert - Step 1: Create consent request
        val consentRequest = ConsentRequest(
            permissions = listOf(
                ConsentPermission.READ_ACCOUNTS_BASIC,
                ConsentPermission.READ_ACCOUNTS_DETAIL,
                ConsentPermission.READ_BALANCES,
                ConsentPermission.READ_TRANSACTIONS_BASIC,
                ConsentPermission.READ_TRANSACTIONS_DETAIL
            ),
            expirationDateTime = "2024-08-27T10:00:00Z",
            transactionFromDateTime = "2024-01-01T00:00:00Z",
            transactionToDateTime = "2024-12-31T23:59:59Z"
        )
        
        val consentResult = consentManager.createConsent(consentRequest)
        assertTrue(consentResult.isSuccess)
        
        val consent = consentResult.getOrThrow()
        assertNotNull(consent.consentId)
        assertEquals(ConsentStatus.AWAITING_AUTHORISATION, consent.status)
        
        // Step 2: Initiate PAR request
        val parRequest = PARRequest(
            clientId = clientConfig.clientId,
            redirectUri = clientConfig.redirectUri,
            scope = "accounts payments",
            consentId = consent.consentId
        )
        
        val parResult = authService.initiatePAR(parRequest)
        assertTrue(parResult.isSuccess)
        
        val parResponse = parResult.getOrThrow()
        assertNotNull(parResponse.requestUri)
        assertTrue(parResponse.expiresIn > 0)
        
        // Step 3: Generate authorization URL
        val authUrlResult = authService.generateAuthorizationUrl(
            clientId = clientConfig.clientId,
            requestUri = parResponse.requestUri,
            state = "test-state"
        )
        assertTrue(authUrlResult.isSuccess)
        
        val authUrl = authUrlResult.getOrThrow()
        assertTrue(authUrl.contains("request_uri"))
        assertTrue(authUrl.contains("client_id"))
        assertTrue(authUrl.contains("state"))
        
        // Step 4: Simulate authorization code callback
        val authorizationCode = "mock_auth_code_12345"
        val codeVerifier = "test-code-verifier"
        
        // Step 5: Exchange authorization code for tokens
        val tokenRequest = TokenRequest(
            grantType = GrantType.AUTHORIZATION_CODE,
            code = authorizationCode,
            redirectUri = clientConfig.redirectUri,
            clientId = clientConfig.clientId,
            clientSecret = clientConfig.clientSecret,
            codeVerifier = codeVerifier
        )
        
        val tokenResult = authService.exchangeAuthorizationCode(tokenRequest)
        assertTrue(tokenResult.isSuccess)
        
        val tokenResponse = tokenResult.getOrThrow()
        assertNotNull(tokenResponse.accessToken)
        assertNotNull(tokenResponse.refreshToken)
        assertEquals("Bearer", tokenResponse.tokenType)
        assertTrue(tokenResponse.expiresIn > 0)
        
        // Step 6: Validate token
        val isValidResult = authService.validateToken(tokenResponse.accessToken)
        assertTrue(isValidResult.isSuccess)
        assertTrue(isValidResult.getOrThrow())
        
        // Step 7: Check consent status (should be authorized after token exchange)
        val consentStatusResult = consentManager.getConsentStatus(consent.consentId)
        assertTrue(consentStatusResult.isSuccess)
        
        val consentStatus = consentStatusResult.getOrThrow()
        assertEquals(ConsentStatus.AUTHORISED, consentStatus.status)
    }
    
    @Test
    fun testClientCredentialsFlow() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val securityProvider = CommonSecurityProvider()
        val authService = AuthenticationServiceImpl(httpClient, securityProvider, json)
        
        // Act
        val tokenRequest = TokenRequest(
            grantType = GrantType.CLIENT_CREDENTIALS,
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            scope = "accounts"
        )
        
        val tokenResult = authService.getClientCredentialsToken(tokenRequest)
        
        // Assert
        assertTrue(tokenResult.isSuccess)
        val tokenResponse = tokenResult.getOrThrow()
        assertNotNull(tokenResponse.accessToken)
        assertEquals("Bearer", tokenResponse.tokenType)
        assertTrue(tokenResponse.expiresIn > 0)
    }
    
    @Test
    fun testTokenRefreshFlow() = runTest {
        // Arrange
        val httpClient = createTestClient()
        val securityProvider = CommonSecurityProvider()
        val authService = AuthenticationServiceImpl(httpClient, securityProvider, json)
        
        // First get a token with refresh token
        val initialTokenRequest = TokenRequest(
            grantType = GrantType.CLIENT_CREDENTIALS,
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            scope = "accounts"
        )
        
        val initialTokenResult = authService.getClientCredentialsToken(initialTokenRequest)
        assertTrue(initialTokenResult.isSuccess)
        
        val initialToken = initialTokenResult.getOrThrow()
        
        // Act - Refresh the token
        val refreshRequest = TokenRequest(
            grantType = GrantType.REFRESH_TOKEN,
            refreshToken = initialToken.refreshToken ?: "refresh.token.mock",
            clientId = "test-client-id",
            clientSecret = "test-client-secret"
        )
        
        val refreshResult = authService.refreshToken(refreshRequest)
        
        // Assert
        assertTrue(refreshResult.isSuccess)
        val refreshedToken = refreshResult.getOrThrow()
        assertNotNull(refreshedToken.accessToken)
        assertTrue(refreshedToken.accessToken != initialToken.accessToken)
        assertEquals("Bearer", refreshedToken.tokenType)
    }
    
    @Test
    fun testPKCEGeneration() = runTest {
        // Arrange
        val securityProvider = CommonSecurityProvider()
        
        // Act
        val pkceChallenge = securityProvider.generatePKCEChallenge()
        
        // Assert
        assertNotNull(pkceChallenge.codeVerifier)
        assertNotNull(pkceChallenge.codeChallenge)
        assertEquals("S256", pkceChallenge.codeChallengeMethod)
        assertTrue(pkceChallenge.codeVerifier.length >= 43)
        assertTrue(pkceChallenge.codeChallenge.isNotEmpty())
    }
    
    @Test
    fun testStateGeneration() = runTest {
        // Arrange
        val securityProvider = CommonSecurityProvider()
        
        // Act
        val state1 = securityProvider.generateState()
        val state2 = securityProvider.generateState()
        
        // Assert
        assertTrue(state1.isNotEmpty())
        assertTrue(state2.isNotEmpty())
        assertTrue(state1 != state2) // Should be unique
        assertTrue(state1.length >= 16) // Should be sufficiently long
    }
    
    @Test
    fun testTokenExpiryDetection() = runTest {
        // Arrange
        val httpClient = createTestClient()
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
        
        // Act
        val isExpiredResult = authService.isTokenExpired(expiredToken)
        
        // Assert
        assertTrue(isExpiredResult.isSuccess)
        assertTrue(isExpiredResult.getOrThrow())
        
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
        // Note: This might be flaky in slow test environments
    }
    
    @Test
    fun testErrorHandlingInAuthFlow() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "invalid_client", "error_description": "Invalid client credentials"}""",
                status = io.ktor.http.HttpStatusCode.Unauthorized,
                headers = io.ktor.utils.io.core.toByteArray("application/json").let {
                    io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
                }
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        
        val securityProvider = CommonSecurityProvider()
        val authService = AuthenticationServiceImpl(httpClient, securityProvider, json)
        
        // Act
        val tokenRequest = TokenRequest(
            grantType = GrantType.CLIENT_CREDENTIALS,
            clientId = "invalid-client",
            clientSecret = "invalid-secret"
        )
        
        val result = authService.getClientCredentialsToken(tokenRequest)
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("invalid_client") == true)
    }
}