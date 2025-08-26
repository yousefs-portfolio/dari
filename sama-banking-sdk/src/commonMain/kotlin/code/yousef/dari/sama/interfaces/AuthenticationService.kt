package code.yousef.dari.sama.interfaces

import code.yousef.dari.sama.models.AuthenticationToken
import code.yousef.dari.sama.models.ParRequest

/**
 * Authentication service following SAMA OAuth 2.0 with FAPI 1 Advanced
 * Implements Pushed Authorization Request (PAR) as required by SAMA
 */
interface AuthenticationService {
    
    /**
     * Initiate Pushed Authorization Request (PAR) as required by SAMA
     * @param clientId The registered client identifier
     * @param redirectUri The registered redirect URI
     * @param scope Requested permissions scope
     * @param state Anti-CSRF state parameter
     * @return Request URI for authorization
     */
    suspend fun initiateParRequest(
        clientId: String,
        redirectUri: String,
        scope: String = "accounts payments",
        state: String? = null
    ): Result<ParRequest>
    
    /**
     * Generate authorization URL using request_uri from PAR
     * @param requestUri The request URI returned from PAR
     * @param clientId The registered client identifier
     * @return Authorization URL for user consent
     */
    suspend fun generateAuthorizationUrl(
        requestUri: String,
        clientId: String
    ): Result<String>
    
    /**
     * Generate PKCE challenge and verifier pair
     * @return Pair of (challenge, verifier)
     */
    fun generatePkceChallenge(): Pair<String, String>
    
    /**
     * Exchange authorization code for access token
     * @param code Authorization code from callback
     * @param codeVerifier PKCE code verifier
     * @param clientId The registered client identifier
     * @param redirectUri Must match the redirect URI used in PAR
     * @return Authentication token response
     */
    suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String
    ): Result<AuthenticationToken>
    
    /**
     * Perform client credentials grant for service-to-service authentication
     * @param clientId The registered client identifier
     * @param clientSecret The client secret
     * @param scope Requested permissions scope
     * @return Authentication token response
     */
    suspend fun clientCredentialsGrant(
        clientId: String,
        clientSecret: String,
        scope: String = "accounts"
    ): Result<AuthenticationToken>
    
    /**
     * Refresh access token using refresh token
     * @param refreshToken The refresh token
     * @param clientId The registered client identifier
     * @return New authentication token response
     */
    suspend fun refreshToken(
        refreshToken: String,
        clientId: String
    ): Result<AuthenticationToken>
    
    /**
     * Check if access token is valid and not expired
     * @param token The access token to validate
     * @return true if token is valid
     */
    suspend fun isTokenValid(token: String): Boolean
    
    /**
     * Detect if token is about to expire (within 5 minutes)
     * @param token Authentication token to check
     * @return true if token will expire soon
     */
    fun isTokenExpiringSoon(token: AuthenticationToken): Boolean
}