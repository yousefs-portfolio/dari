plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

android {
    namespace = "code.yousef.dari"
    compileSdk = 36

    defaultConfig {
        applicationId = "code.yousef.dari"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            buildConfigField("String", "BASE_URL", "\"https://dev-api.dari.app/v1\"")
            buildConfigField("String", "SAMA_BASE_URL", "\"https://sandbox-api.bank.com.sa/open-banking/v1\"")
            buildConfigField("String", "ENVIRONMENT", "\"DEV\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Boolean", "ENABLE_MOCK_DATA", "true")

            resValue("string", "app_name", "Dari Dev")
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            buildConfigField("String", "BASE_URL", "\"https://staging-api.dari.app/v1\"")
            buildConfigField("String", "SAMA_BASE_URL", "\"https://staging-api.bank.com.sa/open-banking/v1\"")
            buildConfigField("String", "ENVIRONMENT", "\"STAGING\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Boolean", "ENABLE_MOCK_DATA", "false")

            resValue("string", "app_name", "Dari Staging")
        }

        create("prod") {
            dimension = "environment"

            buildConfigField("String", "BASE_URL", "\"https://api.dari.app/v1\"")
            buildConfigField("String", "SAMA_BASE_URL", "\"https://api.bank.com.sa/open-banking/v1\"")
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            buildConfigField("Boolean", "ENABLE_MOCK_DATA", "false")

            resValue("string", "app_name", "Dari")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("Boolean", "DEBUG_MODE", "true")
            buildConfigField("Boolean", "ENABLE_STRICT_MODE", "true")

            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            buildConfigField("Boolean", "DEBUG_MODE", "false")
            buildConfigField("Boolean", "ENABLE_STRICT_MODE", "false")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug") // TODO: Use release signing
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Project modules
    implementation(project(":shared"))
    implementation(project(":sama-banking-sdk"))

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // ViewModel and Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Navigation
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.bottom.sheet.navigator)
    implementation(libs.voyager.tab.navigator)
    implementation(libs.voyager.transitions)
    implementation(libs.voyager.koin)

    // Image Loading
    implementation(libs.kamel.image)

    // Android Specific
    implementation(libs.mlkit.text.recognition)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.biometric)
    implementation(libs.security.crypto)
    implementation(libs.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.koin.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

ktlint {
    version.set("1.0.1")
    android.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("**/resources/**")
    }

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}
