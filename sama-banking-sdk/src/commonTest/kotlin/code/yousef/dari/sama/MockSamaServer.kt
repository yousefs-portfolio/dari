package code.yousef.dari.sama

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Mock SAMA Server for testing SDK implementations
 * Provides realistic responses for SAMA Open Banking API endpoints
 */
class MockSamaServer {
    
    companion object {
        private val json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
        
        /**
         * Create MockEngine for testing with predefined responses
         */
        fun createMockEngine(): MockEngine {
            return MockEngine { request ->
                when {
                    // Authentication endpoints
                    request.url.encodedPath.endsWith("/par-request") -> handleParRequest(request)
                    request.url.encodedPath.endsWith("/token") -> handleTokenRequest(request)
                    request.url.encodedPath.contains("/authorize") -> handleAuthorizeRequest(request)
                    
                    // Account endpoints
                    request.url.encodedPath.endsWith("/accounts") -> handleAccountsRequest(request)
                    request.url.encodedPath.matches(Regex(".*/accounts/[^/]+$")) -> handleAccountDetailsRequest(request)
                    request.url.encodedPath.contains("/balances") -> handleBalancesRequest(request)
                    request.url.encodedPath.contains("/transactions") -> handleTransactionsRequest(request)
                    request.url.encodedPath.contains("/standing-orders") -> handleStandingOrdersRequest(request)
                    request.url.encodedPath.contains("/direct-debits") -> handleDirectDebitsRequest(request)
                    request.url.encodedPath.contains("/statements") -> handleStatementsRequest(request)
                    
                    // Payment endpoints
                    request.url.encodedPath.endsWith("/domestic-payments") -> handlePaymentRequest(request)
                    request.url.encodedPath.matches(Regex(".*/domestic-payments/[^/]+$")) -> handlePaymentStatusRequest(request)
                    request.url.encodedPath.endsWith("/domestic-scheduled-payments") -> handleScheduledPaymentRequest(request)
                    
                    // Consent endpoints
                    request.url.encodedPath.endsWith("/consents") -> handleConsentRequest(request)
                    request.url.encodedPath.matches(Regex(".*/consents/[^/]+$")) -> handleConsentStatusRequest(request)
                    
                    else -> respond(
                        content = """{"error": "not_found", "error_description": "Endpoint not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
        
        private fun handleParRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "request_uri" to "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c",
                    "expires_in" to 3600
                )),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleTokenRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "access_token" to "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.mock.token",
                    "token_type" to "Bearer",
                    "expires_in" to 3600,
                    "refresh_token" to "refresh.token.mock",
                    "scope" to "accounts payments"
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleAuthorizeRequest(request: MockRequestHandleScope): HttpResponse {
            val redirectUri = request.url.parameters["redirect_uri"]
            val state = request.url.parameters["state"]
            val authCode = "mock_auth_code_12345"
            
            return respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(
                    HttpHeaders.Location, "$redirectUri?code=$authCode&state=$state"
                )
            )
        }
        
        private fun handleAccountsRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "Account" to listOf(
                            mapOf(
                                "AccountId" to "acc-001",
                                "AccountNumber" to "1234567890123456",
                                "AccountName" to "Current Account",
                                "AccountType" to "CURRENT",
                                "Currency" to "SAR",
                                "IsActive" to true,
                                "OpeningDate" to "2023-01-15T00:00:00Z",
                                "IBAN" to "SA4420000001234567890123456"
                            ),
                            mapOf(
                                "AccountId" to "acc-002",
                                "AccountNumber" to "1234567890123457",
                                "AccountName" to "Savings Account",
                                "AccountType" to "SAVINGS",
                                "Currency" to "SAR",
                                "IsActive" to true,
                                "OpeningDate" to "2023-02-01T00:00:00Z",
                                "IBAN" to "SA4420000001234567890123457"
                            )
                        )
                    ),
                    "Links" to mapOf(
                        "Self" to "https://api.mock-bank.com.sa/open-banking/v1/accounts"
                    ),
                    "Meta" to mapOf(
                        "TotalPages" to 1,
                        "FirstAvailableDateTime" to "2023-01-01T00:00:00Z",
                        "LastAvailableDateTime" to "2024-12-31T23:59:59Z"
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleAccountDetailsRequest(request: MockRequestHandleScope): HttpResponse {
            val accountId = request.url.segments.last()
            
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "Account" to listOf(
                            mapOf(
                                "AccountId" to accountId,
                                "AccountNumber" to "1234567890123456",
                                "AccountName" to "Current Account",
                                "AccountType" to "CURRENT",
                                "Currency" to "SAR",
                                "IsActive" to true,
                                "OpeningDate" to "2023-01-15T00:00:00Z",
                                "IBAN" to "SA4420000001234567890123456",
                                "AccountHolderName" to "Ahmad Mohammed",
                                "BranchCode" to "001",
                                "ProductName" to "Premium Current Account",
                                "InterestRate" to "0.50"
                            )
                        )
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleBalancesRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "Balance" to listOf(
                            mapOf(
                                "AccountId" to "acc-001",
                                "Amount" to mapOf(
                                    "Amount" to "15000.00",
                                    "Currency" to "SAR"
                                ),
                                "CreditDebitIndicator" to "Credit",
                                "Type" to "ClosingBooked",
                                "DateTime" to "2024-08-26T10:00:00Z"
                            ),
                            mapOf(
                                "AccountId" to "acc-001",
                                "Amount" to mapOf(
                                    "Amount" to "14500.00",
                                    "Currency" to "SAR"
                                ),
                                "CreditDebitIndicator" to "Credit",
                                "Type" to "ClosingAvailable",
                                "DateTime" to "2024-08-26T10:00:00Z"
                            )
                        )
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleTransactionsRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "Transaction" to listOf(
                            mapOf(
                                "TransactionId" to "txn-001",
                                "AccountId" to "acc-001",
                                "Amount" to mapOf(
                                    "Amount" to "250.00",
                                    "Currency" to "SAR"
                                ),
                                "CreditDebitIndicator" to "Debit",
                                "Status" to "Booked",
                                "BookingDateTime" to "2024-08-25T14:30:00Z",
                                "ValueDateTime" to "2024-08-25T14:30:00Z",
                                "TransactionInformation" to "ATM Withdrawal",
                                "MerchantDetails" to mapOf(
                                    "MerchantName" to "Samba ATM",
                                    "MerchantCategoryCode" to "6011"
                                ),
                                "Balance" to mapOf(
                                    "Amount" to mapOf(
                                        "Amount" to "14750.00",
                                        "Currency" to "SAR"
                                    ),
                                    "CreditDebitIndicator" to "Credit",
                                    "Type" to "ClosingBooked"
                                )
                            ),
                            mapOf(
                                "TransactionId" to "txn-002",
                                "AccountId" to "acc-001",
                                "Amount" to mapOf(
                                    "Amount" to "3000.00",
                                    "Currency" to "SAR"
                                ),
                                "CreditDebitIndicator" to "Credit",
                                "Status" to "Booked",
                                "BookingDateTime" to "2024-08-24T09:15:00Z",
                                "ValueDateTime" to "2024-08-24T09:15:00Z",
                                "TransactionInformation" to "Salary Transfer",
                                "MerchantDetails" to mapOf(
                                    "MerchantName" to "Tech Solutions LLC",
                                    "MerchantCategoryCode" to "7372"
                                ),
                                "Balance" to mapOf(
                                    "Amount" to mapOf(
                                        "Amount" to "15000.00",
                                        "Currency" to "SAR"
                                    ),
                                    "CreditDebitIndicator" to "Credit",
                                    "Type" to "ClosingBooked"
                                )
                            )
                        )
                    ),
                    "Links" to mapOf(
                        "Self" to "https://api.mock-bank.com.sa/open-banking/v1/accounts/acc-001/transactions"
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleStandingOrdersRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "StandingOrder" to listOf(
                            mapOf(
                                "StandingOrderId" to "so-001",
                                "AccountId" to "acc-001",
                                "Amount" to mapOf(
                                    "Amount" to "500.00",
                                    "Currency" to "SAR"
                                ),
                                "Frequency" to "EveryMonth",
                                "Reference" to "Monthly Rent Payment",
                                "FirstPaymentDateTime" to "2024-01-01T00:00:00Z",
                                "NextPaymentDateTime" to "2024-09-01T00:00:00Z",
                                "FinalPaymentDateTime" to "2024-12-01T00:00:00Z",
                                "StandingOrderStatusCode" to "Active"
                            )
                        )
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleDirectDebitsRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "DirectDebit" to listOf(
                            mapOf(
                                "DirectDebitId" to "dd-001",
                                "AccountId" to "acc-001",
                                "MandateIdentification" to "MANDATE-001",
                                "DirectDebitStatusCode" to "Active",
                                "Name" to "Saudi Electricity Company",
                                "PreviousPaymentDateTime" to "2024-07-15T00:00:00Z",
                                "PreviousPaymentAmount" to mapOf(
                                    "Amount" to "180.50",
                                    "Currency" to "SAR"
                                )
                            )
                        )
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleStatementsRequest(request: MockRequestHandleScope): HttpResponse {
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "Statement" to listOf(
                            mapOf(
                                "StatementId" to "stmt-202407",
                                "AccountId" to "acc-001",
                                "StatementDateTime" to "2024-07-31T23:59:59Z",
                                "Type" to "RegularPeriodic",
                                "StartDateTime" to "2024-07-01T00:00:00Z",
                                "EndDateTime" to "2024-07-31T23:59:59Z"
                            ),
                            mapOf(
                                "StatementId" to "stmt-202406",
                                "AccountId" to "acc-001",
                                "StatementDateTime" to "2024-06-30T23:59:59Z",
                                "Type" to "RegularPeriodic",
                                "StartDateTime" to "2024-06-01T00:00:00Z",
                                "EndDateTime" to "2024-06-30T23:59:59Z"
                            )
                        )
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handlePaymentRequest(request: MockRequestHandleScope): HttpResponse {
            return when (request.method) {
                HttpMethod.Post -> respond(
                    content = json.encodeToString(mapOf(
                        "Data" to mapOf(
                            "DomesticPaymentId" to "pay-001",
                            "ConsentId" to "consent-pay-001",
                            "CreationDateTime" to "2024-08-26T10:30:00Z",
                            "Status" to "AcceptedSettlementInProcess",
                            "StatusUpdateDateTime" to "2024-08-26T10:30:00Z",
                            "ExpectedExecutionDateTime" to "2024-08-26T11:00:00Z",
                            "Charges" to listOf(
                                mapOf(
                                    "Amount" to mapOf(
                                        "Amount" to "2.00",
                                        "Currency" to "SAR"
                                    ),
                                    "Type" to "SAMA_FEE"
                                )
                            )
                        ),
                        "Links" to mapOf(
                            "Self" to "https://api.mock-bank.com.sa/open-banking/v1/domestic-payments/pay-001"
                        )
                    )),
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(
                    content = """{"error": "method_not_allowed", "error_description": "Method not allowed"}""",
                    status = HttpStatusCode.MethodNotAllowed,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        
        private fun handlePaymentStatusRequest(request: MockRequestHandleScope): HttpResponse {
            val paymentId = request.url.segments.last()
            
            return respond(
                content = json.encodeToString(mapOf(
                    "Data" to mapOf(
                        "DomesticPaymentId" to paymentId,
                        "Status" to "AcceptedSettlementCompleted",
                        "StatusUpdateDateTime" to "2024-08-26T11:00:00Z",
                        "Charges" to listOf(
                            mapOf(
                                "Amount" to mapOf(
                                    "Amount" to "2.00",
                                    "Currency" to "SAR"
                                ),
                                "Type" to "SAMA_FEE"
                            )
                        )
                    )
                )),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        private fun handleScheduledPaymentRequest(request: MockRequestHandleScope): HttpResponse {
            return when (request.method) {
                HttpMethod.Post -> respond(
                    content = json.encodeToString(mapOf(
                        "Data" to mapOf(
                            "DomesticScheduledPaymentId" to "scheduled-pay-001",
                            "ConsentId" to "consent-scheduled-pay-001",
                            "CreationDateTime" to "2024-08-26T10:30:00Z",
                            "Status" to "InitiationPending",
                            "StatusUpdateDateTime" to "2024-08-26T10:30:00Z"
                        )
                    )),
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                HttpMethod.Delete -> respond(
                    content = "",
                    status = HttpStatusCode.NoContent
                )
                else -> respond(
                    content = """{"error": "method_not_allowed", "error_description": "Method not allowed"}""",
                    status = HttpStatusCode.MethodNotAllowed,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        
        private fun handleConsentRequest(request: MockRequestHandleScope): HttpResponse {
            return when (request.method) {
                HttpMethod.Post -> respond(
                    content = json.encodeToString(mapOf(
                        "Data" to mapOf(
                            "ConsentId" to "consent-001",
                            "Status" to "AwaitingAuthorisation",
                            "CreationDateTime" to "2024-08-26T10:00:00Z",
                            "StatusUpdateDateTime" to "2024-08-26T10:00:00Z",
                            "Permissions" to listOf(
                                "ReadAccountsBasic",
                                "ReadAccountsDetail",
                                "ReadBalances",
                                "ReadTransactionsBasic",
                                "ReadTransactionsCredits",
                                "ReadTransactionsDebits",
                                "ReadTransactionsDetail"
                            ),
                            "ExpirationDateTime" to "2024-08-27T10:00:00Z"
                        )
                    )),
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(
                    content = """{"error": "method_not_allowed", "error_description": "Method not allowed"}""",
                    status = HttpStatusCode.MethodNotAllowed,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        
        private fun handleConsentStatusRequest(request: MockRequestHandleScope): HttpResponse {
            val consentId = request.url.segments.last()
            
            return when (request.method) {
                HttpMethod.Get -> respond(
                    content = json.encodeToString(mapOf(
                        "Data" to mapOf(
                            "ConsentId" to consentId,
                            "Status" to "Authorised",
                            "CreationDateTime" to "2024-08-26T10:00:00Z",
                            "StatusUpdateDateTime" to "2024-08-26T10:05:00Z",
                            "Permissions" to listOf(
                                "ReadAccountsBasic",
                                "ReadAccountsDetail",
                                "ReadBalances",
                                "ReadTransactionsBasic",
                                "ReadTransactionsCredits",
                                "ReadTransactionsDebits",
                                "ReadTransactionsDetail"
                            ),
                            "ExpirationDateTime" to "2024-08-27T10:00:00Z"
                        )
                    )),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                HttpMethod.Delete -> respond(
                    content = "",
                    status = HttpStatusCode.NoContent
                )
                else -> respond(
                    content = """{"error": "method_not_allowed", "error_description": "Method not allowed"}""",
                    status = HttpStatusCode.MethodNotAllowed,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
    }
}