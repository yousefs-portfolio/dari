package code.yousef.dari.shared.ui.components.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.TransactionType

/**
 * Transaction type selector component
 * Extracted to promote reusability and reduce screen complexity
 */
@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransactionTypeOption(
                    type = TransactionType.EXPENSE,
                    isSelected = selectedType == TransactionType.EXPENSE,
                    onSelected = { onTypeSelected(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f)
                )
                
                TransactionTypeOption(
                    type = TransactionType.INCOME,
                    isSelected = selectedType == TransactionType.INCOME,
                    onSelected = { onTypeSelected(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeOption(
    type: TransactionType,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        when (type) {
            TransactionType.EXPENSE -> MaterialTheme.colorScheme.errorContainer
            TransactionType.INCOME -> MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        when (type) {
            TransactionType.EXPENSE -> MaterialTheme.colorScheme.onErrorContainer
            TransactionType.INCOME -> MaterialTheme.colorScheme.onPrimaryContainer
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = if (isSelected) {
        when (type) {
            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.outline
    }
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelected() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = when (type) {
                TransactionType.EXPENSE -> Icons.Default.TrendingDown
                TransactionType.INCOME -> Icons.Default.TrendingUp
            },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = when (type) {
                TransactionType.EXPENSE -> "Expense"
                TransactionType.INCOME -> "Income"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        
        Text(
            text = when (type) {
                TransactionType.EXPENSE -> "Money out"
                TransactionType.INCOME -> "Money in"
            },
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TransactionTypeTabs(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = when (selectedType) {
            TransactionType.EXPENSE -> 0
            TransactionType.INCOME -> 1
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Tab(
            selected = selectedType == TransactionType.EXPENSE,
            onClick = { onTypeSelected(TransactionType.EXPENSE) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Expense")
                }
            }
        )
        
        Tab(
            selected = selectedType == TransactionType.INCOME,
            onClick = { onTypeSelected(TransactionType.INCOME) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Income")
                }
            }
        )
    }
}