package code.yousef.dari.shared.domain.validation

/**
 * Common validation utilities for use across use cases and domain objects
 * Provides standardized validation methods following DRY principle
 */
object CommonValidators {

    /**
     * Validates that an account ID is not blank
     * @param accountId The account ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateAccountId(accountId: String) {
        if (accountId.isBlank()) {
            throw IllegalArgumentException("Account ID cannot be empty")
        }
    }

    /**
     * Validates that a user ID is not blank
     * @param userId The user ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateUserId(userId: String) {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID cannot be empty")
        }
    }

    /**
     * Validates that a bank code is not blank
     * @param bankCode The bank code to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateBankCode(bankCode: String) {
        if (bankCode.isBlank()) {
            throw IllegalArgumentException("Bank code cannot be empty")
        }
    }

    /**
     * Validates that a search query is not blank and meets minimum requirements
     * @param query The search query to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateQuery(query: String) {
        if (query.isBlank()) {
            throw IllegalArgumentException("Search query cannot be empty")
        }
        if (query.trim().length < 2) {
            throw IllegalArgumentException("Search query must be at least 2 characters long")
        }
    }

    /**
     * Validates that a redirect URL is not blank and has basic URL format
     * @param redirectUrl The redirect URL to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateRedirectUrl(redirectUrl: String) {
        if (redirectUrl.isBlank()) {
            throw IllegalArgumentException("Redirect URL cannot be empty")
        }
        if (!redirectUrl.startsWith("http://") && !redirectUrl.startsWith("https://")) {
            throw IllegalArgumentException("Redirect URL must be a valid HTTP/HTTPS URL")
        }
    }

    /**
     * Validates that a transaction ID is not blank
     * @param transactionId The transaction ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateTransactionId(transactionId: String) {
        if (transactionId.isBlank()) {
            throw IllegalArgumentException("Transaction ID cannot be empty")
        }
    }

    /**
     * Validates that a category ID is not blank
     * @param categoryId The category ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateCategoryId(categoryId: String) {
        if (categoryId.isBlank()) {
            throw IllegalArgumentException("Category ID cannot be empty")
        }
    }

    /**
     * Validates that a goal ID is not blank
     * @param goalId The goal ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateGoalId(goalId: String) {
        if (goalId.isBlank()) {
            throw IllegalArgumentException("Goal ID cannot be empty")
        }
    }

    /**
     * Validates that a budget ID is not blank
     * @param budgetId The budget ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    fun validateBudgetId(budgetId: String) {
        if (budgetId.isBlank()) {
            throw IllegalArgumentException("Budget ID cannot be empty")
        }
    }

    /**
     * Validates pagination parameters
     * @param limit Maximum number of results
     * @param offset Number of results to skip
     * @throws IllegalArgumentException if validation fails
     */
    fun validatePagination(limit: Int, offset: Int) {
        if (limit <= 0) {
            throw IllegalArgumentException("Limit must be positive")
        }
        if (limit > 1000) {
            throw IllegalArgumentException("Limit cannot exceed 1000")
        }
        if (offset < 0) {
            throw IllegalArgumentException("Offset cannot be negative")
        }
    }

    /**
     * Validates that a name/title is not blank and meets length requirements
     * @param name The name to validate
     * @param fieldName The field name for error messages
     * @param maxLength Maximum allowed length (default 255)
     * @throws IllegalArgumentException if validation fails
     */
    fun validateName(name: String, fieldName: String = "Name", maxLength: Int = 255) {
        if (name.isBlank()) {
            throw IllegalArgumentException("$fieldName cannot be empty")
        }
        if (name.length > maxLength) {
            throw IllegalArgumentException("$fieldName cannot exceed $maxLength characters")
        }
    }

    /**
     * Validates that a description meets length requirements (can be blank)
     * @param description The description to validate
     * @param maxLength Maximum allowed length (default 1000)
     * @throws IllegalArgumentException if validation fails
     */
    fun validateDescription(description: String?, maxLength: Int = 1000) {
        if (description != null && description.length > maxLength) {
            throw IllegalArgumentException("Description cannot exceed $maxLength characters")
        }
    }
}