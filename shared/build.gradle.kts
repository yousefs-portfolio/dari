plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.moko.resources)
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "SharedApp"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // Project modules
            implementation(project(":sama-banking-sdk"))
            
            // Kotlin libraries
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Networking with Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // Database with SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            
            // Dependency Injection
            api(libs.koin.core)
            
            // Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.bottom.sheet.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            
            // Image loading
            implementation(libs.kamel.image)
            
            // Resources
            api(libs.moko.resources)
            api(libs.moko.resources.compose)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            
            // Compose Testing
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.driver.android)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.ios)
            implementation(libs.sqldelight.driver.native)
        }
        
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            // JVM-specific implementations can go here later
        }
        
        androidUnitTest.dependencies {
            implementation(libs.junit)
        }
        
        iosTest.dependencies {
            // iOS test dependencies
        }
        
        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}

sqldelight {
    databases {
        create("DariDatabase") {
            packageName.set("code.yousef.dari.shared.database")
        }
    }
}

multiplatformResources {
    resourcesPackage.set("code.yousef.dari.shared.resources")
    resourcesClassName.set("SharedRes")
}

ktlint {
    version.set("1.0.1")
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("**/resources/**")
        include("**/kotlin/**")
        include("**/java/**")
    }
    
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}