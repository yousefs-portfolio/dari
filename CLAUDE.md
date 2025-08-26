# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android Kotlin project named "Dari" - a smart finance tracker application following Clean Architecture principles. The project is currently in its foundation phase with plans to implement Kotlin Multiplatform (KMP) and Compose Multiplatform for cross-platform development.

## Development Commands

### Build and Testing
- **Build project**: `./gradlew build`
- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Run unit tests**: `./gradlew test`
- **Run instrumented tests**: `./gradlew connectedAndroidTest`
- **Run tests with coverage**: `./gradlew testDebugUnitTestCoverage`
- **Clean build**: `./gradlew clean`

### Code Quality
- **Run lint**: `./gradlew lint`
- **Format code**: `./gradlew ktlintFormat` (when ktlint is configured)
- **Check code style**: `./gradlew ktlintCheck` (when ktlint is configured)

### Dependencies
- **Update dependencies**: Check `gradle/libs.versions.toml` and update version references
- **Dependency check**: `./gradlew dependencies`

## Project Structure

### Current Configuration
- **Namespace**: `code.yousef.dari`
- **Application ID**: `code.yousef.dari`  
- **Compile SDK**: 36
- **Target SDK**: 36
- **Min SDK**: 24
- **Java Version**: 11
- **Kotlin JVM Target**: 11

### Key Files
- **Main build configuration**: `build.gradle.kts` (project root)
- **App module**: `app/build.gradle.kts`
- **Version catalog**: `gradle/libs.versions.toml`
- **Settings**: `settings.gradle.kts`
- **Gradle properties**: `gradle.properties`

### Dependencies (from libs.versions.toml)
- **Android Gradle Plugin**: 8.12.1
- **Kotlin**: 2.0.21
- **AndroidX Core KTX**: 1.10.1
- **AppCompat**: 1.6.1
- **Material Design**: 1.10.0
- **JUnit**: 4.13.2
- **AndroidX Test**: 1.1.5
- **Espresso**: 3.5.1

### Architecture Plan
This project is planned to evolve into a comprehensive financial management application using:
- **Kotlin Multiplatform** for shared business logic
- **Compose Multiplatform** for unified UI across Android and iOS
- **Clean Architecture + MVVM** pattern
- **Test-Driven Development (TDD)** methodology
- **SQLDelight** for database management
- **Ktor Client** for networking
- **Koin** for dependency injection

## Development Guidelines

### Code Organization
- Follow Clean Architecture principles with clear separation of layers
- Implement features using TDD approach - write tests first
- Use repository pattern for data access
- Implement use cases for business logic
- Follow MVVM pattern for UI layer

### Testing Strategy
- Write unit tests first (TDD approach)
- Maintain high test coverage (target 80%+ for critical components)
- Use both unit tests and instrumented tests
- Test ViewModels, repositories, and use cases thoroughly

### Security Considerations
- Implement secure storage for sensitive data
- Use proper authentication and authorization patterns
- Follow Android security best practices
- Prepare for financial data compliance requirements

### Future KMP Migration
- Structure code to facilitate future KMP conversion
- Keep business logic separate from platform-specific code
- Design interfaces that can be easily abstracted for multiplatform use
- Consider using expect/actual declarations for platform-specific implementations

## Financial Domain Features (Planned)

### Core Banking Integration
- SAMA Open Banking SDK integration for Saudi banks
- OAuth 2.0 with FAPI 1 Advanced standard
- Support for major Saudi banks (Al Rajhi, SNB, Riyad Bank, etc.)
- Account aggregation and transaction synchronization

### Key Features
- **Budget Management**: Creation, tracking, and forecasting
- **Goal Setting**: Financial goal tracking with progress visualization
- **Receipt Scanning**: OCR-powered receipt digitization
- **Smart Categorization**: AI-powered transaction categorization
- **Debt Management**: Payoff strategies (avalanche/snowball methods)
- **Analytics & Insights**: Spending patterns and recommendations
- **Zakat Calculator**: Saudi-specific religious calculation features

## Repository Context

This is the initial Android project setup that will serve as the foundation for a comprehensive financial management platform. The current structure contains only the basic Android project template, but the development plan in `docs/private/dev-plan.md` outlines an extensive 18-phase development process including SAMA Open Banking compliance, advanced financial features, and cross-platform capabilities.