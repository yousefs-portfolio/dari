package code.yousef.dari.shared.domain.usecase.goal

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.data.repository.GoalRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Use case for creating a new financial goal
 * Validates goal data and creates with milestones and auto-contribution setup
 */
class CreateGoalUseCase(
    private val goalRepository: GoalRepository
) {
    
    /**
     * Create a new financial goal
     */
    suspend fun execute(request: CreateGoalRequest): Result<Goal> {
        return try {
            // Validate request
            val validationResult = validateRequest(request)
            if (validationResult.isFailure) {
                return validationResult
            }
            
            // Create goal with validated data
            val goal = createGoalFromRequest(request)
            
            // Save to repository
            val savedGoal = goalRepository.createGoal(goal).getOrThrow()
            
            Result.success(savedGoal)
        } catch (e: Exception) {
            Result.failure(GoalCreationException("Failed to create goal: ${e.message}", e))
        }
    }
    
    private fun validateRequest(request: CreateGoalRequest): Result<Goal> {
        // Validate basic fields
        if (request.name.isBlank()) {
            return Result.failure(GoalValidationException("Goal name cannot be blank"))
        }
        
        if (request.targetAmount.isNegative() || request.targetAmount.isZero()) {
            return Result.failure(GoalValidationException("Target amount must be positive"))
        }
        
        // Validate dates
        val now = Clock.System.now()
        if (request.targetDate != null && request.targetDate <= now) {
            return Result.failure(GoalValidationException("Target date must be in the future"))
        }
        
        if (request.startDate > now.plus(kotlin.time.Duration.parse("P30D"))) {
            return Result.failure(GoalValidationException("Start date cannot be more than 30 days in the future"))
        }
        
        // Validate current amount
        if (request.currentAmount.isNegative()) {
            return Result.failure(GoalValidationException("Current amount cannot be negative"))
        }
        
        if (request.currentAmount > request.targetAmount) {
            return Result.failure(GoalValidationException("Current amount cannot exceed target amount"))
        }
        
        // Validate currency consistency
        if (request.currentAmount.currency != request.targetAmount.currency) {
            return Result.failure(GoalValidationException("Current and target amounts must use the same currency"))
        }
        
        // Validate auto contribution settings
        if (request.autoContribution != null) {
            val autoValidation = validateAutoContribution(request.autoContribution, request.targetDate)
            if (autoValidation.isFailure) {
                return autoValidation
            }
        }
        
        // Validate milestones
        if (request.milestones.isNotEmpty()) {
            val milestoneValidation = validateMilestones(request.milestones, request.targetAmount)
            if (milestoneValidation.isFailure) {
                return milestoneValidation
            }
        }
        
        // Validate goal type specific requirements
        val typeValidation = validateGoalType(request.goalType, request.targetAmount, request.targetDate)
        if (typeValidation.isFailure) {
            return typeValidation
        }
        
        return Result.success(createGoalFromRequest(request))
    }
    
    private fun validateAutoContribution(
        autoContribution: CreateAutoContributionRequest,
        targetDate: Instant?
    ): Result<Unit> {
        if (autoContribution.amount.isNegative() || autoContribution.amount.isZero()) {
            return Result.failure(GoalValidationException("Auto contribution amount must be positive"))
        }
        
        val now = Clock.System.now()
        if (autoContribution.nextContributionDate <= now) {
            return Result.failure(GoalValidationException("Next contribution date must be in the future"))
        }
        
        if (autoContribution.endDate != null) {
            if (autoContribution.endDate <= autoContribution.nextContributionDate) {
                return Result.failure(GoalValidationException("End date must be after next contribution date"))
            }
            
            if (targetDate != null && autoContribution.endDate > targetDate) {
                return Result.failure(GoalValidationException("Auto contribution end date cannot be after goal target date"))
            }
        }
        
        return Result.success(Unit)
    }
    
    private fun validateMilestones(
        milestones: List<CreateGoalMilestoneRequest>,
        targetAmount: Money
    ): Result<Unit> {
        // Check for duplicate milestone amounts
        val amounts = milestones.map { it.targetAmount.numericValue }.toSet()
        if (amounts.size != milestones.size) {
            return Result.failure(GoalValidationException("Milestone target amounts must be unique"))
        }
        
        // Validate each milestone
        for (milestone in milestones) {
            if (milestone.name.isBlank()) {
                return Result.failure(GoalValidationException("Milestone name cannot be blank"))
            }
            
            if (milestone.targetAmount.isNegative() || milestone.targetAmount.isZero()) {
                return Result.failure(GoalValidationException("Milestone amount must be positive"))
            }
            
            if (milestone.targetAmount >= targetAmount) {
                return Result.failure(GoalValidationException(
                    "Milestone amount (${milestone.targetAmount.format()}) must be less than target amount (${targetAmount.format()})"
                ))
            }
            
            if (milestone.targetAmount.currency != targetAmount.currency) {
                return Result.failure(GoalValidationException("Milestone currency must match goal currency"))
            }
        }
        
        return Result.success(Unit)
    }
    
    private fun validateGoalType(
        goalType: GoalType,
        targetAmount: Money,
        targetDate: Instant?
    ): Result<Unit> {
        when (goalType) {
            GoalType.EMERGENCY_FUND -> {
                // Emergency fund should be 3-6 months of expenses (typically SAR 15,000 - 50,000)
                val minEmergencyFund = Money.fromInt(5000, targetAmount.currency)
                if (targetAmount < minEmergencyFund) {
                    return Result.failure(GoalValidationException(
                        "Emergency fund should be at least ${minEmergencyFund.format()}"
                    ))
                }
            }
            
            GoalType.DEBT_PAYOFF -> {
                if (targetDate == null) {
                    return Result.failure(GoalValidationException("Debt payoff goals must have a target date"))
                }
            }
            
            GoalType.RETIREMENT -> {
                if (targetDate == null) {
                    return Result.failure(GoalValidationException("Retirement goals must have a target date"))
                }
                
                // Retirement goals should have substantial amounts
                val minRetirement = Money.fromInt(100000, targetAmount.currency)
                if (targetAmount < minRetirement) {
                    return Result.failure(GoalValidationException(
                        "Retirement goals should be at least ${minRetirement.format()}"
                    ))
                }
            }
            
            GoalType.HAJJ_UMRAH -> {
                // Hajj/Umrah typically costs 15,000-30,000 SAR
                if (targetAmount.currency == "SAR") {
                    val minHajjAmount = Money.fromInt(10000, "SAR")
                    val maxHajjAmount = Money.fromInt(50000, "SAR")
                    if (targetAmount < minHajjAmount || targetAmount > maxHajjAmount) {
                        // This is a warning, not an error - allow flexibility
                    }
                }
            }
            
            else -> {
                // No specific validation for other goal types
            }
        }
        
        return Result.success(Unit)
    }
    
    private fun createGoalFromRequest(request: CreateGoalRequest): Goal {
        val now = Clock.System.now()
        
        // Create milestones
        val milestones = request.milestones.map { milestoneRequest ->
            GoalMilestone(
                milestoneId = generateMilestoneId(),
                goalId = "", // Will be set when goal is saved
                name = milestoneRequest.name,
                targetAmount = milestoneRequest.targetAmount,
                rewardDescription = milestoneRequest.rewardDescription,
                isAchieved = false,
                achievedAt = null,
                createdAt = now
            )
        }
        
        // Create auto contribution
        val autoContribution = request.autoContribution?.let { autoRequest ->
            AutoContribution(
                enabled = autoRequest.enabled,
                amount = autoRequest.amount,
                frequency = autoRequest.frequency,
                sourceAccountId = autoRequest.sourceAccountId,
                nextContributionDate = autoRequest.nextContributionDate,
                endDate = autoRequest.endDate
            )
        }
        
        return Goal(
            goalId = generateGoalId(),
            name = request.name,
            description = request.description,
            goalType = request.goalType,
            targetAmount = request.targetAmount,
            currentAmount = request.currentAmount,
            currency = request.targetAmount.currency,
            targetDate = request.targetDate,
            startDate = request.startDate,
            isActive = true,
            priority = request.priority,
            strategy = request.strategy,
            autoContribution = autoContribution,
            visualSettings = request.visualSettings,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            contributions = emptyList(),
            milestones = milestones,
            metadata = request.metadata
        )
    }
    
    private fun generateGoalId(): String {
        return "goal_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
    
    private fun generateMilestoneId(): String {
        return "milestone_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}

/**
 * Request data for creating a goal
 */
data class CreateGoalRequest(
    val name: String,
    val description: String? = null,
    val goalType: GoalType,
    val targetAmount: Money,
    val currentAmount: Money = Money.fromInt(0, targetAmount.currency),
    val targetDate: Instant? = null,
    val startDate: Instant = Clock.System.now(),
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val strategy: GoalStrategy,
    val autoContribution: CreateAutoContributionRequest? = null,
    val visualSettings: GoalVisualSettings = GoalVisualSettings(),
    val milestones: List<CreateGoalMilestoneRequest> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Request data for creating auto contribution
 */
data class CreateAutoContributionRequest(
    val enabled: Boolean = true,
    val amount: Money,
    val frequency: ContributionFrequency,
    val sourceAccountId: String? = null,
    val nextContributionDate: Instant,
    val endDate: Instant? = null
)

/**
 * Request data for creating goal milestones
 */
data class CreateGoalMilestoneRequest(
    val name: String,
    val targetAmount: Money,
    val rewardDescription: String? = null
)

/**
 * Exception for goal creation errors
 */
class GoalCreationException(
    message: String, 
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception for goal validation errors
 */
class GoalValidationException(
    message: String
) : Exception(message)