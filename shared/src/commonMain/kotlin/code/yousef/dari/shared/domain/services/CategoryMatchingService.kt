package code.yousef.dari.shared.domain.services

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.data.repository.mappers.toDomainModel
import code.yousef.dari.shared.domain.models.*

/**
 * Category matching service for smart transaction categorization
 * Extracted from repository to follow Single Responsibility Principle
 */
class CategoryMatchingService(
    private val database: DariDatabase
) {

    private val categoryDao = database.categoryDao()

    /**
     * Finds keyword-based category matches
     */
    suspend fun findKeywordMatches(description: String, merchantName: String?): List<CategoryMatch> {
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

    /**
     * Finds merchant pattern-based category matches
     */
    suspend fun findMerchantPatternMatches(merchantName: String): List<CategoryMatch> {
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

    /**
     * Finds rule-based category matches
     */
    suspend fun findRuleBasedMatches(
        description: String,
        merchantName: String?,
        amount: Money
    ): List<CategoryMatch> {
        val matches = mutableListOf<CategoryMatch>()
        val rules = categoryDao.selectActiveRules().executeAsList()
        
        rules.forEach { rule ->
            val ruleObj = rule.toDomainModel()
            if (testRule(ruleObj, description, merchantName, amount)) {
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

    /**
     * Tests if a rule matches given transaction data
     */
    private fun testRule(
        rule: CategorizationRule,
        description: String,
        merchantName: String?,
        amount: Money
    ): Boolean {
        return rule.conditions.all { condition ->
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
    }
}