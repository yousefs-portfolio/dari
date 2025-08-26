package code.yousef.dari.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import code.yousef.dari.shared.database.DariDatabase

/**
 * iOS-specific Database Driver Factory
 * Uses NativeSqliteDriver with SQLite configuration optimized for iOS
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val configuration = DatabaseConfiguration(
            name = DatabaseConfig.DATABASE_NAME,
            version = DatabaseConfig.DATABASE_VERSION,
            create = { connection ->
                wrapConnection(connection) { DariDatabase.Schema.create(it) }
            },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) { 
                    DariDatabase.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) 
                }
            },
            extendedConfig = DatabaseConfiguration.Extended(
                foreignKeyConstraints = DatabaseConfig.ENABLE_FOREIGN_KEYS,
                busyTimeout = 30000, // 30 seconds
                synchronousFlag = DatabaseConfiguration.Extended.SynchronousFlag.NORMAL,
                journalMode = if (DatabaseConfig.ENABLE_WAL_MODE) {
                    DatabaseConfiguration.Extended.JournalMode.WAL
                } else {
                    DatabaseConfiguration.Extended.JournalMode.DELETE
                }
            )
        )

        return NativeSqliteDriver(
            configuration = configuration,
            onConfiguration = { connection ->
                // iOS-specific configuration
                connection.execute("PRAGMA cache_size = ${DatabaseConfig.CACHE_SIZE}", null)
                connection.execute("PRAGMA auto_vacuum = ${DatabaseConfig.AUTO_VACUUM}", null)
                connection.execute("PRAGMA temp_store = MEMORY", null)
                connection.execute("PRAGMA case_sensitive_like = ON", null)
                
                // iOS memory and performance optimizations
                connection.execute("PRAGMA mmap_size = 134217728", null) // 128MB for iOS
                connection.execute("PRAGMA optimize", null) // Analyze and optimize
            }
        )
    }
}