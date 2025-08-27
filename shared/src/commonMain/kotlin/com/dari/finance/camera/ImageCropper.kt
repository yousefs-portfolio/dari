package com.dari.finance.camera

import kotlin.math.*

/**
 * Handles image cropping and perspective correction for receipt scanning
 */
class ImageCropper {

    /**
     * Detects edges in an image to find receipt boundaries
     */
    fun detectEdges(imageBytes: ByteArray): EdgeDetectionResult {
        return try {
            // Convert bytes to grayscale for edge detection
            val grayImage = convertToGrayscale(imageBytes)
            
            // Apply Gaussian blur to reduce noise
            val blurredImage = applyGaussianBlur(grayImage)
            
            // Apply Canny edge detection
            val edges = applyCannyEdgeDetection(blurredImage)
            
            // Find contours from edges
            val contours = findContours(edges)
            
            // Find the largest rectangular contour (likely the receipt)
            val receiptContour = findLargestRectangularContour(contours)
            
            if (receiptContour != null) {
                EdgeDetectionResult(
                    corners = receiptContour,
                    confidence = calculateContourConfidence(receiptContour, edges)
                )
            } else {
                EdgeDetectionResult(corners = emptyList())
            }
        } catch (e: Exception) {
            EdgeDetectionResult(corners = emptyList())
        }
    }

    /**
     * Crops image to detected edges
     */
    fun cropToEdges(imageBytes: ByteArray, edges: EdgeDetectionResult): ByteArray {
        if (edges.corners.size != 4) {
            return imageBytes // Return original if no valid edges
        }

        return try {
            val imageDimensions = getImageDimensions(imageBytes)
            val validCorners = validateAndClampCorners(edges.corners, imageDimensions)
            
            if (validCorners.size != 4) {
                return imageBytes
            }

            // Crop the image to the bounding rectangle of the corners
            cropImageToRectangle(imageBytes, validCorners)
        } catch (e: Exception) {
            imageBytes // Return original on error
        }
    }

    /**
     * Crops image and applies perspective correction
     */
    fun cropAndCorrectPerspective(imageBytes: ByteArray, edges: EdgeDetectionResult): ByteArray {
        if (edges.corners.size != 4) {
            return imageBytes
        }

        return try {
            // First crop to edges
            val croppedImage = cropToEdges(imageBytes, edges)
            
            // Then apply perspective correction
            applyPerspectiveCorrection(croppedImage, edges.corners)
        } catch (e: Exception) {
            imageBytes
        }
    }

    private fun convertToGrayscale(imageBytes: ByteArray): IntArray {
        // Simplified grayscale conversion
        // In real implementation, would properly decode image format
        val grayValues = IntArray(imageBytes.size / 3) // Assuming RGB
        
        for (i in grayValues.indices) {
            val pixelIndex = i * 3
            if (pixelIndex + 2 < imageBytes.size) {
                val r = imageBytes[pixelIndex].toInt() and 0xFF
                val g = imageBytes[pixelIndex + 1].toInt() and 0xFF
                val b = imageBytes[pixelIndex + 2].toInt() and 0xFF
                
                // Standard grayscale conversion formula
                grayValues[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        
        return grayValues
    }

    private fun applyGaussianBlur(grayImage: IntArray): IntArray {
        // Simplified Gaussian blur implementation
        val blurredImage = grayImage.copyOf()
        val size = sqrt(grayImage.size.toDouble()).toInt()
        
        // 3x3 Gaussian kernel
        val kernel = arrayOf(
            intArrayOf(1, 2, 1),
            intArrayOf(2, 4, 2),
            intArrayOf(1, 2, 1)
        )
        val kernelSum = 16
        
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                var sum = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixelIndex = (y + ky) * size + (x + kx)
                        if (pixelIndex in grayImage.indices) {
                            sum += grayImage[pixelIndex] * kernel[ky + 1][kx + 1]
                        }
                    }
                }
                val index = y * size + x
                if (index in blurredImage.indices) {
                    blurredImage[index] = sum / kernelSum
                }
            }
        }
        
        return blurredImage
    }

    private fun applyCannyEdgeDetection(blurredImage: IntArray): IntArray {
        // Simplified Canny edge detection
        val size = sqrt(blurredImage.size.toDouble()).toInt()
        val edges = IntArray(blurredImage.size)
        
        // Sobel operators for gradient calculation
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                var gx = 0
                var gy = 0
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixelIndex = (y + ky) * size + (x + kx)
                        if (pixelIndex in blurredImage.indices) {
                            val pixel = blurredImage[pixelIndex]
                            gx += pixel * sobelX[ky + 1][kx + 1]
                            gy += pixel * sobelY[ky + 1][kx + 1]
                        }
                    }
                }
                
                val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt()
                val index = y * size + x
                if (index in edges.indices) {
                    edges[index] = if (magnitude > 128) 255 else 0 // Threshold
                }
            }
        }
        
        return edges
    }

    private fun findContours(edges: IntArray): List<List<Point>> {
        // Simplified contour finding
        val size = sqrt(edges.size.toDouble()).toInt()
        val contours = mutableListOf<List<Point>>()
        val visited = BooleanArray(edges.size)
        
        for (y in 0 until size) {
            for (x in 0 until size) {
                val index = y * size + x
                if (index in edges.indices && edges[index] > 0 && !visited[index]) {
                    val contour = traceContour(edges, size, x, y, visited)
                    if (contour.size >= 4) {
                        contours.add(contour)
                    }
                }
            }
        }
        
        return contours
    }

    private fun traceContour(
        edges: IntArray,
        size: Int,
        startX: Int,
        startY: Int,
        visited: BooleanArray
    ): List<Point> {
        val contour = mutableListOf<Point>()
        val stack = mutableListOf(Point(startX, startY))
        
        while (stack.isNotEmpty()) {
            val point = stack.removeLastOrNull() ?: break
            val index = point.y * size + point.x
            
            if (index !in edges.indices || visited[index] || edges[index] == 0) {
                continue
            }
            
            visited[index] = true
            contour.add(point)
            
            // Add neighboring points
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val newX = point.x + dx
                    val newY = point.y + dy
                    val newIndex = newY * size + newX
                    
                    if (newX in 0 until size && newY in 0 until size &&
                        newIndex in edges.indices && !visited[newIndex] && edges[newIndex] > 0) {
                        stack.add(Point(newX, newY))
                    }
                }
            }
        }
        
        return contour
    }

    private fun findLargestRectangularContour(contours: List<List<Point>>): List<Point>? {
        var largestContour: List<Point>? = null
        var largestArea = 0.0
        
        for (contour in contours) {
            // Approximate contour to polygon
            val approximatedContour = approximateContourToPolygon(contour)
            
            // Check if it's roughly rectangular (4 corners)
            if (approximatedContour.size == 4) {
                val area = calculatePolygonArea(approximatedContour)
                if (area > largestArea) {
                    largestArea = area
                    largestContour = approximatedContour
                }
            }
        }
        
        return largestContour
    }

    private fun approximateContourToPolygon(contour: List<Point>): List<Point> {
        if (contour.size <= 4) return contour
        
        // Simplified Douglas-Peucker algorithm
        val epsilon = 0.02 * calculateContourPerimeter(contour)
        return douglasPeucker(contour, epsilon)
    }

    private fun douglasPeucker(points: List<Point>, epsilon: Double): List<Point> {
        if (points.size <= 2) return points
        
        // Find the point with maximum distance from line between first and last point
        var maxDistance = 0.0
        var maxIndex = 0
        
        for (i in 1 until points.size - 1) {
            val distance = perpendicularDistance(points[i], points[0], points[points.size - 1])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }
        
        return if (maxDistance > epsilon) {
            // Recursively apply to both halves
            val left = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(points[0], points[points.size - 1])
        }
    }

    private fun perpendicularDistance(point: Point, lineStart: Point, lineEnd: Point): Double {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        
        if (dx == 0 && dy == 0) {
            return sqrt(((point.x - lineStart.x) * (point.x - lineStart.x) + 
                        (point.y - lineStart.y) * (point.y - lineStart.y)).toDouble())
        }
        
        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy).toDouble() / 
                (dx * dx + dy * dy)
        
        val closestX = lineStart.x + t * dx
        val closestY = lineStart.y + t * dy
        
        return sqrt((point.x - closestX) * (point.x - closestX) + 
                   (point.y - closestY) * (point.y - closestY))
    }

    private fun calculateContourPerimeter(contour: List<Point>): Double {
        if (contour.size < 2) return 0.0
        
        var perimeter = 0.0
        for (i in 0 until contour.size) {
            val current = contour[i]
            val next = contour[(i + 1) % contour.size]
            perimeter += sqrt(((next.x - current.x) * (next.x - current.x) + 
                              (next.y - current.y) * (next.y - current.y)).toDouble())
        }
        return perimeter
    }

    private fun calculatePolygonArea(polygon: List<Point>): Double {
        if (polygon.size < 3) return 0.0
        
        var area = 0.0
        for (i in polygon.indices) {
            val current = polygon[i]
            val next = polygon[(i + 1) % polygon.size]
            area += (current.x * next.y - next.x * current.y)
        }
        return abs(area) / 2.0
    }

    private fun calculateContourConfidence(corners: List<Point>, edges: IntArray): Float {
        if (corners.size != 4) return 0.0f
        
        // Calculate confidence based on:
        // 1. How rectangular the shape is
        // 2. Edge strength along the contour
        val rectangularityScore = calculateRectangularityScore(corners)
        val edgeStrengthScore = calculateEdgeStrengthScore(corners, edges)
        
        return (rectangularityScore + edgeStrengthScore) / 2.0f
    }

    private fun calculateRectangularityScore(corners: List<Point>): Float {
        if (corners.size != 4) return 0.0f
        
        // Calculate angles between consecutive corners
        val angles = mutableListOf<Double>()
        for (i in corners.indices) {
            val prev = corners[(i - 1 + corners.size) % corners.size]
            val current = corners[i]
            val next = corners[(i + 1) % corners.size]
            
            val angle = calculateAngle(prev, current, next)
            angles.add(angle)
        }
        
        // Good rectangles should have angles close to 90 degrees
        val idealAngle = PI / 2.0
        val angleScore = angles.map { 1.0 - abs(it - idealAngle) / idealAngle }.average()
        
        return angleScore.toFloat().coerceIn(0.0f, 1.0f)
    }

    private fun calculateAngle(p1: Point, p2: Point, p3: Point): Double {
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y
        
        val dot = v1x * v2x + v1y * v2y
        val mag1 = sqrt((v1x * v1x + v1y * v1y).toDouble())
        val mag2 = sqrt((v2x * v2x + v2y * v2y).toDouble())
        
        if (mag1 == 0.0 || mag2 == 0.0) return 0.0
        
        val cosAngle = dot / (mag1 * mag2)
        return acos(cosAngle.coerceIn(-1.0, 1.0))
    }

    private fun calculateEdgeStrengthScore(corners: List<Point>, edges: IntArray): Float {
        // This would calculate the strength of edges along the contour
        // Simplified implementation returns fixed confidence
        return 0.8f
    }

    private fun getImageDimensions(imageBytes: ByteArray): Pair<Int, Int> {
        // Simplified - in real implementation would decode image headers
        val estimated = sqrt(imageBytes.size.toDouble()).toInt()
        return Pair(estimated, estimated)
    }

    private fun validateAndClampCorners(
        corners: List<Point>,
        dimensions: Pair<Int, Int>
    ): List<Point> {
        val (width, height) = dimensions
        return corners.map { point ->
            Point(
                point.x.coerceIn(0, width - 1),
                point.y.coerceIn(0, height - 1)
            )
        }
    }

    private fun cropImageToRectangle(imageBytes: ByteArray, corners: List<Point>): ByteArray {
        // Find bounding rectangle
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }
        
        // Simplified cropping - return smaller portion of original bytes
        val cropWidth = maxX - minX
        val cropHeight = maxY - minY
        val croppedSize = (imageBytes.size * cropWidth * cropHeight) / 
                         (getImageDimensions(imageBytes).first * getImageDimensions(imageBytes).second)
        
        return imageBytes.copyOf(croppedSize.coerceAtMost(imageBytes.size))
    }

    private fun applyPerspectiveCorrection(imageBytes: ByteArray, corners: List<Point>): ByteArray {
        // Simplified perspective correction
        // In real implementation would apply homography transformation
        return imageBytes
    }
}

data class Point(val x: Int, val y: Int)

data class EdgeDetectionResult(
    val corners: List<Point>,
    val confidence: Float = 0.0f
)