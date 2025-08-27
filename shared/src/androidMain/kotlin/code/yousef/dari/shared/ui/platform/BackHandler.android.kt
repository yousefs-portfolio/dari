package code.yousef.dari.shared.ui.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.navigation.NavController
import kotlinx.coroutines.delay

/**
 * Android-specific back button handling with financial app considerations
 */
@Composable
actual fun HandleBackPress(
    enabled: Boolean,
    onBackPressed: () -> Unit
) {
    BackHandler(enabled = enabled) {
        onBackPressed()
    }
}

/**
 * Financial app specific back handling with confirmation for critical screens
 */
@Composable
fun FinancialBackHandler(
    isTransactionInProgress: Boolean = false,
    hasUnsavedChanges: Boolean = false,
    onBackPressed: () -> Unit,
    onShowExitConfirmation: () -> Unit = {}
) {
    BackHandler(enabled = true) {
        when {
            isTransactionInProgress -> {
                // Show confirmation for in-progress transactions
                onShowExitConfirmation()
            }
            hasUnsavedChanges -> {
                // Show confirmation for unsaved changes
                onShowExitConfirmation()
            }
            else -> {
                onBackPressed()
            }
        }
    }
}

/**
 * Double back press to exit handler for main screens
 */
@Composable
fun DoubleBackPressHandler(
    onShowExitMessage: () -> Unit,
    onExit: () -> Unit
) {
    var backPressedOnce by remember { mutableStateOf(false) }
    
    BackHandler(enabled = true) {
        if (backPressedOnce) {
            onExit()
        } else {
            backPressedOnce = true
            onShowExitMessage()
            
            LaunchedEffect(backPressedOnce) {
                delay(2000) // Reset after 2 seconds
                backPressedOnce = false
            }
        }
    }
}

/**
 * Navigation back handler with breadcrumb support
 */
@Composable
fun NavigationBackHandler(
    navController: NavController,
    canNavigateBack: Boolean = true,
    onBackPressed: (() -> Unit)? = null
) {
    BackHandler(enabled = canNavigateBack) {
        if (onBackPressed != null) {
            onBackPressed()
        } else if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
}

/**
 * Modal back handler for sheets and dialogs
 */
@Composable
fun ModalBackHandler(
    isModalVisible: Boolean,
    onDismissModal: () -> Unit
) {
    BackHandler(enabled = isModalVisible) {
        onDismissModal()
    }
}

/**
 * Transaction form back handler with data loss protection
 */
@Composable
fun TransactionFormBackHandler(
    hasFormData: Boolean,
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onShowSaveDialog: () -> Unit
) {
    BackHandler(enabled = true) {
        if (hasFormData) {
            onShowSaveDialog()
        } else {
            onDiscardAndExit()
        }
    }
}

/**
 * Banking session back handler with security considerations
 */
@Composable
fun BankingSessionBackHandler(
    isInSecureSession: Boolean,
    onSecureExit: () -> Unit,
    onNormalBack: () -> Unit
) {
    BackHandler(enabled = true) {
        if (isInSecureSession) {
            // Clear sensitive data and navigate securely
            onSecureExit()
        } else {
            onNormalBack()
        }
    }
}

/**
 * Camera screen back handler for receipt scanning
 */
@Composable
fun CameraBackHandler(
    isCameraActive: Boolean,
    hasCapture: Boolean,
    onSaveCapture: () -> Unit,
    onDiscardCapture: () -> Unit,
    onCloseCamera: () -> Unit
) {
    BackHandler(enabled = isCameraActive) {
        when {
            hasCapture -> {
                // Show options to save or discard capture
                onSaveCapture()
            }
            else -> {
                onCloseCamera()
            }
        }
    }
}

/**
 * Goal progress back handler to prevent accidental navigation
 */
@Composable
fun GoalProgressBackHandler(
    isGoalCompleted: Boolean,
    onCelebrationComplete: () -> Unit,
    onNormalBack: () -> Unit
) {
    BackHandler(enabled = true) {
        if (isGoalCompleted) {
            // Allow user to complete celebration before navigating back
            onCelebrationComplete()
        } else {
            onNormalBack()
        }
    }
}

/**
 * Settings screen back handler with apply/discard logic
 */
@Composable
fun SettingsBackHandler(
    hasChanges: Boolean,
    onApplyChanges: () -> Unit,
    onDiscardChanges: () -> Unit,
    onShowConfirmation: () -> Unit
) {
    BackHandler(enabled = true) {
        if (hasChanges) {
            onShowConfirmation()
        } else {
            onDiscardChanges()
        }
    }
}

/**
 * Budget creation back handler with draft saving
 */
@Composable
fun BudgetCreationBackHandler(
    budgetData: Any?, // Replace with actual budget data type
    onSaveDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    onShowDraftDialog: () -> Unit
) {
    BackHandler(enabled = true) {
        if (budgetData != null) {
            onShowDraftDialog()
        } else {
            onDiscardDraft()
        }
    }
}

/**
 * Analytics screen back handler
 */
@Composable
fun AnalyticsBackHandler(
    isExporting: Boolean,
    onCancelExport: () -> Unit,
    onNormalBack: () -> Unit
) {
    BackHandler(enabled = true) {
        if (isExporting) {
            onCancelExport()
        } else {
            onNormalBack()
        }
    }
}

/**
 * Biometric authentication back handler
 */
@Composable
fun BiometricBackHandler(
    isBiometricActive: Boolean,
    onCancelBiometric: () -> Unit,
    onNormalBack: () -> Unit
) {
    BackHandler(enabled = true) {
        if (isBiometricActive) {
            onCancelBiometric()
        } else {
            onNormalBack()
        }
    }
}

/**
 * Account connection back handler for OAuth flows
 */
@Composable
fun AccountConnectionBackHandler(
    isConnecting: Boolean,
    connectionProgress: Float,
    onCancelConnection: () -> Unit,
    onNormalBack: () -> Unit
) {
    BackHandler(enabled = true) {
        if (isConnecting && connectionProgress < 1f) {
            onCancelConnection()
        } else {
            onNormalBack()
        }
    }
}

/**
 * Multi-step form back handler
 */
@Composable
fun MultiStepFormBackHandler(
    currentStep: Int,
    totalSteps: Int,
    onPreviousStep: () -> Unit,
    onExitForm: () -> Unit
) {
    BackHandler(enabled = true) {
        if (currentStep > 0) {
            onPreviousStep()
        } else {
            onExitForm()
        }
    }
}