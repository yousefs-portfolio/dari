package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class EndDateOption {
    NEVER,
    ON_DATE,
    AFTER_OCCURRENCES
}

data class EndDateConfiguration(
    val option: EndDateOption = EndDateOption.NEVER,
    val endDate: Instant? = null,
    val maxOccurrences: Int? = null
)

@Composable
fun EndDateConfigurationSelector(
    configuration: EndDateConfiguration,
    onConfigurationChanged: (EndDateConfiguration) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = "End Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        EndDateOption.values().forEach { option ->
            EndDateOptionItem(
                option = option,
                configuration = configuration,
                isSelected = option == configuration.option,
                onSelected = { selectedOption ->
                    onConfigurationChanged(
                        when (selectedOption) {
                            EndDateOption.NEVER -> EndDateConfiguration(
                                option = EndDateOption.NEVER,
                                endDate = null,
                                maxOccurrences = null
                            )

                            EndDateOption.ON_DATE -> EndDateConfiguration(
                                option = EndDateOption.ON_DATE,
                                endDate = configuration.endDate ?: Clock.System.now().plus(365, DateTimeUnit.DAY),
                                maxOccurrences = null
                            )

                            EndDateOption.AFTER_OCCURRENCES -> EndDateConfiguration(
                                option = EndDateOption.AFTER_OCCURRENCES,
                                endDate = null,
                                maxOccurrences = configuration.maxOccurrences ?: 12
                            )
                        }
                    )
                },
                onConfigurationChanged = onConfigurationChanged,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndDateOptionItem(
    option: EndDateOption,
    configuration: EndDateConfiguration,
    isSelected: Boolean,
    onSelected: (EndDateOption) -> Unit,
    onConfigurationChanged: (EndDateConfiguration) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected,
                onClick = { onSelected(option) },
                role = Role.RadioButton,
                enabled = enabled
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSelected) null else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        },
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null, // Handle click in Surface
                    enabled = enabled
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getOptionTitle(option),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )

                    Text(
                        text = getOptionDescription(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }

            // Additional controls for selected options
            if (isSelected && enabled) {
                when (option) {
                    EndDateOption.ON_DATE -> {
                        Spacer(modifier = Modifier.height(12.dp))

                        var showDatePicker by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = configuration.endDate?.let { formatDate(it) } ?: "Select End Date"
                            )
                        }

                        if (showDatePicker) {
                            DatePickerDialog(
                                selectedDate = configuration.endDate,
                                onDateSelected = { selectedDate ->
                                    selectedDate?.let { date ->
                                        onConfigurationChanged(
                                            configuration.copy(endDate = date)
                                        )
                                    }
                                    showDatePicker = false
                                },
                                onDismiss = { showDatePicker = false }
                            )
                        }
                    }

                    EndDateOption.AFTER_OCCURRENCES -> {
                        Spacer(modifier = Modifier.height(12.dp))

                        var occurrenceText by remember(configuration.maxOccurrences) {
                            mutableStateOf(configuration.maxOccurrences?.toString() ?: "12")
                        }

                        OutlinedTextField(
                            value = occurrenceText,
                            onValueChange = { newValue ->
                                occurrenceText = newValue
                                newValue.toIntOrNull()?.let { occurrences ->
                                    if (occurrences > 0) {
                                        onConfigurationChanged(
                                            configuration.copy(maxOccurrences = occurrences)
                                        )
                                    }
                                }
                            },
                            label = { Text("Number of occurrences") },
                            suffix = { Text("payments") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    EndDateOption.NEVER -> {
                        // No additional controls needed
                    }
                }
            }
        }
    }
}

private fun getOptionTitle(option: EndDateOption): String {
    return when (option) {
        EndDateOption.NEVER -> "Never"
        EndDateOption.ON_DATE -> "On Date"
        EndDateOption.AFTER_OCCURRENCES -> "After Occurrences"
    }
}

private fun getOptionDescription(option: EndDateOption): String {
    return when (option) {
        EndDateOption.NEVER -> "Continue until manually stopped"
        EndDateOption.ON_DATE -> "End on a specific date"
        EndDateOption.AFTER_OCCURRENCES -> "End after a certain number of payments"
    }
}

private fun formatDate(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
}

@Composable
private fun DatePickerDialog(
    selectedDate: Instant?,
    onDateSelected: (Instant?) -> Unit,
    onDismiss: () -> Unit
) {
    // This would typically use the platform-specific date picker
    // For now, we'll create a simple implementation
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select End Date") },
        text = {
            Text("Date picker implementation would go here")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // For demo purposes, set a future date
                    onDateSelected(Clock.System.now().plus(365, DateTimeUnit.DAY))
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
