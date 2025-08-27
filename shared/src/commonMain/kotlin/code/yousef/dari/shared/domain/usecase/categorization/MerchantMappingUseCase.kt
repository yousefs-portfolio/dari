package code.yousef.dari.shared.domain.usecase.categorization

import code.yousef.dari.shared.domain.model.CategorySuggestion
import code.yousef.dari.shared.domain.model.MappingSource
import code.yousef.dari.shared.domain.model.MerchantFeedback
import code.yousef.dari.shared.domain.model.MerchantMapping
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.repository.MerchantMappingRepository
import kotlinx.datetime.Clock
import kotlin.math.max

class MerchantMappingUseCase(
    private val merchantMappingRepository: MerchantMappingRepository
) {

    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6
        private const val SIMILARITY_THRESHOLD = 0.8
        private const val MIN_EVIDENCE_SCORE = 3
    }

    suspend fun createMapping(
        merchantName: String,
        categoryId: String,
        confidence: Double,
        source: MappingSource
    ): Result<MerchantMapping> {
        return try {
            val normalizedName = normalizeMerchantName(merchantName)

            // Check if mapping already exists
            val existingMapping = merchantMappingRepository.findByNormalizedName(normalizedName)
            if (existingMapping != null) {
                // Update existing mapping with better confidence if applicable
                if (confidence > existingMapping.confidence) {
                    val updatedMapping = existingMapping.copy(
                        categoryId = categoryId,
                        confidence = confidence,
                        source = source,
                        updatedAt = Clock.System.now()
                    )
                    merchantMappingRepository.update(updatedMapping)
                    return Result.success(updatedMapping)
                }
                return Result.success(existingMapping)
            }

            val mapping = MerchantMapping(
                id = generateMappingId(),
                merchantName = merchantName,
                normalizedName = normalizedName,
                categoryId = categoryId,
                confidence = confidence,
                source = source,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                alternativeNames = generateAlternativeNames(merchantName)
            )

            merchantMappingRepository.create(mapping)
            Result.success(mapping)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findBestMapping(merchantName: String): MerchantMapping? {
        val normalizedName = normalizeMerchantName(merchantName)

        // First try exact match
        merchantMappingRepository.findByNormalizedName(normalizedName)?.let { exactMatch ->
            merchantMappingRepository.updateLastUsed(exactMatch.id, Clock.System.now())
            merchantMappingRepository.incrementSuccessCount(exactMatch.id)
            return exactMatch
        }

        // Try similar matches
        val similarMappings = merchantMappingRepository.findSimilarMappings(normalizedName)
        val bestMatch = similarMappings
            .filter { it.confidence >= MIN_CONFIDENCE_THRESHOLD }
            .maxByOrNull { calculateSimilarity(normalizedName, it.normalizedName) * it.confidence }

        bestMatch?.let {
            val similarity = calculateSimilarity(normalizedName, it.normalizedName)
            if (similarity >= SIMILARITY_THRESHOLD) {
                merchantMappingRepository.updateLastUsed(it.id, Clock.System.now())
                merchantMappingRepository.incrementSuccessCount(it.id)
                return it
            }
        }

        return null
    }

    suspend fun learnFromCorrection(
        merchantName: String,
        newCategoryId: String,
        userFeedback: String
    ): Result<MerchantMapping> {
        return try {
            val normalizedName = normalizeMerchantName(merchantName)
            val existingMapping = merchantMappingRepository.findByNormalizedName(normalizedName)

            if (existingMapping != null) {
                // Record feedback
                val feedback = MerchantFeedback(
                    id = generateFeedbackId(),
                    merchantMappingId = existingMapping.id,
                    originalCategoryId = existingMapping.categoryId,
                    correctedCategoryId = newCategoryId,
                    feedback = userFeedback,
                    createdAt = Clock.System.now()
                )

                merchantMappingRepository.recordFeedback(feedback)

                // Update mapping with improved confidence
                val updatedMapping = existingMapping.copy(
                    categoryId = newCategoryId,
                    confidence = min(existingMapping.confidence + 0.1, 0.99), // Increase confidence but cap at 99%
                    source = MappingSource.USER_CONFIRMED,
                    userFeedback = existingMapping.userFeedback + userFeedback,
                    updatedAt = Clock.System.now()
                )

                merchantMappingRepository.update(updatedMapping)
                Result.success(updatedMapping)
            } else {
                // Create new mapping based on user correction
                createMapping(
                    merchantName = merchantName,
                    categoryId = newCategoryId,
                    confidence = 0.95, // High confidence for user-confirmed mappings
                    source = MappingSource.USER_CONFIRMED
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun suggestCategory(merchantName: String): List<CategorySuggestion> {
        val suggestions = mutableListOf<CategorySuggestion>()
        val normalizedName = normalizeMerchantName(merchantName)

        // Get all existing mappings
        val allMappings = merchantMappingRepository.getActiveMappings()

        // Find similar merchants
        val similarMerchants = allMappings.filter { mapping ->
            calculateSimilarity(normalizedName, mapping.normalizedName) > 0.6
        }.sortedByDescending {
            calculateSimilarity(normalizedName, it.normalizedName) * it.confidence
        }

        // Group by category and calculate suggestion confidence
        val categoryGroups = similarMerchants.groupBy { it.categoryId }

        for ((categoryId, mappings) in categoryGroups) {
            val avgConfidence = mappings.map { it.confidence }.average()
            val evidenceCount = mappings.size
            val maxSimilarity = mappings.maxOfOrNull {
                calculateSimilarity(normalizedName, it.normalizedName)
            } ?: 0.0

            if (evidenceCount >= MIN_EVIDENCE_SCORE && avgConfidence > MIN_CONFIDENCE_THRESHOLD) {
                val suggestion = CategorySuggestion(
                    categoryId = categoryId,
                    categoryName = categoryId, // Would normally lookup category name
                    confidence = avgConfidence * maxSimilarity,
                    reason = "similar_merchant_pattern",
                    evidence = mappings.map { it.merchantName }
                )
                suggestions.add(suggestion)
            }
        }

        return suggestions.sortedByDescending { it.confidence }.take(3)
    }

    suspend fun buildSimilarityIndex(transactions: List<Transaction>): Result<List<List<String>>> {
        return try {
            val merchantNames = transactions.map { it.merchantName }.distinct()
            val similarityGroups = mutableListOf<MutableList<String>>()
            val processed = mutableSetOf<String>()

            for (merchant in merchantNames) {
                if (merchant in processed) continue

                val normalizedMerchant = normalizeMerchantName(merchant)
                val similarGroup = mutableListOf(merchant)
                processed.add(merchant)

                for (otherMerchant in merchantNames) {
                    if (otherMerchant == merchant || otherMerchant in processed) continue

                    val normalizedOther = normalizeMerchantName(otherMerchant)
                    val similarity = calculateSimilarity(normalizedMerchant, normalizedOther)

                    if (similarity >= SIMILARITY_THRESHOLD) {
                        similarGroup.add(otherMerchant)
                        processed.add(otherMerchant)
                    }
                }

                if (similarGroup.size > 1) {
                    similarityGroups.add(similarGroup)
                }
            }

            Result.success(similarityGroups.map { it.toList() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mergeDuplicates(): Result<Int> {
        return try {
            val duplicates = merchantMappingRepository.findDuplicates()
            var mergedCount = 0

            val duplicateGroups = duplicates.groupBy { it.normalizedName }

            for ((normalizedName, mappings) in duplicateGroups) {
                if (mappings.size <= 1) continue

                // Select the best mapping (highest confidence, most recent, user confirmed)
                val bestMapping = mappings.maxWithOrNull(
                    compareBy<MerchantMapping>
                { it.confidence }
                    .thenBy { it.source == MappingSource.USER_CONFIRMED }
                    .thenBy { it.updatedAt }
                ) ?: continue

                // Merge data from other mappings
                val mergedMapping = bestMapping.copy(
                    successfulMappings = mappings.sumOf { it.successfulMappings },
                    failedMappings = mappings.sumOf { it.failedMappings },
                    alternativeNames = mappings.flatMap { it.alternativeNames }.distinct(),
                    userFeedback = mappings.flatMap { it.userFeedback },
                    tags = mappings.flatMap { it.tags }.distinct(),
                    updatedAt = Clock.System.now()
                )

                // Delete old mappings
                val idsToDelete = mappings.filter { it.id != bestMapping.id }.map { it.id }
                merchantMappingRepository.deleteBulk(idsToDelete)

                // Update best mapping with merged data
                merchantMappingRepository.update(mergedMapping)

                mergedCount += idsToDelete.size
            }

            Result.success(mergedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateOverallAccuracy(): Double {
        val mappings = merchantMappingRepository.getAllMappings()
        if (mappings.isEmpty()) return 0.0

        val totalTransactions = mappings.sumOf { it.successfulMappings + it.failedMappings }
        val successfulTransactions = mappings.sumOf { it.successfulMappings }

        return if (totalTransactions > 0) {
            successfulTransactions.toDouble() / totalTransactions.toDouble()
        } else {
            0.0
        }
    }

    suspend fun exportMappings(): Result<String> {
        return try {
            val mappings = merchantMappingRepository.getAllMappings()
            val jsonData = merchantMappingRepository.exportMappings()
            Result.success(jsonData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importMappings(jsonData: String, replaceExisting: Boolean = false): Result<Int> {
        return try {
            if (replaceExisting) {
                merchantMappingRepository.deleteAll()
            }

            val mappings = merchantMappingRepository.importMappings(jsonData)
            merchantMappingRepository.createBulk(mappings)

            Result.success(mappings.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun normalizeMerchantName(merchantName: String): String {
        return merchantName
            .lowercase()
            .trim()
            .replace(Regex("[^\\w\\s]"), "") // Remove special characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("\\b(branch|store|shop|outlet|location|\\d+)\\b"), "") // Remove common suffixes
            .trim()
    }

    private fun generateAlternativeNames(merchantName: String): List<String> {
        val alternatives = mutableListOf<String>()
        val name = merchantName.lowercase()

        // Add common variations
        alternatives.add(name.replace(" ", ""))
        alternatives.add(name.replace("-", " "))
        alternatives.add(name.replace("&", "and"))

        // Add abbreviated forms
        val words = name.split(" ")
        if (words.size > 1) {
            alternatives.add(words.joinToString(" ") { it.take(3) })
        }

        return alternatives.distinct().filter { it != name }
    }

    private fun calculateSimilarity(str1: String, str2: String): Double {
        if (str1 == str2) return 1.0

        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1

        if (longer.isEmpty()) return 1.0

        val editDistance = levenshteinDistance(str1, str2)
        return (longer.length - editDistance) / longer.length.toDouble()
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

        for (i in 0..str1.length) {
            dp[i][0] = i
        }

        for (j in 0..str2.length) {
            dp[0][j] = j
        }

        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[str1.length][str2.length]
    }

    private fun generateMappingId(): String = "mapping_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
    private fun generateFeedbackId(): String = "feedback_${Clock.System.now().epochSeconds}_${(1000..9999).random()}"
    private fun min(a: Double, b: Double): Double = if (a < b) a else b
}
