package code.yousef.dari.shared.di

import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import code.yousef.dari.shared.domain.usecase.GetTransactionsUseCase
import code.yousef.dari.shared.domain.usecase.SyncAccountsUseCase
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

/**
 * Tests for Use Case module injection
 * Following TDD methodology
 */
class UseCaseModuleTest : KoinTest {

    private val getAccountsUseCase: GetAccountsUseCase by inject()
    private val getTransactionsUseCase: GetTransactionsUseCase by inject()
    private val syncAccountsUseCase: SyncAccountsUseCase by inject()

    @Test
    fun `test GetAccountsUseCase injection`() {
        // Setup Koin with all required modules
        startKoin {
            modules(listOf(databaseModule, repositoryModule, useCaseModule))
        }

        // Test injection
        assertNotNull(getAccountsUseCase, "GetAccountsUseCase should be injected")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test GetTransactionsUseCase injection`() {
        // Setup Koin with all required modules
        startKoin {
            modules(listOf(databaseModule, repositoryModule, useCaseModule))
        }

        // Test injection
        assertNotNull(getTransactionsUseCase, "GetTransactionsUseCase should be injected")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test SyncAccountsUseCase injection`() {
        // Setup Koin with all required modules
        startKoin {
            modules(listOf(databaseModule, repositoryModule, useCaseModule))
        }

        // Test injection
        assertNotNull(syncAccountsUseCase, "SyncAccountsUseCase should be injected")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test use cases are factory instances`() {
        // Setup Koin
        startKoin {
            modules(listOf(databaseModule, repositoryModule, useCaseModule))
        }

        // Get multiple instances to verify factory behavior
        val useCase1 = getKoin().get<GetAccountsUseCase>()
        val useCase2 = getKoin().get<GetAccountsUseCase>()

        assertNotNull(useCase1, "First instance should be created")
        assertNotNull(useCase2, "Second instance should be created")

        // For factory instances, each call creates a new instance
        // This test verifies the factory pattern is working
        assertTrue(useCase1 !== useCase2, "Factory should create new instances each time")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test all use cases can be resolved simultaneously`() {
        // Setup Koin
        startKoin {
            modules(listOf(databaseModule, repositoryModule, useCaseModule))
        }

        // Test that all use cases can be resolved at the same time
        val accounts = getKoin().get<GetAccountsUseCase>()
        val transactions = getKoin().get<GetTransactionsUseCase>()
        val sync = getKoin().get<SyncAccountsUseCase>()

        assertNotNull(accounts, "GetAccountsUseCase should be resolvable")
        assertNotNull(transactions, "GetTransactionsUseCase should be resolvable")
        assertNotNull(sync, "SyncAccountsUseCase should be resolvable")

        // Cleanup
        stopKoin()
    }
}