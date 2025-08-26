package code.yousef.dari.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0.00",
    currency: String = "SAR",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val formatter = remember {
        (NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat).apply {
            currency = java.util.Currency.getInstance("SAR")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    
    var isFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { it.isDigit() || it == '.' }
                
                // Prevent multiple decimal points
                val decimalCount = filtered.count { it == '.' }
                val processedText = if (decimalCount <= 1) {
                    // Limit decimal places to 2
                    val parts = filtered.split('.')
                    if (parts.size == 2 && parts[1].length > 2) {
                        "${parts[0]}.${parts[1].take(2)}"
                    } else {
                        filtered
                    }
                } else {
                    // Remove extra decimal points
                    val firstDecimalIndex = filtered.indexOf('.')
                    val beforeDecimal = filtered.substring(0, firstDecimalIndex + 1)
                    val afterDecimal = filtered.substring(firstDecimalIndex + 1).replace(".", "")
                    beforeDecimal + afterDecimal.take(2)
                }
                
                textFieldValue = TextFieldValue(processedText, TextRange(processedText.length))
                onValueChange(processedText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            suffix = { Text(currency) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            isError = isError,
            singleLine = true
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        
        if (!isFocused && value.isNotEmpty()) {
            val numericValue = value.toDoubleOrNull()
            if (numericValue != null) {
                Text(
                    text = "≈ ${formatter.format(numericValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CalculatorAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    currency: String = "SAR",
    enabled: Boolean = true
) {
    var showCalculator by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        AmountInput(
            value = value,
            onValueChange = onValueChange,
            label = label,
            currency = currency,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        
        // TODO: Add calculator dialog/bottom sheet
        if (showCalculator) {
            // Calculator implementation would go here
        }
    }
}

@Preview
@Composable
private fun AmountInputPreview() {
    var amount by remember { mutableStateOf("") }
    
    MaterialTheme {
        AmountInput(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun AmountInputWithValuePreview() {
    var amount by remember { mutableStateOf("125.50") }
    
    MaterialTheme {
        AmountInput(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun AmountInputErrorPreview() {
    var amount by remember { mutableStateOf("abc") }
    
    MaterialTheme {
        AmountInput(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            isError = true,
            errorMessage = "Invalid amount format",
            modifier = Modifier.padding(16.dp)
        )
    }
}