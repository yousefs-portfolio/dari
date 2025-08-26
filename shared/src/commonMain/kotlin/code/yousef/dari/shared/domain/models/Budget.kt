package code.yousef.dari.shared.domain.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Budget Domain Model
 * Represents a financial budget with categories, periods, and tracking
 * Supports both envelope budgeting and zero-based budgeting methodologies
 */
@Serializable
data class Budget(
    val budgetId: String,
    val name: String,
    val description: String?,
    val budgetType: BudgetType,
    val period: BudgetPeriod,
    val startDate: Instant,
    val endDate: Instant?,
    val totalAmount: Money,
    val currency: String,
    val isActive: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val categories: List<BudgetCategory> = emptyList(),
    val rolloverSettings: RolloverSettings = RolloverSettings(),
    val alertSettings: AlertSettings = AlertSettings(),
    val metadata: Map<String, String> = emptyMap()
) {
    
    /**
     * Calculate total allocated amount across all categories
     */
    fun getTotalAllocated(): Money {
        return categories.fold(Money.fromInt(0, currency)) { acc, category ->
            acc + category.allocatedAmount
        }
    }
    
    /**
     * Calculate total spent amount across all categories
     */
    fun getTotalSpent(): Money {
        return categories.fold(Money.fromInt(0, currency)) { acc, category ->
            acc + category.spentAmount
        }
    }
    
    /**
     * Calculate remaining budget amount
     */
    fun getRemainingAmount(): Money {
        return totalAmount - getTotalSpent()
    }
    
    /**
     * Calculate unallocated budget amount
     */
    fun getUnallocatedAmount(): Money {
        return totalAmount - getTotalAllocated()
    }
    
    /**
     * Get budget utilization percentage (0-100)
     */
    fun getUtilizationPercentage(): Double {
        if (totalAmount.isZero()) return 0.0
        val spent = getTotalSpent()
        return (spent.numericValue / totalAmount.numericValue) * 100.0
    }
    
    /**
     * Check if budget is over allocated
     */
    fun isOverAllocated(): Boolean {
        return getTotalAllocated() > totalAmount
    }
    
    /**
     * Check if budget is overspent
     */
    fun isOverspent(): Boolean {
        return getTotalSpent() > totalAmount
    }
    
    /**
     * Get budget status
     */
    fun getBudgetStatus(): BudgetStatus {
        return when {
            !isActive -> BudgetStatus.INACTIVE
            isOverspent() -> BudgetStatus.OVERSPENT
            isOverAllocated() -> BudgetStatus.OVER_ALLOCATED
            getUtilizationPercentage() >= alertSettings.criticalThreshold -> BudgetStatus.CRITICAL
            getUtilizationPercentage() >= alertSettings.warningThreshold -> BudgetStatus.WARNING
            else -> BudgetStatus.ON_TRACK
        }
    }
    
    /**
     * Get categories that are overspent
     */
    fun getOverspentCategories(): List<BudgetCategory> {
        return categories.filter { it.isOverspent() }
    }
    
    /**
     * Get categories approaching their limit
     */
    fun getCategoriesNearLimit(threshold: Double = 80.0): List<BudgetCategory> {
        return categories.filter { 
            it.getUtilizationPercentage() >= threshold && !it.isOverspent()
        }
    }
    
    /**
     * Calculate projected spending based on current pace
     */
    fun getProjectedSpending(): Money {
        val now = Clock.System.now()
        val totalPeriodDays = (endDate ?: now) - startDate
        val elapsedDays = now - startDate
        
        if (totalPeriodDays.inWholeDays <= 0 || elapsedDays.inWholeDays <= 0) {
            return getTotalSpent()
        }
        
        val progressRatio = elapsedDays.inWholeDays.toDouble() / totalPeriodDays.inWholeDays.toDouble()
        val currentSpent = getTotalSpent()
        
        return if (progressRatio > 0) {
            Money.fromDouble(currentSpent.numericValue / progressRatio, currency)
        } else {
            currentSpent
        }
    }
    
    /**
     * Get performance summary for dashboard
     */
    fun getPerformanceSummary(): BudgetPerformanceSummary {
        return BudgetPerformanceSummary(
            budgetId = budgetId,
            budgetName = name,
            totalBudget = totalAmount,
            totalSpent = getTotalSpent(),
            totalAllocated = getTotalAllocated(),
            remainingAmount = getRemainingAmount(),
            utilizationPercentage = getUtilizationPercentage(),
            projectedSpending = getProjectedSpending(),
            status = getBudgetStatus(),
            overspentCategories = getOverspentCategories().size,
            categoriesNearLimit = getCategoriesNearLimit().size,
            daysRemaining = endDate?.let { end ->
                (end - Clock.System.now()).inWholeDays.toInt()
            } ?: 0
        )
    }
    
    /**
     * Check if rollover is applicable
     */
    fun canRollover(): Boolean {
        return rolloverSettings.enabled && !isOverspent()
    }
    
    /**
     * Calculate rollover amount
     */
    fun calculateRolloverAmount(): Money {
        if (!canRollover()) return Money.fromInt(0, currency)
        
        return when (rolloverSettings.type) {
            RolloverType.FULL -> getRemainingAmount()
            RolloverType.PERCENTAGE -> getRemainingAmount().percentage(rolloverSettings.percentage)
            RolloverType.FIXED_AMOUNT -> {
                val remaining = getRemainingAmount()
                val fixed = rolloverSettings.fixedAmount ?: Money.fromInt(0, currency)
                if (remaining >= fixed) fixed else remaining
            }
        }
    }
}

/**
 * Budget Category within a budget
 */
@Serializable
data class BudgetCategory(
    val categoryId: String,
    val categoryName: String,
    val allocatedAmount: Money,
    val spentAmount: Money = Money.fromInt(0, allocatedAmount.currency),
    val isFixed: Boolean = false,
    val priority: CategoryPriority = CategoryPriority.MEDIUM,
    val notes: String? = null
) {
    
    /**
     * Get remaining amount in this category
     */
    fun getRemainingAmount(): Money = allocatedAmount - spentAmount
    
    /**
     * Check if category is overspent
     */
    fun isOverspent(): Boolean = spentAmount > allocatedAmount
    
    /**
     * Get utilization percentage for this category
     */
    fun getUtilizationPercentage(): Double {
        if (allocatedAmount.isZero()) return 0.0
        return (spentAmount.numericValue / allocatedAmount.numericValue) * 100.0
    }
    
    /**
     * Get category status
     */
    fun getCategoryStatus(): CategoryStatus {
        return when {
            isOverspent() -> CategoryStatus.OVERSPENT
            getUtilizationPercentage() >= 90 -> CategoryStatus.CRITICAL
            getUtilizationPercentage() >= 75 -> CategoryStatus.WARNING
            else -> CategoryStatus.ON_TRACK
        }
    }
}

/**
 * Budget types supported
 */
@Serializable
enum class BudgetType(val displayName: String, val displayNameAr: String) {
    MONTHLY("Monthly Budget", "ميزانية شهرية"),
    WEEKLY("Weekly Budget", "ميزانية أسبوعية"),
    YEARLY("Yearly Budget", "ميزانية سنوية"),
    CUSTOM("Custom Period", "فترة مخصصة"),
    PROJECT("Project Budget", "ميزانية مشروع"),
    ENVELOPE("Envelope Budget", "ميزانية المغلفات"),
    ZERO_BASED("Zero-Based Budget", "الميزانية صفرية الأساس")
}

/**
 * Budget periods
 */
@Serializable
enum class BudgetPeriod(val displayName: String, val displayNameAr: String, val durationDays: Int) {
    WEEKLY("Weekly", "أسبوعي", 7),
    BI_WEEKLY("Bi-weekly", "كل أسبوعين", 14),
    MONTHLY("Monthly", "شهري", 30),
    QUARTERLY("Quarterly", "ربع سنوي", 90),
    SEMI_ANNUAL("Semi-annual", "نصف سنوي", 180),
    YEARLY("Yearly", "سنوي", 365),
    CUSTOM("Custom", "مخصص", 0)
}

/**
 * Category priority levels
 */
@Serializable
enum class CategoryPriority(val displayName: String, val displayNameAr: String, val level: Int) {
    CRITICAL("Critical", "حرج", 1),
    HIGH("High", "عالي", 2),
    MEDIUM("Medium", "متوسط", 3),
    LOW("Low", "منخفض", 4),
    OPTIONAL("Optional", "اختياري", 5)
}

/**
 * Budget status indicators
 */
@Serializable
enum class BudgetStatus(val displayName: String, val displayNameAr: String) {
    ON_TRACK("On Track", "في المسار الصحيح"),
    WARNING("Warning", "تحذير"),
    CRITICAL("Critical", "حرج"),
    OVERSPENT("Overspent", "متجاوز"),
    OVER_ALLOCATED("Over Allocated", "مخصص بشكل مفرط"),
    INACTIVE("Inactive", "غير نشط")
}

/**
 * Category status indicators
 */
@Serializable
enum class CategoryStatus(val displayName: String, val displayNameAr: String) {
    ON_TRACK("On Track", "في المسار الصحيح"),
    WARNING("Warning", "تحذير"),
    CRITICAL("Critical", "حرج"),
    OVERSPENT("Overspent", "متجاوز")
}

/**
 * Rollover settings for budget periods
 */
@Serializable
data class RolloverSettings(
    val enabled: Boolean = false,
    val type: RolloverType = RolloverType.FULL,
    val percentage: Double = 100.0,
    val fixedAmount: Money? = null,
    val maxRolloverAmount: Money? = null
)

/**
 * Rollover types
 */
@Serializable
enum class RolloverType(val displayName: String, val displayNameAr: String) {
    FULL("Full Amount", "المبلغ الكامل"),
    PERCENTAGE("Percentage", "نسبة مئوية"),
    FIXED_AMOUNT("Fixed Amount", "مبلغ ثابت")
}

/**
 * Alert settings for budgets
 */
@Serializable
data class AlertSettings(
    val enabled: Boolean = true,
    val warningThreshold: Double = 75.0, // Percentage
    val criticalThreshold: Double = 90.0, // Percentage
    val notifyOnOverspending: Boolean = true,
    val notifyOnCategoryLimits: Boolean = true,
    val dailyDigest: Boolean = false,
    val weeklyReport: Boolean = true
)

/**
 * Budget performance summary for dashboard
 */
@Serializable
data class BudgetPerformanceSummary(
    val budgetId: String,
    val budgetName: String,
    val totalBudget: Money,
    val totalSpent: Money,
    val totalAllocated: Money,
    val remainingAmount: Money,
    val utilizationPercentage: Double,
    val projectedSpending: Money,
    val status: BudgetStatus,
    val overspentCategories: Int,
    val categoriesNearLimit: Int,
    val daysRemaining: Int
) {
    
    /**
     * Check if projected spending exceeds budget
     */
    fun isProjectedOverspent(): Boolean {
        return projectedSpending > totalBudget
    }
    
    /**
     * Calculate savings if under budget
     */
    fun getPotentialSavings(): Money {
        return if (projectedSpending < totalBudget) {
            totalBudget - projectedSpending
        } else {
            Money.fromInt(0, totalBudget.currency)
        }
    }
    
    /**
     * Get health score (0-100)
     */
    fun getHealthScore(): Int {
        var score = 100
        
        // Deduct for overspending
        if (status == BudgetStatus.OVERSPENT) score -= 50
        else if (status == BudgetStatus.CRITICAL) score -= 30
        else if (status == BudgetStatus.WARNING) score -= 15
        
        // Deduct for overspent categories
        score -= (overspentCategories * 10)
        
        // Deduct for categories near limit
        score -= (categoriesNearLimit * 5)
        
        // Deduct for projected overspending
        if (isProjectedOverspent()) score -= 20
        
        return maxOf(0, score)
    }
}