package code.yousef.dari.presentation.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.SyncAccountsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
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
class AccountsGroupingTest {

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
    fun `should group accounts by type correctly`() = runTest {
        // Given
        val accounts = listOf(
            createAccount("acc1", "Primary Checking", AccountType.CHECKING),
            createAccount("acc2", "Business Checking", AccountType.CHECKING),
            createAccount("acc3", "Emergency Savings", AccountType.SAVINGS),
            createAccount("acc4", "Main Credit Card", AccountType.CREDIT_CARD),
            createAccount("acc5", "Business Credit", AccountType.CREDIT_CARD),
            createAccount("acc6", "Investment Account", AccountType.INVESTMENT)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val groupedAccounts = viewModel.uiState.value.groupedAccounts

        assertEquals(4, groupedAccounts.keys.size) // 4 account types
        assertEquals(2, groupedAccounts[AccountType.CHECKING]?.size)
        assertEquals(1, groupedAccounts[AccountType.SAVINGS]?.size)
        assertEquals(2, groupedAccounts[AccountType.CREDIT_CARD]?.size)
        assertEquals(1, groupedAccounts[AccountType.INVESTMENT]?.size)
    }

    @Test
    fun `should maintain account order within groups`() = runTest {
        // Given - Accounts with specific order
        val accounts = listOf(
            createAccount("acc1", "Checking A", AccountType.CHECKING),
            createAccount("acc2", "Savings B", AccountType.SAVINGS),
            createAccount("acc3", "Checking C", AccountType.CHECKING),
            createAccount("acc4", "Savings D", AccountType.SAVINGS)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val groupedAccounts = viewModel.uiState.value.groupedAccounts
        
        val checkingAccounts = groupedAccounts[AccountType.CHECKING] ?: emptyList()
        assertEquals("Checking A", checkingAccounts[0].name)
        assertEquals("Checking C", checkingAccounts[1].name)

        val savingsAccounts = groupedAccounts[AccountType.SAVINGS] ?: emptyList()
        assertEquals("Savings B", savingsAccounts[0].name)
        assertEquals("Savings D", savingsAccounts[1].name)
    }

    @Test
    fun `should handle empty account groups`() = runTest {
        // Given - Only checking accounts, no savings or credit cards
        val accounts = listOf(
            createAccount("acc1", "Primary Checking", AccountType.CHECKING),
            createAccount("acc2", "Business Checking", AccountType.CHECKING)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val groupedAccounts = viewModel.uiState.value.groupedAccounts

        assertEquals(1, groupedAccounts.keys.size)
        assertEquals(2, groupedAccounts[AccountType.CHECKING]?.size)
        assertTrue(groupedAccounts[AccountType.SAVINGS] == null)
        assertTrue(groupedAccounts[AccountType.CREDIT_CARD] == null)
    }

    @Test
    fun `should categorize account types correctly based on balance`() = runTest {
        // Given
        val accounts = listOf(
            createAccount("acc1", "Positive Checking", AccountType.CHECKING, 5000.0),
            createAccount("acc2", "Negative Checking", AccountType.CHECKING, -100.0),
            createAccount("acc3", "High Savings", AccountType.SAVINGS, 50000.0),
            createAccount("acc4", "Credit Card Debt", AccountType.CREDIT_CARD, -5000.0),
            createAccount("acc5", "Investment Growth", AccountType.INVESTMENT, 25000.0)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val balanceSummary = viewModel.uiState.value.balanceSummary

        // Assets: 5000 + 0 + 50000 + 0 + 25000 = 80000 (positive balances only)
        // Liabilities: -100 + -5000 = -5100 (negative balances only)
        // Total: 80000 - 5100 = 74900

        assertEquals(80000.0, balanceSummary.totalAssets.toDouble())
        assertEquals(-5100.0, balanceSummary.totalLiabilities.toDouble())
        assertEquals(74900.0, balanceSummary.totalBalance.toDouble())
    }

    @Test
    fun `should support all Saudi banking account types`() = runTest {
        // Given - All possible account types
        val accounts = listOf(
            createAccount("acc1", "Current Account", AccountType.CHECKING),
            createAccount("acc2", "Savings Account", AccountType.SAVINGS),
            createAccount("acc3", "Credit Card", AccountType.CREDIT_CARD),
            createAccount("acc4", "Investment Account", AccountType.INVESTMENT),
            createAccount("acc5", "Loan Account", AccountType.LOAN),
            createAccount("acc6", "Mortgage Account", AccountType.MORTGAGE)
        )

        coEvery { getAccountsUseCase() } returns flowOf(accounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Then
        val groupedAccounts = viewModel.uiState.value.groupedAccounts

        assertEquals(6, groupedAccounts.keys.size)
        assertTrue(groupedAccounts.containsKey(AccountType.CHECKING))
        assertTrue(groupedAccounts.containsKey(AccountType.SAVINGS))
        assertTrue(groupedAccounts.containsKey(AccountType.CREDIT_CARD))
        assertTrue(groupedAccounts.containsKey(AccountType.INVESTMENT))
        assertTrue(groupedAccounts.containsKey(AccountType.LOAN))
        assertTrue(groupedAccounts.containsKey(AccountType.MORTGAGE))
    }

    @Test
    fun `should update groups when accounts change`() = runTest {
        // Given - Initial accounts
        val initialAccounts = listOf(
            createAccount("acc1", "Checking", AccountType.CHECKING)
        )
        
        val updatedAccounts = listOf(
            createAccount("acc1", "Checking", AccountType.CHECKING),
            createAccount("acc2", "New Savings", AccountType.SAVINGS),
            createAccount("acc3", "New Credit", AccountType.CREDIT_CARD)
        )

        coEvery { getAccountsUseCase() } returns flowOf(initialAccounts) andThen flowOf(updatedAccounts)

        // When
        viewModel = AccountsViewModel(getAccountsUseCase, syncAccountsUseCase, accountRepository)
        advanceUntilIdle()

        // Initial state
        var groupedAccounts = viewModel.uiState.value.groupedAccounts
        assertEquals(1, groupedAccounts.keys.size)

        // When accounts are updated (simulating sync or refresh)
        // The flow will emit updated accounts
        advanceUntilIdle()

        // Then
        groupedAccounts = viewModel.uiState.value.groupedAccounts
        assertEquals(3, groupedAccounts.keys.size)
        assertEquals(1, groupedAccounts[AccountType.CHECKING]?.size)
        assertEquals(1, groupedAccounts[AccountType.SAVINGS]?.size)
        assertEquals(1, groupedAccounts[AccountType.CREDIT_CARD]?.size)
    }

    private fun createAccount(
        id: String, 
        name: String, 
        type: AccountType,
        balance: Double = 0.0
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