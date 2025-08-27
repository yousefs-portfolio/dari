package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic biometric authentication interface
 */
@Composable
expect fun BiometricAuthenticationPrompt(
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailure: () -> Unit
)

/**
 * Check biometric availability
 */
@Composable
expect fun checkBiometricAvailability(): BiometricAvailability

/**
 * Biometric availability status
 */
enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

/**
 * Financial app biometric authentication types
 */
enum class FinancialBiometricType {
    APP_LOGIN,
    TRANSACTION_APPROVAL,
    PAYMENT_AUTHORIZATION,
    ACCOUNT_ACCESS,
    SENSITIVE_DATA,
    SETTINGS_CHANGE
}

/**
 * Common biometric authentication configuration
 */
data class BiometricConfig(
    val title: String,
    val subtitle: String,
    val negativeButtonText: String = "Cancel",
    val allowDeviceCredentials: Boolean = true,
    val requireStrongAuthentication: Boolean = true
)

/**
 * Financial app specific biometric configurations
 */
object FinancialBiometricConfigs {
    val APP_LOGIN = BiometricConfig(
        title = "Unlock Finance App",
        subtitle = "Use your biometric to access your financial data",
        allowDeviceCredentials = true
    )
    
    val TRANSACTION_AUTH = BiometricConfig(
        title = "Authorize Transaction",
        subtitle = "Use your biometric to confirm this transaction",
        allowDeviceCredentials = false,
        requireStrongAuthentication = true
    )
    
    val PAYMENT_AUTH = BiometricConfig(
        title = "Authorize Payment",
        subtitle = "Use your biometric to confirm payment",
        allowDeviceCredentials = false,
        requireStrongAuthentication = true
    )
    
    val ACCOUNT_ACCESS = BiometricConfig(
        title = "Access Account",
        subtitle = "Use your biometric to view account details",
        allowDeviceCredentials = true
    )
    
    val SETTINGS_ACCESS = BiometricConfig(
        title = "Security Settings",
        subtitle = "Use your biometric to access security settings",
        allowDeviceCredentials = false,
        requireStrongAuthentication = true
    )
}

/**
 * Helper function to get availability description
 */
fun BiometricAvailability.getDescription(): String {
    return when (this) {
        BiometricAvailability.AVAILABLE -> "Biometric authentication is available"
        BiometricAvailability.NO_HARDWARE -> "No biometric hardware available"
        BiometricAvailability.HARDWARE_UNAVAILABLE -> "Biometric hardware is currently unavailable"
        BiometricAvailability.NONE_ENROLLED -> "No biometrics enrolled. Please set up biometric authentication"
        BiometricAvailability.SECURITY_UPDATE_REQUIRED -> "Security update required for biometric authentication"
        BiometricAvailability.UNSUPPORTED -> "Biometric authentication is not supported"
        BiometricAvailability.UNKNOWN -> "Biometric authentication status unknown"
    }
}

/**
 * Helper function to check if biometric is usable
 */
fun BiometricAvailability.isUsable(): Boolean {
    return this == BiometricAvailability.AVAILABLE
}