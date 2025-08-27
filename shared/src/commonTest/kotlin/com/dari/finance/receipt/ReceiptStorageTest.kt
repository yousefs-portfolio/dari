package com.dari.finance.receipt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class ReceiptStorageTest {

    @Test
    fun `should store receipt image successfully`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_123"
        val imageBytes = createMockReceiptImageBytes()
        
        // When
        val result = receiptStorage.storeReceiptImage(receiptId, imageBytes)
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(receiptId, result.getOrNull()?.receiptId)
    }

    @Test
    fun `should retrieve stored receipt image`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_456"
        val originalImageBytes = createMockReceiptImageBytes()
        
        // Store image first
        receiptStorage.storeReceiptImage(receiptId, originalImageBytes)
        
        // When
        val retrievedImage = receiptStorage.getReceiptImage(receiptId)
        
        // Then
        assertNotNull(retrievedImage)
        assertEquals(originalImageBytes.size, retrievedImage.size)
        assertTrue(retrievedImage.contentEquals(originalImageBytes))
    }

    @Test
    fun `should store receipt with metadata`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptMetadata = ReceiptMetadata(
            id = "receipt_789",
            fileName = "receipt_2023_12_01.jpg",
            fileSize = 1024L,
            mimeType = "image/jpeg",
            capturedAt = System.currentTimeMillis(),
            ocrProcessed = false,
            confidence = 0.85f
        )
        val imageBytes = createMockReceiptImageBytes()
        
        // When
        val result = receiptStorage.storeReceiptWithMetadata(receiptMetadata, imageBytes)
        
        // Then
        assertTrue(result.isSuccess)
        val storedMetadata = receiptStorage.getReceiptMetadata(receiptMetadata.id)
        assertNotNull(storedMetadata)
        assertEquals(receiptMetadata.fileName, storedMetadata.fileName)
        assertEquals(receiptMetadata.confidence, storedMetadata.confidence)
    }

    @Test
    fun `should delete receipt image and metadata`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_delete_test"
        val imageBytes = createMockReceiptImageBytes()
        
        // Store image first
        receiptStorage.storeReceiptImage(receiptId, imageBytes)
        
        // When
        val deleteResult = receiptStorage.deleteReceipt(receiptId)
        
        // Then
        assertTrue(deleteResult.isSuccess)
        assertNull(receiptStorage.getReceiptImage(receiptId))
        assertNull(receiptStorage.getReceiptMetadata(receiptId))
    }

    @Test
    fun `should handle storage path creation`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_path_test"
        
        // When
        val storagePath = receiptStorage.generateStoragePath(receiptId, "jpg")
        
        // Then
        assertNotNull(storagePath)
        assertTrue(storagePath.contains(receiptId))
        assertTrue(storagePath.endsWith(".jpg"))
    }

    @Test
    fun `should compress large images before storage`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_compression_test"
        val largeImageBytes = createLargeImageBytes() // > 5MB
        
        // When
        val result = receiptStorage.storeReceiptImage(receiptId, largeImageBytes)
        
        // Then
        assertTrue(result.isSuccess)
        val storedImage = receiptStorage.getReceiptImage(receiptId)
        assertNotNull(storedImage)
        assertTrue(storedImage.size < largeImageBytes.size) // Should be compressed
    }

    @Test
    fun `should maintain image quality during compression`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_quality_test"
        val imageBytes = createHighQualityImageBytes()
        
        // When
        val result = receiptStorage.storeReceiptImage(receiptId, imageBytes)
        
        // Then
        assertTrue(result.isSuccess)
        val storedImage = receiptStorage.getReceiptImage(receiptId)
        assertNotNull(storedImage)
        // Quality should be maintained (mock test - in real implementation would check PSNR)
        assertTrue(calculateImageSimilarity(imageBytes, storedImage) > 0.95f)
    }

    @Test
    fun `should store thumbnail along with full image`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_thumbnail_test"
        val imageBytes = createMockReceiptImageBytes()
        
        // When
        val result = receiptStorage.storeReceiptImageWithThumbnail(receiptId, imageBytes)
        
        // Then
        assertTrue(result.isSuccess)
        val thumbnail = receiptStorage.getReceiptThumbnail(receiptId)
        assertNotNull(thumbnail)
        assertTrue(thumbnail.size < imageBytes.size) // Thumbnail should be smaller
    }

    @Test
    fun `should list all stored receipts`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptIds = listOf("receipt_1", "receipt_2", "receipt_3")
        
        // Store multiple receipts
        receiptIds.forEach { id ->
            receiptStorage.storeReceiptImage(id, createMockReceiptImageBytes())
        }
        
        // When
        val storedReceipts = receiptStorage.getAllReceiptMetadata()
        
        // Then
        assertTrue(storedReceipts.size >= receiptIds.size)
        receiptIds.forEach { id ->
            assertTrue(storedReceipts.any { it.id == id })
        }
    }

    @Test
    fun `should handle storage errors gracefully`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receiptId = "receipt_error_test"
        val corruptedImageBytes = ByteArray(0) // Empty bytes
        
        // When
        val result = receiptStorage.storeReceiptImage(receiptId, corruptedImageBytes)
        
        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `should support batch operations`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val receipts = listOf(
            "receipt_batch_1" to createMockReceiptImageBytes(),
            "receipt_batch_2" to createMockReceiptImageBytes(),
            "receipt_batch_3" to createMockReceiptImageBytes()
        )
        
        // When
        val results = receiptStorage.storeReceiptsBatch(receipts)
        
        // Then
        assertEquals(receipts.size, results.size)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun `should calculate storage usage`() = runBlocking {
        // Given
        val receiptStorage = ReceiptStorage()
        val initialUsage = receiptStorage.getStorageUsage()
        
        // Store a receipt
        receiptStorage.storeReceiptImage("storage_test", createMockReceiptImageBytes())
        
        // When
        val newUsage = receiptStorage.getStorageUsage()
        
        // Then
        assertTrue(newUsage.totalBytes > initialUsage.totalBytes)
        assertTrue(newUsage.receiptCount > initialUsage.receiptCount)
    }

    private fun createMockReceiptImageBytes(): ByteArray {
        return ByteArray(1024) { (it % 256).toByte() }
    }

    private fun createLargeImageBytes(): ByteArray {
        return ByteArray(5 * 1024 * 1024) { (it % 256).toByte() } // 5MB
    }

    private fun createHighQualityImageBytes(): ByteArray {
        return ByteArray(2048) { (it % 256).toByte() }
    }

    private fun calculateImageSimilarity(image1: ByteArray, image2: ByteArray): Float {
        // Mock similarity calculation - in real implementation would use PSNR or SSIM
        return if (image1.size == image2.size) 0.95f else 0.7f
    }
}