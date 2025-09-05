package code.yousef.dari.backend.routes

import code.yousef.dari.backend.database.DatabaseManager
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: String,
    val userId: String,
    val category: String,
    val amount: String,
    val spent: String,
    val period: String,
    val startDate: String,
    val endDate: String,
    val createdAt: String
)

fun Route.budgetRoutes() {
    route("/budgets") {
        // Get all budgets for user
        get {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT id, user_id, category, amount, spent, period,
                           start_date, end_date, created_at
                    FROM budgets WHERE user_id = ?
                    ORDER BY created_at DESC
                """
                )
                statement.setString(1, userId)
                val resultSet = statement.executeQuery()

                val budgets = mutableListOf<Budget>()
                while (resultSet.next()) {
                    val budget = Budget(
                        id = resultSet.getString("id"),
                        userId = resultSet.getString("user_id"),
                        category = resultSet.getString("category"),
                        amount = resultSet.getBigDecimal("amount").toString(),
                        spent = resultSet.getBigDecimal("spent").toString(),
                        period = resultSet.getString("period"),
                        startDate = resultSet.getDate("start_date").toString(),
                        endDate = resultSet.getDate("end_date").toString(),
                        createdAt = resultSet.getTimestamp("created_at").toString()
                    )
                    budgets.add(budget)
                }

                call.respond(HttpStatusCode.OK, budgets)

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Get budget summary
        get("/summary") {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT
                        SUM(amount) as total_budgeted,
                        SUM(spent) as total_spent,
                        COUNT(*) as budget_count
                    FROM budgets WHERE user_id = ?
                """
                )
                statement.setString(1, userId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val summary = mapOf(
                        "totalBudgeted" to (resultSet.getBigDecimal("total_budgeted")?.toString() ?: "0.00"),
                        "totalSpent" to (resultSet.getBigDecimal("total_spent")?.toString() ?: "0.00"),
                        "budgetCount" to resultSet.getInt("budget_count"),
                        "currency" to "SAR"
                    )
                    call.respond(HttpStatusCode.OK, summary)
                } else {
                    call.respond(
                        HttpStatusCode.OK, mapOf(
                            "totalBudgeted" to "0.00",
                            "totalSpent" to "0.00",
                            "budgetCount" to 0,
                            "currency" to "SAR"
                        )
                    )
                }

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
