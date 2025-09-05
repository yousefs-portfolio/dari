package code.yousef.dari.backend.database

import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {
    private var connection: Connection? = null

    fun init() {
        try {
            // Initialize H2 in-memory database
            Class.forName("org.h2.Driver")
            connection = DriverManager.getConnection(
                "jdbc:h2:mem:dari_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "sa",
                ""
            )

            // Create tables
            createTables()

            // Insert sample data
            insertSampleData()

            println("Database initialized successfully")
        } catch (e: Exception) {
            println("Error initializing database: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getConnection(): Connection {
        return connection ?: throw IllegalStateException("Database not initialized")
    }

    private fun createTables() {
        val statement = getConnection().createStatement()

        // Users table
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(36) PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                name VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """
        )

        // Accounts table
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                account_number VARCHAR(50) NOT NULL,
                account_type VARCHAR(50) NOT NULL,
                bank_name VARCHAR(255) NOT NULL,
                balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                currency VARCHAR(3) NOT NULL DEFAULT 'SAR',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """
        )

        // Transactions table
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id VARCHAR(36) PRIMARY KEY,
                account_id VARCHAR(36) NOT NULL,
                amount DECIMAL(15,2) NOT NULL,
                description VARCHAR(500),
                category VARCHAR(100),
                transaction_date TIMESTAMP NOT NULL,
                type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE, TRANSFER
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (account_id) REFERENCES accounts(id)
            )
        """
        )

        // Budgets table
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS budgets (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                category VARCHAR(100) NOT NULL,
                amount DECIMAL(15,2) NOT NULL,
                spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                period VARCHAR(20) NOT NULL, -- MONTHLY, WEEKLY, YEARLY
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """
        )

        // Goals table
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                title VARCHAR(255) NOT NULL,
                target_amount DECIMAL(15,2) NOT NULL,
                current_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                target_date DATE,
                category VARCHAR(100),
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, PAUSED
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """
        )

        statement.close()
    }

    private fun insertSampleData() {
        val statement = getConnection().createStatement()

        // Sample user
        statement.execute(
            """
            INSERT INTO users (id, email, name) VALUES
            ('user-1', 'demo@dari.sa', 'Demo User')
        """
        )

        // Sample accounts
        statement.execute(
            """
            INSERT INTO accounts (id, user_id, account_number, account_type, bank_name, balance) VALUES
            ('acc-1', 'user-1', '1234567890', 'CHECKING', 'Al Rajhi Bank', 15000.00),
            ('acc-2', 'user-1', '0987654321', 'SAVINGS', 'Saudi National Bank', 25000.00)
        """
        )

        // Sample transactions
        statement.execute(
            """
            INSERT INTO transactions (id, account_id, amount, description, category, transaction_date, type) VALUES
            ('tx-1', 'acc-1', -850.00, 'Grocery shopping at Carrefour', 'Food', '2025-09-05 10:30:00', 'EXPENSE'),
            ('tx-2', 'acc-1', -1200.00, 'Fuel at ARAMCO station', 'Transportation', '2025-09-04 15:45:00', 'EXPENSE'),
            ('tx-3', 'acc-1', 5000.00, 'Monthly salary', 'Salary', '2025-09-01 09:00:00', 'INCOME'),
            ('tx-4', 'acc-2', 2000.00, 'Investment returns', 'Investment', '2025-09-03 12:00:00', 'INCOME')
        """
        )

        // Sample budgets
        statement.execute(
            """
            INSERT INTO budgets (id, user_id, category, amount, spent, period, start_date, end_date) VALUES
            ('budget-1', 'user-1', 'Food', 2000.00, 850.00, 'MONTHLY', '2025-09-01', '2025-09-30'),
            ('budget-2', 'user-1', 'Transportation', 1500.00, 1200.00, 'MONTHLY', '2025-09-01', '2025-09-30')
        """
        )

        // Sample goals
        statement.execute(
            """
            INSERT INTO goals (id, user_id, title, target_amount, current_amount, target_date, category) VALUES
            ('goal-1', 'user-1', 'Emergency Fund', 50000.00, 25000.00, '2025-12-31', 'Savings'),
            ('goal-2', 'user-1', 'Vacation to Maldives', 15000.00, 5000.00, '2025-06-30', 'Travel')
        """
        )

        statement.close()
    }
}
