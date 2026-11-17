package com.calisthenics.ai.data.repository

import android.content.Context
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.local.database.WorkoutScheduleDao
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.mapper.toAssignmentDomain
import com.calisthenics.ai.data.mapper.toDto
import com.calisthenics.ai.data.mapper.toEntity
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.remote.datasource.RemoteWorkoutPlanDataSource
import com.calisthenics.ai.data.remote.dto.CommitAiWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.GenerateAiWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.ModifyAiWorkoutPlanRequestDto
import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
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
import com.calisthenics.ai.domain.model.WorkoutPlanAssignment
import com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import retrofit2.HttpException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPlanRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenManager: TokenManager,
    private val remoteDataSource: RemoteWorkoutPlanDataSource,
    private val workoutScheduleDao: WorkoutScheduleDao,
    private val connectivityMonitor: ConnectivityMonitor
) : WorkoutPlanRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _planCacheInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val planCacheInvalidations: Flow<Unit> = _planCacheInvalidations.asSharedFlow()

    private data class OwnerSession(
        val userId: Long,
        val generation: Long
    )

    private fun captureOwnerSession(): OwnerSession? {
        val snapshot = tokenManager.currentSessionSnapshot()
        val userId = snapshot.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
            ?: return null
        return OwnerSession(userId = userId, generation = snapshot.generation)
    }

    private fun isSessionStillActive(captured: OwnerSession): Boolean {
        val snapshot = tokenManager.currentSessionSnapshot()
        val currentUserId = snapshot.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        return currentUserId == captured.userId && snapshot.generation == captured.generation
    }

    // --- Observable Room Database methods ---

    override fun observeCachedPlans(): Flow<List<UserWorkoutPlan>> {
        return tokenManager.sessionState.flatMapLatest { session ->
            val ownerUserId = session.activeUserId?.takeIf { it > 0L }
                ?: tokenManager.getUserId()?.takeIf { it > 0L }
            if (ownerUserId == null) {
                flowOf(emptyList())
            } else {
                workoutScheduleDao.getAllPlansFlow(ownerUserId).map { entities ->
                    entities.map { planEntity ->
                        val days = workoutScheduleDao.getDaysForPlan(ownerUserId, planEntity.id)
                        val exercises = workoutScheduleDao.getExercisesForPlan(ownerUserId, planEntity.id)
                        val exercisesByDay = exercises.groupBy { it.dayId }
                        val dayModels = days.map { dayEntity ->
                            dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
                        }
                        planEntity.toDomain(dayModels)
                    }
                }
            }
        }
    }

    override fun observeCachedPlan(planId: Long): Flow<UserWorkoutPlan?> {
        return tokenManager.sessionState.flatMapLatest { session ->
            val ownerUserId = session.activeUserId?.takeIf { it > 0L }
                ?: tokenManager.getUserId()?.takeIf { it > 0L }
            if (ownerUserId == null) {
                flowOf(null)
            } else {
                workoutScheduleDao.getPlanByIdFlow(ownerUserId, planId).map { planEntity ->
                    if (planEntity == null) null else {
                        val days = workoutScheduleDao.getDaysForPlan(ownerUserId, planEntity.id)
                        val exercises = workoutScheduleDao.getExercisesForPlan(ownerUserId, planEntity.id)
                        val exercisesByDay = exercises.groupBy { it.dayId }
                        val dayModels = days.map { dayEntity ->
                            dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
                        }
                        planEntity.toDomain(dayModels)
                    }
                }
            }
        }
    }

    // --- Server-backed REST API Implementation with Room Caching ---

    override suspend fun getPlans(): Result<List<UserWorkoutPlan>> = runCatching {
        val ownerSession = captureOwnerSession() ?: return@runCatching emptyList()
        if (connectivityMonitor.isConnected()) {
            val remoteResult = runCatching {
                val dtos = remoteDataSource.getWorkoutPlans()
                val plans = dtos.map { it.toDomain() }
                if (isSessionStillActive(ownerSession)) {
                    val planEntities = plans.map { it.toEntity(ownerSession.userId) }
                    workoutScheduleDao.syncPersonalPlans(ownerSession.userId, planEntities)
                }

                plans
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        val cached = loadAllPlansFromRoom(ownerSession.userId)
        if (cached.isNotEmpty()) cached else emptyList()
    }

    override suspend fun getClientPersonalPlans(clientId: Long): Result<List<UserWorkoutPlan>> = runCatching {
        check(connectivityMonitor.isConnected()) { "Client personal plans require an internet connection." }
        remoteDataSource.getClientPersonalWorkoutPlans(clientId).map { it.toDomain() }
    }

    override suspend fun assignPlanToClient(relationshipId: Long, planId: Long): Result<Unit> =
        assignPlanToClient(relationshipId, planId, null, null, null)

    override suspend fun assignPlanToClient(
        relationshipId: Long,
        planId: Long,
        startDate: String?,
        endDate: String?,
        notes: String?
    ): Result<Unit> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot assign workout plans while offline." }
        remoteDataSource.assignWorkoutPlan(relationshipId, planId, startDate, endDate, notes)
        _planCacheInvalidations.tryEmit(Unit)
    }

    override suspend fun getMyAssignments() = runCatching {
        remoteDataSource.getMyAssignments()
            .map { it.toAssignmentDomain() }
            .filterNot { it.status.isCancelledOrEnded }
    }

    override suspend fun getMyCoachAssignments() = runCatching {
        remoteDataSource.getMyCoachAssignments()
            .map { it.toAssignmentDomain() }
            .filterNot { it.status.isCancelledOrEnded }
    }

    override suspend fun updateAssignmentStatus(
        assignmentId: Long,
        status: WorkoutPlanAssignmentStatus
    ) = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot change assignment status while offline." }
        val ownerSession = captureOwnerSession()
        val updated = remoteDataSource.updateAssignmentStatus(assignmentId, status.name).toAssignmentDomain()
        if (ownerSession != null && isSessionStillActive(ownerSession)) {
            // A status transition can change today's source from coach to personal. Force
            // dependent screens to resolve the server truth instead of using stale cache rows.
            if (status == WorkoutPlanAssignmentStatus.INACTIVE || status.isCancelledOrEnded) {
                workoutScheduleDao.clearCoachAssignedPlansForUser(ownerSession.userId)
            }
            _planCacheInvalidations.tryEmit(Unit)
        }
        updated
    }

    override suspend fun invalidateCoachAssignedPlans() {
        val ownerSession = captureOwnerSession()
        if (ownerSession != null && isSessionStillActive(ownerSession)) {
            workoutScheduleDao.clearCoachAssignedPlansForUser(ownerSession.userId)
        }
        _planCacheInvalidations.tryEmit(Unit)
    }

    override suspend fun getLatestActivePlan(clientId: Long?): Result<UserWorkoutPlan?> = runCatching {
        check(connectivityMonitor.isConnected()) { "An internet connection is required to verify active plans." }
        if (clientId != null) {
            remoteDataSource.getClientPersonalWorkoutPlans(clientId)
                .map { it.toDomain() }
                .firstOrNull { it.isActive }
        } else {
            val ownerSession = captureOwnerSession()
            val plans = remoteDataSource.getWorkoutPlans().map { it.toDomain() }
            if (ownerSession != null && isSessionStillActive(ownerSession)) {
                workoutScheduleDao.syncPersonalPlans(
                    ownerSession.userId,
                    plans.map { it.toEntity(ownerSession.userId) }
                )
            }
            plans.firstOrNull { it.isActive }
        }
    }

    override suspend fun resolveTodayWorkout(
        date: String,
        activePlanId: Long?
    ): Result<TodayWorkoutResolution> = runCatching {
        val ownerSession = captureOwnerSession()
            ?: return@runCatching emptyResolution(date)
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
                // Persist full schedule into Room Database only if the same user session is still active.
                if (isSessionStillActive(ownerSession)) {
                    persistResolutionToRoom(ownerSession.userId, resolution)
                }
                resolution
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        // Offline or network error: load strictly from the active user's Room cache.
        loadResolutionFromRoom(ownerSession.userId, date, activePlanId)
    }

    override suspend fun getPlanById(id: Long): Result<UserWorkoutPlan> = runCatching {
        val ownerSession = captureOwnerSession()
            ?: throw IllegalStateException("Cached plan #$id requires an active session.")
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
                if (isSessionStillActive(ownerSession)) {
                    val existingEntity = workoutScheduleDao.getPlanById(ownerSession.userId, id)
                    val planEntity = domainPlan.toEntity(
                        ownerUserId = ownerSession.userId,
                        isCoachAssigned = existingEntity?.isCoachAssigned ?: false,
                        coachName = existingEntity?.coachName,
                        assignmentId = existingEntity?.assignmentId,
                        isActive = existingEntity?.isActive ?: domainPlan.isActive
                    )
                    val dayEntities = domainPlan.days.map { it.toEntity(ownerSession.userId, id) }
                    val exerciseEntities = domainPlan.days.flatMap { day ->
                        day.exercises.map { it.toEntity(ownerSession.userId, id, day.id) }
                    }
                    workoutScheduleDao.saveFullPlan(planEntity, dayEntities, exerciseEntities)
                }
                domainPlan
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
            val remoteError = remoteResult.exceptionOrNull()
            if (remoteError is HttpException && (remoteError.code() == 403 || remoteError.code() == 404)) {
                if (isSessionStillActive(ownerSession)) {
                    workoutScheduleDao.deletePlanAndChildren(ownerSession.userId, id)
                }
                throw remoteError
            }
        }
        // Fallback to Room Database for this owner only
        loadPlanFromRoom(ownerSession.userId, id)
            ?: throw IllegalStateException("Cached plan #$id is not available offline.")
    }

    private suspend fun persistResolutionToRoom(
        ownerUserId: Long,
        resolution: TodayWorkoutResolution
    ) {
        runCatching {
            // The server response is authoritative for coach assignments. Remove stale
            // coach rows before writing the resolved candidates so an inactive assignment
            // can never win when the app later falls back to Room offline.
            workoutScheduleDao.clearCoachAssignedPlansForUser(ownerUserId)
            for (candidate in resolution.candidates) {
                val isCoach = (resolution.source == WorkoutSource.COACH && candidate.assignmentId != null)
                val isActive = (candidate.plan.id == resolution.selectedPlanId)
                val planEntity = candidate.plan.toEntity(
                    ownerUserId = ownerUserId,
                    isCoachAssigned = isCoach,
                    coachName = candidate.coachName,
                    assignmentId = candidate.assignmentId,
                    isActive = isActive
                )
                val dayEntities = candidate.plan.days.map { it.toEntity(ownerUserId, candidate.plan.id) }
                val exerciseEntities = candidate.plan.days.flatMap { day ->
                    day.exercises.map { it.toEntity(ownerUserId, candidate.plan.id, day.id) }
                }
                workoutScheduleDao.saveFullPlan(planEntity, dayEntities, exerciseEntities)
            }
        }
    }

    private fun emptyResolution(date: String) = TodayWorkoutResolution(
        workoutDate = date,
        activeCoach = false,
        source = WorkoutSource.NONE,
        selectionRequired = false,
        selectedPlanId = null,
        candidates = emptyList()
    )

    private suspend fun loadResolutionFromRoom(
        ownerUserId: Long,
        date: String,
        activePlanId: Long?
    ): TodayWorkoutResolution {
        val cachedPlans = workoutScheduleDao.getAllPlans(ownerUserId)
        if (cachedPlans.isEmpty()) {
            return emptyResolution(date)
        }

        val candidates = cachedPlans.map { planEntity ->
            val days = workoutScheduleDao.getDaysForPlan(ownerUserId, planEntity.id)
            val exercises = workoutScheduleDao.getExercisesForPlan(ownerUserId, planEntity.id)
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

    private suspend fun loadPlanFromRoom(ownerUserId: Long, id: Long): UserWorkoutPlan? {
        val planEntity = workoutScheduleDao.getPlanById(ownerUserId, id) ?: return null
        val days = workoutScheduleDao.getDaysForPlan(ownerUserId, id)
        val exercises = workoutScheduleDao.getExercisesForPlan(ownerUserId, id)
        val exercisesByDay = exercises.groupBy { it.dayId }
        val dayModels = days.map { dayEntity ->
            dayEntity.toDomain(exercisesByDay[dayEntity.id].orEmpty().map { it.toDomain() })
        }
        return planEntity.toDomain(dayModels)
    }

    private suspend fun loadAllPlansFromRoom(ownerUserId: Long): List<UserWorkoutPlan> {
        val cachedPlans = workoutScheduleDao.getAllPlans(ownerUserId)
        return cachedPlans.map { planEntity ->
            val days = workoutScheduleDao.getDaysForPlan(ownerUserId, planEntity.id)
            val exercises = workoutScheduleDao.getExercisesForPlan(ownerUserId, planEntity.id)
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
        val ownerSession = captureOwnerSession()
            ?: throw IllegalStateException("Sign in to create a workout plan.")
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
        if (isSessionStillActive(ownerSession)) {
            workoutScheduleDao.insertPlan(created.toEntity(ownerSession.userId))
        }
        created
    }

    override suspend fun updatePlan(
        id: Long,
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot modify workout plans while offline." }
        val ownerSession = captureOwnerSession()
            ?: throw IllegalStateException("Sign in to modify a workout plan.")
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
        if (isSessionStillActive(ownerSession)) {
            workoutScheduleDao.insertPlan(updated.toEntity(ownerSession.userId))
        }
        updated
    }

    override suspend fun updatePlanDays(
        id: Long,
        days: List<WorkoutPlanDayConfiguration>
    ): Result<UserWorkoutPlan> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot modify workout plans while offline." }
        val ownerSession = captureOwnerSession()
            ?: throw IllegalStateException("Sign in to modify a workout plan.")
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
        if (isSessionStillActive(ownerSession)) {
            workoutScheduleDao.saveFullPlan(
                updated.toEntity(ownerSession.userId),
                updated.days.map { it.toEntity(ownerSession.userId, id) },
                updated.days.flatMap { day -> day.exercises.map { it.toEntity(ownerSession.userId, id, day.id) } }
            )
        }
        updated
    }

    override suspend fun deletePlan(id: Long): Result<Unit> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot delete workout plans while offline." }
        val ownerSession = captureOwnerSession()
            ?: throw IllegalStateException("Sign in to delete a workout plan.")
        remoteDataSource.deleteWorkoutPlan(id)
        if (isSessionStillActive(ownerSession)) {
            workoutScheduleDao.deletePlanAndChildren(ownerSession.userId, id)
            val scope = accountScope(ownerSession.userId)
            if (preferences.getLong("$scope.active_plan_id", NO_ACTIVE_PLAN_ID) == id) {
                preferences.edit().remove("$scope.active_plan_id").commitOrThrow()
            }
            runCatching { remoteDataSource.getWorkoutPlans().map { it.toDomain() } }
                .getOrNull()
                ?.let { remainingPlans ->
                    if (isSessionStillActive(ownerSession)) {
                        workoutScheduleDao.syncPersonalPlans(
                            ownerSession.userId,
                            remainingPlans.map { it.toEntity(ownerSession.userId) }
                        )
                    }
                }
            _planCacheInvalidations.tryEmit(Unit)
        }
    }

    override suspend fun getDayExercises(planId: Long, dayId: Long): Result<List<UserWorkoutPlanExercise>> = runCatching {
        val ownerSession = captureOwnerSession() ?: return@runCatching emptyList()
        if (connectivityMonitor.isConnected()) {
            val remoteResult = runCatching {
                val exercises = remoteDataSource.getWorkoutPlanDayExercises(planId, dayId).map { it.toDomain() }
                if (exercises.isNotEmpty() && isSessionStillActive(ownerSession)) {
                    val entities = exercises.map { it.toEntity(ownerSession.userId, planId, dayId) }
                    workoutScheduleDao.insertExercises(entities)
                }
                exercises
            }
            if (remoteResult.isSuccess) {
                return@runCatching remoteResult.getOrThrow()
            }
        }
        workoutScheduleDao.getExercisesForDay(ownerSession.userId, dayId).map { it.toDomain() }
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
        val ownerSession = captureOwnerSession() ?: return@runCatching null
        preferences.getLong("${accountScope(ownerSession.userId)}.active_plan_id", NO_ACTIVE_PLAN_ID)
            .takeIf { it != NO_ACTIVE_PLAN_ID }
    }

    override suspend fun setActivePlanId(planId: Long?): Result<Unit> = runCatching {
        val ownerSession = captureOwnerSession()
            ?: throw IllegalStateException("Sign in to update your active workout plan.")
        val scope = accountScope(ownerSession.userId)
        val editor = preferences.edit()
        if (planId == null) {
            editor.remove("$scope.active_plan_id")
        } else {
            editor.putLong("$scope.active_plan_id", planId)
            workoutScheduleDao.setActivePlanId(ownerSession.userId, planId)
        }
        editor.commitOrThrow()
    }

    override suspend fun generateAiWorkoutPlan(
        prompt: String,
        clientId: Long?,
        fitnessLevel: String?,
        preferredSplit: String?,
        startDate: String?,
        durationWeeks: Int?
    ): Result<AiWorkoutPlanPreview> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot generate AI workout plans while offline." }
        val request = GenerateAiWorkoutPlanRequestDto(
            prompt = prompt.trim(),
            clientId = clientId,
            fitnessLevel = fitnessLevel,
            preferredSplit = preferredSplit,
            startDate = startDate,
            durationWeeks = durationWeeks
        )
        remoteDataSource.generateAiWorkoutPlan(request).toDomain()
    }

    override suspend fun modifyAiWorkoutPlan(
        prompt: String,
        currentPlan: AiWorkoutPlanPreview,
        clientId: Long?
    ): Result<AiWorkoutPlanPreview> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot modify AI workout plans while offline." }
        val request = ModifyAiWorkoutPlanRequestDto(
            prompt = prompt.trim(),
            currentPlan = currentPlan.toDto(),
            clientId = clientId
        )
        remoteDataSource.modifyAiWorkoutPlan(request).toDomain()
    }

    override suspend fun commitAiWorkoutPlan(
        plan: AiWorkoutPlanPreview,
        idempotencyKey: String?,
        startDate: String?,
        replaceExisting: Boolean,
        clientId: Long?,
        expectedActivePlanId: Long?
    ): Result<UserWorkoutPlan> = runCatching {
        check(connectivityMonitor.isConnected()) { "Cannot commit AI workout plans while offline." }
        val request = CommitAiWorkoutPlanRequestDto(
            plan = plan.toDto(),
            idempotencyKey = idempotencyKey,
            startDate = startDate,
            replaceExisting = replaceExisting,
            expectedActivePlanId = expectedActivePlanId,
            clientId = clientId
        )
        val ownerSession = captureOwnerSession()
        val createdDto = remoteDataSource.commitAiWorkoutPlan(request)
        // Fetch full plan and cache into Room ONLY if the same session is still active
        val fullPlan = if (ownerSession != null && isSessionStillActive(ownerSession)) {
            getPlanById(createdDto.id).getOrNull() ?: createdDto.toDomain()
        } else {
            createdDto.toDomain()
        }
        if (clientId == null && ownerSession != null && isSessionStillActive(ownerSession)) {
            if (replaceExisting) {
                setActivePlanId(fullPlan.id)
            }
            runCatching { getPlans() }
        }
        fullPlan
    }

    private fun accountScope(explicitUserId: Long? = null): String {
        val resolvedId = explicitUserId?.takeIf { it > 0L }
            ?: tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        resolvedId?.let { return "user_$it" }
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
