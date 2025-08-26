package code.yousef.dari.sama.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Sealed class representing all possible SAMA banking errors
 */
sealed class SamaError(
    open val code: String,
    open val message: String,
    open val details: String?,
    open val userFriendlyMessage: String
) {
    data class BadRequest(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class Unauthorized(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class Forbidden(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class RateLimited(
        override val code: String,
        override val message: String,
        override val details: String?,
        val retryAfterSeconds: Long,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class ServerError(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class NetworkTimeout(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class NetworkError(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)

    data class UnknownError(
        override val code: String,
        override val message: String,
        override val details: String?,
        override val userFriendlyMessage: String
    ) : SamaError(code, message, details, userFriendlyMessage)
}

/**
 * Standardized banking error following SAMA error response format
 */
@Serializable
data class BankingError(
    val code: String,
    val type: BankingErrorType,
    val title: String,
    val detail: String,
    val status: Int? = null,
    val instance: String? = null,
    val timestamp: String = kotlinx.datetime.Clock.System.now().toString(),
    val retryAfter: Long? = null, // seconds
    val isRecoverable: Boolean = false,
    val context: Map<String, String> = emptyMap()
) {
    companion object {
        // Common SAMA error codes
        const val INVALID_REQUEST = "invalid_request"
        const val INVALID_CLIENT = "invalid_client"
        const val INVALID_GRANT = "invalid_grant"
        const val UNAUTHORIZED_CLIENT = "unauthorized_client"
        const val UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type"
        const val INVALID_SCOPE = "invalid_scope"
        const val INSUFFICIENT_SCOPE = "insufficient_scope"
        const val INVALID_TOKEN = "invalid_token"
        const val CONSENT_INVALID = "consent_invalid"
        const val CONSENT_EXPIRED = "consent_expired"
        const val CONSENT_REVOKED = "consent_revoked"
        const val RESOURCE_NOT_FOUND = "resource_not_found"
        const val RESOURCE_CONSENT_MISMATCH = "resource_consent_mismatch"
        const val PAYMENT_INVALID = "payment_invalid"
        const val PAYMENT_REJECTED = "payment_rejected"
        const val RATE_LIMIT_EXCEEDED = "rate_limit_exceeded"
        const val SERVER_ERROR = "server_error"
        const val SERVICE_UNAVAILABLE = "service_unavailable"
        const val NETWORK_ERROR = "network_error"
        const val TIMEOUT_ERROR = "timeout_error"
        const val CERTIFICATE_ERROR = "certificate_error"
        
        // Factory methods for common errors
        fun invalidRequest(detail: String) = BankingError(
            code = INVALID_REQUEST,
            type = BankingErrorType.VALIDATION,
            title = "Invalid Request",
            detail = detail,
            status = 400
        )
        
        fun unauthorized(detail: String = "Authentication required") = BankingError(
            code = INVALID_TOKEN,
            type = BankingErrorType.AUTHENTICATION,
            title = "Unauthorized",
            detail = detail,
            status = 401
        )
        
        fun forbidden(detail: String = "Access denied") = BankingError(
            code = INSUFFICIENT_SCOPE,
            type = BankingErrorType.AUTHORIZATION,
            title = "Forbidden",
            detail = detail,
            status = 403
        )
        
        fun notFound(resource: String) = BankingError(
            code = RESOURCE_NOT_FOUND,
            type = BankingErrorType.RESOURCE,
            title = "Resource Not Found",
            detail = "The requested $resource was not found",
            status = 404
        )
        
        fun rateLimit(retryAfter: Long? = null) = BankingError(
            code = RATE_LIMIT_EXCEEDED,
            type = BankingErrorType.RATE_LIMIT,
            title = "Rate Limit Exceeded",
            detail = "Too many requests. Please retry later.",
            status = 429,
            retryAfter = retryAfter,
            isRecoverable = true
        )
        
        fun serverError(detail: String = "Internal server error occurred") = BankingError(
            code = SERVER_ERROR,
            type = BankingErrorType.SERVER,
            title = "Server Error",
            detail = detail,
            status = 500,
            isRecoverable = true
        )
        
        fun networkError(exception: Exception) = BankingError(
            code = NETWORK_ERROR,
            type = BankingErrorType.NETWORK,
            title = "Network Error",
            detail = "Network connection failed: ${exception.message}",
            isRecoverable = true
        )
        
        fun timeoutError(duration: Long) = BankingError(
            code = TIMEOUT_ERROR,
            type = BankingErrorType.NETWORK,
            title = "Request Timeout",
            detail = "Request timed out after ${duration}ms",
            isRecoverable = true
        )
        
        fun consentExpired(consentId: String) = BankingError(
            code = CONSENT_EXPIRED,
            type = BankingErrorType.CONSENT,
            title = "Consent Expired",
            detail = "Consent $consentId has expired and needs to be renewed",
            status = 403,
            context = mapOf("consentId" to consentId)
        )
    }
}

/**
 * SAMA standard error response format
 */
@Serializable
data class SamaErrorResponse(
    val error: String,
    val errorDescription: String? = null,
    val errorUri: String? = null,
    val errors: List<SamaFieldError>? = null
)

/**
 * Field-specific error in SAMA responses
 */
@Serializable
data class SamaFieldError(
    val field: String,
    val code: String,
    val message: String,
    val path: String? = null
)

/**
 * Error log entry for audit and debugging
 */
@Serializable
data class ErrorLogEntry(
    val error: BankingError,
    val context: Map<String, String>,
    val timestamp: Instant,
    val sessionId: String? = null,
    val userId: String? = null,
    val requestId: String? = null,
    val stackTrace: String? = null
)

// Enums
@Serializable
enum class BankingErrorType {
    VALIDATION,      // Input validation errors
    AUTHENTICATION,  // Authentication failures
    AUTHORIZATION,   // Permission/scope issues
    CONSENT,         // Consent-related errors
    RESOURCE,        // Resource not found/invalid
    PAYMENT,         // Payment-specific errors
    RATE_LIMIT,      // Rate limiting errors
    SERVER,          // Server-side errors
    NETWORK,         // Network connectivity issues
    CERTIFICATE,     // SSL/Certificate errors
    SECURITY,        // Security violations
    BUSINESS_RULE,   // Business logic violations
    TECHNICAL,       // Technical/system errors
    UNKNOWN          // Unclassified errors
}