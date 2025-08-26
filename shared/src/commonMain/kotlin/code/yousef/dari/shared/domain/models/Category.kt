package code.yousef.dari.shared.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Transaction Category Domain Model
 * Supports hierarchical categorization with Saudi-specific categories
 * Includes automatic categorization rules and spending analysis
 */
@Serializable
data class Category(
    val categoryId: String,
    val name: String,
    val nameAr: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val level: CategoryLevel = CategoryLevel.MAIN,
    val categoryType: CategoryType = CategoryType.EXPENSE,
    val iconName: String,
    val colorHex: String,
    val isSystem: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val budgetLimit: Money? = null,
    val monthlyLimit: Money? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val metadata: Map<String, String> = emptyMap(),
    val subcategories: List<Category> = emptyList(),
    val rules: List<CategorizationRule> = emptyList(),
    val keywords: List<String> = emptyList(),
    val merchantPatterns: List<String> = emptyList()
) {
    
    /**
     * Check if this is a root category
     */
    fun isRootCategory(): Boolean = parentCategoryId == null && level == CategoryLevel.MAIN
    
    /**
     * Check if this is a subcategory
     */
    fun isSubcategory(): Boolean = parentCategoryId != null && level == CategoryLevel.SUB
    
    /**
     * Get full category path (Parent > Child)
     */
    fun getFullPath(): String {
        return if (parentCategoryId != null) {
            // In a real implementation, this would look up parent name
            "Parent > $name"
        } else {
            name
        }
    }
    
    /**
     * Get full Arabic category path
     */
    fun getFullPathAr(): String {
        return if (parentCategoryId != null) {
            "الوالد > $nameAr"
        } else {
            nameAr
        }
    }
    
    /**
     * Check if category matches a transaction description
     */
    fun matchesTransaction(description: String, merchantName: String? = null): Boolean {
        val desc = description.lowercase()
        val merchant = merchantName?.lowercase()
        
        // Check keywords
        val keywordMatch = keywords.any { keyword ->
            desc.contains(keyword.lowercase()) || 
            merchant?.contains(keyword.lowercase()) == true
        }
        
        // Check merchant patterns
        val merchantMatch = merchantPatterns.any { pattern ->
            desc.contains(pattern.lowercase()) || 
            merchant?.contains(pattern.lowercase()) == true
        }
        
        return keywordMatch || merchantMatch
    }
    
    /**
     * Calculate match confidence (0-100)
     */
    fun calculateMatchConfidence(description: String, merchantName: String? = null, amount: Money): Int {
        var confidence = 0
        val desc = description.lowercase()
        val merchant = merchantName?.lowercase()
        
        // Keyword matching (40 points max)
        val matchingKeywords = keywords.count { keyword ->
            desc.contains(keyword.lowercase()) || 
            merchant?.contains(keyword.lowercase()) == true
        }
        confidence += minOf(40, matchingKeywords * 10)
        
        // Merchant pattern matching (30 points max)
        val matchingPatterns = merchantPatterns.count { pattern ->
            desc.contains(pattern.lowercase()) || 
            merchant?.contains(pattern.lowercase()) == true
        }
        confidence += minOf(30, matchingPatterns * 15)
        
        // Amount range matching (20 points max)
        monthlyLimit?.let { limit ->
            if (amount <= limit) {
                confidence += 20
            } else if (amount <= limit * 2) {
                confidence += 10
            }
        }
        
        // Category type consistency (10 points max)
        when (categoryType) {
            CategoryType.INCOME -> {
                if (amount.isPositive()) confidence += 10
            }
            CategoryType.EXPENSE -> {
                if (amount.isPositive()) confidence += 10 // Expenses are usually positive amounts
            }
            CategoryType.TRANSFER -> {
                if (desc.contains("transfer") || desc.contains("تحويل")) confidence += 10
            }
        }
        
        return minOf(100, confidence)
    }
    
    /**
     * Get category statistics summary
     */
    fun getCategoryInfo(): CategoryInfo {
        return CategoryInfo(
            categoryId = categoryId,
            name = name,
            nameAr = nameAr,
            fullPath = getFullPath(),
            fullPathAr = getFullPathAr(),
            categoryType = categoryType,
            level = level,
            iconName = iconName,
            colorHex = colorHex,
            isActive = isActive,
            hasSubcategories = subcategories.isNotEmpty(),
            subcategoryCount = subcategories.size,
            ruleCount = rules.size,
            keywordCount = keywords.size
        )
    }
}

/**
 * Categorization rule for automatic transaction categorization
 */
@Serializable
data class CategorizationRule(
    val ruleId: String,
    val categoryId: String,
    val name: String,
    val ruleType: RuleType,
    val conditions: List<RuleCondition>,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val confidence: Int = 80, // Confidence level (0-100)
    val createdAt: Instant = Clock.System.now()
) {
    
    /**
     * Check if this rule matches a transaction
     */
    fun matches(description: String, merchantName: String?, amount: Money, accountId: String? = null): Boolean {
        return conditions.all { condition ->
            condition.evaluate(description, merchantName, amount, accountId)
        }
    }
}

/**
 * Rule condition for categorization
 */
@Serializable
data class RuleCondition(
    val field: ConditionField,
    val operator: ConditionOperator,
    val value: String,
    val caseSensitive: Boolean = false
) {
    
    /**
     * Evaluate this condition against transaction data
     */
    fun evaluate(description: String, merchantName: String?, amount: Money, accountId: String?): Boolean {
        val fieldValue = when (field) {
            ConditionField.DESCRIPTION -> description
            ConditionField.MERCHANT_NAME -> merchantName ?: ""
            ConditionField.AMOUNT -> amount.amount
            ConditionField.ACCOUNT_ID -> accountId ?: ""
        }
        
        val compareValue = if (caseSensitive) fieldValue else fieldValue.lowercase()
        val targetValue = if (caseSensitive) value else value.lowercase()
        
        return when (operator) {
            ConditionOperator.CONTAINS -> compareValue.contains(targetValue)
            ConditionOperator.EQUALS -> compareValue == targetValue
            ConditionOperator.STARTS_WITH -> compareValue.startsWith(targetValue)
            ConditionOperator.ENDS_WITH -> compareValue.endsWith(targetValue)
            ConditionOperator.REGEX -> try {
                Regex(value, if (caseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE))
                    .containsMatchIn(compareValue)
            } catch (e: Exception) {
                false
            }
            ConditionOperator.GREATER_THAN -> {
                if (field == ConditionField.AMOUNT) {
                    try {
                        amount.numericValue > value.toDouble()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    compareValue > targetValue
                }
            }
            ConditionOperator.LESS_THAN -> {
                if (field == ConditionField.AMOUNT) {
                    try {
                        amount.numericValue < value.toDouble()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    compareValue < targetValue
                }
            }
            ConditionOperator.NOT_EQUALS -> compareValue != targetValue
            ConditionOperator.NOT_CONTAINS -> !compareValue.contains(targetValue)
        }
    }
}

/**
 * Category levels in hierarchy
 */
@Serializable
enum class CategoryLevel(val displayName: String, val displayNameAr: String) {
    MAIN("Main Category", "فئة رئيسية"),
    SUB("Subcategory", "فئة فرعية"),
    DETAIL("Detail Category", "فئة تفصيلية")
}

/**
 * Category types
 */
@Serializable
enum class CategoryType(val displayName: String, val displayNameAr: String) {
    INCOME("Income", "دخل"),
    EXPENSE("Expense", "مصروف"),
    TRANSFER("Transfer", "تحويل"),
    INVESTMENT("Investment", "استثمار"),
    SAVINGS("Savings", "مدخرات"),
    LOAN("Loan", "قرض")
}

/**
 * Rule types for categorization
 */
@Serializable
enum class RuleType(val displayName: String, val displayNameAr: String) {
    KEYWORD_BASED("Keyword Based", "قائم على الكلمات المفتاحية"),
    MERCHANT_BASED("Merchant Based", "قائم على التاجر"),
    AMOUNT_BASED("Amount Based", "قائم على المبلغ"),
    PATTERN_BASED("Pattern Based", "قائم على النمط"),
    ML_BASED("Machine Learning", "تعلم الآلة"),
    COMPOSITE("Composite Rule", "قاعدة مركبة")
}

/**
 * Condition fields
 */
@Serializable
enum class ConditionField(val displayName: String, val displayNameAr: String) {
    DESCRIPTION("Description", "الوصف"),
    MERCHANT_NAME("Merchant Name", "اسم التاجر"),
    AMOUNT("Amount", "المبلغ"),
    ACCOUNT_ID("Account ID", "معرف الحساب")
}

/**
 * Condition operators
 */
@Serializable
enum class ConditionOperator(val displayName: String, val displayNameAr: String) {
    CONTAINS("Contains", "يحتوي على"),
    EQUALS("Equals", "يساوي"),
    STARTS_WITH("Starts With", "يبدأ بـ"),
    ENDS_WITH("Ends With", "ينتهي بـ"),
    REGEX("Regex Pattern", "نمط تعبير منتظم"),
    GREATER_THAN("Greater Than", "أكبر من"),
    LESS_THAN("Less Than", "أصغر من"),
    NOT_EQUALS("Not Equals", "لا يساوي"),
    NOT_CONTAINS("Not Contains", "لا يحتوي على")
}

/**
 * Category information for UI display
 */
@Serializable
data class CategoryInfo(
    val categoryId: String,
    val name: String,
    val nameAr: String,
    val fullPath: String,
    val fullPathAr: String,
    val categoryType: CategoryType,
    val level: CategoryLevel,
    val iconName: String,
    val colorHex: String,
    val isActive: Boolean,
    val hasSubcategories: Boolean,
    val subcategoryCount: Int,
    val ruleCount: Int,
    val keywordCount: Int
)

/**
 * Pre-defined Saudi-specific categories
 */
object DefaultCategories {
    
    val INCOME_CATEGORIES = listOf(
        Category(
            categoryId = "income_salary",
            name = "Salary",
            nameAr = "راتب",
            categoryType = CategoryType.INCOME,
            iconName = "work",
            colorHex = "#4CAF50",
            isSystem = true,
            keywords = listOf("salary", "راتب", "مرتب", "أجر")
        ),
        Category(
            categoryId = "income_business",
            name = "Business Income",
            nameAr = "دخل تجاري",
            categoryType = CategoryType.INCOME,
            iconName = "business",
            colorHex = "#2196F3",
            isSystem = true,
            keywords = listOf("business", "تجارة", "أعمال", "profit", "ربح")
        ),
        Category(
            categoryId = "income_investment",
            name = "Investment Returns",
            nameAr = "عوائد استثمارية",
            categoryType = CategoryType.INCOME,
            iconName = "trending_up",
            colorHex = "#FF9800",
            isSystem = true,
            keywords = listOf("dividend", "interest", "توزيعات", "عوائد", "أرباح")
        )
    )
    
    val EXPENSE_CATEGORIES = listOf(
        Category(
            categoryId = "food_dining",
            name = "Food & Dining",
            nameAr = "طعام ومطاعم",
            categoryType = CategoryType.EXPENSE,
            iconName = "restaurant",
            colorHex = "#F44336",
            isSystem = true,
            keywords = listOf("restaurant", "food", "مطعم", "طعام", "وجبة"),
            merchantPatterns = listOf("mcdonalds", "kfc", "starbucks", "subway")
        ),
        Category(
            categoryId = "transport",
            name = "Transportation",
            nameAr = "مواصلات",
            categoryType = CategoryType.EXPENSE,
            iconName = "directions_car",
            colorHex = "#9C27B0",
            isSystem = true,
            keywords = listOf("fuel", "gas", "petrol", "uber", "taxi", "وقود", "بنزين", "تاكسي"),
            merchantPatterns = listOf("aramco", "adnoc", "uber", "careem")
        ),
        Category(
            categoryId = "utilities",
            name = "Utilities",
            nameAr = "الخدمات العامة",
            categoryType = CategoryType.EXPENSE,
            iconName = "flash_on",
            colorHex = "#607D8B",
            isSystem = true,
            keywords = listOf("electricity", "water", "internet", "phone", "كهرباء", "ماء", "انترنت", "هاتف"),
            merchantPatterns = listOf("sec", "swcc", "stc", "mobily", "zain")
        ),
        Category(
            categoryId = "shopping",
            name = "Shopping",
            nameAr = "تسوق",
            categoryType = CategoryType.EXPENSE,
            iconName = "shopping_cart",
            colorHex = "#E91E63",
            isSystem = true,
            keywords = listOf("shopping", "store", "mall", "تسوق", "متجر", "مول"),
            merchantPatterns = listOf("carrefour", "lulu", "danube", "extra", "amazon")
        ),
        Category(
            categoryId = "healthcare",
            name = "Healthcare",
            nameAr = "رعاية صحية",
            categoryType = CategoryType.EXPENSE,
            iconName = "local_hospital",
            colorHex = "#009688",
            isSystem = true,
            keywords = listOf("hospital", "doctor", "pharmacy", "medicine", "مستشفى", "طبيب", "صيدلية", "دواء"),
            merchantPatterns = listOf("nahdi", "aldawaa", "hospital", "clinic")
        ),
        Category(
            categoryId = "education",
            name = "Education",
            nameAr = "تعليم",
            categoryType = CategoryType.EXPENSE,
            iconName = "school",
            colorHex = "#3F51B5",
            isSystem = true,
            keywords = listOf("school", "university", "education", "course", "مدرسة", "جامعة", "تعليم", "دورة")
        ),
        Category(
            categoryId = "entertainment",
            name = "Entertainment",
            nameAr = "ترفيه",
            categoryType = CategoryType.EXPENSE,
            iconName = "movie",
            colorHex = "#FF5722",
            isSystem = true,
            keywords = listOf("cinema", "movie", "game", "entertainment", "سينما", "فيلم", "لعبة", "ترفيه")
        ),
        Category(
            categoryId = "charity",
            name = "Charity & Donations",
            nameAr = "خيرية وتبرعات",
            categoryType = CategoryType.EXPENSE,
            iconName = "favorite",
            colorHex = "#795548",
            isSystem = true,
            keywords = listOf("charity", "donation", "zakat", "خيرية", "تبرع", "زكاة", "صدقة")
        )
    )
    
    val TRANSFER_CATEGORIES = listOf(
        Category(
            categoryId = "bank_transfer",
            name = "Bank Transfer",
            nameAr = "تحويل بنكي",
            categoryType = CategoryType.TRANSFER,
            iconName = "account_balance",
            colorHex = "#607D8B",
            isSystem = true,
            keywords = listOf("transfer", "تحويل", "حوالة")
        ),
        Category(
            categoryId = "payment_apps",
            name = "Payment Apps",
            nameAr = "تطبيقات الدفع",
            categoryType = CategoryType.TRANSFER,
            iconName = "payment",
            colorHex = "#9C27B0",
            isSystem = true,
            keywords = listOf("stcpay", "mada", "applepay", "googlepay"),
            merchantPatterns = listOf("stc pay", "mada", "apple pay", "google pay")
        )
    )
    
    /**
     * Get all default categories
     */
    fun getAllCategories(): List<Category> {
        return INCOME_CATEGORIES + EXPENSE_CATEGORIES + TRANSFER_CATEGORIES
    }
    
    /**
     * Get categories by type
     */
    fun getCategoriesByType(type: CategoryType): List<Category> {
        return when (type) {
            CategoryType.INCOME -> INCOME_CATEGORIES
            CategoryType.EXPENSE -> EXPENSE_CATEGORIES
            CategoryType.TRANSFER -> TRANSFER_CATEGORIES
            else -> emptyList()
        }
    }
}