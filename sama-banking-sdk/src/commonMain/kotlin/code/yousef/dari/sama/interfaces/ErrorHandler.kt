package code.yousef.dari.sama.interfaces

import code.yousef.dari.sama.models.BankingError
import kotlinx.datetime.Instant

/**
 * Error Handler interface for consistent error management
 * Handles SAMA-specific error codes and provides user-friendly messages
 */
interface ErrorHandler {
    
    /**
     * Parse HTTP error response into banking error
     * @param statusCode HTTP status code
     * @param responseBody Response body content
     * @param requestUrl Original request URL for context
     * @return Parsed banking error
     */
    fun parseHttpError(
        statusCode: Int,
        responseBody: String,
        requestUrl: String
    ): BankingError
    
    /**
     * Handle 400 Bad Request errors
     * @param responseBody Error response body
     * @return Structured error with details
     */
    fun handleBadRequest(responseBody: String): BankingError
    
    /**
     * Handle 401 Unauthorized errors
     * @param responseBody Error response body
     * @return Structured error with authentication guidance
     */
    fun handleUnauthorized(responseBody: String): BankingError
    
    /**
     * Handle 403 Forbidden access errors
     * @param responseBody Error response body
     * @return Structured error with permission details
     */
    fun handleForbidden(responseBody: String): BankingError
    
    /**
     * Handle 429 Rate Limit exceeded errors with retry strategy
     * @param responseBody Error response body
     * @param retryAfter Retry-After header value
     * @return Structured error with retry information
     */
    fun handleRateLimit(
        responseBody: String,
        retryAfter: String?
    ): BankingError
    
    /**
     * Handle 500 Internal Server errors
     * @param responseBody Error response body
     * @return Structured error for server issues
     */
    fun handleServerError(responseBody: String): BankingError
    
    /**
     * Handle network timeout errors
     * @param timeoutDuration How long before timeout occurred
     * @return Structured error for timeout
     */
    fun handleTimeout(timeoutDuration: Long): BankingError
    
    /**
     * Handle network connectivity errors
     * @param exception Original network exception
     * @return Structured error for connectivity issues
     */
    fun handleNetworkError(exception: Exception): BankingError
    
    /**
     * Get user-friendly error message for display
     * @param error Banking error to format
     * @param language Language code (ar, en)
     * @return Localized error message
     */
    fun formatErrorMessage(
        error: BankingError,
        language: String = "en"
    ): String
    
    /**
     * Check if error is recoverable with retry
     * @param error Banking error to check
     * @return true if retry might succeed
     */
    fun isRecoverableError(error: BankingError): Boolean
    
    /**
     * Calculate retry delay using exponential backoff
     * @param attemptNumber Current retry attempt (0-based)
     * @param baseDelayMs Base delay in milliseconds
     * @param maxDelayMs Maximum delay in milliseconds
     * @return Delay in milliseconds before next retry
     */
    fun calculateRetryDelay(
        attemptNumber: Int,
        baseDelayMs: Long = 1000,
        maxDelayMs: Long = 30000
    ): Long
    
    /**
     * Log error for debugging and monitoring
     * @param error Banking error to log
     * @param context Additional context information
     * @param timestamp When the error occurred
     */
    fun logError(
        error: BankingError,
        context: Map<String, String> = emptyMap(),
        timestamp: Instant = kotlinx.datetime.Clock.System.now()
    )
    
    /**
     * Report critical errors to monitoring system
     * @param error Critical banking error
     * @param context Additional context for debugging
     */
    suspend fun reportCriticalError(
        error: BankingError,
        context: Map<String, String> = emptyMap()
    ): Result<Unit>
}