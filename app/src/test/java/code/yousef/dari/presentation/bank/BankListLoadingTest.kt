package code.yousef.dari.presentation.bank

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.sama.sdk.OpenBankingClient
import code.yousef.dari.sama.sdk.models.Bank
import code.yousef.dari.shared.data.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BankListLoadingTest {

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
    fun `should load all supported Saudi banks`() = runTest {
        // Given
        val expectedBanks = getSaudiBanks()
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(expectedBanks)

        // When
        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(7, uiState.supportedBanks.size) // All 7 Saudi banks
        assertTrue(uiState.supportedBanks.any { it.id == "al-rajhi" })
        assertTrue(uiState.supportedBanks.any { it.id == "snb" })
        assertTrue(uiState.supportedBanks.any { it.id == "riyad" })
        assertTrue(uiState.supportedBanks.any { it.id == "sabb" })
        assertTrue(uiState.supportedBanks.any { it.id == "alinma" })
        assertTrue(uiState.supportedBanks.any { it.id == "albilad" })
        assertTrue(uiState.supportedBanks.any { it.id == "stc-pay" })
    }

    @Test
    fun `should filter banks correctly when searching`() = runTest {
        // Given
        val banks = getSaudiBank1s()
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When - Search for "national"
        viewModel.searchBanks("national")

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredBanks.size)
        assertEquals("snb", uiState.filteredBanks.first().id)
        assertEquals("Saudi National Bank", uiState.filteredBanks.first().name)
    }

    @Test
    fun `should return all banks when search query is empty`() = runTest {
        // Given
        val banks = getSaudiBanks()
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // Search with non-empty query first
        viewModel.searchBanks("rajhi")
        assertEquals(1, viewModel.uiState.value.filteredBanks.size)

        // When - Clear search
        viewModel.searchBanks("")

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(banks.size, uiState.filteredBanks.size)
        assertEquals("", uiState.searchQuery)
    }

    @Test
    fun `should handle case-insensitive search`() = runTest {
        // Given
        val banks = getSaudiBanks()
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When - Search with different cases
        viewModel.searchBanks("RAJHI")
        val upperCaseResult = viewModel.uiState.value.filteredBanks.size

        viewModel.searchBanks("rajhi")
        val lowerCaseResult = viewModel.uiState.value.filteredBanks.size

        viewModel.searchBanks("Rajhi")
        val titleCaseResult = viewModel.uiState.value.filteredBanks.size

        // Then
        assertEquals(1, upperCaseResult)
        assertEquals(1, lowerCaseResult)
        assertEquals(1, titleCaseResult)
    }

    @Test
    fun `should return empty list when no banks match search`() = runTest {
        // Given
        val banks = getSaudiBank1s()
        coEvery { openBankingClient.getSupportedBanks() } returns Result.success(banks)

        viewModel = BankConnectionViewModel(openBankingClient, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.searchBanks("nonexistent bank")

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.filteredBanks.isEmpty())
        assertEquals("nonexistent bank", uiState.searchQuery)
    }

    private fun getSaudiBanks() = listOf(
        Bank(
            id = "al-rajhi",
            name = "Al Rajhi Bank",
            displayName = "Al Rajhi Bank",
            logoUrl = "https://alrajhibank.com.sa/logo.png",
            baseUrl = "https://api.alrajhibank.com.sa",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        ),
        Bank(
            id = "snb",
            name = "Saudi National Bank",
            displayName = "SNB",
            logoUrl = "https://snb.com.sa/logo.png",
            baseUrl = "https://api.snb.com.sa",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        ),
        Bank(
            id = "riyad",
            name = "Riyad Bank",
            displayName = "Riyad Bank",
            logoUrl = "https://riyadbank.com/logo.png",
            baseUrl = "https://api.riyadbank.com",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        ),
        Bank(
            id = "sabb",
            name = "SABB",
            displayName = "Saudi British Bank",
            logoUrl = "https://sabb.com/logo.png",
            baseUrl = "https://api.sabb.com",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        ),
        Bank(
            id = "alinma",
            name = "Alinma Bank",
            displayName = "Alinma Bank",
            logoUrl = "https://alinma.com/logo.png",
            baseUrl = "https://api.alinma.com",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        ),
        Bank(
            id = "albilad",
            name = "Bank Albilad",
            displayName = "Bank Albilad",
            logoUrl = "https://albilad.com/logo.png",
            baseUrl = "https://api.albilad.com",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        ),
        Bank(
            id = "stc-pay",
            name = "STC Pay",
            displayName = "STC Pay",
            logoUrl = "https://stcpay.com.sa/logo.png",
            baseUrl = "https://api.stcpay.com.sa",
            supportedServices = listOf("accounts", "payments", "balances", "transactions"),
            isActive = true
        )
    )
}