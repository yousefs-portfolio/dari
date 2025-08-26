package code.yousef.dari.shared.di

import code.yousef.dari.sama.interfaces.OpenBankingClient
import code.yousef.dari.sama.interfaces.SecurityProvider
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

/**
 * Tests for SAMA Banking SDK module injection
 * Following TDD methodology
 */
class SDKModuleTest : KoinTest {

    private val securityProvider: SecurityProvider by inject()
    private val openBankingClient: OpenBankingClient by inject()

    @Test
    fun `test SecurityProvider injection`() {
        // Setup Koin
        startKoin {
            modules(securityModule)
        }

        // Test injection
        assertNotNull(securityProvider, "SecurityProvider should be injected")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test OpenBankingClient injection`() {
        // Setup Koin
        startKoin {
            modules(securityModule)
        }

        // Test injection
        assertNotNull(openBankingClient, "OpenBankingClient should be injected")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test SDK module provides required dependencies`() {
        // Setup Koin
        startKoin {
            modules(securityModule)
        }

        // Verify all SDK dependencies are available
        val provider = getKoin().get<SecurityProvider>()
        val client = getKoin().get<OpenBankingClient>()

        assertNotNull(provider, "SecurityProvider should be available")
        assertNotNull(client, "OpenBankingClient should be available")

        // Cleanup
        stopKoin()
    }
}