package code.yousef.dari.shared.ui.platform

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Android-specific biometric authentication implementation
 */
class AndroidBiometricManager(private val context: Context) {
    
    private val biometricManager = BiometricManager.from(context)
    
    fun canAuthenticate(): BiometricAvailability {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.UNKNOWN
        }
    }
    
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailure()
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    fun authenticateWithCrypto(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: (BiometricPrompt.CryptoObject?) -> Unit,
        onError: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result.cryptoObject)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailure()
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }
}

/**
 * Composable function for Android biometric authentication
 */
@Composable
actual fun BiometricAuthenticationPrompt(
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailure: () -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { AndroidBiometricManager(context) }
    
    LaunchedEffect(title) {
        val activity = context as? FragmentActivity
        if (activity != null) {
            when (biometricManager.canAuthenticate()) {
                BiometricAvailability.AVAILABLE -> {
                    biometricManager.authenticate(
                        activity = activity,
                        title = title,
                        subtitle = subtitle,
                        negativeButtonText = negativeButtonText,
                        onSuccess = onSuccess,
                        onError = onError,
                        onFailure = onFailure
                    )
                }
                else -> {
                    onError("Biometric authentication not available")
                }
            }
        } else {
            onError("Activity context required for biometric authentication")
        }
    }
}

/**
 * Check biometric availability
 */
@Composable
actual fun checkBiometricAvailability(): BiometricAvailability {
    val context = LocalContext.current
    val biometricManager = remember { AndroidBiometricManager(context) }
    
    return biometricManager.canAuthenticate()
}

/**
 * Financial app specific biometric prompts
 */
@Composable
fun FinancialBiometricPrompt(
    promptType: FinancialBiometricType,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    val (title, subtitle) = when (promptType) {
        FinancialBiometricType.APP_LOGIN -> "Unlock Dari Finance" to "Use your biometric to access your financial data"
        FinancialBiometricType.TRANSACTION_APPROVAL -> "Approve Transaction" to "Use your biometric to authorize this transaction"
        FinancialBiometricType.PAYMENT_AUTHORIZATION -> "Authorize Payment" to "Use your biometric to confirm payment"
        FinancialBiometricType.ACCOUNT_ACCESS -> "Access Account" to "Use your biometric to view account details"
        FinancialBiometricType.SENSITIVE_DATA -> "Secure Access" to "Use your biometric to access sensitive information"
        FinancialBiometricType.SETTINGS_CHANGE -> "Confirm Changes" to "Use your biometric to save security settings"
    }
    
    BiometricAuthenticationPrompt(
        title = title,
        subtitle = subtitle,
        negativeButtonText = "Cancel",
        onSuccess = onSuccess,
        onError = onError,
        onFailure = onCancel
    )
}

/**
 * Banking-specific biometric authentication with encryption
 */
@Composable
fun BankingBiometricAuth(
    requireEncryption: Boolean = true,
    onAuthenticationSuccess: (encrypted: Boolean) -> Unit,
    onAuthenticationError: (String) -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { AndroidBiometricManager(context) }
    
    LaunchedEffect(requireEncryption) {
        val activity = context as? FragmentActivity
        if (activity != null && biometricManager.canAuthenticate() == BiometricAvailability.AVAILABLE) {
            if (requireEncryption) {
                // TODO: Implement crypto object for encryption
                // This would require setting up KeyStore and creating a CryptoObject
                onAuthenticationSuccess(false) // Placeholder for now
            } else {
                biometricManager.authenticate(
                    activity = activity,
                    title = "Banking Authentication",
                    subtitle = "Authenticate to access your banking features",
                    negativeButtonText = "Use PIN",
                    onSuccess = { onAuthenticationSuccess(false) },
                    onError = onAuthenticationError,
                    onFailure = { onAuthenticationError("Authentication failed") }
                )
            }
        } else {
            onAuthenticationError("Biometric authentication not available")
        }
    }
}

/**
 * Quick authentication for repeated actions
 */
@Composable
fun QuickBiometricAuth(
    actionDescription: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    BiometricAuthenticationPrompt(
        title = "Quick Authentication",
        subtitle = actionDescription,
        negativeButtonText = "Cancel",
        onSuccess = onSuccess,
        onError = { onCancel() },
        onFailure = onCancel
    )
}

/**
 * Transaction signing with biometric
 */
@Composable
fun TransactionSigningBiometric(
    transactionAmount: String,
    recipient: String,
    onSigned: () -> Unit,
    onCancelled: () -> Unit
) {
    BiometricAuthenticationPrompt(
        title = "Sign Transaction",
        subtitle = "Authorize payment of $transactionAmount to $recipient",
        negativeButtonText = "Cancel",
        onSuccess = onSigned,
        onError = { onCancelled() },
        onFailure = onCancelled
    )
}

/**
 * Account setup biometric enrollment
 */
@Composable
fun BiometricEnrollmentPrompt(
    onEnrolled: () -> Unit,
    onSkipped: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { AndroidBiometricManager(context) }
    
    LaunchedEffect(Unit) {
        when (biometricManager.canAuthenticate()) {
            BiometricAvailability.AVAILABLE -> {
                // Already enrolled, proceed
                onEnrolled()
            }
            BiometricAvailability.NONE_ENROLLED -> {
                // Guide user to enrollment settings
                onError("Please set up biometric authentication in your device settings")
            }
            BiometricAvailability.NO_HARDWARE -> {
                onError("Biometric hardware not available")
            }
            else -> {
                onError("Biometric authentication not supported")
            }
        }
    }
}