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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Category
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.Transaction
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlin.math.*

/**
 * Data class for category breakdown data
 */
data class CategoryExpense(
    val category: Category,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int,
    val averageTransaction: Double,
    val subcategoryBreakdown: Map<String, Double> = emptyMap()
)

/**
 * Category breakdown component showing spending by category
 */
@Composable
fun CategoryBreakdown(
    expenses: List<CategoryExpense>,
    modifier: Modifier = Modifier,
    viewType: CategoryBreakdownViewType = CategoryBreakdownViewType.PIE_CHART,
    showPercentages: Boolean = true,
    showSubcategories: Boolean = false,
    maxCategories: Int = 10,
    currency: String = "SAR"
) {
    val sortedExpenses = expenses
        .sortedByDescending { it.amount }
        .take(maxCategories)
    
    var selectedCategory by remember { mutableStateOf<CategoryExpense?>(null) }
    
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
            CategoryBreakdownHeader(
                expenses = sortedExpenses,
                selectedCategory = selectedCategory,
                currency = currency
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main content based on view type
            when (viewType) {
                CategoryBreakdownViewType.PIE_CHART -> PieChartBreakdown(
                    expenses = sortedExpenses,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    currency = currency,
                    modifier = Modifier.height(250.dp)
                )
                CategoryBreakdownViewType.HORIZONTAL_BARS -> HorizontalBarsBreakdown(
                    expenses = sortedExpenses,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    currency = currency,
                    showPercentages = showPercentages
                )
                CategoryBreakdownViewType.LIST -> ListBreakdown(
                    expenses = sortedExpenses,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    currency = currency,
                    showSubcategories = showSubcategories
                )
                CategoryBreakdownViewType.TREEMAP -> TreemapBreakdown(
                    expenses = sortedExpenses,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    currency = currency,
                    modifier = Modifier.height(200.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownHeader(
    expenses: List<CategoryExpense>,
    selectedCategory: CategoryExpense?,
    currency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (selectedCategory != null) {
                Text(
                    text = selectedCategory.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${expenses.size} categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = selectedCategory?.let { formatAmount(it.amount, currency) }
                    ?: formatAmount(expenses.sumOf { it.amount }, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DariTheme.financialColors.Expense
            )
            
            if (selectedCategory != null) {
                Text(
                    text = "${String.format("%.1f", selectedCategory.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PieChartBreakdown(
    expenses: List<CategoryExpense>,
    selectedCategory: CategoryExpense?,
    onCategorySelected: (CategoryExpense?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        EmptyCategoryPlaceholder(modifier = modifier)
        return
    }
    
    Row(modifier = modifier.fillMaxWidth()) {
        // Pie chart
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                expenses = expenses,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxSize()
            )
            
            // Center info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.PieChart,
                    contentDescription = "Categories",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "${expenses.size}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Legend
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(expenses) { expense ->
                PieChartLegendItem(
                    expense = expense,
                    isSelected = selectedCategory == expense,
                    onSelected = { onCategorySelected(if (selectedCategory == expense) null else expense) },
                    currency = currency
                )
            }
        }
    }
}

@Composable
private fun PieChart(
    expenses: List<CategoryExpense>,
    selectedCategory: CategoryExpense?,
    onCategorySelected: (CategoryExpense?) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAngles = expenses.map { expense ->
        animateFloatAsState(
            targetValue = expense.percentage / 100f * 360f,
            animationSpec = tween(durationMillis = 1000),
            label = "pie_slice_${expense.category.id}"
        )
    }
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f * 0.8f
        val strokeWidth = 60.dp.toPx()
        
        var startAngle = 0f
        
        expenses.forEachIndexed { index, expense ->
            val sweepAngle = animatedAngles[index].value
            val isSelected = selectedCategory == expense
            val actualRadius = if (isSelected) radius + 10.dp.toPx() else radius
            
            // Draw pie slice
            drawArc(
                color = getCategoryColor(expense.category, index),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(actualRadius * 2, actualRadius * 2),
                topLeft = Offset(center.x - actualRadius, center.y - actualRadius)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun PieChartLegendItem(
    expense: CategoryExpense,
    isSelected: Boolean,
    onSelected: () -> Unit,
    currency: String
) {
    Surface(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
               else Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(expense.category, 0))
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${String.format("%.1f", expense.percentage)}% • ${expense.transactionCount} transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatAmount(expense.amount, currency),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = DariTheme.financialColors.Expense
            )
        }
    }
}

@Composable
private fun HorizontalBarsBreakdown(
    expenses: List<CategoryExpense>,
    selectedCategory: CategoryExpense?,
    onCategorySelected: (CategoryExpense?) -> Unit,
    currency: String,
    showPercentages: Boolean
) {
    if (expenses.isEmpty()) {
        EmptyCategoryPlaceholder()
        return
    }
    
    val maxAmount = expenses.maxOfOrNull { it.amount } ?: 0.0
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenses) { expense ->
            HorizontalBarItem(
                expense = expense,
                maxAmount = maxAmount,
                isSelected = selectedCategory == expense,
                onSelected = { onCategorySelected(if (selectedCategory == expense) null else expense) },
                currency = currency,
                showPercentages = showPercentages
            )
        }
    }
}

@Composable
private fun HorizontalBarItem(
    expense: CategoryExpense,
    maxAmount: Double,
    isSelected: Boolean,
    onSelected: () -> Unit,
    currency: String,
    showPercentages: Boolean
) {
    val progress = (expense.amount / maxAmount).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "bar_progress_${expense.category.id}"
    )
    
    Surface(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
               else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = expense.category.name,
                        tint = getCategoryColor(expense.category, 0),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = expense.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showPercentages) {
                        Text(
                            text = "${String.format("%.1f", expense.percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = formatAmount(expense.amount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DariTheme.financialColors.Expense
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                color = getCategoryColor(expense.category, 0),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${expense.transactionCount} transactions • Avg: ${formatAmount(expense.averageTransaction, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListBreakdown(
    expenses: List<CategoryExpense>,
    selectedCategory: CategoryExpense?,
    onCategorySelected: (CategoryExpense?) -> Unit,
    currency: String,
    showSubcategories: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(expenses) { expense ->
            CategoryListItem(
                expense = expense,
                isSelected = selectedCategory == expense,
                onSelected = { onCategorySelected(if (selectedCategory == expense) null else expense) },
                currency = currency,
                showSubcategories = showSubcategories && selectedCategory == expense
            )
        }
    }
}

@Composable
private fun CategoryListItem(
    expense: CategoryExpense,
    isSelected: Boolean,
    onSelected: () -> Unit,
    currency: String,
    showSubcategories: Boolean
) {
    Surface(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
               else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category),
                    contentDescription = expense.category.name,
                    tint = getCategoryColor(expense.category, 0),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "${expense.transactionCount} transactions • ${String.format("%.1f", expense.percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAmount(expense.amount, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DariTheme.financialColors.Expense
                    )
                    
                    Text(
                        text = "Avg: ${formatAmount(expense.averageTransaction, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (showSubcategories && expense.subcategoryBreakdown.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                expense.subcategoryBreakdown.entries.forEach { (subcategory, amount) ->
                    SubcategoryItem(
                        name = subcategory,
                        amount = amount,
                        percentage = (amount / expense.amount * 100).toFloat(),
                        currency = currency
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryItem(
    name: String,
    amount: Double,
    percentage: Float,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "${String.format("%.1f", percentage)}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = formatAmount(amount, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = DariTheme.financialColors.Expense
        )
    }
}

@Composable
private fun TreemapBreakdown(
    expenses: List<CategoryExpense>,
    selectedCategory: CategoryExpense?,
    onCategorySelected: (CategoryExpense?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    // Simple treemap implementation
    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalAmount = expenses.sumOf { it.amount }
        
        var currentX = 0f
        var currentY = 0f
        var rowHeight = 0f
        var rowWidth = 0f
        
        expenses.forEach { expense ->
            val area = (expense.amount / totalAmount * canvasWidth * canvasHeight).toFloat()
            val width = sqrt(area * (canvasWidth / canvasHeight))
            val height = area / width
            
            // Check if we need a new row
            if (currentX + width > canvasWidth) {
                currentY += rowHeight
                currentX = 0f
                rowHeight = 0f
                rowWidth = 0f
            }
            
            // Draw rectangle
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        getCategoryColor(expense.category, 0),
                        getCategoryColor(expense.category, 0).copy(alpha = 0.8f)
                    )
                ),
                topLeft = Offset(currentX, currentY),
                size = Size(width, height)
            )
            
            // Draw border
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(currentX, currentY),
                size = Size(width, height),
                style = Stroke(width = 1.dp.toPx())
            )
            
            currentX += width
            rowHeight = maxOf(rowHeight, height)
            rowWidth += width
        }
    }
}

@Composable
private fun EmptyCategoryPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.PieChart,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "No category data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Category breakdown view type enum
 */
enum class CategoryBreakdownViewType {
    PIE_CHART, HORIZONTAL_BARS, LIST, TREEMAP
}

/**
 * Helper functions
 */
@Composable
private fun getCategoryColor(category: Category, index: Int): Color {
    return when (category.name.lowercase()) {
        "food", "dining", "restaurants" -> Color(0xFFFF9800) // Orange
        "transportation", "gas", "fuel" -> Color(0xFF2196F3) // Blue
        "shopping", "retail" -> Color(0xFFE91E63) // Pink
        "entertainment", "movies" -> Color(0xFF9C27B0) // Purple
        "health", "medical" -> Color(0xFFF44336) // Red
        "utilities", "bills" -> Color(0xFF4CAF50) // Green
        "education", "books" -> Color(0xFF3F51B5) // Indigo
        "travel", "vacation" -> Color(0xFF00BCD4) // Cyan
        else -> {
            val colors = listOf(
                Color(0xFF607D8B), // Blue Grey
                Color(0xFF795548), // Brown
                Color(0xFFFFEB3B), // Yellow
                Color(0xFFCDDC39), // Lime
                Color(0xFF8BC34A), // Light Green
                Color(0xFF009688)  // Teal
            )
            colors[index % colors.size]
        }
    }
}

private fun getCategoryIcon(category: Category): ImageVector {
    return when (category.name.lowercase()) {
        "food", "dining", "restaurants" -> Icons.Filled.Restaurant
        "transportation", "gas", "fuel" -> Icons.Filled.DirectionsCar
        "shopping", "retail" -> Icons.Filled.ShoppingCart
        "entertainment", "movies" -> Icons.Filled.Movie
        "health", "medical" -> Icons.Filled.LocalHospital
        "utilities", "bills" -> Icons.Filled.ElectricBolt
        "education", "books" -> Icons.Filled.School
        "travel", "vacation" -> Icons.Filled.Flight
        "home", "housing" -> Icons.Filled.Home
        "insurance" -> Icons.Filled.Security
        else -> Icons.Filled.Category
    }
}

private fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format("%.0f", abs(amount))}"
}

/**
 * Convert transactions to category expenses
 */
fun List<Transaction>.toCategoryExpenses(): List<CategoryExpense> {
    val totalAmount = this.filter { it.amount.amount < 0 }.sumOf { abs(it.amount.amount) }
    
    return this
        .filter { it.amount.amount < 0 } // Only expenses
        .groupBy { it.category }
        .map { (category, transactions) ->
            val categoryAmount = transactions.sumOf { abs(it.amount.amount) }
            
            CategoryExpense(
                category = category,
                amount = categoryAmount,
                percentage = (categoryAmount / totalAmount * 100).toFloat(),
                transactionCount = transactions.size,
                averageTransaction = categoryAmount / transactions.size,
                subcategoryBreakdown = transactions
                    .groupBy { it.subcategory ?: "Other" }
                    .mapValues { (_, transactions) ->
                        transactions.sumOf { abs(it.amount.amount) }
                    }
                    .filter { it.value > 0 }
            )
        }
        .sortedByDescending { it.amount }
}