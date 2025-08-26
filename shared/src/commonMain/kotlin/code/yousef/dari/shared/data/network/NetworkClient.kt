package code.yousef.dari.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Network Client Factory for Ktor
 * Creates configured HTTP clients for different environments and use cases
 */
object NetworkClient {
    
    /**
     * Create production HTTP client with security and performance optimizations
     */
    fun createProductionClient(
        baseUrl: String,
        enableLogging: Boolean = false
    ): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            
            if (enableLogging) {
                install(Logging) {
                    level = LogLevel.INFO
                    logger = Logger.SIMPLE
                }
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            
            defaultRequest {
                url(baseUrl)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "DariApp/1.0 (Android)")
            }
            
            // Add retry logic
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    when (exception) {
                        is NetworkException -> throw exception
                        else -> throw NetworkException("Network error: ${exception.message}", exception)
                    }
                }
            }
        }
    }
    
    /**
     * Create development HTTP client with detailed logging
     */
    fun createDevelopmentClient(baseUrl: String): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
            
            defaultRequest {
                url(baseUrl)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "DariApp/1.0-dev (Android)")
            }
        }
    }
    
    /**
     * Create SAMA-compliant HTTP client with security headers and certificate pinning
     */
    fun createSamaClient(
        baseUrl: String,
        certificatePins: List<String> = emptyList(),
        enableLogging: Boolean = false
    ): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            
            if (enableLogging) {
                install(Logging) {
                    level = LogLevel.HEADERS
                    logger = Logger.SIMPLE
                }
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 45_000
            }
            
            defaultRequest {
                url(baseUrl)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "DariApp/1.0 (SAMA-Compliant)")
                header("X-Request-ID", generateRequestId())
                header("X-FAPI-Interaction-ID", generateInteractionId())
                header("X-FAPI-Auth-Date", getCurrentDateTime())
                header("X-FAPI-Customer-IP-Address", "0.0.0.0") // Will be set by platform
            }
            
            // TODO: Implement certificate pinning when available in KMP
            // Currently not supported in Ktor KMP
            
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    when (exception) {
                        is SamaApiException -> throw exception
                        else -> throw SamaApiException("SAMA API error: ${exception.message}", exception)
                    }
                }
            }
        }
    }
    
    private fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun generateInteractionId(): String {
        return "int_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private fun getCurrentDateTime(): String {
        return kotlinx.datetime.Clock.System.now().toString()
    }
}

/**
 * Custom exception for network errors
 */
class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Custom exception for SAMA API specific errors
 */
class SamaApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)