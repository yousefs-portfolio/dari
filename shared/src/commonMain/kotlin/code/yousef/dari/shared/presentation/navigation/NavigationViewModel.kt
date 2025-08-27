package code.yousef.dari.shared.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.yousef.dari.shared.ui.navigation.DariDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing navigation state and deep linking
 */
class NavigationViewModel : ViewModel() {
    
    private val _currentDestination = MutableStateFlow<String?>(null)
    val currentDestination: StateFlow<String?> = _currentDestination.asStateFlow()
    
    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    private val _deepLinkDestination = MutableStateFlow<String?>(null)
    val deepLinkDestination: StateFlow<String?> = _deepLinkDestination.asStateFlow()
    
    /**
     * Update current destination
     */
    fun updateCurrentDestination(destination: String?) {
        _currentDestination.value = destination
        updateNavigationState(destination)
    }
    
    /**
     * Handle deep link navigation
     */
    fun handleDeepLink(uri: String) {
        viewModelScope.launch {
            val destination = parseDeepLink(uri)
            _deepLinkDestination.value = destination
        }
    }
    
    /**
     * Clear deep link after processing
     */
    fun clearDeepLink() {
        _deepLinkDestination.value = null
    }
    
    /**
     * Set bottom navigation visibility
     */
    fun setBottomNavigationVisible(visible: Boolean) {
        _navigationState.value = _navigationState.value.copy(
            showBottomNavigation = visible
        )
    }
    
    /**
     * Set top app bar visibility
     */
    fun setTopAppBarVisible(visible: Boolean) {
        _navigationState.value = _navigationState.value.copy(
            showTopAppBar = visible
        )
    }
    
    /**
     * Set navigation title
     */
    fun setNavigationTitle(title: String) {
        _navigationState.value = _navigationState.value.copy(
            title = title
        )
    }
    
    /**
     * Set back button visibility
     */
    fun setShowBackButton(show: Boolean) {
        _navigationState.value = _navigationState.value.copy(
            showBackButton = show
        )
    }
    
    /**
     * Update navigation state based on destination
     */
    private fun updateNavigationState(destination: String?) {
        if (destination == null) return
        
        val showBottomNav = shouldShowBottomNavigation(destination)
        val showTopBar = shouldShowTopAppBar(destination)
        val showBack = shouldShowBackButton(destination)
        val title = getTitleForDestination(destination)
        
        _navigationState.value = NavigationState(
            showBottomNavigation = showBottomNav,
            showTopAppBar = showTopBar,
            showBackButton = showBack,
            title = title
        )
    }
    
    /**
     * Parse deep link URI to destination route
     */
    private fun parseDeepLink(uri: String): String? {
        return when {
            uri.contains("dashboard") -> DariDestination.Dashboard.route
            uri.contains("accounts") -> DariDestination.Accounts.route
            uri.contains("transactions") -> DariDestination.Transactions.route
            uri.contains("budget") -> DariDestination.Budget.route
            uri.contains("goals") -> DariDestination.Goals.route
            uri.contains("account/") -> {
                val accountId = uri.substringAfterLast("/")
                DariDestination.AccountDetails.createRoute(accountId)
            }
            uri.contains("transaction/") -> {
                val transactionId = uri.substringAfterLast("/")
                DariDestination.TransactionDetails.createRoute(transactionId)
            }
            uri.contains("goal/") -> {
                val goalId = uri.substringAfterLast("/")
                DariDestination.GoalDetails.createRoute(goalId)
            }
            uri.contains("add-transaction") -> DariDestination.AddTransaction.route
            uri.contains("connect-bank") -> DariDestination.BankConnection.route
            uri.contains("settings") -> DariDestination.Settings.route
            uri.contains("zakat") -> DariDestination.ZakatCalculator.route
            else -> null
        }
    }
    
    /**
     * Determine if bottom navigation should be shown
     */
    private fun shouldShowBottomNavigation(destination: String): Boolean {
        val topLevelDestinations = setOf(
            DariDestination.Login.route,
            DariDestination.Onboarding.route,
            DariDestination.BiometricSetup.route
        )
        
        return !topLevelDestinations.contains(destination) &&
               !destination.contains("_details") &&
               !destination.contains("add_") &&
               !destination.contains("edit_") &&
               !destination.contains("create_") &&
               !destination.contains("scanner")
    }
    
    /**
     * Determine if top app bar should be shown
     */
    private fun shouldShowTopAppBar(destination: String): Boolean {
        val noAppBarDestinations = setOf(
            DariDestination.Login.route,
            DariDestination.Onboarding.route
        )
        
        return !noAppBarDestinations.contains(destination)
    }
    
    /**
     * Determine if back button should be shown
     */
    private fun shouldShowBackButton(destination: String): Boolean {
        val bottomNavRoutes = listOf(
            DariDestination.Dashboard.route,
            DariDestination.Accounts.route,
            DariDestination.Transactions.route,
            DariDestination.Budget.route,
            DariDestination.Goals.route
        )
        
        return !bottomNavRoutes.contains(destination) &&
               destination != DariDestination.Login.route &&
               destination != DariDestination.Onboarding.route
    }
    
    /**
     * Get title for destination
     */
    private fun getTitleForDestination(destination: String): String {
        return when {
            destination == DariDestination.Dashboard.route -> "Dashboard"
            destination == DariDestination.Accounts.route -> "Accounts"
            destination == DariDestination.Transactions.route -> "Transactions"
            destination == DariDestination.Budget.route -> "Budget"
            destination == DariDestination.Goals.route -> "Goals"
            destination == DariDestination.Settings.route -> "Settings"
            destination == DariDestination.AddTransaction.route -> "Add Transaction"
            destination == DariDestination.BankConnection.route -> "Connect Bank"
            destination == DariDestination.Analytics.route -> "Analytics"
            destination == DariDestination.ZakatCalculator.route -> "Zakat Calculator"
            destination.contains("transaction_details") -> "Transaction Details"
            destination.contains("account_details") -> "Account Details"
            destination.contains("goal_details") -> "Goal Details"
            destination.contains("budget_details") -> "Budget Details"
            else -> "Dari"
        }
    }
}

/**
 * Navigation state data class
 */
data class NavigationState(
    val showBottomNavigation: Boolean = true,
    val showTopAppBar: Boolean = true,
    val showBackButton: Boolean = false,
    val title: String = "Dari",
    val isLoading: Boolean = false
)