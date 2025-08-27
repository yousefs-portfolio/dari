package com.dari.finance.camera

/**
 * Camera preview interface for capturing receipt images
 * Provides platform-agnostic camera preview functionality
 */
interface CameraPreview {
    
    /**
     * Starts camera preview
     * @return Result indicating success or failure
     */
    fun startPreview(): CameraResult<Unit>
    
    /**
     * Stops camera preview
     */
    fun stopPreview()
    
    /**
     * Checks if camera preview is currently active
     * @return True if preview is active, false otherwise
     */
    fun isPreviewActive(): Boolean
    
    /**
     * Captures image from camera preview
     * @param callback Callback with captured image data or error
     * @return Result indicating if capture was initiated
     */
    fun captureImage(callback: (CameraResult<ByteArray>) -> Unit): CameraResult<Unit>
    
    /**
     * Toggles flashlight on/off
     * @param enabled True to turn on flashlight, false to turn off
     */
    fun toggleFlashlight(enabled: Boolean)
}

/**
 * Camera operation result sealed class
 */
sealed class CameraResult<out T> {
    data class Success<T>(val data: T) : CameraResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : CameraResult<Nothing>()
}

/**
 * Camera preview configuration
 */
data class CameraPreviewConfig(
    val resolution: CameraResolution = CameraResolution.HD_720P,
    val flashMode: FlashMode = FlashMode.AUTO,
    val focusMode: FocusMode = FocusMode.AUTO,
    val enableImageStabilization: Boolean = true,
    val captureFormat: ImageFormat = ImageFormat.JPEG,
    val jpegQuality: Int = 85 // 0-100
)

/**
 * Camera resolution options
 */
enum class CameraResolution(val width: Int, val height: Int) {
    LOW_480P(640, 480),
    SD_576P(720, 576),
    HD_720P(1280, 720),
    FULL_HD_1080P(1920, 1080),
    UHD_4K(3840, 2160);
    
    fun getAspectRatio(): Float = width.toFloat() / height.toFloat()
}

/**
 * Flash mode options
 */
enum class FlashMode {
    OFF,
    ON,
    AUTO,
    TORCH
}

/**
 * Focus mode options
 */
enum class FocusMode {
    AUTO,
    MANUAL,
    CONTINUOUS,
    MACRO
}

/**
 * Image format options
 */
enum class ImageFormat {
    JPEG,
    PNG,
    RAW
}

/**
 * Camera capabilities information
 */
data class CameraCapabilities(
    val supportedResolutions: List<CameraResolution>,
    val supportedFlashModes: List<FlashMode>,
    val supportedFocusModes: List<FocusMode>,
    val hasFlash: Boolean,
    val supportsAutoFocus: Boolean,
    val maxZoom: Float,
    val minFocusDistance: Float? = null,
    val supportedImageFormats: List<ImageFormat> = listOf(ImageFormat.JPEG)
)

/**
 * Camera state information
 */
data class CameraState(
    val isPreviewActive: Boolean,
    val isFlashlightOn: Boolean,
    val currentResolution: CameraResolution,
    val currentFlashMode: FlashMode,
    val currentFocusMode: FocusMode,
    val currentZoom: Float = 1.0f,
    val isCapturing: Boolean = false
)

/**
 * Camera preview listener for events
 */
interface CameraPreviewListener {
    fun onPreviewStarted()
    fun onPreviewStopped()
    fun onCaptureStarted()
    fun onCaptureCompleted(imageData: ByteArray)
    fun onCaptureFailed(error: String)
    fun onFocusChanged(focused: Boolean)
    fun onZoomChanged(zoom: Float)
    fun onError(error: String)
}

/**
 * Camera manager for handling preview operations
 */
abstract class CameraManager(
    protected val config: CameraPreviewConfig = CameraPreviewConfig()
) : CameraPreview {
    
    protected var listener: CameraPreviewListener? = null
    protected var capabilities: CameraCapabilities? = null
    protected var currentState = CameraState(
        isPreviewActive = false,
        isFlashlightOn = false,
        currentResolution = config.resolution,
        currentFlashMode = config.flashMode,
        currentFocusMode = config.focusMode
    )
    
    /**
     * Sets camera preview listener
     */
    fun setListener(listener: CameraPreviewListener?) {
        this.listener = listener
    }
    
    /**
     * Gets current camera capabilities
     */
    abstract fun getCameraCapabilities(): CameraCapabilities
    
    /**
     * Gets current camera state
     */
    fun getCameraState(): CameraState = currentState
    
    /**
     * Sets camera resolution
     */
    abstract fun setResolution(resolution: CameraResolution): CameraResult<Unit>
    
    /**
     * Sets flash mode
     */
    abstract fun setFlashMode(mode: FlashMode): CameraResult<Unit>
    
    /**
     * Sets focus mode
     */
    abstract fun setFocusMode(mode: FocusMode): CameraResult<Unit>
    
    /**
     * Manual focus at specific point (normalized coordinates 0.0-1.0)
     */
    abstract fun focusAt(x: Float, y: Float): CameraResult<Unit>
    
    /**
     * Sets zoom level
     */
    abstract fun setZoom(zoom: Float): CameraResult<Unit>
    
    /**
     * Sets camera orientation in degrees
     */
    abstract fun setOrientation(degrees: Int): CameraResult<Unit>
    
    protected fun updateState(update: (CameraState) -> CameraState) {
        currentState = update(currentState)
    }
}