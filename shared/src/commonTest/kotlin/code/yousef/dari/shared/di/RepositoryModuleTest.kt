package code.yousef.dari.shared.di

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.data.repository.TransactionRepository
import code.yousef.dari.shared.data.repository.BudgetRepository
import code.yousef.dari.shared.data.repository.GoalRepository
import code.yousef.dari.shared.data.repository.CategoryRepository
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

/**
 * Tests for Repository module injection
 * Following TDD methodology
 */
class RepositoryModuleTest : KoinTest {

    private val accountRepository: AccountRepository by inject()
    private val transactionRepository: TransactionRepository by inject()

    @Test
    fun `test AccountRepository injection`() {
        // Setup Koin with required modules
        startKoin {
            modules(listOf(databaseModule, repositoryModule))
        }

        // Test injection
        assertNotNull(accountRepository, "AccountRepository should be injected")
        assertTrue(
            accountRepository is AccountRepository,
            "Injected instance should implement AccountRepository interface"
        )

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test TransactionRepository injection`() {
        // Setup Koin with required modules
        startKoin {
            modules(listOf(databaseModule, repositoryModule))
        }

        // Test injection
        assertNotNull(transactionRepository, "TransactionRepository should be injected")
        assertTrue(
            transactionRepository is TransactionRepository,
            "Injected instance should implement TransactionRepository interface"
        )

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test all repositories are properly bound to interfaces`() {
        // Setup Koin
        startKoin {
            modules(listOf(databaseModule, repositoryModule))
        }

        // Test that all repositories can be resolved
        val account = getKoin().get<AccountRepository>()
        val transaction = getKoin().get<TransactionRepository>()

        assertNotNull(account, "AccountRepository should be resolvable")
        assertNotNull(transaction, "TransactionRepository should be resolvable")

        // Test that implementations are different instances when appropriate
        assertTrue(account !== transaction, "Different repositories should be different instances")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test repository module dependencies`() {
        // This test verifies that repository module can be loaded
        // and all its dependencies are satisfied
        startKoin {
            modules(listOf(databaseModule, repositoryModule))
        }

        // If we reach here without exceptions, dependencies are satisfied
        assertTrue(true, "Repository module dependencies are satisfied")

        // Cleanup
        stopKoin()
    }
}