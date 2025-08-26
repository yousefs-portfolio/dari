package code.yousef.dari.shared.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ReceiptTest {

    @Test
    fun `should create receipt with valid data`() {
        // Given
        val receiptId = "receipt_123"
        val transactionId = "transaction_456"
        val merchantName = "Al Danube Supermarket"
        val amount = Money.sar(15000) // 150.00 SAR
        val date = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val items = listOf(
            ReceiptItem("Milk", Money.sar(500), 2),
            ReceiptItem("Bread", Money.sar(300), 1)
        )
        val taxAmount = Money.sar(750) // 7.50 SAR VAT
        val imageUrl = "https://storage.example.com/receipts/receipt_123.jpg"

        // When
        val receipt = Receipt(
            id = receiptId,
            transactionId = transactionId,
            merchantName = merchantName,
            totalAmount = amount,
            date = date,
            items = items,
            taxAmount = taxAmount,
            imageUrl = imageUrl,
            category = "Groceries",
            notes = "Weekly shopping"
        )

        // Then
        assertEquals(receiptId, receipt.id)
        assertEquals(transactionId, receipt.transactionId)
        assertEquals(merchantName, receipt.merchantName)
        assertEquals(amount, receipt.totalAmount)
        assertEquals(date, receipt.date)
        assertEquals(items, receipt.items)
        assertEquals(taxAmount, receipt.taxAmount)
        assertEquals(imageUrl, receipt.imageUrl)
        assertEquals("Groceries", receipt.category)
        assertEquals("Weekly shopping", receipt.notes)
    }

    @Test
    fun `should create receipt with minimal required data`() {
        // Given
        val receiptId = "receipt_123"
        val transactionId = "transaction_456"
        val merchantName = "Local Shop"
        val amount = Money.sar(5000) // 50.00 SAR
        val date = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // When
        val receipt = Receipt(
            id = receiptId,
            transactionId = transactionId,
            merchantName = merchantName,
            totalAmount = amount,
            date = date
        )

        // Then
        assertEquals(receiptId, receipt.id)
        assertEquals(transactionId, receipt.transactionId)
        assertEquals(merchantName, receipt.merchantName)
        assertEquals(amount, receipt.totalAmount)
        assertEquals(date, receipt.date)
        assertTrue(receipt.items.isEmpty())
        assertEquals(Money.sar(0), receipt.taxAmount)
        assertEquals("", receipt.imageUrl)
        assertEquals("", receipt.category)
        assertEquals("", receipt.notes)
    }

    @Test
    fun `should calculate total from items when items are provided`() {
        // Given
        val items = listOf(
            ReceiptItem("Coffee", Money.sar(1500), 1),
            ReceiptItem("Cake", Money.sar(2000), 2)
        )
        val expectedTotal = Money.sar(5500) // 15 + 20 + 20 = 55 SAR

        // When
        val receipt = Receipt(
            id = "receipt_123",
            transactionId = "transaction_456",
            merchantName = "Cafe Bateel",
            totalAmount = expectedTotal,
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            items = items
        )

        // Then
        val calculatedTotal = receipt.calculateTotalFromItems()
        assertEquals(expectedTotal, calculatedTotal)
    }

    @Test
    fun `should validate VAT calculation for Saudi receipts`() {
        // Given
        val subtotal = Money.sar(10000) // 100.00 SAR
        val vatRate = 0.15 // 15% VAT in Saudi Arabia
        val expectedVat = Money.sar(1500) // 15.00 SAR
        val expectedTotal = Money.sar(11500) // 115.00 SAR

        // When
        val receipt = Receipt(
            id = "receipt_123",
            transactionId = "transaction_456",
            merchantName = "Saudi Restaurant",
            totalAmount = expectedTotal,
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            taxAmount = expectedVat
        )

        // Then
        assertTrue(receipt.isVatValid(subtotal, vatRate))
        assertEquals(expectedVat, receipt.taxAmount)
    }

    @Test
    fun `should fail with invalid receipt id`() {
        // Given
        val invalidId = ""
        val transactionId = "transaction_456"
        val merchantName = "Test Merchant"
        val amount = Money.sar(5000)
        val date = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Receipt(
                id = invalidId,
                transactionId = transactionId,
                merchantName = merchantName,
                totalAmount = amount,
                date = date
            )
        }
    }

    @Test
    fun `should fail with invalid transaction id`() {
        // Given
        val receiptId = "receipt_123"
        val invalidTransactionId = ""
        val merchantName = "Test Merchant"
        val amount = Money.sar(5000)
        val date = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Receipt(
                id = receiptId,
                transactionId = invalidTransactionId,
                merchantName = merchantName,
                totalAmount = amount,
                date = date
            )
        }
    }

    @Test
    fun `should fail with empty merchant name`() {
        // Given
        val receiptId = "receipt_123"
        val transactionId = "transaction_456"
        val emptyMerchantName = ""
        val amount = Money.sar(5000)
        val date = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Receipt(
                id = receiptId,
                transactionId = transactionId,
                merchantName = emptyMerchantName,
                totalAmount = amount,
                date = date
            )
        }
    }

    @Test
    fun `should fail with negative amount`() {
        // Given
        val receiptId = "receipt_123"
        val transactionId = "transaction_456"
        val merchantName = "Test Merchant"
        val negativeAmount = Money.sar(-5000)
        val date = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Receipt(
                id = receiptId,
                transactionId = transactionId,
                merchantName = merchantName,
                totalAmount = negativeAmount,
                date = date
            )
        }
    }

    @Test
    fun `should support Saudi-specific receipt formats`() {
        // Given
        val receipt = Receipt(
            id = "receipt_123",
            transactionId = "transaction_456",
            merchantName = "مطعم النخيل", // Arabic merchant name
            totalAmount = Money.sar(8500), // 85.00 SAR
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            items = listOf(
                ReceiptItem("كبسة دجاج", Money.sar(3500), 1), // Chicken Kabsa
                ReceiptItem("عصير برتقال", Money.sar(1500), 2) // Orange juice
            ),
            taxAmount = Money.sar(1280), // 15% VAT
            category = "مطاعم", // Restaurants in Arabic
            vatNumber = "300012345600003" // Saudi VAT number format
        )

        // Then
        assertEquals("مطعم النخيل", receipt.merchantName)
        assertEquals("مطاعم", receipt.category)
        assertEquals("300012345600003", receipt.vatNumber)
        assertTrue(receipt.isSaudiVatNumber())
    }
}