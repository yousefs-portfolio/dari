package code.yousef.dari.presentation.addtransaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import code.yousef.dari.R
import code.yousef.dari.presentation.addtransaction.components.*
import code.yousef.dari.shared.ui.components.*
import org.koin.androidx.compose.koinViewModel

/**
 * Add Transaction Screen - Refactored for better maintainability
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
    var showAccountSelector by remember { mutableStateOf(false) }
    var showReceiptOptions by remember { mutableStateOf(false) }

    // Handle transaction saved
    LaunchedEffect(uiState.isTransactionSaved) {
        if (uiState.isTransactionSaved) {
            onTransactionSaved()
        }
    }

    Scaffold(
        topBar = {
            AddTransactionTopBar(
                onNavigateBack = onNavigateBack,
                onClearForm = viewModel::clearForm,
                onSaveTemplate = viewModel::saveAsTemplate
            )
        },
        bottomBar = {
            TransactionActionButtons(
                canSave = uiState.validation.isFormValid,
                isLoading = uiState.isLoading,
                onSave = viewModel::saveTransaction,
                onSaveAndAddAnother = viewModel::saveAndAddAnother
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
                
                uiState.error != null -> {
                    ErrorMessage(
                        error = uiState.error,
                        onRetry = { viewModel.retry() },
                        onDismiss = { viewModel.dismissError() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    AddTransactionContent(
                        uiState = uiState,
                        onAmountChange = viewModel::updateAmount,
                        onDescriptionChange = viewModel::updateDescription,
                        onTransactionTypeChange = viewModel::updateTransactionType,
                        onCategoryClick = { showCategorySelector = true },
                        onDateClick = { showDatePicker = true },
                        onAccountClick = { showAccountSelector = true },
                        onToggleRecurring = viewModel::toggleRecurring,
                        onToggleSplitTransaction = viewModel::toggleSplitTransaction,
                        onToggleAdvancedOptions = viewModel::toggleAdvancedOptions,
                        onAddReceiptClick = { showReceiptOptions = true },
                        onRemoveReceipt = viewModel::removeReceipt
                    )
                }
            }
        }
    }

    // Dialogs and Modals
    AddTransactionModals(
        uiState = uiState,
        showDatePicker = showDatePicker,
        showCategorySelector = showCategorySelector,
        showAccountSelector = showAccountSelector,
        showReceiptOptions = showReceiptOptions,
        onDateSelected = { date ->
            viewModel.updateDate(date)
            showDatePicker = false
        },
        onCategorySelected = { category ->
            viewModel.updateCategory(category)
            showCategorySelector = false
        },
        onAccountSelected = { account ->
            viewModel.updateAccount(account)
            showAccountSelector = false
        },
        onReceiptOptionSelected = { option ->
            showReceiptOptions = false
            when (option) {
                ReceiptOption.CAMERA -> onCameraCapture()
                ReceiptOption.GALLERY -> onGallerySelect()
            }
        },
        onDismissDatePicker = { showDatePicker = false },
        onDismissCategorySelector = { showCategorySelector = false },
        onDismissAccountSelector = { showAccountSelector = false },
        onDismissReceiptOptions = { showReceiptOptions = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionTopBar(
    onNavigateBack: () -> Unit,
    onClearForm: () -> Unit,
    onSaveTemplate: () -> Unit
) {
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
            // Save as template
            IconButton(onClick = onSaveTemplate) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Save as template"
                )
            }
            
            // Clear form
            IconButton(onClick = onClearForm) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.clear_form)
                )
            }
        }
    )
}

@Composable
private fun AddTransactionContent(
    uiState: AddTransactionUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCategoryClick: () -> Unit,
    onDateClick: () -> Unit,
    onAccountClick: () -> Unit,
    onToggleRecurring: () -> Unit,
    onToggleSplitTransaction: () -> Unit,
    onToggleAdvancedOptions: () -> Unit,
    onAddReceiptClick: () -> Unit,
    onRemoveReceipt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Quick transaction templates if available
            if (uiState.recentTemplates.isNotEmpty()) {
                QuickTemplatesSection(
                    templates = uiState.recentTemplates,
                    onTemplateSelected = { /* Handle template selection */ }
                )
            }
        }
        
        item {
            TransactionFormFields(
                uiState = uiState,
                onAmountChange = onAmountChange,
                onDescriptionChange = onDescriptionChange,
                onTransactionTypeChange = onTransactionTypeChange,
                onCategoryClick = onCategoryClick,
                onDateClick = onDateClick,
                onAccountClick = onAccountClick
            )
        }
        
        item {
            TransactionAdvancedOptions(
                uiState = uiState,
                onToggleRecurring = onToggleRecurring,
                onToggleSplitTransaction = onToggleSplitTransaction,
                onAddReceiptClick = onAddReceiptClick,
                onRemoveReceipt = onRemoveReceipt,
                onToggleAdvancedOptions = onToggleAdvancedOptions
            )
        }
        
        // Smart suggestions based on context
        item {
            if (uiState.smartSuggestions.isNotEmpty()) {
                SmartSuggestionsSection(
                    suggestions = uiState.smartSuggestions,
                    onSuggestionApplied = { /* Handle suggestion */ }
                )
            }
        }
        
        // Add spacing for bottom bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun QuickTemplatesSection(
    templates: List<TransactionTemplate>,
    onTemplateSelected: (TransactionTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Templates",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Template chips would go here
            // This is a placeholder for the template selection UI
        }
    }
}

@Composable
private fun SmartSuggestionsSection(
    suggestions: List<TransactionSuggestion>,
    onSuggestionApplied: (TransactionSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Smart Suggestions",
                style = MaterialTheme.typography.titleMedium
            )
            
            // AI-powered suggestions would be displayed here
            // This is a placeholder for the suggestions UI
        }
    }
}

// Placeholder data classes that would be defined in domain models
data class TransactionTemplate(
    val id: String,
    val name: String,
    val amount: Money,
    val category: Category,
    val description: String
)

data class TransactionSuggestion(
    val id: String,
    val type: String,
    val title: String,
    val description: String
)

enum class ReceiptOption {
    CAMERA, GALLERY
}

// Import statements for the placeholder types would be added here
import code.yousef.dari.shared.domain.models.*