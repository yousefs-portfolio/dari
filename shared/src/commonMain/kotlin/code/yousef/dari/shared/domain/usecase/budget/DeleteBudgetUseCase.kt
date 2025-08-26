package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.utils.Result

/**
 * Use case for deleting budgets with comprehensive dependency management
 * Supports Saudi-specific budget types and Islamic considerations
 */
class DeleteBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(request: DeleteBudgetRequest): Result<DeleteBudgetResult> {
        return try {
            // Get the budget to delete
            val budgetResult = budgetRepository.getBudgetById(request.budgetId)
            if (budgetResult is Result.Error) {
                return budgetResult
            }

            val budget = (budgetResult as Result.Success).data

            // Check for child budgets
            val childBudgetsResult = budgetRepository.getChildBudgets(request.budgetId)
            if (childBudgetsResult is Result.Error) {
                return childBudgetsResult
            }

            val childBudgets = (childBudgetsResult as Result.Success).data

            // Validate deletion conditions
            if (childBudgets.isNotEmpty() && !request.cascadeToChildren && !request.cascadeDelete && !request.force) {
                return Result.Error(IllegalStateException(
                    "Budget has ${childBudgets.size} child budgets. Use cascadeToChildren, cascadeDelete, or force to proceed."
                ))
            }

            // Check for active transactions
            val transactionsResult = transactionRepository.getTransactionsByBudgetId(request.budgetId)
            if (transactionsResult is Result.Error) {
                return transactionsResult
            }

            val associatedTransactions = (transactionsResult as Result.Success).data

            if (associatedTransactions.isNotEmpty() && !request.force) {
                return Result.Error(IllegalStateException(
                    "Budget has ${associatedTransactions.size} active transactions. Use force=true to proceed."
                ))
            }

            // Handle special budget types (Islamic budgets should be archived)
            val shouldArchive = shouldArchiveBudget(budget, request)

            // Process deletion/archiving
            val deletionResult = if (shouldArchive) {
                archiveBudget(budget, childBudgets, associatedTransactions, request)
            } else {
                deleteBudget(budget, childBudgets, associatedTransactions, request)
            }

            deletionResult

        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun shouldArchiveBudget(budget: Budget, request: DeleteBudgetRequest): Boolean {
        return when {
            // Always archive if explicitly requested
            request.preserveTransactionHistory -> true
            
            // Archive Islamic/religious budgets by default
            budget.budgetType == BudgetType.CHARITY -> true
            budget.tags.any { it in listOf("zakat", "sadaqah", "ramadan", "hajj", "islamic") } -> true
            budget.category == TransactionCategory.CHARITY -> true
            
            // Archive government/tax budgets
            budget.tags.any { it in listOf("tax", "government", "vat") } -> true
            
            else -> false
        }
    }

    private suspend fun archiveBudget(
        budget: Budget,
        childBudgets: List<Budget>,
        associatedTransactions: List<Transaction>,
        request: DeleteBudgetRequest
    ): Result<DeleteBudgetResult> {
        // Archive the main budget
        val archiveResult = budgetRepository.archiveBudget(request.budgetId)
        if (archiveResult is Result.Error) {
            return archiveResult
        }

        // Handle child budgets
        val (affectedChildren, deletedChildren) = handleChildBudgets(childBudgets, request)

        // Handle associated transactions (unlink but preserve)
        var unlinkedTransactions = 0
        for (transaction in associatedTransactions) {
            val unlinkResult = transactionRepository.updateTransaction(
                transaction.copy(budgetId = null)
            )
            if (unlinkResult is Result.Success) {
                unlinkedTransactions++
            }
        }

        return Result.Success(
            DeleteBudgetResult(
                deletedBudgetId = request.budgetId,
                originalBudgetName = budget.name,
                originalBudgetAmount = budget.amount,
                affectedChildBudgets = affectedChildren,
                deletedChildBudgets = deletedChildren,
                unlinkedTransactions = unlinkedTransactions,
                cancelledRecurringTransactions = 0,
                archived = true
            )
        )
    }

    private suspend fun deleteBudget(
        budget: Budget,
        childBudgets: List<Budget>,
        associatedTransactions: List<Transaction>,
        request: DeleteBudgetRequest
    ): Result<DeleteBudgetResult> {
        // Handle child budgets first
        val (affectedChildren, deletedChildren) = handleChildBudgets(childBudgets, request)

        // Handle associated transactions
        var unlinkedTransactions = 0
        var cancelledRecurring = 0

        for (transaction in associatedTransactions) {
            if (transaction.isRecurring && request.cancelRecurringTransactions) {
                val cancelResult = transactionRepository.cancelRecurringTransaction(transaction.id)
                if (cancelResult is Result.Success) {
                    cancelledRecurring++
                }
            } else if (request.force) {
                val unlinkResult = transactionRepository.updateTransaction(
                    transaction.copy(budgetId = null)
                )
                if (unlinkResult is Result.Success) {
                    unlinkedTransactions++
                }
            }
        }

        // Delete the main budget
        val deleteResult = budgetRepository.deleteBudget(request.budgetId)
        if (deleteResult is Result.Error) {
            return deleteResult
        }

        return Result.Success(
            DeleteBudgetResult(
                deletedBudgetId = request.budgetId,
                originalBudgetName = budget.name,
                originalBudgetAmount = budget.amount,
                affectedChildBudgets = affectedChildren,
                deletedChildBudgets = deletedChildren,
                unlinkedTransactions = unlinkedTransactions,
                cancelledRecurringTransactions = cancelledRecurring,
                archived = false
            )
        )
    }

    private suspend fun handleChildBudgets(
        childBudgets: List<Budget>,
        request: DeleteBudgetRequest
    ): Pair<List<Budget>, List<String>> {
        val affectedChildren = mutableListOf<Budget>()
        val deletedChildren = mutableListOf<String>()

        for (childBudget in childBudgets) {
            when {
                request.cascadeDelete -> {
                    // Delete child budget
                    budgetRepository.deleteBudget(childBudget.id)
                    deletedChildren.add(childBudget.id)
                }
                request.cascadeToChildren -> {
                    // Remove parent relationship, make child independent
                    val updatedChild = childBudget.copy(parentBudgetId = null)
                    val updateResult = budgetRepository.updateBudget(updatedChild)
                    if (updateResult is Result.Success) {
                        affectedChildren.add(updateResult.data)
                    }
                }
            }
        }

        return Pair(affectedChildren, deletedChildren)
    }
}

/**
 * Request for budget deletion
 */
data class DeleteBudgetRequest(
    val budgetId: String,
    val force: Boolean = false,
    val cascadeToChildren: Boolean = false,
    val cascadeDelete: Boolean = false,
    val preserveTransactionHistory: Boolean = false,
    val cancelRecurringTransactions: Boolean = false
)

/**
 * Result of budget deletion
 */
data class DeleteBudgetResult(
    val deletedBudgetId: String,
    val originalBudgetName: String,
    val originalBudgetAmount: Money,
    val affectedChildBudgets: List<Budget> = emptyList(),
    val deletedChildBudgets: List<String> = emptyList(),
    val unlinkedTransactions: Int = 0,
    val cancelledRecurringTransactions: Int = 0,
    val archived: Boolean = false
)