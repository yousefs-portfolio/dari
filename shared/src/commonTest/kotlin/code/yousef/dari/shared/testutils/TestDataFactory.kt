package code.yousef.dari.shared.testutils

import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/**
 * Common test data factory for creating domain model instances
 * Reduces duplication across test files and provides consistent test data
 */
object TestDataFactory {

    private val random = Random.Default
    private val sampleCategories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Utilities")
    private val sampleMerchants = listOf("McDonald's", "Shell", "Amazon", "Netflix", "Saudi Electricity Company")
    private val sampleAccountNames = listOf("Checking Account", "Savings Account", "Credit Card")
    private val sampleBankCodes = listOf("RIBLSARI", "NCBKSAJE", "SABBSARI", "ALBISARI")

    /**
     * Creates a test Account with optional parameter overrides
     */
    fun createAccount(
        accountId: String = "test_account_${random.nextInt(1000)}",
        accountName: String = sampleAccountNames.random(),
        bankCode: String = sampleBankCodes.random(),
        accountNumber: String = "SA${random.nextLong(10000000, 99999999)}",
        balance: Money = Money("${random.nextDouble(100.0, 10000.0)}", "SAR"),
        accountType: AccountType = AccountType.CHECKING,
        isActive: Boolean = true,
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now()
    ) = Account(
        accountId = accountId,
        accountName = accountName,
        bankCode = bankCode,
        accountNumber = accountNumber,
        balance = balance,
        accountType = accountType,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Creates a test Transaction with optional parameter overrides
     */
    fun createTransaction(
        transactionId: String = "test_transaction_${random.nextInt(1000)}",
        accountId: String = "test_account_${random.nextInt(100)}",
        amount: Money = Money("${random.nextDouble(10.0, 500.0)}", "SAR"),
        description: String = "Test transaction ${random.nextInt(1000)}",
        merchantName: String? = sampleMerchants.random(),
        categoryId: String? = "test_category_${random.nextInt(10)}",
        transactionType: TransactionType = if (random.nextBoolean()) TransactionType.EXPENSE else TransactionType.INCOME,
        date: LocalDate = LocalDate(2024, random.nextInt(1, 13), random.nextInt(1, 29)),
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now()
    ) = Transaction(
        transactionId = transactionId,
        accountId = accountId,
        amount = amount,
        description = description,
        merchantName = merchantName,
        categoryId = categoryId,
        transactionType = transactionType,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Creates a test Category with optional parameter overrides
     */
    fun createCategory(
        categoryId: String = "test_category_${random.nextInt(1000)}",
        name: String = sampleCategories.random(),
        description: String = "Test category description",
        categoryType: CategoryType = CategoryType.EXPENSE,
        parentCategoryId: String? = null,
        iconName: String = "category",
        colorHex: String = "#FF0000",
        keywords: List<String> = listOf("test", "keyword"),
        merchantPatterns: List<String> = listOf("test", "pattern"),
        isActive: Boolean = true,
        isSystemCategory: Boolean = false,
        sortOrder: Int = random.nextInt(1, 100),
        level: Int = if (parentCategoryId == null) 0 else 1,
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now(),
        metadata: Map<String, String> = emptyMap()
    ) = Category(
        categoryId = categoryId,
        name = name,
        description = description,
        categoryType = categoryType,
        parentCategoryId = parentCategoryId,
        iconName = iconName,
        colorHex = colorHex,
        keywords = keywords,
        merchantPatterns = merchantPatterns,
        isActive = isActive,
        isSystemCategory = isSystemCategory,
        sortOrder = sortOrder,
        level = level,
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadata = metadata
    )

    /**
     * Creates a test Budget with optional parameter overrides
     */
    fun createBudget(
        budgetId: String = "test_budget_${random.nextInt(1000)}",
        name: String = "Test Budget",
        budgetType: BudgetType = BudgetType.MONTHLY,
        categoryId: String = "test_category_${random.nextInt(10)}",
        allocatedAmount: Money = Money("${random.nextDouble(100.0, 1000.0)}", "SAR"),
        spentAmount: Money = Money("${random.nextDouble(0.0, 100.0)}", "SAR"),
        period: BudgetPeriod = BudgetPeriod.CURRENT_MONTH,
        isActive: Boolean = true,
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now()
    ) = Budget(
        budgetId = budgetId,
        name = name,
        budgetType = budgetType,
        categoryId = categoryId,
        allocatedAmount = allocatedAmount,
        spentAmount = spentAmount,
        period = period,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Creates a test Goal with optional parameter overrides
     */
    fun createGoal(
        goalId: String = "test_goal_${random.nextInt(1000)}",
        name: String = "Test Goal",
        description: String = "Test goal description",
        goalType: GoalType = GoalType.SAVINGS,
        targetAmount: Money = Money("${random.nextDouble(1000.0, 10000.0)}", "SAR"),
        currentAmount: Money = Money("${random.nextDouble(0.0, 500.0)}", "SAR"),
        targetDate: LocalDate = LocalDate(2025, random.nextInt(1, 13), random.nextInt(1, 29)),
        status: GoalStatus = GoalStatus.ACTIVE,
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now()
    ) = Goal(
        goalId = goalId,
        name = name,
        description = description,
        goalType = goalType,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Creates a test CategoryMatch with optional parameter overrides
     */
    fun createCategoryMatch(
        category: Category = createCategory(),
        confidence: Int = random.nextInt(50, 100),
        matchReasons: List<MatchReason> = listOf(
            MatchReason(MatchType.KEYWORD, "test keyword", confidence)
        )
    ) = CategoryMatch(
        category = category,
        confidence = confidence,
        matchReasons = matchReasons
    )

    /**
     * Creates a test CategorizationRule with optional parameter overrides
     */
    fun createCategorizationRule(
        ruleId: String = "test_rule_${random.nextInt(1000)}",
        categoryId: String = "test_category_${random.nextInt(10)}",
        name: String = "Test Rule",
        description: String = "Test rule description",
        ruleType: RuleType = RuleType.AUTOMATIC,
        conditions: List<RuleCondition> = listOf(
            RuleCondition(
                type = RuleConditionType.DESCRIPTION_CONTAINS,
                operator = RuleOperator.EQUALS,
                value = "test"
            )
        ),
        priority: Int = random.nextInt(1, 10),
        isActive: Boolean = true,
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now()
    ) = CategorizationRule(
        ruleId = ruleId,
        categoryId = categoryId,
        name = name,
        description = description,
        ruleType = ruleType,
        conditions = conditions,
        priority = priority,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * Creates a list of test transactions
     */
    fun createTransactionList(count: Int = 5, accountId: String = "test_account"): List<Transaction> {
        return (1..count).map { index ->
            createTransaction(
                transactionId = "transaction_$index",
                accountId = accountId,
                description = "Transaction $index"
            )
        }
    }

    /**
     * Creates a list of test categories with parent-child relationships
     */
    fun createCategoryHierarchy(): List<Category> {
        val parent = createCategory(
            categoryId = "parent_category",
            name = "Parent Category",
            level = 0
        )
        
        val child1 = createCategory(
            categoryId = "child_category_1",
            name = "Child Category 1",
            parentCategoryId = parent.categoryId,
            level = 1
        )
        
        val child2 = createCategory(
            categoryId = "child_category_2",
            name = "Child Category 2",
            parentCategoryId = parent.categoryId,
            level = 1
        )
        
        return listOf(parent, child1, child2)
    }
}