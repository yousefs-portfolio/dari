package code.yousef.dari.shared.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Financial Goal Domain Model
 * Represents savings goals, debt payoff goals, and investment targets
 * Supports SMART goal methodology and Islamic finance principles
 */
@Serializable
data class Goal(
    val goalId: String,
    val name: String,
    val description: String?,
    val goalType: GoalType,
    val targetAmount: Money,
    val currentAmount: Money = Money.fromInt(0, targetAmount.currency),
    val currency: String = targetAmount.currency,
    val targetDate: Instant?,
    val startDate: Instant = Clock.System.now(),
    val isActive: Boolean = true,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val strategy: GoalStrategy,
    val autoContribution: AutoContribution? = null,
    val visualSettings: GoalVisualSettings = GoalVisualSettings(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val completedAt: Instant? = null,
    val contributions: List<GoalContribution> = emptyList(),
    val milestones: List<GoalMilestone> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    
    /**
     * Calculate progress percentage (0-100)
     */
    fun getProgressPercentage(): Double {
        if (targetAmount.isZero()) return 0.0
        return (currentAmount.numericValue / targetAmount.numericValue) * 100.0
    }
    
    /**
     * Calculate remaining amount to reach target
     */
    fun getRemainingAmount(): Money {
        return targetAmount - currentAmount
    }
    
    /**
     * Check if goal is completed
     */
    fun isCompleted(): Boolean {
        return currentAmount >= targetAmount || completedAt != null
    }
    
    /**
     * Check if goal is overdue
     */
    fun isOverdue(): Boolean {
        val target = targetDate ?: return false
        return Clock.System.now() > target && !isCompleted()
    }
    
    /**
     * Calculate days remaining until target date
     */
    fun getDaysRemaining(): Int? {
        val target = targetDate ?: return null
        val now = Clock.System.now()
        return if (now < target) {
            (target - now).inWholeDays.toInt()
        } else {
            0
        }
    }
    
    /**
     * Calculate days since goal started
     */
    fun getDaysActive(): Int {
        val now = Clock.System.now()
        return (now - startDate).inWholeDays.toInt()
    }
    
    /**
     * Calculate required monthly contribution to reach target
     */
    fun getRequiredMonthlyContribution(): Money? {
        val target = targetDate ?: return null
        val remaining = getRemainingAmount()
        val now = Clock.System.now()
        
        if (target <= now) return remaining // Overdue, need full amount
        
        val monthsRemaining = (target - now).inWholeDays / 30.0
        if (monthsRemaining <= 0) return remaining
        
        return Money.fromDouble(remaining.numericValue / monthsRemaining, currency)
    }
    
    /**
     * Calculate required weekly contribution to reach target
     */
    fun getRequiredWeeklyContribution(): Money? {
        val target = targetDate ?: return null
        val remaining = getRemainingAmount()
        val now = Clock.System.now()
        
        if (target <= now) return remaining
        
        val weeksRemaining = (target - now).inWholeDays / 7.0
        if (weeksRemaining <= 0) return remaining
        
        return Money.fromDouble(remaining.numericValue / weeksRemaining, currency)
    }
    
    /**
     * Calculate average contribution amount
     */
    fun getAverageContribution(): Money {
        if (contributions.isEmpty()) return Money.fromInt(0, currency)
        
        val total = contributions.fold(Money.fromInt(0, currency)) { acc, contribution ->
            acc + contribution.amount
        }
        
        return total / contributions.size
    }
    
    /**
     * Get goal status
     */
    fun getGoalStatus(): GoalStatus {
        return when {
            isCompleted() -> GoalStatus.COMPLETED
            !isActive -> GoalStatus.PAUSED
            isOverdue() -> GoalStatus.OVERDUE
            getProgressPercentage() >= 90 -> GoalStatus.NEARLY_COMPLETE
            getProgressPercentage() >= 50 -> GoalStatus.ON_TRACK
            getDaysActive() > 30 && getProgressPercentage() < 10 -> GoalStatus.AT_RISK
            else -> GoalStatus.ACTIVE
        }
    }
    
    /**
     * Calculate projected completion date based on current pace
     */
    fun getProjectedCompletionDate(): Instant? {
        if (isCompleted()) return completedAt
        
        val recentContributions = contributions
            .filter { it.createdAt >= Clock.System.now() - Duration.parse("P30D") }
            .sortedByDescending { it.createdAt }
        
        if (recentContributions.isEmpty()) return targetDate
        
        val totalRecentContributions = recentContributions.fold(Money.fromInt(0, currency)) { acc, contribution ->
            acc + contribution.amount
        }
        
        val avgDailyContribution = totalRecentContributions.numericValue / 30.0
        if (avgDailyContribution <= 0) return targetDate
        
        val remaining = getRemainingAmount().numericValue
        val daysToComplete = (remaining / avgDailyContribution).toInt()
        
        return Clock.System.now() + Duration.parse("P${daysToComplete}D")
    }
    
    /**
     * Check if goal is on track to meet target date
     */
    fun isOnTrack(): Boolean {
        val target = targetDate ?: return true
        val projected = getProjectedCompletionDate() ?: return false
        return projected <= target
    }
    
    /**
     * Get next milestone to achieve
     */
    fun getNextMilestone(): GoalMilestone? {
        return milestones
            .filter { !it.isAchieved && currentAmount >= it.targetAmount }
            .minByOrNull { it.targetAmount.numericValue }
    }
    
    /**
     * Calculate milestone progress
     */
    fun getMilestoneProgress(): Double {
        val achieved = milestones.count { it.isAchieved }
        if (milestones.isEmpty()) return 0.0
        return (achieved.toDouble() / milestones.size) * 100.0
    }
    
    /**
     * Get goal performance summary
     */
    fun getPerformanceSummary(): GoalPerformanceSummary {
        return GoalPerformanceSummary(
            goalId = goalId,
            goalName = name,
            goalType = goalType,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            progressPercentage = getProgressPercentage(),
            remainingAmount = getRemainingAmount(),
            daysRemaining = getDaysRemaining(),
            status = getGoalStatus(),
            isOnTrack = isOnTrack(),
            projectedCompletionDate = getProjectedCompletionDate(),
            requiredMonthlyContribution = getRequiredMonthlyContribution(),
            averageContribution = getAverageContribution(),
            milestonesAchieved = milestones.count { it.isAchieved },
            totalMilestones = milestones.size
        )
    }
    
    /**
     * Calculate compound interest if applicable
     */
    fun calculateCompoundInterest(annualRate: Double, compoundingFrequency: Int = 12): Money {
        if (goalType != GoalType.SAVINGS && goalType != GoalType.INVESTMENT) {
            return currentAmount
        }
        
        val monthsRemaining = getDaysRemaining()?.let { it / 30.0 } ?: 0.0
        val yearsRemaining = monthsRemaining / 12.0
        
        if (yearsRemaining <= 0) return currentAmount
        
        val monthlyContribution = getRequiredMonthlyContribution()?.numericValue ?: 0.0
        val principal = currentAmount.numericValue
        val rate = annualRate / 100.0
        
        // Compound interest with regular contributions
        val periodicRate = rate / compoundingFrequency
        val totalPeriods = compoundingFrequency * yearsRemaining
        
        val futureValuePrincipal = principal * Math.pow(1 + periodicRate, totalPeriods)
        val futureValueContributions = if (monthlyContribution > 0) {
            monthlyContribution * ((Math.pow(1 + periodicRate, totalPeriods) - 1) / periodicRate)
        } else {
            0.0
        }
        
        return Money.fromDouble(futureValuePrincipal + futureValueContributions, currency)
    }
}

/**
 * Goal contribution record
 */
@Serializable
data class GoalContribution(
    val contributionId: String,
    val goalId: String,
    val amount: Money,
    val contributionType: ContributionType,
    val source: ContributionSource,
    val note: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val transactionId: String? = null
)

/**
 * Goal milestone
 */
@Serializable
data class GoalMilestone(
    val milestoneId: String,
    val goalId: String,
    val name: String,
    val targetAmount: Money,
    val rewardDescription: String? = null,
    val isAchieved: Boolean = false,
    val achievedAt: Instant? = null,
    val createdAt: Instant = Clock.System.now()
)

/**
 * Auto contribution settings
 */
@Serializable
data class AutoContribution(
    val enabled: Boolean = false,
    val amount: Money,
    val frequency: ContributionFrequency,
    val sourceAccountId: String?,
    val nextContributionDate: Instant,
    val endDate: Instant? = null
)

/**
 * Visual settings for goal display
 */
@Serializable
data class GoalVisualSettings(
    val iconName: String = "savings",
    val colorHex: String = "#4CAF50",
    val imageUrl: String? = null,
    val showProgressBar: Boolean = true,
    val showMilestones: Boolean = true
)

/**
 * Goal types
 */
@Serializable
enum class GoalType(val displayName: String, val displayNameAr: String) {
    SAVINGS("Savings Goal", "هدف ادخار"),
    EMERGENCY_FUND("Emergency Fund", "صندوق الطوارئ"),
    DEBT_PAYOFF("Debt Payoff", "سداد الديون"),
    INVESTMENT("Investment Goal", "هدف استثماري"),
    PURCHASE("Purchase Goal", "هدف شراء"),
    TRAVEL("Travel Fund", "صندوق السفر"),
    EDUCATION("Education Fund", "صندوق التعليم"),
    RETIREMENT("Retirement", "التقاعد"),
    HOME_DOWN_PAYMENT("Home Down Payment", "دفعة أولى للمنزل"),
    BUSINESS("Business Fund", "صندوق الأعمال"),
    CHARITY("Charity Goal", "هدف خيري"),
    HAJJ_UMRAH("Hajj/Umrah Fund", "صندوق الحج والعمرة"),
    WEDDING("Wedding Fund", "صندوق الزفاف"),
    CAR("Car Purchase", "شراء سيارة"),
    CUSTOM("Custom Goal", "هدف مخصص")
}

/**
 * Goal priority levels
 */
@Serializable
enum class GoalPriority(val displayName: String, val displayNameAr: String, val level: Int) {
    CRITICAL("Critical", "حرج", 1),
    HIGH("High", "عالي", 2),
    MEDIUM("Medium", "متوسط", 3),
    LOW("Low", "منخفض", 4)
}

/**
 * Goal strategies
 */
@Serializable
enum class GoalStrategy(val displayName: String, val displayNameAr: String) {
    FIXED_AMOUNT("Fixed Amount", "مبلغ ثابت"),
    PERCENTAGE_INCOME("Percentage of Income", "نسبة من الدخل"),
    ROUND_UP("Round-Up Savings", "ادخار التقريب"),
    CHALLENGE_BASED("Challenge-Based", "قائم على التحدي"),
    AUTOMATIC("Automatic Transfer", "تحويل تلقائي"),
    MANUAL("Manual Contributions", "مساهمات يدوية")
}

/**
 * Goal status indicators
 */
@Serializable
enum class GoalStatus(val displayName: String, val displayNameAr: String) {
    ACTIVE("Active", "نشط"),
    ON_TRACK("On Track", "في المسار الصحيح"),
    AT_RISK("At Risk", "في خطر"),
    OVERDUE("Overdue", "متأخر"),
    NEARLY_COMPLETE("Nearly Complete", "قريب من الاكتمال"),
    COMPLETED("Completed", "مكتمل"),
    PAUSED("Paused", "متوقف"),
    CANCELLED("Cancelled", "ملغي")
}

/**
 * Contribution types
 */
@Serializable
enum class ContributionType(val displayName: String, val displayNameAr: String) {
    REGULAR("Regular Contribution", "مساهمة منتظمة"),
    BONUS("Bonus Contribution", "مساهمة إضافية"),
    WINDFALL("Windfall", "مال غير متوقع"),
    ROUND_UP("Round-up", "تقريب"),
    AUTOMATIC("Automatic Transfer", "تحويل تلقائي"),
    MANUAL("Manual Deposit", "إيداع يدوي")
}

/**
 * Contribution sources
 */
@Serializable
enum class ContributionSource(val displayName: String, val displayNameAr: String) {
    BANK_TRANSFER("Bank Transfer", "تحويل بنكي"),
    CASH("Cash", "نقد"),
    SALARY("Salary", "راتب"),
    BONUS("Bonus", "مكافأة"),
    GIFT("Gift", "هدية"),
    REFUND("Refund", "استرداد"),
    INVESTMENT_RETURN("Investment Return", "عائد استثمار"),
    OTHER("Other", "أخرى")
}

/**
 * Contribution frequency
 */
@Serializable
enum class ContributionFrequency(val displayName: String, val displayNameAr: String, val intervalDays: Int) {
    DAILY("Daily", "يومي", 1),
    WEEKLY("Weekly", "أسبوعي", 7),
    BI_WEEKLY("Bi-weekly", "كل أسبوعين", 14),
    MONTHLY("Monthly", "شهري", 30),
    QUARTERLY("Quarterly", "ربع سنوي", 90),
    SEMI_ANNUAL("Semi-annual", "نصف سنوي", 180),
    YEARLY("Yearly", "سنوي", 365)
}

/**
 * Goal performance summary for dashboard
 */
@Serializable
data class GoalPerformanceSummary(
    val goalId: String,
    val goalName: String,
    val goalType: GoalType,
    val targetAmount: Money,
    val currentAmount: Money,
    val progressPercentage: Double,
    val remainingAmount: Money,
    val daysRemaining: Int?,
    val status: GoalStatus,
    val isOnTrack: Boolean,
    val projectedCompletionDate: Instant?,
    val requiredMonthlyContribution: Money?,
    val averageContribution: Money,
    val milestonesAchieved: Int,
    val totalMilestones: Int
) {
    
    /**
     * Get progress indicator emoji
     */
    fun getProgressIndicator(): String {
        return when {
            progressPercentage >= 100 -> "🎉" // Completed
            progressPercentage >= 75 -> "🔥" // Great progress
            progressPercentage >= 50 -> "📈" // Good progress
            progressPercentage >= 25 -> "🎯" // Making progress
            else -> "🌱" // Just started
        }
    }
    
    /**
     * Get time urgency indicator
     */
    fun getUrgencyIndicator(): String {
        return when {
            daysRemaining == null -> "⏳" // No deadline
            daysRemaining <= 7 -> "🚨" // Very urgent
            daysRemaining <= 30 -> "⚡" // Urgent
            daysRemaining <= 90 -> "⏰" // Some urgency
            else -> "📅" // Plenty of time
        }
    }
    
    /**
     * Calculate momentum score (0-100)
     */
    fun getMomentumScore(): Int {
        var score = progressPercentage.toInt()
        
        // Bonus for being on track
        if (isOnTrack) score += 10
        
        // Bonus for milestone achievements
        if (totalMilestones > 0) {
            val milestoneRatio = milestonesAchieved.toDouble() / totalMilestones
            score += (milestoneRatio * 20).toInt()
        }
        
        // Penalty for being overdue
        if (status == GoalStatus.OVERDUE) score -= 30
        else if (status == GoalStatus.AT_RISK) score -= 15
        
        return minOf(100, maxOf(0, score))
    }
}