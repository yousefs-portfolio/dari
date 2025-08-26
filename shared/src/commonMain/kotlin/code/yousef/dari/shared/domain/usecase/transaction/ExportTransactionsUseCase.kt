package code.yousef.dari.shared.domain.usecase.transaction

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for exporting transactions to various formats (CSV, PDF, Excel)
 * Supports Saudi-specific formatting and filtering options
 */
class ExportTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(request: ExportTransactionsRequest): Result<ExportData> {
        return try {
            // Validate request
            if (request.startDate > request.endDate) {
                return Result.Error(IllegalArgumentException("Start date must be before end date"))
            }

            // Get transactions from repository
            val transactionsResult = transactionRepository.getTransactionsByDateRange(
                accountId = request.accountId,
                startDate = request.startDate,
                endDate = request.endDate
            )

            when (transactionsResult) {
                is Result.Success -> {
                    val filteredTransactions = filterTransactions(
                        transactions = transactionsResult.data,
                        includeCategories = request.includeCategories
                    )

                    val exportData = when (request.format) {
                        ExportFormat.CSV -> generateCsvExport(filteredTransactions, request)
                        ExportFormat.PDF -> generatePdfExport(filteredTransactions, request)
                        ExportFormat.EXCEL -> generateExcelExport(filteredTransactions, request)
                    }

                    Result.Success(exportData)
                }
                is Result.Error -> transactionsResult
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        includeCategories: List<TransactionCategory>
    ): List<Transaction> {
        return if (includeCategories.isEmpty()) {
            transactions
        } else {
            transactions.filter { it.category in includeCategories }
        }
    }

    private fun generateCsvExport(
        transactions: List<Transaction>,
        request: ExportTransactionsRequest
    ): ExportData {
        val csvBuilder = StringBuilder()

        // Add headers if requested
        if (request.includeHeaders) {
            val headers = mutableListOf(
                "Date", "Description", "Amount", "Currency", "Category", "Type", 
                "Merchant", "Location", "Payment Method", "Status", "Tags"
            )
            if (request.includeReceiptUrls) {
                headers.add("Receipt URL")
            }
            csvBuilder.appendLine(headers.joinToString(","))
        }

        // Add transaction data
        transactions.forEach { transaction ->
            val row = mutableListOf(
                formatDate(transaction.date),
                escapeCSVField(transaction.description),
                formatAmount(transaction.amount, request.locale),
                transaction.amount.currency.name,
                transaction.category.name,
                transaction.type.name,
                escapeCSVField(transaction.merchant?.name ?: ""),
                escapeCSVField(transaction.location?.address ?: ""),
                transaction.paymentMethod.name,
                transaction.status.name,
                escapeCSVField(transaction.tags.joinToString(";"))
            )

            if (request.includeReceiptUrls) {
                row.add(escapeCSVField(transaction.receipt?.url ?: ""))
            }

            csvBuilder.appendLine(row.joinToString(","))
        }

        return ExportData(
            content = csvBuilder.toString(),
            format = ExportFormat.CSV,
            fileName = generateFileName(request, "csv"),
            totalTransactions = transactions.size
        )
    }

    private fun generatePdfExport(
        transactions: List<Transaction>,
        request: ExportTransactionsRequest
    ): ExportData {
        // For PDF generation, we'll create a simplified content structure
        // In a real implementation, this would use a PDF generation library
        val pdfContent = buildString {
            appendLine("DARI - Transaction Export Report")
            appendLine("Generated on: ${formatDate(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))}")
            appendLine("Period: ${formatDate(request.startDate)} to ${formatDate(request.endDate)}")
            appendLine("Account: ${request.accountId}")
            appendLine("Total Transactions: ${transactions.size}")
            appendLine()
            appendLine("TRANSACTION DETAILS")
            appendLine("=" .repeat(50))
            
            transactions.forEach { transaction ->
                appendLine()
                appendLine("Date: ${formatDate(transaction.date)}")
                appendLine("Description: ${transaction.description}")
                appendLine("Amount: ${formatAmount(transaction.amount, request.locale)}")
                appendLine("Category: ${transaction.category.name}")
                appendLine("Type: ${transaction.type.name}")
                if (transaction.merchant != null) {
                    appendLine("Merchant: ${transaction.merchant.name}")
                }
                if (transaction.location != null) {
                    appendLine("Location: ${transaction.location.address}")
                }
                if (request.includeReceiptUrls && transaction.receipt != null) {
                    appendLine("Receipt: ${transaction.receipt.url}")
                }
                appendLine("-".repeat(30))
            }
        }

        return ExportData(
            content = pdfContent,
            format = ExportFormat.PDF,
            fileName = generateFileName(request, "pdf"),
            totalTransactions = transactions.size
        )
    }

    private fun generateExcelExport(
        transactions: List<Transaction>,
        request: ExportTransactionsRequest
    ): ExportData {
        // For Excel generation, we'll create a structure that can be processed by Excel libraries
        // In a real implementation, this would use Apache POI or similar
        val excelContent = buildString {
            // Sheet definition
            appendLine("WORKSHEET: Transactions")
            appendLine()
            
            // Headers
            val headers = listOf(
                "Date", "Description", "Amount", "Currency", "Category", "Type",
                "Merchant", "Location", "Payment Method", "Status", "Tags"
            ) + if (request.includeReceiptUrls) listOf("Receipt URL") else emptyList()
            
            appendLine(headers.joinToString("\t"))
            
            // Data rows
            transactions.forEach { transaction ->
                val row = listOf(
                    formatDate(transaction.date),
                    transaction.description,
                    formatAmount(transaction.amount, request.locale),
                    transaction.amount.currency.name,
                    transaction.category.name,
                    transaction.type.name,
                    transaction.merchant?.name ?: "",
                    transaction.location?.address ?: "",
                    transaction.paymentMethod.name,
                    transaction.status.name,
                    transaction.tags.joinToString(";")
                ) + if (request.includeReceiptUrls) listOf(transaction.receipt?.url ?: "") else emptyList()
                
                appendLine(row.joinToString("\t"))
            }
        }

        return ExportData(
            content = excelContent,
            format = ExportFormat.EXCEL,
            fileName = generateFileName(request, "xlsx"),
            totalTransactions = transactions.size
        )
    }

    private fun formatDate(date: LocalDateTime): String {
        return date.format(LocalDateTime.Format {
            year(); char('-'); monthNumber(); char('-'); dayOfMonth()
            char(' ')
            hour(); char(':'); minute()
        })
    }

    private fun formatAmount(amount: Money, locale: String?): String {
        return when (locale) {
            "ar-SA" -> "${amount.value} ${amount.currency.getArabicSymbol()}"
            else -> "${amount.value} ${amount.currency.symbol}"
        }
    }

    private fun escapeCSVField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\"" 
        } else {
            field
        }
    }

    private fun generateFileName(request: ExportTransactionsRequest, extension: String): String {
        val startDateStr = request.startDate.format(LocalDateTime.Format {
            year(); char('-'); monthNumber(); char('-'); dayOfMonth()
        })
        val endDateStr = request.endDate.format(LocalDateTime.Format {
            year(); char('-'); monthNumber(); char('-'); dayOfMonth()
        })
        return "transactions_${startDateStr}_${endDateStr}.${extension}"
    }
}

/**
 * Request data class for transaction export
 */
data class ExportTransactionsRequest(
    val accountId: String,
    val format: ExportFormat,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val includeCategories: List<TransactionCategory> = emptyList(),
    val includeHeaders: Boolean = true,
    val includeReceiptUrls: Boolean = false,
    val locale: String? = null
)

/**
 * Export format enumeration
 */
enum class ExportFormat {
    CSV, PDF, EXCEL
}

/**
 * Export result data class
 */
data class ExportData(
    val content: String,
    val format: ExportFormat,
    val fileName: String,
    val totalTransactions: Int
)

/**
 * Extension function to get Arabic currency symbols for Saudi localization
 */
private fun Currency.getArabicSymbol(): String = when (this) {
    Currency.SAR -> "ر.س"
    Currency.USD -> "دولار أمريكي"
    Currency.EUR -> "يورو"
    Currency.GBP -> "جنيه إسترليني"
    Currency.AED -> "د.إ"
}