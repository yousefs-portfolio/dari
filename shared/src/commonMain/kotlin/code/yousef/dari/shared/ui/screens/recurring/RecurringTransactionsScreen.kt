package code.yousef.dari.shared.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.ui.components.EmptyState
import code.yousef.dari.shared.ui.components.LoadingIndicator
import code.yousef.dari.shared.ui.components.PullToRefresh
import code.yousef.dari.shared.viewmodel.recurring.RecurringTransactionsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    viewModel: RecurringTransactionsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions") },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Add Recurring Transaction")
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingIndicator(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    uiState.recurringTransactions.isEmpty() -> {
                        EmptyState(
                            title = "No Recurring Transactions",
                            subtitle = "Set up automatic transactions for subscriptions, bills, and regular payments",
                            actionText = "Add Recurring Transaction",
                            onAction = onNavigateToCreate,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        RecurringTransactionsList(
                            recurringTransactions = uiState.recurringTransactions,
                            onToggleStatus = viewModel::toggleStatus,
                            onItemClick = onNavigateToDetails,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Handle error states
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or error dialog
        }
    }
}

@Composable
private fun RecurringTransactionsList(
    recurringTransactions: List<RecurringTransaction>,
    onToggleStatus: (String) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group transactions by status
        val activeTransactions = recurringTransactions.filter {
            it.status == RecurringTransactionStatus.ACTIVE
        }
        val inactiveTransactions = recurringTransactions.filter {
            it.status != RecurringTransactionStatus.ACTIVE
        }

        if (activeTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Active (${activeTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(activeTransactions, key = { it.id }) { transaction ->
                RecurringTransactionCard(
                    transaction = transaction,
                    onToggleStatus = onToggleStatus,
                    onClick = onItemClick
                )
            }
        }

        if (inactiveTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Inactive (${inactiveTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(inactiveTransactions, key = { it.id }) { transaction ->
                RecurringTransactionCard(
                    transaction = transaction,
                    onToggleStatus = onToggleStatus,
                    onClick = onItemClick
                )
            }
        }
    }
}
