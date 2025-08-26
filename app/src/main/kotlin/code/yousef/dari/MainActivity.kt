package code.yousef.dari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.ui.theme.DariTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            DariTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DariApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DariApp() {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dari - Smart Finance",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { 
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { 
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            Screen.DASHBOARD -> DashboardScreen(
                modifier = Modifier.padding(paddingValues)
            )
            Screen.ACCOUNTS -> AccountsScreen(
                modifier = Modifier.padding(paddingValues)
            )
            Screen.TRANSACTIONS -> TransactionsScreen(
                modifier = Modifier.padding(paddingValues)
            )
            Screen.BUDGET -> BudgetScreen(
                modifier = Modifier.padding(paddingValues)
            )
            Screen.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

enum class Screen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    ACCOUNTS("Accounts", Icons.Filled.AccountBalance),
    TRANSACTIONS("Transactions", Icons.Filled.Receipt),
    BUDGET("Budget", Icons.Filled.PieChart),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = "Bank",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to Dari",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your Smart Finance Tracker",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { /* TODO: Implement bank connection */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Your Bank")
                }
            }
        }
    }
}

@Composable
fun AccountsScreen(modifier: Modifier = Modifier) {
    CenteredMessage(
        modifier = modifier,
        icon = Icons.Filled.AccountBalance,
        title = "Accounts",
        message = "Your connected bank accounts will appear here"
    )
}

@Composable
fun TransactionsScreen(modifier: Modifier = Modifier) {
    CenteredMessage(
        modifier = modifier,
        icon = Icons.Filled.Receipt,
        title = "Transactions",
        message = "Your transaction history will appear here"
    )
}

@Composable
fun BudgetScreen(modifier: Modifier = Modifier) {
    CenteredMessage(
        modifier = modifier,
        icon = Icons.Filled.PieChart,
        title = "Budget",
        message = "Your budget tracking will appear here"
    )
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    CenteredMessage(
        modifier = modifier,
        icon = Icons.Filled.Settings,
        title = "Settings",
        message = "App settings and preferences"
    )
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}