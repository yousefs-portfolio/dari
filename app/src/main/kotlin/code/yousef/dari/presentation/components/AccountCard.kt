package code.yousef.dari.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class AccountCardData(
    val id: String,
    val accountNumber: String,
    val accountType: String,
    val bankName: String,
    val balance: Double,
    val currency: String = "SAR",
    val isConnected: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Composable
fun AccountCard(
    account: AccountCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBalance: Boolean = true
) {
    var isBalanceVisible by remember { mutableStateOf(showBalance) }
    
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(gradientColors)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = account.accountType,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                AccountTypeIcon(
                    accountType = account.accountType,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBalanceVisible) {
                            formatCurrency(account.balance, account.currency)
                        } else {
                            "••••••"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "•••• ${account.accountNumber.takeLast(4)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(
                    onClick = { isBalanceVisible = !isBalanceVisible }
                ) {
                    Icon(
                        imageVector = if (isBalanceVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (!account.isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "Connection Lost - Tap to Reconnect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactAccountCard(
    account: AccountCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                AccountTypeIcon(
                    accountType = account.accountType,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${account.bankName} ${account.accountType}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "•••• ${account.accountNumber.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatCurrency(account.balance, account.currency),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AccountTypeIcon(
    accountType: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val icon: ImageVector = when (accountType.lowercase()) {
        "savings", "saving" -> Icons.Default.Savings
        "credit", "credit card" -> Icons.Default.CreditCard
        else -> Icons.Default.CreditCard
    }
    
    Icon(
        imageVector = icon,
        contentDescription = accountType,
        modifier = modifier,
        tint = tint
    )
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val currency = Currency.getInstance(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = currency
        formatter.format(amount)
    } catch (e: Exception) {
        "$currencyCode ${String.format("%.2f", amount)}"
    }
}

@Preview
@Composable
private fun AccountCardPreview() {
    val sampleAccount = AccountCardData(
        id = "1",
        accountNumber = "1234567890",
        accountType = "Checking Account",
        bankName = "Al Rajhi Bank",
        balance = 15420.50,
        currency = "SAR"
    )
    
    MaterialTheme {
        AccountCard(
            account = sampleAccount,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun CompactAccountCardPreview() {
    val sampleAccount = AccountCardData(
        id = "1",
        accountNumber = "1234567890",
        accountType = "Savings",
        bankName = "SNB",
        balance = 25300.00,
        currency = "SAR"
    )
    
    MaterialTheme {
        CompactAccountCard(
            account = sampleAccount,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun DisconnectedAccountCardPreview() {
    val sampleAccount = AccountCardData(
        id = "1",
        accountNumber = "1234567890",
        accountType = "Credit Card",
        bankName = "Riyad Bank",
        balance = -2500.00,
        currency = "SAR",
        isConnected = false
    )
    
    MaterialTheme {
        AccountCard(
            account = sampleAccount,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}