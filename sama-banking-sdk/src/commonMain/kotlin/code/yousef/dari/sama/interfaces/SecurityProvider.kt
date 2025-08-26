package code.yousef.dari.sama.interfaces

/**
 * Security Provider interface for platform-specific security implementations
 * Handles encryption, secure storage, and certificate management
 */
interface SecurityProvider {
    
    /**
     * Generate a new encryption key for data protection
     * @param keyAlias Unique identifier for the key
     * @return Success/failure result
     */
    suspend fun generateEncryptionKey(keyAlias: String): Result<Unit>
    
    /**
     * Encrypt sensitive data using platform-specific secure encryption
     * @param data Data to encrypt
     * @param keyAlias Key identifier for encryption
     * @return Encrypted data as Base64 string
     */
    suspend fun encryptData(data: String, keyAlias: String): Result<String>
    
    /**
     * Decrypt previously encrypted data
     * @param encryptedData Encrypted data as Base64 string
     * @param keyAlias Key identifier used for encryption
     * @return Decrypted plain text data
     */
    suspend fun decryptData(encryptedData: String, keyAlias: String): Result<String>
    
    /**
     * Store sensitive data in platform-specific secure storage
     * Android: EncryptedSharedPreferences with Keystore
     * iOS: Keychain Services
     * @param key Storage key identifier
     * @param value Sensitive value to store
     * @return Success/failure result
     */
    suspend fun storeSecurely(key: String, value: String): Result<Unit>
    
    /**
     * Retrieve sensitive data from secure storage
     * @param key Storage key identifier
     * @return Retrieved value or null if not found
     */
    suspend fun retrieveSecurely(key: String): Result<String?>
    
    /**
     * Delete sensitive data from secure storage
     * @param key Storage key identifier
     * @return Success/failure result
     */
    suspend fun deleteSecurely(key: String): Result<Unit>
    
    /**
     * Check if biometric authentication is available on device
     * @return true if biometric authentication is supported
     */
    fun isBiometricAvailable(): Boolean
    
    /**
     * Prompt for biometric authentication
     * @param title Dialog title
     * @param subtitle Dialog subtitle
     * @param description Dialog description
     * @return Success/failure result of authentication
     */
    suspend fun authenticateWithBiometric(
        title: String,
        subtitle: String,
        description: String
    ): Result<Unit>
    
    /**
     * Validate SSL certificate against pinned certificates
     * @param hostname Server hostname
     * @param certificateChain Server certificate chain
     * @return true if certificate is valid and pinned
     */
    suspend fun validateCertificate(
        hostname: String,
        certificateChain: List<String>
    ): Boolean
    
    /**
     * Generate cryptographically secure random string
     * @param length Length of random string
     * @return Secure random string
     */
    fun generateSecureRandom(length: Int): String
    
    /**
     * Create SHA256 hash of input string
     * @param input String to hash
     * @return SHA256 hash as hex string
     */
    fun createSha256Hash(input: String): String
    
    /**
     * Create HMAC-SHA256 signature
     * @param data Data to sign
     * @param secret Secret key for HMAC
     * @return HMAC signature as hex string
     */
    fun createHmacSignature(data: String, secret: String): String
}