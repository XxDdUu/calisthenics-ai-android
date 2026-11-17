package com.calisthenics.ai.data.repository

import android.content.Context
import android.util.Log
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.local.database.CachedActiveWorkoutSessionEntity
import com.calisthenics.ai.data.local.database.WorkoutSessionDao
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.FinishWorkoutSessionRequestDto
import com.calisthenics.ai.data.remote.dto.StartWorkoutSessionRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutSessionExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutSessionSetRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutSessionResponseDto
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionHistoryPage
import com.calisthenics.ai.domain.model.WorkoutSessionSet
import com.calisthenics.ai.domain.model.WorkoutSessionSetUpdate
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSessionRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val api: CalisthenicsApi,
    private val tokenManager: TokenManager,
    private val workoutSessionDao: WorkoutSessionDao,
    private val gettingStartedRepository: GettingStartedRepository
) : WorkoutSessionRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()
    private val conflictedUpdates = mutableSetOf<PendingConflict>()

    override suspend fun startOrResume(
        planId: Long,
        dayId: Long,
        date: String,
        assignmentId: Long?
    ): Result<WorkoutSession> =
        runCatching {
            mergeAndSync(
                api.startOrResumeWorkoutSession(
                    StartWorkoutSessionRequestDto(planId, dayId, date, assignmentId)
                ).toDomain()
            )
        }

    override suspend fun startScheduledSession(sessionId: Long): Result<WorkoutSession> =
        runCatching {
            val session = mergeAndSync(api.startScheduledWorkoutSession(sessionId).toDomain())
            if (session.source == "GETTING_STARTED") {
                gettingStartedRepository.notifyEnrollmentChanged()
            }
            session
        }

    override suspend fun getSession(sessionId: Long): Result<WorkoutSession> = runCatching {
        try {
            mergeAndSync(api.getWorkoutSession(sessionId).toDomain())
        } catch (e: Exception) {
            val ownerUserId = currentUserId()
            val cached = if (ownerUserId != null) {
                workoutSessionDao.getActiveSession(ownerUserId, sessionId)?.let { entity ->
                    runCatching { gson.fromJson(entity.sessionJson, WorkoutSession::class.java) }.getOrNull()
                }
            } else null
            if (cached != null) {
                val localSets = pendingSets(ownerUserId!!).filter { it.sessionId == sessionId }
                val localExercises = pendingExercises(ownerUserId).filter { it.sessionId == sessionId }
                applyPending(cached, localSets, localExercises)
            } else {
                throw e
            }
        }
    }

    override suspend fun getActiveSessions(): Result<List<WorkoutSession>> = runCatching {
        val today = LocalDate.now()
        val ownerUserId = currentUserId()
        try {
            api.getActiveWorkoutSessions(today.toString())
                .map { mergeAndSync(it.toDomain()) }
                .filter {
                    it.effectiveClassification(today) ==
                        com.calisthenics.ai.domain.model.WorkoutSessionClassification.UNFINISHED
                }
        } catch (e: Exception) {
            if (ownerUserId != null) {
                val cachedEntities = workoutSessionDao.getActiveSessionsForUser(ownerUserId)
                val localSets = pendingSets(ownerUserId)
                val localExercises = pendingExercises(ownerUserId)
                cachedEntities.mapNotNull { entity ->
                    runCatching { gson.fromJson(entity.sessionJson, WorkoutSession::class.java) }.getOrNull()
                }.map { session ->
                    val sSets = localSets.filter { it.sessionId == session.id }
                    val sExs = localExercises.filter { it.sessionId == session.id }
                    applyPending(session, sSets, sExs)
                }.filter {
                    it.effectiveClassification(today) ==
                        com.calisthenics.ai.domain.model.WorkoutSessionClassification.UNFINISHED
                }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun getUpcomingSessions(): Result<List<WorkoutSession>> = runCatching {
        val today = LocalDate.now()
        api.getUpcomingWorkoutSessions(today.toString())
            .map { it.toDomain() }
            .filter {
                it.effectiveClassification(today) ==
                    com.calisthenics.ai.domain.model.WorkoutSessionClassification.UPCOMING
            }
    }

    override suspend fun getHistory(page: Int, size: Int): Result<WorkoutSessionHistoryPage> = runCatching {
        api.getWorkoutSessionHistory(page, size).let { response ->
            WorkoutSessionHistoryPage(
                content = response.content.map { it.toDomain() },
                page = response.page,
                size = response.size,
                totalElements = response.totalElements,
                totalPages = response.totalPages
            )
        }
    }

    override suspend fun saveSetDraft(
        sessionId: Long,
        exerciseId: Long,
        setId: Long,
        update: WorkoutSessionSetUpdate
    ): Result<Unit> = runCatching {
        val ownerUserId = requireUserId()
        val pending = PendingSetUpdate(sessionId, exerciseId, setId, update)
        synchronized(lock) {
            val key = pendingSetKey(ownerUserId)
            val updates = readPendingSets(key).filterNot { it.setId == setId && it.sessionId == sessionId } + pending
            commit(key, gson.toJson(updates))
            conflictedUpdates.remove(PendingConflict(ownerUserId, sessionId, setId, isExercise = false))
        }
        val cachedEntity = workoutSessionDao.getActiveSession(ownerUserId, sessionId)
        if (cachedEntity != null) {
            val cachedSession = runCatching { gson.fromJson(cachedEntity.sessionJson, WorkoutSession::class.java) }.getOrNull()
            if (cachedSession != null) {
                val updatedSession = cachedSession.updateSet(exerciseId, setId, update)
                if (updatedSession != null) {
                    cacheSessionLocally(updatedSession)
                }
            }
        }
    }

    override suspend fun updateSet(
        sessionId: Long,
        exerciseId: Long,
        setId: Long,
        update: WorkoutSessionSetUpdate
    ): Result<WorkoutSession> = runCatching {
        val ownerUserId = requireUserId()
        val pending = PendingSetUpdate(sessionId, exerciseId, setId, update)
        savePendingSet(ownerUserId, pending)
        val responseDto = try {
            api.updateWorkoutSessionSet(sessionId, exerciseId, setId, update.toDto())
        } catch (error: HttpException) {
            if (error.code() == 409) markConflict(PendingConflict(ownerUserId, sessionId, setId, isExercise = false))
            throw error
        }
        val response = responseDto.toDomain()
        removePendingSet(ownerUserId, pending)
        response
    }

    override suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        status: String,
        expectedVersion: Long
    ): Result<WorkoutSession> = runCatching {
        val ownerUserId = requireUserId()
        val pending = PendingExerciseUpdate(sessionId, exerciseId, status, expectedVersion)
        savePendingExercise(ownerUserId, pending)
        val responseDto = try {
            api.updateWorkoutSessionExercise(
                sessionId,
                exerciseId,
                UpdateWorkoutSessionExerciseRequestDto(status, expectedVersion)
            )
        } catch (error: HttpException) {
            if (error.code() == 409) markConflict(PendingConflict(ownerUserId, sessionId, exerciseId, isExercise = true))
            throw error
        }
        val response = responseDto.toDomain()
        removePendingExercise(ownerUserId, pending)
        response
    }

    override suspend fun finish(sessionId: Long, confirmPartial: Boolean): Result<WorkoutSession> = runCatching {
        val ownerUserId = requireUserId()
        synchronizePending(ownerUserId, sessionId)
        if (hasPending(ownerUserId, sessionId)) error("Some set changes are still waiting to sync. Reconnect and try again.")
        val finished = api.finishWorkoutSession(sessionId, FinishWorkoutSessionRequestDto(confirmPartial)).toDomain()
        workoutSessionDao.deleteActiveSession(ownerUserId, sessionId)
        if (finished.source == "GETTING_STARTED") {
            gettingStartedRepository.notifyEnrollmentChanged()
        }
        finished
    }

    override suspend fun addSet(sessionId: Long, exerciseId: Long): Result<WorkoutSession> = runCatching {
        val ownerUserId = requireUserId()
        try {
            val response = api.addWorkoutSessionSet(sessionId, exerciseId).toDomain()
            mergeAndSync(response)
        } catch (e: Exception) {
            val cachedEntity = workoutSessionDao.getActiveSession(ownerUserId, sessionId)
            val current = cachedEntity?.let {
                runCatching { gson.fromJson(it.sessionJson, WorkoutSession::class.java) }.getOrNull()
            } ?: throw e
            val targetExercise = current.exercises.firstOrNull { it.id == exerciseId } ?: throw e
            val nextSetNum = (targetExercise.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
            val localSetId = -System.currentTimeMillis()
            val newSet = WorkoutSessionSet(
                id = localSetId,
                version = 0L,
                setNumber = nextSetNum,
                reps = targetExercise.targetReps,
                holdDurationSeconds = targetExercise.targetHoldDurationSeconds,
                weightKg = null,
                assistanceWeightKg = null,
                completed = false,
                completedAt = null,
                notes = null
            )
            val updatedEx = targetExercise.copy(
                targetSets = targetExercise.targetSets + 1,
                sets = targetExercise.sets + newSet
            )
            val updatedSession = current.copy(
                exercises = current.exercises.map { if (it.id == exerciseId) updatedEx else it }
            )
            cacheSessionLocally(updatedSession)
            updatedSession
        }
    }

    override suspend fun deleteSet(sessionId: Long, exerciseId: Long, setId: Long): Result<WorkoutSession> = runCatching {
        val ownerUserId = requireUserId()
        try {
            val response = api.deleteWorkoutSessionSet(sessionId, exerciseId, setId).toDomain()
            mergeAndSync(response)
        } catch (e: Exception) {
            val cachedEntity = workoutSessionDao.getActiveSession(ownerUserId, sessionId)
            val current = cachedEntity?.let {
                runCatching { gson.fromJson(it.sessionJson, WorkoutSession::class.java) }.getOrNull()
            } ?: throw e
            val targetExercise = current.exercises.firstOrNull { it.id == exerciseId } ?: throw e
            val updatedSets = targetExercise.sets.filterNot { it.id == setId }
                .mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }
            val updatedEx = targetExercise.copy(
                targetSets = updatedSets.size,
                sets = updatedSets
            )
            val updatedSession = current.copy(
                exercises = current.exercises.map { if (it.id == exerciseId) updatedEx else it }
            )
            cacheSessionLocally(updatedSession)
            updatedSession
        }
    }

    override fun observeActiveSessions(): Flow<List<WorkoutSession>> {
        val ownerUserId = currentUserId() ?: return emptyFlow()
        return workoutSessionDao.observeActiveSessionsForUser(ownerUserId).map { entities ->
            val today = LocalDate.now()
            entities.mapNotNull { entity ->
                runCatching { gson.fromJson(entity.sessionJson, WorkoutSession::class.java) }.getOrNull()
            }.filter {
                it.effectiveClassification(today) ==
                    com.calisthenics.ai.domain.model.WorkoutSessionClassification.UNFINISHED
            }
        }
    }

    override fun hasPendingChanges(sessionId: Long): Boolean {
        val ownerUserId = currentUserId() ?: return false
        return hasPending(ownerUserId, sessionId)
    }

    override fun hasSyncConflict(sessionId: Long): Boolean {
        val ownerUserId = currentUserId() ?: return false
        return synchronized(lock) {
            conflictedUpdates.any { it.userId == ownerUserId && it.sessionId == sessionId }
        }
    }

    override suspend fun reapplyConflictedChanges(sessionId: Long): Result<WorkoutSession> = runCatching {
        val ownerUserId = requireUserId()
        val current = api.getWorkoutSession(sessionId).toDomain()
        pendingSets(ownerUserId).filter {
            it.sessionId == sessionId && hasConflict(PendingConflict(ownerUserId, sessionId, it.setId, isExercise = false))
        }.forEach { pending ->
            val latestVersion = current.exercises.firstOrNull { it.id == pending.exerciseId }
                ?.sets?.firstOrNull { it.id == pending.setId }
                ?.version ?: error("The conflicting set no longer exists in this workout.")
            val retried = pending.copy(update = pending.update.copy(expectedVersion = latestVersion))
            savePendingSet(ownerUserId, retried)
            try {
                api.updateWorkoutSessionSet(
                    sessionId, pending.exerciseId, pending.setId, retried.update.toDto()
                )
            } catch (error: HttpException) {
                if (error.code() == 409) markConflict(PendingConflict(ownerUserId, sessionId, pending.setId, isExercise = false))
                throw error
            }
            removePendingSet(ownerUserId, retried)
        }
        pendingExercises(ownerUserId).filter {
            it.sessionId == sessionId &&
                hasConflict(PendingConflict(ownerUserId, sessionId, it.exerciseId, isExercise = true))
        }.forEach { pending ->
            val latestVersion = current.exercises.firstOrNull { it.id == pending.exerciseId }
                ?.version ?: error("The conflicting exercise no longer exists in this workout.")
            val retried = pending.copy(expectedVersion = latestVersion)
            savePendingExercise(ownerUserId, retried)
            try {
                api.updateWorkoutSessionExercise(
                    sessionId,
                    pending.exerciseId,
                    UpdateWorkoutSessionExerciseRequestDto(retried.status, latestVersion)
                )
            } catch (error: HttpException) {
                if (error.code() == 409) markConflict(PendingConflict(ownerUserId, sessionId, pending.exerciseId, isExercise = true))
                throw error
            }
            removePendingExercise(ownerUserId, retried)
        }
        api.getWorkoutSession(sessionId).toDomain()
    }

    private suspend fun mergeAndSync(server: WorkoutSession): WorkoutSession {
        val ownerUserId = currentUserId() ?: return server
        val localSets = pendingSets(ownerUserId).filter { it.sessionId == server.id }
        val localExercises = pendingExercises(ownerUserId).filter { it.sessionId == server.id }
        var merged = applyPending(server, localSets, localExercises)
        synchronizePending(ownerUserId, server.id)
        val remainingSets = pendingSets(ownerUserId).filter { it.sessionId == server.id }
        val remainingExercises = pendingExercises(ownerUserId).filter { it.sessionId == server.id }
        if (remainingSets.isEmpty() && remainingExercises.isEmpty()) {
            merged = api.getWorkoutSession(server.id).toDomain()
        } else if (hasSyncConflict(server.id)) {
            merged = applyPending(api.getWorkoutSession(server.id).toDomain(), remainingSets, remainingExercises)
        } else {
            merged = applyPending(merged, remainingSets, remainingExercises)
        }
        cacheSessionLocally(merged)
        return merged
    }

    private suspend fun cacheSessionLocally(session: WorkoutSession) {
        val ownerUserId = currentUserId() ?: return
        runCatching {
            workoutSessionDao.saveActiveSession(
                CachedActiveWorkoutSessionEntity(
                    ownerUserId = ownerUserId,
                    sessionId = session.id,
                    sessionJson = gson.toJson(session),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun synchronizePending(ownerUserId: Long, sessionId: Long) {
        pendingSets(ownerUserId).filter {
            it.sessionId == sessionId &&
                !hasConflict(PendingConflict(ownerUserId, sessionId, it.setId, isExercise = false))
        }.forEach { pending ->
            runCatching {
                api.updateWorkoutSessionSet(
                    pending.sessionId,
                    pending.exerciseId,
                    pending.setId,
                    pending.update.toDto()
                )
                removePendingSet(ownerUserId, pending)
            }.onFailure { error ->
                if (error is HttpException && error.code() == 409) {
                    markConflict(PendingConflict(ownerUserId, pending.sessionId, pending.setId, isExercise = false))
                }
                Log.w(TAG, "Workout set ${pending.setId} remains queued for synchronization.", error)
            }
        }
        pendingExercises(ownerUserId).filter {
            it.sessionId == sessionId &&
                !hasConflict(PendingConflict(ownerUserId, sessionId, it.exerciseId, isExercise = true))
        }.forEach { pending ->
            runCatching {
                api.updateWorkoutSessionExercise(
                    pending.sessionId,
                    pending.exerciseId,
                    UpdateWorkoutSessionExerciseRequestDto(pending.status, pending.expectedVersion)
                )
                removePendingExercise(ownerUserId, pending)
            }.onFailure { error ->
                if (error is HttpException && error.code() == 409) {
                    markConflict(PendingConflict(ownerUserId, pending.sessionId, pending.exerciseId, isExercise = true))
                }
                Log.w(TAG, "Workout exercise ${pending.exerciseId} remains queued for synchronization.", error)
            }
        }
    }

    private fun applyPending(
        session: WorkoutSession,
        sets: List<PendingSetUpdate>,
        exercises: List<PendingExerciseUpdate>
    ): WorkoutSession = session.copy(
        exercises = session.exercises.map { exercise ->
            val pendingStatus = exercises.lastOrNull {
                it.exerciseId == exercise.id && it.sessionId == session.id
            }?.status
            exercise.copy(
                status = pendingStatus ?: exercise.status,
                sets = exercise.sets.map { set ->
                    sets.lastOrNull { it.setId == set.id && it.sessionId == session.id }
                        ?.update?.let { set.applyUpdate(it) } ?: set
                }
            )
        }
    )

    private fun WorkoutSessionSet.applyUpdate(update: WorkoutSessionSetUpdate) = copy(
        reps = update.reps,
        holdDurationSeconds = update.holdDurationSeconds,
        weightKg = update.weightKg,
        assistanceWeightKg = update.assistanceWeightKg,
        completed = update.completed,
        completedAt = if (update.completed) java.time.Instant.now().toString() else null,
        notes = update.notes
    )

    private fun savePendingSet(ownerUserId: Long, pending: PendingSetUpdate) {
        synchronized(lock) {
            val key = pendingSetKey(ownerUserId)
            val entries = readPendingSets(key).filterNot {
                it.setId == pending.setId && it.sessionId == pending.sessionId
            } + pending
            commit(key, gson.toJson(entries))
            conflictedUpdates.remove(PendingConflict(ownerUserId, pending.sessionId, pending.setId, isExercise = false))
        }
    }

    private fun savePendingExercise(ownerUserId: Long, pending: PendingExerciseUpdate) {
        synchronized(lock) {
            val key = pendingExerciseKey(ownerUserId)
            val entries = readPendingExercises(key)
                .filterNot { it.exerciseId == pending.exerciseId && it.sessionId == pending.sessionId } + pending
            commit(key, gson.toJson(entries))
            conflictedUpdates.remove(PendingConflict(ownerUserId, pending.sessionId, pending.exerciseId, isExercise = true))
        }
    }

    private fun removePendingSet(ownerUserId: Long, expected: PendingSetUpdate) {
        synchronized(lock) {
            val key = pendingSetKey(ownerUserId)
            commit(key, gson.toJson(readPendingSets(key).filterNot {
                it.sessionId == expected.sessionId && it.setId == expected.setId && it == expected
            }))
            conflictedUpdates.remove(PendingConflict(ownerUserId, expected.sessionId, expected.setId, isExercise = false))
        }
    }

    private fun removePendingExercise(ownerUserId: Long, expected: PendingExerciseUpdate) {
        synchronized(lock) {
            val key = pendingExerciseKey(ownerUserId)
            commit(key, gson.toJson(readPendingExercises(key).filterNot {
                it.sessionId == expected.sessionId && it.exerciseId == expected.exerciseId && it == expected
            }))
            conflictedUpdates.remove(PendingConflict(ownerUserId, expected.sessionId, expected.exerciseId, isExercise = true))
        }
    }

    private fun hasPending(ownerUserId: Long, sessionId: Long): Boolean =
        pendingSets(ownerUserId).any { it.sessionId == sessionId } ||
            pendingExercises(ownerUserId).any { it.sessionId == sessionId }

    private fun pendingSets(ownerUserId: Long): List<PendingSetUpdate> = synchronized(lock) {
        readPendingSets(pendingSetKey(ownerUserId))
    }

    private fun pendingExercises(ownerUserId: Long): List<PendingExerciseUpdate> = synchronized(lock) {
        readPendingExercises(pendingExerciseKey(ownerUserId))
    }

    private fun readPendingSets(key: String): List<PendingSetUpdate> {
        val json = preferences.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<PendingSetUpdate>>() {}.type
        return gson.fromJson<List<PendingSetUpdate>>(json, type).orEmpty()
    }

    private fun readPendingExercises(key: String): List<PendingExerciseUpdate> {
        val json = preferences.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<PendingExerciseUpdate>>() {}.type
        return gson.fromJson<List<PendingExerciseUpdate>>(json, type).orEmpty()
    }

    private fun pendingSetKey(ownerUserId: Long) = "user.$ownerUserId.pending_set_updates"
    private fun pendingExerciseKey(ownerUserId: Long) = "user.$ownerUserId.pending_exercise_updates"

    private fun currentUserId(): Long? =
        tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }

    private fun requireUserId(): Long =
        requireNotNull(currentUserId()) { "Sign in to save workout session changes." }

    fun clearConflictedUpdatesForOtherUsers(activeUserId: Long?) {
        synchronized(lock) {
            if (activeUserId == null || activeUserId <= 0L) {
                conflictedUpdates.clear()
            } else {
                conflictedUpdates.removeAll { it.userId != activeUserId }
            }
        }
    }

    private fun commit(key: String, value: String) {
        check(preferences.edit().putString(key, value).commit()) {
            "Could not save the pending workout change on this device."
        }
    }

    private fun WorkoutSessionSetUpdate.toDto() = UpdateWorkoutSessionSetRequestDto(
        reps, holdDurationSeconds, weightKg, assistanceWeightKg, completed, notes, expectedVersion
    )

    private data class PendingSetUpdate(
        val sessionId: Long,
        val exerciseId: Long,
        val setId: Long,
        val update: WorkoutSessionSetUpdate
    )

    private data class PendingExerciseUpdate(
        val sessionId: Long,
        val exerciseId: Long,
        val status: String,
        val expectedVersion: Long
    )

    private data class PendingConflict(
        val userId: Long,
        val sessionId: Long,
        val itemId: Long,
        val isExercise: Boolean
    )

    private fun markConflict(conflict: PendingConflict) {
        synchronized(lock) { conflictedUpdates.add(conflict) }
    }

    private fun hasConflict(conflict: PendingConflict): Boolean = synchronized(lock) {
        conflict in conflictedUpdates
    }

    private companion object {
        const val PREFERENCES_NAME = "workout_session_recovery"
        const val TAG = "WorkoutSessionRepo"
    }
}
