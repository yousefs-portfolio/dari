package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SearchTransactionsUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val searchTransactionsUseCase = SearchTransactionsUseCase(
        transactionRepository = transactionRepository
    )

    private val sampleTransactions = listOf(
        Transaction(
            id = "txn_1",
            accountId = "acc_123",
            amount = Money.sar(-15000), // -150 SAR
            type = TransactionType.DEBIT,
            description = "Al Danube Supermarket",
            merchantName = "Al Danube",
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            category = "Groceries",
            reference = "REF001"
        ),
        Transaction(
            id = "txn_2",
            accountId = "acc_123",
            amount = Money.sar(-8000), // -80 SAR
            type = TransactionType.DEBIT,
            description = "ADNOC Fuel Station",
            merchantName = "ADNOC",
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            category = "Fuel",
            reference = "REF002"
        ),
        Transaction(
            id = "txn_3",
            accountId = "acc_123",
            amount = Money.sar(250000), // +2,500 SAR
            type = TransactionType.CREDIT,
            description = "Salary Payment",
            merchantName = "Employer ABC",
            date = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            category = "Income",
            reference = "SAL001"
        )
    )

    @Test
    fun `should search transactions by query string`() = runTest {
        // Given
        val accountId = "acc_123"
        val query = "Danube"
        val expectedTransactions = listOf(sampleTransactions[0])

        coEvery { 
            transactionRepository.searchTransactions(accountId, query, any(), any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase(accountId, query)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchTransactions(accountId, query, any(), any(), any()) }
    }

    @Test
    fun `should search transactions by category`() = runTest {
        // Given
        val accountId = "acc_123"
        val category = "Groceries"
        val expectedTransactions = listOf(sampleTransactions[0])

        coEvery { 
            transactionRepository.searchByCategory(accountId, category, any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.searchByCategory(accountId, category)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchByCategory(accountId, category, any(), any()) }
    }

    @Test
    fun `should search transactions by amount range`() = runTest {
        // Given
        val accountId = "acc_123"
        val minAmount = Money.sar(5000)  // 50 SAR
        val maxAmount = Money.sar(20000) // 200 SAR
        val expectedTransactions = listOf(sampleTransactions[0], sampleTransactions[1])

        coEvery { 
            transactionRepository.searchByAmountRange(accountId, minAmount, maxAmount, any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.searchByAmountRange(accountId, minAmount, maxAmount)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchByAmountRange(accountId, minAmount, maxAmount, any(), any()) }
    }

    @Test
    fun `should search transactions by date range`() = runTest {
        // Given
        val accountId = "acc_123"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val expectedTransactions = sampleTransactions

        coEvery { 
            transactionRepository.searchByDateRange(accountId, startDate, endDate, any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.searchByDateRange(accountId, startDate, endDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchByDateRange(accountId, startDate, endDate, any(), any()) }
    }

    @Test
    fun `should search transactions by type`() = runTest {
        // Given
        val accountId = "acc_123"
        val transactionType = TransactionType.DEBIT
        val expectedTransactions = listOf(sampleTransactions[0], sampleTransactions[1])

        coEvery { 
            transactionRepository.searchByType(accountId, transactionType, any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.searchByType(accountId, transactionType)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchByType(accountId, transactionType, any(), any()) }
    }

    @Test
    fun `should search transactions by merchant`() = runTest {
        // Given
        val accountId = "acc_123"
        val merchantName = "Al Danube"
        val expectedTransactions = listOf(sampleTransactions[0])

        coEvery { 
            transactionRepository.searchByMerchant(accountId, merchantName, any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.searchByMerchant(accountId, merchantName)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchByMerchant(accountId, merchantName, any(), any()) }
    }

    @Test
    fun `should perform advanced search with multiple filters`() = runTest {
        // Given
        val accountId = "acc_123"
        val filters = SearchFilters(
            query = "ADNOC",
            categories = listOf("Fuel"),
            types = listOf(TransactionType.DEBIT),
            minAmount = Money.sar(5000),
            maxAmount = Money.sar(15000),
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 12, 31)
        )
        val expectedTransactions = listOf(sampleTransactions[1])

        coEvery { 
            transactionRepository.advancedSearch(accountId, filters, any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.advancedSearch(accountId, filters)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.advancedSearch(accountId, filters, any(), any()) }
    }

    @Test
    fun `should handle empty search results`() = runTest {
        // Given
        val accountId = "acc_123"
        val query = "NonExistentMerchant"

        coEvery { 
            transactionRepository.searchTransactions(accountId, query, any(), any(), any()) 
        } returns emptyList()

        // When
        val result = searchTransactionsUseCase(accountId, query)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
        
        coVerify { transactionRepository.searchTransactions(accountId, query, any(), any(), any()) }
    }

    @Test
    fun `should handle search errors gracefully`() = runTest {
        // Given
        val accountId = "acc_123"
        val query = "Test"
        val searchError = Exception("Database search failed")

        coEvery { 
            transactionRepository.searchTransactions(accountId, query, any(), any(), any()) 
        } throws searchError

        // When
        val result = searchTransactionsUseCase(accountId, query)

        // Then
        assertTrue(result.isFailure)
        assertEquals(searchError, result.exceptionOrNull())
        
        coVerify { transactionRepository.searchTransactions(accountId, query, any(), any(), any()) }
    }

    @Test
    fun `should validate search parameters`() = runTest {
        // Given
        val emptyAccountId = ""
        val query = "Test"

        // When
        val result = searchTransactionsUseCase(emptyAccountId, query)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid") == true)
        
        coVerify(exactly = 0) { transactionRepository.searchTransactions(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should search transactions with pagination`() = runTest {
        // Given
        val accountId = "acc_123"
        val query = "Transaction"
        val limit = 10
        val offset = 0

        coEvery { 
            transactionRepository.searchTransactions(accountId, query, limit, offset, any()) 
        } returns sampleTransactions

        // When
        val result = searchTransactionsUseCase.searchWithPagination(accountId, query, limit, offset)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(sampleTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchTransactions(accountId, query, limit, offset, any()) }
    }

    @Test
    fun `should search across multiple accounts`() = runTest {
        // Given
        val accountIds = listOf("acc_123", "acc_456")
        val query = "Danube"
        val expectedResults = mapOf(
            "acc_123" to listOf(sampleTransactions[0]),
            "acc_456" to emptyList<Transaction>()
        )

        accountIds.forEach { accountId ->
            val expectedForAccount = expectedResults[accountId] ?: emptyList()
            coEvery { 
                transactionRepository.searchTransactions(accountId, query, any(), any(), any()) 
            } returns expectedForAccount
        }

        // When
        val result = searchTransactionsUseCase.searchMultipleAccounts(accountIds, query)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedResults, result.getOrNull())
        
        accountIds.forEach { accountId ->
            coVerify { transactionRepository.searchTransactions(accountId, query, any(), any(), any()) }
        }
    }

    @Test
    fun `should search transactions by reference number`() = runTest {
        // Given
        val accountId = "acc_123"
        val reference = "REF001"
        val expectedTransactions = listOf(sampleTransactions[0])

        coEvery { 
            transactionRepository.searchByReference(accountId, reference) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.searchByReference(accountId, reference)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchByReference(accountId, reference) }
    }

    @Test
    fun `should search similar transactions based on pattern`() = runTest {
        // Given
        val baseTransactionId = "txn_1"
        val similarityThreshold = 0.8
        val expectedTransactions = listOf(sampleTransactions[0])

        coEvery { 
            transactionRepository.findSimilarTransactions(baseTransactionId, similarityThreshold) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase.findSimilarTransactions(baseTransactionId, similarityThreshold)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.findSimilarTransactions(baseTransactionId, similarityThreshold) }
    }

    @Test
    fun `should handle Saudi-specific search features`() = runTest {
        // Given
        val accountId = "acc_123"
        val arabicQuery = "دانوب" // Danube in Arabic
        val expectedTransactions = listOf(sampleTransactions[0])

        coEvery { 
            transactionRepository.searchTransactions(accountId, arabicQuery, any(), any(), any()) 
        } returns expectedTransactions

        // When
        val result = searchTransactionsUseCase(accountId, arabicQuery)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTransactions, result.getOrNull())
        
        coVerify { transactionRepository.searchTransactions(accountId, arabicQuery, any(), any(), any()) }
    }

    @Test
    fun `should search transactions with Islamic category filters`() = runTest {
        // Given
        val accountId = "acc_123"
        val islamicCategories = listOf("Zakat", "Charity", "زكاة", "صدقة")
        val filters = SearchFilters(categories = islamicCategories)

        coEvery { 
            transactionRepository.advancedSearch(accountId, filters, any(), any()) 
        } returns sampleTransactions

        // When
        val result = searchTransactionsUseCase.advancedSearch(accountId, filters)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(sampleTransactions, result.getOrNull())
        
        coVerify { transactionRepository.advancedSearch(accountId, filters, any(), any()) }
    }
}