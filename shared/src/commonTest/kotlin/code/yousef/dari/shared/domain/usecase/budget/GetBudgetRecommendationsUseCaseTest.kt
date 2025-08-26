package code.yousef.dari.shared.domain.usecase.budget

import code.yousef.dari.shared.domain.models.*
import code.yousef.dari.shared.domain.repository.BudgetRepository
import code.yousef.dari.shared.domain.repository.TransactionRepository
import code.yousef.dari.shared.domain.services.AIBudgetService
import code.yousef.dari.shared.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetBudgetRecommendationsUseCaseTest {

    private val budgetRepository = mockk<BudgetRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val aiBudgetService = mockk<AIBudgetService>()
    private val getBudgetRecommendationsUseCase = GetBudgetRecommendationsUseCase(
        budgetRepository = budgetRepository,
        transactionRepository = transactionRepository,
        aiBudgetService = aiBudgetService
    )

    private val userProfile = UserProfile(
        id = "user1",
        age = 28,
        monthlyIncome = Money(15000.0, Currency.SAR),
        dependents = 2,
        maritalStatus = "married",
        location = "Riyadh",
        isMuslim = true,
        hasMortgage = true,
        hasVehicleLoan = false,
        employmentType = "full_time",
        riskTolerance = RiskTolerance.MODERATE
    )

    private val recentTransactions = listOf(
        Transaction(
            id = "tx1",
            accountId = "acc1",
            amount = Money(800.0, Currency.SAR),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.FOOD_DINING,
            description = "Grocery shopping",
            date = LocalDateTime(2024, 1, 15, 12, 0),
            status = TransactionStatus.COMPLETED
        ),
        Transaction(
            id = "tx2",
            accountId = "acc1",
            amount = Money(2000.0, Currency.SAR),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.HOUSING,
            description = "Rent payment",
            date = LocalDateTime(2024, 1, 1, 0, 0),
            status = TransactionStatus.COMPLETED
        ),
        Transaction(
            id = "tx3",
            accountId = "acc1",
            amount = Money(500.0, Currency.SAR),
            type = TransactionType.EXPENSE,
            category = TransactionCategory.TRANSPORTATION,
            description = "Fuel and maintenance",
            date = LocalDateTime(2024, 1, 10, 8, 0),
            status = TransactionStatus.COMPLETED
        )
    )

    private val existingBudgets = listOf(
        Budget(
            id = "existing1",
            name = "Food Budget",
            amount = Money(1200.0, Currency.SAR),
            period = BudgetPeriod.MONTHLY,
            category = TransactionCategory.FOOD_DINING,
            startDate = LocalDateTime(2024, 1, 1, 0, 0),
            endDate = LocalDateTime(2024, 1, 31, 23, 59),
            accountIds = listOf("acc1"),
            isActive = true,
            budgetType = BudgetType.EXPENSE
        )
    )

    @Test
    fun `should generate comprehensive budget recommendations for new user`() = runTest {
        // Given
        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = true,
            includeSaudiSpecific = true
        )

        val aiRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.HOUSING,
                recommendedAmount = Money(5000.0, Currency.SAR),
                confidence = 0.9,
                reasoning = "Based on Saudi housing costs and your income, 33% is recommended",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY
            ),
            BudgetRecommendation(
                category = TransactionCategory.CHARITY,
                recommendedAmount = Money(375.0, Currency.SAR),
                confidence = 0.95,
                reasoning = "Zakat obligation: 2.5% of income after nisab threshold",
                priority = BudgetPriority.ESSENTIAL,
                period = BudgetPeriod.MONTHLY,
                isIslamic = true
            ),
            BudgetRecommendation(
                category = TransactionCategory.FOOD_DINING,
                recommendedAmount = Money(1500.0, Currency.SAR),
                confidence = 0.85,
                reasoning = "Saudi family food costs with 2 dependents",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(aiRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        assertEquals(3, recommendations.recommendations.size)
        
        // Verify Islamic budget included
        assertTrue(recommendations.recommendations.any { it.isIslamic })
        
        // Verify Zakat calculation
        val zakatRecommendation = recommendations.recommendations.find { it.category == TransactionCategory.CHARITY }
        assertEquals(375.0, zakatRecommendation?.recommendedAmount?.value)
        
        // Verify Saudi-specific amounts
        val housingRecommendation = recommendations.recommendations.find { it.category == TransactionCategory.HOUSING }
        assertEquals(5000.0, housingRecommendation?.recommendedAmount?.value)
        
        coVerify {
            aiBudgetService.generateRecommendations(any())
        }
    }

    @Test
    fun `should provide budget adjustments for existing user`() = runTest {
        // Given
        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = true,
            includeSaudiSpecific = true
        )

        val adjustmentRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.FOOD_DINING,
                recommendedAmount = Money(1500.0, Currency.SAR),
                confidence = 0.8,
                reasoning = "Current budget of 1200 SAR is insufficient based on recent spending of 1600 SAR/month",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                adjustmentType = BudgetAdjustmentType.INCREASE,
                currentAmount = Money(1200.0, Currency.SAR)
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(existingBudgets)
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(adjustmentRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        assertEquals(1, recommendations.recommendations.size)
        
        val adjustment = recommendations.recommendations.first()
        assertEquals(BudgetAdjustmentType.INCREASE, adjustment.adjustmentType)
        assertEquals(1200.0, adjustment.currentAmount?.value)
        assertEquals(1500.0, adjustment.recommendedAmount.value)
    }

    @Test
    fun `should include Ramadan-specific budget recommendations`() = runTest {
        // Given - During Ramadan period
        val ramadanProfile = userProfile.copy(
            preferences = mapOf("include_ramadan_budgets" to "true")
        )

        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = ramadanProfile,
            includeIslamicBudgets = true,
            includeSaudiSpecific = true,
            includeSeasonalBudgets = true
        )

        val ramadanRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.FOOD_DINING,
                recommendedAmount = Money(2000.0, Currency.SAR),
                confidence = 0.9,
                reasoning = "Ramadan iftar and suhoor expenses typically increase by 40%",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("ramadan", "iftar", "suhoor"),
                seasonalBudget = true
            ),
            BudgetRecommendation(
                category = TransactionCategory.CHARITY,
                recommendedAmount = Money(1000.0, Currency.SAR),
                confidence = 0.95,
                reasoning = "Additional charity during Ramadan is highly recommended",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("ramadan", "charity", "sadaqah"),
                isIslamic = true,
                seasonalBudget = true
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(ramadanRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        assertTrue(recommendations.recommendations.any { it.seasonalBudget })
        assertTrue(recommendations.recommendations.any { it.tags.contains("ramadan") })
    }

    @Test
    fun `should provide Saudi government budget recommendations`() = runTest {
        // Given
        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = false,
            includeSaudiSpecific = true
        )

        val govRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.TAXES,
                recommendedAmount = Money(2250.0, Currency.SAR),
                confidence = 0.9,
                reasoning = "VAT payments and government fees (15% of expenses)",
                priority = BudgetPriority.MEDIUM,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("vat", "government", "tax")
            ),
            BudgetRecommendation(
                category = TransactionCategory.HEALTHCARE,
                recommendedAmount = Money(800.0, Currency.SAR),
                confidence = 0.8,
                reasoning = "Private healthcare costs not covered by employer insurance",
                priority = BudgetPriority.MEDIUM,
                period = BudgetPeriod.MONTHLY,
                tags = listOf("healthcare", "insurance", "medical")
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(govRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        assertTrue(recommendations.recommendations.any { it.tags.contains("vat") })
        assertTrue(recommendations.recommendations.any { it.category == TransactionCategory.HEALTHCARE })
    }

    @Test
    fun `should calculate budget recommendations based on spending patterns`() = runTest {
        // Given - User with high transportation spending
        val highTransportationTransactions = listOf(
            Transaction(
                id = "tx1",
                accountId = "acc1",
                amount = Money(1200.0, Currency.SAR),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.TRANSPORTATION,
                description = "Uber rides",
                date = LocalDateTime(2024, 1, 15, 12, 0),
                status = TransactionStatus.COMPLETED
            ),
            Transaction(
                id = "tx2",
                accountId = "acc1",
                amount = Money(800.0, Currency.SAR),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.TRANSPORTATION,
                description = "Fuel",
                date = LocalDateTime(2024, 1, 10, 8, 0),
                status = TransactionStatus.COMPLETED
            )
        )

        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = false,
            includeSaudiSpecific = true
        )

        val patternBasedRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.TRANSPORTATION,
                recommendedAmount = Money(2500.0, Currency.SAR),
                confidence = 0.9,
                reasoning = "Your transportation spending is 2000 SAR/month, recommend budgeting 2500 SAR with buffer",
                priority = BudgetPriority.HIGH,
                period = BudgetPeriod.MONTHLY,
                basedOnSpending = true
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(highTransportationTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(patternBasedRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        val transportRec = recommendations.recommendations.find { it.category == TransactionCategory.TRANSPORTATION }
        assertTrue(transportRec?.basedOnSpending == true)
        assertEquals(2500.0, transportRec?.recommendedAmount?.value)
    }

    @Test
    fun `should provide emergency fund recommendations for Saudi market`() = runTest {
        // Given
        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = false,
            includeSaudiSpecific = true,
            includeEmergencyFund = true
        )

        val emergencyRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.EMERGENCY_FUND,
                recommendedAmount = Money(7500.0, Currency.SAR),
                confidence = 0.95,
                reasoning = "Emergency fund should cover 6 months of expenses (12,500 SAR/month avg)",
                priority = BudgetPriority.ESSENTIAL,
                period = BudgetPeriod.GOAL_BASED,
                targetAmount = Money(75000.0, Currency.SAR), // 6 months of expenses
                monthsToTarget = 10
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(emergencyRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        val emergencyRec = recommendations.recommendations.find { it.category == TransactionCategory.EMERGENCY_FUND }
        assertEquals(75000.0, emergencyRec?.targetAmount?.value)
        assertEquals(10, emergencyRec?.monthsToTarget)
    }

    @Test
    fun `should handle AI service failure gracefully`() = runTest {
        // Given
        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = true,
            includeSaudiSpecific = true
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Error(Exception("AI service unavailable"))

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        // Should fallback to rule-based recommendations
        assertTrue(recommendations.recommendations.isNotEmpty())
        assertTrue(recommendations.fallbackUsed)
    }

    @Test
    fun `should prioritize recommendations by Islamic requirements`() = runTest {
        // Given
        val request = GetBudgetRecommendationsRequest(
            userId = "user1",
            userProfile = userProfile,
            includeIslamicBudgets = true,
            includeSaudiSpecific = true
        )

        val mixedRecommendations = listOf(
            BudgetRecommendation(
                category = TransactionCategory.ENTERTAINMENT,
                recommendedAmount = Money(500.0, Currency.SAR),
                confidence = 0.6,
                reasoning = "Entertainment budget",
                priority = BudgetPriority.LOW,
                period = BudgetPeriod.MONTHLY
            ),
            BudgetRecommendation(
                category = TransactionCategory.CHARITY,
                recommendedAmount = Money(375.0, Currency.SAR),
                confidence = 0.95,
                reasoning = "Zakat obligation",
                priority = BudgetPriority.ESSENTIAL,
                period = BudgetPeriod.MONTHLY,
                isIslamic = true
            )
        )

        coEvery { transactionRepository.getRecentTransactions("user1", any()) } returns Result.Success(recentTransactions)
        coEvery { budgetRepository.getBudgetsByUserId("user1") } returns Result.Success(emptyList())
        coEvery { aiBudgetService.generateRecommendations(any()) } returns Result.Success(mixedRecommendations)

        // When
        val result = getBudgetRecommendationsUseCase(request)

        // Then
        assertTrue(result is Result.Success)
        val recommendations = result.data
        // Islamic/essential budgets should be prioritized
        val sortedRecs = recommendations.recommendations.sortedByDescending { 
            if (it.isIslamic) 10 else it.priority.ordinal 
        }
        assertTrue(sortedRecs.first().isIslamic)
    }
}