plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("application")
}

group = "code.yousef.dari.backend"
version = "1.0.0"

application {
    mainClass.set("code.yousef.dari.backend.ApplicationKt")
}

dependencies {
    // Shared module for common models and DTOs
    implementation(project(":shared"))

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Kotlinx libraries
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    // Database
    implementation("com.h2database:h2:2.2.224")

    // Koin for dependency injection
    implementation(libs.koin.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.koin.test)
}

kotlin {
    jvmToolchain(17)
}
