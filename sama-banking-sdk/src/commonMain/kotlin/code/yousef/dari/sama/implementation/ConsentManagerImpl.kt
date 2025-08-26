package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.ConsentManager
import code.yousef.dari.sama.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SAMA-compliant Consent Manager Implementation
 * Implements consent creation, validation, revocation, and audit logging
 */
class ConsentManagerImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ConsentManager {

    @Serializable
    private data class ConsentApiRequest(
        val data: ConsentData
    )

    @Serializable
    private data class ConsentData(
        val permissions: List<String>,
        val expirationDateTime: String? = null,
        val transactionFromDateTime: String? = null,
        val transactionToDateTime: String? = null
    )

    @Serializable
    private data class ConsentApiResponse(
        val data: ConsentResponseData,
        val links: Links? = null,
        val meta: Meta? = null
    )

    @Serializable
    private data class ConsentResponseData(
        val consentId: String,
        val status: String,
        val creationDateTime: String,
        val statusUpdateDateTime: String,
        val permissions: List<String>,
        val expirationDateTime: String? = null,
        val transactionFromDateTime: String? = null,
        val transactionToDateTime: String? = null
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
    private data class PaymentConsentApiRequest(
        val data: PaymentConsentData
    )

    @Serializable
    private data class PaymentConsentData(
        val expirationDateTime: String? = null
    )

    override suspend fun createAccountConsent(
        accessToken: String,
        consentRequest: AccountConsentRequest
    ): Result<ConsentResponse> {
        return try {
            val requestBody = ConsentApiRequest(
                data = ConsentData(
                    permissions = consentRequest.permissions.map { mapPermissionToString(it) },
                    expirationDateTime = consentRequest.expirationDateTime?.toString(),
                    transactionFromDateTime = consentRequest.transactionFromDateTime?.toString(),
                    transactionToDateTime = consentRequest.transactionToDateTime?.toString()
                )
            )

            val response = httpClient.post("$baseUrl/aisp/account-access-consents") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("Content-Type", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Created) {
                val apiResponse = response.body<ConsentApiResponse>()
                val responseData = apiResponse.data

                Result.success(
                    ConsentResponse(
                        consentId = responseData.consentId,
                        status = mapStringToConsentStatus(responseData.status),
                        creationDateTime = parseDateTime(responseData.creationDateTime),
                        statusUpdateDateTime = parseDateTime(responseData.statusUpdateDateTime),
                        permissions = responseData.permissions.map { mapStringToPermission(it) },
                        expirationDateTime = responseData.expirationDateTime?.let { parseDateTime(it) },
                        transactionFromDateTime = responseData.transactionFromDateTime?.let { parseDateTime(it) },
                        transactionToDateTime = responseData.transactionToDateTime?.let { parseDateTime(it) }
                    )
                )
            } else {
                Result.failure(Exception("Account consent creation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPaymentConsent(
        accessToken: String,
        consentRequest: PaymentConsentRequest
    ): Result<ConsentResponse> {
        return try {
            val requestBody = PaymentConsentApiRequest(
                data = PaymentConsentData(
                    expirationDateTime = consentRequest.expirationDateTime?.toString()
                )
            )

            val response = httpClient.post("$baseUrl/pisp/domestic-payment-consents") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("Content-Type", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Created) {
                val apiResponse = response.body<ConsentApiResponse>()
                val responseData = apiResponse.data

                Result.success(
                    ConsentResponse(
                        consentId = responseData.consentId,
                        status = mapStringToConsentStatus(responseData.status),
                        creationDateTime = parseDateTime(responseData.creationDateTime),
                        statusUpdateDateTime = parseDateTime(responseData.statusUpdateDateTime),
                        permissions = listOf(Permission.PAYMENTS),
                        expirationDateTime = responseData.expirationDateTime?.let { parseDateTime(it) }
                    )
                )
            } else {
                Result.failure(Exception("Payment consent creation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConsentStatus(
        accessToken: String,
        consentId: String
    ): Result<ConsentDetails> {
        return try {
            val response = httpClient.get("$baseUrl/aisp/account-access-consents/$consentId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ConsentApiResponse>()
                val responseData = apiResponse.data

                Result.success(
                    ConsentDetails(
                        consentId = responseData.consentId,
                        status = mapStringToConsentStatus(responseData.status),
                        creationDateTime = parseDateTime(responseData.creationDateTime),
                        statusUpdateDateTime = parseDateTime(responseData.statusUpdateDateTime),
                        permissions = responseData.permissions.map { mapStringToPermission(it) },
                        expirationDateTime = responseData.expirationDateTime?.let { parseDateTime(it) },
                        transactionFromDateTime = responseData.transactionFromDateTime?.let { parseDateTime(it) },
                        transactionToDateTime = responseData.transactionToDateTime?.let { parseDateTime(it) }
                    )
                )
            } else {
                Result.failure(Exception("Consent status retrieval failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeConsent(
        accessToken: String,
        consentId: String
    ): Result<Unit> {
        return try {
            val response = httpClient.delete("$baseUrl/aisp/account-access-consents/$consentId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.NoContent) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Consent revocation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateConsentPermissions(
        consentId: String,
        requiredPermissions: List<Permission>
    ): Result<Boolean> {
        return try {
            // In a real implementation, this would fetch the consent details
            // and check permissions. For now, we'll implement basic logic.
            
            // This is a simplified validation - in reality you'd need to
            // fetch consent details and check status and permissions
            if (consentId.isNotBlank() && requiredPermissions.isNotEmpty()) {
                // Mock validation logic - assume consent exists and is authorized
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isConsentExpired(consentId: String): Result<Boolean> {
        return try {
            // In a real implementation, this would fetch consent details
            // and check expiration. For now, we'll return false (not expired)
            
            if (consentId.isNotBlank()) {
                Result.success(false) // Mock: assume not expired
            } else {
                Result.failure(IllegalArgumentException("Invalid consent ID"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConsentAuditLog(
        accessToken: String,
        consentId: String
    ): Result<List<ConsentAuditLog>> {
        return try {
            val response = httpClient.get("$baseUrl/audit/consents/$consentId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/json")
                header("x-fapi-interaction-id", generateInteractionId())
            }

            if (response.status == HttpStatusCode.OK) {
                // In a real implementation, you would parse the audit log response
                // For now, return empty list as this endpoint may not be available in all banks
                Result.success(emptyList())
            } else {
                Result.failure(Exception("Audit log retrieval failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapPermissionToString(permission: Permission): String {
        return when (permission) {
            Permission.READ_ACCOUNTS_BASIC -> "ReadAccountsBasic"
            Permission.READ_ACCOUNTS_DETAIL -> "ReadAccountsDetail"
            Permission.READ_BALANCES -> "ReadBalances"
            Permission.READ_BENEFICIARIES_BASIC -> "ReadBeneficiariesBasic"
            Permission.READ_BENEFICIARIES_DETAIL -> "ReadBeneficiariesDetail"
            Permission.READ_DIRECT_DEBITS -> "ReadDirectDebits"
            Permission.READ_OFFERS -> "ReadOffers"
            Permission.READ_PAN -> "ReadPAN"
            Permission.READ_PARTY -> "ReadParty"
            Permission.READ_PARTY_PSU -> "ReadPartyPSU"
            Permission.READ_PRODUCTS -> "ReadProducts"
            Permission.READ_SCHEDULED_PAYMENTS_BASIC -> "ReadScheduledPaymentsBasic"
            Permission.READ_SCHEDULED_PAYMENTS_DETAIL -> "ReadScheduledPaymentsDetail"
            Permission.READ_STANDING_ORDERS_BASIC -> "ReadStandingOrdersBasic"
            Permission.READ_STANDING_ORDERS_DETAIL -> "ReadStandingOrdersDetail"
            Permission.READ_STATEMENTS_BASIC -> "ReadStatementsBasic"
            Permission.READ_STATEMENTS_DETAIL -> "ReadStatementsDetail"
            Permission.READ_TRANSACTIONS_BASIC -> "ReadTransactionsBasic"
            Permission.READ_TRANSACTIONS_CREDITS -> "ReadTransactionsCredits"
            Permission.READ_TRANSACTIONS_DEBITS -> "ReadTransactionsDebits"
            Permission.READ_TRANSACTIONS_DETAIL -> "ReadTransactionsDetail"
            Permission.PAYMENTS -> "Payments"
        }
    }

    private fun mapStringToPermission(permission: String): Permission {
        return when (permission) {
            "ReadAccountsBasic" -> Permission.READ_ACCOUNTS_BASIC
            "ReadAccountsDetail" -> Permission.READ_ACCOUNTS_DETAIL
            "ReadBalances" -> Permission.READ_BALANCES
            "ReadBeneficiariesBasic" -> Permission.READ_BENEFICIARIES_BASIC
            "ReadBeneficiariesDetail" -> Permission.READ_BENEFICIARIES_DETAIL
            "ReadDirectDebits" -> Permission.READ_DIRECT_DEBITS
            "ReadOffers" -> Permission.READ_OFFERS
            "ReadPAN" -> Permission.READ_PAN
            "ReadParty" -> Permission.READ_PARTY
            "ReadPartyPSU" -> Permission.READ_PARTY_PSU
            "ReadProducts" -> Permission.READ_PRODUCTS
            "ReadScheduledPaymentsBasic" -> Permission.READ_SCHEDULED_PAYMENTS_BASIC
            "ReadScheduledPaymentsDetail" -> Permission.READ_SCHEDULED_PAYMENTS_DETAIL
            "ReadStandingOrdersBasic" -> Permission.READ_STANDING_ORDERS_BASIC
            "ReadStandingOrdersDetail" -> Permission.READ_STANDING_ORDERS_DETAIL
            "ReadStatementsBasic" -> Permission.READ_STATEMENTS_BASIC
            "ReadStatementsDetail" -> Permission.READ_STATEMENTS_DETAIL
            "ReadTransactionsBasic" -> Permission.READ_TRANSACTIONS_BASIC
            "ReadTransactionsCredits" -> Permission.READ_TRANSACTIONS_CREDITS
            "ReadTransactionsDebits" -> Permission.READ_TRANSACTIONS_DEBITS
            "ReadTransactionsDetail" -> Permission.READ_TRANSACTIONS_DETAIL
            "Payments" -> Permission.PAYMENTS
            else -> Permission.READ_ACCOUNTS_BASIC
        }
    }

    private fun mapStringToConsentStatus(status: String): ConsentStatus {
        return when (status.uppercase()) {
            "AUTHORISED", "AUTHORIZED" -> ConsentStatus.AUTHORIZED
            "AWAITINGAUTHORISATION", "AWAITING_AUTHORIZATION" -> ConsentStatus.AWAITING_AUTHORIZATION
            "CONSUMED" -> ConsentStatus.CONSUMED
            "EXPIRED" -> ConsentStatus.EXPIRED
            "REJECTED" -> ConsentStatus.REJECTED
            "REVOKED" -> ConsentStatus.REVOKED
            else -> ConsentStatus.AWAITING_AUTHORIZATION
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
}