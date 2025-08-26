package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetAccountDetailsUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val getAccountDetailsUseCase = GetAccountDetailsUseCase(
        accountRepository = accountRepository,
        transactionRepository = transactionRepository
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

    private val sampleTransactions = listOf(
        Transaction(
            id = "txn_1",
            accountId = "acc_123",
            amount = Money.sar(-5000), // -50 SAR (debit)
            type = TransactionType.DEBIT,
            description = "Supermarket Purchase",
            merchantName = "Al Danube",
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            category = "Groceries"
        ),
        Transaction(
            id = "txn_2",
            accountId = "acc_123",
            amount = Money.sar(100000), // +1,000 SAR (credit)
            type = TransactionType.CREDIT,
            description = "Salary Deposit",
            merchantName = "Company XYZ",
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            category = "Income"
        )
    )

    @Test
    fun `should get complete account details successfully`() = runTest {
        // Given
        val accountId = "acc_123"
        val expectedStats = AccountStats(
            totalTransactions = 2,
            totalSpent = Money.sar(5000),
            totalReceived = Money.sar(100000),
            averageTransaction = Money.sar(47500),
            mostFrequentCategory = "Groceries"
        )

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            transactionRepository.getRecentTransactions(accountId, 10) 
        } returns sampleTransactions
        
        coEvery { 
            accountRepository.getAccountStats(accountId) 
        } returns expectedStats

        // When
        val result = getAccountDetailsUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()
        assertNotNull(details)
        assertEquals(sampleAccount, details.account)
        assertEquals(sampleTransactions, details.recentTransactions)
        assertEquals(expectedStats, details.stats)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { transactionRepository.getRecentTransactions(accountId, 10) }
        coVerify { accountRepository.getAccountStats(accountId) }
    }

    @Test
    fun `should fail when account not found`() = runTest {
        // Given
        val invalidAccountId = "invalid_acc"

        coEvery { 
            accountRepository.getAccountById(invalidAccountId) 
        } returns null

        // When
        val result = getAccountDetailsUseCase(invalidAccountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
        
        coVerify { accountRepository.getAccountById(invalidAccountId) }
        coVerify(exactly = 0) { transactionRepository.getRecentTransactions(any(), any()) }
        coVerify(exactly = 0) { accountRepository.getAccountStats(any()) }
    }

    @Test
    fun `should handle inactive account details request`() = runTest {
        // Given
        val inactiveAccount = sampleAccount.copy(isActive = false)
        val accountId = "acc_123"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns inactiveAccount
        
        coEvery { 
            transactionRepository.getRecentTransactions(accountId, 10) 
        } returns sampleTransactions
        
        coEvery { 
            accountRepository.getAccountStats(accountId) 
        } returns AccountStats()

        // When
        val result = getAccountDetailsUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()
        assertNotNull(details)
        assertEquals(inactiveAccount, details.account)
        assertFalse(details.account.isActive)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { transactionRepository.getRecentTransactions(accountId, 10) }
        coVerify { accountRepository.getAccountStats(accountId) }
    }

    @Test
    fun `should get detailed account information with custom transaction limit`() = runTest {
        // Given
        val accountId = "acc_123"
        val customLimit = 25

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            transactionRepository.getRecentTransactions(accountId, customLimit) 
        } returns sampleTransactions
        
        coEvery { 
            accountRepository.getAccountStats(accountId) 
        } returns AccountStats()

        // When
        val result = getAccountDetailsUseCase.getDetailedInfo(accountId, customLimit)

        // Then
        assertTrue(result.isSuccess)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { transactionRepository.getRecentTransactions(accountId, customLimit) }
        coVerify { accountRepository.getAccountStats(accountId) }
    }

    @Test
    fun `should handle error when transactions cannot be retrieved`() = runTest {
        // Given
        val accountId = "acc_123"
        val transactionError = Exception("Transaction service unavailable")

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            transactionRepository.getRecentTransactions(accountId, 10) 
        } throws transactionError
        
        coEvery { 
            accountRepository.getAccountStats(accountId) 
        } returns AccountStats()

        // When
        val result = getAccountDetailsUseCase(accountId)

        // Then
        assertTrue(result.isSuccess) // Should still succeed with empty transactions
        val details = result.getOrNull()
        assertNotNull(details)
        assertEquals(sampleAccount, details.account)
        assertTrue(details.recentTransactions.isEmpty())
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { transactionRepository.getRecentTransactions(accountId, 10) }
        coVerify { accountRepository.getAccountStats(accountId) }
    }

    @Test
    fun `should get account summary without detailed statistics`() = runTest {
        // Given
        val accountId = "acc_123"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount

        // When
        val result = getAccountDetailsUseCase.getAccountSummary(accountId)

        // Then
        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals(sampleAccount.accountId, summary.accountId)
        assertEquals(sampleAccount.accountName, summary.accountName)
        assertEquals(sampleAccount.balance, summary.balance)
        assertEquals(sampleAccount.bankCode, summary.bankCode)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify(exactly = 0) { transactionRepository.getRecentTransactions(any(), any()) }
        coVerify(exactly = 0) { accountRepository.getAccountStats(any()) }
    }

    @Test
    fun `should validate account ID before retrieval`() = runTest {
        // Given
        val emptyAccountId = ""

        // When
        val result = getAccountDetailsUseCase(emptyAccountId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { accountRepository.getAccountById(any()) }
        coVerify(exactly = 0) { transactionRepository.getRecentTransactions(any(), any()) }
        coVerify(exactly = 0) { accountRepository.getAccountStats(any()) }
    }

    @Test
    fun `should get account details with spending analysis`() = runTest {
        // Given
        val accountId = "acc_123"
        val spendingAnalysis = SpendingAnalysis(
            categoryBreakdown = mapOf(
                "Groceries" to Money.sar(15000),
                "Fuel" to Money.sar(8000),
                "Dining" to Money.sar(6000)
            ),
            monthlyTrend = listOf(
                Money.sar(25000), // Last month
                Money.sar(29000), // Current month
            ),
            averageDailySpending = Money.sar(1000)
        )

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns sampleAccount
        
        coEvery { 
            transactionRepository.getRecentTransactions(accountId, 10) 
        } returns sampleTransactions
        
        coEvery { 
            accountRepository.getAccountStats(accountId) 
        } returns AccountStats()
        
        coEvery { 
            transactionRepository.getSpendingAnalysis(accountId, 30) 
        } returns spendingAnalysis

        // When
        val result = getAccountDetailsUseCase.getDetailedInfoWithAnalysis(accountId)

        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()
        assertNotNull(details)
        assertEquals(spendingAnalysis, details.spendingAnalysis)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { transactionRepository.getRecentTransactions(accountId, 10) }
        coVerify { accountRepository.getAccountStats(accountId) }
        coVerify { transactionRepository.getSpendingAnalysis(accountId, 30) }
    }

    @Test
    fun `should handle Saudi-specific account details`() = runTest {
        // Given
        val saudiAccount = sampleAccount.copy(
            bankCode = "RIBLSARI", // Riyad Bank
            accountName = "حساب جاري", // Current account in Arabic
            iban = "SA0380000123456789101112" // Saudi IBAN format
        )
        val accountId = "acc_saudi"

        coEvery { 
            accountRepository.getAccountById(accountId) 
        } returns saudiAccount
        
        coEvery { 
            transactionRepository.getRecentTransactions(accountId, 10) 
        } returns sampleTransactions
        
        coEvery { 
            accountRepository.getAccountStats(accountId) 
        } returns AccountStats()

        // When
        val result = getAccountDetailsUseCase(accountId)

        // Then
        assertTrue(result.isSuccess)
        val details = result.getOrNull()
        assertNotNull(details)
        assertEquals("حساب جاري", details.account.accountName)
        assertEquals("RIBLSARI", details.account.bankCode)
        assertTrue(details.account.iban.startsWith("SA"))
        assertEquals("SAR", details.account.currency)
        
        coVerify { accountRepository.getAccountById(accountId) }
        coVerify { transactionRepository.getRecentTransactions(accountId, 10) }
        coVerify { accountRepository.getAccountStats(accountId) }
    }
}