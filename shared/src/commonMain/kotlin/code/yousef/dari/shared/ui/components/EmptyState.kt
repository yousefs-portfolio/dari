package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Generic empty state component
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        
        if (primaryAction != null || secondaryAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                primaryAction?.let { (text, action) ->
                    FilledButton(
                        onClick = action,
                        modifier = Modifier.widthIn(min = 120.dp)
                    ) {
                        Text(text)
                    }
                }
                
                secondaryAction?.let { (text, action) ->
                    TextButton(onClick = action) {
                        Text(text)
                    }
                }
            }
        }
    }
}

/**
 * Empty transactions state
 */
@Composable
fun EmptyTransactionsState(
    onAddTransaction: () -> Unit,
    onConnectBank: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Transactions Yet",
        description = "Connect your bank account to automatically import transactions, or add them manually to get started.",
        icon = Icons.Filled.Receipt,
        primaryAction = "Add Transaction" to onAddTransaction,
        secondaryAction = "Connect Bank" to onConnectBank,
        modifier = modifier
    )
}

/**
 * Empty accounts state
 */
@Composable
fun EmptyAccountsState(
    onConnectBank: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Bank Accounts Connected",
        description = "Connect your bank accounts to see your balances and transactions in one place.",
        icon = Icons.Filled.AccountBalance,
        primaryAction = "Connect Bank Account" to onConnectBank,
        modifier = modifier
    )
}

/**
 * Empty budget state
 */
@Composable
fun EmptyBudgetState(
    onCreateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Budget Created",
        description = "Create a budget to track your spending and achieve your financial goals.",
        icon = Icons.Filled.PieChart,
        primaryAction = "Create Budget" to onCreateBudget,
        modifier = modifier
    )
}

/**
 * Empty goals state
 */
@Composable
fun EmptyGoalsState(
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Financial Goals",
        description = "Set savings goals, debt payoff targets, or investment objectives to stay motivated.",
        icon = Icons.Filled.TrendingUp,
        primaryAction = "Create Goal" to onCreateGoal,
        modifier = modifier
    )
}

/**
 * Empty receipts state
 */
@Composable
fun EmptyReceiptsState(
    onScanReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Receipts Scanned",
        description = "Scan receipts to automatically extract transaction details and keep better records.",
        icon = Icons.Filled.CameraAlt,
        primaryAction = "Scan Receipt" to onScanReceipt,
        modifier = modifier
    )
}

/**
 * Empty categories state
 */
@Composable
fun EmptyCategoriesState(
    onCreateCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Custom Categories",
        description = "Create custom categories to better organize and track your spending patterns.",
        icon = Icons.Filled.Category,
        primaryAction = "Add Category" to onCreateCategory,
        modifier = modifier
    )
}

/**
 * Empty search results
 */
@Composable
fun EmptySearchState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Results Found",
        description = "No items match your search for \"$searchQuery\". Try different keywords or clear the search.",
        icon = Icons.Filled.SearchOff,
        primaryAction = "Clear Search" to onClearSearch,
        modifier = modifier
    )
}

/**
 * Empty notifications state
 */
@Composable
fun EmptyNotificationsState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "All Caught Up!",
        description = "You have no new notifications. Check back later for updates on your finances.",
        icon = Icons.Filled.Notifications,
        modifier = modifier
    )
}

/**
 * Offline empty state
 */
@Composable
fun OfflineEmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Data Available",
        description = "You're offline and no cached data is available. Connect to the internet to load content.",
        icon = Icons.Filled.CloudOff,
        primaryAction = "Refresh" to onRefresh,
        modifier = modifier
    )
}