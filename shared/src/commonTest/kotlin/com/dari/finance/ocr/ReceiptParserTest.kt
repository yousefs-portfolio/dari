package com.dari.finance.ocr

import kotlin.test.*

class ReceiptParserTest {

    private val receiptParser = ReceiptParser()

    @Test
    fun `should extract receipt data from text`() {
        val sampleReceiptText = """
            LULU HYPERMARKET
            King Fahd Road, Riyadh
            Tax ID: 300001234567890
            
            Date: 15/01/2024    Time: 14:30
            
            Apples                 12.50
            Milk 1L                8.75
            Bread                  3.25
            
            Subtotal:             24.50
            VAT (15%):             3.68
            TOTAL:                28.18 SAR
            
            Payment: CASH
            Change: 1.82
        """.trimIndent()

        val result = receiptParser.parseReceiptText(sampleReceiptText)

        assertEquals("LULU HYPERMARKET", result.merchantName)
        assertEquals("15/01/2024", result.date)
        assertEquals("28.18", result.total)
        assertEquals("SAR", result.currency)
        assertEquals("3.68", result.tax)
        assertEquals("24.50", result.subtotal)
        assertEquals(3, result.items.size)
    }

    @Test
    fun `should extract items with prices and quantities`() {
        val receiptText = """
            Store Name
            Item 1 x2               20.00
            Item 2                  15.50
            Item 3 x3               45.00
            Total:                  80.50
        """.trimIndent()

        val result = receiptParser.parseReceiptText(receiptText)

        assertEquals(3, result.items.size)
        
        val item1 = result.items.find { it.name.contains("Item 1") }
        assertNotNull(item1)
        assertEquals(2, item1.quantity)
        assertEquals("20.00", item1.price)
        
        val item2 = result.items.find { it.name.contains("Item 2") }
        assertNotNull(item2)
        assertEquals(1, item2.quantity)
        assertEquals("15.50", item2.price)
    }

    @Test
    fun `should handle Arabic receipt text`() {
        val arabicReceiptText = """
            هايبر بندة
            طريق الملك فهد، الرياض
            
            التاريخ: ١٥/٠١/٢٠٢٤
            
            تفاح                    ١٢.٥٠ ريال
            حليب                    ٨.٧٥ ريال
            خبز                     ٣.٢٥ ريال
            
            المجموع الفرعي:         ٢٤.٥٠ ريال
            ضريبة القيمة المضافة:    ٣.٦٨ ريال
            المجموع الكلي:          ٢٨.١٨ ريال
        """.trimIndent()

        val result = receiptParser.parseReceiptText(arabicReceiptText)

        assertEquals("هايبر بندة", result.merchantName)
        assertNotNull(result.date)
        assertTrue(result.items.isNotEmpty())
        assertEquals("SAR", result.currency) // Default currency for Saudi receipts
    }

    @Test
    fun `should detect common merchant names`() {
        val merchantVariations = listOf(
            "CARREFOUR HYPERMARKET" to "CARREFOUR",
            "LULU Hypermarket" to "LULU",
            "Extra Stores" to "EXTRA",
            "Danube" to "DANUBE",
            "Panda" to "PANDA",
            "McDonald's" to "MCDONALD'S",
            "KFC Restaurant" to "KFC"
        )

        merchantVariations.forEach { (input, expected) ->
            val receiptText = """
                $input
                Date: 15/01/2024
                Total: 50.00 SAR
            """.trimIndent()

            val result = receiptParser.parseReceiptText(receiptText)
            assertTrue(result.merchantName!!.contains(expected, ignoreCase = true))
        }
    }

    @Test
    fun `should extract different date formats`() {
        val dateFormats = listOf(
            "15/01/2024" to "15/01/2024",
            "2024-01-15" to "2024-01-15",
            "Jan 15, 2024" to "Jan 15, 2024",
            "15 Jan 2024" to "15 Jan 2024",
            "١٥/٠١/٢٠٢٤" to "15/01/2024"
        )

        dateFormats.forEach { (input, expected) ->
            val receiptText = """
                Test Store
                Date: $input
                Total: 25.00
            """.trimIndent()

            val result = receiptParser.parseReceiptText(receiptText)
            assertNotNull(result.date)
            assertTrue(result.date!!.isNotEmpty())
        }
    }

    @Test
    fun `should extract amounts with different currency formats`() {
        val amountFormats = listOf(
            "25.99 SAR",
            "SAR 25.99",
            "25.99 ريال",
            "ريال 25.99",
            "25,99 SAR",
            "$25.99"
        )

        amountFormats.forEach { amount ->
            val receiptText = """
                Test Store
                Total: $amount
            """.trimIndent()

            val result = receiptParser.parseReceiptText(receiptText)
            assertNotNull(result.total)
            assertTrue(result.total!!.contains("25"))
        }
    }

    @Test
    fun `should calculate parsing confidence`() {
        val completeReceipt = ReceiptData(
            merchantName = "LULU HYPERMARKET",
            date = "15/01/2024",
            total = "28.18",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Apples", "12.50"),
                ReceiptItem("Milk", "8.75")
            ),
            tax = "3.68",
            subtotal = "24.50"
        )

        val partialReceipt = ReceiptData(
            total = "28.18",
            currency = "SAR"
        )

        val emptyReceipt = ReceiptData()

        val completeConfidence = receiptParser.getParsingConfidence(completeReceipt)
        val partialConfidence = receiptParser.getParsingConfidence(partialReceipt)
        val emptyConfidence = receiptParser.getParsingConfidence(emptyReceipt)

        assertTrue(completeConfidence > partialConfidence)
        assertTrue(partialConfidence > emptyConfidence)
        assertTrue(completeConfidence >= 0.8f)
        assertTrue(emptyConfidence <= 0.2f)
    }

    @Test
    fun `should handle malformed receipt text`() {
        val malformedTexts = listOf(
            "",
            "Random text without structure",
            "123456789",
            "!@#$%^&*()",
            "No receipt data here at all"
        )

        malformedTexts.forEach { text ->
            val result = receiptParser.parseReceiptText(text)
            val confidence = receiptParser.getParsingConfidence(result)
            
            // Should handle gracefully without crashing
            assertNotNull(result)
            assertTrue(confidence <= 0.5f) // Low confidence for malformed text
        }
    }

    @Test
    fun `should extract line items with categories`() {
        val receiptText = """
            TAMIMI SUPERMARKET
            
            FRUITS & VEGETABLES
            Apples                 12.50
            Bananas                8.00
            
            DAIRY
            Milk                   7.50
            Cheese                15.00
            
            Total:                43.00 SAR
        """.trimIndent()

        val result = receiptParser.parseReceiptText(receiptText)

        assertTrue(result.items.size >= 4)
        
        // Check if categories are detected (optional feature)
        val apples = result.items.find { it.name.contains("Apples") }
        val milk = result.items.find { it.name.contains("Milk") }
        
        assertNotNull(apples)
        assertNotNull(milk)
    }

    @Test
    fun `should handle receipts without items list`() {
        val simpleReceiptText = """
            QuickMart
            Date: 15/01/2024
            Total: 45.99 SAR
            Thank you!
        """.trimIndent()

        val result = receiptParser.parseReceiptText(simpleReceiptText)

        assertEquals("QuickMart", result.merchantName)
        assertEquals("15/01/2024", result.date)
        assertEquals("45.99", result.total)
        assertTrue(result.items.isEmpty())
    }
}