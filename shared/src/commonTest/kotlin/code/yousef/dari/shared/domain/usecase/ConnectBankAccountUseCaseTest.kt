package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.sama.ConsentStatus
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

class ConnectBankAccountUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val openBankingClient = mockk<OpenBankingClient>()
    private val connectBankAccountUseCase = ConnectBankAccountUseCase(
        accountRepository = accountRepository,
        openBankingClient = openBankingClient
    )

    @Test
    fun `should successfully connect bank account with valid consent`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val consentId = "consent_456"
        val redirectUrl = "https://app.dari.com/callback"
        
        val expectedAccount = FinancialAccount(
            accountId = "acc_123",
            accountName = "Current Account",
            bankCode = bankCode,
            accountType = "Current",
            balance = Money.sar(250000), // 2,500 SAR
            currency = "SAR",
            accountNumber = "****1234",
            iban = "SA03800001234567891011",
            isActive = true,
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        )

        coEvery { 
            openBankingClient.initiateConsent(bankCode, userId, any()) 
        } returns Result.success(consentId)
        
        coEvery { 
            openBankingClient.getConsentStatus(consentId) 
        } returns ConsentStatus.AUTHORIZED
        
        coEvery { 
            accountRepository.connectBankAccount(bankCode, consentId) 
        } returns Result.success(expectedAccount)

        // When
        val result = connectBankAccountUseCase(bankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAccount, result.getOrNull())
        
        coVerify { openBankingClient.initiateConsent(bankCode, userId, redirectUrl) }
        coVerify { openBankingClient.getConsentStatus(consentId) }
        coVerify { accountRepository.connectBankAccount(bankCode, consentId) }
    }

    @Test
    fun `should fail when consent initiation fails`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val redirectUrl = "https://app.dari.com/callback"
        val error = Exception("Network error")

        coEvery { 
            openBankingClient.initiateConsent(bankCode, userId, redirectUrl) 
        } returns Result.failure(error)

        // When
        val result = connectBankAccountUseCase(bankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        
        coVerify { openBankingClient.initiateConsent(bankCode, userId, redirectUrl) }
        coVerify(exactly = 0) { openBankingClient.getConsentStatus(any()) }
        coVerify(exactly = 0) { accountRepository.connectBankAccount(any(), any()) }
    }

    @Test
    fun `should fail when consent is rejected`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val consentId = "consent_456"
        val redirectUrl = "https://app.dari.com/callback"

        coEvery { 
            openBankingClient.initiateConsent(bankCode, userId, redirectUrl) 
        } returns Result.success(consentId)
        
        coEvery { 
            openBankingClient.getConsentStatus(consentId) 
        } returns ConsentStatus.REJECTED

        // When
        val result = connectBankAccountUseCase(bankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("rejected") == true)
        
        coVerify { openBankingClient.initiateConsent(bankCode, userId, redirectUrl) }
        coVerify { openBankingClient.getConsentStatus(consentId) }
        coVerify(exactly = 0) { accountRepository.connectBankAccount(any(), any()) }
    }

    @Test
    fun `should handle consent timeout`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val consentId = "consent_456"
        val redirectUrl = "https://app.dari.com/callback"

        coEvery { 
            openBankingClient.initiateConsent(bankCode, userId, redirectUrl) 
        } returns Result.success(consentId)
        
        coEvery { 
            openBankingClient.getConsentStatus(consentId) 
        } returns ConsentStatus.EXPIRED

        // When
        val result = connectBankAccountUseCase(bankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("expired") == true)
        
        coVerify { openBankingClient.initiateConsent(bankCode, userId, redirectUrl) }
        coVerify { openBankingClient.getConsentStatus(consentId) }
        coVerify(exactly = 0) { accountRepository.connectBankAccount(any(), any()) }
    }

    @Test
    fun `should fail when account connection fails after successful consent`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val consentId = "consent_456"
        val redirectUrl = "https://app.dari.com/callback"
        val connectionError = Exception("Account connection failed")

        coEvery { 
            openBankingClient.initiateConsent(bankCode, userId, redirectUrl) 
        } returns Result.success(consentId)
        
        coEvery { 
            openBankingClient.getConsentStatus(consentId) 
        } returns ConsentStatus.AUTHORIZED
        
        coEvery { 
            accountRepository.connectBankAccount(bankCode, consentId) 
        } returns Result.failure(connectionError)

        // When
        val result = connectBankAccountUseCase(bankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isFailure)
        assertEquals(connectionError, result.exceptionOrNull())
        
        coVerify { openBankingClient.initiateConsent(bankCode, userId, redirectUrl) }
        coVerify { openBankingClient.getConsentStatus(consentId) }
        coVerify { accountRepository.connectBankAccount(bankCode, consentId) }
    }

    @Test
    fun `should validate bank code format before connection`() = runTest {
        // Given
        val invalidBankCode = ""
        val userId = "user_123"
        val redirectUrl = "https://app.dari.com/callback"

        // When
        val result = connectBankAccountUseCase(invalidBankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { openBankingClient.initiateConsent(any(), any(), any()) }
        coVerify(exactly = 0) { accountRepository.connectBankAccount(any(), any()) }
    }

    @Test
    fun `should validate user ID before connection`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val invalidUserId = ""
        val redirectUrl = "https://app.dari.com/callback"

        // When
        val result = connectBankAccountUseCase(bankCode, invalidUserId, redirectUrl)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { openBankingClient.initiateConsent(any(), any(), any()) }
        coVerify(exactly = 0) { accountRepository.connectBankAccount(any(), any()) }
    }

    @Test
    fun `should handle multiple accounts from single bank connection`() = runTest {
        // Given
        val bankCode = "SAMA_BANK_001"
        val userId = "user_123"
        val consentId = "consent_456"
        val redirectUrl = "https://app.dari.com/callback"
        
        val expectedAccount = FinancialAccount(
            accountId = "acc_123",
            accountName = "Current Account",
            bankCode = bankCode,
            accountType = "Current",
            balance = Money.sar(250000),
            currency = "SAR",
            accountNumber = "****1234",
            iban = "SA03800001234567891011",
            isActive = true,
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        )

        coEvery { 
            openBankingClient.initiateConsent(bankCode, userId, redirectUrl) 
        } returns Result.success(consentId)
        
        coEvery { 
            openBankingClient.getConsentStatus(consentId) 
        } returns ConsentStatus.AUTHORIZED
        
        coEvery { 
            accountRepository.connectBankAccount(bankCode, consentId) 
        } returns Result.success(expectedAccount)

        // When
        val result = connectBankAccountUseCase(bankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isSuccess)
        val connectedAccount = result.getOrNull()
        assertEquals(bankCode, connectedAccount?.bankCode)
        assertEquals("Current", connectedAccount?.accountType)
        assertTrue(connectedAccount?.isActive == true)
    }

    @Test
    fun `should support Saudi bank-specific connection parameters`() = runTest {
        // Given
        val saudiBankCode = "RIBLSARI" // Riyad Bank
        val userId = "user_123"
        val consentId = "consent_456"
        val redirectUrl = "https://app.dari.com/callback"
        
        val saudiAccount = FinancialAccount(
            accountId = "acc_riyadh_123",
            accountName = "حساب جاري", // Current account in Arabic
            bankCode = saudiBankCode,
            accountType = "Current",
            balance = Money.sar(500000), // 5,000 SAR
            currency = "SAR",
            accountNumber = "****5678",
            iban = "SA0380000123456789101112", // Saudi IBAN format
            isActive = true,
            lastUpdated = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        )

        coEvery { 
            openBankingClient.initiateConsent(saudiBankCode, userId, redirectUrl) 
        } returns Result.success(consentId)
        
        coEvery { 
            openBankingClient.getConsentStatus(consentId) 
        } returns ConsentStatus.AUTHORIZED
        
        coEvery { 
            accountRepository.connectBankAccount(saudiBankCode, consentId) 
        } returns Result.success(saudiAccount)

        // When
        val result = connectBankAccountUseCase(saudiBankCode, userId, redirectUrl)

        // Then
        assertTrue(result.isSuccess)
        val connectedAccount = result.getOrNull()
        assertEquals("حساب جاري", connectedAccount?.accountName)
        assertEquals("SAR", connectedAccount?.currency)
        assertTrue(connectedAccount?.iban?.startsWith("SA") == true)
        assertTrue(connectedAccount?.iban?.length == 24) // Saudi IBAN length
    }
}