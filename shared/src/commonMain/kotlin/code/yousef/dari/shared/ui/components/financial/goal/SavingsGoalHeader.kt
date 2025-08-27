package code.yousef.dari.shared.ui.components.financial.goal

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Goal
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.utils.DateFormatter
import code.yousef.dari.shared.ui.utils.AmountFormatter

/**
 * Header section for savings goal card
 */
@Composable
fun SavingsGoalHeader(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier,
    showTargetDate: Boolean = true,
    isCompact: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Goal name and icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getGoalIcon(goal.category),
                    contentDescription = null,
                    tint = getGoalColor(goal.category),
                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                )
                
                Text(
                    text = goal.name,
                    style = if (isCompact) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Target amount
            Text(
                text = "Target: ${AmountFormatter.format(goal.targetAmount.amount, goal.targetAmount.currency)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Target date (if not compact)
            if (!isCompact && showTargetDate) {
                goal.targetDate?.let { targetDate ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Text(
                            text = "by ${DateFormatter.formatMedium(targetDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Priority indicator
        if (!isCompact) {
            GoalPriorityIndicator(
                priority = goal.priority,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun GoalPriorityIndicator(
    priority: Goal.Priority,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (priority) {
        Goal.Priority.HIGH -> MaterialTheme.colorScheme.error to Icons.Default.PriorityHigh
        Goal.Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary to Icons.Default.Remove
        Goal.Priority.LOW -> MaterialTheme.colorScheme.outline to Icons.Default.KeyboardArrowDown
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            
            Text(
                text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Utility functions for goal visualization
private fun getGoalIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "house", "home" -> Icons.Default.Home
        "car", "vehicle" -> Icons.Default.DirectionsCar
        "vacation", "travel" -> Icons.Default.Flight
        "wedding" -> Icons.Default.Favorite
        "education" -> Icons.Default.School
        "emergency" -> Icons.Default.Security
        "investment" -> Icons.Default.TrendingUp
        "gadget", "electronics" -> Icons.Default.Smartphone
        else -> Icons.Default.Savings
    }
}

private fun getGoalColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category.lowercase()) {
        "house", "home" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "car", "vehicle" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        "vacation", "travel" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        "wedding" -> androidx.compose.ui.graphics.Color(0xFFE91E63)
        "education" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        "emergency" -> androidx.compose.ui.graphics.Color(0xFFF44336)
        "investment" -> androidx.compose.ui.graphics.Color(0xFF009688)
        "gadget", "electronics" -> androidx.compose.ui.graphics.Color(0xFF607D8B)
        else -> androidx.compose.ui.graphics.Color(0xFF3F51B5)
    }
}