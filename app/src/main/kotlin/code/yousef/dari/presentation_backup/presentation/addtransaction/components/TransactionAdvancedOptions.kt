package code.yousef.dari.presentation.addtransaction.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.ui.components.*

/**
 * Advanced transaction options like split transactions, recurring setup, receipt attachment
 */
@Composable
fun TransactionAdvancedOptions(
    uiState: AddTransactionUiState,
    onToggleRecurring: () -> Unit,
    onToggleSplitTransaction: () -> Unit,
    onAddReceiptClick: () -> Unit,
    onRemoveReceipt: (String) -> Unit,
    onToggleAdvancedOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Advanced Options Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(onClick = onToggleAdvancedOptions) {
                    Icon(
                        imageVector = if (uiState.showAdvancedOptions) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null
                    )
                }
            }
            
            AnimatedVisibility(
                visible = uiState.showAdvancedOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Options Row
                    QuickOptionsRow(
                        isRecurring = uiState.isRecurring,
                        isSplitTransaction = uiState.isSplitTransaction,
                        hasReceipts = uiState.attachedReceipts.isNotEmpty(),
                        onToggleRecurring = onToggleRecurring,
                        onToggleSplitTransaction = onToggleSplitTransaction,
                        onAddReceiptClick = onAddReceiptClick
                    )
                    
                    // Recurring Transaction Setup
                    AnimatedVisibility(
                        visible = uiState.isRecurring,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        RecurringTransactionSetup(
                            recurringConfig = uiState.recurringConfig,
                            onRecurringConfigChange = { /* Handle config change */ }
                        )
                    }
                    
                    // Split Transaction Setup
                    AnimatedVisibility(
                        visible = uiState.isSplitTransaction,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SplitTransactionSetup(
                            splitTransactions = uiState.splitTransactions,
                            totalAmount = uiState.amount.toDoubleOrNull() ?: 0.0,
                            onAddSplit = { /* Handle add split */ },
                            onRemoveSplit = { /* Handle remove split */ },
                            onUpdateSplit = { _, _ -> /* Handle update split */ }
                        )
                    }
                    
                    // Receipt Attachments
                    if (uiState.attachedReceipts.isNotEmpty()) {
                        ReceiptAttachmentsList(
                            receipts = uiState.attachedReceipts,
                            onRemoveReceipt = onRemoveReceipt
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickOptionsRow(
    isRecurring: Boolean,
    isSplitTransaction: Boolean,
    hasReceipts: Boolean,
    onToggleRecurring: () -> Unit,
    onToggleSplitTransaction: () -> Unit,
    onAddReceiptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FilterChip(
                selected = isRecurring,
                onClick = onToggleRecurring,
                label = { Text("Recurring") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        item {
            FilterChip(
                selected = isSplitTransaction,
                onClick = onToggleSplitTransaction,
                label = { Text("Split") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        item {
            FilterChip(
                selected = hasReceipts,
                onClick = onAddReceiptClick,
                label = { Text("Receipt") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun RecurringTransactionSetup(
    recurringConfig: RecurringTransactionConfig?,
    onRecurringConfigChange: (RecurringTransactionConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recurring Transaction Setup",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "This transaction will repeat automatically based on your settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Frequency selector, end date configuration, etc.
            // This would contain FrequencySelector and EndDateConfiguration components
        }
    }
}

@Composable
private fun SplitTransactionSetup(
    splitTransactions: List<SplitTransaction>,
    totalAmount: Double,
    onAddSplit: () -> Unit,
    onRemoveSplit: (String) -> Unit,
    onUpdateSplit: (String, SplitTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Split Transaction",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                val allocatedAmount = splitTransactions.sumOf { it.amount.amount }
                val remaining = totalAmount - allocatedAmount
                
                Text(
                    text = "Remaining: ${"%.2f".format(remaining)} SAR",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Split transaction items would go here
            // Each item would have amount, category, description fields
            
            OutlinedButton(
                onClick = onAddSplit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Split")
            }
        }
    }
}

@Composable
private fun ReceiptAttachmentsList(
    receipts: List<ReceiptAttachment>,
    onRemoveReceipt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Attached Receipts (${receipts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            receipts.forEach { receipt ->
                ReceiptAttachmentItem(
                    receipt = receipt,
                    onRemove = { onRemoveReceipt(receipt.id) }
                )
            }
        }
    }
}

@Composable
private fun ReceiptAttachmentItem(
    receipt: ReceiptAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = receipt.fileName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove receipt",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Placeholder data classes - these would be defined in domain models
data class RecurringTransactionConfig(
    val frequency: TransactionFrequency,
    val endDate: kotlinx.datetime.LocalDate?
)

data class SplitTransaction(
    val id: String,
    val amount: Money,
    val category: Category,
    val description: String
)

data class ReceiptAttachment(
    val id: String,
    val fileName: String,
    val filePath: String
)

enum class TransactionFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}