package code.yousef.dari.shared.ui.components.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Money
import kotlinx.datetime.LocalDate

/**
 * Reusable form fields for transaction creation/editing
 * Extracted from AddTransactionScreen to reduce complexity and improve reusability
 */

@Composable
fun TransactionAmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            placeholder = { Text("0.00") },
            leadingIcon = { Text(currency, style = MaterialTheme.typography.bodyMedium) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun TransactionDescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("Enter transaction description") },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = isError,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun TransactionMerchantField(
    merchantName: String?,
    onMerchantChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = merchantName ?: "",
        onValueChange = onMerchantChange,
        label = { Text("Merchant (Optional)") },
        placeholder = { Text("Where did you spend?") },
        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun TransactionCategoryField(
    selectedCategory: String?,
    onCategoryClick: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedCategory ?: "",
            onValueChange = { },
            label = { Text("Category") },
            placeholder = { Text("Select category") },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onCategoryClick) {
                    Icon(Icons.Default.Category, contentDescription = "Select category")
                }
            },
            readOnly = true,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun TransactionDateField(
    selectedDate: LocalDate,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = selectedDate.toString(),
        onValueChange = { },
        label = { Text("Date") },
        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onDateClick) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
            }
        },
        readOnly = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun TransactionNotesField(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notes (Optional)") },
        placeholder = { Text("Add any additional notes...") },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        maxLines = 3,
        modifier = modifier.fillMaxWidth()
    )
}