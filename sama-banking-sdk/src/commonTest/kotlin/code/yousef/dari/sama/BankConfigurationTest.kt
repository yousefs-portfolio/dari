package code.yousef.dari.sama

import code.yousef.dari.sama.models.BankConfiguration
import code.yousef.dari.sama.implementation.BankConfigurationRegistry
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test for Bank Configuration Registry - TDD approach
 * Tests loading and accessing configurations for all 7 Saudi banks
 */
class BankConfigurationTest {

    private val bankRegistry = BankConfigurationRegistry()

    @Test
    fun `should load Al Rajhi Bank configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("alrajhi")

        // Then
        assertNotNull(config, "Al Rajhi Bank configuration should be available")
        assertEquals("alrajhi", config.bankCode)
        assertEquals("Al Rajhi Bank", config.bankName)
        assertEquals("https://api.alrajhibank.com.sa/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
        assertNotNull(config.clientId, "Client ID should be configured")
        assertNotNull(config.certificateFingerprints, "Certificate fingerprints should be configured")
        assertTrue(config.certificateFingerprints.isNotEmpty(), "Should have certificate fingerprints")
    }

    @Test
    fun `should load Saudi National Bank configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("snb")

        // Then
        assertNotNull(config, "SNB configuration should be available")
        assertEquals("snb", config.bankCode)
        assertEquals("Saudi National Bank", config.bankName)
        assertEquals("https://api.alahli.com/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
        assertNotNull(config.clientId, "Client ID should be configured")
        assertNotNull(config.certificateFingerprints, "Certificate fingerprints should be configured")
    }

    @Test
    fun `should load Riyad Bank configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("riyadbank")

        // Then
        assertNotNull(config, "Riyad Bank configuration should be available")
        assertEquals("riyadbank", config.bankCode)
        assertEquals("Riyad Bank", config.bankName)
        assertEquals("https://api.riyadbank.com/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
    }

    @Test
    fun `should load SABB configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("sabb")

        // Then
        assertNotNull(config, "SABB configuration should be available")
        assertEquals("sabb", config.bankCode)
        assertEquals("The Saudi British Bank (SABB)", config.bankName)
        assertEquals("https://api.sabb.com/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
    }

    @Test
    fun `should load Alinma Bank configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("alinma")

        // Then
        assertNotNull(config, "Alinma Bank configuration should be available")
        assertEquals("alinma", config.bankCode)
        assertEquals("Alinma Bank", config.bankName)
        assertEquals("https://api.alinma.com/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
    }

    @Test
    fun `should load Bank Albilad configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("albilad")

        // Then
        assertNotNull(config, "Bank Albilad configuration should be available")
        assertEquals("albilad", config.bankCode)
        assertEquals("Bank Albilad", config.bankName)
        assertEquals("https://api.bankalbilad.com/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
    }

    @Test
    fun `should load STC Pay configuration`() {
        // When
        val config = bankRegistry.getBankConfiguration("stcpay")

        // Then
        assertNotNull(config, "STC Pay configuration should be available")
        assertEquals("stcpay", config.bankCode)
        assertEquals("STC Pay", config.bankName)
        assertEquals("https://api.stcpay.com.sa/open-banking/v1", config.baseUrl)
        assertTrue(config.supportedScopes.contains("accounts"))
        assertTrue(config.supportedScopes.contains("payments"))
    }

    @Test
    fun `should return null for unknown bank`() {
        // When
        val config = bankRegistry.getBankConfiguration("unknown-bank")

        // Then
        assertNull(config, "Unknown bank should return null")
    }

    @Test
    fun `should list all available banks`() {
        // When
        val banks = bankRegistry.getAllBanks()

        // Then
        assertEquals(7, banks.size, "Should have 7 supported banks")
        assertTrue(banks.any { it.bankCode == "alrajhi" }, "Should include Al Rajhi Bank")
        assertTrue(banks.any { it.bankCode == "snb" }, "Should include Saudi National Bank")
        assertTrue(banks.any { it.bankCode == "riyadbank" }, "Should include Riyad Bank")
        assertTrue(banks.any { it.bankCode == "sabb" }, "Should include SABB")
        assertTrue(banks.any { it.bankCode == "alinma" }, "Should include Alinma Bank")
        assertTrue(banks.any { it.bankCode == "albilad" }, "Should include Bank Albilad")
        assertTrue(banks.any { it.bankCode == "stcpay" }, "Should include STC Pay")
    }

    @Test
    fun `should validate bank configuration completeness`() {
        // When
        val allBanks = bankRegistry.getAllBanks()

        // Then
        allBanks.forEach { config ->
            assertTrue(config.bankCode.isNotBlank(), "${config.bankName} should have bank code")
            assertTrue(config.bankName.isNotBlank(), "${config.bankName} should have bank name")
            assertTrue(config.baseUrl.isNotBlank(), "${config.bankName} should have base URL")
            assertTrue(config.baseUrl.startsWith("https://"), "${config.bankName} should use HTTPS")
            assertTrue(config.clientId.isNotBlank(), "${config.bankName} should have client ID")
            assertTrue(config.supportedScopes.isNotEmpty(), "${config.bankName} should have supported scopes")
            assertTrue(config.supportedScopes.contains("accounts"), "${config.bankName} should support accounts scope")
            assertTrue(config.certificateFingerprints.isNotEmpty(), "${config.bankName} should have certificate fingerprints")
        }
    }

    @Test
    fun `should support environment-specific configurations`() {
        // When
        val sandboxConfig = bankRegistry.getBankConfiguration("alrajhi", environment = "sandbox")
        val productionConfig = bankRegistry.getBankConfiguration("alrajhi", environment = "production")

        // Then
        assertNotNull(sandboxConfig, "Should have sandbox configuration")
        assertNotNull(productionConfig, "Should have production configuration")
        assertTrue(sandboxConfig.baseUrl.contains("sandbox") || sandboxConfig.baseUrl.contains("test"), 
                  "Sandbox URL should indicate test environment")
        assertFalse(productionConfig.baseUrl.contains("sandbox") || productionConfig.baseUrl.contains("test"),
                   "Production URL should not indicate test environment")
    }

    @Test
    fun `should validate certificate fingerprints format`() {
        // When
        val allBanks = bankRegistry.getAllBanks()

        // Then
        allBanks.forEach { config ->
            config.certificateFingerprints.forEach { fingerprint ->
                assertTrue(fingerprint.matches(Regex("[a-fA-F0-9:]{47,95}")), 
                          "${config.bankName} certificate fingerprint should be valid hex format: $fingerprint")
            }
        }
    }

    @Test
    fun `should have OAuth endpoints configured`() {
        // When
        val allBanks = bankRegistry.getAllBanks()

        // Then
        allBanks.forEach { config ->
            assertNotNull(config.authorizationEndpoint, "${config.bankName} should have authorization endpoint")
            assertNotNull(config.tokenEndpoint, "${config.bankName} should have token endpoint")
            assertNotNull(config.parEndpoint, "${config.bankName} should have PAR endpoint")
            assertTrue(config.authorizationEndpoint.startsWith("https://"), 
                      "${config.bankName} authorization endpoint should use HTTPS")
            assertTrue(config.tokenEndpoint.startsWith("https://"), 
                      "${config.bankName} token endpoint should use HTTPS")
            assertTrue(config.parEndpoint.startsWith("https://"), 
                      "${config.bankName} PAR endpoint should use HTTPS")
        }
    }
}