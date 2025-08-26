package code.yousef.dari.sama.interfaces

import code.yousef.dari.sama.models.*
import kotlinx.datetime.Instant

/**
 * Consent Manager following SAMA Open Banking consent requirements
 * Handles consent lifecycle, permissions, and audit logging
 */
interface ConsentManager {
    
    /**
     * Create a new consent request for account access
     * @param permissions List of requested permissions
     * @param expirationDate When consent should expire (ISO 8601)
     * @param transactionFromDate Historical transaction access from date
     * @param transactionToDate Historical transaction access to date
     * @return Consent creation response with consent ID
     */
    suspend fun createAccountAccessConsent(
        permissions: List<ConsentPermission>,
        expirationDate: String,
        transactionFromDate: String? = null,
        transactionToDate: String? = null
    ): Result<ConsentResponse>
    
    /**
     * Create a consent request for payment initiation
     * @param paymentRequest The payment to be authorized
     * @return Payment consent response
     */
    suspend fun createPaymentConsent(
        paymentRequest: DomesticPaymentRequest
    ): Result<PaymentConsentResponse>
    
    /**
     * Get current status and details of a consent
     * @param consentId Unique consent identifier
     * @return Current consent status and details
     */
    suspend fun getConsentStatus(consentId: String): Result<ConsentStatus>
    
    /**
     * Revoke an existing consent
     * @param consentId Unique consent identifier
     * @return Revocation result
     */
    suspend fun revokeConsent(consentId: String): Result<ConsentRevocation>
    
    /**
     * Check if consent has expired
     * @param consentId Unique consent identifier
     * @return true if consent is expired
     */
    suspend fun isConsentExpired(consentId: String): Boolean
    
    /**
     * Validate if requested permission is granted in consent
     * @param consentId Unique consent identifier
     * @param permission Permission to check
     * @return true if permission is granted
     */
    suspend fun hasPermission(
        consentId: String,
        permission: ConsentPermission
    ): Boolean
    
    /**
     * Get all active consents for audit purposes
     * @return List of active consents
     */
    suspend fun getActiveConsents(): Result<List<ConsentStatus>>
    
    /**
     * Store consent data securely with encryption
     * @param consentId Unique consent identifier
     * @param consentData Consent details to store
     */
    suspend fun storeConsent(consentId: String, consentData: ConsentStatus): Result<Unit>
    
    /**
     * Log consent-related activities for audit trail
     * @param consentId Unique consent identifier
     * @param action Action performed
     * @param details Additional details
     * @param timestamp When the action occurred
     */
    suspend fun auditConsentAction(
        consentId: String,
        action: ConsentAuditAction,
        details: String,
        timestamp: Instant = kotlinx.datetime.Clock.System.now()
    ): Result<Unit>
    
    /**
     * Get audit trail for a specific consent
     * @param consentId Unique consent identifier
     * @return List of audit entries
     */
    suspend fun getConsentAuditTrail(consentId: String): Result<List<ConsentAuditEntry>>
}