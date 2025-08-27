package code.yousef.dari.shared.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import code.yousef.dari.shared.ui.theme.DariTheme

/**
 * Bottom navigation component for the main app navigation
 */
@Composable
fun DariBottomNavigation(
    navController: NavHostController,
    navigationActions: DariNavigationActions,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp
    ) {
        bottomNavDestinations.forEach { destination ->
            DariBottomNavigationItem(
                destination = destination,
                selected = navController.isDestinationSelected(destination),
                onClick = { 
                    navigationActions.navigateToBottomNavDestination(destination)
                }
            )
        }
    }
}

/**
 * Individual bottom navigation item
 */
@Composable
private fun RowScope.DariBottomNavigationItem(
    destination: DariDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (selected) {
                    destination.selectedIcon ?: destination.icon!!
                } else {
                    destination.icon!!
                },
                contentDescription = destination.title,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Text(
                text = destination.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = DariTheme.financialColors.Income,
            selectedTextColor = DariTheme.financialColors.Income,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = DariTheme.financialColors.Income.copy(alpha = 0.12f)
        ),
        modifier = modifier
    )
}