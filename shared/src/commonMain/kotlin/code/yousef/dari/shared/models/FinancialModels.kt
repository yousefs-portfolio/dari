package code.yousef.dari.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val userId: String,
    val accountNumber: String,
    val accountType: String,
    val bankName: String,
    val balance: String,
    val currency: String = "SAR",
    val createdAt: String
)

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: String,
    val description: String?,
    val category: String?,
    val transactionDate: String,
    val type: TransactionType,
    val createdAt: String
)

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Serializable
data class Budget(
    val id: String,
    val userId: String,
    val category: String,
    val amount: String,
    val spent: String,
    val period: BudgetPeriod,
    val startDate: String,
    val endDate: String,
    val createdAt: String
)

@Serializable
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Serializable
data class Goal(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: String,
    val currentAmount: String,
    val targetDate: String?,
    val category: String?,
    val status: GoalStatus,
    val createdAt: String
)

@Serializable
enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    PAUSED,
    CANCELLED
}

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: String? = null
)

@Serializable
data class AccountSummary(
    val totalAccounts: Int,
    val totalBalance: String,
    val currency: String = "SAR",
    val accounts: List<Account>
)

@Serializable
data class BudgetSummary(
    val totalBudgeted: String,
    val totalSpent: String,
    val budgetCount: Int,
    val currency: String = "SAR"
)

@Serializable
data class GoalSummary(
    val totalGoals: Int,
    val completedGoals: Int,
    val totalTarget: String,
    val totalSaved: String,
    val currency: String = "SAR"
)

// API Request/Response models
@Serializable
data class LoginRequest(
    val email: String,
    val password: String = ""
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null,
    val token: String? = null
)

@Serializable
data class CreateTransactionRequest(
    val accountId: String,
    val amount: String,
    val description: String?,
    val category: String?,
    val type: TransactionType
)

@Serializable
data class TransactionResponse(
    val success: Boolean,
    val message: String,
    val transaction: Transaction? = null
)

@Serializable
data class ApiError(
    val error: String,
    val code: Int? = null,
    val timestamp: String? = null
)
