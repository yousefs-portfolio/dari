package code.yousef.dari.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.presentation.dashboard.BudgetStatus

@Composable
fun BudgetProgressIndicator(
    spentAmount: Money,
    totalAmount: Money,
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val progress = if (totalAmount.toDouble() > 0) {
        (spentAmount.toDouble() / totalAmount.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    
    val progressColor = when (status) {
        BudgetStatus.ON_TRACK -> Color(0xFF4CAF50)
        BudgetStatus.NEAR_LIMIT -> Color(0xFFFF9800)
        BudgetStatus.OVER_BUDGET -> Color(0xFFF44336)
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spent",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = progressColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = spentAmount.formatCurrency(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = totalAmount.formatCurrency(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CircularBudgetProgress(
    spentAmount: Money,
    totalAmount: Money,
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val progress = if (totalAmount.toDouble() > 0) {
        (spentAmount.toDouble() / totalAmount.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    
    val progressColor = when (status) {
        BudgetStatus.ON_TRACK -> Color(0xFF4CAF50)
        BudgetStatus.NEAR_LIMIT -> Color(0xFFFF9800)
        BudgetStatus.OVER_BUDGET -> Color(0xFFF44336)
    }
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 8.dp
        )
        
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            strokeWidth = 8.dp
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
            Text(
                text = "spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// Extension function for Money formatting
private fun Money.formatCurrency(): String {
    return "${this.currency} ${String.format("%.2f", this.toDouble())}"
}