package com.example.ai.datetime

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ParsedDateTime(
    val timeMillis: Long,
    val formattedHint: String,
    val isAmbiguous: Boolean = false,
    val disambiguationQuestion: String? = null
)

/**
 * Robust Tunisian / Arabic natural language datetime parser for Rafiqah V3.
 */
object NaturalDateTimeParser {

    /**
     * Parse Tunisian and Arabic time expressions into concrete timestamps.
     * Examples:
     * - "غدوة على الثمانية" (tomorrow at 8)
     * - "غدوة الصباح على الثمانية" (tomorrow morning at 8:00)
     * - "بعد ساعتين" (after 2 hours)
     * - "بعد ساعة" (after 1 hour)
     * - "اليوم في الليل على التسعة" (tonight at 21:00)
     * - "يوم الاثنين على 10" (Monday at 10:00)
     * - "العشرة" (10 - ambiguous AM or PM without context)
     */
    fun parse(expression: String, referenceTimeMillis: Long = System.currentTimeMillis()): ParsedDateTime? {
        val text = expression.trim().lowercase(Locale.ROOT)
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = referenceTimeMillis
        }

        // Relative offsets: بعد ساعة / بعد ساعتين / بعد نصف ساعة
        if (text.contains("بعد ساعة") || text.contains("بعد ساعه")) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
            return ParsedDateTime(
                timeMillis = cal.timeInMillis,
                formattedHint = "بعد ساعة (${formatTime(cal)})"
            )
        }
        if (text.contains("بعد ساعتين")) {
            cal.add(Calendar.HOUR_OF_DAY, 2)
            return ParsedDateTime(
                timeMillis = cal.timeInMillis,
                formattedHint = "بعد ساعتين (${formatTime(cal)})"
            )
        }
        if (text.contains("بعد نصف ساعة") || text.contains("بعد نص ساعة")) {
            cal.add(Calendar.MINUTE, 30)
            return ParsedDateTime(
                timeMillis = cal.timeInMillis,
                formattedHint = "بعد 30 دقيقة (${formatTime(cal)})"
            )
        }

        val hasTomorrow = text.contains("غدوة") || text.contains("غدا") || text.contains("بكرة")
        val hasAfterTomorrow = text.contains("بعد غدوة") || text.contains("بعد غدا")
        val isMorning = text.contains("الصباح") || text.contains("صباحا") || text.contains("صباح")
        val isNight = text.contains("الليل") || text.contains("مساء") || text.contains("العشية") || text.contains("ليلا")

        if (hasAfterTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
        } else if (hasTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Extract hour number
        val hourMatch = Regex("(\\d{1,2})").find(text)
        val wordHour = when {
            text.contains("الثمانية") || text.contains("الـ8") || text.contains("8") -> 8
            text.contains("التسعة") || text.contains("الـ9") || text.contains("9") -> 9
            text.contains("العشرة") || text.contains("الـ10") || text.contains("10") -> 10
            text.contains("الحداش") || text.contains("الحادية عشرة") || text.contains("11") -> 11
            text.contains("نصف النهار") || text.contains("الثانية عشرة") || text.contains("12") -> 12
            text.contains("الواحدة") || text.contains("الوحدة") || text.contains("1") -> 1
            text.contains("الثانية") || text.contains("الساعتين") || text.contains("2") -> 2
            text.contains("الثلاثة") || text.contains("3") -> 3
            text.contains("الأربعة") || text.contains("الاربعة") || text.contains("4") -> 4
            text.contains("الخمسة") || text.contains("5") -> 5
            text.contains("الستة") || text.contains("6") -> 6
            text.contains("السبعة") || text.contains("7") -> 7
            else -> hourMatch?.value?.toIntOrNull()
        } ?: return null

        var targetHour = wordHour
        if (isNight && targetHour in 1..11) {
            targetHour += 12
        } else if (!isMorning && !isNight && targetHour in 1..7) {
            // Usually 1-7 in daytime implies afternoon in daily context unless specified
            targetHour += 12
        }

        // Check ambiguity if neither morning nor evening was stated and hour is 8, 9, 10
        val isAmbiguous = (!isMorning && !isNight && (wordHour in 8..11) && !text.contains(":"))
        val question = if (isAmbiguous) {
            "يا أمي تقصدي ${wordHour} متاع الصباح ولا ${wordHour} في الليل؟"
        } else null

        cal.set(Calendar.HOUR_OF_DAY, targetHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If today and hour has already passed, adjust to tomorrow
        if (!hasTomorrow && !hasAfterTomorrow && cal.timeInMillis < referenceTimeMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dayLabel = if (hasAfterTomorrow) "بعد غدوة" else if (hasTomorrow) "غدوة" else "اليوم"
        val timeLabel = String.format(Locale.ROOT, "%02d:00", targetHour)

        return ParsedDateTime(
            timeMillis = cal.timeInMillis,
            formattedHint = "$dayLabel على $timeLabel",
            isAmbiguous = isAmbiguous,
            disambiguationQuestion = question
        )
    }

    private fun formatTime(calendar: Calendar): String {
        return String.format(
            Locale.ROOT,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
}
