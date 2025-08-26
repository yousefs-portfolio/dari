package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.sama.ConsentStatus
import code.yousef.dari.sama.OpenBankingClient

/**
 * Connect Bank Account Use Case
 * Handles the complete flow of connecting a bank account through SAMA Open Banking
 * Includes consent management, authorization, and account linking
 */
class ConnectBankAccountUseCase(
    private val accountRepository: AccountRepository,
    private val openBankingClient: OpenBankingClient
) {

    /**
     * Connect a bank account using Open Banking consent flow
     * 
     * @param bankCode The SAMA bank identifier code
     * @param userId The user's unique identifier
     * @param redirectUrl The callback URL after consent authorization
     * @return Result containing the connected FinancialAccount or error
     */
    suspend operator fun invoke(
        bankCode: String,
        userId: String,
        redirectUrl: String
    ): Result<FinancialAccount> {
        return try {
            // Validate input parameters
            validateInputParameters(bankCode, userId, redirectUrl)

            // Step 1: Initiate consent with the bank
            val consentResult = openBankingClient.initiateConsent(bankCode, userId, redirectUrl)
            if (consentResult.isFailure) {
                return Result.failure(
                    consentResult.exceptionOrNull() ?: Exception("Failed to initiate consent")
                )
            }

            val consentId = consentResult.getOrNull() 
                ?: return Result.failure(Exception("Invalid consent ID received"))

            // Step 2: Wait for consent authorization
            val consentStatus = openBankingClient.getConsentStatus(consentId)
            when (consentStatus) {
                ConsentStatus.AUTHORIZED -> {
                    // Step 3: Connect the account using the authorized consent
                    val connectionResult = accountRepository.connectBankAccount(bankCode, consentId)
                    if (connectionResult.isFailure) {
                        return Result.failure(
                            connectionResult.exceptionOrNull() ?: Exception("Failed to connect account")
                        )
                    }

                    connectionResult
                }
                ConsentStatus.REJECTED -> {
                    Result.failure(Exception("Bank account connection was rejected by user"))
                }
                ConsentStatus.EXPIRED -> {
                    Result.failure(Exception("Bank account connection consent has expired"))
                }
                ConsentStatus.PENDING -> {
                    Result.failure(Exception("Bank account connection is still pending authorization"))
                }
                ConsentStatus.REVOKED -> {
                    Result.failure(Exception("Bank account connection consent was revoked"))
                }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect bank account with custom consent parameters
     * Useful for Saudi-specific banking requirements
     */
    suspend fun connectWithCustomConsent(
        bankCode: String,
        userId: String,
        redirectUrl: String,
        permissions: List<String> = defaultPermissions(),
        expiryDays: Int = 90 // SAMA standard consent expiry
    ): Result<FinancialAccount> {
        return try {
            validateInputParameters(bankCode, userId, redirectUrl)

            // Initiate consent with custom parameters
            val consentResult = openBankingClient.initiateConsent(
                bankCode = bankCode,
                userId = userId,
                redirectUrl = redirectUrl,
                permissions = permissions,
                expiryDays = expiryDays
            )

            if (consentResult.isFailure) {
                return Result.failure(
                    consentResult.exceptionOrNull() ?: Exception("Failed to initiate custom consent")
                )
            }

            val consentId = consentResult.getOrNull() 
                ?: return Result.failure(Exception("Invalid consent ID received"))

            // Wait for authorization and connect
            val consentStatus = openBankingClient.getConsentStatus(consentId)
            when (consentStatus) {
                ConsentStatus.AUTHORIZED -> {
                    accountRepository.connectBankAccount(bankCode, consentId)
                }
                else -> {
                    Result.failure(Exception("Consent not authorized: $consentStatus"))
                }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a bank account can be connected
     * Validates bank availability and user eligibility
     */
    suspend fun canConnectBank(bankCode: String, userId: String): Result<Boolean> {
        return try {
            validateBankCode(bankCode)
            validateUserId(userId)

            // Check if bank supports Open Banking
            val bankInfo = openBankingClient.getBankInfo(bankCode)
            if (bankInfo.isFailure) {
                return Result.failure(Exception("Bank not supported: $bankCode"))
            }

            val bank = bankInfo.getOrNull()
            if (bank == null || !bank.supportsOpenBanking) {
                return Result.failure(Exception("Bank does not support Open Banking: $bankCode"))
            }

            // Check if user already has this bank connected
            val existingAccounts = accountRepository.getAccountsByBank(bankCode)
            val hasExistingConnection = existingAccounts.any { it.isActive }

            if (hasExistingConnection) {
                return Result.failure(Exception("Bank account already connected: $bankCode"))
            }

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the connection status for a bank
     */
    suspend fun getConnectionStatus(bankCode: String): ConnectionStatus {
        return try {
            val accounts = accountRepository.getAccountsByBank(bankCode)
            when {
                accounts.isEmpty() -> ConnectionStatus.NOT_CONNECTED
                accounts.any { it.isActive } -> ConnectionStatus.CONNECTED
                else -> ConnectionStatus.DISCONNECTED
            }
        } catch (e: Exception) {
            ConnectionStatus.ERROR
        }
    }

    /**
     * Validate input parameters
     */
    private fun validateInputParameters(bankCode: String, userId: String, redirectUrl: String) {
        validateBankCode(bankCode)
        validateUserId(userId)
        validateRedirectUrl(redirectUrl)
    }

    /**
     * Validate bank code format
     */
    private fun validateBankCode(bankCode: String) {
        if (bankCode.isBlank()) {
            throw IllegalArgumentException("Bank code cannot be empty")
        }
        
        // Saudi bank codes typically follow SAMA standards
        if (!bankCode.matches(Regex("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?\$")) && 
            !bankCode.startsWith("SAMA_")) {
            throw IllegalArgumentException("Invalid bank code format: $bankCode")
        }
    }

    /**
     * Validate user ID format
     */
    private fun validateUserId(userId: String) {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User ID cannot be empty")
        }
    }

    /**
     * Validate redirect URL format
     */
    private fun validateRedirectUrl(redirectUrl: String) {
        if (redirectUrl.isBlank()) {
            throw IllegalArgumentException("Redirect URL cannot be empty")
        }
        
        if (!redirectUrl.startsWith("https://")) {
            throw IllegalArgumentException("Redirect URL must use HTTPS")
        }
    }

    /**
     * Get default permissions for Saudi banking
     */
    private fun defaultPermissions(): List<String> {
        return listOf(
            "ReadAccountsBasic",
            "ReadAccountsDetail", 
            "ReadBalances",
            "ReadTransactionsBasic",
            "ReadTransactionsCredits",
            "ReadTransactionsDebits",
            "ReadTransactionsDetail"
        )
    }
}

/**
 * Bank account connection status
 */
enum class ConnectionStatus {
    NOT_CONNECTED,
    CONNECTED,
    DISCONNECTED,
    ERROR
}