package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Category Repository Implementation
 * Implements offline-first data access for categories with SQLDelight
 * Handles smart categorization and rule-based matching
 */
class CategoryRepositoryImpl(
    private val database: DariDatabase
) : CategoryRepository {

    private val categoryDao = database.categoryDao()

    override suspend fun createCategory(category: Category): Result<Category> {
        return withContext(Dispatchers.Default) {
            try {
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
                Result.success(category)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateCategory(category: Category): Result<Category> {
        return withContext(Dispatchers.Default) {
            try {
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
                Result.success(category)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    // Delete related rules first
                    categoryDao.deleteCategorizationRulesByCategory(categoryId)
                    // Delete the category
                    categoryDao.deleteCategory(categoryId)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoryById(categoryId: String): Result<Category?> {
        return withContext(Dispatchers.Default) {
            try {
                val category = categoryDao.selectById(categoryId).executeAsOneOrNull()?.toDomainModel()
                Result.success(category)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getActiveCategories(): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectActiveCategories()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAllCategories(): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectAll()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoriesByType(type: CategoryType): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectByType(type.name)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getRootCategories(): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectRootCategories()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSubcategories(parentCategoryId: String): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectSubcategories(parentCategoryId)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSystemCategories(): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectSystemCategories()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserCategories(): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.selectUserCategories()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchCategories(query: String): Result<List<Category>> {
        return withContext(Dispatchers.Default) {
            try {
                val categories = categoryDao.searchCategories(query, query, query)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(categories)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun findBestMatch(
        description: String,
        merchantName: String?,
        amount: Money,
        accountId: String?
    ): Result<CategoryMatch?> {
        return withContext(Dispatchers.Default) {
            try {
                val matches = getMatchingSuggestions(description, merchantName, amount, 1)
                val bestMatch = matches.getOrNull()?.firstOrNull()
                Result.success(bestMatch)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMatchingSuggestions(
        description: String,
        merchantName: String?,
        amount: Money,
        limit: Int
    ): Result<List<CategoryMatch>> {
        return withContext(Dispatchers.Default) {
            try {
                val matches = mutableListOf<CategoryMatch>()
                
                // 1. Check exact keyword matches
                val keywordMatches = findKeywordMatches(description, merchantName)
                matches.addAll(keywordMatches)
                
                // 2. Check merchant pattern matches
                if (merchantName != null) {
                    val merchantMatches = findMerchantPatternMatches(merchantName)
                    matches.addAll(merchantMatches)
                }
                
                // 3. Check rule-based matches
                val ruleMatches = findRuleBasedMatches(description, merchantName, amount)
                matches.addAll(ruleMatches)
                
                // 4. Sort by confidence and take top matches
                val sortedMatches = matches
                    .distinctBy { it.category.categoryId }
                    .sortedByDescending { it.confidence }
                    .take(limit)
                
                Result.success(sortedMatches)
            } catch (e: Exception) {
                Result.failure(e)
            }
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
        return withContext(Dispatchers.Default) {
            try {
                val defaultCategories = getDefaultCategories()
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
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
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
        return withContext(Dispatchers.Default) {
            try {
                val allCategories = categoryDao.selectAll().executeAsList().map { it.toDomainModel() }
                val tree = buildCategoryTree(allCategories)
                Result.success(tree)
            } catch (e: Exception) {
                Result.failure(e)
            }
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
        return withContext(Dispatchers.Default) {
            try {
                // Store learning data for future ML model training
                categoryDao.insertCategorizationLearning(
                    learningId = generateId(),
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
                    val keywords = extractKeywordsFromDescription(transactionDescription)
                    if (keywords.isNotEmpty()) {
                        addCategoryKeywords(selectedCategoryId, keywords)
                    }
                    
                    if (merchantName != null) {
                        addMerchantPatterns(selectedCategoryId, listOf(merchantName))
                    }
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Helper functions

    private suspend fun findKeywordMatches(description: String, merchantName: String?): List<CategoryMatch> {
        val matches = mutableListOf<CategoryMatch>()
        val categories = categoryDao.selectCategoriesWithKeywords().executeAsList()
        
        val searchText = listOfNotNull(description, merchantName).joinToString(" ").lowercase()
        
        categories.forEach { category ->
            val keywords = category.keywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val matchingKeywords = keywords.filter { keyword ->
                searchText.contains(keyword.lowercase())
            }
            
            if (matchingKeywords.isNotEmpty()) {
                val confidence = minOf(95, 60 + matchingKeywords.size * 15)
                val reasons = matchingKeywords.map { keyword ->
                    MatchReason(MatchType.KEYWORD, keyword, confidence)
                }
                matches.add(
                    CategoryMatch(
                        category = category.toDomainModel(),
                        confidence = confidence,
                        matchReasons = reasons
                    )
                )
            }
        }
        
        return matches
    }

    private suspend fun findMerchantPatternMatches(merchantName: String): List<CategoryMatch> {
        val matches = mutableListOf<CategoryMatch>()
        val categories = categoryDao.selectCategoriesWithMerchantPatterns().executeAsList()
        
        categories.forEach { category ->
            val patterns = category.merchantPatterns?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val matchingPatterns = patterns.filter { pattern ->
                merchantName.lowercase().contains(pattern.lowercase())
            }
            
            if (matchingPatterns.isNotEmpty()) {
                val confidence = minOf(90, 70 + matchingPatterns.size * 10)
                val reasons = matchingPatterns.map { pattern ->
                    MatchReason(MatchType.MERCHANT_PATTERN, pattern, confidence)
                }
                matches.add(
                    CategoryMatch(
                        category = category.toDomainModel(),
                        confidence = confidence,
                        matchReasons = reasons
                    )
                )
            }
        }
        
        return matches
    }

    private suspend fun findRuleBasedMatches(
        description: String,
        merchantName: String?,
        amount: Money
    ): List<CategoryMatch> {
        val matches = mutableListOf<CategoryMatch>()
        val rules = categoryDao.selectActiveRules().executeAsList()
        
        rules.forEach { rule ->
            val ruleObj = rule.toDomainModel()
            val testResult = testRule(ruleObj, description, merchantName, amount)
            if (testResult.getOrDefault(false)) {
                val category = categoryDao.selectById(rule.categoryId).executeAsOneOrNull()
                if (category != null) {
                    val confidence = 80 + rule.priority.toInt() // Base confidence + priority
                    val reason = MatchReason(MatchType.RULE_BASED, ruleObj.name, confidence)
                    matches.add(
                        CategoryMatch(
                            category = category.toDomainModel(),
                            confidence = confidence,
                            matchReasons = listOf(reason)
                        )
                    )
                }
            }
        }
        
        return matches
    }

    private fun buildCategoryTree(categories: List<Category>): List<CategoryTreeNode> {
        val categoryMap = categories.associateBy { it.categoryId }
        val rootCategories = categories.filter { it.parentCategoryId == null }
        
        fun buildNode(category: Category, level: Int = 0): CategoryTreeNode {
            val children = categories
                .filter { it.parentCategoryId == category.categoryId }
                .map { buildNode(it, level + 1) }
                .sortedBy { it.category.sortOrder }
            
            return CategoryTreeNode(category, children, level)
        }
        
        return rootCategories
            .map { buildNode(it) }
            .sortedBy { it.category.sortOrder }
    }

    private fun extractKeywordsFromDescription(description: String): List<String> {
        // Simple keyword extraction - in production would use NLP
        return description
            .lowercase()
            .split(" ")
            .filter { it.length > 3 && !isCommonWord(it) }
            .take(3) // Limit to 3 keywords
    }

    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf("and", "the", "for", "with", "from", "this", "that", "have", "been", "will")
        return word in commonWords
    }

    private fun getDefaultCategories(): List<Category> {
        val now = Clock.System.now()
        
        return listOf(
            Category(
                categoryId = "cat_food_dining",
                name = "Food & Dining",
                description = "Restaurants, groceries, food delivery",
                categoryType = CategoryType.EXPENSE,
                iconName = "restaurant",
                colorHex = "#FF9800",
                keywords = listOf("restaurant", "food", "grocery", "supermarket", "dining", "cafe"),
                merchantPatterns = listOf("restaurant", "grocery", "supermarket", "food"),
                isSystemCategory = true,
                sortOrder = 1,
                level = 0,
                createdAt = now,
                updatedAt = now
            ),
            Category(
                categoryId = "cat_transportation",
                name = "Transportation",
                description = "Gas, public transport, taxi, car maintenance",
                categoryType = CategoryType.EXPENSE,
                iconName = "directions_car",
                colorHex = "#2196F3",
                keywords = listOf("gas", "fuel", "taxi", "uber", "transport", "car", "bus"),
                merchantPatterns = listOf("gas", "fuel", "taxi", "uber", "transport"),
                isSystemCategory = true,
                sortOrder = 2,
                level = 0,
                createdAt = now,
                updatedAt = now
            ),
            Category(
                categoryId = "cat_shopping",
                name = "Shopping",
                description = "Clothing, electronics, general shopping",
                categoryType = CategoryType.EXPENSE,
                iconName = "shopping_bag",
                colorHex = "#E91E63",
                keywords = listOf("shopping", "store", "mall", "clothing", "electronics"),
                merchantPatterns = listOf("store", "shop", "mall", "retail"),
                isSystemCategory = true,
                sortOrder = 3,
                level = 0,
                createdAt = now,
                updatedAt = now
            ),
            Category(
                categoryId = "cat_salary",
                name = "Salary",
                description = "Monthly salary and wages",
                categoryType = CategoryType.INCOME,
                iconName = "work",
                colorHex = "#4CAF50",
                keywords = listOf("salary", "wage", "payroll", "income"),
                merchantPatterns = listOf("salary", "payroll", "wage"),
                isSystemCategory = true,
                sortOrder = 1,
                level = 0,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun serializeMetadata(metadata: Map<String, String>): String {
        return if (metadata.isEmpty()) {
            "{}"
        } else {
            metadata.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { "\"${it.key}\":\"${it.value}\"" }
        }
    }

    private fun serializeConditions(conditions: List<RuleCondition>): String {
        // Simple serialization - in production would use kotlinx.serialization
        return conditions.joinToString("|") { condition ->
            "${condition.type.name}:${condition.operator.name}:${condition.value}"
        }
    }

    private fun deserializeConditions(serialized: String): List<RuleCondition> {
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split("|").mapNotNull { condition ->
            try {
                val parts = condition.split(":")
                if (parts.size >= 3) {
                    RuleCondition(
                        type = RuleConditionType.valueOf(parts[0]),
                        operator = RuleOperator.valueOf(parts[1]),
                        value = parts.drop(2).joinToString(":") // Rejoin in case value contains ":"
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun generateId(): String {
        return "learn_${Clock.System.now().epochSeconds}_${(0..9999).random()}"
    }

    private fun deserializeMetadata(json: String): Map<String, String> {
        if (json == "{}") return emptyMap()
        
        return try {
            json.removeSurrounding("{", "}")
                .split(",")
                .associate { pair ->
                    val (key, value) = pair.split(":")
                    key.removeSurrounding("\"") to value.removeSurrounding("\"")
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// Extension functions to convert between database and domain models
private fun code.yousef.dari.shared.database.Category.toDomainModel(): Category {
    return Category(
        categoryId = categoryId,
        name = name,
        description = description,
        categoryType = CategoryType.valueOf(categoryType),
        parentCategoryId = parentCategoryId,
        iconName = iconName,
        colorHex = colorHex,
        keywords = keywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        merchantPatterns = merchantPatterns?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        isActive = isActive == 1L,
        isSystemCategory = isSystemCategory == 1L,
        sortOrder = sortOrder?.toInt() ?: 0,
        level = level?.toInt() ?: 0,
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        metadata = deserializeMetadata(metadata ?: "{}")
    )
}

private fun code.yousef.dari.shared.database.CategorizationRule.toDomainModel(): CategorizationRule {
    return CategorizationRule(
        ruleId = ruleId,
        categoryId = categoryId,
        name = name,
        description = description,
        ruleType = RuleType.valueOf(ruleType),
        conditions = deserializeConditions(conditions),
        priority = priority.toInt(),
        isActive = isActive == 1L,
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt)
    )
}

private fun deserializeConditions(serialized: String): List<RuleCondition> {
    if (serialized.isEmpty()) return emptyList()
    
    return serialized.split("|").mapNotNull { condition ->
        try {
            val parts = condition.split(":")
            if (parts.size >= 3) {
                RuleCondition(
                    type = RuleConditionType.valueOf(parts[0]),
                    operator = RuleOperator.valueOf(parts[1]),
                    value = parts.drop(2).joinToString(":")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

private fun deserializeMetadata(json: String): Map<String, String> {
    if (json == "{}") return emptyMap()
    
    return try {
        json.removeSurrounding("{", "}")
            .split(",")
            .associate { pair ->
                val (key, value) = pair.split(":")
                key.removeSurrounding("\"") to value.removeSurrounding("\"")
            }
    } catch (e: Exception) {
        emptyMap()
    }
}