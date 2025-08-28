package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.ErrorHandler
import code.yousef.dari.sama.models.BankingError
import code.yousef.dari.sama.models.BankingErrorType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min
import kotlin.math.pow

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

    override fun parseHttpError(
        statusCode: Int,
        responseBody: String,
        requestUrl: String
    ): BankingError {
        return when (statusCode) {
            400 -> handleBadRequest(responseBody)
            401 -> handleUnauthorized(responseBody)
            403 -> handleForbidden(responseBody)
            429 -> handleRateLimit(responseBody, null)
            500, 502, 503, 504 -> handleServerError(responseBody)
            else -> BankingError(
                code = "HTTP_$statusCode",
                type = if (statusCode >= 500) BankingErrorType.SERVER else BankingErrorType.TECHNICAL,
                title = "HTTP Error",
                detail = "HTTP error $statusCode",
                status = statusCode,
                isRecoverable = statusCode >= 500
            )
        }
    }

    override fun handleBadRequest(responseBody: String): BankingError {
        val errorDetails = parseErrorDetails(responseBody)
        return BankingError(
            code = errorDetails["code"] ?: "BAD_REQUEST",
            type = BankingErrorType.VALIDATION,
            title = "Bad Request",
            detail = errorDetails["message"] ?: "Bad request",
            status = 400,
            isRecoverable = false
        )
    }

    override fun handleUnauthorized(responseBody: String): BankingError {
        val errorDetails = parseErrorDetails(responseBody)
        return BankingError(
            code = errorDetails["code"] ?: "UNAUTHORIZED",
            type = BankingErrorType.AUTHENTICATION,
            title = "Unauthorized",
            detail = errorDetails["message"] ?: "Authentication failed",
            status = 401,
            isRecoverable = false
        )
    }

    override fun handleForbidden(responseBody: String): BankingError {
        val errorDetails = parseErrorDetails(responseBody)
        return BankingError(
            code = errorDetails["code"] ?: "FORBIDDEN",
            type = BankingErrorType.AUTHORIZATION,
            title = "Forbidden",
            detail = errorDetails["message"] ?: "Access forbidden",
            status = 403,
            isRecoverable = false
        )
    }

    override fun handleRateLimit(responseBody: String, retryAfter: String?): BankingError {
        val errorDetails = parseErrorDetails(responseBody)
        val retryDelay = retryAfter?.toLongOrNull() ?: 60
        
        return BankingError(
            code = errorDetails["code"] ?: "RATE_LIMIT",
            type = BankingErrorType.RATE_LIMIT,
            title = "Rate Limit Exceeded",
            detail = errorDetails["message"] ?: "Rate limit exceeded",
            status = 429,
            isRecoverable = true,
            retryAfter = retryDelay
        )
    }

    override fun handleServerError(responseBody: String): BankingError {
        val errorDetails = parseErrorDetails(responseBody)
        return BankingError(
            code = errorDetails["code"] ?: "SERVER_ERROR",
            type = BankingErrorType.SERVER,
            title = "Server Error",
            detail = errorDetails["message"] ?: "Internal server error",
            status = 500,
            isRecoverable = true
        )
    }

    override fun handleTimeout(timeoutDuration: Long): BankingError {
        return BankingError(
            code = "TIMEOUT",
            type = BankingErrorType.NETWORK,
            title = "Request Timeout",
            detail = "Request timed out after ${timeoutDuration}ms",
            isRecoverable = true
        )
    }

    override fun handleNetworkError(exception: Exception): BankingError {
        return BankingError(
            code = "NETWORK_ERROR",
            type = BankingErrorType.NETWORK,
            title = "Network Error",
            detail = exception.message ?: "Network connection failed",
            isRecoverable = true
        )
    }

    override fun formatErrorMessage(error: BankingError, language: String): String {
        return when (language) {
            "ar" -> formatErrorMessageArabic(error)
            else -> formatErrorMessageEnglish(error)
        }
    }

    override fun isRecoverableError(error: BankingError): Boolean {
        return error.isRecoverable && when (error.code) {
            "UNAUTHORIZED", "FORBIDDEN", "BAD_REQUEST" -> false
            "RATE_LIMIT", "TIMEOUT", "NETWORK_ERROR", "SERVER_ERROR" -> true
            else -> error.isRecoverable
        }
    }

    override fun calculateRetryDelay(
        attemptNumber: Int,
        baseDelayMs: Long,
        maxDelayMs: Long
    ): Long {
        val exponentialDelay = baseDelayMs * (2.0.pow(attemptNumber.toDouble())).toLong()
        return min(exponentialDelay, maxDelayMs)
    }

    override fun logError(
        error: BankingError,
        context: Map<String, String>,
        timestamp: Instant
    ) {
        // In a real implementation, this would log to appropriate logging system
        println("ERROR [${timestamp}]: ${error.code} - ${error.title}")
        if (context.isNotEmpty()) {
            println("Context: $context")
        }
        if (error.detail.isNotBlank()) {
            println("Details: ${error.detail}")
        }
    }

    override suspend fun reportCriticalError(
        error: BankingError,
        context: Map<String, String>
    ): Result<Unit> {
        return try {
            // In a real implementation, this would report to monitoring/alerting system
            logError(error, context, Clock.System.now())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorDetails(responseBody: String): Map<String, String> {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            if (jsonElement is JsonObject) {
                val result = mutableMapOf<String, String>()
                
                // Try to extract common error fields
                jsonElement["error_code"]?.jsonPrimitive?.content?.let { result["code"] = it }
                jsonElement["error"]?.jsonPrimitive?.content?.let { result["code"] = it }
                jsonElement["code"]?.jsonPrimitive?.content?.let { result["code"] = it }
                
                jsonElement["error_description"]?.jsonPrimitive?.content?.let { result["message"] = it }
                jsonElement["message"]?.jsonPrimitive?.content?.let { result["message"] = it }
                jsonElement["description"]?.jsonPrimitive?.content?.let { result["message"] = it }
                
                jsonElement["request_id"]?.jsonPrimitive?.content?.let { result["requestId"] = it }
                jsonElement["requestId"]?.jsonPrimitive?.content?.let { result["requestId"] = it }
                
                result
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun formatErrorMessageEnglish(error: BankingError): String {
        return when (error.code) {
            "UNAUTHORIZED" -> "Authentication failed. Please log in again."
            "FORBIDDEN" -> "You don't have permission to access this resource."
            "BAD_REQUEST" -> "Invalid request. Please check your input and try again."
            "RATE_LIMIT" -> "Too many requests. Please wait and try again."
            "TIMEOUT" -> "Request timed out. Please check your connection and try again."
            "NETWORK_ERROR" -> "Network connection failed. Please check your internet connection."
            "SERVER_ERROR" -> "Server error occurred. Please try again later."
            else -> error.detail
        }
    }

    private fun formatErrorMessageArabic(error: BankingError): String {
        return when (error.code) {
            "UNAUTHORIZED" -> "فشل التحقق من الهوية. يرجى تسجيل الدخول مرة أخرى."
            "FORBIDDEN" -> "ليس لديك إذن للوصول إلى هذا المورد."
            "BAD_REQUEST" -> "طلب غير صالح. يرجى التحقق من المدخلات والمحاولة مرة أخرى."
            "RATE_LIMIT" -> "طلبات كثيرة جداً. يرجى الانتظار والمحاولة مرة أخرى."
            "TIMEOUT" -> "انتهت مهلة الطلب. يرجى التحقق من الاتصال والمحاولة مرة أخرى."
            "NETWORK_ERROR" -> "فشل الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت."
            "SERVER_ERROR" -> "حدث خطأ في الخادم. يرجى المحاولة مرة أخرى لاحقاً."
            else -> error.detail
        }
    }
}