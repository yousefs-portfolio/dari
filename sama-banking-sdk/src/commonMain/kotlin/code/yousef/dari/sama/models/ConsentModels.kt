package code.yousef.dari.sama.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Account consent request
 */
@Serializable
data class AccountConsentRequest(
    val permissions: List<Permission>,
    val expirationDateTime: Instant? = null,
    val transactionFromDateTime: Instant? = null,
    val transactionToDateTime: Instant? = null
)

/**
 * Payment consent request
 */
@Serializable
data class PaymentConsentRequest(
    val expirationDateTime: Instant? = null
)

/**
 * Consent creation response
 */
@Serializable
data class ConsentResponse(
    val consentId: String,
    val status: ConsentStatus,
    val creationDateTime: Instant,
    val statusUpdateDateTime: Instant,
    val permissions: List<Permission>,
    val expirationDateTime: Instant? = null,
    val transactionFromDateTime: Instant? = null,
    val transactionToDateTime: Instant? = null
)

/**
 * Payment consent response
 */
@Serializable
data class PaymentConsentResponse(
    val consentId: String,
    val status: ConsentStatusCode,
    val creationDateTime: String, // ISO 8601 datetime
    val statusUpdateDateTime: String, // ISO 8601 datetime
    val cutOffDateTime: String? = null, // ISO 8601 datetime
    val charges: List<Charge>? = null,
    val initiation: DomesticPaymentRequest,
    val authorisation: PaymentAuthorisation? = null
)

/**
 * Consent details
 */
@Serializable
data class ConsentDetails(
    val consentId: String,
    val status: ConsentStatus,
    val creationDateTime: Instant,
    val statusUpdateDateTime: Instant,
    val permissions: List<Permission>,
    val expirationDateTime: Instant? = null,
    val transactionFromDateTime: Instant? = null,
    val transactionToDateTime: Instant? = null
)

/**
 * Consent revocation response
 */
@Serializable
data class ConsentRevocation(
    val consentId: String,
    val status: ConsentStatusCode,
    val revocationDateTime: String, // ISO 8601 datetime
    val revocationReason: String? = null
)

/**
 * Consent audit log
 */
@Serializable
data class ConsentAuditLog(
    val consentId: String,
    val action: ConsentAction,
    val timestamp: Instant,
    val permissions: List<Permission>,
    val details: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val sessionId: String? = null
)

/**
 * Payment authorization details
 */
@Serializable
data class PaymentAuthorisation(
    val authorisationType: AuthorisationType,
    val completionDateTime: String? = null // ISO 8601 datetime
)

/**
 * Consent risk assessment data
 */
@Serializable
data class ConsentRiskData(
    val deliveryAddress: DeliveryAddress? = null,
    val paymentContextCode: PaymentContextCode? = null,
    val merchantCategoryCode: String? = null,
    val merchantCustomerIdentification: String? = null
)

/**
 * Delivery address for risk assessment
 */
@Serializable
data class DeliveryAddress(
    val addressLine: List<String>? = null,
    val streetName: String? = null,
    val buildingNumber: String? = null,
    val postCode: String? = null,
    val townName: String? = null,
    val countrySubDivision: List<String>? = null,
    val country: String? = null // ISO 3166-1 alpha-2 country code
)

// Enums
@Serializable
enum class Permission {
    READ_ACCOUNTS_BASIC,
    READ_ACCOUNTS_DETAIL,
    READ_BALANCES,
    READ_BENEFICIARIES_BASIC,
    READ_BENEFICIARIES_DETAIL,
    READ_DIRECT_DEBITS,
    READ_OFFERS,
    READ_PAN,
    READ_PARTY,
    READ_PARTY_PSU,
    READ_PRODUCTS,
    READ_SCHEDULED_PAYMENTS_BASIC,
    READ_SCHEDULED_PAYMENTS_DETAIL,
    READ_STANDING_ORDERS_BASIC,
    READ_STANDING_ORDERS_DETAIL,
    READ_STATEMENTS_BASIC,
    READ_STATEMENTS_DETAIL,
    READ_TRANSACTIONS_BASIC,
    READ_TRANSACTIONS_CREDITS,
    READ_TRANSACTIONS_DEBITS,
    READ_TRANSACTIONS_DETAIL,
    PAYMENTS
}

@Serializable
enum class ConsentStatus {
    AUTHORIZED,
    AWAITING_AUTHORIZATION,
    CONSUMED,
    EXPIRED,
    REJECTED,
    REVOKED
}

@Serializable
enum class ConsentAction {
    CREATED,
    AUTHORIZED,
    CONSUMED,
    EXPIRED,
    REJECTED,
    REVOKED,
    PERMISSION_CHECKED,
    DATA_ACCESSED,
    ERROR_OCCURRED
}

@Serializable
enum class AuthorisationType {
    Any,
    Single
}

@Serializable
enum class PaymentContextCode {
    BillPayment,
    EcommerceGoods,
    EcommerceServices,
    Other,
    PartyToParty
}