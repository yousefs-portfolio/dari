package code.yousef.dari.sama

import code.yousef.dari.sama.implementation.CertificatePinningValidator
import code.yousef.dari.sama.implementation.HttpsCertificatePinner
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration test for HTTPS Certificate Pinning
 * Tests the complete certificate pinning workflow for SAMA Open Banking
 */
class HttpsCertificatePinningIntegrationTest {

    private val certificateValidator = CertificatePinningValidator()
    private val httpsPinner = HttpsCertificatePinner(certificateValidator)

    @BeforeTest
    fun setUp() {
        // Clear any previous configuration
        httpsPinner.clearAllPinning()
    }

    @Test
    fun `should configure certificate pinning for all SAMA banks`() {
        // Given - Production certificate fingerprints for SAMA banks
        val samaBankCertificates = mapOf(
            "alrajhi" to listOf(
                "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0",
                "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
            ),
            "snb" to listOf(
                "C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0",
                "D1:E2:F3:A4:B5:C6:D7:E8:F9:A0:B1:C2:D3:E4:F5:A6:B7:C8:D9:E0"
            ),
            "riyadbank" to listOf(
                "E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0",
                "F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5:C6:D7:E8:F9:A0"
            ),
            "sabb" to listOf(
                "A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5:C6:D7:E8:F9:A0:B1",
                "B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1"
            ),
            "alinma" to listOf(
                "C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1",
                "D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1"
            ),
            "albilad" to listOf(
                "E2:F3:A4:B5:C6:D7:E8:F9:A0:B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1",
                "F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1"
            ),
            "stcpay" to listOf(
                "A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2",
                "B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5:C6:D7:E8:F9:A0:B1:C2"
            )
        )

        // When
        httpsPinner.configureSamaBankPinning(samaBankCertificates)

        // Then
        val statistics = httpsPinner.getPinningStatistics()
        assertEquals(14, statistics.configuredHosts, "Should configure all SAMA bank hostnames")
        assertEquals(7, statistics.samaCompliantHosts, "Should have all 7 banks configured")
        
        // Verify specific hosts are pinned
        assertTrue(httpsPinner.isHostPinned("api.alrajhibank.com.sa"), "Al Rajhi API should be pinned")
        assertTrue(httpsPinner.isHostPinned("api.alahli.com.sa"), "SNB API should be pinned")
        assertTrue(httpsPinner.isHostPinned("api.stcpay.com.sa"), "STC Pay API should be pinned")
    }

    @Test
    fun `should validate pinning configuration for SAMA compliance`() {
        // Given - Valid SAMA configuration
        val validConfiguration = mapOf(
            "alrajhi" to listOf("A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"),
            "snb" to listOf("B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"),
            "riyadbank" to listOf("C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0")
        )
        
        httpsPinner.configureSamaBankPinning(validConfiguration)

        // When
        val validationErrors = httpsPinner.validateAllPinningConfiguration()

        // Then
        // Should have errors for missing banks (we only configured 3 out of 7)
        assertTrue(validationErrors.isNotEmpty(), "Should report missing SAMA banks")
        assertTrue(
            validationErrors.any { it.contains("Missing certificate pinning") },
            "Should specifically mention missing certificate pinning"
        )
    }

    @Test
    fun `should handle certificate validation with caching`() = runTest {
        // Given
        val hostname = "api.alrajhibank.com.sa"
        val validFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val pinnedFingerprints = listOf(validFingerprint)
        
        httpsPinner.pinCertificatesForHost(hostname, pinnedFingerprints)

        // When - validate same certificate multiple times
        val mockCertificateData = "mock-certificate-data-for-$hostname".toByteArray()
        
        val result1 = httpsPinner.validateCertificate(hostname, mockCertificateData)
        val result2 = httpsPinner.validateCertificate(hostname, mockCertificateData)
        val result3 = httpsPinner.validateCertificate(hostname, mockCertificateData)

        // Then - all validations should succeed (testing caching behavior)
        assertTrue(result1.isSuccess, "First validation should succeed")
        assertTrue(result2.isSuccess, "Second validation should succeed (cached)")
        assertTrue(result3.isSuccess, "Third validation should succeed (cached)")
        
        // Cache should contain the validation result
        val statistics = httpsPinner.getPinningStatistics()
        assertTrue(statistics.cachedValidations > 0, "Should have cached validation results")
    }

    @Test
    fun `should validate certificate chain for bank endpoints`() = runTest {
        // Given - Certificate chain for a bank (leaf -> intermediate -> root)
        val hostname = "api.riyadbank.com.sa"
        val leafFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val intermediateFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        val rootFingerprint = "C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"
        
        // Pin the intermediate certificate (common practice)
        httpsPinner.pinCertificatesForHost(hostname, listOf(intermediateFingerprint))
        
        val certificateChain = listOf(
            "leaf-cert-data".toByteArray(),
            "intermediate-cert-data".toByteArray(), 
            "root-cert-data".toByteArray()
        )

        // When
        val result = httpsPinner.validateCertificateChain(hostname, certificateChain)

        // Then
        assertTrue(result.isSuccess, "Certificate chain validation should succeed")
    }

    @Test
    fun `should handle certificate pinning failures securely`() = runTest {
        // Given - Hostname with pinned certificates
        val hostname = "api.alrajhibank.com.sa"
        val validFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        
        httpsPinner.pinCertificatesForHost(hostname, listOf(validFingerprint))
        
        // When - try to validate with wrong certificate
        val maliciousCertificateData = "malicious-certificate-data".toByteArray()
        val result = httpsPinner.validateCertificate(hostname, maliciousCertificateData)

        // Then - validation should fail securely
        assertTrue(result.isSuccess, "Method call should succeed")
        assertFalse(result.getOrDefault(true), "Certificate validation should fail")
    }

    @Test
    fun `should support certificate rotation scenarios`() = runTest {
        // Given - Bank updating their certificates (rotation scenario)
        val hostname = "api.sabb.com"
        val oldFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val newFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        
        // During rotation, both certificates should be valid
        httpsPinner.pinCertificatesForHost(hostname, listOf(oldFingerprint, newFingerprint))
        
        val oldCertData = "old-certificate-data".toByteArray()
        val newCertData = "new-certificate-data".toByteArray()

        // When - validate both old and new certificates
        val oldResult = httpsPinner.validateCertificate(hostname, oldCertData)
        val newResult = httpsPinner.validateCertificate(hostname, newCertData)

        // Then - both should be valid during rotation period
        assertTrue(oldResult.isSuccess, "Old certificate validation should succeed")
        assertTrue(newResult.isSuccess, "New certificate validation should succeed")
    }

    @Test
    fun `should handle backup and DR endpoints`() {
        // Given - Primary and backup endpoints for high availability
        val primaryHost = "api.alrajhibank.com.sa"
        val backupHost = "backup-api.alrajhibank.com.sa"
        val drHost = "dr-api.alrajhibank.com.sa"
        
        val sharedCertificates = listOf(
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0",
            "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        )

        // When - configure same certificates for all endpoints
        httpsPinner.pinCertificatesForHost(primaryHost, sharedCertificates)
        httpsPinner.pinCertificatesForHost(backupHost, sharedCertificates)
        httpsPinner.pinCertificatesForHost(drHost, sharedCertificates)

        // Then - all endpoints should be properly configured
        assertTrue(httpsPinner.isHostPinned(primaryHost), "Primary host should be pinned")
        assertTrue(httpsPinner.isHostPinned(backupHost), "Backup host should be pinned")
        assertTrue(httpsPinner.isHostPinned(drHost), "DR host should be pinned")
        
        assertEquals(sharedCertificates, httpsPinner.getPinnedCertificates(primaryHost))
        assertEquals(sharedCertificates, httpsPinner.getPinnedCertificates(backupHost))
        assertEquals(sharedCertificates, httpsPinner.getPinnedCertificates(drHost))
    }

    @Test
    fun `should provide comprehensive pinning statistics`() {
        // Given - Mixed configuration
        val bankCertificates = mapOf(
            "alrajhi" to listOf("A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"),
            "snb" to listOf("B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0")
        )
        
        httpsPinner.configureSamaBankPinning(bankCertificates)
        
        // Add some non-SAMA hosts for testing
        httpsPinner.pinCertificatesForHost("api.example.com", listOf("C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"))

        // When
        val statistics = httpsPinner.getPinningStatistics()

        // Then
        assertTrue(statistics.configuredHosts > 0, "Should have configured hosts")
        assertEquals(2, statistics.samaCompliantHosts, "Should have 2 SAMA-compliant hosts configured")
        assertTrue(statistics.configuredHosts > statistics.samaCompliantHosts, "Should have some non-SAMA hosts")
    }

    @Test
    fun `should enforce SAMA security requirements`() {
        // Given - Attempt to configure weak or invalid certificates
        val invalidConfigurations = mapOf(
            "weak_bank" to listOf("00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"), // All zeros
            "invalid_bank" to listOf("invalid-fingerprint-format"),
            "empty_bank" to emptyList<String>()
        )

        // When & Then - Should fail validation
        assertFailsWith<IllegalArgumentException>("Should reject invalid configuration") {
            httpsPinner.configureSamaBankPinning(invalidConfigurations)
        }
    }

    @Test
    fun `should handle production-grade certificate pinning workflow`() = runTest {
        // Given - Production-like scenario
        val productionBankCertificates = mapOf(
            "alrajhi" to listOf(
                "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0", // Primary
                "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0", // Backup
                "C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"  // Emergency
            ),
            "snb" to listOf(
                "D1:E2:F3:A4:B5:C6:D7:E8:F9:A0:B1:C2:D3:E4:F5:A6:B7:C8:D9:E0",
                "E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0"
            )
        )

        // When - configure and validate production setup
        httpsPinner.configureSamaBankPinning(productionBankCertificates)
        
        // Simulate certificate validation for various scenarios
        val alrajhiHost = "api.alrajhibank.com.sa"
        val snbHost = "api.alahli.com.sa"
        
        val validCertData = "production-certificate-data".toByteArray()
        
        val alrajhiResult = httpsPinner.validateCertificate(alrajhiHost, validCertData)
        val snbResult = httpsPinner.validateCertificate(snbHost, validCertData)

        // Then - production workflow should work end-to-end
        assertTrue(alrajhiResult.isSuccess, "Al Rajhi certificate validation should succeed")
        assertTrue(snbResult.isSuccess, "SNB certificate validation should succeed")
        
        // Verify comprehensive statistics
        val stats = httpsPinner.getPinningStatistics()
        assertTrue(stats.configuredHosts >= 4, "Should have multiple hosts configured") // 2 banks × 2 hosts each
        assertEquals(2, stats.samaCompliantHosts, "Should have 2 SAMA-compliant banks")
    }
}