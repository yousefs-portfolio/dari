package code.yousef.dari.shared.domain.models

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Money Value Object
 * Represents monetary amounts with currency following SAMA Open Banking standards
 * Immutable and supports arithmetic operations with proper currency validation
 */
@Serializable
data class Money(
    val amount: String,
    val currency: String
) {
    
    init {
        validateAmount(amount)
        validateCurrency(currency)
    }

    companion object {
        private val SUPPORTED_CURRENCIES = setOf(
            "SAR", "USD", "EUR", "GBP", "AED", "KWD", "QAR", "OMR", "BHD"
        )
        
        private val AMOUNT_REGEX = Regex("""^-?\d+(\.\d{1,2})?$""")
        
        val ZERO_SAR = Money("0.00", "SAR")
        val ZERO_USD = Money("0.00", "USD")
        
        /**
         * Create Money from double value
         */
        fun fromDouble(value: Double, currency: String): Money {
            val formattedAmount = "%.2f".format(value)
            return Money(formattedAmount, currency)
        }
        
        /**
         * Create Money from integer value (treated as major currency units)
         */
        fun fromInt(value: Int, currency: String): Money {
            return Money("$value.00", currency)
        }
    }

    /**
     * Get the numeric value as Double for calculations
     */
    val numericValue: Double
        get() = amount.toDouble()

    /**
     * Check if this money amount is zero
     */
    fun isZero(): Boolean = numericValue == 0.0

    /**
     * Check if this money amount is positive
     */
    fun isPositive(): Boolean = numericValue > 0.0

    /**
     * Check if this money amount is negative
     */
    fun isNegative(): Boolean = numericValue < 0.0

    /**
     * Add two Money values (must have same currency)
     */
    operator fun plus(other: Money): Money {
        validateSameCurrency(other)
        val result = numericValue + other.numericValue
        return Money("%.2f".format(result), currency)
    }

    /**
     * Subtract two Money values (must have same currency)
     */
    operator fun minus(other: Money): Money {
        validateSameCurrency(other)
        val result = numericValue - other.numericValue
        return Money("%.2f".format(result), currency)
    }

    /**
     * Multiply Money by a factor
     */
    operator fun times(factor: Int): Money {
        val result = numericValue * factor
        return Money("%.2f".format(result), currency)
    }

    /**
     * Multiply Money by a factor
     */
    operator fun times(factor: Double): Money {
        val result = numericValue * factor
        return Money("%.2f".format(result), currency)
    }

    /**
     * Divide Money by a factor
     */
    operator fun div(factor: Int): Money {
        require(factor != 0) { "Cannot divide by zero" }
        val result = numericValue / factor
        return Money("%.2f".format(result), currency)
    }

    /**
     * Divide Money by a factor
     */
    operator fun div(factor: Double): Money {
        require(factor != 0.0) { "Cannot divide by zero" }
        val result = numericValue / factor
        return Money("%.2f".format(result), currency)
    }

    /**
     * Compare two Money values (must have same currency)
     */
    operator fun compareTo(other: Money): Int {
        validateSameCurrency(other)
        return numericValue.compareTo(other.numericValue)
    }

    /**
     * Calculate percentage of this Money amount
     */
    fun percentage(percent: Int): Money {
        val result = numericValue * (percent / 100.0)
        return Money("%.2f".format(result), currency)
    }

    /**
     * Calculate percentage of this Money amount
     */
    fun percentage(percent: Double): Money {
        val result = numericValue * (percent / 100.0)
        return Money("%.2f".format(result), currency)
    }

    /**
     * Get absolute value of this Money amount
     */
    fun abs(): Money {
        return if (isNegative()) {
            Money("%.2f".format(abs(numericValue)), currency)
        } else {
            this
        }
    }

    /**
     * Convert to another currency (requires exchange rate)
     */
    fun convertTo(targetCurrency: String, exchangeRate: String): Money {
        validateCurrency(targetCurrency)
        val rate = exchangeRate.toDouble()
        val convertedAmount = numericValue * rate
        return Money("%.2f".format(convertedAmount), targetCurrency)
    }

    /**
     * Format Money for display in English
     */
    fun format(): String {
        return formatAmount(amount) + " " + currency
    }

    /**
     * Format Money for display in Arabic
     */
    fun formatArabic(): String {
        val arabicAmount = formatAmountArabic(amount)
        val arabicCurrency = when (currency) {
            "SAR" -> "ر.س"
            "USD" -> "ج.أ"
            "EUR" -> "يورو"
            "GBP" -> "ج.إ"
            else -> currency
        }
        return "$arabicAmount $arabicCurrency"
    }

    /**
     * Format for JSON serialization (ISO format)
     */
    fun toIsoString(): String {
        return "$amount $currency"
    }

    private fun validateAmount(amount: String) {
        require(amount.isNotBlank()) { "Amount cannot be blank" }
        require(AMOUNT_REGEX.matches(amount)) { 
            "Invalid amount format: $amount. Expected format: 123.45 or -123.45" 
        }
        
        // Normalize decimal places
        val parts = amount.split(".")
        if (parts.size == 1) {
            // No decimal part, add .00
            this.amount = "$amount.00"
        } else if (parts[1].length == 1) {
            // One decimal place, add trailing zero
            this.amount = "${amount}0"
        }
        // Otherwise keep as-is (should be exactly 2 decimal places)
    }

    private fun validateCurrency(currency: String) {
        require(currency.isNotBlank()) { "Currency cannot be blank" }
        require(currency.length == 3) { "Currency must be 3 characters (ISO 4217)" }
        require(currency.uppercase() in SUPPORTED_CURRENCIES) { 
            "Unsupported currency: $currency. Supported: $SUPPORTED_CURRENCIES" 
        }
    }

    private fun validateSameCurrency(other: Money) {
        require(currency == other.currency) { 
            "Cannot perform operation on different currencies: $currency vs ${other.currency}" 
        }
    }

    private fun formatAmount(amount: String): String {
        val parts = amount.split(".")
        val wholePart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else "00"
        
        // Add thousand separators
        val formattedWhole = wholePart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        
        return "$formattedWhole.$decimalPart"
    }

    private fun formatAmountArabic(amount: String): String {
        // Convert Western Arabic numerals to Eastern Arabic numerals
        val westernToArabic = mapOf(
            '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣', '4' to '٤',
            '5' to '٥', '6' to '٦', '7' to '٧', '8' to '٨', '9' to '٩'
        )
        
        val parts = amount.split(".")
        val wholePart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else "00"
        
        // Add thousand separators and convert to Arabic numerals
        val formattedWhole = wholePart.reversed()
            .chunked(3)
            .joinToString("٬")
            .reversed()
            .map { westernToArabic[it] ?: it }
            .joinToString("")
        
        val formattedDecimal = decimalPart
            .map { westernToArabic[it] ?: it }
            .joinToString("")
        
        return "$formattedWhole٫$formattedDecimal"
    }

    /**
     * Create a copy with normalized amount (ensures proper decimal formatting)
     */
    private fun normalized(): Money {
        return Money("%.2f".format(numericValue), currency)
    }
}