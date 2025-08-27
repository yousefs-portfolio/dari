package code.yousef.dari.shared.domain.services

import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Category data service providing utility functions for category management
 * Extracted from repository to improve modularity and testability
 */
class CategoryDataService {

    /**
     * Builds a hierarchical category tree from flat list
     */
    fun buildCategoryTree(categories: List<Category>): List<CategoryTreeNode> {
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

    /**
     * Extracts keywords from transaction description for learning
     */
    fun extractKeywordsFromDescription(description: String): List<String> {
        // Simple keyword extraction - in production would use NLP
        return description
            .lowercase()
            .split(" ")
            .filter { it.length > 3 && !isCommonWord(it) }
            .take(3) // Limit to 3 keywords
    }

    /**
     * Checks if a word is too common to be useful as a keyword
     */
    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf("and", "the", "for", "with", "from", "this", "that", "have", "been", "will")
        return word in commonWords
    }

    /**
     * Gets default system categories for initialization
     */
    fun getDefaultCategories(): List<Category> {
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

    /**
     * Generates a unique learning ID
     */
    fun generateLearningId(): String {
        return "learn_${Clock.System.now().epochSeconds}_${(0..9999).random()}"
    }
}