package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Goal
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlin.math.abs
import kotlin.math.min

/**
 * Goal progress ring component showing savings progress
 */
@Composable
fun GoalProgressRing(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 12.dp,
    showDetails: Boolean = true,
    animated: Boolean = true
) {
    val progress = calculateGoalProgress(currentAmount, goal.targetAmount)
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else progress,
        animationSpec = tween(durationMillis = 1500),
        label = "goal_progress"
    )
    
    val progressColor by animateColorAsState(
        targetValue = getGoalProgressColor(progress, goal.type),
        animationSpec = tween(durationMillis = 500),
        label = "goal_color"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGoalProgressRing(
                    progress = animatedProgress,
                    color = progressColor,
                    strokeWidth = strokeWidth.toPx(),
                    size = this.size
                )
            }
            
            // Center content
            GoalRingCenter(
                goal = goal,
                progress = progress,
                color = progressColor
            )
        }
        
        if (showDetails) {
            Spacer(modifier = Modifier.height(12.dp))
            
            GoalProgressDetails(
                goal = goal,
                currentAmount = currentAmount,
                progress = progress
            )
        }
    }
}

@Composable
private fun GoalRingCenter(
    goal: Goal,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Goal type icon
        Icon(
            imageVector = getGoalTypeIcon(goal.type),
            contentDescription = goal.type.name,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress percentage
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        
        // Goal status
        Text(
            text = getGoalStatusText(progress),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoalProgressDetails(
    goal: Goal,
    currentAmount: Money,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Goal name
        Text(
            text = goal.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Current vs Target
        Text(
            text = "${formatMoney(currentAmount)} / ${formatMoney(goal.targetAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Time to goal
        goal.targetDate?.let { targetDate ->
            TimeToGoalIndicator(
                goal = goal,
                currentAmount = currentAmount,
                targetDate = targetDate
            )
        }
    }
}

@Composable
private fun TimeToGoalIndicator(
    goal: Goal,
    currentAmount: Money,
    targetDate: kotlinx.datetime.LocalDate
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = "Time to goal",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = calculateTimeToGoal(goal, currentAmount, targetDate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact goal progress ring for dashboard
 */
@Composable
fun CompactGoalProgressRing(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 6.dp
) {
    val progress = calculateGoalProgress(currentAmount, goal.targetAmount)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "compact_goal_progress"
    )
    
    val progressColor = getGoalProgressColor(progress, goal.type)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGoalProgressRing(
                progress = animatedProgress,
                color = progressColor,
                strokeWidth = strokeWidth.toPx(),
                size = this.size
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getGoalTypeIcon(goal.type),
                contentDescription = goal.type.name,
                tint = progressColor,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
    }
}

/**
 * Goal progress card with ring and details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalProgressCard(
    goal: Goal,
    currentAmount: Money,
    onGoalClick: (Goal) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onGoalClick(goal) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactGoalProgressRing(
                goal = goal,
                currentAmount = currentAmount,
                size = 70.dp,
                strokeWidth = 8.dp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${formatMoney(currentAmount)} saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${formatMoney(Money(
                        amount = goal.targetAmount.amount - currentAmount.amount,
                        currency = goal.targetAmount.currency
                    ))} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress indicator
                LinearProgressIndicator(
                    progress = min(calculateGoalProgress(currentAmount, goal.targetAmount), 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = getGoalProgressColor(
                        calculateGoalProgress(currentAmount, goal.targetAmount),
                        goal.type
                    ),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            }
            
            // Goal priority indicator
            if (goal.priority == Goal.Priority.HIGH) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = "High priority",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawGoalProgressRing(
    progress: Float,
    color: Color,
    strokeWidth: Float,
    size: Size
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = (min(size.width, size.height) - strokeWidth) / 2f
    val sweepAngle = 360f * min(progress, 1f)
    
    // Background circle
    drawCircle(
        color = Color.Gray.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // Progress arc
    if (progress > 0f) {
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    color.copy(alpha = 0.6f),
                    color,
                    color.copy(alpha = 0.8f)
                )
            ),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            size = Size(radius * 2f, radius * 2f),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
    }
    
    // Achievement indicator for completed goals
    if (progress >= 1f) {
        drawAchievementIndicator(center, radius, strokeWidth)
    }
}

private fun DrawScope.drawAchievementIndicator(
    center: Offset,
    radius: Float,
    strokeWidth: Float
) {
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.Gold.copy(alpha = 0.3f),
                Color.Transparent
            ),
            radius = radius + strokeWidth
        ),
        radius = radius + strokeWidth,
        center = center
    )
    
    // Achievement ring
    drawCircle(
        color = Color.Gold,
        radius = radius + strokeWidth / 2f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

/**
 * Get goal type icon
 */
private fun getGoalTypeIcon(type: Goal.Type): ImageVector {
    return when (type) {
        Goal.Type.SAVINGS -> Icons.Filled.Savings
        Goal.Type.DEBT_PAYOFF -> Icons.Filled.CreditCardOff
        Goal.Type.INVESTMENT -> Icons.Filled.TrendingUp
        Goal.Type.EMERGENCY_FUND -> Icons.Filled.Security
        Goal.Type.VACATION -> Icons.Filled.Flight
        Goal.Type.HOME_PURCHASE -> Icons.Filled.Home
        Goal.Type.EDUCATION -> Icons.Filled.School
        Goal.Type.RETIREMENT -> Icons.Filled.ElderlyWoman
        Goal.Type.OTHER -> Icons.Filled.FlagCircle
    }
}

/**
 * Get goal progress color based on type and progress
 */
@Composable
private fun getGoalProgressColor(progress: Float, type: Goal.Type): Color {
    val baseColor = when (type) {
        Goal.Type.SAVINGS -> DariTheme.financialColors.Income
        Goal.Type.DEBT_PAYOFF -> Color(0xFFE91E63) // Pink
        Goal.Type.INVESTMENT -> Color(0xFF9C27B0) // Purple
        Goal.Type.EMERGENCY_FUND -> Color(0xFFFF5722) // Deep Orange
        Goal.Type.VACATION -> Color(0xFF00BCD4) // Cyan
        Goal.Type.HOME_PURCHASE -> Color(0xFF8BC34A) // Light Green
        Goal.Type.EDUCATION -> Color(0xFF3F51B5) // Indigo
        Goal.Type.RETIREMENT -> Color(0xFF795548) // Brown
        Goal.Type.OTHER -> MaterialTheme.colorScheme.primary
    }
    
    return when {
        progress >= 1.0f -> Color.Gold
        progress >= 0.8f -> baseColor
        progress >= 0.5f -> baseColor.copy(alpha = 0.9f)
        else -> baseColor.copy(alpha = 0.7f)
    }
}

/**
 * Get goal status text
 */
private fun getGoalStatusText(progress: Float): String {
    return when {
        progress >= 1.0f -> "Completed!"
        progress >= 0.8f -> "Almost there"
        progress >= 0.5f -> "On track"
        progress >= 0.2f -> "Getting started"
        else -> "Just started"
    }
}

/**
 * Calculate goal progress
 */
private fun calculateGoalProgress(current: Money, target: Money): Float {
    if (target.amount == 0.0) return 0f
    return (abs(current.amount) / abs(target.amount)).toFloat()
}

/**
 * Calculate time to goal
 */
private fun calculateTimeToGoal(
    goal: Goal,
    currentAmount: Money,
    targetDate: kotlinx.datetime.LocalDate
): String {
    // TODO: Implement proper time calculation based on contribution rate
    return "6 months remaining"
}

/**
 * Format money for display
 */
private fun formatMoney(money: Money): String {
    return "${money.currency} ${String.format("%.0f", abs(money.amount))}"
}