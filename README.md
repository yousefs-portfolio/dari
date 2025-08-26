# Smart Finance Tracker - KMP/Compose Multiplatform

A comprehensive financial management application for Saudi Arabia built with Kotlin Multiplatform (KMP) and Compose Multiplatform. Features SAMA Open Banking integration, OCR receipt scanning, goal tracking, budget forecasting, and Material Design 3 dynamic theming.

## Features

### Core Features
- **SAMA Open Banking SDK**: Secure integration with Saudi banks
- **Multi-platform**: iOS and Android from shared codebase
- **OCR Receipt Scanning**: Automatic expense tracking with ML Kit/Vision
- **Smart Budgeting**: AI-powered budget forecasting and recommendations
- **Goal Tracking**: Financial goal management with progress visualization
- **Offline-first**: SQLDelight database with sync capabilities
- **Material Design 3**: Dynamic theming with platform adaptations

### Supported Banks
- Al Rajhi Bank
- Saudi National Bank (SNB)
- Riyad Bank
- SABB
- Alinma Bank
- Bank Albilad
- STC Pay

## Architecture

### Tech Stack
- **Kotlin Multiplatform**: Shared business logic
- **Compose Multiplatform**: Unified UI framework
- **Clean Architecture + MVVM**: Scalable architecture pattern
- **Koin**: Dependency injection
- **SQLDelight**: Multiplatform database
- **Ktor Client**: Networking library
- **Voyager**: Navigation library

### Project Structure
```
dari/
├── app/                    # Android application module
├── shared/                 # Shared KMP module (UI + business logic)
├── sama-banking-sdk/       # SAMA Open Banking SDK module
├── buildSrc/              # Gradle build logic
├── gradle/                # Gradle configuration
│   └── libs.versions.toml # Version catalog
└── docs/                  # Documentation
    └── private/           # Development documentation
```

## Setup Instructions

### Prerequisites

#### Development Environment
- **JDK 17+** for Kotlin compilation
- **Android Studio Hedgehog+** for Android development
- **Xcode 15+** for iOS development (macOS only)
- **Kotlin 1.9.22+** for multiplatform support
- **Gradle 8.2+** for build management

#### SDK Requirements
- **Android SDK**: API 24+ (Android 7.0)
- **iOS**: iOS 14.0+
- **Compile SDK**: Android API 34

### Building the Project

#### 1. Clone the Repository
```bash
git clone [repository-url]
cd dari
```

#### 2. Setup Environment
```bash
# Ensure Java 17+ is active
java -version

# Verify Gradle wrapper
./gradlew --version
```

#### 3. Build Android App
```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (requires signing configuration)
./gradlew :app:assembleRelease
```

#### 4. Build iOS Framework (macOS only)
```bash
# Build iOS framework
./gradlew :shared:assembleXCFramework

# Open iOS project in Xcode
open iosApp/iosApp.xcodeproj
```

#### 5. Run Tests
```bash
# Run all tests
./gradlew test

# Run Android tests
./gradlew :app:testDebugUnitTest

# Run shared module tests
./gradlew :shared:testDebugUnitTest

# Run SAMA SDK tests
./gradlew :sama-banking-sdk:test
```

#### 6. Code Quality
```bash
# Run ktlint formatting
./gradlew ktlintFormat

# Check code formatting
./gradlew ktlintCheck

# Run static analysis
./gradlew detekt
```

### Configuration

#### Build Variants
The project supports three environments:
- **dev**: Development with mock data and verbose logging
- **staging**: Staging environment for testing
- **prod**: Production environment

#### Environment Variables
Configure the following in your local environment:

```bash
# SAMA API Configuration
SAMA_CLIENT_ID=your_client_id
SAMA_CLIENT_SECRET=your_client_secret

# Development URLs (configured per build variant)
DEV_API_URL=https://dev-api.dari.app/v1
STAGING_API_URL=https://staging-api.dari.app/v1
PROD_API_URL=https://api.dari.app/v1
```

#### Signing Configuration (Android)
Create `keystore.properties` in project root:
```properties
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

### SAMA SDK Integration

#### Authentication Flow
```kotlin
// Initialize SDK
val samaSdk = SamaOpenBankingSDK.Builder()
    .environment(Environment.SANDBOX)
    .clientId("your_client_id")
    .clientSecret("your_client_secret")
    .build()

// Authenticate user
val authResult = samaSdk.authenticate(
    bankId = "alrajhi",
    permissions = listOf(
        Permission.ACCOUNT_DETAILS,
        Permission.ACCOUNT_BALANCES,
        Permission.TRANSACTIONS
    )
)
```

#### API Usage
```kotlin
// Get accounts
val accounts = samaSdk.accountService.getAccounts()

// Get transactions
val transactions = samaSdk.accountService.getTransactions(
    accountId = "account_id",
    fromDate = LocalDate.now().minusMonths(3),
    toDate = LocalDate.now()
)

// Initiate payment
val payment = samaSdk.paymentService.initiateDomesticPayment(
    DebitAccount(accountId = "source_account"),
    CreditAccount(accountId = "target_account"),
    amount = Money(amount = "100.00", currency = "SAR")
)
```

### Development Guidelines

#### Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for automatic formatting
- Maximum line length: 120 characters
- Use meaningful variable and function names

#### Testing Strategy
- **TDD**: Write tests first, then implementation
- **Unit Tests**: 80%+ coverage for SDK, 75%+ for use cases
- **Integration Tests**: API calls and database operations
- **UI Tests**: Compose UI components and user flows

#### Git Workflow
```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Commit with conventional format
git commit -m "feat: add SAMA bank connection flow"

# Push and create PR
git push origin feature/your-feature-name
```

#### Security Requirements
- **OAuth 2.0 with PKCE** for authentication
- **TLS 1.2+** for all network connections
- **Certificate pinning** for production
- **Biometric authentication** for app access
- **Encrypted storage** for sensitive data
- **Audit logging** for financial operations

### Troubleshooting

#### Common Issues

**Build Fails with Plugin Version Conflicts**
```bash
# Clean build
./gradlew clean
rm -rf .gradle build

# Rebuild
./gradlew build
```

**iOS Framework Build Issues**
- Ensure Xcode Command Line Tools are installed
- Verify iOS deployment target is set correctly
- Check CocoaPods installation

**SAMA API Connection Issues**
- Verify sandbox credentials are correct
- Check network connectivity
- Review certificate pinning configuration

### Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Run code formatting and linting
6. Submit a pull request with detailed description

### License

[Add your license information here]

### Support

For development questions and support:
- Review the [development plan](docs/private/dev-plan.md)
- Check existing GitHub issues
- Create a new issue with detailed information

---

**Built with ❤️ for Saudi Arabia's financial ecosystem**