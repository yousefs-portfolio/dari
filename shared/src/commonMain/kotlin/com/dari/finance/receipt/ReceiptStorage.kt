package com.dari.finance.receipt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Handles receipt image storage and retrieval
 */
class ReceiptStorage {
    
    // In-memory storage for testing - real implementation would use file system
    private val imageStorage = mutableMapOf<String, ByteArray>()
    private val metadataStorage = mutableMapOf<String, ReceiptMetadata>()
    private val thumbnailStorage = mutableMapOf<String, ByteArray>()
    
    private val maxFileSize = 5 * 1024 * 1024 // 5MB
    private val thumbnailSize = 200 // 200x200 pixels
    private val compressionQuality = 0.8f
    
    /**
     * Stores receipt image with auto-generated metadata
     */
    suspend fun storeReceiptImage(receiptId: String, imageBytes: ByteArray): Result<ReceiptMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                if (imageBytes.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Image bytes cannot be empty"))
                }
                
                // Process and compress image if needed
                val processedImage = processImageForStorage(imageBytes)
                
                // Generate metadata
                val metadata = ReceiptMetadata(
                    id = receiptId,
                    fileName = "$receiptId.jpg",
                    fileSize = processedImage.size.toLong(),
                    mimeType = "image/jpeg",
                    capturedAt = System.currentTimeMillis(),
                    ocrProcessed = false,
                    confidence = 0.0f
                )
                
                // Store image and metadata
                imageStorage[receiptId] = processedImage
                metadataStorage[receiptId] = metadata
                
                // Generate and store thumbnail
                val thumbnail = generateThumbnail(processedImage)
                thumbnailStorage[receiptId] = thumbnail
                
                Result.success(metadata)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Stores receipt with provided metadata
     */
    suspend fun storeReceiptWithMetadata(
        metadata: ReceiptMetadata,
        imageBytes: ByteArray
    ): Result<ReceiptMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val processedImage = processImageForStorage(imageBytes)
                
                // Update metadata with actual file size
                val updatedMetadata = metadata.copy(
                    fileSize = processedImage.size.toLong()
                )
                
                imageStorage[metadata.id] = processedImage
                metadataStorage[metadata.id] = updatedMetadata
                
                val thumbnail = generateThumbnail(processedImage)
                thumbnailStorage[metadata.id] = thumbnail
                
                Result.success(updatedMetadata)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Stores receipt image with thumbnail
     */
    suspend fun storeReceiptImageWithThumbnail(
        receiptId: String,
        imageBytes: ByteArray
    ): Result<ReceiptMetadata> {
        return storeReceiptImage(receiptId, imageBytes)
    }
    
    /**
     * Retrieves receipt image by ID
     */
    suspend fun getReceiptImage(receiptId: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            imageStorage[receiptId]
        }
    }
    
    /**
     * Retrieves receipt thumbnail by ID
     */
    suspend fun getReceiptThumbnail(receiptId: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            thumbnailStorage[receiptId]
        }
    }
    
    /**
     * Retrieves receipt metadata by ID
     */
    suspend fun getReceiptMetadata(receiptId: String): ReceiptMetadata? {
        return withContext(Dispatchers.IO) {
            metadataStorage[receiptId]
        }
    }
    
    /**
     * Deletes receipt and its metadata
     */
    suspend fun deleteReceipt(receiptId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                imageStorage.remove(receiptId)
                metadataStorage.remove(receiptId)
                thumbnailStorage.remove(receiptId)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Gets all receipt metadata
     */
    suspend fun getAllReceiptMetadata(): List<ReceiptMetadata> {
        return withContext(Dispatchers.IO) {
            metadataStorage.values.toList()
        }
    }
    
    /**
     * Stores multiple receipts in batch
     */
    suspend fun storeReceiptsBatch(receipts: List<Pair<String, ByteArray>>): List<Result<ReceiptMetadata>> {
        return withContext(Dispatchers.IO) {
            receipts.map { (receiptId, imageBytes) ->
                storeReceiptImage(receiptId, imageBytes)
            }
        }
    }
    
    /**
     * Gets current storage usage
     */
    suspend fun getStorageUsage(): StorageUsage {
        return withContext(Dispatchers.IO) {
            val totalBytes = imageStorage.values.sumOf { it.size.toLong() } +
                           thumbnailStorage.values.sumOf { it.size.toLong() }
            
            StorageUsage(
                totalBytes = totalBytes,
                receiptCount = imageStorage.size,
                thumbnailBytes = thumbnailStorage.values.sumOf { it.size.toLong() }
            )
        }
    }
    
    /**
     * Generates storage path for receipt
     */
    fun generateStoragePath(receiptId: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        val year = timestamp / (365L * 24 * 60 * 60 * 1000) + 1970
        val month = (timestamp % (365L * 24 * 60 * 60 * 1000)) / (30L * 24 * 60 * 60 * 1000) + 1
        
        return "receipts/$year/$month/$receiptId.$extension"
    }
    
    /**
     * Cleans up old receipts based on retention policy
     */
    suspend fun cleanupOldReceipts(retentionDays: Int = 365): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
                var deletedCount = 0
                
                val toDelete = metadataStorage.filter { (_, metadata) ->
                    metadata.capturedAt < cutoffTime
                }.keys
                
                toDelete.forEach { receiptId ->
                    deleteReceipt(receiptId)
                    deletedCount++
                }
                
                Result.success(deletedCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun processImageForStorage(imageBytes: ByteArray): ByteArray {
        return withContext(Dispatchers.Default) {
            if (imageBytes.size <= maxFileSize) {
                return@withContext imageBytes
            }
            
            // Compress image if it exceeds max size
            compressImage(imageBytes, compressionQuality)
        }
    }
    
    private fun compressImage(imageBytes: ByteArray, quality: Float): ByteArray {
        // Simplified compression - in real implementation would use platform-specific image compression
        val compressionRatio = quality.coerceIn(0.1f, 1.0f)
        val targetSize = (imageBytes.size * compressionRatio).toInt()
        
        return if (targetSize < imageBytes.size) {
            // Simulate compression by taking every nth byte
            val step = imageBytes.size / targetSize
            ByteArray(targetSize) { i ->
                imageBytes[min(i * step, imageBytes.size - 1)]
            }
        } else {
            imageBytes
        }
    }
    
    private fun generateThumbnail(imageBytes: ByteArray): ByteArray {
        // Simplified thumbnail generation - in real implementation would resize image
        val thumbnailTargetSize = min(imageBytes.size / 4, 1024) // Quarter size, max 1KB
        return if (thumbnailTargetSize < imageBytes.size) {
            imageBytes.copyOf(thumbnailTargetSize)
        } else {
            imageBytes
        }
    }
}

/**
 * Receipt metadata model
 */
data class ReceiptMetadata(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val capturedAt: Long,
    val ocrProcessed: Boolean = false,
    val confidence: Float = 0.0f,
    val storageLocation: String? = null,
    val thumbnailLocation: String? = null,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING
)

/**
 * Processing status for receipts
 */
enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REQUIRES_MANUAL_REVIEW
}

/**
 * Storage usage information
 */
data class StorageUsage(
    val totalBytes: Long,
    val receiptCount: Int,
    val thumbnailBytes: Long
) {
    val totalMB: Double get() = totalBytes / (1024.0 * 1024.0)
    val averageReceiptSize: Double get() = if (receiptCount > 0) totalBytes.toDouble() / receiptCount else 0.0
}

/**
 * Storage configuration
 */
data class StorageConfig(
    val maxFileSize: Long = 5 * 1024 * 1024, // 5MB
    val compressionQuality: Float = 0.8f,
    val thumbnailSize: Int = 200,
    val retentionDays: Int = 365,
    val storageLocation: String = "receipts"
)