package code.yousef.dari.sama.implementation

import kotlin.native.concurrent.ThreadLocal

/**
 * Certificate Pinning Validator
 * Validates SSL/TLS certificates against pinned fingerprints for SAMA Open Banking security
 * Implements certificate pinning as required by SAMA guidelines
 */
class CertificatePinningValidator {

    @ThreadLocal
    companion object {
        // SHA-1 fingerprint: 40 hex characters (20 bytes) + 19 colons = 59 characters
        private val SHA1_PATTERN = Regex("[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){19}")
        // SHA-256 fingerprint: 64 hex characters (32 bytes) + 31 colons = 95 characters  
        private val SHA256_PATTERN = Regex("[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){31}")
        
        private const val COLON_SEPARATOR = ":"
    }

    /**
     * Validate certificate fingerprint against pinned fingerprints
     * @param hostname The hostname being validated
     * @param certificateFingerprint The actual certificate fingerprint
     * @param pinnedFingerprints List of acceptable pinned fingerprints
     * @return true if certificate is pinned and valid, false otherwise
     */
    fun validateCertificateFingerprint(
        hostname: String,
        certificateFingerprint: String,
        pinnedFingerprints: List<String>
    ): Boolean {
        return try {
            if (pinnedFingerprints.isEmpty()) {
                logSecurityEvent("No pinned certificates configured for $hostname")
                return false
            }

            if (!isValidFingerprintFormat(certificateFingerprint)) {
                logSecurityEvent("Invalid certificate fingerprint format for $hostname: $certificateFingerprint")
                return false
            }

            val normalizedActual = normalizeFingerprint(certificateFingerprint)
            val normalizedPinned = pinnedFingerprints.map { normalizeFingerprint(it) }

            val isValid = normalizedPinned.contains(normalizedActual)
            
            if (!isValid) {
                logSecurityEvent(
                    "Certificate pinning failure for $hostname. " +
                    "Actual: $normalizedActual, Expected one of: $normalizedPinned"
                )
            } else {
                logSecurityEvent("Certificate pinning validation successful for $hostname")
            }

            isValid
        } catch (e: Exception) {
            logSecurityEvent("Certificate validation error for $hostname: ${e.message}")
            false
        }
    }

    /**
     * Validate an entire certificate chain
     * @param hostname The hostname being validated
     * @param certificateChain List of certificate fingerprints in the chain (leaf first)
     * @param pinnedFingerprints List of acceptable pinned fingerprints
     * @return true if any certificate in the chain matches a pinned fingerprint
     */
    fun validateCertificateChain(
        hostname: String,
        certificateChain: List<String>,
        pinnedFingerprints: List<String>
    ): Boolean {
        if (certificateChain.isEmpty()) {
            logSecurityEvent("Empty certificate chain for $hostname")
            return false
        }

        // Check if any certificate in the chain is pinned
        return certificateChain.any { certFingerprint ->
            validateCertificateFingerprint(hostname, certFingerprint, pinnedFingerprints)
        }
    }

    /**
     * Extract fingerprint from certificate data
     * @param certificateData Raw certificate data (DER encoded)
     * @return SHA-256 fingerprint in hex format with colons
     */
    fun extractFingerprint(certificateData: ByteArray): String {
        // For the test implementation, we'll create a mock fingerprint
        // Real implementation would use proper certificate parsing and hashing
        return createMockFingerprint(certificateData)
    }

    /**
     * Check if fingerprint format is valid (SHA-1 or SHA-256)
     * @param fingerprint The fingerprint to validate
     * @return true if format is valid
     */
    fun isValidFingerprintFormat(fingerprint: String): Boolean {
        val normalized = fingerprint.trim().uppercase()
        return SHA1_PATTERN.matches(normalized) || SHA256_PATTERN.matches(normalized)
    }

    /**
     * Normalize fingerprint format (uppercase, with colons)
     * @param fingerprint Raw fingerprint string
     * @return Normalized fingerprint
     */
    fun normalizeFingerprint(fingerprint: String): String {
        val cleaned = fingerprint.trim().uppercase().replace(" ", "")
        
        // If already has colons and is valid format, return as-is
        if (cleaned.contains(COLON_SEPARATOR) && isValidFingerprintFormat(cleaned)) {
            return cleaned
        }
        
        // Remove existing colons and add them back in correct positions
        val hexOnly = cleaned.replace(COLON_SEPARATOR, "")
        
        return if (hexOnly.length % 2 == 0 && hexOnly.matches(Regex("[0-9A-Fa-f]+"))) {
            hexOnly.chunked(2).joinToString(COLON_SEPARATOR)
        } else {
            fingerprint // Return original if can't normalize
        }
    }

    /**
     * Create certificate pinning configuration for a bank
     * @param bankCode The bank identifier
     * @param fingerprints List of certificate fingerprints for the bank
     * @return Configuration map for use with HTTP client
     */
    fun createPinningConfiguration(
        bankCode: String,
        fingerprints: List<String>
    ): Map<String, List<String>> {
        val normalizedFingerprints = fingerprints
            .filter { isValidFingerprintFormat(it) }
            .map { normalizeFingerprint(it) }
        
        return mapOf(bankCode to normalizedFingerprints)
    }

    /**
     * Validate pinning configuration completeness
     * @param bankConfigurations Map of bank codes to their pinning configurations
     * @return List of validation errors, empty if all valid
     */
    fun validatePinningConfiguration(
        bankConfigurations: Map<String, List<String>>
    ): List<String> {
        val errors = mutableListOf<String>()
        
        bankConfigurations.forEach { (bankCode, fingerprints) ->
            if (fingerprints.isEmpty()) {
                errors.add("No certificate fingerprints configured for bank: $bankCode")
            }
            
            fingerprints.forEach { fingerprint ->
                if (!isValidFingerprintFormat(fingerprint)) {
                    errors.add("Invalid fingerprint format for $bankCode: $fingerprint")
                }
            }
        }
        
        return errors
    }

    /**
     * Create a mock fingerprint for testing purposes
     * In production, this would use actual SHA-256 hashing of certificate data
     */
    private fun createMockFingerprint(data: ByteArray): String {
        // Create a deterministic "fingerprint" based on data hash
        val hash = data.fold(0L) { acc, byte -> acc * 31 + byte }
        val hexString = hash.toString(16).padStart(32, '0').take(32)
        
        return hexString.uppercase().chunked(2).joinToString(COLON_SEPARATOR)
    }

    /**
     * Log security events for monitoring and alerting
     * In production, this should integrate with security monitoring systems
     */
    private fun logSecurityEvent(message: String) {
        // For now, we'll just print to console
        // In production, this should log to a secure audit trail
        println("SECURITY EVENT: $message")
        
        // TODO: Integrate with security monitoring system
        // - Send to SIEM
        // - Alert security team for pinning failures
        // - Track metrics for certificate validation success/failure rates
    }
}