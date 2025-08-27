package code.yousef.dari.shared.ui.components.financial.goal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Goal
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.utils.AmountFormatter

/**
 * Action buttons and controls for savings goal
 */
@Composable
fun SavingsGoalActions(
    goal: Goal,
    currentAmount: Money,
    onContribute: (Goal) -> Unit,
    onWithdraw: (Goal) -> Unit,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    modifier: Modifier = Modifier,
    allowContribution: Boolean = true,
    allowWithdraw: Boolean = true,
    isCompact: Boolean = false
) {
    if (isCompact) {
        CompactGoalActions(
            goal = goal,
            onContribute = onContribute,
            allowContribution = allowContribution
        )
    } else {
        FullGoalActions(
            goal = goal,
            currentAmount = currentAmount,
            onContribute = onContribute,
            onWithdraw = onWithdraw,
            onEdit = onEdit,
            onDelete = onDelete,
            allowContribution = allowContribution,
            allowWithdraw = allowWithdraw,
            modifier = modifier
        )
    }
}

@Composable
private fun FullGoalActions(
    goal: Goal,
    currentAmount: Money,
    onContribute: (Goal) -> Unit,
    onWithdraw: (Goal) -> Unit,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    allowContribution: Boolean,
    allowWithdraw: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primary actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contribute button
            Button(
                onClick = { onContribute(goal) },
                enabled = allowContribution && currentAmount.amount < goal.targetAmount.amount,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contribute")
            }
            
            // Withdraw button (if has savings)
            if (allowWithdraw && currentAmount.amount > 0) {
                OutlinedButton(
                    onClick = { onWithdraw(goal) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Withdraw")
                }
            }
        }
        
        // Secondary actions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getGoalSecondaryActions()) { action ->
                GoalActionChip(
                    action = action,
                    onClick = {
                        when (action.type) {
                            GoalActionType.EDIT -> onEdit(goal)
                            GoalActionType.DELETE -> onDelete(goal)
                            GoalActionType.SHARE -> { /* Handle share */ }
                            GoalActionType.REMINDER -> { /* Handle reminder */ }
                            GoalActionType.ANALYTICS -> { /* Handle analytics */ }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactGoalActions(
    goal: Goal,
    onContribute: (Goal) -> Unit,
    allowContribution: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onContribute(goal) },
            enabled = allowContribution
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Contribute",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        IconButton(onClick = { /* Handle more options */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Quick contribution suggestions
 */
@Composable
fun QuickContributionSuggestions(
    goal: Goal,
    currentAmount: Money,
    onContributeAmount: (Money) -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingAmount = goal.targetAmount.amount - currentAmount.amount
    val suggestions = generateContributionSuggestions(remainingAmount, goal.targetAmount.currency)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Contribute",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onContributeAmount(suggestion) },
                        label = {
                            Text(AmountFormatter.formatCompact(suggestion.amount, suggestion.currency))
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Goal milestone tracker
 */
@Composable
fun GoalMilestoneTracker(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier
) {
    val milestones = generateGoalMilestones(goal.targetAmount)
    val currentProgress = currentAmount.amount / goal.targetAmount.amount * 100
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Milestones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            milestones.forEach { milestone ->
                MilestoneItem(
                    milestone = milestone,
                    isCompleted = currentProgress >= milestone.percentage,
                    isCurrent = currentProgress >= milestone.percentage - 25 && currentProgress < milestone.percentage
                )
            }
        }
    }
}

@Composable
private fun MilestoneItem(
    milestone: GoalMilestone,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = milestone.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        Text(
            text = "${milestone.percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Data classes and utility functions
@Composable
private fun GoalActionChip(
    action: GoalAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(action.label) },
        leadingIcon = {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
    )
}

data class GoalAction(
    val type: GoalActionType,
    val label: String,
    val icon: ImageVector
)

enum class GoalActionType {
    EDIT, DELETE, SHARE, REMINDER, ANALYTICS
}

data class GoalMilestone(
    val percentage: Double,
    val title: String,
    val description: String
)

// Utility functions
private fun getGoalSecondaryActions(): List<GoalAction> {
    return listOf(
        GoalAction(GoalActionType.EDIT, "Edit", Icons.Default.Edit),
        GoalAction(GoalActionType.REMINDER, "Reminder", Icons.Default.Notifications),
        GoalAction(GoalActionType.ANALYTICS, "Analytics", Icons.Default.Analytics),
        GoalAction(GoalActionType.SHARE, "Share", Icons.Default.Share),
        GoalAction(GoalActionType.DELETE, "Delete", Icons.Default.Delete)
    )
}

private fun generateContributionSuggestions(remainingAmount: Double, currency: String): List<Money> {
    return listOf(
        Money(50.0, currency),
        Money(100.0, currency),
        Money(250.0, currency),
        Money(500.0, currency),
        Money(minOf(remainingAmount, 1000.0), currency)
    ).filter { it.amount <= remainingAmount }
}

private fun generateGoalMilestones(targetAmount: Money): List<GoalMilestone> {
    return listOf(
        GoalMilestone(25.0, "Getting Started", "First quarter complete"),
        GoalMilestone(50.0, "Halfway There", "Half of your goal reached"),
        GoalMilestone(75.0, "Almost Done", "Three quarters complete"),
        GoalMilestone(100.0, "Goal Achieved!", "Congratulations on reaching your goal")
    )
}