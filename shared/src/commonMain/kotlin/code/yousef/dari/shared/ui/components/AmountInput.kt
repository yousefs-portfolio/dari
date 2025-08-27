package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlin.math.abs

/**
 * Amount input field with currency formatting and validation
 */
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    placeholder: String = "0.00",
    currency: String = "SAR",
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onImeAction: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Format and validate the input
                val formatted = formatAmountInput(newValue)
                onValueChange(formatted)
            },
            label = { Text(label) },
            placeholder = { Text("$currency $placeholder") },
            leadingIcon = leadingIcon ?: {
                Text(
                    text = currency,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp)
                )
            },
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { 
                    focusManager.moveFocus(FocusDirection.Next)
                    onImeAction()
                }
            ),
            supportingText = supportingText?.let { text ->
                { Text(text) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Large amount input with enhanced styling
 */
@Composable
fun LargeAmountInput(
    amount: Money?,
    onAmountChange: (Money) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    isIncome: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var textValue by remember(amount) {
        mutableStateOf(amount?.amount?.toString() ?: "")
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = amount?.currency ?: "SAR",
                    style = DariTheme.financialTextStyles.CurrencyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        val formatted = formatAmountInput(newValue)
                        textValue = formatted
                        
                        formatted.toDoubleOrNull()?.let { amount ->
                            onAmountChange(Money(amount, "SAR"))
                        }
                    },
                    placeholder = { 
                        Text(
                            text = "0.00",
                            style = DariTheme.financialTextStyles.CurrencyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = DariTheme.financialTextStyles.CurrencyLarge.copy(
                        textAlign = TextAlign.Center,
                        color = if (isIncome) {
                            DariTheme.financialColors.Income
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isIncome) {
                            DariTheme.financialColors.Income
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    isError = isError,
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            }
            
            if (isError && errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Calculator-style amount input
 */
@Composable
fun CalculatorAmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "SAR"
) {
    Column(modifier = modifier) {
        // Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = currency,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = if (amount.isEmpty()) "0.00" else formatDisplayAmount(amount),
                    style = DariTheme.financialTextStyles.CurrencyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calculator buttons
        CalculatorKeypad(
            onNumberClick = { number ->
                onAmountChange(amount + number)
            },
            onDecimalClick = {
                if (!amount.contains(".")) {
                    onAmountChange(amount + ".")
                }
            },
            onBackspaceClick = {
                if (amount.isNotEmpty()) {
                    onAmountChange(amount.dropLast(1))
                }
            },
            onClearClick = {
                onAmountChange("")
            }
        )
    }
}

/**
 * Calculator keypad
 */
@Composable
private fun CalculatorKeypad(
    onNumberClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: 7, 8, 9, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton("7", Modifier.weight(1f)) { onNumberClick("7") }
            CalculatorButton("8", Modifier.weight(1f)) { onNumberClick("8") }
            CalculatorButton("9", Modifier.weight(1f)) { onNumberClick("9") }
            CalculatorButton("C", Modifier.weight(1f), isSecondary = true) { onClearClick() }
        }
        
        // Row 2: 4, 5, 6, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton("4", Modifier.weight(1f)) { onNumberClick("4") }
            CalculatorButton("5", Modifier.weight(1f)) { onNumberClick("5") }
            CalculatorButton("6", Modifier.weight(1f)) { onNumberClick("6") }
            CalculatorIconButton(
                icon = Icons.Filled.Backspace,
                modifier = Modifier.weight(1f),
                isSecondary = true
            ) { onBackspaceClick() }
        }
        
        // Row 3: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton("1", Modifier.weight(1f)) { onNumberClick("1") }
            CalculatorButton("2", Modifier.weight(1f)) { onNumberClick("2") }
            CalculatorButton("3", Modifier.weight(1f)) { onNumberClick("3") }
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Row 4: 0, decimal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton("0", Modifier.weight(2f)) { onNumberClick("0") }
            CalculatorButton(".", Modifier.weight(1f)) { onDecimalClick() }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = if (isSecondary) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun CalculatorIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = if (isSecondary) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Format amount input (remove invalid characters, limit decimal places)
 */
private fun formatAmountInput(input: String): String {
    // Remove all non-digit and non-decimal characters
    val cleaned = input.filter { it.isDigit() || it == '.' }
    
    // Ensure only one decimal point
    val parts = cleaned.split(".")
    return if (parts.size > 2) {
        "${parts[0]}.${parts.drop(1).joinToString("")}"
    } else if (parts.size == 2) {
        // Limit to 2 decimal places
        val decimal = parts[1].take(2)
        "${parts[0]}.$decimal"
    } else {
        cleaned
    }
}

/**
 * Format amount for display
 */
private fun formatDisplayAmount(amount: String): String {
    if (amount.isEmpty()) return "0.00"
    
    val number = amount.toDoubleOrNull() ?: return amount
    
    return String.format("%.2f", number)
}