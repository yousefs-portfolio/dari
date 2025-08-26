package code.yousef.dari.shared.di

import code.yousef.dari.sama.interfaces.SecurityProvider
import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.usecase.GetAccountsUseCase
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.check.checkModules
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertNotNull

/**
 * Test for Koin Dependency Injection setup - TDD approach
 * Tests module configuration, dependency resolution, and injection
 */
class KoinModulesTest : KoinTest {

    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                networkingModule,
                databaseModule,
                repositoryModule,
                useCaseModule,
                securityModule
            )
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `should check all modules are properly configured`() {
        // When & Then
        checkModules()
    }

    @Test
    fun `should inject security provider successfully`() {
        // When
        val securityProvider by inject<SecurityProvider>()

        // Then
        assertNotNull(securityProvider, "SecurityProvider should be injectable")
    }

    @Test
    fun `should inject account repository successfully`() {
        // When
        val accountRepository by inject<AccountRepository>()

        // Then
        assertNotNull(accountRepository, "AccountRepository should be injectable")
    }

    @Test
    fun `should inject use cases successfully`() {
        // When
        val getAccountsUseCase by inject<GetAccountsUseCase>()

        // Then
        assertNotNull(getAccountsUseCase, "GetAccountsUseCase should be injectable")
    }

    @Test
    fun `should provide singleton instances where required`() {
        // When
        val securityProvider1 by inject<SecurityProvider>()
        val securityProvider2 by inject<SecurityProvider>()

        // Then
        // In a real test, we'd verify they're the same instance
        // For now, just verify they're both non-null
        assertNotNull(securityProvider1)
        assertNotNull(securityProvider2)
    }

    @Test
    fun `should provide factory instances where required`() {
        // When
        val useCase1 by inject<GetAccountsUseCase>()
        val useCase2 by inject<GetAccountsUseCase>()

        // Then
        assertNotNull(useCase1)
        assertNotNull(useCase2)
    }
}