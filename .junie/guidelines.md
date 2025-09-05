# Dari Project Guidelines

## Project Overview

**Dari** is a Smart Finance Tracker built with Kotlin Multiplatform (KMP) and Compose Multiplatform for Saudi Arabia's
financial ecosystem. The project features SAMA Open Banking integration, OCR receipt scanning, goal tracking, budget
forecasting, and Material Design 3 dynamic theming.

### Key Features

- SAMA Open Banking SDK for secure integration with Saudi banks
- Multi-platform support (iOS and Android from shared codebase)
- OCR receipt scanning with ML Kit/Vision
- AI-powered budget forecasting and recommendations
- Offline-first architecture with SQLDelight database
- Material Design 3 with dynamic theming

## Project Structure

```
dari/
├── app/                    # Android application module
├── shared/                 # Shared KMP module (UI + business logic)
├── sama-banking-sdk/       # SAMA Open Banking SDK module
├── iosApp/                # iOS application
├── buildSrc/              # Gradle build logic
├── gradle/                # Gradle configuration
└── docs/                  # Documentation
```

### Module Descriptions

- **app/**: Android-specific application code and resources
- **shared/**: Contains shared business logic, UI components, and domain layer
- **sama-banking-sdk/**: Dedicated SDK for SAMA Open Banking API integration
- **iosApp/**: iOS application with Swift UI components

## Testing Guidelines

### Test Execution

- **Run all tests**: Use `./gradlew test` (or `gradlew test` on Windows)
- **Android tests**: `./gradlew :app:testDebugUnitTest`
- **Shared module tests**: `./gradlew :shared:testDebugUnitTest`
- **SAMA SDK tests**: `./gradlew :sama-banking-sdk:test`

### Testing Strategy

- Follow TDD (Test-Driven Development) approach
- Maintain 80%+ coverage for SDK components
- Maintain 75%+ coverage for use cases
- Include integration tests for API calls and database operations
- Write UI tests for Compose components and user flows

### Test Requirements

Junie should **always run tests** related to modified files to ensure:

1. No regression issues are introduced
2. All existing functionality continues to work
3. New code is properly tested

## Build Instructions

### Building the Project

- **Android Debug**: `./gradlew :app:assembleDebug`
- **Android Release**: `./gradlew :app:assembleRelease`
- **iOS Framework**: `./gradlew :shared:assembleXCFramework` (macOS only)

### Build Verification

Junie should **build the project** when making significant changes to:

- Core SDK functionality
- Shared business logic
- Build configuration files

Use: `./gradlew build` to verify the entire project builds successfully.

## Code Style Guidelines

### Kotlin Conventions

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Maximum line length: 120 characters
- Use meaningful variable and function names
- Apply ktlint formatting: `./gradlew ktlintFormat`
- Check formatting: `./gradlew ktlintCheck`

### Architecture Patterns

- Clean Architecture + MVVM pattern
- Dependency injection with Koin
- Repository pattern for data access
- Use cases for business logic

### Security Requirements

- OAuth 2.0 with PKCE for authentication
- TLS 1.2+ for network connections
- Certificate pinning for production
- Encrypted storage for sensitive data

## Development Environment

### Requirements

- JDK 17+ for Kotlin compilation
- Android SDK API 24+ (Android 7.0)
- Kotlin 1.9.22+ for multiplatform support
- Gradle 8.2+ for build management

### Supported Platforms

- **Android**: API 24+ (Android 7.0)
- **iOS**: iOS 14.0+

## Special Considerations

### SAMA SDK Integration

The project includes a custom SAMA Open Banking SDK that requires:

- Proper credential configuration
- Sandbox/Production environment handling
- OAuth 2.0 authentication flows
- Certificate pinning for security

### Multiplatform Development

- Shared code lives in `shared/src/commonMain`
- Platform-specific implementations in respective platform directories
- Use expect/actual declarations for platform-specific functionality

When working with this project, prioritize testing, follow the established architecture patterns, and ensure security
requirements are met, especially when dealing with financial data and banking integrations.
