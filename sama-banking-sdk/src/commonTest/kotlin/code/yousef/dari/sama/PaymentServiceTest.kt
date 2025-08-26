package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.PaymentService
import code.yousef.dari.sama.models.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for PaymentService implementation - TDD approach
 * Tests payment initiation, status checking, scheduled payments, and limits validation
 */
class PaymentServiceTest {

    private class MockPaymentService : PaymentService {
        private val payments = mutableMapOf<String, DomesticPayment>()
        private val scheduledPayments = mutableMapOf<String, ScheduledPayment>()

        override suspend fun initiateDomesticPayment(
            accessToken: String,
            consentId: String,
            payment: DomesticPaymentRequest
        ): Result<DomesticPayment> {
            return if (accessToken.isNotBlank() && consentId.isNotBlank()) {
                val paymentId = "pay-${System.currentTimeMillis()}"
                val domesticPayment = DomesticPayment(
                    domesticPaymentId = paymentId,
                    status = PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                    creationDateTime = Clock.System.now(),
                    statusUpdateDateTime = Clock.System.now(),
                    expectedExecutionDateTime = Clock.System.now(),
                    instructedAmount = payment.instructedAmount,
                    debtorAccount = payment.debtorAccount,
                    creditorAccount = payment.creditorAccount,
                    remittanceInformation = payment.remittanceInformation
                )
                payments[paymentId] = domesticPayment
                Result.success(domesticPayment)
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun getDomesticPaymentStatus(
            accessToken: String,
            domesticPaymentId: String
        ): Result<DomesticPayment> {
            return if (accessToken.isNotBlank() && domesticPaymentId.isNotBlank()) {
                val payment = payments[domesticPaymentId]
                if (payment != null) {
                    Result.success(payment)
                } else {
                    Result.failure(Exception("Payment not found"))
                }
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun createScheduledPayment(
            accessToken: String,
            consentId: String,
            scheduledPayment: ScheduledPaymentRequest
        ): Result<ScheduledPayment> {
            return if (accessToken.isNotBlank() && consentId.isNotBlank()) {
                val paymentId = "scheduled-${System.currentTimeMillis()}"
                val payment = ScheduledPayment(
                    scheduledPaymentId = paymentId,
                    status = PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                    creationDateTime = Clock.System.now(),
                    statusUpdateDateTime = Clock.System.now(),
                    requestedExecutionDateTime = scheduledPayment.requestedExecutionDateTime,
                    instructedAmount = scheduledPayment.instructedAmount,
                    debtorAccount = scheduledPayment.debtorAccount,
                    creditorAccount = scheduledPayment.creditorAccount,
                    remittanceInformation = scheduledPayment.remittanceInformation
                )
                scheduledPayments[paymentId] = payment
                Result.success(payment)
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun cancelScheduledPayment(
            accessToken: String,
            scheduledPaymentId: String
        ): Result<Unit> {
            return if (accessToken.isNotBlank() && scheduledPaymentId.isNotBlank()) {
                scheduledPayments.remove(scheduledPaymentId)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun confirmPayment(
            accessToken: String,
            paymentId: String,
            confirmationCode: String
        ): Result<PaymentConfirmation> {
            return if (accessToken.isNotBlank() && paymentId.isNotBlank() && confirmationCode.isNotBlank()) {
                Result.success(
                    PaymentConfirmation(
                        paymentId = paymentId,
                        status = PaymentStatus.ACCEPTED_SETTLEMENT_COMPLETED,
                        confirmationDateTime = Clock.System.now(),
                        confirmationCode = confirmationCode
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun validatePaymentLimits(
            accessToken: String,
            amount: Amount,
            accountId: String
        ): Result<PaymentLimitsValidation> {
            return if (accessToken.isNotBlank() && accountId.isNotBlank()) {
                val amountValue = amount.amount.toDoubleOrNull() ?: 0.0
                Result.success(
                    PaymentLimitsValidation(
                        isValid = amountValue <= 50000.0, // Mock limit
                        dailyLimit = Amount("50000.00", "SAR"),
                        monthlyLimit = Amount("500000.00", "SAR"),
                        remainingDailyLimit = Amount("${50000.0 - amountValue}", "SAR"),
                        remainingMonthlyLimit = Amount("${500000.0 - amountValue}", "SAR"),
                        violations = if (amountValue > 50000.0) listOf("Exceeds daily limit") else emptyList()
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }
    }

    private val paymentService = MockPaymentService()

    @Test
    fun `should initiate domestic payment successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentId = "consent-123"
        val paymentRequest = DomesticPaymentRequest(
            instructedAmount = Amount("1000.00", "SAR"),
            debtorAccount = DebtorAccount(
                iban = "SA0310000001234567890",
                name = "John Doe"
            ),
            creditorAccount = CreditorAccount(
                iban = "SA0320000001234567891", 
                name = "Jane Smith"
            ),
            remittanceInformation = RemittanceInformation(
                unstructured = "Payment for services"
            )
        )

        // When
        val result = paymentService.initiateDomesticPayment(accessToken, consentId, paymentRequest)

        // Then
        assertTrue(result.isSuccess, "Payment initiation should succeed")
        val payment = result.getOrNull()
        assertNotNull(payment, "Payment should not be null")
        assertTrue(payment.domesticPaymentId.startsWith("pay-"), "Payment ID should have correct prefix")
        assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, payment.status)
        assertEquals("1000.00", payment.instructedAmount.amount)
        assertEquals("SAR", payment.instructedAmount.currency)
        assertEquals("SA0310000001234567890", payment.debtorAccount.iban)
        assertEquals("SA0320000001234567891", payment.creditorAccount.iban)
        assertEquals("Payment for services", payment.remittanceInformation?.unstructured)
    }

    @Test
    fun `should fail to initiate payment with invalid token`() = runTest {
        // Given
        val invalidToken = ""
        val consentId = "consent-123"
        val paymentRequest = DomesticPaymentRequest(
            instructedAmount = Amount("1000.00", "SAR"),
            debtorAccount = DebtorAccount(iban = "SA0310000001234567890", name = "John Doe"),
            creditorAccount = CreditorAccount(iban = "SA0320000001234567891", name = "Jane Smith"),
            remittanceInformation = RemittanceInformation(unstructured = "Payment for services")
        )

        // When
        val result = paymentService.initiateDomesticPayment(invalidToken, consentId, paymentRequest)

        // Then
        assertTrue(result.isFailure, "Should fail with invalid token")
    }

    @Test
    fun `should get payment status successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentId = "consent-123"
        val paymentRequest = DomesticPaymentRequest(
            instructedAmount = Amount("1000.00", "SAR"),
            debtorAccount = DebtorAccount(iban = "SA0310000001234567890", name = "John Doe"),
            creditorAccount = CreditorAccount(iban = "SA0320000001234567891", name = "Jane Smith"),
            remittanceInformation = RemittanceInformation(unstructured = "Payment for services")
        )

        // Create payment first
        val createResult = paymentService.initiateDomesticPayment(accessToken, consentId, paymentRequest)
        val paymentId = createResult.getOrNull()!!.domesticPaymentId

        // When
        val result = paymentService.getDomesticPaymentStatus(accessToken, paymentId)

        // Then
        assertTrue(result.isSuccess, "Should get payment status successfully")
        val payment = result.getOrNull()
        assertNotNull(payment, "Payment should not be null")
        assertEquals(paymentId, payment.domesticPaymentId)
        assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, payment.status)
    }

    @Test
    fun `should fail to get status for non-existent payment`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val nonExistentPaymentId = "non-existent-payment"

        // When
        val result = paymentService.getDomesticPaymentStatus(accessToken, nonExistentPaymentId)

        // Then
        assertTrue(result.isFailure, "Should fail for non-existent payment")
    }

    @Test
    fun `should create scheduled payment successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentId = "consent-123"
        val scheduledPaymentRequest = ScheduledPaymentRequest(
            requestedExecutionDateTime = Clock.System.now(),
            instructedAmount = Amount("2000.00", "SAR"),
            debtorAccount = DebtorAccount(iban = "SA0310000001234567890", name = "John Doe"),
            creditorAccount = CreditorAccount(iban = "SA0320000001234567891", name = "Jane Smith"),
            remittanceInformation = RemittanceInformation(unstructured = "Monthly rent payment")
        )

        // When
        val result = paymentService.createScheduledPayment(accessToken, consentId, scheduledPaymentRequest)

        // Then
        assertTrue(result.isSuccess, "Scheduled payment creation should succeed")
        val payment = result.getOrNull()
        assertNotNull(payment, "Scheduled payment should not be null")
        assertTrue(payment.scheduledPaymentId.startsWith("scheduled-"), "Payment ID should have correct prefix")
        assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, payment.status)
        assertEquals("2000.00", payment.instructedAmount.amount)
        assertEquals("Monthly rent payment", payment.remittanceInformation?.unstructured)
    }

    @Test
    fun `should cancel scheduled payment successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentId = "consent-123"
        val scheduledPaymentRequest = ScheduledPaymentRequest(
            requestedExecutionDateTime = Clock.System.now(),
            instructedAmount = Amount("2000.00", "SAR"),
            debtorAccount = DebtorAccount(iban = "SA0310000001234567890", name = "John Doe"),
            creditorAccount = CreditorAccount(iban = "SA0320000001234567891", name = "Jane Smith"),
            remittanceInformation = RemittanceInformation(unstructured = "Monthly rent payment")
        )

        // Create scheduled payment first
        val createResult = paymentService.createScheduledPayment(accessToken, consentId, scheduledPaymentRequest)
        val paymentId = createResult.getOrNull()!!.scheduledPaymentId

        // When
        val result = paymentService.cancelScheduledPayment(accessToken, paymentId)

        // Then
        assertTrue(result.isSuccess, "Scheduled payment cancellation should succeed")
    }

    @Test
    fun `should confirm payment successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val paymentId = "pay-123"
        val confirmationCode = "123456"

        // When
        val result = paymentService.confirmPayment(accessToken, paymentId, confirmationCode)

        // Then
        assertTrue(result.isSuccess, "Payment confirmation should succeed")
        val confirmation = result.getOrNull()
        assertNotNull(confirmation, "Confirmation should not be null")
        assertEquals(paymentId, confirmation.paymentId)
        assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_COMPLETED, confirmation.status)
        assertEquals(confirmationCode, confirmation.confirmationCode)
    }

    @Test
    fun `should validate payment limits successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val amount = Amount("25000.00", "SAR") // Within limit
        val accountId = "acc-001"

        // When
        val result = paymentService.validatePaymentLimits(accessToken, amount, accountId)

        // Then
        assertTrue(result.isSuccess, "Payment limits validation should succeed")
        val validation = result.getOrNull()
        assertNotNull(validation, "Validation result should not be null")
        assertTrue(validation.isValid, "Amount should be within limits")
        assertEquals("50000.00", validation.dailyLimit.amount)
        assertEquals("500000.00", validation.monthlyLimit.amount)
        assertEquals("25000.00", validation.remainingDailyLimit.amount)
        assertTrue(validation.violations.isEmpty(), "Should have no violations")
    }

    @Test
    fun `should detect payment limit violations`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val amount = Amount("75000.00", "SAR") // Exceeds daily limit
        val accountId = "acc-001"

        // When
        val result = paymentService.validatePaymentLimits(accessToken, amount, accountId)

        // Then
        assertTrue(result.isSuccess, "Payment limits validation should succeed")
        val validation = result.getOrNull()
        assertNotNull(validation, "Validation result should not be null")
        assertFalse(validation.isValid, "Amount should exceed limits")
        assertTrue(validation.violations.isNotEmpty(), "Should have violations")
        assertTrue(validation.violations.contains("Exceeds daily limit"))
    }

    @Test
    fun `should handle empty confirmation code`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val paymentId = "pay-123"
        val emptyConfirmationCode = ""

        // When
        val result = paymentService.confirmPayment(accessToken, paymentId, emptyConfirmationCode)

        // Then
        assertTrue(result.isFailure, "Should fail with empty confirmation code")
    }

    @Test
    fun `should handle invalid scheduled payment cancellation`() = runTest {
        // Given
        val accessToken = ""
        val paymentId = "scheduled-123"

        // When
        val result = paymentService.cancelScheduledPayment(accessToken, paymentId)

        // Then
        assertTrue(result.isFailure, "Should fail with invalid access token")
    }
}