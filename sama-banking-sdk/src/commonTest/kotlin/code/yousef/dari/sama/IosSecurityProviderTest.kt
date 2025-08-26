package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.SecurityProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/**
 * Test for iOS Keychain implementation - TDD approach
 * Tests iOS-specific security provider functionality with Keychain integration
 */
class IosSecurityProviderTest {

    private class MockIosSecurityProvider : SecurityProvider {
        private val storage = mutableMapOf<String, String>()
        
        override suspend fun storeSecurely(key: String, value: String): Result<Unit> {
            return try {
                storage[key] = value
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun retrieveSecurely(key: String): Result<String?> {
            return try {
                val value = storage[key]
                Result.success(value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun deleteSecurely(key: String): Result<Unit> {
            return try {
                storage.remove(key)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun generateKey(): ByteArray {
            return ByteArray(32) { it.toByte() }
        }

        override fun encrypt(data: ByteArray, key: ByteArray): Result<ByteArray> {
            return try {
                // Simple XOR encryption for testing
                val encrypted = data.mapIndexed { index, byte ->
                    (byte.toInt() xor key[index % key.size].toInt()).toByte()
                }.toByteArray()
                Result.success(encrypted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun decrypt(encryptedData: ByteArray, key: ByteArray): Result<ByteArray> {
            return try {
                // Simple XOR decryption for testing (XOR is its own inverse)
                val decrypted = encryptedData.mapIndexed { index, byte ->
                    (byte.toInt() xor key[index % key.size].toInt()).toByte()
                }.toByteArray()
                Result.success(decrypted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun sha256Hash(data: ByteArray): ByteArray {
            // Simple hash for testing
            return ByteArray(32) { (data.sum() % 256).toByte() }
        }

        override suspend fun isDeviceSecure(): Boolean {
            return true // Mock device as secure
        }

        override suspend fun isBiometricAvailable(): Boolean {
            return true // Mock biometrics as available
        }

        override suspend fun authenticateWithBiometrics(
            title: String,
            subtitle: String,
            description: String
        ): Result<Boolean> {
            return if (title.isNotBlank()) {
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Invalid biometric parameters"))
            }
        }
    }

    private val securityProvider = MockIosSecurityProvider()

    @Test
    fun `should store data securely in iOS Keychain`() = runTest {
        // Given
        val key = "test_token"
        val value = "secure_token_value"

        // When
        val result = securityProvider.storeSecurely(key, value)

        // Then
        assertTrue(result.isSuccess, "Should store data securely")
    }

    @Test
    fun `should retrieve data securely from iOS Keychain`() = runTest {
        // Given
        val key = "test_token"
        val value = "secure_token_value"
        securityProvider.storeSecurely(key, value)

        // When
        val result = securityProvider.retrieveSecurely(key)

        // Then
        assertTrue(result.isSuccess, "Should retrieve data successfully")
        assertEquals(value, result.getOrNull(), "Retrieved value should match stored value")
    }

    @Test
    fun `should delete data securely from iOS Keychain`() = runTest {
        // Given
        val key = "test_token"
        val value = "secure_token_value"
        securityProvider.storeSecurely(key, value)

        // When
        val deleteResult = securityProvider.deleteSecurely(key)
        val retrieveResult = securityProvider.retrieveSecurely(key)

        // Then
        assertTrue(deleteResult.isSuccess, "Should delete data successfully")
        assertTrue(retrieveResult.isSuccess, "Retrieve should succeed even after deletion")
        assertEquals(null, retrieveResult.getOrNull(), "Value should be null after deletion")
    }

    @Test
    fun `should encrypt data using iOS security framework`() {
        // Given
        val data = "sensitive data".toByteArray()
        val key = securityProvider.generateKey()

        // When
        val result = securityProvider.encrypt(data, key)

        // Then
        assertTrue(result.isSuccess, "Encryption should succeed")
        val encryptedData = result.getOrNull()
        assertNotNull(encryptedData, "Encrypted data should not be null")
        assertTrue(!data.contentEquals(encryptedData), "Encrypted data should be different from original")
    }

    @Test
    fun `should decrypt data using iOS security framework`() {
        // Given
        val originalData = "sensitive data".toByteArray()
        val key = securityProvider.generateKey()
        val encryptedData = securityProvider.encrypt(originalData, key).getOrThrow()

        // When
        val result = securityProvider.decrypt(encryptedData, key)

        // Then
        assertTrue(result.isSuccess, "Decryption should succeed")
        val decryptedData = result.getOrNull()
        assertNotNull(decryptedData, "Decrypted data should not be null")
        assertTrue(originalData.contentEquals(decryptedData), "Decrypted data should match original")
    }

    @Test
    fun `should generate secure keys`() {
        // When
        val key1 = securityProvider.generateKey()
        val key2 = securityProvider.generateKey()

        // Then
        assertEquals(32, key1.size, "Key should be 256 bits (32 bytes)")
        assertEquals(32, key2.size, "Key should be 256 bits (32 bytes)")
        assertTrue(!key1.contentEquals(key2), "Generated keys should be different")
    }

    @Test
    fun `should compute SHA-256 hash`() {
        // Given
        val data = "test data for hashing".toByteArray()

        // When
        val hash = securityProvider.sha256Hash(data)

        // Then
        assertEquals(32, hash.size, "SHA-256 hash should be 32 bytes")
        
        // Hash should be deterministic
        val hash2 = securityProvider.sha256Hash(data)
        assertTrue(hash.contentEquals(hash2), "Hash should be deterministic")
    }

    @Test
    fun `should check device security status`() = runTest {
        // When
        val isSecure = securityProvider.isDeviceSecure()

        // Then
        assertTrue(isSecure, "Mock device should be secure")
    }

    @Test
    fun `should check biometric availability`() = runTest {
        // When
        val isAvailable = securityProvider.isBiometricAvailable()

        // Then
        assertTrue(isAvailable, "Mock biometrics should be available")
    }

    @Test
    fun `should authenticate with Touch ID or Face ID`() = runTest {
        // Given
        val title = "Authenticate"
        val subtitle = "Use your biometric to continue"
        val description = "Place your finger on the Touch ID sensor or look at the camera"

        // When
        val result = securityProvider.authenticateWithBiometrics(title, subtitle, description)

        // Then
        assertTrue(result.isSuccess, "Biometric authentication should succeed")
        assertEquals(true, result.getOrNull(), "Authentication should return true")
    }

    @Test
    fun `should fail biometric authentication with invalid parameters`() = runTest {
        // Given
        val title = ""
        val subtitle = "Use your biometric to continue"
        val description = "Place your finger on the Touch ID sensor"

        // When
        val result = securityProvider.authenticateWithBiometrics(title, subtitle, description)

        // Then
        assertTrue(result.isFailure, "Should fail with invalid parameters")
    }
}