package code.yousef.dari.shared.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class hierarchy for all app destinations
 * Defines the navigation structure for the Dari app
 */
sealed class DariDestination(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null
) {
    
    // Main navigation destinations (Bottom Navigation)
    object Dashboard : DariDestination(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home
    )
    
    object Accounts : DariDestination(
        route = "accounts",
        title = "Accounts", 
        icon = Icons.Outlined.AccountBalance,
        selectedIcon = Icons.Filled.AccountBalance
    )
    
    object Transactions : DariDestination(
        route = "transactions",
        title = "Transactions",
        icon = Icons.Outlined.Receipt,
        selectedIcon = Icons.Filled.Receipt
    )
    
    object Budget : DariDestination(
        route = "budget",
        title = "Budget",
        icon = Icons.Outlined.PieChart,
        selectedIcon = Icons.Filled.PieChart
    )
    
    object Goals : DariDestination(
        route = "goals", 
        title = "Goals",
        icon = Icons.Outlined.TrendingUp,
        selectedIcon = Icons.Filled.TrendingUp
    )
    
    // Secondary destinations
    object BankConnection : DariDestination(
        route = "bank_connection",
        title = "Connect Bank"
    )
    
    object AddTransaction : DariDestination(
        route = "add_transaction",
        title = "Add Transaction"
    )
    
    object TransactionDetails : DariDestination(
        route = "transaction_details/{transactionId}",
        title = "Transaction Details"
    ) {
        fun createRoute(transactionId: String) = "transaction_details/$transactionId"
    }
    
    object AccountDetails : DariDestination(
        route = "account_details/{accountId}",
        title = "Account Details"
    ) {
        fun createRoute(accountId: String) = "account_details/$accountId"
    }
    
    object BudgetSimulator : DariDestination(
        route = "budget_simulator",
        title = "Budget Simulator"
    )
    
    object BudgetDetails : DariDestination(
        route = "budget_details/{budgetId}",
        title = "Budget Details"
    ) {
        fun createRoute(budgetId: String) = "budget_details/$budgetId"
    }
    
    object GoalDetails : DariDestination(
        route = "goal_details/{goalId}",
        title = "Goal Details"  
    ) {
        fun createRoute(goalId: String) = "goal_details/$goalId"
    }
    
    object CreateGoal : DariDestination(
        route = "create_goal",
        title = "Create Goal"
    )
    
    object EditGoal : DariDestination(
        route = "edit_goal/{goalId}",
        title = "Edit Goal"
    ) {
        fun createRoute(goalId: String) = "edit_goal/$goalId"
    }
    
    object DebtPayoffCalculator : DariDestination(
        route = "debt_calculator",
        title = "Debt Calculator"
    )
    
    object Analytics : DariDestination(
        route = "analytics",
        title = "Analytics"
    )
    
    object ReceiptScanner : DariDestination(
        route = "receipt_scanner",
        title = "Scan Receipt"
    )
    
    object Settings : DariDestination(
        route = "settings",
        title = "Settings"
    )
    
    object Profile : DariDestination(
        route = "profile",
        title = "Profile"
    )
    
    object Security : DariDestination(
        route = "security",
        title = "Security"
    )
    
    object Notifications : DariDestination(
        route = "notifications",
        title = "Notifications"
    )
    
    object DataExport : DariDestination(
        route = "data_export",
        title = "Export Data"
    )
    
    object ZakatCalculator : DariDestination(
        route = "zakat_calculator",
        title = "Zakat Calculator"
    )
    
    object CategoryManagement : DariDestination(
        route = "categories",
        title = "Categories"
    )
    
    object SubscriptionManager : DariDestination(
        route = "subscriptions",
        title = "Subscriptions"
    )
    
    object Reports : DariDestination(
        route = "reports",
        title = "Reports"
    )
    
    // Authentication flow
    object Login : DariDestination(
        route = "login",
        title = "Login"
    )
    
    object Onboarding : DariDestination(
        route = "onboarding",
        title = "Welcome"
    )
    
    object BiometricSetup : DariDestination(
        route = "biometric_setup",
        title = "Security Setup"
    )
}

/**
 * Bottom navigation destinations
 */
val bottomNavDestinations = listOf(
    DariDestination.Dashboard,
    DariDestination.Accounts,
    DariDestination.Transactions,
    DariDestination.Budget,
    DariDestination.Goals
)

/**
 * Top-level destinations that don't show bottom navigation
 */
val topLevelDestinations = setOf(
    DariDestination.Login.route,
    DariDestination.Onboarding.route,
    DariDestination.BiometricSetup.route
)