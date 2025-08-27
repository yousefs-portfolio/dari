package code.yousef.dari.shared.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import code.yousef.dari.shared.ui.theme.DariMotion

/**
 * Main navigation host for the Dari app with animations and transitions
 */
@Composable
fun DariNavHost(
    navController: NavHostController,
    startDestination: String = DariDestination.Dashboard.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { DariMotion.ScreenTransitions.DefaultEnter },
        exitTransition = { DariMotion.ScreenTransitions.DefaultExit },
        popEnterTransition = { DariMotion.ScreenTransitions.DefaultPopEnter },
        popExitTransition = { DariMotion.ScreenTransitions.DefaultPopExit }
    ) {
        
        // Bottom navigation destinations with fade transitions
        composable(
            route = DariDestination.Dashboard.route,
            enterTransition = { DariMotion.ScreenTransitions.TabEnter },
            exitTransition = { DariMotion.ScreenTransitions.TabExit },
            popEnterTransition = { DariMotion.ScreenTransitions.TabEnter },
            popExitTransition = { DariMotion.ScreenTransitions.TabExit }
        ) {
            // DashboardScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Dashboard")
        }
        
        composable(
            route = DariDestination.Accounts.route,
            enterTransition = { DariMotion.ScreenTransitions.TabEnter },
            exitTransition = { DariMotion.ScreenTransitions.TabExit },
            popEnterTransition = { DariMotion.ScreenTransitions.TabEnter },
            popExitTransition = { DariMotion.ScreenTransitions.TabExit }
        ) {
            // AccountsScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Accounts")
        }
        
        composable(
            route = DariDestination.Transactions.route,
            enterTransition = { DariMotion.ScreenTransitions.TabEnter },
            exitTransition = { DariMotion.ScreenTransitions.TabExit },
            popEnterTransition = { DariMotion.ScreenTransitions.TabEnter },
            popExitTransition = { DariMotion.ScreenTransitions.TabExit }
        ) {
            // TransactionsScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Transactions")
        }
        
        composable(
            route = DariDestination.Budget.route,
            enterTransition = { DariMotion.ScreenTransitions.TabEnter },
            exitTransition = { DariMotion.ScreenTransitions.TabExit },
            popEnterTransition = { DariMotion.ScreenTransitions.TabEnter },
            popExitTransition = { DariMotion.ScreenTransitions.TabExit }
        ) {
            // BudgetScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Budget")
        }
        
        composable(
            route = DariDestination.Goals.route,
            enterTransition = { DariMotion.ScreenTransitions.TabEnter },
            exitTransition = { DariMotion.ScreenTransitions.TabExit },
            popEnterTransition = { DariMotion.ScreenTransitions.TabEnter },
            popExitTransition = { DariMotion.ScreenTransitions.TabExit }
        ) {
            // GoalsScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Goals")
        }
        
        // Modal-style screens with scale transitions
        composable(
            route = DariDestination.AddTransaction.route,
            enterTransition = { DariMotion.ScreenTransitions.ModalEnter },
            exitTransition = { DariMotion.ScreenTransitions.ModalExit },
            popEnterTransition = { DariMotion.ScreenTransitions.ModalEnter },
            popExitTransition = { DariMotion.ScreenTransitions.ModalExit }
        ) {
            // AddTransactionScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Add Transaction")
        }
        
        composable(
            route = DariDestination.BankConnection.route,
            enterTransition = { DariMotion.ScreenTransitions.ModalEnter },
            exitTransition = { DariMotion.ScreenTransitions.ModalExit }
        ) {
            // BankConnectionScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Bank Connection")
        }
        
        // Bottom sheet style screens
        composable(
            route = DariDestination.Settings.route,
            enterTransition = { DariMotion.ScreenTransitions.BottomSheetEnter },
            exitTransition = { DariMotion.ScreenTransitions.BottomSheetExit }
        ) {
            // SettingsScreen() - Will be implemented in Phase 5
            PlaceholderScreen("Settings")
        }
        
        // Detail screens with standard slide transitions
        composable(
            route = DariDestination.TransactionDetails.route,
        ) {
            // TransactionDetailsScreen() - Will be implemented in Phase 5
            val transactionId = it.arguments?.getString("transactionId") ?: ""
            PlaceholderScreen("Transaction Details: $transactionId")
        }
        
        composable(
            route = DariDestination.AccountDetails.route,
        ) {
            // AccountDetailsScreen() - Will be implemented in Phase 5
            val accountId = it.arguments?.getString("accountId") ?: ""
            PlaceholderScreen("Account Details: $accountId")
        }
        
        composable(
            route = DariDestination.GoalDetails.route,
        ) {
            // GoalDetailsScreen() - Will be implemented in Phase 5
            val goalId = it.arguments?.getString("goalId") ?: ""
            PlaceholderScreen("Goal Details: $goalId")
        }
        
        // Additional screens
        composable(DariDestination.BudgetSimulator.route) {
            PlaceholderScreen("Budget Simulator")
        }
        
        composable(DariDestination.DebtPayoffCalculator.route) {
            PlaceholderScreen("Debt Calculator")
        }
        
        composable(DariDestination.Analytics.route) {
            PlaceholderScreen("Analytics")
        }
        
        composable(DariDestination.ReceiptScanner.route) {
            PlaceholderScreen("Receipt Scanner")
        }
        
        composable(DariDestination.ZakatCalculator.route) {
            PlaceholderScreen("Zakat Calculator")
        }
    }
}

/**
 * Placeholder screen for unimplemented screens
 */
@Composable
private fun PlaceholderScreen(title: String) {
    // Temporary placeholder - will be replaced with actual screens in Phase 5
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$title Screen\n(Coming in Phase 5)",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}