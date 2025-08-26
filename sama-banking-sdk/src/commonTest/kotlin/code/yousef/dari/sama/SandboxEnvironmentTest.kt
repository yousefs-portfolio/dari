package code.yousef.dari.sama

import code.yousef.dari.sama.implementation.BankConfigurationRegistry
import code.yousef.dari.sama.models.BankConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Test for SAMA Sandbox Environment Configuration - TDD approach
 * Tests configuration and validation of sandbox environments for all supported banks
 */
class SandboxEnvironmentTest {

    private val bankRegistry = BankConfigurationRegistry()

    @Test
    fun `should have sandbox configuration for all banks`() {
        // When
        val allBanks = bankRegistry.getAllBanks(environment = "production")
        
        // Then - Every production bank should have sandbox equivalent
        allBanks.forEach { prodConfig ->
            val sandboxConfig = bankRegistry.getBankConfiguration(prodConfig.bankCode, environment = "sandbox")
            assertNotNull(sandboxConfig, "${prodConfig.bankName} should have sandbox configuration")
            assertEquals("sandbox", sandboxConfig.environment)
            assertEquals(prodConfig.bankCode, sandboxConfig.bankCode)
            assertEquals(prodConfig.bankName, sandboxConfig.bankName)
        }
    }

    @Test
    fun `should validate sandbox URL patterns`() {
        // When
        val sandboxBanks = bankRegistry.getAllBanks(environment = "sandbox")
        
        // Then
        sandboxBanks.forEach { config ->
            assertTrue(
                config.baseUrl.contains("sandbox") || 
                config.baseUrl.contains("test") || 
                config.baseUrl.contains("dev"),
                "${config.bankName} sandbox URL should indicate test environment: ${config.baseUrl}"
            )
            
            assertTrue(
                config.authorizationEndpoint.contains("sandbox") || 
                config.authorizationEndpoint.contains("test") || 
                config.authorizationEndpoint.contains("dev"),
                "${config.bankName} sandbox auth endpoint should indicate test environment"
            )
            
            assertTrue(
                config.tokenEndpoint.contains("sandbox") || 
                config.tokenEndpoint.contains("test") || 
                config.tokenEndpoint.contains("dev"),
                "${config.bankName} sandbox token endpoint should indicate test environment"
            )
            
            assertTrue(
                config.parEndpoint.contains("sandbox") || 
                config.parEndpoint.contains("test") || 
                config.parEndpoint.contains("dev"),
                "${config.bankName} sandbox PAR endpoint should indicate test environment"
            )
        }
    }

    @Test
    fun `should have sandbox-specific client IDs`() {
        // When
        val allBanks = bankRegistry.getAllBanks(environment = "production")
        
        // Then
        allBanks.forEach { prodConfig ->
            val sandboxConfig = bankRegistry.getBankConfiguration(prodConfig.bankCode, environment = "sandbox")
            assertNotNull(sandboxConfig)
            
            // Client IDs should be different between environments
            assertTrue(
                prodConfig.clientId != sandboxConfig.clientId,
                "${prodConfig.bankName} should have different client IDs for prod and sandbox"
            )
            
            assertTrue(
                sandboxConfig.clientId.contains("sandbox") || sandboxConfig.clientId.contains("test"),
                "${prodConfig.bankName} sandbox client ID should indicate test environment"
            )
        }
    }

    @Test
    fun `should have appropriate rate limits for sandbox`() {
        // When
        val sandboxBanks = bankRegistry.getAllBanks(environment = "sandbox")
        
        // Then
        sandboxBanks.forEach { config ->
            // Sandbox should have reasonable rate limits for testing
            assertTrue(
                config.rateLimits.requestsPerMinute >= 60,
                "${config.bankName} sandbox should allow at least 60 requests per minute for testing"
            )
            
            assertTrue(
                config.rateLimits.requestsPerHour >= 1000,
                "${config.bankName} sandbox should allow at least 1000 requests per hour for testing"
            )
        }
    }

    @Test
    fun `should have contact info for sandbox support`() {
        // When
        val sandboxBanks = bankRegistry.getAllBanks(environment = "sandbox")
        
        // Then
        sandboxBanks.forEach { config ->
            assertNotNull(config.contactInfo, "${config.bankName} should have contact info for sandbox")
            
            val contactInfo = config.contactInfo!!
            assertTrue(
                contactInfo.supportEmail.contains("sandbox") || 
                contactInfo.supportEmail.contains("test") || 
                contactInfo.supportEmail.contains("dev"),
                "${config.bankName} should have sandbox-specific support email"
            )
            
            assertTrue(
                contactInfo.website.contains("developer") || 
                contactInfo.website.contains("sandbox") || 
                contactInfo.website.contains("test"),
                "${config.bankName} should have developer/sandbox website"
            )
        }
    }

    @Test
    fun `should validate Al Rajhi Bank sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("alrajhi", environment = "sandbox")
        
        // Then
        assertNotNull(config, "Al Rajhi Bank sandbox configuration should be available")
        assertEquals("alrajhi", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox-api.alrajhibank.com.sa"))
        assertTrue(config.authorizationEndpoint.contains("sandbox-identity.alrajhibank.com.sa"))
        assertTrue(config.clientId.contains("sandbox"))
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
        assertFalse(config.certificateFingerprints.isEmpty())
    }

    @Test
    fun `should validate SNB sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("snb", environment = "sandbox")
        
        // Then
        assertNotNull(config, "SNB sandbox configuration should be available")
        assertEquals("snb", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox.alahli.com"))
        assertTrue(config.authorizationEndpoint.contains("sandbox-identity.alahli.com"))
        assertTrue(config.clientId.contains("sandbox"))
        assertTrue(config.supportedScopes.contains("standing-orders"))
        assertTrue(config.features.supportsBulkPayments)
    }

    @Test
    fun `should validate Riyad Bank sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("riyadbank", environment = "sandbox")
        
        // Then
        assertNotNull(config, "Riyad Bank sandbox configuration should be available")
        assertEquals("riyadbank", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox-api.riyadbank.com"))
        assertTrue(config.supportedScopes.contains("direct-debits"))
    }

    @Test
    fun `should validate SABB sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("sabb", environment = "sandbox")
        
        // Then
        assertNotNull(config, "SABB sandbox configuration should be available")
        assertEquals("sabb", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox-api.sabb.com"))
        assertTrue(config.features.supportsInternationalPayments)
        assertTrue(config.features.supportedCurrencies.contains("USD"))
    }

    @Test
    fun `should validate Alinma Bank sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("alinma", environment = "sandbox")
        
        // Then
        assertNotNull(config, "Alinma Bank sandbox configuration should be available")
        assertEquals("alinma", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox-api.alinma.com"))
    }

    @Test
    fun `should validate Bank Albilad sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("albilad", environment = "sandbox")
        
        // Then
        assertNotNull(config, "Bank Albilad sandbox configuration should be available")
        assertEquals("albilad", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox-api.bankalbilad.com"))
    }

    @Test
    fun `should validate STC Pay sandbox configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("stcpay", environment = "sandbox")
        
        // Then
        assertNotNull(config, "STC Pay sandbox configuration should be available")
        assertEquals("stcpay", config.bankCode)
        assertEquals("sandbox", config.environment)
        assertTrue(config.baseUrl.contains("sandbox-api.stcpay.com.sa"))
        assertEquals("50000.00", config.features.maxTransactionAmount)
        assertTrue(config.features.supportedPaymentMethods.contains("DIGITAL_WALLET"))
    }

    @Test
    fun `should provide environment switching functionality`() {
        // Given
        val bankCode = "alrajhi"
        
        // When
        val prodConfig = bankRegistry.getBankConfiguration(bankCode, "production")
        val sandboxConfig = bankRegistry.getBankConfiguration(bankCode, "sandbox")
        
        // Then
        assertNotNull(prodConfig)
        assertNotNull(sandboxConfig)
        
        // Should be different configurations
        assertTrue(prodConfig.baseUrl != sandboxConfig.baseUrl)
        assertTrue(prodConfig.clientId != sandboxConfig.clientId)
        assertTrue(prodConfig.authorizationEndpoint != sandboxConfig.authorizationEndpoint)
        assertTrue(prodConfig.tokenEndpoint != sandboxConfig.tokenEndpoint)
        assertTrue(prodConfig.parEndpoint != sandboxConfig.parEndpoint)
        
        // But same bank code and name
        assertEquals(prodConfig.bankCode, sandboxConfig.bankCode)
        assertEquals(prodConfig.bankName, sandboxConfig.bankName)
        assertEquals(prodConfig.bankNameAr, sandboxConfig.bankNameAr)
    }

    @Test
    fun `should support development environment`() {
        // When
        val supportedEnvironments = bankRegistry.getSupportedEnvironments("alrajhi")
        
        // Then
        assertTrue(supportedEnvironments.contains("production"))
        assertTrue(supportedEnvironments.contains("sandbox"))
        
        // Development environment should be configurable
        assertTrue(supportedEnvironments.size >= 2)
    }

    @Test
    fun `should validate sandbox certificate pinning`() {
        // When
        val sandboxBanks = bankRegistry.getAllBanks(environment = "sandbox")
        
        // Then
        sandboxBanks.forEach { config ->
            assertFalse(
                config.certificateFingerprints.isEmpty(),
                "${config.bankName} sandbox should have certificate fingerprints configured"
            )
            
            // Sandbox certificates might be different from production
            config.certificateFingerprints.forEach { fingerprint ->
                assertTrue(
                    fingerprint.matches(Regex("[a-fA-F0-9:]{47,95}")),
                    "${config.bankName} sandbox certificate fingerprint should be valid hex format: $fingerprint"
                )
            }
        }
    }

    @Test
    fun `should validate sandbox feature flags`() {
        // When
        val sandboxBanks = bankRegistry.getAllBanks(environment = "sandbox")
        
        // Then
        sandboxBanks.forEach { config ->
            // Sandbox should support all basic features for testing
            assertTrue(
                config.features.supportsPayments,
                "${config.bankName} sandbox should support payments for testing"
            )
            
            assertTrue(
                config.features.supportsScheduledPayments,
                "${config.bankName} sandbox should support scheduled payments for testing"
            )
            
            assertTrue(
                config.features.supportsStandingOrders,
                "${config.bankName} sandbox should support standing orders for testing"
            )
            
            assertTrue(
                config.features.supportsDirectDebits,
                "${config.bankName} sandbox should support direct debits for testing"
            )
            
            assertTrue(
                config.features.supportsStatements,
                "${config.bankName} sandbox should support statements for testing"
            )
        }
    }
}