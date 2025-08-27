package com.dari.finance.receipt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class ReceiptTransactionMatchingTest {

    @Test
    fun `should match receipt to transaction by exact amount and date`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 45.67,
            merchantName = "Starbucks",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 45.67,
                description = "STARBUCKS COFFEE",
                date = "2023-12-01"
            ),
            createMockTransaction(
                amount = 50.00,
                description = "TARGET STORE",
                date = "2023-12-01"
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNotNull(match)
        assertEquals(45.67, match.transaction.amount)
        assertTrue(match.confidence > 0.9f)
        assertEquals(MatchReason.EXACT_AMOUNT_AND_DATE, match.primaryReason)
    }

    @Test
    fun `should match receipt to transaction by merchant name similarity`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 25.99,
            merchantName = "McDonald's Restaurant",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 25.99,
                description = "MCDONALDS #1234",
                date = "2023-12-01"
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNotNull(match)
        assertEquals("MCDONALDS #1234", match.transaction.description)
        assertTrue(match.confidence > 0.8f)
        assertEquals(MatchReason.MERCHANT_SIMILARITY, match.primaryReason)
    }

    @Test
    fun `should match receipt with slight amount difference`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 19.99,
            merchantName = "Target",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 20.45, // Includes tax/tip
                description = "TARGET T-1234",
                date = "2023-12-01"
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNotNull(match)
        assertEquals(20.45, match.transaction.amount)
        assertTrue(match.confidence > 0.7f)
        assertTrue(match.reasons.contains(MatchReason.APPROXIMATE_AMOUNT))
    }

    @Test
    fun `should match receipt within date tolerance`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 15.50,
            merchantName = "Shell Gas Station",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 15.50,
                description = "SHELL OIL",
                date = "2023-12-02" // Next day
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNotNull(match)
        assertTrue(match.confidence > 0.6f)
        assertTrue(match.reasons.contains(MatchReason.DATE_TOLERANCE))
    }

    @Test
    fun `should not match with significant amount difference`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 100.00,
            merchantName = "Best Buy",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 200.00, // Too different
                description = "BEST BUY STORE",
                date = "2023-12-01"
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNull(match) // Should not match due to large amount difference
    }

    @Test
    fun `should not match outside date tolerance`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 30.00,
            merchantName = "Walmart",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 30.00,
                description = "WALMART SUPERCENTER",
                date = "2023-11-25" // Too far in past
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNull(match) // Should not match due to date difference
    }

    @Test
    fun `should prefer higher confidence matches`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 12.34,
            merchantName = "Subway",
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 12.50, // Close but not exact
                description = "FAST FOOD RESTAURANT",
                date = "2023-12-01"
            ),
            createMockTransaction(
                amount = 12.34, // Exact match
                description = "SUBWAY #5678",
                date = "2023-12-01"
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNotNull(match)
        assertEquals(12.34, match.transaction.amount)
        assertEquals("SUBWAY #5678", match.transaction.description)
    }

    @Test
    fun `should match multiple receipts to transactions in batch`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipts = listOf(
            createMockReceipt(25.00, "Starbucks", "2023-12-01"),
            createMockReceipt(50.00, "Target", "2023-12-01"),
            createMockReceipt(15.99, "McDonald's", "2023-12-02")
        )
        val transactions = listOf(
            createMockTransaction(25.00, "STARBUCKS", "2023-12-01"),
            createMockTransaction(50.00, "TARGET STORE", "2023-12-01"),
            createMockTransaction(15.99, "MCDONALDS", "2023-12-02")
        )
        
        // When
        val matches = matcher.matchReceiptsToTransactions(receipts, transactions)
        
        // Then
        assertEquals(3, matches.size)
        matches.forEach { match ->
            assertTrue(match.confidence > 0.8f)
        }
    }

    @Test
    fun `should handle partial receipt information`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 35.00,
            merchantName = "", // No merchant name
            date = "2023-12-01"
        )
        val transactions = listOf(
            createMockTransaction(
                amount = 35.00,
                description = "POS PURCHASE",
                date = "2023-12-01"
            )
        )
        
        // When
        val match = matcher.findBestMatch(receipt, transactions)
        
        // Then
        assertNotNull(match)
        assertTrue(match.confidence > 0.5f) // Lower confidence due to missing info
        assertEquals(MatchReason.AMOUNT_AND_DATE_ONLY, match.primaryReason)
    }

    @Test
    fun `should calculate match confidence correctly`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 42.50,
            merchantName = "Apple Store",
            date = "2023-12-01"
        )
        val transaction = createMockTransaction(
            amount = 42.50,
            description = "APPLE STORE ONLINE",
            date = "2023-12-01"
        )
        
        // When
        val confidence = matcher.calculateMatchConfidence(receipt, transaction)
        
        // Then
        assertTrue(confidence > 0.9f) // High confidence for exact match
    }

    @Test
    fun `should identify fuzzy merchant name matches`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        
        val testCases = listOf(
            "McDonald's" to "MCDONALDS #1234" to true,
            "Starbucks Coffee" to "STARBUCKS STORE #567" to true,
            "Target Store" to "TARGET T-1234" to true,
            "Best Buy" to "BESTBUY.COM" to true,
            "Walmart" to "SHELL GAS STATION" to false
        )
        
        // When & Then
        testCases.forEach { (receiptMerchant, transactionDesc, shouldMatch) ->
            val similarity = matcher.calculateMerchantSimilarity(receiptMerchant, transactionDesc)
            if (shouldMatch) {
                assertTrue(similarity > 0.6f, "Should match: $receiptMerchant -> $transactionDesc")
            } else {
                assertTrue(similarity < 0.4f, "Should not match: $receiptMerchant -> $transactionDesc")
            }
        }
    }

    @Test
    fun `should handle currency and formatting differences`() = runBlocking {
        // Given
        val matcher = ReceiptTransactionMatcher()
        val receipt = createMockReceipt(
            amount = 29.99,
            merchantName = "Amazon",
            date = "2023-12-01"
        )
        val transaction = createMockTransaction(
            amount = 29.99,
            description = "AMAZON.COM AMZN.COM/BILL",
            date = "2023-12-01"
        )
        
        // When
        val match = matcher.findBestMatch(receipt, listOf(transaction))
        
        // Then
        assertNotNull(match)
        assertTrue(match.confidence > 0.8f)
    }

    private fun createMockReceipt(
        amount: Double,
        merchantName: String,
        date: String
    ): ProcessedReceipt {
        return ProcessedReceipt(
            id = "receipt_${(1000..9999).random()}",
            merchantName = merchantName,
            totalAmount = amount,
            transactionDate = date,
            items = emptyList(),
            confidence = 0.9f
        )
    }

    private fun createMockTransaction(
        amount: Double,
        description: String,
        date: String
    ): Transaction {
        return Transaction(
            id = "txn_${(1000..9999).random()}",
            amount = amount,
            description = description,
            date = date,
            accountId = "account_123",
            category = "Uncategorized"
        )
    }
}

// Mock data classes for testing
data class ProcessedReceipt(
    val id: String,
    val merchantName: String,
    val totalAmount: Double,
    val transactionDate: String,
    val items: List<String>,
    val confidence: Float
)

data class Transaction(
    val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    val accountId: String,
    val category: String
)