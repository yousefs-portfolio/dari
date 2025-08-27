package com.dari.finance.ocr

/**
 * Amount extraction utility for OCR receipt processing
 * Handles various amount formats, currencies, and languages
 */
class AmountExtractor {

    // Priority keywords for amount identification
    private val totalKeywords = listOf(
        "total", "grand total", "amount", "final", "sum",
        "المجموع", "الإجمالي", "المبلغ", "المجموع الكلي"
    )
    
    private val subtotalKeywords = listOf(
        "subtotal", "sub total", "before tax",
        "المجموع الفرعي", "قبل الضريبة"
    )
    
    private val taxKeywords = listOf(
        "tax", "vat", "levy", "duty",
        "ضريبة", "القيمة المضافة", "الضريبة"
    )

    // Currency symbols and names
    private val currencyMap = mapOf(
        "SAR" to "SAR", "ريال" to "SAR", "sr" to "SAR",
        "USD" to "USD", "$" to "USD", "dollar" to "USD",
        "EUR" to "EUR", "€" to "EUR", "euro" to "EUR",
        "AED" to "AED", "درهم" to "AED", "dirham" to "AED",
        "KWD" to "KWD", "دينار" to "KWD", "dinar" to "KWD"
    )

    // Arabic to English number mapping
    private val arabicNumbers = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    /**
     * Extracts the primary amount from text (usually total)
     */
    fun extractAmount(text: String): String? {
        val amounts = extractAllAmounts(text)
        if (amounts.isEmpty()) return null

        // First try to find total amounts
        val totalAmount = findAmountByKeywords(text, totalKeywords)
        if (totalAmount != null) return totalAmount

        // Then try subtotal
        val subtotalAmount = findAmountByKeywords(text, subtotalKeywords)
        if (subtotalAmount != null) return subtotalAmount

        // Return largest amount if no keywords match
        return amounts.maxByOrNull { it.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * Extracts amount with currency information
     */
    fun extractAmountWithCurrency(text: String): AmountWithCurrency? {
        val amountPatterns = listOf(
            // Currency before amount
            Regex("(SAR|ريال|USD|EUR|AED|درهم|KWD|دينار|\\$|€)\\s*([\\d,٠-٩]+[\\.٫]\\d{2})", RegexOption.IGNORE_CASE),
            // Currency after amount
            Regex("([\\d,٠-٩]+[\\.٫]\\d{2})\\s*(SAR|ريال|USD|EUR|AED|درهم|KWD|دينار|SR)", RegexOption.IGNORE_CASE),
            // Amount with $ or € symbols
            Regex("([\\$€])([\\d,]+\\.\\d{2})"),
            // Standalone amounts (default to SAR for Saudi market)
            Regex("([\\d,٠-٩]+[\\.٫]\\d{2})")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val groups = match.groupValues
                
                when {
                    groups.size >= 3 && groups[1].isNotEmpty() -> {
                        // Currency before amount
                        val currency = currencyMap[groups[1].lowercase()] ?: groups[1].uppercase()
                        val amount = normalizeAmount(groups[2])
                        return AmountWithCurrency(amount, currency)
                    }
                    groups.size >= 3 && groups[2].isNotEmpty() -> {
                        // Currency after amount
                        val amount = normalizeAmount(groups[1])
                        val currency = currencyMap[groups[2].lowercase()] ?: groups[2].uppercase()
                        return AmountWithCurrency(amount, currency)
                    }
                    groups.size >= 2 -> {
                        // Amount with currency symbol
                        val currencySymbol = groups[1]
                        val amount = normalizeAmount(groups[2])
                        val currency = currencyMap[currencySymbol] ?: "SAR"
                        return AmountWithCurrency(amount, currency)
                    }
                    else -> {
                        // Standalone amount
                        val amount = normalizeAmount(groups[1])
                        return AmountWithCurrency(amount, "SAR") // Default currency
                    }
                }
            }
        }

        return null
    }

    /**
     * Extracts all monetary amounts from text
     */
    fun extractAllAmounts(text: String): List<String> {
        val amounts = mutableListOf<String>()
        val amountPattern = Regex("([\\d,٠-٩]+[\\.٫]\\d{2})")
        
        val matches = amountPattern.findAll(text)
        for (match in matches) {
            val amount = normalizeAmount(match.value)
            if (isValidAmount(amount)) {
                amounts.add(amount)
            }
        }
        
        return amounts.distinct()
    }

    /**
     * Extracts amount with priority-based logic
     */
    fun extractAmountByPriority(text: String): String? {
        // Priority 1: Total amounts
        val totalAmount = findAmountByKeywords(text, totalKeywords)
        if (totalAmount != null) return totalAmount

        // Priority 2: Subtotal amounts
        val subtotalAmount = findAmountByKeywords(text, subtotalKeywords)
        if (subtotalAmount != null) return subtotalAmount

        // Priority 3: Last amount in text (often the total)
        val amounts = extractAllAmounts(text)
        return amounts.lastOrNull()
    }

    /**
     * Extracts amount with confidence score
     */
    fun extractAmountWithConfidence(text: String): AmountWithConfidence? {
        val amount = extractAmount(text) ?: return null
        
        var confidence = 0.5f // Base confidence
        
        // Boost confidence based on context
        val lowerText = text.lowercase()
        if (totalKeywords.any { lowerText.contains(it) }) confidence += 0.3f
        if (currencyMap.keys.any { lowerText.contains(it) }) confidence += 0.2f
        if (amount.contains('.') && amount.split('.')[1].length == 2) confidence += 0.1f
        if (text.contains(Regex("\\d+\\.\\d{2}\\s*(SAR|ريال)", RegexOption.IGNORE_CASE))) confidence += 0.2f
        
        return AmountWithConfidence(amount, confidence.coerceIn(0.0f, 1.0f))
    }

    /**
     * Extracts tax amount specifically
     */
    fun extractTaxAmount(text: String): String? {
        return findAmountByKeywords(text, taxKeywords)
    }

    /**
     * Extracts itemized amounts with their descriptions
     */
    fun extractItemAmounts(text: String): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        val lines = text.split('\n')
        
        for (line in lines) {
            // Skip lines that look like totals or headers
            if (totalKeywords.any { line.lowercase().contains(it) } ||
                subtotalKeywords.any { line.lowercase().contains(it) } ||
                taxKeywords.any { line.lowercase().contains(it) }) {
                continue
            }
            
            val itemPattern = Regex("(.+?)\\s+([\\d,٠-٩]+[\\.٫]\\d{2})\\s*(?:SAR|ريال|SR)?")
            val match = itemPattern.find(line.trim())
            
            if (match != null) {
                val itemName = match.groupValues[1].trim()
                val itemPrice = normalizeAmount(match.groupValues[2])
                
                if (itemName.isNotEmpty() && isValidAmount(itemPrice)) {
                    items.add(itemName to itemPrice)
                }
            }
        }
        
        return items
    }

    /**
     * Normalizes amount format (converts Arabic numbers, standardizes decimal separator)
     */
    fun normalizeAmount(amount: String): String {
        var normalized = amount
        
        // Convert Arabic numbers to English
        for ((arabic, english) in arabicNumbers) {
            normalized = normalized.replace(arabic, english)
        }
        
        // Replace Arabic decimal separator
        normalized = normalized.replace('٫', '.')
        
        // Handle different decimal separator conventions
        if (normalized.count { it == ',' } == 1 && normalized.count { it == '.' } == 0) {
            // European format: 25,99
            normalized = normalized.replace(',', '.')
        } else if (normalized.contains(',')) {
            // Remove thousands separators: 1,250.75 -> 1250.75
            val parts = normalized.split('.')
            if (parts.size == 2) {
                val wholePart = parts[0].replace(",", "")
                normalized = "$wholePart.${parts[1]}"
            }
        }
        
        return normalized
    }

    /**
     * Validates amount format
     */
    fun isValidAmount(amount: String?): Boolean {
        if (amount.isNullOrBlank()) return false
        
        return try {
            val value = amount.toDouble()
            value >= 0 && amount.matches(Regex("\\d+\\.\\d{2}"))
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun findAmountByKeywords(text: String, keywords: List<String>): String? {
        val lowerText = text.lowercase()
        
        for (keyword in keywords) {
            val keywordIndex = lowerText.indexOf(keyword.lowercase())
            if (keywordIndex != -1) {
                // Look for amount after the keyword
                val afterKeyword = text.substring(keywordIndex + keyword.length)
                val amountPattern = Regex("\\s*:?\\s*([\\d,٠-٩]+[\\.٫]\\d{2})")
                val match = amountPattern.find(afterKeyword)
                
                if (match != null) {
                    val amount = normalizeAmount(match.groupValues[1])
                    if (isValidAmount(amount)) {
                        return amount
                    }
                }
            }
        }
        
        return null
    }
}