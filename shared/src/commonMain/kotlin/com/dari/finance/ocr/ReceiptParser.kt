package com.dari.finance.ocr

import kotlin.math.max

/**
 * Receipt parser for extracting structured data from OCR text
 * Handles both English and Arabic receipt formats common in Saudi Arabia
 */
class ReceiptParser {

    // Common merchant patterns for Saudi market
    private val merchantPatterns = mapOf(
        "lulu" to "LULU HYPERMARKET",
        "carrefour" to "CARREFOUR",
        "extra" to "EXTRA",
        "danube" to "DANUBE",
        "panda" to "PANDA",
        "tamimi" to "TAMIMI MARKETS",
        "bin dawood" to "BIN DAWOOD",
        "mcdonald" to "MCDONALD'S",
        "kfc" to "KFC",
        "subway" to "SUBWAY",
        "starbucks" to "STARBUCKS",
        "hardees" to "HARDEE'S",
        "burger king" to "BURGER KING",
        "بندة" to "PANDA",
        "لولو" to "LULU HYPERMARKET",
        "كارفور" to "CARREFOUR",
        "اكسترا" to "EXTRA",
        "الدانوب" to "DANUBE",
        "التميمي" to "TAMIMI MARKETS"
    )

    // Arabic to English number conversion
    private val arabicNumbers = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    /**
     * Parses receipt text and extracts structured data
     */
    fun parseReceiptText(text: String): ReceiptData {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        
        return ReceiptData(
            merchantName = extractMerchantName(lines),
            date = extractDate(text),
            total = extractTotal(text),
            currency = extractCurrency(text),
            items = extractItems(lines),
            tax = extractTax(text),
            subtotal = extractSubtotal(text),
            rawText = text
        )
    }

    /**
     * Calculates parsing confidence based on extracted data completeness
     */
    fun getParsingConfidence(receiptData: ReceiptData): Float {
        var confidence = 0.0f
        
        // Base confidence factors
        if (receiptData.merchantName?.isNotEmpty() == true) confidence += 0.2f
        if (receiptData.date?.isNotEmpty() == true) confidence += 0.15f
        if (receiptData.total?.isNotEmpty() == true) confidence += 0.25f
        if (receiptData.items.isNotEmpty()) confidence += 0.2f
        if (receiptData.tax?.isNotEmpty() == true) confidence += 0.1f
        if (receiptData.subtotal?.isNotEmpty() == true) confidence += 0.1f
        
        // Bonus for well-structured data
        if (receiptData.items.size >= 2) confidence += 0.1f
        if (receiptData.currency.isNotEmpty()) confidence += 0.05f
        
        return confidence.coerceIn(0.0f, 1.0f)
    }

    private fun extractMerchantName(lines: List<String>): String? {
        // Look for merchant name in first few lines
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i].lowercase()
            
            // Check against known merchant patterns
            for ((pattern, name) in merchantPatterns) {
                if (line.contains(pattern)) {
                    return name
                }
            }
            
            // If first line looks like a store name (contains common words)
            if (i == 0 && (
                line.contains("market") || line.contains("store") || 
                line.contains("shop") || line.contains("restaurant") ||
                line.contains("hypermarket") || line.contains("supermarket") ||
                line.contains("متجر") || line.contains("سوق") || 
                line.contains("مطعم") || line.contains("هايبر")
            )) {
                return lines[i].uppercase()
            }
        }
        
        // Return first non-empty line if no pattern matched
        return lines.firstOrNull()?.takeIf { 
            it.length > 3 && !it.matches(Regex("\\d+.*")) 
        }?.uppercase()
    }

    private fun extractDate(text: String): String? {
        val datePatterns = listOf(
            // DD/MM/YYYY, DD-MM-YYYY
            Regex("\\b(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})\\b"),
            // YYYY-MM-DD
            Regex("\\b(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})\\b"),
            // DD MMM YYYY, DD Month YYYY
            Regex("\\b(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|\\w+)\\s+(\\d{4})\\b", RegexOption.IGNORE_CASE),
            // Arabic date patterns
            Regex("[٠-٩]{1,2}[/\\-][٠-٩]{1,2}[/\\-][٠-٩]{4}")
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                var dateString = match.value
                // Convert Arabic numbers to English
                dateString = convertArabicNumbers(dateString)
                return dateString
            }
        }

        return null
    }

    private fun extractTotal(text: String): String? {
        val totalPatterns = listOf(
            // English patterns
            Regex("total[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("amount[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("grand total[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            // Arabic patterns
            Regex("المجموع[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})"),
            Regex("الإجمالي[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})"),
            Regex("المجموع الكلي[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})"),
            // General amount patterns
            Regex("([\\d,]+\\.\\d{2})\\s*(?:SAR|ريال|SR)", RegexOption.IGNORE_CASE)
        )

        for (pattern in totalPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amount = match.groupValues[1]
                return convertArabicNumbers(amount).replace(",", "")
            }
        }

        return null
    }

    private fun extractCurrency(text: String): String {
        return when {
            text.contains("SAR", ignoreCase = true) -> "SAR"
            text.contains("ريال") -> "SAR"
            text.contains("درهم") -> "AED"
            text.contains("دينار") -> "KWD"
            text.contains("USD", ignoreCase = true) || text.contains("$") -> "USD"
            text.contains("EUR", ignoreCase = true) || text.contains("€") -> "EUR"
            else -> "SAR" // Default for Saudi market
        }
    }

    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        var inItemsSection = false
        
        for (line in lines) {
            val cleanLine = line.trim()
            
            // Skip header lines
            if (cleanLine.matches(Regex(".*(?:date|time|tax|vat|receipt|invoice|فاتورة|إيصال|التاريخ|الوقت).*", RegexOption.IGNORE_CASE))) {
                continue
            }
            
            // Skip total/subtotal lines
            if (cleanLine.matches(Regex(".*(?:total|subtotal|amount|tax|المجموع|الضريبة).*", RegexOption.IGNORE_CASE))) {
                continue
            }
            
            // Look for item pattern: name + price
            val itemPattern = Regex("(.+?)\\s+([\\d,٠-٩]+[\\.٫]\\d{2})")
            val match = itemPattern.find(cleanLine)
            
            if (match != null) {
                val itemName = match.groupValues[1].trim()
                val itemPrice = convertArabicNumbers(match.groupValues[2]).replace(",", "")
                
                // Extract quantity if present
                val quantityPattern = Regex("(.+?)\\s*x(\\d+)|(.+?)\\s*(\\d+)\\s*x", RegexOption.IGNORE_CASE)
                val quantityMatch = quantityPattern.find(itemName)
                
                val (finalName, quantity) = if (quantityMatch != null) {
                    val name = (quantityMatch.groupValues[1].takeIf { it.isNotEmpty() } 
                               ?: quantityMatch.groupValues[3]).trim()
                    val qty = (quantityMatch.groupValues[2].takeIf { it.isNotEmpty() } 
                              ?: quantityMatch.groupValues[4]).toIntOrNull() ?: 1
                    name to qty
                } else {
                    itemName to 1
                }
                
                if (finalName.isNotEmpty() && finalName.length > 2) {
                    items.add(ReceiptItem(
                        name = finalName,
                        price = itemPrice,
                        quantity = quantity,
                        category = categorizeItem(finalName)
                    ))
                }
            }
        }
        
        return items
    }

    private fun extractTax(text: String): String? {
        val taxPatterns = listOf(
            Regex("vat[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("tax[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("ضريبة[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})"),
            Regex("القيمة المضافة[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})"),
            Regex("\\(?15%\\)?[:\\s]*([\\d,]+\\.\\d{2})")
        )

        for (pattern in taxPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return convertArabicNumbers(match.groupValues[1]).replace(",", "")
            }
        }

        return null
    }

    private fun extractSubtotal(text: String): String? {
        val subtotalPatterns = listOf(
            Regex("subtotal[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("sub total[:\\s]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("المجموع الفرعي[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})"),
            Regex("المجموع قبل الضريبة[:\\s]*([\\d,٠-٩]+[\\.٫]\\d{2})")
        )

        for (pattern in subtotalPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return convertArabicNumbers(match.groupValues[1]).replace(",", "")
            }
        }

        return null
    }

    private fun categorizeItem(itemName: String): String? {
        val name = itemName.lowercase()
        
        return when {
            name.contains("apple") || name.contains("orange") || name.contains("banana") ||
            name.contains("تفاح") || name.contains("برتقال") || name.contains("موز") -> "Fruits"
            
            name.contains("milk") || name.contains("cheese") || name.contains("yogurt") ||
            name.contains("حليب") || name.contains("جبن") || name.contains("لبن") -> "Dairy"
            
            name.contains("bread") || name.contains("rice") || name.contains("pasta") ||
            name.contains("خبز") || name.contains("أرز") || name.contains("معكرونة") -> "Grains"
            
            name.contains("chicken") || name.contains("beef") || name.contains("fish") ||
            name.contains("دجاج") || name.contains("لحم") || name.contains("سمك") -> "Meat"
            
            name.contains("water") || name.contains("juice") || name.contains("coffee") ||
            name.contains("ماء") || name.contains("عصير") || name.contains("قهوة") -> "Beverages"
            
            else -> null
        }
    }

    private fun convertArabicNumbers(text: String): String {
        var result = text
        for ((arabic, english) in arabicNumbers) {
            result = result.replace(arabic, english)
        }
        return result
    }
}