package code.yousef.dari.presentation.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import code.yousef.dari.shared.domain.models.*
import org.koin.androidx.compose.getViewModel

/**
 * Budget Screen - Main budget management interface
 * Features: budget overview, category cards, period selector, charts, templates
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = getViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val budgetHealth = remember(uiState.budgets, uiState.budgetStatuses) {
        viewModel.calculateOverallBudgetHealth()
    }
    
    var showCreateBudgetDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showPeriodSelector by remember { mutableStateOf(false) }
    var selectedBudgetForEdit by remember { mutableStateOf<Budget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getBudgetRecommendations()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        BudgetTopAppBar(
            onNavigateBack = onNavigateBack,
            onShowTemplates = { showTemplatesDialog = true },
            onCreateBudget = { showCreateBudgetDialog = true }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Budget Health Overview
                item {
                    BudgetHealthOverview(
                        budgetHealth = budgetHealth,
                        isLoading = uiState.isLoading
                    )
                }

                // Period Selector
                item {
                    BudgetPeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = viewModel::changePeriod,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category Filter
                item {
                    BudgetCategoryFilter(
                        selectedCategory = uiState.selectedCategory,
                        budgets = uiState.budgets,
                        onCategorySelected = viewModel::filterByCategory,
                        onClearFilter = viewModel::clearCategoryFilter
                    )
                }

                // Budget vs Actual Chart
                if (uiState.budgets.isNotEmpty() && uiState.budgetStatuses.isNotEmpty()) {
                    item {
                        BudgetActualChart(
                            budgets = uiState.filteredBudgets.take(5), // Show top 5 categories
                            statuses = uiState.budgetStatuses,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                // Budget Cards
                items(
                    items = uiState.filteredBudgets,
                    key = { it.id }
                ) { budget ->
                    val status = uiState.budgetStatuses.find { it.budgetId == budget.id }
                    
                    BudgetCard(
                        budget = budget,
                        status = status,
                        onEdit = { selectedBudgetForEdit = budget },
                        onDelete = { viewModel.deleteBudget(budget.id) },
                        onToggleActive = { viewModel.toggleBudgetActive(budget.id, !budget.isActive) },
                        onUpdateAlertThreshold = { threshold ->
                            viewModel.updateAlertThreshold(budget.id, threshold)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    )
                }

                // Empty State
                if (uiState.filteredBudgets.isEmpty() && !uiState.isLoading) {
                    item {
                        BudgetEmptyState(
                            onCreateBudget = { showCreateBudgetDialog = true },
                            onShowTemplates = { showTemplatesDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }

                // Budget Recommendations
                if (uiState.budgetRecommendations.isNotEmpty()) {
                    item {
                        BudgetRecommendations(
                            recommendations = uiState.budgetRecommendations,
                            onApplyRecommendation = { recommendation ->
                                viewModel.createBudget(
                                    name = recommendation.category,
                                    category = recommendation.category,
                                    amount = recommendation.suggestedAmount,
                                    period = uiState.selectedPeriod
                                )
                            }
                        )
                    }
                }

                // Footer spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or error dialog
            viewModel.dismissError()
        }
    }

    // Dialogs
    if (showCreateBudgetDialog) {
        CreateBudgetDialog(
            onDismiss = { showCreateBudgetDialog = false },
            onCreate = { name, category, amount, period, alertThreshold ->
                viewModel.createBudget(name, category, amount, period, alertThreshold)
                showCreateBudgetDialog = false
            }
        )
    }

    if (showTemplatesDialog) {
        BudgetTemplatesDialog(
            templates = viewModel.getBudgetTemplates(),
            onDismiss = { showTemplatesDialog = false },
            onApplyTemplate = { template ->
                viewModel.applyBudgetTemplate(template, uiState.selectedPeriod)
                showTemplatesDialog = false
            }
        )
    }

    selectedBudgetForEdit?.let { budget ->
        EditBudgetDialog(
            budget = budget,
            onDismiss = { selectedBudgetForEdit = null },
            onUpdate = { name, amount, alertThreshold ->
                viewModel.updateBudget(
                    budgetId = budget.id,
                    name = name,
                    amount = amount,
                    alertThreshold = alertThreshold
                )
                selectedBudgetForEdit = null
            }
        )
    }
}

/**
 * Budget Top App Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetTopAppBar(
    onNavigateBack: () -> Unit,
    onShowTemplates: () -> Unit,
    onCreateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "Budget Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            // Templates button
            IconButton(onClick = onShowTemplates) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Budget templates"
                )
            }
            
            // Add budget button
            IconButton(onClick = onCreateBudget) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create budget"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Budget Health Overview Card
 */
@Composable
private fun BudgetHealthOverview(
    budgetHealth: BudgetHealth,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (budgetHealth.healthLevel) {
                HealthLevel.EXCELLENT -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                HealthLevel.GOOD -> Color(0xFF8BC34A).copy(alpha = 0.1f)
                HealthLevel.FAIR -> Color(0xFFFF9800).copy(alpha = 0.1f)
                HealthLevel.POOR -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Health",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                BudgetHealthIndicator(
                    healthLevel = budgetHealth.healthLevel,
                    healthScore = budgetHealth.healthScore
                )
            }

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetMetricCard(
                    title = "Budgeted",
                    amount = budgetHealth.totalBudgeted,
                    color = MaterialTheme.colorScheme.primary
                )
                
                BudgetMetricCard(
                    title = "Spent",
                    amount = budgetHealth.totalSpent,
                    color = when {
                        budgetHealth.totalSpent.amount > budgetHealth.totalBudgeted.amount -> 
                            MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
                
                BudgetMetricCard(
                    title = "Remaining",
                    amount = budgetHealth.remainingAmount,
                    color = if (budgetHealth.remainingAmount.amount >= 0) 
                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }

            // Over Budget Warning
            if (budgetHealth.overBudgetCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "${budgetHealth.overBudgetCount} budget(s) over limit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Budget Period Selector
 */
@Composable
private fun BudgetPeriodSelector(
    selectedPeriod: BudgetPeriod,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(BudgetPeriod.values()) { period ->
            FilterChip(
                onClick = { onPeriodSelected(period) },
                label = { 
                    Text(
                        text = when (period) {
                            BudgetPeriod.WEEKLY -> "Weekly"
                            BudgetPeriod.MONTHLY -> "Monthly" 
                            BudgetPeriod.QUARTERLY -> "Quarterly"
                            BudgetPeriod.YEARLY -> "Yearly"
                            BudgetPeriod.CUSTOM -> "Custom"
                        }
                    )
                },
                selected = period == selectedPeriod,
                leadingIcon = if (period == selectedPeriod) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

/**
 * Budget Category Filter
 */
@Composable
private fun BudgetCategoryFilter(
    selectedCategory: String?,
    budgets: List<Budget>,
    onCategorySelected: (String) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = budgets.map { it.category }.distinct()
    
    if (categories.isNotEmpty()) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Clear filter chip
            if (selectedCategory != null) {
                item {
                    FilterChip(
                        onClick = onClearFilter,
                        label = { Text("All Categories") },
                        selected = false,
                        leadingIcon = {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
            
            // Category chips
            items(categories) { category ->
                FilterChip(
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    selected = category == selectedCategory,
                    leadingIcon = if (category == selectedCategory) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

/**
 * Budget vs Actual Chart
 */
@Composable
private fun BudgetActualChart(
    budgets: List<Budget>,
    statuses: List<BudgetStatus>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Budget vs Actual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Simple horizontal bar chart
            budgets.forEach { budget ->
                val status = statuses.find { it.budgetId == budget.id }
                if (status != null) {
                    BudgetComparisonBar(
                        budget = budget,
                        status = status,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Budget Comparison Bar
 */
@Composable
private fun BudgetComparisonBar(
    budget: Budget,
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Category name and amounts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = budget.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${status.spent.amount.toInt()} / ${budget.amount.amount.toInt()} ${budget.amount.currency}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = (status.percentageUsed / 100f).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                status.isOverBudget -> MaterialTheme.colorScheme.error
                status.percentageUsed > 80 -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Budget Health Indicator
 */
@Composable
private fun BudgetHealthIndicator(
    healthLevel: HealthLevel,
    healthScore: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (color, icon, text) = when (healthLevel) {
            HealthLevel.EXCELLENT -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Excellent")
            HealthLevel.GOOD -> Triple(Color(0xFF8BC34A), Icons.Default.ThumbUp, "Good") 
            HealthLevel.FAIR -> Triple(Color(0xFFFF9800), Icons.Default.Warning, "Fair")
            HealthLevel.POOR -> Triple(Color(0xFFF44336), Icons.Default.Error, "Poor")
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
        
        Text(
            text = "${healthScore.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Budget Metric Card
 */
@Composable
private fun BudgetMetricCard(
    title: String,
    amount: Money,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "${amount.amount.toInt()}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = amount.currency,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Budget Empty State
 */
@Composable
private fun BudgetEmptyState(
    onCreateBudget: () -> Unit,
    onShowTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "No budgets yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Create your first budget to start tracking your spending and reaching your financial goals",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onShowTemplates) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Template")
            }
            
            Button(onClick = onCreateBudget) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Budget")
            }
        }
    }
}

/**
 * Budget Recommendations
 */
@Composable
private fun BudgetRecommendations(
    recommendations: List<BudgetRecommendation>,
    onApplyRecommendation: (BudgetRecommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Budget Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.take(3).forEach { recommendation ->
                BudgetRecommendationItem(
                    recommendation = recommendation,
                    onApply = { onApplyRecommendation(recommendation) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Budget Recommendation Item
 */
@Composable
private fun BudgetRecommendationItem(
    recommendation: BudgetRecommendation,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${recommendation.suggestedAmount.amount.toInt()} ${recommendation.suggestedAmount.currency}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = recommendation.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(onClick = onApply) {
                Text("Apply")
            }
        }
    }
}