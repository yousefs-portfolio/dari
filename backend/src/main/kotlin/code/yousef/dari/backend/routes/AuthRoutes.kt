package code.yousef.dari.backend.routes

import code.yousef.dari.backend.database.DatabaseManager
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LoginRequest(
    val email: String,
    val password: String = ""
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserInfo? = null,
    val token: String? = null
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val name: String
)

fun Route.authRoutes() {
    route("/auth") {
        // Simple login endpoint (demo purposes)
        post("/login") {
            try {
                val loginRequest = call.receive<LoginRequest>()

                // Query user from database
                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    "SELECT id, email, name FROM users WHERE email = ?"
                )
                statement.setString(1, loginRequest.email)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val user = UserInfo(
                        id = resultSet.getString("id"),
                        email = resultSet.getString("email"),
                        name = resultSet.getString("name")
                    )

                    // Generate simple token (in real app, use JWT)
                    val token = UUID.randomUUID().toString()

                    call.respond(
                        HttpStatusCode.OK, LoginResponse(
                            success = true,
                            message = "Login successful",
                            user = user,
                            token = token
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.OK, LoginResponse(
                            success = false,
                            message = "User not found. Using demo user instead."
                        )
                    )

                    // For demo purposes, create and return demo user
                    val demoUser = UserInfo(
                        id = "user-1",
                        email = "demo@dari.sa",
                        name = "Demo User"
                    )

                    call.respond(
                        HttpStatusCode.OK, LoginResponse(
                            success = true,
                            message = "Demo login successful",
                            user = demoUser,
                            token = "demo-token-123"
                        )
                    )
                }

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, LoginResponse(
                        success = false,
                        message = "Login failed: ${e.message}"
                    )
                )
            }
        }

        // Get user profile
        get("/profile") {
            try {
                val userId = call.request.headers["X-User-Id"] ?: "user-1"

                val connection = DatabaseManager.getConnection()
                val statement = connection.prepareStatement(
                    "SELECT id, email, name FROM users WHERE id = ?"
                )
                statement.setString(1, userId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val user = UserInfo(
                        id = resultSet.getString("id"),
                        email = resultSet.getString("email"),
                        name = resultSet.getString("name")
                    )
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }

                resultSet.close()
                statement.close()

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
