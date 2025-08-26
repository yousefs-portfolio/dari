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

class DisconnectBankAccountUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val openBankingClient = mockk<OpenBankingClient>()
    private val disconnectBankAccountUseCase = DisconnectBankAccountUseCase(
        accountRepository = accountRepository,
        openBankingClient = openBankingClient
    )

    private val sampleAccount = FinancialAccount(
        accountId = "acc_123",
        accountName = "Current Account",
        bankCode = "SAMA_BANK_001",
        accountType = "Current",
        balance = Money.sar(250000),
        currency = "SAR",
        accountNumber = "****1234",
        iban = "SA03800001234567891011",
        isActive = true,
        lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    )

    @Test
    fun `should successfully disconnect bank account`() = runTest {
        // Given
        val accountId = "acc_123"
        val userId = "user_123"
        val reason = "User requested disconnection"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.revokeConsent("SAMA_BANK_001", userId) 
        } returns Result.success(Unit)
        
        coEvery { 
            accountRepository.disconnectAccount(accountId, reason) 
        } returns Result.success(Unit)

        // When
        val result = disconnectBankAccountUseCase(accountId, userId, reason)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.revokeConsent("SAMA_BANK_001", userId) }
        coVerify { accountRepository.disconnectAccount(accountId, reason) }
    }

    @Test
    fun `should fail when account not found`() = runTest {
        // Given
        val invalidAccountId = "invalid_acc"
        val userId = "user_123"
        val reason = "User requested disconnection"

        coEvery { 
            accountRepository.getAccountById(invalidAccountId) 
        } returns null

        // When
        val result = disconnectBankAccountUseCase(invalidAccountId, userId, reason)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
        
        coVerify { accountRepository.getAccountById(invalidAccountId) }
        coVerify(exactly = 0) { openBankingClient.revokeConsent(any(), any()) }
        coVerify(exactly = 0) { accountRepository.disconnectAccount(any(), any()) }
    }

    @Test
    fun `should continue disconnection even if consent revocation fails`() = runTest {
        // Given
        val accountId = "acc_123"
        val userId = "user_123"
        val reason = "User requested disconnection"
        val consentError = Exception("Consent revocation failed")

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.revokeConsent("SAMA_BANK_001", userId) 
        } returns Result.failure(consentError)
        
        coEvery { 
            accountRepository.disconnectAccount(accountId, reason) 
        } returns Result.success(Unit)

        // When
        val result = disconnectBankAccountUseCase(accountId, userId, reason)

        // Then
        assertTrue(result.isSuccess) // Should still succeed locally
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.revokeConsent("SAMA_BANK_001", userId) }
        coVerify { accountRepository.disconnectAccount(accountId, reason) }
    }

    @Test
    fun `should fail when local disconnection fails`() = runTest {
        // Given
        val accountId = "acc_123"
        val userId = "user_123"
        val reason = "User requested disconnection"
        val disconnectionError = Exception("Database error")

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.revokeConsent("SAMA_BANK_001", userId) 
        } returns Result.success(Unit)
        
        coEvery { 
            accountRepository.disconnectAccount(accountId, reason) 
        } returns Result.failure(disconnectionError)

        // When
        val result = disconnectBankAccountUseCase(accountId, userId, reason)

        // Then
        assertTrue(result.isFailure)
        assertEquals(disconnectionError, result.exceptionOrNull())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.revokeConsent("SAMA_BANK_001", userId) }
        coVerify { accountRepository.disconnectAccount(accountId, reason) }
    }

    @Test
    fun `should validate account ID before disconnection`() = runTest {
        // Given
        val emptyAccountId = ""
        val userId = "user_123"
        val reason = "Test"

        // When
        val result = disconnectBankAccountUseCase(emptyAccountId, userId, reason)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { accountRepository.getAccountById(any()) }
        coVerify(exactly = 0) { openBankingClient.revokeConsent(any(), any()) }
        coVerify(exactly = 0) { accountRepository.disconnectAccount(any(), any()) }
    }

    @Test
    fun `should validate user ID before disconnection`() = runTest {
        // Given
        val accountId = "acc_123"
        val emptyUserId = ""
        val reason = "Test"

        // When
        val result = disconnectBankAccountUseCase(accountId, emptyUserId, reason)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { accountRepository.getAccountById(any()) }
        coVerify(exactly = 0) { openBankingClient.revokeConsent(any(), any()) }
        coVerify(exactly = 0) { accountRepository.disconnectAccount(any(), any()) }
    }

    @Test
    fun `should disconnect all accounts for a bank`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val reason = "Bank requested disconnection"
        
        val accounts = listOf(
            sampleAccount,
            sampleAccount.copy(accountId = "acc_456", accountName = "Savings Account")
        )

        coEvery { 
            accountRepository.getAccountsByBank(bankCode) 
        } returns accounts
        
        coEvery { 
            openBankingClient.revokeConsent(bankCode, userId) 
        } returns Result.success(Unit)
        
        accounts.forEach { account ->
            coEvery { 
                accountRepository.disconnectAccount(account.accountId, reason) 
            } returns Result.success(Unit)
        }

        // When
        val result = disconnectBankAccountUseCase.disconnectAllAccountsForBank(bankCode, userId, reason)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        
        coVerify { accountRepository.getAccountsByBank(bankCode) }
        coVerify { openBankingClient.revokeConsent(bankCode, userId) }
        accounts.forEach { account ->
            coVerify { accountRepository.disconnectAccount(account.accountId, reason) }
        }
    }

    @Test
    fun `should handle inactive account disconnection`() = runTest {
        // Given
        val inactiveAccount = sampleAccount.copy(isActive = false)
        val accountId = "acc_123"
        val userId = "user_123"
        val reason = "Cleanup inactive accounts"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns inactiveAccount
        
        coEvery { 
            accountRepository.disconnectAccount(accountId, reason) 
        } returns Result.success(Unit)

        // When
        val result = disconnectBankAccountUseCase(accountId, userId, reason)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { accountRepository.getAccountById(accountId) }
        // Should not attempt consent revocation for inactive accounts
        coVerify(exactly = 0) { openBankingClient.revokeConsent(any(), any()) }
        coVerify { accountRepository.disconnectAccount(accountId, reason) }
    }

    @Test
    fun `should track disconnection metrics`() = runTest {
        // Given
        val accountId = "acc_123"
        val userId = "user_123"
        val reason = "User requested disconnection"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            openBankingClient.revokeConsent("SAMA_BANK_001", userId) 
        } returns Result.success(Unit)
        
        coEvery { 
            accountRepository.disconnectAccount(accountId, reason) 
        } returns Result.success(Unit)

        coEvery { 
            accountRepository.recordDisconnectionEvent(accountId, userId, reason, any()) 
        } returns Unit

        // When
        val result = disconnectBankAccountUseCase.disconnectWithMetrics(accountId, userId, reason)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.revokeConsent("SAMA_BANK_001", userId) }
        coVerify { accountRepository.disconnectAccount(accountId, reason) }
        coVerify { accountRepository.recordDisconnectionEvent(accountId, userId, reason, any()) }
    }

    @Test
    fun `should handle Saudi-specific disconnection requirements`() = runTest {
        // Given
        val saudiAccount = sampleAccount.copy(
            bankCode = "RIBLSARI", // Riyad Bank
            accountName = "حساب جاري" // Current account in Arabic
        )
        val accountId = "acc_123"
        val userId = "user_123"
        val reason = "إلغاء ربط الحساب" // Account disconnection in Arabic

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns saudiAccount
        
        coEvery { 
            openBankingClient.revokeConsent("RIBLSARI", userId) 
        } returns Result.success(Unit)
        
        coEvery { 
            accountRepository.disconnectAccount(accountId, reason) 
        } returns Result.success(Unit)

        // When
        val result = disconnectBankAccountUseCase(accountId, userId, reason)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { openBankingClient.revokeConsent("RIBLSARI", userId) }
        coVerify { accountRepository.disconnectAccount(accountId, reason) }
    }
}