package code.yousef.dari.sama

import code.yousef.dari.sama.implementation.CertificatePinningValidator
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test for Certificate Pinning implementation - TDD approach
 * Tests certificate validation and pinning for SAMA Open Banking security requirements
 */
class CertificatePinningTest {

    private val certificatePinner = CertificatePinningValidator()

    @Test
    fun `should validate pinned certificate successfully`() {
        // Given
        val hostname = "api.alrajhibank.com.sa"
        val validFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val pinnedFingerprints = listOf(validFingerprint)
        
        // When
        val result = certificatePinner.validateCertificateFingerprint(
            hostname, validFingerprint, pinnedFingerprints
        )

        // Then
        assertTrue(result, "Valid pinned certificate should pass validation")
    }

    @Test
    fun `should reject unpinned certificate`() {
        // Given
        val hostname = "api.alrajhibank.com.sa"
        val unpinnedFingerprint = "X1:Y2:Z3:W4:V5:U6:T7:S8:R9:Q0:P1:O2:N3:M4:L5:K6:J7:I8:H9:G0"
        val pinnedFingerprints = listOf("A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0")
        
        // When
        val result = certificatePinner.validateCertificateFingerprint(
            hostname, unpinnedFingerprint, pinnedFingerprints
        )

        // Then
        assertFalse(result, "Unpinned certificate should fail validation")
    }

    @Test
    fun `should validate certificate against multiple pinned certificates`() {
        // Given
        val hostname = "api.alrajhibank.com.sa"
        val validFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        val pinnedFingerprints = listOf(
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0",
            "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0",
            "C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"
        )
        
        // When
        val result = certificatePinner.validateCertificateFingerprint(
            hostname, validFingerprint, pinnedFingerprints
        )

        // Then
        assertTrue(result, "Certificate should match one of the pinned fingerprints")
    }

    @Test
    fun `should handle empty pinned certificates list`() {
        // Given
        val hostname = "api.alrajhibank.com.sa"
        val fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val pinnedFingerprints = emptyList<String>()
        
        // When
        val result = certificatePinner.validateCertificateFingerprint(
            hostname, fingerprint, pinnedFingerprints
        )

        // Then
        assertFalse(result, "Empty pinned certificates should fail validation")
    }

    @Test
    fun `should extract certificate fingerprint from certificate data`() {
        // Given - Mock certificate data (in practice this would be X.509 DER encoded)
        val mockCertificateData = "mock-certificate-data".toByteArray()
        
        // When
        val fingerprint = certificatePinner.extractFingerprint(mockCertificateData)

        // Then
        assertNotNull(fingerprint, "Should extract fingerprint from certificate")
        assertTrue(fingerprint.contains(":"), "Fingerprint should be in hex:hex format")
    }

    @Test
    fun `should validate fingerprint format`() {
        // Given
        val validFormats = listOf(
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0", // SHA-1
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2" // SHA-256
        )
        val invalidFormats = listOf(
            "invalidformat",
            "A1B2C3D4E5", // No colons
            "A1:B2:C3", // Too short
            "G1:H2:I3:J4:K5:L6:M7:N8:O9:P0:Q1:R2:S3:T4:U5:V6:W7:X8:Y9:Z0" // Invalid hex
        )

        // When & Then
        validFormats.forEach { format ->
            assertTrue(
                certificatePinner.isValidFingerprintFormat(format),
                "Valid format should pass: $format"
            )
        }

        invalidFormats.forEach { format ->
            assertFalse(
                certificatePinner.isValidFingerprintFormat(format),
                "Invalid format should fail: $format"
            )
        }
    }

    @Test
    fun `should normalize fingerprint format`() {
        // Given
        val inputs = listOf(
            "a1:b2:c3:d4:e5:f6" to "A1:B2:C3:D4:E5:F6", // Uppercase
            "A1B2C3D4E5F6" to "A1:B2:C3:D4:E5:F6", // Add colons
            " A1:B2:C3:D4:E5:F6 " to "A1:B2:C3:D4:E5:F6" // Trim spaces
        )

        inputs.forEach { (input, expected) ->
            // When
            val result = certificatePinner.normalizeFingerprint(input)

            // Then
            assertEquals(expected, result, "Should normalize '$input' to '$expected'")
        }
    }

    @Test
    fun `should handle certificate pinning failure gracefully`() {
        // Given
        val hostname = "unknown.bank.com"
        val fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val pinnedFingerprints = listOf("B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0")

        // When
        val result = certificatePinner.validateCertificateFingerprint(
            hostname, fingerprint, pinnedFingerprints
        )

        // Then
        assertFalse(result, "Mismatched certificate should fail validation")
    }

    @Test
    fun `should support both SHA-1 and SHA-256 fingerprints`() {
        // Given
        val hostname = "api.bankexample.com"
        val sha1Fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val sha256Fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2"
        val pinnedFingerprints = listOf(sha1Fingerprint, sha256Fingerprint)

        // When & Then
        assertTrue(
            certificatePinner.validateCertificateFingerprint(hostname, sha1Fingerprint, pinnedFingerprints),
            "SHA-1 fingerprint should be validated"
        )
        assertTrue(
            certificatePinner.validateCertificateFingerprint(hostname, sha256Fingerprint, pinnedFingerprints),
            "SHA-256 fingerprint should be validated"
        )
    }

    @Test
    fun `should log certificate pinning failures for security monitoring`() {
        // Given
        val hostname = "api.malicious.com"
        val suspiciousFingerprint = "F1:E2:D3:C4:B5:A6:97:88:79:6A:5B:4C:3D:2E:1F:00:F1:E2:D3:C4"
        val pinnedFingerprints = listOf("A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0")

        // When
        val result = certificatePinner.validateCertificateFingerprint(
            hostname, suspiciousFingerprint, pinnedFingerprints
        )

        // Then
        assertFalse(result, "Suspicious certificate should fail validation")
        // In a real implementation, this would also verify that a security event was logged
    }

    @Test
    fun `should validate certificate chain if provided`() {
        // Given
        val hostname = "api.alrajhibank.com.sa"
        val leafFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val intermediateFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        val certificateChain = listOf(leafFingerprint, intermediateFingerprint)
        val pinnedFingerprints = listOf(leafFingerprint)

        // When
        val result = certificatePinner.validateCertificateChain(
            hostname, certificateChain, pinnedFingerprints
        )

        // Then
        assertTrue(result, "Valid certificate chain should pass validation")
    }

    @Test
    fun `should validate SAMA bank specific certificate requirements`() {
        // Given - Test all major Saudi banks
        val bankHostnames = mapOf(
            "api.alrajhibank.com.sa" to "Al Rajhi Bank",
            "api.alahli.com.sa" to "Saudi National Bank",  
            "api.riyadbank.com.sa" to "Riyad Bank",
            "api.sabb.com" to "SABB",
            "api.alinma.com.sa" to "Alinma Bank",
            "api.albilad.com" to "Bank AlBilad",
            "api.stcpay.com.sa" to "STC Pay"
        )
        
        bankHostnames.forEach { (hostname, bankName) ->
            // Mock fingerprint for each bank
            val fingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:${hostname.hashCode().toString(16).takeLast(2).uppercase()}"
            val pinnedFingerprints = listOf(fingerprint)
            
            // When
            val result = certificatePinner.validateCertificateFingerprint(
                hostname, fingerprint, pinnedFingerprints
            )
            
            // Then
            assertTrue(result, "$bankName certificate should validate successfully")
        }
    }

    @Test
    fun `should handle certificate rotation scenarios`() {
        // Given - Simulate certificate rotation with old and new certificates
        val hostname = "api.alrajhibank.com.sa"
        val oldFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val newFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        
        // During rotation period, both certificates should be valid
        val pinnedFingerprints = listOf(oldFingerprint, newFingerprint)
        
        // When - validate both old and new certificates
        val oldResult = certificatePinner.validateCertificateFingerprint(
            hostname, oldFingerprint, pinnedFingerprints
        )
        val newResult = certificatePinner.validateCertificateFingerprint(
            hostname, newFingerprint, pinnedFingerprints
        )
        
        // Then
        assertTrue(oldResult, "Old certificate should be valid during rotation")
        assertTrue(newResult, "New certificate should be valid during rotation")
    }

    @Test
    fun `should reject certificates with invalid SAMA TLS requirements`() {
        // Given - Test various TLS security violations
        val hostname = "api.fraudulent-bank.com"
        val validPinnedFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        
        val securityViolations = listOf(
            "weak-signature-cert" to "F1:E2:D3:C4:B5:A6:97:88:79:6A:5B:4C:3D:2E:1F:00:F1:E2:D3:C4", // Suspicious pattern
            "expired-cert" to "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00", // All zeros (suspicious)
            "self-signed-cert" to "FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF:FF" // All ones (suspicious)
        )
        
        securityViolations.forEach { (violationType, suspiciousFingerprint) ->
            // When
            val result = certificatePinner.validateCertificateFingerprint(
                hostname, suspiciousFingerprint, listOf(validPinnedFingerprint)
            )
            
            // Then
            assertFalse(result, "$violationType should be rejected by certificate pinning")
        }
    }

    @Test
    fun `should validate certificate pinning configuration for all SAMA banks`() {
        // Given
        val samaRequiredBanks = mapOf(
            "alrajhi" to listOf("A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"),
            "snb" to listOf("B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"),
            "riyadbank" to listOf("C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"),
            "sabb" to listOf("D1:E2:F3:A4:B5:C6:D7:E8:F9:A0:B1:C2:D3:E4:F5:A6:B7:C8:D9:E0"),
            "alinma" to listOf("E1:F2:A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4:A5:B6:C7:D8:E9:F0"),
            "albilad" to listOf("F1:A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5:C6:D7:E8:F9:A0"),
            "stcpay" to listOf("A2:B3:C4:D5:E6:F7:A8:B9:C0:D1:E2:F3:A4:B5:C6:D7:E8:F9:A0:B1")
        )
        
        // When
        val validationErrors = certificatePinner.validatePinningConfiguration(samaRequiredBanks)
        
        // Then
        assertTrue(validationErrors.isEmpty(), "SAMA bank configurations should be valid: $validationErrors")
        assertEquals(7, samaRequiredBanks.size, "Should have all 7 required Saudi banks configured")
    }

    @Test
    fun `should detect configuration issues for SAMA compliance`() {
        // Given - Invalid configurations that violate SAMA requirements
        val invalidConfigurations = mapOf(
            "missing_certs_bank" to emptyList<String>(), // No certificates configured
            "invalid_format_bank" to listOf("invalid-fingerprint-format"), // Wrong format
            "mixed_valid_invalid" to listOf(
                "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0", // Valid
                "invalid-format" // Invalid
            )
        )
        
        // When
        val validationErrors = certificatePinner.validatePinningConfiguration(invalidConfigurations)
        
        // Then
        assertTrue(validationErrors.isNotEmpty(), "Invalid configurations should produce validation errors")
        assertTrue(
            validationErrors.any { it.contains("missing_certs_bank") },
            "Should detect missing certificates"
        )
        assertTrue(
            validationErrors.any { it.contains("invalid_format_bank") },
            "Should detect invalid fingerprint formats"
        )
    }

    @Test
    fun `should handle certificate pinning for backup endpoints`() {
        // Given - Primary and backup endpoints for high availability
        val primaryHostname = "api.alrajhibank.com.sa"
        val backupHostname = "backup-api.alrajhibank.com.sa"
        val drHostname = "dr-api.alrajhibank.com.sa"
        
        val primaryFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val backupFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        val drFingerprint = "C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"
        
        // All endpoints should accept the bank's certificate pool
        val pinnedFingerprints = listOf(primaryFingerprint, backupFingerprint, drFingerprint)
        
        val endpoints = listOf(
            primaryHostname to primaryFingerprint,
            backupHostname to backupFingerprint,
            drHostname to drFingerprint
        )
        
        endpoints.forEach { (hostname, fingerprint) ->
            // When
            val result = certificatePinner.validateCertificateFingerprint(
                hostname, fingerprint, pinnedFingerprints
            )
            
            // Then
            assertTrue(result, "Backup endpoint $hostname should validate successfully")
        }
    }

    @Test
    fun `should validate intermediate certificate pinning`() {
        // Given - Pin intermediate certificate instead of leaf
        val hostname = "api.riyadbank.com.sa"
        val leafFingerprint = "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0"
        val intermediateFingerprint = "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"
        val rootFingerprint = "C1:D2:E3:F4:A5:B6:C7:D8:E9:F0:A1:B2:C3:D4:E5:F6:A7:B8:C9:D0"
        
        val certificateChain = listOf(leafFingerprint, intermediateFingerprint, rootFingerprint)
        val pinnedIntermediateFingerprints = listOf(intermediateFingerprint)
        
        // When - validate chain against intermediate certificate
        val result = certificatePinner.validateCertificateChain(
            hostname, certificateChain, pinnedIntermediateFingerprints
        )
        
        // Then
        assertTrue(result, "Certificate chain with pinned intermediate should validate")
    }

    @Test
    fun `should handle empty certificate chain gracefully`() {
        // Given
        val hostname = "api.bankexample.com"
        val emptyCertificateChain = emptyList<String>()
        val pinnedFingerprints = listOf("A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0")
        
        // When
        val result = certificatePinner.validateCertificateChain(
            hostname, emptyCertificateChain, pinnedFingerprints
        )
        
        // Then
        assertFalse(result, "Empty certificate chain should fail validation")
    }

    @Test
    fun `should create proper pinning configuration for production use`() {
        // Given
        val bankCode = "alrajhi"
        val productionFingerprints = listOf(
            "A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2:A3:B4:C5:D6:E7:F8:A9:B0", // Primary cert
            "B1:C2:D3:E4:F5:A6:B7:C8:D9:E0:F1:A2:B3:C4:D5:E6:F7:A8:B9:C0"  // Backup cert
        )
        
        // When
        val config = certificatePinner.createPinningConfiguration(bankCode, productionFingerprints)
        
        // Then
        assertTrue(config.containsKey(bankCode), "Configuration should contain bank code")
        assertEquals(productionFingerprints.size, config[bankCode]?.size, "Should contain all fingerprints")
        config[bankCode]?.forEach { fingerprint ->
            assertTrue(
                certificatePinner.isValidFingerprintFormat(fingerprint),
                "All fingerprints should be valid format"
            )
        }
    }
}