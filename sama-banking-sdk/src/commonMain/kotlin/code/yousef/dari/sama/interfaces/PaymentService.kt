package code.yousef.dari.sama.interfaces

import code.yousef.dari.sama.models.*

/**
 * Payment Initiation Service (PIS) following SAMA Open Banking specifications
 * Handles domestic payments and scheduled payments with proper security
 */
interface PaymentService {
    
    /**
     * Initiate a domestic payment within Saudi Arabia
     * @param accessToken Valid access token with payments scope
     * @param paymentRequest Payment initiation request
     * @return Payment initiation response with payment ID
     */
    suspend fun initiateDomesticPayment(
        accessToken: String,
        paymentRequest: DomesticPaymentRequest
    ): Result<PaymentInitiationResponse>
    
    /**
     * Get the status of a payment
     * @param accessToken Valid access token
     * @param paymentId Unique payment identifier from initiation
     * @return Current payment status
     */
    suspend fun getPaymentStatus(
        accessToken: String,
        paymentId: String
    ): Result<PaymentStatus>
    
    /**
     * Create a scheduled domestic payment
     * @param accessToken Valid access token with payments scope
     * @param scheduledPaymentRequest Scheduled payment request
     * @return Scheduled payment response
     */
    suspend fun createScheduledPayment(
        accessToken: String,
        scheduledPaymentRequest: ScheduledPaymentRequest
    ): Result<ScheduledPaymentResponse>
    
    /**
     * Get details of a scheduled payment
     * @param accessToken Valid access token
     * @param scheduledPaymentId Unique scheduled payment identifier
     * @return Scheduled payment details
     */
    suspend fun getScheduledPayment(
        accessToken: String,
        scheduledPaymentId: String
    ): Result<ScheduledPayment>
    
    /**
     * Cancel a scheduled payment
     * @param accessToken Valid access token
     * @param scheduledPaymentId Unique scheduled payment identifier
     * @return Cancellation result
     */
    suspend fun cancelScheduledPayment(
        accessToken: String,
        scheduledPaymentId: String
    ): Result<Boolean>
    
    /**
     * Confirm a payment (for banks requiring explicit confirmation)
     * @param accessToken Valid access token
     * @param paymentId Unique payment identifier
     * @param confirmationCode Optional confirmation code from SMS/email
     * @return Payment confirmation result
     */
    suspend fun confirmPayment(
        accessToken: String,
        paymentId: String,
        confirmationCode: String? = null
    ): Result<PaymentConfirmation>
    
    /**
     * Validate payment limits before initiation
     * @param accessToken Valid access token
     * @param amount Payment amount
     * @param currency Payment currency (ISO 4217)
     * @return Validation result with limit information
     */
    suspend fun validatePaymentLimits(
        accessToken: String,
        amount: String,
        currency: String = "SAR"
    ): Result<PaymentLimitsValidation>
}