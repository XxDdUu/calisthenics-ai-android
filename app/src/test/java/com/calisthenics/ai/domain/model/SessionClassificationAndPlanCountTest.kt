package com.calisthenics.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SessionClassificationAndPlanCountTest {

    @Test
    fun trainingDayCount_deduplicatesWeekdays_excludesRestDays_andNeverExceedsSeven() {
        val duplicatedFourteenDays = WorkoutDayOfWeek.entries.flatMapIndexed { idx, day ->
            val sessionType = if (day == WorkoutDayOfWeek.SUNDAY) WorkoutSessionType.REST else WorkoutSessionType.PUSH
            listOf(
                UserWorkoutPlanDay(id = idx * 2L + 1, dayOfWeek = day, sessionType = sessionType),
                UserWorkoutPlanDay(id = idx * 2L + 2, dayOfWeek = day, sessionType = sessionType)
            )
        }
        val plan = UserWorkoutPlan(
            id = 77L,
            name = "Legacy Duplicate Plan",
            splitType = WorkoutSplitType.CUSTOM,
            days = duplicatedFourteenDays
        )

        assertTrue(plan.trainingDayCount <= 7)
        assertEquals(6, plan.trainingDayCount)
    }

    @Test
    fun trainingDayCount_threeTrainingDaysWithFourRestDays_returnsThree() {
        val schedule = listOf(
            WorkoutDayOfWeek.MONDAY to WorkoutSessionType.PUSH,
            WorkoutDayOfWeek.TUESDAY to WorkoutSessionType.REST,
            WorkoutDayOfWeek.WEDNESDAY to WorkoutSessionType.PULL,
            WorkoutDayOfWeek.THURSDAY to WorkoutSessionType.REST,
            WorkoutDayOfWeek.FRIDAY to WorkoutSessionType.LEGS,
            WorkoutDayOfWeek.SATURDAY to WorkoutSessionType.REST,
            WorkoutDayOfWeek.SUNDAY to WorkoutSessionType.REST
        ).mapIndexed { index, (day, type) ->
            UserWorkoutPlanDay(id = index + 1L, dayOfWeek = day, sessionType = type)
        }
        val plan = UserWorkoutPlan(
            id = 88L,
            name = "4-Week 3-Day Calisthenics",
            splitType = WorkoutSplitType.FULL_BODY,
            days = schedule
        )

        assertEquals(3, plan.trainingDayCount)
    }

    @Test
    fun effectiveClassification_strictlySeparatesUpcomingUnfinishedCompletedAndCancelled() {
        val today = LocalDate.of(2026, 11, 10)

        val futureLegacyInProgress = session(date = "2026-11-16", status = "IN_PROGRESS", completedAt = null)
        val futureScheduled = session(date = "2026-12-01", status = "SCHEDULED", completedAt = null)
        val todayScheduled = session(date = "2026-11-10", status = "SCHEDULED", completedAt = null)
        val pastUnfinished = session(date = "2026-11-08", status = "IN_PROGRESS", completedAt = null)
        val completed = session(date = "2026-11-09", status = "COMPLETED", completedAt = "2026-11-09T11:00:00Z")
        val cancelled = session(date = "2026-11-12", status = "CANCELLED", completedAt = null)

        assertEquals(WorkoutSessionClassification.UPCOMING, futureLegacyInProgress.effectiveClassification(today))
        assertEquals(WorkoutSessionClassification.UPCOMING, futureScheduled.effectiveClassification(today))
        assertEquals(WorkoutSessionClassification.UNFINISHED, todayScheduled.effectiveClassification(today))
        assertEquals(WorkoutSessionClassification.UNFINISHED, pastUnfinished.effectiveClassification(today))
        assertEquals(WorkoutSessionClassification.COMPLETED, completed.effectiveClassification(today))
        assertEquals(WorkoutSessionClassification.CANCELLED, cancelled.effectiveClassification(today))
    }

    private fun session(date: String, status: String, completedAt: String?) = WorkoutSession(
        id = 1L,
        workoutPlanId = 10L,
        workoutPlanDayId = 20L,
        assignmentId = null,
        source = "PERSONAL",
        title = "Calisthenics Session",
        workoutDate = date,
        status = status,
        startedAt = null,
        lastSavedAt = null,
        completedAt = completedAt,
        durationSeconds = 0L,
        completedExercises = 0,
        skippedExercises = 0,
        unfinishedExercises = 1,
        completedSets = 0,
        totalReps = 0,
        totalHoldDurationSeconds = 0,
        exercises = emptyList()
    )
}
