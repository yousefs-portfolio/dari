/**
 * Android build configuration constants
 */
object AndroidConfig {
    const val compileSdk = 35
    const val minSdk = 24
    const val targetSdk = 35
    const val versionCode = 1
    const val versionName = "1.0.0"
    
    const val applicationId = "code.yousef.dari"
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    const val jvmTarget = "11"
    const val kotlinCompilerExtensionVersion = "1.5.15"
    
    object ProGuard {
        const val androidOptimizedRules = "proguard-android-optimize.txt"
        const val rules = "proguard-rules.pro"
        const val consumerRules = "consumer-rules.pro"
    }
    
    object BuildTypes {
        const val debug = "debug"
        const val release = "release"
        const val staging = "staging"
    }
    
    object Flavors {
        const val development = "development"
        const val staging = "staging"
        const val production = "production"
    }
}