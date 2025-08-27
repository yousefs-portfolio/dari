package com.dari.finance.ocr

import kotlin.test.*

class LineItemExtractorTest {

    private val lineItemExtractor = LineItemExtractor()

    @Test
    fun `should extract basic line items with prices`() {
        val receiptText = """
            LULU HYPERMARKET
            
            Apples Red 1KG         12.50
            Milk Fresh 1L           8.75
            Bread White Loaf        3.25
            
            Subtotal               24.50
            Total                  24.50 SAR
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertEquals(3, items.size)
        
        val apples = items.find { it.description.contains("Apples") }
        assertNotNull(apples)
        assertEquals("12.50", apples.price)
        assertEquals(1, apples.quantity)
        
        val milk = items.find { it.description.contains("Milk") }
        assertNotNull(milk)
        assertEquals("8.75", milk.price)
        
        val bread = items.find { it.description.contains("Bread") }
        assertNotNull(bread)
        assertEquals("3.25", bread.price)
    }

    @Test
    fun `should extract items with quantities`() {
        val receiptText = """
            Store Receipt
            
            Item A x2              20.00
            Item B x3              45.00
            Single Item            15.00
            Item C 5x              50.00
            
            Total                 130.00
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertEquals(4, items.size)
        
        val itemA = items.find { it.description.contains("Item A") }
        assertNotNull(itemA)
        assertEquals(2, itemA.quantity)
        assertEquals("20.00", itemA.price)
        
        val itemB = items.find { it.description.contains("Item B") }
        assertNotNull(itemB)
        assertEquals(3, itemB.quantity)
        
        val itemC = items.find { it.description.contains("Item C") }
        assertNotNull(itemC)
        assertEquals(5, itemC.quantity)
        
        val singleItem = items.find { it.description.contains("Single Item") }
        assertNotNull(singleItem)
        assertEquals(1, singleItem.quantity)
    }

    @Test
    fun `should extract Arabic line items`() {
        val receiptText = """
            هايبر بندة
            
            تفاح أحمر ١ كيلو        ١٢.٥٠ ريال
            حليب طازج ١ لتر         ٨.٧٥ ريال
            خبز أبيض رغيف           ٣.٢٥ ريال
            
            المجموع الفرعي          ٢٤.٥٠ ريال
            المجموع الكلي           ٢٤.٥٠ ريال
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertEquals(3, items.size)
        
        items.forEach { item ->
            assertNotNull(item.description)
            assertTrue(item.description.isNotEmpty())
            assertTrue(item.price.toDoubleOrNull() != null)
        }
    }

    @Test
    fun `should extract items with unit prices and quantities`() {
        val receiptText = """
            TAMIMI MARKETS
            
            Bananas @4.50/kg
            Weight: 1.2kg          5.40
            
            Tomatoes @8.00/kg  
            Weight: 0.8kg          6.40
            
            Chicken @25.00/kg
            Weight: 2.5kg         62.50
            
            Total                 74.30
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertTrue(items.size >= 3)
        
        val bananas = items.find { it.description.contains("Bananas") }
        assertNotNull(bananas)
        assertEquals("5.40", bananas.price)
        assertTrue(bananas.unitPrice?.contains("4.50") == true)
        
        val chicken = items.find { it.description.contains("Chicken") }
        assertNotNull(chicken)
        assertEquals("62.50", chicken.price)
        assertTrue(chicken.weight?.contains("2.5") == true)
    }

    @Test
    fun `should extract items with discounts`() {
        val receiptText = """
            CARREFOUR HYPERMARKET
            
            Orange Juice 1L        15.00
            Discount -10%          -1.50
            Final Price            13.50
            
            Chips Family Size      12.00
            Member Discount        -2.00
            After Discount         10.00
            
            Total                  23.50
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        val orangeJuice = items.find { it.description.contains("Orange Juice") }
        assertNotNull(orangeJuice)
        assertTrue(orangeJuice.discount != null)
        
        val chips = items.find { it.description.contains("Chips") }
        assertNotNull(chips)
        assertTrue(chips.discount != null)
    }

    @Test
    fun `should categorize items automatically`() {
        val receiptText = """
            EXTRA STORES
            
            Apple iPhone Case      89.00
            Fresh Milk 2L          16.50
            White Bread Loaf        4.25
            Chicken Breast 1kg     28.00
            Orange Juice 1L        12.75
            
            Total                 150.50
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        val milk = items.find { it.description.contains("Milk") }
        assertEquals(ItemCategory.DAIRY, milk?.category)
        
        val bread = items.find { it.description.contains("Bread") }
        assertEquals(ItemCategory.BAKERY, bread?.category)
        
        val chicken = items.find { it.description.contains("Chicken") }
        assertEquals(ItemCategory.MEAT, chicken?.category)
        
        val juice = items.find { it.description.contains("Juice") }
        assertEquals(ItemCategory.BEVERAGES, juice?.category)
        
        val phoneCase = items.find { it.description.contains("iPhone") }
        assertEquals(ItemCategory.ELECTRONICS, phoneCase?.category)
    }

    @Test
    fun `should handle complex receipt formats`() {
        val complexReceipt = """
            DANUBE SUPERMARKET
            Store #123 - Riyadh Branch
            Date: 15/01/2024 Time: 14:30
            
            FRUITS & VEGETABLES
            ==================
            1. Red Apples 1kg               12.50
            2. Bananas Bundle               8.75
            3. Tomatoes 500g               6.25
            
            DAIRY PRODUCTS  
            ==============
            4. Fresh Milk 1L               7.50
            5. Yogurt Natural 450g         9.25
            
            BAKERY
            ======
            6. Arabic Bread 5pcs           3.00
            
            HOUSEHOLD
            =========  
            7. Tissue Box 200pcs          15.75
            
            Subtotal:                     63.00
            VAT (15%):                     9.45
            Total Amount:                 72.45 SAR
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(complexReceipt)
        
        assertTrue(items.size >= 7)
        
        // Verify numbered items are extracted correctly
        val apples = items.find { it.description.contains("Apples") }
        assertNotNull(apples)
        assertEquals("12.50", apples.price)
        
        val tissue = items.find { it.description.contains("Tissue") }
        assertNotNull(tissue)
        assertEquals("15.75", tissue.price)
        assertEquals(ItemCategory.HOUSEHOLD, tissue.category)
    }

    @Test
    fun `should extract items with barcodes and SKUs`() {
        val receiptText = """
            LULU HYPERMARKET
            
            Coca Cola 330ml
            SKU: CC330ML001        4.50
            Barcode: 1234567890123
            
            Samsung Galaxy Case
            SKU: SGC-BLK-001      89.99
            Barcode: 9876543210987
            
            Total                  94.49
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertEquals(2, items.size)
        
        val cocaCola = items.find { it.description.contains("Coca Cola") }
        assertNotNull(cocaCola)
        assertEquals("CC330ML001", cocaCola.sku)
        assertEquals("1234567890123", cocaCola.barcode)
        
        val samsungCase = items.find { it.description.contains("Samsung") }
        assertNotNull(samsungCase)
        assertEquals("SGC-BLK-001", samsungCase.sku)
        assertEquals("9876543210987", samsungCase.barcode)
    }

    @Test
    fun `should handle tax-inclusive and tax-exclusive items`() {
        val receiptText = """
            SACO STORE
            
            Basic Food Items (0% VAT)
            Milk 1L                    8.00
            Bread Loaf                 3.50
            
            Other Items (15% VAT)
            Shampoo 400ml             25.00
            Phone Charger             45.00
            
            Subtotal (0% VAT):        11.50
            Subtotal (15% VAT):       70.00
            VAT Amount:               10.50
            Total:                    92.00
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertTrue(items.size >= 4)
        
        val milk = items.find { it.description.contains("Milk") }
        assertEquals(0.0f, milk?.taxRate)
        
        val shampoo = items.find { it.description.contains("Shampoo") }
        assertEquals(15.0f, shampoo?.taxRate)
    }

    @Test
    fun `should extract return and exchange items`() {
        val receiptText = """
            TAMIMI MARKETS
            RETURN RECEIPT
            
            Original Items:
            T-shirt Large Blue        89.00
            Jeans 32W 34L           156.00
            
            Returned Items:
            T-shirt Large Blue       -89.00
            
            Exchange:
            T-shirt Large Red         89.00
            
            Net Amount:              156.00
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        val returnedItem = items.find { it.description.contains("Blue") && it.price.startsWith("-") }
        assertNotNull(returnedItem)
        assertTrue(returnedItem.isReturn)
        
        val exchangeItem = items.find { it.description.contains("Red") }
        assertNotNull(exchangeItem)
        assertFalse(exchangeItem.isReturn)
    }

    @Test
    fun `should handle multi-line item descriptions`() {
        val receiptText = """
            JARIR BOOKSTORE
            
            Microsoft Office 365
            Personal Subscription
            1 Year License            399.00
            
            HP Laptop Bag
            Water Resistant
            15.6 inch Compatible      125.00
            
            Canon Ink Cartridge
            Black PG-245XL
            High Yield               89.50
            
            Total                    613.50
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        assertEquals(3, items.size)
        
        val office = items.find { it.description.contains("Office") }
        assertNotNull(office)
        assertTrue(office.description.contains("Personal Subscription"))
        assertEquals("399.00", office.price)
        
        val laptopBag = items.find { it.description.contains("Laptop Bag") }
        assertNotNull(laptopBag)
        assertEquals("125.00", laptopBag.price)
    }

    @Test
    fun `should extract items with multiple pricing tiers`() {
        val receiptText = """
            CARREFOUR HYPERMARKET
            
            Buy 2 Get 1 Free Promotion
            Shampoo 400ml x3          50.00
            (Original: 3x25.00 = 75.00)
            Savings: -25.00
            
            Bulk Discount
            Rice 5kg bags x4         120.00
            Unit price: 32.00 each
            Bulk discount: -8.00
            
            Total Savings:           -33.00
            Final Total:             137.00
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        val shampoo = items.find { it.description.contains("Shampoo") }
        assertNotNull(shampoo)
        assertEquals(3, shampoo.quantity)
        assertTrue(shampoo.promotion?.contains("Buy 2 Get 1 Free") == true)
        
        val rice = items.find { it.description.contains("Rice") }
        assertNotNull(rice)
        assertEquals(4, rice.quantity)
        assertTrue(rice.discount != null)
    }

    @Test
    fun `should handle edge cases gracefully`() {
        val edgeCases = listOf(
            "", // Empty text
            "Total: 25.00", // Only total, no items
            "Store Name\nDate: 15/01/2024", // Header only
            "Item without price", // Missing price
            "12.50", // Price without item
            "Very very very very very very very very very very very very long item name that should be handled properly 99.99"
        )

        edgeCases.forEach { input ->
            val result = lineItemExtractor.extractLineItems(input)
            // Should not crash
            assertNotNull(result)
            assertTrue(result.all { it.description.isNotEmpty() })
            assertTrue(result.all { it.price.toDoubleOrNull() != null })
        }
    }

    @Test
    fun `should extract loyalty points and rewards`() {
        val receiptText = """
            EXTRA STORES
            
            Regular Items:
            Chips Bag Large           12.00
            Soda 2L Bottle             8.50
            
            Rewards Items:
            Free Coffee (150 pts)      0.00
            
            Points Earned: +25
            Points Used: -150
            Points Balance: 875
            
            Total Paid:               20.50
        """.trimIndent()

        val items = lineItemExtractor.extractLineItems(receiptText)
        
        val freeItem = items.find { it.description.contains("Free Coffee") }
        assertNotNull(freeItem)
        assertEquals("0.00", freeItem.price)
        assertTrue(freeItem.isRewardItem)
        assertTrue(freeItem.loyaltyPoints == 150)
    }
}

enum class ItemCategory {
    GROCERY, DAIRY, MEAT, SEAFOOD, FRUITS, VEGETABLES, 
    BAKERY, BEVERAGES, SNACKS, FROZEN, HOUSEHOLD, 
    ELECTRONICS, CLOTHING, HEALTH, BEAUTY, AUTOMOTIVE, 
    BOOKS, TOYS, SPORTS, OTHER
}