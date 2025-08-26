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
}