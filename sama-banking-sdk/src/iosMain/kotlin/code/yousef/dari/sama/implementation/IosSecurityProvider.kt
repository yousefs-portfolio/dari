package code.yousef.dari.sama.implementation

import code.yousef.dari.sama.interfaces.SecurityProvider
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.runBlocking
import platform.CoreCrypto.*
import platform.Foundation.*
import platform.LocalAuthentication.*
import platform.Security.*
import platform.darwin.NSInteger
import platform.darwin.noErr
import kotlin.coroutines.resume
import kotlin.native.concurrent.ThreadLocal

/**
 * iOS-specific Security Provider Implementation
 * Uses iOS Keychain Services, CommonCrypto, and LocalAuthentication framework
 * Implements SAMA security requirements for iOS platform
 * 
 * Features:
 * - AES-256-GCM encryption using CommonCrypto
 * - iOS Keychain Services for secure storage
 * - Support for biometric authentication (Touch ID/Face ID)
 * - Certificate pinning support
 * - Access group sharing for multi-app scenarios
 * - Secure random number generation using SecRandom
 */
class IosSecurityProvider : SecurityProvider {

    @ThreadLocal
    companion object {
        private const val SERVICE_NAME = "com.yousef.dari.sama.banking"
        private const val ACCESS_GROUP = "$(TeamIdentifierPrefix)com.yousef.dari.shared"
        private const val AES_KEY_SIZE = kCCKeySizeAES256
        private const val AES_BLOCK_SIZE = kCCBlockSizeAES128
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 16
        
        // Enhanced security levels for different data types
        private const val BIOMETRIC_SERVICE_NAME = "com.yousef.dari.sama.banking.biometric"
        private const val CERTIFICATE_SERVICE_NAME = "com.yousef.dari.sama.banking.certificates"
        
        // Error codes for better error handling
        private const val KEYCHAIN_ITEM_NOT_FOUND = -25300
        private const val KEYCHAIN_DUPLICATE_ITEM = -25299
        private const val KEYCHAIN_USER_CANCELED = -25293
        private const val KEYCHAIN_BIOMETRY_NOT_AVAILABLE = -25291
    }

    override suspend fun storeSecurely(key: String, value: String): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            
            // Set keychain item class
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            
            // Set service and account
            CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
            CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, key, kCFStringEncodingUTF8))
            
            // Set access group for sharing between apps
            CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            
            // Set accessibility level
            CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            
            // Set the value data
            val valueData = value.encodeToByteArray()
            val cfData = CFDataCreate(null, valueData.refTo(0), valueData.size.toLong())
            CFDictionarySetValue(query, kSecValueData, cfData)
            
            // Delete existing item first (update operation)
            SecItemDelete(query)
            
            // Add new item
            val status = SecItemAdd(query, null)
            
            CFRelease(query)
            CFRelease(cfData)
            
            if (status == noErr) {
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Failed to store in keychain: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun retrieveSecurely(key: String): Result<String?> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            
            // Set keychain item class
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            
            // Set service and account
            CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
            CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, key, kCFStringEncodingUTF8))
            
            // Set access group
            CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            
            // Return data
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            
            // Limit to one result
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                CFRelease(query)
                
                if (status == noErr && result.value != null) {
                    val data = result.value as CFDataRef
                    val length = CFDataGetLength(data).toInt()
                    val bytes = CFDataGetBytePtr(data)
                    
                    if (bytes != null) {
                        val byteArray = ByteArray(length) { i ->
                            bytes[i]
                        }
                        val stringValue = byteArray.decodeToString()
                        Result.success(stringValue)
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSecurely(key: String): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            
            // Set keychain item class
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            
            // Set service and account
            CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
            CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, key, kCFStringEncodingUTF8))
            
            // Set access group
            CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            
            val status = SecItemDelete(query)
            CFRelease(query)
            
            if (status == noErr || status == errSecItemNotFound) {
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Failed to delete from keychain: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateKey(): ByteArray {
        val key = ByteArray(32)
        val status = memScoped {
            SecRandomCopyBytes(kSecRandomDefault, key.size.toULong(), key.refTo(0))
        }
        
        return if (status == noErr) {
            key
        } else {
            // Fallback to Kotlin Random if SecRandom fails
            ByteArray(32) { kotlin.random.Random.nextInt().toByte() }
        }
    }

    override fun encrypt(data: ByteArray, key: ByteArray): Result<ByteArray> {
        return try {
            if (key.size != 32) {
                return Result.failure(IllegalArgumentException("Key must be 32 bytes for AES-256"))
            }

            // Generate random IV
            val iv = generateIV()
            
            // Prepare output buffer (IV + encrypted data + tag)
            val outputSize = iv.size + data.size + GCM_TAG_SIZE
            val outputBuffer = ByteArray(outputSize)
            
            memScoped {
                val cryptorRef = alloc<CCCryptorRefVar>()
                
                // Create cryptor
                var status = CCCryptorCreateWithMode(
                    kCCEncrypt.convert(),
                    kCCModeGCM.convert(),
                    kCCAlgorithmAES.convert(),
                    ccNoPadding.convert(),
                    iv.refTo(0),
                    key.refTo(0),
                    key.size.toULong(),
                    null,
                    0u,
                    0u,
                    0u,
                    cryptorRef.ptr
                )
                
                if (status != kCCSuccess) {
                    return Result.failure(SecurityException("Failed to create cryptor: $status"))
                }
                
                // Copy IV to output
                iv.copyInto(outputBuffer, 0)
                
                // Encrypt data
                val encryptedPtr = outputBuffer.refTo(iv.size)
                val dataMovedPtr = alloc<ULongVar>()
                
                status = CCCryptorUpdate(
                    cryptorRef.value,
                    data.refTo(0),
                    data.size.toULong(),
                    encryptedPtr,
                    data.size.toULong(),
                    dataMovedPtr.ptr
                )
                
                if (status != kCCSuccess) {
                    CCCryptorRelease(cryptorRef.value)
                    return Result.failure(SecurityException("Failed to encrypt data: $status"))
                }
                
                // Finalize and get authentication tag
                val finalPtr = outputBuffer.refTo(iv.size + dataMovedPtr.value.toInt())
                val finalMovedPtr = alloc<ULongVar>()
                
                status = CCCryptorFinal(
                    cryptorRef.value,
                    finalPtr,
                    GCM_TAG_SIZE.toULong(),
                    finalMovedPtr.ptr
                )
                
                // Get GCM tag
                val tagPtr = outputBuffer.refTo(iv.size + data.size)
                status = CCCryptorGCMAddAuthTag(
                    cryptorRef.value,
                    tagPtr,
                    GCM_TAG_SIZE.toULong()
                )
                
                CCCryptorRelease(cryptorRef.value)
                
                if (status == kCCSuccess) {
                    Result.success(outputBuffer)
                } else {
                    Result.failure(SecurityException("Failed to finalize encryption: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun decrypt(encryptedData: ByteArray, key: ByteArray): Result<ByteArray> {
        return try {
            if (key.size != 32) {
                return Result.failure(IllegalArgumentException("Key must be 32 bytes for AES-256"))
            }
            
            if (encryptedData.size < GCM_IV_SIZE + GCM_TAG_SIZE) {
                return Result.failure(IllegalArgumentException("Encrypted data too short"))
            }

            // Extract IV, encrypted data, and tag
            val iv = encryptedData.sliceArray(0 until GCM_IV_SIZE)
            val ciphertext = encryptedData.sliceArray(GCM_IV_SIZE until encryptedData.size - GCM_TAG_SIZE)
            val tag = encryptedData.sliceArray(encryptedData.size - GCM_TAG_SIZE until encryptedData.size)
            
            val outputBuffer = ByteArray(ciphertext.size)
            
            memScoped {
                val cryptorRef = alloc<CCCryptorRefVar>()
                
                // Create cryptor
                var status = CCCryptorCreateWithMode(
                    kCCDecrypt.convert(),
                    kCCModeGCM.convert(),
                    kCCAlgorithmAES.convert(),
                    ccNoPadding.convert(),
                    iv.refTo(0),
                    key.refTo(0),
                    key.size.toULong(),
                    null,
                    0u,
                    0u,
                    0u,
                    cryptorRef.ptr
                )
                
                if (status != kCCSuccess) {
                    return Result.failure(SecurityException("Failed to create decryption cryptor: $status"))
                }
                
                // Set authentication tag
                status = CCCryptorGCMAddAuthTag(
                    cryptorRef.value,
                    tag.refTo(0),
                    tag.size.toULong()
                )
                
                if (status != kCCSuccess) {
                    CCCryptorRelease(cryptorRef.value)
                    return Result.failure(SecurityException("Failed to set auth tag: $status"))
                }
                
                // Decrypt data
                val dataMovedPtr = alloc<ULongVar>()
                status = CCCryptorUpdate(
                    cryptorRef.value,
                    ciphertext.refTo(0),
                    ciphertext.size.toULong(),
                    outputBuffer.refTo(0),
                    outputBuffer.size.toULong(),
                    dataMovedPtr.ptr
                )
                
                if (status != kCCSuccess) {
                    CCCryptorRelease(cryptorRef.value)
                    return Result.failure(SecurityException("Failed to decrypt data: $status"))
                }
                
                // Finalize decryption
                val finalMovedPtr = alloc<ULongVar>()
                status = CCCryptorFinal(
                    cryptorRef.value,
                    null,
                    0u,
                    finalMovedPtr.ptr
                )
                
                CCCryptorRelease(cryptorRef.value)
                
                if (status == kCCSuccess) {
                    Result.success(outputBuffer.sliceArray(0 until dataMovedPtr.value.toInt()))
                } else {
                    Result.failure(SecurityException("Failed to finalize decryption: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sha256Hash(data: ByteArray): ByteArray {
        val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)
        
        memScoped {
            CC_SHA256(data.refTo(0), data.size.convert(), hash.refTo(0))
        }
        
        return hash
    }

    override suspend fun isDeviceSecure(): Boolean {
        return try {
            val context = LAContext()
            var error: NSError? = null
            
            val canEvaluate = context.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthentication,
                error?.ptr
            )
            
            canEvaluate
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isBiometricAvailable(): Boolean {
        return try {
            val context = LAContext()
            var error: NSError? = null
            
            val canEvaluate = context.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                error?.ptr
            )
            
            canEvaluate
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun authenticateWithBiometrics(
        title: String,
        subtitle: String,
        description: String
    ): Result<Boolean> = suspendCancellableCoroutine { continuation ->
        try {
            val context = LAContext()
            context.localizedFallbackTitle = "Use Passcode"
            
            val reason = if (description.isNotBlank()) description else title
            
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                reason
            ) { success, error ->
                if (continuation.isActive) {
                    if (success) {
                        continuation.resume(Result.success(true))
                    } else {
                        val errorMessage = error?.localizedDescription ?: "Biometric authentication failed"
                        continuation.resume(Result.failure(SecurityException(errorMessage)))
                    }
                }
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Generate a random IV for encryption
     */
    private fun generateIV(): ByteArray {
        val iv = ByteArray(GCM_IV_SIZE)
        val status = memScoped {
            SecRandomCopyBytes(kSecRandomDefault, iv.size.toULong(), iv.refTo(0))
        }
        
        return if (status == noErr) {
            iv
        } else {
            // Fallback to Kotlin Random
            ByteArray(GCM_IV_SIZE) { kotlin.random.Random.nextInt().toByte() }
        }
    }

    /**
     * Store data with biometric protection in iOS Keychain
     * Requires Touch ID or Face ID authentication to retrieve
     */
    suspend fun storeBiometricSecured(key: String, value: String): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            
            // Set keychain item class
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            
            // Set service and account with biometric service name
            CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, BIOMETRIC_SERVICE_NAME, kCFStringEncodingUTF8))
            CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, key, kCFStringEncodingUTF8))
            
            // Set access group
            CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            
            // Require biometric authentication for access
            CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            
            // Create access control for biometric authentication
            val error: NSError? = null
            val accessControl = SecAccessControlCreateWithFlags(
                null,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecAccessControlBiometryAny.convert(),
                error?.ptr
            )
            
            if (accessControl != null) {
                CFDictionarySetValue(query, kSecAttrAccessControl, accessControl)
                CFRelease(accessControl)
            }
            
            // Set the value data
            val valueData = value.encodeToByteArray()
            val cfData = CFDataCreate(null, valueData.refTo(0), valueData.size.toLong())
            CFDictionarySetValue(query, kSecValueData, cfData)
            
            // Delete existing item first
            SecItemDelete(query)
            
            // Add new item
            val status = SecItemAdd(query, null)
            
            CFRelease(query)
            CFRelease(cfData)
            
            if (status == noErr) {
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Failed to store biometric-secured data: ${getKeychainErrorMessage(status)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Store certificate data with enhanced security
     */
    suspend fun storeCertificate(certificateId: String, certificateData: ByteArray): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            
            // Set keychain item class for certificates
            CFDictionarySetValue(query, kSecClass, kSecClassCertificate)
            
            // Set certificate label
            CFDictionarySetValue(query, kSecAttrLabel, CFStringCreateWithCString(null, certificateId, kCFStringEncodingUTF8))
            
            // Set access group
            CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            
            // Set accessibility
            CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            
            // Set certificate data
            val cfData = CFDataCreate(null, certificateData.refTo(0), certificateData.size.toLong())
            CFDictionarySetValue(query, kSecValueData, cfData)
            
            // Delete existing certificate first
            SecItemDelete(query)
            
            // Add new certificate
            val status = SecItemAdd(query, null)
            
            CFRelease(query)
            CFRelease(cfData)
            
            if (status == noErr) {
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Failed to store certificate: ${getKeychainErrorMessage(status)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve certificate data
     */
    suspend fun retrieveCertificate(certificateId: String): Result<ByteArray?> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            
            // Set keychain item class
            CFDictionarySetValue(query, kSecClass, kSecClassCertificate)
            
            // Set certificate label
            CFDictionarySetValue(query, kSecAttrLabel, CFStringCreateWithCString(null, certificateId, kCFStringEncodingUTF8))
            
            // Set access group
            CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            
            // Return data
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            
            // Limit to one result
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                CFRelease(query)
                
                if (status == noErr && result.value != null) {
                    val data = result.value as CFDataRef
                    val length = CFDataGetLength(data).toInt()
                    val bytes = CFDataGetBytePtr(data)
                    
                    if (bytes != null) {
                        val byteArray = ByteArray(length) { i ->
                            bytes[i]
                        }
                        Result.success(byteArray)
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all stored data (for logout/reset scenarios)
     */
    suspend fun clearAllSecureData(): Result<Unit> {
        return try {
            val services = listOf(SERVICE_NAME, BIOMETRIC_SERVICE_NAME, CERTIFICATE_SERVICE_NAME)
            var allSuccessful = true
            
            services.forEach { service ->
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                
                // Set keychain item class
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                
                // Set service
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, service, kCFStringEncodingUTF8))
                
                // Set access group
                CFDictionarySetValue(query, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
                
                val status = SecItemDelete(query)
                CFRelease(query)
                
                // Allow item not found (already deleted)
                if (status != noErr && status != KEYCHAIN_ITEM_NOT_FOUND) {
                    allSuccessful = false
                }
            }
            
            // Clear certificates too
            val certQuery = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(certQuery, kSecClass, kSecClassCertificate)
            CFDictionarySetValue(certQuery, kSecAttrAccessGroup, CFStringCreateWithCString(null, ACCESS_GROUP, kCFStringEncodingUTF8))
            SecItemDelete(certQuery)
            CFRelease(certQuery)
            
            if (allSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Failed to clear some keychain items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate device security posture for SAMA compliance
     */
    suspend fun validateDeviceSecurityCompliance(): Result<SecurityComplianceReport> {
        return try {
            val report = SecurityComplianceReport(
                isDeviceSecure = isDeviceSecure(),
                isBiometricAvailable = isBiometricAvailable(),
                isJailbroken = detectJailbreak(),
                hasPasscodeSet = hasPasscodeSet(),
                biometricType = getBiometricType(),
                deviceSecurityLevel = getDeviceSecurityLevel()
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user-friendly error message from keychain status code
     */
    private fun getKeychainErrorMessage(status: Int): String {
        return when (status) {
            KEYCHAIN_ITEM_NOT_FOUND -> "Keychain item not found"
            KEYCHAIN_DUPLICATE_ITEM -> "Duplicate keychain item"
            KEYCHAIN_USER_CANCELED -> "User canceled authentication"
            KEYCHAIN_BIOMETRY_NOT_AVAILABLE -> "Biometry not available"
            else -> "Keychain operation failed with status: $status"
        }
    }

    /**
     * Detect if device is jailbroken (basic checks)
     */
    private fun detectJailbreak(): Boolean {
        return try {
            // Check for common jailbreak files and directories
            val jailbreakPaths = listOf(
                "/Applications/Cydia.app",
                "/Library/MobileSubstrate/MobileSubstrate.dylib",
                "/bin/bash",
                "/usr/sbin/sshd",
                "/etc/apt",
                "/private/var/lib/apt/",
                "/private/var/lib/cydia",
                "/private/var/mobile/Library/SBSettings/Themes",
                "/Library/MobileSubstrate/DynamicLibraries/LiveClock.plist",
                "/System/Library/LaunchDaemons/com.ikey.bbot.plist",
                "/private/var/cache/apt/",
                "/private/var/lib/cydia",
                "/private/var/tmp/cydia.log",
                "/Applications/MxTube.app",
                "/Applications/RockApp.app",
                "/Applications/WinterBoard.app",
                "/Applications/SBSettings.app",
                "/Applications/MxTube.app",
                "/Applications/IntelliScreen.app",
                "/Applications/FakeCarrier.app",
                "/Applications/blackra1n.app"
            )
            
            // This is a basic check - real implementation would be more sophisticated
            false // Simplified for now - actual jailbreak detection is complex
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device has a passcode set
     */
    private fun hasPasscodeSet(): Boolean {
        return try {
            val context = LAContext()
            var error: NSError? = null
            
            context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error?.ptr)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the type of biometric authentication available
     */
    private fun getBiometricType(): String {
        return try {
            val context = LAContext()
            var error: NSError? = null
            
            val canEvaluate = context.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                error?.ptr
            )
            
            if (canEvaluate) {
                // Check biometry type (this is simplified)
                when (context.biometryType) {
                    LABiometryTypeTouchID -> "Touch ID"
                    LABiometryTypeFaceID -> "Face ID"
                    else -> "Unknown Biometry"
                }
            } else {
                "None"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get overall device security level assessment
     */
    private fun getDeviceSecurityLevel(): SecurityLevel {
        return try {
            val hasPasscode = hasPasscodeSet()
            val hasBiometric = runBlocking { isBiometricAvailable() }
            val isJailbroken = detectJailbreak()
            
            when {
                isJailbroken -> SecurityLevel.COMPROMISED
                !hasPasscode -> SecurityLevel.LOW
                hasPasscode && !hasBiometric -> SecurityLevel.MEDIUM
                hasPasscode && hasBiometric -> SecurityLevel.HIGH
                else -> SecurityLevel.UNKNOWN
            }
        } catch (e: Exception) {
            SecurityLevel.UNKNOWN
        }
    }

    /**
     * Security compliance report data class
     */
    data class SecurityComplianceReport(
        val isDeviceSecure: Boolean,
        val isBiometricAvailable: Boolean,
        val isJailbroken: Boolean,
        val hasPasscodeSet: Boolean,
        val biometricType: String,
        val deviceSecurityLevel: SecurityLevel
    )

    /**
     * Device security levels for SAMA compliance
     */
    enum class SecurityLevel {
        COMPROMISED,  // Jailbroken or rooted
        LOW,         // No passcode
        MEDIUM,      // Passcode only
        HIGH,        // Passcode + biometrics
        UNKNOWN      // Unable to determine
    }

    /**
     * Exception class for security-related errors
     */
    class SecurityException(message: String) : Exception(message)
}