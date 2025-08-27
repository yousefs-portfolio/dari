package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Budget
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlin.math.abs
import kotlin.math.min

/**
 * Budget progress bar component showing spending vs budget
 */
@Composable
fun BudgetProgressBar(
    budget: Budget,
    spent: Money,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    animated: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 16.dp
) {
    val progress = calculateProgress(spent, budget.amount)
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else progress,
        animationSpec = tween(durationMillis = 1000),
        label = "budget_progress"
    )
    
    val progressColor by animateColorAsState(
        targetValue = getProgressColor(progress),
        label = "progress_color"
    )
    
    Column(modifier = modifier) {
        if (showDetails) {
            BudgetProgressHeader(
                budget = budget,
                spent = spent,
                progress = progress
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        BudgetProgressIndicator(
            progress = animatedProgress,
            color = progressColor,
            height = height
        )
        
        if (showDetails) {
            Spacer(modifier = Modifier.height(4.dp))
            
            BudgetProgressLabels(
                spent = spent,
                remaining = Money(
                    amount = budget.amount.amount - spent.amount,
                    currency = budget.amount.currency
                )
            )
        }
    }
}

@Composable
private fun BudgetProgressHeader(
    budget: Budget,
    spent: Money,
    progress: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = budget.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = budget.category.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = getProgressColor(progress)
        )
    }
}

@Composable
private fun BudgetProgressIndicator(
    progress: Float,
    color: Color,
    height: androidx.compose.ui.unit.Dp
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
    ) {
        val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        
        // Background track
        drawRoundRect(
            color = Color.Gray.copy(alpha = 0.2f),
            size = size,
            cornerRadius = cornerRadius
        )
        
        // Progress fill
        if (progress > 0f) {
            val progressWidth = size.width * min(progress, 1f)
            
            drawRoundRect(
                brush = if (progress > 1f) {
                    Brush.horizontalGradient(
                        listOf(color, color.copy(alpha = 0.8f))
                    )
                } else {
                    Brush.horizontalGradient(listOf(color, color))
                },
                size = Size(progressWidth, size.height),
                cornerRadius = cornerRadius
            )
        }
        
        // Over-budget indicator
        if (progress > 1f) {
            drawOverBudgetIndicator(progress, size)
        }
    }
}

private fun DrawScope.drawOverBudgetIndicator(
    progress: Float,
    canvasSize: Size
) {
    val overageWidth = canvasSize.width * (progress - 1f)
    val startX = canvasSize.width - overageWidth
    
    drawRect(
        color = Color.Red.copy(alpha = 0.3f),
        topLeft = Offset(startX, 0f),
        size = Size(overageWidth, canvasSize.height)
    )
    
    // Add diagonal stripes for over-budget
    val stripeWidth = 4.dp.toPx()
    val stripeSpacing = 8.dp.toPx()
    var stripeX = startX
    
    while (stripeX < canvasSize.width) {
        drawLine(
            color = Color.Red.copy(alpha = 0.5f),
            start = Offset(stripeX, 0f),
            end = Offset(stripeX, canvasSize.height),
            strokeWidth = stripeWidth,
            cap = StrokeCap.Round
        )
        stripeX += stripeSpacing
    }
}

@Composable
private fun BudgetProgressLabels(
    spent: Money,
    remaining: Money
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(spent),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = DariTheme.financialColors.Expense
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (remaining.amount >= 0) "Remaining" else "Over Budget",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(remaining),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (remaining.amount >= 0) {
                    DariTheme.financialColors.Income
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/**
 * Compact budget progress bar for list items
 */
@Composable
fun CompactBudgetProgressBar(
    budget: Budget,
    spent: Money,
    modifier: Modifier = Modifier
) {
    val progress = calculateProgress(spent, budget.amount)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "compact_progress"
    )
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = getProgressColor(progress)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = min(animatedProgress, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = getProgressColor(progress),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMoney(spent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatMoney(budget.amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Budget progress ring for compact display
 */
@Composable
fun BudgetProgressRing(
    budget: Budget,
    spent: Money,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 6.dp
) {
    val progress = calculateProgress(spent, budget.amount)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "ring_progress"
    )
    
    val progressColor = getProgressColor(progress)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeWidth.toPx()) / 2f
            val sweepAngle = 360f * min(animatedProgress, 1f)
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(progressColor.copy(alpha = 0.7f), progressColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    ),
                    size = Size(radius * 2f, radius * 2f),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
        }
        
        // Center text
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = progressColor
        )
    }
}

/**
 * Calculate progress percentage
 */
private fun calculateProgress(spent: Money, budget: Money): Float {
    if (budget.amount == 0.0) return 0f
    return (abs(spent.amount) / abs(budget.amount)).toFloat()
}

/**
 * Get progress color based on percentage
 */
@Composable
private fun getProgressColor(progress: Float): Color {
    return when {
        progress >= 1.0f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> DariTheme.financialColors.Warning
        progress >= 0.6f -> Color(0xFFFF9800) // Orange
        else -> DariTheme.financialColors.Income
    }
}

/**
 * Format money for display
 */
private fun formatMoney(money: Money): String {
    return "${money.currency} ${String.format("%.0f", abs(money.amount))}"
}