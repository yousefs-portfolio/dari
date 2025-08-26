package code.yousef.dari.shared.domain.models

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for FinancialAccount Domain Model - TDD approach
 * Tests account creation, balance calculations, and account management
 */
class FinancialAccountTest {

    @Test
    fun `should create current account with valid data`() {
        // Given
        val accountId = "acc_12345"
        val accountNumber = "1234567890"
        val bankCode = "alrajhi"
        val accountName = "My Current Account"
        
        // When
        val account = FinancialAccount(
            accountId = accountId,
            accountNumber = accountNumber,
            bankCode = bankCode,
            accountName = accountName,
            accountType = AccountType.CURRENT,
            currency = "SAR",
            isActive = true,
            createdAt = Clock.System.now()
        )

        // Then
        assertEquals(accountId, account.accountId)
        assertEquals(accountNumber, account.accountNumber)
        assertEquals(bankCode, account.bankCode)
        assertEquals(AccountType.CURRENT, account.accountType)
        assertTrue(account.isActive)
    }

    @Test
    fun `should create savings account with different properties`() {
        // When
        val account = FinancialAccount(
            accountId = "acc_savings_123",
            accountNumber = "9876543210",
            bankCode = "snb",
            accountName = "Emergency Savings",
            accountType = AccountType.SAVINGS,
            currency = "SAR",
            isActive = true,
            createdAt = Clock.System.now()
        )

        // Then
        assertEquals(AccountType.SAVINGS, account.accountType)
        assertEquals("Emergency Savings", account.accountName)
    }

    @Test
    fun `should set current balance for account`() {
        // Given
        val account = createTestAccount()
        val balance = Money("5000.00", "SAR")

        // When
        val updatedAccount = account.copy(currentBalance = balance)

        // Then
        assertEquals(balance, updatedAccount.currentBalance)
    }

    @Test
    fun `should set available balance for account`() {
        // Given
        val account = createTestAccount()
        val availableBalance = Money("4500.00", "SAR")

        // When
        val updatedAccount = account.copy(availableBalance = availableBalance)

        // Then
        assertEquals(availableBalance, updatedAccount.availableBalance)
    }

    @Test
    fun `should calculate account summary correctly`() {
        // Given
        val account = createTestAccount().copy(
            currentBalance = Money("5000.00", "SAR"),
            availableBalance = Money("4500.00", "SAR")
        )

        // When
        val summary = account.getAccountSummary()

        // Then
        assertEquals("5000.00", summary.currentBalance.amount)
        assertEquals("4500.00", summary.availableBalance.amount)
        assertEquals("500.00", summary.reservedAmount.amount) // 5000 - 4500
    }

    @Test
    fun `should identify account as low balance when threshold is met`() {
        // Given
        val account = createTestAccount().copy(
            currentBalance = Money("50.00", "SAR"),
            lowBalanceThreshold = Money("100.00", "SAR")
        )

        // When & Then
        assertTrue(account.isLowBalance(), "Account with balance below threshold should be low balance")
    }

    @Test
    fun `should not identify account as low balance when above threshold`() {
        // Given
        val account = createTestAccount().copy(
            currentBalance = Money("150.00", "SAR"),
            lowBalanceThreshold = Money("100.00", "SAR")
        )

        // When & Then
        assertFalse(account.isLowBalance(), "Account with balance above threshold should not be low balance")
    }

    @Test
    fun `should handle account without low balance threshold`() {
        // Given
        val account = createTestAccount().copy(
            currentBalance = Money("50.00", "SAR"),
            lowBalanceThreshold = null
        )

        // When & Then
        assertFalse(account.isLowBalance(), "Account without threshold should never be low balance")
    }

    @Test
    fun `should format account display name correctly`() {
        // Given
        val account = createTestAccount().copy(
            accountName = "My Primary Account",
            accountNumber = "1234567890"
        )

        // When
        val displayName = account.getDisplayName()

        // Then
        assertEquals("My Primary Account (...7890)", displayName)
    }

    @Test
    fun `should format account display name with short account number`() {
        // Given
        val account = createTestAccount().copy(
            accountName = "Test Account",
            accountNumber = "123"
        )

        // When
        val displayName = account.getDisplayName()

        // Then
        assertEquals("Test Account (123)", displayName)
    }

    @Test
    fun `should get masked account number for security`() {
        // Given
        val account = createTestAccount().copy(
            accountNumber = "1234567890"
        )

        // When
        val maskedNumber = account.getMaskedAccountNumber()

        // Then
        assertEquals("******7890", maskedNumber)
    }

    @Test
    fun `should handle short account numbers in masking`() {
        // Given
        val account = createTestAccount().copy(
            accountNumber = "123"
        )

        // When
        val maskedNumber = account.getMaskedAccountNumber()

        // Then
        assertEquals("123", maskedNumber, "Short account numbers should not be masked")
    }

    @Test
    fun `should identify inactive accounts correctly`() {
        // Given
        val activeAccount = createTestAccount().copy(isActive = true)
        val inactiveAccount = createTestAccount().copy(isActive = false)

        // When & Then
        assertTrue(activeAccount.isActive, "Active account should be identified as active")
        assertFalse(inactiveAccount.isActive, "Inactive account should be identified as inactive")
    }

    @Test
    fun `should support different account types`() {
        // Given
        val accountTypes = listOf(
            AccountType.CURRENT,
            AccountType.SAVINGS,
            AccountType.CREDIT,
            AccountType.LOAN,
            AccountType.INVESTMENT
        )

        accountTypes.forEach { type ->
            // When
            val account = createTestAccount().copy(accountType = type)

            // Then
            assertEquals(type, account.accountType, "Account type should be set correctly")
        }
    }

    @Test
    fun `should support different currencies`() {
        // Given
        val currencies = listOf("SAR", "USD", "EUR", "GBP")

        currencies.forEach { currency ->
            // When
            val account = createTestAccount().copy(
                currency = currency,
                currentBalance = Money("1000.00", currency)
            )

            // Then
            assertEquals(currency, account.currency, "Account currency should be set correctly")
            assertEquals(currency, account.currentBalance?.currency, "Balance currency should match")
        }
    }

    @Test
    fun `should calculate days since last transaction`() {
        // Given
        val now = Clock.System.now()
        val threeDaysAgo = now.minus(kotlinx.datetime.DateTimeUnit.DAY * 3)
        val account = createTestAccount().copy(
            lastTransactionDate = threeDaysAgo
        )

        // When
        val daysSince = account.getDaysSinceLastTransaction()

        // Then
        assertTrue(daysSince >= 3, "Should calculate days since last transaction correctly")
    }

    @Test
    fun `should handle account with no transactions`() {
        // Given
        val account = createTestAccount().copy(
            lastTransactionDate = null
        )

        // When
        val daysSince = account.getDaysSinceLastTransaction()

        // Then
        assertEquals(Int.MAX_VALUE, daysSince, "Account with no transactions should return max value")
    }

    @Test
    fun `should create account summary with all required fields`() {
        // Given
        val account = createTestAccount().copy(
            currentBalance = Money("1000.00", "SAR"),
            availableBalance = Money("900.00", "SAR")
        )

        // When
        val summary = account.getAccountSummary()

        // Then
        assertNotNull(summary.accountId)
        assertNotNull(summary.accountName)
        assertNotNull(summary.bankCode)
        assertNotNull(summary.currentBalance)
        assertNotNull(summary.availableBalance)
        assertNotNull(summary.reservedAmount)
    }

    private fun createTestAccount(): FinancialAccount {
        return FinancialAccount(
            accountId = "test_account_123",
            accountNumber = "1234567890",
            bankCode = "alrajhi",
            accountName = "Test Account",
            accountType = AccountType.CURRENT,
            currency = "SAR",
            isActive = true,
            createdAt = Clock.System.now()
        )
    }
}