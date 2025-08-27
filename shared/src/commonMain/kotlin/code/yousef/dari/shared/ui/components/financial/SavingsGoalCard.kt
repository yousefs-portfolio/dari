package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Goal
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlinx.datetime.LocalDate
import kotlin.math.*

/**
 * Enhanced savings goal card with Islamic finance principles
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
    allowContribution: Boolean = true
) {
    val progress = calculateSavingsProgress(currentAmount, goal.targetAmount)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1200),
        label = "savings_progress"
    )
    
    val progressColor by animateColorAsState(
        targetValue = getSavingsProgressColor(progress, goal),
        animationSpec = tween(durationMillis = 500),
        label = "savings_color"
    )
    
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
                progress = animatedProgress,
                progressColor = progressColor
            )
        } else {
            FullSavingsContent(
                goal = goal,
                currentAmount = currentAmount,
                progress = animatedProgress,
                progressColor = progressColor,
                showDetails = showDetails,
                allowContribution = allowContribution
            )
        }
    }
}

@Composable
private fun FullSavingsContent(
    goal: Goal,
    currentAmount: Money,
    progress: Float,
    progressColor: Color,
    showDetails: Boolean,
    allowContribution: Boolean
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SavingsGoalIcon(
                    goalType = goal.type,
                    isCompleted = progress >= 1f,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = getGoalTypeDisplayName(goal.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Islamic savings indicator
                    if (isIslamicCompliant(goal)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Halal compliant",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            Text(
                                text = "Halal",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
            
            SavingsGoalStatusBadge(
                goal = goal,
                progress = progress
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Baseline
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                SavingsProgressIndicator(
                    progress = progress,
                    color = progressColor,
                    height = 8.dp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(currentAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = progressColor
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(goal.targetAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Savings progress ring
            SavingsProgressRing(
                progress = progress,
                color = progressColor,
                goal = goal,
                size = 80.dp
            )
        }
        
        if (showDetails) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details and insights
            SavingsGoalDetails(
                goal = goal,
                currentAmount = currentAmount,
                progress = progress
            )
        }
        
        if (allowContribution && progress < 1f) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick contribution buttons
            SavingsContributionButtons(
                goal = goal,
                onContribute = { amount ->
                    // TODO: Handle contribution
                }
            )
        }
    }
}

@Composable
private fun CompactSavingsContent(
    goal: Goal,
    currentAmount: Money,
    progress: Float,
    progressColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SavingsGoalIcon(
            goalType = goal.type,
            isCompleted = progress >= 1f,
            modifier = Modifier.size(36.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "${formatMoney(currentAmount)} / ${formatMoney(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = progressColor
        )
    }
}

@Composable
private fun SavingsGoalIcon(
    goalType: Goal.Type,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = getSavingsGoalGradient(goalType, isCompleted),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = getSavingsGoalIcon(goalType),
                contentDescription = goalType.name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SavingsGoalStatusBadge(
    goal: Goal,
    progress: Float
) {
    val (text, color) = when {
        progress >= 1f -> "Completed" to DariTheme.financialColors.Income
        progress >= 0.8f -> "Almost There!" to Color(0xFFFF9800)
        progress >= 0.5f -> "On Track" to DariTheme.financialColors.Income
        progress >= 0.2f -> "Getting Started" to Color(0xFF2196F3)
        goal.priority == Goal.Priority.HIGH -> "High Priority" to MaterialTheme.colorScheme.error
        else -> getGoalStatusText(goal) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SavingsProgressIndicator(
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
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
        
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
                brush = Brush.horizontalGradient(
                    listOf(
                        color.copy(alpha = 0.8f),
                        color,
                        color.copy(alpha = 0.9f)
                    )
                ),
                size = Size(progressWidth, size.height),
                cornerRadius = cornerRadius
            )
        }
        
        // Achievement sparkles for completed goals
        if (progress >= 1f) {
            drawAchievementSparkles(size)
        }
    }
}

private fun DrawScope.drawAchievementSparkles(canvasSize: Size) {
    val sparkles = listOf(
        Offset(canvasSize.width * 0.2f, canvasSize.height * 0.3f),
        Offset(canvasSize.width * 0.5f, canvasSize.height * 0.1f),
        Offset(canvasSize.width * 0.8f, canvasSize.height * 0.4f)
    )
    
    sparkles.forEach { sparkleCenter ->
        drawCircle(
            color = Color.Gold.copy(alpha = 0.8f),
            radius = 2.dp.toPx(),
            center = sparkleCenter
        )
    }
}

@Composable
private fun SavingsProgressRing(
    progress: Float,
    color: Color,
    goal: Goal,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 10.dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (min(this.size.width, this.size.height) - strokeWidth.toPx()) / 2f
            val sweepAngle = 360f * min(progress, 1f)
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            
            // Progress arc
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            color.copy(alpha = 0.7f),
                            color,
                            color.copy(alpha = 0.9f),
                            if (progress >= 1f) Color.Gold else color
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                    size = Size(radius * 2f, radius * 2f),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
            
            // Achievement indicator
            if (progress >= 1f) {
                drawAchievementRing(center, radius, strokeWidth.toPx())
            }
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (progress >= 1f) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Achievement",
                    tint = Color.Gold,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = getSavingsGoalIcon(goal.type),
                    contentDescription = goal.type.name,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (progress >= 1f) Color.Gold else color
            )
        }
    }
}

private fun DrawScope.drawAchievementRing(
    center: Offset,
    radius: Float,
    strokeWidth: Float
) {
    // Outer celebration ring
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.Gold.copy(alpha = 0.3f),
                Color.Transparent
            ),
            radius = radius + strokeWidth * 1.5f
        ),
        radius = radius + strokeWidth * 1.5f,
        center = center
    )
    
    // Achievement ring
    drawCircle(
        color = Color.Gold,
        radius = radius + strokeWidth / 3f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

@Composable
private fun SavingsGoalDetails(
    goal: Goal,
    currentAmount: Money,
    progress: Float
) {
    val remaining = Money(
        amount = goal.targetAmount.amount - currentAmount.amount,
        currency = goal.targetAmount.currency
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Amount remaining
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = "Remaining",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Amount remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatMoney(remaining),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Target date and timeline
        goal.targetDate?.let { targetDate ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Target date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "Target date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = formatTargetDate(targetDate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Monthly savings needed
            if (progress < 1f) {
                val monthsRemaining = calculateMonthsRemaining(targetDate)
                if (monthsRemaining > 0) {
                    val monthlyNeeded = remaining.amount / monthsRemaining
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Monthly target",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "Monthly target",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = formatAmount(monthlyNeeded, remaining.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DariTheme.financialColors.Goal
                        )
                    }
                }
            }
        }
        
        // Islamic finance compliance
        if (isIslamicCompliant(goal)) {
            IslamicComplianceNote()
        }
    }
}

@Composable
private fun IslamicComplianceNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Halal compliant",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = "Halal Compliant Savings",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
                
                Text(
                    text = "This goal follows Islamic banking principles without riba (interest)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SavingsContributionButtons(
    goal: Goal,
    onContribute: (Double) -> Unit
) {
    val suggestedAmounts = listOf(50.0, 100.0, 250.0, 500.0)
    
    Column {
        Text(
            text = "Quick Contribute",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedAmounts.forEach { amount ->
                Button(
                    onClick = { onContribute(amount) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${goal.targetAmount.currency} ${amount.toInt()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Helper functions
 */
private fun calculateSavingsProgress(current: Money, target: Money): Float {
    if (target.amount == 0.0) return 0f
    return (current.amount / target.amount).toFloat().coerceIn(0f, 1f)
}

@Composable
private fun getSavingsProgressColor(progress: Float, goal: Goal): Color {
    return when {
        progress >= 1f -> Color.Gold
        progress >= 0.8f -> DariTheme.financialColors.Income
        progress >= 0.5f -> getSavingsGoalColor(goal.type)
        progress >= 0.2f -> getSavingsGoalColor(goal.type).copy(alpha = 0.8f)
        else -> getSavingsGoalColor(goal.type).copy(alpha = 0.6f)
    }
}

@Composable
private fun getSavingsGoalColor(type: Goal.Type): Color {
    return when (type) {
        Goal.Type.EMERGENCY_FUND -> Color(0xFFFF5722) // Deep Orange
        Goal.Type.VACATION -> Color(0xFF00BCD4) // Cyan
        Goal.Type.HOME_PURCHASE -> Color(0xFF4CAF50) // Green
        Goal.Type.EDUCATION -> Color(0xFF3F51B5) // Indigo
        Goal.Type.RETIREMENT -> Color(0xFF795548) // Brown
        Goal.Type.SAVINGS -> DariTheme.financialColors.Income
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun getSavingsGoalGradient(type: Goal.Type, isCompleted: Boolean): Brush {
    return if (isCompleted) {
        Brush.linearGradient(listOf(Color.Gold, Color(0xFFFFC107)))
    } else {
        when (type) {
            Goal.Type.EMERGENCY_FUND -> Brush.linearGradient(
                listOf(Color(0xFFFF5722), Color(0xFFFF8A65))
            )
            Goal.Type.VACATION -> Brush.linearGradient(
                listOf(Color(0xFF00BCD4), Color(0xFF4DD0E1))
            )
            Goal.Type.HOME_PURCHASE -> Brush.linearGradient(
                listOf(Color(0xFF4CAF50), Color(0xFF81C784))
            )
            Goal.Type.EDUCATION -> Brush.linearGradient(
                listOf(Color(0xFF3F51B5), Color(0xFF7986CB))
            )
            Goal.Type.RETIREMENT -> Brush.linearGradient(
                listOf(Color(0xFF795548), Color(0xFFA1887F))
            )
            else -> Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            )
        }
    }
}

private fun getSavingsGoalIcon(type: Goal.Type): ImageVector {
    return when (type) {
        Goal.Type.EMERGENCY_FUND -> Icons.Filled.Security
        Goal.Type.VACATION -> Icons.Filled.Flight
        Goal.Type.HOME_PURCHASE -> Icons.Filled.Home
        Goal.Type.EDUCATION -> Icons.Filled.School
        Goal.Type.RETIREMENT -> Icons.Filled.ElderlyWoman
        Goal.Type.SAVINGS -> Icons.Filled.Savings
        else -> Icons.Filled.FlagCircle
    }
}

private fun getGoalTypeDisplayName(type: Goal.Type): String {
    return when (type) {
        Goal.Type.EMERGENCY_FUND -> "Emergency Fund"
        Goal.Type.VACATION -> "Vacation"
        Goal.Type.HOME_PURCHASE -> "Home Purchase"
        Goal.Type.EDUCATION -> "Education"
        Goal.Type.RETIREMENT -> "Retirement"
        Goal.Type.SAVINGS -> "Savings"
        else -> "General Goal"
    }
}

private fun getGoalStatusText(goal: Goal): String {
    return when (goal.priority) {
        Goal.Priority.HIGH -> "High Priority"
        Goal.Priority.MEDIUM -> "Medium Priority"
        Goal.Priority.LOW -> "Low Priority"
    }
}

private fun isIslamicCompliant(goal: Goal): Boolean {
    // Islamic finance principles: no interest-based investments
    return when (goal.type) {
        Goal.Type.EMERGENCY_FUND,
        Goal.Type.VACATION,
        Goal.Type.HOME_PURCHASE,
        Goal.Type.EDUCATION,
        Goal.Type.SAVINGS -> true
        else -> false // Investment goals may involve interest
    }
}

private fun formatMoney(money: Money): String {
    return "${money.currency} ${String.format("%.0f", abs(money.amount))}"
}

private fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format("%.0f", abs(amount))}"
}

private fun formatTargetDate(date: LocalDate): String {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return "${months[date.monthNumber - 1]} ${date.year}"
}

private fun calculateMonthsRemaining(targetDate: LocalDate): Int {
    // TODO: Implement proper date calculation
    return 12
}