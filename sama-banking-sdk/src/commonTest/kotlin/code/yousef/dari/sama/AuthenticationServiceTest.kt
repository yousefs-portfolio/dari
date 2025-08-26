package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.AuthenticationService
import code.yousef.dari.sama.models.AuthenticationToken
import code.yousef.dari.sama.models.ParRequest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test for AuthenticationService implementation - TDD approach
 * Tests PAR request initiation, PKCE challenge generation, token exchange, and refresh
 */
class AuthenticationServiceTest {

    private class MockAuthenticationService : AuthenticationService {
        override suspend fun initiateParRequest(
            clientId: String,
            redirectUri: String,
            scope: String,
            state: String?
        ): Result<ParRequest> {
            return if (clientId.isNotBlank() && redirectUri.isNotBlank()) {
                Result.success(
                    ParRequest(
                        requestUri = "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c",
                        expiresIn = 90
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid client credentials"))
            }
        }

        override suspend fun generateAuthorizationUrl(
            requestUri: String,
            clientId: String
        ): Result<String> {
            return if (requestUri.startsWith("urn:ietf:params:oauth:request_uri:")) {
                Result.success("https://bank.example.com/oauth/authorize?request_uri=$requestUri&client_id=$clientId")
            } else {
                Result.failure(IllegalArgumentException("Invalid request URI"))
            }
        }

        override fun generatePkceChallenge(): Pair<String, String> {
            val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
            val challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
            return Pair(challenge, verifier)
        }

        override suspend fun exchangeCodeForToken(
            code: String,
            codeVerifier: String,
            clientId: String,
            redirectUri: String
        ): Result<AuthenticationToken> {
            return if (code.isNotBlank() && codeVerifier.isNotBlank()) {
                Result.success(
                    AuthenticationToken(
                        accessToken = "ya29.a0ARrdaM9example",
                        tokenType = "Bearer",
                        expiresIn = 3600,
                        refreshToken = "1//04example",
                        scope = "accounts payments"
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid authorization code"))
            }
        }

        override suspend fun clientCredentialsGrant(
            clientId: String,
            clientSecret: String,
            scope: String
        ): Result<AuthenticationToken> {
            return if (clientId.isNotBlank() && clientSecret.isNotBlank()) {
                Result.success(
                    AuthenticationToken(
                        accessToken = "ya29.client-credentials-example",
                        tokenType = "Bearer",
                        expiresIn = 3600,
                        scope = scope
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid client credentials"))
            }
        }

        override suspend fun refreshToken(
            refreshToken: String,
            clientId: String
        ): Result<AuthenticationToken> {
            return if (refreshToken.isNotBlank()) {
                Result.success(
                    AuthenticationToken(
                        accessToken = "ya29.refreshed-example",
                        tokenType = "Bearer",
                        expiresIn = 3600,
                        refreshToken = refreshToken,
                        scope = "accounts payments"
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid refresh token"))
            }
        }

        override suspend fun isTokenValid(token: String): Boolean {
            return token.isNotBlank() && token.startsWith("ya29.")
        }

        override fun isTokenExpiringSoon(token: AuthenticationToken): Boolean {
            return token.isExpiringSoon()
        }
    }

    private val authService = MockAuthenticationService()

    @Test
    fun `should initiate PAR request successfully`() = runTest {
        // Given
        val clientId = "test-client-id"
        val redirectUri = "https://app.example.com/callback"
        val scope = "accounts payments"
        val state = "random-state-value"

        // When
        val result = authService.initiateParRequest(clientId, redirectUri, scope, state)

        // Then
        assertTrue(result.isSuccess, "PAR request should succeed")
        val parRequest = result.getOrNull()
        assertNotNull(parRequest, "PAR request should not be null")
        assertTrue(parRequest.requestUri.startsWith("urn:ietf:params:oauth:request_uri:"))
        assertEquals(90, parRequest.expiresIn)
    }

    @Test
    fun `should fail PAR request with invalid client ID`() = runTest {
        // Given
        val clientId = ""
        val redirectUri = "https://app.example.com/callback"

        // When
        val result = authService.initiateParRequest(clientId, redirectUri)

        // Then
        assertTrue(result.isFailure, "PAR request should fail with empty client ID")
    }

    @Test
    fun `should generate authorization URL successfully`() = runTest {
        // Given
        val requestUri = "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c"
        val clientId = "test-client-id"

        // When
        val result = authService.generateAuthorizationUrl(requestUri, clientId)

        // Then
        assertTrue(result.isSuccess, "Authorization URL generation should succeed")
        val authUrl = result.getOrNull()
        assertNotNull(authUrl, "Authorization URL should not be null")
        assertTrue(authUrl.contains("request_uri=$requestUri"))
        assertTrue(authUrl.contains("client_id=$clientId"))
    }

    @Test
    fun `should generate PKCE challenge and verifier`() {
        // When
        val (challenge, verifier) = authService.generatePkceChallenge()

        // Then
        assertTrue(challenge.isNotBlank(), "Challenge should not be blank")
        assertTrue(verifier.isNotBlank(), "Verifier should not be blank")
        assertTrue(challenge.length >= 43, "Challenge should be at least 43 characters")
        assertTrue(verifier.length >= 43, "Verifier should be at least 43 characters")
    }

    @Test
    fun `should exchange authorization code for token`() = runTest {
        // Given
        val code = "authorization-code-example"
        val codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val clientId = "test-client-id"
        val redirectUri = "https://app.example.com/callback"

        // When
        val result = authService.exchangeCodeForToken(code, codeVerifier, clientId, redirectUri)

        // Then
        assertTrue(result.isSuccess, "Token exchange should succeed")
        val token = result.getOrNull()
        assertNotNull(token, "Token should not be null")
        assertTrue(token.accessToken.startsWith("ya29."))
        assertEquals("Bearer", token.tokenType)
        assertEquals(3600, token.expiresIn)
        assertNotNull(token.refreshToken, "Refresh token should be present")
        assertEquals("accounts payments", token.scope)
    }

    @Test
    fun `should perform client credentials grant`() = runTest {
        // Given
        val clientId = "test-client-id"
        val clientSecret = "test-client-secret"
        val scope = "accounts"

        // When
        val result = authService.clientCredentialsGrant(clientId, clientSecret, scope)

        // Then
        assertTrue(result.isSuccess, "Client credentials grant should succeed")
        val token = result.getOrNull()
        assertNotNull(token, "Token should not be null")
        assertTrue(token.accessToken.startsWith("ya29."))
        assertEquals("Bearer", token.tokenType)
        assertEquals(3600, token.expiresIn)
        assertEquals(scope, token.scope)
        assertNull(token.refreshToken, "Refresh token should be null for client credentials")
    }

    @Test
    fun `should refresh access token`() = runTest {
        // Given
        val refreshToken = "1//04example"
        val clientId = "test-client-id"

        // When
        val result = authService.refreshToken(refreshToken, clientId)

        // Then
        assertTrue(result.isSuccess, "Token refresh should succeed")
        val token = result.getOrNull()
        assertNotNull(token, "Token should not be null")
        assertTrue(token.accessToken.startsWith("ya29."))
        assertEquals("Bearer", token.tokenType)
        assertEquals(3600, token.expiresIn)
        assertEquals(refreshToken, token.refreshToken)
    }

    @Test
    fun `should validate token correctly`() = runTest {
        // Given
        val validToken = "ya29.a0ARrdaM9example"
        val invalidToken = "invalid-token"

        // When & Then
        assertTrue(authService.isTokenValid(validToken), "Valid token should pass validation")
        assertFalse(authService.isTokenValid(invalidToken), "Invalid token should fail validation")
        assertFalse(authService.isTokenValid(""), "Empty token should fail validation")
    }

    @Test
    fun `should detect token expiry correctly`() = runTest {
        // Given
        val currentTime = Clock.System.now()
        val expiringToken = AuthenticationToken(
            accessToken = "ya29.expiring-example",
            tokenType = "Bearer",
            expiresIn = 60, // 1 minute
            issuedAt = currentTime
        )
        val validToken = AuthenticationToken(
            accessToken = "ya29.valid-example",
            tokenType = "Bearer",
            expiresIn = 3600, // 1 hour
            issuedAt = currentTime
        )

        // When & Then
        assertTrue(authService.isTokenExpiringSoon(expiringToken), "Token expiring in 1 minute should be detected")
        assertFalse(authService.isTokenExpiringSoon(validToken), "Token expiring in 1 hour should not be detected")
    }
}