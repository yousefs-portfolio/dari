package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Filter chip component for selecting options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DariFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        trailingIcon = trailingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Multi-select filter chip group
 */
@Composable
fun MultiSelectFilterGroup(
    options: List<FilterOption>,
    selectedOptions: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Column(modifier = modifier) {
        title?.let { 
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(options) { option ->
                DariFilterChip(
                    selected = selectedOptions.contains(option.key),
                    onClick = {
                        val newSelection = if (selectedOptions.contains(option.key)) {
                            selectedOptions - option.key
                        } else {
                            selectedOptions + option.key
                        }
                        onSelectionChange(newSelection)
                    },
                    label = option.label,
                    leadingIcon = option.icon,
                    enabled = option.enabled
                )
            }
        }
    }
}

/**
 * Single-select filter chip group
 */
@Composable
fun SingleSelectFilterGroup(
    options: List<FilterOption>,
    selectedOption: String?,
    onSelectionChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    allowDeselection: Boolean = false
) {
    Column(modifier = modifier) {
        title?.let { 
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(options) { option ->
                DariFilterChip(
                    selected = selectedOption == option.key,
                    onClick = {
                        val newSelection = if (selectedOption == option.key && allowDeselection) {
                            null
                        } else {
                            option.key
                        }
                        onSelectionChange(newSelection)
                    },
                    label = option.label,
                    leadingIcon = option.icon,
                    enabled = option.enabled
                )
            }
        }
    }
}

/**
 * Financial category filter chips
 */
@Composable
fun CategoryFilterChips(
    selectedCategories: Set<String>,
    onCategoriesChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryOptions = remember {
        listOf(
            FilterOption("food", "Food & Dining", Icons.Filled.Restaurant),
            FilterOption("transport", "Transportation", Icons.Filled.DirectionsCar),
            FilterOption("shopping", "Shopping", Icons.Filled.ShoppingCart),
            FilterOption("bills", "Bills & Utilities", Icons.Filled.Receipt),
            FilterOption("entertainment", "Entertainment", Icons.Filled.Movie),
            FilterOption("health", "Health & Medical", Icons.Filled.LocalHospital),
            FilterOption("education", "Education", Icons.Filled.School),
            FilterOption("income", "Income", Icons.Filled.AttachMoney),
            FilterOption("other", "Other", Icons.Filled.Category)
        )
    }
    
    MultiSelectFilterGroup(
        options = categoryOptions,
        selectedOptions = selectedCategories,
        onSelectionChange = onCategoriesChange,
        title = "Categories",
        modifier = modifier
    )
}

/**
 * Time period filter chips
 */
@Composable
fun TimePeriodFilterChips(
    selectedPeriod: String?,
    onPeriodChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val periodOptions = remember {
        listOf(
            FilterOption("today", "Today", Icons.Filled.Today),
            FilterOption("week", "This Week", Icons.Filled.CalendarViewWeek),
            FilterOption("month", "This Month", Icons.Filled.CalendarToday),
            FilterOption("quarter", "This Quarter", Icons.Filled.DateRange),
            FilterOption("year", "This Year", Icons.Filled.CalendarMonth),
            FilterOption("all", "All Time", Icons.Filled.History)
        )
    }
    
    SingleSelectFilterGroup(
        options = periodOptions,
        selectedOption = selectedPeriod,
        onSelectionChange = onPeriodChange,
        title = "Time Period",
        allowDeselection = true,
        modifier = modifier
    )
}

/**
 * Transaction type filter chips
 */
@Composable
fun TransactionTypeFilterChips(
    selectedTypes: Set<String>,
    onTypesChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val typeOptions = remember {
        listOf(
            FilterOption("income", "Income", Icons.Filled.TrendingUp),
            FilterOption("expense", "Expense", Icons.Filled.TrendingDown),
            FilterOption("transfer", "Transfer", Icons.Filled.SwapHoriz)
        )
    }
    
    MultiSelectFilterGroup(
        options = typeOptions,
        selectedOptions = selectedTypes,
        onSelectionChange = onTypesChange,
        title = "Transaction Type",
        modifier = modifier
    )
}

/**
 * Amount range filter chips
 */
@Composable
fun AmountRangeFilterChips(
    selectedRange: String?,
    onRangeChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeOptions = remember {
        listOf(
            FilterOption("0-100", "Under 100 SAR", Icons.Filled.Money),
            FilterOption("100-500", "100 - 500 SAR", Icons.Filled.Money),
            FilterOption("500-1000", "500 - 1,000 SAR", Icons.Filled.Money),
            FilterOption("1000-5000", "1,000 - 5,000 SAR", Icons.Filled.Money),
            FilterOption("5000+", "Over 5,000 SAR", Icons.Filled.Money)
        )
    }
    
    SingleSelectFilterGroup(
        options = rangeOptions,
        selectedOption = selectedRange,
        onSelectionChange = onRangeChange,
        title = "Amount Range",
        allowDeselection = true,
        modifier = modifier
    )
}

/**
 * Filter option data class
 */
data class FilterOption(
    val key: String,
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true
)

/**
 * Clear all filters button
 */
@Composable
fun ClearFiltersButton(
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    hasActiveFilters: Boolean = true
) {
    if (hasActiveFilters) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClearAll) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All Filters")
            }
        }
    }
}