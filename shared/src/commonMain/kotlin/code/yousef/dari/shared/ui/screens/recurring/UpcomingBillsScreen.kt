package code.yousef.dari.shared.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.model.RecurringTransactionOccurrence
import code.yousef.dari.shared.ui.components.EmptyState
import code.yousef.dari.shared.ui.components.LoadingIndicator
import code.yousef.dari.shared.ui.components.MoneyText
import code.yousef.dari.shared.ui.theme.DariColors
import code.yousef.dari.shared.viewmodel.recurring.UpcomingBillsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingBillsScreen(
    onNavigateToRecurringDetails: (String) -> Unit,
    viewModel: UpcomingBillsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upcoming Bills") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Card
            if (uiState.upcomingOccurrences.isNotEmpty()) {
                UpcomingBillsSummary(
                    upcomingOccurrences = uiState.upcomingOccurrences,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.upcomingOccurrences.isEmpty() -> {
                    EmptyState(
                        title = "No Upcoming Bills",
                        subtitle = "All your recurring payments are up to date",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    UpcomingBillsList(
                        upcomingOccurrences = uiState.upcomingOccurrences,
                        onItemClick = onNavigateToRecurringDetails,
                        onMarkPaid = viewModel::markAsPaid,
                        onSkip = viewModel::skipOccurrence,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingBillsSummary(
    upcomingOccurrences: List<RecurringTransactionOccurrence>,
    modifier: Modifier = Modifier
) {
    val now = Clock.System.now()
    val overdueBills = upcomingOccurrences.filter { it.scheduledDate < now }
    val dueTodayBills = upcomingOccurrences.filter {
        val daysDiff = (it.scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
        daysDiff == 0L
    }
    val upcomingThisWeekBills = upcomingOccurrences.filter {
        val daysDiff = (it.scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
        daysDiff in 1..7
    }

    val totalAmount = upcomingOccurrences.sumOf { it.amount.amount }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bills Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (overdueBills.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Overdue bills",
                        tint = DariColors.warning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MoneyText(
                money = code.yousef.dari.shared.domain.model.Money(totalAmount, "SAR"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Total upcoming bills",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BillSummaryItem(
                    count = overdueBills.size,
                    label = "Overdue",
                    color = DariColors.warning,
                    modifier = Modifier.weight(1f)
                )

                BillSummaryItem(
                    count = dueTodayBills.size,
                    label = "Due Today",
                    color = DariColors.expense,
                    modifier = Modifier.weight(1f)
                )

                BillSummaryItem(
                    count = upcomingThisWeekBills.size,
                    label = "This Week",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BillSummaryItem(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun UpcomingBillsList(
    upcomingOccurrences: List<RecurringTransactionOccurrence>,
    onItemClick: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    onSkip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group by time periods
        val now = Clock.System.now()
        val overdueOccurrences = upcomingOccurrences.filter { it.scheduledDate < now }
        val todayOccurrences = upcomingOccurrences.filter {
            val daysDiff = (it.scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
            daysDiff == 0L
        }
        val tomorrowOccurrences = upcomingOccurrences.filter {
            val daysDiff = (it.scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
            daysDiff == 1L
        }
        val thisWeekOccurrences = upcomingOccurrences.filter {
            val daysDiff = (it.scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
            daysDiff in 2..7
        }
        val laterOccurrences = upcomingOccurrences.filter {
            val daysDiff = (it.scheduledDate.epochSeconds - now.epochSeconds) / (24 * 60 * 60)
            daysDiff > 7
        }

        if (overdueOccurrences.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Overdue (${overdueOccurrences.size})",
                    color = DariColors.warning
                )
            }

            items(overdueOccurrences, key = { it.id }) { occurrence ->
                UpcomingBillCard(
                    occurrence = occurrence,
                    isOverdue = true,
                    onClick = { onItemClick(occurrence.recurringTransactionId) },
                    onMarkPaid = { onMarkPaid(occurrence.id) },
                    onSkip = { onSkip(occurrence.id) }
                )
            }
        }

        if (todayOccurrences.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Due Today (${todayOccurrences.size})",
                    color = DariColors.expense
                )
            }

            items(todayOccurrences, key = { it.id }) { occurrence ->
                UpcomingBillCard(
                    occurrence = occurrence,
                    isDueToday = true,
                    onClick = { onItemClick(occurrence.recurringTransactionId) },
                    onMarkPaid = { onMarkPaid(occurrence.id) },
                    onSkip = { onSkip(occurrence.id) }
                )
            }
        }

        if (tomorrowOccurrences.isNotEmpty()) {
            item {
                SectionHeader(title = "Tomorrow (${tomorrowOccurrences.size})")
            }

            items(tomorrowOccurrences, key = { it.id }) { occurrence ->
                UpcomingBillCard(
                    occurrence = occurrence,
                    onClick = { onItemClick(occurrence.recurringTransactionId) },
                    onMarkPaid = { onMarkPaid(occurrence.id) },
                    onSkip = { onSkip(occurrence.id) }
                )
            }
        }

        if (thisWeekOccurrences.isNotEmpty()) {
            item {
                SectionHeader(title = "This Week (${thisWeekOccurrences.size})")
            }

            items(thisWeekOccurrences, key = { it.id }) { occurrence ->
                UpcomingBillCard(
                    occurrence = occurrence,
                    onClick = { onItemClick(occurrence.recurringTransactionId) },
                    onMarkPaid = { onMarkPaid(occurrence.id) },
                    onSkip = { onSkip(occurrence.id) }
                )
            }
        }

        if (laterOccurrences.isNotEmpty()) {
            item {
                SectionHeader(title = "Later (${laterOccurrences.size})")
            }

            items(laterOccurrences, key = { it.id }) { occurrence ->
                UpcomingBillCard(
                    occurrence = occurrence,
                    onClick = { onItemClick(occurrence.recurringTransactionId) },
                    onMarkPaid = { onMarkPaid(occurrence.id) },
                    onSkip = { onSkip(occurrence.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier.padding(vertical = 8.dp)
    )
}
