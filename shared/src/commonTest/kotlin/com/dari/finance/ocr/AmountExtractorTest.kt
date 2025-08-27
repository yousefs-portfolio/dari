package com.dari.finance.ocr

import kotlin.test.*

class AmountExtractorTest {

    private val amountExtractor = AmountExtractor()

    @Test
    fun `should detect amounts in various formats`() {
        val testCases = listOf(
            "Total: 25.99" to "25.99",
            "Amount: SAR 150.50" to "150.50",
            "Price: $45.00" to "45.00",
            "Cost: 1,250.75" to "1250.75",
            "المجموع: ١٥٠.٥٠ ريال" to "150.50",
            "15.99 SAR" to "15.99",
            "SAR15.99" to "15.99",
            "99.99SR" to "99.99"
        )

        testCases.forEach { (input, expected) ->
            val result = amountExtractor.extractAmount(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `should handle multiple amounts and return the largest`() {
        val text = """
            Subtotal: 25.50
            Tax: 3.83
            Total: 29.33
        """.trimIndent()

        val result = amountExtractor.extractAmount(text)
        assertEquals("29.33", result)
    }

    @Test
    fun `should prefer total over other amounts`() {
        val text = """
            Item 1: 100.00
            Item 2: 200.00
            Total: 50.00
            Change: 10.00
        """.trimIndent()

        val result = amountExtractor.extractAmount(text)
        assertEquals("50.00", result) // Should prefer total even if smaller
    }

    @Test
    fun `should extract amount with currency detection`() {
        val testCases = listOf(
            "25.99 SAR" to AmountWithCurrency("25.99", "SAR"),
            "ريال 150.50" to AmountWithCurrency("150.50", "SAR"),
            "$45.00" to AmountWithCurrency("45.00", "USD"),
            "€33.75" to AmountWithCurrency("33.75", "EUR"),
            "AED 275.00" to AmountWithCurrency("275.00", "AED"),
            "درهم 125.25" to AmountWithCurrency("125.25", "AED")
        )

        testCases.forEach { (input, expected) ->
            val result = amountExtractor.extractAmountWithCurrency(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `should handle Arabic numerals conversion`() {
        val testCases = listOf(
            "المجموع: ١٢٣.٤٥" to "123.45",
            "السعر: ٩٨.٧٦ ريال" to "98.76",
            "الإجمالي: ٥٠٠.٠٠" to "500.00",
            "مبلغ: ١،٢٣٤.٥٦" to "1234.56"
        )

        testCases.forEach { (input, expected) ->
            val result = amountExtractor.extractAmount(input)
            assertEquals(expected, result, "Failed for Arabic input: $input")
        }
    }

    @Test
    fun `should extract amounts with different decimal separators`() {
        val testCases = listOf(
            "25.99" to "25.99",
            "25,99" to "25.99", // European format
            "25٫99" to "25.99", // Arabic decimal separator
            "1,250.75" to "1250.75", // Thousands separator
            "1.250,75" to "1250.75", // European thousands separator
            "١،٢٣٤٫٥٦" to "1234.56" // Arabic with separators
        )

        testCases.forEach { (input, expected) ->
            val result = amountExtractor.normalizeAmount(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `should validate amount format`() {
        val validAmounts = listOf("25.99", "0.50", "1000.00", "99999.99")
        val invalidAmounts = listOf("", "abc", "25.999", "25.", ".99", "-25.99")

        validAmounts.forEach { amount ->
            assertTrue(amountExtractor.isValidAmount(amount), "Should be valid: $amount")
        }

        invalidAmounts.forEach { amount ->
            assertFalse(amountExtractor.isValidAmount(amount), "Should be invalid: $amount")
        }
    }

    @Test
    fun `should extract all amounts from text`() {
        val receiptText = """
            Store Name
            Item 1: 12.50
            Item 2: 8.75
            Item 3: 15.00
            Subtotal: 36.25
            Tax: 5.44
            Total: 41.69
        """.trimIndent()

        val result = amountExtractor.extractAllAmounts(receiptText)
        
        assertTrue(result.contains("12.50"))
        assertTrue(result.contains("8.75"))
        assertTrue(result.contains("15.00"))
        assertTrue(result.contains("36.25"))
        assertTrue(result.contains("5.44"))
        assertTrue(result.contains("41.69"))
        assertEquals(6, result.size)
    }

    @Test
    fun `should handle edge cases gracefully`() {
        val edgeCases = listOf(
            "",
            "No amounts here",
            "Text with numbers but no currency: 123 456",
            "Just a period: .",
            "Multiple periods: 12.34.56",
            "Only currency symbols: $ € ريال"
        )

        edgeCases.forEach { input ->
            val result = amountExtractor.extractAmount(input)
            // Should not crash and return null for invalid cases
            assertTrue(result == null || amountExtractor.isValidAmount(result))
        }
    }

    @Test
    fun `should prioritize amounts by context keywords`() {
        val text = """
            Item cost: 500.00
            Discount: 50.00
            Tax amount: 75.00
            Final total: 525.00
            Change given: 25.00
        """.trimIndent()

        val result = amountExtractor.extractAmountByPriority(text)
        assertEquals("525.00", result) // Should prioritize "total"
    }

    @Test
    fun `should extract amounts with confidence scores`() {
        val testCases = listOf(
            "Total: 25.99 SAR" to 0.9f, // High confidence - has total keyword and currency
            "25.99" to 0.5f, // Medium confidence - just amount
            "Amount maybe: 25" to 0.3f, // Low confidence - no decimal places
            "xyz 25.99 abc" to 0.6f // Medium - has proper decimal format
        )

        testCases.forEach { (input, expectedMinConfidence) ->
            val result = amountExtractor.extractAmountWithConfidence(input)
            assertNotNull(result)
            assertTrue(result.confidence >= expectedMinConfidence, 
                "Confidence too low for: $input (got ${result.confidence})")
        }
    }

    @Test
    fun `should handle amounts in different languages`() {
        val multiLanguageText = """
            Store Name محل
            Total المجموع: 125.50 ريال SAR
            Amount المبلغ: ١٢٥٫٥٠
            Price السعر: 125.50
        """.trimIndent()

        val result = amountExtractor.extractAmount(multiLanguageText)
        assertEquals("125.50", result)
    }

    @Test
    fun `should detect tax amounts specifically`() {
        val text = """
            Subtotal: 100.00
            VAT (15%): 15.00
            Service charge: 5.00
            Total: 120.00
        """.trimIndent()

        val taxAmount = amountExtractor.extractTaxAmount(text)
        assertEquals("15.00", taxAmount)
    }

    @Test
    fun `should extract itemized amounts`() {
        val text = """
            Apple x2          10.00
            Milk 1L           8.50
            Bread             3.25
            Total            21.75
        """.trimIndent()

        val items = amountExtractor.extractItemAmounts(text)
        assertEquals(3, items.size)
        assertTrue(items.any { it.first.contains("Apple") && it.second == "10.00" })
        assertTrue(items.any { it.first.contains("Milk") && it.second == "8.50" })
        assertTrue(items.any { it.first.contains("Bread") && it.second == "3.25" })
    }
}

data class AmountWithCurrency(
    val amount: String,
    val currency: String
)

data class AmountWithConfidence(
    val amount: String,
    val confidence: Float
)