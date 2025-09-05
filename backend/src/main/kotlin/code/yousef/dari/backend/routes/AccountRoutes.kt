package code.yousef.dari.backend.routes

import code.yousef.dari.backend.database.DatabaseManager
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Account(
    val id: String,
    val userId: String,
    val accountNumber: String,
    val accountType: String,
    val bankName: String,
    val balance: String,
    val currency: String,
    val createdAt: String
)

@Serializable
data class AccountSummary(
    val totalAccounts: Int,
    val totalBalance: String,
    val currency: String,
    val accounts: List<Account>
)

fun Route.accountRoutes() {
    route("/accounts") {
        // Get all accounts for user
        get {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT id, user_id, account_number, account_type, bank_name,
                           balance, currency, created_at
                    FROM accounts WHERE user_id = ?
                """
                )
                statement.setString(1, userId)
                val resultSet = statement.executeQuery()

                val accounts = mutableListOf<Account>()
                var totalBalance = BigDecimal.ZERO

                while (resultSet.next()) {
                    val account = Account(
                        id = resultSet.getString("id"),
                        userId = resultSet.getString("user_id"),
                        accountNumber = resultSet.getString("account_number"),
                        accountType = resultSet.getString("account_type"),
                        bankName = resultSet.getString("bank_name"),
                        balance = resultSet.getBigDecimal("balance").toString(),
                        currency = resultSet.getString("currency"),
                        createdAt = resultSet.getTimestamp("created_at").toString()
                    )
                    accounts.add(account)
                    totalBalance = totalBalance.add(resultSet.getBigDecimal("balance"))
                }

                val summary = AccountSummary(
                    totalAccounts = accounts.size,
                    totalBalance = totalBalance.toString(),
                    currency = "SAR",
                    accounts = accounts
                )

                call.respond(HttpStatusCode.OK, summary)

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Get specific account details
        get("/{accountId}") {
            try {
                val accountId = call.parameters["accountId"]
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                if (accountId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Account ID is required"))
                    return@get
                }

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT id, user_id, account_number, account_type, bank_name,
                           balance, currency, created_at
                    FROM accounts WHERE id = ? AND user_id = ?
                """
                )
                statement.setString(1, accountId)
                statement.setString(2, userId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val account = Account(
                        id = resultSet.getString("id"),
                        userId = resultSet.getString("user_id"),
                        accountNumber = resultSet.getString("account_number"),
                        accountType = resultSet.getString("account_type"),
                        bankName = resultSet.getString("bank_name"),
                        balance = resultSet.getBigDecimal("balance").toString(),
                        currency = resultSet.getString("currency"),
                        createdAt = resultSet.getTimestamp("created_at").toString()
                    )
                    call.respond(HttpStatusCode.OK, account)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Account not found"))
                }

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Get account balance
        get("/{accountId}/balance") {
            try {
                val accountId = call.parameters["accountId"]
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                if (accountId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Account ID is required"))
                    return@get
                }

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT balance, currency FROM accounts
                    WHERE id = ? AND user_id = ?
                """
                )
                statement.setString(1, accountId)
                statement.setString(2, userId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val balance = mapOf(
                        "accountId" to accountId,
                        "balance" to resultSet.getBigDecimal("balance").toString(),
                        "currency" to resultSet.getString("currency"),
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                    call.respond(HttpStatusCode.OK, balance)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Account not found"))
                }

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
