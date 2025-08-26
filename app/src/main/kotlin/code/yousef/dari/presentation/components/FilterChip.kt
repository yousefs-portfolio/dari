package code.yousef.dari.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DariFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        } else null
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipGroup(
    chips: List<FilterChipData>,
    onChipClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        chips.forEachIndexed { index, chip ->
            DariFilterChip(
                label = chip.label,
                selected = chip.selected,
                onClick = { onChipClick(index) },
                enabled = chip.enabled
            )
        }
    }
}

data class FilterChipData(
    val label: String,
    val selected: Boolean = false,
    val enabled: Boolean = true
)

@Preview
@Composable
private fun FilterChipPreview() {
    MaterialTheme {
        DariFilterChip(
            label = "Dining",
            selected = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun FilterChipGroupPreview() {
    var chips by remember {
        mutableStateOf(
            listOf(
                FilterChipData("All", selected = true),
                FilterChipData("Income", selected = false),
                FilterChipData("Expenses", selected = false),
                FilterChipData("Dining", selected = true),
                FilterChipData("Transport", selected = false),
                FilterChipData("Shopping", selected = false)
            )
        )
    }
    
    MaterialTheme {
        FilterChipGroup(
            chips = chips,
            onChipClick = { index ->
                chips = chips.mapIndexed { i, chip ->
                    if (i == index) chip.copy(selected = !chip.selected)
                    else chip
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}