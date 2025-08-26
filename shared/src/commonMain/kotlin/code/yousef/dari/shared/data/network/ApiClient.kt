package code.yousef.dari.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * API Client wrapper for Ktor
 * Provides centralized HTTP client with error handling and authentication
 */
class ApiClient(
    private val httpClient: HttpClient,
    private val json: Json
) {
    
    /**
     * Perform GET request
     */
    suspend inline fun <reified T> get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return try {
            val response: HttpResponse = httpClient.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<T>())
            } else {
                Result.failure(ApiException(response.status, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform POST request
     */
    suspend inline fun <reified T> post(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return try {
            val response: HttpResponse = httpClient.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<T>())
            } else {
                Result.failure(ApiException(response.status, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform PUT request
     */
    suspend inline fun <reified T> put(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return try {
            val response: HttpResponse = httpClient.put(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<T>())
            } else {
                Result.failure(ApiException(response.status, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform DELETE request
     */
    suspend fun delete(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return try {
            val response: HttpResponse = httpClient.delete(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException(response.status, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Custom API Exception
 */
data class ApiException(
    val httpStatus: HttpStatusCode,
    val errorBody: String
) : Exception("HTTP ${httpStatus.value}: $errorBody")