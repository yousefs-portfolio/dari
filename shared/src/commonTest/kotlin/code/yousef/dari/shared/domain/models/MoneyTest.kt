package code.yousef.dari.shared.domain.models

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Test for Money Value Object - TDD approach
 * Tests currency handling, arithmetic operations, and validation
 */
class MoneyTest {

    @Test
    fun `should create Money with valid amount and currency`() {
        // When
        val money = Money("100.50", "SAR")

        // Then
        assertEquals("100.50", money.amount)
        assertEquals("SAR", money.currency)
    }

    @Test
    fun `should create Money with zero amount`() {
        // When
        val money = Money("0.00", "SAR")

        // Then
        assertEquals("0.00", money.amount)
        assertTrue(money.isZero(), "Money with 0.00 amount should be zero")
    }

    @Test
    fun `should fail to create Money with invalid currency`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Money("100.00", "INVALID")
        }
    }

    @Test
    fun `should fail to create Money with invalid amount format`() {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            Money("invalid-amount", "SAR")
        }
    }

    @Test
    fun `should add two Money values with same currency`() {
        // Given
        val money1 = Money("100.50", "SAR")
        val money2 = Money("50.25", "SAR")

        // When
        val result = money1 + money2

        // Then
        assertEquals(Money("150.75", "SAR"), result)
    }

    @Test
    fun `should subtract two Money values with same currency`() {
        // Given
        val money1 = Money("100.50", "SAR")
        val money2 = Money("50.25", "SAR")

        // When
        val result = money1 - money2

        // Then
        assertEquals(Money("50.25", "SAR"), result)
    }

    @Test
    fun `should fail to add Money with different currencies`() {
        // Given
        val sarMoney = Money("100.00", "SAR")
        val usdMoney = Money("100.00", "USD")

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            sarMoney + usdMoney
        }
    }

    @Test
    fun `should compare Money values correctly`() {
        // Given
        val money1 = Money("100.00", "SAR")
        val money2 = Money("50.00", "SAR")
        val money3 = Money("100.00", "SAR")

        // When & Then
        assertTrue(money1 > money2, "100 SAR should be greater than 50 SAR")
        assertTrue(money2 < money1, "50 SAR should be less than 100 SAR")
        assertEquals(money1, money3, "100 SAR should equal 100 SAR")
    }

    @Test
    fun `should detect negative amounts`() {
        // Given
        val negativeMoney = Money("-100.00", "SAR")
        val positiveMoney = Money("100.00", "SAR")

        // When & Then
        assertTrue(negativeMoney.isNegative(), "Negative amount should be detected")
        assertFalse(positiveMoney.isNegative(), "Positive amount should not be negative")
    }

    @Test
    fun `should detect positive amounts`() {
        // Given
        val positiveMoney = Money("100.00", "SAR")
        val zeroMoney = Money("0.00", "SAR")

        // When & Then
        assertTrue(positiveMoney.isPositive(), "Positive amount should be detected")
        assertFalse(zeroMoney.isPositive(), "Zero amount should not be positive")
    }

    @Test
    fun `should format Money for display`() {
        // Given
        val money = Money("1234.56", "SAR")

        // When
        val formatted = money.format()

        // Then
        assertEquals("1,234.56 SAR", formatted)
    }

    @Test
    fun `should format Money in Arabic`() {
        // Given
        val money = Money("1234.56", "SAR")

        // When
        val formatted = money.formatArabic()

        // Then
        assertEquals("١٬٢٣٤٫٥٦ ر.س", formatted)
    }

    @Test
    fun `should handle currency conversion concept`() {
        // Given
        val sarMoney = Money("100.00", "SAR")
        val exchangeRate = "0.267" // 1 SAR = 0.267 USD

        // When
        val convertedMoney = sarMoney.convertTo("USD", exchangeRate)

        // Then
        assertEquals("USD", convertedMoney.currency)
        assertEquals("26.70", convertedMoney.amount)
    }

    @Test
    fun `should support common Saudi currencies`() {
        // Given
        val supportedCurrencies = listOf("SAR", "USD", "EUR", "GBP")

        supportedCurrencies.forEach { currency ->
            // When & Then (should not throw exception)
            assertNotNull(Money("100.00", currency))
        }
    }

    @Test
    fun `should multiply Money by factor`() {
        // Given
        val money = Money("100.50", "SAR")

        // When
        val result = money * 2

        // Then
        assertEquals(Money("201.00", "SAR"), result)
    }

    @Test
    fun `should divide Money by factor`() {
        // Given
        val money = Money("201.00", "SAR")

        // When
        val result = money / 2

        // Then
        assertEquals(Money("100.50", "SAR"), result)
    }

    @Test
    fun `should handle percentage calculations`() {
        // Given
        val money = Money("1000.00", "SAR")

        // When
        val percentage = money.percentage(25) // 25% of 1000

        // Then
        assertEquals(Money("250.00", "SAR"), percentage)
    }

    @Test
    fun `should calculate absolute value`() {
        // Given
        val negativeMoney = Money("-100.50", "SAR")

        // When
        val absolute = negativeMoney.abs()

        // Then
        assertEquals(Money("100.50", "SAR"), absolute)
    }

    @Test
    fun `should handle different decimal precisions correctly`() {
        // Given
        val money1 = Money("100.5", "SAR")   // One decimal
        val money2 = Money("100.50", "SAR")  // Two decimals
        val money3 = Money("100", "SAR")     // No decimals

        // When & Then
        assertEquals("100.50", money1.amount, "Should normalize to 2 decimal places")
        assertEquals("100.50", money2.amount, "Should keep 2 decimal places")
        assertEquals("100.00", money3.amount, "Should add decimal places")
    }
}