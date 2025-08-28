package code.yousef.dari.presentation.bank

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.sama.sdk.OpenBankingClient
import code.yousef.dari.sama.sdk.models.Bank
import code.yousef.dari.sama.sdk.models.ConsentRequest
import code.yousef.dari.sama.sdk.models.ConsentResponse
import code.yousef.dari.sama.sdk.models.PermissionType
import code.yousef.dari.shared.data.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OAuthFlowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val openBankingClient = mockk<OpenBankingClient>()
    private val accountRepository = mockk<AccountRepository>()

    private lateinit var viewModel: BankConnectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default responses
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should initiate OAuth flow with correct consent parameters`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        val consentResponse = ConsentResponse(
            consentId = "consent-123",
            authorizationUrl = "https://alrajhibank.com.sa/oauth/authorize?consent_id=consent-123",
            expiresAt = "2024-12-31T23:59:59Z"
        )
        
        val consentRequestSlot = slot<ConsentRequest>()
        coEvery { openBankingClient.createConsent(capture(consentRequestSlot)) } returns Result.success(consentResponse)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.connectToBank(bank)
        advanceUntilIdle()

        // Then
        val capturedRequest = consentRequestSlot.captured
        assertEquals("al-rajhi", capturedRequest.bankId)
        assertTrue(capturedRequest.permissions.contains(PermissionType.READ_ACCOUNTS))
        assertTrue(capturedRequest.permissions.contains(PermissionType.READ_BALANCES))
        assertTrue(capturedRequest.permissions.contains(PermissionType.READ_TRANSACTIONS))
        assertNotNull(capturedRequest.expirationDateTime)
        assertNotNull(capturedRequest.transactionFromDateTime)
        assertNotNull(capturedRequest.transactionToDateTime)
    }

    @Test
    fun `should update UI state correctly when OAuth flow is initiated`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        val consentResponse = ConsentResponse(
            consentId = "consent-123",
            authorizationUrl = "https://alrajhibank.com.sa/oauth/authorize?consent_id=consent-123",
            expiresAt = "2024-12-31T23:59:59Z"
        )
        
        coEvery { openBankingClient.createConsent(any()) } returns Result.success(consentResponse)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.connectToBank(bank)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertEquals(bank, uiState.selectedBank)
        assertEquals("consent-123", uiState.currentConsentId)
        assertEquals("https://alrajhibank.com.sa/oauth/authorize?consent_id=consent-123", uiState.authorizationUrl)
        assertTrue(uiState.error == null)
        assertFalse(uiState.connectionSuccessful)
    }

    @Test
    fun `should show connecting state during OAuth initiation`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        
        coEvery { openBankingClient.createConsent(any()) } coAnswers {
            // Simulate delay
            kotlinx.coroutines.delay(100)
            Result.success(ConsentResponse("consent-123", "https://example.com", "2024-12-31T23:59:59Z"))
        }

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.connectToBank(bank)

        // Then - Check connecting state
        val connectingState = viewModel.uiState.value
        assertTrue(connectingState.isConnecting)

        // Wait for completion
        advanceUntilIdle()

        // Then - Check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isConnecting)
    }

    @Test
    fun `should handle OAuth initiation failure`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        val errorMessage = "Consent creation failed"
        
        coEvery { openBankingClient.createConsent(any()) } returns Result.failure(Exception(errorMessage))

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.connectToBank(bank)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertEquals(errorMessage, uiState.error)
        assertTrue(uiState.authorizationUrl == null)
        assertTrue(uiState.currentConsentId == null)
        assertTrue(uiState.selectedBank == null)
    }

    @Test
    fun `should complete authorization flow successfully`() = runTest {
        // Given
        val authorizationCode = "auth-code-123"
        val consentId = "consent-123"
        
        coEvery { openBankingClient.exchangeAuthorizationCode(authorizationCode, consentId) } returns Result.success(Unit)
        coEvery { accountRepository.syncAccountsForConsent(consentId) } returns Result.success(Unit)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.completeAuthorization(authorizationCode, consentId)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertTrue(uiState.connectionSuccessful)
        assertTrue(uiState.error == null)

        coVerify { 
            openBankingClient.exchangeAuthorizationCode(authorizationCode, consentId)
            accountRepository.syncAccountsForConsent(consentId)
        }
    }

    @Test
    fun `should handle authorization completion failure`() = runTest {
        // Given
        val authorizationCode = "auth-code-123"
        val consentId = "consent-123"
        val errorMessage = "Token exchange failed"
        
        coEvery { openBankingClient.exchangeAuthorizationCode(authorizationCode, consentId) } returns Result.failure(Exception(errorMessage))

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.completeAuthorization(authorizationCode, consentId)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertFalse(uiState.connectionSuccessful)
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `should handle account sync failure after successful authorization`() = runTest {
        // Given
        val authorizationCode = "auth-code-123"
        val consentId = "consent-123"
        val syncErrorMessage = "Account sync failed"
        
        coEvery { openBankingClient.exchangeAuthorizationCode(authorizationCode, consentId) } returns Result.success(Unit)
        coEvery { accountRepository.syncAccountsForConsent(consentId) } returns Result.failure(Exception(syncErrorMessage))

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.completeAuthorization(authorizationCode, consentId)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertFalse(uiState.connectionSuccessful)
        assertEquals(syncErrorMessage, uiState.error)
        
        // Verify both steps were called
        coVerify { 
            openBankingClient.exchangeAuthorizationCode(authorizationCode, consentId)
            accountRepository.syncAccountsForConsent(consentId)
        }
    }

    @Test
    fun `should reset connection state when requested`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        val consentResponse = ConsentResponse(
            consentId = "consent-123",
            authorizationUrl = "https://example.com",
            expiresAt = "2024-12-31T23:59:59Z"
        )
        
        coEvery { openBankingClient.createConsent(any()) } returns Result.success(consentResponse)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Setup some state
        viewModel.connectToBank(bank)
        advanceUntilIdle()

        // Verify state is set
        assertTrue(viewModel.uiState.value.selectedBank != null)
        assertTrue(viewModel.uiState.value.currentConsentId != null)

        // When
        viewModel.resetConnectionState()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.selectedBank == null)
        assertTrue(uiState.currentConsentId == null)
        assertTrue(uiState.authorizationUrl == null)
        assertFalse(uiState.connectionSuccessful)
        assertFalse(uiState.isConnecting)
        assertTrue(uiState.error == null)
    }

    private fun createMockBank(id: String, name: String) = Bank(
        id = id,
        name = name,
        displayName = name,
        logoUrl = "https://example.com/logo.png",
        baseUrl = "https://api.$id.com.sa",
        supportedServices = listOf("accounts", "payments", "balances", "transactions"),
        isActive = true
    )
}