# Shared Module Consumer ProGuard Rules
# Keep all public API classes
-keep public class code.yousef.dari.shared.** { *; }

# Keep data models for serialization
-keep @kotlinx.serialization.Serializable class ** {
    *;
}

# SQLDelight
-keep class **.*Queries { *; }
-keep class app.cash.sqldelight.** { *; }

# Koin
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }