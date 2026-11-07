package com.calisthenics.ai.ui.workoutplan

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.PlannedExercise
import com.calisthenics.ai.domain.model.PlannedWorkout
import com.calisthenics.ai.domain.model.WorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.WorkoutSessionProgress
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPlanViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun createAndSaveWorkoutPersistsItInThePlan() = runTest {
        val plans = FakeWorkoutPlanRepository()
        val viewModel = WorkoutPlanViewModel(
            workoutPlanRepository = plans,
            getExercisesUseCase = GetExercisesUseCase(FakeExerciseRepository()),
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()

        viewModel.createPlan("Strength block")
        advanceUntilIdle()
        viewModel.openCreateWorkout()
        viewModel.updateEditor {
            it.copy(name = "Push", category = "Push", estimatedDuration = "50")
        }
        viewModel.saveWorkout()
        advanceUntilIdle()

        val plan = requireNotNull(viewModel.uiState.value.plan)
        assertEquals("Strength block", plan.name)
        assertEquals(1, plan.workouts.size)
        assertEquals("Push", plan.workouts.single().name)
        assertEquals(50, plan.workouts.single().estimatedDurationMinutes)
    }

    @Test
    fun weeklyOverviewCountsOnlyCompletionRecordedForThatWeek() {
        val monday = LocalDate.of(2026, 10, 26)
        val first = PlannedWorkout("a", 1, "Push", "Push", 45)
        val second = PlannedWorkout("b", 3, "Pull", "Pull", 45, exercises = listOf(PlannedExercise("e", name = "Rows")))
        val overview = calculateWeeklyOverview(
            plan = WorkoutPlan("plan", "Plan", listOf(first, second)),
            progress = listOf(WorkoutSessionProgress("a", monday.toString(), isWorkoutCompleted = true)),
            weekStart = monday
        )

        assertEquals(2, overview.sessions)
        assertEquals(1, overview.exercises)
        assertEquals(1, overview.completedSessions)
        assertEquals(50, overview.completionPercent)
    }

    private class FakeWorkoutPlanRepository : WorkoutPlanRepository {
        private var plan: WorkoutPlan? = null
        private var progress: List<WorkoutSessionProgress> = emptyList()

        override suspend fun getSnapshot() = Result.success(WorkoutPlanSnapshot(plan, progress))
        override suspend fun savePlan(plan: WorkoutPlan): Result<Unit> {
            this.plan = plan
            return Result.success(Unit)
        }
        override suspend fun clearPlan(): Result<Unit> {
            plan = null
            progress = emptyList()
            return Result.success(Unit)
        }
        override suspend fun saveSessionProgress(progress: WorkoutSessionProgress): Result<Unit> {
            this.progress = this.progress.filterNot { it.workoutId == progress.workoutId && it.date == progress.date } + progress
            return Result.success(Unit)
        }
        override suspend fun getActivePlanId(): Result<Long?> = Result.success(null)
        override suspend fun setActivePlanId(planId: Long?): Result<Unit> = Result.success(Unit)

        override suspend fun getPlans(): Result<List<com.calisthenics.ai.domain.model.UserWorkoutPlan>> = Result.success(emptyList())
        override fun observeCachedPlans(): kotlinx.coroutines.flow.Flow<List<com.calisthenics.ai.domain.model.UserWorkoutPlan>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override fun observeCachedPlan(planId: Long): kotlinx.coroutines.flow.Flow<com.calisthenics.ai.domain.model.UserWorkoutPlan?> = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun resolveTodayWorkout(
            date: String,
            activePlanId: Long?
        ): Result<TodayWorkoutResolution> = Result.failure(UnsupportedOperationException())
        override suspend fun getPlanById(id: Long): Result<com.calisthenics.ai.domain.model.UserWorkoutPlan> = Result.failure(UnsupportedOperationException())
        override suspend fun getPlanTemplates(): Result<List<com.calisthenics.ai.domain.model.UserWorkoutPlanTemplate>> = Result.success(emptyList())
        override suspend fun createPlan(
            name: String,
            splitType: com.calisthenics.ai.domain.model.WorkoutSplitType,
            days: List<Pair<com.calisthenics.ai.domain.model.WorkoutDayOfWeek, com.calisthenics.ai.domain.model.WorkoutSessionType>>
        ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlan> = Result.failure(UnsupportedOperationException())
        override suspend fun updatePlan(
            id: Long,
            name: String,
            splitType: com.calisthenics.ai.domain.model.WorkoutSplitType,
            days: List<Pair<com.calisthenics.ai.domain.model.WorkoutDayOfWeek, com.calisthenics.ai.domain.model.WorkoutSessionType>>
        ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlan> = Result.failure(UnsupportedOperationException())
        override suspend fun updatePlanDays(
            id: Long,
            days: List<com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration>
        ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlan> = Result.failure(UnsupportedOperationException())
        override suspend fun deletePlan(id: Long): Result<Unit> = Result.success(Unit)
        override suspend fun getDayExercises(planId: Long, dayId: Long): Result<List<com.calisthenics.ai.domain.model.UserWorkoutPlanExercise>> = Result.success(emptyList())
        override suspend fun addExercise(
            planId: Long,
            dayId: Long,
            exerciseId: Long,
            sets: Int,
            reps: Int?,
            durationSeconds: Int?,
            restSeconds: Int,
            displayOrder: Int,
            notes: String?
        ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlanExercise> = Result.failure(UnsupportedOperationException())
        override suspend fun updateExercise(
            planId: Long,
            dayId: Long,
            assignmentId: Long,
            exerciseId: Long,
            sets: Int,
            reps: Int?,
            durationSeconds: Int?,
            restSeconds: Int,
            displayOrder: Int,
            notes: String?
        ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlanExercise> = Result.failure(UnsupportedOperationException())
        override suspend fun updateDayExercises(
            planId: Long,
            dayId: Long,
            exercises: List<com.calisthenics.ai.domain.model.UserWorkoutPlanExercise>
        ): Result<List<com.calisthenics.ai.domain.model.UserWorkoutPlanExercise>> = Result.success(exercises)
        override suspend fun removeExercise(planId: Long, dayId: Long, assignmentId: Long): Result<Unit> = Result.success(Unit)
    }

    private class FakeExerciseRepository : ExerciseRepository {
        override suspend fun getExercises(): Result<List<Exercise>> = Result.success(emptyList())
        override suspend fun getExerciseById(id: Long): Result<Exercise> = Result.failure(UnsupportedOperationException())
        override suspend fun getExerciseByCode(code: String): Result<Exercise> = Result.failure(UnsupportedOperationException())
    }
}
