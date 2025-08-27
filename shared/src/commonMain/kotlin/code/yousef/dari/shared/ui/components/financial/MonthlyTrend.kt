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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlinx.datetime.LocalDate
import kotlin.math.*

/**
 * Data class for monthly trend data points
 */
data class MonthlyTrendData(
    val month: LocalDate,
    val income: Double,
    val expenses: Double,
    val netFlow: Double,
    val savingsRate: Double,
    val transactionCount: Int,
    val budgetVariance: Double = 0.0
)

/**
 * Monthly trend component showing financial trends over months
 */
@Composable
fun MonthlyTrend(
    data: List<MonthlyTrendData>,
    modifier: Modifier = Modifier,
    trendType: TrendType = TrendType.NET_FLOW,
    showComparison: Boolean = true,
    showProjection: Boolean = false,
    currency: String = "SAR"
) {
    var selectedDataPoint by remember { mutableStateOf<MonthlyTrendData?>(null) }
    
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
            MonthlyTrendHeader(
                data = data,
                selectedDataPoint = selectedDataPoint,
                trendType = trendType,
                currency = currency
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart
            when (trendType) {
                TrendType.NET_FLOW -> NetFlowChart(
                    data = data,
                    selectedDataPoint = selectedDataPoint,
                    onDataPointSelected = { selectedDataPoint = it },
                    showProjection = showProjection,
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                TrendType.INCOME_VS_EXPENSES -> IncomeVsExpensesChart(
                    data = data,
                    selectedDataPoint = selectedDataPoint,
                    onDataPointSelected = { selectedDataPoint = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                TrendType.SAVINGS_RATE -> SavingsRateChart(
                    data = data,
                    selectedDataPoint = selectedDataPoint,
                    onDataPointSelected = { selectedDataPoint = it },
                    modifier = Modifier.height(200.dp)
                )
                TrendType.SPENDING_VARIANCE -> SpendingVarianceChart(
                    data = data,
                    selectedDataPoint = selectedDataPoint,
                    onDataPointSelected = { selectedDataPoint = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
            }
            
            if (data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Statistics and insights
                MonthlyTrendStats(
                    data = data,
                    trendType = trendType,
                    currency = currency,
                    showComparison = showComparison
                )
            }
        }
    }
}

@Composable
private fun MonthlyTrendHeader(
    data: List<MonthlyTrendData>,
    selectedDataPoint: MonthlyTrendData?,
    trendType: TrendType,
    currency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = trendType.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = if (selectedDataPoint != null) {
                    formatMonthYear(selectedDataPoint.month)
                } else {
                    "${data.size} months"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = trendType.icon,
                contentDescription = trendType.displayName,
                tint = getTrendColor(data, trendType),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = getTrendValue(data, selectedDataPoint, trendType, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = getTrendColor(data, trendType)
                )
                
                Text(
                    text = getTrendChange(data, trendType),
                    style = MaterialTheme.typography.bodySmall,
                    color = getTrendChangeColor(data, trendType)
                )
            }
        }
    }
}

@Composable
private fun NetFlowChart(
    data: List<MonthlyTrendData>,
    selectedDataPoint: MonthlyTrendData?,
    onDataPointSelected: (MonthlyTrendData?) -> Unit,
    showProjection: Boolean,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyTrendPlaceholder(modifier = modifier)
        return
    }
    
    val maxValue = data.maxOfOrNull { maxOf(abs(it.netFlow), abs(it.income), abs(it.expenses)) } ?: 0.0
    val minValue = data.minOfOrNull { it.netFlow } ?: 0.0
    val range = maxValue - minValue
    val paddedMax = maxValue + (range * 0.1)
    val paddedMin = minValue - (range * 0.1)
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw grid
        drawTrendGrid(
            maxValue = paddedMax,
            minValue = paddedMin,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = padding
        )
        
        // Draw zero line
        val zeroY = padding + chartHeight - ((0.0 - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
        drawLine(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            start = Offset(padding, zeroY),
            end = Offset(padding + chartWidth, zeroY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(5.dp.toPx(), 5.dp.toPx())
            )
        )
        
        // Draw net flow line
        if (data.size > 1) {
            val path = Path()
            val points = mutableListOf<Offset>()
            
            data.forEachIndexed { index, dataPoint ->
                val x = padding + (index * chartWidth / (data.size - 1))
                val y = padding + chartHeight - ((dataPoint.netFlow - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
                
                points.add(Offset(x, y))
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Draw line
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    listOf(
                        DariTheme.financialColors.Income.copy(alpha = 0.8f),
                        DariTheme.financialColors.Income
                    )
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Draw data points
            points.forEachIndexed { index, point ->
                val dataPoint = data[index]
                val pointColor = if (dataPoint.netFlow >= 0) {
                    DariTheme.financialColors.Income
                } else {
                    DariTheme.financialColors.Expense
                }
                
                drawCircle(
                    color = pointColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
                
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
            
            // Draw projection if enabled
            if (showProjection) {
                drawNetFlowProjection(
                    lastPoint = points.last(),
                    data = data,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    padding = padding,
                    paddedMax = paddedMax,
                    paddedMin = paddedMin
                )
            }
        }
    }
}

@Composable
private fun IncomeVsExpensesChart(
    data: List<MonthlyTrendData>,
    selectedDataPoint: MonthlyTrendData?,
    onDataPointSelected: (MonthlyTrendData?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyTrendPlaceholder(modifier = modifier)
        return
    }
    
    val maxValue = data.maxOfOrNull { maxOf(it.income, abs(it.expenses)) } ?: 0.0
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw income and expenses as dual lines
        if (data.size > 1) {
            // Income path
            val incomePath = Path()
            val incomePoints = mutableListOf<Offset>()
            
            // Expenses path
            val expensesPath = Path()
            val expensePoints = mutableListOf<Offset>()
            
            data.forEachIndexed { index, dataPoint ->
                val x = padding + (index * chartWidth / (data.size - 1))
                
                // Income point
                val incomeY = padding + chartHeight - (dataPoint.income / maxValue * chartHeight).toFloat()
                incomePoints.add(Offset(x, incomeY))
                if (index == 0) {
                    incomePath.moveTo(x, incomeY)
                } else {
                    incomePath.lineTo(x, incomeY)
                }
                
                // Expenses point
                val expenseY = padding + chartHeight - (abs(dataPoint.expenses) / maxValue * chartHeight).toFloat()
                expensePoints.add(Offset(x, expenseY))
                if (index == 0) {
                    expensesPath.moveTo(x, expenseY)
                } else {
                    expensesPath.lineTo(x, expenseY)
                }
            }
            
            // Draw income line
            drawPath(
                path = incomePath,
                color = DariTheme.financialColors.Income,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw expenses line
            drawPath(
                path = expensesPath,
                color = DariTheme.financialColors.Expense,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw income points
            incomePoints.forEach { point ->
                drawCircle(
                    color = DariTheme.financialColors.Income,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
            
            // Draw expense points
            expensePoints.forEach { point ->
                drawCircle(
                    color = DariTheme.financialColors.Expense,
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
private fun SavingsRateChart(
    data: List<MonthlyTrendData>,
    selectedDataPoint: MonthlyTrendData?,
    onDataPointSelected: (MonthlyTrendData?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyTrendPlaceholder(modifier = modifier)
        return
    }
    
    val maxRate = data.maxOfOrNull { it.savingsRate } ?: 0.0
    val minRate = data.minOfOrNull { it.savingsRate } ?: 0.0
    val paddedMax = maxOf(maxRate + 5.0, 50.0) // At least 50% scale
    val paddedMin = minOf(minRate - 5.0, -10.0)
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw target savings rate line (20%)
        val targetY = padding + chartHeight - ((20.0 - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
        drawLine(
            color = DariTheme.financialColors.Goal,
            start = Offset(padding, targetY),
            end = Offset(padding + chartWidth, targetY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(10.dp.toPx(), 5.dp.toPx())
            )
        )
        
        // Draw savings rate area chart
        if (data.size > 1) {
            val path = Path()
            val points = mutableListOf<Offset>()
            
            // Start from bottom
            path.moveTo(padding, padding + chartHeight)
            
            data.forEachIndexed { index, dataPoint ->
                val x = padding + (index * chartWidth / (data.size - 1))
                val y = padding + chartHeight - ((dataPoint.savingsRate - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
                
                points.add(Offset(x, y))
                
                if (index == 0) {
                    path.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Close path
            path.lineTo(padding + chartWidth, padding + chartHeight)
            path.close()
            
            // Fill area
            val fillColor = if (data.average { it.savingsRate } >= 20.0) {
                DariTheme.financialColors.Income.copy(alpha = 0.3f)
            } else {
                DariTheme.financialColors.Warning.copy(alpha = 0.3f)
            }
            
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(fillColor, Color.Transparent)
                )
            )
            
            // Draw border line
            val borderPath = Path()
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    borderPath.moveTo(point.x, point.y)
                } else {
                    borderPath.lineTo(point.x, point.y)
                }
            }
            
            drawPath(
                path = borderPath,
                color = if (data.average { it.savingsRate } >= 20.0) {
                    DariTheme.financialColors.Income
                } else {
                    DariTheme.financialColors.Warning
                },
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun SpendingVarianceChart(
    data: List<MonthlyTrendData>,
    selectedDataPoint: MonthlyTrendData?,
    onDataPointSelected: (MonthlyTrendData?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyTrendPlaceholder(modifier = modifier)
        return
    }
    
    val maxVariance = data.maxOfOrNull { abs(it.budgetVariance) } ?: 0.0
    val animatedBars = data.map { dataPoint ->
        animateFloatAsState(
            targetValue = (dataPoint.budgetVariance / maxVariance).toFloat(),
            animationSpec = tween(durationMillis = 1000),
            label = "variance_bar_${data.indexOf(dataPoint)}"
        )
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        val centerY = padding + chartHeight / 2f
        val barWidth = if (data.size > 1) chartWidth / data.size * 0.7f else chartWidth * 0.5f
        val barSpacing = if (data.size > 1) chartWidth / data.size * 0.3f else 0f
        
        // Draw center line (zero variance)
        drawLine(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            start = Offset(padding, centerY),
            end = Offset(padding + chartWidth, centerY),
            strokeWidth = 1.dp.toPx()
        )
        
        data.forEachIndexed { index, dataPoint ->
            val barHeight = (chartHeight / 2f) * abs(animatedBars[index].value)
            val x = padding + (index * (barWidth + barSpacing)) + (barSpacing / 2)
            
            val (y, color) = if (dataPoint.budgetVariance >= 0) {
                // Over budget (positive variance)
                centerY - barHeight to DariTheme.financialColors.Expense
            } else {
                // Under budget (negative variance)
                centerY to DariTheme.financialColors.Income
            }
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawTrendGrid(
    maxValue: Double,
    minValue: Double,
    chartWidth: Float,
    chartHeight: Float,
    padding: Float
) {
    val gridLines = 5
    
    repeat(gridLines) { i ->
        val y = padding + (chartHeight / (gridLines - 1)) * i
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawNetFlowProjection(
    lastPoint: Offset,
    data: List<MonthlyTrendData>,
    chartWidth: Float,
    chartHeight: Float,
    padding: Float,
    paddedMax: Double,
    paddedMin: Double
) {
    if (data.size < 2) return
    
    // Calculate trend
    val lastTwo = data.takeLast(2)
    val trend = lastTwo[1].netFlow - lastTwo[0].netFlow
    val projectedValue = lastTwo[1].netFlow + trend
    
    val projectedX = padding + chartWidth + 50.dp.toPx()
    val projectedY = padding + chartHeight - 
        ((projectedValue - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
    
    drawLine(
        color = DariTheme.financialColors.Income.copy(alpha = 0.5f),
        start = lastPoint,
        end = Offset(projectedX, projectedY),
        strokeWidth = 2.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(5.dp.toPx(), 5.dp.toPx())
        )
    )
    
    drawCircle(
        color = DariTheme.financialColors.Income.copy(alpha = 0.5f),
        radius = 4.dp.toPx(),
        center = Offset(projectedX, projectedY)
    )
}

@Composable
private fun MonthlyTrendStats(
    data: List<MonthlyTrendData>,
    trendType: TrendType,
    currency: String,
    showComparison: Boolean
) {
    if (data.isEmpty()) return
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        when (trendType) {
            TrendType.NET_FLOW -> {
                TrendStatItem(
                    label = "Best Month",
                    value = data.maxByOrNull { it.netFlow }?.let { 
                        formatAmount(it.netFlow, currency)
                    } ?: "N/A",
                    color = DariTheme.financialColors.Income
                )
                
                TrendStatItem(
                    label = "Average",
                    value = formatAmount(data.map { it.netFlow }.average(), currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TrendStatItem(
                    label = "Worst Month",
                    value = data.minByOrNull { it.netFlow }?.let { 
                        formatAmount(it.netFlow, currency)
                    } ?: "N/A",
                    color = if (data.minOfOrNull { it.netFlow } ?: 0.0 < 0) {
                        DariTheme.financialColors.Expense
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            TrendType.SAVINGS_RATE -> {
                TrendStatItem(
                    label = "Best Rate",
                    value = "${String.format("%.1f", data.maxOfOrNull { it.savingsRate } ?: 0.0)}%",
                    color = DariTheme.financialColors.Income
                )
                
                TrendStatItem(
                    label = "Average",
                    value = "${String.format("%.1f", data.map { it.savingsRate }.average())}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TrendStatItem(
                    label = "Target",
                    value = "20%",
                    color = DariTheme.financialColors.Goal
                )
            }
            else -> {
                TrendStatItem(
                    label = "Highest",
                    value = "N/A",
                    color = DariTheme.financialColors.Income
                )
                
                TrendStatItem(
                    label = "Average",
                    value = "N/A",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TrendStatItem(
                    label = "Lowest",
                    value = "N/A",
                    color = DariTheme.financialColors.Expense
                )
            }
        }
    }
}

@Composable
private fun TrendStatItem(
    label: String,
    value: String,
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
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun EmptyTrendPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No trend data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Trend type enum
 */
enum class TrendType(val displayName: String, val icon: ImageVector) {
    NET_FLOW("Net Cash Flow", Icons.Filled.TrendingUp),
    INCOME_VS_EXPENSES("Income vs Expenses", Icons.Filled.CompareArrows),
    SAVINGS_RATE("Savings Rate", Icons.Filled.Savings),
    SPENDING_VARIANCE("Budget Variance", Icons.Filled.Analytics)
}

/**
 * Helper functions
 */
@Composable
private fun getTrendColor(data: List<MonthlyTrendData>, trendType: TrendType): Color {
    if (data.isEmpty()) return MaterialTheme.colorScheme.onSurfaceVariant
    
    return when (trendType) {
        TrendType.NET_FLOW -> {
            val avgNetFlow = data.map { it.netFlow }.average()
            if (avgNetFlow >= 0) DariTheme.financialColors.Income else DariTheme.financialColors.Expense
        }
        TrendType.SAVINGS_RATE -> {
            val avgSavingsRate = data.map { it.savingsRate }.average()
            if (avgSavingsRate >= 20.0) DariTheme.financialColors.Income else DariTheme.financialColors.Warning
        }
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getTrendValue(
    data: List<MonthlyTrendData>,
    selectedDataPoint: MonthlyTrendData?,
    trendType: TrendType,
    currency: String
): String {
    if (data.isEmpty()) return "N/A"
    
    val dataPoint = selectedDataPoint ?: data.lastOrNull() ?: return "N/A"
    
    return when (trendType) {
        TrendType.NET_FLOW -> formatAmount(dataPoint.netFlow, currency)
        TrendType.INCOME_VS_EXPENSES -> formatAmount(dataPoint.netFlow, currency)
        TrendType.SAVINGS_RATE -> "${String.format("%.1f", dataPoint.savingsRate)}%"
        TrendType.SPENDING_VARIANCE -> formatAmount(dataPoint.budgetVariance, currency)
    }
}

private fun getTrendChange(data: List<MonthlyTrendData>, trendType: TrendType): String {
    if (data.size < 2) return "No change"
    
    val recent = data.takeLast(2)
    val change = when (trendType) {
        TrendType.NET_FLOW -> recent[1].netFlow - recent[0].netFlow
        TrendType.SAVINGS_RATE -> recent[1].savingsRate - recent[0].savingsRate
        else -> 0.0
    }
    
    val sign = if (change > 0) "+" else ""
    return when (trendType) {
        TrendType.SAVINGS_RATE -> "${sign}${String.format("%.1f", change)}%"
        else -> "${sign}${String.format("%.0f", change)}"
    }
}

@Composable
private fun getTrendChangeColor(data: List<MonthlyTrendData>, trendType: TrendType): Color {
    if (data.size < 2) return MaterialTheme.colorScheme.onSurfaceVariant
    
    val recent = data.takeLast(2)
    val change = when (trendType) {
        TrendType.NET_FLOW -> recent[1].netFlow - recent[0].netFlow
        TrendType.SAVINGS_RATE -> recent[1].savingsRate - recent[0].savingsRate
        else -> 0.0
    }
    
    return when {
        change > 0 -> DariTheme.financialColors.Income
        change < 0 -> DariTheme.financialColors.Expense
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format("%.0f", abs(amount))}"
}

private fun formatMonthYear(date: LocalDate): String {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return "${months[date.monthNumber - 1]} ${date.year}"
}

/**
 * Convert transactions to monthly trend data
 */
fun List<Transaction>.toMonthlyTrendData(startDate: LocalDate, endDate: LocalDate): List<MonthlyTrendData> {
    return this
        .filter { it.date >= startDate && it.date <= endDate }
        .groupBy { LocalDate(it.date.year, it.date.month, 1) }
        .map { (month, transactions) ->
            val income = transactions.filter { it.amount.amount > 0 }.sumOf { it.amount.amount }
            val expenses = abs(transactions.filter { it.amount.amount < 0 }.sumOf { it.amount.amount })
            val netFlow = income - expenses
            val savingsRate = if (income > 0) ((netFlow / income) * 100) else 0.0
            
            MonthlyTrendData(
                month = month,
                income = income,
                expenses = expenses,
                netFlow = netFlow,
                savingsRate = savingsRate,
                transactionCount = transactions.size,
                budgetVariance = 0.0 // TODO: Calculate based on budget
            )
        }
        .sortedBy { it.month }
}