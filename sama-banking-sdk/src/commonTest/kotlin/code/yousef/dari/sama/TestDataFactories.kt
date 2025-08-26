package code.yousef.dari.sama

import code.yousef.dari.sama.models.*
import kotlin.random.Random

/**
 * Test Data Factories for SAMA Banking SDK
 * Provides realistic test data for all banking models and use cases
 */
object TestDataFactories {

    /**
     * Account Test Data Factory
     */
    object AccountFactory {
        
        private val accountTypes = listOf("CURRENT", "SAVINGS", "INVESTMENT", "BUSINESS", "DIGITAL_WALLET")
        private val saudiNames = listOf(
            "Ahmad Mohammed", "Fatima Ali", "Abdullah Rahman", "Norah Hassan",
            "Khalid Ibrahim", "Sarah Ahmed", "Omar Mansour", "Aisha Khalil",
            "Yousef Abdulaziz", "Maryam Sultan", "Faisal Alharbi", "Lina Alotaibi"
        )
        
        fun createAccount(
            bankCode: String = "alrajhi",
            accountType: String = accountTypes.random(),
            isActive: Boolean = true
        ): Account {
            val accountNumber = generateAccountNumber(bankCode)
            return Account(
                accountId = "acc-${Random.nextLong(1000, 9999)}",
                accountNumber = accountNumber,
                accountName = "$accountType Account",
                accountType = accountType,
                currency = "SAR",
                isActive = isActive,
                openingDate = "2023-${Random.nextInt(1, 12).toString().padStart(2, '0')}-${Random.nextInt(1, 28).toString().padStart(2, '0')}T00:00:00Z",
                iban = generateIBAN(bankCode, accountNumber),
                accountHolderName = saudiNames.random(),
                branchCode = Random.nextInt(1, 999).toString().padStart(3, '0'),
                productName = "${accountType.lowercase().replaceFirstChar { it.uppercase() }} Account",
                interestRate = if (accountType == "SAVINGS" || accountType == "INVESTMENT") 
                    "${Random.nextDouble(0.1, 2.5)}" else null
            )
        }
        
        fun createSaudiBankAccounts(count: Int = 5): List<Account> {
            val bankCodes = listOf("alrajhi", "snb", "riyadbank", "sabb", "alinma", "albilad")
            return (1..count).map { 
                createAccount(bankCode = bankCodes.random())
            }
        }
        
        fun createTestAccountsForBank(bankCode: String, count: Int = 3): List<Account> {
            return (1..count).map { 
                createAccount(bankCode = bankCode, accountType = accountTypes.random())
            }
        }
        
        private fun generateAccountNumber(bankCode: String): String {
            return when (bankCode) {
                "alrajhi" -> "20${Random.nextLong(100000000000000, 999999999999999)}"
                "snb" -> "03${Random.nextLong(100000000000000, 999999999999999)}"  
                "riyadbank" -> "15${Random.nextLong(100000000000000, 999999999999999)}"
                "sabb" -> "45${Random.nextLong(100000000000000, 999999999999999)}"
                "alinma" -> "58${Random.nextLong(100000000000000, 999999999999999)}"
                "albilad" -> "73${Random.nextLong(100000000000000, 999999999999999)}"
                "stcpay" -> "STCPAY${Random.nextLong(100000000000000, 999999999999999)}"
                else -> Random.nextLong(100000000000000000, 999999999999999999).toString()
            }
        }
        
        private fun generateIBAN(bankCode: String, accountNumber: String): String {
            val bankCode2Digit = when (bankCode) {
                "alrajhi" -> "20"
                "snb" -> "03"
                "riyadbank" -> "15" 
                "sabb" -> "45"
                "alinma" -> "58"
                "albilad" -> "73"
                else -> "20"
            }
            return "SA$bankCode2Digit${bankCode2Digit}000${accountNumber}"
        }
    }

    /**
     * Transaction Test Data Factory
     */
    object TransactionFactory {
        
        private val transactionTypes = listOf(
            "ATM Withdrawal", "POS Purchase", "Online Transfer", "Salary Transfer",
            "Bill Payment", "Cash Deposit", "Cheque Deposit", "Standing Order",
            "Direct Debit", "Investment Purchase", "Loan Payment", "Card Payment"
        )
        
        private val merchants = listOf(
            "Samba ATM", "NCB ATM", "Carrefour", "Extra", "Panda", "Tamimi Markets",
            "McDonald's", "Starbucks", "Shell", "ADNOC", "STC", "Mobily", "Zain",
            "Saudi Electricity Company", "National Water Company", "Saudi Aramco",
            "Al Rajhi Bank", "SABB", "Riyad Bank", "Tech Solutions LLC", "Consulting Co"
        )
        
        private val merchantCategoryCodes = mapOf(
            "ATM" to "6011",
            "Grocery" to "5411",
            "Restaurant" to "5812", 
            "Gas Station" to "5541",
            "Telecom" to "4814",
            "Utilities" to "4900",
            "Bank" to "6010",
            "Business Services" to "7372"
        )
        
        fun createTransaction(
            accountId: String = "acc-001",
            creditDebitIndicator: String = listOf("Credit", "Debit").random(),
            status: String = "Booked"
        ): Transaction {
            val transactionInfo = transactionTypes.random()
            val merchantName = merchants.random()
            val isCredit = creditDebitIndicator == "Credit"
            
            return Transaction(
                transactionId = "txn-${Random.nextLong(1000, 99999)}",
                accountId = accountId,
                amount = Amount(
                    amount = generateTransactionAmount(isCredit),
                    currency = "SAR"
                ),
                creditDebitIndicator = creditDebitIndicator,
                status = status,
                bookingDateTime = generateRecentDateTime(),
                valueDateTime = generateRecentDateTime(),
                transactionInformation = transactionInfo,
                merchantDetails = MerchantDetails(
                    merchantName = merchantName,
                    merchantCategoryCode = getMerchantCategoryCode(merchantName)
                ),
                balance = generateBalanceAfterTransaction(accountId, isCredit)
            )
        }
        
        fun createTransactionHistory(
            accountId: String = "acc-001",
            count: Int = 10,
            daysBack: Int = 30
        ): List<Transaction> {
            return (1..count).map { 
                createTransaction(
                    accountId = accountId,
                    creditDebitIndicator = if (Random.nextDouble() > 0.7) "Credit" else "Debit"
                )
            }.sortedByDescending { it.bookingDateTime }
        }
        
        fun createSalaryTransaction(accountId: String, amount: String = "5000.00"): Transaction {
            return Transaction(
                transactionId = "txn-salary-${Random.nextLong(1000, 9999)}",
                accountId = accountId,
                amount = Amount(amount = amount, currency = "SAR"),
                creditDebitIndicator = "Credit",
                status = "Booked",
                bookingDateTime = "${generateRecentDate()}T09:${Random.nextInt(10, 30)}:00Z",
                valueDateTime = "${generateRecentDate()}T09:${Random.nextInt(10, 30)}:00Z",
                transactionInformation = "Salary Transfer",
                merchantDetails = MerchantDetails(
                    merchantName = "Tech Solutions LLC",
                    merchantCategoryCode = "7372"
                ),
                balance = generateBalanceAfterTransaction(accountId, true)
            )
        }
        
        fun createBillPaymentTransaction(accountId: String, amount: String = "150.00"): Transaction {
            return Transaction(
                transactionId = "txn-bill-${Random.nextLong(1000, 9999)}",
                accountId = accountId,
                amount = Amount(amount = amount, currency = "SAR"),
                creditDebitIndicator = "Debit",
                status = "Booked", 
                bookingDateTime = generateRecentDateTime(),
                valueDateTime = generateRecentDateTime(),
                transactionInformation = "Bill Payment",
                merchantDetails = MerchantDetails(
                    merchantName = "Saudi Electricity Company",
                    merchantCategoryCode = "4900"
                ),
                balance = generateBalanceAfterTransaction(accountId, false)
            )
        }
        
        private fun generateTransactionAmount(isCredit: Boolean): String {
            return if (isCredit) {
                // Credits tend to be larger (salary, transfers)
                "${Random.nextDouble(100.0, 10000.0).toBigDecimal().setScale(2)}"
            } else {
                // Debits tend to be smaller (purchases, bills)
                "${Random.nextDouble(10.0, 1000.0).toBigDecimal().setScale(2)}"
            }
        }
        
        private fun generateRecentDateTime(): String {
            val daysBack = Random.nextInt(0, 30)
            return "${generateDateDaysBack(daysBack)}T${Random.nextInt(8, 22)}:${Random.nextInt(0, 59)}:${Random.nextInt(0, 59)}Z"
        }
        
        private fun generateRecentDate(): String {
            return generateDateDaysBack(Random.nextInt(0, 7))
        }
        
        private fun generateDateDaysBack(days: Int): String {
            // Simple date calculation for testing
            val baseDay = 26 // August 26
            val day = if (baseDay - days > 0) baseDay - days else baseDay - days + 30
            val month = if (baseDay - days > 0) 8 else 7
            return "2024-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        }
        
        private fun getMerchantCategoryCode(merchantName: String): String {
            return when {
                merchantName.contains("ATM") -> merchantCategoryCodes["ATM"]!!
                merchantName.contains("Carrefour") || merchantName.contains("Panda") -> merchantCategoryCodes["Grocery"]!!
                merchantName.contains("McDonald") || merchantName.contains("Starbucks") -> merchantCategoryCodes["Restaurant"]!!
                merchantName.contains("Shell") || merchantName.contains("ADNOC") -> merchantCategoryCodes["Gas Station"]!!
                merchantName.contains("STC") || merchantName.contains("Mobily") -> merchantCategoryCodes["Telecom"]!!
                merchantName.contains("Electricity") || merchantName.contains("Water") -> merchantCategoryCodes["Utilities"]!!
                merchantName.contains("Bank") -> merchantCategoryCodes["Bank"]!!
                else -> merchantCategoryCodes["Business Services"]!!
            }
        }
        
        private fun generateBalanceAfterTransaction(accountId: String, isCredit: Boolean): TransactionBalance {
            val baseAmount = Random.nextDouble(10000.0, 50000.0)
            val balanceAmount = if (isCredit) baseAmount + Random.nextDouble(100.0, 5000.0) else baseAmount - Random.nextDouble(10.0, 1000.0)
            
            return TransactionBalance(
                amount = Amount(
                    amount = "${balanceAmount.toBigDecimal().setScale(2)}",
                    currency = "SAR"
                ),
                creditDebitIndicator = "Credit",
                type = "ClosingBooked"
            )
        }
    }

    /**
     * Balance Test Data Factory
     */
    object BalanceFactory {
        
        fun createBalance(
            accountId: String = "acc-001",
            balanceType: String = "ClosingBooked",
            amount: String = "${Random.nextDouble(1000.0, 50000.0).toBigDecimal().setScale(2)}"
        ): Balance {
            return Balance(
                accountId = accountId,
                amount = Amount(
                    amount = amount,
                    currency = "SAR"
                ),
                creditDebitIndicator = "Credit",
                type = balanceType,
                dateTime = "2024-08-26T${Random.nextInt(8, 18)}:${Random.nextInt(0, 59)}:00Z"
            )
        }
        
        fun createAccountBalances(accountId: String): List<Balance> {
            val closingBooked = Random.nextDouble(10000.0, 100000.0)
            val closingAvailable = closingBooked - Random.nextDouble(100.0, 1000.0) // Account for pending transactions
            
            return listOf(
                createBalance(accountId, "ClosingBooked", "${closingBooked.toBigDecimal().setScale(2)}"),
                createBalance(accountId, "ClosingAvailable", "${closingAvailable.toBigDecimal().setScale(2)}"),
                createBalance(accountId, "InterimAvailable", "${(closingAvailable - Random.nextDouble(0.0, 500.0)).toBigDecimal().setScale(2)}")
            )
        }
        
        fun createLowBalanceAccount(accountId: String): List<Balance> {
            val lowAmount = Random.nextDouble(10.0, 100.0)
            return listOf(
                createBalance(accountId, "ClosingBooked", "${lowAmount.toBigDecimal().setScale(2)}"),
                createBalance(accountId, "ClosingAvailable", "${(lowAmount * 0.9).toBigDecimal().setScale(2)}")
            )
        }
    }

    /**
     * Payment Test Data Factory
     */
    object PaymentFactory {
        
        fun createDomesticPaymentRequest(
            debtorIban: String = "SA4420000001234567890123456",
            creditorIban: String = "SA4420000001234567890123457",
            amount: String = "1000.00",
            currency: String = "SAR"
        ): DomesticPaymentRequest {
            return DomesticPaymentRequest(
                instructionIdentification = "PAY-${Random.nextLong(10000, 99999)}",
                endToEndIdentification = "E2E-${Random.nextLong(10000, 99999)}",
                instructedAmount = Amount(amount = amount, currency = currency),
                debtorAccount = Account(iban = debtorIban),
                creditorAccount = Account(
                    iban = creditorIban,
                    name = AccountFactory.saudiNames.random()
                ),
                creditorName = AccountFactory.saudiNames.random(),
                remittanceInformation = RemittanceInformation(
                    unstructured = listOf("Test payment ${Random.nextInt(1, 1000)}")
                )
            )
        }
        
        fun createScheduledPaymentRequest(
            executionDateTime: String = "2024-09-01T10:00:00Z",
            amount: String = "500.00"
        ): DomesticScheduledPaymentRequest {
            return DomesticScheduledPaymentRequest(
                instructionIdentification = "SCHED-${Random.nextLong(10000, 99999)}",
                endToEndIdentification = "SCHED-E2E-${Random.nextLong(10000, 99999)}",
                requestedExecutionDateTime = executionDateTime,
                instructedAmount = Amount(amount = amount, currency = "SAR"),
                debtorAccount = Account(iban = "SA4420000001234567890123456"),
                creditorAccount = Account(
                    iban = "SA4420000001234567890123457",
                    name = "Scheduled Payment Beneficiary"
                ),
                creditorName = "Scheduled Payment Beneficiary",
                remittanceInformation = RemittanceInformation(
                    unstructured = listOf("Scheduled payment - ${Random.nextInt(1, 100)}")
                )
            )
        }
        
        fun createRecurringPayments(count: Int = 5): List<DomesticScheduledPaymentRequest> {
            return (1..count).map {
                val day = Random.nextInt(1, 28)
                createScheduledPaymentRequest(
                    executionDateTime = "2024-09-${day.toString().padStart(2, '0')}T10:00:00Z",
                    amount = "${Random.nextDouble(100.0, 2000.0).toBigDecimal().setScale(2)}"
                )
            }
        }
        
        fun createBulkPaymentRequests(count: Int = 10): List<DomesticPaymentRequest> {
            return (1..count).map {
                createDomesticPaymentRequest(
                    amount = "${Random.nextDouble(50.0, 500.0).toBigDecimal().setScale(2)}"
                )
            }
        }
    }

    /**
     * Consent Test Data Factory
     */
    object ConsentFactory {
        
        fun createConsentRequest(
            permissions: List<ConsentPermission> = listOf(
                ConsentPermission.READ_ACCOUNTS_BASIC,
                ConsentPermission.READ_ACCOUNTS_DETAIL,
                ConsentPermission.READ_BALANCES,
                ConsentPermission.READ_TRANSACTIONS_BASIC,
                ConsentPermission.READ_TRANSACTIONS_DETAIL
            ),
            expirationHours: Int = 24
        ): ConsentRequest {
            return ConsentRequest(
                permissions = permissions,
                expirationDateTime = "2024-08-${Random.nextInt(27, 31)}T${Random.nextInt(10, 23)}:00:00Z",
                transactionFromDateTime = "2024-01-01T00:00:00Z",
                transactionToDateTime = "2024-12-31T23:59:59Z"
            )
        }
        
        fun createPaymentConsentRequest(
            amount: String = "1000.00",
            debtorIban: String = "SA4420000001234567890123456",
            creditorIban: String = "SA4420000001234567890123457"
        ): PaymentConsentRequest {
            return PaymentConsentRequest(
                permissions = listOf(ConsentPermission.CREATE_DOMESTIC_PAYMENTS),
                instructedAmount = Amount(amount = amount, currency = "SAR"),
                debtorAccount = Account(iban = debtorIban),
                creditorAccount = Account(
                    iban = creditorIban,
                    name = AccountFactory.saudiNames.random()
                ),
                creditorName = AccountFactory.saudiNames.random(),
                remittanceInformation = RemittanceInformation(
                    unstructured = listOf("Payment consent ${Random.nextInt(1, 1000)}")
                ),
                expirationDateTime = "2024-08-27T23:59:59Z"
            )
        }
    }

    /**
     * Authentication Test Data Factory
     */
    object AuthFactory {
        
        fun createTokenResponse(
            accessToken: String = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.mock.${Random.nextLong()}",
            expiresIn: Int = 3600
        ): TokenResponse {
            return TokenResponse(
                accessToken = accessToken,
                tokenType = "Bearer",
                expiresIn = expiresIn,
                refreshToken = "refresh.token.${Random.nextLong()}",
                scope = "accounts payments"
            )
        }
        
        fun createPARRequest(
            clientId: String = "test-client-${Random.nextLong(1000, 9999)}",
            scope: String = "accounts payments",
            consentId: String = "consent-${Random.nextLong(1000, 9999)}"
        ): PARRequest {
            return PARRequest(
                clientId = clientId,
                redirectUri = "https://app.test.com/callback",
                scope = scope,
                consentId = consentId
            )
        }
        
        fun createTokenRequest(
            grantType: GrantType = GrantType.AUTHORIZATION_CODE,
            clientId: String = "test-client-${Random.nextLong(1000, 9999)}"
        ): TokenRequest {
            return TokenRequest(
                grantType = grantType,
                code = if (grantType == GrantType.AUTHORIZATION_CODE) "auth_code_${Random.nextLong()}" else null,
                redirectUri = "https://app.test.com/callback",
                clientId = clientId,
                clientSecret = "client-secret-${Random.nextLong()}",
                codeVerifier = if (grantType == GrantType.AUTHORIZATION_CODE) "code-verifier-${Random.nextLong()}" else null,
                refreshToken = if (grantType == GrantType.REFRESH_TOKEN) "refresh_${Random.nextLong()}" else null,
                scope = if (grantType == GrantType.CLIENT_CREDENTIALS) "accounts" else null
            )
        }
    }

    /**
     * Error Test Data Factory
     */
    object ErrorFactory {
        
        fun createBankError(
            errorCode: String = "invalid_request",
            errorDescription: String = "The request is malformed"
        ): BankError {
            return BankError(
                error = errorCode,
                errorDescription = errorDescription,
                errorUri = "https://sama.gov.sa/errors/$errorCode"
            )
        }
        
        fun createCommonBankErrors(): List<BankError> {
            return listOf(
                createBankError("invalid_client", "Invalid client credentials"),
                createBankError("invalid_grant", "Invalid authorization grant"),
                createBankError("invalid_scope", "Requested scope is invalid"),
                createBankError("insufficient_funds", "Insufficient balance in account"),
                createBankError("invalid_account", "Account not found or inactive"),
                createBankError("limit_exceeded", "Transaction limit exceeded"),
                createBankError("consent_expired", "Consent has expired"),
                createBankError("consent_revoked", "Consent has been revoked")
            )
        }
    }

    /**
     * Utility methods for test data generation
     */
    object Utils {
        
        fun randomSaudiIBAN(bankCode: String = "20"): String {
            val accountNumber = Random.nextLong(100000000000000, 999999999999999)
            return "SA${bankCode}${bankCode}000${accountNumber}"
        }
        
        fun randomAmount(min: Double = 10.0, max: Double = 10000.0): String {
            return "${Random.nextDouble(min, max).toBigDecimal().setScale(2)}"
        }
        
        fun randomDateTime(daysFromNow: Int = 0): String {
            val day = 26 + daysFromNow
            return "2024-08-${day.toString().padStart(2, '0')}T${Random.nextInt(8, 18)}:${Random.nextInt(0, 59)}:00Z"
        }
        
        fun randomTransactionReference(): String {
            val prefixes = listOf("TXN", "PAY", "TRAN", "REF", "ID")
            return "${prefixes.random()}-${Random.nextLong(10000, 99999)}"
        }
        
        fun randomPhoneNumber(): String {
            return "+9665${Random.nextLong(10000000, 99999999)}"
        }
        
        fun randomNationalId(): String {
            return "${Random.nextLong(1000000000, 9999999999)}"
        }
    }
}