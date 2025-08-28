package code.yousef.dari.shared.data.network

import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Authentication Interceptor for Ktor
 * Automatically adds bearer tokens to requests and handles token refresh
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) {
    
    companion object : ClientPlugin<AuthInterceptor, AuthInterceptor> {
        override val key: AttributeKey<AuthInterceptor> = AttributeKey("AuthInterceptor")
        
        override fun prepare(block: AuthInterceptor.() -> Unit): AuthInterceptor {
            return AuthInterceptor(SimpleTokenProvider()).apply(block)
        }
        
        override fun install(plugin: AuthInterceptor, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val token = plugin.tokenProvider.getValidToken()
                if (token != null) {
                    context.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
                proceed()
            }
            
            scope.responsePipeline.intercept(HttpResponsePipeline.Receive) {
                val response = subject
                
                // Handle 401 Unauthorized - attempt token refresh
                if (response.status == HttpStatusCode.Unauthorized) {
                    val refreshed = plugin.tokenProvider.refreshTokenIfNeeded()
                    if (refreshed) {
                        // Retry the request with new token
                        val newToken = plugin.tokenProvider.getValidToken()
                        if (newToken != null) {
                            val request = context.request
                            val retryRequest = HttpRequestBuilder().apply {
                                takeFromWithExecutionContext(request)
                                headers.remove(HttpHeaders.Authorization)
                                headers.append(HttpHeaders.Authorization, "Bearer $newToken")
                            }
                            
                            // This would need custom retry logic implementation
                            // For now, we'll let the error propagate
                        }
                    }
                }
                
                proceed()
            }
        }
    }
}

/**
 * Token Provider interface for managing authentication tokens
 */
interface TokenProvider {
    suspend fun getValidToken(): String?
    suspend fun refreshTokenIfNeeded(): Boolean
    suspend fun clearTokens()
}

/**
 * Simple token provider implementation
 * In production, this would integrate with secure storage
 */
class SimpleTokenProvider : TokenProvider {
    private val mutex = Mutex()
    private var currentToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiryTime: Long = 0L
    
    override suspend fun getValidToken(): String? = mutex.withLock {
        if (isTokenExpired()) {
            null
        } else {
            currentToken
        }
    }
    
    override suspend fun refreshTokenIfNeeded(): Boolean = mutex.withLock {
        if (refreshToken == null || isTokenExpired()) {
            return@withLock false
        }
        
        // TODO: Implement actual token refresh logic
        // This would call the authentication service to refresh the token
        return@withLock true
    }
    
    override suspend fun clearTokens() = mutex.withLock {
        currentToken = null
        refreshToken = null
        tokenExpiryTime = 0L
    }
    
    suspend fun setTokens(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Int
    ) = mutex.withLock {
        this.currentToken = accessToken
        this.refreshToken = refreshToken
        this.tokenExpiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000L)
    }
    
    private fun isTokenExpired(): Boolean {
        return System.currentTimeMillis() >= tokenExpiryTime
    }
}

/**
 * Token Provider that integrates with SAMA Banking SDK
 */
class SamaTokenProvider(
    private val authService: code.yousef.dari.sama.interfaces.AuthenticationService,
    private val securityProvider: code.yousef.dari.sama.interfaces.SecurityProvider
) : TokenProvider {
    
    private val mutex = Mutex()
    private var cachedToken: String? = null
    private var cachedRefreshToken: String? = null
    private var tokenExpiryTime: Long = 0L
    
    override suspend fun getValidToken(): String? = mutex.withLock {
        if (isTokenExpired()) {
            // Try to refresh token automatically
            if (refreshTokenIfNeeded()) {
                return@withLock cachedToken
            }
            return@withLock null
        }
        return@withLock cachedToken
    }
    
    override suspend fun refreshTokenIfNeeded(): Boolean = mutex.withLock {
        val refreshToken = cachedRefreshToken ?: return@withLock false
        
        val tokenRequest = code.yousef.dari.sama.models.TokenRequest(
            grantType = code.yousef.dari.sama.models.GrantType.REFRESH_TOKEN,
            refreshToken = refreshToken,
            clientId = getStoredClientId(),
            clientSecret = getStoredClientSecret()
        )
        
        val result = authService.refreshToken(tokenRequest)
        
        return@withLock if (result.isSuccess) {
            val tokenResponse = result.getOrThrow()
            cachedToken = tokenResponse.accessToken
            cachedRefreshToken = tokenResponse.refreshToken ?: cachedRefreshToken
            tokenExpiryTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L)
            
            // Store tokens securely
            storeTokensSecurely(tokenResponse)
            true
        } else {
            // Refresh failed, clear tokens
            clearTokens()
            false
        }
    }
    
    override suspend fun clearTokens() = mutex.withLock {
        cachedToken = null
        cachedRefreshToken = null
        tokenExpiryTime = 0L
        
        // Clear from secure storage
        clearStoredTokens()
    }
    
    suspend fun setInitialTokens(tokenResponse: code.yousef.dari.sama.models.TokenResponse) = mutex.withLock {
        cachedToken = tokenResponse.accessToken
        cachedRefreshToken = tokenResponse.refreshToken
        tokenExpiryTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L)
        
        // Store tokens securely
        storeTokensSecurely(tokenResponse)
    }
    
    private fun isTokenExpired(): Boolean {
        // Add 5 minute buffer to prevent edge cases
        return System.currentTimeMillis() >= (tokenExpiryTime - 300_000)
    }
    
    private suspend fun storeTokensSecurely(tokenResponse: code.yousef.dari.sama.models.TokenResponse) {
        // TODO: Implement secure storage using SecurityProvider
        try {
            securityProvider.encryptData(
                "access_token", 
                tokenResponse.accessToken.toByteArray()
            )
            tokenResponse.refreshToken?.let { refresh ->
                securityProvider.encryptData(
                    "refresh_token", 
                    refresh.toByteArray()
                )
            }
        } catch (e: Exception) {
            // Log error but don't fail the token setting
            println("Failed to store tokens securely: ${e.message}")
        }
    }
    
    private suspend fun getStoredClientId(): String {
        return try {
            val encryptedData = securityProvider.decryptData("client_id")
            String(encryptedData.getOrThrow())
        } catch (e: Exception) {
            "" // Return empty if not found, should be handled by caller
        }
    }
    
    private suspend fun getStoredClientSecret(): String {
        return try {
            val encryptedData = securityProvider.decryptData("client_secret")
            String(encryptedData.getOrThrow())
        } catch (e: Exception) {
            "" // Return empty if not found, should be handled by caller
        }
    }
    
    private suspend fun clearStoredTokens() {
        try {
            securityProvider.deleteSecureData("access_token")
            securityProvider.deleteSecureData("refresh_token")
        } catch (e: Exception) {
            // Log error but don't fail the clearing
            println("Failed to clear stored tokens: ${e.message}")
        }
    }
}