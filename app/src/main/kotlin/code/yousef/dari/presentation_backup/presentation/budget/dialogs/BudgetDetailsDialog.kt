package code.yousef.dari.presentation.budget.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.ui.components.financial.*
import kotlin.math.max

/**
 * Budget Details Dialog
 * Shows comprehensive budget information including transactions and analytics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsDialog(
    budget: Budget,
    recentTransactions: List<Transaction>,
    onDismiss: () -> Unit,
    onEditBudget: () -> Unit,
    onViewAllTransactions: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                BudgetDetailsHeader(
                    budget = budget,
                    onDismiss = onDismiss,
                    onEdit = onEditBudget
                )
                
                // Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Budget Progress Overview
                    item {
                        BudgetProgressOverview(budget = budget)
                    }
                    
                    // Spending Analytics
                    item {
                        BudgetSpendingAnalytics(budget = budget)
                    }
                    
                    // Recent Transactions
                    item {
                        BudgetRecentTransactions(
                            transactions = recentTransactions,
                            onViewAll = onViewAllTransactions
                        )
                    }
                    
                    // Recommendations
                    item {
                        BudgetRecommendations(budget = budget)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetDetailsHeader(
    budget: Budget,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${budget.category.name} • ${budget.period.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget"
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressOverview(
    budget: Budget,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Progress Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val progressPercentage = (budget.spent.amount / budget.amount.amount * 100).coerceAtMost(100.0)
            val remainingAmount = max(0.0, budget.amount.amount - budget.spent.amount)
            
            // Progress Ring or Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // This would contain a circular progress indicator
                CircularProgressIndicator(
                    progress = { progressPercentage.toFloat() / 100f },
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 8.dp,
                    color = when {
                        progressPercentage >= 100 -> MaterialTheme.colorScheme.error
                        progressPercentage >= budget.alertThreshold -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            progressPercentage >= 100 -> MaterialTheme.colorScheme.error
                            progressPercentage >= budget.alertThreshold -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = "used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Amount Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetAmountItem(
                    label = "Spent",
                    amount = budget.spent,
                    color = MaterialTheme.colorScheme.error
                )
                
                BudgetAmountItem(
                    label = "Remaining",
                    amount = Money(remainingAmount, budget.amount.currency),
                    color = MaterialTheme.colorScheme.primary
                )
                
                BudgetAmountItem(
                    label = "Budget",
                    amount = budget.amount,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BudgetAmountItem(
    label: String,
    amount: Money,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${amount.currency} ${amount.amount}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BudgetSpendingAnalytics(
    budget: Budget,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Spending Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Daily/Weekly average, trends, etc.
            Text(
                text = "Analytics and trends would be displayed here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetRecentTransactions(
    transactions: List<Transaction>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onViewAll) {
                    Text("View All")
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                transactions.take(5).forEach { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onTransactionClick = { /* Handle click */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetRecommendations(
    budget: Budget,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Smart recommendations based on spending patterns
            Text(
                text = "AI-powered recommendations would be displayed here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Extension property for BudgetPeriod display name
private val BudgetPeriod.displayName: String
    get() = when (this) {
        BudgetPeriod.WEEKLY -> "Weekly"
        BudgetPeriod.MONTHLY -> "Monthly"
        BudgetPeriod.QUARTERLY -> "Quarterly"
        BudgetPeriod.YEARLY -> "Yearly"
    }