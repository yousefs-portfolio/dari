package code.yousef.dari.shared.domain.usecase.base

import code.yousef.dari.shared.domain.validation.CommonValidators

/**
 * Base use case class providing common functionality
 * Reduces duplication across use case implementations
 */
abstract class BaseUseCase {

    /**
     * Executes an operation with common error handling
     */
    protected suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates account ID using common validator
     */
    protected fun validateAccountId(accountId: String) {
        CommonValidators.validateAccountId(accountId)
    }

    /**
     * Validates user ID using common validator
     */
    protected fun validateUserId(userId: String) {
        CommonValidators.validateUserId(userId)
    }

    /**
     * Validates search query using common validator
     */
    protected fun validateQuery(query: String) {
        CommonValidators.validateQuery(query)
    }

    /**
     * Validates pagination parameters using common validator
     */
    protected fun validatePagination(limit: Int, offset: Int) {
        CommonValidators.validatePagination(limit, offset)
    }

    /**
     * Validates transaction ID using common validator
     */
    protected fun validateTransactionId(transactionId: String) {
        CommonValidators.validateTransactionId(transactionId)
    }

    /**
     * Validates category ID using common validator
     */
    protected fun validateCategoryId(categoryId: String) {
        CommonValidators.validateCategoryId(categoryId)
    }

    /**
     * Validates goal ID using common validator
     */
    protected fun validateGoalId(goalId: String) {
        CommonValidators.validateGoalId(goalId)
    }

    /**
     * Validates budget ID using common validator
     */
    protected fun validateBudgetId(budgetId: String) {
        CommonValidators.validateBudgetId(budgetId)
    }

    /**
     * Validates name/title using common validator
     */
    protected fun validateName(name: String, fieldName: String = "Name", maxLength: Int = 255) {
        CommonValidators.validateName(name, fieldName, maxLength)
    }

    /**
     * Validates description using common validator
     */
    protected fun validateDescription(description: String?, maxLength: Int = 1000) {
        CommonValidators.validateDescription(description, maxLength)
    }
}