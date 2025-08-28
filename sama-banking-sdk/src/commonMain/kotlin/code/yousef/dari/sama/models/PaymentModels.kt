package code.yousef.dari.sama.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domestic payment request for Saudi Arabia
 */
@Serializable
data class DomesticPaymentRequest(
    val instructedAmount: Amount,
    val debtorAccount: DebtorAccount,
    val creditorAccount: CreditorAccount,
    val remittanceInformation: RemittanceInformation? = null,
    val instructionIdentification: String? = null,
    val endToEndIdentification: String? = null
)

/**
 * Domestic payment response
 */
@Serializable
data class DomesticPayment(
    val domesticPaymentId: String,
    val status: PaymentStatus,
    val creationDateTime: Instant,
    val statusUpdateDateTime: Instant,
    val expectedExecutionDateTime: Instant? = null,
    val instructedAmount: Amount,
    val debtorAccount: DebtorAccount,
    val creditorAccount: CreditorAccount,
    val remittanceInformation: RemittanceInformation? = null,
    val charges: List<Charge>? = null
)

/**
 * Scheduled payment request
 */
@Serializable
data class ScheduledPaymentRequest(
    val requestedExecutionDateTime: Instant,
    val instructedAmount: Amount,
    val debtorAccount: DebtorAccount,
    val creditorAccount: CreditorAccount,
    val remittanceInformation: RemittanceInformation? = null,
    val instructionIdentification: String? = null,
    val endToEndIdentification: String? = null
)

/**
 * Scheduled payment response
 */
@Serializable
data class ScheduledPayment(
    val scheduledPaymentId: String,
    val status: PaymentStatus,
    val creationDateTime: Instant,
    val statusUpdateDateTime: Instant,
    val requestedExecutionDateTime: Instant,
    val instructedAmount: Amount,
    val debtorAccount: DebtorAccount,
    val creditorAccount: CreditorAccount,
    val remittanceInformation: RemittanceInformation? = null,
    val charges: List<Charge>? = null
)

/**
 * Payment confirmation response
 */
@Serializable
data class PaymentConfirmation(
    val paymentId: String,
    val status: PaymentStatus,
    val confirmationDateTime: Instant,
    val confirmationCode: String
)

/**
 * Payment limits validation response
 */
@Serializable
data class PaymentLimitsValidation(
    val isValid: Boolean,
    val dailyLimit: Amount,
    val monthlyLimit: Amount,
    val remainingDailyLimit: Amount,
    val remainingMonthlyLimit: Amount,
    val violations: List<String> = emptyList()
)

// Supporting data classes
@Serializable
data class DebtorAccount(
    val iban: String,
    val name: String
)

// Supporting data classes
@Serializable
data class RemittanceInformation(
    val unstructured: String? = null,
    val reference: String? = null
)

@Serializable
data class Charge(
    val chargeBearer: ChargeBearerType,
    val type: ChargeType,
    val amount: Amount
)

@Serializable
data class PaymentStatusDetail(
    val status: String,
    val statusReason: String? = null,
    val statusReasonDescription: String? = null
)

// Enums
@Serializable
enum class PaymentStatus {
    ACCEPTED_CREDIT_SETTLEMENT_COMPLETED,
    ACCEPTED_CUSTOMER_PROFILE,
    ACCEPTED_FUNDS_CHECKED,
    ACCEPTED_SETTLEMENT_COMPLETED,
    ACCEPTED_SETTLEMENT_IN_PROCESS,
    ACCEPTED_TECHNICAL_VALIDATION,
    ACCEPTED_WITH_CHANGE,
    ACCEPTED_WITHOUT_POSTING,
    PENDING,
    RECEIVED,
    REJECTED
}

@Serializable
enum class ChargeBearerType {
    BorneByCreditor,
    BorneByDebtor,
    FollowingServiceLevel,
    Shared
}

@Serializable
enum class ChargeType {
    SAMA_FEE,
    BANK_FEE,
    CORRESPONDENT_BANK_FEE,
    REGULATORY_FEE
}

/**
 * Payment initiation response for SAMA compliance
 */
@Serializable
data class PaymentInitiationResponse(
    val paymentId: String,
    val status: PaymentStatus,
    val creationDateTime: String, // ISO 8601 datetime
    val statusUpdateDateTime: String, // ISO 8601 datetime
    val paymentExecutionDateTime: String? = null,
    val charges: List<Charge>? = null,
    val links: PaymentLinks? = null
)

/**
 * Scheduled payment response for SAMA compliance
 */
@Serializable
data class ScheduledPaymentResponse(
    val scheduledPaymentId: String,
    val status: PaymentStatus,
    val creationDateTime: String, // ISO 8601 datetime
    val statusUpdateDateTime: String, // ISO 8601 datetime
    val requestedExecutionDateTime: String,
    val charges: List<Charge>? = null,
    val links: PaymentLinks? = null
)

@Serializable
data class PaymentLinks(
    val self: String? = null,
    val payment: String? = null
)
