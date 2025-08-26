package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.domain.models.*

/**
 * Repository interface for Category data operations
 * Handles CRUD operations and categorization logic
 */
interface CategoryRepository {
    
    /**
     * Create a new category
     */
    suspend fun createCategory(category: Category): Result<Category>
    
    /**
     * Update an existing category
     */
    suspend fun updateCategory(category: Category): Result<Category>
    
    /**
     * Delete a category
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    
    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: String): Result<Category?>
    
    /**
     * Get all active categories
     */
    suspend fun getActiveCategories(): Result<List<Category>>
    
    /**
     * Get all categories (active and inactive)
     */
    suspend fun getAllCategories(): Result<List<Category>>
    
    /**
     * Get categories by type
     */
    suspend fun getCategoriesByType(type: CategoryType): Result<List<Category>>
    
    /**
     * Get root categories (parent categories)
     */
    suspend fun getRootCategories(): Result<List<Category>>
    
    /**
     * Get subcategories for a parent category
     */
    suspend fun getSubcategories(parentCategoryId: String): Result<List<Category>>
    
    /**
     * Get system (default) categories
     */
    suspend fun getSystemCategories(): Result<List<Category>>
    
    /**
     * Get user-created categories
     */
    suspend fun getUserCategories(): Result<List<Category>>
    
    /**
     * Search categories by name
     */
    suspend fun searchCategories(query: String): Result<List<Category>>
    
    /**
     * Find best matching category for a transaction
     */
    suspend fun findBestMatch(
        description: String,
        merchantName: String?,
        amount: Money,
        accountId: String? = null
    ): Result<CategoryMatch?>
    
    /**
     * Get multiple category matches with confidence scores
     */
    suspend fun getMatchingSuggestions(
        description: String,
        merchantName: String?,
        amount: Money,
        limit: Int = 5
    ): Result<List<CategoryMatch>>
    
    /**
     * Create categorization rule
     */
    suspend fun createCategorizationRule(rule: CategorizationRule): Result<CategorizationRule>
    
    /**
     * Update categorization rule
     */
    suspend fun updateCategorizationRule(rule: CategorizationRule): Result<CategorizationRule>
    
    /**
     * Delete categorization rule
     */
    suspend fun deleteCategorizationRule(ruleId: String): Result<Unit>
    
    /**
     * Get rules for a category
     */
    suspend fun getCategoryRules(categoryId: String): Result<List<CategorizationRule>>
    
    /**
     * Get all active categorization rules
     */
    suspend fun getActiveRules(): Result<List<CategorizationRule>>
    
    /**
     * Test a rule against sample data
     */
    suspend fun testRule(
        rule: CategorizationRule,
        description: String,
        merchantName: String?,
        amount: Money
    ): Result<Boolean>
    
    /**
     * Add keywords to category
     */
    suspend fun addCategoryKeywords(categoryId: String, keywords: List<String>): Result<Unit>
    
    /**
     * Remove keywords from category
     */
    suspend fun removeCategoryKeywords(categoryId: String, keywords: List<String>): Result<Unit>
    
    /**
     * Add merchant patterns to category
     */
    suspend fun addMerchantPatterns(categoryId: String, patterns: List<String>): Result<Unit>
    
    /**
     * Remove merchant patterns from category
     */
    suspend fun removeMerchantPatterns(categoryId: String, patterns: List<String>): Result<Unit>
    
    /**
     * Get category usage statistics
     */
    suspend fun getCategoryUsageStats(categoryId: String): Result<CategoryUsageStats>
    
    /**
     * Get most frequently used categories
     */
    suspend fun getMostUsedCategories(limit: Int = 10): Result<List<CategoryUsageStats>>
    
    /**
     * Initialize default categories
     */
    suspend fun initializeDefaultCategories(): Result<Unit>
    
    /**
     * Import categories from predefined set
     */
    suspend fun importCategories(categories: List<Category>): Result<List<Category>>
    
    /**
     * Export categories for backup
     */
    suspend fun exportCategories(): Result<List<Category>>
    
    /**
     * Check if category name exists
     */
    suspend fun isCategoryNameExists(name: String, parentId: String? = null): Result<Boolean>
    
    /**
     * Get category hierarchy as tree structure
     */
    suspend fun getCategoryTree(): Result<List<CategoryTreeNode>>
    
    /**
     * Merge categories (combine two categories into one)
     */
    suspend fun mergeCategories(
        sourceCategoryId: String,
        targetCategoryId: String
    ): Result<Unit>
    
    /**
     * Learn from user categorization (improve ML models)
     */
    suspend fun learnFromCategorization(
        transactionDescription: String,
        merchantName: String?,
        amount: Money,
        selectedCategoryId: String,
        confidence: Int
    ): Result<Unit>
}

/**
 * Category match result with confidence
 */
data class CategoryMatch(
    val category: Category,
    val confidence: Int, // 0-100
    val matchReasons: List<MatchReason>
)

/**
 * Reason for category match
 */
data class MatchReason(
    val type: MatchType,
    val value: String,
    val confidence: Int
)

/**
 * Types of category matches
 */
enum class MatchType {
    KEYWORD,
    MERCHANT_PATTERN,
    RULE_BASED,
    AMOUNT_RANGE,
    ML_PREDICTION,
    USER_HISTORY
}

/**
 * Category usage statistics
 */
data class CategoryUsageStats(
    val categoryId: String,
    val categoryName: String,
    val usageCount: Int,
    val totalAmount: Money,
    val averageAmount: Money,
    val lastUsedDate: kotlinx.datetime.Instant?,
    val usageFrequency: Double // uses per month
)

/**
 * Category tree node for hierarchical display
 */
data class CategoryTreeNode(
    val category: Category,
    val children: List<CategoryTreeNode> = emptyList(),
    val level: Int = 0
) {
    
    /**
     * Flatten tree to list
     */
    fun flatten(): List<Category> {
        val result = mutableListOf<Category>()
        result.add(category)
        children.forEach { child ->
            result.addAll(child.flatten())
        }
        return result
    }
    
    /**
     * Find category in tree
     */
    fun findCategory(categoryId: String): CategoryTreeNode? {
        if (category.categoryId == categoryId) return this
        
        children.forEach { child ->
            val found = child.findCategory(categoryId)
            if (found != null) return found
        }
        
        return null
    }
    
    /**
     * Get all leaf categories (no children)
     */
    fun getLeafCategories(): List<Category> {
        if (children.isEmpty()) return listOf(category)
        
        val result = mutableListOf<Category>()
        children.forEach { child ->
            result.addAll(child.getLeafCategories())
        }
        return result
    }
}