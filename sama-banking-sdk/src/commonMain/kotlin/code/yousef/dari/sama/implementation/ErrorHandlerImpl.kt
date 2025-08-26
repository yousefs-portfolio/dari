package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.ErrorHandler
import code.yousef.dari.sama.models.SamaError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * SAMA-compliant Error Handler Implementation
 * Handles API errors, network errors, retry logic, and localization
 */
class ErrorHandlerImpl(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ErrorHandler {

    companion object {
        private const val MAX_RETRY_DELAY = 30000L // 30 seconds
        private const val BASE_RETRY_DELAY = 1000L // 1 second
    }

    override suspend fun handleApiError(
        httpStatusCode: Int,
        responseBody: String?,
        headers: Map<String, String>
    ): SamaError {
        val errorDetails = parseErrorDetails(responseBody)
        
        return when (httpStatusCode) {
            400 -> SamaError.BadRequest(
                code = errorDetails?.code ?: "SAMA_BAD_REQUEST",
                message = errorDetails?.message ?: "Bad request - invalid parameters",
                details = responseBody,
                userFriendlyMessage = "Please check your input and try again"
            )
            
            401 -> SamaError.Unauthorized(
                code = errorDetails?.code ?: "SAMA_UNAUTHORIZED",
                message = errorDetails?.message ?: "Authentication required",
                details = responseBody,
                userFriendlyMessage = "Please log in again to continue"
            )
            
            403 -> SamaError.Forbidden(
                code = errorDetails?.code ?: "SAMA_FORBIDDEN",
                message = errorDetails?.message ?: "Insufficient permissions",
                details = responseBody,
                userFriendlyMessage = "You don't have permission to perform this action"
            )
            
            404 -> SamaError.BadRequest(
                code = errorDetails?.code ?: "SAMA_NOT_FOUND",
                message = errorDetails?.message ?: "Resource not found",
                details = responseBody,
                userFriendlyMessage = "The requested resource was not found"
            )
            
            429 -> {
                val retryAfter = headers["Retry-After"]?.toLongOrNull() ?: 60L
                SamaError.RateLimited(
                    code = errorDetails?.code ?: "SAMA_RATE_LIMITED",
                    message = errorDetails?.message ?: "Rate limit exceeded",
                    details = responseBody,
                    retryAfterSeconds = retryAfter,
                    userFriendlyMessage = "Too many requests. Please wait and try again"
                )
            }
            
            in 500..599 -> SamaError.ServerError(
                code = errorDetails?.code ?: "SAMA_SERVER_ERROR",
                message = errorDetails?.message ?: "Server error occurred",
                details = responseBody,
                userFriendlyMessage = "A server error occurred. Please try again later"
            )
            
            else -> SamaError.UnknownError(
                code = errorDetails?.code ?: "SAMA_UNKNOWN_ERROR",
                message = errorDetails?.message ?: "Unknown error occurred",
                details = "HTTP $httpStatusCode: $responseBody",
                userFriendlyMessage = "An unexpected error occurred"
            )
        }
    }

    override suspend fun handleNetworkError(exception: Throwable): SamaError {
        val message = exception.message ?: exception.toString()
        
        return when {
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) -> {
                SamaError.NetworkTimeout(
                    code = "SAMA_NETWORK_TIMEOUT",
                    message = "Network request timed out",
                    details = message,
                    userFriendlyMessage = "Connection timed out. Please check your internet connection"
                )
            }
            
            message.contains("connection", ignoreCase = true) ||
            message.contains("unreachable", ignoreCase = true) ||
            message.contains("refused", ignoreCase = true) -> {
                SamaError.NetworkError(
                    code = "SAMA_NETWORK_ERROR",
                    message = "Network connection failed",
                    details = message,
                    userFriendlyMessage = "Unable to connect. Please check your internet connection"
                )
            }
            
            message.contains("ssl", ignoreCase = true) ||
            message.contains("certificate", ignoreCase = true) ||
            message.contains("handshake", ignoreCase = true) -> {
                SamaError.NetworkError(
                    code = "SAMA_SSL_ERROR",
                    message = "SSL/Certificate error",
                    details = message,
                    userFriendlyMessage = "Secure connection failed. Please try again"
                )
            }
            
            else -> SamaError.UnknownError(
                code = "SAMA_UNKNOWN_ERROR",
                message = "Unknown network error",
                details = message,
                userFriendlyMessage = "An unexpected error occurred"
            )
        }
    }

    override suspend fun shouldRetry(
        error: SamaError,
        attemptNumber: Int,
        maxRetries: Int
    ): Boolean {
        if (attemptNumber >= maxRetries) return false
        
        return when (error) {
            is SamaError.NetworkTimeout -> true
            is SamaError.NetworkError -> true
            is SamaError.ServerError -> attemptNumber < 3 // Limit server error retries
            is SamaError.RateLimited -> true
            else -> false
        }
    }

    override suspend fun calculateRetryDelay(
        error: SamaError,
        attemptNumber: Int
    ): Long {
        return when (error) {
            is SamaError.RateLimited -> {
                // Use the retry-after header value
                error.retryAfterSeconds * 1000L
            }
            
            is SamaError.NetworkTimeout,
            is SamaError.NetworkError,
            is SamaError.ServerError -> {
                // Exponential backoff with jitter
                val baseDelay = BASE_RETRY_DELAY
                val exponentialDelay = baseDelay * (1L shl (attemptNumber - 1))
                val jitter = (exponentialDelay * 0.1 * Math.random()).toLong()
                minOf(exponentialDelay + jitter, MAX_RETRY_DELAY)
            }
            
            else -> 0L
        }
    }

    override fun getUserFriendlyMessage(error: SamaError, locale: String): String {
        return when (locale.lowercase()) {
            "ar", "ar-sa" -> getArabicMessage(error)
            else -> error.userFriendlyMessage
        }
    }

    override suspend fun logError(
        error: SamaError,
        context: String?,
        userId: String?
    ) {
        // In a real implementation, this would integrate with logging frameworks
        // like Timber, Firebase Crashlytics, or custom analytics
        
        val logMessage = buildString {
            append("SAMA Error: ${error.code} - ${error.message}")
            context?.let { append(" | Context: $it") }
            userId?.let { append(" | User: $it") }
            error.details?.let { append(" | Details: $it") }
        }
        
        // For now, just print to console
        // In production, replace with proper logging
        println(logMessage)
        
        // Example of how you might integrate with analytics:
        // analyticsService.logError(
        //     errorCode = error.code,
        //     errorMessage = error.message,
        //     context = context,
        //     userId = userId,
        //     details = error.details
        // )
    }

    override fun isRetryableError(error: SamaError): Boolean {
        return when (error) {
            is SamaError.NetworkTimeout -> true
            is SamaError.NetworkError -> true
            is SamaError.ServerError -> true
            is SamaError.RateLimited -> true
            else -> false
        }
    }

    private fun parseErrorDetails(responseBody: String?): ErrorDetails? {
        if (responseBody.isNullOrBlank()) return null
        
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            if (jsonElement is JsonObject) {
                val errorCode = jsonElement["error"]?.jsonPrimitive?.content
                    ?: jsonElement["code"]?.jsonPrimitive?.content
                val errorMessage = jsonElement["error_description"]?.jsonPrimitive?.content
                    ?: jsonElement["message"]?.jsonPrimitive?.content
                    ?: jsonElement["detail"]?.jsonPrimitive?.content
                
                if (errorCode != null || errorMessage != null) {
                    ErrorDetails(errorCode, errorMessage)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getArabicMessage(error: SamaError): String {
        return when (error) {
            is SamaError.BadRequest -> "يرجى التحقق من البيانات المدخلة والمحاولة مرة أخرى"
            is SamaError.Unauthorized -> "يرجى تسجيل الدخول مرة أخرى للمتابعة"
            is SamaError.Forbidden -> "ليس لديك صلاحية للقيام بهذا الإجراء"
            is SamaError.RateLimited -> "عدد كبير من الطلبات. يرجى الانتظار والمحاولة مرة أخرى"
            is SamaError.ServerError -> "حدث خطأ في الخادم. يرجى المحاولة لاحقاً"
            is SamaError.NetworkTimeout -> "انتهت مهلة الاتصال. يرجى التحقق من اتصال الإنترنت"
            is SamaError.NetworkError -> "تعذر الاتصال. يرجى التحقق من اتصال الإنترنت"
            is SamaError.UnknownError -> "حدث خطأ غير متوقع"
        }
    }

    private data class ErrorDetails(
        val code: String?,
        val message: String?
    )
}

/**
 * Utility extension for creating retry logic
 */
suspend fun <T> ErrorHandler.withRetry(
    maxRetries: Int = 3,
    operation: suspend () -> Result<T>
): Result<T> {
    var lastError: SamaError? = null
    
    repeat(maxRetries) { attempt ->
        val attemptNumber = attempt + 1
        
        try {
            val result = operation()
            if (result.isSuccess) {
                return result
            } else {
                // Handle failed result if it contains error information
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    lastError = handleNetworkError(exception)
                }
            }
        } catch (e: Exception) {
            lastError = handleNetworkError(e)
        }
        
        lastError?.let { error ->
            if (shouldRetry(error, attemptNumber, maxRetries)) {
                val delay = calculateRetryDelay(error, attemptNumber)
                if (delay > 0) {
                    kotlinx.coroutines.delay(delay)
                }
            } else {
                logError(error, "Retry failed", null)
                return Result.failure(Exception(error.message))
            }
        }
    }
    
    return Result.failure(Exception(lastError?.message ?: "Operation failed after retries"))
}