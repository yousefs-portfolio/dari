/**
 * KtLint configuration for consistent code formatting across the project
 */

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set("1.0.1")
    android.set(true)
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
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
}