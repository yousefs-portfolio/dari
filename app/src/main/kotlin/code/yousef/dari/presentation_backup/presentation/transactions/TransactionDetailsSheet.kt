package code.yousef.dari.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.R
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionStatus
import code.yousef.dari.shared.domain.models.TransactionType
import code.yousef.dari.shared.presentation.components.*

/**
 * Transaction Details Bottom Sheet
 * Displays comprehensive transaction information with editing capabilities
 * Supports receipt attachments, category editing, and transaction actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onCategoryEdit: (String) -> Unit,
    onAttachReceipt: () -> Unit,
    onEditTransaction: () -> Unit,
    onDeleteTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategoryEditor by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Header
            item {
                TransactionHeader(
                    transaction = transaction,
                    onEdit = onEditTransaction
                )
            }

            // Transaction Amount
            item {
                TransactionAmountCard(
                    amount = transaction.amount,
                    type = transaction.type,
                    currency = transaction.amount.currency
                )
            }

            // Transaction Details
            item {
                TransactionDetailsCard(transaction = transaction)
            }

            // Category Section
            item {
                TransactionCategoryCard(
                    category = transaction.category,
                    onCategoryEdit = { showCategoryEditor = true }
                )
            }

            // Receipt Section
            item {
                TransactionReceiptCard(
                    hasReceipt = transaction.metadata.containsKey("receipt_url"),
                    receiptUrl = transaction.metadata["receipt_url"] as? String,
                    onAttachReceipt = onAttachReceipt,
                    onViewReceipt = { /* TODO: Implement receipt viewer */ }
                )
            }

            // Merchant Information
            if (transaction.merchant != null) {
                item {
                    TransactionMerchantCard(
                        merchant = transaction.merchant,
                        location = transaction.metadata["location"] as? String
                    )
                }
            }

            // Transaction Metadata
            if (transaction.metadata.isNotEmpty()) {
                item {
                    TransactionMetadataCard(
                        metadata = transaction.metadata
                    )
                }
            }

            // Action Buttons
            item {
                TransactionActionButtons(
                    onEdit = onEditTransaction,
                    onDelete = { showDeleteConfirmation = true }
                )
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Category Editor Dialog
    if (showCategoryEditor) {
        CategoryEditorDialog(
            currentCategory = transaction.category,
            onCategorySelected = { newCategory ->
                onCategoryEdit(newCategory)
                showCategoryEditor = false
            },
            onDismiss = { showCategoryEditor = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction()
                        showDeleteConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TransactionHeader(
    transaction: Transaction,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = transaction.date.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_transaction)
            )
        }
    }
}

@Composable
private fun TransactionAmountCard(
    amount: Money,
    type: TransactionType,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (type) {
                TransactionType.CREDIT -> MaterialTheme.colorScheme.primaryContainer
                TransactionType.DEBIT -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.amount),
                style = MaterialTheme.typography.labelMedium,
                color = when (type) {
                    TransactionType.CREDIT -> MaterialTheme.colorScheme.onPrimaryContainer
                    TransactionType.DEBIT -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
            
            Text(
                text = "${if (type == TransactionType.DEBIT) "-" else "+"}${amount.formattedAmount}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = when (type) {
                    TransactionType.CREDIT -> MaterialTheme.colorScheme.onPrimaryContainer
                    TransactionType.DEBIT -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

@Composable
private fun TransactionDetailsCard(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.transaction_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_id),
                value = transaction.id
            )

            TransactionDetailRow(
                label = stringResource(R.string.reference),
                value = transaction.reference
            )

            TransactionDetailRow(
                label = stringResource(R.string.status),
                value = transaction.status.name,
                valueColor = when (transaction.status) {
                    TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    TransactionStatus.PENDING -> MaterialTheme.colorScheme.secondary
                    TransactionStatus.FAILED -> MaterialTheme.colorScheme.error
                }
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_type),
                value = when (transaction.type) {
                    TransactionType.CREDIT -> stringResource(R.string.income)
                    TransactionType.DEBIT -> stringResource(R.string.expense)
                }
            )
        }
    }
}

@Composable
private fun TransactionCategoryCard(
    category: String,
    onCategoryEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategoryEdit() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_category),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TransactionReceiptCard(
    hasReceipt: Boolean,
    receiptUrl: String?,
    onAttachReceipt: () -> Unit,
    onViewReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.receipt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (hasReceipt) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewReceipt() }
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.view_receipt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onAttachReceipt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.attach_receipt))
                }
            }
        }
    }
}

@Composable
private fun TransactionMerchantCard(
    merchant: code.yousef.dari.shared.domain.models.Merchant,
    location: String?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.merchant_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TransactionDetailRow(
                label = stringResource(R.string.merchant_name),
                value = merchant.name
            )

            TransactionDetailRow(
                label = stringResource(R.string.merchant_category),
                value = merchant.category
            )

            location?.let { loc ->
                TransactionDetailRow(
                    label = stringResource(R.string.location),
                    value = loc
                )
            }
        }
    }
}

@Composable
private fun TransactionMetadataCard(
    metadata: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.additional_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            metadata.forEach { (key, value) ->
                if (key != "receipt_url" && key != "location") { // These are handled separately
                    TransactionDetailRow(
                        label = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                        value = value.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionActionButtons(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.edit))
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
private fun TransactionDetailRow(
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CategoryEditorDialog(
    currentCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            "Food & Dining",
            "Transportation",
            "Shopping",
            "Entertainment",
            "Bills & Utilities",
            "Healthcare",
            "Education",
            "Income",
            "Investment",
            "Groceries",
            "Travel",
            "Gifts",
            "Insurance",
            "Taxes",
            "Other"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_category)) },
        text = {
            LazyColumn {
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = category == currentCategory,
                            onClick = { onCategorySelected(category) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}