package code.yousef.dari.shared.domain.model

import kotlinx.datetime.Instant

enum class MappingSource {
    USER_CONFIRMED,
    AUTO_DETECTED,
    MACHINE_LEARNING,
    IMPORTED,
    RULE_BASED
}

data class MerchantMapping(
    val id: String,
    val merchantName: String,
    val normalizedName: String,
    val categoryId: String,
    val confidence: Double,
    val source: MappingSource,
    val successfulMappings: Int = 0,
    val failedMappings: Int = 0,
    val lastUsedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isActive: Boolean = true,
    val userFeedback: List<String> = emptyList(),
    val alternativeNames: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

data class MerchantFeedback(
    val id: String,
    val merchantMappingId: String,
    val originalCategoryId: String,
    val correctedCategoryId: String,
    val feedback: String,
    val userId: String? = null,
    val createdAt: Instant
)

data class CategorySuggestion(
    val categoryId: String,
    val categoryName: String,
    val confidence: Double,
    val reason: String,
    val evidence: List<String> = emptyList()
)

data class MerchantSimilarity(
    val merchant1: String,
    val merchant2: String,
    val similarity: Double,
    val commonPatterns: List<String>
)

data class MappingStats(
    val totalMappings: Int,
    val averageConfidence: Double,
    val successRate: Double,
    val topCategories: List<Pair<String, Int>>,
    val recentActivity: Int,
    val userCorrectionRate: Double
)