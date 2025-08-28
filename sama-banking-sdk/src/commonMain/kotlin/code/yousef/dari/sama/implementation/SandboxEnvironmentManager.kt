package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.models.BankConfiguration
import kotlinx.datetime.Clock

/**
 * SAMA Sandbox Environment Manager
 * Manages sandbox-specific configurations and provides utilities for testing
 */
class SandboxEnvironmentManager {

    private val bankRegistry = BankConfigurationRegistry()

    companion object {
        // Simple in-memory property store for multiplatform compatibility
        private val properties = mutableMapOf<String, String>()
        const val SANDBOX_ENV = "sandbox"
        const val PRODUCTION_ENV = "production" 
        const val DEVELOPMENT_ENV = "development"

        // SAMA Sandbox Base URLs
        private val SAMA_SANDBOX_ENDPOINTS = mapOf(
            "base_consent_api" to "https://sandbox-consent.sama.gov.sa/open-banking/v1",
            "base_account_api" to "https://sandbox-accounts.sama.gov.sa/open-banking/v1", 
            "base_payment_api" to "https://sandbox-payments.sama.gov.sa/open-banking/v1",
            "base_auth_api" to "https://sandbox-auth.sama.gov.sa/oauth2"
        )

        // Test Data Constants
        val SANDBOX_TEST_ACCOUNTS = mapOf(
            "alrajhi" to listOf(
                "SA4420000001234567890123456", // Current Account - 15000 SAR
                "SA4420000001234567890123457", // Savings Account - 25000 SAR
                "SA4420000001234567890123458"  // Investment Account - 50000 SAR
            ),
            "snb" to listOf(
                "SA0310000001234567890123456", // Current Account - 20000 SAR
                "SA0310000001234567890123457", // Savings Account - 35000 SAR
                "SA0310000001234567890123458"  // Business Account - 75000 SAR
            ),
            "riyadbank" to listOf(
                "SA1520000001234567890123456", // Current Account - 18000 SAR
                "SA1520000001234567890123457"  // Savings Account - 28000 SAR
            ),
            "sabb" to listOf(
                "SA4540000001234567890123456", // Multi-Currency Account - 22000 SAR
                "SA4540000001234567890123457"  // USD Account - 5000 USD
            ),
            "alinma" to listOf(
                "SA0580000001234567890123456", // Islamic Current - 16000 SAR
                "SA0580000001234567890123457"  // Islamic Savings - 30000 SAR
            ),
            "albilad" to listOf(
                "SA7370000001234567890123456", // Current Account - 14000 SAR
                "SA7370000001234567890123457"  // Savings Account - 21000 SAR
            ),
            "stcpay" to listOf(
                "STCPAY001234567890123456",     // Digital Wallet - 5000 SAR
                "STCPAY001234567890123457"      // Business Wallet - 15000 SAR
            )
        )

        // Sandbox Test Credentials
        val SANDBOX_TEST_CREDENTIALS = mapOf(
            "test_user_1" to mapOf(
                "username" to "ahmad.mohammed",
                "password" to "Test123!",
                "phone" to "+966501234567",
                "national_id" to "1234567890"
            ),
            "test_user_2" to mapOf(
                "username" to "fatima.ali",
                "password" to "Test456!",
                "phone" to "+966507654321", 
                "national_id" to "0987654321"
            ),
            "business_user" to mapOf(
                "username" to "business.test",
                "password" to "Business789!",
                "commercial_registration" to "1010123456",
                "vat_number" to "300012345678903"
            )
        )
    }

    /**
     * Get sandbox configuration for a specific bank
     */
    fun getSandboxConfiguration(bankCode: String): BankConfiguration? {
        return bankRegistry.getBankConfiguration(bankCode, SANDBOX_ENV)
    }

    /**
     * Get all available sandbox configurations
     */
    fun getAllSandboxConfigurations(): List<BankConfiguration> {
        return bankRegistry.getAllBanks(SANDBOX_ENV)
    }

    /**
     * Check if sandbox environment is properly configured for a bank
     */
    fun isSandboxConfigured(bankCode: String): Boolean {
        val config = getSandboxConfiguration(bankCode)
        return config != null && 
               config.environment == SANDBOX_ENV &&
               config.baseUrl.contains("sandbox", ignoreCase = true) &&
               config.clientId.contains("sandbox", ignoreCase = true)
    }

    /**
     * Validate sandbox environment configuration
     */
    fun validateSandboxConfiguration(bankCode: String): Result<Boolean> {
        return try {
            val config = getSandboxConfiguration(bankCode)
                ?: return Result.failure(Exception("Sandbox configuration not found for bank: $bankCode"))

            // Validate URLs contain sandbox indicators
            val urlChecks = listOf(
                config.baseUrl to "baseUrl",
                config.authorizationEndpoint to "authorizationEndpoint", 
                config.tokenEndpoint to "tokenEndpoint",
                config.parEndpoint to "parEndpoint"
            )

            urlChecks.forEach { (url, field) ->
                if (!url.contains("sandbox", ignoreCase = true) && 
                    !url.contains("test", ignoreCase = true) &&
                    !url.contains("dev", ignoreCase = true)) {
                    return Result.failure(Exception("$field does not appear to be sandbox URL: $url"))
                }
                
                if (!url.startsWith("https://")) {
                    return Result.failure(Exception("$field must use HTTPS: $url"))
                }
            }

            // Validate client ID is sandbox-specific
            if (!config.clientId.contains("sandbox", ignoreCase = true) &&
                !config.clientId.contains("test", ignoreCase = true)) {
                return Result.failure(Exception("Client ID does not appear to be for sandbox: ${config.clientId}"))
            }

            // Validate certificate fingerprints are configured
            if (config.certificateFingerprints.isEmpty()) {
                return Result.failure(Exception("Certificate fingerprints not configured for sandbox"))
            }

            // Validate supported scopes include required permissions
            val requiredScopes = listOf("accounts", "payments")
            requiredScopes.forEach { scope ->
                if (!config.supportedScopes.contains(scope)) {
                    return Result.failure(Exception("Required scope '$scope' not supported in sandbox"))
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get test accounts for a specific bank in sandbox
     */
    fun getTestAccounts(bankCode: String): List<String> {
        return SANDBOX_TEST_ACCOUNTS[bankCode] ?: emptyList()
    }

    /**
     * Get test user credentials for sandbox testing
     */
    fun getTestCredentials(userType: String = "test_user_1"): Map<String, String> {
        return SANDBOX_TEST_CREDENTIALS[userType] ?: emptyMap()
    }

    /**
     * Create sandbox-specific HTTP client configuration
     */
    fun createSandboxClientConfig(bankCode: String): Map<String, Any>? {
        val config = getSandboxConfiguration(bankCode) ?: return null
        
        return mapOf(
            "baseUrl" to config.baseUrl,
            "clientId" to config.clientId,
            "certificatePins" to config.certificateFingerprints,
            "timeout" to 30_000L, // Increased timeout for sandbox
            "retryAttempts" to 3,
            "rateLimits" to config.rateLimits,
            "environment" to SANDBOX_ENV
        )
    }

    /**
     * Generate sandbox test consent request
     */
    fun generateTestConsentRequest(): Map<String, Any> {
        return mapOf(
            "permissions" to listOf(
                "ReadAccountsBasic",
                "ReadAccountsDetail", 
                "ReadBalances",
                "ReadTransactionsBasic",
                "ReadTransactionsDetail",
                "ReadStandingOrders",
                "ReadDirectDebits"
            ),
            "expirationDateTime" to "2024-12-31T23:59:59Z",
            "transactionFromDateTime" to "2024-01-01T00:00:00Z",
            "transactionToDateTime" to "2024-12-31T23:59:59Z"
        )
    }

    /**
     * Generate sandbox test payment request
     */
    fun generateTestPaymentRequest(
        fromAccount: String,
        toAccount: String,
        amount: String = "100.00",
        currency: String = "SAR"
    ): Map<String, Any> {
        return mapOf(
            "instructionIdentification" to "SANDBOX-PAY-${Clock.System.now().toEpochMilliseconds()}",
            "endToEndIdentification" to "SANDBOX-E2E-${Clock.System.now().toEpochMilliseconds()}",
            "instructedAmount" to mapOf(
                "amount" to amount,
                "currency" to currency
            ),
            "debtorAccount" to mapOf(
                "iban" to fromAccount
            ),
            "creditorAccount" to mapOf(
                "iban" to toAccount
            ),
            "creditorName" to "Test Beneficiary",
            "remittanceInformation" to mapOf(
                "unstructured" to listOf("Sandbox test payment")
            )
        )
    }

    /**
     * Check if we're currently running in sandbox mode
     */
    fun isRunningInSandbox(): Boolean {
        // Can be determined by environment variables, build configuration, etc.
        return (properties["sama.environment"] ?: "production") == SANDBOX_ENV
    }

    /**
     * Switch environment for testing purposes
     */
    fun switchToSandbox(): String {
        properties["sama.environment"] = SANDBOX_ENV
        return SANDBOX_ENV
    }

    /**
     * Switch back to production environment
     */
    fun switchToProduction(): String {
        properties["sama.environment"] = PRODUCTION_ENV
        return PRODUCTION_ENV
    }

    /**
     * Get current active environment
     */
    fun getCurrentEnvironment(): String {
        return properties["sama.environment"] ?: PRODUCTION_ENV
    }

    /**
     * Reset sandbox state (useful for testing)
     */
    fun resetSandboxState() {
        // Clear any cached sandbox state
        properties.remove("sama.environment")
    }

    /**
     * Get SAMA official sandbox endpoints
     */
    fun getSamaOfficialSandboxEndpoints(): Map<String, String> {
        return SAMA_SANDBOX_ENDPOINTS.toMap()
    }

    /**
     * Validate connection to sandbox environment
     */
    suspend fun validateSandboxConnection(bankCode: String): Result<Boolean> {
        return try {
            val config = getSandboxConfiguration(bankCode)
                ?: return Result.failure(Exception("Sandbox configuration not found"))

            // Basic validation - in real implementation would make HTTP requests
            val isValid = config.environment == SANDBOX_ENV &&
                         config.baseUrl.startsWith("https://") &&
                         config.clientId.isNotBlank()

            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}