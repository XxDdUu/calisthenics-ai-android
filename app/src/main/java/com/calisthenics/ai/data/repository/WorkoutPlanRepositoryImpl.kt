package com.calisthenics.ai.data.repository

import android.content.Context
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteWorkoutPlanDataSource
import com.calisthenics.ai.data.remote.dto.CreateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutDayExercisesBatchRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanDayRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseRequestDto
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.TodayWorkoutCandidate
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.UserWorkoutPlanTemplate
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.WorkoutSessionProgress
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPlanRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenManager: TokenManager,
    private val remoteDataSource: RemoteWorkoutPlanDataSource
) : WorkoutPlanRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Server-backed REST API Implementation ---

    override suspend fun getPlans(): Result<List<UserWorkoutPlan>> = runCatching {
        val dtos = remoteDataSource.getWorkoutPlans()
        dtos.map { it.toDomain() }
    }

    override suspend fun resolveTodayWorkout(
        date: String,
        activePlanId: Long?
    ): Result<TodayWorkoutResolution> = runCatching {
        val response = remoteDataSource.resolveTodayWorkout(date, activePlanId)
        val source = WorkoutSource.valueOf(response.source)
        TodayWorkoutResolution(
            workoutDate = response.workoutDate,
            activeCoach = response.activeCoach,
            source = source,
            selectionRequired = response.selectionRequired,
            selectedPlanId = response.selectedPlanId,
            candidates = response.candidates.map { candidate ->
                TodayWorkoutCandidate(
                    plan = candidate.workoutPlan.toDomain(),
                    assignmentId = candidate.assignmentId,
                    coachName = candidate.coachName
                )
            }
        )
    }

    override suspend fun getPlanById(id: Long): Result<UserWorkoutPlan> = runCatching {
        val planDto = remoteDataSource.getWorkoutPlanById(id)
        val days = planDto.days.orEmpty()

        // Fetch exercises for all days concurrently to populate weekly schedule
        val dayExercisesMap = coroutineScope {
            days.map { dayDto ->
                async {
                    val dayId = dayDto.id
                    if (dayId != null && dayId > 0) {
                        val exercises = remoteDataSource.getWorkoutPlanDayExercises(id, dayId)
                        dayId to exercises.map { it.toDomain() }
                    } else {
                        0L to emptyList<UserWorkoutPlanExercise>()
                    }
                }
            }.awaitAll().toMap()
        }

        planDto.toDomain(dayExercisesMap)
    }

    override suspend fun getPlanTemplates(): Result<List<UserWorkoutPlanTemplate>> = runCatching {
        remoteDataSource.getWorkoutPlanTemplates().map { it.toDomain() }
    }

    override suspend fun createPlan(
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan> = runCatching {
        val request = CreateWorkoutPlanRequestDto(
            name = name.trim(),
            splitType = splitType.name,
            days = days.map { (dayOfWeek, sessionType) ->
                WorkoutPlanDayRequestDto(
                    dayOfWeek = dayOfWeek.name,
                    sessionType = sessionType.name
                )
            }
        )
        remoteDataSource.createWorkoutPlan(request).toDomain()
    }

    override suspend fun updatePlan(
        id: Long,
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan> = runCatching {
        val request = UpdateWorkoutPlanRequestDto(
            name = name.trim(),
            splitType = splitType.name,
            days = days.map { (dayOfWeek, sessionType) ->
                WorkoutPlanDayRequestDto(
                    dayOfWeek = dayOfWeek.name,
                    sessionType = sessionType.name
                )
            }
        )
        remoteDataSource.updateWorkoutPlan(id, request).toDomain()
    }

    override suspend fun deletePlan(id: Long): Result<Unit> = runCatching {
        remoteDataSource.deleteWorkoutPlan(id)
    }

    override suspend fun getDayExercises(planId: Long, dayId: Long): Result<List<UserWorkoutPlanExercise>> = runCatching {
        remoteDataSource.getWorkoutPlanDayExercises(planId, dayId).map { it.toDomain() }
    }

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
    ): Result<UserWorkoutPlanExercise> = runCatching {
        val request = WorkoutPlanExerciseRequestDto(
            exerciseId = exerciseId,
            sets = sets,
            reps = reps,
            durationSeconds = durationSeconds,
            restSeconds = restSeconds,
            displayOrder = displayOrder,
            notes = notes?.takeIf { it.isNotBlank() }
        )
        remoteDataSource.addExerciseToWorkoutPlanDay(planId, dayId, request).toDomain()
    }

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
    ): Result<UserWorkoutPlanExercise> = runCatching {
        val request = WorkoutPlanExerciseRequestDto(
            exerciseId = exerciseId,
            sets = sets,
            reps = reps,
            durationSeconds = durationSeconds,
            restSeconds = restSeconds,
            displayOrder = displayOrder,
            notes = notes?.takeIf { it.isNotBlank() }
        )
        remoteDataSource.updateWorkoutPlanExercise(planId, dayId, assignmentId, request).toDomain()
    }

    override suspend fun updateDayExercises(
        planId: Long,
        dayId: Long,
        exercises: List<UserWorkoutPlanExercise>
    ): Result<List<UserWorkoutPlanExercise>> = runCatching {
        val request = WorkoutDayExercisesBatchRequestDto(
            exercises = exercises.mapIndexed { index, ex ->
                WorkoutPlanExerciseRequestDto(
                    id = ex.id.takeIf { it > 0 },
                    exerciseId = ex.exercise.id,
                    sets = ex.sets,
                    reps = ex.reps,
                    durationSeconds = ex.durationSeconds,
                    restSeconds = ex.restSeconds,
                    displayOrder = index + 1,
                    notes = ex.notes?.takeIf { it.isNotBlank() }
                )
            }
        )
        remoteDataSource.updateWorkoutPlanDayExercises(planId, dayId, request).map { it.toDomain() }
    }

    override suspend fun removeExercise(planId: Long, dayId: Long, assignmentId: Long): Result<Unit> = runCatching {
        remoteDataSource.removeExerciseFromWorkoutPlanDay(planId, dayId, assignmentId)
    }

    // --- Backward-Compatibility Local Snapshot Implementation ---

    override suspend fun getSnapshot(): Result<WorkoutPlanSnapshot> = runCatching {
        val scope = accountScope()
        WorkoutPlanSnapshot(
            plan = preferences.getString("$scope.plan", null)?.let { json.decodeFromString<WorkoutPlan>(it) },
            sessionProgress = preferences.getString("$scope.progress", null)
                ?.let { json.decodeFromString<List<WorkoutSessionProgress>>(it) }
                .orEmpty()
        )
    }

    override suspend fun savePlan(plan: WorkoutPlan): Result<Unit> = runCatching {
        val scope = accountScope()
        preferences.edit().putString("$scope.plan", json.encodeToString(plan)).commitOrThrow()
    }

    override suspend fun clearPlan(): Result<Unit> = runCatching {
        val scope = accountScope()
        preferences.edit()
            .remove("$scope.plan")
            .remove("$scope.progress")
            .commitOrThrow()
    }

    override suspend fun saveSessionProgress(progress: WorkoutSessionProgress): Result<Unit> = runCatching {
        val scope = accountScope()
        val existing = preferences.getString("$scope.progress", null)
            ?.let { json.decodeFromString<List<WorkoutSessionProgress>>(it) }
            .orEmpty()
        val updated = existing.filterNot {
            it.workoutId == progress.workoutId && it.date == progress.date
        } + progress
        preferences.edit()
            .putString("$scope.progress", json.encodeToString(updated))
            .commitOrThrow()
    }

    override suspend fun getActivePlanId(): Result<Long?> = runCatching {
        preferences.getLong("${accountScope()}.active_plan_id", NO_ACTIVE_PLAN_ID)
            .takeIf { it != NO_ACTIVE_PLAN_ID }
    }

    override suspend fun setActivePlanId(planId: Long?): Result<Unit> = runCatching {
        val editor = preferences.edit()
        if (planId == null) {
            editor.remove("${accountScope()}.active_plan_id")
        } else {
            editor.putLong("${accountScope()}.active_plan_id", planId)
        }
        editor.commitOrThrow()
    }

    private fun accountScope(): String {
        tokenManager.getUserId()?.let { return "user_$it" }
        val token = tokenManager.getAccessToken() ?: "anonymous"
        val hash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return hash.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun android.content.SharedPreferences.Editor.commitOrThrow() {
        check(commit()) { "Could not save your workout plan on this device." }
    }

    private companion object {
        const val PREFERENCES_NAME = "workout_plan_preferences"
        const val NO_ACTIVE_PLAN_ID = -1L
    }
}
