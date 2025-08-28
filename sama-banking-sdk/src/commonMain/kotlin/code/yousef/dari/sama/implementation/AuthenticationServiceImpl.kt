package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.AuthenticationService
import code.yousef.dari.sama.interfaces.SecurityProvider
import code.yousef.dari.sama.models.AuthenticationToken
import code.yousef.dari.sama.models.ParRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * SAMA-compliant Authentication Service Implementation
 * Implements OAuth 2.0 with FAPI 1 Advanced security profile
 * Supports PAR (Pushed Authorization Request) as required by SAMA
 */
class AuthenticationServiceImpl(
    private val httpClient: HttpClient,
    private val securityProvider: SecurityProvider,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AuthenticationService {

    @Serializable
    private data class ParResponse(
        val request_uri: String,
        val expires_in: Int
    )

    @Serializable
    private data class TokenResponse(
        val access_token: String,
        val token_type: String = "Bearer",
        val expires_in: Int,
        val refresh_token: String? = null,
        val scope: String? = null
    )

    override suspend fun initiateParRequest(
        clientId: String,
        redirectUri: String,
        scope: String,
        state: String?
    ): Result<ParRequest> {
        return try {
            val (challenge, verifier) = generatePkceChallenge()
            
            // Store the code verifier for later use in token exchange
            securityProvider.storeSecurely("pkce_verifier_$clientId", verifier)

            val params = Parameters.build {
                append("response_type", "code")
                append("client_id", clientId)
                append("redirect_uri", redirectUri)
                append("scope", scope)
                append("code_challenge", challenge)
                append("code_challenge_method", "S256")
                state?.let { append("state", it) }
                // FAPI 1 Advanced requirements
                append("response_mode", "jwt")
                append("nonce", generateNonce())
            }

            val response = httpClient.post("$baseUrl/oauth/par") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(params))
                header("Accept", "application/json")
            }

            if (response.status == HttpStatusCode.Created) {
                val parResponse = response.body<ParResponse>()
                Result.success(
                    ParRequest(
                        requestUri = parResponse.request_uri,
                        expiresIn = parResponse.expires_in
                    )
                )
            } else {
                Result.failure(Exception("PAR request failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateAuthorizationUrl(
        requestUri: String,
        clientId: String
    ): Result<String> {
        return try {
            if (!requestUri.startsWith("urn:ietf:params:oauth:request_uri:")) {
                return Result.failure(IllegalArgumentException("Invalid request URI format"))
            }

            val authUrl = URLBuilder("$baseUrl/oauth/authorize").apply {
                parameters.append("request_uri", requestUri)
                parameters.append("client_id", clientId)
            }.build()

            Result.success(authUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun generatePkceChallenge(): Pair<String, String> {
        // Generate code verifier (128 random bytes, base64url encoded)
        val codeVerifier = ByteArray(128).apply {
            Random.Default.nextBytes(this)
        }.let { bytes ->
            // Convert to base64url encoding
            val base64 = bytesToBase64(bytes)
            base64.replace("+", "-").replace("/", "_").trimEnd('=')
        }

        // Generate code challenge (SHA256 of verifier, base64url encoded)
        val codeChallenge = securityProvider.createSha256Hash(codeVerifier)
            .replace("+", "-")
            .replace("/", "_")
            .trimEnd('=')

        return Pair(codeChallenge, codeVerifier)
    }

    private fun bytesToBase64(bytes: ByteArray): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val padding = "="
        
        var result = ""
        var i = 0
        
        while (i < bytes.size) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b3 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            
            val combined = (b1 shl 16) or (b2 shl 8) or b3
            
            result += chars[(combined shr 18) and 0x3F]
            result += chars[(combined shr 12) and 0x3F]
            result += if (i + 1 < bytes.size) chars[(combined shr 6) and 0x3F] else padding
            result += if (i + 2 < bytes.size) chars[combined and 0x3F] else padding
            
            i += 3
        }
        
        return result
    }

    override suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String
    ): Result<AuthenticationToken> {
        return try {
            val params = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", clientId)
                append("code_verifier", codeVerifier)
            }

            val response = httpClient.post("$baseUrl/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(params))
                header("Accept", "application/json")
                // FAPI 1 Advanced requires client authentication
                header("Authorization", "Basic ${encodeClientCredentials(clientId, "")}")
            }

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                Result.success(
                    AuthenticationToken(
                        accessToken = tokenResponse.access_token,
                        tokenType = tokenResponse.token_type,
                        expiresIn = tokenResponse.expires_in,
                        refreshToken = tokenResponse.refresh_token,
                        scope = tokenResponse.scope
                    )
                )
            } else {
                Result.failure(Exception("Token exchange failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clientCredentialsGrant(
        clientId: String,
        clientSecret: String,
        scope: String
    ): Result<AuthenticationToken> {
        return try {
            val params = Parameters.build {
                append("grant_type", "client_credentials")
                append("scope", scope)
            }

            val response = httpClient.post("$baseUrl/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(params))
                header("Accept", "application/json")
                header("Authorization", "Basic ${encodeClientCredentials(clientId, clientSecret)}")
            }

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                Result.success(
                    AuthenticationToken(
                        accessToken = tokenResponse.access_token,
                        tokenType = tokenResponse.token_type,
                        expiresIn = tokenResponse.expires_in,
                        refreshToken = null, // No refresh token for client credentials
                        scope = tokenResponse.scope
                    )
                )
            } else {
                Result.failure(Exception("Client credentials grant failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(
        refreshToken: String,
        clientId: String
    ): Result<AuthenticationToken> {
        return try {
            val params = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", clientId)
            }

            val response = httpClient.post("$baseUrl/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(params))
                header("Accept", "application/json")
                header("Authorization", "Basic ${encodeClientCredentials(clientId, "")}")
            }

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                Result.success(
                    AuthenticationToken(
                        accessToken = tokenResponse.access_token,
                        tokenType = tokenResponse.token_type,
                        expiresIn = tokenResponse.expires_in,
                        refreshToken = tokenResponse.refresh_token ?: refreshToken,
                        scope = tokenResponse.scope
                    )
                )
            } else {
                Result.failure(Exception("Token refresh failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isTokenValid(token: String): Boolean {
        return try {
            val response = httpClient.get("$baseUrl/oauth/introspect") {
                parameter("token", token)
                header("Accept", "application/json")
            }
            
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    override fun isTokenExpiringSoon(token: AuthenticationToken): Boolean {
        return token.isExpiringSoon()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeClientCredentials(clientId: String, clientSecret: String): String {
        val credentials = "$clientId:$clientSecret"
        return bytesToBase64(credentials.encodeToByteArray())
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateNonce(): String {
        val nonce = ByteArray(32).apply {
            Random.Default.nextBytes(this)
        }
        return Base64.UrlSafe.encode(nonce).trimEnd('=')
    }
}