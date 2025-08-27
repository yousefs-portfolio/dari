package com.dari.finance.camera

import kotlin.math.*

/**
 * Handles image enhancement operations for better OCR results
 */
class ImageEnhancer {

    /**
     * Enhances image contrast using histogram stretching
     */
    fun enhanceContrast(imageBytes: ByteArray, factor: Float): ByteArray {
        if (imageBytes.isEmpty()) return imageBytes
        
        val histogram = calculateHistogram(imageBytes)
        val enhancedBytes = ByteArray(imageBytes.size)
        
        // Find current min and max values
        val minValue = findMinValue(histogram)
        val maxValue = findMaxValue(histogram)
        
        if (minValue == maxValue) return imageBytes // No contrast to enhance
        
        val range = maxValue - minValue
        
        for (i in imageBytes.indices) {
            val originalValue = imageBytes[i].toInt() and 0xFF
            
            // Apply contrast enhancement
            val normalized = (originalValue - minValue).toFloat() / range
            val enhanced = normalized * factor
            val stretched = (enhanced * 255).toInt().coerceIn(0, 255)
            
            enhancedBytes[i] = stretched.toByte()
        }
        
        return enhancedBytes
    }

    /**
     * Adjusts image brightness by adding/subtracting a constant value
     */
    fun adjustBrightness(imageBytes: ByteArray, adjustment: Int): ByteArray {
        if (imageBytes.isEmpty()) return imageBytes
        
        val adjustedBytes = ByteArray(imageBytes.size)
        
        for (i in imageBytes.indices) {
            val originalValue = imageBytes[i].toInt() and 0xFF
            val adjustedValue = (originalValue + adjustment).coerceIn(0, 255)
            adjustedBytes[i] = adjustedValue.toByte()
        }
        
        return adjustedBytes
    }

    /**
     * Applies gamma correction to adjust midtone brightness
     */
    fun applyGammaCorrection(imageBytes: ByteArray, gamma: Float): ByteArray {
        if (imageBytes.isEmpty() || gamma <= 0) return imageBytes
        
        // Pre-calculate lookup table for efficiency
        val gammaLookup = IntArray(256) { i ->
            (255.0 * (i / 255.0).pow(1.0 / gamma)).toInt().coerceIn(0, 255)
        }
        
        val correctedBytes = ByteArray(imageBytes.size)
        
        for (i in imageBytes.indices) {
            val originalValue = imageBytes[i].toInt() and 0xFF
            correctedBytes[i] = gammaLookup[originalValue].toByte()
        }
        
        return correctedBytes
    }

    /**
     * Sharpens image using unsharp masking technique
     */
    fun sharpenImage(imageBytes: ByteArray, intensity: Float): ByteArray {
        if (imageBytes.isEmpty()) return imageBytes
        
        val size = sqrt(imageBytes.size.toDouble()).toInt()
        if (size * size != imageBytes.size) {
            // If not square, apply simple sharpening
            return applySimpleSharpening(imageBytes, intensity)
        }
        
        // Create Gaussian blur of the image
        val blurred = applyGaussianBlur(imageBytes, size)
        
        // Apply unsharp mask: original + intensity * (original - blurred)
        val sharpened = ByteArray(imageBytes.size)
        
        for (i in imageBytes.indices) {
            val original = imageBytes[i].toInt() and 0xFF
            val blurredPixel = blurred[i].toInt() and 0xFF
            val difference = original - blurredPixel
            val sharpened_value = original + (intensity * difference).toInt()
            sharpened[i] = sharpened_value.coerceIn(0, 255).toByte()
        }
        
        return sharpened
    }

    /**
     * Removes noise using median filtering
     */
    fun removeNoise(imageBytes: ByteArray): ByteArray {
        if (imageBytes.isEmpty()) return imageBytes
        
        val size = sqrt(imageBytes.size.toDouble()).toInt()
        if (size * size != imageBytes.size) {
            return applySimpleNoiseReduction(imageBytes)
        }
        
        val filtered = ByteArray(imageBytes.size)
        
        // Apply 3x3 median filter
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val neighbors = mutableListOf<Int>()
                
                // Collect 3x3 neighborhood
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val index = (y + dy) * size + (x + dx)
                        if (index in imageBytes.indices) {
                            neighbors.add(imageBytes[index].toInt() and 0xFF)
                        }
                    }
                }
                
                // Find median
                neighbors.sort()
                val median = neighbors[neighbors.size / 2]
                
                val currentIndex = y * size + x
                if (currentIndex in filtered.indices) {
                    filtered[currentIndex] = median.toByte()
                }
            }
        }
        
        // Copy edges from original
        for (i in imageBytes.indices) {
            if (filtered[i] == 0.toByte()) {
                filtered[i] = imageBytes[i]
            }
        }
        
        return filtered
    }

    /**
     * Auto-enhance receipt image for better OCR results
     */
    fun autoEnhanceReceipt(imageBytes: ByteArray): ByteArray {
        if (imageBytes.isEmpty()) return imageBytes
        
        var enhanced = imageBytes
        
        // Step 1: Noise reduction
        enhanced = removeNoise(enhanced)
        
        // Step 2: Gamma correction for better midtone visibility
        enhanced = applyGammaCorrection(enhanced, 0.8f)
        
        // Step 3: Contrast enhancement
        enhanced = enhanceContrast(enhanced, 1.3f)
        
        // Step 4: Brightness adjustment if needed
        val averageBrightness = calculateAveragePixelValue(enhanced)
        if (averageBrightness < 100) {
            enhanced = adjustBrightness(enhanced, (120 - averageBrightness).toInt())
        } else if (averageBrightness > 180) {
            enhanced = adjustBrightness(enhanced, (150 - averageBrightness).toInt())
        }
        
        // Step 5: Light sharpening for text clarity
        enhanced = sharpenImage(enhanced, 0.3f)
        
        return enhanced
    }

    /**
     * Calculates histogram of pixel values
     */
    fun calculateHistogram(imageBytes: ByteArray): IntArray {
        val histogram = IntArray(256)
        
        for (byte in imageBytes) {
            val value = byte.toInt() and 0xFF
            histogram[value]++
        }
        
        return histogram
    }

    private fun findMinValue(histogram: IntArray): Int {
        for (i in histogram.indices) {
            if (histogram[i] > 0) return i
        }
        return 0
    }

    private fun findMaxValue(histogram: IntArray): Int {
        for (i in histogram.indices.reversed()) {
            if (histogram[i] > 0) return i
        }
        return 255
    }

    private fun applyGaussianBlur(imageBytes: ByteArray, size: Int): ByteArray {
        val blurred = ByteArray(imageBytes.size)
        
        // 3x3 Gaussian kernel
        val kernel = arrayOf(
            doubleArrayOf(1.0, 2.0, 1.0),
            doubleArrayOf(2.0, 4.0, 2.0),
            doubleArrayOf(1.0, 2.0, 1.0)
        )
        val kernelSum = 16.0
        
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                var sum = 0.0
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixelIndex = (y + ky) * size + (x + kx)
                        if (pixelIndex in imageBytes.indices) {
                            val pixelValue = imageBytes[pixelIndex].toInt() and 0xFF
                            sum += pixelValue * kernel[ky + 1][kx + 1]
                        }
                    }
                }
                
                val index = y * size + x
                if (index in blurred.indices) {
                    blurred[index] = (sum / kernelSum).toInt().coerceIn(0, 255).toByte()
                }
            }
        }
        
        // Copy edges from original
        for (i in imageBytes.indices) {
            if (blurred[i] == 0.toByte()) {
                blurred[i] = imageBytes[i]
            }
        }
        
        return blurred
    }

    private fun applySimpleSharpening(imageBytes: ByteArray, intensity: Float): ByteArray {
        val sharpened = ByteArray(imageBytes.size)
        
        // Simple sharpening by enhancing differences between adjacent pixels
        for (i in 1 until imageBytes.size - 1) {
            val current = imageBytes[i].toInt() and 0xFF
            val prev = imageBytes[i - 1].toInt() and 0xFF
            val next = imageBytes[i + 1].toInt() and 0xFF
            
            val avgNeighbor = (prev + next) / 2
            val difference = current - avgNeighbor
            val sharpenedValue = current + (intensity * difference).toInt()
            
            sharpened[i] = sharpenedValue.coerceIn(0, 255).toByte()
        }
        
        // Copy first and last pixels
        sharpened[0] = imageBytes[0]
        sharpened[imageBytes.size - 1] = imageBytes[imageBytes.size - 1]
        
        return sharpened
    }

    private fun applySimpleNoiseReduction(imageBytes: ByteArray): ByteArray {
        val filtered = ByteArray(imageBytes.size)
        
        // Simple 3-point median filter for 1D case
        for (i in 1 until imageBytes.size - 1) {
            val values = listOf(
                imageBytes[i - 1].toInt() and 0xFF,
                imageBytes[i].toInt() and 0xFF,
                imageBytes[i + 1].toInt() and 0xFF
            ).sorted()
            
            filtered[i] = values[1].toByte() // Median value
        }
        
        // Copy first and last pixels
        filtered[0] = imageBytes[0]
        filtered[imageBytes.size - 1] = imageBytes[imageBytes.size - 1]
        
        return filtered
    }

    private fun calculateAveragePixelValue(imageBytes: ByteArray): Double {
        if (imageBytes.isEmpty()) return 0.0
        
        var sum = 0.0
        for (byte in imageBytes) {
            sum += byte.toInt() and 0xFF
        }
        
        return sum / imageBytes.size
    }
}

/**
 * Enhancement parameters for different image types
 */
data class EnhancementParams(
    val contrastFactor: Float = 1.2f,
    val brightnessAdjustment: Int = 0,
    val gamma: Float = 1.0f,
    val sharpenIntensity: Float = 0.2f,
    val applyNoiseReduction: Boolean = true
) {
    companion object {
        val RECEIPT_OPTIMIZED = EnhancementParams(
            contrastFactor = 1.3f,
            brightnessAdjustment = 10,
            gamma = 0.8f,
            sharpenIntensity = 0.3f,
            applyNoiseReduction = true
        )
        
        val DOCUMENT_OPTIMIZED = EnhancementParams(
            contrastFactor = 1.4f,
            brightnessAdjustment = 5,
            gamma = 0.9f,
            sharpenIntensity = 0.4f,
            applyNoiseReduction = true
        )
        
        val LOW_LIGHT_OPTIMIZED = EnhancementParams(
            contrastFactor = 1.5f,
            brightnessAdjustment = 30,
            gamma = 0.7f,
            sharpenIntensity = 0.2f,
            applyNoiseReduction = true
        )
    }
}