package com.dari.finance.receipt

import com.dari.finance.ocr.ReceiptData
import com.dari.finance.ocr.ReceiptItem
import kotlin.test.*

class ReceiptValidatorTest {

    private val validator = ReceiptValidator()

    @Test
    fun `should validate complete receipt data`() {
        val validReceipt = ReceiptData(
            merchantName = "LULU HYPERMARKET",
            date = "15/01/2024",
            total = "125.50",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Apples", "25.00"),
                ReceiptItem("Milk", "12.50"),
                ReceiptItem("Bread", "8.00")
            ),
            tax = "18.75",
            subtotal = "106.75"
        )

        val result = validator.validateReceipt(validReceipt)

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun `should detect missing required fields`() {
        val incompleteReceipt = ReceiptData(
            merchantName = null,
            date = null,
            total = "125.50",
            currency = "SAR",
            items = emptyList()
        )

        val result = validator.validateReceipt(incompleteReceipt)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.type == ValidationErrorType.MISSING_MERCHANT })
        assertTrue(result.errors.any { it.type == ValidationErrorType.MISSING_DATE })
        assertTrue(result.errors.any { it.type == ValidationErrorType.NO_ITEMS })
    }

    @Test
    fun `should validate amount formats`() {
        val testCases = listOf(
            "125.50" to true,
            "0.99" to true,
            "1000.00" to true,
            "125.5" to false, // Missing second decimal
            "125" to false, // No decimals
            "-125.50" to false, // Negative amount
            "abc.def" to false, // Not a number
            "" to false // Empty
        )

        testCases.forEach { (amount, shouldBeValid) ->
            val receipt = ReceiptData(total = amount, currency = "SAR")
            val result = validator.validateReceipt(receipt)

            if (shouldBeValid) {
                assertFalse(result.errors.any { it.type == ValidationErrorType.INVALID_AMOUNT_FORMAT },
                    "Amount $amount should be valid")
            } else {
                assertTrue(result.errors.any { it.type == ValidationErrorType.INVALID_AMOUNT_FORMAT },
                    "Amount $amount should be invalid")
            }
        }
    }

    @Test
    fun `should validate date formats`() {
        val validDates = listOf(
            "15/01/2024",
            "2024-01-15",
            "Jan 15, 2024",
            "15 January 2024"
        )

        val invalidDates = listOf(
            "32/01/2024", // Invalid day
            "15/13/2024", // Invalid month
            "15/01/1900", // Too old
            "15/01/2050", // Too far in future
            "invalid-date",
            ""
        )

        validDates.forEach { date ->
            val receipt = ReceiptData(date = date, total = "25.00", currency = "SAR")
            val result = validator.validateReceipt(receipt)
            assertFalse(result.errors.any { it.type == ValidationErrorType.INVALID_DATE_FORMAT },
                "Date $date should be valid")
        }

        invalidDates.forEach { date ->
            val receipt = ReceiptData(date = date, total = "25.00", currency = "SAR")
            val result = validator.validateReceipt(receipt)
            assertTrue(result.errors.any { it.type == ValidationErrorType.INVALID_DATE_FORMAT },
                "Date $date should be invalid")
        }
    }

    @Test
    fun `should validate mathematical consistency`() {
        // Test correct calculation
        val correctReceipt = ReceiptData(
            total = "127.50",
            subtotal = "110.00",
            tax = "17.50", // 15% VAT
            currency = "SAR",
            items = listOf(
                ReceiptItem("Item 1", "50.00"),
                ReceiptItem("Item 2", "60.00")
            )
        )

        var result = validator.validateReceipt(correctReceipt)
        assertFalse(result.errors.any { it.type == ValidationErrorType.MATH_INCONSISTENCY })

        // Test incorrect calculation
        val incorrectReceipt = ReceiptData(
            total = "200.00", // Should be 127.50
            subtotal = "110.00",
            tax = "17.50",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Item 1", "50.00"),
                ReceiptItem("Item 2", "60.00")
            )
        )

        result = validator.validateReceipt(incorrectReceipt)
        assertTrue(result.errors.any { it.type == ValidationErrorType.MATH_INCONSISTENCY })
    }

    @Test
    fun `should validate currency codes`() {
        val validCurrencies = listOf("SAR", "USD", "EUR", "AED", "KWD")
        val invalidCurrencies = listOf("", "INVALID", "123", "ريال")

        validCurrencies.forEach { currency ->
            val receipt = ReceiptData(currency = currency, total = "25.00")
            val result = validator.validateReceipt(receipt)
            assertFalse(result.errors.any { it.type == ValidationErrorType.INVALID_CURRENCY },
                "Currency $currency should be valid")
        }

        invalidCurrencies.forEach { currency ->
            val receipt = ReceiptData(currency = currency, total = "25.00")
            val result = validator.validateReceipt(receipt)
            assertTrue(result.errors.any { it.type == ValidationErrorType.INVALID_CURRENCY },
                "Currency $currency should be invalid")
        }
    }

    @Test
    fun `should validate item consistency`() {
        val receipt = ReceiptData(
            total = "125.50",
            subtotal = "110.00",
            items = listOf(
                ReceiptItem("Valid Item", "25.00"),
                ReceiptItem("", "30.00"), // Empty name
                ReceiptItem("Another Item", "invalid"), // Invalid price
                ReceiptItem("Negative Item", "-15.00") // Negative price
            ),
            currency = "SAR"
        )

        val result = validator.validateReceipt(receipt)

        assertTrue(result.errors.any { it.type == ValidationErrorType.INVALID_ITEM })
        assertTrue(result.warnings.any { it.contains("empty name") })
    }

    @Test
    fun `should detect duplicate items`() {
        val receiptWithDuplicates = ReceiptData(
            items = listOf(
                ReceiptItem("Apple Juice", "12.50"),
                ReceiptItem("Milk", "8.00"),
                ReceiptItem("Apple Juice", "12.50"), // Exact duplicate
                ReceiptItem("Bread", "3.50")
            ),
            total = "36.50",
            currency = "SAR"
        )

        val result = validator.validateReceipt(receiptWithDuplicates)

        assertTrue(result.warnings.any { it.contains("duplicate") })
    }

    @Test
    fun `should validate merchant name format`() {
        val testCases = listOf(
            "LULU HYPERMARKET" to true,
            "McDonald's Restaurant" to true,
            "Café Corner" to true,
            "" to false, // Empty
            "A" to false, // Too short
            "X".repeat(100) to false, // Too long
            "123456789" to false, // Only numbers
            "!@#$%^&*()" to false // Only special characters
        )

        testCases.forEach { (merchantName, shouldBeValid) ->
            val receipt = ReceiptData(
                merchantName = merchantName,
                total = "25.00",
                currency = "SAR"
            )
            val result = validator.validateReceipt(receipt)

            if (shouldBeValid) {
                assertFalse(result.errors.any { it.type == ValidationErrorType.INVALID_MERCHANT_NAME },
                    "Merchant name '$merchantName' should be valid")
            } else {
                assertTrue(result.errors.any { it.type == ValidationErrorType.INVALID_MERCHANT_NAME },
                    "Merchant name '$merchantName' should be invalid")
            }
        }
    }

    @Test
    fun `should calculate confidence score correctly`() {
        // High confidence receipt
        val completeReceipt = ReceiptData(
            merchantName = "LULU HYPERMARKET",
            date = "15/01/2024",
            total = "125.50",
            subtotal = "110.00",
            tax = "15.50",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Item 1", "50.00"),
                ReceiptItem("Item 2", "60.00")
            )
        )

        val highConfidenceResult = validator.validateReceipt(completeReceipt)
        assertTrue(highConfidenceResult.confidence > 0.8f)

        // Low confidence receipt
        val incompleteReceipt = ReceiptData(
            total = "25.00",
            currency = "SAR"
        )

        val lowConfidenceResult = validator.validateReceipt(incompleteReceipt)
        assertTrue(lowConfidenceResult.confidence < 0.5f)
    }

    @Test
    fun `should validate Saudi-specific receipt patterns`() {
        val saudiReceipt = ReceiptData(
            merchantName = "هايبر بندة", // Arabic merchant name
            date = "١٥/٠١/٢٠٢٤", // Arabic numerals
            total = "١٢٥.٥٠",
            currency = "SAR",
            tax = "18.75" // 15% VAT rate for Saudi Arabia
        )

        val result = validator.validateReceipt(saudiReceipt)

        // Should handle Arabic text and numbers
        assertFalse(result.errors.any { it.type == ValidationErrorType.INVALID_MERCHANT_NAME })
        assertFalse(result.errors.any { it.type == ValidationErrorType.INVALID_DATE_FORMAT })
    }

    @Test
    fun `should validate receipt totals with tolerance`() {
        // Account for rounding differences
        val receiptWithRounding = ReceiptData(
            total = "125.51", // Slight difference due to rounding
            subtotal = "110.00",
            tax = "15.51", // 15% of 110.00 = 16.50, but OCR read as 15.51
            currency = "SAR"
        )

        val result = validator.validateReceipt(receiptWithRounding)

        // Should allow small tolerance for rounding errors
        assertFalse(result.errors.any { it.type == ValidationErrorType.MATH_INCONSISTENCY })
        // But should include a warning
        assertTrue(result.warnings.any { it.contains("rounding") || it.contains("calculation") })
    }

    @Test
    fun `should provide correction suggestions`() {
        val receiptWithErrors = ReceiptData(
            merchantName = "",
            date = "invalid-date",
            total = "abc",
            currency = "INVALID",
            items = emptyList()
        )

        val result = validator.validateReceipt(receiptWithErrors)

        assertFalse(result.isValid)
        assertTrue(result.suggestions.isNotEmpty())
        assertTrue(result.suggestions.any { it.contains("merchant") })
        assertTrue(result.suggestions.any { it.contains("date") })
        assertTrue(result.suggestions.any { it.contains("amount") })
    }

    @Test
    fun `should handle edge cases gracefully`() {
        val edgeCases = listOf(
            ReceiptData(), // Empty receipt
            ReceiptData(total = "0.00", currency = "SAR"), // Zero total
            ReceiptData(total = "999999.99", currency = "SAR") // Very large amount
        )

        edgeCases.forEach { receipt ->
            val result = validator.validateReceipt(receipt)
            // Should not crash and provide some validation result
            assertNotNull(result)
            assertTrue(result.confidence >= 0.0f && result.confidence <= 1.0f)
        }
    }
}