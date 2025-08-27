package com.dari.finance.camera

/**
 * Image capture interface for taking receipt photos
 * Provides high-quality image capture optimized for OCR processing
 */
interface ImageCapture {
    
    /**
     * Captures an image with current settings
     * @param onSuccess Callback with captured image data
     * @param onError Callback with error message
     */
    fun captureImage(
        onSuccess: (ByteArray) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Configures capture settings
     * @param settings Capture configuration
     * @return Result indicating success or failure
     */
    fun configureCaptureSettings(settings: CaptureSettings): CameraResult<Unit>
    
    /**
     * Checks if image capture is currently in progress
     * @return True if capturing, false otherwise
     */
    fun isCapturingInProgress(): Boolean
}

/**
 * Capture settings configuration
 */
data class CaptureSettings(
    val resolution: CameraResolution = CameraResolution.HD_720P,
    val format: ImageFormat = ImageFormat.JPEG,
    val quality: Int = 85, // 0-100 for JPEG
    val enableHDR: Boolean = false,
    val enableNoiseReduction: Boolean = true,
    val enableImageStabilization: Boolean = true,
    val captureMode: CaptureMode = CaptureMode.SINGLE,
    val autoExposure: Boolean = true,
    val autoWhiteBalance: Boolean = true
) {
    companion object {
        /**
         * Optimized settings for receipt scanning
         */
        fun forReceiptScanning(): CaptureSettings {
            return CaptureSettings(
                resolution = CameraResolution.FULL_HD_1080P,
                format = ImageFormat.JPEG,
                quality = 90, // High quality for OCR
                enableHDR = true, // Better text contrast
                enableNoiseReduction = true,
                enableImageStabilization = true,
                autoExposure = true,
                autoWhiteBalance = true
            )
        }
        
        /**
         * Fast capture settings for quick scanning
         */
        fun forQuickScan(): CaptureSettings {
            return CaptureSettings(
                resolution = CameraResolution.HD_720P,
                format = ImageFormat.JPEG,
                quality = 80,
                enableHDR = false, // Faster capture
                enableNoiseReduction = false,
                enableImageStabilization = false
            )
        }
        
        /**
         * High quality settings for archival
         */
        fun forArchival(): CaptureSettings {
            return CaptureSettings(
                resolution = CameraResolution.UHD_4K,
                format = ImageFormat.JPEG,
                quality = 95,
                enableHDR = true,
                enableNoiseReduction = true,
                enableImageStabilization = true
            )
        }
    }
}

/**
 * Capture mode options
 */
enum class CaptureMode {
    SINGLE,    // Single image capture
    BURST,     // Multiple rapid captures
    HDR,       // High dynamic range
    NIGHT,     // Low light optimization
    DOCUMENT   // Document/text optimization
}

/**
 * Image metadata information
 */
data class ImageMetadata(
    val timestamp: Long,
    val location: LocationData? = null,
    val orientation: Int = 0, // Degrees: 0, 90, 180, 270
    val deviceInfo: DeviceInfo? = null,
    val cameraSettings: CameraSettings? = null,
    val fileSize: Long = 0,
    val dimensions: ImageDimensions? = null
)

/**
 * Location data for geotagging
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val altitude: Double? = null
)

/**
 * Device information
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val appVersion: String = "1.0.0"
)

/**
 * Camera settings used for capture
 */
data class CameraSettings(
    val iso: Int = 0,
    val shutterSpeed: String = "auto",
    val aperture: String = "auto",
    val focalLength: Float = 0f,
    val flashUsed: Boolean = false,
    val whiteBalance: String = "auto",
    val exposureCompensation: Float = 0f
)

/**
 * Image dimensions
 */
data class ImageDimensions(
    val width: Int,
    val height: Int,
    val aspectRatio: Float = width.toFloat() / height.toFloat()
)

/**
 * Advanced image capture interface with more features
 */
interface AdvancedImageCapture : ImageCapture {
    
    /**
     * Captures image with metadata
     */
    fun captureImageWithMetadata(
        metadata: ImageMetadata,
        onSuccess: (ByteArray, ImageMetadata) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Captures multiple images in burst mode
     */
    fun captureBurst(
        count: Int,
        interval: Long = 100L, // ms between captures
        onImageCaptured: (index: Int, imageData: ByteArray) -> Unit,
        onBurstComplete: (images: List<ByteArray>) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Captures image with auto-enhancement for documents
     */
    fun captureDocument(
        enhanceContrast: Boolean = true,
        enhanceSharpness: Boolean = true,
        onSuccess: (ByteArray) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Cancels ongoing capture operation
     */
    fun cancelCapture()
    
    /**
     * Gets capture capabilities of current device
     */
    fun getCaptureCapabilities(): CaptureCapabilities
}

/**
 * Capture capabilities information
 */
data class CaptureCapabilities(
    val supportedResolutions: List<CameraResolution>,
    val supportedFormats: List<ImageFormat>,
    val maxBurstCount: Int,
    val supportsHDR: Boolean,
    val supportsRAW: Boolean,
    val supportsManualControls: Boolean,
    val maxISO: Int,
    val minShutterSpeed: String,
    val maxShutterSpeed: String
)

/**
 * Image capture manager with advanced features
 */
abstract class ImageCaptureManager(
    protected val initialSettings: CaptureSettings = CaptureSettings()
) : AdvancedImageCapture {
    
    protected var currentSettings = initialSettings
    protected var isCapturing = false
    protected val listeners = mutableListOf<ImageCaptureListener>()
    
    /**
     * Adds capture event listener
     */
    fun addListener(listener: ImageCaptureListener) {
        listeners.add(listener)
    }
    
    /**
     * Removes capture event listener
     */
    fun removeListener(listener: ImageCaptureListener) {
        listeners.remove(listener)
    }
    
    protected fun notifyListeners(event: CaptureEvent) {
        listeners.forEach { it.onCaptureEvent(event) }
    }
    
    override fun isCapturingInProgress(): Boolean = isCapturing
    
    /**
     * Optimizes image for OCR processing
     */
    protected fun optimizeForOCR(imageData: ByteArray): ByteArray {
        // Platform-specific image optimization
        return imageData // Default implementation returns original
    }
    
    /**
     * Enhances image contrast and sharpness
     */
    protected fun enhanceImage(
        imageData: ByteArray,
        enhanceContrast: Boolean,
        enhanceSharpness: Boolean
    ): ByteArray {
        // Platform-specific image enhancement
        return imageData // Default implementation returns original
    }
}

/**
 * Image capture event listener
 */
interface ImageCaptureListener {
    fun onCaptureEvent(event: CaptureEvent)
}

/**
 * Capture events
 */
sealed class CaptureEvent {
    object CaptureStarted : CaptureEvent()
    object CaptureCompleted : CaptureEvent()
    data class CaptureProgress(val progress: Float) : CaptureEvent()
    data class CaptureFailed(val error: String) : CaptureEvent()
    object CaptureCancelled : CaptureEvent()
}

/**
 * Image processing utilities
 */
object ImageProcessingUtils {
    
    /**
     * Calculates optimal image quality for given file size target
     */
    fun calculateOptimalQuality(
        targetSizeKB: Int,
        resolution: CameraResolution
    ): Int {
        val pixelCount = resolution.width * resolution.height
        val baseQuality = 85
        val sizeRatio = targetSizeKB / (pixelCount / 1000.0)
        
        return (baseQuality * sizeRatio.coerceIn(0.5, 1.5)).toInt().coerceIn(50, 95)
    }
    
    /**
     * Gets recommended settings for specific use cases
     */
    fun getRecommendedSettings(useCase: ImageUseCase): CaptureSettings {
        return when (useCase) {
            ImageUseCase.RECEIPT_SCANNING -> CaptureSettings.forReceiptScanning()
            ImageUseCase.QUICK_SCAN -> CaptureSettings.forQuickScan()
            ImageUseCase.ARCHIVAL -> CaptureSettings.forArchival()
            ImageUseCase.SOCIAL_SHARING -> CaptureSettings(
                resolution = CameraResolution.HD_720P,
                quality = 80,
                format = ImageFormat.JPEG
            )
        }
    }
}

enum class ImageUseCase {
    RECEIPT_SCANNING,
    QUICK_SCAN,
    ARCHIVAL,
    SOCIAL_SHARING
}