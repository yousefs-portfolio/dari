package com.dari.finance.receipt

import kotlin.math.*

/**
 * Matches receipts to bank transactions automatically
 */
class ReceiptTransactionMatcher {
    
    private val dateTolerance = 3 // Days
    private val amountTolerance = 0.15 // 15% tolerance
    private val minimumConfidence = 0.5f
    private val merchantSimilarityThreshold = 0.6f
    
    /**
     * Finds the best matching transaction for a receipt
     */
    suspend fun findBestMatch(
        receipt: ProcessedReceipt,
        transactions: List<Transaction>
    ): TransactionMatch? {
        val potentialMatches = transactions.mapNotNull { transaction ->
            val confidence = calculateMatchConfidence(receipt, transaction)
            if (confidence >= minimumConfidence) {
                val reasons = identifyMatchReasons(receipt, transaction)
                TransactionMatch(
                    receipt = receipt,
                    transaction = transaction,
                    confidence = confidence,
                    reasons = reasons,
                    primaryReason = reasons.maxByOrNull { it.weight } ?: MatchReason.UNKNOWN
                )
            } else null
        }
        
        return potentialMatches.maxByOrNull { it.confidence }
    }
    
    /**
     * Matches multiple receipts to transactions in batch
     */
    suspend fun matchReceiptsToTransactions(
        receipts: List<ProcessedReceipt>,
        transactions: List<Transaction>
    ): List<TransactionMatch> {
        val matches = mutableListOf<TransactionMatch>()
        val availableTransactions = transactions.toMutableList()
        
        // Sort receipts by confidence to prioritize high-confidence matches
        val sortedReceipts = receipts.sortedByDescending { it.confidence }
        
        for (receipt in sortedReceipts) {
            val match = findBestMatch(receipt, availableTransactions)
            if (match != null) {
                matches.add(match)
                // Remove matched transaction to avoid duplicate matches
                availableTransactions.remove(match.transaction)
            }
        }
        
        return matches
    }
    
    /**
     * Calculates confidence score for receipt-transaction match
     */
    fun calculateMatchConfidence(
        receipt: ProcessedReceipt,
        transaction: Transaction
    ): Float {
        var confidence = 0.0f
        var totalWeight = 0.0f
        
        // Amount similarity (weight: 0.4)
        val amountWeight = 0.4f
        val amountSimilarity = calculateAmountSimilarity(receipt.totalAmount, transaction.amount)
        confidence += amountSimilarity * amountWeight
        totalWeight += amountWeight
        
        // Date similarity (weight: 0.3)
        val dateWeight = 0.3f
        val dateSimilarity = calculateDateSimilarity(receipt.transactionDate, transaction.date)
        confidence += dateSimilarity * dateWeight
        totalWeight += dateWeight
        
        // Merchant similarity (weight: 0.25)
        if (receipt.merchantName.isNotBlank()) {
            val merchantWeight = 0.25f
            val merchantSimilarity = calculateMerchantSimilarity(receipt.merchantName, transaction.description)
            confidence += merchantSimilarity * merchantWeight
            totalWeight += merchantWeight
        }
        
        // Receipt confidence bonus (weight: 0.05)
        val receiptConfidenceWeight = 0.05f
        confidence += receipt.confidence * receiptConfidenceWeight
        totalWeight += receiptConfidenceWeight
        
        return if (totalWeight > 0) confidence / totalWeight else 0.0f
    }
    
    /**
     * Calculates similarity between amounts
     */
    private fun calculateAmountSimilarity(receiptAmount: Double, transactionAmount: Double): Float {
        if (receiptAmount == 0.0 || transactionAmount == 0.0) return 0.0f
        
        val difference = abs(receiptAmount - transactionAmount)
        val average = (receiptAmount + transactionAmount) / 2.0
        val percentageDifference = difference / average
        
        return when {
            difference < 0.01 -> 1.0f // Exact match (accounting for floating point)
            percentageDifference <= amountTolerance -> {
                // Gradual decrease in similarity as difference increases
                (1.0f - (percentageDifference / amountTolerance).toFloat()).coerceAtLeast(0.0f)
            }
            else -> 0.0f
        }
    }
    
    /**
     * Calculates similarity between dates
     */
    private fun calculateDateSimilarity(receiptDate: String, transactionDate: String): Float {
        val daysDifference = calculateDaysDifference(receiptDate, transactionDate)
        
        return when {
            daysDifference == 0 -> 1.0f // Same day
            daysDifference <= dateTolerance -> {
                // Gradual decrease in similarity as days increase
                1.0f - (daysDifference.toFloat() / dateTolerance.toFloat())
            }
            else -> 0.0f
        }
    }
    
    /**
     * Calculates similarity between merchant name and transaction description
     */
    fun calculateMerchantSimilarity(merchantName: String, transactionDescription: String): Float {
        if (merchantName.isBlank() || transactionDescription.isBlank()) return 0.0f
        
        val cleanMerchant = cleanMerchantName(merchantName)
        val cleanTransaction = cleanMerchantName(transactionDescription)
        
        // Check for exact substring match
        if (cleanTransaction.contains(cleanMerchant, ignoreCase = true) ||
            cleanMerchant.contains(cleanTransaction, ignoreCase = true)) {
            return 0.95f
        }
        
        // Check common merchant patterns
        val merchantPatterns = mapOf(
            "mcdonald" to listOf("mcdonald", "mcdonalds"),
            "starbucks" to listOf("starbucks", "sbux"),
            "walmart" to listOf("walmart", "wal-mart", "wm supercenter"),
            "target" to listOf("target", "tgt"),
            "amazon" to listOf("amazon", "amzn", "amazon.com"),
            "apple" to listOf("apple", "apl", "apple store"),
            "google" to listOf("google", "googl", "goog"),
            "shell" to listOf("shell", "shell oil"),
            "exxon" to listOf("exxon", "exxonmobil", "esso"),
            "chevron" to listOf("chevron", "texaco")
        )
        
        val merchantKey = findMerchantKey(cleanMerchant, merchantPatterns)
        val transactionKey = findMerchantKey(cleanTransaction, merchantPatterns)
        
        if (merchantKey != null && merchantKey == transactionKey) {
            return 0.9f
        }
        
        // Fuzzy string matching using Levenshtein distance
        return calculateLevenshteinSimilarity(cleanMerchant, cleanTransaction)
    }
    
    /**
     * Identifies the reasons for a match
     */
    private fun identifyMatchReasons(
        receipt: ProcessedReceipt,
        transaction: Transaction
    ): List<MatchReason> {
        val reasons = mutableListOf<MatchReason>()
        
        val amountSimilarity = calculateAmountSimilarity(receipt.totalAmount, transaction.amount)
        val dateSimilarity = calculateDateSimilarity(receipt.transactionDate, transaction.date)
        val merchantSimilarity = calculateMerchantSimilarity(receipt.merchantName, transaction.description)
        
        // Exact matches
        if (abs(receipt.totalAmount - transaction.amount) < 0.01 && dateSimilarity == 1.0f) {
            reasons.add(MatchReason.EXACT_AMOUNT_AND_DATE)
        }
        
        // High similarity matches
        if (merchantSimilarity > merchantSimilarityThreshold) {
            reasons.add(MatchReason.MERCHANT_SIMILARITY)
        }
        
        if (amountSimilarity > 0.8f && amountSimilarity < 1.0f) {
            reasons.add(MatchReason.APPROXIMATE_AMOUNT)
        }
        
        if (dateSimilarity < 1.0f && dateSimilarity > 0.0f) {
            reasons.add(MatchReason.DATE_TOLERANCE)
        }
        
        if (receipt.merchantName.isBlank() && amountSimilarity > 0.9f && dateSimilarity > 0.8f) {
            reasons.add(MatchReason.AMOUNT_AND_DATE_ONLY)
        }
        
        if (reasons.isEmpty()) {
            reasons.add(MatchReason.UNKNOWN)
        }
        
        return reasons
    }
    
    private fun cleanMerchantName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun findMerchantKey(
        cleanName: String,
        patterns: Map<String, List<String>>
    ): String? {
        return patterns.entries.find { (_, variations) ->
            variations.any { variation ->
                cleanName.contains(variation, ignoreCase = true) ||
                variation.contains(cleanName, ignoreCase = true)
            }
        }?.key
    }
    
    private fun calculateLevenshteinSimilarity(str1: String, str2: String): Float {
        val maxLength = maxOf(str1.length, str2.length)
        if (maxLength == 0) return 1.0f
        
        val distance = calculateLevenshteinDistance(str1, str2)
        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }
    
    private fun calculateLevenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return matrix[len1][len2]
    }
    
    private fun calculateDaysDifference(date1: String, date2: String): Int {
        // Simplified date parsing - in real implementation would use proper date parsing
        try {
            val parts1 = date1.split("-")
            val parts2 = date2.split("-")
            
            if (parts1.size == 3 && parts2.size == 3) {
                val year1 = parts1[0].toInt()
                val month1 = parts1[1].toInt()
                val day1 = parts1[2].toInt()
                
                val year2 = parts2[0].toInt()
                val month2 = parts2[1].toInt()
                val day2 = parts2[2].toInt()
                
                // Simplified calculation - in real implementation would use proper date library
                val dayOfYear1 = month1 * 30 + day1 + year1 * 365
                val dayOfYear2 = month2 * 30 + day2 + year2 * 365
                
                return abs(dayOfYear1 - dayOfYear2)
            }
        } catch (e: Exception) {
            // Parsing failed
        }
        
        return Int.MAX_VALUE // Unable to parse dates
    }
}

/**
 * Result of matching a receipt to a transaction
 */
data class TransactionMatch(
    val receipt: ProcessedReceipt,
    val transaction: Transaction,
    val confidence: Float,
    val reasons: List<MatchReason>,
    val primaryReason: MatchReason,
    val matchedAt: Long = System.currentTimeMillis()
)

/**
 * Reasons why a receipt was matched to a transaction
 */
enum class MatchReason(val weight: Float, val description: String) {
    EXACT_AMOUNT_AND_DATE(1.0f, "Exact amount and date match"),
    MERCHANT_SIMILARITY(0.9f, "Merchant name similarity"),
    APPROXIMATE_AMOUNT(0.8f, "Amount within tolerance"),
    DATE_TOLERANCE(0.7f, "Date within tolerance"),
    AMOUNT_AND_DATE_ONLY(0.6f, "Amount and date match (no merchant)"),
    UNKNOWN(0.1f, "Unknown matching criteria")
}

/**
 * Configuration for matching algorithm
 */
data class MatchingConfig(
    val dateTolerance: Int = 3,
    val amountTolerance: Double = 0.15,
    val minimumConfidence: Float = 0.5f,
    val merchantSimilarityThreshold: Float = 0.6f,
    val allowDuplicateMatches: Boolean = false
)