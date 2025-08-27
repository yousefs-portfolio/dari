package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.*
import platform.LocalAuthentication.*
import platform.Foundation.*

/**
 * iOS-specific biometric authentication implementation using Face ID/Touch ID
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
    LaunchedEffect(title) {
        val context = LAContext()
        
        // Check if biometric authentication is available
        val error = NSError()
        val canEvaluate = context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error
        )
        
        if (canEvaluate) {
            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = subtitle
            ) { success, authError ->
                if (success) {
                    onSuccess()
                } else {
                    authError?.let { error ->
                        when (error.code) {
                            LAErrorUserCancel -> onFailure()
                            LAErrorUserFallback -> onError("User selected fallback")
                            LAErrorSystemCancel -> onError("System cancelled authentication")
                            LAErrorPasscodeNotSet -> onError("Passcode not set")
                            LAErrorTouchIDNotAvailable -> onError("Touch ID not available")
                            LAErrorTouchIDNotEnrolled -> onError("Touch ID not enrolled")
                            LAErrorTouchIDLockout -> onError("Touch ID locked out")
                            else -> onError(error.localizedDescription)
                        }
                    } ?: onError("Unknown authentication error")
                }
            }
        } else {
            onError("Biometric authentication not available")
        }
    }
}

/**
 * Check biometric availability on iOS
 */
@Composable
actual fun checkBiometricAvailability(): BiometricAvailability {
    val context = LAContext()
    val error = NSError()
    
    return if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error)) {
        BiometricAvailability.AVAILABLE
    } else {
        when (error.code) {
            LAErrorBiometryNotAvailable -> BiometricAvailability.NO_HARDWARE
            LAErrorBiometryNotEnrolled -> BiometricAvailability.NONE_ENROLLED
            LAErrorBiometryLockout -> BiometricAvailability.HARDWARE_UNAVAILABLE
            else -> BiometricAvailability.UNKNOWN
        }
    }
}

/**
 * iOS-specific biometric manager
 */
class IOSBiometricManager {
    private val context = LAContext()
    
    fun getBiometryType(): BiometryType {
        return when (context.biometryType) {
            LABiometryTypeFaceID -> BiometryType.FACE_ID
            LABiometryTypeTouchID -> BiometryType.TOUCH_ID
            LABiometryTypeNone -> BiometryType.NONE
            else -> BiometryType.UNKNOWN
        }
    }
    
    fun authenticateWithFaceID(
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (context.biometryType == LABiometryTypeFaceID) {
            authenticate(reason, onSuccess, onError)
        } else {
            onError("Face ID not available")
        }
    }
    
    fun authenticateWithTouchID(
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (context.biometryType == LABiometryTypeTouchID) {
            authenticate(reason, onSuccess, onError)
        } else {
            onError("Touch ID not available")
        }
    }
    
    private fun authenticate(
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason
        ) { success, error ->
            if (success) {
                onSuccess()
            } else {
                error?.let { err ->
                    onError(err.localizedDescription)
                } ?: onError("Authentication failed")
            }
        }
    }
    
    fun authenticateWithPasscode(
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthentication,
            localizedReason = reason
        ) { success, error ->
            if (success) {
                onSuccess()
            } else {
                error?.let { err ->
                    onError(err.localizedDescription)
                } ?: onError("Authentication failed")
            }
        }
    }
}

/**
 * iOS biometry types
 */
enum class BiometryType {
    NONE,
    TOUCH_ID,
    FACE_ID,
    UNKNOWN
}

/**
 * iOS-specific Face ID prompt
 */
@Composable
fun FaceIDPrompt(
    reason: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val biometricManager = remember { IOSBiometricManager() }
    
    LaunchedEffect(reason) {
        if (biometricManager.getBiometryType() == BiometryType.FACE_ID) {
            biometricManager.authenticateWithFaceID(
                reason = reason,
                onSuccess = onSuccess,
                onError = onError
            )
        } else {
            onError("Face ID not available on this device")
        }
    }
}

/**
 * iOS-specific Touch ID prompt
 */
@Composable
fun TouchIDPrompt(
    reason: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val biometricManager = remember { IOSBiometricManager() }
    
    LaunchedEffect(reason) {
        if (biometricManager.getBiometryType() == BiometryType.TOUCH_ID) {
            biometricManager.authenticateWithTouchID(
                reason = reason,
                onSuccess = onSuccess,
                onError = onError
            )
        } else {
            onError("Touch ID not available on this device")
        }
    }
}

/**
 * Financial app specific iOS biometric authentication
 */
@Composable
fun IOSFinancialBiometricAuth(
    biometricType: FinancialBiometricType,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    val biometricManager = remember { IOSBiometricManager() }
    val deviceBiometryType = remember { biometricManager.getBiometryType() }
    
    val reason = when (biometricType) {
        FinancialBiometricType.APP_LOGIN -> when (deviceBiometryType) {
            BiometryType.FACE_ID -> "Use Face ID to unlock your financial app"
            BiometryType.TOUCH_ID -> "Use Touch ID to unlock your financial app"
            else -> "Authenticate to unlock your financial app"
        }
        FinancialBiometricType.TRANSACTION_APPROVAL -> when (deviceBiometryType) {
            BiometryType.FACE_ID -> "Use Face ID to approve this transaction"
            BiometryType.TOUCH_ID -> "Use Touch ID to approve this transaction"
            else -> "Authenticate to approve this transaction"
        }
        FinancialBiometricType.PAYMENT_AUTHORIZATION -> when (deviceBiometryType) {
            BiometryType.FACE_ID -> "Use Face ID to authorize payment"
            BiometryType.TOUCH_ID -> "Use Touch ID to authorize payment"
            else -> "Authenticate to authorize payment"
        }
        FinancialBiometricType.ACCOUNT_ACCESS -> when (deviceBiometryType) {
            BiometryType.FACE_ID -> "Use Face ID to access account details"
            BiometryType.TOUCH_ID -> "Use Touch ID to access account details"
            else -> "Authenticate to access account details"
        }
        FinancialBiometricType.SENSITIVE_DATA -> when (deviceBiometryType) {
            BiometryType.FACE_ID -> "Use Face ID to access sensitive information"
            BiometryType.TOUCH_ID -> "Use Touch ID to access sensitive information"
            else -> "Authenticate to access sensitive information"
        }
        FinancialBiometricType.SETTINGS_CHANGE -> when (deviceBiometryType) {
            BiometryType.FACE_ID -> "Use Face ID to change security settings"
            BiometryType.TOUCH_ID -> "Use Touch ID to change security settings"
            else -> "Authenticate to change security settings"
        }
    }
    
    LaunchedEffect(biometricType) {
        when (biometricManager.getBiometryType()) {
            BiometryType.FACE_ID -> {
                biometricManager.authenticateWithFaceID(
                    reason = reason,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            BiometryType.TOUCH_ID -> {
                biometricManager.authenticateWithTouchID(
                    reason = reason,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            BiometryType.NONE -> {
                // Fallback to passcode authentication
                biometricManager.authenticateWithPasscode(
                    reason = reason,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            BiometryType.UNKNOWN -> {
                onError("Biometric authentication not available")
            }
        }
    }
}

/**
 * Check specific biometry type available
 */
@Composable
fun isFaceIDAvailable(): Boolean {
    val biometricManager = remember { IOSBiometricManager() }
    return biometricManager.getBiometryType() == BiometryType.FACE_ID
}

@Composable
fun isTouchIDAvailable(): Boolean {
    val biometricManager = remember { IOSBiometricManager() }
    return biometricManager.getBiometryType() == BiometryType.TOUCH_ID
}

/**
 * Get biometric type display name
 */
fun BiometryType.displayName(): String {
    return when (this) {
        BiometryType.FACE_ID -> "Face ID"
        BiometryType.TOUCH_ID -> "Touch ID"
        BiometryType.NONE -> "Passcode"
        BiometryType.UNKNOWN -> "Biometric"
    }
}