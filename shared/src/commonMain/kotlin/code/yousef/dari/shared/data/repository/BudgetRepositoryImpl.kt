package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.domain.models.Budget
import code.yousef.dari.shared.domain.models.BudgetCategory
import code.yousef.dari.shared.domain.models.BudgetPerformanceSummary
import code.yousef.dari.shared.domain.models.BudgetType
import code.yousef.dari.shared.domain.models.BudgetPeriod
import code.yousef.dari.shared.domain.models.BudgetStatus
import code.yousef.dari.shared.domain.models.Money
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Budget Repository Implementation
 * Implements offline-first data access for budgets with SQLDelight
 */
class BudgetRepositoryImpl(
    private val database: DariDatabase
) : BudgetRepository {

    private val budgetDao = database.budgetDao()

    override suspend fun createBudget(budget: Budget): Result<Budget> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    budgetDao.insertBudget(
                        budgetId = budget.budgetId,
                        name = budget.name,
                        description = budget.description,
                        budgetType = budget.budgetType.name,
                        period = budget.period.name,
                        totalAmount = budget.totalAmount.amount,
                        currency = budget.totalAmount.currency,
                        spentAmount = budget.spentAmount.amount,
                        remainingAmount = budget.remainingAmount.amount,
                        startDate = budget.startDate.epochSeconds,
                        endDate = budget.endDate.epochSeconds,
                        isActive = if (budget.isActive) 1 else 0,
                        alertThreshold = budget.alertThreshold,
                        rolloverEnabled = if (budget.rolloverEnabled) 1 else 0,
                        createdAt = budget.createdAt.epochSeconds,
                        updatedAt = budget.updatedAt.epochSeconds,
                        metadata = serializeMetadata(budget.metadata)
                    )
                    
                    // Insert budget categories
                    budget.categories.forEach { category ->
                        budgetDao.insertBudgetCategory(
                            budgetId = budget.budgetId,
                            categoryId = category.categoryId,
                            categoryName = category.name,
                            budgetedAmount = category.budgetedAmount.amount,
                            spentAmount = category.spentAmount.amount,
                            remainingAmount = category.remainingAmount.amount
                        )
                    }
                }
                Result.success(budget)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateBudget(budget: Budget): Result<Budget> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    budgetDao.updateBudget(
                        name = budget.name,
                        description = budget.description,
                        budgetType = budget.budgetType.name,
                        period = budget.period.name,
                        totalAmount = budget.totalAmount.amount,
                        currency = budget.totalAmount.currency,
                        spentAmount = budget.spentAmount.amount,
                        remainingAmount = budget.remainingAmount.amount,
                        startDate = budget.startDate.epochSeconds,
                        endDate = budget.endDate.epochSeconds,
                        isActive = if (budget.isActive) 1 else 0,
                        alertThreshold = budget.alertThreshold,
                        rolloverEnabled = if (budget.rolloverEnabled) 1 else 0,
                        updatedAt = Clock.System.now().epochSeconds,
                        metadata = serializeMetadata(budget.metadata),
                        budgetId = budget.budgetId
                    )
                    
                    // Update budget categories
                    budgetDao.deleteBudgetCategories(budget.budgetId)
                    budget.categories.forEach { category ->
                        budgetDao.insertBudgetCategory(
                            budgetId = budget.budgetId,
                            categoryId = category.categoryId,
                            categoryName = category.name,
                            budgetedAmount = category.budgetedAmount.amount,
                            spentAmount = category.spentAmount.amount,
                            remainingAmount = category.remainingAmount.amount
                        )
                    }
                }
                Result.success(budget)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    budgetDao.deleteBudgetCategories(budgetId)
                    budgetDao.deleteBudget(budgetId)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetById(budgetId: String): Result<Budget?> {
        return withContext(Dispatchers.Default) {
            try {
                val budget = budgetDao.selectById(budgetId).executeAsOneOrNull()?.toDomainModel()
                Result.success(budget)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getActiveBudgets(): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectActiveBudgets()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAllBudgets(): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectAll()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetsByType(type: BudgetType): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectByType(type.name)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetsForDateRange(
        startDate: Instant,
        endDate: Instant
    ): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectByDateRange(
                    startDate.epochSeconds,
                    endDate.epochSeconds
                ).executeAsList().map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCurrentBudget(): Result<Budget?> {
        return withContext(Dispatchers.Default) {
            try {
                val now = Clock.System.now().epochSeconds
                val budget = budgetDao.selectCurrentBudget(now).executeAsOneOrNull()?.toDomainModel()
                Result.success(budget)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetPerformanceSummaries(): Result<List<BudgetPerformanceSummary>> {
        return withContext(Dispatchers.Default) {
            try {
                val summaries = budgetDao.selectPerformanceSummaries()
                    .executeAsList()
                    .map { it.toPerformanceSummary() }
                Result.success(summaries)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getOverspentBudgets(): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectOverspentBudgets()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetsNearLimit(thresholdPercentage: Double): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectBudgetsNearLimit(thresholdPercentage)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateBudgetSpending(
        budgetId: String,
        categorySpending: Map<String, Money>
    ): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    categorySpending.forEach { (categoryId, spentAmount) ->
                        val category = budgetDao.selectBudgetCategory(budgetId, categoryId).executeAsOneOrNull()
                        if (category != null) {
                            val remainingAmount = Money(category.budgetedAmount, spentAmount.currency) - spentAmount
                            budgetDao.updateBudgetCategorySpending(
                                spentAmount = spentAmount.amount,
                                remainingAmount = remainingAmount.amount,
                                budgetId = budgetId,
                                categoryId = categoryId
                            )
                        }
                    }
                    
                    // Update total budget spent amount
                    val totalSpent = categorySpending.values.fold(Money.fromInt(0, "SAR")) { acc, amount ->
                        acc + amount
                    }
                    val budget = budgetDao.selectById(budgetId).executeAsOneOrNull()
                    if (budget != null) {
                        val remainingAmount = Money(budget.totalAmount, totalSpent.currency) - totalSpent
                        budgetDao.updateBudgetSpending(
                            spentAmount = totalSpent.amount,
                            remainingAmount = remainingAmount.amount,
                            updatedAt = Clock.System.now().epochSeconds,
                            budgetId = budgetId
                        )
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetByCategory(categoryId: String): Result<Budget?> {
        return withContext(Dispatchers.Default) {
            try {
                val budget = budgetDao.selectByCategory(categoryId).executeAsOneOrNull()?.toDomainModel()
                Result.success(budget)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchBudgets(query: String): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.searchBudgets(query, query)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getBudgetHistory(limit: Int): Result<List<Budget>> {
        return withContext(Dispatchers.Default) {
            try {
                val budgets = budgetDao.selectBudgetHistory(limit.toLong())
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(budgets)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun isBudgetNameExists(name: String, excludeBudgetId: String?): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                val exists = if (excludeBudgetId != null) {
                    budgetDao.checkBudgetNameExistsExcluding(name, excludeBudgetId).executeAsOne() > 0
                } else {
                    budgetDao.checkBudgetNameExists(name).executeAsOne() > 0
                }
                Result.success(exists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTotalBudgetAmount(startDate: Instant, endDate: Instant): Result<Money?> {
        return withContext(Dispatchers.Default) {
            try {
                val totalAmount = budgetDao.getTotalBudgetAmount(
                    startDate.epochSeconds,
                    endDate.epochSeconds
                ).executeAsOneOrNull()
                val amount = totalAmount?.let { Money(it, "SAR") }
                Result.success(amount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTotalSpentAmount(startDate: Instant, endDate: Instant): Result<Money?> {
        return withContext(Dispatchers.Default) {
            try {
                val totalSpent = budgetDao.getTotalSpentAmount(
                    startDate.epochSeconds,
                    endDate.epochSeconds
                ).executeAsOneOrNull()
                val amount = totalSpent?.let { Money(it, "SAR") }
                Result.success(amount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun serializeMetadata(metadata: Map<String, String>): String {
        return if (metadata.isEmpty()) {
            "{}"
        } else {
            metadata.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { "\"${it.key}\":\"${it.value}\"" }
        }
    }

    private fun deserializeMetadata(json: String): Map<String, String> {
        if (json == "{}") return emptyMap()
        
        return try {
            json.removeSurrounding("{", "}")
                .split(",")
                .associate { pair ->
                    val (key, value) = pair.split(":")
                    key.removeSurrounding("\"") to value.removeSurrounding("\"")
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// Extension functions to convert between database and domain models
private suspend fun code.yousef.dari.shared.database.Budget.toDomainModel(): Budget {
    // This would need to load budget categories from the database
    // For now, return a simplified version
    return Budget(
        budgetId = budgetId,
        name = name,
        description = description,
        budgetType = BudgetType.valueOf(budgetType),
        period = BudgetPeriod.valueOf(period),
        totalAmount = Money(totalAmount, currency),
        spentAmount = Money(spentAmount, currency),
        remainingAmount = Money(remainingAmount, currency),
        startDate = Instant.fromEpochSeconds(startDate),
        endDate = Instant.fromEpochSeconds(endDate),
        isActive = isActive == 1L,
        alertThreshold = alertThreshold,
        rolloverEnabled = rolloverEnabled == 1L,
        categories = emptyList(), // Would load from database
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        metadata = deserializeMetadata(metadata ?: "{}")
    )
}

private fun code.yousef.dari.shared.database.SelectPerformanceSummaries.toPerformanceSummary(): BudgetPerformanceSummary {
    return BudgetPerformanceSummary(
        budgetId = budgetId,
        budgetName = name,
        totalAmount = Money(totalAmount, currency),
        spentAmount = Money(spentAmount, currency),
        remainingAmount = Money(remainingAmount, currency),
        spentPercentage = if (totalAmount != "0") {
            (spentAmount.toDouble() / totalAmount.toDouble()) * 100.0
        } else 0.0,
        daysRemaining = calculateDaysRemaining(endDate),
        status = when {
            spentAmount.toDouble() > totalAmount.toDouble() -> BudgetStatus.OVER_BUDGET
            spentAmount.toDouble() >= totalAmount.toDouble() * 0.8 -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.ON_TRACK
        },
        isActive = isActive == 1L
    )
}

private fun calculateDaysRemaining(endDateSeconds: Long): Int {
    val endDate = Instant.fromEpochSeconds(endDateSeconds)
    val now = Clock.System.now()
    val duration = endDate - now
    return maxOf(0, (duration.inWholeDays.toInt()))
}

private fun deserializeMetadata(json: String): Map<String, String> {
    if (json == "{}") return emptyMap()
    
    return try {
        json.removeSurrounding("{", "}")
            .split(",")
            .associate { pair ->
                val (key, value) = pair.split(":")
                key.removeSurrounding("\"") to value.removeSurrounding("\"")
            }
    } catch (e: Exception) {
        emptyMap()
    }
}