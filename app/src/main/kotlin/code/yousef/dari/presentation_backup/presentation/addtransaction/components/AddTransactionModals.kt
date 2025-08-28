package code.yousef.dari.presentation.addtransaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.ui.components.*
import kotlinx.datetime.LocalDate

/**
 * All modal dialogs and sheets for the add transaction screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModals(
    uiState: AddTransactionUiState,
    showDatePicker: Boolean,
    showCategorySelector: Boolean,
    showAccountSelector: Boolean,
    showReceiptOptions: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onAccountSelected: (Account) -> Unit,
    onReceiptOptionSelected: (ReceiptOption) -> Unit,
    onDismissDatePicker: () -> Unit,
    onDismissCategorySelector: () -> Unit,
    onDismissAccountSelector: () -> Unit,
    onDismissReceiptOptions: () -> Unit
) {
    // Date Picker Modal
    if (showDatePicker) {
        DatePicker(
            selectedDate = uiState.date,
            onDateSelected = onDateSelected,
            onDismiss = onDismissDatePicker
        )
    }
    
    // Category Selector Modal
    if (showCategorySelector) {
        ModalBottomSheet(
            onDismissRequest = onDismissCategorySelector,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CategorySelectorContent(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected,
                onDismiss = onDismissCategorySelector
            )
        }
    }
    
    // Account Selector Modal
    if (showAccountSelector) {
        ModalBottomSheet(
            onDismissRequest = onDismissAccountSelector,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AccountSelectorContent(
                accounts = uiState.availableAccounts,
                selectedAccount = uiState.selectedAccount,
                onAccountSelected = onAccountSelected,
                onDismiss = onDismissAccountSelector
            )
        }
    }
    
    // Receipt Options Modal
    if (showReceiptOptions) {
        ReceiptOptionsModal(
            onOptionSelected = onReceiptOptionSelected,
            onDismiss = onDismissReceiptOptions
        )
    }
}

@Composable
private fun CategorySelectorContent(
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Category",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                onCategorySelected(category)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountSelectorContent(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts) { account ->
                AccountListItem(
                    account = account,
                    isSelected = account.id == selectedAccount?.id,
                    onClick = {
                        onAccountSelected(account)
                        onDismiss()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountListItem(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary
                )
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = account.type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${account.balance.currency} ${account.balance.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (account.balance.amount >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptOptionsModal(
    onOptionSelected: (ReceiptOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Receipt")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReceiptOptionItem(
                    icon = Icons.Default.Camera,
                    title = "Take Photo",
                    description = "Capture receipt with camera",
                    onClick = { 
                        onOptionSelected(ReceiptOption.CAMERA)
                    }
                )
                
                ReceiptOptionItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Choose from Gallery",
                    description = "Select existing photo",
                    onClick = { 
                        onOptionSelected(ReceiptOption.GALLERY)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReceiptOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Extension property for AccountType display name
private val AccountType.displayName: String
    get() = when (this) {
        AccountType.CHECKING -> "Checking Account"
        AccountType.SAVINGS -> "Savings Account"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.INVESTMENT -> "Investment Account"
        else -> "Unknown Account"
    }

// Placeholder data classes
data class AddTransactionUiState(
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault()),
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedAccount: Account? = null,
    val selectedCategory: Category? = null,
    val availableAccounts: List<Account> = emptyList(),
    val isRecurring: Boolean = false,
    val isSplitTransaction: Boolean = false,
    val showAdvancedOptions: Boolean = false,
    val attachedReceipts: List<String> = emptyList(),
    val splitTransactions: List<Any> = emptyList(),
    val recurringConfig: Any? = null,
    val recentTemplates: List<Any> = emptyList(),
    val smartSuggestions: List<Any> = emptyList(),
    val validation: ValidationState = ValidationState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTransactionSaved: Boolean = false
)

data class ValidationState(
    val isAmountError: Boolean = false,
    val amountError: String? = null,
    val isAccountError: Boolean = false,
    val accountError: String? = null,
    val isCategoryError: Boolean = false,
    val categoryError: String? = null,
    val isFormValid: Boolean = true
)

enum class AccountType {
    CHECKING, SAVINGS, CREDIT_CARD, INVESTMENT
}

enum class ReceiptOption {
    CAMERA, GALLERY
}