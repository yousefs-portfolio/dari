package com.dari.finance.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageEnhancementTest {

    @Test
    fun `should enhance image contrast`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val lowContrastImage = createLowContrastImageBytes()
        
        // When
        val enhancedImage = imageEnhancer.enhanceContrast(lowContrastImage, 1.5f)
        
        // Then
        assertNotNull(enhancedImage)
        assertTrue(enhancedImage.isNotEmpty())
        // Enhanced image should have different histogram distribution
        val originalHistogram = imageEnhancer.calculateHistogram(lowContrastImage)
        val enhancedHistogram = imageEnhancer.calculateHistogram(enhancedImage)
        assertTrue(histogramSpread(enhancedHistogram) > histogramSpread(originalHistogram))
    }

    @Test
    fun `should adjust image brightness`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val darkImage = createDarkImageBytes()
        
        // When
        val brightenedImage = imageEnhancer.adjustBrightness(darkImage, 50)
        
        // Then
        assertNotNull(brightenedImage)
        assertTrue(brightenedImage.isNotEmpty())
        // Brightened image should have higher average pixel values
        val originalAverage = calculateAveragePixelValue(darkImage)
        val brightenedAverage = calculateAveragePixelValue(brightenedImage)
        assertTrue(brightenedAverage > originalAverage)
    }

    @Test
    fun `should apply gamma correction`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val image = createTestImageBytes()
        val gamma = 1.2f
        
        // When
        val correctedImage = imageEnhancer.applyGammaCorrection(image, gamma)
        
        // Then
        assertNotNull(correctedImage)
        assertTrue(correctedImage.isNotEmpty())
        assertEquals(image.size, correctedImage.size)
    }

    @Test
    fun `should sharpen image`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val blurryImage = createBlurryImageBytes()
        
        // When
        val sharpenedImage = imageEnhancer.sharpenImage(blurryImage, 0.5f)
        
        // Then
        assertNotNull(sharpenedImage)
        assertTrue(sharpenedImage.isNotEmpty())
        // Sharpened image should have more defined edges
        val originalEdgeCount = countEdges(blurryImage)
        val sharpenedEdgeCount = countEdges(sharpenedImage)
        assertTrue(sharpenedEdgeCount >= originalEdgeCount)
    }

    @Test
    fun `should remove noise from image`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val noisyImage = createNoisyImageBytes()
        
        // When
        val denoisedImage = imageEnhancer.removeNoise(noisyImage)
        
        // Then
        assertNotNull(denoisedImage)
        assertTrue(denoisedImage.isNotEmpty())
        // Denoised image should be smoother
        val originalVariance = calculatePixelVariance(noisyImage)
        val denoisedVariance = calculatePixelVariance(denoisedImage)
        assertTrue(denoisedVariance < originalVariance)
    }

    @Test
    fun `should auto-enhance receipt image`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val receiptImage = createMockReceiptImageBytes()
        
        // When
        val autoEnhanced = imageEnhancer.autoEnhanceReceipt(receiptImage)
        
        // Then
        assertNotNull(autoEnhanced)
        assertTrue(autoEnhanced.isNotEmpty())
        // Auto-enhanced should improve OCR readability metrics
        val originalReadability = calculateReadabilityScore(receiptImage)
        val enhancedReadability = calculateReadabilityScore(autoEnhanced)
        assertTrue(enhancedReadability > originalReadability)
    }

    @Test
    fun `should handle extreme enhancement values gracefully`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val image = createTestImageBytes()
        
        // When applying extreme values
        val extremeContrast = imageEnhancer.enhanceContrast(image, 10.0f)
        val extremeBrightness = imageEnhancer.adjustBrightness(image, 500)
        val extremeGamma = imageEnhancer.applyGammaCorrection(image, 0.1f)
        
        // Then all should return valid images without crashing
        assertNotNull(extremeContrast)
        assertNotNull(extremeBrightness)
        assertNotNull(extremeGamma)
        assertTrue(extremeContrast.isNotEmpty())
        assertTrue(extremeBrightness.isNotEmpty())
        assertTrue(extremeGamma.isNotEmpty())
    }

    @Test
    fun `should preserve image dimensions after enhancement`() {
        // Given
        val imageEnhancer = ImageEnhancer()
        val originalImage = createTestImageBytes()
        
        // When
        val contrastEnhanced = imageEnhancer.enhanceContrast(originalImage, 1.3f)
        val brightnessAdjusted = imageEnhancer.adjustBrightness(originalImage, 25)
        
        // Then
        assertEquals(originalImage.size, contrastEnhanced.size)
        assertEquals(originalImage.size, brightnessAdjusted.size)
    }

    private fun createLowContrastImageBytes(): ByteArray {
        // Create image bytes with low contrast (values clustered around middle)
        return ByteArray(300) { (120 + (it % 20)).toByte() }
    }

    private fun createDarkImageBytes(): ByteArray {
        // Create dark image bytes (low pixel values)
        return ByteArray(300) { (it % 60).toByte() }
    }

    private fun createTestImageBytes(): ByteArray {
        // Create test image with varied pixel values
        return ByteArray(300) { (it % 256).toByte() }
    }

    private fun createBlurryImageBytes(): ByteArray {
        // Create image that simulates blur (smooth transitions)
        return ByteArray(300) { i ->
            val value = 128 + (50 * kotlin.math.sin(i / 10.0)).toInt()
            value.coerceIn(0, 255).toByte()
        }
    }

    private fun createNoisyImageBytes(): ByteArray {
        // Create image with noise (random pixel variations)
        return ByteArray(300) { i ->
            val base = (i % 128) + 64
            val noise = (-20..20).random()
            (base + noise).coerceIn(0, 255).toByte()
        }
    }

    private fun createMockReceiptImageBytes(): ByteArray {
        // Mock receipt image data
        return ByteArray(1000) { (it % 256).toByte() }
    }

    private fun histogramSpread(histogram: IntArray): Double {
        if (histogram.isEmpty()) return 0.0
        
        var min = histogram.indices.first { histogram[it] > 0 }
        var max = histogram.indices.last { histogram[it] > 0 }
        
        return (max - min).toDouble()
    }

    private fun calculateAveragePixelValue(imageBytes: ByteArray): Double {
        return imageBytes.map { (it.toInt() and 0xFF).toDouble() }.average()
    }

    private fun countEdges(imageBytes: ByteArray): Int {
        // Simplified edge counting - count significant pixel differences
        var edgeCount = 0
        for (i in 1 until imageBytes.size) {
            val diff = kotlin.math.abs((imageBytes[i].toInt() and 0xFF) - (imageBytes[i-1].toInt() and 0xFF))
            if (diff > 50) edgeCount++
        }
        return edgeCount
    }

    private fun calculatePixelVariance(imageBytes: ByteArray): Double {
        val mean = calculateAveragePixelValue(imageBytes)
        val squaredDiffs = imageBytes.map { 
            val pixel = (it.toInt() and 0xFF).toDouble()
            (pixel - mean) * (pixel - mean)
        }
        return squaredDiffs.average()
    }

    private fun calculateReadabilityScore(imageBytes: ByteArray): Double {
        // Simplified readability score based on contrast and edge definition
        val contrast = histogramSpread(IntArray(256)) / 255.0
        val edges = countEdges(imageBytes).toDouble() / imageBytes.size
        return contrast * 0.6 + edges * 0.4
    }
}