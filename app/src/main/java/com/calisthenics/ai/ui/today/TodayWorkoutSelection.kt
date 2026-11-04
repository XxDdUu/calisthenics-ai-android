package com.calisthenics.ai.ui.today

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSource
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

enum class TodayWorkoutKind {
    NO_PLANS,
    PLAN_SELECTION_REQUIRED,
    NOT_SCHEDULED,
    REST_DAY,
    SCHEDULED
}

data class TodayWorkoutSelection(
    val kind: TodayWorkoutKind,
    val plan: UserWorkoutPlan? = null,
    val day: UserWorkoutPlanDay? = null,
    val source: WorkoutSource = WorkoutSource.NONE,
    val assignmentId: Long? = null,
    val coachName: String? = null
)

fun selectTodayWorkout(
    plans: List<UserWorkoutPlan>,
    date: LocalDate,
    activePlanId: Long?,
    source: WorkoutSource = if (plans.isEmpty()) WorkoutSource.NONE else WorkoutSource.PERSONAL,
    assignmentId: Long? = null,
    coachName: String? = null
): TodayWorkoutSelection {
    if (plans.isEmpty()) return TodayWorkoutSelection(TodayWorkoutKind.NO_PLANS, source = source)

    val plan = when {
        activePlanId != null -> plans.firstOrNull { it.id == activePlanId }
        plans.size == 1 -> plans.single()
        else -> null
    } ?: return TodayWorkoutSelection(
        TodayWorkoutKind.PLAN_SELECTION_REQUIRED,
        source = source
    )

    val dayOfWeek = WorkoutDayOfWeek.fromOrder(date.dayOfWeek.value)
    val day = plan.days.firstOrNull { it.dayOfWeek == dayOfWeek }
        ?: return TodayWorkoutSelection(
            TodayWorkoutKind.NOT_SCHEDULED,
            plan,
            source = source,
            assignmentId = assignmentId,
            coachName = coachName
        )

    return TodayWorkoutSelection(
        kind = if (day.sessionType == WorkoutSessionType.REST) {
            TodayWorkoutKind.REST_DAY
        } else {
            TodayWorkoutKind.SCHEDULED
        },
        plan = plan,
        day = day,
        source = source,
        assignmentId = assignmentId,
        coachName = coachName
    )
}

fun datesForWeek(date: LocalDate, weekOffset: Long = 0): List<LocalDate> {
    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset)
    return (0L..6L).map(monday::plusDays)
}
