package code.yousef.dari.presentation.addtransaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import code.yousef.dari.R
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.domain.models.TransactionType
import code.yousef.dari.shared.presentation.components.*
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * Add Transaction Screen
 * Comprehensive transaction creation with validation, smart categorization,
 * split transactions, recurring setup, and receipt attachments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    onTransactionSaved: () -> Unit,
    onCameraCapture: () -> Unit,
    onGallerySelect: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var showReceiptOptions by remember { mutableStateOf(false) }

    // Handle transaction saved
    LaunchedEffect(uiState.isTransactionSaved) {
        if (uiState.isTransactionSaved) {
            onTransactionSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.add_transaction),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    // Clear form
                    IconButton(
                        onClick = { viewModel.clearForm() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.clear_form)
                        )
                    }
                    
                    // Apply category suggestion
                    if (uiState.merchantName.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.applyCategorySuggestion() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.suggest_category),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            AddTransactionBottomBar(
                onSave = { viewModel.saveTransaction() },
                onCamera = { showReceiptOptions = true },
                isValid = viewModel.validateInput().isValid,
                isSaving = uiState.isSaving
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Transaction Info
            item {
                TransactionBasicInfoCard(
                    uiState = uiState,
                    onDescriptionChange = viewModel::updateDescription,
                    onAmountChange = viewModel::updateAmount,
                    onTypeChange = viewModel::updateType,
                    focusManager = focusManager
                )
            }

            // Category Selection
            item {
                TransactionCategoryCard(
                    selectedCategory = uiState.category,
                    onCategoryClick = { showCategorySelector = true }
                )
            }

            // Date Selection
            item {
                TransactionDateCard(
                    selectedDate = uiState.date,
                    onDateClick = { showDatePicker = true }
                )
            }

            // Merchant Information
            item {
                TransactionMerchantCard(
                    merchantName = uiState.merchantName,
                    onMerchantNameChange = viewModel::updateMerchantName,
                    focusManager = focusManager
                )
            }

            // Receipts Section
            if (uiState.receiptUrls.isNotEmpty()) {
                item {
                    TransactionReceiptsCard(
                        receiptUrls = uiState.receiptUrls,
                        onReceiptRemove = viewModel::removeReceiptUrl,
                        onReceiptView = { /* TODO: View receipt */ }
                    )
                }
            }

            // Advanced Options
            item {
                TransactionAdvancedOptionsCard(
                    isRecurring = uiState.isRecurring,
                    isSplitTransaction = uiState.isSplitTransaction,
                    onToggleRecurring = viewModel::toggleRecurring,
                    onToggleSplit = viewModel::toggleSplitTransaction
                )
            }

            // Recurring Transaction Settings
            AnimatedVisibility(
                visible = uiState.isRecurring,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                item {
                    RecurringTransactionCard(
                        frequency = uiState.recurrenceFrequency,
                        endDate = uiState.recurrenceEndDate,
                        onFrequencyChange = viewModel::updateRecurrenceFrequency,
                        onEndDateChange = viewModel::updateRecurrenceEndDate
                    )
                }
            }

            // Split Transaction Settings
            AnimatedVisibility(
                visible = uiState.isSplitTransaction,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                item {
                    SplitTransactionCard(
                        totalAmount = uiState.amount,
                        splitAmounts = uiState.splitAmounts,
                        onSplitAmountUpdate = viewModel::updateSplitAmount,
                        onAddSplit = { viewModel.addSplitAmount(
                            SplitAmount("Split ${uiState.splitAmounts.size + 1}", Money.fromDouble(0.0, "SAR"))
                        ) },
                        onRemoveSplit = viewModel::removeSplitAmount
                    )
                }
            }

            // Add some bottom spacing
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = uiState.date,
            onDateSelected = { newDate ->
                viewModel.updateDate(newDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Category Selector Dialog
    if (showCategorySelector) {
        CategorySelectorDialog(
            selectedCategory = uiState.category,
            onCategorySelected = { category ->
                viewModel.updateCategory(category)
                showCategorySelector = false
            },
            onDismiss = { showCategorySelector = false }
        )
    }

    // Receipt Options Sheet
    if (showReceiptOptions) {
        ReceiptOptionsBottomSheet(
            onCameraCapture = {
                onCameraCapture()
                showReceiptOptions = false
            },
            onGallerySelect = {
                onGallerySelect()
                showReceiptOptions = false
            },
            onDismiss = { showReceiptOptions = false }
        )
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.dismissError()
        }
    }
}

@Composable
private fun TransactionBasicInfoCard(
    uiState: AddTransactionUiState,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.transaction_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Description Field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.description_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                supportingText = {
                    Text(stringResource(R.string.description_hint))
                }
            )

            // Amount and Type Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount Field
                OutlinedTextField(
                    value = if (uiState.amount.amount > 0) uiState.amount.amount.toString() else "",
                    onValueChange = { input ->
                        input.toDoubleOrNull()?.let { amount ->
                            onAmountChange(amount)
                        }
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    leadingIcon = {
                        Text(
                            text = "SAR",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Transaction Type Toggle
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SegmentedButton(
                        options = listOf(
                            stringResource(R.string.expense) to TransactionType.DEBIT,
                            stringResource(R.string.income) to TransactionType.CREDIT
                        ),
                        selectedOption = uiState.type,
                        onOptionSelected = onTypeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCategoryCard(
    selectedCategory: String,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCategoryClick() }
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
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedCategory,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionDateCard(
    selectedDate: LocalDate,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDateClick() }
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
                    text = stringResource(R.string.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedDate.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TransactionMerchantCard(
    merchantName: String,
    onMerchantNameChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.merchant_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = merchantName,
                onValueChange = onMerchantNameChange,
                label = { Text(stringResource(R.string.merchant_name)) },
                placeholder = { Text(stringResource(R.string.merchant_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                leadingIcon = {
                    Icon(Icons.Default.Store, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun TransactionReceiptsCard(
    receiptUrls: List<String>,
    onReceiptRemove: (String) -> Unit,
    onReceiptView: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.receipts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(receiptUrls) { receiptUrl ->
                    ReceiptPreviewCard(
                        receiptUrl = receiptUrl,
                        onView = { onReceiptView(receiptUrl) },
                        onRemove = { onReceiptRemove(receiptUrl) },
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionAdvancedOptionsCard(
    isRecurring: Boolean,
    isSplitTransaction: Boolean,
    onToggleRecurring: () -> Unit,
    onToggleSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.advanced_options),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Recurring Transaction Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRecurring() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.recurring_transaction),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.recurring_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isRecurring,
                    onCheckedChange = { onToggleRecurring() }
                )
            }

            Divider()

            // Split Transaction Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSplit() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.split_transaction),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.split_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isSplitTransaction,
                    onCheckedChange = { onToggleSplit() }
                )
            }
        }
    }
}

@Composable
private fun RecurringTransactionCard(
    frequency: RecurrenceFrequency,
    endDate: LocalDate?,
    onFrequencyChange: (RecurrenceFrequency) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.recurring_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Frequency Selection
            Text(
                text = stringResource(R.string.frequency),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RecurrenceFrequency.entries) { freq ->
                    FilterChip(
                        onClick = { onFrequencyChange(freq) },
                        label = { Text(freq.name) },
                        selected = frequency == freq
                    )
                }
            }

            // End Date (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.end_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = endDate?.toString() ?: stringResource(R.string.never),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                TextButton(
                    onClick = { /* TODO: Show date picker */ }
                ) {
                    Text(stringResource(R.string.change))
                }
            }
        }
    }
}

@Composable
private fun SplitTransactionCard(
    totalAmount: Money,
    splitAmounts: List<SplitAmount>,
    onSplitAmountUpdate: (Int, SplitAmount) -> Unit,
    onAddSplit: () -> Unit,
    onRemoveSplit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.split_amounts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Total: ${totalAmount.formattedAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            splitAmounts.forEachIndexed { index, splitAmount ->
                SplitAmountRow(
                    index = index,
                    splitAmount = splitAmount,
                    onUpdate = onSplitAmountUpdate,
                    onRemove = if (splitAmounts.size > 1) onRemoveSplit else null
                )
            }

            OutlinedButton(
                onClick = onAddSplit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_split))
            }

            // Validation Summary
            val totalSplitAmount = splitAmounts.sumOf { it.amount.amount }
            val difference = totalAmount.amount - totalSplitAmount
            
            if (kotlin.math.abs(difference) > 0.01) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.split_validation_error,
                            Money.fromDouble(difference, "SAR").formattedAmount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitAmountRow(
    index: Int,
    splitAmount: SplitAmount,
    onUpdate: (Int, SplitAmount) -> Unit,
    onRemove: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = splitAmount.description,
            onValueChange = { newDescription ->
                onUpdate(index, splitAmount.copy(description = newDescription))
            },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        OutlinedTextField(
            value = if (splitAmount.amount.amount > 0) splitAmount.amount.amount.toString() else "",
            onValueChange = { input ->
                input.toDoubleOrNull()?.let { amount ->
                    onUpdate(index, splitAmount.copy(amount = Money.fromDouble(amount, "SAR")))
                }
            },
            label = { Text(stringResource(R.string.amount)) },
            modifier = Modifier.width(100.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        onRemove?.let { removeAction ->
            IconButton(
                onClick = { removeAction(index) }
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.remove_split),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddTransactionBottomBar(
    onSave: () -> Unit,
    onCamera: () -> Unit,
    isValid: Boolean,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        actions = {
            IconButton(onClick = onCamera) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.attach_receipt)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onSave,
                icon = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                },
                text = { 
                    Text(
                        if (isSaving) stringResource(R.string.saving) 
                        else stringResource(R.string.save_transaction)
                    )
                },
                containerColor = if (isValid) 
                    MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isValid) 
                    MaterialTheme.colorScheme.onPrimary 
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

// Helper composables and dialogs would be implemented here
@Composable
fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement Material3 DatePicker
}

@Composable
fun CategorySelectorDialog(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement category selection dialog
}

@Composable
fun ReceiptOptionsBottomSheet(
    onCameraCapture: () -> Unit,
    onGallerySelect: () -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement receipt options bottom sheet
}

@Composable
fun SegmentedButton(
    options: List<Pair<String, TransactionType>>,
    selectedOption: TransactionType,
    onOptionSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Implement segmented button for transaction type selection
}