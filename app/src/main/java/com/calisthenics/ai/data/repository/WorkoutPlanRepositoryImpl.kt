package com.calisthenics.ai.data.repository

import android.content.Context
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.local.database.WorkoutScheduleDao
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.mapper.toEntity
import com.calisthenics.ai.data.network.ConnectivityMonitor
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
import com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration
import com.calisthenics.ai.domain.model.WorkoutSessionProgress
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPlanRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenManager: TokenManager,
    private val remoteDataSource: RemoteWorkoutPlanDataSource,
    private val workoutScheduleDao: WorkoutScheduleDao,
    private val connectivityMonitor: ConnectivityMonitor
) : WorkoutPlanRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Observable Room Database methods ---

    override fun observeCachedPlans(): Flow<List<UserWorkoutPlan>> {
        return workoutScheduleDao.getAllPlansFlow().map { entities ->
            entities.map { planEntity ->
                val days = workoutScheduleDao.getDaysForPlan(planEntity.id)
                val exercises = workoutScheduleDao.getExercisesForPlan(planEntity.id)
                val exercisesByDay = exercises.groupBy { it.dayId }
                val dayModels = days.map { dayEntity ->
                    dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
                }
                planEntity.toDomain(dayModels)
            }
        }
    }

    override fun observeCachedPlan(planId: Long): Flow<UserWorkoutPlan?> {
        return workoutScheduleDao.getPlanByIdFlow(planId).map { planEntity ->
            if (planEntity == null) null else {
                val days = workoutScheduleDao.getDaysForPlan(planEntity.id)
                val exercises = workoutScheduleDao.getExercisesForPlan(planEntity.id)
                val exercisesByDay = exercises.groupBy { it.dayId }
                val dayModels = days.map { dayEntity ->
                    dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
                }
                planEntity.toDomain(dayModels)
            }
        }
    }

    // --- Server-backed REST API Implementation with Room Caching ---

    override suspend fun getPlans(): Result<List<UserWorkoutPlan>> = runCatching {
        if (connectivityMonitor.isConnected()) {
            val remoteResult = runCatching {
                val dtos = remoteDataSource.getWorkoutPlans()
                val plans = dtos.map { it.toDomain() }
                if (plans.isNotEmpty()) {
                    val planEntities = plans.map { it.toEntity() }
                    workoutScheduleDao.insertPlans(planEntities)
                }
                plans
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        val cached = loadAllPlansFromRoom()
        if (cached.isNotEmpty()) cached else emptyList()
    }

    override suspend fun resolveTodayWorkout(
        date: String,
        activePlanId: Long?
    ): Result<TodayWorkoutResolution> = runCatching {
        if (connectivityMonitor.isConnected()) {
            val remoteResult = runCatching {
                val response = remoteDataSource.resolveTodayWorkout(date, activePlanId)
                val source = WorkoutSource.valueOf(response.source)
                val candidates = response.candidates.map { candidate ->
                    TodayWorkoutCandidate(
                        plan = candidate.workoutPlan.toDomain(),
                        assignmentId = candidate.assignmentId,
                        coachName = candidate.coachName
                    )
                }
                val resolution = TodayWorkoutResolution(
                    workoutDate = response.workoutDate,
                    activeCoach = response.activeCoach,
                    source = source,
                    selectionRequired = response.selectionRequired,
                    selectedPlanId = response.selectedPlanId,
                    candidates = candidates
                )
                // Persist full schedule into Room Database for offline access
                persistResolutionToRoom(resolution)
                resolution
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        // Offline or network error: load from Room Database
        loadResolutionFromRoom(date, activePlanId)
    }

    override suspend fun getPlanById(id: Long): Result<UserWorkoutPlan> = runCatching {
        if (connectivityMonitor.isConnected()) {
            val remoteResult = runCatching {
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

                val domainPlan = planDto.toDomain(dayExercisesMap)
                // Persist to Room
                val planEntity = domainPlan.toEntity()
                val dayEntities = domainPlan.days.map { it.toEntity(id) }
                val exerciseEntities = domainPlan.days.flatMap { day ->
                    day.exercises.map { it.toEntity(id, day.id) }
                }
                workoutScheduleDao.saveFullPlan(planEntity, dayEntities, exerciseEntities)
                domainPlan
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        // Fallback to Room Database
        loadPlanFromRoom(id) ?: throw IllegalStateException("Cached plan #$id is not available offline.")
    }

    private suspend fun persistResolutionToRoom(resolution: TodayWorkoutResolution) {
        runCatching {
            for (candidate in resolution.candidates) {
                val isCoach = (resolution.source == WorkoutSource.COACH && candidate.assignmentId != null)
                val isActive = (candidate.plan.id == resolution.selectedPlanId)
                val planEntity = candidate.plan.toEntity(
                    isCoachAssigned = isCoach,
                    coachName = candidate.coachName,
                    assignmentId = candidate.assignmentId,
                    isActive = isActive
                )
                val dayEntities = candidate.plan.days.map { it.toEntity(candidate.plan.id) }
                val exerciseEntities = candidate.plan.days.flatMap { day ->
                    day.exercises.map { it.toEntity(candidate.plan.id, day.id) }
                }
                workoutScheduleDao.saveFullPlan(planEntity, dayEntities, exerciseEntities)
            }
        }
    }

    private suspend fun loadResolutionFromRoom(date: String, activePlanId: Long?): TodayWorkoutResolution {
        val cachedPlans = workoutScheduleDao.getAllPlans()
        if (cachedPlans.isEmpty()) {
            return TodayWorkoutResolution(
                workoutDate = date,
                activeCoach = false,
                source = WorkoutSource.NONE,
                selectionRequired = false,
                selectedPlanId = null,
                candidates = emptyList()
            )
        }

        val candidates = cachedPlans.map { planEntity ->
            val days = workoutScheduleDao.getDaysForPlan(planEntity.id)
            val exercises = workoutScheduleDao.getExercisesForPlan(planEntity.id)
            val exercisesByDay = exercises.groupBy { it.dayId }
            val dayModels = days.map { dayEntity ->
                dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
            }
            val domainPlan = planEntity.toDomain(dayModels)
            TodayWorkoutCandidate(
                plan = domainPlan,
                assignmentId = planEntity.assignmentId,
                coachName = planEntity.coachName
            )
        }

        // Coach-assigned plan takes priority over personal plan
        val coachCandidate = candidates.firstOrNull { it.assignmentId != null || cachedPlans.any { p -> p.id == it.plan.id && p.isCoachAssigned } }
        val source = if (coachCandidate != null) WorkoutSource.COACH else WorkoutSource.PERSONAL
        val selectedId = if (source == WorkoutSource.COACH && coachCandidate != null) {
            coachCandidate.plan.id
        } else {
            activePlanId ?: cachedPlans.firstOrNull { it.isActive }?.id ?: cachedPlans.first().id
        }

        return TodayWorkoutResolution(
            workoutDate = date,
            activeCoach = (source == WorkoutSource.COACH),
            source = source,
            selectionRequired = false,
            selectedPlanId = selectedId,
            candidates = candidates
        )
    }

    private suspend fun loadPlanFromRoom(id: Long): UserWorkoutPlan? {
        val planEntity = workoutScheduleDao.getPlanById(id) ?: return null
        val days = workoutScheduleDao.getDaysForPlan(id)
        val exercises = workoutScheduleDao.getExercisesForPlan(id)
        val exercisesByDay = exercises.groupBy { it.dayId }
        val dayModels = days.map { dayEntity ->
            dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
        }
        return planEntity.toDomain(dayModels)
    }

    private suspend fun loadAllPlansFromRoom(): List<UserWorkoutPlan> {
        val cachedPlans = workoutScheduleDao.getAllPlans()
        return cachedPlans.map { planEntity ->
            val days = workoutScheduleDao.getDaysForPlan(planEntity.id)
            val exercises = workoutScheduleDao.getExercisesForPlan(planEntity.id)
            val exercisesByDay = exercises.groupBy { it.dayId }
            val dayModels = days.map { dayEntity ->
                dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
            }
            planEntity.toDomain(dayModels)
        }
    }

    override suspend fun getPlanTemplates(): Result<List<UserWorkoutPlanTemplate>> = runCatching {
        remoteDataSource.getWorkoutPlanTemplates().map { it.toDomain() }
    }

    override suspend fun createPlan(
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot create workout plans while offline." }
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
        val created = remoteDataSource.createWorkoutPlan(request).toDomain()
        workoutScheduleDao.insertPlan(created.toEntity())
        created
    }

    override suspend fun updatePlan(
        id: Long,
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot modify workout plans while offline." }
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
        val updated = remoteDataSource.updateWorkoutPlan(id, request).toDomain()
        workoutScheduleDao.insertPlan(updated.toEntity())
        updated
    }

    override suspend fun updatePlanDays(
        id: Long,
        days: List<WorkoutPlanDayConfiguration>
    ): Result<UserWorkoutPlan> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot modify workout plans while offline." }
        require(days.size == WorkoutDayOfWeek.entries.size) {
            "A workout plan must configure all seven days."
        }
        val configurations = days.associateBy { it.dayOfWeek }
        require(configurations.size == WorkoutDayOfWeek.entries.size) {
            "Each weekday may only be configured once."
        }

        val currentPlan = getPlanById(id).getOrThrow()
        val request = UpdateWorkoutPlanRequestDto(
            name = currentPlan.name,
            splitType = currentPlan.splitType.name,
            days = WorkoutDayOfWeek.entries.map { dayOfWeek ->
                val configuration = requireNotNull(configurations[dayOfWeek])
                val isRestDay = configuration.isRestDay ||
                    configuration.workoutType == WorkoutSessionType.REST
                WorkoutPlanDayRequestDto(
                    dayOfWeek = dayOfWeek.name,
                    sessionType = if (isRestDay || configuration.workoutType == WorkoutSessionType.UNSPECIFIED) {
                        null
                    } else {
                        configuration.workoutType?.name
                    },
                    name = if (isRestDay) null else configuration.name?.trim()?.takeIf(String::isNotEmpty),
                    description = if (isRestDay) null else configuration.description?.trim()?.takeIf(String::isNotEmpty),
                    isRestDay = isRestDay,
                    displayOrder = configuration.displayOrder
                )
            }
        )
        val response = remoteDataSource.updateWorkoutPlan(id, request)
        val exercisesByDay = currentPlan.days.associate { day -> day.id to day.exercises }
        val updated = response.toDomain(exercisesByDay)
        workoutScheduleDao.saveFullPlan(
            updated.toEntity(),
            updated.days.map { it.toEntity(id) },
            updated.days.flatMap { day -> day.exercises.map { it.toEntity(id, day.id) } }
        )
        updated
    }

    override suspend fun deletePlan(id: Long): Result<Unit> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot delete workout plans while offline." }
        remoteDataSource.deleteWorkoutPlan(id)
        workoutScheduleDao.deletePlan(id)
    }

    override suspend fun getDayExercises(planId: Long, dayId: Long): Result<List<UserWorkoutPlanExercise>> = runCatching {
        if (connectivityMonitor.isConnected()) {
            val remoteResult = runCatching {
                val exercises = remoteDataSource.getWorkoutPlanDayExercises(planId, dayId).map { it.toDomain() }
                if (exercises.isNotEmpty()) {
                    val entities = exercises.map { it.toEntity(planId, dayId) }
                    workoutScheduleDao.insertExercises(entities)
                }
                exercises
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        workoutScheduleDao.getExercisesForDay(dayId).map { it.toDomain() }
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
        check(connectivityMonitor.isConnected()) { "Cannot modify exercises while offline." }
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
        check(connectivityMonitor.isConnected()) { "Cannot modify exercises while offline." }
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
        check(connectivityMonitor.isConnected()) { "Cannot modify exercises while offline." }
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
        check(connectivityMonitor.isConnected()) { "Cannot modify exercises while offline." }
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
            workoutScheduleDao.setActivePlanId(planId)
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
