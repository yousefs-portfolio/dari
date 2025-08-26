package code.yousef.dari.shared.data.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Database Driver Factory
 * Provides platform-specific database driver implementation
 * Supports SQLite with optional encryption for sensitive financial data
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Database Configuration
 * Contains common database settings and migration information
 */
object DatabaseConfig {
    const val DATABASE_NAME = "dari_database.db"
    const val DATABASE_VERSION = 1
    
    // Encryption settings for production
    const val SHOULD_ENCRYPT = true
    const val ENCRYPTION_KEY_ALIAS = "dari_db_key"
    
    // Performance settings
    const val ENABLE_WAL_MODE = true
    const val ENABLE_FOREIGN_KEYS = true
    const val CACHE_SIZE = 2000 // Pages
    
    // Maintenance settings
    const val VACUUM_ON_STARTUP = false
    const val AUTO_VACUUM = "INCREMENTAL"
}