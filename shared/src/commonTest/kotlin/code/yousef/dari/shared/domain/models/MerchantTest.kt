package code.yousef.dari.shared.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MerchantTest {

    @Test
    fun `should create merchant with valid data`() {
        // Given
        val merchantId = "merchant_123"
        val name = "Al Danube Supermarket"
        val category = "Groceries"
        val vatNumber = "300012345600003"
        val location = MerchantLocation(
            address = "King Fahd Road, Riyadh",
            city = "Riyadh",
            country = "Saudi Arabia",
            latitude = 24.7136,
            longitude = 46.6753
        )

        // When
        val merchant = Merchant(
            id = merchantId,
            name = name,
            category = category,
            vatNumber = vatNumber,
            location = location,
            logoUrl = "https://example.com/logo.png",
            website = "https://aldanube.com",
            phone = "+966112345678"
        )

        // Then
        assertEquals(merchantId, merchant.id)
        assertEquals(name, merchant.name)
        assertEquals(category, merchant.category)
        assertEquals(vatNumber, merchant.vatNumber)
        assertEquals(location, merchant.location)
        assertEquals("https://example.com/logo.png", merchant.logoUrl)
        assertEquals("https://aldanube.com", merchant.website)
        assertEquals("+966112345678", merchant.phone)
        assertTrue(merchant.isActive)
    }

    @Test
    fun `should create merchant with minimal required data`() {
        // Given
        val merchantId = "merchant_123"
        val name = "Local Shop"
        val category = "Retail"

        // When
        val merchant = Merchant(
            id = merchantId,
            name = name,
            category = category
        )

        // Then
        assertEquals(merchantId, merchant.id)
        assertEquals(name, merchant.name)
        assertEquals(category, merchant.category)
        assertEquals("", merchant.vatNumber)
        assertNotNull(merchant.location)
        assertEquals("", merchant.location.address)
        assertEquals("", merchant.logoUrl)
        assertEquals("", merchant.website)
        assertEquals("", merchant.phone)
        assertTrue(merchant.isActive)
    }

    @Test
    fun `should validate Saudi VAT number format`() {
        // Given
        val validVatNumber = "300012345600003"
        val invalidVatNumber = "123456789"

        // When
        val validMerchant = Merchant(
            id = "merchant_123",
            name = "Valid Merchant",
            category = "Retail",
            vatNumber = validVatNumber
        )

        val invalidMerchant = Merchant(
            id = "merchant_456",
            name = "Invalid Merchant",
            category = "Retail",
            vatNumber = invalidVatNumber
        )

        // Then
        assertTrue(validMerchant.isSaudiVatNumber())
        assertFalse(invalidMerchant.isSaudiVatNumber())
    }

    @Test
    fun `should support Arabic merchant names`() {
        // Given
        val arabicName = "مطعم النخيل"
        val category = "مطاعم" // Restaurants in Arabic

        // When
        val merchant = Merchant(
            id = "merchant_123",
            name = arabicName,
            category = category
        )

        // Then
        assertEquals(arabicName, merchant.name)
        assertEquals(category, merchant.category)
        assertTrue(merchant.hasArabicName())
    }

    @Test
    fun `should validate Saudi phone number format`() {
        // Given
        val validSaudiPhone = "+966501234567"
        val invalidPhone = "123456"

        // When
        val merchant = Merchant(
            id = "merchant_123",
            name = "Test Merchant",
            category = "Retail",
            phone = validSaudiPhone
        )

        val merchantWithInvalidPhone = Merchant(
            id = "merchant_456",
            name = "Test Merchant 2",
            category = "Retail",
            phone = invalidPhone
        )

        // Then
        assertTrue(merchant.isSaudiPhoneNumber())
        assertFalse(merchantWithInvalidPhone.isSaudiPhoneNumber())
    }

    @Test
    fun `should calculate distance from location`() {
        // Given
        val riyadhMerchant = Merchant(
            id = "merchant_123",
            name = "Riyadh Store",
            category = "Retail",
            location = MerchantLocation(
                address = "King Fahd Road, Riyadh",
                city = "Riyadh",
                country = "Saudi Arabia",
                latitude = 24.7136,
                longitude = 46.6753
            )
        )

        val userLocation = Location(24.7500, 46.7000) // Slightly different location

        // When
        val distance = riyadhMerchant.calculateDistanceFrom(userLocation)

        // Then
        assertTrue(distance > 0.0)
        assertTrue(distance < 10.0) // Should be less than 10km within Riyadh
    }

    @Test
    fun `should categorize merchant types correctly`() {
        // Given
        val groceryMerchant = Merchant(
            id = "grocery_1",
            name = "Supermarket",
            category = "Groceries"
        )

        val restaurantMerchant = Merchant(
            id = "restaurant_1",
            name = "Restaurant",
            category = "Food & Dining"
        )

        val gasStationMerchant = Merchant(
            id = "gas_1",
            name = "Petrol Station",
            category = "Fuel"
        )

        // Then
        assertEquals(MerchantType.GROCERY, groceryMerchant.getMerchantType())
        assertEquals(MerchantType.RESTAURANT, restaurantMerchant.getMerchantType())
        assertEquals(MerchantType.GAS_STATION, gasStationMerchant.getMerchantType())
    }

    @Test
    fun `should track transaction statistics`() {
        // Given
        val merchant = Merchant(
            id = "merchant_123",
            name = "Test Merchant",
            category = "Retail"
        )

        val stats = MerchantStats(
            totalTransactions = 25,
            totalAmount = Money.sar(125000), // 1,250 SAR
            averageAmount = Money.sar(5000), // 50 SAR
            lastTransactionDate = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            favoriteCategory = "Groceries"
        )

        // When
        val merchantWithStats = merchant.copy(stats = stats)

        // Then
        assertEquals(25, merchantWithStats.stats?.totalTransactions)
        assertEquals(Money.sar(125000), merchantWithStats.stats?.totalAmount)
        assertEquals(Money.sar(5000), merchantWithStats.stats?.averageAmount)
        assertEquals("Groceries", merchantWithStats.stats?.favoriteCategory)
    }

    @Test
    fun `should fail with invalid merchant id`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Merchant(
                id = "",
                name = "Test Merchant",
                category = "Retail"
            )
        }
    }

    @Test
    fun `should fail with empty merchant name`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Merchant(
                id = "merchant_123",
                name = "",
                category = "Retail"
            )
        }
    }

    @Test
    fun `should fail with empty category`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Merchant(
                id = "merchant_123",
                name = "Test Merchant",
                category = ""
            )
        }
    }

    @Test
    fun `should support merchant chain grouping`() {
        // Given
        val danube1 = Merchant(
            id = "danube_riyadh",
            name = "Al Danube Supermarket - Riyadh",
            category = "Groceries",
            chainName = "Al Danube",
            location = MerchantLocation(city = "Riyadh", country = "Saudi Arabia")
        )

        val danube2 = Merchant(
            id = "danube_jeddah",
            name = "Al Danube Supermarket - Jeddah", 
            category = "Groceries",
            chainName = "Al Danube",
            location = MerchantLocation(city = "Jeddah", country = "Saudi Arabia")
        )

        // Then
        assertEquals("Al Danube", danube1.chainName)
        assertEquals("Al Danube", danube2.chainName)
        assertTrue(danube1.isSameChain(danube2))
        assertTrue(danube1.isChainStore())
    }

    @Test
    fun `should support halal certification tracking`() {
        // Given
        val halalRestaurant = Merchant(
            id = "restaurant_123",
            name = "Halal Kitchen",
            category = "Food & Dining",
            isHalalCertified = true,
            certificationBody = "SFDA" // Saudi Food and Drug Authority
        )

        // Then
        assertTrue(halalRestaurant.isHalalCertified)
        assertEquals("SFDA", halalRestaurant.certificationBody)
        assertTrue(halalRestaurant.isCompliantWithIslamicPrinciples())
    }
}