package code.yousef.dari.sama.implementation

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.MessageDigest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android-specific Security Provider Implementation
 * Uses Android Keystore, EncryptedSharedPreferences, and BiometricPrompt
 * Implements SAMA security requirements for Android platform
 */
class AndroidSecurityProvider(
    private val context: Context,
    private val activity: FragmentActivity? = null
) : code.yousef.dari.sama.interfaces.SecurityProvider {

    companion object {
        private const val KEYSTORE_ALIAS = "SamaSecurityKey"
        private const val SHARED_PREFS_NAME = "sama_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private val encryptedSharedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            SHARED_PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val biometricManager: BiometricManager by lazy {
        BiometricManager.from(context)
    }

    override suspend fun storeSecurely(key: String, value: String): Result<Unit> {
        return try {
            encryptedSharedPrefs.edit()
                .putString(key, value)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun retrieveSecurely(key: String): Result<String?> {
        return try {
            val value = encryptedSharedPrefs.getString(key, null)
            Result.success(value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSecurely(key: String): Result<Unit> {
        return try {
            encryptedSharedPrefs.edit()
                .remove(key)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    override suspend fun authenticateWithBiometric(
        title: String,
        subtitle: String,
        description: String
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val activity = this.activity
        if (activity == null) {
            continuation.resume(Result.failure(IllegalStateException("Activity required for biometric authentication")))
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Authentication error: $errString")))
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Authentication failed")))
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }

        continuation.invokeOnCancellation {
            try {
                biometricPrompt.cancelAuthentication()
            } catch (e: Exception) {
                // Ignore cancellation errors
            }
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        } else {
            createSecretKey()
        }
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set to true if you want to require auth for each use
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Initialize certificate pinning for HTTPS connections
     * This should be called during app initialization
     */
    fun initializeCertificatePinning(pinnedCertificates: Map<String, List<String>>) {
        // Implementation would go here for certificate pinning
        // This would typically involve configuring OkHttp with CertificatePinner
    }

    /**
     * Validate certificate against pinned certificates
     */
    override suspend fun validateCertificate(hostname: String, certificateChain: List<String>): Boolean {
        // Implementation would validate the certificate against pinned values
        return true // Placeholder
    }

    override suspend fun generateEncryptionKey(keyAlias: String): Result<Unit> {
        return try {
            createSecretKey()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun encryptData(data: String, keyAlias: String): Result<String> {
        return try {
            val cipher = Cipher.getInstance(AES_MODE)
            val secretKey = getOrCreateSecretKey()

            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray())

            // Prepend IV to encrypted data
            val result = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)

            Result.success(android.util.Base64.encodeToString(result, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun decryptData(encryptedData: String, keyAlias: String): Result<String> {
        return try {
            val encryptedBytes = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
            val cipher = Cipher.getInstance(AES_MODE)
            val secretKey = getOrCreateSecretKey()

            // Extract IV from encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val actualEncryptedData = ByteArray(encryptedBytes.size - GCM_IV_LENGTH)
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, actualEncryptedData, 0, actualEncryptedData.size)

            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decryptedData = cipher.doFinal(actualEncryptedData)

            Result.success(String(decryptedData))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateSecureRandom(length: Int): String {
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    override fun createSha256Hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override fun createHmacSignature(data: String, secret: String): String {
        val secretKeySpec = javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val signatureBytes = mac.doFinal(data.toByteArray())
        return signatureBytes.joinToString("") { "%02x".format(it) }
    }
}
