package code.yousef.dari.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.runBlocking
import code.yousef.dari.shared.database.DariDatabase as GeneratedDatabase

/**
 * Database Wrapper Class
 * Provides convenient access to DAOs and database operations
 * Implements repository pattern for data access
 */
class DariDatabase(
    private val driver: SqlDriver
) {
    private val database = GeneratedDatabase(driver)
    
    // DAO access
    fun accountDao() = database.accountQueries
    fun transactionDao() = database.transactionQueries
    
    // Future DAOs will be added here as they're implemented
    // fun budgetDao() = database.budgetQueries
    // fun goalDao() = database.goalQueries
    // fun categoryDao() = database.categoryQueries
    
    /**
     * Execute a transaction with automatic rollback on failure
     */
    suspend fun <T> withTransaction(block: suspend () -> T): T {
        return try {
            database.transactionWithResult {
                runBlocking { block() }
            }
        } catch (e: Exception) {
            // Log error and rethrow
            println("Database transaction failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Close database connection
     */
    fun close() {
        driver.close()
    }
    
    /**
     * Vacuum database to reclaim space
     */
    suspend fun vacuum() {
        database.transaction {
            driver.execute(null, "VACUUM", 0)
        }
    }
    
    /**
     * Get database file size in bytes (if supported by platform)
     */
    fun getDatabaseSize(): Long {
        return try {
            // This would be implemented per platform
            0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Analyze database for optimization
     */
    suspend fun analyze() {
        database.transaction {
            driver.execute(null, "ANALYZE", 0)
        }
    }
}