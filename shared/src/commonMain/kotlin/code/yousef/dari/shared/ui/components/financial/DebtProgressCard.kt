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
import androidx.compose.ui.geometry.CornerRadius
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
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlinx.datetime.LocalDate
import kotlin.math.*

/**
 * Data class for debt information
 */
data class DebtInfo(
    val id: String,
    val name: String,
    val type: DebtType,
    val originalAmount: Money,
    val currentBalance: Money,
    val minimumPayment: Money,
    val interestRate: Double,
    val payoffDate: LocalDate?,
    val monthsRemaining: Int?,
    val totalInterest: Double,
    val nextPaymentDate: LocalDate?,
    val payoffStrategy: PayoffStrategy = PayoffStrategy.MINIMUM_PAYMENT
)

/**
 * Debt type enum
 */
enum class DebtType(val displayName: String, val icon: ImageVector) {
    CREDIT_CARD("Credit Card", Icons.Filled.CreditCard),
    PERSONAL_LOAN("Personal Loan", Icons.Filled.AccountBalance),
    CAR_LOAN("Car Loan", Icons.Filled.DirectionsCar),
    MORTGAGE("Mortgage", Icons.Filled.Home),
    STUDENT_LOAN("Student Loan", Icons.Filled.School),
    OTHER("Other", Icons.Filled.Receipt)
}

/**
 * Payoff strategy enum
 */
enum class PayoffStrategy(val displayName: String) {
    MINIMUM_PAYMENT("Minimum Payment"),
    DEBT_AVALANCHE("Debt Avalanche"),
    DEBT_SNOWBALL("Debt Snowball"),
    CUSTOM("Custom Strategy")
}

/**
 * Debt progress card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtProgressCard(
    debt: DebtInfo,
    onDebtClick: (DebtInfo) -> Unit,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    isCompact: Boolean = false
) {
    val progress = calculateDebtProgress(debt.currentBalance, debt.originalAmount)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "debt_progress"
    )
    
    Card(
        onClick = { onDebtClick(debt) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (isCompact) {
            CompactDebtContent(debt = debt, progress = animatedProgress)
        } else {
            FullDebtContent(debt = debt, progress = animatedProgress, showDetails = showDetails)
        }
    }
}

@Composable
private fun FullDebtContent(
    debt: DebtInfo,
    progress: Float,
    showDetails: Boolean
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
                DebtTypeIcon(
                    type = debt.type,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = debt.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            DebtStatusBadge(debt = debt)
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
                        text = "Paid Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getDebtProgressColor(progress)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = getDebtProgressColor(progress),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMoney(debt.currentBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DariTheme.financialColors.Expense
                    )
                    
                    Text(
                        text = "of ${formatMoney(debt.originalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Debt progress ring
            DebtProgressRing(
                debt = debt,
                progress = progress,
                size = 70.dp
            )
        }
        
        if (showDetails) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details
            DebtDetailsSection(debt = debt)
        }
    }
}

@Composable
private fun CompactDebtContent(
    debt: DebtInfo,
    progress: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebtTypeIcon(
            type = debt.type,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = debt.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatMoney(debt.currentBalance),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = DariTheme.financialColors.Expense
            )
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = getDebtProgressColor(progress),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = getDebtProgressColor(progress)
        )
    }
}

@Composable
private fun DebtTypeIcon(
    type: DebtType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = getDebtTypeGradient(type),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = type.displayName,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DebtStatusBadge(debt: DebtInfo) {
    val (text, color) = when {
        debt.currentBalance.amount <= 0 -> "Paid Off" to DariTheme.financialColors.Income
        debt.monthsRemaining != null && debt.monthsRemaining <= 12 -> "Almost Done!" to DariTheme.financialColors.Warning
        debt.interestRate > 15.0 -> "High Interest" to MaterialTheme.colorScheme.error
        else -> debt.payoffStrategy.displayName to MaterialTheme.colorScheme.onSurfaceVariant
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
private fun DebtProgressRing(
    debt: DebtInfo,
    progress: Float,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (min(this.size.width, this.size.height) - strokeWidth.toPx()) / 2f
            val sweepAngle = 360f * progress
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            
            // Progress arc
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            getDebtProgressColor(progress).copy(alpha = 0.6f),
                            getDebtProgressColor(progress)
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
            
            // Achievement indicator for fully paid debt
            if (progress >= 1f) {
                drawPaidOffIndicator(center, radius, strokeWidth.toPx())
            }
        }
        
        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (debt.currentBalance.amount <= 0) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Paid off",
                    tint = DariTheme.financialColors.Income,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = getDebtProgressColor(progress)
                )
            }
        }
    }
}

private fun DrawScope.drawPaidOffIndicator(
    center: Offset,
    radius: Float,
    strokeWidth: Float
) {
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.Green.copy(alpha = 0.3f),
                Color.Transparent
            ),
            radius = radius + strokeWidth
        ),
        radius = radius + strokeWidth,
        center = center
    )
    
    // Achievement ring
    drawCircle(
        color = DariTheme.financialColors.Income,
        radius = radius + strokeWidth / 2f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

@Composable
private fun DebtDetailsSection(debt: DebtInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DebtDetailItem(
                label = "Interest Rate",
                value = "${String.format("%.2f", debt.interestRate)}%",
                icon = Icons.Filled.Percent
            )
            
            DebtDetailItem(
                label = "Min Payment",
                value = formatMoney(debt.minimumPayment),
                icon = Icons.Filled.Payment
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DebtDetailItem(
                label = "Months Left",
                value = debt.monthsRemaining?.toString() ?: "Unknown",
                icon = Icons.Filled.Schedule
            )
            
            DebtDetailItem(
                label = "Total Interest",
                value = formatAmount(debt.totalInterest, debt.currentBalance.currency),
                icon = Icons.Filled.TrendingUp
            )
        }
        
        debt.payoffDate?.let { payoffDate ->
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Payoff date",
                    tint = DariTheme.financialColors.Goal,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Expected payoff: ${formatDate(payoffDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DebtDetailItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Debt summary card showing all debts
 */
@Composable
fun DebtSummaryCard(
    debts: List<DebtInfo>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "SAR"
) {
    if (debts.isEmpty()) return
    
    val totalDebt = debts.sumOf { it.currentBalance.amount }
    val totalOriginal = debts.sumOf { it.originalAmount.amount }
    val totalProgress = if (totalOriginal > 0) {
        ((totalOriginal - totalDebt) / totalOriginal).toFloat()
    } else 0f
    
    val highestInterestDebt = debts.maxByOrNull { it.interestRate }
    val nearestPayoff = debts
        .filter { it.monthsRemaining != null }
        .minByOrNull { it.monthsRemaining ?: Int.MAX_VALUE }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Debt Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "${debts.size} debts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                TextButton(onClick = onViewAllClick) {
                    Text("View All")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total debt and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Debt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatAmount(totalDebt, currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = DariTheme.financialColors.Expense
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = totalProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = getDebtProgressColor(totalProgress),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${(totalProgress * 100).toInt()}% paid off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                DebtProgressRing(
                    debt = DebtInfo(
                        id = "summary",
                        name = "Total",
                        type = DebtType.OTHER,
                        originalAmount = Money(totalOriginal, currency),
                        currentBalance = Money(totalDebt, currency),
                        minimumPayment = Money(0.0, currency),
                        interestRate = 0.0,
                        payoffDate = null,
                        monthsRemaining = null,
                        totalInterest = 0.0
                    ),
                    progress = totalProgress,
                    size = 70.dp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Key insights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                highestInterestDebt?.let { debt ->
                    DebtInsightItem(
                        label = "Highest Interest",
                        value = "${String.format("%.1f", debt.interestRate)}%",
                        subtitle = debt.name,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                nearestPayoff?.let { debt ->
                    DebtInsightItem(
                        label = "Next Payoff",
                        value = "${debt.monthsRemaining} months",
                        subtitle = debt.name,
                        color = DariTheme.financialColors.Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtInsightItem(
    label: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Helper functions
 */
private fun calculateDebtProgress(current: Money, original: Money): Float {
    if (original.amount == 0.0) return 0f
    val paid = original.amount - current.amount
    return (paid / original.amount).toFloat().coerceIn(0f, 1f)
}

@Composable
private fun getDebtProgressColor(progress: Float): Color {
    return when {
        progress >= 1.0f -> DariTheme.financialColors.Income
        progress >= 0.8f -> Color(0xFF4CAF50) // Light Green
        progress >= 0.5f -> Color(0xFFFF9800) // Orange
        progress >= 0.3f -> Color(0xFFFFEB3B) // Yellow
        else -> DariTheme.financialColors.Expense
    }
}

@Composable
private fun getDebtTypeGradient(type: DebtType): Brush {
    return when (type) {
        DebtType.CREDIT_CARD -> Brush.linearGradient(
            listOf(Color(0xFFE91E63), Color(0xFFF06292))
        )
        DebtType.PERSONAL_LOAN -> Brush.linearGradient(
            listOf(Color(0xFF2196F3), Color(0xFF64B5F6))
        )
        DebtType.CAR_LOAN -> Brush.linearGradient(
            listOf(Color(0xFF9C27B0), Color(0xFFBA68C8))
        )
        DebtType.MORTGAGE -> Brush.linearGradient(
            listOf(Color(0xFF4CAF50), Color(0xFF81C784))
        )
        DebtType.STUDENT_LOAN -> Brush.linearGradient(
            listOf(Color(0xFF3F51B5), Color(0xFF7986CB))
        )
        DebtType.OTHER -> Brush.linearGradient(
            listOf(Color(0xFF607D8B), Color(0xFF90A4AE))
        )
    }
}

private fun formatMoney(money: Money): String {
    return "${money.currency} ${String.format("%.0f", abs(money.amount))}"
}

private fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format("%.0f", abs(amount))}"
}

private fun formatDate(date: LocalDate): String {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return "${months[date.monthNumber - 1]} ${date.year}"
}