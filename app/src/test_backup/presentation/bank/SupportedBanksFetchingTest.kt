package code.yousef.dari.presentation.bank

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.sama.sdk.OpenBankingClient
import code.yousef.dari.sama.sdk.models.Bank
import code.yousef.dari.shared.data.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.mockk
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SupportedBanksFetchingTest {

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
    fun `should successfully fetch and display supported banks`() = runTest {
        // Given
        val banks = listOf(
            createBank("al-rajhi", "Al Rajhi Bank"),
            createBank("snb", "Saudi National Bank")
        )
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(2, uiState.supportedBanks.size)
        assertEquals(banks, uiState.supportedBanks)
        assertEquals(banks, uiState.filteredBanks) // Initially, filtered should equal all banks
        assertTrue(uiState.error == null)
    }

    @Test
    fun `should handle network error when fetching banks`() = runTest {
        // Given
        coEvery { openBankingClient.getSupportedBanks() } returns Result.failure(IOException("Network error"))

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.supportedBanks.isEmpty())
        assertTrue(uiState.filteredBanks.isEmpty())
        assertEquals("Network error", uiState.error)
    }

    @Test
    fun `should handle empty bank list response`() = runTest {
        // Given
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(emptyList())

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.supportedBanks.isEmpty())
        assertTrue(uiState.filteredBanks.isEmpty())
        assertTrue(uiState.error == null)
    }

    @Test
    fun `should show loading state initially`() = runTest {
        // Given
        coEvery { openBankingClient.getSupportedBanks() } coAnswers {
            // Simulate delay
            kotlinx.coroutines.delay(100)
            Result.success(emptyList())
        }

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)

        // Then - Check loading state before completion
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
        assertTrue(initialState.supportedBanks.isEmpty())

        // Wait for completion
        advanceUntilIdle()

        // Then - Check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
    }

    @Test
    fun `should retry loading banks when refresh is called`() = runTest {
        // Given - First call fails
        coEvery { openBankingClient.getSupportedBanks() } returns Result.failure(Exception("First failure"))

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Verify failure state
        assertTrue(viewModel.uiState.value.error != null)

        // Given - Second call succeeds
        val banks = listOf(createBank("al-rajhi", "Al Rajhi Bank"))
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(banks, uiState.supportedBanks)
        assertTrue(uiState.error == null)
    }

    @Test
    fun `should validate bank data integrity`() = runTest {
        // Given
        val banks = listOf(
            createBank("al-rajhi", "Al Rajhi Bank", "https://alrajhibank.com.sa/logo.png"),
            createBank("snb", "Saudi National Bank", "https://snb.com.sa/logo.png")
        )
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        val loadedBanks = uiState.supportedBanks

        // Validate first bank
        val alRajhi = loadedBanks.find { it.id == "al-rajhi" }
        assertNotNull(alRajhi)
        assertEquals("Al Rajhi Bank", alRajhi!!.name)
        assertEquals("https://alrajhibank.com.sa/logo.png", alRajhi.logoUrl)
        assertTrue(alRajhi.isActive)
        assertTrue(alRajhi.supportedServices.isNotEmpty())

        // Validate second bank
        val snb = loadedBanks.find { it.id == "snb" }
        assertNotNull(snb)
        assertEquals("Saudi National Bank", snb!!.name)
        assertEquals("https://snb.com.sa/logo.png", snb.logoUrl)
        assertTrue(snb.isActive)
        assertTrue(snb.supportedServices.isNotEmpty())
    }

    private fun createBank(
        id: String, 
        name: String, 
        logoUrl: String = "https://example.com/logo.png"
    ) = Bank(
        id = id,
        name = name,
        displayName = name,
        logoUrl = logoUrl,
        baseUrl = "https://api.$id.com.sa",
        supportedServices = listOf("accounts", "payments", "balances", "transactions"),
        isActive = true
    )
}