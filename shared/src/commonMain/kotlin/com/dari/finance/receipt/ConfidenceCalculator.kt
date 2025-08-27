package com.dari.finance.receipt

import com.dari.finance.ocr.ReceiptData
import com.dari.finance.ocr.ReceiptItem
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * OCR confidence calculator for receipt processing
 * Evaluates confidence based on multiple factors including data quality,
 * mathematical consistency, and Saudi-specific validation patterns
 */
class ConfidenceCalculator {

    // Weight factors for different confidence components
    private val weights = ConfidenceWeights(
        dataCompleteness = 0.25f,
        textQuality = 0.15f,
        mathematicalConsistency = 0.20f,
        formatConsistency = 0.15f,
        businessLogic = 0.15f,
        ocrAccuracy = 0.10f
    )

    // Arabic to English number mapping for normalization
    private val arabicNumbers = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    /**
     * Calculates overall confidence score for receipt data
     */
    fun calculateOverallConfidence(receiptData: ReceiptData): Float {
        val factors = ConfidenceFactors(
            dataCompleteness = assessDataCompleteness(receiptData),
            textQuality = analyzeTextQuality(receiptData.rawText),
            mathematicalConsistency = evaluateMathematicalConsistency(receiptData),
            formatConsistency = evaluateFormatConsistency(receiptData),
            businessLogic = evaluateBusinessLogic(receiptData),
            ocrAccuracy = estimateOCRAccuracy(receiptData.rawText)
        )

        return calculateWeightedConfidence(factors)
    }

    /**
     * Calculates weighted confidence from individual factors
     */
    fun calculateWeightedConfidence(factors: ConfidenceFactors): Float {
        return (factors.dataCompleteness * weights.dataCompleteness +
                factors.textQuality * weights.textQuality +
                factors.mathematicalConsistency * weights.mathematicalConsistency +
                factors.formatConsistency * weights.formatConsistency +
                factors.businessLogic * weights.businessLogic +
                factors.ocrAccuracy * weights.ocrAccuracy).coerceIn(0.0f, 1.0f)
    }

    /**
     * Analyzes text quality based on clarity and OCR errors
     */
    fun analyzeTextQuality(text: String): Float {
        if (text.isBlank()) return 0.0f

        var qualityScore = 1.0f
        val textLength = text.length

        // Count suspicious characters that might indicate OCR errors
        val suspiciousChars = text.count { char ->
            char in "!@#$%^&*()+={}[]|\\:;\"'<>?/~`" ||
            char.isDigit() && text.indexOf(char) > 0 && 
            text.getOrNull(text.indexOf(char) - 1)?.isLetter() == true
        }

        // Penalize for high ratio of suspicious characters
        if (textLength > 0) {
            val suspiciousRatio = suspiciousChars.toFloat() / textLength
            qualityScore -= suspiciousRatio * 2.0f
        }

        // Count character substitution patterns common in OCR errors
        val substitutionPatterns = listOf(
            '0' to 'O', '1' to 'l', '1' to 'I', '5' to 'S', '8' to 'B',
            '3' to 'E', '6' to 'G', '9' to 'g'
        )

        var substitutionCount = 0
        substitutionPatterns.forEach { (digit, letter) ->
            val digitCount = text.count { it == digit }
            val letterCount = text.count { it.uppercaseChar() == letter.uppercaseChar() }
            if (digitCount > 0 && letterCount > 0) {
                substitutionCount += min(digitCount, letterCount)
            }
        }

        qualityScore -= (substitutionCount.toFloat() / max(textLength, 1)) * 0.5f

        // Bonus for well-structured text (presence of keywords)
        val structureKeywords = listOf(
            "total", "subtotal", "tax", "date", "المجموع", "الإجمالي", 
            "التاريخ", "ضريبة", "SAR", "ريال"
        )

        val keywordMatches = structureKeywords.count { keyword ->
            text.contains(keyword, ignoreCase = true)
        }

        qualityScore += (keywordMatches.toFloat() / structureKeywords.size) * 0.3f

        // Bonus for proper date formats
        val datePatterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{4}"),
            Regex("\\d{4}-\\d{1,2}-\\d{1,2}"),
            Regex("[٠-٩]{1,2}/[٠-٩]{1,2}/[٠-٩]{4}")
        )

        if (datePatterns.any { it.find(text) != null }) {
            qualityScore += 0.1f
        }

        // Bonus for proper currency amounts
        val amountPatterns = listOf(
            Regex("\\d+\\.\\d{2}"),
            Regex("[٠-٩]+\\.[٠-٩]{2}"),
            Regex("\\d+\\.\\d{2}\\s*(?:SAR|ريال)")
        )

        val amountMatches = amountPatterns.sumOf { pattern ->
            pattern.findAll(text).count()
        }

        if (amountMatches > 0) {
            qualityScore += min(amountMatches * 0.05f, 0.2f)
        }

        return qualityScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Evaluates mathematical consistency of amounts
     */
    fun evaluateMathematicalConsistency(receiptData: ReceiptData): Float {
        val total = receiptData.total?.let { parseAmount(it) }
        val subtotal = receiptData.subtotal?.let { parseAmount(it) }
        val tax = receiptData.tax?.let { parseAmount(it) }

        var consistencyScore = 0.5f // Base score for missing data

        // Check subtotal + tax = total
        if (total != null && subtotal != null && tax != null) {
            val calculatedTotal = subtotal + tax
            val difference = abs(calculatedTotal - total)
            
            when {
                difference < 0.01 -> consistencyScore = 1.0f // Perfect match
                difference < 0.05 -> consistencyScore = 0.9f // Minor rounding difference
                difference < 0.10 -> consistencyScore = 0.7f // Small discrepancy
                difference < 1.00 -> consistencyScore = 0.4f // Moderate discrepancy
                else -> consistencyScore = 0.1f // Large discrepancy
            }
        } else if (total != null && (subtotal != null || tax != null)) {
            consistencyScore = 0.6f // Partial data available
        } else if (total != null) {
            consistencyScore = 0.7f // At least have total
        }

        // Check items total vs subtotal
        if (receiptData.items.isNotEmpty() && subtotal != null) {
            val itemsTotal = receiptData.items.sumOf { item ->
                parseAmount(item.price) * item.quantity
            }
            val itemsDifference = abs(itemsTotal - subtotal)
            
            val itemsConsistency = when {
                itemsDifference < 0.01 -> 1.0f
                itemsDifference < 0.10 -> 0.8f
                itemsDifference < 1.00 -> 0.6f
                else -> 0.3f
            }
            
            consistencyScore = (consistencyScore + itemsConsistency) / 2.0f
        }

        // Validate Saudi VAT rate if applicable
        if (total != null && subtotal != null && tax != null && receiptData.currency == "SAR") {
            val vatRate = if (subtotal > 0) (tax / subtotal) * 100 else 0.0
            val saudiVATRates = listOf(0.0, 5.0, 15.0) // Common Saudi VAT rates
            
            val isValidVATRate = saudiVATRates.any { rate -> abs(vatRate - rate) < 2.0 }
            if (!isValidVATRate) {
                consistencyScore -= 0.1f // Penalty for unusual VAT rate
            }
        }

        return consistencyScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Assesses data completeness
     */
    fun assessDataCompleteness(receiptData: ReceiptData): Float {
        return assessDataCompleteness(receiptDataToMap(receiptData))
    }

    fun assessDataCompleteness(fields: Map<String, String?>): Float {
        val importantFields = mapOf(
            "merchantName" to 0.20f,
            "total" to 0.30f,
            "date" to 0.15f,
            "currency" to 0.10f,
            "items" to 0.15f,
            "tax" to 0.05f,
            "subtotal" to 0.05f
        )

        var completenessScore = 0.0f

        importantFields.forEach { (fieldName, weight) ->
            val fieldValue = fields[fieldName]
            if (!fieldValue.isNullOrBlank() && fieldValue != "0") {
                completenessScore += weight
                
                // Bonus for high-quality field values
                when (fieldName) {
                    "total", "tax", "subtotal" -> {
                        if (isValidAmount(fieldValue)) completenessScore += weight * 0.2f
                    }
                    "date" -> {
                        if (isValidDateFormat(fieldValue)) completenessScore += weight * 0.2f
                    }
                    "merchantName" -> {
                        if (fieldValue.length > 5) completenessScore += weight * 0.2f
                    }
                    "items" -> {
                        val itemCount = fieldValue.toIntOrNull() ?: 0
                        if (itemCount > 0) completenessScore += weight * min(itemCount * 0.1f, weight)
                    }
                }
            }
        }

        return completenessScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Evaluates format consistency across fields
     */
    fun evaluateFormatConsistency(receiptData: ReceiptData): Float {
        var formatScore = 1.0f
        var totalFields = 0
        var validFields = 0

        // Check amount formats
        listOf(receiptData.total, receiptData.subtotal, receiptData.tax).forEach { amount ->
            if (amount != null) {
                totalFields++
                if (isValidAmount(amount)) {
                    validFields++
                } else {
                    formatScore -= 0.2f
                }
            }
        }

        // Check date format
        receiptData.date?.let { date ->
            totalFields++
            if (isValidDateFormat(date)) {
                validFields++
            } else {
                formatScore -= 0.2f
            }
        }

        // Check currency format
        if (receiptData.currency.isNotBlank()) {
            totalFields++
            val validCurrencies = setOf("SAR", "USD", "EUR", "AED", "KWD", "BHD", "OMR", "QAR", "JOD")
            if (validCurrencies.contains(receiptData.currency)) {
                validFields++
            } else {
                formatScore -= 0.1f
            }
        }

        // Check item formats
        receiptData.items.forEach { item ->
            totalFields += 2 // name and price
            if (item.name.isNotBlank()) validFields++
            if (isValidAmount(item.price)) validFields++
        }

        // Calculate format consistency ratio
        val consistencyRatio = if (totalFields > 0) validFields.toFloat() / totalFields else 1.0f
        
        return (formatScore * 0.5f + consistencyRatio * 0.5f).coerceIn(0.0f, 1.0f)
    }

    /**
     * Evaluates business logic validity
     */
    fun evaluateBusinessLogic(receiptData: ReceiptData): Float {
        var logicScore = 1.0f

        // Check for reasonable amounts
        receiptData.total?.let { total ->
            val amount = parseAmount(total)
            when {
                amount < 0 -> logicScore -= 0.5f // Negative amounts are invalid
                amount == 0.0 -> logicScore -= 0.2f // Zero amounts are unusual
                amount > 10000 -> logicScore -= 0.1f // Very large amounts are unusual
            }
        }

        // Check date reasonableness
        receiptData.date?.let { date ->
            if (isDateTooOld(date) || isDateInFuture(date)) {
                logicScore -= 0.2f
            }
        }

        // Check for reasonable merchant names
        receiptData.merchantName?.let { merchantName ->
            when {
                merchantName.length < 2 -> logicScore -= 0.2f
                merchantName.matches(Regex("\\d+")) -> logicScore -= 0.3f // Only numbers
                merchantName.matches(Regex("[^\\p{L}\\p{N}\\s'&.-]+")) -> logicScore -= 0.2f // Only special chars
            }
        }

        // Check for Islamic finance compliance (avoid riba/interest)
        val suspiciousTerms = listOf("interest", "finance charge", "late fee", "penalty", "فائدة")
        receiptData.items.forEach { item ->
            suspiciousTerms.forEach { term ->
                if (item.name.contains(term, ignoreCase = true)) {
                    logicScore -= 0.1f
                }
            }
        }

        // Check for reasonable item prices
        receiptData.items.forEach { item ->
            val price = parseAmount(item.price)
            if (price < 0) logicScore -= 0.1f
            if (price > 1000) logicScore -= 0.05f // Very expensive items
        }

        return logicScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * Estimates OCR accuracy based on text patterns
     */
    fun estimateOCRAccuracy(text: String): Float {
        if (text.isBlank()) return 0.0f

        var accuracy = 1.0f
        val textLength = text.length

        // Count character recognition errors
        val errorPatterns = mapOf(
            Regex("[Il1|]{2,}") to 0.05f, // Common OCR confusion
            Regex("[0OoQ]{2,}") to 0.05f,
            Regex("[5S]{2,}") to 0.03f,
            Regex("[6G]{2,}") to 0.03f,
            Regex("[8B]{2,}") to 0.03f,
            Regex("[@#$%^&*]{2,}") to 0.1f // Noise characters
        )

        errorPatterns.forEach { (pattern, penalty) ->
            val matches = pattern.findAll(text).count()
            accuracy -= (matches.toFloat() / max(textLength / 10, 1)) * penalty
        }

        // Bonus for well-recognized patterns
        val recognizedPatterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{4}"), // Dates
            Regex("\\d+\\.\\d{2}\\s*SAR"), // Currency amounts
            Regex("Total|المجموع", RegexOption.IGNORE_CASE), // Key terms
            Regex("[A-Z]{2,}\\s+[A-Z]{2,}") // Merchant names
        )

        recognizedPatterns.forEach { pattern ->
            if (pattern.find(text) != null) {
                accuracy += 0.05f
            }
        }

        // Penalize for excessive punctuation or special characters
        val specialCharRatio = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }.toFloat() / textLength
        if (specialCharRatio > 0.3) {
            accuracy -= (specialCharRatio - 0.3f) * 0.5f
        }

        return accuracy.coerceIn(0.0f, 1.0f)
    }

    /**
     * Provides detailed confidence breakdown
     */
    fun getConfidenceBreakdown(receiptData: ReceiptData): ConfidenceBreakdown {
        val dataCompleteness = assessDataCompleteness(receiptData)
        val textQuality = analyzeTextQuality(receiptData.rawText)
        val mathematicalConsistency = evaluateMathematicalConsistency(receiptData)
        val formatConsistency = evaluateFormatConsistency(receiptData)
        val businessLogic = evaluateBusinessLogic(receiptData)
        val ocrAccuracy = estimateOCRAccuracy(receiptData.rawText)

        val overall = calculateWeightedConfidence(ConfidenceFactors(
            dataCompleteness, textQuality, mathematicalConsistency,
            formatConsistency, businessLogic, ocrAccuracy
        ))

        return ConfidenceBreakdown(
            overall = overall,
            dataCompleteness = dataCompleteness,
            textQuality = textQuality,
            mathematicalConsistency = mathematicalConsistency,
            formatConsistency = formatConsistency,
            businessLogic = businessLogic,
            ocrAccuracy = ocrAccuracy
        )
    }

    /**
     * Provides suggestions for improving confidence
     */
    fun getSuggestions(receiptData: ReceiptData): List<String> {
        val suggestions = mutableListOf<String>()

        if (receiptData.merchantName.isNullOrBlank()) {
            suggestions.add("Add merchant name to improve accuracy")
        }

        if (receiptData.date.isNullOrBlank()) {
            suggestions.add("Add transaction date for better tracking")
        }

        if (receiptData.total.isNullOrBlank()) {
            suggestions.add("Ensure total amount is clearly visible")
        }

        if (receiptData.items.isEmpty()) {
            suggestions.add("Scan item details for complete record")
        }

        receiptData.total?.let { total ->
            if (!isValidAmount(total)) {
                suggestions.add("Check total amount format (should be XX.XX)")
            }
        }

        receiptData.date?.let { date ->
            if (!isValidDateFormat(date)) {
                suggestions.add("Verify date format (DD/MM/YYYY recommended)")
            }
        }

        if (receiptData.subtotal != null && receiptData.tax != null && receiptData.total != null) {
            val subtotal = parseAmount(receiptData.subtotal!!)
            val tax = parseAmount(receiptData.tax!!)
            val total = parseAmount(receiptData.total!!)
            
            if (abs((subtotal + tax) - total) > 0.1) {
                suggestions.add("Check if amounts add up correctly")
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Receipt data looks good!")
        }

        return suggestions
    }

    // Helper functions
    private fun receiptDataToMap(receiptData: ReceiptData): Map<String, String?> {
        return mapOf(
            "merchantName" to receiptData.merchantName,
            "date" to receiptData.date,
            "total" to receiptData.total,
            "currency" to receiptData.currency,
            "items" to if (receiptData.items.isNotEmpty()) receiptData.items.size.toString() else null,
            "tax" to receiptData.tax,
            "subtotal" to receiptData.subtotal
        )
    }

    private fun isValidAmount(amount: String?): Boolean {
        if (amount.isNullOrBlank()) return false
        val normalized = convertArabicNumbers(amount.trim())
        return try {
            val value = normalized.toDouble()
            value >= 0 && normalized.matches(Regex("\\d+\\.\\d{2}"))
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun isValidDateFormat(date: String): Boolean {
        val normalized = convertArabicNumbers(date.trim())
        val datePatterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{4}"),
            Regex("\\d{4}-\\d{1,2}-\\d{1,2}"),
            Regex("\\d{1,2}\\s+\\w+\\s+\\d{4}")
        )
        return datePatterns.any { it.matches(normalized) }
    }

    private fun parseAmount(amount: String): Double {
        return try {
            convertArabicNumbers(amount.trim()).toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    private fun isDateTooOld(date: String): Boolean {
        val currentYear = 2024
        val yearPattern = Regex("(\\d{4})")
        val match = yearPattern.find(convertArabicNumbers(date))
        return match?.let { 
            val year = it.groupValues[1].toInt()
            (currentYear - year) > 2
        } ?: false
    }

    private fun isDateInFuture(date: String): Boolean {
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
 * Confidence factors for different aspects
 */
data class ConfidenceFactors(
    val dataCompleteness: Float,
    val textQuality: Float,
    val mathematicalConsistency: Float,
    val formatConsistency: Float,
    val businessLogic: Float,
    val ocrAccuracy: Float
)

/**
 * Weight factors for confidence calculation
 */
data class ConfidenceWeights(
    val dataCompleteness: Float,
    val textQuality: Float,
    val mathematicalConsistency: Float,
    val formatConsistency: Float,
    val businessLogic: Float,
    val ocrAccuracy: Float
)

/**
 * Detailed confidence breakdown
 */
data class ConfidenceBreakdown(
    val overall: Float,
    val dataCompleteness: Float,
    val textQuality: Float,
    val mathematicalConsistency: Float,
    val formatConsistency: Float,
    val businessLogic: Float,
    val ocrAccuracy: Float
)