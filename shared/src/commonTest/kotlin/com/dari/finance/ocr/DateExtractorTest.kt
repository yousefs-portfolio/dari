package com.dari.finance.ocr

import kotlin.test.*

class DateExtractorTest {

    private val dateExtractor = DateExtractor()

    @Test
    fun `should extract common date formats`() {
        val testCases = listOf(
            "Date: 15/01/2024" to "15/01/2024",
            "15-01-2024" to "15-01-2024", 
            "2024-01-15" to "2024-01-15",
            "Jan 15, 2024" to "Jan 15, 2024",
            "15 January 2024" to "15 January 2024",
            "15 Jan 2024" to "15 Jan 2024",
            "January 15th, 2024" to "January 15th, 2024"
        )

        testCases.forEach { (input, expected) ->
            val result = dateExtractor.extractDate(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `should extract Arabic date formats`() {
        val testCases = listOf(
            "التاريخ: ١٥/٠١/٢٠٢٤" to "15/01/2024",
            "١٥-٠١-٢٠٢٤" to "15-01-2024",
            "٢٠٢٤/٠١/١٥" to "2024/01/15",
            "٣١ ديسمبر ٢٠٢٣" to "31 December 2023"
        )

        testCases.forEach { (input, expected) ->
            val result = dateExtractor.extractDate(input)
            assertEquals(expected, result, "Failed for Arabic input: $input")
        }
    }

    @Test
    fun `should extract Hijri dates`() {
        val testCases = listOf(
            "١٥ محرم ١٤٤٥ هـ" to "15 Muharram 1445 AH",
            "٢٧ رمضان ١٤٤٤" to "27 Ramadan 1444",
            "١٠ ذو الحجة ١٤٤٥" to "10 Dhul Hijjah 1445"
        )

        testCases.forEach { (input, expected) ->
            val result = dateExtractor.extractHijriDate(input)
            assertEquals(expected, result, "Failed for Hijri input: $input")
        }
    }

    @Test
    fun `should extract date and time combinations`() {
        val testCases = listOf(
            "15/01/2024 14:30" to DateTimeInfo("15/01/2024", "14:30"),
            "2024-01-15T14:30:45" to DateTimeInfo("2024-01-15", "14:30:45"),
            "Jan 15, 2024 at 2:30 PM" to DateTimeInfo("Jan 15, 2024", "2:30 PM"),
            "التاريخ: ١٥/٠١/٢٠٢٤ الوقت: ١٤:٣٠" to DateTimeInfo("15/01/2024", "14:30")
        )

        testCases.forEach { (input, expected) ->
            val result = dateExtractor.extractDateTime(input)
            assertEquals(expected.date, result?.date, "Date failed for: $input")
            assertEquals(expected.time, result?.time, "Time failed for: $input")
        }
    }

    @Test
    fun `should prioritize recent dates over future dates`() {
        val currentYear = 2024
        val receiptText = """
            Store Receipt
            Expiry: 15/01/2025
            Purchase Date: 15/01/2024
            Valid until: 31/12/2025
        """.trimIndent()

        val result = dateExtractor.extractMostLikelyDate(receiptText)
        assertEquals("15/01/2024", result) // Should prefer the purchase date
    }

    @Test
    fun `should detect invalid dates`() {
        val invalidDates = listOf(
            "32/01/2024", // Invalid day
            "15/13/2024", // Invalid month
            "15/01/1900", // Too old
            "15/01/2050", // Too far in future
            "00/01/2024", // Zero day
            "15/00/2024"  // Zero month
        )

        invalidDates.forEach { invalidDate ->
            val isValid = dateExtractor.isValidDate(invalidDate)
            assertFalse(isValid, "Should be invalid: $invalidDate")
        }
    }

    @Test
    fun `should validate reasonable date ranges`() {
        val validDates = listOf(
            "15/01/2024",
            "31/12/2023", 
            "01/01/2024",
            "29/02/2024" // Leap year
        )

        val invalidDates = listOf(
            "15/01/1990", // Too old for receipt
            "15/01/2030", // Too far in future
            "29/02/2023", // Not a leap year
            "31/04/2024"  // April doesn't have 31 days
        )

        validDates.forEach { date ->
            assertTrue(dateExtractor.isValidDate(date), "Should be valid: $date")
        }

        invalidDates.forEach { date ->
            assertFalse(dateExtractor.isValidDate(date), "Should be invalid: $date")
        }
    }

    @Test
    fun `should extract date with confidence scoring`() {
        val testCases = listOf(
            // High confidence - explicit date label
            "Date: 15/01/2024" to 0.9f,
            // Medium confidence - date format but no label
            "15/01/2024" to 0.7f,
            // Lower confidence - partial date info
            "Jan 2024" to 0.4f,
            // Low confidence - ambiguous
            "15/01" to 0.3f
        )

        testCases.forEach { (input, expectedMinConfidence) ->
            val result = dateExtractor.extractDateWithConfidence(input)
            assertNotNull(result, "Should extract date from: $input")
            assertTrue(result.confidence >= expectedMinConfidence,
                "Confidence too low for: $input (got ${result.confidence})")
        }
    }

    @Test
    fun `should handle different calendar systems`() {
        val receiptText = """
            LULU HYPERMARKET
            Date: 15/01/2024 - ١٥/٠١/٢٠٢٤
            Hijri: ٣ رجب ١٤٤٥ هـ
            Time: 14:30
        """.trimIndent()

        val dateInfo = dateExtractor.extractAllDateFormats(receiptText)
        
        assertNotNull(dateInfo.gregorianDate)
        assertNotNull(dateInfo.hijriDate)
        assertEquals("15/01/2024", dateInfo.gregorianDate)
        assertTrue(dateInfo.hijriDate!!.contains("رجب"))
    }

    @Test
    fun `should normalize date formats`() {
        val testCases = listOf(
            "15/1/2024" to "15/01/2024",
            "15-01-24" to "15-01-2024", 
            "Jan 15, 2024" to "2024-01-15",
            "15th January 2024" to "2024-01-15",
            "١٥/١/٢٠٢ل" to "15/01/2024"
        )

        testCases.forEach { (input, expected) ->
            val result = dateExtractor.normalizeDate(input)
            assertEquals(expected, result, "Failed to normalize: $input")
        }
    }

    @Test
    fun `should extract receipt expiry dates`() {
        val receiptText = """
            CARREFOUR RECEIPT
            Purchase: 15/01/2024
            Return Policy: 30 days
            Valid until: 14/02/2024
            Warranty expires: 15/01/2025
        """.trimIndent()

        val dates = dateExtractor.extractAllDates(receiptText)
        
        assertTrue(dates.any { it.contains("15/01/2024") })
        assertTrue(dates.any { it.contains("14/02/2024") })
        assertTrue(dates.any { it.contains("15/01/2025") })
        assertEquals(3, dates.size)
    }

    @Test
    fun `should handle multilingual month names`() {
        val monthTests = listOf(
            "15 يناير 2024" to "15 January 2024",
            "٣٠ رمضان ١٤٤٥" to "30 Ramadan 1445",
            "25 Dec 2024" to "25 December 2024"
        )

        monthTests.forEach { (input, expected) ->
            val result = dateExtractor.translateMonthNames(input)
            assertTrue(result.contains("January") || result.contains("Ramadan") || result.contains("December"),
                "Failed to translate months in: $input -> $result")
        }
    }

    @Test
    fun `should detect date context keywords`() {
        val contextTests = listOf(
            "Purchase date: 15/01/2024" to DateContext.PURCHASE,
            "Expiry: 31/12/2024" to DateContext.EXPIRY,
            "Return by: 15/02/2024" to DateContext.RETURN,
            "Valid until: 30/06/2024" to DateContext.VALIDITY,
            "Printed on: 15/01/2024" to DateContext.PRINTED
        )

        contextTests.forEach { (input, expectedContext) ->
            val context = dateExtractor.detectDateContext(input)
            assertEquals(expectedContext, context, "Wrong context for: $input")
        }
    }

    @Test
    fun `should handle edge cases gracefully`() {
        val edgeCases = listOf(
            "",
            "No dates here",
            "123456789",
            "Random text without any date information",
            "Date: invalid",
            "التاريخ: غير صحيح"
        )

        edgeCases.forEach { input ->
            val result = dateExtractor.extractDate(input)
            // Should not crash and return null for no valid dates
            if (result != null) {
                assertTrue(dateExtractor.isValidDate(result), "Invalid date returned: $result")
            }
        }
    }

    @Test
    fun `should extract time components separately`() {
        val timeTests = listOf(
            "14:30" to TimeInfo(14, 30),
            "2:30 PM" to TimeInfo(14, 30),
            "09:15 AM" to TimeInfo(9, 15),
            "١٤:٣٠" to TimeInfo(14, 30),
            "23:59" to TimeInfo(23, 59)
        )

        timeTests.forEach { (input, expected) ->
            val result = dateExtractor.extractTimeComponents(input)
            assertNotNull(result, "Failed to extract time from: $input")
            assertEquals(expected.hour, result.hour)
            assertEquals(expected.minute, result.minute)
        }
    }

    @Test
    fun `should prefer transaction dates over printed dates`() {
        val receiptText = """
            Receipt printed: 16/01/2024 10:00
            Transaction date: 15/01/2024 14:30
            System date: 17/01/2024
        """.trimIndent()

        val primaryDate = dateExtractor.extractPrimaryDate(receiptText)
        assertEquals("15/01/2024", primaryDate) // Should prefer transaction date
    }

    @Test
    fun `should handle relative date expressions`() {
        val relativeTests = listOf(
            "Today" to DateType.TODAY,
            "Yesterday" to DateType.YESTERDAY,
            "أمس" to DateType.YESTERDAY,
            "اليوم" to DateType.TODAY
        )

        // Note: Actual date calculation would depend on current date
        relativeTests.forEach { (input, expectedType) ->
            val type = dateExtractor.classifyDateExpression(input)
            assertEquals(expectedType, type, "Wrong classification for: $input")
        }
    }
}

data class DateTimeInfo(val date: String, val time: String)
data class TimeInfo(val hour: Int, val minute: Int)

data class DateWithConfidence(
    val date: String,
    val confidence: Float
)

data class ComprehensiveDateInfo(
    val gregorianDate: String?,
    val hijriDate: String?,
    val timeInfo: String?
)

enum class DateContext {
    PURCHASE, EXPIRY, RETURN, VALIDITY, PRINTED, TRANSACTION
}

enum class DateType {
    TODAY, YESTERDAY, ABSOLUTE, RELATIVE
}