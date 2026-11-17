package com.calisthenics.ai.ui.workoutplan.details

import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkoutPlanDateFormatterTest {

    @Test
    fun exactFormatForStandardWeek() {
        val startDate = "2026-11-16" // Monday
        val startLocalDate = LocalDate.of(2026, 11, 16)

        assertEquals("MON 16", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, startLocalDate))
        assertEquals("TUE 17", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.TUESDAY, startLocalDate))
        assertEquals("WED 18", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.WEDNESDAY, startLocalDate))
        assertEquals("THU 19", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.THURSDAY, startLocalDate))
        assertEquals("FRI 20", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.FRIDAY, startLocalDate))
        assertEquals("SAT 21", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SATURDAY, startLocalDate))
        assertEquals("SUN 22", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SUNDAY, startLocalDate))

        // Same using string overload
        assertEquals("MON 16", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, startDate))
        assertEquals("TUE 17", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.TUESDAY, startDate))
        assertEquals("WED 18", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.WEDNESDAY, startDate))
        assertEquals("THU 19", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.THURSDAY, startDate))
        assertEquals("FRI 20", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.FRIDAY, startDate))
        assertEquals("SAT 21", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SATURDAY, startDate))
        assertEquals("SUN 22", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SUNDAY, startDate))
    }

    @Test
    fun englishUppercaseAbbreviations() {
        assertEquals("MON", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.MONDAY))
        assertEquals("TUE", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.TUESDAY))
        assertEquals("WED", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.WEDNESDAY))
        assertEquals("THU", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.THURSDAY))
        assertEquals("FRI", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.FRIDAY))
        assertEquals("SAT", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.SATURDAY))
        assertEquals("SUN", WorkoutPlanDateFormatter.getEnglishWeekdayAbbreviation(WorkoutDayOfWeek.SUNDAY))
    }

    @Test
    fun monthBoundaryHandling() {
        // Starts on Wednesday Oct 28, 2026
        val startLocalDate = LocalDate.of(2026, 10, 28)

        assertEquals("WED 28", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.WEDNESDAY, startLocalDate))
        assertEquals("THU 29", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.THURSDAY, startLocalDate))
        assertEquals("FRI 30", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.FRIDAY, startLocalDate))
        assertEquals("SAT 31", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SATURDAY, startLocalDate))
        // Transitions into November:
        assertEquals("SUN 1", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SUNDAY, startLocalDate))
        assertEquals("MON 2", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, startLocalDate))
        assertEquals("TUE 3", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.TUESDAY, startLocalDate))
    }

    @Test
    fun yearBoundaryHandling() {
        // Starts on Monday Dec 28, 2026
        val startLocalDate = LocalDate.of(2026, 12, 28)

        assertEquals("MON 28", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, startLocalDate))
        assertEquals("TUE 29", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.TUESDAY, startLocalDate))
        assertEquals("WED 30", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.WEDNESDAY, startLocalDate))
        assertEquals("THU 31", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.THURSDAY, startLocalDate))
        // Transitions into January 2027:
        assertEquals("FRI 1", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.FRIDAY, startLocalDate))
        assertEquals("SAT 2", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SATURDAY, startLocalDate))
        assertEquals("SUN 3", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SUNDAY, startLocalDate))
    }

    @Test
    fun leapYearHandling() {
        // Starts on Monday Feb 26, 2024 (Leap year)
        val startLocalDate = LocalDate.of(2024, 2, 26)

        assertEquals("MON 26", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, startLocalDate))
        assertEquals("TUE 27", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.TUESDAY, startLocalDate))
        assertEquals("WED 28", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.WEDNESDAY, startLocalDate))
        assertEquals("THU 29", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.THURSDAY, startLocalDate))
        // Transitions into March:
        assertEquals("FRI 1", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.FRIDAY, startLocalDate))
        assertEquals("SAT 2", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SATURDAY, startLocalDate))
        assertEquals("SUN 3", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.SUNDAY, startLocalDate))
    }

    @Test
    fun parseStartDateRespectsUserTimezone() {
        // 2026-11-15T20:00:00Z in UTC is Sunday Nov 15th
        val isoTimestamp = "2026-11-15T20:00:00Z"

        // In Asia/Ho_Chi_Minh (UTC+7), it is 2026-11-16 03:00 (Monday Nov 16th)
        val vnZone = ZoneId.of("Asia/Ho_Chi_Minh")
        val vnParsed = WorkoutPlanDateFormatter.parseStartDate(isoTimestamp, vnZone)
        assertEquals(LocalDate.of(2026, 11, 16), vnParsed)
        assertEquals("MON 16", WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, isoTimestamp, vnZone))

        // In America/New_York (UTC-5), it is 2026-11-15 15:00 (Sunday Nov 15th)
        val nyZone = ZoneId.of("America/New_York")
        val nyParsed = WorkoutPlanDateFormatter.parseStartDate(isoTimestamp, nyZone)
        assertEquals(LocalDate.of(2026, 11, 15), nyParsed)
    }

    @Test
    fun fallbackWhenStartDateIsMissingOrInvalid() {
        assertNull(WorkoutPlanDateFormatter.parseStartDate(null))
        assertNull(WorkoutPlanDateFormatter.parseStartDate(""))
        assertNull(WorkoutPlanDateFormatter.parseStartDate("   "))
        assertNull(WorkoutPlanDateFormatter.parseStartDate("not-a-valid-date"))

        assertNull(WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, null as LocalDate?))
        assertNull(WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, null as String?))
        assertNull(WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, ""))
        assertNull(WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(WorkoutDayOfWeek.MONDAY, "invalid-date"))
    }
}
