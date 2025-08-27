package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*

/**
 * Date picker component with Material 3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DariDatePicker(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select Date",
    enabled: Boolean = true,
    dateValidator: (LocalDate) -> Boolean = { true },
    initialDisplayedMonth: YearMonth = Clock.System.todayIn(TimeZone.currentSystemDefault()).let { 
        YearMonth(it.year, it.month) 
    }
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = selectedDate?.toString() ?: "",
        onValueChange = { /* Read-only */ },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = "Select date"
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { showDatePicker = true },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Open calendar"
                )
            }
        },
        readOnly = true,
        enabled = enabled,
        singleLine = true
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            initialDate = selectedDate,
            dateValidator = dateValidator,
            initialDisplayedMonth = initialDisplayedMonth
        )
    }
}

/**
 * Date picker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate? = null,
    dateValidator: (LocalDate) -> Boolean = { true },
    initialDisplayedMonth: YearMonth
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toEpochDays()?.times(24 * 60 * 60 * 1000L),
        initialDisplayedMonthMillis = initialDisplayedMonth.atDay(1).toEpochDays() * 24 * 60 * 60 * 1000L,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.fromEpochMilliseconds(utcTimeMillis)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
                return dateValidator(date)
            }
        }
    )
    
    DatePickerDialog(
        onDateSelected = { millis ->
            millis?.let { 
                val date = Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
                onDateSelected(date)
            }
        },
        onDismiss = onDismiss,
        datePickerState = datePickerState
    )
}

/**
 * Date range picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DariDateRangePicker(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeSelected: (startDate: LocalDate?, endDate: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    startLabel: String = "Start Date",
    endLabel: String = "End Date",
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DariDatePicker(
                selectedDate = startDate,
                onDateSelected = { date ->
                    onDateRangeSelected(date, endDate?.let { if (date <= it) it else null })
                },
                label = startLabel,
                enabled = enabled,
                dateValidator = { date ->
                    endDate?.let { date <= it } ?: true
                },
                modifier = Modifier.weight(1f)
            )
            
            DariDatePicker(
                selectedDate = endDate,
                onDateSelected = { date ->
                    onDateRangeSelected(startDate?.let { if (it <= date) it else null }, date)
                },
                label = endLabel,
                enabled = enabled,
                dateValidator = { date ->
                    startDate?.let { date >= it } ?: true
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Quick range selections
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(quickRangeOptions) { option ->
                FilterChip(
                    selected = false,
                    onClick = {
                        val range = option.getRange()
                        onDateRangeSelected(range.first, range.second)
                    },
                    label = { Text(option.label) }
                )
            }
        }
    }
}

/**
 * Month/Year picker
 */
@Composable
fun MonthYearPicker(
    selectedMonth: YearMonth?,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select Month",
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = selectedMonth?.let { "${it.month.name} ${it.year}" } ?: "",
        onValueChange = { /* Read-only */ },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = "Select month"
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { showPicker = true },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Open picker"
                )
            }
        },
        readOnly = true,
        enabled = enabled,
        singleLine = true
    )
    
    if (showPicker) {
        MonthYearPickerDialog(
            onMonthSelected = { month ->
                onMonthSelected(month)
                showPicker = false
            },
            onDismiss = { showPicker = false },
            initialMonth = selectedMonth
        )
    }
}

/**
 * Month/Year picker dialog
 */
@Composable
private fun MonthYearPickerDialog(
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    initialMonth: YearMonth? = null
) {
    val currentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var selectedYear by remember { mutableStateOf(initialMonth?.year ?: currentDate.year) }
    var selectedMonth by remember { mutableStateOf(initialMonth?.month ?: currentDate.month) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selection
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
                    }
                    
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next year")
                    }
                }
                
                // Month selection grid
                val months = Month.values()
                Column {
                    for (row in months.chunked(3)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (month in row) {
                                FilterChip(
                                    selected = month == selectedMonth,
                                    onClick = { selectedMonth = month },
                                    label = { Text(month.name.take(3)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onMonthSelected(YearMonth(selectedYear, selectedMonth)) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Quick date range options
 */
private val quickRangeOptions = listOf(
    QuickRangeOption("Today") {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        today to today
    },
    QuickRangeOption("Yesterday") {
        val yesterday = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY)
        yesterday to yesterday
    },
    QuickRangeOption("This Week") {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startOfWeek = today.minus((today.dayOfWeek.ordinal), DateTimeUnit.DAY)
        startOfWeek to today
    },
    QuickRangeOption("This Month") {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startOfMonth = LocalDate(today.year, today.month, 1)
        startOfMonth to today
    },
    QuickRangeOption("Last Month") {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val lastMonth = today.minus(1, DateTimeUnit.MONTH)
        val startOfLastMonth = LocalDate(lastMonth.year, lastMonth.month, 1)
        val endOfLastMonth = startOfLastMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        startOfLastMonth to endOfLastMonth
    }
)

private data class QuickRangeOption(
    val label: String,
    val getRange: () -> Pair<LocalDate, LocalDate>
)