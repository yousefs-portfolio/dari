package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.data.database.DariDatabase
import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Goal Repository Implementation
 * Implements offline-first data access for goals with SQLDelight
 */
class GoalRepositoryImpl(
    private val database: DariDatabase
) : GoalRepository {

    private val goalDao = database.goalDao()

    override suspend fun createGoal(goal: Goal): Result<Goal> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    goalDao.insertGoal(
                        goalId = goal.goalId,
                        name = goal.name,
                        description = goal.description,
                        goalType = goal.goalType.name,
                        priority = goal.priority.name,
                        targetAmount = goal.targetAmount.amount,
                        currentAmount = goal.currentAmount.amount,
                        currency = goal.currency,
                        targetDate = goal.targetDate?.epochSeconds,
                        startDate = goal.startDate.epochSeconds,
                        completedDate = goal.completedDate?.epochSeconds,
                        isActive = if (goal.isActive) 1 else 0,
                        isCompleted = if (goal.isCompleted) 1 else 0,
                        categoryId = goal.categoryId,
                        iconName = goal.iconName,
                        colorHex = goal.colorHex,
                        reminderEnabled = if (goal.reminderEnabled) 1 else 0,
                        reminderFrequency = goal.reminderFrequency?.name,
                        autoContributionEnabled = if (goal.autoContribution != null) 1 else 0,
                        autoContributionAmount = goal.autoContribution?.amount?.amount,
                        autoContributionFrequency = goal.autoContribution?.frequency?.name,
                        createdAt = goal.createdAt.epochSeconds,
                        updatedAt = goal.updatedAt.epochSeconds,
                        metadata = serializeMetadata(goal.metadata)
                    )
                    
                    // Insert milestones
                    goal.milestones.forEach { milestone ->
                        goalDao.insertGoalMilestone(
                            milestoneId = milestone.milestoneId,
                            goalId = goal.goalId,
                            name = milestone.name,
                            description = milestone.description,
                            targetAmount = milestone.targetAmount.amount,
                            targetDate = milestone.targetDate?.epochSeconds,
                            isAchieved = if (milestone.isAchieved) 1 else 0,
                            achievedDate = milestone.achievedDate?.epochSeconds,
                            order_ = milestone.order.toLong(),
                            rewardDescription = milestone.rewardDescription,
                            createdAt = milestone.createdAt.epochSeconds
                        )
                    }
                }
                Result.success(goal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateGoal(goal: Goal): Result<Goal> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    goalDao.updateGoal(
                        name = goal.name,
                        description = goal.description,
                        goalType = goal.goalType.name,
                        priority = goal.priority.name,
                        targetAmount = goal.targetAmount.amount,
                        currentAmount = goal.currentAmount.amount,
                        currency = goal.currency,
                        targetDate = goal.targetDate?.epochSeconds,
                        startDate = goal.startDate.epochSeconds,
                        completedDate = goal.completedDate?.epochSeconds,
                        isActive = if (goal.isActive) 1 else 0,
                        isCompleted = if (goal.isCompleted) 1 else 0,
                        categoryId = goal.categoryId,
                        iconName = goal.iconName,
                        colorHex = goal.colorHex,
                        reminderEnabled = if (goal.reminderEnabled) 1 else 0,
                        reminderFrequency = goal.reminderFrequency?.name,
                        autoContributionEnabled = if (goal.autoContribution != null) 1 else 0,
                        autoContributionAmount = goal.autoContribution?.amount?.amount,
                        autoContributionFrequency = goal.autoContribution?.frequency?.name,
                        updatedAt = Clock.System.now().epochSeconds,
                        metadata = serializeMetadata(goal.metadata),
                        goalId = goal.goalId
                    )
                    
                    // Update milestones
                    goalDao.deleteGoalMilestones(goal.goalId)
                    goal.milestones.forEach { milestone ->
                        goalDao.insertGoalMilestone(
                            milestoneId = milestone.milestoneId,
                            goalId = goal.goalId,
                            name = milestone.name,
                            description = milestone.description,
                            targetAmount = milestone.targetAmount.amount,
                            targetDate = milestone.targetDate?.epochSeconds,
                            isAchieved = if (milestone.isAchieved) 1 else 0,
                            achievedDate = milestone.achievedDate?.epochSeconds,
                            order_ = milestone.order.toLong(),
                            rewardDescription = milestone.rewardDescription,
                            createdAt = milestone.createdAt.epochSeconds
                        )
                    }
                }
                Result.success(goal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    goalDao.deleteGoalContributions(goalId)
                    goalDao.deleteGoalMilestones(goalId)
                    goalDao.deleteGoal(goalId)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalById(goalId: String): Result<Goal?> {
        return withContext(Dispatchers.Default) {
            try {
                val goal = goalDao.selectById(goalId).executeAsOneOrNull()?.toDomainModel()
                Result.success(goal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getActiveGoals(): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectActiveGoals()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAllGoals(): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectAll()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalsByType(type: GoalType): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectByType(type.name)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalsByPriority(priority: GoalPriority): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectByPriority(priority.name)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCompletedGoals(): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectCompletedGoals()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getOverdueGoals(): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val now = Clock.System.now().epochSeconds
                val goals = goalDao.selectOverdueGoals(now)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalsNearCompletion(thresholdPercentage: Double): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectGoalsNearCompletion(thresholdPercentage)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalPerformanceSummaries(): Result<List<GoalPerformanceSummary>> {
        return withContext(Dispatchers.Default) {
            try {
                val summaries = goalDao.selectPerformanceSummaries()
                    .executeAsList()
                    .map { it.toPerformanceSummary() }
                Result.success(summaries)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addGoalContribution(contribution: GoalContribution): Result<GoalContribution> {
        return withContext(Dispatchers.Default) {
            try {
                database.withTransaction {
                    goalDao.insertGoalContribution(
                        contributionId = contribution.contributionId,
                        goalId = contribution.goalId,
                        amount = contribution.amount.amount,
                        currency = contribution.amount.currency,
                        contributionDate = contribution.contributionDate.epochSeconds,
                        description = contribution.description,
                        source = contribution.source.name,
                        transactionId = contribution.transactionId,
                        createdAt = contribution.createdAt.epochSeconds
                    )
                    
                    // Update goal current amount
                    val goal = goalDao.selectById(contribution.goalId).executeAsOneOrNull()
                    if (goal != null) {
                        val newAmount = Money(goal.currentAmount, contribution.amount.currency) + contribution.amount
                        goalDao.updateGoalProgress(
                            currentAmount = newAmount.amount,
                            updatedAt = Clock.System.now().epochSeconds,
                            goalId = contribution.goalId
                        )
                        
                        // Check if goal is completed
                        if (newAmount >= Money(goal.targetAmount, contribution.amount.currency)) {
                            goalDao.completeGoal(
                                isCompleted = 1,
                                completedDate = Clock.System.now().epochSeconds,
                                updatedAt = Clock.System.now().epochSeconds,
                                goalId = contribution.goalId
                            )
                        }
                    }
                }
                Result.success(contribution)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalContributions(goalId: String): Result<List<GoalContribution>> {
        return withContext(Dispatchers.Default) {
            try {
                val contributions = goalDao.selectGoalContributions(goalId)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(contributions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getRecentContributions(limit: Int): Result<List<GoalContribution>> {
        return withContext(Dispatchers.Default) {
            try {
                val contributions = goalDao.selectRecentContributions(limit.toLong())
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(contributions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateGoalProgress(goalId: String, currentAmount: Money): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.updateGoalProgress(
                    currentAmount = currentAmount.amount,
                    updatedAt = Clock.System.now().epochSeconds,
                    goalId = goalId
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun completeGoal(goalId: String, completedAt: Instant): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.completeGoal(
                    isCompleted = 1,
                    completedDate = completedAt.epochSeconds,
                    updatedAt = Clock.System.now().epochSeconds,
                    goalId = goalId
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun setGoalActiveStatus(goalId: String, isActive: Boolean): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.setGoalActiveStatus(
                    isActive = if (isActive) 1 else 0,
                    updatedAt = Clock.System.now().epochSeconds,
                    goalId = goalId
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addMilestone(milestone: GoalMilestone): Result<GoalMilestone> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.insertGoalMilestone(
                    milestoneId = milestone.milestoneId,
                    goalId = milestone.goalId,
                    name = milestone.name,
                    description = milestone.description,
                    targetAmount = milestone.targetAmount.amount,
                    targetDate = milestone.targetDate?.epochSeconds,
                    isAchieved = if (milestone.isAchieved) 1 else 0,
                    achievedDate = milestone.achievedDate?.epochSeconds,
                    order_ = milestone.order.toLong(),
                    rewardDescription = milestone.rewardDescription,
                    createdAt = milestone.createdAt.epochSeconds
                )
                Result.success(milestone)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateMilestone(milestone: GoalMilestone): Result<GoalMilestone> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.updateGoalMilestone(
                    name = milestone.name,
                    description = milestone.description,
                    targetAmount = milestone.targetAmount.amount,
                    targetDate = milestone.targetDate?.epochSeconds,
                    isAchieved = if (milestone.isAchieved) 1 else 0,
                    achievedDate = milestone.achievedDate?.epochSeconds,
                    order_ = milestone.order.toLong(),
                    rewardDescription = milestone.rewardDescription,
                    milestoneId = milestone.milestoneId
                )
                Result.success(milestone)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun achieveMilestone(milestoneId: String, achievedAt: Instant): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.achieveMilestone(
                    isAchieved = 1,
                    achievedDate = achievedAt.epochSeconds,
                    milestoneId = milestoneId
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalMilestones(goalId: String): Result<List<GoalMilestone>> {
        return withContext(Dispatchers.Default) {
            try {
                val milestones = goalDao.selectGoalMilestones(goalId)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(milestones)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getNextMilestone(goalId: String): Result<GoalMilestone?> {
        return withContext(Dispatchers.Default) {
            try {
                val milestone = goalDao.selectNextMilestone(goalId).executeAsOneOrNull()?.toDomainModel()
                Result.success(milestone)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchGoals(query: String): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.searchGoals(query, query)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalsWithAutoContribution(): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val goals = goalDao.selectGoalsWithAutoContribution()
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateAutoContribution(goalId: String, autoContribution: AutoContribution?): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                goalDao.updateAutoContribution(
                    autoContributionEnabled = if (autoContribution != null) 1 else 0,
                    autoContributionAmount = autoContribution?.amount?.amount,
                    autoContributionFrequency = autoContribution?.frequency?.name,
                    updatedAt = Clock.System.now().epochSeconds,
                    goalId = goalId
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalsRequiringAttention(): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val now = Clock.System.now().epochSeconds
                val goals = goalDao.selectGoalsRequiringAttention(now)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTotalTargetAmount(): Result<Money?> {
        return withContext(Dispatchers.Default) {
            try {
                val total = goalDao.getTotalTargetAmount().executeAsOneOrNull()
                val amount = total?.let { Money(it, "SAR") }
                Result.success(amount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTotalCurrentAmount(): Result<Money?> {
        return withContext(Dispatchers.Default) {
            try {
                val total = goalDao.getTotalCurrentAmount().executeAsOneOrNull()
                val amount = total?.let { Money(it, "SAR") }
                Result.success(amount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTotalContributions(startDate: Instant, endDate: Instant): Result<Money?> {
        return withContext(Dispatchers.Default) {
            try {
                val total = goalDao.getTotalContributions(
                    startDate.epochSeconds,
                    endDate.epochSeconds
                ).executeAsOneOrNull()
                val amount = total?.let { Money(it, "SAR") }
                Result.success(amount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalsDueWithin(days: Int): Result<List<Goal>> {
        return withContext(Dispatchers.Default) {
            try {
                val cutoffDate = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY * days).epochSeconds
                val goals = goalDao.selectGoalsDueWithin(cutoffDate)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalContributionStats(goalId: String): Result<GoalContributionStats> {
        return withContext(Dispatchers.Default) {
            try {
                val stats = goalDao.selectGoalContributionStats(goalId).executeAsOneOrNull()
                if (stats != null) {
                    val contributionStats = GoalContributionStats(
                        goalId = goalId,
                        totalContributions = Money(stats.totalContributions ?: "0", "SAR"),
                        contributionCount = (stats.contributionCount ?: 0).toInt(),
                        averageContribution = Money(stats.averageContribution ?: "0", "SAR"),
                        largestContribution = Money(stats.largestContribution ?: "0", "SAR"),
                        smallestContribution = Money(stats.smallestContribution ?: "0", "SAR"),
                        lastContributionDate = stats.lastContributionDate?.let { Instant.fromEpochSeconds(it) },
                        monthlyAverage = Money(stats.monthlyAverage ?: "0", "SAR"),
                        contributionFrequency = stats.contributionFrequency ?: 0.0
                    )
                    Result.success(contributionStats)
                } else {
                    Result.success(
                        GoalContributionStats(
                            goalId = goalId,
                            totalContributions = Money.fromInt(0, "SAR"),
                            contributionCount = 0,
                            averageContribution = Money.fromInt(0, "SAR"),
                            largestContribution = Money.fromInt(0, "SAR"),
                            smallestContribution = Money.fromInt(0, "SAR"),
                            lastContributionDate = null,
                            monthlyAverage = Money.fromInt(0, "SAR"),
                            contributionFrequency = 0.0
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGoalProgressHistory(goalId: String): Result<List<GoalProgressSnapshot>> {
        return withContext(Dispatchers.Default) {
            try {
                val snapshots = goalDao.selectGoalProgressHistory(goalId)
                    .executeAsList()
                    .map { it.toDomainModel() }
                Result.success(snapshots)
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
private suspend fun code.yousef.dari.shared.database.Goal.toDomainModel(): Goal {
    return Goal(
        goalId = goalId,
        name = name,
        description = description,
        goalType = GoalType.valueOf(goalType),
        priority = GoalPriority.valueOf(priority),
        targetAmount = Money(targetAmount, currency),
        currentAmount = Money(currentAmount, currency),
        currency = currency,
        targetDate = targetDate?.let { Instant.fromEpochSeconds(it) },
        startDate = Instant.fromEpochSeconds(startDate),
        completedDate = completedDate?.let { Instant.fromEpochSeconds(it) },
        isActive = isActive == 1L,
        isCompleted = isCompleted == 1L,
        categoryId = categoryId,
        iconName = iconName,
        colorHex = colorHex,
        reminderEnabled = reminderEnabled == 1L,
        reminderFrequency = reminderFrequency?.let { ReminderFrequency.valueOf(it) },
        autoContribution = if (autoContributionEnabled == 1L && autoContributionAmount != null && autoContributionFrequency != null) {
            AutoContribution(
                amount = Money(autoContributionAmount, currency),
                frequency = AutoContributionFrequency.valueOf(autoContributionFrequency)
            )
        } else null,
        milestones = emptyList(), // Would be loaded separately
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        metadata = deserializeMetadata(metadata ?: "{}")
    )
}

private fun code.yousef.dari.shared.database.SelectPerformanceSummaries.toPerformanceSummary(): GoalPerformanceSummary {
    return GoalPerformanceSummary(
        goalId = goalId,
        goalName = name,
        targetAmount = Money(targetAmount, currency),
        currentAmount = Money(currentAmount, currency),
        progressPercentage = if (targetAmount != "0") {
            (currentAmount.toDouble() / targetAmount.toDouble()) * 100.0
        } else 0.0,
        daysRemaining = targetDate?.let { calculateDaysRemaining(it) },
        status = when {
            isCompleted == 1L -> GoalStatus.COMPLETED
            targetDate?.let { Clock.System.now().epochSeconds > it } == true -> GoalStatus.OVERDUE
            currentAmount.toDouble() >= targetAmount.toDouble() * 0.9 -> GoalStatus.NEAR_COMPLETION
            else -> GoalStatus.ON_TRACK
        },
        isActive = isActive == 1L
    )
}

private fun code.yousef.dari.shared.database.GoalContribution.toDomainModel(): GoalContribution {
    return GoalContribution(
        contributionId = contributionId,
        goalId = goalId,
        amount = Money(amount, currency),
        contributionDate = Instant.fromEpochSeconds(contributionDate),
        description = description,
        source = ContributionSource.valueOf(source),
        transactionId = transactionId,
        createdAt = Instant.fromEpochSeconds(createdAt)
    )
}

private fun code.yousef.dari.shared.database.GoalMilestone.toDomainModel(): GoalMilestone {
    return GoalMilestone(
        milestoneId = milestoneId,
        goalId = goalId,
        name = name,
        description = description,
        targetAmount = Money(targetAmount, "SAR"), // Default currency
        targetDate = targetDate?.let { Instant.fromEpochSeconds(it) },
        isAchieved = isAchieved == 1L,
        achievedDate = achievedDate?.let { Instant.fromEpochSeconds(it) },
        order = order_.toInt(),
        rewardDescription = rewardDescription,
        createdAt = Instant.fromEpochSeconds(createdAt)
    )
}

private fun code.yousef.dari.shared.database.SelectGoalProgressHistory.toDomainModel(): GoalProgressSnapshot {
    return GoalProgressSnapshot(
        goalId = goalId,
        currentAmount = Money(currentAmount, currency),
        progressPercentage = if (targetAmount != "0") {
            (currentAmount.toDouble() / targetAmount.toDouble()) * 100.0
        } else 0.0,
        snapshotDate = Instant.fromEpochSeconds(snapshotDate),
        daysRemaining = targetDate?.let { calculateDaysRemaining(it) },
        projectedCompletionDate = calculateProjectedCompletion(currentAmount, targetAmount, snapshotDate)
    )
}

private fun calculateDaysRemaining(targetDateSeconds: Long): Int {
    val targetDate = Instant.fromEpochSeconds(targetDateSeconds)
    val now = Clock.System.now()
    val duration = targetDate - now
    return maxOf(0, duration.inWholeDays.toInt())
}

private fun calculateProjectedCompletion(currentAmount: String, targetAmount: String, snapshotDateSeconds: Long): Instant? {
    // Simple projection based on current progress - would be more sophisticated in production
    return try {
        val current = currentAmount.toDouble()
        val target = targetAmount.toDouble()
        val progressPercentage = if (target > 0) current / target else 0.0
        
        if (progressPercentage >= 1.0) {
            Instant.fromEpochSeconds(snapshotDateSeconds) // Already completed
        } else if (progressPercentage > 0) {
            val estimatedDaysToCompletion = ((1.0 - progressPercentage) / progressPercentage) * 30 // Rough estimate
            Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.DAY * estimatedDaysToCompletion.toInt())
        } else {
            null // No progress yet
        }
    } catch (e: Exception) {
        null
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