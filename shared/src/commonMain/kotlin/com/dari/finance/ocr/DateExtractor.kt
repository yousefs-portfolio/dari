package com.dari.finance.ocr

/**
 * Date extraction utility for OCR receipt processing
 * Handles Gregorian, Hijri, and Arabic date formats common in Saudi Arabia
 */
class DateExtractor {

    // Arabic to English number mapping
    private val arabicNumbers = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    // Gregorian months in Arabic
    private val arabicMonths = mapOf(
        "يناير" to "January", "فبراير" to "February", "مارس" to "March",
        "أبريل" to "April", "مايو" to "May", "يونيو" to "June",
        "يوليو" to "July", "أغسطس" to "August", "سبتمبر" to "September",
        "أكتوبر" to "October", "نوفمبر" to "November", "ديسمبر" to "December"
    )

    // Hijri months
    private val hijriMonths = mapOf(
        "محرم" to "Muharram", "صفر" to "Safar", "ربيع الأول" to "Rabi al-Awwal",
        "ربيع الآخر" to "Rabi al-Thani", "جمادى الأولى" to "Jumada al-Awwal",
        "جمادى الآخرة" to "Jumada al-Thani", "رجب" to "Rajab",
        "شعبان" to "Shaban", "رمضان" to "Ramadan", "شوال" to "Shawwal",
        "ذو القعدة" to "Dhul Qadah", "ذو الحجة" to "Dhul Hijjah"
    )

    // Date context keywords
    private val contextKeywords = mapOf(
        DateContext.PURCHASE to listOf("date", "purchase", "bought", "التاريخ", "الشراء"),
        DateContext.TRANSACTION to listOf("transaction", "trans", "المعاملة"),
        DateContext.EXPIRY to listOf("expiry", "expires", "valid until", "انتهاء", "صالح حتى"),
        DateContext.RETURN to listOf("return by", "return", "استرداد"),
        DateContext.PRINTED to listOf("printed", "generated", "طباعة"),
        DateContext.VALIDITY to listOf("valid", "validity", "صالح")
    )

    /**
     * Extracts primary date from receipt text
     */
    fun extractDate(text: String): String? {
        // Try different date patterns in order of preference
        val patterns = listOf(
            // DD/MM/YYYY or DD-MM-YYYY
            Regex("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})"),
            // YYYY-MM-DD (ISO format)
            Regex("(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})"),
            // DD Month YYYY
            Regex("(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{4})", RegexOption.IGNORE_CASE),
            // Month DD, YYYY
            Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{1,2}),?\\s+(\\d{4})", RegexOption.IGNORE_CASE),
            // Arabic date patterns
            Regex("([٠-٩]{1,2})[/\\-]([٠-٩]{1,2})[/\\-]([٠-٩]{4})")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val dateString = match.value
                val normalizedDate = convertArabicNumbers(dateString)
                
                if (isValidDate(normalizedDate)) {
                    return normalizedDate
                }
            }
        }

        return null
    }

    /**
     * Extracts Hijri date from text
     */
    fun extractHijriDate(text: String): String? {
        // Pattern for Hijri dates: DD MonthName YYYY هـ
        val hijriPattern = Regex("([٠-٩\\d]{1,2})\\s+(${hijriMonths.keys.joinToString("|")})\\s+([٠-٩\\d]{4})\\s*(?:هـ)?")
        
        val match = hijriPattern.find(text)
        if (match != null) {
            val day = convertArabicNumbers(match.groupValues[1])
            val monthArabic = match.groupValues[2]
            val year = convertArabicNumbers(match.groupValues[3])
            val monthEnglish = hijriMonths[monthArabic] ?: monthArabic
            
            return "$day $monthEnglish $year AH"
        }
        
        return null
    }

    /**
     * Extracts date and time information together
     */
    fun extractDateTime(text: String): DateTimeInfo? {
        val date = extractDate(text)
        val time = extractTime(text)
        
        return if (date != null) {
            DateTimeInfo(date, time ?: "")
        } else null
    }

    /**
     * Extracts the most likely transaction date from receipt
     */
    fun extractMostLikelyDate(text: String): String? {
        val allDates = extractAllDates(text)
        if (allDates.isEmpty()) return null

        // Prefer dates with transaction context
        val transactionDate = findDateByContext(text, DateContext.TRANSACTION)
        if (transactionDate != null) return transactionDate

        val purchaseDate = findDateByContext(text, DateContext.PURCHASE)
        if (purchaseDate != null) return purchaseDate

        // Filter out future dates (likely expiry dates)
        val currentYear = 2024 // This would be dynamic in real implementation
        val likelyDates = allDates.filter { date ->
            val year = extractYear(date)
            year != null && year <= currentYear
        }

        return likelyDates.firstOrNull() ?: allDates.first()
    }

    /**
     * Validates if a date string represents a reasonable receipt date
     */
    fun isValidDate(dateString: String): Boolean {
        try {
            val normalizedDate = convertArabicNumbers(dateString)
            
            // Check basic format
            if (!normalizedDate.matches(Regex("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{4}")) &&
                !normalizedDate.matches(Regex("\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}")) &&
                !normalizedDate.contains(Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)", RegexOption.IGNORE_CASE))) {
                return false
            }

            // Extract components for validation
            val year = extractYear(normalizedDate) ?: return false
            val components = parseDateComponents(normalizedDate) ?: return false
            
            // Reasonable year range for receipts
            if (year < 2020 || year > 2030) return false
            
            // Validate month
            if (components.month < 1 || components.month > 12) return false
            
            // Validate day
            if (components.day < 1 || components.day > 31) return false
            
            // Basic month-day validation
            val daysInMonth = when (components.month) {
                2 -> if (isLeapYear(year)) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
            
            return components.day <= daysInMonth
            
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Extracts date with confidence score
     */
    fun extractDateWithConfidence(text: String): DateWithConfidence? {
        val date = extractDate(text) ?: return null
        
        var confidence = 0.5f // Base confidence
        
        // Increase confidence for context keywords
        val lowerText = text.lowercase()
        if (contextKeywords[DateContext.PURCHASE]?.any { lowerText.contains(it) } == true) confidence += 0.3f
        if (contextKeywords[DateContext.TRANSACTION]?.any { lowerText.contains(it) } == true) confidence += 0.2f
        
        // Increase confidence for proper format
        if (date.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) confidence += 0.2f
        
        // Decrease confidence for future dates (likely expiry)
        val year = extractYear(date)
        if (year != null && year > 2024) confidence -= 0.2f
        
        return DateWithConfidence(date, confidence.coerceIn(0.0f, 1.0f))
    }

    /**
     * Extracts all date formats present in text
     */
    fun extractAllDateFormats(text: String): ComprehensiveDateInfo {
        return ComprehensiveDateInfo(
            gregorianDate = extractDate(text),
            hijriDate = extractHijriDate(text),
            timeInfo = extractTime(text)
        )
    }

    /**
     * Normalizes date to standard format
     */
    fun normalizeDate(dateString: String): String {
        val normalized = convertArabicNumbers(dateString)
        
        // Handle different formats
        return when {
            // DD/MM/YYYY -> DD/MM/YYYY (keep as is)
            normalized.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}")) -> {
                val parts = normalized.split("/")
                "${parts[0].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[2]}"
            }
            
            // DD-MM-YYYY -> DD-MM-YYYY (keep as is) 
            normalized.matches(Regex("\\d{1,2}-\\d{1,2}-\\d{4}")) -> {
                val parts = normalized.split("-")
                "${parts[0].padStart(2, '0')}-${parts[1].padStart(2, '0')}-${parts[2]}"
            }
            
            // YYYY-MM-DD -> YYYY-MM-DD (ISO format)
            normalized.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}")) -> {
                val parts = normalized.split("-")
                "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
            }
            
            // Month name formats -> YYYY-MM-DD
            normalized.contains(Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)", RegexOption.IGNORE_CASE)) -> {
                convertTextDateToISO(normalized)
            }
            
            else -> normalized
        }
    }

    /**
     * Extracts all dates found in text
     */
    fun extractAllDates(text: String): List<String> {
        val dates = mutableListOf<String>()
        val patterns = listOf(
            Regex("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{4}"),
            Regex("\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}"),
            Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}", RegexOption.IGNORE_CASE),
            Regex("[٠-٩]{1,2}[/\\-][٠-٩]{1,2}[/\\-][٠-٩]{4}")
        )

        for (pattern in patterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                val dateString = convertArabicNumbers(match.value)
                if (isValidDate(dateString)) {
                    dates.add(dateString)
                }
            }
        }

        return dates.distinct()
    }

    /**
     * Translates month names from Arabic to English
     */
    fun translateMonthNames(text: String): String {
        var result = text
        
        for ((arabic, english) in arabicMonths) {
            result = result.replace(arabic, english)
        }
        
        for ((arabic, english) in hijriMonths) {
            result = result.replace(arabic, english)
        }
        
        return result
    }

    /**
     * Detects date context based on surrounding keywords
     */
    fun detectDateContext(text: String): DateContext {
        val lowerText = text.lowercase()
        
        for ((context, keywords) in contextKeywords) {
            if (keywords.any { lowerText.contains(it) }) {
                return context
            }
        }
        
        return DateContext.PURCHASE // Default context
    }

    /**
     * Extracts time components from text
     */
    fun extractTimeComponents(timeString: String): TimeInfo? {
        val timePatterns = listOf(
            // 24-hour format
            Regex("([\\d٠-٩]{1,2}):([\\d٠-٩]{2})"),
            // 12-hour format  
            Regex("([\\d٠-٩]{1,2}):([\\d٠-٩]{2})\\s*(AM|PM)", RegexOption.IGNORE_CASE)
        )

        for (pattern in timePatterns) {
            val match = pattern.find(timeString)
            if (match != null) {
                val hour = convertArabicNumbers(match.groupValues[1]).toIntOrNull() ?: continue
                val minute = convertArabicNumbers(match.groupValues[2]).toIntOrNull() ?: continue
                
                val adjustedHour = if (match.groupValues.size > 3) {
                    val period = match.groupValues[3].uppercase()
                    when {
                        period == "PM" && hour != 12 -> hour + 12
                        period == "AM" && hour == 12 -> 0
                        else -> hour
                    }
                } else hour

                if (adjustedHour in 0..23 && minute in 0..59) {
                    return TimeInfo(adjustedHour, minute)
                }
            }
        }

        return null
    }

    /**
     * Extracts primary transaction date
     */
    fun extractPrimaryDate(text: String): String? {
        // Priority order for date extraction
        val priorities = listOf(
            DateContext.TRANSACTION,
            DateContext.PURCHASE,
            DateContext.PRINTED
        )

        for (context in priorities) {
            val date = findDateByContext(text, context)
            if (date != null) return date
        }

        // Fallback to any valid date
        return extractDate(text)
    }

    /**
     * Classifies date expression type
     */
    fun classifyDateExpression(expression: String): DateType {
        val lower = expression.lowercase()
        return when {
            lower.contains("today") || lower.contains("اليوم") -> DateType.TODAY
            lower.contains("yesterday") || lower.contains("أمس") -> DateType.YESTERDAY
            lower.matches(Regex("\\d+[/\\-]\\d+[/\\-]\\d+")) -> DateType.ABSOLUTE
            else -> DateType.RELATIVE
        }
    }

    private fun extractTime(text: String): String? {
        val timePattern = Regex("([\\d٠-٩]{1,2}):([\\d٠-٩]{2})(?::([\\d٠-٩]{2}))?(?:\\s*(AM|PM))?", RegexOption.IGNORE_CASE)
        val match = timePattern.find(text)
        return match?.let { convertArabicNumbers(it.value) }
    }

    private fun findDateByContext(text: String, context: DateContext): String? {
        val keywords = contextKeywords[context] ?: return null
        val lowerText = text.lowercase()
        
        for (keyword in keywords) {
            val keywordIndex = lowerText.indexOf(keyword)
            if (keywordIndex != -1) {
                // Look for date near the keyword
                val contextWindow = text.substring(
                    maxOf(0, keywordIndex - 20),
                    minOf(text.length, keywordIndex + keyword.length + 30)
                )
                val date = extractDate(contextWindow)
                if (date != null) return date
            }
        }
        
        return null
    }

    private fun extractYear(dateString: String): Int? {
        val yearPattern = Regex("(\\d{4})")
        return yearPattern.find(dateString)?.value?.toIntOrNull()
    }

    private fun parseDateComponents(dateString: String): DateComponents? {
        val ddmmyyyy = Regex("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})")
        val yyyymmdd = Regex("(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})")
        
        val match1 = ddmmyyyy.find(dateString)
        if (match1 != null) {
            return DateComponents(
                match1.groupValues[1].toInt(),
                match1.groupValues[2].toInt(),
                match1.groupValues[3].toInt()
            )
        }
        
        val match2 = yyyymmdd.find(dateString)
        if (match2 != null) {
            return DateComponents(
                match2.groupValues[3].toInt(),
                match2.groupValues[2].toInt(),
                match2.groupValues[1].toInt()
            )
        }
        
        return null
    }

    private fun convertTextDateToISO(dateString: String): String {
        // This would contain logic to convert "Jan 15, 2024" -> "2024-01-15"
        // Simplified implementation
        return dateString // For now, return as-is
    }

    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }

    private fun convertArabicNumbers(text: String): String {
        var result = text
        for ((arabic, english) in arabicNumbers) {
            result = result.replace(arabic, english)
        }
        return result
    }
}

private data class DateComponents(val day: Int, val month: Int, val year: Int)