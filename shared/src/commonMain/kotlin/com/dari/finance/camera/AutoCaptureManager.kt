package com.dari.finance.camera

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Manages automatic receipt capture when receipt is properly aligned
 */
class AutoCaptureManager {
    
    private var _alignmentState = MutableStateFlow(AlignmentState.SEARCHING)
    val alignmentState: StateFlow<AlignmentState> = _alignmentState.asStateFlow()
    
    private var _captureCountdown = MutableStateFlow(0)
    val captureCountdown: StateFlow<Int> = _captureCountdown.asStateFlow()
    
    private var isEnabled = false
    private var alignmentJob: Job? = null
    private var captureJob: Job? = null
    
    private val imageCropper = ImageCropper()
    private val alignmentThreshold = 0.7f
    private val stabilityRequiredMs = 2000L // 2 seconds of stability required
    private val countdownDurationSec = 3 // 3 second countdown
    
    /**
     * Enable auto-capture functionality
     */
    fun enable() {
        isEnabled = true
        _alignmentState.value = AlignmentState.SEARCHING
    }
    
    /**
     * Disable auto-capture functionality
     */
    fun disable() {
        isEnabled = false
        alignmentJob?.cancel()
        captureJob?.cancel()
        _alignmentState.value = AlignmentState.DISABLED
        _captureCountdown.value = 0
    }
    
    /**
     * Process camera frame for alignment detection
     */
    fun processFrame(imageBytes: ByteArray, onAutoCapture: () -> Unit) {
        if (!isEnabled) return
        
        alignmentJob?.cancel()
        alignmentJob = CoroutineScope(Dispatchers.Default).launch {
            val alignmentInfo = detectReceiptAlignment(imageBytes)
            updateAlignmentState(alignmentInfo, onAutoCapture)
        }
    }
    
    private suspend fun detectReceiptAlignment(imageBytes: ByteArray): AlignmentInfo {
        return withContext(Dispatchers.Default) {
            val edgeResult = imageCropper.detectEdges(imageBytes)
            
            when {
                edgeResult.corners.size != 4 -> {
                    AlignmentInfo(
                        state = AlignmentState.NO_RECEIPT_DETECTED,
                        confidence = 0.0f
                    )
                }
                edgeResult.confidence < alignmentThreshold -> {
                    AlignmentInfo(
                        state = AlignmentState.RECEIPT_DETECTED,
                        confidence = edgeResult.confidence
                    )
                }
                !isReceiptProperlyFramed(edgeResult.corners) -> {
                    AlignmentInfo(
                        state = AlignmentState.NEEDS_ADJUSTMENT,
                        confidence = edgeResult.confidence,
                        adjustmentHint = calculateAdjustmentHint(edgeResult.corners)
                    )
                }
                else -> {
                    AlignmentInfo(
                        state = AlignmentState.ALIGNED,
                        confidence = edgeResult.confidence
                    )
                }
            }
        }
    }
    
    private fun isReceiptProperlyFramed(corners: List<Point>): Boolean {
        if (corners.size != 4) return false
        
        // Check if receipt fills appropriate amount of frame
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }
        
        val width = maxX - minX
        val height = maxY - minY
        
        // Assume frame size for validation (would be actual frame size in real implementation)
        val frameWidth = 1000
        val frameHeight = 1000
        
        // Receipt should fill 60-90% of frame width and height
        val widthRatio = width.toFloat() / frameWidth
        val heightRatio = height.toFloat() / frameHeight
        
        return widthRatio in 0.6f..0.9f && heightRatio in 0.6f..0.9f &&
               isReceiptCentered(corners, frameWidth, frameHeight)
    }
    
    private fun isReceiptCentered(corners: List<Point>, frameWidth: Int, frameHeight: Int): Boolean {
        val centerX = corners.map { it.x }.average()
        val centerY = corners.map { it.y }.average()
        
        val frameCenterX = frameWidth / 2.0
        val frameCenterY = frameHeight / 2.0
        
        val offsetX = kotlin.math.abs(centerX - frameCenterX) / frameCenterX
        val offsetY = kotlin.math.abs(centerY - frameCenterY) / frameCenterY
        
        // Allow 20% offset from center
        return offsetX < 0.2 && offsetY < 0.2
    }
    
    private fun calculateAdjustmentHint(corners: List<Point>): AdjustmentHint {
        if (corners.size != 4) return AdjustmentHint.MOVE_CLOSER
        
        // Assume frame dimensions
        val frameWidth = 1000
        val frameHeight = 1000
        val frameCenterX = frameWidth / 2.0
        val frameCenterY = frameHeight / 2.0
        
        val receiptCenterX = corners.map { it.x }.average()
        val receiptCenterY = corners.map { it.y }.average()
        
        val width = corners.maxOf { it.x } - corners.minOf { it.x }
        val height = corners.maxOf { it.y } - corners.minOf { it.y }
        
        val widthRatio = width.toFloat() / frameWidth
        val heightRatio = height.toFloat() / frameHeight
        
        return when {
            widthRatio < 0.4f || heightRatio < 0.4f -> AdjustmentHint.MOVE_CLOSER
            widthRatio > 0.95f || heightRatio > 0.95f -> AdjustmentHint.MOVE_BACK
            receiptCenterX < frameCenterX * 0.8 -> AdjustmentHint.MOVE_RIGHT
            receiptCenterX > frameCenterX * 1.2 -> AdjustmentHint.MOVE_LEFT
            receiptCenterY < frameCenterY * 0.8 -> AdjustmentHint.MOVE_DOWN
            receiptCenterY > frameCenterY * 1.2 -> AdjustmentHint.MOVE_UP
            else -> AdjustmentHint.HOLD_STEADY
        }
    }
    
    private suspend fun updateAlignmentState(alignmentInfo: AlignmentInfo, onAutoCapture: () -> Unit) {
        _alignmentState.value = alignmentInfo.state
        
        when (alignmentInfo.state) {
            AlignmentState.ALIGNED -> {
                startCaptureCountdown(onAutoCapture)
            }
            else -> {
                captureJob?.cancel()
                _captureCountdown.value = 0
            }
        }
    }
    
    private fun startCaptureCountdown(onAutoCapture: () -> Unit) {
        captureJob?.cancel()
        captureJob = CoroutineScope(Dispatchers.Main).launch {
            // Wait for stability
            delay(stabilityRequiredMs)
            
            // Check if still aligned after stability period
            if (_alignmentState.value == AlignmentState.ALIGNED) {
                // Start countdown
                for (i in countdownDurationSec downTo 1) {
                    _captureCountdown.value = i
                    delay(1000L)
                    
                    // Check if still aligned during countdown
                    if (_alignmentState.value != AlignmentState.ALIGNED) {
                        _captureCountdown.value = 0
                        return@launch
                    }
                }
                
                _captureCountdown.value = 0
                onAutoCapture()
            }
        }
    }
}

/**
 * Receipt alignment states
 */
enum class AlignmentState {
    DISABLED,
    SEARCHING,
    NO_RECEIPT_DETECTED,
    RECEIPT_DETECTED,
    NEEDS_ADJUSTMENT,
    ALIGNED
}

/**
 * Adjustment hints for user guidance
 */
enum class AdjustmentHint {
    MOVE_CLOSER,
    MOVE_BACK,
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,
    HOLD_STEADY
}

/**
 * Alignment information result
 */
data class AlignmentInfo(
    val state: AlignmentState,
    val confidence: Float,
    val adjustmentHint: AdjustmentHint? = null
)