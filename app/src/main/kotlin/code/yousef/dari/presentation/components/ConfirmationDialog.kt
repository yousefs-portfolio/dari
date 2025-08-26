package code.yousef.dari.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class ConfirmationDialogType {
    INFO,
    WARNING,
    DESTRUCTIVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    type: ConfirmationDialogType = ConfirmationDialogType.INFO,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    icon: ImageVector? = null
) {
    val (dialogIcon, iconTint) = when (type) {
        ConfirmationDialogType.INFO -> {
            (icon ?: Icons.Outlined.Info) to MaterialTheme.colorScheme.primary
        }
        ConfirmationDialogType.WARNING -> {
            (icon ?: Icons.Outlined.Warning) to MaterialTheme.colorScheme.tertiary
        }
        ConfirmationDialogType.DESTRUCTIVE -> {
            (icon ?: Icons.Outlined.Delete) to MaterialTheme.colorScheme.error
        }
    }
    
    val confirmButtonColor = when (type) {
        ConfirmationDialogType.DESTRUCTIVE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = dialogIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = dismissText,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = confirmText,
                            color = confirmButtonColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Delete $itemName?",
        message = "This action cannot be undone. Are you sure you want to delete this $itemName?",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        type = ConfirmationDialogType.DESTRUCTIVE,
        confirmText = "Delete",
        dismissText = "Cancel",
        modifier = modifier
    )
}

@Composable
fun DisconnectAccountDialog(
    bankName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConfirmationDialog(
        title = "Disconnect $bankName?",
        message = "This will remove all account data from $bankName. You can reconnect at any time to restore your data.",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        type = ConfirmationDialogType.WARNING,
        confirmText = "Disconnect",
        dismissText = "Cancel",
        modifier = modifier
    )
}

@Preview
@Composable
private fun ConfirmationDialogInfoPreview() {
    var showDialog by remember { mutableStateOf(true) }
    
    MaterialTheme {
        if (showDialog) {
            ConfirmationDialog(
                title = "Save Changes",
                message = "Do you want to save your changes before leaving?",
                onConfirm = { showDialog = false },
                onDismiss = { showDialog = false },
                type = ConfirmationDialogType.INFO
            )
        }
    }
}

@Preview
@Composable
private fun ConfirmationDialogWarningPreview() {
    var showDialog by remember { mutableStateOf(true) }
    
    MaterialTheme {
        if (showDialog) {
            ConfirmationDialog(
                title = "Budget Limit Exceeded",
                message = "This transaction will put you over your monthly budget. Do you want to proceed?",
                onConfirm = { showDialog = false },
                onDismiss = { showDialog = false },
                type = ConfirmationDialogType.WARNING,
                confirmText = "Proceed"
            )
        }
    }
}

@Preview
@Composable
private fun ConfirmationDialogDestructivePreview() {
    var showDialog by remember { mutableStateOf(true) }
    
    MaterialTheme {
        if (showDialog) {
            DeleteConfirmationDialog(
                itemName = "transaction",
                onConfirm = { showDialog = false },
                onDismiss = { showDialog = false }
            )
        }
    }
}