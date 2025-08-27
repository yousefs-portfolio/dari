package code.yousef.dari.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Generic confirmation dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    showIcon: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = if (showIcon) {
            {
                Icon(
                    imageVector = icon ?: if (isDestructive) Icons.Filled.Warning else Icons.Filled.Info,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        } else null,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Delete confirmation dialog
 */
@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    itemType: String = "item"
) {
    ConfirmationDialog(
        title = "Delete $itemType?",
        message = "Are you sure you want to delete \"$itemName\"? This action cannot be undone.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Delete",
        dismissText = "Cancel",
        icon = Icons.Filled.Delete,
        isDestructive = true,
        modifier = modifier
    )
}

/**
 * Bank account disconnect confirmation
 */
@Composable
fun DisconnectBankDialog(
    bankName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Disconnect Bank Account?",
        message = "Are you sure you want to disconnect your $bankName account? You'll no longer receive automatic transaction updates.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Disconnect",
        dismissText = "Keep Connected",
        icon = Icons.Filled.AccountBalance,
        isDestructive = true,
        modifier = modifier
    )
}

/**
 * Transaction deletion confirmation with amount
 */
@Composable
fun DeleteTransactionDialog(
    amount: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Delete Transaction?",
        message = "Are you sure you want to delete this transaction?\n\n$amount - $description\n\nThis action cannot be undone.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Delete",
        dismissText = "Cancel",
        icon = Icons.Filled.Delete,
        isDestructive = true,
        modifier = modifier
    )
}

/**
 * Budget reset confirmation
 */
@Composable
fun ResetBudgetDialog(
    budgetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Reset Budget?",
        message = "Are you sure you want to reset the \"$budgetName\" budget? All current spending data will be cleared and the budget will restart.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Reset",
        dismissText = "Cancel",
        icon = Icons.Filled.RestartAlt,
        isDestructive = true,
        modifier = modifier
    )
}

/**
 * Goal completion confirmation
 */
@Composable
fun CompleteGoalDialog(
    goalName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Complete Goal?",
        message = "Congratulations! Are you sure you want to mark \"$goalName\" as completed? This will move it to your completed goals.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Complete Goal",
        dismissText = "Not Yet",
        icon = Icons.Filled.CheckCircle,
        isDestructive = false,
        modifier = modifier
    )
}

/**
 * Data export confirmation
 */
@Composable
fun ExportDataDialog(
    dataType: String,
    format: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Export Data?",
        message = "Export your $dataType as a $format file? This may take a few moments depending on the amount of data.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Export",
        dismissText = "Cancel",
        icon = Icons.Filled.Download,
        isDestructive = false,
        modifier = modifier
    )
}

/**
 * Logout confirmation
 */
@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Sign Out?",
        message = "Are you sure you want to sign out? You'll need to sign back in to access your financial data.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Sign Out",
        dismissText = "Cancel",
        icon = Icons.Filled.Logout,
        isDestructive = false,
        modifier = modifier
    )
}

/**
 * Clear all data confirmation
 */
@Composable
fun ClearAllDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Clear All Data?",
        message = "This will permanently delete all your financial data including transactions, budgets, and goals. This action cannot be undone.\n\nAre you absolutely sure?",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Clear All Data",
        dismissText = "Keep Data",
        icon = Icons.Filled.Warning,
        isDestructive = true,
        modifier = modifier
    )
}

/**
 * Permission request dialog
 */
@Composable
fun PermissionDialog(
    permissionName: String,
    rationale: String,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Permission Required",
        message = "This app needs $permissionName permission to $rationale. Please grant this permission to continue.",
        onConfirm = onGrantPermission,
        onDismiss = onDismiss,
        confirmText = "Grant Permission",
        dismissText = "Not Now",
        icon = Icons.Filled.Security,
        isDestructive = false,
        modifier = modifier
    )
}

/**
 * Update available dialog
 */
@Composable
fun UpdateAvailableDialog(
    version: String,
    features: List<String>,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    AlertDialog(
        onDismissRequest = if (isRequired) { {} } else onSkip,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = "Update Available",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (isRequired) "Required Update" else "Update Available",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Version $version is now available with the following improvements:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (isRequired) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This update is required to continue using the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text("Update Now")
            }
        },
        dismissButton = if (!isRequired) {
            {
                TextButton(onClick = onSkip) {
                    Text("Later")
                }
            }
        } else null
    )
}