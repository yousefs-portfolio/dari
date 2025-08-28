package code.yousef.dari.presentation.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import code.yousef.dari.shared.ui.components.*
import code.yousef.dari.shared.ui.components.financial.*
import code.yousef.dari.shared.domain.models.*
import org.koin.androidx.compose.koinViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToTransactions: (String) -> Unit = {},
    onNavigateToAddAccount: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        AccountsTopBar(
            onNavigateBack = onNavigateBack,
            onSync = { viewModel.syncAccounts() },
            isRefreshing = uiState.isRefreshing,
            lastSyncTime = uiState.lastSyncTime
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    ErrorMessage(
                        error = uiState.error,
                        onRetry = { viewModel.refresh() },
                        onDismiss = { viewModel.dismissError() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.accounts.isEmpty() -> {
                    EmptyAccountsState(
                        onAddAccount = onNavigateToAddAccount,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    AccountsContent(
                        uiState = uiState,
                        onAccountTypeFilterChanged = { viewModel.filterByAccountType(it) },
                        onSearchQueryChanged = { viewModel.searchAccounts(it) },
                        onAccountClicked = { onNavigateToTransactions(it.id) },
                        onAccountDetailsClicked = { viewModel.showAccountDetails(it) },
                        onDisconnectAccount = { viewModel.disconnectAccount(it) },
                        onClearFilters = { viewModel.clearFilters() }
                    )
                }
            }
        }
    }

    // Account details bottom sheet
    if (uiState.selectedAccountId != null) {
        val selectedAccount = uiState.accounts.find { it.id == uiState.selectedAccountId }
        selectedAccount?.let { account ->
            AccountDetailsBottomSheet(
                account = account,
                onDismiss = { viewModel.hideAccountDetails() },
                onViewTransactions = { onNavigateToTransactions(account.id) },
                onDisconnect = { 
                    viewModel.disconnectAccount(account.id)
                    viewModel.hideAccountDetails()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsTopBar(
    onNavigateBack: () -> Unit,
    onSync: () -> Unit,
    isRefreshing: Boolean,
    lastSyncTime: kotlinx.datetime.Instant?
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (lastSyncTime != null) {
                    val localTime = lastSyncTime.toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        text = "Last synced: ${localTime.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSync,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Sync accounts"
                    )
                }
            }
        }
    )
}

@Composable
private fun AccountsContent(
    uiState: AccountsUiState,
    onAccountTypeFilterChanged: (AccountType?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAccountClicked: (FinancialAccount) -> Unit,
    onAccountDetailsClicked: (String) -> Unit,
    onDisconnectAccount: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { /* Handle pull to refresh */ }
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance summary
            item {
                AccountsBalanceSummaryCard(
                    summary = uiState.balanceSummary
                )
            }

            // Account type filters
            item {
                AccountTypeFilters(
                    selectedType = uiState.selectedAccountType,
                    onTypeSelected = onAccountTypeFilterChanged,
                    accountCounts = uiState.groupedAccounts.mapValues { it.value.size }
                )
            }

            // Search bar
            item {
                AccountSearchBar(
                    query = uiState.searchQuery,
                    onQueryChanged = onSearchQueryChanged
                )
            }

            // Clear filters button (if filters applied)
            if (uiState.selectedAccountType != null || uiState.searchQuery.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterAltOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Filters")
                    }
                }
            }

            // Accounts list
            if (uiState.filteredAccounts.isNotEmpty()) {
                items(uiState.filteredAccounts) { account ->
                    AccountCard(
                        account = account,
                        onClick = { onAccountClicked(account) },
                        onDetailsClick = { onAccountDetailsClicked(account.id) }
                    )
                }
            } else if (uiState.searchQuery.isNotEmpty() || uiState.selectedAccountType != null) {
                item {
                    EmptyFilteredAccountsState()
                }
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullRefreshState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsBalanceSummaryCard(
    summary: AccountBalanceSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = summary.totalBalance.formatCurrency(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceSummaryItem(
                    label = "Assets",
                    amount = summary.totalAssets,
                    color = Color(0xFF4CAF50)
                )

                BalanceSummaryItem(
                    label = "Liabilities",
                    amount = summary.totalLiabilities,
                    color = Color(0xFFF44336)
                )

                BalanceSummaryItem(
                    label = "Accounts",
                    amount = Money.fromDouble(summary.accountCount.toDouble(), ""),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    isCount = true
                )
            }
        }
    }
}

@Composable
private fun BalanceSummaryItem(
    label: String,
    amount: Money,
    color: Color,
    isCount: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Text(
            text = if (isCount) "${amount.toDouble().toInt()}" else amount.formatCurrency(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun AccountTypeFilters(
    selectedType: AccountType?,
    onTypeSelected: (AccountType?) -> Unit,
    accountCounts: Map<AccountType, Int>
) {
    val accountTypes = listOf(
        AccountType.CHECKING,
        AccountType.SAVINGS,
        AccountType.CREDIT_CARD,
        AccountType.INVESTMENT
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" filter
        item {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("All") }
            )
        }

        items(accountTypes) { type ->
            val count = accountCounts[type] ?: 0
            if (count > 0) {
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { 
                        Text("${type.displayName} ($count)")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search accounts...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    account: FinancialAccount,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccountTypeIcon(type = account.type)
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${account.metadata["bankName"]} • ${account.accountNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = onDetailsClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Account details"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = account.currentBalance?.formatCurrency() ?: "N/A",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getBalanceColor(account.currentBalance?.toDouble() ?: 0.0)
                    )
                }

                if (account.availableBalance != null && account.availableBalance != account.currentBalance) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = account.availableBalance.formatCurrency(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Sync status indicator
            if (account.lastSyncTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                val syncTime = account.lastSyncTime.toLocalDateTime(TimeZone.currentSystemDefault())
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Updated ${syncTime.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountTypeIcon(type: AccountType) {
    val (icon, color) = when (type) {
        AccountType.CHECKING -> Icons.Filled.AccountBalance to MaterialTheme.colorScheme.primary
        AccountType.SAVINGS -> Icons.Filled.Savings to Color(0xFF4CAF50)
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard to Color(0xFFFF9800)
        AccountType.INVESTMENT -> Icons.Filled.TrendingUp to Color(0xFF9C27B0)
        AccountType.LOAN -> Icons.Filled.AccountBalanceWallet to Color(0xFFF44336)
        AccountType.MORTGAGE -> Icons.Filled.Home to Color(0xFF795548)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            modifier = Modifier.size(32.dp),
            tint = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailsBottomSheet(
    account: FinancialAccount,
    onDismiss: () -> Unit,
    onViewTransactions: () -> Unit,
    onDisconnect: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Account header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountTypeIcon(type = account.type)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${account.metadata["bankName"]}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account details
            AccountDetailItem("Account Number", account.accountNumber)
            AccountDetailItem("Account Type", account.type.displayName)
            AccountDetailItem("Currency", account.currency)
            
            if (account.lastSyncTime != null) {
                val syncTime = account.lastSyncTime.toLocalDateTime(TimeZone.currentSystemDefault())
                AccountDetailItem("Last Sync", syncTime.toString())
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Button(
                onClick = onViewTransactions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Transactions")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.LinkOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnect Account")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountDetailItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyAccountsState(
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Accounts Connected",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect your bank accounts to start managing your finances",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddAccount
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect Bank Account")
        }
    }
}

@Composable
private fun EmptyFilteredAccountsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Matching Accounts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try adjusting your search or filter criteria",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// Helper functions
private fun getBalanceColor(balance: Double): Color {
    return when {
        balance > 0 -> Color(0xFF4CAF50)
        balance < 0 -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

// Extension function for AccountType display names
private val AccountType.displayName: String
    get() = when (this) {
        AccountType.CHECKING -> "Checking"
        AccountType.SAVINGS -> "Savings"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.INVESTMENT -> "Investment"
        AccountType.LOAN -> "Loan"
        AccountType.MORTGAGE -> "Mortgage"
    }

// Extension function for Money formatting
private fun Money.formatCurrency(): String {
    return "${this.currency} ${String.format("%.2f", this.toDouble())}"
}