package code.yousef.dari

import android.app.Application
import android.content.Context
import code.yousef.dari.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class DariApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin DI
        initKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@DariApplication)
        }
        
        // Initialize strict mode in debug builds
        if (BuildConfig.DEBUG_MODE) {
            enableStrictMode()
        }
    }

    private fun enableStrictMode() {
        // Enable StrictMode for better debugging
        android.os.StrictMode.setThreadPolicy(
            android.os.StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )

        android.os.StrictMode.setVmPolicy(
            android.os.StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}