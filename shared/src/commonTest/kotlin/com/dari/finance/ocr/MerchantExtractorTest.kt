package com.dari.finance.ocr

import kotlin.test.*

class MerchantExtractorTest {

    private val merchantExtractor = MerchantExtractor()

    @Test
    fun `should extract known Saudi merchants`() {
        val testCases = listOf(
            "LULU HYPERMARKET\nKing Fahd Road" to "LULU HYPERMARKET",
            "Carrefour Hypermarket\nRiyadh Mall" to "CARREFOUR HYPERMARKET", 
            "Extra Stores\nLocation: Jeddah" to "EXTRA STORES",
            "Al Danube Supermarket\nBranch: Dammam" to "DANUBE SUPERMARKET",
            "Panda Retail Company\nStore #123" to "PANDA",
            "Tamimi Markets\nKhobar Branch" to "TAMIMI MARKETS",
            "BinDawood Supermarkets\nMakkah" to "BINDAWOOD SUPERMARKETS"
        )

        testCases.forEach { (input, expected) ->
            val result = merchantExtractor.extractMerchantName(input)
            assertTrue(result!!.contains(expected.split(" ")[0], ignoreCase = true), 
                "Failed for input: $input, expected: $expected, got: $result")
        }
    }

    @Test
    fun `should extract Arabic merchant names`() {
        val testCases = listOf(
            "لولو هايبرماركت\nطريق الملك فهد" to "LULU HYPERMARKET",
            "كارفور\nالرياض مول" to "CARREFOUR",
            "اكسترا\nجدة" to "EXTRA",
            "الدانوب\nفرع الدمام" to "DANUBE",
            "بندة\nمتجر رقم ١٢٣" to "PANDA",
            "التميمي\nفرع الخبر" to "TAMIMI MARKETS",
            "بن داود\nمكة المكرمة" to "BINDAWOOD"
        )

        testCases.forEach { (input, expected) ->
            val result = merchantExtractor.extractMerchantName(input)
            assertNotNull(result, "Failed to extract merchant from: $input")
            // Should normalize to English equivalent
        }
    }

    @Test
    fun `should extract restaurant chains`() {
        val testCases = listOf(
            "McDonald's\nKing Fahd Road\nRiyadh" to "MCDONALD'S",
            "KFC Restaurant\nOlaya Street" to "KFC",
            "SUBWAY\nSandwich Shop" to "SUBWAY",
            "Starbucks Coffee\nTahlia Street" to "STARBUCKS",
            "Hardee's\nFast Food Restaurant" to "HARDEE'S",
            "Burger King\nPrince Sultan Road" to "BURGER KING",
            "Pizza Hut\nDelivery Service" to "PIZZA HUT"
        )

        testCases.forEach { (input, expected) ->
            val result = merchantExtractor.extractMerchantName(input)
            assertTrue(result!!.contains(expected.split(" ")[0], ignoreCase = true),
                "Failed for restaurant: $input, expected: $expected, got: $result")
        }
    }

    @Test
    fun `should extract merchant with confidence scoring`() {
        val testCases = listOf(
            // High confidence - well-known brand at start of text
            "LULU HYPERMARKET\nKing Fahd Road\nRiyadh" to 0.9f,
            // Medium confidence - partial match
            "Lulu Store\nShopping Center" to 0.7f,
            // Low confidence - generic name
            "Corner Store\nLocal Shop" to 0.3f,
            // Very low confidence - unclear text
            "Store Name\nSome Location" to 0.2f
        )

        testCases.forEach { (input, expectedMinConfidence) ->
            val result = merchantExtractor.extractMerchantWithConfidence(input)
            assertNotNull(result, "Should extract some merchant from: $input")
            assertTrue(result.confidence >= expectedMinConfidence,
                "Confidence too low for: $input (got ${result.confidence}, expected >= $expectedMinConfidence)")
        }
    }

    @Test
    fun `should extract merchant location information`() {
        val receiptText = """
            LULU HYPERMARKET
            King Fahd Road, Al Olaya
            Riyadh 12211
            Saudi Arabia
            Tel: +966 11 123 4567
        """.trimIndent()

        val merchantInfo = merchantExtractor.extractMerchantInfo(receiptText)
        
        assertEquals("LULU HYPERMARKET", merchantInfo.name)
        assertTrue(merchantInfo.location!!.contains("Riyadh"))
        assertTrue(merchantInfo.address!!.contains("King Fahd Road"))
        assertTrue(merchantInfo.phone!!.contains("+966"))
    }

    @Test
    fun `should categorize merchants by type`() {
        val testCases = listOf(
            "LULU HYPERMARKET" to MerchantCategory.GROCERY,
            "McDonald's" to MerchantCategory.RESTAURANT,
            "Starbucks Coffee" to MerchantCategory.RESTAURANT,
            "SACO Store" to MerchantCategory.RETAIL,
            "Jarir Bookstore" to MerchantCategory.RETAIL,
            "Shell Station" to MerchantCategory.GAS_STATION,
            "Saudi Telecom" to MerchantCategory.TELECOM,
            "SABB Bank" to MerchantCategory.FINANCIAL,
            "Dr. Ahmad Clinic" to MerchantCategory.HEALTHCARE
        )

        testCases.forEach { (merchantName, expectedCategory) ->
            val category = merchantExtractor.categorizeMerchant(merchantName)
            assertEquals(expectedCategory, category, "Wrong category for: $merchantName")
        }
    }

    @Test
    fun `should handle merchant name variations`() {
        val variations = listOf(
            "LULU HYPERMARKET",
            "Lulu Hypermarket",
            "lulu hypermarket", 
            "LULU",
            "LuLu",
            "لولو هايبرماركت",
            "لولو"
        )

        variations.forEach { variation ->
            val result = merchantExtractor.normalizeMerchantName(variation)
            assertTrue(result.contains("LULU"), "Failed to normalize: $variation -> $result")
        }
    }

    @Test
    fun `should extract merchant from different receipt positions`() {
        val receiptFormats = listOf(
            // Header position
            """
                CARREFOUR HYPERMARKET
                Invoice #12345
                Date: 15/01/2024
                Total: 125.50 SAR
            """.trimIndent(),
            
            // Middle position with address
            """
                Invoice #12345
                EXTRA STORES
                Riyadh Park Mall
                Total: 89.75 SAR
            """.trimIndent(),
            
            // Mixed with other text
            """
                Receipt
                Thank you for shopping at
                DANUBE SUPERMARKET
                Your savings: 15.50 SAR
            """.trimIndent()
        )

        receiptFormats.forEach { receipt ->
            val result = merchantExtractor.extractMerchantName(receipt)
            assertNotNull(result, "Failed to extract merchant from receipt format")
            assertTrue(result.length > 3, "Merchant name too short: $result")
        }
    }

    @Test
    fun `should detect franchise and branch information`() {
        val receiptText = """
            KFC Restaurant
            Branch: Riyadh - Olaya
            Store #456
            Franchise: Alshaya Group
        """.trimIndent()

        val merchantInfo = merchantExtractor.extractMerchantInfo(receiptText)
        
        assertEquals("KFC", merchantInfo.name)
        assertTrue(merchantInfo.branch!!.contains("Olaya"))
        assertTrue(merchantInfo.storeNumber == "456")
    }

    @Test
    fun `should handle multi-language merchant names`() {
        val mixedLanguageText = """
            LULU لولو HYPERMARKET
            King Fahd Road طريق الملك فهد
            Riyadh الرياض
            Total المجموع: 75.50 SAR ريال
        """.trimIndent()

        val result = merchantExtractor.extractMerchantName(mixedLanguageText)
        assertEquals("LULU HYPERMARKET", result)
    }

    @Test
    fun `should extract merchant tax ID and registration info`() {
        val receiptText = """
            TAMIMI MARKETS
            C.R. No: 1010123456
            Tax ID: 300012345678903
            VAT Reg: 123456789012345
        """.trimIndent()

        val merchantInfo = merchantExtractor.extractMerchantInfo(receiptText)
        
        assertNotNull(merchantInfo.taxId)
        assertNotNull(merchantInfo.registrationNumber)
        assertTrue(merchantInfo.taxId!!.startsWith("300"))
    }

    @Test
    fun `should handle edge cases gracefully`() {
        val edgeCases = listOf(
            "",
            "123456789",
            "!@#$%^&*()",
            "Short",
            "A very long merchant name that probably doesn't exist in real life but we should handle gracefully",
            "Normal Store Name", // Generic name
            "مجهول", // Unknown in Arabic
            "Store متجر Shop"
        )

        edgeCases.forEach { input ->
            val result = merchantExtractor.extractMerchantName(input)
            // Should not crash and return reasonable result
            if (result != null) {
                assertTrue(result.length >= 2, "Result too short for: $input")
            }
        }
    }

    @Test
    fun `should prioritize merchant names over other text`() {
        val noisyReceiptText = """
            Receipt #12345
            Date: 15/01/2024 Time: 14:30
            Cashier: Ahmed
            CARREFOUR HYPERMARKET
            Location: Riyadh Park
            Customer: Valued Customer
            Thank you for shopping
            Items purchased: 5
            Total saved: 25.50 SAR
        """.trimIndent()

        val result = merchantExtractor.extractMerchantName(noisyReceiptText)
        assertEquals("CARREFOUR HYPERMARKET", result)
    }

    @Test
    fun `should extract online merchant information`() {
        val onlineReceiptText = """
            noon.com
            Online Order #ABC123456
            Delivery Address: Riyadh
            Customer Service: 800-NOON
        """.trimIndent()

        val merchantInfo = merchantExtractor.extractMerchantInfo(onlineReceiptText)
        
        assertEquals("NOON.COM", merchantInfo.name)
        assertTrue(merchantInfo.isOnline)
        assertNotNull(merchantInfo.orderNumber)
    }

    @Test
    fun `should match merchant aliases and variations`() {
        val aliases = mapOf(
            "Carrefour" to "CARREFOUR HYPERMARKET",
            "Extra" to "EXTRA STORES", 
            "Lulu" to "LULU HYPERMARKET",
            "McDonald's" to "MCDONALD'S",
            "KFC" to "KFC",
            "بندة" to "PANDA",
            "لولو" to "LULU HYPERMARKET"
        )

        aliases.forEach { (alias, expectedFull) ->
            val normalized = merchantExtractor.normalizeMerchantName(alias)
            assertTrue(normalized.contains(expectedFull.split(" ")[0]),
                "Failed to normalize alias: $alias -> $normalized")
        }
    }
}