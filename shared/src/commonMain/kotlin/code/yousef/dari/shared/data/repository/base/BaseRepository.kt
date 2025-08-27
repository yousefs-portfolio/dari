package code.yousef.dari.shared.data.repository.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base repository class providing common functionality and patterns
 * Implements consistent error handling and context switching
 */
abstract class BaseRepository {

    /**
     * Executes a database operation with proper context switching and error handling
     * @param operation The operation to execute
     * @return Result wrapper with success or failure
     */
    protected suspend fun <T> safeDbOperation(
        operation: suspend () -> T
    ): Result<T> {
        return withContext(Dispatchers.Default) {
            try {
                val result = operation()
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a database operation that returns Unit with proper context switching and error handling
     * @param operation The operation to execute
     * @return Result wrapper with success or failure
     */
    protected suspend fun safeDbAction(
        operation: suspend () -> Unit
    ): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                operation()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a list operation with proper context switching and error handling
     * Maps database entities to domain models
     * @param operation The operation that returns a list of database entities
     * @param mapper Function to map database entity to domain model
     * @return Result wrapper with mapped list or failure
     */
    protected suspend fun <DbEntity, DomainModel> safeListOperation(
        operation: suspend () -> List<DbEntity>,
        mapper: (DbEntity) -> DomainModel
    ): Result<List<DomainModel>> {
        return withContext(Dispatchers.Default) {
            try {
                val entities = operation()
                val domainModels = entities.map(mapper)
                Result.success(domainModels)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a nullable operation with proper context switching and error handling
     * @param operation The operation that may return null
     * @param mapper Function to map database entity to domain model
     * @return Result wrapper with mapped object, null, or failure
     */
    protected suspend fun <DbEntity, DomainModel> safeNullableOperation(
        operation: suspend () -> DbEntity?,
        mapper: (DbEntity) -> DomainModel
    ): Result<DomainModel?> {
        return withContext(Dispatchers.Default) {
            try {
                val entity = operation()
                val domainModel = entity?.let(mapper)
                Result.success(domainModel)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}