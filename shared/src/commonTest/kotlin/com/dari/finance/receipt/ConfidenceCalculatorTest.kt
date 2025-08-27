package com.dari.finance.receipt

import com.dari.finance.ocr.ReceiptData
import com.dari.finance.ocr.ReceiptItem
import kotlin.test.*

class ConfidenceCalculatorTest {

    private val calculator = ConfidenceCalculator()

    @Test
    fun `should calculate confidence for complete receipt data`() {
        val completeReceipt = ReceiptData(
            merchantName = "LULU HYPERMARKET",
            date = "15/01/2024",
            total = "125.50",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Apples", "25.00"),
                ReceiptItem("Milk", "12.50"),
                ReceiptItem("Bread", "8.00")
            ),
            tax = "18.75",
            subtotal = "106.75",
            rawText = "LULU HYPERMARKET\nDate: 15/01/2024\nApples 25.00\nMilk 12.50\nBread 8.00\nSubtotal: 106.75\nTax: 18.75\nTotal: 125.50 SAR"
        )

        val confidence = calculator.calculateOverallConfidence(completeReceipt)

        assertTrue(confidence > 0.85f, "Complete receipt should have high confidence")
        assertTrue(confidence <= 1.0f, "Confidence should not exceed 1.0")
    }

    @Test
    fun `should calculate confidence for minimal receipt data`() {
        val minimalReceipt = ReceiptData(
            total = "25.00",
            currency = "SAR"
        )

        val confidence = calculator.calculateOverallConfidence(minimalReceipt)

        assertTrue(confidence < 0.6f, "Minimal receipt should have low confidence")
        assertTrue(confidence > 0.0f, "Confidence should be positive")
    }

    @Test
    fun `should weight different confidence factors correctly`() {
        val factors = ConfidenceFactors(
            dataCompleteness = 0.8f,
            textQuality = 0.7f,
            mathematicalConsistency = 0.9f,
            formatConsistency = 0.6f,
            businessLogic = 0.8f,
            ocrAccuracy = 0.75f
        )

        val overallConfidence = calculator.calculateWeightedConfidence(factors)

        // Should be weighted average, roughly around 0.75
        assertTrue(overallConfidence > 0.70f && overallConfidence < 0.80f)
    }

    @Test
    fun `should analyze text quality metrics`() {
        val clearText = "LULU HYPERMARKET\nDate: 15/01/2024\nTotal: 125.50 SAR"
        val unclearText = "LUlU HyP3RM4RK3T\nD4t3: 1S/01/Z0Z4\nT0t41: 1ZS.S0 S4R"
        val veryUnclearText = "###@@ @@### \n???: ??/??/????  \n@@@: ???.?? @@@"

        val clearQuality = calculator.analyzeTextQuality(clearText)
        val unclearQuality = calculator.analyzeTextQuality(unclearText)
        val veryUnclearQuality = calculator.analyzeTextQuality(veryUnclearText)

        assertTrue(clearQuality > 0.8f, "Clear text should have high quality score")
        assertTrue(unclearQuality < clearQuality, "Unclear text should have lower quality")
        assertTrue(veryUnclearQuality < unclearQuality, "Very unclear text should have lowest quality")
    }

    @Test
    fun `should evaluate mathematical consistency`() {
        // Consistent math
        val consistentReceipt = ReceiptData(
            total = "115.00",
            subtotal = "100.00",
            tax = "15.00"
        )

        val consistentScore = calculator.evaluateMathematicalConsistency(consistentReceipt)
        assertTrue(consistentScore > 0.9f, "Consistent math should score highly")

        // Inconsistent math
        val inconsistentReceipt = ReceiptData(
            total = "200.00",
            subtotal = "100.00",
            tax = "15.00"
        )

        val inconsistentScore = calculator.evaluateMathematicalConsistency(inconsistentReceipt)
        assertTrue(inconsistentScore < 0.3f, "Inconsistent math should score poorly")

        // Missing data
        val incompleteReceipt = ReceiptData(
            total = "115.00"
        )

        val incompleteScore = calculator.evaluateMathematicalConsistency(incompleteReceipt)
        assertTrue(incompleteScore < 0.8f, "Incomplete data should have moderate score")
    }

    @Test
    fun `should assess data completeness`() {
        val fields = mapOf(
            "merchantName" to "LULU HYPERMARKET",
            "date" to "15/01/2024",
            "total" to "125.50",
            "currency" to "SAR",
            "items" to "3 items",
            "tax" to "18.75",
            "subtotal" to "106.75"
        )

        val fullCompleteness = calculator.assessDataCompleteness(fields)
        assertTrue(fullCompleteness > 0.9f)

        val partialFields = mapOf(
            "total" to "125.50",
            "currency" to "SAR"
        )

        val partialCompleteness = calculator.assessDataCompleteness(partialFields)
        assertTrue(partialCompleteness < 0.5f)
    }

    @Test
    fun `should validate format consistency`() {
        val consistentReceipt = ReceiptData(
            date = "15/01/2024",
            total = "125.50",
            subtotal = "106.75",
            tax = "18.75",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Item 1", "25.50"),
                ReceiptItem("Item 2", "81.25")
            )
        )

        val consistentScore = calculator.evaluateFormatConsistency(consistentReceipt)
        assertTrue(consistentScore > 0.8f, "Consistent formats should score highly")

        val inconsistentReceipt = ReceiptData(
            date = "invalid-date",
            total = "125.5", // Missing decimal place
            subtotal = "106.754", // Too many decimal places
            tax = "abc", // Non-numeric
            currency = "INVALID",
            items = listOf(
                ReceiptItem("", "25.50"), // Empty name
                ReceiptItem("Item 2", "-10.00") // Negative price
            )
        )

        val inconsistentScore = calculator.evaluateFormatConsistency(inconsistentReceipt)
        assertTrue(inconsistentScore < 0.4f, "Inconsistent formats should score poorly")
    }

    @Test
    fun `should apply business logic validation`() {
        // Normal business transaction
        val normalReceipt = ReceiptData(
            merchantName = "LULU HYPERMARKET",
            date = "15/01/2024",
            total = "125.50",
            currency = "SAR",
            items = listOf(
                ReceiptItem("Apples", "25.00"),
                ReceiptItem("Milk", "8.50")
            )
        )

        val normalScore = calculator.evaluateBusinessLogic(normalReceipt)
        assertTrue(normalScore > 0.8f, "Normal receipt should pass business logic")

        // Unusual business transaction
        val unusualReceipt = ReceiptData(
            merchantName = "Unknown Store",
            date = "01/01/1990", // Very old date
            total = "0.00", // Zero total
            currency = "XYZ", // Invalid currency
            items = listOf(
                ReceiptItem("Expensive Item", "10000.00") // Very expensive
            )
        )

        val unusualScore = calculator.evaluateBusinessLogic(unusualReceipt)
        assertTrue(unusualScore < 0.5f, "Unusual receipt should fail business logic")
    }

    @Test
    fun `should estimate OCR accuracy`() {
        val highQualityText = "LULU HYPERMARKET King Fahd Road Riyadh Date: 15/01/2024 Total: 125.50 SAR"
        val mediumQualityText = "LULU HYPER4ARK3T King Fahd R0ad Riyadh Dat3: 15/01/2024 T0tal: 125.50 SAR"
        val lowQualityText = "LULU HYP3R@@RK3T K1ng F@hd R0@d R1y@dh D@t3: 1S/01/20Z4 T0t@l: 1ZS.S0 S@R"

        val highAccuracy = calculator.estimateOCRAccuracy(highQualityText)
        val mediumAccuracy = calculator.estimateOCRAccuracy(mediumQualityText)
        val lowAccuracy = calculator.estimateOCRAccuracy(lowQualityText)

        assertTrue(highAccuracy > 0.9f, "High quality text should have high OCR accuracy")
        assertTrue(mediumAccuracy < highAccuracy, "Medium quality should have lower accuracy")
        assertTrue(lowAccuracy < mediumAccuracy, "Low quality should have lowest accuracy")
    }

    @Test
    fun `should provide confidence breakdown`() {
        val receipt = ReceiptData(
            merchantName = "LULU HYPERMARKET",
            date = "15/01/2024",
            total = "125.50",
            currency = "SAR",
            items = listOf(ReceiptItem("Test Item", "25.00")),
            rawText = "LULU HYPERMARKET\nDate: 15/01/2024\nTest Item 25.00\nTotal: 125.50 SAR"
        )

        val breakdown = calculator.getConfidenceBreakdown(receipt)

        assertNotNull(breakdown)
        assertTrue(breakdown.overall >= 0.0f && breakdown.overall <= 1.0f)
        assertTrue(breakdown.dataCompleteness >= 0.0f && breakdown.dataCompleteness <= 1.0f)
        assertTrue(breakdown.textQuality >= 0.0f && breakdown.textQuality <= 1.0f)
        assertTrue(breakdown.mathematicalConsistency >= 0.0f && breakdown.mathematicalConsistency <= 1.0f)
        assertTrue(breakdown.formatConsistency >= 0.0f && breakdown.formatConsistency <= 1.0f)
        assertTrue(breakdown.businessLogic >= 0.0f && breakdown.businessLogic <= 1.0f)
        assertTrue(breakdown.ocrAccuracy >= 0.0f && breakdown.ocrAccuracy <= 1.0f)
    }

    @Test
    fun `should handle edge cases gracefully`() {
        // Empty receipt
        val emptyReceipt = ReceiptData()
        val emptyConfidence = calculator.calculateOverallConfidence(emptyReceipt)
        assertTrue(emptyConfidence >= 0.0f && emptyConfidence <= 1.0f)

        // Receipt with null values
        val nullReceipt = ReceiptData(
            merchantName = null,
            date = null,
            total = null,
            currency = "",
            items = emptyList()
        )
        val nullConfidence = calculator.calculateOverallConfidence(nullReceipt)
        assertTrue(nullConfidence >= 0.0f && nullConfidence <= 1.0f)

        // Very long text
        val longText = "A".repeat(10000)
        val longTextQuality = calculator.analyzeTextQuality(longText)
        assertTrue(longTextQuality >= 0.0f && longTextQuality <= 1.0f)
    }

    @Test
    fun `should adjust confidence for Saudi-specific patterns`() {
        // Saudi receipt with Arabic elements
        val saudiReceipt = ReceiptData(
            merchantName = "هايبر بندة", // Arabic name
            date = "١٥/٠١/٢٠٢٤", // Arabic numerals
            total = "١٢٥.٥٠", // Arabic numerals
            currency = "SAR",
            tax = "18.75", // 15% VAT
            rawText = "هايبر بندة\nالتاريخ: ١٥/٠١/٢٠٢٤\nالمجموع: ١٢٥.٥٠ ريال"
        )

        val saudiConfidence = calculator.calculateOverallConfidence(saudiReceipt)
        
        // Should handle Arabic text properly and not penalize it
        assertTrue(saudiConfidence > 0.6f, "Saudi receipt with Arabic should have reasonable confidence")

        // International receipt for comparison
        val internationalReceipt = ReceiptData(
            merchantName = "International Store",
            date = "15/01/2024",
            total = "125.50",
            currency = "USD", // Non-SAR currency
            tax = "12.55", // Non-standard VAT rate
            rawText = "International Store\nDate: 15/01/2024\nTotal: 125.50 USD"
        )

        val intlConfidence = calculator.calculateOverallConfidence(internationalReceipt)
        
        // Should still be valid but might have slightly different scoring
        assertTrue(intlConfidence > 0.6f, "International receipt should also have reasonable confidence")
    }

    @Test
    fun `should provide confidence improvement suggestions`() {
        val lowConfidenceReceipt = ReceiptData(
            total = "abc", // Invalid format
            currency = "INVALID"
        )

        val suggestions = calculator.getSuggestions(lowConfidenceReceipt)
        
        assertNotNull(suggestions)
        assertTrue(suggestions.isNotEmpty(), "Should provide suggestions for low confidence receipt")
        assertTrue(suggestions.any { it.contains("merchant") }, "Should suggest adding merchant")
        assertTrue(suggestions.any { it.contains("date") }, "Should suggest adding date")
        assertTrue(suggestions.any { it.contains("format") }, "Should suggest fixing format issues")
    }
}