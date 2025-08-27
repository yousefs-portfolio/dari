package code.yousef.dari.shared.ui.components.financial.goal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Goal
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.utils.AmountFormatter
import code.yousef.dari.shared.ui.utils.ProgressUtils
import kotlin.math.*

/**
 * Animated circular progress ring for savings goal
 */
@Composable
fun SavingsProgressRing(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 12.dp,
    showPercentage: Boolean = true,
    showAmount: Boolean = true
) {
    val progress = ProgressUtils.calculateGoalProgress(currentAmount, goal.targetAmount) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1200),
        label = "savings_progress"
    )
    
    val progressColor = getSavingsProgressColor(progress * 100, goal)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Progress Ring
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawSavingsProgressRing(
                progress = animatedProgress,
                strokeWidth = strokeWidth.toPx(),
                progressColor = progressColor,
                backgroundColor = Color.Gray.copy(alpha = 0.1f)
            )
        }
        
        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showPercentage) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                    textAlign = TextAlign.Center
                )
            }
            
            if (showAmount) {
                Text(
                    text = AmountFormatter.formatCompact(currentAmount.amount, currentAmount.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Compact linear progress indicator for savings
 */
@Composable
fun SavingsProgressBar(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 8.dp,
    showLabels: Boolean = false
) {
    val progress = ProgressUtils.calculateGoalProgress(currentAmount, goal.targetAmount) / 100f
    val progressColor = getSavingsProgressColor(progress * 100, goal)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AmountFormatter.formatCompact(currentAmount.amount, currentAmount.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
                
                Text(
                    text = AmountFormatter.formatCompact(goal.targetAmount.amount, goal.targetAmount.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * Enhanced progress ring with gradient effect
 */
@Composable
fun EnhancedSavingsProgressRing(
    goal: Goal,
    currentAmount: Money,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 150.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 16.dp
) {
    val progress = ProgressUtils.calculateGoalProgress(currentAmount, goal.targetAmount) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500),
        label = "enhanced_progress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawEnhancedProgressRing(
                progress = animatedProgress,
                strokeWidth = strokeWidth.toPx(),
                goal = goal
            )
        }
        
        // Center content with more details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = getSavingsProgressColor(progress * 100, goal)
            )
            
            Text(
                text = "completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val remaining = goal.targetAmount.amount - currentAmount.amount
            if (remaining > 0) {
                Text(
                    text = AmountFormatter.formatCompact(remaining, goal.targetAmount.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Goal Reached!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Canvas drawing functions
private fun DrawScope.drawSavingsProgressRing(
    progress: Float,
    strokeWidth: Float,
    progressColor: Color,
    backgroundColor: Color
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) / 2 - strokeWidth / 2
    
    // Background ring
    drawCircle(
        color = backgroundColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    
    // Progress arc
    val sweepAngle = 360f * progress.coerceIn(0f, 1f)
    drawArc(
        color = progressColor,
        startAngle = -90f, // Start from top
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawEnhancedProgressRing(
    progress: Float,
    strokeWidth: Float,
    goal: Goal
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = min(size.width, size.height) / 2 - strokeWidth / 2
    
    // Background ring with subtle gradient
    val backgroundGradient = Brush.radialGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.1f),
            Color.Gray.copy(alpha = 0.05f)
        ),
        center = center,
        radius = radius
    )
    
    drawCircle(
        brush = backgroundGradient,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth * 0.5f)
    )
    
    // Progress arc with gradient
    val progressGradient = Brush.sweepGradient(
        colors = listOf(
            getSavingsProgressColor(progress * 100, goal),
            getSavingsProgressColor(progress * 100, goal).copy(alpha = 0.7f),
            getSavingsProgressColor(progress * 100, goal)
        ),
        center = center
    )
    
    val sweepAngle = 360f * progress.coerceIn(0f, 1f)
    drawArc(
        brush = progressGradient,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

// Helper function for progress color
internal fun getSavingsProgressColor(progress: Double, goal: Goal): Color {
    return when {
        progress >= 100.0 -> Color(0xFF4CAF50) // Green - Goal achieved
        progress >= 75.0 -> Color(0xFF8BC34A)  // Light green - Almost there
        progress >= 50.0 -> Color(0xFFFF9800)  // Orange - Halfway
        progress >= 25.0 -> Color(0xFFFFC107)  // Yellow - Getting started
        else -> Color(0xFFE91E63)               // Pink - Just started
    }
}