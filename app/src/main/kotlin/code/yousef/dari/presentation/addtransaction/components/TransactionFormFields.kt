package code.yousef.dari.presentation.addtransaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.TransactionType
import code.yousef.dari.shared.ui.components.*
import code.yousef.dari.shared.ui.components.forms.*
import kotlinx.datetime.LocalDate

/**
 * Core transaction form fields component
 */
@Composable
fun TransactionFormFields(
    uiState: AddTransactionUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCategoryClick: () -> Unit,
    onDateClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transaction Type Selector
        TransactionTypeSelector(
            selectedType = uiState.transactionType,
            onTypeSelected = onTransactionTypeChange,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Amount Input
        AmountInput(
            value = uiState.amount,
            onValueChange = onAmountChange,
            label = "Amount",
            currency = "SAR",
            isError = uiState.validation.isAmountError,
            errorMessage = uiState.validation.amountError,
            onImeAction = {
                focusManager.moveFocus(FocusDirection.Next)
            }
        )
        
        // Account Selection
        AccountSelector(
            selectedAccount = uiState.selectedAccount,
            onAccountSelected = onAccountClick,
            isError = uiState.validation.isAccountError,
            errorMessage = uiState.validation.accountError
        )
        
        // Category Selection
        CategorySelectorField(
            selectedCategory = uiState.selectedCategory,
            onCategoryClick = onCategoryClick,
            isError = uiState.validation.isCategoryError,
            errorMessage = uiState.validation.categoryError
        )
        
        // Description
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("Enter description (optional)") },
            leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null)
            },
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Date Selection
        DateSelectorField(
            selectedDate = uiState.date,
            onDateClick = onDateClick,
            label = "Transaction Date"
        )
    }
}

@Composable
private fun AccountSelector(
    selectedAccount: Account?,
    onAccountSelected: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = selectedAccount?.name ?: "",
        onValueChange = { },
        readOnly = true,
        label = { Text("Account") },
        placeholder = { Text("Select account") },
        leadingIcon = {
            Icon(Icons.Default.AccountBalance, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onAccountSelected) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .clickableWithoutRipple { onAccountSelected() }
    )
}

@Composable
private fun CategorySelectorField(
    selectedCategory: Category?,
    onCategoryClick: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = selectedCategory?.name ?: "",
        onValueChange = { },
        readOnly = true,
        label = { Text("Category") },
        placeholder = { Text("Select category") },
        leadingIcon = {
            selectedCategory?.let { category ->
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = getCategoryColor(category)
                )
            } ?: Icon(Icons.Default.Category, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onCategoryClick) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .clickableWithoutRipple { onCategoryClick() }
    )
}

@Composable
private fun DateSelectorField(
    selectedDate: LocalDate,
    onDateClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = formatDate(selectedDate),
        onValueChange = { },
        readOnly = true,
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Default.CalendarToday, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onDateClick) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickableWithoutRipple { onDateClick() }
    )
}

private fun formatDate(date: LocalDate): String {
    return "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
}

// Placeholder extensions - these would be implemented properly
private fun getCategoryIcon(category: Category) = Icons.Default.Category
private fun getCategoryColor(category: Category) = androidx.compose.ui.graphics.Color.Unspecified

@Composable
private fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}