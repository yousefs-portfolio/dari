package code.yousef.dari.sama

import code.yousef.dari.sama.interfaces.SecurityProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals

/**
 * Test for SecurityProvider implementation - TDD approach
 * Tests encryption, decryption, key generation, and secure storage
 */
class SecurityProviderTest {

    private class MockSecurityProvider : SecurityProvider {
        private val storage = mutableMapOf<String, String>()
        
        override suspend fun storeSecurely(key: String, value: String): Result<Unit> {
            return try {
                val encrypted = "encrypted_$value"
                storage[key] = encrypted
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun retrieveSecurely(key: String): Result<String?> {
            return try {
                val encrypted = storage[key]
                val decrypted = encrypted?.removePrefix("encrypted_")
                Result.success(decrypted)
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

        override fun encrypt(data: ByteArray, key: ByteArray): Result<ByteArray> {
            return try {
                // Mock encryption - simply XOR with key
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
                // Mock decryption - same as encryption (XOR is reversible)
                val decrypted = encryptedData.mapIndexed { index, byte ->
                    (byte.toInt() xor key[index % key.size].toInt()).toByte()
                }.toByteArray()
                Result.success(decrypted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun generateKey(): ByteArray {
            // Generate a mock 256-bit (32-byte) key
            return ByteArray(32) { (it * 7 + 42).toByte() }
        }

        override fun sha256Hash(data: ByteArray): ByteArray {
            // Mock SHA256 - simple hash function for testing
            val hash = data.fold(0) { acc, byte ->
                ((acc * 31 + byte.toInt()) and 0xFF)
            }
            return ByteArray(32) { (hash + it).toByte() }
        }

        override suspend fun isDeviceSecure(): Boolean {
            return true // Mock secure device
        }

        override suspend fun authenticateWithBiometrics(
            title: String,
            subtitle: String,
            description: String
        ): Result<Boolean> {
            return Result.success(true) // Mock successful biometric auth
        }

        override suspend fun isBiometricAvailable(): Boolean {
            return true // Mock biometric availability
        }
    }

    private val securityProvider = MockSecurityProvider()

    @Test
    fun `should store and retrieve data securely`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-sensitive-data"

        // When
        val storeResult = securityProvider.storeSecurely(key, value)
        val retrieveResult = securityProvider.retrieveSecurely(key)

        // Then
        assertTrue(storeResult.isSuccess, "Should store data successfully")
        assertTrue(retrieveResult.isSuccess, "Should retrieve data successfully")
        assertEquals(value, retrieveResult.getOrNull(), "Retrieved data should match stored data")
    }

    @Test
    fun `should return null for non-existent key`() = runTest {
        // Given
        val nonExistentKey = "non-existent-key"

        // When
        val result = securityProvider.retrieveSecurely(nonExistentKey)

        // Then
        assertTrue(result.isSuccess, "Should not fail for non-existent key")
        assertEquals(null, result.getOrNull(), "Should return null for non-existent key")
    }

    @Test
    fun `should delete stored data securely`() = runTest {
        // Given
        val key = "test-key-to-delete"
        val value = "test-data-to-delete"
        securityProvider.storeSecurely(key, value)

        // When
        val deleteResult = securityProvider.deleteSecurely(key)
        val retrieveResult = securityProvider.retrieveSecurely(key)

        // Then
        assertTrue(deleteResult.isSuccess, "Should delete data successfully")
        assertTrue(retrieveResult.isSuccess, "Retrieve should not fail after deletion")
        assertEquals(null, retrieveResult.getOrNull(), "Should return null after deletion")
    }

    @Test
    fun `should encrypt and decrypt data successfully`() {
        // Given
        val originalData = "Hello, SAMA Banking!".toByteArray()
        val key = securityProvider.generateKey()

        // When
        val encryptResult = securityProvider.encrypt(originalData, key)
        val decryptResult = encryptResult.getOrNull()?.let { encrypted ->
            securityProvider.decrypt(encrypted, key)
        }

        // Then
        assertTrue(encryptResult.isSuccess, "Encryption should succeed")
        assertNotNull(decryptResult, "Decryption result should not be null")
        assertTrue(decryptResult.isSuccess, "Decryption should succeed")
        
        val decryptedData = decryptResult.getOrNull()
        assertNotNull(decryptedData, "Decrypted data should not be null")
        assertEquals(
            originalData.decodeToString(),
            decryptedData.decodeToString(),
            "Decrypted data should match original"
        )
    }

    @Test
    fun `should generate unique encryption keys`() {
        // When
        val key1 = securityProvider.generateKey()
        val key2 = securityProvider.generateKey()

        // Then
        assertEquals(32, key1.size, "Key should be 256 bits (32 bytes)")
        assertEquals(32, key2.size, "Key should be 256 bits (32 bytes)")
        assertNotEquals(key1.toList(), key2.toList(), "Keys should be different")
    }

    @Test
    fun `should generate consistent SHA256 hash`() {
        // Given
        val data = "test data for hashing".toByteArray()

        // When
        val hash1 = securityProvider.sha256Hash(data)
        val hash2 = securityProvider.sha256Hash(data)

        // Then
        assertEquals(32, hash1.size, "Hash should be 256 bits (32 bytes)")
        assertEquals(32, hash2.size, "Hash should be 256 bits (32 bytes)")
        assertEquals(hash1.toList(), hash2.toList(), "Same input should produce same hash")
    }

    @Test
    fun `should detect device security status`() = runTest {
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
        assertTrue(isAvailable, "Mock device should have biometrics available")
    }

    @Test
    fun `should authenticate with biometrics successfully`() = runTest {
        // Given
        val title = "Authenticate"
        val subtitle = "Use your fingerprint"
        val description = "Please authenticate to access your financial data"

        // When
        val result = securityProvider.authenticateWithBiometrics(title, subtitle, description)

        // Then
        assertTrue(result.isSuccess, "Biometric authentication should succeed")
        assertTrue(result.getOrNull() == true, "Authentication should return true")
    }

    @Test
    fun `should handle different data sizes for encryption`() {
        // Given
        val key = securityProvider.generateKey()
        val smallData = "Hi".toByteArray()
        val largeData = "A".repeat(1000).toByteArray()

        // When
        val encryptSmallResult = securityProvider.encrypt(smallData, key)
        val encryptLargeResult = securityProvider.encrypt(largeData, key)

        // Then
        assertTrue(encryptSmallResult.isSuccess, "Should encrypt small data")
        assertTrue(encryptLargeResult.isSuccess, "Should encrypt large data")
        
        val decryptSmallResult = encryptSmallResult.getOrNull()?.let {
            securityProvider.decrypt(it, key)
        }
        val decryptLargeResult = encryptLargeResult.getOrNull()?.let {
            securityProvider.decrypt(it, key)
        }
        
        assertNotNull(decryptSmallResult, "Should decrypt small data")
        assertNotNull(decryptLargeResult, "Should decrypt large data")
        assertTrue(decryptSmallResult!!.isSuccess, "Small data decryption should succeed")
        assertTrue(decryptLargeResult!!.isSuccess, "Large data decryption should succeed")
    }
}