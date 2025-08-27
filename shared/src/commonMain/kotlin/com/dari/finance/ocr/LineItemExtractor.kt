package com.dari.finance.ocr

/**
 * Line item extractor for parsing individual items from receipt text
 * Handles various receipt formats common in Saudi retail
 */
class LineItemExtractor {

    private val amountExtractor = AmountExtractor()
    
    // Skip patterns for non-item lines
    private val skipPatterns = listOf(
        // Headers and footers
        Regex(".*(?:receipt|invoice|bill|ticket|store|market|hypermarket|branch).*", RegexOption.IGNORE_CASE),
        // Date and time
        Regex(".*(?:date|time|التاريخ|الوقت).*", RegexOption.IGNORE_CASE),
        // Totals and subtotals
        Regex(".*(?:total|subtotal|amount|sum|المجموع|الإجمالي).*", RegexOption.IGNORE_CASE),
        // Tax and VAT
        Regex(".*(?:tax|vat|levy|ضريبة|القيمة المضافة).*", RegexOption.IGNORE_CASE),
        // Payment info
        Regex(".*(?:payment|cash|card|credit|change|visa|mastercard|الدفع|نقد|بطاقة).*", RegexOption.IGNORE_CASE),
        // Customer info
        Regex(".*(?:customer|cashier|served by|thank you|الكاشير|العميل|شكرا).*", RegexOption.IGNORE_CASE),
        // Section headers
        Regex("^[=\\-_]{3,}$|^[A-Z\\s&]{3,}$", RegexOption.IGNORE_CASE),
        // Address and contact
        Regex(".*(?:tel|phone|fax|email|address|road|street|طريق|شارع|هاتف).*", RegexOption.IGNORE_CASE)
    )

    // Category keywords for automatic categorization
    private val categoryKeywords = mapOf(
        ItemCategory.DAIRY to listOf("milk", "cheese", "yogurt", "butter", "cream", "حليب", "جبن", "لبن", "زبدة"),
        ItemCategory.MEAT to listOf("chicken", "beef", "lamb", "meat", "sausage", "دجاج", "لحم", "خروف", "سجق"),
        ItemCategory.SEAFOOD to listOf("fish", "salmon", "tuna", "shrimp", "crab", "سمك", "سلمون", "تونة", "جمبري"),
        ItemCategory.FRUITS to listOf("apple", "orange", "banana", "grape", "mango", "تفاح", "برتقال", "موز", "عنب", "مانجو"),
        ItemCategory.VEGETABLES to listOf("tomato", "onion", "carrot", "potato", "lettuce", "طماطم", "بصل", "جزر", "بطاطس", "خس"),
        ItemCategory.BAKERY to listOf("bread", "cake", "pastry", "croissant", "bagel", "خبز", "كعك", "معجنات"),
        ItemCategory.BEVERAGES to listOf("water", "juice", "soda", "coffee", "tea", "ماء", "عصير", "قهوة", "شاي", "مشروب"),
        ItemCategory.SNACKS to listOf("chips", "chocolate", "candy", "nuts", "biscuit", "شيبس", "شوكولاته", "حلوى", "مكسرات"),
        ItemCategory.HOUSEHOLD to listOf("soap", "detergent", "tissue", "shampoo", "toothpaste", "صابون", "منظف", "مناديل", "شامبو"),
        ItemCategory.ELECTRONICS to listOf("phone", "charger", "battery", "cable", "earphone", "هاتف", "شاحن", "بطارية", "سماعة"),
        ItemCategory.CLOTHING to listOf("shirt", "pants", "dress", "shoes", "sock", "قميص", "بنطال", "فستان", "حذاء"),
        ItemCategory.HEALTH to listOf("medicine", "vitamin", "painkiller", "bandage", "دواء", "فيتامين", "مسكن", "ضمادة"),
        ItemCategory.BEAUTY to listOf("makeup", "lipstick", "perfume", "moisturizer", "مكياج", "أحمر شفاه", "عطر", "مرطب")
    )

    /**
     * Extracts all line items from receipt text
     */
    fun extractLineItems(text: String): List<LineItem> {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        val items = mutableListOf<LineItem>()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            // Skip non-item lines
            if (shouldSkipLine(line)) {
                i++
                continue
            }
            
            // Try to extract item from current line
            val item = extractItemFromLine(line)
            if (item != null) {
                // Look ahead for additional item details
                val enhancedItem = enhanceItemWithContext(item, lines, i)
                items.add(enhancedItem)
            }
            
            i++
        }
        
        return items.distinctBy { "${it.description}_${it.price}" }
    }

    /**
     * Extracts a single line item from a text line
     */
    private fun extractItemFromLine(line: String): LineItem? {
        // Pattern 1: Item description followed by price
        val pattern1 = Regex("(.+?)\\s+([\\d,٠-٩]+[\\.٫]\\d{2})\\s*(?:SAR|ريال|SR)?")
        val match1 = pattern1.find(line)
        
        if (match1 != null) {
            val description = cleanItemDescription(match1.groupValues[1])
            val price = amountExtractor.normalizeAmount(match1.groupValues[2])
            
            if (description.isNotEmpty() && amountExtractor.isValidAmount(price)) {
                return createLineItem(description, price, line)
            }
        }
        
        // Pattern 2: Numbered items (1. Item name  price)
        val pattern2 = Regex("\\d+\\.\\s*(.+?)\\s+([\\d,٠-٩]+[\\.٫]\\d{2})")
        val match2 = pattern2.find(line)
        
        if (match2 != null) {
            val description = cleanItemDescription(match2.groupValues[1])
            val price = amountExtractor.normalizeAmount(match2.groupValues[2])
            
            if (description.isNotEmpty() && amountExtractor.isValidAmount(price)) {
                return createLineItem(description, price, line)
            }
        }
        
        return null
    }

    /**
     * Creates a line item with extracted information
     */
    private fun createLineItem(description: String, price: String, originalLine: String): LineItem {
        return LineItem(
            description = description,
            price = price,
            quantity = extractQuantity(originalLine),
            unitPrice = extractUnitPrice(originalLine),
            weight = extractWeight(originalLine),
            sku = extractSKU(originalLine),
            barcode = extractBarcode(originalLine),
            category = categorizeItem(description),
            discount = extractDiscount(originalLine),
            taxRate = extractTaxRate(originalLine),
            isReturn = isReturnItem(originalLine),
            isRewardItem = isRewardItem(originalLine),
            loyaltyPoints = extractLoyaltyPoints(originalLine),
            promotion = extractPromotion(originalLine)
        )
    }

    /**
     * Enhances item with context from surrounding lines
     */
    private fun enhanceItemWithContext(item: LineItem, lines: List<String>, currentIndex: Int): LineItem {
        var enhancedItem = item
        
        // Look at next few lines for additional details
        for (i in currentIndex + 1 until minOf(currentIndex + 4, lines.size)) {
            val nextLine = lines[i].trim()
            
            // Skip if next line is another item
            if (extractItemFromLine(nextLine) != null) break
            
            // Extract additional details
            if (enhancedItem.sku == null) {
                enhancedItem = enhancedItem.copy(sku = extractSKU(nextLine))
            }
            
            if (enhancedItem.barcode == null) {
                enhancedItem = enhancedItem.copy(barcode = extractBarcode(nextLine))
            }
            
            if (enhancedItem.weight == null) {
                enhancedItem = enhancedItem.copy(weight = extractWeight(nextLine))
            }
            
            if (enhancedItem.unitPrice == null) {
                enhancedItem = enhancedItem.copy(unitPrice = extractUnitPrice(nextLine))
            }
            
            // Merge multi-line descriptions
            if (nextLine.length > 10 && !nextLine.matches(Regex(".*\\d+\\.\\d{2}.*")) && 
                !nextLine.matches(Regex("(?i).*(?:sku|barcode|weight).*"))) {
                enhancedItem = enhancedItem.copy(
                    description = "${enhancedItem.description} ${nextLine}".trim()
                )
            }
        }
        
        return enhancedItem
    }

    private fun shouldSkipLine(line: String): Boolean {
        if (line.length < 3) return true
        
        return skipPatterns.any { pattern -> pattern.matches(line) }
    }

    private fun cleanItemDescription(description: String): String {
        var cleaned = description.trim()
        
        // Remove quantity indicators from description
        cleaned = cleaned.replace(Regex("\\s*x\\d+\\s*$", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("\\s*\\d+x\\s*", RegexOption.IGNORE_CASE), " ")
        
        // Remove numbering
        cleaned = cleaned.replace(Regex("^\\d+\\.\\s*"), "")
        
        // Remove extra whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        
        return cleaned.trim()
    }

    private fun extractQuantity(line: String): Int {
        val quantityPatterns = listOf(
            Regex("\\s*x(\\d+)\\s*", RegexOption.IGNORE_CASE),
            Regex("\\s*(\\d+)x\\s*", RegexOption.IGNORE_CASE),
            Regex("qty[:\\s]*(\\d+)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in quantityPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 1
            }
        }
        
        return 1 // Default quantity
    }

    private fun extractUnitPrice(line: String): String? {
        val unitPricePattern = Regex("@([\\d,]+\\.\\d{2})(?:/kg|/lb|/pc|each)?", RegexOption.IGNORE_CASE)
        val match = unitPricePattern.find(line)
        return match?.groupValues?.get(1)?.let { amountExtractor.normalizeAmount(it) }
    }

    private fun extractWeight(line: String): String? {
        val weightPattern = Regex("([\\d,]+(?:\\.\\d+)?)\\s*(kg|g|lb|oz|lbs)", RegexOption.IGNORE_CASE)
        val match = weightPattern.find(line)
        return match?.value
    }

    private fun extractSKU(line: String): String? {
        val skuPattern = Regex("sku[:\\s]*([A-Z0-9\\-]+)", RegexOption.IGNORE_CASE)
        val match = skuPattern.find(line)
        return match?.groupValues?.get(1)
    }

    private fun extractBarcode(line: String): String? {
        val barcodePattern = Regex("barcode[:\\s]*(\\d{8,14})", RegexOption.IGNORE_CASE)
        val match = barcodePattern.find(line)
        return match?.groupValues?.get(1)
    }

    private fun categorizeItem(description: String): ItemCategory {
        val lowerDescription = description.lowercase()
        
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerDescription.contains(it) }) {
                return category
            }
        }
        
        return ItemCategory.OTHER
    }

    private fun extractDiscount(line: String): String? {
        val discountPatterns = listOf(
            Regex("discount[:\\s]*-?([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("save[d]?[:\\s]*-?([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE),
            Regex("-([\\d,]+\\.\\d{2})")
        )
        
        for (pattern in discountPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return null
    }

    private fun extractTaxRate(line: String): Float {
        return when {
            line.contains("15%", ignoreCase = true) -> 15.0f
            line.contains("5%", ignoreCase = true) -> 5.0f
            line.contains("0%", ignoreCase = true) || 
            line.contains("tax free", ignoreCase = true) -> 0.0f
            else -> 15.0f // Default VAT rate in Saudi Arabia
        }
    }

    private fun isReturnItem(line: String): Boolean {
        return line.contains(Regex("return|refund|استرداد", RegexOption.IGNORE_CASE)) ||
               line.contains(Regex("-[\\d,]+\\.\\d{2}"))
    }

    private fun isRewardItem(line: String): Boolean {
        return line.contains(Regex("free|reward|bonus|complimentary|مجان", RegexOption.IGNORE_CASE)) &&
               line.contains("0.00")
    }

    private fun extractLoyaltyPoints(line: String): Int? {
        val pointsPattern = Regex("\\(?([\\d,]+)\\s*(?:pts?|points?)\\)?", RegexOption.IGNORE_CASE)
        val match = pointsPattern.find(line)
        return match?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
    }

    private fun extractPromotion(line: String): String? {
        val promotionPatterns = listOf(
            Regex("buy\\s+\\d+\\s+get\\s+\\d+\\s+free", RegexOption.IGNORE_CASE),
            Regex("\\d+\\s*for\\s*[\\d,]+\\.\\d{2}", RegexOption.IGNORE_CASE),
            Regex("bulk\\s+discount", RegexOption.IGNORE_CASE),
            Regex("member\\s+(?:price|discount)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in promotionPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.value
            }
        }
        
        return null
    }
}

/**
 * Represents a single line item from a receipt
 */
data class LineItem(
    val description: String,
    val price: String,
    val quantity: Int = 1,
    val unitPrice: String? = null,
    val weight: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val category: ItemCategory = ItemCategory.OTHER,
    val discount: String? = null,
    val taxRate: Float = 15.0f, // Default Saudi VAT rate
    val isReturn: Boolean = false,
    val isRewardItem: Boolean = false,
    val loyaltyPoints: Int? = null,
    val promotion: String? = null
)