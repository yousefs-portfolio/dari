# Dari Finance App - ProGuard/R8 Configuration

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class code.yousef.dari.**$$serializer { *; }
-keepclassmembers class code.yousef.dari.** {
    *** Companion;
}
-keepclasseswithmembers class code.yousef.dari.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Koin
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.dsl.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class code.yousef.dari.shared.database.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class androidx.compose.** { *; }

# Voyager Navigation
-keep class cafe.adriel.voyager.** { *; }

# Domain Models - Keep all data classes
-keep class code.yousef.dari.shared.domain.models.** { *; }
-keep class code.yousef.dari.sama.models.** { *; }

# SAMA Banking SDK
-keep class code.yousef.dari.sama.** { *; }
-keep interface code.yousef.dari.sama.interfaces.** { *; }

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Optimize and shrink resources
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep BuildConfig
-keep class code.yousef.dari.BuildConfig { *; }

# WebView JavaScript Interface (if used later for OAuth)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Biometric authentication
-keep class androidx.biometric.** { *; }

# Camera X
-keep class androidx.camera.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**