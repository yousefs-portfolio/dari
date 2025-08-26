package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.ErrorHandler
import code.yousef.dari.sama.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for ErrorHandler implementation - TDD approach
 * Tests error parsing, handling, retry logic, and localization
 */
class ErrorHandlerTest {

    private class MockErrorHandler : ErrorHandler {
        private val retryCount = mutableMapOf<String, Int>()

        override suspend fun handleApiError(
            httpStatusCode: Int,
            responseBody: String?,
            headers: Map<String, String>
        ): SamaError {
            return when (httpStatusCode) {
                400 -> SamaError.BadRequest(
                    code = "SAMA_BAD_REQUEST",
                    message = "Invalid request parameters",
                    details = responseBody,
                    userFriendlyMessage = "Please check your input and try again"
                )
                401 -> SamaError.Unauthorized(
                    code = "SAMA_UNAUTHORIZED",
                    message = "Invalid or expired access token",
                    details = responseBody,
                    userFriendlyMessage = "Please log in again to continue"
                )
                403 -> SamaError.Forbidden(
                    code = "SAMA_FORBIDDEN",
                    message = "Insufficient permissions for this operation",
                    details = responseBody,
                    userFriendlyMessage = "You don't have permission to perform this action"
                )
                429 -> {
                    val retryAfter = headers["Retry-After"]?.toLongOrNull() ?: 60L
                    SamaError.RateLimited(
                        code = "SAMA_RATE_LIMITED",
                        message = "Too many requests",
                        details = responseBody,
                        retryAfterSeconds = retryAfter,
                        userFriendlyMessage = "Too many requests. Please wait and try again"
                    )
                }
                500 -> SamaError.ServerError(
                    code = "SAMA_SERVER_ERROR",
                    message = "Internal server error",
                    details = responseBody,
                    userFriendlyMessage = "A server error occurred. Please try again later"
                )
                else -> SamaError.UnknownError(
                    code = "SAMA_UNKNOWN_ERROR",
                    message = "Unknown error occurred",
                    details = "HTTP $httpStatusCode: $responseBody",
                    userFriendlyMessage = "An unexpected error occurred"
                )
            }
        }

        override suspend fun handleNetworkError(exception: Throwable): SamaError {
            return when {
                exception.message?.contains("timeout", ignoreCase = true) == true -> {
                    SamaError.NetworkTimeout(
                        code = "SAMA_NETWORK_TIMEOUT",
                        message = "Network timeout occurred",
                        details = exception.message,
                        userFriendlyMessage = "Connection timed out. Please check your internet connection"
                    )
                }
                exception.message?.contains("connection", ignoreCase = true) == true -> {
                    SamaError.NetworkError(
                        code = "SAMA_NETWORK_ERROR",
                        message = "Network connection error",
                        details = exception.message,
                        userFriendlyMessage = "Unable to connect. Please check your internet connection"
                    )
                }
                else -> {
                    SamaError.UnknownError(
                        code = "SAMA_UNKNOWN_ERROR",
                        message = "Unknown error occurred",
                        details = exception.message,
                        userFriendlyMessage = "An unexpected error occurred"
                    )
                }
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
                is SamaError.ServerError -> attemptNumber < 3 // Retry server errors up to 3 times
                is SamaError.RateLimited -> true
                else -> false
            }
        }

        override suspend fun calculateRetryDelay(
            error: SamaError,
            attemptNumber: Int
        ): Long {
            return when (error) {
                is SamaError.RateLimited -> error.retryAfterSeconds * 1000L
                is SamaError.NetworkTimeout -> exponentialBackoff(attemptNumber)
                is SamaError.NetworkError -> exponentialBackoff(attemptNumber)
                is SamaError.ServerError -> exponentialBackoff(attemptNumber)
                else -> 0L
            }
        }

        override fun getUserFriendlyMessage(error: SamaError, locale: String): String {
            return when (locale) {
                "ar" -> when (error) {
                    is SamaError.BadRequest -> "يرجى التحقق من البيانات المدخلة والمحاولة مرة أخرى"
                    is SamaError.Unauthorized -> "يرجى تسجيل الدخول مرة أخرى للمتابعة"
                    is SamaError.Forbidden -> "ليس لديك صلاحية للقيام بهذا الإجراء"
                    is SamaError.RateLimited -> "عدد كبير من الطلبات. يرجى الانتظار والمحاولة مرة أخرى"
                    is SamaError.ServerError -> "حدث خطأ في الخادم. يرجى المحاولة لاحقاً"
                    is SamaError.NetworkTimeout -> "انتهت مهلة الاتصال. يرجى التحقق من اتصال الإنترنت"
                    is SamaError.NetworkError -> "تعذر الاتصال. يرجى التحقق من اتصال الإنترنت"
                    else -> "حدث خطأ غير متوقع"
                }
                else -> error.userFriendlyMessage
            }
        }

        override suspend fun logError(
            error: SamaError,
            context: String?,
            userId: String?
        ) {
            // Mock logging - in real implementation this would log to analytics/crashlytics
            println("Error logged: ${error.code} - ${error.message} - Context: $context - User: $userId")
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

        private fun exponentialBackoff(attemptNumber: Int): Long {
            val baseDelay = 1000L // 1 second
            val maxDelay = 30000L // 30 seconds
            val delay = baseDelay * (1L shl (attemptNumber - 1)) // 2^(n-1) * baseDelay
            return minOf(delay, maxDelay)
        }
    }

    private val errorHandler = MockErrorHandler()

    @Test
    fun `should handle bad request error correctly`() = runTest {
        // Given
        val httpStatusCode = 400
        val responseBody = """{"error": "Invalid account ID"}"""
        val headers = emptyMap<String, String>()

        // When
        val error = errorHandler.handleApiError(httpStatusCode, responseBody, headers)

        // Then
        assertTrue(error is SamaError.BadRequest, "Should be BadRequest error")
        val badRequestError = error as SamaError.BadRequest
        assertEquals("SAMA_BAD_REQUEST", badRequestError.code)
        assertEquals("Invalid request parameters", badRequestError.message)
        assertEquals(responseBody, badRequestError.details)
        assertEquals("Please check your input and try again", badRequestError.userFriendlyMessage)
    }

    @Test
    fun `should handle unauthorized error correctly`() = runTest {
        // Given
        val httpStatusCode = 401
        val responseBody = """{"error": "Token expired"}"""
        val headers = emptyMap<String, String>()

        // When
        val error = errorHandler.handleApiError(httpStatusCode, responseBody, headers)

        // Then
        assertTrue(error is SamaError.Unauthorized, "Should be Unauthorized error")
        val unauthorizedError = error as SamaError.Unauthorized
        assertEquals("SAMA_UNAUTHORIZED", unauthorizedError.code)
        assertEquals("Invalid or expired access token", unauthorizedError.message)
        assertEquals("Please log in again to continue", unauthorizedError.userFriendlyMessage)
    }

    @Test
    fun `should handle forbidden error correctly`() = runTest {
        // Given
        val httpStatusCode = 403
        val responseBody = """{"error": "Insufficient permissions"}"""
        val headers = emptyMap<String, String>()

        // When
        val error = errorHandler.handleApiError(httpStatusCode, responseBody, headers)

        // Then
        assertTrue(error is SamaError.Forbidden, "Should be Forbidden error")
        val forbiddenError = error as SamaError.Forbidden
        assertEquals("SAMA_FORBIDDEN", forbiddenError.code)
        assertEquals("Insufficient permissions for this operation", forbiddenError.message)
        assertEquals("You don't have permission to perform this action", forbiddenError.userFriendlyMessage)
    }

    @Test
    fun `should handle rate limited error with retry after`() = runTest {
        // Given
        val httpStatusCode = 429
        val responseBody = """{"error": "Rate limit exceeded"}"""
        val headers = mapOf("Retry-After" to "120")

        // When
        val error = errorHandler.handleApiError(httpStatusCode, responseBody, headers)

        // Then
        assertTrue(error is SamaError.RateLimited, "Should be RateLimited error")
        val rateLimitedError = error as SamaError.RateLimited
        assertEquals("SAMA_RATE_LIMITED", rateLimitedError.code)
        assertEquals(120L, rateLimitedError.retryAfterSeconds)
        assertEquals("Too many requests. Please wait and try again", rateLimitedError.userFriendlyMessage)
    }

    @Test
    fun `should handle server error correctly`() = runTest {
        // Given
        val httpStatusCode = 500
        val responseBody = """{"error": "Internal server error"}"""
        val headers = emptyMap<String, String>()

        // When
        val error = errorHandler.handleApiError(httpStatusCode, responseBody, headers)

        // Then
        assertTrue(error is SamaError.ServerError, "Should be ServerError")
        val serverError = error as SamaError.ServerError
        assertEquals("SAMA_SERVER_ERROR", serverError.code)
        assertEquals("Internal server error", serverError.message)
        assertEquals("A server error occurred. Please try again later", serverError.userFriendlyMessage)
    }

    @Test
    fun `should handle network timeout error`() = runTest {
        // Given
        val exception = RuntimeException("Connection timeout")

        // When
        val error = errorHandler.handleNetworkError(exception)

        // Then
        assertTrue(error is SamaError.NetworkTimeout, "Should be NetworkTimeout error")
        val timeoutError = error as SamaError.NetworkTimeout
        assertEquals("SAMA_NETWORK_TIMEOUT", timeoutError.code)
        assertEquals("Network timeout occurred", timeoutError.message)
        assertEquals("Connection timed out. Please check your internet connection", timeoutError.userFriendlyMessage)
    }

    @Test
    fun `should handle network connection error`() = runTest {
        // Given
        val exception = RuntimeException("Connection refused")

        // When
        val error = errorHandler.handleNetworkError(exception)

        // Then
        assertTrue(error is SamaError.NetworkError, "Should be NetworkError")
        val networkError = error as SamaError.NetworkError
        assertEquals("SAMA_NETWORK_ERROR", networkError.code)
        assertEquals("Network connection error", networkError.message)
        assertEquals("Unable to connect. Please check your internet connection", networkError.userFriendlyMessage)
    }

    @Test
    fun `should determine retry for retryable errors`() = runTest {
        // Given
        val networkTimeout = SamaError.NetworkTimeout("TIMEOUT", "Timeout", "Details", "User message")
        val serverError = SamaError.ServerError("SERVER", "Server error", "Details", "User message")
        val rateLimited = SamaError.RateLimited("RATE", "Rate limited", "Details", 60L, "User message")
        val badRequest = SamaError.BadRequest("BAD", "Bad request", "Details", "User message")

        // When & Then
        assertTrue(errorHandler.shouldRetry(networkTimeout, 1, 3), "Should retry network timeout")
        assertTrue(errorHandler.shouldRetry(serverError, 1, 3), "Should retry server error")
        assertTrue(errorHandler.shouldRetry(rateLimited, 1, 3), "Should retry rate limited")
        assertFalse(errorHandler.shouldRetry(badRequest, 1, 3), "Should not retry bad request")
    }

    @Test
    fun `should not retry when max attempts reached`() = runTest {
        // Given
        val networkTimeout = SamaError.NetworkTimeout("TIMEOUT", "Timeout", "Details", "User message")

        // When & Then
        assertFalse(errorHandler.shouldRetry(networkTimeout, 3, 3), "Should not retry when max attempts reached")
        assertFalse(errorHandler.shouldRetry(networkTimeout, 5, 3), "Should not retry when attempts exceed max")
    }

    @Test
    fun `should calculate retry delay correctly`() = runTest {
        // Given
        val rateLimited = SamaError.RateLimited("RATE", "Rate limited", "Details", 120L, "User message")
        val networkTimeout = SamaError.NetworkTimeout("TIMEOUT", "Timeout", "Details", "User message")
        val badRequest = SamaError.BadRequest("BAD", "Bad request", "Details", "User message")

        // When
        val rateLimitDelay = errorHandler.calculateRetryDelay(rateLimited, 1)
        val timeoutDelay = errorHandler.calculateRetryDelay(networkTimeout, 1)
        val badRequestDelay = errorHandler.calculateRetryDelay(badRequest, 1)

        // Then
        assertEquals(120000L, rateLimitDelay, "Should use retry-after for rate limited")
        assertEquals(1000L, timeoutDelay, "Should use exponential backoff for timeout")
        assertEquals(0L, badRequestDelay, "Should not have delay for non-retryable errors")
    }

    @Test
    fun `should calculate exponential backoff correctly`() = runTest {
        // Given
        val networkTimeout = SamaError.NetworkTimeout("TIMEOUT", "Timeout", "Details", "User message")

        // When
        val delay1 = errorHandler.calculateRetryDelay(networkTimeout, 1)
        val delay2 = errorHandler.calculateRetryDelay(networkTimeout, 2)
        val delay3 = errorHandler.calculateRetryDelay(networkTimeout, 3)

        // Then
        assertEquals(1000L, delay1, "First retry should be 1 second")
        assertEquals(2000L, delay2, "Second retry should be 2 seconds")
        assertEquals(4000L, delay3, "Third retry should be 4 seconds")
    }

    @Test
    fun `should provide localized error messages`() = runTest {
        // Given
        val badRequest = SamaError.BadRequest("BAD", "Bad request", "Details", "Please check your input and try again")

        // When
        val englishMessage = errorHandler.getUserFriendlyMessage(badRequest, "en")
        val arabicMessage = errorHandler.getUserFriendlyMessage(badRequest, "ar")

        // Then
        assertEquals("Please check your input and try again", englishMessage, "Should return English message")
        assertEquals("يرجى التحقق من البيانات المدخلة والمحاولة مرة أخرى", arabicMessage, "Should return Arabic message")
    }

    @Test
    fun `should identify retryable errors correctly`() = runTest {
        // Given
        val retryableErrors = listOf(
            SamaError.NetworkTimeout("TIMEOUT", "Timeout", "Details", "User message"),
            SamaError.NetworkError("NETWORK", "Network error", "Details", "User message"),
            SamaError.ServerError("SERVER", "Server error", "Details", "User message"),
            SamaError.RateLimited("RATE", "Rate limited", "Details", 60L, "User message")
        )
        val nonRetryableErrors = listOf(
            SamaError.BadRequest("BAD", "Bad request", "Details", "User message"),
            SamaError.Unauthorized("UNAUTH", "Unauthorized", "Details", "User message"),
            SamaError.Forbidden("FORBIDDEN", "Forbidden", "Details", "User message")
        )

        // When & Then
        retryableErrors.forEach { error ->
            assertTrue(errorHandler.isRetryableError(error), "Should identify ${error::class.simpleName} as retryable")
        }
        nonRetryableErrors.forEach { error ->
            assertFalse(errorHandler.isRetryableError(error), "Should identify ${error::class.simpleName} as non-retryable")
        }
    }

    @Test
    fun `should log errors correctly`() = runTest {
        // Given
        val error = SamaError.ServerError("SERVER", "Server error", "Details", "User message")
        val context = "Payment initiation"
        val userId = "user123"

        // When - This would normally be tested with a mock logger
        errorHandler.logError(error, context, userId)

        // Then - In a real test, you'd verify the log was written correctly
        // For this mock, we just ensure it doesn't throw an exception
        assertTrue(true, "Error logging should complete without exception")
    }

    @Test
    fun `should handle unknown error codes`() = runTest {
        // Given
        val httpStatusCode = 418 // I'm a teapot
        val responseBody = """{"error": "Unknown error"}"""
        val headers = emptyMap<String, String>()

        // When
        val error = errorHandler.handleApiError(httpStatusCode, responseBody, headers)

        // Then
        assertTrue(error is SamaError.UnknownError, "Should be UnknownError")
        val unknownError = error as SamaError.UnknownError
        assertEquals("SAMA_UNKNOWN_ERROR", unknownError.code)
        assertEquals("Unknown error occurred", unknownError.message)
        assertEquals("An unexpected error occurred", unknownError.userFriendlyMessage)
    }
}