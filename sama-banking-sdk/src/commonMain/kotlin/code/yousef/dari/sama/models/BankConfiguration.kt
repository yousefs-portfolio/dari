package code.yousef.dari.sama.models

import kotlinx.serialization.Serializable

/**
 * Bank Configuration Data Model
 * Contains all necessary configuration for connecting to a specific Saudi bank's Open Banking APIs
 */
@Serializable
data class BankConfiguration(
    val bankCode: String,
    val bankName: String,
    val bankNameAr: String,
    val baseUrl: String,
    val clientId: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val parEndpoint: String,
    val supportedScopes: List<String>,
    val certificateFingerprints: List<String>,
    val environment: String = "production", // production, sandbox, development
    val isActive: Boolean = true,
    val rateLimits: RateLimits = RateLimits(),
    val features: BankFeatures = BankFeatures(),
    val contactInfo: BankContactInfo? = null
)

/**
 * Rate Limiting Configuration for Bank APIs
 */
@Serializable
data class RateLimits(
    val requestsPerMinute: Int = 60,
    val requestsPerHour: Int = 1000,
    val requestsPerDay: Int = 10000,
    val burstLimit: Int = 10
)

/**
 * Feature Support Configuration for each bank
 */
@Serializable
data class BankFeatures(
    val supportsPayments: Boolean = true,
    val supportsScheduledPayments: Boolean = true,
    val supportsStandingOrders: Boolean = true,
    val supportsDirectDebits: Boolean = true,
    val supportsStatements: Boolean = true,
    val supportsBulkPayments: Boolean = false,
    val supportsInternationalPayments: Boolean = false,
    val maxTransactionAmount: String? = null, // In SAR
    val supportedCurrencies: List<String> = listOf("SAR"),
    val supportedPaymentMethods: List<String> = listOf("DOMESTIC_TRANSFER")
)

/**
 * Bank Contact Information for support and compliance
 */
@Serializable
data class BankContactInfo(
    val supportEmail: String,
    val supportPhone: String,
    val technicalContactEmail: String,
    val complianceEmail: String,
    val website: String,
    val privacyPolicyUrl: String? = null,
    val termsOfServiceUrl: String? = null
)

/**
 * Bank Registry Entry for listing available banks
 */
@Serializable
data class BankRegistryEntry(
    val bankCode: String,
    val bankName: String,
    val bankNameAr: String,
    val logoUrl: String? = null,
    val primaryColor: String? = null,
    val isPopular: Boolean = false,
    val sortOrder: Int = 0
)