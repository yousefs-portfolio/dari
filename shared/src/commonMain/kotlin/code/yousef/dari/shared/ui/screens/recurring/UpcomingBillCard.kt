package code.yousef.dari.shared.ui.screens.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.model.RecurringTransactionOccurrence
import code.yousef.dari.shared.ui.components.MoneyText
import code.yousef.dari.shared.ui.theme.DariColors
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingBillCard(
    occurrence: RecurringTransactionOccurrence,
    onClick: () -> Unit,
    onMarkPaid: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    isOverdue: Boolean = false,
    isDueToday: Boolean = false
) {
    var showActions by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> DariColors.warning.copy(alpha = 0.1f)
                isDueToday -> DariColors.expense.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isOverdue -> androidx.compose.foundation.BorderStroke(1.dp, DariColors.warning.copy(alpha = 0.3f))
            isDueToday -> androidx.compose.foundation.BorderStroke(1.dp, DariColors.expense.copy(alpha = 0.3f))
            else -> null
        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isOverdue) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Overdue",
                                tint = DariColors.warning,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Netflix Subscription", // This should come from the recurring transaction
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = formatDueDate(occurrence.scheduledDate, isOverdue, isDueToday),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isOverdue -> DariColors.warning
                            isDueToday -> DariColors.expense
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                MoneyText(
                    money = occurrence.amount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DariColors.expense
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip")
                }

                Button(
                    onClick = onMarkPaid,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Paid")
                }
            }
        }
    }
}

private fun formatDueDate(
    scheduledDate: kotlinx.datetime.Instant,
    isOverdue: Boolean,
    isDueToday: Boolean
): String {
    val now = Clock.System.now()
    val scheduledDateTime = scheduledDate.toLocalDateTime(TimeZone.currentSystemDefault())

    when {
        isOverdue -> {
            val daysDiff = (now.epochSeconds - scheduledDate.epochSeconds) / (24 * 60 * 60)
            return when {
                daysDiff == 1L -> "1 day overdue"
                daysDiff < 7 -> "$daysDiff days overdue"
                daysDiff < 30 -> "${daysDiff / 7} weeks overdue"
                else -> "Overdue since ${scheduledDateTime.dayOfMonth}/${scheduledDateTime.monthNumber}"
            }
        }

        isDueToday -> return "Due today"

        else -> {
            val daysDiff = (scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
            return when {
                daysDiff == 1L -> "Tomorrow"
                daysDiff < 7 -> "${scheduledDateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
                daysDiff < 30 -> "in ${daysDiff} days"
                daysDiff < 365 -> "${scheduledDateTime.month.name.take(3)} ${scheduledDateTime.dayOfMonth}"
                else -> "${scheduledDateTime.dayOfMonth}/${scheduledDateTime.monthNumber}/${scheduledDateTime.year}"
            }
        }
    }
}
