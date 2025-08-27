package code.yousef.dari.shared.ui.components.financial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.domain.models.FinancialAccount
import code.yousef.dari.shared.domain.models.Money
import code.yousef.dari.shared.ui.theme.DariTheme
import kotlinx.datetime.LocalDateTime

/**
 * Account card component showing account information and balance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCard(
    account: FinancialAccount,
    onAccountClick: (FinancialAccount) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showLastUpdated: Boolean = true,
    isCompact: Boolean = false
) {
    Card(
        onClick = { onAccountClick(account) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                ).brush
            )
        } else null
    ) {
        if (isCompact) {
            CompactAccountContent(
                account = account,
                isSelected = isSelected,
                showLastUpdated = showLastUpdated
            )
        } else {
            FullAccountContent(
                account = account,
                isSelected = isSelected,
                showLastUpdated = showLastUpdated
            )
        }
    }
}

@Composable
private fun FullAccountContent(
    account: FinancialAccount,
    isSelected: Boolean,
    showLastUpdated: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with bank logo and account name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                BankLogo(
                    bankName = account.institutionName,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = account.institutionName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Account status indicator
            AccountStatusIndicator(
                isConnected = account.isActive,
                syncInProgress = false // TODO: Add sync status
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Baseline
        ) {
            Column {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = formatMoney(account.balance),
                    style = DariTheme.financialTextStyles.CurrencyMedium,
                    color = getBalanceColor(account.balance),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Account type badge
            AccountTypeBadge(type = account.type)
        }
        
        // Last updated time
        if (showLastUpdated) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Updated ${formatLastUpdated(account.lastSync)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                }
            )
        }
    }
}

@Composable
private fun CompactAccountContent(
    account: FinancialAccount,
    isSelected: Boolean,
    showLastUpdated: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BankLogo(
            bankName = account.institutionName,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatMoney(account.balance),
                style = MaterialTheme.typography.bodySmall,
                color = getBalanceColor(account.balance),
                fontWeight = FontWeight.Medium
            )
        }
        
        AccountStatusIndicator(
            isConnected = account.isActive,
            syncInProgress = false,
            size = 16.dp
        )
    }
}

/**
 * Bank logo placeholder
 */
@Composable
private fun BankLogo(
    bankName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = getBankGradient(bankName),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getBankIcon(bankName),
            contentDescription = bankName,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Account status indicator
 */
@Composable
private fun AccountStatusIndicator(
    isConnected: Boolean,
    syncInProgress: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (syncInProgress) 360f else 0f,
        label = "sync_rotation"
    )
    
    Icon(
        imageVector = when {
            syncInProgress -> Icons.Filled.Sync
            isConnected -> Icons.Filled.CheckCircle
            else -> Icons.Filled.Error
        },
        contentDescription = when {
            syncInProgress -> "Syncing"
            isConnected -> "Connected"
            else -> "Disconnected"
        },
        tint = when {
            syncInProgress -> MaterialTheme.colorScheme.tertiary
            isConnected -> DariTheme.financialColors.Income
            else -> MaterialTheme.colorScheme.error
        },
        modifier = modifier
            .size(size)
            .rotate(rotationAngle)
    )
}

/**
 * Account type badge
 */
@Composable
private fun AccountTypeBadge(
    type: FinancialAccount.AccountType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Get bank icon based on name
 */
private fun getBankIcon(bankName: String): ImageVector {
    return when (bankName.lowercase()) {
        "al rajhi bank", "alrajhi" -> Icons.Filled.AccountBalance
        "saudi national bank", "snb" -> Icons.Filled.AccountBalance
        "riyad bank" -> Icons.Filled.AccountBalance
        "sabb" -> Icons.Filled.AccountBalance
        "alinma bank" -> Icons.Filled.AccountBalance
        "bank albilad" -> Icons.Filled.AccountBalance
        "stc pay" -> Icons.Filled.Payment
        else -> Icons.Filled.AccountBalance
    }
}

/**
 * Get bank gradient colors
 */
@Composable
private fun getBankGradient(bankName: String): Brush {
    return when (bankName.lowercase()) {
        "al rajhi bank", "alrajhi" -> Brush.linearGradient(
            listOf(Color(0xFF1B5E20), Color(0xFF4CAF50))
        )
        "saudi national bank", "snb" -> Brush.linearGradient(
            listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
        )
        "riyad bank" -> Brush.linearGradient(
            listOf(Color(0xFF7B1FA2), Color(0xFFBA68C8))
        )
        "sabb" -> Brush.linearGradient(
            listOf(Color(0xFFE65100), Color(0xFFFF9800))
        )
        "alinma bank" -> Brush.linearGradient(
            listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
        )
        "bank albilad" -> Brush.linearGradient(
            listOf(Color(0xFF1976D2), Color(0xFF2196F3))
        )
        "stc pay" -> Brush.linearGradient(
            listOf(Color(0xFF4A148C), Color(0xFF9C27B0))
        )
        else -> Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        )
    }
}

/**
 * Get balance color
 */
@Composable
private fun getBalanceColor(balance: Money): Color {
    return when {
        balance.amount > 0 -> DariTheme.financialColors.Income
        balance.amount < 0 -> DariTheme.financialColors.Expense
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Format money for display
 */
private fun formatMoney(money: Money): String {
    return "${money.currency} ${String.format("%.2f", kotlin.math.abs(money.amount))}"
}

/**
 * Format last updated time
 */
private fun formatLastUpdated(lastSync: LocalDateTime?): String {
    if (lastSync == null) return "Never"
    
    // TODO: Implement proper relative time formatting
    return "just now"
}

/**
 * Extension property for account type display name
 */
private val FinancialAccount.AccountType.displayName: String
    get() = when (this) {
        FinancialAccount.AccountType.CHECKING -> "Checking"
        FinancialAccount.AccountType.SAVINGS -> "Savings"
        FinancialAccount.AccountType.CREDIT -> "Credit"
        FinancialAccount.AccountType.INVESTMENT -> "Investment"
        FinancialAccount.AccountType.LOAN -> "Loan"
        FinancialAccount.AccountType.OTHER -> "Other"
    }