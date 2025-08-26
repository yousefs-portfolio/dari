package code.yousef.dari.sama.interfaces

import code.yousef.dari.sama.models.*

/**
 * Account Information Service (AIS) following SAMA Open Banking specifications
 * Provides access to account data with proper consent management
 */
interface AccountService {
    
    /**
     * Retrieve all accounts accessible with current consent
     * @param accessToken Valid access token with accounts scope
     * @return List of account summaries
     */
    suspend fun getAccounts(accessToken: String): Result<List<Account>>
    
    /**
     * Get detailed information for a specific account
     * @param accessToken Valid access token
     * @param accountId Unique account identifier
     * @return Detailed account information
     */
    suspend fun getAccountDetails(
        accessToken: String,
        accountId: String
    ): Result<AccountDetails>
    
    /**
     * Get current and available balances for an account
     * @param accessToken Valid access token
     * @param accountId Unique account identifier
     * @return Account balance information
     */
    suspend fun getAccountBalances(
        accessToken: String,
        accountId: String
    ): Result<AccountBalances>
    
    /**
     * Retrieve transactions for an account with pagination
     * @param accessToken Valid access token
     * @param accountId Unique account identifier
     * @param fromDate Start date for transaction filter (ISO 8601)
     * @param toDate End date for transaction filter (ISO 8601)
     * @param limit Maximum number of transactions to return (default 100)
     * @param offset Pagination offset (default 0)
     * @return Paginated list of transactions
     */
    suspend fun getTransactions(
        accessToken: String,
        accountId: String,
        fromDate: String? = null,
        toDate: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<TransactionResponse>
    
    /**
     * Get standing orders for an account
     * @param accessToken Valid access token
     * @param accountId Unique account identifier
     * @return List of standing orders
     */
    suspend fun getStandingOrders(
        accessToken: String,
        accountId: String
    ): Result<List<StandingOrder>>
    
    /**
     * Get direct debits for an account
     * @param accessToken Valid access token
     * @param accountId Unique account identifier
     * @return List of direct debits
     */
    suspend fun getDirectDebits(
        accessToken: String,
        accountId: String
    ): Result<List<DirectDebit>>
    
    /**
     * Get account statements
     * @param accessToken Valid access token
     * @param accountId Unique account identifier
     * @param fromDate Start date for statements (ISO 8601)
     * @param toDate End date for statements (ISO 8601)
     * @return List of account statements
     */
    suspend fun getStatements(
        accessToken: String,
        accountId: String,
        fromDate: String,
        toDate: String
    ): Result<List<Statement>>
}