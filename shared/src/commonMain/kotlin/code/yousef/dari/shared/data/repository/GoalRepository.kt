package code.yousef.dari.shared.data.repository

import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.Instant

/**
 * Repository interface for Goal data operations
 * Handles CRUD operations and goal-specific queries
 */
interface GoalRepository {
    
    /**
     * Create a new goal
     */
    suspend fun createGoal(goal: Goal): Result<Goal>
    
    /**
     * Update an existing goal
     */
    suspend fun updateGoal(goal: Goal): Result<Goal>
    
    /**
     * Delete a goal
     */
    suspend fun deleteGoal(goalId: String): Result<Unit>
    
    /**
     * Get goal by ID
     */
    suspend fun getGoalById(goalId: String): Result<Goal?>
    
    /**
     * Get all active goals
     */
    suspend fun getActiveGoals(): Result<List<Goal>>
    
    /**
     * Get all goals (active and inactive)
     */
    suspend fun getAllGoals(): Result<List<Goal>>
    
    /**
     * Get goals by type
     */
    suspend fun getGoalsByType(type: GoalType): Result<List<Goal>>
    
    /**
     * Get goals by priority
     */
    suspend fun getGoalsByPriority(priority: GoalPriority): Result<List<Goal>>
    
    /**
     * Get completed goals
     */
    suspend fun getCompletedGoals(): Result<List<Goal>>
    
    /**
     * Get overdue goals
     */
    suspend fun getOverdueGoals(): Result<List<Goal>>
    
    /**
     * Get goals near completion (within percentage)
     */
    suspend fun getGoalsNearCompletion(thresholdPercentage: Double = 90.0): Result<List<Goal>>
    
    /**
     * Get goal performance summaries for dashboard
     */
    suspend fun getGoalPerformanceSummaries(): Result<List<GoalPerformanceSummary>>
    
    /**
     * Add contribution to goal
     */
    suspend fun addGoalContribution(contribution: GoalContribution): Result<GoalContribution>
    
    /**
     * Get contributions for a goal
     */
    suspend fun getGoalContributions(goalId: String): Result<List<GoalContribution>>
    
    /**
     * Get recent contributions across all goals
     */
    suspend fun getRecentContributions(limit: Int = 10): Result<List<GoalContribution>>
    
    /**
     * Update goal progress (current amount)
     */
    suspend fun updateGoalProgress(goalId: String, currentAmount: Money): Result<Unit>
    
    /**
     * Mark goal as completed
     */
    suspend fun completeGoal(goalId: String, completedAt: Instant): Result<Unit>
    
    /**
     * Pause/resume goal
     */
    suspend fun setGoalActiveStatus(goalId: String, isActive: Boolean): Result<Unit>
    
    /**
     * Add milestone to goal
     */
    suspend fun addMilestone(milestone: GoalMilestone): Result<GoalMilestone>
    
    /**
     * Update milestone
     */
    suspend fun updateMilestone(milestone: GoalMilestone): Result<GoalMilestone>
    
    /**
     * Mark milestone as achieved
     */
    suspend fun achieveMilestone(milestoneId: String, achievedAt: Instant): Result<Unit>
    
    /**
     * Get milestones for a goal
     */
    suspend fun getGoalMilestones(goalId: String): Result<List<GoalMilestone>>
    
    /**
     * Get next unachieved milestone for goal
     */
    suspend fun getNextMilestone(goalId: String): Result<GoalMilestone?>
    
    /**
     * Search goals by name
     */
    suspend fun searchGoals(query: String): Result<List<Goal>>
    
    /**
     * Get goals with auto contribution enabled
     */
    suspend fun getGoalsWithAutoContribution(): Result<List<Goal>>
    
    /**
     * Update auto contribution settings
     */
    suspend fun updateAutoContribution(goalId: String, autoContribution: AutoContribution?): Result<Unit>
    
    /**
     * Get goals requiring attention (overdue, at risk, etc.)
     */
    suspend fun getGoalsRequiringAttention(): Result<List<Goal>>
    
    /**
     * Get total target amount across all active goals
     */
    suspend fun getTotalTargetAmount(): Result<Money?>
    
    /**
     * Get total current amount across all active goals
     */
    suspend fun getTotalCurrentAmount(): Result<Money?>
    
    /**
     * Get total contributions for period
     */
    suspend fun getTotalContributions(
        startDate: Instant,
        endDate: Instant
    ): Result<Money?>
    
    /**
     * Get goals due within specified days
     */
    suspend fun getGoalsDueWithin(days: Int): Result<List<Goal>>
    
    /**
     * Get contribution statistics for goal
     */
    suspend fun getGoalContributionStats(goalId: String): Result<GoalContributionStats>
    
    /**
     * Get goal progress history (snapshots over time)
     */
    suspend fun getGoalProgressHistory(goalId: String): Result<List<GoalProgressSnapshot>>
}

/**
 * Goal contribution statistics
 */
data class GoalContributionStats(
    val goalId: String,
    val totalContributions: Money,
    val contributionCount: Int,
    val averageContribution: Money,
    val largestContribution: Money,
    val smallestContribution: Money,
    val lastContributionDate: Instant?,
    val monthlyAverage: Money,
    val contributionFrequency: Double // contributions per month
)

/**
 * Goal progress snapshot for tracking over time
 */
data class GoalProgressSnapshot(
    val goalId: String,
    val currentAmount: Money,
    val progressPercentage: Double,
    val snapshotDate: Instant,
    val daysRemaining: Int?,
    val projectedCompletionDate: Instant?
)