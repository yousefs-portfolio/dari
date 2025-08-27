package code.yousef.dari.shared.domain.usecase.categorization

import code.yousef.dari.shared.domain.model.Money
import code.yousef.dari.shared.domain.model.Transaction
import code.yousef.dari.shared.domain.model.TransactionType
import code.yousef.dari.shared.domain.repository.MerchantMappingRepository
import code.yousef.dari.shared.testutil.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MerchantMappingUseCaseTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val merchantMappingRepository = mockk<MerchantMappingRepository>()
    private lateinit var merchantMappingUseCase: MerchantMappingUseCase

    @BeforeTest
    fun setup() {
        merchantMappingUseCase = MerchantMappingUseCase(merchantMappingRepository)
    }

    @Test
    fun `create merchant mapping from transaction`() = runTest {
        // Given
        val transaction = createTransaction("PANDA HYPERMARKET RIYADH", "Groceries")
        coEvery { merchantMappingRepository.create(any()) } returns Unit

        // When
        val result = merchantMappingUseCase.createMapping(
            merchantName = transaction.merchantName,
            categoryId = "cat_groceries",
            confidence = 0.95,
            source = MappingSource.USER_CONFIRMED
        )

        // Then
        assertTrue(result.isSuccess)
        val mapping = result.getOrNull()
        assertNotNull(mapping)
        assertEquals("PANDA HYPERMARKET RIYADH", mapping.merchantName)
        assertEquals("cat_groceries", mapping.categoryId)
        assertEquals(0.95, mapping.confidence)
        assertEquals(MappingSource.USER_CONFIRMED, mapping.source)

        coVerify { merchantMappingRepository.create(any()) }
    }

    @Test
    fun `normalize merchant name variants`() = runTest {
        // Given
        val variants = listOf(
            "PANDA HYPERMARKET",
            "PANDA HYPERMARKET RIYADH",
            "Panda Hypermarket - Branch 123",
            "PANDA HYPER MARKET",
            "panda hypermarket dammam"
        )

        // When & Then
        variants.forEach { variant ->
            val normalized = merchantMappingUseCase.normalizeMerchantName(variant)
            assertEquals("panda hypermarket", normalized)
        }
    }

    @Test
    fun `find best matching mapping for merchant`() = runTest {
        // Given
        val mappings = listOf(
            createMerchantMapping("starbucks coffee", "cat_dining", 0.95),
            createMerchantMapping("starbucks", "cat_dining", 0.90),
            createMerchantMapping("starbucks cafe", "cat_dining", 0.85)
        )

        coEvery {
            merchantMappingRepository.findSimilarMappings("starbucks coffee riyadh")
        } returns mappings

        // When
        val result = merchantMappingUseCase.findBestMapping("Starbucks Coffee Riyadh")

        // Then
        assertNotNull(result)
        assertEquals("cat_dining", result.categoryId)
        assertEquals(0.95, result.confidence)
    }

    @Test
    fun `learn from user corrections`() = runTest {
        // Given
        val originalMapping = createMerchantMapping("netflix", "cat_entertainment", 0.90)
        coEvery { merchantMappingRepository.getByMerchantName("netflix") } returns originalMapping
        coEvery { merchantMappingRepository.update(any()) } returns Unit
        coEvery { merchantMappingRepository.recordFeedback(any()) } returns Unit

        // When
        val result = merchantMappingUseCase.learnFromCorrection(
            merchantName = "netflix",
            newCategoryId = "cat_streaming", // User corrected to more specific category
            userFeedback = "Netflix should be in Streaming, not general Entertainment"
        )

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            merchantMappingRepository.update(
                match {
                    it.categoryId == "cat_streaming" &&
                        it.confidence > originalMapping.confidence
                }
            )
        }
    }

    @Test
    fun `build merchant similarity index`() = runTest {
        // Given
        val transactions = listOf(
            createTransaction("STARBUCKS COFFEE", "Dining"),
            createTransaction("STARBUCKS RIYADH", "Dining"),
            createTransaction("DUNKIN DONUTS", "Dining"),
            createTransaction("COSTA COFFEE", "Dining")
        )

        coEvery { merchantMappingRepository.getAllMappings() } returns emptyList()
        coEvery { merchantMappingRepository.createBulk(any()) } returns Unit

        // When
        val result = merchantMappingUseCase.buildSimilarityIndex(transactions)

        // Then
        assertTrue(result.isSuccess)
        val similarityGroups = result.getOrNull()
        assertNotNull(similarityGroups)

        // Should group similar coffee shops together
        val coffeeGroup = similarityGroups.find { group ->
            group.any { it.contains("starbucks") } &&
                group.any { it.contains("costa") }
        }
        assertNotNull(coffeeGroup)
    }

    @Test
    fun `suggest category based on merchant patterns`() = runTest {
        // Given
        val mappings = listOf(
            createMerchantMapping("mcdonalds", "cat_dining", 0.95),
            createMerchantMapping("burger king", "cat_dining", 0.95),
            createMerchantMapping("kfc", "cat_dining", 0.95)
        )

        coEvery { merchantMappingRepository.getAllMappings() } returns mappings

        // When
        val suggestions = merchantMappingUseCase.suggestCategory("Pizza Hut")

        // Then
        assertTrue(suggestions.isNotEmpty())
        val topSuggestion = suggestions.first()
        assertEquals("cat_dining", topSuggestion.categoryId)
        assertTrue(topSuggestion.confidence > 0.8) // Should be high confidence based on similar merchants
        assertEquals("similar_merchant_pattern", topSuggestion.reason)
    }

    @Test
    fun `handle Arabic merchant names`() = runTest {
        // Given
        val arabicMerchantName = "مطعم البيك"
        val englishEquivalent = "Al Baik Restaurant"

        coEvery { merchantMappingRepository.findByArabicName(arabicMerchantName) } returns
            createMerchantMapping(englishEquivalent, "cat_dining", 0.90)

        // When
        val result = merchantMappingUseCase.findBestMapping(arabicMerchantName)

        // Then
        assertNotNull(result)
        assertEquals("cat_dining", result.categoryId)
    }

    @Test
    fun `merge duplicate merchant mappings`() = runTest {
        // Given
        val duplicateMappings = listOf(
            createMerchantMapping("starbucks", "cat_dining", 0.85),
            createMerchantMapping("starbucks coffee", "cat_dining", 0.90),
            createMerchantMapping("starbucks cafe", "cat_dining", 0.80)
        )

        coEvery { merchantMappingRepository.findDuplicates() } returns duplicateMappings
        coEvery { merchantMappingRepository.delete(any()) } returns Unit
        coEvery { merchantMappingRepository.create(any()) } returns Unit

        // When
        val result = merchantMappingUseCase.mergeDuplicates()

        // Then
        assertTrue(result.isSuccess)
        val mergedCount = result.getOrNull()!!
        assertTrue(mergedCount > 0)

        coVerify(exactly = duplicateMappings.size) { merchantMappingRepository.delete(any()) }
        coVerify { merchantMappingRepository.create(any()) } // One merged mapping
    }

    @Test
    fun `calculate mapping accuracy over time`() = runTest {
        // Given
        val mappings = listOf(
            createMerchantMapping("netflix", "cat_entertainment", 0.95, 150, 10), // 93% accuracy
            createMerchantMapping("spotify", "cat_entertainment", 0.90, 100, 5), // 95% accuracy
            createMerchantMapping("unknown", "cat_other", 0.60, 20, 15) // 25% accuracy
        )

        coEvery { merchantMappingRepository.getAllMappings() } returns mappings

        // When
        val accuracy = merchantMappingUseCase.calculateOverallAccuracy()

        // Then
        assertTrue(accuracy > 0.8) // Should be high overall accuracy
    }

    @Test
    fun `export and import merchant mappings`() = runTest {
        // Given
        val mappings = listOf(
            createMerchantMapping("starbucks", "cat_dining", 0.95),
            createMerchantMapping("shell", "cat_transport", 0.90)
        )

        coEvery { merchantMappingRepository.getAllMappings() } returns mappings
        coEvery { merchantMappingRepository.createBulk(any()) } returns Unit
        coEvery { merchantMappingRepository.deleteAll() } returns Unit

        // When - Export
        val exportResult = merchantMappingUseCase.exportMappings()
        assertTrue(exportResult.isSuccess)
        val exportedData = exportResult.getOrNull()!!

        // When - Import
        val importResult = merchantMappingUseCase.importMappings(exportedData, replaceExisting = true)

        // Then
        assertTrue(importResult.isSuccess)
        coVerify { merchantMappingRepository.deleteAll() }
        coVerify { merchantMappingRepository.createBulk(mappings) }
    }

    private fun createTransaction(
        merchantName: String,
        category: String,
        amount: Money = Money(BigDecimal("-100.00"), "SAR")
    ) = Transaction(
        id = "txn_${(1000..9999).random()}",
        accountId = "account1",
        amount = amount,
        type = TransactionType.DEBIT,
        description = merchantName,
        merchantName = merchantName,
        date = Clock.System.now(),
        categoryId = "cat_$category"
    )

    private fun createMerchantMapping(
        merchantName: String,
        categoryId: String,
        confidence: Double,
        successfulMappings: Int = 50,
        failedMappings: Int = 2
    ) = MerchantMapping(
        id = "mapping_${(1000..9999).random()}",
        merchantName = merchantName,
        normalizedName = merchantName.lowercase(),
        categoryId = categoryId,
        confidence = confidence,
        source = MappingSource.AUTO_DETECTED,
        successfulMappings = successfulMappings,
        failedMappings = failedMappings,
        lastUsedAt = Clock.System.now(),
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
