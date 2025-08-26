package code.yousef.dari.shared.domain.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Transaction Domain Model
 * Represents a financial transaction with comprehensive metadata
 * Follows SAMA Open Banking transaction structure
 */
@Serializable
data class Transaction(
    val transactionId: String,
    val accountId: String,
    val bankCode: String,
    val amount: Money,
    val transactionType: TransactionType,
    val status: TransactionStatus,
    val description: String?,
    val merchantName: String?,
    val merchantCategory: String?,
    val reference: String?,
    val transactionDate: Instant,
    val bookingDate: Instant?,
    val valueDate: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val isRecurring: Boolean = false,
    val recurringGroupId: String? = null,
    val location: TransactionLocation? = null,
    val receipt: TransactionReceipt? = null,
    val originalAmount: Money? = null, // For currency conversions
    val exchangeRate: String? = null,
    val bankTransactionCode: String? = null,
    val purposeCode: String? = null,
    val runningBalance: Money? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    
    /**
     * Check if transaction is debit (outgoing)
     */
    fun isDebit(): Boolean = transactionType == TransactionType.DEBIT
    
    /**
     * Check if transaction is credit (incoming)
     */
    fun isCredit(): Boolean = transactionType == TransactionType.CREDIT
    
    /**
     * Check if transaction is pending
     */
    fun isPending(): Boolean = status == TransactionStatus.PENDING
    
    /**
     * Check if transaction is completed
     */
    fun isCompleted(): Boolean = status == TransactionStatus.COMPLETED
    
    /**
     * Get display amount with proper sign
     */
    fun getDisplayAmount(): Money {
        return if (isDebit()) {
            Money("-${amount.amount}", amount.currency)
        } else {
            amount
        }
    }
    
    /**
     * Get transaction display text
     */
    fun getDisplayText(): String {
        return merchantName ?: description ?: reference ?: "Transaction"
    }
    
    /**
     * Check if transaction has receipt
     */
    fun hasReceipt(): Boolean = receipt != null
    
    /**
     * Check if transaction is categorized
     */
    fun isCategorized(): Boolean = categoryId != null
    
    /**
     * Get transaction age in days
     */
    fun getAgeDays(): Int {
        val now = kotlinx.datetime.Clock.System.now()
        val duration = now - transactionDate
        return duration.inWholeDays.toInt()
    }
}

/**
 * Transaction Type enumeration
 */
@Serializable
enum class TransactionType(val displayName: String, val displayNameAr: String) {
    DEBIT("Debit", "خصم"),
    CREDIT("Credit", "إيداع")
}

/**
 * Transaction Status enumeration
 */
@Serializable
enum class TransactionStatus(val displayName: String, val displayNameAr: String) {
    PENDING("Pending", "معلق"),
    COMPLETED("Completed", "مكتمل"),
    FAILED("Failed", "فاشل"),
    CANCELLED("Cancelled", "ملغي")
}

/**
 * Transaction Location information
 */
@Serializable
data class TransactionLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String?
)

/**
 * Transaction Receipt information
 */
@Serializable
data class TransactionReceipt(
    val imagePath: String,
    val extractedData: Map<String, String> = emptyMap(),
    val confidence: Double = 0.0,
    val isVerified: Boolean = false
)