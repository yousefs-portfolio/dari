package code.yousef.dari.shared.ui.components.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.BudgetPeriod

/**
 * Budget period selector component
 * Extracted from BudgetScreen to promote reusability
 */

@Composable
fun BudgetPeriodSelector(
    selectedPeriod: BudgetPeriod,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = remember {
        listOf(
            PeriodOption(BudgetPeriod.CURRENT_MONTH, "This Month", Icons.Default.CalendarMonth),
            PeriodOption(BudgetPeriod.LAST_MONTH, "Last Month", Icons.Default.History),
            PeriodOption(BudgetPeriod.CURRENT_QUARTER, "Quarter", Icons.Default.CalendarViewMonth),
            PeriodOption(BudgetPeriod.CURRENT_YEAR, "Year", Icons.Default.CalendarViewWeek),
            PeriodOption(BudgetPeriod.CUSTOM, "Custom", Icons.Default.DateRange)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Budget Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(periods) { period ->
                    BudgetPeriodChip(
                        period = period,
                        isSelected = selectedPeriod == period.type,
                        onClick = { onPeriodSelected(period.type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetPeriodChip(
    period: PeriodOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = period.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        
        Text(
            text = period.label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun BudgetPeriodHeader(
    selectedPeriod: BudgetPeriod,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousPeriod) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous period"
                )
            }
            
            Text(
                text = formatPeriodTitle(selectedPeriod),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onNextPeriod) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next period"
                )
            }
        }
    }
}

@Composable
fun CompactBudgetPeriodSelector(
    selectedPeriod: BudgetPeriod,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = formatPeriodTitle(selectedPeriod),
            onValueChange = { },
            readOnly = true,
            label = { Text("Period") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BudgetPeriod.values().forEach { period ->
                DropdownMenuItem(
                    text = { Text(formatPeriodTitle(period)) },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

private data class PeriodOption(
    val type: BudgetPeriod,
    val label: String,
    val icon: ImageVector
)

private fun formatPeriodTitle(period: BudgetPeriod): String {
    return when (period) {
        BudgetPeriod.CURRENT_MONTH -> "This Month"
        BudgetPeriod.LAST_MONTH -> "Last Month"
        BudgetPeriod.CURRENT_QUARTER -> "Current Quarter"
        BudgetPeriod.CURRENT_YEAR -> "This Year"
        BudgetPeriod.CUSTOM -> "Custom Period"
    }
}