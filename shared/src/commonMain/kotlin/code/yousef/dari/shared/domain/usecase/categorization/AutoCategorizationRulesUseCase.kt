package code.yousef.dari.shared.domain.usecase.categorization

import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.model.TransactionType
import code.yousef.dari.shared.domain.repository.CategoryRepository
import java.math.BigDecimal
import java.util.*

data class CategorizationResult(
    val categoryId: String,
    val categoryName: String,
    val confidence: Double,
    val rules: List<String>
)

data class CategorizationRule(
    val name: String,
    val categoryName: String,
    val priority: Int,
    val conditions: List<RuleCondition>,
    val confidence: Double
)

data class RuleCondition(
    val field: String, // "merchant", "description", "amount", "type"
    val operator: String, // "contains", "equals", "greater_than", "less_than", "regex"
    val value: String,
    val weight: Double = 1.0
)

class AutoCategorizationRulesUseCase(
    private val categoryRepository: CategoryRepository
) {
    private val saudiMerchantPatterns = mapOf(
        // Grocery stores
        "panda" to "Groceries",
        "carrefour" to "Groceries",
        "lulu" to "Groceries",
        "danube" to "Groceries",
        "bin dawood" to "Groceries",
        "tamimi" to "Groceries",
        "al othaim" to "Groceries",

        // Gas stations
        "aramco" to "Transportation",
        "shell" to "Transportation",
        "mobil" to "Transportation",
        "total" to "Transportation",
        "petro" to "Transportation",

        // Restaurants & Fast Food
        "mcdonald" to "Dining",
        "burger king" to "Dining",
        "kfc" to "Dining",
        "pizza hut" to "Dining",
        "domino" to "Dining",
        "hardee" to "Dining",
        "herfy" to "Dining",
        "kudu" to "Dining",
        "maroush" to "Dining",

        // Shopping
        "jarir" to "Shopping",
        "extra" to "Shopping",
        "saco" to "Shopping",
        "virgin" to "Shopping",
        "centrepoint" to "Shopping",
        "marks & spencer" to "Shopping",
        "h&m" to "Shopping",
        "zara" to "Shopping",

        // Pharmacies
        "al dawaa" to "Healthcare",
        "nahdi" to "Healthcare",
        "pharmacy" to "Healthcare",
        "صيدلية" to "Healthcare",

        // Banks & ATMs
        "ncb" to "Cash & ATM",
        "sabb" to "Cash & ATM",
        "rajhi" to "Cash & ATM",
        "riyad bank" to "Cash & ATM",
        "alinma" to "Cash & ATM",
        "atm" to "Cash & ATM",

        // Telecom
        "stc" to "Utilities",
        "mobily" to "Utilities",
        "zain" to "Utilities",

        // Utilities
        "sec" to "Utilities", // Saudi Electricity Company
        "nwc" to "Utilities", // National Water Company
        "electricity" to "Utilities",
        "water" to "Utilities"
    )

    private val globalMerchantPatterns = mapOf(
        // Streaming & Entertainment
        "netflix" to "Entertainment",
        "amazon prime" to "Entertainment",
        "disney" to "Entertainment",
        "hbo" to "Entertainment",
        "spotify" to "Entertainment",
        "apple music" to "Entertainment",
        "youtube" to "Entertainment",
        "anghami" to "Entertainment",

        // Food Delivery
        "hungerstation" to "Dining",
        "talabat" to "Dining",
        "jahez" to "Dining",
        "deliveroo" to "Dining",
        "uber eats" to "Dining",

        // Transportation
        "uber" to "Transportation",
        "careem" to "Transportation",
        "lyft" to "Transportation",

        // Technology
        "microsoft" to "Software",
        "adobe" to "Software",
        "google" to "Software",
        "apple" to "Software",
        "amazon" to "Shopping",

        // Airlines
        "saudia" to "Travel",
        "flynas" to "Travel",
        "emirates" to "Travel",
        "etihad" to "Travel",
        "flyadeal" to "Travel"
    )

    private val keywordPatterns = mapOf(
        // Income patterns
        listOf("salary", "payroll", "wages", "راتب", "أجر") to "Salary",
        listOf("bonus", "commission", "مكافأة", "عمولة") to "Income",
        listOf("refund", "return", "مرتجع", "استرداد") to "Refunds",

        // Expense patterns
        listOf("rent", "housing", "إيجار", "سكن") to "Housing",
        listOf("grocery", "supermarket", "food", "بقالة", "طعام") to "Groceries",
        listOf("restaurant", "cafe", "coffee", "مطعم", "مقهى") to "Dining",
        listOf("gas", "fuel", "petrol", "وقود", "بنزين") to "Transportation",
        listOf("pharmacy", "medicine", "medical", "صيدلية", "دواء") to "Healthcare",
        listOf("subscription", "monthly", "اشتراك", "شهري") to "Entertainment",
        listOf("utility", "electricity", "water", "كهرباء", "ماء") to "Utilities",
        listOf("cash", "withdrawal", "atm", "صراف", "سحب") to "Cash & ATM"
    )

    suspend fun categorize(transaction: Transaction): CategorizationResult? {
        val merchantName = transaction.merchantName.lowercase(Locale.getDefault())
        val description = transaction.description.lowercase(Locale.getDefault())
        val amount = transaction.amount.amount.abs()
        val isIncome = transaction.type == TransactionType.CREDIT

        val matchingRules = mutableListOf<Pair<String, Double>>()

        // Rule 1: Saudi merchant patterns (highest priority)
        saudiMerchantPatterns.forEach { (pattern, category) ->
            if (merchantName.contains(pattern) || description.contains(pattern)) {
                matchingRules.add(Pair(category, 0.95))
            }
        }

        // Rule 2: Global merchant patterns
        globalMerchantPatterns.forEach { (pattern, category) ->
            if (merchantName.contains(pattern) || description.contains(pattern)) {
                matchingRules.add(Pair(category, 0.90))
            }
        }

        // Rule 3: Keyword patterns
        keywordPatterns.forEach { (keywords, category) ->
            keywords.forEach { keyword ->
                if (merchantName.contains(keyword) || description.contains(keyword)) {
                    matchingRules.add(Pair(category, 0.85))
                }
            }
        }

        // Rule 4: Transaction type patterns
        if (isIncome) {
            when {
                merchantName.contains("payroll") || description.contains("salary") ->
                    matchingRules.add(Pair("Salary", 0.95))

                amount > BigDecimal("5000") ->
                    matchingRules.add(Pair("Salary", 0.80))

                else ->
                    matchingRules.add(Pair("Income", 0.70))
            }
        }

        // Rule 5: Amount-based patterns
        if (!isIncome) {
            when {
                amount >= BigDecimal("2000") && amount <= BigDecimal("5000") -> {
                    if (merchantName.contains("property") || description.contains("rent")) {
                        matchingRules.add(Pair("Housing", 0.90))
                    }
                }

                amount >= BigDecimal("50") && amount <= BigDecimal("200") &&
                    (merchantName.contains("restaurant") || description.contains("dining")) -> {
                    matchingRules.add(Pair("Dining", 0.85))
                }

                amount == BigDecimal("500") || amount == BigDecimal("1000") -> {
                    if (merchantName.contains("atm") || description.contains("cash")) {
                        matchingRules.add(Pair("Cash & ATM", 0.90))
                    }
                }
            }
        }

        // Rule 6: Recurring transaction patterns (subscription detection)
        if (amount < BigDecimal("200") && !isIncome) {
            val recurringIndicators = listOf(".com", "monthly", "subscription", "premium", "plus")
            if (recurringIndicators.any { merchantName.contains(it) || description.contains(it) }) {
                matchingRules.add(Pair("Entertainment", 0.80))
            }
        }

        // Select the best matching rule
        val bestMatch = matchingRules
            .groupBy { it.first }
            .mapValues { entry -> entry.value.maxOf { it.second } }
            .maxByOrNull { it.value }

        if (bestMatch != null && bestMatch.value > 0.7) {
            val category = categoryRepository.getCategoryByName(bestMatch.key)
            if (category != null) {
                return CategorizationResult(
                    categoryId = category.id,
                    categoryName = category.name,
                    confidence = bestMatch.value,
                    rules = determineAppliedRules(transaction, bestMatch.key, bestMatch.value)
                )
            }
        }

        return null
    }

    fun getConfidenceLevel(confidence: Double): String {
        return when {
            confidence >= 0.95 -> "Very High"
            confidence >= 0.85 -> "High"
            confidence >= 0.75 -> "Medium"
            confidence >= 0.65 -> "Low"
            else -> "Very Low"
        }
    }

    fun explainCategorization(result: CategorizationResult): String {
        val confidenceLevel = getConfidenceLevel(result.confidence)
        val rules = result.rules.joinToString(", ")

        return "Categorized as '${result.categoryName}' with $confidenceLevel confidence (${(result.confidence * 100).toInt()}%) " +
            "based on: $rules"
    }

    private fun determineAppliedRules(
        transaction: Transaction,
        category: String,
        confidence: Double
    ): List<String> {
        val rules = mutableListOf<String>()
        val merchantName = transaction.merchantName.lowercase()
        val description = transaction.description.lowercase()

        // Check which rules were applied
        if (saudiMerchantPatterns.any { merchantName.contains(it.key) }) {
            rules.add("saudi_merchant_match")
        }

        if (globalMerchantPatterns.any { merchantName.contains(it.key) }) {
            rules.add("global_merchant_match")
        }

        if (keywordPatterns.any { entry ->
                entry.key.any { keyword -> merchantName.contains(keyword) || description.contains(keyword) }
            }) {
            rules.add("keyword_match")
        }

        if (transaction.type == TransactionType.CREDIT) {
            rules.add("income_type")
        }

        val amount = transaction.amount.amount.abs()
        when {
            amount >= BigDecimal("2000") -> rules.add("large_amount_pattern")
            amount <= BigDecimal("50") -> rules.add("small_amount_pattern")
            else -> rules.add("medium_amount_pattern")
        }

        if (merchantName.contains(".com") || description.contains("subscription")) {
            rules.add("subscription_pattern")
        }

        if (confidence >= 0.95) {
            rules.add("high_confidence_match")
        }

        return rules
    }
}
