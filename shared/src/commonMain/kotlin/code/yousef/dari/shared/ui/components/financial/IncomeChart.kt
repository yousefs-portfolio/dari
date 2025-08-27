package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.max

/**
 * Data class for income chart data points
 */
data class IncomeDataPoint(
    val date: LocalDate,
    val amount: Double,
    val sourceBreakdown: Map<String, Double> = emptyMap(),
    val isRecurring: Boolean = false,
    val source: String? = null
)

/**
 * Income chart component showing income trends over time
 */
@Composable
fun IncomeChart(
    data: List<IncomeDataPoint>,
    modifier: Modifier = Modifier,
    chartType: IncomeChartType = IncomeChartType.LINE,
    period: ChartPeriod = ChartPeriod.MONTH,
    showProjection: Boolean = false,
    showRecurring: Boolean = true,
    currency: String = "SAR"
) {
    var selectedDataPoint by remember { mutableStateOf<IncomeDataPoint?>(null) }
    
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
            // Chart header
            IncomeChartHeader(
                data = data,
                period = period,
                currency = currency,
                selectedDataPoint = selectedDataPoint
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart content
            when (chartType) {
                IncomeChartType.LINE -> LineIncomeChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    showProjection = showProjection,
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                IncomeChartType.BAR -> BarIncomeChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    showRecurring = showRecurring,
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                IncomeChartType.AREA -> AreaIncomeChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                IncomeChartType.STACKED -> StackedIncomeChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
            }
            
            if (data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chart legend and stats
                IncomeChartLegend(
                    data = data,
                    currency = currency,
                    showRecurring = showRecurring
                )
            }
        }
    }
}

@Composable
private fun IncomeChartHeader(
    data: List<IncomeDataPoint>,
    period: ChartPeriod,
    currency: String,
    selectedDataPoint: IncomeDataPoint?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Income",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = period.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = "Income",
                tint = DariTheme.financialColors.Income,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = selectedDataPoint?.let { formatAmount(it.amount, currency) }
                        ?: formatAmount(calculateTotalIncome(data), currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DariTheme.financialColors.Income
                )
                
                if (selectedDataPoint != null) {
                    Text(
                        text = formatDate(selectedDataPoint.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val growthRate = calculateGrowthRate(data)
                    Text(
                        text = if (growthRate > 0) "+${String.format("%.1f", growthRate)}%" 
                              else "${String.format("%.1f", growthRate)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (growthRate >= 0) DariTheme.financialColors.Income 
                               else DariTheme.financialColors.Expense
                    )
                }
            }
        }
    }
}

@Composable
private fun LineIncomeChart(
    data: List<IncomeDataPoint>,
    onDataPointSelected: (IncomeDataPoint?) -> Unit,
    showProjection: Boolean,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyIncomeChartPlaceholder(modifier = modifier)
        return
    }
    
    val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
    val minAmount = data.minOfOrNull { it.amount } ?: 0.0
    val range = maxAmount - minAmount
    val paddedMax = maxAmount + (range * 0.1)
    val paddedMin = maxAmount(minAmount - (range * 0.1), 0.0)
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw grid lines
        drawIncomeChartGrid(
            maxAmount = paddedMax,
            minAmount = paddedMin,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = padding
        )
        
        // Draw line chart
        if (data.size > 1) {
            val path = Path()
            val points = mutableListOf<Offset>()
            val recurringPoints = mutableListOf<Offset>()
            
            data.forEachIndexed { index, dataPoint ->
                val x = padding + (index * chartWidth / (data.size - 1))
                val y = padding + chartHeight - ((dataPoint.amount - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
                
                points.add(Offset(x, y))
                if (dataPoint.isRecurring) {
                    recurringPoints.add(Offset(x, y))
                }
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Draw main line
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
            
            // Draw all data points
            points.forEach { point ->
                drawCircle(
                    color = DariTheme.financialColors.Income,
                    radius = 4.dp.toPx(),
                    center = point
                )
                
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
            
            // Highlight recurring income points
            recurringPoints.forEach { point ->
                drawCircle(
                    color = DariTheme.financialColors.Income,
                    radius = 6.dp.toPx(),
                    center = point,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Draw projection if enabled
            if (showProjection && data.size >= 2) {
                drawIncomeProjection(
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
private fun BarIncomeChart(
    data: List<IncomeDataPoint>,
    onDataPointSelected: (IncomeDataPoint?) -> Unit,
    showRecurring: Boolean,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyIncomeChartPlaceholder(modifier = modifier)
        return
    }
    
    val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
    val animatedBars = data.map { dataPoint ->
        animateFloatAsState(
            targetValue = (dataPoint.amount / maxAmount).toFloat(),
            animationSpec = tween(durationMillis = 1000, delayMillis = data.indexOf(dataPoint) * 100),
            label = "income_bar_animation"
        )
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        val barWidth = if (data.size > 1) chartWidth / data.size * 0.7f else chartWidth * 0.5f
        val barSpacing = if (data.size > 1) chartWidth / data.size * 0.3f else 0f
        
        data.forEachIndexed { index, dataPoint ->
            val barHeight = chartHeight * animatedBars[index].value
            val x = padding + (index * (barWidth + barSpacing)) + (barSpacing / 2)
            val y = padding + chartHeight - barHeight
            
            // Draw bar with different colors for recurring vs one-time income
            val barBrush = if (showRecurring && dataPoint.isRecurring) {
                Brush.verticalGradient(
                    listOf(
                        DariTheme.financialColors.Income,
                        DariTheme.financialColors.Income.copy(alpha = 0.7f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        DariTheme.financialColors.Income.copy(alpha = 0.8f),
                        DariTheme.financialColors.Income.copy(alpha = 0.6f)
                    )
                )
            }
            
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            
            // Add pattern for recurring income
            if (showRecurring && dataPoint.isRecurring) {
                drawRecurringPattern(
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun AreaIncomeChart(
    data: List<IncomeDataPoint>,
    onDataPointSelected: (IncomeDataPoint?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyIncomeChartPlaceholder(modifier = modifier)
        return
    }
    
    val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
    val minAmount = 0.0 // Start area chart from zero
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        if (data.size > 1) {
            val path = Path()
            val points = mutableListOf<Offset>()
            
            // Start from bottom left
            path.moveTo(padding, padding + chartHeight)
            
            data.forEachIndexed { index, dataPoint ->
                val x = padding + (index * chartWidth / (data.size - 1))
                val y = padding + chartHeight - ((dataPoint.amount - minAmount) / (maxAmount - minAmount) * chartHeight).toFloat()
                
                points.add(Offset(x, y))
                
                if (index == 0) {
                    path.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Close path at bottom right
            path.lineTo(padding + chartWidth, padding + chartHeight)
            path.close()
            
            // Draw filled area
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(
                        DariTheme.financialColors.Income.copy(alpha = 0.4f),
                        DariTheme.financialColors.Income.copy(alpha = 0.2f),
                        DariTheme.financialColors.Income.copy(alpha = 0.1f),
                        Color.Transparent
                    )
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
                color = DariTheme.financialColors.Income,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun StackedIncomeChart(
    data: List<IncomeDataPoint>,
    onDataPointSelected: (IncomeDataPoint?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyIncomeChartPlaceholder(modifier = modifier)
        return
    }
    
    val maxAmount = data.maxOfOrNull { dataPoint ->
        dataPoint.sourceBreakdown.values.sum()
    } ?: 0.0
    
    val allSources = data.flatMap { it.sourceBreakdown.keys }.distinct()
    val sourceColors = generateSourceColors(allSources)
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        val barWidth = if (data.size > 1) chartWidth / data.size * 0.8f else chartWidth * 0.6f
        val barSpacing = if (data.size > 1) chartWidth / data.size * 0.2f else 0f
        
        data.forEachIndexed { index, dataPoint ->
            val x = padding + (index * (barWidth + barSpacing)) + (barSpacing / 2)
            var currentY = padding + chartHeight
            
            allSources.forEach { source ->
                val sourceAmount = dataPoint.sourceBreakdown[source] ?: 0.0
                if (sourceAmount > 0) {
                    val segmentHeight = (sourceAmount / maxAmount * chartHeight).toFloat()
                    currentY -= segmentHeight
                    
                    drawRoundRect(
                        color = sourceColors[source] ?: DariTheme.financialColors.Income,
                        topLeft = Offset(x, currentY),
                        size = Size(barWidth, segmentHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawIncomeChartGrid(
    maxAmount: Double,
    minAmount: Double,
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

private fun DrawScope.drawIncomeProjection(
    lastPoint: Offset,
    data: List<IncomeDataPoint>,
    chartWidth: Float,
    chartHeight: Float,
    padding: Float,
    paddedMax: Double,
    paddedMin: Double
) {
    if (data.size < 2) return
    
    // Calculate trend from last two points
    val lastTwo = data.takeLast(2)
    val trend = lastTwo[1].amount - lastTwo[0].amount
    val projectedAmount = lastTwo[1].amount + trend
    
    val projectedX = padding + chartWidth + 50.dp.toPx()
    val projectedY = padding + chartHeight - 
        ((projectedAmount - paddedMin) / (paddedMax - paddedMin) * chartHeight).toFloat()
    
    // Draw projection line
    drawLine(
        color = DariTheme.financialColors.Income.copy(alpha = 0.5f),
        start = lastPoint,
        end = Offset(projectedX, projectedY),
        strokeWidth = 2.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(5.dp.toPx(), 5.dp.toPx())
        )
    )
    
    // Draw projection point
    drawCircle(
        color = DariTheme.financialColors.Income.copy(alpha = 0.5f),
        radius = 4.dp.toPx(),
        center = Offset(projectedX, projectedY)
    )
}

private fun DrawScope.drawRecurringPattern(
    topLeft: Offset,
    size: Size
) {
    val stripeWidth = 2.dp.toPx()
    val stripeSpacing = 6.dp.toPx()
    var stripeX = topLeft.x + stripeSpacing
    
    while (stripeX < topLeft.x + size.width - stripeSpacing) {
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(stripeX, topLeft.y),
            end = Offset(stripeX, topLeft.y + size.height),
            strokeWidth = stripeWidth,
            cap = StrokeCap.Round
        )
        stripeX += stripeSpacing * 2
    }
}

@Composable
private fun IncomeChartLegend(
    data: List<IncomeDataPoint>,
    currency: String,
    showRecurring: Boolean
) {
    if (data.isEmpty()) return
    
    val totalIncome = calculateTotalIncome(data)
    val averageIncome = data.map { it.amount }.average()
    val recurringIncome = data.filter { it.isRecurring }.sumOf { it.amount }
    val oneTimeIncome = totalIncome - recurringIncome
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IncomeLegendItem(
                label = "Total",
                value = formatAmount(totalIncome, currency),
                color = DariTheme.financialColors.Income
            )
            
            IncomeLegendItem(
                label = "Average",
                value = formatAmount(averageIncome, currency),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            data.maxByOrNull { it.amount }?.let {
                IncomeLegendItem(
                    label = "Highest",
                    value = formatAmount(it.amount, currency),
                    color = DariTheme.financialColors.Income.copy(alpha = 0.8f)
                )
            }
        }
        
        if (showRecurring && recurringIncome > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IncomeLegendItem(
                    label = "Recurring",
                    value = formatAmount(recurringIncome, currency),
                    color = DariTheme.financialColors.Income
                )
                
                IncomeLegendItem(
                    label = "One-time",
                    value = formatAmount(oneTimeIncome, currency),
                    color = DariTheme.financialColors.Income.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun IncomeLegendItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
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

@Composable
private fun EmptyIncomeChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No income data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Income chart type enum
 */
enum class IncomeChartType {
    LINE, BAR, AREA, STACKED
}

/**
 * Helper functions
 */
private fun calculateTotalIncome(data: List<IncomeDataPoint>): Double {
    return data.sumOf { it.amount }
}

private fun calculateGrowthRate(data: List<IncomeDataPoint>): Double {
    if (data.size < 2) return 0.0
    val firstHalf = data.take(data.size / 2).map { it.amount }.average()
    val secondHalf = data.drop(data.size / 2).map { it.amount }.average()
    return if (firstHalf > 0) ((secondHalf - firstHalf) / firstHalf) * 100 else 0.0
}

private fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format("%.0f", abs(amount))}"
}

private fun formatDate(date: LocalDate): String {
    return "${date.dayOfMonth}/${date.monthNumber}"
}

private fun generateSourceColors(sources: List<String>): Map<String, Color> {
    val colors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548)  // Brown
    )
    
    return sources.mapIndexed { index, source ->
        source to colors[index % colors.size]
    }.toMap()
}

/**
 * Convert transactions to income data points
 */
fun List<Transaction>.toIncomeDataPoints(
    startDate: LocalDate,
    endDate: LocalDate,
    groupBy: ChartPeriod = ChartPeriod.MONTH
): List<IncomeDataPoint> {
    return this
        .filter { it.date >= startDate && it.date <= endDate }
        .filter { it.amount.amount > 0 } // Only income
        .groupBy { transaction ->
            when (groupBy) {
                ChartPeriod.WEEK -> {
                    // Group by week
                    transaction.date
                }
                ChartPeriod.MONTH -> {
                    // Group by month
                    LocalDate(transaction.date.year, transaction.date.month, 1)
                }
                else -> transaction.date
            }
        }
        .map { (date, transactions) ->
            val isRecurring = transactions.any { 
                it.merchantName?.contains("salary", ignoreCase = true) == true ||
                it.merchantName?.contains("payroll", ignoreCase = true) == true
            }
            
            IncomeDataPoint(
                date = date,
                amount = transactions.sumOf { it.amount.amount },
                sourceBreakdown = transactions
                    .groupBy { it.merchantName ?: "Unknown" }
                    .mapValues { (_, transactions) -> 
                        transactions.sumOf { it.amount.amount }
                    },
                isRecurring = isRecurring,
                source = transactions.firstOrNull()?.merchantName
            )
        }
        .sortedBy { it.date }
}