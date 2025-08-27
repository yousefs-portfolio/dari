package code.yousef.dari.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Navigation utilities for the Dari app
 */
class DariNavigationActions(
    private val navController: NavHostController
) {
    /**
     * Navigate to a destination
     */
    fun navigateTo(destination: DariDestination) {
        navController.navigate(destination.route) {
            // Pop up to the start destination to avoid building up a large stack
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination
            launchSingleTop = true
            // Restore state when re-selecting a previously selected item
            restoreState = true
        }
    }
    
    /**
     * Navigate to a destination with route
     */
    fun navigateToRoute(route: String) {
        navController.navigate(route)
    }
    
    /**
     * Navigate back
     */
    fun navigateBack() {
        navController.popBackStack()
    }
    
    /**
     * Navigate up
     */
    fun navigateUp(): Boolean {
        return navController.navigateUp()
    }
    
    /**
     * Navigate to bottom nav destination
     */
    fun navigateToBottomNavDestination(destination: DariDestination) {
        navController.navigate(destination.route) {
            // Pop up to the start destination to avoid large stack
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies
            launchSingleTop = true
            // Restore state when re-selecting
            restoreState = true
        }
    }
    
    /**
     * Clear back stack and navigate to destination
     */
    fun navigateAndClearBackStack(destination: DariDestination) {
        navController.navigate(destination.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
    
    /**
     * Navigate with arguments
     */
    fun navigateWithArgs(route: String, args: Map<String, Any>) {
        var routeWithArgs = route
        args.forEach { (key, value) ->
            routeWithArgs = routeWithArgs.replace("{$key}", value.toString())
        }
        navController.navigate(routeWithArgs)
    }
}

/**
 * Get current destination
 */
@Composable
fun NavHostController.currentDestination(): String? {
    val navBackStackEntry by currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

/**
 * Check if destination is selected
 */
@Composable
fun NavHostController.isDestinationSelected(destination: DariDestination): Boolean {
    val navBackStackEntry by currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.hierarchy?.any { 
        it.route == destination.route 
    } == true
}

/**
 * Check if current destination should show bottom navigation
 */
@Composable
fun NavHostController.shouldShowBottomNavigation(): Boolean {
    val currentDestination = currentDestination()
    return currentDestination != null && 
           !topLevelDestinations.contains(currentDestination) &&
           !currentDestination.contains("_details") &&
           !currentDestination.contains("add_") &&
           !currentDestination.contains("edit_") &&
           !currentDestination.contains("create_")
}

/**
 * Check if current destination should show top app bar
 */
@Composable  
fun NavHostController.shouldShowTopAppBar(): Boolean {
    val currentDestination = currentDestination()
    return currentDestination != null && 
           !topLevelDestinations.contains(currentDestination)
}

/**
 * Get title for current destination
 */
@Composable
fun NavHostController.getCurrentTitle(): String {
    val currentRoute = currentDestination()
    
    return when {
        currentRoute == null -> "Dari"
        currentRoute.contains("transaction_details") -> "Transaction Details"
        currentRoute.contains("account_details") -> "Account Details"
        currentRoute.contains("budget_details") -> "Budget Details"
        currentRoute.contains("goal_details") -> "Goal Details"
        currentRoute.contains("edit_goal") -> "Edit Goal"
        else -> {
            bottomNavDestinations.find { it.route == currentRoute }?.title
                ?: when (currentRoute) {
                    DariDestination.BankConnection.route -> DariDestination.BankConnection.title
                    DariDestination.AddTransaction.route -> DariDestination.AddTransaction.title
                    DariDestination.BudgetSimulator.route -> DariDestination.BudgetSimulator.title
                    DariDestination.CreateGoal.route -> DariDestination.CreateGoal.title
                    DariDestination.DebtPayoffCalculator.route -> DariDestination.DebtPayoffCalculator.title
                    DariDestination.Analytics.route -> DariDestination.Analytics.title
                    DariDestination.ReceiptScanner.route -> DariDestination.ReceiptScanner.title
                    DariDestination.Settings.route -> DariDestination.Settings.title
                    DariDestination.Profile.route -> DariDestination.Profile.title
                    DariDestination.Security.route -> DariDestination.Security.title
                    DariDestination.Notifications.route -> DariDestination.Notifications.title
                    DariDestination.DataExport.route -> DariDestination.DataExport.title
                    DariDestination.ZakatCalculator.route -> DariDestination.ZakatCalculator.title
                    DariDestination.CategoryManagement.route -> DariDestination.CategoryManagement.title
                    DariDestination.SubscriptionManager.route -> DariDestination.SubscriptionManager.title
                    DariDestination.Reports.route -> DariDestination.Reports.title
                    else -> "Dari"
                }
        }
    }
}