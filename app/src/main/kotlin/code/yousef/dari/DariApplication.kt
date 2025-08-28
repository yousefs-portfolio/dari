package code.yousef.dari

import android.app.Application

class DariApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Note: Koin DI initialization will be added when shared modules are ready

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
                .build(),
        )

        android.os.StrictMode.setVmPolicy(
            android.os.StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build(),
        )
    }
}
