package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Category
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Transaction item component for lists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    showAccount: Boolean = false,
    showDate: Boolean = false,
    showReceipt: Boolean = true,
    isSelected: Boolean = false,
    onLongClick: ((Transaction) -> Unit)? = null
) {
    Card(
        onClick = { onTransactionClick(transaction) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction icon/category
            TransactionIcon(
                transaction = transaction,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Amount
                    Text(
                        text = formatTransactionAmount(transaction.amount),
                        style = DariTheme.financialTextStyles.TransactionAmount,
                        color = getTransactionAmountColor(transaction),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Secondary info row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category
                        transaction.categoryName?.let { categoryName ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = getCategoryColor(transaction).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getCategoryColor(transaction),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            
                            if (showAccount || showDate || showReceipt) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        
                        // Account name (if showing)
                        if (showAccount && transaction.accountName != null) {
                            Text(
                                text = transaction.accountName!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (showDate || showReceipt) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Date (if showing)
                        if (showDate) {
                            Text(
                                text = formatTransactionDate(transaction.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Status indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Receipt indicator
                        if (showReceipt && transaction.hasReceipt) {
                            Icon(
                                imageVector = Icons.Filled.Receipt,
                                contentDescription = "Has receipt",
                                modifier = Modifier.size(14.dp),
                                tint = DariTheme.financialColors.Income
                            )
                        }
                        
                        // Pending indicator
                        if (transaction.isPending) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Pending",
                                modifier = Modifier.size(14.dp),
                                tint = DariTheme.financialColors.Warning
                            )
                        }
                        
                        // Recurring indicator
                        if (transaction.isRecurring) {
                            Icon(
                                imageVector = Icons.Filled.Repeat,
                                contentDescription = "Recurring",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact transaction item for dense lists
 */
@Composable
fun CompactTransactionItem(
    transaction: Transaction,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = transaction.description,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = transaction.categoryName ?: "Uncategorized",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            TransactionIcon(
                transaction = transaction,
                modifier = Modifier.size(32.dp)
            )
        },
        trailingContent = {
            Text(
                text = formatTransactionAmount(transaction.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = getTransactionAmountColor(transaction),
                fontWeight = FontWeight.Medium
            )
        },
        modifier = modifier.clickable { onTransactionClick(transaction) }
    )
}

/**
 * Transaction item with expandable details
 */
@Composable
fun ExpandableTransactionItem(
    transaction: Transaction,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Collapsed content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransactionIcon(
                    transaction = transaction,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = transaction.categoryName ?: "Uncategorized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatTransactionAmount(transaction.amount),
                        style = DariTheme.financialTextStyles.TransactionAmount,
                        color = getTransactionAmountColor(transaction),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    TransactionDetails(transaction = transaction)
                    
                    // Action buttons
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { onTransactionClick(transaction) }
                        ) {
                            Text("View Details")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Transaction icon based on type and category
 */
@Composable
private fun TransactionIcon(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val backgroundColor = getCategoryColor(transaction).copy(alpha = 0.12f)
    val iconColor = getCategoryColor(transaction)
    val icon = getTransactionIcon(transaction)
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = transaction.categoryName,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Transaction details for expanded view
 */
@Composable
private fun TransactionDetails(transaction: Transaction) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailRow("Date", formatTransactionDate(transaction.date))
        
        transaction.accountName?.let { accountName ->
            DetailRow("Account", accountName)
        }
        
        transaction.merchantName?.let { merchant ->
            DetailRow("Merchant", merchant)
        }
        
        if (transaction.notes.isNotEmpty()) {
            DetailRow("Notes", transaction.notes)
        }
        
        if (transaction.hasReceipt) {
            DetailRow("Receipt", "Available")
        }
        
        if (transaction.isPending) {
            DetailRow("Status", "Pending")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f)
        )
    }
}

/**
 * Get transaction icon based on category and type
 */
private fun getTransactionIcon(transaction: Transaction): ImageVector {
    return when (transaction.type) {
        Transaction.TransactionType.INCOME -> Icons.Filled.TrendingUp
        Transaction.TransactionType.EXPENSE -> {
            when (transaction.categoryName?.lowercase()) {
                "food", "food & dining", "restaurants" -> Icons.Filled.Restaurant
                "transportation", "transport", "fuel" -> Icons.Filled.DirectionsCar
                "shopping", "retail" -> Icons.Filled.ShoppingCart
                "bills", "utilities" -> Icons.Filled.Receipt
                "entertainment" -> Icons.Filled.Movie
                "health", "medical" -> Icons.Filled.LocalHospital
                "education" -> Icons.Filled.School
                "home", "housing" -> Icons.Filled.Home
                else -> Icons.Filled.TrendingDown
            }
        }
        Transaction.TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    }
}

/**
 * Get category color
 */
@Composable
private fun getCategoryColor(transaction: Transaction): Color {
    return when (transaction.type) {
        Transaction.TransactionType.INCOME -> DariTheme.financialColors.Income
        Transaction.TransactionType.EXPENSE -> DariTheme.financialColors.Expense
        Transaction.TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
    }
}

/**
 * Get transaction amount color
 */
@Composable
private fun getTransactionAmountColor(transaction: Transaction): Color {
    return when (transaction.type) {
        Transaction.TransactionType.INCOME -> DariTheme.financialColors.Income
        Transaction.TransactionType.EXPENSE -> DariTheme.financialColors.Expense
        Transaction.TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
    }
}

/**
 * Format transaction amount with sign
 */
private fun formatTransactionAmount(amount: Money): String {
    val sign = if (amount.amount >= 0) "+" else ""
    return "$sign${amount.currency} ${String.format("%.2f", amount.amount)}"
}

/**
 * Format transaction date
 */
private fun formatTransactionDate(date: LocalDateTime): String {
    // TODO: Implement proper date formatting
    return "${date.date}"
}