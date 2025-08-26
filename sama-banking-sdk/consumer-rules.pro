# SAMA Banking SDK Consumer ProGuard Rules
# Keep all public API classes
-keep public class code.yousef.dari.sama.** { *; }

# Keep data models for serialization
-keep @kotlinx.serialization.Serializable class ** {
    *;
}

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses, Signature, Exception
-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable

# Ktor client
-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class kotlinx.coroutines.** { *; }

# OkHttp (used by Ktor Android)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okio.**
-dontwarn okhttp3.**

# Security
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }