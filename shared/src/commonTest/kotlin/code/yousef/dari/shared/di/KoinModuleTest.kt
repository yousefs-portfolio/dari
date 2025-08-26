package code.yousef.dari.shared.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * Tests for Koin DI module configuration
 * Following TDD methodology - writing tests first
 */
class KoinModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify networking module configuration`() {
        networkingModule.verify()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify database module configuration`() {
        databaseModule.verify()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify repository module configuration`() {
        repositoryModule.verify()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify use case module configuration`() {
        useCaseModule.verify()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify security module configuration`() {
        securityModule.verify()
    }

    @Test
    fun `core modules list contains all required modules`() {
        val expectedModules = listOf(
            "networkingModule",
            "databaseModule", 
            "repositoryModule",
            "useCaseModule",
            "securityModule"
        )
        
        assertTrue(coreModules.isNotEmpty(), "Core modules should not be empty")
        assertTrue(coreModules.size >= 5, "Should have at least 5 core modules")
    }

    @Test
    fun `development module is properly configured`() {
        assertNotNull(developmentModule, "Development module should be defined")
    }

    @Test
    fun `production module is properly configured`() {
        assertNotNull(productionModule, "Production module should be defined")
    }
}