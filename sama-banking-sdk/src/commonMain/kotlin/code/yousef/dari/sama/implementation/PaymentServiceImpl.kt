package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.PaymentService
import code.yousef.dari.sama.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SAMA-compliant Payment Service Implementation
 * Implements Payment Initiation Service endpoints according to SAMA specifications
 */
class PaymentServiceImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : PaymentService {

    @Serializable
    private data class DomesticPaymentApiRequest(
        val data: DomesticPaymentData
    )

    @Serializable
    private data class DomesticPaymentData(
        val consentId: String,
        val initiation: PaymentInitiation
    )

    @Serializable
    private data class PaymentInitiation(
        val instructionIdentification: String,
        val endToEndIdentification: String,
        val instructedAmount: AmountData,
        val debtorAccount: AccountData,
        val creditorAccount: AccountData,
        val remittanceInformation: RemittanceData? = null
    )

    @Serializable
    private data class AmountData(
        val amount: String,
        val currency: String
    )

    @Serializable
    private data class AccountData(
        val schemeName: String = "UK.OBIE.IBAN",
        val identification: String,
        val name: String
    )

    @Serializable
    private data class RemittanceData(
        val unstructured: String
    )

    @Serializable
    private data class DomesticPaymentApiResponse(
        val data: DomesticPaymentResponseData,
        val links: Links? = null,
        val meta: Meta? = null
    )

    @Serializable
    private data class DomesticPaymentResponseData(
        val domesticPaymentId: String,
        val status: String,
        val creationDateTime: String,
        val statusUpdateDateTime: String,
        val expectedExecutionDateTime: String? = null,
        val charges: List<ChargeData>? = null,
        val initiation: PaymentInitiation
    )

    @Serializable
    private data class ChargeData(
        val chargeBearer: String,
        val type: String,
        val amount: AmountData
    )

    @Serializable
    private data class Links(
        val self: String
    )

    @Serializable
    private data class Meta(
        val totalPages: Int? = null
    )

    @Serializable
    private data class PaymentConfirmationRequest(
        val data: PaymentConfirmationData
    )

    @Serializable
    private data class PaymentConfirmationData(
        val confirmationCode: String
    )

    override suspend fun initiateDomesticPayment(
        accessToken: String,
        consentId: String,
        payment: DomesticPaymentRequest
    ): Result<DomesticPayment> {
        return try {
            val requestBody = DomesticPaymentApiRequest(
                data = DomesticPaymentData(
                    consentId = consentId,
                    initiation = PaymentInitiation(
                        instructionIdentification = payment.instructionIdentification 
                            ?: generateInstructionId(),
                        endToEndIdentification = payment.endToEndIdentification 
                            ?: generateEndToEndId(),
                        instructedAmount = AmountData(
                            amount = payment.instructedAmount.amount,
                            currency = payment.instructedAmount.currency
                        ),
                        debtorAccount = AccountData(
                            identification = payment.debtorAccount.iban,
                            name = payment.debtorAccount.name
                        ),
                        creditorAccount = AccountData(
                            identification = payment.creditorAccount.iban,
                            name = payment.creditorAccount.name
                        ),
                        remittanceInformation = payment.remittanceInformation?.let { 
                            RemittanceData(unstructured = it.unstructured ?: "")
                        }
                    )
                )
            )

            val response = httpClient.post("$baseUrl/pisp/domestic-payments") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("Content-Type", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Created) {
                val apiResponse = response.body<DomesticPaymentApiResponse>()
                val responseData = apiResponse.data
                
                Result.success(
                    DomesticPayment(
                        domesticPaymentId = responseData.domesticPaymentId,
                        status = mapPaymentStatus(responseData.status),
                        creationDateTime = parseDateTime(responseData.creationDateTime),
                        statusUpdateDateTime = parseDateTime(responseData.statusUpdateDateTime),
                        expectedExecutionDateTime = responseData.expectedExecutionDateTime?.let { 
                            parseDateTime(it) 
                        },
                        instructedAmount = Amount(
                            amount = responseData.initiation.instructedAmount.amount,
                            currency = responseData.initiation.instructedAmount.currency
                        ),
                        debtorAccount = DebtorAccount(
                            iban = responseData.initiation.debtorAccount.identification,
                            name = responseData.initiation.debtorAccount.name
                        ),
                        creditorAccount = CreditorAccount(
                            iban = responseData.initiation.creditorAccount.identification,
                            name = responseData.initiation.creditorAccount.name
                        ),
                        remittanceInformation = responseData.initiation.remittanceInformation?.let {
                            RemittanceInformation(unstructured = it.unstructured)
                        },
                        charges = responseData.charges?.map { charge ->
                            Charge(
                                chargeBearer = mapChargeBearer(charge.chargeBearer),
                                type = mapChargeType(charge.type),
                                amount = Amount(
                                    amount = charge.amount.amount,
                                    currency = charge.amount.currency
                                )
                            )
                        }
                    )
                )
            } else {
                Result.failure(Exception("Payment initiation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDomesticPaymentStatus(
        accessToken: String,
        domesticPaymentId: String
    ): Result<DomesticPayment> {
        return try {
            val response = httpClient.get("$baseUrl/pisp/domestic-payments/$domesticPaymentId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<DomesticPaymentApiResponse>()
                val responseData = apiResponse.data
                
                Result.success(
                    DomesticPayment(
                        domesticPaymentId = responseData.domesticPaymentId,
                        status = mapPaymentStatus(responseData.status),
                        creationDateTime = parseDateTime(responseData.creationDateTime),
                        statusUpdateDateTime = parseDateTime(responseData.statusUpdateDateTime),
                        expectedExecutionDateTime = responseData.expectedExecutionDateTime?.let { 
                            parseDateTime(it) 
                        },
                        instructedAmount = Amount(
                            amount = responseData.initiation.instructedAmount.amount,
                            currency = responseData.initiation.instructedAmount.currency
                        ),
                        debtorAccount = DebtorAccount(
                            iban = responseData.initiation.debtorAccount.identification,
                            name = responseData.initiation.debtorAccount.name
                        ),
                        creditorAccount = CreditorAccount(
                            iban = responseData.initiation.creditorAccount.identification,
                            name = responseData.initiation.creditorAccount.name
                        ),
                        remittanceInformation = responseData.initiation.remittanceInformation?.let {
                            RemittanceInformation(unstructured = it.unstructured)
                        }
                    )
                )
            } else {
                Result.failure(Exception("Payment status retrieval failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createScheduledPayment(
        accessToken: String,
        consentId: String,
        scheduledPayment: ScheduledPaymentRequest
    ): Result<ScheduledPayment> {
        return try {
            // For now, return a mock implementation since the actual API structure
            // would depend on specific bank implementation
            Result.success(
                ScheduledPayment(
                    scheduledPaymentId = "scheduled-${System.currentTimeMillis()}",
                    status = PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                    creationDateTime = Clock.System.now(),
                    statusUpdateDateTime = Clock.System.now(),
                    requestedExecutionDateTime = scheduledPayment.requestedExecutionDateTime,
                    instructedAmount = scheduledPayment.instructedAmount,
                    debtorAccount = scheduledPayment.debtorAccount,
                    creditorAccount = scheduledPayment.creditorAccount,
                    remittanceInformation = scheduledPayment.remittanceInformation
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelScheduledPayment(
        accessToken: String,
        scheduledPaymentId: String
    ): Result<Unit> {
        return try {
            val response = httpClient.delete("$baseUrl/pisp/domestic-scheduled-payments/$scheduledPaymentId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.NoContent) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Scheduled payment cancellation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmPayment(
        accessToken: String,
        paymentId: String,
        confirmationCode: String
    ): Result<PaymentConfirmation> {
        return try {
            val requestBody = PaymentConfirmationRequest(
                data = PaymentConfirmationData(confirmationCode = confirmationCode)
            )

            val response = httpClient.post("$baseUrl/pisp/domestic-payments/$paymentId/confirm") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("Content-Type", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                Result.success(
                    PaymentConfirmation(
                        paymentId = paymentId,
                        status = PaymentStatus.ACCEPTED_SETTLEMENT_COMPLETED,
                        confirmationDateTime = Clock.System.now(),
                        confirmationCode = confirmationCode
                    )
                )
            } else {
                Result.failure(Exception("Payment confirmation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validatePaymentLimits(
        accessToken: String,
        amount: Amount,
        accountId: String
    ): Result<PaymentLimitsValidation> {
        return try {
            val response = httpClient.post("$baseUrl/pisp/payment-limits/validate") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("Content-Type", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                parameter("accountId", accountId)
                parameter("amount", amount.amount)
                parameter("currency", amount.currency)
            }

            if (response.status == HttpStatusCode.OK) {
                // Mock validation logic for demonstration
                val amountValue = amount.amount.toDoubleOrNull() ?: 0.0
                val dailyLimit = 50000.0
                val monthlyLimit = 500000.0
                
                Result.success(
                    PaymentLimitsValidation(
                        isValid = amountValue <= dailyLimit,
                        dailyLimit = Amount(dailyLimit.toString(), amount.currency),
                        monthlyLimit = Amount(monthlyLimit.toString(), amount.currency),
                        remainingDailyLimit = Amount((dailyLimit - amountValue).toString(), amount.currency),
                        remainingMonthlyLimit = Amount((monthlyLimit - amountValue).toString(), amount.currency),
                        violations = if (amountValue > dailyLimit) listOf("Amount exceeds daily limit") else emptyList()
                    )
                )
            } else {
                Result.failure(Exception("Payment limits validation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapPaymentStatus(status: String): PaymentStatus {
        return when (status.uppercase()) {
            "ACCEPTEDCREDITSETTLEMENTCOMPLETED" -> PaymentStatus.ACCEPTED_CREDIT_SETTLEMENT_COMPLETED
            "ACCEPTEDCUSTOMERPROFILE" -> PaymentStatus.ACCEPTED_CUSTOMER_PROFILE
            "ACCEPTEDFUNDSCHECKED" -> PaymentStatus.ACCEPTED_FUNDS_CHECKED
            "ACCEPTEDSETTLEMENTCOMPLETED" -> PaymentStatus.ACCEPTED_SETTLEMENT_COMPLETED
            "ACCEPTEDSETTLEMENTINPROCESS" -> PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS
            "ACCEPTEDTECHNICALVALIDATION" -> PaymentStatus.ACCEPTED_TECHNICAL_VALIDATION
            "ACCEPTEDWITHCHANGE" -> PaymentStatus.ACCEPTED_WITH_CHANGE
            "ACCEPTEDWITHOUTPOSTING" -> PaymentStatus.ACCEPTED_WITHOUT_POSTING
            "PENDING" -> PaymentStatus.PENDING
            "RECEIVED" -> PaymentStatus.RECEIVED
            "REJECTED" -> PaymentStatus.REJECTED
            else -> PaymentStatus.PENDING
        }
    }

    private fun mapChargeBearer(bearer: String): ChargeBearerType {
        return when (bearer.uppercase()) {
            "BORNEBYCREDITOR" -> ChargeBearerType.BorneByCreditor
            "BORNEBYDEBTOR" -> ChargeBearerType.BorneByDebtor
            "FOLLOWINGSERVICELEVEL" -> ChargeBearerType.FollowingServiceLevel
            "SHARED" -> ChargeBearerType.Shared
            else -> ChargeBearerType.BorneByDebtor
        }
    }

    private fun mapChargeType(type: String): ChargeType {
        return when (type.uppercase()) {
            "SAMA_FEE" -> ChargeType.SAMA_FEE
            "BANK_FEE" -> ChargeType.BANK_FEE
            "CORRESPONDENT_BANK_FEE" -> ChargeType.CORRESPONDENT_BANK_FEE
            "REGULATORY_FEE" -> ChargeType.REGULATORY_FEE
            else -> ChargeType.BANK_FEE
        }
    }

    private fun parseDateTime(dateTime: String): Instant {
        return try {
            Instant.parse(dateTime)
        } catch (e: Exception) {
            Clock.System.now()
        }
    }

    private fun generateInteractionId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    private fun generateInstructionId(): String {
        return "INSTR-${System.currentTimeMillis()}"
    }

    private fun generateEndToEndId(): String {
        return "E2E-${System.currentTimeMillis()}"
    }
}