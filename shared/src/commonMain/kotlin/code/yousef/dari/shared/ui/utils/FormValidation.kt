package code.yousef.dari.shared.ui.utils

import code.yousef.dari.shared.domain.models.*

/**
 * Common form validation utilities to reduce duplicate validation logic
 */

/**
 * Validation result for form fields
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun valid() = ValidationResult(true)
        fun invalid(message: String) = ValidationResult(false, message)
    }
}

/**
 * Common form field validators
 */
object FormValidators {
    
    // Amount validation
    fun validateAmount(amount: String, required: Boolean = true): ValidationResult {
        return when {
            required && amount.isBlank() -> ValidationResult.invalid("Amount is required")
            !required && amount.isBlank() -> ValidationResult.valid()
            amount.toDoubleOrNull() == null -> ValidationResult.invalid("Invalid amount format")
            amount.toDoubleOrNull()!! <= 0 -> ValidationResult.invalid("Amount must be greater than 0")
            amount.toDoubleOrNull()!! > 1_000_000_000 -> ValidationResult.invalid("Amount too large")
            else -> ValidationResult.valid()
        }
    }
    
    // Name/Title validation
    fun validateName(name: String, fieldName: String = "Name", required: Boolean = true): ValidationResult {
        return when {
            required && name.isBlank() -> ValidationResult.invalid("$fieldName is required")
            !required && name.isBlank() -> ValidationResult.valid()
            name.length < 2 -> ValidationResult.invalid("$fieldName must be at least 2 characters")
            name.length > 100 -> ValidationResult.invalid("$fieldName must be less than 100 characters")
            else -> ValidationResult.valid()
        }
    }
    
    // Description validation
    fun validateDescription(description: String, maxLength: Int = 500, required: Boolean = false): ValidationResult {
        return when {
            required && description.isBlank() -> ValidationResult.invalid("Description is required")
            !required && description.isBlank() -> ValidationResult.valid()
            description.length > maxLength -> ValidationResult.invalid("Description must be less than $maxLength characters")
            else -> ValidationResult.valid()
        }
    }
    
    // Category validation
    fun validateCategory(category: Category?, required: Boolean = true): ValidationResult {
        return when {
            required && category == null -> ValidationResult.invalid("Category is required")
            else -> ValidationResult.valid()
        }
    }
    
    // Account validation
    fun validateAccount(account: Account?, required: Boolean = true): ValidationResult {
        return when {
            required && account == null -> ValidationResult.invalid("Account is required")
            else -> ValidationResult.valid()
        }
    }
    
    // Email validation
    fun validateEmail(email: String, required: Boolean = true): ValidationResult {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return when {
            required && email.isBlank() -> ValidationResult.invalid("Email is required")
            !required && email.isBlank() -> ValidationResult.valid()
            !email.matches(emailRegex) -> ValidationResult.invalid("Invalid email format")
            else -> ValidationResult.valid()
        }
    }
    
    // Phone validation (Saudi numbers)
    fun validatePhone(phone: String, required: Boolean = true): ValidationResult {
        val cleanedPhone = phone.replace(Regex("[^\\d]"), "")
        return when {
            required && phone.isBlank() -> ValidationResult.invalid("Phone number is required")
            !required && phone.isBlank() -> ValidationResult.valid()
            cleanedPhone.length < 9 -> ValidationResult.invalid("Invalid phone number")
            !cleanedPhone.startsWith("05") && !cleanedPhone.startsWith("9665") -> 
                ValidationResult.invalid("Invalid Saudi phone number format")
            else -> ValidationResult.valid()
        }
    }
    
    // Percentage validation (for budget alerts, etc.)
    fun validatePercentage(percentage: Double, min: Double = 0.0, max: Double = 100.0): ValidationResult {
        return when {
            percentage < min -> ValidationResult.invalid("Value must be at least $min%")
            percentage > max -> ValidationResult.invalid("Value must be at most $max%")
            else -> ValidationResult.valid()
        }
    }
    
    // Date validation
    fun validateFutureDate(date: kotlinx.datetime.LocalDate, required: Boolean = true): ValidationResult {
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        return when {
            required && date < today -> ValidationResult.invalid("Date must be in the future")
            else -> ValidationResult.valid()
        }
    }
    
    // Password validation
    fun validatePassword(password: String, minLength: Int = 8): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.invalid("Password is required")
            password.length < minLength -> ValidationResult.invalid("Password must be at least $minLength characters")
            !password.any { it.isUpperCase() } -> ValidationResult.invalid("Password must contain at least one uppercase letter")
            !password.any { it.isLowerCase() } -> ValidationResult.invalid("Password must contain at least one lowercase letter")
            !password.any { it.isDigit() } -> ValidationResult.invalid("Password must contain at least one number")
            else -> ValidationResult.valid()
        }
    }
    
    // Confirm password validation
    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult.invalid("Please confirm your password")
            password != confirmPassword -> ValidationResult.invalid("Passwords do not match")
            else -> ValidationResult.valid()
        }
    }
}

/**
 * Form state management for complex forms
 */
data class FormFieldState<T>(
    val value: T,
    val validation: ValidationResult = ValidationResult.valid(),
    val isDirty: Boolean = false
) {
    val isValid: Boolean get() = validation.isValid
    val errorMessage: String? get() = if (isDirty) validation.errorMessage else null
    
    fun updateValue(newValue: T): FormFieldState<T> {
        return copy(value = newValue, isDirty = true)
    }
    
    fun validate(validator: (T) -> ValidationResult): FormFieldState<T> {
        return copy(validation = validator(value))
    }
    
    fun markClean(): FormFieldState<T> {
        return copy(isDirty = false)
    }
}

/**
 * Complete form state management
 */
class FormState {
    private val fields = mutableMapOf<String, FormFieldState<*>>()
    
    fun <T> addField(name: String, initialValue: T): FormFieldState<T> {
        val field = FormFieldState(initialValue)
        fields[name] = field
        return field
    }
    
    fun <T> getField(name: String): FormFieldState<T>? {
        @Suppress("UNCHECKED_CAST")
        return fields[name] as? FormFieldState<T>
    }
    
    fun <T> updateField(name: String, value: T, validator: ((T) -> ValidationResult)? = null): FormFieldState<T>? {
        @Suppress("UNCHECKED_CAST")
        val currentField = fields[name] as? FormFieldState<T>
        return currentField?.let { field ->
            val updatedField = field.updateValue(value)
            val validatedField = validator?.let { updatedField.validate(it) } ?: updatedField
            fields[name] = validatedField
            validatedField
        }
    }
    
    val isValid: Boolean
        get() = fields.values.all { it.isValid }
    
    val hasErrors: Boolean
        get() = fields.values.any { it.isDirty && !it.isValid }
    
    fun validate() {
        // Trigger validation on all fields
        // This would be implemented based on specific form requirements
    }
    
    fun reset() {
        fields.clear()
    }
}

/**
 * Transaction form validation
 */
object TransactionFormValidation {
    data class TransactionValidationState(
        val amountError: String? = null,
        val categoryError: String? = null,
        val accountError: String? = null,
        val descriptionError: String? = null
    ) {
        val isValid: Boolean
            get() = amountError == null && categoryError == null && accountError == null && descriptionError == null
    }
    
    fun validateTransactionForm(
        amount: String,
        category: Category?,
        account: Account?,
        description: String = ""
    ): TransactionValidationState {
        return TransactionValidationState(
            amountError = FormValidators.validateAmount(amount).errorMessage,
            categoryError = FormValidators.validateCategory(category).errorMessage,
            accountError = FormValidators.validateAccount(account).errorMessage,
            descriptionError = FormValidators.validateDescription(description, required = false).errorMessage
        )
    }
}

/**
 * Budget form validation
 */
object BudgetFormValidation {
    data class BudgetValidationState(
        val nameError: String? = null,
        val amountError: String? = null,
        val categoryError: String? = null,
        val thresholdError: String? = null
    ) {
        val isValid: Boolean
            get() = nameError == null && amountError == null && categoryError == null && thresholdError == null
    }
    
    fun validateBudgetForm(
        name: String,
        amount: String,
        category: Category?,
        alertThreshold: Double
    ): BudgetValidationState {
        return BudgetValidationState(
            nameError = FormValidators.validateName(name, "Budget name").errorMessage,
            amountError = FormValidators.validateAmount(amount).errorMessage,
            categoryError = FormValidators.validateCategory(category).errorMessage,
            thresholdError = FormValidators.validatePercentage(alertThreshold, 0.0, 100.0).errorMessage
        )
    }
}