package code.yousef.dari.shared.ui.components.financial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Goal
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.components.financial.goal.*

/**
 * Enhanced savings goal card - Refactored for better maintainability
 * Components are now extracted into separate files for better organization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalCard(
    goal: Goal,
    currentAmount: Money,
    onGoalClick: (Goal) -> Unit,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    isCompact: Boolean = false,
    allowContribution: Boolean = true,
    onContribute: ((Goal) -> Unit)? = null,
    onWithdraw: ((Goal) -> Unit)? = null,
    onEdit: ((Goal) -> Unit)? = null,
    onDelete: ((Goal) -> Unit)? = null
) {
    Card(
        onClick = { onGoalClick(goal) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (isCompact) {
            CompactSavingsContent(
                goal = goal,
                currentAmount = currentAmount,
                onContribute = onContribute
            )
        } else {
            FullSavingsContent(
                goal = goal,
                currentAmount = currentAmount,
                showDetails = showDetails,
                allowContribution = allowContribution,
                onContribute = onContribute,
                onWithdraw = onWithdraw,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun FullSavingsContent(
    goal: Goal,
    currentAmount: Money,
    showDetails: Boolean,
    allowContribution: Boolean,
    onContribute: ((Goal) -> Unit)?,
    onWithdraw: ((Goal) -> Unit)?,
    onEdit: ((Goal) -> Unit)?,
    onDelete: ((Goal) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with goal info and priority
        SavingsGoalHeader(
            goal = goal,
            currentAmount = currentAmount,
            showTargetDate = showDetails,
            isCompact = false
        )
        
        // Progress visualization
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circular progress ring
            SavingsProgressRing(
                goal = goal,
                currentAmount = currentAmount,
                size = 100.dp,
                strokeWidth = 10.dp
            )
            
            // Additional details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SavingsProgressBar(
                    goal = goal,
                    currentAmount = currentAmount,
                    showLabels = true
                )
                
                if (showDetails) {
                    SavingsGoalInsights(
                        goal = goal,
                        currentAmount = currentAmount
                    )
                }
            }
        }
        
        // Actions (if handlers are provided)
        if (onContribute != null || onWithdraw != null || onEdit != null || onDelete != null) {
            SavingsGoalActions(
                goal = goal,
                currentAmount = currentAmount,
                onContribute = onContribute ?: { },
                onWithdraw = onWithdraw ?: { },
                onEdit = onEdit ?: { },
                onDelete = onDelete ?: { },
                allowContribution = allowContribution,
                allowWithdraw = currentAmount.amount > 0,
                isCompact = false
            )
        }
    }
}

@Composable
private fun CompactSavingsContent(
    goal: Goal,
    currentAmount: Money,
    onContribute: ((Goal) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Compact progress ring
        SavingsProgressRing(
            goal = goal,
            currentAmount = currentAmount,
            size = 60.dp,
            strokeWidth = 6.dp,
            showAmount = false
        )
        
        // Goal info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            SavingsGoalHeader(
                goal = goal,
                currentAmount = currentAmount,
                showTargetDate = false,
                isCompact = true
            )
            
            SavingsProgressBar(
                goal = goal,
                currentAmount = currentAmount,
                height = 4.dp
            )
        }
        
        // Compact actions
        if (onContribute != null) {
            SavingsGoalActions(
                goal = goal,
                currentAmount = currentAmount,
                onContribute = onContribute,
                onWithdraw = { },
                onEdit = { },
                onDelete = { },
                isCompact = true
            )
        }
    }
}

/**
 * Insights and analytics section for the goal
 */
@Composable
private fun SavingsGoalInsights(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier
) {
    val remainingAmount = goal.targetAmount.amount - currentAmount.amount
    val daysToTarget = goal.targetDate?.let { targetDate ->
        // Calculate days between now and target date
        // This is a simplified calculation
        30 // Placeholder
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (remainingAmount > 0) {
            Text(
                text = "Remaining: ${remainingAmount.format()} ${goal.targetAmount.currency}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        daysToTarget?.let { days ->
            if (days > 0 && remainingAmount > 0) {
                val dailyTarget = remainingAmount / days
                Text(
                    text = "Save ${dailyTarget.format()} ${goal.targetAmount.currency}/day to reach goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Extension function for number formatting
private fun Double.format(): String {
    return if (this % 1.0 == 0.0) {
        this.toInt().toString()
    } else {
        String.format("%.2f", this)
    }
}