plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
    implementation("com.android.tools.build:gradle:8.12.1")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.0")
}