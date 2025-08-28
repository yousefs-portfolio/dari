package code.yousef.dari.presentation.bank

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.sama.sdk.OpenBankingClient
import code.yousef.dari.sama.sdk.models.Bank
import code.yousef.dari.sama.sdk.models.ConsentRequest
import code.yousef.dari.sama.sdk.models.ConsentResponse
import code.yousef.dari.shared.data.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BankConnectionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val openBankingClient = mockk<OpenBankingClient>()
    private val accountRepository = mockk<AccountRepository>()

    private lateinit var viewModel: BankConnectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load supported banks on initialization`() = runTest {
        // Given
        val expectedBanks = listOf(
            createMockBank("al-rajhi", "Al Rajhi Bank"),
            createMockBank("snb", "Saudi National Bank"),
            createMockBank("riyad", "Riyad Bank")
        )
        
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(expectedBanks)

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(expectedBanks, uiState.supportedBanks)
        assertTrue(uiState.error == null)
    }

    @Test
    fun `should handle error when loading banks fails`() = runTest {
        // Given
        val errorMessage = "Failed to load banks"
        coEvery { openBankingClient.getSupportedBanks() } returns Result.failure(Exception(errorMessage))

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.supportedBanks.isEmpty())
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `should filter banks based on search query`() = runTest {
        // Given
        val banks = listOf(
            createMockBank("al-rajhi", "Al Rajhi Bank"),
            createMockBank("snb", "Saudi National Bank"),
            createMockBank("alinma", "Alinma Bank")
        )
        
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.searchBanks("rajhi")

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredBanks.size)
        assertEquals("Al Rajhi Bank", uiState.filteredBanks.first().name)
    }

    @Test
    fun `should initiate bank connection successfully`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        val consentResponse = ConsentResponse(
            consentId = "consent-123",
            authorizationUrl = "https://bank.com/auth",
            expiresAt = "2024-12-31T23:59:59Z"
        )
        
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(listOf(bank))
        coEvery { openBankingClient.createConsent(any()) } returns Result.success(consentResponse)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.connectToBank(bank)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertEquals("https://bank.com/auth", uiState.authorizationUrl)
        assertTrue(uiState.error == null)
        
        coVerify {
            openBankingClient.createConsent(
                withArg<ConsentRequest> { request ->
                    assertEquals(bank.id, request.bankId)
                }
            )
        }
    }

    @Test
    fun `should handle error when bank connection fails`() = runTest {
        // Given
        val bank = createMockBank("al-rajhi", "Al Rajhi Bank")
        val errorMessage = "Consent creation failed"
        
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(listOf(bank))
        coEvery { openBankingClient.createConsent(any()) } returns Result.failure(Exception(errorMessage))

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.connectToBank(bank)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isConnecting)
        assertTrue(uiState.authorizationUrl == null)
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `should complete authorization flow successfully`() = runTest {
        // Given
        val authorizationCode = "auth-code-123"
        val consentId = "consent-123"
        
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(emptyList())
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

    private fun createMockBank(id: String, name: String) = Bank(
        id = id,
        name = name,
        displayName = name,
        logoUrl = "https://example.com/logo.png",
        baseUrl = "https://api.$id.com.sa",
        supportedServices = listOf("accounts", "payments"),
        isActive = true
    )
}