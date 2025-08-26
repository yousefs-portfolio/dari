/**
 * Common Kotlin Multiplatform configuration
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    // JVM target for desktop and testing
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = AndroidConfig.jvmTarget
            }
        }
    }
    
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = AndroidConfig.jvmTarget
            }
        }
    }
    
    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(Dependencies.Kotlinx.coroutinesCore)
            implementation(Dependencies.Kotlinx.serialization)
            implementation(Dependencies.Kotlinx.datetime)
        }
        
        commonTest.dependencies {
            implementation(Dependencies.Testing.kotlinTest)
            implementation(Dependencies.Kotlinx.coroutinesTest)
        }
        
        androidMain.dependencies {
            implementation(Dependencies.Kotlinx.coroutinesAndroid)
        }
        
        iosMain.dependencies {
            // iOS specific dependencies
        }
        
        androidUnitTest.dependencies {
            implementation(Dependencies.Testing.junit)
        }
        
        iosTest.dependencies {
            // iOS test dependencies
        }
        
        jvmTest.dependencies {
            implementation(Dependencies.Testing.junit)
        }
    }
}