package code.yousef.dari.presentation.budget.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.ui.components.*
import code.yousef.dari.shared.ui.components.budget.*

/**
 * Edit Budget Dialog
 * Allows users to modify existing budget settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetDialog(
    budget: Budget,
    onDismiss: () -> Unit,
    onUpdate: (Budget) -> Unit
) {
    var name by remember { mutableStateOf(budget.name) }
    var amount by remember { mutableStateOf(budget.amount.amount.toString()) }
    var selectedPeriod by remember { mutableStateOf(budget.period) }
    var alertThreshold by remember { mutableStateOf(budget.alertThreshold) }
    
    val isValid = name.isNotBlank() && 
                  amount.isNotBlank() && 
                  amount.toDoubleOrNull() != null &&
                  amount.toDoubleOrNull()!! > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Budget",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = budget.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Divider()
                
                // Budget Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Amount Input
                AmountInput(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Budget Amount",
                    placeholder = budget.amount.amount.toString(),
                    currency = budget.amount.currency
                )
                
                // Budget Period
                BudgetPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Alert Threshold
                BudgetAlertThresholdSlider(
                    threshold = alertThreshold,
                    onThresholdChange = { alertThreshold = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Budget Progress Summary
                BudgetProgressSummary(
                    budget = budget,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider()
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val updatedBudget = budget.copy(
                                name = name,
                                amount = Money(
                                    amount = amount.toDouble(),
                                    currency = budget.amount.currency
                                ),
                                period = selectedPeriod,
                                alertThreshold = alertThreshold
                            )
                            onUpdate(updatedBudget)
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update")
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertThresholdSlider(
    threshold: Double,
    onThresholdChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alert Threshold",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${threshold.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = threshold.toFloat(),
            onValueChange = { onThresholdChange(it.toDouble()) },
            valueRange = 50f..100f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "You'll be notified when spending reaches this percentage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetProgressSummary(
    budget: Budget,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${budget.spent.currency} ${budget.spent.amount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Remaining: ${budget.amount.currency} ${budget.amount.amount - budget.spent.amount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            val progressPercentage = (budget.spent.amount / budget.amount.amount * 100).toInt()
            
            LinearProgressIndicator(
                progress = { budget.spent.amount.toFloat() / budget.amount.amount.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    progressPercentage >= 100 -> MaterialTheme.colorScheme.error
                    progressPercentage >= budget.alertThreshold -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            
            Text(
                text = "$progressPercentage% used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}