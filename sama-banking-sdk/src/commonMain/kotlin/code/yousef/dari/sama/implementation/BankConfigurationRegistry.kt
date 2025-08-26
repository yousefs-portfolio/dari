package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.models.*

/**
 * Bank Configuration Registry
 * Central registry for all Saudi bank configurations supporting SAMA Open Banking
 * Contains configuration for all 7 supported banks with environment-specific settings
 */
class BankConfigurationRegistry {

    companion object {
        private const val PRODUCTION_ENV = "production"
        private const val SANDBOX_ENV = "sandbox"
        private const val DEVELOPMENT_ENV = "development"
    }

    private val configurations = mapOf(
        // Al Rajhi Bank - Largest Islamic bank in Saudi Arabia
        "alrajhi" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "alrajhi",
                bankName = "Al Rajhi Bank",
                bankNameAr = "مصرف الراجحي",
                baseUrl = "https://api.alrajhibank.com.sa/open-banking/v1",
                clientId = "alrajhi_prod_client",
                authorizationEndpoint = "https://identity.alrajhibank.com.sa/oauth2/authorize",
                tokenEndpoint = "https://identity.alrajhibank.com.sa/oauth2/token",
                parEndpoint = "https://identity.alrajhibank.com.sa/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions"),
                certificateFingerprints = listOf(
                    "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0",
                    "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
                ),
                rateLimits = RateLimits(requestsPerMinute = 100, requestsPerHour = 2000),
                features = BankFeatures(
                    maxTransactionAmount = "1000000.00",
                    supportedPaymentMethods = listOf("DOMESTIC_TRANSFER", "BILL_PAYMENT")
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@alrajhibank.com.sa",
                    supportPhone = "+966920003344",
                    technicalContactEmail = "api-support@alrajhibank.com.sa",
                    complianceEmail = "compliance@alrajhibank.com.sa",
                    website = "https://www.alrajhibank.com.sa",
                    privacyPolicyUrl = "https://www.alrajhibank.com.sa/en/privacy-policy",
                    termsOfServiceUrl = "https://www.alrajhibank.com.sa/en/terms-conditions"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "alrajhi",
                bankName = "Al Rajhi Bank",
                bankNameAr = "مصرف الراجحي",
                baseUrl = "https://sandbox-api.alrajhibank.com.sa/open-banking/v1",
                clientId = "alrajhi_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.alrajhibank.com.sa/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.alrajhibank.com.sa/oauth2/token",
                parEndpoint = "https://sandbox-identity.alrajhibank.com.sa/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions"),
                certificateFingerprints = listOf(
                    "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox-support@alrajhibank.com.sa",
                    supportPhone = "+966920003344",
                    technicalContactEmail = "sandbox-api@alrajhibank.com.sa",
                    complianceEmail = "compliance@alrajhibank.com.sa",
                    website = "https://developer.alrajhibank.com.sa"
                )
            )
        ),

        // Saudi National Bank (formerly NCB and Samba)
        "snb" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "snb",
                bankName = "Saudi National Bank",
                bankNameAr = "البنك الأهلي السعودي",
                baseUrl = "https://api.alahli.com/open-banking/v1",
                clientId = "snb_prod_client",
                authorizationEndpoint = "https://identity.alahli.com/oauth2/authorize",
                tokenEndpoint = "https://identity.alahli.com/oauth2/token",
                parEndpoint = "https://identity.alahli.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "standing-orders"),
                certificateFingerprints = listOf(
                    "B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1",
                    "C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1"
                ),
                rateLimits = RateLimits(requestsPerMinute = 120, requestsPerHour = 2500),
                features = BankFeatures(
                    maxTransactionAmount = "2000000.00",
                    supportsBulkPayments = true,
                    supportedPaymentMethods = listOf("DOMESTIC_TRANSFER", "BILL_PAYMENT", "BULK_TRANSFER")
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@alahli.com",
                    supportPhone = "+966920000232",
                    technicalContactEmail = "api-dev@alahli.com",
                    complianceEmail = "compliance@alahli.com",
                    website = "https://www.alahli.com",
                    privacyPolicyUrl = "https://www.alahli.com/privacy-policy",
                    termsOfServiceUrl = "https://www.alahli.com/terms-conditions"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "snb",
                bankName = "Saudi National Bank",
                bankNameAr = "البنك الأهلي السعودي",
                baseUrl = "https://sandbox.alahli.com/open-banking/v1",
                clientId = "snb_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.alahli.com/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.alahli.com/oauth2/token",
                parEndpoint = "https://sandbox-identity.alahli.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "standing-orders"),
                certificateFingerprints = listOf(
                    "B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox@alahli.com",
                    supportPhone = "+966920000232",
                    technicalContactEmail = "sandbox-api@alahli.com",
                    complianceEmail = "compliance@alahli.com",
                    website = "https://developer.alahli.com"
                )
            )
        ),

        // Riyad Bank
        "riyadbank" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "riyadbank",
                bankName = "Riyad Bank",
                bankNameAr = "بنك الرياض",
                baseUrl = "https://api.riyadbank.com/open-banking/v1",
                clientId = "riyadbank_prod_client",
                authorizationEndpoint = "https://identity.riyadbank.com/oauth2/authorize",
                tokenEndpoint = "https://identity.riyadbank.com/oauth2/token",
                parEndpoint = "https://identity.riyadbank.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "direct-debits"),
                certificateFingerprints = listOf(
                    "C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2",
                    "D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2"
                ),
                rateLimits = RateLimits(requestsPerMinute = 80, requestsPerHour = 1500),
                features = BankFeatures(
                    maxTransactionAmount = "1500000.00",
                    supportedPaymentMethods = listOf("DOMESTIC_TRANSFER", "BILL_PAYMENT", "DIRECT_DEBIT")
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@riyadbank.com",
                    supportPhone = "+966920000089",
                    technicalContactEmail = "api-support@riyadbank.com",
                    complianceEmail = "compliance@riyadbank.com",
                    website = "https://www.riyadbank.com",
                    privacyPolicyUrl = "https://www.riyadbank.com/privacy",
                    termsOfServiceUrl = "https://www.riyadbank.com/terms"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "riyadbank",
                bankName = "Riyad Bank",
                bankNameAr = "بنك الرياض",
                baseUrl = "https://sandbox-api.riyadbank.com/open-banking/v1",
                clientId = "riyadbank_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.riyadbank.com/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.riyadbank.com/oauth2/token",
                parEndpoint = "https://sandbox-identity.riyadbank.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "direct-debits"),
                certificateFingerprints = listOf(
                    "C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox@riyadbank.com",
                    supportPhone = "+966920000089",
                    technicalContactEmail = "sandbox-api@riyadbank.com",
                    complianceEmail = "compliance@riyadbank.com",
                    website = "https://developer.riyadbank.com"
                )
            )
        ),

        // The Saudi British Bank (SABB)
        "sabb" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "sabb",
                bankName = "The Saudi British Bank (SABB)",
                bankNameAr = "البنك السعودي البريطاني (ساب)",
                baseUrl = "https://api.sabb.com/open-banking/v1",
                clientId = "sabb_prod_client",
                authorizationEndpoint = "https://identity.sabb.com/oauth2/authorize",
                tokenEndpoint = "https://identity.sabb.com/oauth2/token",
                parEndpoint = "https://identity.sabb.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "statements"),
                certificateFingerprints = listOf(
                    "D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3",
                    "E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3"
                ),
                rateLimits = RateLimits(requestsPerMinute = 90, requestsPerHour = 1800),
                features = BankFeatures(
                    maxTransactionAmount = "1800000.00",
                    supportsInternationalPayments = true,
                    supportedCurrencies = listOf("SAR", "USD", "EUR", "GBP"),
                    supportedPaymentMethods = listOf("DOMESTIC_TRANSFER", "INTERNATIONAL_TRANSFER", "BILL_PAYMENT")
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@sabb.com",
                    supportPhone = "+966920008888",
                    technicalContactEmail = "api-team@sabb.com",
                    complianceEmail = "compliance@sabb.com",
                    website = "https://www.sabb.com",
                    privacyPolicyUrl = "https://www.sabb.com/privacy-policy",
                    termsOfServiceUrl = "https://www.sabb.com/terms"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "sabb",
                bankName = "The Saudi British Bank (SABB)",
                bankNameAr = "البنك السعودي البريطاني (ساب)",
                baseUrl = "https://sandbox-api.sabb.com/open-banking/v1",
                clientId = "sabb_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.sabb.com/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.sabb.com/oauth2/token",
                parEndpoint = "https://sandbox-identity.sabb.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "statements"),
                certificateFingerprints = listOf(
                    "D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox@sabb.com",
                    supportPhone = "+966920008888",
                    technicalContactEmail = "sandbox@sabb.com",
                    complianceEmail = "compliance@sabb.com",
                    website = "https://developer.sabb.com"
                )
            )
        ),

        // Alinma Bank
        "alinma" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "alinma",
                bankName = "Alinma Bank",
                bankNameAr = "مصرف الإنماء",
                baseUrl = "https://api.alinma.com/open-banking/v1",
                clientId = "alinma_prod_client",
                authorizationEndpoint = "https://identity.alinma.com/oauth2/authorize",
                tokenEndpoint = "https://identity.alinma.com/oauth2/token",
                parEndpoint = "https://identity.alinma.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions"),
                certificateFingerprints = listOf(
                    "E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4",
                    "F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4"
                ),
                rateLimits = RateLimits(requestsPerMinute = 70, requestsPerHour = 1200),
                features = BankFeatures(
                    maxTransactionAmount = "1200000.00",
                    supportedPaymentMethods = listOf("DOMESTIC_TRANSFER", "BILL_PAYMENT")
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@alinma.com",
                    supportPhone = "+966920001111",
                    technicalContactEmail = "api-support@alinma.com",
                    complianceEmail = "compliance@alinma.com",
                    website = "https://www.alinma.com",
                    privacyPolicyUrl = "https://www.alinma.com/privacy",
                    termsOfServiceUrl = "https://www.alinma.com/terms"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "alinma",
                bankName = "Alinma Bank",
                bankNameAr = "مصرف الإنماء",
                baseUrl = "https://sandbox-api.alinma.com/open-banking/v1",
                clientId = "alinma_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.alinma.com/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.alinma.com/oauth2/token",
                parEndpoint = "https://sandbox-identity.alinma.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions"),
                certificateFingerprints = listOf(
                    "E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox@alinma.com",
                    supportPhone = "+966920001111",
                    technicalContactEmail = "sandbox@alinma.com",
                    complianceEmail = "compliance@alinma.com",
                    website = "https://developer.alinma.com"
                )
            )
        ),

        // Bank Albilad
        "albilad" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "albilad",
                bankName = "Bank Albilad",
                bankNameAr = "بنك البلاد",
                baseUrl = "https://api.bankalbilad.com/open-banking/v1",
                clientId = "albilad_prod_client",
                authorizationEndpoint = "https://identity.bankalbilad.com/oauth2/authorize",
                tokenEndpoint = "https://identity.bankalbilad.com/oauth2/token",
                parEndpoint = "https://identity.bankalbilad.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "standing-orders"),
                certificateFingerprints = listOf(
                    "F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5",
                    "A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5"
                ),
                rateLimits = RateLimits(requestsPerMinute = 60, requestsPerHour = 1000),
                features = BankFeatures(
                    maxTransactionAmount = "1000000.00",
                    supportedPaymentMethods = listOf("DOMESTIC_TRANSFER", "BILL_PAYMENT")
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@bankalbilad.com",
                    supportPhone = "+966920000616",
                    technicalContactEmail = "api@bankalbilad.com",
                    complianceEmail = "compliance@bankalbilad.com",
                    website = "https://www.bankalbilad.com",
                    privacyPolicyUrl = "https://www.bankalbilad.com/privacy-policy",
                    termsOfServiceUrl = "https://www.bankalbilad.com/terms"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "albilad",
                bankName = "Bank Albilad",
                bankNameAr = "بنك البلاد",
                baseUrl = "https://sandbox-api.bankalbilad.com/open-banking/v1",
                clientId = "albilad_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.bankalbilad.com/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.bankalbilad.com/oauth2/token",
                parEndpoint = "https://sandbox-identity.bankalbilad.com/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions", "standing-orders"),
                certificateFingerprints = listOf(
                    "F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox@bankalbilad.com",
                    supportPhone = "+966920000616",
                    technicalContactEmail = "sandbox@bankalbilad.com",
                    complianceEmail = "compliance@bankalbilad.com",
                    website = "https://developer.bankalbilad.com"
                )
            )
        ),

        // STC Pay - Digital wallet and payment service
        "stcpay" to mapOf(
            PRODUCTION_ENV to BankConfiguration(
                bankCode = "stcpay",
                bankName = "STC Pay",
                bankNameAr = "إس تي سي باي",
                baseUrl = "https://api.stcpay.com.sa/open-banking/v1",
                clientId = "stcpay_prod_client",
                authorizationEndpoint = "https://identity.stcpay.com.sa/oauth2/authorize",
                tokenEndpoint = "https://identity.stcpay.com.sa/oauth2/token",
                parEndpoint = "https://identity.stcpay.com.sa/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions"),
                certificateFingerprints = listOf(
                    "A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6",
                    "B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5:C6"
                ),
                rateLimits = RateLimits(requestsPerMinute = 150, requestsPerHour = 3000),
                features = BankFeatures(
                    maxTransactionAmount = "50000.00", // Lower limit for digital wallet
                    supportedPaymentMethods = listOf("DIGITAL_WALLET", "BILL_PAYMENT", "P2P_TRANSFER"),
                    supportsBulkPayments = false
                ),
                contactInfo = BankContactInfo(
                    supportEmail = "openbanking@stcpay.com.sa",
                    supportPhone = "+966920000951",
                    technicalContactEmail = "developers@stcpay.com.sa",
                    complianceEmail = "compliance@stcpay.com.sa",
                    website = "https://www.stcpay.com.sa",
                    privacyPolicyUrl = "https://www.stcpay.com.sa/privacy",
                    termsOfServiceUrl = "https://www.stcpay.com.sa/terms"
                )
            ),
            SANDBOX_ENV to BankConfiguration(
                bankCode = "stcpay",
                bankName = "STC Pay",
                bankNameAr = "إس تي سي باي",
                baseUrl = "https://sandbox-api.stcpay.com.sa/open-banking/v1",
                clientId = "stcpay_sandbox_client",
                authorizationEndpoint = "https://sandbox-identity.stcpay.com.sa/oauth2/authorize",
                tokenEndpoint = "https://sandbox-identity.stcpay.com.sa/oauth2/token",
                parEndpoint = "https://sandbox-identity.stcpay.com.sa/oauth2/par",
                supportedScopes = listOf("accounts", "payments", "balances", "transactions"),
                certificateFingerprints = listOf(
                    "A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6"
                ),
                environment = SANDBOX_ENV,
                contactInfo = BankContactInfo(
                    supportEmail = "sandbox@stcpay.com.sa",
                    supportPhone = "+966920000951",
                    technicalContactEmail = "sandbox@stcpay.com.sa",
                    complianceEmail = "compliance@stcpay.com.sa",
                    website = "https://developer.stcpay.com.sa"
                )
            )
        )
    )

    /**
     * Get bank configuration by bank code and environment
     */
    fun getBankConfiguration(bankCode: String, environment: String = PRODUCTION_ENV): BankConfiguration? {
        return configurations[bankCode]?.get(environment)
    }

    /**
     * Get all available banks for the specified environment
     */
    fun getAllBanks(environment: String = PRODUCTION_ENV): List<BankConfiguration> {
        return configurations.values.mapNotNull { it[environment] }
            .sortedBy { it.bankName }
    }

    /**
     * Get bank registry entries for UI display
     */
    fun getBankRegistryEntries(): List<BankRegistryEntry> {
        return listOf(
            BankRegistryEntry(
                bankCode = "alrajhi",
                bankName = "Al Rajhi Bank",
                bankNameAr = "مصرف الراجحي",
                primaryColor = "#006633",
                isPopular = true,
                sortOrder = 1
            ),
            BankRegistryEntry(
                bankCode = "snb",
                bankName = "Saudi National Bank",
                bankNameAr = "البنك الأهلي السعودي",
                primaryColor = "#0066CC",
                isPopular = true,
                sortOrder = 2
            ),
            BankRegistryEntry(
                bankCode = "riyadbank",
                bankName = "Riyad Bank",
                bankNameAr = "بنك الرياض",
                primaryColor = "#0F4C75",
                isPopular = true,
                sortOrder = 3
            ),
            BankRegistryEntry(
                bankCode = "sabb",
                bankName = "SABB",
                bankNameAr = "ساب",
                primaryColor = "#D32F2F",
                isPopular = true,
                sortOrder = 4
            ),
            BankRegistryEntry(
                bankCode = "alinma",
                bankName = "Alinma Bank",
                bankNameAr = "مصرف الإنماء",
                primaryColor = "#4CAF50",
                sortOrder = 5
            ),
            BankRegistryEntry(
                bankCode = "albilad",
                bankName = "Bank Albilad",
                bankNameAr = "بنك البلاد",
                primaryColor = "#FF9800",
                sortOrder = 6
            ),
            BankRegistryEntry(
                bankCode = "stcpay",
                bankName = "STC Pay",
                bankNameAr = "إس تي سي باي",
                primaryColor = "#9C27B0",
                isPopular = true,
                sortOrder = 7
            )
        )
    }

    /**
     * Check if a bank is supported
     */
    fun isBankSupported(bankCode: String): Boolean {
        return configurations.containsKey(bankCode)
    }

    /**
     * Get supported environments for a bank
     */
    fun getSupportedEnvironments(bankCode: String): List<String> {
        return configurations[bankCode]?.keys?.toList() ?: emptyList()
    }
}