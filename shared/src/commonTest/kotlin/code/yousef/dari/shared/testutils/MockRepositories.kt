package code.yousef.dari.shared.testutils

import code.yousef.dari.shared.data.repository.*
import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.LocalDate

/**
 * Mock repository implementations for testing
 * Provides in-memory implementations to avoid database dependencies in unit tests
 */

class MockCategoryRepository : CategoryRepository {
    
    private val categories = mutableMapOf<String, Category>()
    private val rules = mutableMapOf<String, CategorizationRule>()
    
    override suspend fun createCategory(category: Category): Result<Category> {
        categories[category.categoryId] = category
        return Result.success(category)
    }

    override suspend fun updateCategory(category: Category): Result<Category> {
        categories[category.categoryId] = category
        return Result.success(category)
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        categories.remove(categoryId)
        return Result.success(Unit)
    }

    override suspend fun getCategoryById(categoryId: String): Result<Category?> {
        return Result.success(categories[categoryId])
    }

    override suspend fun getActiveCategories(): Result<List<Category>> {
        return Result.success(categories.values.filter { it.isActive })
    }

    override suspend fun getAllCategories(): Result<List<Category>> {
        return Result.success(categories.values.toList())
    }

    override suspend fun getCategoriesByType(type: CategoryType): Result<List<Category>> {
        return Result.success(categories.values.filter { it.categoryType == type })
    }

    override suspend fun getRootCategories(): Result<List<Category>> {
        return Result.success(categories.values.filter { it.parentCategoryId == null })
    }

    override suspend fun getSubcategories(parentCategoryId: String): Result<List<Category>> {
        return Result.success(categories.values.filter { it.parentCategoryId == parentCategoryId })
    }

    override suspend fun getSystemCategories(): Result<List<Category>> {
        return Result.success(categories.values.filter { it.isSystemCategory })
    }

    override suspend fun getUserCategories(): Result<List<Category>> {
        return Result.success(categories.values.filter { !it.isSystemCategory })
    }

    override suspend fun searchCategories(query: String): Result<List<Category>> {
        return Result.success(
            categories.values.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
        )
    }

    override suspend fun findBestMatch(
        description: String,
        merchantName: String?,
        amount: Money,
        accountId: String?
    ): Result<CategoryMatch?> {
        // Simple mock implementation
        val category = categories.values.firstOrNull()
        return if (category != null) {
            Result.success(
                CategoryMatch(
                    category = category,
                    confidence = 85,
                    matchReasons = listOf(MatchReason(MatchType.KEYWORD, "mock", 85))
                )
            )
        } else {
            Result.success(null)
        }
    }

    override suspend fun getMatchingSuggestions(
        description: String,
        merchantName: String?,
        amount: Money,
        limit: Int
    ): Result<List<CategoryMatch>> {
        val matches = categories.values.take(limit).map { category ->
            CategoryMatch(
                category = category,
                confidence = 80,
                matchReasons = listOf(MatchReason(MatchType.KEYWORD, "mock", 80))
            )
        }
        return Result.success(matches)
    }

    override suspend fun createCategorizationRule(rule: CategorizationRule): Result<CategorizationRule> {
        rules[rule.ruleId] = rule
        return Result.success(rule)
    }

    override suspend fun updateCategorizationRule(rule: CategorizationRule): Result<CategorizationRule> {
        rules[rule.ruleId] = rule
        return Result.success(rule)
    }

    override suspend fun deleteCategorizationRule(ruleId: String): Result<Unit> {
        rules.remove(ruleId)
        return Result.success(Unit)
    }

    override suspend fun getCategoryRules(categoryId: String): Result<List<CategorizationRule>> {
        return Result.success(rules.values.filter { it.categoryId == categoryId })
    }

    override suspend fun getActiveRules(): Result<List<CategorizationRule>> {
        return Result.success(rules.values.filter { it.isActive })
    }

    override suspend fun testRule(
        rule: CategorizationRule,
        description: String,
        merchantName: String?,
        amount: Money
    ): Result<Boolean> {
        return Result.success(true) // Simple mock
    }

    override suspend fun addCategoryKeywords(categoryId: String, keywords: List<String>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun removeCategoryKeywords(categoryId: String, keywords: List<String>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun addMerchantPatterns(categoryId: String, patterns: List<String>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun removeMerchantPatterns(categoryId: String, patterns: List<String>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getCategoryUsageStats(categoryId: String): Result<CategoryUsageStats> {
        val category = categories[categoryId]
        return if (category != null) {
            Result.success(
                CategoryUsageStats(
                    categoryId = categoryId,
                    categoryName = category.name,
                    usageCount = 10,
                    totalAmount = Money("500.00", "SAR"),
                    averageAmount = Money("50.00", "SAR"),
                    lastUsedDate = null,
                    usageFrequency = 0.5
                )
            )
        } else {
            Result.failure(Exception("Category not found"))
        }
    }

    override suspend fun getMostUsedCategories(limit: Int): Result<List<CategoryUsageStats>> {
        return Result.success(emptyList())
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun importCategories(categories: List<Category>): Result<List<Category>> {
        categories.forEach { this.categories[it.categoryId] = it }
        return Result.success(categories)
    }

    override suspend fun exportCategories(): Result<List<Category>> {
        return getAllCategories()
    }

    override suspend fun isCategoryNameExists(name: String, parentId: String?): Result<Boolean> {
        return Result.success(
            categories.values.any { it.name == name && it.parentCategoryId == parentId }
        )
    }

    override suspend fun getCategoryTree(): Result<List<CategoryTreeNode>> {
        return Result.success(emptyList())
    }

    override suspend fun mergeCategories(sourceCategoryId: String, targetCategoryId: String): Result<Unit> {
        categories.remove(sourceCategoryId)
        return Result.success(Unit)
    }

    override suspend fun learnFromCategorization(
        transactionDescription: String,
        merchantName: String?,
        amount: Money,
        selectedCategoryId: String,
        confidence: Int
    ): Result<Unit> {
        return Result.success(Unit)
    }

    // Helper methods for tests
    fun addCategory(category: Category) {
        categories[category.categoryId] = category
    }

    fun clear() {
        categories.clear()
        rules.clear()
    }
}

class MockTransactionRepository : TransactionRepository {
    private val transactions = mutableMapOf<String, Transaction>()

    override suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        transactions[transaction.transactionId] = transaction
        return Result.success(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        transactions[transaction.transactionId] = transaction
        return Result.success(transaction)
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        transactions.remove(transactionId)
        return Result.success(Unit)
    }

    override suspend fun getTransactionById(transactionId: String): Result<Transaction?> {
        return Result.success(transactions[transactionId])
    }

    override suspend fun getTransactionsByAccount(
        accountId: String,
        limit: Int,
        offset: Int
    ): Result<List<Transaction>> {
        return Result.success(
            transactions.values
                .filter { it.accountId == accountId }
                .drop(offset)
                .take(limit)
        )
    }

    override suspend fun getTransactionsByDateRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Transaction>> {
        return Result.success(
            transactions.values.filter { 
                it.accountId == accountId && it.date >= startDate && it.date <= endDate 
            }
        )
    }

    override suspend fun searchTransactions(
        accountId: String,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: String
    ): Result<List<Transaction>> {
        return Result.success(
            transactions.values
                .filter { it.accountId == accountId && it.description.contains(query, ignoreCase = true) }
                .drop(offset)
                .take(limit)
        )
    }

    override suspend fun searchByCategory(
        accountId: String,
        category: String,
        limit: Int,
        offset: Int
    ): Result<List<Transaction>> {
        return Result.success(
            transactions.values
                .filter { it.accountId == accountId && it.categoryId == category }
                .drop(offset)
                .take(limit)
        )
    }

    // Helper methods for tests
    fun addTransaction(transaction: Transaction) {
        transactions[transaction.transactionId] = transaction
    }

    fun clear() {
        transactions.clear()
    }
}

class MockAccountRepository : AccountRepository {
    private val accounts = mutableMapOf<String, Account>()

    override suspend fun createAccount(account: Account): Result<Account> {
        accounts[account.accountId] = account
        return Result.success(account)
    }

    override suspend fun updateAccount(account: Account): Result<Account> {
        accounts[account.accountId] = account
        return Result.success(account)
    }

    override suspend fun deleteAccount(accountId: String): Result<Unit> {
        accounts.remove(accountId)
        return Result.success(Unit)
    }

    override suspend fun getAccountById(accountId: String): Result<Account?> {
        return Result.success(accounts[accountId])
    }

    override suspend fun getAllAccounts(): Result<List<Account>> {
        return Result.success(accounts.values.toList())
    }

    override suspend fun getActiveAccounts(): Result<List<Account>> {
        return Result.success(accounts.values.filter { it.isActive })
    }

    override suspend fun getAccountsByType(type: AccountType): Result<List<Account>> {
        return Result.success(accounts.values.filter { it.accountType == type })
    }

    override suspend fun updateAccountBalance(accountId: String, newBalance: Money): Result<Unit> {
        val account = accounts[accountId]
        if (account != null) {
            accounts[accountId] = account.copy(balance = newBalance)
            return Result.success(Unit)
        }
        return Result.failure(Exception("Account not found"))
    }

    // Helper methods for tests
    fun addAccount(account: Account) {
        accounts[account.accountId] = account
    }

    fun clear() {
        accounts.clear()
    }
}