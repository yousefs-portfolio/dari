/**
 * Centralized dependency management for Dari KMP project
 * Contains all version numbers and dependency declarations
 */
object Dependencies {

    object Versions {
        const val kotlin = "2.0.21"
        const val ksp = "2.0.21-1.0.27"
        const val agp = "8.12.1"
        
        // Compose
        const val compose = "1.7.1"
        const val composeCompiler = "1.5.15"
        const val composeBom = "2024.12.01"
        const val activityCompose = "1.9.3"
        const val navigationCompose = "2.8.5"
        
        // AndroidX
        const val coreKtx = "1.13.1"
        const val lifecycle = "2.8.7"
        const val appcompat = "1.7.0"
        const val material = "1.12.0"
        const val material3 = "1.3.1"
        const val biometric = "1.2.0"
        const val security = "1.1.0"
        const val work = "2.10.0"
        const val camera = "1.4.0"
        
        // Networking & Serialization
        const val ktor = "3.0.2"
        const val kotlinxSerialization = "1.7.3"
        const val kotlinxDatetime = "0.6.1"
        
        // Database
        const val sqldelight = "2.0.2"
        
        // DI & Architecture
        const val koin = "4.0.0"
        const val coroutines = "1.9.0"
        
        // Navigation
        const val voyager = "1.1.0"
        
        // Image Loading
        const val kamel = "1.0.0"
        
        // ML & Vision
        const val mlKit = "16.0.1"
        
        // Testing
        const val junit = "4.13.2"
        const val junitAndroid = "1.2.1"
        const val espresso = "3.6.1"
        
        // Code Quality
        const val ktlint = "12.1.0"
        const val detekt = "1.23.4"
    }

    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
        const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
        const val material = "com.google.android.material:material:${Versions.material}"
        const val biometric = "androidx.biometric:biometric:${Versions.biometric}"
        const val securityCrypto = "androidx.security:security-crypto:${Versions.security}"
        const val workRuntime = "androidx.work:work-runtime-ktx:${Versions.work}"
        
        // Lifecycle
        const val lifecycleViewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
        const val lifecycleViewmodelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}"
        const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycle}"
        
        // Camera
        const val cameraCamera2 = "androidx.camera:camera-camera2:${Versions.camera}"
        const val cameraLifecycle = "androidx.camera:camera-lifecycle:${Versions.camera}"
        const val cameraView = "androidx.camera:camera-view:${Versions.camera}"
    }

    object Compose {
        const val bom = "androidx.compose:compose-bom:${Versions.composeBom}"
        const val ui = "androidx.compose.ui:ui"
        const val uiPreview = "androidx.compose.ui:ui-tooling-preview"
        const val uiTooling = "androidx.compose.ui:ui-tooling"
        const val uiTestManifest = "androidx.compose.ui:ui-test-manifest"
        const val uiTestJunit4 = "androidx.compose.ui:ui-test-junit4"
        const val material3 = "androidx.compose.material3:material3:${Versions.material3}"
        const val iconsExtended = "androidx.compose.material:material-icons-extended"
        const val activity = "androidx.activity:activity-compose:${Versions.activityCompose}"
        const val navigation = "androidx.navigation:navigation-compose:${Versions.navigationCompose}"
    }

    object Networking {
        const val ktorCore = "io.ktor:ktor-client-core:${Versions.ktor}"
        const val ktorAndroid = "io.ktor:ktor-client-android:${Versions.ktor}"
        const val ktorIos = "io.ktor:ktor-client-ios:${Versions.ktor}"
        const val ktorLogging = "io.ktor:ktor-client-logging:${Versions.ktor}"
        const val ktorContentNegotiation = "io.ktor:ktor-client-content-negotiation:${Versions.ktor}"
        const val ktorSerialization = "io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}"
    }

    object Database {
        const val sqldelightRuntime = "app.cash.sqldelight:runtime:${Versions.sqldelight}"
        const val sqldelightAndroid = "app.cash.sqldelight:android-driver:${Versions.sqldelight}"
        const val sqldelightNative = "app.cash.sqldelight:native-driver:${Versions.sqldelight}"
        const val sqldelightCoroutines = "app.cash.sqldelight:coroutines-extensions:${Versions.sqldelight}"
    }

    object DI {
        const val koinCore = "io.insert-koin:koin-core:${Versions.koin}"
        const val koinAndroid = "io.insert-koin:koin-android:${Versions.koin}"
        const val koinCompose = "io.insert-koin:koin-androidx-compose:${Versions.koin}"
        const val koinTest = "io.insert-koin:koin-test:${Versions.koin}"
    }

    object Kotlinx {
        const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
        const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}"
        const val datetime = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}"
    }

    object Navigation {
        const val voyagerNavigator = "cafe.adriel.voyager:voyager-navigator:${Versions.voyager}"
        const val voyagerBottomSheet = "cafe.adriel.voyager:voyager-bottom-sheet-navigator:${Versions.voyager}"
        const val voyagerTab = "cafe.adriel.voyager:voyager-tab-navigator:${Versions.voyager}"
        const val voyagerTransitions = "cafe.adriel.voyager:voyager-transitions:${Versions.voyager}"
        const val voyagerKoin = "cafe.adriel.voyager:voyager-koin:${Versions.voyager}"
    }

    object Image {
        const val kamel = "media.kamel:kamel-image:${Versions.kamel}"
    }

    object ML {
        const val textRecognition = "com.google.mlkit:text-recognition:${Versions.mlKit}"
    }

    object Testing {
        const val junit = "junit:junit:${Versions.junit}"
        const val junitAndroid = "androidx.test.ext:junit:${Versions.junitAndroid}"
        const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test"
    }

    object Plugins {
        const val androidApplication = "com.android.application"
        const val androidLibrary = "com.android.library"
        const val kotlinAndroid = "org.jetbrains.kotlin.android"
        const val kotlinMultiplatform = "org.jetbrains.kotlin.multiplatform"
        const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
        const val composeCompiler = "org.jetbrains.kotlin.plugin.compose"
        const val composeMultiplatform = "org.jetbrains.compose"
        const val ksp = "com.google.devtools.ksp"
        const val sqldelight = "app.cash.sqldelight"
        const val ktlint = "org.jlleitschuh.gradle.ktlint"
    }
}