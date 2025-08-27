package com.dari.finance.receipt

import kotlin.math.*

/**
 * Detects duplicate receipts based on multiple criteria
 */
class DuplicateReceiptDetector {
    
    private val amountTolerance = 0.05 // 5% tolerance for amount differences
    private val timeTolerance = 2 // Days tolerance for time differences
    private val minimumConfidence = 0.7f
    private val itemsSimilarityThreshold = 0.8f
    private val merchantSimilarityThreshold = 0.8f
    
    /**
     * Checks if a receipt is a duplicate of existing receipts
     */
    suspend fun checkForDuplicates(
        receipt: ProcessedReceipt,
        existingReceipts: List<ProcessedReceipt>
    ): DuplicateDetectionResult {
        val potentialDuplicates = mutableListOf<PotentialDuplicate>()
        
        for (existingReceipt in existingReceipts) {
            if (existingReceipt.id == receipt.id) continue // Skip self
            
            val duplicateAnalysis = analyzeDuplicate(receipt, existingReceipt)
            
            if (duplicateAnalysis.overallSimilarity >= minimumConfidence) {
                val reasons = identifyDuplicateReasons(duplicateAnalysis)
                potentialDuplicates.add(
                    PotentialDuplicate(
                        receipt = existingReceipt,
                        confidence = duplicateAnalysis.overallSimilarity,
                        reasons = reasons,
                        analysis = duplicateAnalysis
                    )
                )
            }
        }
        
        // Sort by confidence descending
        potentialDuplicates.sortByDescending { it.confidence }
        
        val isDuplicate = potentialDuplicates.isNotEmpty()
        val primaryReason = if (isDuplicate) {
            potentialDuplicates.first().reasons.maxByOrNull { it.weight } ?: DuplicateReason.UNKNOWN
        } else {
            DuplicateReason.NOT_DUPLICATE
        }
        
        return DuplicateDetectionResult(
            isDuplicate = isDuplicate,
            confidence = potentialDuplicates.firstOrNull()?.confidence ?: 0.0f,
            potentialDuplicates = potentialDuplicates,
            primaryReason = primaryReason,
            reasons = potentialDuplicates.firstOrNull()?.reasons ?: emptyList()
        )
    }
    
    /**
     * Detects duplicates in a batch of receipts
     */
    suspend fun detectDuplicatesInBatch(
        newReceipts: List<ProcessedReceipt>,
        existingReceipts: List<ProcessedReceipt>
    ): List<DuplicateDetectionResult> {
        return newReceipts.map { receipt ->
            checkForDuplicates(receipt, existingReceipts)
        }
    }
    
    /**
     * Analyzes similarity between two receipts
     */
    fun analyzeDuplicate(
        receipt1: ProcessedReceipt,
        receipt2: ProcessedReceipt
    ): DuplicateAnalysis {
        val merchantSimilarity = calculateMerchantSimilarity(receipt1.merchantName, receipt2.merchantName)
        val amountSimilarity = calculateAmountSimilarity(receipt1.totalAmount, receipt2.totalAmount)
        val dateSimilarity = calculateDateSimilarity(receipt1.transactionDate, receipt2.transactionDate)
        val itemsSimilarity = calculateItemsSimilarity(receipt1.items, receipt2.items)
        val confidenceFactor = min(receipt1.confidence, receipt2.confidence)
        
        // Calculate overall similarity with weighted factors
        val overallSimilarity = (
            merchantSimilarity * 0.3f +
            amountSimilarity * 0.3f +
            dateSimilarity * 0.2f +
            itemsSimilarity * 0.15f +
            confidenceFactor * 0.05f
        ).coerceIn(0.0f, 1.0f)
        
        return DuplicateAnalysis(
            merchantSimilarity = merchantSimilarity,
            amountSimilarity = amountSimilarity,
            dateSimilarity = dateSimilarity,
            itemsSimilarity = itemsSimilarity,
            overallSimilarity = overallSimilarity,
            confidenceFactor = confidenceFactor
        )
    }
    
    /**
     * Calculates image similarity using hash comparison
     */
    fun calculateImageSimilarity(hash1: String, hash2: String): Float {
        if (hash1.isBlank() || hash2.isBlank() || hash1.length != hash2.length) {
            return 0.0f
        }
        
        var matchingBits = 0
        for (i in hash1.indices) {
            if (hash1[i] == hash2[i]) {
                matchingBits++
            }
        }
        
        return matchingBits.toFloat() / hash1.length.toFloat()
    }
    
    private fun calculateMerchantSimilarity(merchant1: String, merchant2: String): Float {
        if (merchant1.isBlank() && merchant2.isBlank()) return 1.0f
        if (merchant1.isBlank() || merchant2.isBlank()) return 0.0f
        
        val clean1 = cleanMerchantName(merchant1)
        val clean2 = cleanMerchantName(merchant2)
        
        // Exact match
        if (clean1.equals(clean2, ignoreCase = true)) {
            return 1.0f
        }
        
        // Substring match
        if (clean1.contains(clean2, ignoreCase = true) || clean2.contains(clean1, ignoreCase = true)) {
            return 0.9f
        }
        
        // Fuzzy matching using Jaccard similarity
        return calculateJaccardSimilarity(clean1, clean2)
    }
    
    private fun calculateAmountSimilarity(amount1: Double, amount2: Double): Float {
        if (amount1 <= 0 || amount2 <= 0) return 0.0f
        
        val difference = abs(amount1 - amount2)
        val average = (amount1 + amount2) / 2.0
        val percentageDifference = difference / average
        
        return when {
            difference < 0.01 -> 1.0f // Exact match
            percentageDifference <= amountTolerance -> {
                1.0f - (percentageDifference / amountTolerance).toFloat()
            }
            else -> 0.0f
        }
    }
    
    private fun calculateDateSimilarity(date1: String, date2: String): Float {
        if (date1.isBlank() || date2.isBlank()) return 0.0f
        
        val daysDifference = calculateDaysDifference(date1, date2)
        
        return when {
            daysDifference == 0 -> 1.0f
            daysDifference <= timeTolerance -> {
                1.0f - (daysDifference.toFloat() / timeTolerance.toFloat())
            }
            else -> 0.0f
        }
    }
    
    private fun calculateItemsSimilarity(items1: List<String>, items2: List<String>): Float {
        if (items1.isEmpty() && items2.isEmpty()) return 1.0f
        if (items1.isEmpty() || items2.isEmpty()) return 0.0f
        
        val cleanItems1 = items1.map { it.lowercase().trim() }.toSet()
        val cleanItems2 = items2.map { it.lowercase().trim() }.toSet()
        
        val intersection = cleanItems1.intersect(cleanItems2)
        val union = cleanItems1.union(cleanItems2)
        
        return if (union.isNotEmpty()) {
            intersection.size.toFloat() / union.size.toFloat()
        } else {
            0.0f
        }
    }
    
    private fun identifyDuplicateReasons(analysis: DuplicateAnalysis): List<DuplicateReason> {
        val reasons = mutableListOf<DuplicateReason>()
        
        if (analysis.merchantSimilarity > 0.95f && analysis.amountSimilarity > 0.95f && analysis.dateSimilarity > 0.95f) {
            reasons.add(DuplicateReason.EXACT_MATCH)
        }
        
        if (analysis.merchantSimilarity > merchantSimilarityThreshold && analysis.amountSimilarity > 0.9f) {
            reasons.add(DuplicateReason.SIMILAR_AMOUNT_AND_MERCHANT)
        }
        
        if (analysis.itemsSimilarity > itemsSimilarityThreshold) {
            reasons.add(DuplicateReason.SIMILAR_ITEMS)
        }
        
        if (analysis.dateSimilarity < 1.0f && analysis.dateSimilarity > 0.5f) {
            reasons.add(DuplicateReason.TIME_TOLERANCE)
        }
        
        if (analysis.amountSimilarity > 0.8f && analysis.amountSimilarity < 1.0f) {
            reasons.add(DuplicateReason.SIMILAR_AMOUNT)
        }
        
        if (analysis.merchantSimilarity > 0.8f && analysis.merchantSimilarity < 1.0f) {
            reasons.add(DuplicateReason.SIMILAR_MERCHANT)
        }
        
        if (reasons.isEmpty()) {
            reasons.add(DuplicateReason.UNKNOWN)
        }
        
        return reasons
    }
    
    private fun cleanMerchantName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun calculateJaccardSimilarity(str1: String, str2: String): Float {
        val words1 = str1.split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = str2.split(" ").filter { it.isNotBlank() }.toSet()
        
        if (words1.isEmpty() && words2.isEmpty()) return 1.0f
        if (words1.isEmpty() || words2.isEmpty()) return 0.0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toFloat() / union.toFloat()
    }
    
    private fun calculateDaysDifference(date1: String, date2: String): Int {
        return try {
            val parts1 = date1.split("-")
            val parts2 = date2.split("-")
            
            if (parts1.size == 3 && parts2.size == 3) {
                val year1 = parts1[0].toInt()
                val month1 = parts1[1].toInt()
                val day1 = parts1[2].toInt()
                
                val year2 = parts2[0].toInt()
                val month2 = parts2[1].toInt()
                val day2 = parts2[2].toInt()
                
                // Simplified calculation
                val dayOfYear1 = month1 * 30 + day1 + year1 * 365
                val dayOfYear2 = month2 * 30 + day2 + year2 * 365
                
                abs(dayOfYear1 - dayOfYear2)
            } else {
                Int.MAX_VALUE
            }
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }
}

/**
 * Result of duplicate detection
 */
data class DuplicateDetectionResult(
    val isDuplicate: Boolean,
    val confidence: Float,
    val potentialDuplicates: List<PotentialDuplicate>,
    val primaryReason: DuplicateReason,
    val reasons: List<DuplicateReason>
)

/**
 * A potential duplicate receipt
 */
data class PotentialDuplicate(
    val receipt: ProcessedReceipt,
    val confidence: Float,
    val reasons: List<DuplicateReason>,
    val analysis: DuplicateAnalysis
)

/**
 * Detailed analysis of duplicate comparison
 */
data class DuplicateAnalysis(
    val merchantSimilarity: Float,
    val amountSimilarity: Float,
    val dateSimilarity: Float,
    val itemsSimilarity: Float,
    val overallSimilarity: Float,
    val confidenceFactor: Float
)

/**
 * Reasons for duplicate detection
 */
enum class DuplicateReason(val weight: Float, val description: String) {
    EXACT_MATCH(1.0f, "Exact match in all fields"),
    SIMILAR_AMOUNT_AND_MERCHANT(0.9f, "Similar amount and merchant"),
    SIMILAR_ITEMS(0.85f, "Similar items purchased"),
    TIME_TOLERANCE(0.8f, "Within time tolerance"),
    SIMILAR_AMOUNT(0.75f, "Similar transaction amount"),
    SIMILAR_MERCHANT(0.7f, "Similar merchant name"),
    IMAGE_SIMILARITY(0.65f, "Similar receipt images"),
    UNKNOWN(0.1f, "Unknown similarity criteria"),
    NOT_DUPLICATE(0.0f, "Not a duplicate")
}

/**
 * Configuration for duplicate detection
 */
data class DuplicateDetectionConfig(
    val amountTolerance: Double = 0.05,
    val timeTolerance: Int = 2,
    val minimumConfidence: Float = 0.7f,
    val itemsSimilarityThreshold: Float = 0.8f,
    val merchantSimilarityThreshold: Float = 0.8f,
    val enableImageComparison: Boolean = false
)