package code.yousef.dari.shared.domain.models

import kotlinx.datetime.LocalDateTime

/**
 * Receipt item representing individual line items on a receipt
 */
data class ReceiptItem(
    val description: String,
    val amount: Money,
    val quantity: Int = 1,
    val unitPrice: Money = amount,
    val category: String = "",
    val sku: String = ""
) {
    init {
        require(description.isNotBlank()) { "Receipt item description cannot be blank" }
        require(quantity > 0) { "Receipt item quantity must be positive" }
        require(amount.amount >= 0) { "Receipt item amount cannot be negative" }
    }

    /**
     * Calculate total amount for this item (unitPrice * quantity)
     */
    fun calculateTotal(): Money {
        return Money(unitPrice.amount * quantity, unitPrice.currency)
    }
}

/**
 * Receipt domain model representing a purchase receipt
 * Supports Saudi-specific receipt formats and VAT requirements
 */
data class Receipt(
    val id: String,
    val transactionId: String,
    val merchantName: String,
    val totalAmount: Money,
    val date: LocalDateTime,
    val items: List<ReceiptItem> = emptyList(),
    val taxAmount: Money = Money(0, totalAmount.currency),
    val imageUrl: String = "",
    val category: String = "",
    val notes: String = "",
    val vatNumber: String = "", // Saudi VAT number (15 digits starting with 3)
    val location: String = "",
    val paymentMethod: String = "",
    val receiptNumber: String = "",
    val cashierName: String = "",
    val subtotalAmount: Money = totalAmount - taxAmount
) {
    init {
        require(id.isNotBlank()) { "Receipt ID cannot be blank" }
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank" }
        require(merchantName.isNotBlank()) { "Merchant name cannot be blank" }
        require(totalAmount.amount >= 0) { "Receipt total amount cannot be negative" }
        require(taxAmount.amount >= 0) { "Tax amount cannot be negative" }
        require(subtotalAmount.amount >= 0) { "Subtotal amount cannot be negative" }
        
        // Validate that total = subtotal + tax (with small tolerance for rounding)
        val calculatedTotal = subtotalAmount + taxAmount
        val tolerance = Money(1, totalAmount.currency) // 1 cent/halala tolerance
        require(
            kotlin.math.abs(totalAmount.amount - calculatedTotal.amount) <= tolerance.amount
        ) { "Total amount must equal subtotal + tax amount" }
    }

    /**
     * Calculate total amount from individual receipt items
     */
    fun calculateTotalFromItems(): Money {
        if (items.isEmpty()) return totalAmount
        
        return items.fold(Money(0, totalAmount.currency)) { acc, item ->
            acc + item.calculateTotal()
        }
    }

    /**
     * Validate VAT calculation based on Saudi tax rate (15%)
     */
    fun isVatValid(subtotal: Money, vatRate: Double = 0.15): Boolean {
        val expectedVat = Money((subtotal.amount * vatRate).toLong(), subtotal.currency)
        val tolerance = Money(1, subtotal.currency) // 1 cent/halala tolerance
        return kotlin.math.abs(taxAmount.amount - expectedVat.amount) <= tolerance.amount
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
     * Get receipt formatted as text for export/sharing
     */
    fun toFormattedText(): String {
        return buildString {
            appendLine("=== RECEIPT ===")
            appendLine("Merchant: $merchantName")
            if (location.isNotBlank()) appendLine("Location: $location")
            appendLine("Date: $date")
            if (receiptNumber.isNotBlank()) appendLine("Receipt #: $receiptNumber")
            if (vatNumber.isNotBlank()) appendLine("VAT #: $vatNumber")
            
            if (items.isNotEmpty()) {
                appendLine("\n--- ITEMS ---")
                items.forEach { item ->
                    val itemTotal = item.calculateTotal()
                    appendLine("${item.quantity}x ${item.description}")
                    appendLine("  ${item.unitPrice} each = ${itemTotal}")
                }
            }
            
            appendLine("\n--- TOTALS ---")
            if (subtotalAmount.amount > 0 && taxAmount.amount > 0) {
                appendLine("Subtotal: $subtotalAmount")
                appendLine("VAT (15%): $taxAmount")
            }
            appendLine("TOTAL: $totalAmount")
            
            if (paymentMethod.isNotBlank()) {
                appendLine("Payment: $paymentMethod")
            }
            
            if (notes.isNotBlank()) {
                appendLine("\nNotes: $notes")
            }
            
            appendLine("===============")
        }
    }

    /**
     * Check if receipt is eligible for expense claims (business category)
     */
    fun isBusinessExpense(): Boolean {
        val businessCategories = setOf(
            "Business Meals",
            "Office Supplies", 
            "Travel",
            "Fuel",
            "Parking",
            "Professional Services",
            "اجتماعات عمل", // Business meetings in Arabic
            "مستلزمات مكتبية", // Office supplies in Arabic
            "سفر عمل" // Business travel in Arabic
        )
        return category in businessCategories
    }

    /**
     * Get tax year for this receipt based on Saudi fiscal year
     */
    fun getSaudiTaxYear(): Int {
        // Saudi tax year follows calendar year
        return date.year
    }

    /**
     * Check if receipt qualifies for Zakat calculation
     */
    fun isZakatEligible(): Boolean {
        // Zakat typically applies to saved money and business income
        val zakatCategories = setOf(
            "Business Income",
            "Investment",
            "Rental Income",
            "دخل تجاري", // Business income in Arabic
            "استثمار", // Investment in Arabic
            "دخل إيجار" // Rental income in Arabic
        )
        return category in zakatCategories
    }

    /**
     * Convert receipt to summary for reporting
     */
    fun toSummary(): ReceiptSummary {
        return ReceiptSummary(
            id = id,
            merchantName = merchantName,
            totalAmount = totalAmount,
            date = date,
            category = category,
            hasImage = imageUrl.isNotBlank(),
            itemCount = items.size,
            isBusinessExpense = isBusinessExpense()
        )
    }
}

/**
 * Summary representation of a receipt for lists and reporting
 */
data class ReceiptSummary(
    val id: String,
    val merchantName: String,
    val totalAmount: Money,
    val date: LocalDateTime,
    val category: String,
    val hasImage: Boolean,
    val itemCount: Int,
    val isBusinessExpense: Boolean
)