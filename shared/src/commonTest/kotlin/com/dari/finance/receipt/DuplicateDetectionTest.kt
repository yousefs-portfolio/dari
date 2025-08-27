package com.dari.finance.receipt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

class DuplicateDetectionTest {

    @Test
    fun `should detect exact duplicate receipts`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val originalReceipt = createMockProcessedReceipt(
            merchantName = "Starbucks",
            amount = 5.99,
            date = "2023-12-01",
            items = listOf("Coffee", "Pastry")
        )
        val duplicateReceipt = originalReceipt.copy(
            id = "different_id"
        )
        val existingReceipts = listOf(originalReceipt)
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(duplicateReceipt, existingReceipts)
        
        // Then
        assertTrue(duplicateResult.isDuplicate)
        assertEquals(1, duplicateResult.potentialDuplicates.size)
        assertEquals(originalReceipt.id, duplicateResult.potentialDuplicates.first().receipt.id)
        assertTrue(duplicateResult.confidence > 0.95f)
    }

    @Test
    fun `should detect near duplicate with slight amount difference`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val originalReceipt = createMockProcessedReceipt(
            merchantName = "McDonald's",
            amount = 8.99,
            date = "2023-12-01"
        )
        val nearDuplicateReceipt = createMockProcessedReceipt(
            merchantName = "McDonald's",
            amount = 9.03, // Slight difference due to tax calculation
            date = "2023-12-01"
        )
        val existingReceipts = listOf(originalReceipt)
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(nearDuplicateReceipt, existingReceipts)
        
        // Then
        assertTrue(duplicateResult.isDuplicate)
        assertTrue(duplicateResult.confidence > 0.85f)
        assertEquals(DuplicateReason.SIMILAR_AMOUNT_AND_MERCHANT, duplicateResult.primaryReason)
    }

    @Test
    fun `should detect duplicates with time difference within tolerance`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val originalReceipt = createMockProcessedReceipt(
            merchantName = "Target",
            amount = 45.67,
            date = "2023-12-01"
        )
        val timeShiftedReceipt = createMockProcessedReceipt(
            merchantName = "Target",
            amount = 45.67,
            date = "2023-12-02" // Next day
        )
        val existingReceipts = listOf(originalReceipt)
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(timeShiftedReceipt, existingReceipts)
        
        // Then
        assertTrue(duplicateResult.isDuplicate)
        assertTrue(duplicateResult.confidence > 0.8f)
        assertTrue(duplicateResult.reasons.contains(DuplicateReason.TIME_TOLERANCE))
    }

    @Test
    fun `should not detect duplicates for different merchants`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val receipt1 = createMockProcessedReceipt(
            merchantName = "Starbucks",
            amount = 5.99,
            date = "2023-12-01"
        )
        val receipt2 = createMockProcessedReceipt(
            merchantName = "Dunkin' Donuts",
            amount = 5.99,
            date = "2023-12-01"
        )
        val existingReceipts = listOf(receipt1)
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(receipt2, existingReceipts)
        
        // Then
        assertFalse(duplicateResult.isDuplicate)
    }

    @Test
    fun `should not detect duplicates for significantly different amounts`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val receipt1 = createMockProcessedReceipt(
            merchantName = "Walmart",
            amount = 25.00,
            date = "2023-12-01"
        )
        val receipt2 = createMockProcessedReceipt(
            merchantName = "Walmart",
            amount = 125.00, // 5x difference
            date = "2023-12-01"
        )
        val existingReceipts = listOf(receipt1)
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(receipt2, existingReceipts)
        
        // Then
        assertFalse(duplicateResult.isDuplicate)
    }

    @Test
    fun `should detect duplicates based on item similarity`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val originalReceipt = createMockProcessedReceipt(
            merchantName = "Grocery Store",
            amount = 35.67,
            date = "2023-12-01",
            items = listOf("Milk", "Bread", "Eggs", "Bananas")
        )
        val duplicateWithSimilarItems = createMockProcessedReceipt(
            merchantName = "Grocery Store",
            amount = 35.89, // Slight difference
            date = "2023-12-01",
            items = listOf("Milk", "Bread", "Eggs", "Bananas", "Apples") // One extra item
        )
        val existingReceipts = listOf(originalReceipt)
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(duplicateWithSimilarItems, existingReceipts)
        
        // Then
        assertTrue(duplicateResult.isDuplicate)
        assertTrue(duplicateResult.reasons.contains(DuplicateReason.SIMILAR_ITEMS))
    }

    @Test
    fun `should calculate image similarity for visual duplicates`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val originalImageHash = "abcd1234567890ef"
        val similarImageHash = "abcd1234567890ee" // Very similar hash
        val differentImageHash = "1234567890abcdef" // Different hash
        
        // When
        val similarityScore1 = duplicateDetector.calculateImageSimilarity(originalImageHash, similarImageHash)
        val similarityScore2 = duplicateDetector.calculateImageSimilarity(originalImageHash, differentImageHash)
        
        // Then
        assertTrue(similarityScore1 > 0.9f)
        assertTrue(similarityScore2 < 0.5f)
    }

    @Test
    fun `should rank potential duplicates by confidence`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val newReceipt = createMockProcessedReceipt(
            merchantName = "Best Buy",
            amount = 199.99,
            date = "2023-12-01"
        )
        val existingReceipts = listOf(
            createMockProcessedReceipt(
                merchantName = "Best Buy",
                amount = 199.99,
                date = "2023-12-01"
            ), // Exact match - highest confidence
            createMockProcessedReceipt(
                merchantName = "Best Buy",
                amount = 199.50,
                date = "2023-12-01"
            ), // Close match - medium confidence
            createMockProcessedReceipt(
                merchantName = "Best Buy Electronics",
                amount = 200.99,
                date = "2023-12-02"
            ) // Similar match - lower confidence
        )
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(newReceipt, existingReceipts)
        
        // Then
        assertTrue(duplicateResult.isDuplicate)
        assertTrue(duplicateResult.potentialDuplicates.size >= 2)
        // Should be sorted by confidence descending
        val confidences = duplicateResult.potentialDuplicates.map { it.confidence }
        assertEquals(confidences, confidences.sortedDescending())
    }

    @Test
    fun `should handle batch duplicate detection`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val existingReceipts = listOf(
            createMockProcessedReceipt("Starbucks", 5.99, "2023-12-01"),
            createMockProcessedReceipt("McDonald's", 8.50, "2023-12-01"),
            createMockProcessedReceipt("Target", 45.67, "2023-12-01")
        )
        val newReceipts = listOf(
            createMockProcessedReceipt("Starbucks", 5.99, "2023-12-01"), // Duplicate
            createMockProcessedReceipt("Subway", 12.34, "2023-12-01"), // Not duplicate
            createMockProcessedReceipt("McDonald's", 8.55, "2023-12-01") // Near duplicate
        )
        
        // When
        val batchResults = duplicateDetector.detectDuplicatesInBatch(newReceipts, existingReceipts)
        
        // Then
        assertEquals(3, batchResults.size)
        assertTrue(batchResults[0].isDuplicate) // Starbucks duplicate
        assertFalse(batchResults[1].isDuplicate) // Subway not duplicate
        assertTrue(batchResults[2].isDuplicate) // McDonald's near duplicate
    }

    @Test
    fun `should provide detailed duplicate analysis`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val originalReceipt = createMockProcessedReceipt(
            merchantName = "Apple Store",
            amount = 1299.00,
            date = "2023-12-01",
            items = listOf("iPhone 15", "Case", "Screen Protector")
        )
        val suspectedDuplicate = createMockProcessedReceipt(
            merchantName = "Apple Store Online",
            amount = 1299.00,
            date = "2023-12-01",
            items = listOf("iPhone 15", "Protective Case", "Screen Protector")
        )
        
        // When
        val analysis = duplicateDetector.analyzeDuplicate(suspectedDuplicate, originalReceipt)
        
        // Then
        assertNotNull(analysis)
        assertTrue(analysis.overallSimilarity > 0.8f)
        assertTrue(analysis.merchantSimilarity > 0.8f)
        assertTrue(analysis.amountSimilarity > 0.95f)
        assertTrue(analysis.itemsSimilarity > 0.8f)
    }

    @Test
    fun `should handle edge cases gracefully`() = runBlocking {
        // Given
        val duplicateDetector = DuplicateReceiptDetector()
        val receiptWithMissingData = ProcessedReceipt(
            id = "incomplete",
            merchantName = "",
            totalAmount = 0.0,
            transactionDate = "",
            items = emptyList(),
            confidence = 0.3f
        )
        val existingReceipts = listOf(
            createMockProcessedReceipt("Test Store", 10.00, "2023-12-01")
        )
        
        // When
        val duplicateResult = duplicateDetector.checkForDuplicates(receiptWithMissingData, existingReceipts)
        
        // Then
        assertFalse(duplicateResult.isDuplicate) // Should not match due to insufficient data
    }

    private fun createMockProcessedReceipt(
        merchantName: String,
        amount: Double,
        date: String,
        items: List<String> = emptyList()
    ): ProcessedReceipt {
        return ProcessedReceipt(
            id = "receipt_${(1000..9999).random()}",
            merchantName = merchantName,
            totalAmount = amount,
            transactionDate = date,
            items = items,
            confidence = 0.9f
        )
    }
}