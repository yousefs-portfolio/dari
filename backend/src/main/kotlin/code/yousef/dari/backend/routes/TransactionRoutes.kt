package code.yousef.dari.backend.routes

import code.yousef.dari.backend.database.DatabaseManager
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: String,
    val description: String?,
    val category: String?,
    val transactionDate: String,
    val type: String,
    val createdAt: String
)

@Serializable
data class CreateTransactionRequest(
    val accountId: String,
    val amount: String,
    val description: String?,
    val category: String?,
    val type: String // INCOME, EXPENSE, TRANSFER
)

@Serializable
data class TransactionResponse(
    val success: Boolean,
    val message: String,
    val transaction: Transaction? = null
)

fun Route.transactionRoutes() {
    route("/transactions") {
        // Get all transactions for user
        get {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT t.id, t.account_id, t.amount, t.description, t.category,
                           t.transaction_date, t.type, t.created_at
                    FROM transactions t
                    INNER JOIN accounts a ON t.account_id = a.id
                    WHERE a.user_id = ?
                    ORDER BY t.transaction_date DESC
                    LIMIT ? OFFSET ?
                """
                )
                statement.setString(1, userId)
                statement.setInt(2, limit)
                statement.setInt(3, offset)
                val resultSet = statement.executeQuery()

                val transactions = mutableListOf<Transaction>()
                while (resultSet.next()) {
                    val transaction = Transaction(
                        id = resultSet.getString("id"),
                        accountId = resultSet.getString("account_id"),
                        amount = resultSet.getBigDecimal("amount").toString(),
                        description = resultSet.getString("description"),
                        category = resultSet.getString("category"),
                        transactionDate = resultSet.getTimestamp("transaction_date").toString(),
                        type = resultSet.getString("type"),
                        createdAt = resultSet.getTimestamp("created_at").toString()
                    )
                    transactions.add(transaction)
                }

                call.respond(HttpStatusCode.OK, transactions)

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Get transactions for specific account
        get("/account/{accountId}") {
            try {
                val accountId = call.parameters["accountId"]
                val userId = call.request.headers["X-User-Id"] ?: "user-1"
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                if (accountId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Account ID is required"))
                    return@get
                }

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT t.id, t.account_id, t.amount, t.description, t.category,
                           t.transaction_date, t.type, t.created_at
                    FROM transactions t
                    INNER JOIN accounts a ON t.account_id = a.id
                    WHERE t.account_id = ? AND a.user_id = ?
                    ORDER BY t.transaction_date DESC
                    LIMIT ? OFFSET ?
                """
                )
                statement.setString(1, accountId)
                statement.setString(2, userId)
                statement.setInt(3, limit)
                statement.setInt(4, offset)
                val resultSet = statement.executeQuery()

                val transactions = mutableListOf<Transaction>()
                while (resultSet.next()) {
                    val transaction = Transaction(
                        id = resultSet.getString("id"),
                        accountId = resultSet.getString("account_id"),
                        amount = resultSet.getBigDecimal("amount").toString(),
                        description = resultSet.getString("description"),
                        category = resultSet.getString("category"),
                        transactionDate = resultSet.getTimestamp("transaction_date").toString(),
                        type = resultSet.getString("type"),
                        createdAt = resultSet.getTimestamp("created_at").toString()
                    )
                    transactions.add(transaction)
                }

                call.respond(HttpStatusCode.OK, transactions)

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Create new transaction
        post {
            try {
                val request = call.receive<CreateTransactionRequest>()
                val transactionId = UUID.randomUUID().toString()

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    INSERT INTO transactions (id, account_id, amount, description, category, transaction_date, type)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """
                )
                statement.setString(1, transactionId)
                statement.setString(2, request.accountId)
                statement.setBigDecimal(3, request.amount.toBigDecimal())
                statement.setString(4, request.description)
                statement.setString(5, request.category)
                statement.setString(6, request.type)

                val rowsAffected = statement.executeUpdate()

                if (rowsAffected > 0) {
                    // Update account balance
                    val balanceUpdateStatement = connection.prepareStatement(
                        """
                        UPDATE accounts SET balance = balance + ? WHERE id = ?
                    """
                    )
                    val amount = request.amount.toBigDecimal()
                    val balanceChange = if (request.type == "EXPENSE") amount.negate() else amount
                    balanceUpdateStatement.setBigDecimal(1, balanceChange)
                    balanceUpdateStatement.setString(2, request.accountId)
                    balanceUpdateStatement.executeUpdate()
                    balanceUpdateStatement.close()

                    call.respond(
                        HttpStatusCode.Created, TransactionResponse(
                            success = true,
                            message = "Transaction created successfully"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest, TransactionResponse(
                            success = false,
                            message = "Failed to create transaction"
                        )
                    )
                }

                statement.close()

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, TransactionResponse(
                        success = false,
                        message = "Failed to create transaction: ${e.message}"
                    )
                )
            }
        }
    }
}
