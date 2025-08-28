package code.yousef.dari.presentation.budget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Budget
import code.yousef.dari.shared.domain.models.BudgetStatus
import kotlin.math.min

/**
 * Budget Card Component
 * Displays individual budget information with progress, status, and actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetCard(
    budget: Budget,
    status: BudgetStatus?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onUpdateAlertThreshold: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }

    // Animation values
    val progressAnimated by animateFloatAsState(
        targetValue = if (status != null) (status.percentageUsed / 100f).coerceIn(0f, 1.2f) else 0f,
        label = "progress"
    )
    
    val cardColor by animateColorAsState(
        targetValue = when {
            !budget.isActive -> MaterialTheme.colorScheme.surfaceVariant
            status?.isOverBudget == true -> MaterialTheme.colorScheme.errorContainer
            status?.percentageUsed ?: 0.0 > budget.alertThreshold -> 
                MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "cardColor"
    )

    Card(
        onClick = { expanded = !expanded },
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Budget Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = budget.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Status and Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Over budget indicator
                    if (status?.isOverBudget == true) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Over budget",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Alert threshold reached
                    else if (status != null && status.percentageUsed > budget.alertThreshold) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert threshold reached",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Active/Inactive toggle
                    Switch(
                        checked = budget.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Budget Progress
            BudgetProgressSection(
                budget = budget,
                status = status,
                progressAnimated = progressAnimated
            )
            
            // Expanded Content
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                BudgetExpandedContent(
                    budget = budget,
                    status = status,
                    onEdit = onEdit,
                    onDelete = { showDeleteDialog = true },
                    onUpdateAlert = { showAlertDialog = true }
                )
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteBudgetDialog(
            budgetName = budget.name,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    
    // Alert Threshold Dialog
    if (showAlertDialog) {
        AlertThresholdDialog(
            currentThreshold = budget.alertThreshold,
            onConfirm = { newThreshold ->
                onUpdateAlertThreshold(newThreshold)
                showAlertDialog = false
            },
            onDismiss = { showAlertDialog = false }
        )
    }
}

/**
 * Budget Progress Section
 */
@Composable
private fun BudgetProgressSection(
    budget: Budget,
    status: BudgetStatus?,
    progressAnimated: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Amount Information
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Baseline
        ) {
            // Spent Amount
            Column {
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (status != null) {
                        "${status.spent.amount.toInt()} ${status.spent.currency}"
                    } else {
                        "0 ${budget.amount.currency}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        status?.isOverBudget == true -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            // Budget Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${budget.amount.amount.toInt()} ${budget.amount.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(min(progressAnimated, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            status?.isOverBudget == true -> MaterialTheme.colorScheme.error
                            status?.percentageUsed ?: 0.0 > budget.alertThreshold -> 
                                Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
            
            // Over-budget indicator
            if (progressAnimated > 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(min(progressAnimated - 1f, 0.2f))
                        .offset(x = 0.dp) // Start from the end of normal progress
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Percentage and Days Remaining
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (status != null) {
                    "${status.percentageUsed.toInt()}% used"
                } else {
                    "0% used"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    status?.isOverBudget == true -> MaterialTheme.colorScheme.error
                    status?.percentageUsed ?: 0.0 > budget.alertThreshold -> 
                        Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (status != null && status.daysRemaining > 0) {
                Text(
                    text = "${status.daysRemaining} days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Budget Expanded Content
 */
@Composable
private fun BudgetExpandedContent(
    budget: Budget,
    status: BudgetStatus?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider()
        
        // Budget Details
        BudgetDetailsSection(budget = budget, status = status)
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Edit Button
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
            
            // Alert Button
            OutlinedButton(
                onClick = onUpdateAlert,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Alert")
            }
            
            // Delete Button
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

/**
 * Budget Details Section
 */
@Composable
private fun BudgetDetailsSection(
    budget: Budget,
    status: BudgetStatus?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Period
        DetailRow(
            label = "Period",
            value = when (budget.period) {
                code.yousef.dari.shared.domain.models.BudgetPeriod.WEEKLY -> "Weekly"
                code.yousef.dari.shared.domain.models.BudgetPeriod.MONTHLY -> "Monthly"
                code.yousef.dari.shared.domain.models.BudgetPeriod.QUARTERLY -> "Quarterly"
                code.yousef.dari.shared.domain.models.BudgetPeriod.YEARLY -> "Yearly"
                code.yousef.dari.shared.domain.models.BudgetPeriod.CUSTOM -> "Custom"
            }
        )
        
        // Alert Threshold
        DetailRow(
            label = "Alert at",
            value = "${budget.alertThreshold.toInt()}%"
        )
        
        // Remaining Amount
        if (status != null) {
            DetailRow(
                label = "Remaining",
                value = "${status.remaining.amount.toInt()} ${status.remaining.currency}",
                valueColor = if (status.remaining.amount >= 0) 
                    Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
        
        // Start Date
        DetailRow(
            label = "Started",
            value = budget.startDate.toString()
        )
    }
}

/**
 * Detail Row Component
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Delete Budget Confirmation Dialog
 */
@Composable
private fun DeleteBudgetDialog(
    budgetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Delete Budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete the \"$budgetName\" budget? This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Alert Threshold Dialog
 */
@Composable
private fun AlertThresholdDialog(
    currentThreshold: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Alert Threshold",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Set when you want to be notified about this budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Alert at ${threshold.toInt()}% usage",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { threshold = it.toDouble() },
                    valueRange = 50f..100f,
                    steps = 9 // 50, 60, 70, 80, 90, 100
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "50%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "100%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(threshold) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}