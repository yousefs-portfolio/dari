package code.yousef.dari.presentation.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import code.yousef.dari.R
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionType
import code.yousef.dari.shared.presentation.components.*
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * Transactions Screen
 * Displays transactions with filtering, searching, grouping, and infinite scrolling
 * Supports bulk operations, category editing, and receipt attachments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactionDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    var showFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showAmountRangePicker by remember { mutableStateOf(false) }

    // Infinite scroll effect
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex > totalItems - 5 && !uiState.isLoadingMore && !uiState.hasReachedEnd
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreTransactions()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.transactions_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back)
                    )
                }
            },
            actions = {
                // Search toggle
                IconButton(
                    onClick = { /* Toggle search */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_transactions)
                    )
                }
                
                // Filters toggle
                IconButton(
                    onClick = { showFilters = !showFilters }
                ) {
                    Badge(
                        containerColor = if (uiState.selectedCategories.isNotEmpty() || 
                                              uiState.dateRange != null || 
                                              uiState.amountRange != null) 
                                          MaterialTheme.colorScheme.primary 
                                        else Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_transactions)
                        )
                    }
                }
                
                // Bulk selection toggle
                IconButton(
                    onClick = { viewModel.toggleBulkSelectionMode() }
                ) {
                    Icon(
                        imageVector = if (uiState.isBulkSelectionMode) 
                                        Icons.Default.CheckCircle 
                                      else Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.bulk_selection),
                        tint = if (uiState.isBulkSelectionMode) 
                                 MaterialTheme.colorScheme.primary 
                               else LocalContentColor.current
                    )
                }
            }
        )

        // Search Bar
        AnimatedVisibility(
            visible = uiState.searchQuery.isNotEmpty() || uiState.isSearching,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.searchTransactions(it) },
                onSearch = { viewModel.searchTransactions(it) },
                active = false,
                onActiveChange = { },
                placeholder = { Text(stringResource(R.string.search_transactions_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {}
        }

        // Filter Section
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TransactionFiltersSection(
                uiState = uiState,
                onCategoryFilter = viewModel::filterByCategory,
                onDateRangeFilter = { showDateRangePicker = true },
                onAmountRangeFilter = { showAmountRangePicker = true },
                onClearFilters = viewModel::clearAllFilters,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bulk Selection Actions
        AnimatedVisibility(
            visible = uiState.isBulkSelectionMode && uiState.selectedTransactionIds.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BulkSelectionActions(
                selectedCount = uiState.selectedTransactionIds.size,
                onSelectAll = viewModel::selectAllVisibleTransactions,
                onClearSelection = viewModel::clearAllSelections,
                onBulkCategorize = { /* TODO: Implement bulk categorization */ },
                onBulkDelete = { /* TODO: Implement bulk deletion */ },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Transaction List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    ErrorMessage(
                        message = uiState.error,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.filteredTransactions.isEmpty() -> {
                    EmptyTransactionsState(
                        isSearching = uiState.isSearching,
                        hasFilters = uiState.selectedCategories.isNotEmpty() || 
                                   uiState.dateRange != null || 
                                   uiState.amountRange != null,
                        onAddTransaction = onNavigateToAddTransaction,
                        onClearFilters = viewModel::clearAllFilters,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Group transactions by date
                        uiState.groupedTransactions.forEach { (dateGroup, transactions) ->
                            item(key = "header_$dateGroup") {
                                TransactionDateHeader(
                                    date = dateGroup,
                                    transactionCount = transactions.size,
                                    totalAmount = transactions.sumOf { 
                                        if (it.type == TransactionType.CREDIT) it.amount.amount else 0.0 
                                    } - transactions.sumOf { 
                                        if (it.type == TransactionType.DEBIT) Math.abs(it.amount.amount) else 0.0 
                                    }
                                )
                            }
                            
                            items(
                                items = transactions,
                                key = { "transaction_${it.id}" }
                            ) { transaction ->
                                TransactionItem(
                                    transaction = transaction,
                                    isSelected = uiState.selectedTransactionIds.contains(transaction.id),
                                    isBulkSelectionMode = uiState.isBulkSelectionMode,
                                    onTransactionClick = { 
                                        if (uiState.isBulkSelectionMode) {
                                            viewModel.toggleTransactionSelection(transaction.id)
                                        } else {
                                            onNavigateToTransactionDetails(transaction.id)
                                        }
                                    },
                                    onTransactionLongClick = {
                                        if (!uiState.isBulkSelectionMode) {
                                            viewModel.toggleBulkSelectionMode()
                                            viewModel.toggleTransactionSelection(transaction.id)
                                        }
                                    },
                                    onCategoryEdit = { newCategory ->
                                        viewModel.categorizeTransaction(transaction.id, newCategory)
                                    }
                                )
                            }
                        }
                        
                        // Loading more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // Pull to refresh
            PullToRefreshContainer(
                state = rememberPullToRefreshState(),
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::syncTransactions,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToAddTransaction,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_transaction)
            )
        }
    }

    // Date Range Picker Dialog
    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDateRangeSelected = { startDate, endDate ->
                viewModel.filterByDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }

    // Amount Range Picker Dialog
    if (showAmountRangePicker) {
        AmountRangePickerDialog(
            onAmountRangeSelected = { minAmount, maxAmount ->
                viewModel.filterByAmount(minAmount, maxAmount)
                showAmountRangePicker = false
            },
            onDismiss = { showAmountRangePicker = false }
        )
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.dismissError()
        }
    }
}

@Composable
private fun TransactionFiltersSection(
    uiState: TransactionsUiState,
    onCategoryFilter: (String) -> Unit,
    onDateRangeFilter: () -> Unit,
    onAmountRangeFilter: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.filters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(getTransactionCategories()) { category ->
                    FilterChip(
                        onClick = { onCategoryFilter(category) },
                        label = { Text(category) },
                        selected = uiState.selectedCategories.contains(category)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Date and Amount filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDateRangeFilter,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.dateRange?.let { 
                            "${it.first} - ${it.second}"
                        } ?: stringResource(R.string.date_range)
                    )
                }
                
                OutlinedButton(
                    onClick = onAmountRangeFilter,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.amountRange?.let { 
                            "${it.first.amount} - ${it.second.amount}"
                        } ?: stringResource(R.string.amount_range)
                    )
                }
            }
            
            // Clear filters button
            if (uiState.selectedCategories.isNotEmpty() || 
                uiState.dateRange != null || 
                uiState.amountRange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.clear_all_filters))
                }
            }
        }
    }
}

@Composable
private fun BulkSelectionActions(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onBulkCategorize: () -> Unit,
    onBulkDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.select_all))
                }
                
                TextButton(onClick = onClearSelection) {
                    Text(stringResource(R.string.clear_selection))
                }
                
                TextButton(onClick = onBulkCategorize) {
                    Text(stringResource(R.string.categorize))
                }
                
                TextButton(onClick = onBulkDelete) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDateHeader(
    date: String,
    transactionCount: Int,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.transaction_count, transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = Money.fromDouble(totalAmount, "SAR").formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalAmount >= 0) 
                          MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyTransactionsState(
    isSearching: Boolean,
    hasFilters: Boolean,
    onAddTransaction: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Receipt,
        title = when {
            isSearching -> stringResource(R.string.no_search_results)
            hasFilters -> stringResource(R.string.no_filtered_transactions)
            else -> stringResource(R.string.no_transactions)
        },
        description = when {
            isSearching -> stringResource(R.string.try_different_search)
            hasFilters -> stringResource(R.string.try_clearing_filters)
            else -> stringResource(R.string.add_first_transaction)
        },
        actionText = when {
            hasFilters -> stringResource(R.string.clear_filters)
            else -> stringResource(R.string.add_transaction)
        },
        onAction = if (hasFilters) onClearFilters else onAddTransaction,
        modifier = modifier
    )
}

// Helper functions
private fun getTransactionCategories(): List<String> {
    return listOf(
        "Food & Dining",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Bills & Utilities",
        "Healthcare",
        "Education",
        "Income",
        "Investment",
        "Other"
    )
}

@Composable
fun DateRangePickerDialog(
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement date range picker dialog
    // This is a placeholder - implement with Material3 DateRangePicker when available
}

@Composable
fun AmountRangePickerDialog(
    onAmountRangeSelected: (Money, Money) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement amount range picker dialog
    // This is a placeholder - implement custom amount range picker
}