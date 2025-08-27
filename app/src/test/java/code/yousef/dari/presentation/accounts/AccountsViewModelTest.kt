package code.yousef.dari.presentation.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.SyncAccountsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getAccountsUseCase = mockk<GetAccountsUseCase>()
    private val syncAccountsUseCase = mockk<SyncAccountsUseCase>()
    private val accountRepository = mockk<AccountRepository>()

    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load and group accounts correctly`() = runTest {
        // Given
        val accounts = listOf(
            createMockAccount("acc1", "Checking Account", AccountType.CHECKING, 5000.0),
            createMockAccount("acc2", "Savings Account", AccountType.SAVINGS, 15000.0),
            createMockAccount("acc3", "Credit Card", AccountType.CREDIT_CARD, -2000.0),
            createMockAccount("acc4", "Another Checking", AccountType.CHECKING, 3000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(4, uiState.accounts.size)
        
        // Check grouping
        val groupedAccounts = uiState.groupedAccounts
        assertEquals(3, groupedAccounts.size) // 3 account types
        assertEquals(2, groupedAccounts[AccountType.CHECKING]?.size ?: 0)
        assertEquals(1, groupedAccounts[AccountType.SAVINGS]?.size ?: 0)
        assertEquals(1, groupedAccounts[AccountType.CREDIT_CARD]?.size ?: 0)
    }

    @Test
    fun `should calculate balance aggregation correctly`() = runTest {
        // Given
        val accounts = listOf(
            createMockAccount("acc1", "Checking", AccountType.CHECKING, 5000.0),
            createMockAccount("acc2", "Savings", AccountType.SAVINGS, 15000.0),
            createMockAccount("acc3", "Credit Card", AccountType.CREDIT_CARD, -2000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        val summary = uiState.balanceSummary
        
        assertEquals(18000.0, summary.totalBalance.toDouble()) // 5000 + 15000 - 2000
        assertEquals(20000.0, summary.totalAssets.toDouble()) // 5000 + 15000
        assertEquals(-2000.0, summary.totalLiabilities.toDouble()) // -2000
        assertEquals(3, summary.accountCount)
    }

    @Test
    fun `should filter accounts by type`() = runTest {
        // Given
        val accounts = listOf(
            createMockAccount("acc1", "Checking", AccountType.CHECKING, 5000.0),
            createMockAccount("acc2", "Savings", AccountType.SAVINGS, 15000.0),
            createMockAccount("acc3", "Credit Card", AccountType.CREDIT_CARD, -2000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.filterByAccountType(AccountType.CHECKING)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredAccounts.size)
        assertEquals(AccountType.CHECKING, uiState.filteredAccounts.first().type)
        assertEquals(AccountType.CHECKING, uiState.selectedAccountType)
    }

    @Test
    fun `should search accounts by name`() = runTest {
        // Given
        val accounts = listOf(
            createMockAccount("acc1", "Al Rajhi Checking", AccountType.CHECKING, 5000.0),
            createMockAccount("acc2", "SNB Savings", AccountType.SAVINGS, 15000.0),
            createMockAccount("acc3", "Riyad Credit Card", AccountType.CREDIT_CARD, -2000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.searchAccounts("rajhi")

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.filteredAccounts.size)
        assertEquals("Al Rajhi Checking", uiState.filteredAccounts.first().name)
        assertEquals("rajhi", uiState.searchQuery)
    }

    @Test
    fun `should sync accounts and update UI state`() = runTest {
        // Given
        val initialAccounts = listOf(
            createMockAccount("acc1", "Checking", AccountType.CHECKING, 5000.0)
        )
        val syncedAccounts = listOf(
            createMockAccount("acc1", "Checking", AccountType.CHECKING, 5500.0),
            createMockAccount("acc2", "New Savings", AccountType.SAVINGS, 10000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(initialAccounts) andThen flowOf(syncedAccounts)
        coEvery { syncAccountsUseCase() } returns Result.success(Unit)

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.syncAccounts()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isRefreshing)
        assertEquals(2, uiState.accounts.size)
        assertTrue(uiState.error == null)
        
        coVerify { syncAccountsUseCase() }
    }

    @Test
    fun `should handle sync error`() = runTest {
        // Given
        val accounts = listOf(
            createMockAccount("acc1", "Checking", AccountType.CHECKING, 5000.0)
        )
        val errorMessage = "Sync failed"

        coEvery { getAccountsUseCase() } returns flowOf(accounts)
        coEvery { syncAccountsUseCase() } returns Result.failure(Exception(errorMessage))

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.syncAccounts()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isRefreshing)
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `should disconnect account successfully`() = runTest {
        // Given
        val account = createMockAccount("acc1", "Test Account", AccountType.CHECKING, 5000.0)
        
        coEvery { getAccountsUseCase() } returns flowOf(listOf(account))
        coEvery { accountRepository.disconnectAccount(account.id) } returns Result.success(Unit)

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // When
        viewModel.disconnectAccount(account.id)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.error == null)
        
        coVerify { accountRepository.disconnectAccount(account.id) }
    }

    @Test
    fun `should show and hide account details`() = runTest {
        // Given
        val account = createMockAccount("acc1", "Test Account", AccountType.CHECKING, 5000.0)
        
        coEvery { getAccountsUseCase() } returns flowOf(listOf(account))

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // When - Show details
        viewModel.showAccountDetails(account.id)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(account.id, uiState.selectedAccountId)

        // When - Hide details
        viewModel.hideAccountDetails()

        // Then
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.selectedAccountId == null)
    }

    @Test
    fun `should clear all filters when requested`() = runTest {
        // Given
        val accounts = listOf(
            createMockAccount("acc1", "Checking", AccountType.CHECKING, 5000.0),
            createMockAccount("acc2", "Savings", AccountType.SAVINGS, 15000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Apply filters
        viewModel.filterByAccountType(AccountType.CHECKING)
        viewModel.searchAccounts("test")

        // When
        viewModel.clearFilters()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(accounts.size, uiState.filteredAccounts.size)
        assertEquals("", uiState.searchQuery)
        assertTrue(uiState.selectedAccountType == null)
    }

    private fun createMockAccount(
        id: String,
        name: String,
        type: AccountType,
        balance: Double
    ) = FinancialAccount(
        id = id,
        name = name,
        type = type,
        bankId = "test-bank",
        accountNumber = "****1234",
        currency = "SAR",
        currentBalance = Money.fromDouble(balance, "SAR"),
        availableBalance = Money.fromDouble(balance, "SAR"),
        isActive = true,
        lastSyncTime = Clock.System.now(),
        consentId = "consent-123",
        metadata = mapOf("bankName" to "Test Bank")
    )
}