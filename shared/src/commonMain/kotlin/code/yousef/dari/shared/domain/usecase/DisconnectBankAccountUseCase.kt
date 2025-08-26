package code.yousef.dari.shared.domain.usecase

import code.yousef.dari.shared.data.repository.AccountRepository
import code.yousef.dari.sama.OpenBankingClient
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Disconnect Bank Account Use Case
 * Handles the complete flow of disconnecting a bank account
 * Includes consent revocation and local account deactivation
 */
class DisconnectBankAccountUseCase(
    private val accountRepository: AccountRepository,
    private val openBankingClient: OpenBankingClient
) {

    /**
     * Disconnect a specific bank account
     * 
     * @param accountId The account ID to disconnect
     * @param userId The user's unique identifier
     * @param reason The reason for disconnection (for audit purposes)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        accountId: String,
        userId: String,
        reason: String = "User requested disconnection"
    ): Result<Unit> {
        return try {
            // Validate input parameters
            validateInputParameters(accountId, userId)

            // Step 1: Get the account to be disconnected
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            // Step 2: Revoke consent with the bank (if account is active)
            if (account.isActive) {
                try {
                    val revokeResult = openBankingClient.revokeConsent(account.bankCode, userId)
                    if (revokeResult.isFailure) {
                        // Log the error but continue with local disconnection
                        // This ensures the account is still marked as disconnected locally
                        // even if the remote revocation fails
                        println("Warning: Failed to revoke consent with bank ${account.bankCode}: ${revokeResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    // Continue with local disconnection even if remote revocation fails
                    println("Warning: Exception during consent revocation: ${e.message}")
                }
            }

            // Step 3: Disconnect the account locally
            val disconnectResult = accountRepository.disconnectAccount(accountId, reason)
            if (disconnectResult.isFailure) {
                return Result.failure(
                    disconnectResult.exceptionOrNull() ?: Exception("Failed to disconnect account locally")
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect all accounts for a specific bank
     * 
     * @param bankCode The bank identifier code
     * @param userId The user's unique identifier
     * @param reason The reason for disconnection
     * @return Result containing the number of accounts disconnected
     */
    suspend fun disconnectAllAccountsForBank(
        bankCode: String,
        userId: String,
        reason: String = "Bank disconnection requested"
    ): Result<Int> {
        return try {
            validateBankCode(bankCode)
            validateUserId(userId)

            // Get all accounts for this bank
            val accounts = accountRepository.getAccountsByBank(bankCode)
            if (accounts.isEmpty()) {
                return Result.success(0)
            }

            // Revoke consent with the bank once for all accounts
            try {
                val revokeResult = openBankingClient.revokeConsent(bankCode, userId)
                if (revokeResult.isFailure) {
                    println("Warning: Failed to revoke consent with bank $bankCode: ${revokeResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("Warning: Exception during consent revocation for bank $bankCode: ${e.message}")
            }

            // Disconnect each account locally
            var disconnectedCount = 0
            accounts.forEach { account ->
                try {
                    val disconnectResult = accountRepository.disconnectAccount(account.accountId, reason)
                    if (disconnectResult.isSuccess) {
                        disconnectedCount++
                    } else {
                        println("Warning: Failed to disconnect account ${account.accountId}: ${disconnectResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("Warning: Exception disconnecting account ${account.accountId}: ${e.message}")
                }
            }

            Result.success(disconnectedCount)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect account with detailed metrics tracking
     * 
     * @param accountId The account ID to disconnect
     * @param userId The user's unique identifier
     * @param reason The reason for disconnection
     * @return Result indicating success or failure
     */
    suspend fun disconnectWithMetrics(
        accountId: String,
        userId: String,
        reason: String = "User requested disconnection"
    ): Result<Unit> {
        return try {
            val startTime = Clock.System.now()
            
            // Perform the disconnection
            val result = invoke(accountId, userId, reason)
            
            val endTime = Clock.System.now()
            val duration = endTime.minus(startTime)

            // Record disconnection event for analytics
            try {
                accountRepository.recordDisconnectionEvent(
                    accountId = accountId,
                    userId = userId,
                    reason = reason,
                    timestamp = endTime.toLocalDateTime(TimeZone.UTC),
                    duration = duration,
                    success = result.isSuccess
                )
            } catch (e: Exception) {
                println("Warning: Failed to record disconnection metrics: ${e.message}")
            }

            result

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Soft disconnect - marks account as inactive but preserves data
     * Useful for temporary disconnections or troubleshooting
     */
    suspend fun softDisconnect(
        accountId: String,
        reason: String = "Temporary disconnection"
    ): Result<Unit> {
        return try {
            validateAccountId(accountId)

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            if (!account.isActive) {
                return Result.success(Unit) // Already inactive
            }

            // Mark account as inactive without full disconnection
            val deactivateResult = accountRepository.deactivateAccount(accountId, reason)
            if (deactivateResult.isFailure) {
                return Result.failure(
                    deactivateResult.exceptionOrNull() ?: Exception("Failed to deactivate account")
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hard disconnect - completely removes account and all associated data
     * Use with caution - this cannot be undone
     */
    suspend fun hardDisconnect(
        accountId: String,
        userId: String,
        confirmationToken: String,
        reason: String = "Permanent account removal"
    ): Result<Unit> {
        return try {
            validateInputParameters(accountId, userId)
            
            if (confirmationToken.isBlank()) {
                return Result.failure(Exception("Confirmation token required for hard disconnect"))
            }

            // Verify confirmation token (implementation would validate this)
            if (!isValidConfirmationToken(confirmationToken, accountId, userId)) {
                return Result.failure(Exception("Invalid confirmation token"))
            }

            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(Exception("Account not found: $accountId"))

            // Revoke consent if account is active
            if (account.isActive) {
                try {
                    openBankingClient.revokeConsent(account.bankCode, userId)
                } catch (e: Exception) {
                    println("Warning: Failed to revoke consent during hard disconnect: ${e.message}")
                }
            }

            // Permanently delete the account and all associated data
            val deleteResult = accountRepository.permanentlyDeleteAccount(accountId, reason)
            if (deleteResult.isFailure) {
                return Result.failure(
                    deleteResult.exceptionOrNull() ?: Exception("Failed to permanently delete account")
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get disconnection status for an account
     */
    suspend fun getDisconnectionStatus(accountId: String): DisconnectionStatus {
        return try {
            val account = accountRepository.getAccountById(accountId)
            when {
                account == null -> DisconnectionStatus.NOT_FOUND
                account.isActive -> DisconnectionStatus.CONNECTED
                else -> DisconnectionStatus.DISCONNECTED
            }
        } catch (e: Exception) {
            DisconnectionStatus.ERROR
        }
    }

    /**
     * Validate input parameters
     */
    private fun validateInputParameters(accountId: String, userId: String) {
        validateAccountId(accountId)
        validateUserId(userId)
    }

    /**
     * Validate account ID format
     */
    private fun validateAccountId(accountId: String) {
        if (accountId.isBlank()) {
            throw IllegalArgumentException("Account ID cannot be empty")
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
     * Validate bank code format
     */
    private fun validateBankCode(bankCode: String) {
        if (bankCode.isBlank()) {
            throw IllegalArgumentException("Bank code cannot be empty")
        }
    }

    /**
     * Validate confirmation token for hard disconnect
     * In a real implementation, this would verify a secure token
     */
    private fun isValidConfirmationToken(token: String, accountId: String, userId: String): Boolean {
        // Simple validation - in production, this would be more sophisticated
        return token.length >= 8 && token.contains(accountId.takeLast(4))
    }
}

/**
 * Account disconnection status
 */
enum class DisconnectionStatus {
    CONNECTED,
    DISCONNECTED,
    NOT_FOUND,
    ERROR
}