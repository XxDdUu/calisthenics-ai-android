package com.calisthenics.ai.ui.today

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TodayWorkoutSelectionTest {

    @Test
    fun mapsEveryIsoWeekdayToItsConfiguredDay() {
        val plan = plan(
            days = WorkoutDayOfWeek.entries.map { day ->
                UserWorkoutPlanDay(day.order.toLong(), day, WorkoutSessionType.PUSH)
            }
        )

        WorkoutDayOfWeek.entries.forEach { day ->
            val result = selectTodayWorkout(
                plans = listOf(plan),
                date = LocalDate.of(2026, 10, 26).plusDays((day.order - 1).toLong()),
                activePlanId = plan.id
            )
            assertEquals(day, result.day?.dayOfWeek)
            assertEquals(TodayWorkoutKind.SCHEDULED, result.kind)
        }
    }

    @Test
    fun distinguishesExplicitRestFromUnassignedDay() {
        val monday = UserWorkoutPlanDay(1, WorkoutDayOfWeek.MONDAY, WorkoutSessionType.REST)
        val plan = plan(days = listOf(monday))

        val rest = selectTodayWorkout(listOf(plan), LocalDate.of(2026, 10, 26), plan.id)
        val unassigned = selectTodayWorkout(listOf(plan), LocalDate.of(2026, 10, 27), plan.id)

        assertEquals(TodayWorkoutKind.REST_DAY, rest.kind)
        assertEquals(TodayWorkoutKind.NOT_SCHEDULED, unassigned.kind)
        assertNull(unassigned.day)
    }

    @Test
    fun requiresSelectionForMultiplePlansAndRespectsChosenPlan() {
        val monday = UserWorkoutPlanDay(1, WorkoutDayOfWeek.MONDAY, WorkoutSessionType.PUSH)
        val first = plan(id = 1, name = "First", days = listOf(monday))
        val second = plan(id = 2, name = "Second", days = listOf(monday.copy(sessionType = WorkoutSessionType.PULL)))

        val ambiguous = selectTodayWorkout(listOf(first, second), LocalDate.of(2026, 10, 26), null)
        val selected = selectTodayWorkout(listOf(first, second), LocalDate.of(2026, 10, 26), second.id)

        assertEquals(TodayWorkoutKind.PLAN_SELECTION_REQUIRED, ambiguous.kind)
        assertEquals(second.id, selected.plan?.id)
        assertEquals(WorkoutSessionType.PULL, selected.day?.sessionType)
    }

    @Test
    fun returnsEmptyStateWhenUserHasNoPlans() {
        val result = selectTodayWorkout(emptyList(), LocalDate.of(2026, 10, 26), null)

        assertEquals(TodayWorkoutKind.NO_PLANS, result.kind)
        assertNull(result.plan)
    }

    @Test
    fun weeklyDatesAreMondayThroughSundayAcrossYearBoundary() {
        val dates = datesForWeek(LocalDate.of(2024, 12, 31))

        assertEquals(LocalDate.of(2024, 12, 30), dates.first())
        assertEquals(LocalDate.of(2025, 1, 5), dates.last())
        assertEquals(7, dates.size)
    }

    private fun plan(
        id: Long = 12L,
        name: String = "Test plan",
        days: List<UserWorkoutPlanDay> = emptyList()
    ) = UserWorkoutPlan(id, name, WorkoutSplitType.PPL, days = days)
}
