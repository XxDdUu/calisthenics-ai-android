package com.calisthenics.ai.ui.workoutplan.details

import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Utility for parsing and formatting workout plan dates.
 *
 * Provides date labels in the exact format:
 * - MON 16
 * - TUE 17
 * - WED 18
 * - THU 19
 * - FRI 20
 * - SAT 21
 * - SUN 22
 *
 * Uppercase English weekday abbreviations are always used, and dates are
 * calculated sequentially from the plan's actual start date respecting
 * the user's local timezone.
 */
object WorkoutPlanDateFormatter {

    /**
     * Parses a start date string into a [LocalDate] respecting the given [zoneId].
     * Supports ISO date (yyyy-MM-dd), ISO Instant / ZonedDateTime / OffsetDateTime,
     * and LocalDateTime. Returns null if invalid or blank.
     */
    fun parseStartDate(
        startDate: String?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate? {
        if (startDate.isNullOrBlank()) return null
        val trimmed = startDate.trim()

        // 1. Standard ISO date "yyyy-MM-dd"
        if (trimmed.length == 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            try {
                return LocalDate.parse(trimmed)
            } catch (_: Exception) { }
        }

        // 2. ISO DateTime with offset / zone (e.g. "2026-11-16T10:00:00+07:00")
        try {
            return ZonedDateTime.parse(trimmed)
                .withZoneSameInstant(zoneId)
                .toLocalDate()
        } catch (_: Exception) { }

        // 3. ISO Instant (e.g. "2026-11-16T00:00:00Z")
        try {
            return Instant.parse(trimmed)
                .atZone(zoneId)
                .toLocalDate()
        } catch (_: Exception) { }

        // 4. OffsetDateTime
        try {
            return OffsetDateTime.parse(trimmed)
                .atZoneSameInstant(zoneId)
                .toLocalDate()
        } catch (_: Exception) { }

        // 5. LocalDateTime without offset
        try {
            return LocalDateTime.parse(trimmed).toLocalDate()
        } catch (_: Exception) { }

        return null
    }

    /**
     * Calculates the sequential date for a given [dayOfWeek] starting from [startLocalDate].
     *
     * Day offset is calculated sequentially from the plan start date:
     * offset = (dayOfWeek.order - startLocalDate.dayOfWeek.value + 7) % 7
     *
     * This guarantees:
     * 1. 7 sequential days starting on startLocalDate.
     * 2. Calculated date's actual dayOfWeek matches dayOfWeek.order.
     * 3. Seamless month and year boundary transitions.
     */
    fun calculateDateForDay(
        dayOfWeek: WorkoutDayOfWeek,
        startLocalDate: LocalDate
    ): LocalDate {
        val dayOffset = (dayOfWeek.order - startLocalDate.dayOfWeek.value + 7) % 7
        return startLocalDate.plusDays(dayOffset.toLong())
    }

    /**
     * Returns the uppercase English 3-letter weekday abbreviation:
     * MON, TUE, WED, THU, FRI, SAT, SUN.
     */
    fun getEnglishWeekdayAbbreviation(dayOfWeek: WorkoutDayOfWeek): String {
        return when (dayOfWeek) {
            WorkoutDayOfWeek.MONDAY -> "MON"
            WorkoutDayOfWeek.TUESDAY -> "TUE"
            WorkoutDayOfWeek.WEDNESDAY -> "WED"
            WorkoutDayOfWeek.THURSDAY -> "THU"
            WorkoutDayOfWeek.FRIDAY -> "FRI"
            WorkoutDayOfWeek.SATURDAY -> "SAT"
            WorkoutDayOfWeek.SUNDAY -> "SUN"
        }
    }

    /**
     * Formats the workout day into the exact requested format:
     * "MON 16", "TUE 17", "WED 18", "THU 19", "FRI 20", "SAT 21", "SUN 22".
     *
     * Returns null if [startLocalDate] is null, allowing caller to fall back
     * to existing schedule logic rather than generating misleading dates.
     */
    fun formatWorkoutDayDateLabel(
        dayOfWeek: WorkoutDayOfWeek,
        startLocalDate: LocalDate?
    ): String? {
        if (startLocalDate == null) return null
        val date = calculateDateForDay(dayOfWeek, startLocalDate)
        val abbreviation = getEnglishWeekdayAbbreviation(dayOfWeek)
        return "$abbreviation ${date.dayOfMonth}"
    }

    /**
     * Overload accepting a raw start date string.
     */
    fun formatWorkoutDayDateLabel(
        dayOfWeek: WorkoutDayOfWeek,
        startDate: String?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val startLocalDate = parseStartDate(startDate, zoneId) ?: return null
        return formatWorkoutDayDateLabel(dayOfWeek, startLocalDate)
    }
}
