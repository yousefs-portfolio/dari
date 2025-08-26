package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.CategoryRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Use case for creating a new budget
 * Validates budget data and creates with proper categorization
 */
class CreateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    
    /**
     * Create a new budget
     */
    suspend fun execute(request: CreateBudgetRequest): Result<Budget> {
        return try {
            // Validate request
            val validationResult = validateRequest(request)
            if (validationResult.isFailure) {
                return validationResult
            }
            
            // Create budget with validated data
            val budget = createBudgetFromRequest(request)
            
            // Save to repository
            val savedBudget = budgetRepository.createBudget(budget).getOrThrow()
            
            Result.success(savedBudget)
        } catch (e: Exception) {
            Result.failure(BudgetCreationException("Failed to create budget: ${e.message}", e))
        }
    }
    
    private suspend fun validateRequest(request: CreateBudgetRequest): Result<Unit> {
        // Validate basic fields
        if (request.name.isBlank()) {
            return Result.failure(BudgetValidationException("Budget name cannot be blank"))
        }
        
        if (request.totalAmount.isNegative() || request.totalAmount.isZero()) {
            return Result.failure(BudgetValidationException("Budget amount must be positive"))
        }
        
        // Validate dates
        val now = Clock.System.now()
        if (request.startDate > (request.endDate ?: now.plus(kotlin.time.Duration.parse("P365D")))) {
            return Result.failure(BudgetValidationException("Start date cannot be after end date"))
        }
        
        // Validate categories if provided
        if (request.categories.isNotEmpty()) {
            val categoryValidation = validateBudgetCategories(request.categories, request.totalAmount)
            if (categoryValidation.isFailure) {
                return categoryValidation
            }
        }
        
        // Validate period matches date range
        if (!isValidPeriodForDateRange(request.period, request.startDate, request.endDate)) {
            return Result.failure(BudgetValidationException("Budget period does not match date range"))
        }
        
        return Result.success(Unit)
    }
    
    private suspend fun validateBudgetCategories(
        categories: List<CreateBudgetCategoryRequest>, 
        totalBudget: Money
    ): Result<Unit> {
        val totalAllocated = categories.fold(Money.fromInt(0, totalBudget.currency)) { acc, category ->
            acc + category.allocatedAmount
        }
        
        if (totalAllocated > totalBudget) {
            return Result.failure(BudgetValidationException(
                "Total allocated amount (${totalAllocated.format()}) exceeds budget (${totalBudget.format()})"
            ))
        }
        
        // Validate each category exists
        for (categoryRequest in categories) {
            val category = categoryRepository.getCategoryById(categoryRequest.categoryId).getOrNull()
            if (category == null) {
                return Result.failure(BudgetValidationException(
                    "Category not found: ${categoryRequest.categoryId}"
                ))
            }
            
            if (categoryRequest.allocatedAmount.isNegative() || categoryRequest.allocatedAmount.isZero()) {
                return Result.failure(BudgetValidationException(
                    "Category allocation must be positive: ${category.name}"
                ))
            }
        }
        
        return Result.success(Unit)
    }
    
    private fun isValidPeriodForDateRange(
        period: BudgetPeriod, 
        startDate: Instant, 
        endDate: Instant?
    ): Boolean {
        if (period == BudgetPeriod.CUSTOM) return true
        if (endDate == null) return true
        
        val actualDays = (endDate - startDate).inWholeDays
        val expectedDays = period.durationDays
        
        // Allow 10% variance for month variations (28-31 days)
        val tolerance = (expectedDays * 0.1).toInt()
        
        return kotlin.math.abs(actualDays - expectedDays) <= tolerance
    }
    
    private suspend fun createBudgetFromRequest(request: CreateBudgetRequest): Budget {
        val now = Clock.System.now()
        
        // Create budget categories
        val budgetCategories = request.categories.map { categoryRequest ->
            BudgetCategory(
                categoryId = categoryRequest.categoryId,
                categoryName = categoryRequest.categoryName,
                allocatedAmount = categoryRequest.allocatedAmount,
                spentAmount = Money.fromInt(0, categoryRequest.allocatedAmount.currency),
                isFixed = categoryRequest.isFixed,
                priority = categoryRequest.priority,
                notes = categoryRequest.notes
            )
        }
        
        return Budget(
            budgetId = generateBudgetId(),
            name = request.name,
            description = request.description,
            budgetType = request.budgetType,
            period = request.period,
            startDate = request.startDate,
            endDate = request.endDate,
            totalAmount = request.totalAmount,
            currency = request.totalAmount.currency,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            categories = budgetCategories,
            rolloverSettings = request.rolloverSettings,
            alertSettings = request.alertSettings,
            metadata = request.metadata
        )
    }
    
    private fun generateBudgetId(): String {
        return "budget_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}

/**
 * Request data for creating a budget
 */
data class CreateBudgetRequest(
    val name: String,
    val description: String? = null,
    val budgetType: BudgetType,
    val period: BudgetPeriod,
    val startDate: Instant,
    val endDate: Instant? = null,
    val totalAmount: Money,
    val categories: List<CreateBudgetCategoryRequest> = emptyList(),
    val rolloverSettings: RolloverSettings = RolloverSettings(),
    val alertSettings: AlertSettings = AlertSettings(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Request data for creating budget categories
 */
data class CreateBudgetCategoryRequest(
    val categoryId: String,
    val categoryName: String,
    val allocatedAmount: Money,
    val isFixed: Boolean = false,
    val priority: CategoryPriority = CategoryPriority.MEDIUM,
    val notes: String? = null
)

/**
 * Exception for budget creation errors
 */
class BudgetCreationException(
    message: String, 
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception for budget validation errors
 */
class BudgetValidationException(
    message: String
) : Exception(message)