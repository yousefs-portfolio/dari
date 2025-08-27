package code.yousef.dari.presentation.budget.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.ui.components.*
import code.yousef.dari.shared.ui.components.budget.*

/**
 * Create Budget Dialog
 * Allows users to create new budgets with validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBudgetDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Money, BudgetPeriod, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var alertThreshold by remember { mutableStateOf(80.0) }
    
    val isValid = name.isNotBlank() && 
                  category.isNotBlank() && 
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
                Text(
                    text = "Create Budget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider()
                
                // Budget Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    placeholder = { Text("e.g., Monthly Groceries") },
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Category Selection
                CategorySelector(
                    selectedCategory = if (category.isNotBlank()) {
                        Category(
                            id = category,
                            name = category,
                            type = CategoryType.EXPENSE
                        )
                    } else null,
                    onCategorySelected = { selected ->
                        category = selected.name
                    },
                    label = "Category",
                    placeholder = "Select budget category"
                )
                
                // Amount Input
                AmountInput(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Budget Amount",
                    placeholder = "0.00",
                    currency = "SAR"
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
                            val budgetAmount = Money(
                                amount = amount.toDouble(),
                                currency = "SAR"
                            )
                            onCreate(name, category, budgetAmount, selectedPeriod, alertThreshold)
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
                        Text("Create")
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
            steps = 9, // 50%, 55%, 60%, ... 100%
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "You'll be notified when spending reaches this percentage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}