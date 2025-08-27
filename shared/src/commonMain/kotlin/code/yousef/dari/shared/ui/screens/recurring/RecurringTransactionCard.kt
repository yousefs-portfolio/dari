package code.yousef.dari.shared.ui.screens.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.model.RecurringTransaction
import code.yousef.dari.shared.domain.model.RecurringTransactionStatus
import code.yousef.dari.shared.ui.components.MoneyText
import code.yousef.dari.shared.ui.theme.DariColors
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionCard(
    transaction: RecurringTransaction,
    onToggleStatus: (String) -> Unit,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (transaction.status) {
                RecurringTransactionStatus.ACTIVE -> MaterialTheme.colorScheme.surface
                RecurringTransactionStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                RecurringTransactionStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                RecurringTransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (transaction.description.isNotBlank() && transaction.description != transaction.merchantName) {
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onToggleStatus(transaction.id) },
                    enabled = transaction.status != RecurringTransactionStatus.COMPLETED &&
                        transaction.status != RecurringTransactionStatus.CANCELLED
                ) {
                    Icon(
                        imageVector = when (transaction.status) {
                            RecurringTransactionStatus.ACTIVE -> Icons.Default.Pause
                            RecurringTransactionStatus.PAUSED -> Icons.Default.PlayArrow
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = when (transaction.status) {
                            RecurringTransactionStatus.ACTIVE -> "Pause"
                            RecurringTransactionStatus.PAUSED -> "Resume"
                            else -> null
                        },
                        tint = when (transaction.status) {
                            RecurringTransactionStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                            RecurringTransactionStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                    MoneyText(
                        money = transaction.amount,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (transaction.amount.amount >= 0.toBigDecimal()) {
                            DariColors.income
                        } else {
                            DariColors.expense
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = transaction.frequency.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    RecurringTransactionStatusChip(
                        status = transaction.status,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "Next: ${formatNextDueDate(transaction.nextDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress indicator for limited occurrences
            transaction.maxOccurrences?.let { maxOccurrences ->
                LinearProgressIndicator(
                    progress = { transaction.totalOccurrences.toFloat() / maxOccurrences.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                Text(
                    text = "${transaction.totalOccurrences} of $maxOccurrences completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecurringTransactionStatusChip(
    status: RecurringTransactionStatus,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        RecurringTransactionStatus.ACTIVE -> "Active" to MaterialTheme.colorScheme.primary
        RecurringTransactionStatus.PAUSED -> "Paused" to MaterialTheme.colorScheme.secondary
        RecurringTransactionStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.tertiary
        RecurringTransactionStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatNextDueDate(nextDueDate: kotlinx.datetime.Instant): String {
    val now = Clock.System.now()
    val nextDueDateTime = nextDueDate.toLocalDateTime(TimeZone.currentSystemDefault())
    val nowDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())

    val daysDiff = (nextDueDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)

    return when {
        daysDiff < 0 -> "Overdue"
        daysDiff == 0L -> "Today"
        daysDiff == 1L -> "Tomorrow"
        daysDiff < 7 -> "${nextDueDateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
        daysDiff < 30 -> "in ${daysDiff} days"
        daysDiff < 365 -> "${nextDueDateTime.month.name.take(3)} ${nextDueDateTime.dayOfMonth}"
        else -> "${nextDueDateTime.dayOfMonth}/${nextDueDateTime.monthNumber}/${nextDueDateTime.year}"
    }
}
