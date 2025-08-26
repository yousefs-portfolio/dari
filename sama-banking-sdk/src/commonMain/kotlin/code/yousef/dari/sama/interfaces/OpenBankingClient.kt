package code.yousef.dari.sama.interfaces

/**
 * Main entry point for SAMA Open Banking SDK
 * Follows SAMA Open Banking Framework specifications
 */
interface OpenBankingClient {
    val authService: AuthenticationService
    val accountService: AccountService
    val paymentService: PaymentService
    val consentManager: ConsentManager
    val securityProvider: SecurityProvider
    val errorHandler: ErrorHandler
    
    /**
     * Check if the client is properly configured with bank credentials
     */
    fun isConfigured(): Boolean
    
    /**
     * Verify SSL certificate pinning is enabled for secure communication
     */
    fun isCertificatePinningEnabled(): Boolean
    
    /**
     * Initialize the client with bank-specific configuration
     */
    suspend fun initialize(bankId: String): Result<Unit>
}