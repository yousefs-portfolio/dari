package com.dari.finance.ocr

/**
 * Merchant name extraction utility for OCR receipt processing
 * Handles Saudi retail chains, restaurants, and local merchants
 */
class MerchantExtractor {

    // Known merchant patterns for Saudi market
    private val knownMerchants = mapOf(
        // Hypermarkets & Supermarkets
        "lulu" to "LULU HYPERMARKET",
        "carrefour" to "CARREFOUR HYPERMARKET", 
        "extra" to "EXTRA STORES",
        "danube" to "DANUBE SUPERMARKET",
        "panda" to "PANDA",
        "tamimi" to "TAMIMI MARKETS",
        "bin dawood" to "BINDAWOOD SUPERMARKETS",
        "bindawood" to "BINDAWOOD SUPERMARKETS",
        "nesto" to "NESTO HYPERMARKET",
        "farm" to "FARM SUPERMARKET",
        
        // Arabic names
        "لولو" to "LULU HYPERMARKET",
        "كارفور" to "CARREFOUR HYPERMARKET",
        "اكسترا" to "EXTRA STORES", 
        "الدانوب" to "DANUBE SUPERMARKET",
        "بندة" to "PANDA",
        "التميمي" to "TAMIMI MARKETS",
        "بن داود" to "BINDAWOOD SUPERMARKETS",
        
        // Restaurants
        "mcdonald" to "MCDONALD'S",
        "kfc" to "KFC",
        "subway" to "SUBWAY", 
        "starbucks" to "STARBUCKS",
        "hardee" to "HARDEE'S",
        "burger king" to "BURGER KING",
        "pizza hut" to "PIZZA HUT",
        "domino" to "DOMINO'S PIZZA",
        "papa johns" to "PAPA JOHN'S",
        "dunkin" to "DUNKIN' DONUTS",
        
        // Retail
        "saco" to "SACO STORE",
        "jarir" to "JARIR BOOKSTORE",
        "centrepoint" to "CENTREPOINT",
        "max" to "MAX FASHION",
        "home centre" to "HOME CENTRE",
        "ikea" to "IKEA",
        
        // Gas Stations
        "aramco" to "SAUDI ARAMCO",
        "shell" to "SHELL",
        "total" to "TOTAL",
        "adnoc" to "ADNOC",
        
        // Online
        "noon" to "NOON.COM",
        "amazon" to "AMAZON",
        "souq" to "SOUQ.COM"
    )

    private val merchantIndicators = listOf(
        "market", "hypermarket", "supermarket", "store", "shop", "mall",
        "restaurant", "cafe", "coffee", "food", "kitchen", "grill",
        "station", "service", "center", "centre", "company", "group",
        "متجر", "سوق", "هايبر", "مطعم", "مقهى", "محل", "شركة"
    )

    /**
     * Extracts merchant name from receipt text
     */
    fun extractMerchantName(text: String): String? {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        // Try to find known merchant first
        val knownMerchant = findKnownMerchant(text)
        if (knownMerchant != null) return knownMerchant

        // Look in first few lines for merchant-like patterns
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            
            // Skip obvious non-merchant lines
            if (isNonMerchantLine(line)) continue
            
            // Check if line looks like a merchant name
            if (looksLikeMerchantName(line)) {
                return normalizeMerchantName(line)
            }
        }

        // Fallback: return first non-trivial line
        return lines.firstOrNull { 
            it.length > 3 && !it.matches(Regex("\\d+.*")) && !isNonMerchantLine(it)
        }?.let { normalizeMerchantName(it) }
    }

    /**
     * Extracts merchant with confidence score
     */
    fun extractMerchantWithConfidence(text: String): MerchantWithConfidence? {
        val merchantName = extractMerchantName(text) ?: return null
        
        var confidence = 0.3f // Base confidence
        
        // Increase confidence for known merchants
        if (isKnownMerchant(merchantName)) confidence += 0.4f
        
        // Increase confidence for merchant indicators
        if (containsMerchantIndicators(merchantName)) confidence += 0.2f
        
        // Increase confidence if found in header position
        val lines = text.split('\n').map { it.trim() }
        if (lines.take(3).any { normalizeMerchantName(it) == merchantName }) confidence += 0.2f
        
        // Increase confidence for proper case and format
        if (merchantName.matches(Regex("[A-Z][A-Z\\s'&.]+[A-Z]"))) confidence += 0.1f
        
        return MerchantWithConfidence(merchantName, confidence.coerceIn(0.0f, 1.0f))
    }

    /**
     * Extracts comprehensive merchant information
     */
    fun extractMerchantInfo(text: String): MerchantInfo {
        val merchantName = extractMerchantName(text)
        
        return MerchantInfo(
            name = merchantName ?: "UNKNOWN",
            address = extractAddress(text),
            location = extractLocation(text),
            phone = extractPhone(text),
            taxId = extractTaxId(text),
            registrationNumber = extractRegistrationNumber(text),
            branch = extractBranch(text),
            storeNumber = extractStoreNumber(text),
            category = categorizeMerchant(merchantName ?: ""),
            isOnline = detectOnlineMerchant(text),
            orderNumber = extractOrderNumber(text)
        )
    }

    /**
     * Categorizes merchant by business type
     */
    fun categorizeMerchant(merchantName: String): MerchantCategory {
        val lowerName = merchantName.lowercase()
        
        return when {
            // Grocery/Hypermarket
            listOf("lulu", "carrefour", "extra", "danube", "panda", "tamimi", "bindawood", "nesto", "farm")
                .any { lowerName.contains(it) } -> MerchantCategory.GROCERY
            
            // Restaurants
            listOf("mcdonald", "kfc", "subway", "starbucks", "hardee", "burger", "pizza", "domino", "papa", "dunkin")
                .any { lowerName.contains(it) } -> MerchantCategory.RESTAURANT
            
            // Gas Stations
            listOf("aramco", "shell", "total", "adnoc", "station", "petrol", "fuel")
                .any { lowerName.contains(it) } -> MerchantCategory.GAS_STATION
            
            // Financial
            listOf("bank", "sabb", "rajhi", "riyadh", "snb", "alinma", "albilad", "stc pay", "mada")
                .any { lowerName.contains(it) } -> MerchantCategory.FINANCIAL
            
            // Healthcare
            listOf("hospital", "clinic", "pharmacy", "medical", "dr.", "مستشفى", "عيادة", "صيدلية")
                .any { lowerName.contains(it) } -> MerchantCategory.HEALTHCARE
            
            // Telecom
            listOf("stc", "mobily", "zain", "telecom", "اتصالات", "موبايلي", "زين")
                .any { lowerName.contains(it) } -> MerchantCategory.TELECOM
            
            // Online
            listOf("noon", "amazon", "souq", ".com", "online", "delivery")
                .any { lowerName.contains(it) } -> MerchantCategory.ONLINE
            
            // Default retail
            else -> MerchantCategory.RETAIL
        }
    }

    /**
     * Normalizes merchant name to standard format
     */
    fun normalizeMerchantName(merchantName: String): String {
        var normalized = merchantName.trim().uppercase()
        
        // Remove common prefixes/suffixes
        normalized = normalized.removePrefix("THE ").removeSuffix(" LLC")
            .removeSuffix(" CO.").removeSuffix(" LTD")
        
        // Handle known merchant variations
        for ((pattern, standardName) in knownMerchants) {
            if (normalized.lowercase().contains(pattern)) {
                return standardName
            }
        }
        
        // Clean up extra spaces and special characters
        normalized = normalized.replace(Regex("\\s+"), " ")
            .replace(Regex("[^A-Z0-9\\s'&.-]"), "")
        
        return normalized
    }

    private fun findKnownMerchant(text: String): String? {
        val lowerText = text.lowercase()
        
        for ((pattern, merchantName) in knownMerchants) {
            if (lowerText.contains(pattern)) {
                return merchantName
            }
        }
        
        return null
    }

    private fun isKnownMerchant(merchantName: String): Boolean {
        return knownMerchants.values.any { 
            merchantName.contains(it.split(" ")[0], ignoreCase = true) 
        }
    }

    private fun looksLikeMerchantName(line: String): Boolean {
        val cleanLine = line.trim()
        
        // Too short or too long
        if (cleanLine.length < 3 || cleanLine.length > 50) return false
        
        // Contains merchant indicators
        if (containsMerchantIndicators(cleanLine)) return true
        
        // Looks like a business name (title case, multiple words)
        if (cleanLine.split(" ").size >= 2 && 
            cleanLine.any { it.isLetter() } &&
            !cleanLine.matches(Regex("\\d+.*"))) return true
        
        return false
    }

    private fun containsMerchantIndicators(text: String): Boolean {
        val lowerText = text.lowercase()
        return merchantIndicators.any { lowerText.contains(it) }
    }

    private fun isNonMerchantLine(line: String): Boolean {
        val lowerLine = line.lowercase()
        
        // Skip header/footer information
        val skipPatterns = listOf(
            "receipt", "invoice", "bill", "ticket", "date", "time", "cashier",
            "customer", "thank you", "total", "subtotal", "tax", "vat",
            "payment", "change", "balance", "card", "cash",
            "إيصال", "فاتورة", "التاريخ", "الوقت", "الكاشير", "شكرا",
            "المجموع", "الضريبة", "الدفع", "الباقي"
        )
        
        return skipPatterns.any { lowerLine.contains(it) } ||
               line.matches(Regex("\\d{2}/\\d{2}/\\d{4}.*")) || // Date pattern
               line.matches(Regex("\\d{2}:\\d{2}.*")) || // Time pattern
               line.matches(Regex("#\\d+")) // Receipt number
    }

    private fun extractAddress(text: String): String? {
        val addressPattern = Regex("(.*)(?:road|street|avenue|blvd|طريق|شارع|حي)(.*)", RegexOption.IGNORE_CASE)
        val match = addressPattern.find(text)
        return match?.value?.trim()?.takeIf { it.length > 10 }
    }

    private fun extractLocation(text: String): String? {
        val saudiCities = listOf(
            "riyadh", "jeddah", "mecca", "medina", "dammam", "khobar", "dhahran",
            "tabuk", "abha", "najran", "jazan", "hail", "qassim", "الرياض", 
            "جدة", "مكة", "المدينة", "الدمام", "الخبر", "الظهران"
        )
        
        val lowerText = text.lowercase()
        return saudiCities.find { lowerText.contains(it) }?.uppercase()
    }

    private fun extractPhone(text: String): String? {
        val phonePattern = Regex("(?:\\+966|966|0)?\\s*[15][0-9]\\s*\\d{3}\\s*\\d{4}")
        return phonePattern.find(text)?.value?.replace(Regex("\\s+"), "")
    }

    private fun extractTaxId(text: String): String? {
        val taxIdPattern = Regex("(?:tax id|vat reg|tax reg)[:\\s]*([\\d]+)", RegexOption.IGNORE_CASE)
        return taxIdPattern.find(text)?.groupValues?.get(1)
    }

    private fun extractRegistrationNumber(text: String): String? {
        val crPattern = Regex("(?:c\\.r\\.|cr)[:\\s]*no[:\\s]*([\\d]+)", RegexOption.IGNORE_CASE)
        return crPattern.find(text)?.groupValues?.get(1)
    }

    private fun extractBranch(text: String): String? {
        val branchPattern = Regex("branch[:\\s]*([^\\n]+)", RegexOption.IGNORE_CASE)
        return branchPattern.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractStoreNumber(text: String): String? {
        val storePattern = Regex("store[:\\s]*#?([\\d]+)", RegexOption.IGNORE_CASE)
        return storePattern.find(text)?.groupValues?.get(1)
    }

    private fun detectOnlineMerchant(text: String): Boolean {
        val onlineIndicators = listOf(".com", "online", "app", "website", "delivery", "order #")
        return onlineIndicators.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractOrderNumber(text: String): String? {
        val orderPattern = Regex("order[:\\s]*#?([A-Z0-9]+)", RegexOption.IGNORE_CASE)
        return orderPattern.find(text)?.groupValues?.get(1)
    }
}

/**
 * Merchant information data class
 */
data class MerchantInfo(
    val name: String,
    val address: String? = null,
    val location: String? = null,
    val phone: String? = null,
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val branch: String? = null,
    val storeNumber: String? = null,
    val category: MerchantCategory = MerchantCategory.RETAIL,
    val isOnline: Boolean = false,
    val orderNumber: String? = null
)

/**
 * Merchant with confidence score
 */
data class MerchantWithConfidence(
    val name: String,
    val confidence: Float
)

/**
 * Merchant category enumeration
 */
enum class MerchantCategory {
    GROCERY,
    RESTAURANT, 
    RETAIL,
    GAS_STATION,
    FINANCIAL,
    HEALTHCARE,
    TELECOM,
    ONLINE,
    OTHER
}