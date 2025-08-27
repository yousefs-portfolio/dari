package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
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
import kotlin.math.min

/**
 * Data class for expense chart data points
 */
data class ExpenseDataPoint(
    val date: LocalDate,
    val amount: Double,
    val categoryBreakdown: Map<String, Double> = emptyMap()
)

/**
 * Expense chart component showing spending trends over time
 */
@Composable
fun ExpenseChart(
    data: List<ExpenseDataPoint>,
    modifier: Modifier = Modifier,
    chartType: ExpenseChartType = ExpenseChartType.LINE,
    period: ChartPeriod = ChartPeriod.MONTH,
    showAverage: Boolean = true,
    currency: String = "SAR"
) {
    var selectedDataPoint by remember { mutableStateOf<ExpenseDataPoint?>(null) }
    
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
            ExpenseChartHeader(
                data = data,
                period = period,
                currency = currency,
                selectedDataPoint = selectedDataPoint
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart content
            when (chartType) {
                ExpenseChartType.LINE -> LineExpenseChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    showAverage = showAverage,
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                ExpenseChartType.BAR -> BarExpenseChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
                ExpenseChartType.AREA -> AreaExpenseChart(
                    data = data,
                    onDataPointSelected = { selectedDataPoint = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
            }
            
            if (data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chart legend and stats
                ExpenseChartLegend(
                    data = data,
                    currency = currency,
                    showAverage = showAverage
                )
            }
        }
    }
}

@Composable
private fun ExpenseChartHeader(
    data: List<ExpenseDataPoint>,
    period: ChartPeriod,
    currency: String,
    selectedDataPoint: ExpenseDataPoint?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Expenses",
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
                imageVector = Icons.Filled.TrendingDown,
                contentDescription = "Expenses",
                tint = DariTheme.financialColors.Expense,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = selectedDataPoint?.let { formatAmount(it.amount, currency) }
                        ?: formatAmount(calculateTotalExpenses(data), currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DariTheme.financialColors.Expense
                )
                
                if (selectedDataPoint != null) {
                    Text(
                        text = formatDate(selectedDataPoint.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LineExpenseChart(
    data: List<ExpenseDataPoint>,
    onDataPointSelected: (ExpenseDataPoint?) -> Unit,
    showAverage: Boolean,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier = modifier)
        return
    }
    
    val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
    val minAmount = data.minOfOrNull { it.amount } ?: 0.0
    val average = data.map { it.amount }.average()
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 40.dp.toPx()
        val chartWidth = canvasWidth - (padding * 2)
        val chartHeight = canvasHeight - (padding * 2)
        
        // Draw grid lines
        drawChartGrid(
            maxAmount = maxAmount,
            minAmount = minAmount,
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = padding
        )
        
        // Draw line chart
        if (data.size > 1) {
            val path = Path()
            val points = mutableListOf<Offset>()
            
            data.forEachIndexed { index, dataPoint ->
                val x = padding + (index * chartWidth / (data.size - 1))
                val y = padding + chartHeight - ((dataPoint.amount - minAmount) / (maxAmount - minAmount) * chartHeight).toFloat()
                
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
                        DariTheme.financialColors.Expense.copy(alpha = 0.8f),
                        DariTheme.financialColors.Expense
                    )
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Draw data points
            points.forEach { point ->
                drawCircle(
                    color = DariTheme.financialColors.Expense,
                    radius = 4.dp.toPx(),
                    center = point
                )
                
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
        
        // Draw average line if enabled
        if (showAverage && data.isNotEmpty()) {
            val averageY = padding + chartHeight - ((average - minAmount) / (maxAmount - minAmount) * chartHeight).toFloat()
            
            drawLine(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                start = Offset(padding, averageY),
                end = Offset(padding + chartWidth, averageY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(5.dp.toPx(), 5.dp.toPx())
                )
            )
        }
    }
}

@Composable
private fun BarExpenseChart(
    data: List<ExpenseDataPoint>,
    onDataPointSelected: (ExpenseDataPoint?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier = modifier)
        return
    }
    
    val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
    val animatedBars = data.map { dataPoint ->
        animateFloatAsState(
            targetValue = (dataPoint.amount / maxAmount).toFloat(),
            animationSpec = tween(durationMillis = 1000),
            label = "bar_animation"
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
            
            // Draw bar with gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        DariTheme.financialColors.Expense.copy(alpha = 0.8f),
                        DariTheme.financialColors.Expense
                    )
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

@Composable
private fun AreaExpenseChart(
    data: List<ExpenseDataPoint>,
    onDataPointSelected: (ExpenseDataPoint?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(modifier = modifier)
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
                        DariTheme.financialColors.Expense.copy(alpha = 0.3f),
                        DariTheme.financialColors.Expense.copy(alpha = 0.1f),
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
                color = DariTheme.financialColors.Expense,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

private fun DrawScope.drawChartGrid(
    maxAmount: Double,
    minAmount: Double,
    chartWidth: Float,
    chartHeight: Float,
    padding: Float
) {
    val gridLines = 5
    val range = maxAmount - minAmount
    
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

@Composable
private fun ExpenseChartLegend(
    data: List<ExpenseDataPoint>,
    currency: String,
    showAverage: Boolean
) {
    if (data.isEmpty()) return
    
    val totalExpenses = calculateTotalExpenses(data)
    val averageExpenses = data.map { it.amount }.average()
    val highestExpense = data.maxByOrNull { it.amount }
    val lowestExpense = data.minByOrNull { it.amount }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            label = "Total",
            value = formatAmount(totalExpenses, currency),
            color = DariTheme.financialColors.Expense
        )
        
        if (showAverage) {
            LegendItem(
                label = "Average",
                value = formatAmount(averageExpenses, currency),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        highestExpense?.let {
            LegendItem(
                label = "Highest",
                value = formatAmount(it.amount, currency),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LegendItem(
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
private fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingDown,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No expense data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Chart type enum
 */
enum class ExpenseChartType {
    LINE, BAR, AREA
}

/**
 * Chart period enum
 */
enum class ChartPeriod(val displayName: String) {
    WEEK("This Week"),
    MONTH("This Month"),
    QUARTER("This Quarter"),
    YEAR("This Year"),
    CUSTOM("Custom Period")
}

/**
 * Helper functions
 */
private fun calculateTotalExpenses(data: List<ExpenseDataPoint>): Double {
    return data.sumOf { it.amount }
}

private fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format("%.0f", abs(amount))}"
}

private fun formatDate(date: LocalDate): String {
    return "${date.dayOfMonth}/${date.monthNumber}"
}

/**
 * Convert transactions to expense data points
 */
fun List<Transaction>.toExpenseDataPoints(
    startDate: LocalDate,
    endDate: LocalDate,
    groupBy: ChartPeriod = ChartPeriod.MONTH
): List<ExpenseDataPoint> {
    return this
        .filter { it.date >= startDate && it.date <= endDate }
        .filter { it.amount.amount < 0 } // Only expenses
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
            ExpenseDataPoint(
                date = date,
                amount = abs(transactions.sumOf { it.amount.amount }),
                categoryBreakdown = transactions
                    .groupBy { it.category.name }
                    .mapValues { (_, transactions) -> 
                        abs(transactions.sumOf { it.amount.amount })
                    }
            )
        }
        .sortedBy { it.date }
}