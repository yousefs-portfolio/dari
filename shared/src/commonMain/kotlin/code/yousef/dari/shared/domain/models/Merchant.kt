package code.yousef.dari.shared.domain.models

import kotlinx.datetime.LocalDateTime
import kotlin.math.*

/**
 * Location data for merchants
 */
data class MerchantLocation(
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val postalCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun hasCoordinates(): Boolean = latitude != 0.0 && longitude != 0.0
    
    fun isInSaudiArabia(): Boolean = country.equals("Saudi Arabia", ignoreCase = true) || 
                                    country.equals("المملكة العربية السعودية", ignoreCase = true)
}

/**
 * Generic location class for distance calculations
 */
data class Location(
    val latitude: Double,
    val longitude: Double
)

/**
 * Merchant transaction statistics
 */
data class MerchantStats(
    val totalTransactions: Int = 0,
    val totalAmount: Money = Money.sar(0),
    val averageAmount: Money = Money.sar(0),
    val lastTransactionDate: LocalDateTime? = null,
    val favoriteCategory: String = "",
    val monthlySpending: Map<String, Money> = emptyMap(), // Month -> Amount
    val frequencyRank: Int = 0 // How often user visits this merchant
)

/**
 * Merchant types for categorization
 */
enum class MerchantType {
    GROCERY,
    RESTAURANT,
    GAS_STATION,
    PHARMACY,
    CLOTHING,
    ELECTRONICS,
    BANK,
    ATM,
    HEALTHCARE,
    EDUCATION,
    TRANSPORT,
    ENTERTAINMENT,
    UTILITIES,
    GOVERNMENT,
    RELIGIOUS,
    OTHER
}

/**
 * Merchant domain model representing business entities
 * Supports Saudi-specific merchant features and Islamic compliance
 */
data class Merchant(
    val id: String,
    val name: String,
    val category: String,
    val vatNumber: String = "",
    val location: MerchantLocation = MerchantLocation(),
    val logoUrl: String = "",
    val website: String = "",
    val phone: String = "",
    val email: String = "",
    val isActive: Boolean = true,
    val chainName: String = "", // For chain stores like Al Danube, Carrefour
    val tags: List<String> = emptyList(),
    val stats: MerchantStats? = null,
    val isHalalCertified: Boolean = false,
    val certificationBody: String = "", // SFDA, SASO, etc.
    val businessHours: Map<String, String> = emptyMap(), // Day -> Hours
    val acceptedPayments: List<String> = emptyList(), // Cash, Card, Apple Pay, etc.
    val socialMediaLinks: Map<String, String> = emptyMap(),
    val lastUpdated: LocalDateTime? = null
) {
    init {
        require(id.isNotBlank()) { "Merchant ID cannot be blank" }
        require(name.isNotBlank()) { "Merchant name cannot be blank" }
        require(category.isNotBlank()) { "Merchant category cannot be blank" }
    }

    /**
     * Check if VAT number follows Saudi format (15 digits starting with 3)
     */
    fun isSaudiVatNumber(): Boolean {
        return vatNumber.length == 15 && 
               vatNumber.startsWith("3") && 
               vatNumber.all { it.isDigit() }
    }

    /**
     * Check if merchant name contains Arabic characters
     */
    fun hasArabicName(): Boolean {
        return name.any { char ->
            char in '\u0600'..'\u06FF' || // Arabic block
            char in '\u0750'..'\u077F' || // Arabic Supplement
            char in '\uFB50'..'\uFDFF' || // Arabic Presentation Forms-A
            char in '\uFE70'..'\uFEFF'    // Arabic Presentation Forms-B
        }
    }

    /**
     * Validate Saudi phone number format
     */
    fun isSaudiPhoneNumber(): Boolean {
        if (phone.isBlank()) return false
        
        // Remove spaces, hyphens, and parentheses
        val cleanPhone = phone.replace(Regex("[\\s\\-()]"), "")
        
        // Saudi phone patterns:
        // +966XXXXXXXXX (international format)
        // 05XXXXXXXX (mobile)
        // 01XXXXXXXX (landline)
        return when {
            cleanPhone.startsWith("+966") -> cleanPhone.length == 13 && cleanPhone.substring(4).all { it.isDigit() }
            cleanPhone.startsWith("05") -> cleanPhone.length == 10 && cleanPhone.all { it.isDigit() }
            cleanPhone.startsWith("01") -> cleanPhone.length == 10 && cleanPhone.all { it.isDigit() }
            else -> false
        }
    }

    /**
     * Calculate distance from a given location using Haversine formula
     */
    fun calculateDistanceFrom(userLocation: Location): Double {
        if (!location.hasCoordinates()) return Double.MAX_VALUE
        
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val lat1Rad = Math.toRadians(location.latitude)
        val lat2Rad = Math.toRadians(userLocation.latitude)
        val deltaLatRad = Math.toRadians(userLocation.latitude - location.latitude)
        val deltaLonRad = Math.toRadians(userLocation.longitude - location.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }

    /**
     * Get merchant type based on category
     */
    fun getMerchantType(): MerchantType {
        return when (category.lowercase()) {
            "groceries", "supermarket", "بقالة", "سوبرماركت" -> MerchantType.GROCERY
            "food & dining", "restaurant", "fast food", "مطاعم", "طعام" -> MerchantType.RESTAURANT
            "fuel", "gas station", "petrol", "وقود", "محطة بنزين" -> MerchantType.GAS_STATION
            "pharmacy", "medical", "صيدلية", "طبي" -> MerchantType.PHARMACY
            "clothing", "fashion", "ملابس", "أزياء" -> MerchantType.CLOTHING
            "electronics", "technology", "إلكترونيات", "تكنولوجيا" -> MerchantType.ELECTRONICS
            "bank", "banking", "مصرف", "بنك" -> MerchantType.BANK
            "atm", "cash machine", "صراف آلي" -> MerchantType.ATM
            "healthcare", "hospital", "clinic", "رعاية صحية", "مستشفى" -> MerchantType.HEALTHCARE
            "education", "school", "university", "تعليم", "جامعة" -> MerchantType.EDUCATION
            "transport", "transportation", "نقل", "مواصلات" -> MerchantType.TRANSPORT
            "entertainment", "cinema", "ترفيه", "سينما" -> MerchantType.ENTERTAINMENT
            "utilities", "electricity", "water", "مرافق", "كهرباء" -> MerchantType.UTILITIES
            "government", "municipal", "حكومي", "بلدية" -> MerchantType.GOVERNMENT
            "religious", "mosque", "ديني", "مسجد" -> MerchantType.RELIGIOUS
            else -> MerchantType.OTHER
        }
    }

    /**
     * Check if merchant belongs to a chain
     */
    fun isChainStore(): Boolean = chainName.isNotBlank()

    /**
     * Check if two merchants belong to the same chain
     */
    fun isSameChain(other: Merchant): Boolean {
        return chainName.isNotBlank() && chainName.equals(other.chainName, ignoreCase = true)
    }

    /**
     * Check if merchant is compliant with Islamic principles
     */
    fun isCompliantWithIslamicPrinciples(): Boolean {
        // Basic check for known non-halal categories
        val nonHalalCategories = setOf(
            "alcohol", "liquor", "wine", "beer", "gambling", "casino", "lottery",
            "interest-based lending", "conventional banking",
            "كحول", "خمور", "مقامرة", "كازينو", "يانصيب"
        )
        
        val categoryLower = category.lowercase()
        val nameLower = name.lowercase()
        
        return !nonHalalCategories.any { nonHalal ->
            categoryLower.contains(nonHalal) || nameLower.contains(nonHalal)
        }
    }

    /**
     * Get merchant rating based on transaction frequency and amounts
     */
    fun getFrequencyRating(): String {
        return when (stats?.frequencyRank ?: 0) {
            in 1..3 -> "Very Frequent"
            in 4..10 -> "Frequent"
            in 11..20 -> "Occasional"
            else -> "Rare"
        }
    }

    /**
     * Check if merchant is open at current time
     * Simplified version - would need more complex logic for actual implementation
     */
    fun isOpenNow(): Boolean {
        // TODO: Implement with actual time checking and business hours
        return isActive
    }

    /**
     * Get merchant summary for lists and quick display
     */
    fun toSummary(): MerchantSummary {
        return MerchantSummary(
            id = id,
            name = name,
            category = category,
            logoUrl = logoUrl,
            location = location.city,
            totalSpent = stats?.totalAmount ?: Money.sar(0),
            transactionCount = stats?.totalTransactions ?: 0,
            isHalalCertified = isHalalCertified,
            hasArabicName = hasArabicName()
        )
    }

    /**
     * Format merchant for display with proper Arabic/English handling
     */
    fun getDisplayName(): String {
        return if (hasArabicName()) {
            // For Arabic names, prefer Arabic display
            name
        } else {
            // For English names, capitalize properly
            name.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
    }

    /**
     * Get merchant location string for display
     */
    fun getLocationString(): String {
        return when {
            location.city.isNotBlank() && location.state.isNotBlank() -> 
                "${location.city}, ${location.state}"
            location.city.isNotBlank() -> location.city
            location.address.isNotBlank() -> location.address
            else -> location.country
        }
    }
}

/**
 * Summary representation of a merchant for lists and reporting
 */
data class MerchantSummary(
    val id: String,
    val name: String,
    val category: String,
    val logoUrl: String,
    val location: String,
    val totalSpent: Money,
    val transactionCount: Int,
    val isHalalCertified: Boolean,
    val hasArabicName: Boolean
)