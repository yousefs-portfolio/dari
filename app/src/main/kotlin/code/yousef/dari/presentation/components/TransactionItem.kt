package code.yousef.dari.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.domain.models.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction icon
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = getTransactionIconColor(transaction.transactionType).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getTransactionIcon(transaction.transactionType),
                        contentDescription = null,
                        tint = getTransactionIconColor(transaction.transactionType),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.merchantName ?: transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (transaction.categoryName != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = transaction.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = formatTransactionDate(transaction.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Amount
            Text(
                text = formatTransactionAmount(transaction),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = getTransactionAmountColor(transaction.transactionType)
            )
        }
    }
}

@Composable
fun TransactionItemCompact(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction icon (smaller)
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                color = getTransactionIconColor(transaction.transactionType).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getTransactionIcon(transaction.transactionType),
                        contentDescription = null,
                        tint = getTransactionIconColor(transaction.transactionType),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.merchantName ?: transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                
                Text(
                    text = formatTransactionDate(transaction.transactionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Amount
            Text(
                text = formatTransactionAmount(transaction),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = getTransactionAmountColor(transaction.transactionType)
            )
        }
    }
}

private fun getTransactionIcon(type: TransactionType) = when (type) {
    TransactionType.DEBIT -> Icons.Filled.ArrowDownward
    TransactionType.CREDIT -> Icons.Filled.ArrowUpward
    TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    TransactionType.PAYMENT -> Icons.Filled.Payment
    TransactionType.ATM_WITHDRAWAL -> Icons.Filled.LocalAtm
    TransactionType.CHECK -> Icons.Filled.Receipt
    TransactionType.FEE -> Icons.Filled.MonetizationOn
    TransactionType.INTEREST -> Icons.Filled.TrendingUp
    TransactionType.DIVIDEND -> Icons.Filled.AccountBalance
    TransactionType.OTHER -> Icons.Filled.MoreHoriz
}

private fun getTransactionIconColor(type: TransactionType) = when (type) {
    TransactionType.DEBIT,
    TransactionType.ATM_WITHDRAWAL,
    TransactionType.PAYMENT,
    TransactionType.FEE -> Color(0xFFF44336) // Red for outgoing money
    
    TransactionType.CREDIT,
    TransactionType.INTEREST,
    TransactionType.DIVIDEND -> Color(0xFF4CAF50) // Green for incoming money
    
    TransactionType.TRANSFER -> Color(0xFF2196F3) // Blue for transfers
    TransactionType.CHECK -> Color(0xFF9C27B0) // Purple for checks
    TransactionType.OTHER -> Color(0xFF757575) // Gray for others
}

private fun getTransactionAmountColor(type: TransactionType) = when (type) {
    TransactionType.DEBIT,
    TransactionType.ATM_WITHDRAWAL,
    TransactionType.PAYMENT,
    TransactionType.FEE -> Color(0xFFF44336) // Red for outgoing money
    
    TransactionType.CREDIT,
    TransactionType.INTEREST,
    TransactionType.DIVIDEND -> Color(0xFF4CAF50) // Green for incoming money
    
    else -> Color(0xFF757575) // Gray for neutral transactions
}

private fun formatTransactionAmount(transaction: Transaction): String {
    val prefix = when (transaction.transactionType) {
        TransactionType.DEBIT,
        TransactionType.ATM_WITHDRAWAL,
        TransactionType.PAYMENT,
        TransactionType.FEE -> "-"
        
        TransactionType.CREDIT,
        TransactionType.INTEREST,
        TransactionType.DIVIDEND -> "+"
        
        else -> ""
    }
    
    return "$prefix${transaction.amount.currency} ${String.format("%.2f", transaction.amount.toDouble())}"
}

private fun formatTransactionDate(date: Instant): String {
    val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDate.monthNumber}/${localDate.dayOfMonth}"
}