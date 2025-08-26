package code.yousef.dari.sama.models

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable

/**
 * PAR (Pushed Authorization Request) response model
 */
@Serializable
data class ParRequest(
    val requestUri: String,
    val expiresIn: Int // seconds
)

/**
 * Authentication token response following OAuth 2.0
 */
@Serializable
data class AuthenticationToken(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int, // seconds
    val refreshToken: String? = null,
    val scope: String? = null,
    val issuedAt: Instant = Clock.System.now()
) {
    /**
     * Check if token is expired
     */
    fun isExpired(): Boolean {
        val now = Clock.System.now()
        val expirationTime = issuedAt.plus(expiresIn.seconds)
        return now >= expirationTime
    }
    
    /**
     * Check if token expires within the next 5 minutes
     */
    fun isExpiringSoon(): Boolean {
        val now = Clock.System.now()
        val expirationTime = issuedAt.plus(expiresIn.seconds)
        val fiveMinutesFromNow = now.plus(5.minutes)
        return fiveMinutesFromNow >= expirationTime
    }
}

/**
 * Token storage interface for secure persistence
 */
interface TokenStorage {
    suspend fun storeToken(token: AuthenticationToken): Result<Unit>
    suspend fun retrieveToken(): Result<AuthenticationToken?>
    suspend fun deleteToken(): Result<Unit>
}