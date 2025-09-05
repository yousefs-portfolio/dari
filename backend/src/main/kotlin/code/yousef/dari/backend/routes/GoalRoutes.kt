package code.yousef.dari.backend.routes

import code.yousef.dari.backend.database.DatabaseManager
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: String,
    val currentAmount: String,
    val targetDate: String?,
    val category: String?,
    val status: String,
    val createdAt: String
)

fun Route.goalRoutes() {
    route("/goals") {
        // Get all goals for user
        get {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT id, user_id, title, target_amount, current_amount,
                           target_date, category, status, created_at
                    FROM goals WHERE user_id = ?
                    ORDER BY created_at DESC
                """
                )
                statement.setString(1, userId)
                val resultSet = statement.executeQuery()

                val goals = mutableListOf<Goal>()
                while (resultSet.next()) {
                    val goal = Goal(
                        id = resultSet.getString("id"),
                        userId = resultSet.getString("user_id"),
                        title = resultSet.getString("title"),
                        targetAmount = resultSet.getBigDecimal("target_amount").toString(),
                        currentAmount = resultSet.getBigDecimal("current_amount").toString(),
                        targetDate = resultSet.getDate("target_date")?.toString(),
                        category = resultSet.getString("category"),
                        status = resultSet.getString("status"),
                        createdAt = resultSet.getTimestamp("created_at").toString()
                    )
                    goals.add(goal)
                }

                call.respond(HttpStatusCode.OK, goals)

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Get goals summary
        get("/summary") {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    """
                    SELECT
                        COUNT(*) as total_goals,
                        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_goals,
                        SUM(target_amount) as total_target,
                        SUM(current_amount) as total_saved
                    FROM goals WHERE user_id = ?
                """
                )
                statement.setString(1, userId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val summary = mapOf(
                        "totalGoals" to resultSet.getInt("total_goals"),
                        "completedGoals" to resultSet.getInt("completed_goals"),
                        "totalTarget" to (resultSet.getBigDecimal("total_target")?.toString() ?: "0.00"),
                        "totalSaved" to (resultSet.getBigDecimal("total_saved")?.toString() ?: "0.00"),
                        "currency" to "SAR"
                    )
                    call.respond(HttpStatusCode.OK, summary)
                } else {
                    call.respond(
                        HttpStatusCode.OK, mapOf(
                            "totalGoals" to 0,
                            "completedGoals" to 0,
                            "totalTarget" to "0.00",
                            "totalSaved" to "0.00",
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
