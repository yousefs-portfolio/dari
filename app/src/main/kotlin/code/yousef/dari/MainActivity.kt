package code.yousef.dari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.models.Account
import code.yousef.dari.shared.models.Transaction
import code.yousef.dari.shared.models.TransactionType
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DariTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}

@Composable
fun DariTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF1B5E20),
            secondary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            background = androidx.compose.ui.graphics.Color(0xFFF8F9FA)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Simulate loading data from backend
        coroutineScope.launch {
            try {
                // Mock data for demonstration
                accounts = listOf(
                    Account(
                        id = "acc-1",
                        userId = "user-1",
                        accountNumber = "1234567890",
                        accountType = "CHECKING",
                        bankName = "Al Rajhi Bank",
                        balance = "15000.00",
                        currency = "SAR",
                        createdAt = "2025-09-01T09:00:00"
                    ),
                    Account(
                        id = "acc-2",
                        userId = "user-1",
                        accountNumber = "0987654321",
                        accountType = "SAVINGS",
                        bankName = "Saudi National Bank",
                        balance = "25000.00",
                        currency = "SAR",
                        createdAt = "2025-09-01T09:00:00"
                    )
                )

                transactions = listOf(
                    Transaction(
                        id = "tx-1",
                        accountId = "acc-1",
                        amount = "-850.00",
                        description = "Grocery shopping at Carrefour",
                        category = "Food",
                        transactionDate = "2025-09-05T10:30:00",
                        type = TransactionType.EXPENSE,
                        createdAt = "2025-09-05T10:30:00"
                    ),
                    Transaction(
                        id = "tx-2",
                        accountId = "acc-1",
                        amount = "-1200.00",
                        description = "Fuel at ARAMCO station",
                        category = "Transportation",
                        transactionDate = "2025-09-04T15:45:00",
                        type = TransactionType.EXPENSE,
                        createdAt = "2025-09-04T15:45:00"
                    ),
                    Transaction(
                        id = "tx-3",
                        accountId = "acc-1",
                        amount = "5000.00",
                        description = "Monthly salary",
                        category = "Salary",
                        transactionDate = "2025-09-01T09:00:00",
                        type = TransactionType.INCOME,
                        createdAt = "2025-09-01T09:00:00"
                    )
                )

                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dari - Smart Finance Tracker") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                // Account Summary
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { account ->
                        AccountCard(account = account)
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(transactions) { transaction ->
                        TransactionCard(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = account.bankName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.accountType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Account: ${account.accountNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "${account.balance} ${account.currency}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: "Unknown Transaction",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.category ?: "Uncategorized",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${transaction.amount} SAR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.INCOME) {
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                } else {
                    androidx.compose.ui.graphics.Color(0xFFF44336)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    DariTheme {
        DashboardScreen()
    }
}
