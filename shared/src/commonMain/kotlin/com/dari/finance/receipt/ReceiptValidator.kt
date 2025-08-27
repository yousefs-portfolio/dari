package com.dari.finance.receipt

import com.dari.finance.ocr.ReceiptData
import com.dari.finance.ocr.ReceiptItem
import kotlin.math.abs

/**
 * Receipt validation utility for ensuring data quality and consistency
 * Handles validation specific to Saudi market and Islamic finance principles
 */
class ReceiptValidator {

    // Arabic to English number mapping
    private val arabicNumbers = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    // Valid ISO currency codes commonly used in Saudi Arabia
    private val validCurrencies = setOf("SAR", "USD", "EUR", "AED", "KWD", "BHD", "OMR", "QAR", "JOD")

    // Saudi VAT rates
    private val saudiVATRates = setOf(0.0f, 5.0f, 15.0f) // 0%, 5%, 15%

    /**
     * Validates receipt data and returns comprehensive validation result
     */
    fun validateReceipt(receiptData: ReceiptData): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // Validate required fields
        validateRequiredFields(receiptData, errors, suggestions)

        // Validate field formats
        validateFieldFormats(receiptData, errors, suggestions)

        // Validate mathematical consistency
        validateMathematicalConsistency(receiptData, errors, warnings)

        // Validate business rules
        validateBusinessRules(receiptData, errors, warnings)

        // Check for potential issues
        checkForWarnings(receiptData, warnings)

        val confidence = calculateConfidenceScore(receiptData, errors, warnings)
        val isValid = errors.isEmpty() || errors.all { it.severity == ValidationSeverity.WARNING }

        return ValidationResult(
            isValid = isValid,
            confidence = confidence,
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        )
    }

    private fun validateRequiredFields(
        receiptData: ReceiptData,
        errors: MutableList<ValidationError>,
        suggestions: MutableList<String>
    ) {
        if (receiptData.merchantName.isNullOrBlank()) {
            errors.add(ValidationError(
                type = ValidationErrorType.MISSING_MERCHANT,
                message = "Merchant name is missing",
                severity = ValidationSeverity.ERROR
            ))
            suggestions.add("Try to manually enter the merchant name or rescan the receipt")
        }

        if (receiptData.total.isNullOrBlank()) {
            errors.add(ValidationError(
                type = ValidationErrorType.MISSING_TOTAL,
                message = "Total amount is missing",
                severity = ValidationSeverity.ERROR
            ))
            suggestions.add("Look for the total amount at the bottom of the receipt")
        }

        if (receiptData.date.isNullOrBlank()) {
            errors.add(ValidationError(
                type = ValidationErrorType.MISSING_DATE,
                message = "Transaction date is missing",
                severity = ValidationSeverity.WARNING
            ))
            suggestions.add("Check the top or bottom of the receipt for the date")
        }

        if (receiptData.items.isEmpty()) {
            errors.add(ValidationError(
                type = ValidationErrorType.NO_ITEMS,
                message = "No items found in receipt",
                severity = ValidationSeverity.WARNING
            ))
            suggestions.add("The receipt might be for services only, or items weren't detected properly")
        }
    }

    private fun validateFieldFormats(
        receiptData: ReceiptData,
        errors: MutableList<ValidationError>,
        suggestions: MutableList<String>
    ) {
        // Validate amount format
        receiptData.total?.let { total ->
            if (!isValidAmount(total)) {
                errors.add(ValidationError(
                    type = ValidationErrorType.INVALID_AMOUNT_FORMAT,
                    message = "Invalid total amount format: $total",
                    severity = ValidationSeverity.ERROR
                ))
                suggestions.add("Amount should be in format XX.XX (e.g., 125.50)")
            }
        }

        receiptData.subtotal?.let { subtotal ->
            if (!isValidAmount(subtotal)) {
                errors.add(ValidationError(
                    type = ValidationErrorType.INVALID_AMOUNT_FORMAT,
                    message = "Invalid subtotal format: $subtotal",
                    severity = ValidationSeverity.WARNING
                ))
            }
        }

        receiptData.tax?.let { tax ->
            if (!isValidAmount(tax)) {
                errors.add(ValidationError(
                    type = ValidationErrorType.INVALID_AMOUNT_FORMAT,
                    message = "Invalid tax amount format: $tax",
                    severity = ValidationSeverity.WARNING
                ))
            }
        }

        // Validate date format
        receiptData.date?.let { date ->
            if (!isValidDate(date)) {
                errors.add(ValidationError(
                    type = ValidationErrorType.INVALID_DATE_FORMAT,
                    message = "Invalid date format: $date",
                    severity = ValidationSeverity.WARNING
                ))
                suggestions.add("Date should be in DD/MM/YYYY or similar format")
            }
        }

        // Validate currency
        if (!validCurrencies.contains(receiptData.currency)) {
            errors.add(ValidationError(
                type = ValidationErrorType.INVALID_CURRENCY,
                message = "Invalid currency: ${receiptData.currency}",
                severity = ValidationSeverity.WARNING
            ))
            suggestions.add("Currency should be SAR for Saudi Arabia")
        }

        // Validate merchant name format
        receiptData.merchantName?.let { merchantName ->
            if (!isValidMerchantName(merchantName)) {
                errors.add(ValidationError(
                    type = ValidationErrorType.INVALID_MERCHANT_NAME,
                    message = "Invalid merchant name format",
                    severity = ValidationSeverity.WARNING
                ))
            }
        }

        // Validate items
        receiptData.items.forEachIndexed { index, item ->
            if (!isValidItem(item)) {
                errors.add(ValidationError(
                    type = ValidationErrorType.INVALID_ITEM,
                    message = "Invalid item at position ${index + 1}",
                    severity = ValidationSeverity.WARNING
                ))
            }
        }
    }

    private fun validateMathematicalConsistency(
        receiptData: ReceiptData,
        errors: MutableList<ValidationError>,
        warnings: MutableList<String>
    ) {
        val total = receiptData.total?.let { parseAmount(it) }
        val subtotal = receiptData.subtotal?.let { parseAmount(it) }
        val tax = receiptData.tax?.let { parseAmount(it) }

        if (total != null && subtotal != null && tax != null) {
            val calculatedTotal = subtotal + tax
            val difference = abs(calculatedTotal - total)
            val tolerance = 0.05 // 5 cents tolerance for rounding

            if (difference > tolerance) {
                errors.add(ValidationError(
                    type = ValidationErrorType.MATH_INCONSISTENCY,
                    message = "Total ($total) doesn't match subtotal + tax (${calculatedTotal})",
                    severity = ValidationSeverity.ERROR
                ))
            } else if (difference > 0.01) {
                warnings.add("Small calculation difference detected, might be due to rounding")
            }
        }

        // Validate items total if available
        if (receiptData.items.isNotEmpty() && subtotal != null) {
            val itemsTotal = receiptData.items.sumOf { item ->
                parseAmount(item.price) * item.quantity
            }
            val difference = abs(itemsTotal - subtotal)
            
            if (difference > 0.1) {
                warnings.add("Items total ($itemsTotal) doesn't match subtotal ($subtotal)")
            }
        }

        // Validate Saudi VAT rate
        if (total != null && subtotal != null && tax != null && receiptData.currency == "SAR") {
            val vatRate = (tax / subtotal) * 100
            val isValidVATRate = saudiVATRates.any { abs(vatRate - it) < 1.0f }
            
            if (!isValidVATRate) {
                warnings.add("VAT rate ${String.format("%.1f", vatRate)}% is unusual for Saudi Arabia")
            }
        }
    }

    private fun validateBusinessRules(
        receiptData: ReceiptData,
        errors: MutableList<ValidationError>,
        warnings: MutableList<String>
    ) {
        // Check for reasonable amounts
        receiptData.total?.let { total ->
            val amount = parseAmount(total)
            if (amount < 0) {
                errors.add(ValidationError(
                    type = ValidationErrorType.NEGATIVE_AMOUNT,
                    message = "Total amount cannot be negative",
                    severity = ValidationSeverity.ERROR
                ))
            } else if (amount > 10000) {
                warnings.add("Large transaction amount: ${total} ${receiptData.currency}")
            } else if (amount == 0.0) {
                warnings.add("Zero amount transaction")
            }
        }

        // Check date reasonableness (not too far in past or future)
        receiptData.date?.let { date ->
            if (isDateTooOld(date)) {
                warnings.add("Receipt date appears to be very old")
            } else if (isDateInFuture(date)) {
                warnings.add("Receipt date is in the future")
            }
        }

        // Check for Islamic finance compliance (no interest/riba)
        val suspiciousTerms = listOf("interest", "finance charge", "late fee", "penalty")
        receiptData.items.forEach { item ->
            suspiciousTerms.forEach { term ->
                if (item.name.contains(term, ignoreCase = true)) {
                    warnings.add("Item '${item.name}' might not be Sharia-compliant")
                }
            }
        }
    }

    private fun checkForWarnings(receiptData: ReceiptData, warnings: MutableList<String>) {
        // Check for duplicate items
        val itemNames = receiptData.items.map { it.name.trim().lowercase() }
        val duplicates = itemNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        
        if (duplicates.isNotEmpty()) {
            warnings.add("Duplicate items detected: ${duplicates.keys.joinToString(", ")}")
        }

        // Check for empty item names
        val emptyNameItems = receiptData.items.filter { it.name.isBlank() }
        if (emptyNameItems.isNotEmpty()) {
            warnings.add("${emptyNameItems.size} items have empty names")
        }

        // Check for unusual prices
        receiptData.items.forEach { item ->
            val price = parseAmount(item.price)
            if (price < 0) {
                warnings.add("Item '${item.name}' has negative price")
            } else if (price > 1000) {
                warnings.add("Item '${item.name}' has unusually high price: ${item.price}")
            }
        }
    }

    private fun calculateConfidenceScore(
        receiptData: ReceiptData,
        errors: List<ValidationError>,
        warnings: List<String>
    ): Float {
        var score = 0.5f // Base score

        // Add points for available data
        if (!receiptData.merchantName.isNullOrBlank()) score += 0.15f
        if (!receiptData.date.isNullOrBlank()) score += 0.10f
        if (!receiptData.total.isNullOrBlank()) score += 0.20f
        if (receiptData.items.isNotEmpty()) score += 0.15f
        if (!receiptData.tax.isNullOrBlank()) score += 0.05f
        if (!receiptData.subtotal.isNullOrBlank()) score += 0.05f

        // Subtract points for errors
        errors.forEach { error ->
            when (error.severity) {
                ValidationSeverity.ERROR -> score -= 0.15f
                ValidationSeverity.WARNING -> score -= 0.05f
            }
        }

        // Subtract points for warnings
        score -= warnings.size * 0.02f

        // Bonus for mathematical consistency
        if (receiptData.total != null && receiptData.subtotal != null && receiptData.tax != null) {
            val total = parseAmount(receiptData.total!!)
            val subtotal = parseAmount(receiptData.subtotal!!)
            val tax = parseAmount(receiptData.tax!!)
            
            if (abs((subtotal + tax) - total) < 0.01) {
                score += 0.10f
            }
        }

        return score.coerceIn(0.0f, 1.0f)
    }

    private fun isValidAmount(amount: String): Boolean {
        val normalized = convertArabicNumbers(amount.trim())
        return try {
            val value = normalized.toDouble()
            value >= 0 && normalized.matches(Regex("\\d+\\.\\d{2}"))
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun isValidDate(date: String): Boolean {
        val normalized = convertArabicNumbers(date.trim())
        
        val datePatterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{4}"), // DD/MM/YYYY
            Regex("\\d{4}-\\d{1,2}-\\d{1,2}"), // YYYY-MM-DD
            Regex("\\d{1,2}\\s+\\w+\\s+\\d{4}") // DD Month YYYY
        )
        
        return datePatterns.any { it.matches(normalized) }
    }

    private fun isValidMerchantName(merchantName: String): Boolean {
        val name = merchantName.trim()
        return when {
            name.length < 2 -> false
            name.length > 80 -> false
            name.matches(Regex("\\d+")) -> false // Only numbers
            name.matches(Regex("[^\\p{L}\\p{N}\\s'&.-]+")) -> false // Only special chars
            else -> true
        }
    }

    private fun isValidItem(item: ReceiptItem): Boolean {
        return item.name.trim().isNotEmpty() && 
               isValidAmount(item.price) && 
               item.quantity > 0
    }

    private fun parseAmount(amount: String): Double {
        return try {
            convertArabicNumbers(amount.trim()).toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    private fun isDateTooOld(date: String): Boolean {
        // Simple check - receipts older than 2 years are unusual
        val currentYear = 2024 // This would be dynamic in real implementation
        val yearPattern = Regex("(\\d{4})")
        val match = yearPattern.find(convertArabicNumbers(date))
        
        return match?.let { 
            val year = it.groupValues[1].toInt()
            (currentYear - year) > 2
        } ?: false
    }

    private fun isDateInFuture(date: String): Boolean {
        // Simple check - receipts in future are invalid
        val currentYear = 2024
        val yearPattern = Regex("(\\d{4})")
        val match = yearPattern.find(convertArabicNumbers(date))
        
        return match?.let { 
            val year = it.groupValues[1].toInt()
            year > currentYear
        } ?: false
    }

    private fun convertArabicNumbers(text: String): String {
        var result = text
        for ((arabic, english) in arabicNumbers) {
            result = result.replace(arabic, english)
        }
        return result
    }
}

/**
 * Receipt validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val confidence: Float,
    val errors: List<ValidationError>,
    val warnings: List<String>,
    val suggestions: List<String>
)

/**
 * Validation error information
 */
data class ValidationError(
    val type: ValidationErrorType,
    val message: String,
    val severity: ValidationSeverity,
    val field: String? = null
)

/**
 * Types of validation errors
 */
enum class ValidationErrorType {
    MISSING_MERCHANT,
    MISSING_DATE,
    MISSING_TOTAL,
    NO_ITEMS,
    INVALID_AMOUNT_FORMAT,
    INVALID_DATE_FORMAT,
    INVALID_CURRENCY,
    INVALID_MERCHANT_NAME,
    INVALID_ITEM,
    MATH_INCONSISTENCY,
    NEGATIVE_AMOUNT,
    BUSINESS_RULE_VIOLATION
}

/**
 * Validation error severity levels
 */
enum class ValidationSeverity {
    ERROR,   // Critical error that prevents processing
    WARNING  // Non-critical issue that doesn't prevent processing
}