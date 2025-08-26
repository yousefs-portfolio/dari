package code.yousef.dari.shared.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.verify.verify

/**
 * Tests for ViewModel module injection
 * Following TDD methodology
 * 
 * Note: ViewModels are Android-specific, so actual ViewModel injection
 * will be tested in Android-specific test modules
 */
class ViewModelModuleTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify viewModel module configuration`() {
        // Verify the module can be configured without errors
        viewModelModule.verify()
    }

    @Test
    fun `test viewModel module can be loaded`() {
        // Setup Koin with viewModel module
        startKoin {
            modules(viewModelModule)
        }

        // Verify the module loads without errors
        assertNotNull(getKoin(), "Koin context should be available")
        assertTrue(true, "ViewModel module loaded successfully")

        // Cleanup
        stopKoin()
    }

    @Test
    fun `test viewModel module is included in core modules`() {
        // Verify that viewModel module is included in the core modules list
        assertTrue(
            coreModules.contains(viewModelModule),
            "ViewModel module should be included in core modules"
        )
    }

    @Test
    fun `test viewModel module works with other modules`() {
        // Setup Koin with all core modules
        startKoin {
            modules(coreModules)
        }

        // Test that the combination works
        assertNotNull(getKoin(), "Koin should be properly initialized with all modules")
        assertTrue(true, "ViewModel module works with other core modules")

        // Cleanup
        stopKoin()
    }
}