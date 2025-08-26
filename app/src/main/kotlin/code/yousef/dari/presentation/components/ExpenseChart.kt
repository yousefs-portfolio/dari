package code.yousef.dari.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ExpenseData(
    val category: String,
    val amount: Double,
    val color: Color,
    val percentage: Float
)

data class MonthlyExpenseData(
    val month: String,
    val amount: Double
)

@Composable
fun ExpenseDonutChart(
    expenseData: List<ExpenseData>,
    modifier: Modifier = Modifier,
    centerContent: @Composable (() -> Unit)? = null
) {
    val total = expenseData.sumOf { it.amount }
    
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = minOf(canvasWidth, canvasHeight) / 2f * 0.8f
            val strokeWidth = radius * 0.3f
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            
            var startAngle = -90f
            
            expenseData.forEach { data ->
                val sweepAngle = (data.amount / total * 360).toFloat()
                
                drawArc(
                    color = data.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                
                startAngle += sweepAngle
            }
        }
        
        centerContent?.invoke()
    }
}

@Composable
fun ExpenseBarChart(
    monthlyData: List<MonthlyExpenseData>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxAmount = monthlyData.maxOfOrNull { it.amount } ?: 0.0
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Monthly Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(monthlyData) { data ->
                    ExpenseBar(
                        month = data.month,
                        amount = data.amount,
                        maxAmount = maxAmount,
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseBar(
    month: String,
    amount: Double,
    maxAmount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val barHeight = 120.dp
    val barWidth = 32.dp
    val heightRatio = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight * heightRatio)
            ) {
                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0.7f)
                    )
                )
                
                drawRoundRect(
                    brush = gradient,
                    size = Size(size.width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = month,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ExpenseAreaChart(
    monthlyData: List<MonthlyExpenseData>,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val maxAmount = monthlyData.maxOfOrNull { it.amount } ?: 0.0
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Expense Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (monthlyData.isEmpty()) return@Canvas
                
                val canvasWidth = size.width
                val canvasHeight = size.height
                val padding = 40.dp.toPx()
                
                val chartWidth = canvasWidth - (padding * 2)
                val chartHeight = canvasHeight - (padding * 2)
                
                val stepX = chartWidth / (monthlyData.size - 1).coerceAtLeast(1)
                
                // Create path for area
                val areaPath = Path()
                val linePath = Path()
                
                monthlyData.forEachIndexed { index, data ->
                    val x = padding + (index * stepX)
                    val y = padding + (chartHeight - (data.amount / maxAmount * chartHeight).toFloat())
                    
                    if (index == 0) {
                        areaPath.moveTo(x, size.height - padding)
                        areaPath.lineTo(x, y)
                        linePath.moveTo(x, y)
                    } else {
                        areaPath.lineTo(x, y)
                        linePath.lineTo(x, y)
                    }
                    
                    // Draw data points
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                
                // Complete area path
                if (monthlyData.isNotEmpty()) {
                    areaPath.lineTo(padding + ((monthlyData.size - 1) * stepX), size.height - padding)
                    areaPath.close()
                }
                
                // Draw area
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fillColor,
                            fillColor.copy(alpha = 0.1f)
                        )
                    )
                )
                
                // Draw line
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun ExpenseCategoryLegend(
    expenseData: List<ExpenseData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            expenseData.forEach { data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = CircleShape,
                        color = data.color
                    ) {}
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = data.category,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatAmount(data.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${data.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return try {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = Currency.getInstance("SAR")
        formatter.format(amount)
    } catch (e: Exception) {
        "SAR ${String.format("%.0f", amount)}"
    }
}

@Preview
@Composable
private fun ExpenseChartsPreview() {
    val sampleExpenseData = listOf(
        ExpenseData("Dining", 2500.0, Color(0xFF6200EE), 35f),
        ExpenseData("Transport", 1800.0, Color(0xFF3700B3), 25f),
        ExpenseData("Shopping", 1500.0, Color(0xFF03DAC6), 21f),
        ExpenseData("Entertainment", 850.0, Color(0xFFFF6B6B), 12f),
        ExpenseData("Others", 500.0, Color(0xFF4ECDC4), 7f)
    )
    
    val sampleMonthlyData = listOf(
        MonthlyExpenseData("Jan", 5200.0),
        MonthlyExpenseData("Feb", 4800.0),
        MonthlyExpenseData("Mar", 6200.0),
        MonthlyExpenseData("Apr", 5500.0),
        MonthlyExpenseData("May", 7150.0),
        MonthlyExpenseData("Jun", 6800.0)
    )
    
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpenseDonutChart(
                expenseData = sampleExpenseData,
                centerContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatAmount(sampleExpenseData.sumOf { it.amount }),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
            
            ExpenseCategoryLegend(expenseData = sampleExpenseData)
            
            ExpenseBarChart(monthlyData = sampleMonthlyData)
            
            ExpenseAreaChart(monthlyData = sampleMonthlyData)
        }
    }
}