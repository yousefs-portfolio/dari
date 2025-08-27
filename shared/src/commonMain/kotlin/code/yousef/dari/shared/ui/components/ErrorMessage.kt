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
 * Error message component with retry functionality
 */
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Error,
    onRetry: (() -> Unit)? = null,
    retryText: String = "Retry"
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                FilledTonalButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(retryText)
                }
            }
        }
    }
}

/**
 * Inline error message for form fields
 */
@Composable
fun InlineErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = "Error",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Network error message with specific handling
 */
@Composable
fun NetworkErrorMessage(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    ErrorMessage(
        message = "Unable to connect to the internet. Please check your connection and try again.",
        icon = Icons.Filled.SignalWifiOff,
        onRetry = onRetry,
        retryText = "Try Again",
        modifier = modifier
    )
}

/**
 * Empty state with error styling
 */
@Composable
fun ErrorState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.ErrorOutline,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (primaryAction != null || secondaryAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                secondaryAction?.let { (text, action) ->
                    OutlinedButton(onClick = action) {
                        Text(text)
                    }
                }
                
                primaryAction?.let { (text, action) ->
                    FilledButton(onClick = action) {
                        Text(text)
                    }
                }
            }
        }
    }
}

/**
 * Banking-specific error messages
 */
@Composable
fun BankConnectionError(
    bankName: String,
    onRetry: () -> Unit,
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorState(
        title = "Connection Failed",
        description = "Unable to connect to $bankName. This could be due to maintenance or temporary issues with the bank's servers.",
        icon = Icons.Filled.AccountBalance,
        primaryAction = "Retry Connection" to onRetry,
        secondaryAction = "Contact Support" to onContactSupport,
        modifier = modifier
    )
}

/**
 * Authentication error message
 */
@Composable
fun AuthenticationError(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorState(
        title = "Authentication Required",
        description = "Your session has expired. Please log in again to continue.",
        icon = Icons.Filled.Lock,
        primaryAction = "Log In" to onLogin,
        modifier = modifier
    )
}

/**
 * Permission error message
 */
@Composable
fun PermissionError(
    permission: String,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorState(
        title = "Permission Required",
        description = "This feature requires $permission permission to work properly.",
        icon = Icons.Filled.Security,
        primaryAction = "Grant Permission" to onGrantPermission,
        modifier = modifier
    )
}