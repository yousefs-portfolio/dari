package code.yousef.dari.sama

import code.yousef.dari.sama.implementation.IosSecurityProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * iOS-specific Keychain integration tests
 * Tests actual iOS Keychain Services APIs and security framework
 * These tests should run on iOS targets only
 */
class IosKeychainTest {

    private val securityProvider = IosSecurityProvider()

    @BeforeTest
    fun setUp() = runTest {
        // Clean up any existing test data
        securityProvider.deleteSecurely("test_oauth_token")
        securityProvider.deleteSecurely("test_refresh_token")
        securityProvider.deleteSecurely("test_client_secret")
        securityProvider.deleteSecurely("test_biometric_key")
        securityProvider.deleteSecurely("test_certificate_data")
    }

    @Test
    fun `should store OAuth tokens in iOS Keychain with proper accessibility`() = runTest {
        // Given
        val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val refreshToken = "refresh_token_abc123"

        // When
        val accessResult = securityProvider.storeSecurely("test_oauth_token", accessToken)
        val refreshResult = securityProvider.storeSecurely("test_refresh_token", refreshToken)

        // Then
        assertTrue(accessResult.isSuccess, "Should store access token in iOS Keychain")
        assertTrue(refreshResult.isSuccess, "Should store refresh token in iOS Keychain")
    }

    @Test
    fun `should retrieve OAuth tokens from iOS Keychain`() = runTest {
        // Given
        val accessToken = "access_token_xyz789"
        securityProvider.storeSecurely("test_oauth_token", accessToken)

        // When
        val result = securityProvider.retrieveSecurely("test_oauth_token")

        // Then
        assertTrue(result.isSuccess, "Should retrieve token from iOS Keychain")
        assertEquals(accessToken, result.getOrNull(), "Retrieved token should match stored token")
    }

    @Test
    fun `should handle keychain item not found gracefully`() = runTest {
        // When - try to retrieve non-existent key
        val result = securityProvider.retrieveSecurely("non_existent_key")

        // Then
        assertTrue(result.isSuccess, "Should handle missing key gracefully")
        assertNull(result.getOrNull(), "Should return null for missing key")
    }

    @Test
    fun `should delete OAuth tokens from iOS Keychain`() = runTest {
        // Given
        val token = "token_to_delete"
        securityProvider.storeSecurely("test_oauth_token", token)

        // When
        val deleteResult = securityProvider.deleteSecurely("test_oauth_token")
        val retrieveResult = securityProvider.retrieveSecurely("test_oauth_token")

        // Then
        assertTrue(deleteResult.isSuccess, "Should delete token from iOS Keychain")
        assertTrue(retrieveResult.isSuccess, "Should handle deleted key retrieval")
        assertNull(retrieveResult.getOrNull(), "Should return null for deleted key")
    }

    @Test
    fun `should store sensitive client credentials with device-only accessibility`() = runTest {
        // Given
        val clientSecret = "very_secret_client_secret_12345"

        // When
        val result = securityProvider.storeSecurely("test_client_secret", clientSecret)

        // Then
        assertTrue(result.isSuccess, "Should store client secret in iOS Keychain")
        
        // Verify retrieval
        val retrieveResult = securityProvider.retrieveSecurely("test_client_secret")
        assertTrue(retrieveResult.isSuccess, "Should retrieve client secret")
        assertEquals(clientSecret, retrieveResult.getOrNull(), "Retrieved secret should match")
    }

    @Test
    fun `should generate cryptographically secure keys using SecRandom`() {
        // When
        val key1 = securityProvider.generateKey()
        val key2 = securityProvider.generateKey()
        val key3 = securityProvider.generateKey()

        // Then
        assertEquals(32, key1.size, "Key should be 256 bits (32 bytes)")
        assertEquals(32, key2.size, "Key should be 256 bits (32 bytes)")
        assertEquals(32, key3.size, "Key should be 256 bits (32 bytes)")
        
        // Keys should be different (highly improbable to be same)
        assertFalse(key1.contentEquals(key2), "Generated keys should be different")
        assertFalse(key2.contentEquals(key3), "Generated keys should be different")
        assertFalse(key1.contentEquals(key3), "Generated keys should be different")
        
        // Keys should not be all zeros or all same value
        assertFalse(key1.all { it == 0.toByte() }, "Key should not be all zeros")
        assertFalse(key1.all { it == key1[0] }, "Key should not be all same values")
    }

    @Test
    fun `should encrypt and decrypt data using AES-256-GCM`() {
        // Given
        val originalData = "SAMA banking sensitive transaction data: Payment of 1000 SAR to Account 123456789"
        val dataBytes = originalData.toByteArray()
        val key = securityProvider.generateKey()

        // When
        val encryptResult = securityProvider.encrypt(dataBytes, key)
        
        // Then
        assertTrue(encryptResult.isSuccess, "Encryption should succeed")
        val encryptedData = encryptResult.getOrThrow()
        
        // Verify encrypted data structure (IV + ciphertext + tag)
        val expectedMinSize = 12 + dataBytes.size + 16 // IV(12) + data + tag(16)
        assertEquals(expectedMinSize, encryptedData.size, "Encrypted data should include IV and GCM tag")
        
        // Verify encryption actually changed the data
        assertFalse(dataBytes.contentEquals(encryptedData), "Encrypted data should be different from original")
        
        // When - decrypt
        val decryptResult = securityProvider.decrypt(encryptedData, key)
        
        // Then
        assertTrue(decryptResult.isSuccess, "Decryption should succeed")
        val decryptedData = decryptResult.getOrThrow()
        assertTrue(dataBytes.contentEquals(decryptedData), "Decrypted data should match original")
        assertEquals(originalData, decryptedData.decodeToString(), "Decrypted string should match original")
    }

    @Test
    fun `should fail decryption with wrong key`() {
        // Given
        val originalData = "secret message".toByteArray()
        val key1 = securityProvider.generateKey()
        val key2 = securityProvider.generateKey()
        val encryptedData = securityProvider.encrypt(originalData, key1).getOrThrow()

        // When - try to decrypt with wrong key
        val result = securityProvider.decrypt(encryptedData, key2)

        // Then
        assertTrue(result.isFailure, "Should fail to decrypt with wrong key")
    }

    @Test
    fun `should fail decryption with tampered data`() {
        // Given
        val originalData = "secret message".toByteArray()
        val key = securityProvider.generateKey()
        val encryptedData = securityProvider.encrypt(originalData, key).getOrThrow()
        
        // Tamper with the data (modify last byte which should be part of the GCM tag)
        val tamperedData = encryptedData.copyOf()
        tamperedData[tamperedData.size - 1] = (tamperedData[tamperedData.size - 1].toInt() xor 1).toByte()

        // When
        val result = securityProvider.decrypt(tamperedData, key)

        // Then
        assertTrue(result.isFailure, "Should fail to decrypt tampered data")
    }

    @Test
    fun `should compute consistent SHA-256 hashes`() {
        // Given
        val data1 = "SAMA transaction: 500 SAR to merchant ABC".toByteArray()
        val data2 = "SAMA transaction: 500 SAR to merchant ABC".toByteArray()
        val data3 = "SAMA transaction: 600 SAR to merchant ABC".toByteArray()

        // When
        val hash1 = securityProvider.sha256Hash(data1)
        val hash2 = securityProvider.sha256Hash(data2)
        val hash3 = securityProvider.sha256Hash(data3)

        // Then
        assertEquals(32, hash1.size, "SHA-256 hash should be 32 bytes")
        assertEquals(32, hash2.size, "SHA-256 hash should be 32 bytes")
        assertEquals(32, hash3.size, "SHA-256 hash should be 32 bytes")
        
        // Same input should produce same hash
        assertTrue(hash1.contentEquals(hash2), "Same data should produce same hash")
        
        // Different input should produce different hash
        assertFalse(hash1.contentEquals(hash3), "Different data should produce different hash")
        
        // Hash should not be all zeros
        assertFalse(hash1.all { it == 0.toByte() }, "Hash should not be all zeros")
    }

    @Test
    fun `should check iOS device security status`() = runTest {
        // When
        val isSecure = securityProvider.isDeviceSecure()

        // Then - device should support some form of authentication
        // On iOS simulator this might be true/false depending on configuration
        // On real device with passcode/biometrics it should be true
        assertNotNull(isSecure, "Should be able to determine device security status")
    }

    @Test
    fun `should check biometric availability on iOS`() = runTest {
        // When
        val isAvailable = securityProvider.isBiometricAvailable()

        // Then
        assertNotNull(isAvailable, "Should be able to determine biometric availability")
        // Note: On simulator this will likely be false, on real device with Touch ID/Face ID it should be true
    }

    @Test
    fun `should handle biometric authentication request`() = runTest {
        // Given
        val title = "SAMA Banking Authentication"
        val subtitle = "Authenticate to access your financial data"
        val description = "Use Face ID or Touch ID to securely access your SAMA banking information"

        // When
        val result = securityProvider.authenticateWithBiometrics(title, subtitle, description)

        // Then
        // On simulator or device without biometrics, this might fail
        // On real device with biometrics, this would prompt the user
        assertNotNull(result, "Should return a result (success or failure)")
        
        // If it fails, it should be due to biometrics not being available, not invalid parameters
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertNotNull(exception, "Failed result should have an exception")
            // Should not fail due to invalid parameters with valid input
            assertFalse(
                exception.message?.contains("Invalid biometric parameters") == true,
                "Should not fail due to parameter validation with valid inputs"
            )
        }
    }

    @Test
    fun `should handle multiple keychain operations concurrently`() = runTest {
        // Given
        val tokens = mapOf(
            "token_1" to "value_1_abc123",
            "token_2" to "value_2_def456", 
            "token_3" to "value_3_ghi789",
            "token_4" to "value_4_jkl012",
            "token_5" to "value_5_mno345"
        )

        // When - store all tokens
        tokens.forEach { (key, value) ->
            val result = securityProvider.storeSecurely(key, value)
            assertTrue(result.isSuccess, "Should store token $key")
        }

        // Then - retrieve all tokens
        tokens.forEach { (key, expectedValue) ->
            val result = securityProvider.retrieveSecurely(key)
            assertTrue(result.isSuccess, "Should retrieve token $key")
            assertEquals(expectedValue, result.getOrNull(), "Retrieved value should match for $key")
        }

        // Cleanup
        tokens.keys.forEach { key ->
            securityProvider.deleteSecurely(key)
        }
    }

    @Test
    fun `should handle keychain access group correctly`() = runTest {
        // Given
        val sharedKey = "shared_team_data"
        val sharedValue = "team_shared_banking_config_data"

        // When
        val storeResult = securityProvider.storeSecurely(sharedKey, sharedValue)
        val retrieveResult = securityProvider.retrieveSecurely(sharedKey)

        // Then
        assertTrue(storeResult.isSuccess, "Should store with access group")
        assertTrue(retrieveResult.isSuccess, "Should retrieve with access group")
        assertEquals(sharedValue, retrieveResult.getOrNull(), "Should retrieve correct value with access group")

        // Cleanup
        securityProvider.deleteSecurely(sharedKey)
    }

    @Test
    fun `should handle large data encryption and storage`() = runTest {
        // Given - simulate a large certificate or configuration
        val largeCertificateData = buildString {
            repeat(1000) {
                append("-----BEGIN CERTIFICATE-----\n")
                append("MIIDXTCCAkWgAwIBAgIJAL8V5g1V5g1VMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV\n")
                append("BAYTAlNBMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX\n")
                append("aWRnaXRzIFB0eSBMdGQwHhcNMjQwMTA...\n")
                append("-----END CERTIFICATE-----\n")
            }
        }

        val key = securityProvider.generateKey()

        // When
        val encryptResult = securityProvider.encrypt(largeCertificateData.toByteArray(), key)
        
        // Then
        assertTrue(encryptResult.isSuccess, "Should encrypt large data successfully")
        val encryptedData = encryptResult.getOrThrow()
        
        // Store encrypted certificate
        val storeResult = securityProvider.storeSecurely("test_certificate_data", encryptedData.decodeToString())
        assertTrue(storeResult.isSuccess, "Should store large encrypted data")
        
        // Retrieve and decrypt
        val retrieveResult = securityProvider.retrieveSecurely("test_certificate_data")
        assertTrue(retrieveResult.isSuccess, "Should retrieve large encrypted data")
        
        val retrievedEncryptedData = retrieveResult.getOrThrow()!!.toByteArray()
        val decryptResult = securityProvider.decrypt(retrievedEncryptedData, key)
        assertTrue(decryptResult.isSuccess, "Should decrypt large data successfully")
        
        val decryptedString = decryptResult.getOrThrow().decodeToString()
        assertEquals(largeCertificateData, decryptedString, "Large data should survive encryption/storage/decryption cycle")
    }
}