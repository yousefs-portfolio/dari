package code.yousef.dari.shared.domain.repository

import code.yousef.dari.shared.domain.model.MerchantFeedback
import code.yousef.dari.shared.domain.model.MerchantMapping
import code.yousef.dari.shared.domain.model.MappingSource
import kotlinx.datetime.Instant

interface MerchantMappingRepository {

    // Basic CRUD operations
    suspend fun create(mapping: MerchantMapping)
    suspend fun update(mapping: MerchantMapping)
    suspend fun delete(id: String)
    suspend fun getById(id: String): MerchantMapping?
    suspend fun getByMerchantName(merchantName: String): MerchantMapping?
    suspend fun getAllMappings(): List<MerchantMapping>
    suspend fun getActiveMappings(): List<MerchantMapping>

    // Search and matching
    suspend fun findSimilarMappings(merchantName: String): List<MerchantMapping>
    suspend fun findByNormalizedName(normalizedName: String): MerchantMapping?
    suspend fun findByArabicName(arabicName: String): MerchantMapping?
    suspend fun searchMappings(query: String): List<MerchantMapping>
    suspend fun findExactMatch(merchantName: String): MerchantMapping?

    // Bulk operations
    suspend fun createBulk(mappings: List<MerchantMapping>)
    suspend fun updateBulk(mappings: List<MerchantMapping>)
    suspend fun deleteBulk(ids: List<String>)
    suspend fun deleteAll()

    // Analytics and insights
    suspend fun getMostUsedMappings(limit: Int = 50): List<MerchantMapping>
    suspend fun getLeastConfidentMappings(threshold: Double = 0.7): List<MerchantMapping>
    suspend fun getMappingsBySource(source: MappingSource): List<MerchantMapping>
    suspend fun getMappingsByCategory(categoryId: String): List<MerchantMapping>
    suspend fun getUnusedMappings(daysSinceLastUse: Int): List<MerchantMapping>

    // Feedback and learning
    suspend fun recordFeedback(feedback: MerchantFeedback)
    suspend fun getFeedbackForMapping(mappingId: String): List<MerchantFeedback>
    suspend fun incrementSuccessCount(mappingId: String)
    suspend fun incrementFailureCount(mappingId: String)
    suspend fun updateLastUsed(mappingId: String, timestamp: Instant)

    // Quality and maintenance
    suspend fun findDuplicates(): List<MerchantMapping>
    suspend fun findConflictingMappings(): List<Pair<MerchantMapping, MerchantMapping>>
    suspend fun getStaleMapping(daysSinceUpdate: Int): List<MerchantMapping>
    suspend fun validateMappings(): List<String> // Returns list of issues found

    // Statistics
    suspend fun getTotalMappingCount(): Int
    suspend fun getAverageConfidence(): Double
    suspend fun getSuccessRate(): Double
    suspend fun getMappingCountByCategory(): Map<String, Int>
    suspend fun getRecentActivityCount(days: Int): Int

    // Import/Export
    suspend fun exportMappings(): String // JSON format
    suspend fun importMappings(jsonData: String): List<MerchantMapping>
    suspend fun backupMappings(): ByteArray
    suspend fun restoreFromBackup(backup: ByteArray): Boolean
}
