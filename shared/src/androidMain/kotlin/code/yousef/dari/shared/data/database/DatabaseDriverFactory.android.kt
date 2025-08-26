package code.yousef.dari.shared.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import code.yousef.dari.shared.database.DariDatabase

/**
 * Android-specific Database Driver Factory
 * Uses AndroidSqliteDriver with optional SQLCipher encryption
 */
actual class DatabaseDriverFactory(
    private val context: Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = DariDatabase.Schema,
            context = context,
            name = DatabaseConfig.DATABASE_NAME,
            callback = AndroidDatabaseCallback()
        )
    }

    /**
     * Android Database Callback for configuration and migrations
     */
    private class AndroidDatabaseCallback : AndroidSqliteDriver.Callback(DariDatabase.Schema) {
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            
            // Enable foreign keys
            if (DatabaseConfig.ENABLE_FOREIGN_KEYS) {
                db.execSQL("PRAGMA foreign_keys = ON")
            }
            
            // Enable WAL mode for better concurrency
            if (DatabaseConfig.ENABLE_WAL_MODE) {
                db.execSQL("PRAGMA journal_mode = WAL")
            }
            
            // Set cache size for performance
            db.execSQL("PRAGMA cache_size = ${DatabaseConfig.CACHE_SIZE}")
            
            // Configure auto vacuum
            db.execSQL("PRAGMA auto_vacuum = ${DatabaseConfig.AUTO_VACUUM}")
            
            // Performance optimizations
            db.execSQL("PRAGMA synchronous = NORMAL") // Balance between safety and performance
            db.execSQL("PRAGMA temp_store = MEMORY") // Store temporary tables in memory
            db.execSQL("PRAGMA mmap_size = 268435456") // 256MB memory-mapped I/O
        }

        override fun onConfigure(db: SupportSQLiteDatabase) {
            super.onConfigure(db)
            
            // Additional configuration before database is opened
            db.execSQL("PRAGMA case_sensitive_like = ON")
        }
    }
}