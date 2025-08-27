package com.dari.finance.camera

/**
 * Camera permission handler interface for managing camera access permissions
 * Provides platform-agnostic permission management
 */
interface CameraPermissionHandler {
    
    /**
     * Checks current camera permission status
     * @return Current permission status
     */
    fun checkCameraPermission(): CameraPermissionStatus
    
    /**
     * Requests camera permission from user
     * @param callback Callback invoked with permission result
     */
    fun requestCameraPermission(callback: (CameraPermissionStatus) -> Unit)
    
    /**
     * Checks if permission rationale should be shown to user
     * @return True if rationale should be shown, false otherwise
     */
    fun shouldShowPermissionRationale(): Boolean
    
    /**
     * Opens app settings for manual permission management
     */
    fun openAppSettings()
}

/**
 * Camera permission status enumeration
 */
enum class CameraPermissionStatus {
    /** Permission is granted and camera can be used */
    GRANTED,
    
    /** Permission is denied but can be requested again */
    DENIED,
    
    /** Permission is permanently denied, user must grant via settings */
    PERMANENTLY_DENIED,
    
    /** Permission status is unknown or not determined */
    NOT_DETERMINED
}

/**
 * Camera permission request result
 */
data class PermissionResult(
    val status: CameraPermissionStatus,
    val canRequest: Boolean,
    val shouldShowRationale: Boolean
)

/**
 * Camera permission manager utility
 */
class CameraPermissionManager(
    private val permissionHandler: CameraPermissionHandler
) {
    
    /**
     * Comprehensive permission check with detailed result
     */
    fun checkPermissionWithDetails(): PermissionResult {
        val status = permissionHandler.checkCameraPermission()
        
        return PermissionResult(
            status = status,
            canRequest = status != CameraPermissionStatus.PERMANENTLY_DENIED,
            shouldShowRationale = permissionHandler.shouldShowPermissionRationale()
        )
    }
    
    /**
     * Handles complete permission flow with user guidance
     */
    suspend fun requestPermissionWithFlow(
        onRationaleNeeded: () -> Unit = {},
        onPermissionDenied: () -> Unit = {},
        onPermissionGranted: () -> Unit = {},
        onSettingsNeeded: () -> Unit = {}
    ) {
        val currentStatus = permissionHandler.checkCameraPermission()
        
        when (currentStatus) {
            CameraPermissionStatus.GRANTED -> {
                onPermissionGranted()
                return
            }
            
            CameraPermissionStatus.PERMANENTLY_DENIED -> {
                onSettingsNeeded()
                return
            }
            
            CameraPermissionStatus.DENIED,
            CameraPermissionStatus.NOT_DETERMINED -> {
                if (permissionHandler.shouldShowPermissionRationale()) {
                    onRationaleNeeded()
                }
                
                requestPermissionInternal(
                    onPermissionGranted,
                    onPermissionDenied,
                    onSettingsNeeded
                )
            }
        }
    }
    
    private fun requestPermissionInternal(
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onSettingsNeeded: () -> Unit
    ) {
        permissionHandler.requestCameraPermission { status ->
            when (status) {
                CameraPermissionStatus.GRANTED -> onGranted()
                CameraPermissionStatus.PERMANENTLY_DENIED -> onSettingsNeeded()
                else -> onDenied()
            }
        }
    }
}

/**
 * Permission rationale messages
 */
object CameraPermissionMessages {
    
    const val PERMISSION_RATIONALE_TITLE = "Camera Permission Required"
    
    const val PERMISSION_RATIONALE_MESSAGE = 
        "Camera access is needed to scan receipts and automatically extract transaction details. " +
        "This helps you track your expenses more efficiently."
    
    const val PERMISSION_DENIED_MESSAGE = 
        "Camera permission is required for receipt scanning. You can enable it in app settings."
    
    const val PERMISSION_PERMANENTLY_DENIED_TITLE = "Enable Camera in Settings"
    
    const val PERMISSION_PERMANENTLY_DENIED_MESSAGE = 
        "To scan receipts, please enable camera permission in your device settings."
    
    // Arabic translations for Saudi market
    const val PERMISSION_RATIONALE_TITLE_AR = "مطلوب إذن الكاميرا"
    
    const val PERMISSION_RATIONALE_MESSAGE_AR = 
        "نحتاج للوصول إلى الكاميرا لمسح الفواتير واستخراج تفاصيل المعاملات تلقائياً. " +
        "هذا يساعدك في تتبع مصروفاتك بكفاءة أكبر."
    
    const val PERMISSION_DENIED_MESSAGE_AR = 
        "إذن الكاميرا مطلوب لمسح الفواتير. يمكنك تفعيله في إعدادات التطبيق."
    
    const val PERMISSION_PERMANENTLY_DENIED_MESSAGE_AR = 
        "لمسح الفواتير، يرجى تفعيل إذن الكاميرا في إعدادات الجهاز."
}