package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.ConsentManager
import code.yousef.dari.sama.models.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for ConsentManager implementation - TDD approach
 * Tests consent creation, validation, revocation, and audit logging
 */
class ConsentManagerTest {

    private class MockConsentManager : ConsentManager {
        private val consents = mutableMapOf<String, ConsentDetails>()
        private val auditLogs = mutableListOf<ConsentAuditLog>()

        override suspend fun createAccountConsent(
            accessToken: String,
            consentRequest: AccountConsentRequest
        ): Result<ConsentResponse> {
            return if (accessToken.isNotBlank()) {
                val consentId = "consent-${System.currentTimeMillis()}"
                val consent = ConsentDetails(
                    consentId = consentId,
                    status = ConsentStatus.AWAITING_AUTHORIZATION,
                    permissions = consentRequest.permissions,
                    expirationDateTime = consentRequest.expirationDateTime,
                    transactionFromDateTime = consentRequest.transactionFromDateTime,
                    transactionToDateTime = consentRequest.transactionToDateTime,
                    creationDateTime = Clock.System.now(),
                    statusUpdateDateTime = Clock.System.now()
                )
                consents[consentId] = consent
                
                // Log audit event
                auditLogs.add(
                    ConsentAuditLog(
                        consentId = consentId,
                        action = ConsentAction.CREATED,
                        timestamp = Clock.System.now(),
                        permissions = consentRequest.permissions
                    )
                )
                
                Result.success(
                    ConsentResponse(
                        consentId = consentId,
                        status = ConsentStatus.AWAITING_AUTHORIZATION,
                        creationDateTime = consent.creationDateTime,
                        statusUpdateDateTime = consent.statusUpdateDateTime,
                        permissions = consentRequest.permissions,
                        expirationDateTime = consentRequest.expirationDateTime
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid access token"))
            }
        }

        override suspend fun createPaymentConsent(
            accessToken: String,
            consentRequest: PaymentConsentRequest
        ): Result<ConsentResponse> {
            return if (accessToken.isNotBlank()) {
                val consentId = "payment-consent-${System.currentTimeMillis()}"
                val consent = ConsentDetails(
                    consentId = consentId,
                    status = ConsentStatus.AWAITING_AUTHORIZATION,
                    permissions = listOf(Permission.PAYMENTS),
                    expirationDateTime = consentRequest.expirationDateTime,
                    creationDateTime = Clock.System.now(),
                    statusUpdateDateTime = Clock.System.now()
                )
                consents[consentId] = consent
                
                Result.success(
                    ConsentResponse(
                        consentId = consentId,
                        status = ConsentStatus.AWAITING_AUTHORIZATION,
                        creationDateTime = consent.creationDateTime,
                        statusUpdateDateTime = consent.statusUpdateDateTime,
                        permissions = listOf(Permission.PAYMENTS),
                        expirationDateTime = consentRequest.expirationDateTime
                    )
                )
            } else {
                Result.failure(IllegalArgumentException("Invalid access token"))
            }
        }

        override suspend fun getConsentStatus(
            accessToken: String,
            consentId: String
        ): Result<ConsentDetails> {
            return if (accessToken.isNotBlank() && consentId.isNotBlank()) {
                val consent = consents[consentId]
                if (consent != null) {
                    Result.success(consent)
                } else {
                    Result.failure(Exception("Consent not found"))
                }
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun revokeConsent(
            accessToken: String,
            consentId: String
        ): Result<Unit> {
            return if (accessToken.isNotBlank() && consentId.isNotBlank()) {
                val consent = consents[consentId]
                if (consent != null) {
                    val updatedConsent = consent.copy(
                        status = ConsentStatus.REVOKED,
                        statusUpdateDateTime = Clock.System.now()
                    )
                    consents[consentId] = updatedConsent
                    
                    // Log audit event
                    auditLogs.add(
                        ConsentAuditLog(
                            consentId = consentId,
                            action = ConsentAction.REVOKED,
                            timestamp = Clock.System.now(),
                            permissions = consent.permissions
                        )
                    )
                    
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Consent not found"))
                }
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        override suspend fun validateConsentPermissions(
            consentId: String,
            requiredPermissions: List<Permission>
        ): Result<Boolean> {
            return if (consentId.isNotBlank()) {
                val consent = consents[consentId]
                if (consent != null && consent.status == ConsentStatus.AUTHORIZED) {
                    val hasAllPermissions = requiredPermissions.all { required ->
                        consent.permissions.contains(required)
                    }
                    Result.success(hasAllPermissions)
                } else {
                    Result.success(false)
                }
            } else {
                Result.failure(IllegalArgumentException("Invalid consent ID"))
            }
        }

        override suspend fun isConsentExpired(consentId: String): Result<Boolean> {
            return if (consentId.isNotBlank()) {
                val consent = consents[consentId]
                if (consent != null) {
                    val now = Clock.System.now()
                    val isExpired = consent.expirationDateTime?.let { expiry ->
                        now >= expiry
                    } ?: false
                    Result.success(isExpired)
                } else {
                    Result.failure(Exception("Consent not found"))
                }
            } else {
                Result.failure(IllegalArgumentException("Invalid consent ID"))
            }
        }

        override suspend fun getConsentAuditLog(
            accessToken: String,
            consentId: String
        ): Result<List<ConsentAuditLog>> {
            return if (accessToken.isNotBlank() && consentId.isNotBlank()) {
                val logs = auditLogs.filter { it.consentId == consentId }
                Result.success(logs)
            } else {
                Result.failure(IllegalArgumentException("Invalid parameters"))
            }
        }

        // Helper method for testing
        fun authorizeConsent(consentId: String) {
            consents[consentId]?.let { consent ->
                consents[consentId] = consent.copy(
                    status = ConsentStatus.AUTHORIZED,
                    statusUpdateDateTime = Clock.System.now()
                )
            }
        }
    }

    private val consentManager = MockConsentManager()

    @Test
    fun `should create account consent successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC, Permission.READ_ACCOUNTS_DETAIL),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90),
            transactionFromDateTime = Clock.System.now().minus(kotlinx.datetime.DateTimeUnit.DAY, 30),
            transactionToDateTime = null
        )

        // When
        val result = consentManager.createAccountConsent(accessToken, consentRequest)

        // Then
        assertTrue(result.isSuccess, "Account consent creation should succeed")
        val consent = result.getOrNull()
        assertNotNull(consent, "Consent should not be null")
        assertTrue(consent.consentId.startsWith("consent-"), "Consent ID should have correct prefix")
        assertEquals(ConsentStatus.AWAITING_AUTHORIZATION, consent.status)
        assertEquals(consentRequest.permissions, consent.permissions)
        assertEquals(consentRequest.expirationDateTime, consent.expirationDateTime)
    }

    @Test
    fun `should fail to create consent with invalid token`() = runTest {
        // Given
        val invalidToken = ""
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90)
        )

        // When
        val result = consentManager.createAccountConsent(invalidToken, consentRequest)

        // Then
        assertTrue(result.isFailure, "Should fail with invalid token")
    }

    @Test
    fun `should create payment consent successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = PaymentConsentRequest(
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.HOUR, 1)
        )

        // When
        val result = consentManager.createPaymentConsent(accessToken, consentRequest)

        // Then
        assertTrue(result.isSuccess, "Payment consent creation should succeed")
        val consent = result.getOrNull()
        assertNotNull(consent, "Consent should not be null")
        assertTrue(consent.consentId.startsWith("payment-consent-"), "Consent ID should have correct prefix")
        assertEquals(ConsentStatus.AWAITING_AUTHORIZATION, consent.status)
        assertTrue(consent.permissions.contains(Permission.PAYMENTS))
    }

    @Test
    fun `should get consent status successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90)
        )

        // Create consent first
        val createResult = consentManager.createAccountConsent(accessToken, consentRequest)
        val consentId = createResult.getOrNull()!!.consentId

        // When
        val result = consentManager.getConsentStatus(accessToken, consentId)

        // Then
        assertTrue(result.isSuccess, "Should get consent status successfully")
        val consentDetails = result.getOrNull()
        assertNotNull(consentDetails, "Consent details should not be null")
        assertEquals(consentId, consentDetails.consentId)
        assertEquals(ConsentStatus.AWAITING_AUTHORIZATION, consentDetails.status)
    }

    @Test
    fun `should revoke consent successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90)
        )

        // Create consent first
        val createResult = consentManager.createAccountConsent(accessToken, consentRequest)
        val consentId = createResult.getOrNull()!!.consentId

        // When
        val result = consentManager.revokeConsent(accessToken, consentId)

        // Then
        assertTrue(result.isSuccess, "Consent revocation should succeed")
        
        // Verify consent is revoked
        val statusResult = consentManager.getConsentStatus(accessToken, consentId)
        assertTrue(statusResult.isSuccess, "Should get status after revocation")
        assertEquals(ConsentStatus.REVOKED, statusResult.getOrNull()!!.status)
    }

    @Test
    fun `should validate consent permissions correctly`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC, Permission.READ_ACCOUNTS_DETAIL),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90)
        )

        // Create and authorize consent
        val createResult = consentManager.createAccountConsent(accessToken, consentRequest)
        val consentId = createResult.getOrNull()!!.consentId
        consentManager.authorizeConsent(consentId)

        // When - Test with valid permissions
        val validPermissions = listOf(Permission.READ_ACCOUNTS_BASIC)
        val validResult = consentManager.validateConsentPermissions(consentId, validPermissions)

        // When - Test with invalid permissions
        val invalidPermissions = listOf(Permission.PAYMENTS)
        val invalidResult = consentManager.validateConsentPermissions(consentId, invalidPermissions)

        // Then
        assertTrue(validResult.isSuccess, "Valid permissions check should succeed")
        assertTrue(validResult.getOrNull() == true, "Should have valid permissions")
        
        assertTrue(invalidResult.isSuccess, "Invalid permissions check should succeed")
        assertTrue(invalidResult.getOrNull() == false, "Should not have invalid permissions")
    }

    @Test
    fun `should detect consent expiry correctly`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val expiredRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().minus(kotlinx.datetime.DateTimeUnit.DAY, 1) // Expired
        )
        val validRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90) // Valid
        )

        // Create consents
        val expiredConsentId = consentManager.createAccountConsent(accessToken, expiredRequest)
            .getOrNull()!!.consentId
        val validConsentId = consentManager.createAccountConsent(accessToken, validRequest)
            .getOrNull()!!.consentId

        // When
        val expiredResult = consentManager.isConsentExpired(expiredConsentId)
        val validResult = consentManager.isConsentExpired(validConsentId)

        // Then
        assertTrue(expiredResult.isSuccess, "Expired consent check should succeed")
        assertTrue(expiredResult.getOrNull() == true, "Should detect expired consent")
        
        assertTrue(validResult.isSuccess, "Valid consent check should succeed")
        assertTrue(validResult.getOrNull() == false, "Should detect valid consent")
    }

    @Test
    fun `should get consent audit log successfully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90)
        )

        // Create and revoke consent to generate audit logs
        val createResult = consentManager.createAccountConsent(accessToken, consentRequest)
        val consentId = createResult.getOrNull()!!.consentId
        consentManager.revokeConsent(accessToken, consentId)

        // When
        val result = consentManager.getConsentAuditLog(accessToken, consentId)

        // Then
        assertTrue(result.isSuccess, "Should get audit log successfully")
        val auditLogs = result.getOrNull()
        assertNotNull(auditLogs, "Audit logs should not be null")
        assertEquals(2, auditLogs.size, "Should have 2 audit logs (created and revoked)")
        
        val createLog = auditLogs.find { it.action == ConsentAction.CREATED }
        val revokeLog = auditLogs.find { it.action == ConsentAction.REVOKED }
        
        assertNotNull(createLog, "Should have create log")
        assertNotNull(revokeLog, "Should have revoke log")
        assertEquals(consentId, createLog.consentId)
        assertEquals(consentId, revokeLog.consentId)
    }

    @Test
    fun `should handle non-existent consent gracefully`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val nonExistentConsentId = "non-existent-consent"

        // When
        val statusResult = consentManager.getConsentStatus(accessToken, nonExistentConsentId)
        val revokeResult = consentManager.revokeConsent(accessToken, nonExistentConsentId)
        val expiredResult = consentManager.isConsentExpired(nonExistentConsentId)

        // Then
        assertTrue(statusResult.isFailure, "Should fail for non-existent consent status")
        assertTrue(revokeResult.isFailure, "Should fail for non-existent consent revocation")
        assertTrue(expiredResult.isFailure, "Should fail for non-existent consent expiry check")
    }

    @Test
    fun `should validate unauthorized consent permissions correctly`() = runTest {
        // Given
        val accessToken = "valid-access-token"
        val consentRequest = AccountConsentRequest(
            permissions = listOf(Permission.READ_ACCOUNTS_BASIC),
            expirationDateTime = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY, 90)
        )

        // Create consent but don't authorize it
        val createResult = consentManager.createAccountConsent(accessToken, consentRequest)
        val consentId = createResult.getOrNull()!!.consentId

        // When
        val result = consentManager.validateConsentPermissions(consentId, listOf(Permission.READ_ACCOUNTS_BASIC))

        // Then
        assertTrue(result.isSuccess, "Permission validation should succeed")
        assertTrue(result.getOrNull() == false, "Should reject unauthorized consent")
    }
}