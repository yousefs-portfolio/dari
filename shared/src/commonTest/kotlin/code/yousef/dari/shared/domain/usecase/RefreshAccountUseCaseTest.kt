package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.sama.OpenBankingClient
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

class RefreshAccountUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val openBankingClient = mockk<OpenBankingClient>()
    private val refreshAccountUseCase = RefreshAccountUseCase(
        accountRepository = accountRepository,
        openBankingClient = openBankingClient
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
    fun `should successfully refresh account data`() = runTest {
        // Given
        val accountId = "acc_123"
        val updatedAccount = sampleAccount.copy(
            balance = Money.sar(275000), // Updated balance
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        )

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.getAccountInfo(sampleAccount.bankCode, accountId) 
        } returns Result.success(updatedAccount)
        
        coEvery { 
            accountRepository.updateAccount(updatedAccount) 
        } returns Result.success(updatedAccount)

        // When
        val result = refreshAccountUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedAccount, result.getOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.getAccountInfo(sampleAccount.bankCode, accountId) }
        coVerify { accountRepository.updateAccount(updatedAccount) }
    }

    @Test
    fun `should fail when account not found`() = runTest {
        // Given
        val invalidAccountId = "invalid_acc"

        coEvery { 
            accountRepository.getAccountById(invalidAccountId) 
        } returns null

        // When
        val result = refreshAccountUseCase(invalidAccountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
        
        coVerify { accountRepository.getAccountById(invalidAccountId) }
        coVerify(exactly = 0) { openBankingClient.getAccountInfo(any(), any()) }
        coVerify(exactly = 0) { accountRepository.updateAccount(any()) }
    }

    @Test
    fun `should fail when account is inactive`() = runTest {
        // Given
        val inactiveAccount = sampleAccount.copy(isActive = false)
        val accountId = "acc_123"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns inactiveAccount

        // When
        val result = refreshAccountUseCase(accountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("inactive") == true)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify(exactly = 0) { openBankingClient.getAccountInfo(any(), any()) }
        coVerify(exactly = 0) { accountRepository.updateAccount(any()) }
    }

    @Test
    fun `should handle remote refresh failure gracefully`() = runTest {
        // Given
        val accountId = "acc_123"
        val refreshError = Exception("Network connection failed")

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.getAccountInfo(sampleAccount.bankCode, accountId) 
        } returns Result.failure(refreshError)

        // When
        val result = refreshAccountUseCase(accountId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(refreshError, result.exceptionOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.getAccountInfo(sampleAccount.bankCode, accountId) }
        coVerify(exactly = 0) { accountRepository.updateAccount(any()) }
    }

    @Test
    fun `should handle local update failure after successful remote refresh`() = runTest {
        // Given
        val accountId = "acc_123"
        val updatedAccount = sampleAccount.copy(balance = Money.sar(275000))
        val updateError = Exception("Database update failed")

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.getAccountInfo(sampleAccount.bankCode, accountId) 
        } returns Result.success(updatedAccount)
        
        coEvery { 
            accountRepository.updateAccount(updatedAccount) 
        } returns Result.failure(updateError)

        // When
        val result = refreshAccountUseCase(accountId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(updateError, result.exceptionOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.getAccountInfo(sampleAccount.bankCode, accountId) }
        coVerify { accountRepository.updateAccount(updatedAccount) }
    }

    @Test
    fun `should refresh all accounts for a user`() = runTest {
        // Given
        val userId = "user_123"
        val accounts = listOf(
            sampleAccount,
            sampleAccount.copy(accountId = "acc_456", balance = Money.sar(100000))
        )
        val updatedAccounts = accounts.map { account ->
            account.copy(balance = Money(account.balance.amount + 10000, account.balance.currency))
        }

        coEvery { 
            accountRepository.getAccountsByUserId(userId) 
        } returns accounts
        
        accounts.forEachIndexed { index, account ->
            coEvery { 
                openBankingClient.getAccountInfo(account.bankCode, account.accountId) 
            } returns Result.success(updatedAccounts[index])
            
            coEvery { 
                accountRepository.updateAccount(updatedAccounts[index]) 
            } returns Result.success(updatedAccounts[index])
        }

        // When
        val result = refreshAccountUseCase.refreshAllAccounts(userId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedAccounts, result.getOrNull())
        
        coVerify { accountRepository.getAccountsByUserId(userId) }
        accounts.forEach { account ->
            coVerify { openBankingClient.getAccountInfo(account.bankCode, account.accountId) }
        }
        updatedAccounts.forEach { account ->
            coVerify { accountRepository.updateAccount(account) }
        }
    }

    @Test
    fun `should refresh multiple specific accounts`() = runTest {
        // Given
        val accountIds = listOf("acc_123", "acc_456")
        val accounts = listOf(
            sampleAccount,
            sampleAccount.copy(accountId = "acc_456", balance = Money.sar(100000))
        )
        val updatedAccounts = accounts.map { account ->
            account.copy(balance = Money(account.balance.amount + 5000, account.balance.currency))
        }

        accountIds.forEachIndexed { index, accountId ->
            coEvery { 
                accountRepository.getAccountById(accountId) 
            } returns accounts[index]
            
            coEvery { 
                openBankingClient.getAccountInfo(accounts[index].bankCode, accountId) 
            } returns Result.success(updatedAccounts[index])
            
            coEvery { 
                accountRepository.updateAccount(updatedAccounts[index]) 
            } returns Result.success(updatedAccounts[index])
        }

        // When
        val result = refreshAccountUseCase.refreshMultipleAccounts(accountIds)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        
        accountIds.forEach { accountId ->
            coVerify { accountRepository.getAccountById(accountId) }
        }
    }

    @Test
    fun `should handle partial failures when refreshing multiple accounts`() = runTest {
        // Given
        val accountIds = listOf("acc_123", "acc_456", "acc_789")
        val accounts = listOf(
            sampleAccount,
            sampleAccount.copy(accountId = "acc_456", balance = Money.sar(100000)),
            null // Third account doesn't exist
        )
        val updatedAccounts = listOf(
            sampleAccount.copy(balance = Money.sar(255000)),
            accounts[1]!!.copy(balance = Money.sar(105000))
        )

        // First account - success
        coEvery { accountRepository.getAccountById("acc_123") } returns accounts[0]
        coEvery { 
            openBankingClient.getAccountInfo(sampleAccount.bankCode, "acc_123") 
        } returns Result.success(updatedAccounts[0])
        coEvery { 
            accountRepository.updateAccount(updatedAccounts[0]) 
        } returns Result.success(updatedAccounts[0])

        // Second account - success
        coEvery { accountRepository.getAccountById("acc_456") } returns accounts[1]
        coEvery { 
            openBankingClient.getAccountInfo(accounts[1]!!.bankCode, "acc_456") 
        } returns Result.success(updatedAccounts[1])
        coEvery { 
            accountRepository.updateAccount(updatedAccounts[1]) 
        } returns Result.success(updatedAccounts[1])

        // Third account - not found
        coEvery { accountRepository.getAccountById("acc_789") } returns null

        // When
        val result = refreshAccountUseCase.refreshMultipleAccounts(accountIds)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size) // Should succeed for 2 out of 3 accounts
        
        accountIds.forEach { accountId ->
            coVerify { accountRepository.getAccountById(accountId) }
        }
    }

    @Test
    fun `should validate account ID before refresh`() = runTest {
        // Given
        val emptyAccountId = ""

        // When
        val result = refreshAccountUseCase(emptyAccountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { accountRepository.getAccountById(any()) }
        coVerify(exactly = 0) { openBankingClient.getAccountInfo(any(), any()) }
        coVerify(exactly = 0) { accountRepository.updateAccount(any()) }
    }

    @Test
    fun `should handle forced refresh even with recent updates`() = runTest {
        // Given
        val accountId = "acc_123"
        val recentlyUpdatedAccount = sampleAccount.copy(
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC) // Just updated
        )
        val freshAccount = recentlyUpdatedAccount.copy(
            balance = Money.sar(285000) // Even newer balance
        )

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns recentlyUpdatedAccount
        
        coEvery { 
            openBankingClient.getAccountInfo(recentlyUpdatedAccount.bankCode, accountId) 
        } returns Result.success(freshAccount)
        
        coEvery { 
            accountRepository.updateAccount(freshAccount) 
        } returns Result.success(freshAccount)

        // When
        val result = refreshAccountUseCase.forceRefresh(accountId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(freshAccount, result.getOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.getAccountInfo(recentlyUpdatedAccount.bankCode, accountId) }
        coVerify { accountRepository.updateAccount(freshAccount) }
    }

    @Test
    fun `should handle Saudi-specific account refresh`() = runTest {
        // Given
        val saudiAccount = sampleAccount.copy(
            bankCode = "RIBLSARI", // Riyad Bank
            accountName = "حساب جاري", // Current account in Arabic
            currency = "SAR"
        )
        val accountId = "acc_saudi"
        val updatedSaudiAccount = saudiAccount.copy(
            balance = Money.sar(400000), // Updated balance
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        )

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns saudiAccount
        
        coEvery { 
            openBankingClient.getAccountInfo("RIBLSARI", accountId) 
        } returns Result.success(updatedSaudiAccount)
        
        coEvery { 
            accountRepository.updateAccount(updatedSaudiAccount) 
        } returns Result.success(updatedSaudiAccount)

        // When
        val result = refreshAccountUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        val refreshedAccount = result.getOrNull()
        assertEquals("حساب جاري", refreshedAccount?.accountName)
        assertEquals("RIBLSARI", refreshedAccount?.bankCode)
        assertEquals("SAR", refreshedAccount?.currency)
        assertEquals(Money.sar(400000), refreshedAccount?.balance)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.getAccountInfo("RIBLSARI", accountId) }
        coVerify { accountRepository.updateAccount(updatedSaudiAccount) }
    }
}