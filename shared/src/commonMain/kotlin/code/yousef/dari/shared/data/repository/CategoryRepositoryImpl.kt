package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.data.repository.base.BaseRepository
import code.yousef.dari.shared.data.repository.mappers.*
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.services.CategoryMatchingService
import code.yousef.dari.shared.domain.services.CategoryDataService
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

/**
 * Category Repository Implementation
 * Implements offline-first data access for categories with SQLDelight
 * Handles smart categorization and rule-based matching
 * 
 * Refactored to use BaseRepository and extracted services following SRP
 */
class CategoryRepositoryImpl(
    private val database: DariDatabase,
    private val categoryMatchingService: CategoryMatchingService = CategoryMatchingService(database),
    private val categoryDataService: CategoryDataService = CategoryDataService()
) : BaseRepository(), CategoryRepository {

    private val categoryDao = database.categoryDao()

    override suspend fun createCategory(category: Category): Result<Category> {
        return safeDbOperation {
            database.withTransaction {
                categoryDao.insertCategory(
                    categoryId = category.categoryId,
                    name = category.name,
                    description = category.description,
                    categoryType = category.categoryType.name,
                    parentCategoryId = category.parentCategoryId,
                    iconName = category.iconName,
                    colorHex = category.colorHex,
                    keywords = category.keywords.joinToString(","),
                    merchantPatterns = category.merchantPatterns.joinToString(","),
                    isActive = if (category.isActive) 1 else 0,
                    isSystemCategory = if (category.isSystemCategory) 1 else 0,
                    sortOrder = category.sortOrder.toLong(),
                    level = category.level.toLong(),
                    createdAt = category.createdAt.epochSeconds,
                    updatedAt = category.updatedAt.epochSeconds,
                    metadata = serializeMetadata(category.metadata)
                )
            }
            category
        }
    }

    override suspend fun updateCategory(category: Category): Result<Category> {
        return safeDbOperation {
            categoryDao.updateCategory(
                name = category.name,
                description = category.description,
                categoryType = category.categoryType.name,
                parentCategoryId = category.parentCategoryId,
                iconName = category.iconName,
                colorHex = category.colorHex,
                keywords = category.keywords.joinToString(","),
                merchantPatterns = category.merchantPatterns.joinToString(","),
                isActive = if (category.isActive) 1 else 0,
                isSystemCategory = if (category.isSystemCategory) 1 else 0,
                sortOrder = category.sortOrder.toLong(),
                level = category.level.toLong(),
                updatedAt = Clock.System.now().epochSeconds,
                metadata = serializeMetadata(category.metadata),
                categoryId = category.categoryId
            )
            category
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return safeDbAction {
            database.withTransaction {
                // Delete related rules first
                categoryDao.deleteCategorizationRulesByCategory(categoryId)
                // Delete the category
                categoryDao.deleteCategory(categoryId)
            }
        }
    }

    override suspend fun getCategoryById(categoryId: String): Result<Category?> {
        return safeNullableOperation(
            operation = { categoryDao.selectById(categoryId).executeAsOneOrNull() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getActiveCategories(): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectActiveCategories().executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getAllCategories(): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectAll().executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getCategoriesByType(type: CategoryType): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectByType(type.name).executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getRootCategories(): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectRootCategories().executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getSubcategories(parentCategoryId: String): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectSubcategories(parentCategoryId).executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getSystemCategories(): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectSystemCategories().executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun getUserCategories(): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.selectUserCategories().executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun searchCategories(query: String): Result<List<Category>> {
        return safeListOperation(
            operation = { categoryDao.searchCategories(query, query, query).executeAsList() },
            mapper = { it.toDomainModel() }
        )
    }

    override suspend fun findBestMatch(
        description: String,
        merchantName: String?,
        amount: Money,
        accountId: String?
    ): Result<CategoryMatch?> {
        return safeDbOperation {
            val matches = getMatchingSuggestions(description, merchantName, amount, 1)
            matches.getOrNull()?.firstOrNull()
        }
    }

    override suspend fun getMatchingSuggestions(
        description: String,
        merchantName: String?,
        amount: Money,
        limit: Int
    ): Result<List<CategoryMatch>> {
        return safeDbOperation {
            val matches = mutableListOf<CategoryMatch>()
            
            // 1. Check exact keyword matches
            val keywordMatches = categoryMatchingService.findKeywordMatches(description, merchantName)
            matches.addAll(keywordMatches)
            
            // 2. Check merchant pattern matches
            if (merchantName != null) {
                val merchantMatches = categoryMatchingService.findMerchantPatternMatches(merchantName)
                matches.addAll(merchantMatches)
            }
            
            // 3. Check rule-based matches
            val ruleMatches = categoryMatchingService.findRuleBasedMatches(description, merchantName, amount)
            matches.addAll(ruleMatches)
            
            // 4. Sort by confidence and take top matches
            matches
                .distinctBy { it.category.categoryId }
                .sortedByDescending { it.confidence }
                .take(limit)
        }
    }

    override suspend fun createCategorizationRule(rule: CategorizationRule): Result<CategorizationRule> {
        return withContext(Dispatchers.Default) {
            try {
                categoryDao.insertCategorizationRule(
                    ruleId = rule.ruleId,
                    categoryId = rule.categoryId,
                    name = rule.name,
                    description = rule.description,
                    ruleType = rule.ruleType.name,
                    conditions = serializeConditions(rule.conditions),
                    priority = rule.priority.toLong(),
                    isActive = if (rule.isActive) 1 else 0,
                    createdAt = rule.createdAt.epochSeconds,
                    updatedAt = rule.updatedAt.epochSeconds
                )
                Result.success(rule)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateCategorizationRule(rule: CategorizationRule): Result<CategorizationRule> {
        return withContext(Dispatchers.Default) {
            try {
                categoryDao.updateCategorizationRule(
                    name = rule.name,
                    description = rule.description,
                    ruleType = rule.ruleType.name,
                    conditions = serializeConditions(rule.conditions),
                    priority = rule.priority.toLong(),
                    isActive = if (rule.isActive) 1 else 0,
                    updatedAt = Clock.System.now().epochSeconds,
                    ruleId = rule.ruleId
                )
                Result.success(rule)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteCategorizationRule(ruleId: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                categoryDao.deleteCategorizationRule(ruleId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoryRules(categoryId: String): Result<List<CategorizationRule>> {
        return withContext(Dispatchers.Default) {
            try {
                val rules = categoryDao.selectRulesByCategory(categoryId)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(rules)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getActiveRules(): Result<List<CategorizationRule>> {
        return withContext(Dispatchers.Default) {
            try {
                val rules = categoryDao.selectActiveRules()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(rules)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun testRule(
        rule: CategorizationRule,
        description: String,
        merchantName: String?,
        amount: Money
    ): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                val matches = rule.conditions.all { condition ->
                    when (condition.type) {
                        RuleConditionType.DESCRIPTION_CONTAINS -> {
                            description.lowercase().contains(condition.value.lowercase())
                        }
                        RuleConditionType.MERCHANT_EQUALS -> {
                            merchantName?.lowercase() == condition.value.lowercase()
                        }
                        RuleConditionType.MERCHANT_CONTAINS -> {
                            merchantName?.lowercase()?.contains(condition.value.lowercase()) == true
                        }
                        RuleConditionType.AMOUNT_GREATER_THAN -> {
                            amount.toDouble() > condition.value.toDoubleOrNull() ?: 0.0
                        }
                        RuleConditionType.AMOUNT_LESS_THAN -> {
                            amount.toDouble() < condition.value.toDoubleOrNull() ?: Double.MAX_VALUE
                        }
                        RuleConditionType.AMOUNT_EQUALS -> {
                            amount.toDouble() == condition.value.toDoubleOrNull()
                        }
                    }
                }
                Result.success(matches)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addCategoryKeywords(categoryId: String, keywords: List<String>): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val category = categoryDao.selectById(categoryId).executeAsOneOrNull()
                if (category != null) {
                    val existingKeywords = category.keywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    val newKeywords = (existingKeywords + keywords).distinct()
                    categoryDao.updateCategoryKeywords(
                        keywords = newKeywords.joinToString(","),
                        updatedAt = Clock.System.now().epochSeconds,
                        categoryId = categoryId
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun removeCategoryKeywords(categoryId: String, keywords: List<String>): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val category = categoryDao.selectById(categoryId).executeAsOneOrNull()
                if (category != null) {
                    val existingKeywords = category.keywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    val newKeywords = existingKeywords.filter { it !in keywords }
                    categoryDao.updateCategoryKeywords(
                        keywords = newKeywords.joinToString(","),
                        updatedAt = Clock.System.now().epochSeconds,
                        categoryId = categoryId
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addMerchantPatterns(categoryId: String, patterns: List<String>): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val category = categoryDao.selectById(categoryId).executeAsOneOrNull()
                if (category != null) {
                    val existingPatterns = category.merchantPatterns?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    val newPatterns = (existingPatterns + patterns).distinct()
                    categoryDao.updateCategoryMerchantPatterns(
                        merchantPatterns = newPatterns.joinToString(","),
                        updatedAt = Clock.System.now().epochSeconds,
                        categoryId = categoryId
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun removeMerchantPatterns(categoryId: String, patterns: List<String>): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val category = categoryDao.selectById(categoryId).executeAsOneOrNull()
                if (category != null) {
                    val existingPatterns = category.merchantPatterns?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    val newPatterns = existingPatterns.filter { it !in patterns }
                    categoryDao.updateCategoryMerchantPatterns(
                        merchantPatterns = newPatterns.joinToString(","),
                        updatedAt = Clock.System.now().epochSeconds,
                        categoryId = categoryId
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoryUsageStats(categoryId: String): Result<CategoryUsageStats> {
        return withContext(Dispatchers.Default) {
            try {
                val stats = categoryDao.selectCategoryUsageStats(categoryId).executeAsOneOrNull()
                if (stats != null) {
                    val usageStats = CategoryUsageStats(
                        categoryId = categoryId,
                        categoryName = stats.categoryName,
                        usageCount = (stats.usageCount ?: 0).toInt(),
                        totalAmount = Money(stats.totalAmount ?: "0", "SAR"),
                        averageAmount = Money(stats.averageAmount ?: "0", "SAR"),
                        lastUsedDate = stats.lastUsedDate?.let { Instant.fromEpochSeconds(it) },
                        usageFrequency = stats.usageFrequency ?: 0.0
                    )
                    Result.success(usageStats)
                } else {
                    Result.failure(Exception("Category not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMostUsedCategories(limit: Int): Result<List<CategoryUsageStats>> {
        return withContext(Dispatchers.Default) {
            try {
                val stats = categoryDao.selectMostUsedCategories(limit.toLong())
                    .executeAsList()
                    .map { stat ->
                        CategoryUsageStats(
                            categoryId = stat.categoryId,
                            categoryName = stat.categoryName,
                            usageCount = (stat.usageCount ?: 0).toInt(),
                            totalAmount = Money(stat.totalAmount ?: "0", "SAR"),
                            averageAmount = Money(stat.averageAmount ?: "0", "SAR"),
                            lastUsedDate = stat.lastUsedDate?.let { Instant.fromEpochSeconds(it) },
                            usageFrequency = stat.usageFrequency ?: 0.0
                        )
                    }
                Result.success(stats)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return safeDbAction {
            val defaultCategories = categoryDataService.getDefaultCategories()
            database.withTransaction {
                defaultCategories.forEach { category ->
                    categoryDao.insertCategory(
                        categoryId = category.categoryId,
                        name = category.name,
                        description = category.description,
                        categoryType = category.categoryType.name,
                        parentCategoryId = category.parentCategoryId,
                        iconName = category.iconName,
                        colorHex = category.colorHex,
                        keywords = category.keywords.joinToString(","),
                        merchantPatterns = category.merchantPatterns.joinToString(","),
                        isActive = if (category.isActive) 1 else 0,
                        isSystemCategory = if (category.isSystemCategory) 1 else 0,
                        sortOrder = category.sortOrder.toLong(),
                        level = category.level.toLong(),
                        createdAt = category.createdAt.epochSeconds,
                        updatedAt = category.updatedAt.epochSeconds,
                        metadata = serializeMetadata(category.metadata)
                    )
                }
            }
        }
    }

    override suspend fun importCategories(categories: List<Category>): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    categories.forEach { category ->
                        createCategory(category)
                    }
                }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun exportCategories(): Result<List<Category>> {
        return getAllCategories()
    }

    override suspend fun isCategoryNameExists(name: String, parentId: String?): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                val exists = if (parentId != null) {
                    categoryDao.checkCategoryNameExistsWithParent(name, parentId).executeAsOne() > 0
                } else {
                    categoryDao.checkCategoryNameExists(name).executeAsOne() > 0
                }
                Result.success(exists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoryTree(): Result<List<CategoryTreeNode>> {
        return safeDbOperation {
            val allCategories = categoryDao.selectAll().executeAsList().map { it.toDomainModel() }
            categoryDataService.buildCategoryTree(allCategories)
        }
    }

    override suspend fun mergeCategories(
        sourceCategoryId: String,
        targetCategoryId: String
    ): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    // Update all transactions to use target category
                    categoryDao.updateTransactionCategories(targetCategoryId, sourceCategoryId)
                    
                    // Update all child categories to have new parent
                    categoryDao.updateChildCategoriesParent(targetCategoryId, sourceCategoryId)
                    
                    // Delete source category
                    categoryDao.deleteCategory(sourceCategoryId)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun learnFromCategorization(
        transactionDescription: String,
        merchantName: String?,
        amount: Money,
        selectedCategoryId: String,
        confidence: Int
    ): Result<Unit> {
        return safeDbAction {
            // Store learning data for future ML model training
            categoryDao.insertCategorizationLearning(
                learningId = categoryDataService.generateLearningId(),
                transactionDescription = transactionDescription,
                merchantName = merchantName,
                amount = amount.amount,
                currency = amount.currency,
                selectedCategoryId = selectedCategoryId,
                confidence = confidence.toLong(),
                createdAt = Clock.System.now().epochSeconds
            )
            
            // Update category keywords based on learning
            if (confidence > 70) {
                val keywords = categoryDataService.extractKeywordsFromDescription(transactionDescription)
                if (keywords.isNotEmpty()) {
                    addCategoryKeywords(selectedCategoryId, keywords)
                }
                
                if (merchantName != null) {
                    addMerchantPatterns(selectedCategoryId, listOf(merchantName))
                }
            }
        }
    }

}