package code.yousef.dari.shared.ui.components.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.BudgetHealth
import code.yousef.dari.shared.domain.models.Money

/**
 * Budget health overview components
 * Extracted from BudgetScreen to improve modularity and reusability
 */

@Composable
fun BudgetHealthOverview(
    budgetHealth: BudgetHealth?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isLoading) {
            BudgetHealthLoadingState()
        } else if (budgetHealth != null) {
            BudgetHealthContent(budgetHealth = budgetHealth)
        } else {
            BudgetHealthEmptyState()
        }
    }
}

@Composable
private fun BudgetHealthContent(
    budgetHealth: BudgetHealth,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with health score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budget Health",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            BudgetHealthScore(
                score = budgetHealth.overallScore,
                modifier = Modifier.size(60.dp)
            )
        }
        
        // Health metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BudgetHealthMetric(
                title = "Total Budget",
                value = budgetHealth.totalBudget.formatAmount(),
                icon = Icons.Default.AccountBalance,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            BudgetHealthMetric(
                title = "Spent",
                value = budgetHealth.totalSpent.formatAmount(),
                icon = Icons.Default.TrendingDown,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            
            BudgetHealthMetric(
                title = "Remaining",
                value = budgetHealth.totalRemaining.formatAmount(),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Health indicators
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BudgetHealthIndicator(
                    label = "On Track",
                    count = budgetHealth.budgetsOnTrack,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            item {
                BudgetHealthIndicator(
                    label = "At Risk",
                    count = budgetHealth.budgetsAtRisk,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            item {
                BudgetHealthIndicator(
                    label = "Exceeded",
                    count = budgetHealth.budgetsExceeded,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BudgetHealthScore(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1500),
        label = "health_score"
    )
    
    val scoreColor = when {
        score >= 80 -> MaterialTheme.colorScheme.tertiary
        score >= 60 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBudgetHealthCircle(
                score = animatedScore,
                color = scoreColor
            )
        }
        
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = scoreColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun DrawScope.drawBudgetHealthCircle(
    score: Float,
    color: Color
) {
    val strokeWidth = 6.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // Background circle
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = radius,
        center = center
    )
    
    // Progress arc
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * score,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = Size(radius * 2, radius * 2)
    )
}

@Composable
private fun BudgetHealthMetric(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BudgetHealthIndicator(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BudgetHealthLoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "Loading budget health...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BudgetHealthEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        
        Text(
            text = "No budget data available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Text(
            text = "Create your first budget to see health metrics",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// Extension function for Money formatting
private fun Money.formatAmount(): String {
    return "${this.currency} ${this.amount}"
}