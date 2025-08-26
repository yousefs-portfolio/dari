package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.Money
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetAccountBalanceUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val getAccountBalanceUseCase = GetAccountBalanceUseCase(
        accountRepository = accountRepository
    )

    private val sampleAccount = FinancialAccount(
        accountId = "acc_123",
        accountName = "Current Account",
        bankCode = "SAMA_BANK_001",
        accountType = "Current",
        balance = Money.sar(250000), // 2,500 SAR
        currency = "SAR",
        accountNumber = "****1234",
        iban = "SA03800001234567891011",
        isActive = true,
        lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    )

    @Test
    fun `should get current account balance successfully`() = runTest {
        // Given
        val accountId = "acc_123"
        val expectedBalance = Money.sar(250000)

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount

        // When
        val result = getAccountBalanceUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedBalance, result.getOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
    }

    @Test
    fun `should fail when account not found`() = runTest {
        // Given
        val invalidAccountId = "invalid_acc"

        coEvery { 
            accountRepository.getAccountById(invalidAccountId) 
        } returns null

        // When
        val result = getAccountBalanceUseCase(invalidAccountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
        
        coVerify { accountRepository.getAccountById(invalidAccountId) }
    }

    @Test
    fun `should handle inactive account balance request`() = runTest {
        // Given
        val inactiveAccount = sampleAccount.copy(isActive = false)
        val accountId = "acc_123"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns inactiveAccount

        // When
        val result = getAccountBalanceUseCase(accountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("inactive") == true)
        
        coVerify { accountRepository.getAccountById(accountId) }
    }

    @Test
    fun `should get fresh balance from remote when requested`() = runTest {
        // Given
        val accountId = "acc_123"
        val cachedBalance = Money.sar(250000)
        val freshBalance = Money.sar(275000) // Updated balance

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            accountRepository.refreshAccountBalance(accountId) 
        } returns Result.success(freshBalance)

        // When
        val result = getAccountBalanceUseCase.getFreshBalance(accountId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(freshBalance, result.getOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { accountRepository.refreshAccountBalance(accountId) }
    }

    @Test
    fun `should handle multiple account balances request`() = runTest {
        // Given
        val accountIds = listOf("acc_123", "acc_456", "acc_789")
        val accounts = listOf(
            sampleAccount,
            sampleAccount.copy(accountId = "acc_456", balance = Money.sar(100000)),
            sampleAccount.copy(accountId = "acc_789", balance = Money.sar(500000))
        )
        val expectedBalances = mapOf(
            "acc_123" to Money.sar(250000),
            "acc_456" to Money.sar(100000),
            "acc_789" to Money.sar(500000)
        )

        accounts.forEach { account ->
            coEvery { 
                accountRepository.getAccountById(account.accountId) 
            } returns account
        }

        // When
        val result = getAccountBalanceUseCase.getMultipleBalances(accountIds)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedBalances, result.getOrNull())
        
        accountIds.forEach { accountId ->
            coVerify { accountRepository.getAccountById(accountId) }
        }
    }

    @Test
    fun `should get total balance across all accounts`() = runTest {
        // Given
        val accountIds = listOf("acc_123", "acc_456")
        val accounts = listOf(
            sampleAccount.copy(balance = Money.sar(250000)), // 2,500 SAR
            sampleAccount.copy(accountId = "acc_456", balance = Money.sar(150000)) // 1,500 SAR
        )
        val expectedTotal = Money.sar(400000) // 4,000 SAR total

        accounts.forEach { account ->
            coEvery { 
                accountRepository.getAccountById(account.accountId) 
            } returns account
        }

        // When
        val result = getAccountBalanceUseCase.getTotalBalance(accountIds)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTotal, result.getOrNull())
        
        accountIds.forEach { accountId ->
            coVerify { accountRepository.getAccountById(accountId) }
        }
    }

    @Test
    fun `should handle mixed currency balances`() = runTest {
        // Given
        val accountIds = listOf("acc_sar", "acc_usd")
        val sarAccount = sampleAccount.copy(
            accountId = "acc_sar",
            balance = Money.sar(200000),
            currency = "SAR"
        )
        val usdAccount = sampleAccount.copy(
            accountId = "acc_usd",
            balance = Money(50000, "USD"), // $500 USD
            currency = "USD"
        )

        coEvery { accountRepository.getAccountById("acc_sar") } returns sarAccount
        coEvery { accountRepository.getAccountById("acc_usd") } returns usdAccount

        // When
        val result = getAccountBalanceUseCase.getMultipleBalances(accountIds)

        // Then
        assertTrue(result.isSuccess)
        val balances = result.getOrNull()
        assertEquals(Money.sar(200000), balances?.get("acc_sar"))
        assertEquals(Money(50000, "USD"), balances?.get("acc_usd"))
        
        coVerify { accountRepository.getAccountById("acc_sar") }
        coVerify { accountRepository.getAccountById("acc_usd") }
    }

    @Test
    fun `should validate account ID before balance retrieval`() = runTest {
        // Given
        val emptyAccountId = ""

        // When
        val result = getAccountBalanceUseCase(emptyAccountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { accountRepository.getAccountById(any()) }
    }

    @Test
    fun `should handle balance with historical data`() = runTest {
        // Given
        val accountId = "acc_123"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            accountRepository.getBalanceHistory(accountId, 30) 
        } returns listOf(
            Money.sar(240000), // 30 days ago
            Money.sar(245000), // 15 days ago
            Money.sar(250000)  // current
        )

        // When
        val result = getAccountBalanceUseCase.getBalanceWithHistory(accountId, 30)

        // Then
        assertTrue(result.isSuccess)
        val balanceHistory = result.getOrNull()
        assertEquals(Money.sar(250000), balanceHistory?.currentBalance)
        assertEquals(3, balanceHistory?.history?.size)
        assertEquals(Money.sar(240000), balanceHistory?.history?.first())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { accountRepository.getBalanceHistory(accountId, 30) }
    }

    @Test
    fun `should get available balance vs current balance`() = runTest {
        // Given
        val accountId = "acc_123"
        val currentBalance = Money.sar(250000)
        val availableBalance = Money.sar(230000) // Less due to holds/pending transactions

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            accountRepository.getAvailableBalance(accountId) 
        } returns Result.success(availableBalance)

        // When
        val currentResult = getAccountBalanceUseCase(accountId)
        val availableResult = getAccountBalanceUseCase.getAvailableBalance(accountId)

        // Then
        assertTrue(currentResult.isSuccess)
        assertTrue(availableResult.isSuccess)
        assertEquals(currentBalance, currentResult.getOrNull())
        assertEquals(availableBalance, availableResult.getOrNull())
        assertTrue((currentResult.getOrNull()?.amount ?: 0) >= (availableResult.getOrNull()?.amount ?: 0))
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { accountRepository.getAvailableBalance(accountId) }
    }

    @Test
    fun `should handle Saudi-specific balance features`() = runTest {
        // Given
        val saudiAccount = sampleAccount.copy(
            bankCode = "RIBLSARI", // Riyad Bank
            currency = "SAR",
            balance = Money.sar(375000) // 3,750 SAR
        )
        val accountId = "acc_saudi"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns saudiAccount

        // When
        val result = getAccountBalanceUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        val balance = result.getOrNull()
        assertEquals("SAR", balance?.currency)
        assertEquals(375000, balance?.amount) // Amount in halalas (1 SAR = 100 halalas)
        
        coVerify { accountRepository.getAccountById(accountId) }
    }
}