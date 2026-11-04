package com.calisthenics.ai.data.repository

import android.content.Context
import android.util.Log
import com.calisthenics.ai.data.local.TokenManager
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
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSessionRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val api: CalisthenicsApi,
    private val tokenManager: TokenManager
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

    override suspend fun getSession(sessionId: Long): Result<WorkoutSession> = runCatching {
        mergeAndSync(api.getWorkoutSession(sessionId).toDomain())
    }

    override suspend fun getActiveSessions(): Result<List<WorkoutSession>> = runCatching {
        api.getActiveWorkoutSessions().map { mergeAndSync(it.toDomain()) }
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
        val pending = PendingSetUpdate(sessionId, exerciseId, setId, update)
        synchronized(lock) {
            val key = pendingSetKey()
            val updates = readPendingSets(key).filterNot { it.setId == setId && it.sessionId == sessionId } + pending
            commit(key, gson.toJson(updates))
            conflictedUpdates.remove(PendingConflict(sessionId, setId, isExercise = false))
        }
    }

    override suspend fun updateSet(
        sessionId: Long,
        exerciseId: Long,
        setId: Long,
        update: WorkoutSessionSetUpdate
    ): Result<WorkoutSession> = runCatching {
        val pending = PendingSetUpdate(sessionId, exerciseId, setId, update)
        savePendingSet(pending)
        val responseDto = try {
            api.updateWorkoutSessionSet(sessionId, exerciseId, setId, update.toDto())
        } catch (error: HttpException) {
            if (error.code() == 409) markConflict(PendingConflict(sessionId, setId, isExercise = false))
            throw error
        }
        val response = responseDto.toDomain()
        removePendingSet(pending)
        response
    }

    override suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        status: String,
        expectedVersion: Long
    ): Result<WorkoutSession> = runCatching {
        val pending = PendingExerciseUpdate(sessionId, exerciseId, status, expectedVersion)
        savePendingExercise(pending)
        val responseDto = try {
            api.updateWorkoutSessionExercise(
                sessionId,
                exerciseId,
                UpdateWorkoutSessionExerciseRequestDto(status, expectedVersion)
            )
        } catch (error: HttpException) {
            if (error.code() == 409) markConflict(PendingConflict(sessionId, exerciseId, isExercise = true))
            throw error
        }
        val response = responseDto.toDomain()
        removePendingExercise(pending)
        response
    }

    override suspend fun finish(sessionId: Long, confirmPartial: Boolean): Result<WorkoutSession> = runCatching {
        synchronizePending(sessionId)
        if (hasPending(sessionId)) error("Some set changes are still waiting to sync. Reconnect and try again.")
        api.finishWorkoutSession(sessionId, FinishWorkoutSessionRequestDto(confirmPartial)).toDomain()
    }

    override fun hasPendingChanges(sessionId: Long): Boolean = hasPending(sessionId)

    override fun hasSyncConflict(sessionId: Long): Boolean = synchronized(lock) {
        conflictedUpdates.any { it.sessionId == sessionId }
    }

    override suspend fun reapplyConflictedChanges(sessionId: Long): Result<WorkoutSession> = runCatching {
        val current = api.getWorkoutSession(sessionId).toDomain()
        pendingSets().filter {
            it.sessionId == sessionId && hasConflict(PendingConflict(sessionId, it.setId, isExercise = false))
        }.forEach { pending ->
            val latestVersion = current.exercises.firstOrNull { it.id == pending.exerciseId }
                ?.sets?.firstOrNull { it.id == pending.setId }
                ?.version ?: error("The conflicting set no longer exists in this workout.")
            val retried = pending.copy(update = pending.update.copy(expectedVersion = latestVersion))
            savePendingSet(retried)
            try {
                api.updateWorkoutSessionSet(
                    sessionId, pending.exerciseId, pending.setId, retried.update.toDto()
                )
            } catch (error: HttpException) {
                if (error.code() == 409) markConflict(PendingConflict(sessionId, pending.setId, isExercise = false))
                throw error
            }
            removePendingSet(retried)
        }
        pendingExercises().filter {
            it.sessionId == sessionId &&
                hasConflict(PendingConflict(sessionId, it.exerciseId, isExercise = true))
        }.forEach { pending ->
            val latestVersion = current.exercises.firstOrNull { it.id == pending.exerciseId }
                ?.version ?: error("The conflicting exercise no longer exists in this workout.")
            val retried = pending.copy(expectedVersion = latestVersion)
            savePendingExercise(retried)
            try {
                api.updateWorkoutSessionExercise(
                    sessionId,
                    pending.exerciseId,
                    UpdateWorkoutSessionExerciseRequestDto(retried.status, latestVersion)
                )
            } catch (error: HttpException) {
                if (error.code() == 409) markConflict(PendingConflict(sessionId, pending.exerciseId, isExercise = true))
                throw error
            }
            removePendingExercise(retried)
        }
        api.getWorkoutSession(sessionId).toDomain()
    }

    private suspend fun mergeAndSync(server: WorkoutSession): WorkoutSession {
        val localSets = pendingSets().filter { it.sessionId == server.id }
        val localExercises = pendingExercises().filter { it.sessionId == server.id }
        var merged = applyPending(server, localSets, localExercises)
        synchronizePending(server.id)
        val remainingSets = pendingSets().filter { it.sessionId == server.id }
        val remainingExercises = pendingExercises().filter { it.sessionId == server.id }
        if (remainingSets.isEmpty() && remainingExercises.isEmpty()) {
            merged = api.getWorkoutSession(server.id).toDomain()
        } else if (hasSyncConflict(server.id)) {
            merged = applyPending(api.getWorkoutSession(server.id).toDomain(), remainingSets, remainingExercises)
        } else {
            merged = applyPending(merged, remainingSets, remainingExercises)
        }
        return merged
    }

    private suspend fun synchronizePending(sessionId: Long) {
        pendingSets().filter {
            it.sessionId == sessionId &&
                !hasConflict(PendingConflict(sessionId, it.setId, isExercise = false))
        }.forEach { pending ->
            runCatching {
                api.updateWorkoutSessionSet(
                    pending.sessionId,
                    pending.exerciseId,
                    pending.setId,
                    pending.update.toDto()
                )
                removePendingSet(pending)
            }.onFailure { error ->
                if (error is HttpException && error.code() == 409) {
                    markConflict(PendingConflict(pending.sessionId, pending.setId, isExercise = false))
                }
                Log.w(TAG, "Workout set ${pending.setId} remains queued for synchronization.", error)
            }
        }
        pendingExercises().filter {
            it.sessionId == sessionId &&
                !hasConflict(PendingConflict(sessionId, it.exerciseId, isExercise = true))
        }.forEach { pending ->
            runCatching {
                api.updateWorkoutSessionExercise(
                    pending.sessionId,
                    pending.exerciseId,
                    UpdateWorkoutSessionExerciseRequestDto(pending.status, pending.expectedVersion)
                )
                removePendingExercise(pending)
            }.onFailure { error ->
                if (error is HttpException && error.code() == 409) {
                    markConflict(PendingConflict(pending.sessionId, pending.exerciseId, isExercise = true))
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

    private fun savePendingSet(pending: PendingSetUpdate) {
        synchronized(lock) {
            val key = pendingSetKey()
            val entries = readPendingSets(key).filterNot {
                it.setId == pending.setId && it.sessionId == pending.sessionId
            } + pending
            commit(key, gson.toJson(entries))
            conflictedUpdates.remove(PendingConflict(pending.sessionId, pending.setId, isExercise = false))
        }
    }

    private fun savePendingExercise(pending: PendingExerciseUpdate) {
        synchronized(lock) {
            val key = pendingExerciseKey()
            val entries = readPendingExercises(key)
                .filterNot { it.exerciseId == pending.exerciseId && it.sessionId == pending.sessionId } + pending
            commit(key, gson.toJson(entries))
            conflictedUpdates.remove(PendingConflict(pending.sessionId, pending.exerciseId, isExercise = true))
        }
    }

    private fun removePendingSet(expected: PendingSetUpdate) {
        synchronized(lock) {
            val key = pendingSetKey()
            commit(key, gson.toJson(readPendingSets(key).filterNot {
                it.sessionId == expected.sessionId && it.setId == expected.setId && it == expected
            }))
            conflictedUpdates.remove(PendingConflict(expected.sessionId, expected.setId, isExercise = false))
        }
    }

    private fun removePendingExercise(expected: PendingExerciseUpdate) {
        synchronized(lock) {
            val key = pendingExerciseKey()
            commit(key, gson.toJson(readPendingExercises(key).filterNot {
                it.sessionId == expected.sessionId && it.exerciseId == expected.exerciseId && it == expected
            }))
            conflictedUpdates.remove(PendingConflict(expected.sessionId, expected.exerciseId, isExercise = true))
        }
    }

    private fun hasPending(sessionId: Long): Boolean =
        pendingSets().any { it.sessionId == sessionId } ||
            pendingExercises().any { it.sessionId == sessionId }

    private fun pendingSets(): List<PendingSetUpdate> = synchronized(lock) {
        readPendingSets(pendingSetKey())
    }

    private fun pendingExercises(): List<PendingExerciseUpdate> = synchronized(lock) {
        readPendingExercises(pendingExerciseKey())
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

    private fun pendingSetKey() = "user.${requireUserId()}.pending_set_updates"
    private fun pendingExerciseKey() = "user.${requireUserId()}.pending_exercise_updates"

    private fun requireUserId(): Long =
        requireNotNull(tokenManager.getUserId()) { "Sign in to save workout session changes." }

    private fun commit(key: String, value: String) {
        check(preferences.edit().putString(key, value).commit()) {
            "Could not save the pending workout change on this device."
        }
    }

    private fun WorkoutSessionSetUpdate.toDto() = UpdateWorkoutSessionSetRequestDto(
        reps, holdDurationSeconds, weightKg, assistanceWeightKg, completed, notes, expectedVersion
    )

    private fun WorkoutSessionResponseDto.toDomain() = WorkoutSession(
        id, workoutPlanId, workoutPlanDayId, assignmentId, source, title, workoutDate, status, startedAt, lastSavedAt,
        completedAt, durationSeconds, completedExercises, skippedExercises, unfinishedExercises,
        completedSets, totalReps, totalHoldDurationSeconds, exercises.map { it.toDomain() }
    )

    private fun com.calisthenics.ai.data.remote.dto.WorkoutSessionExerciseDto.toDomain() =
        WorkoutSessionExercise(
            id, version, exerciseId, exerciseName, instructions, notes, orderIndex, targetSets, targetReps,
            targetHoldDurationSeconds, restSeconds, status, sets.map { it.toDomain() }
        )

    private fun com.calisthenics.ai.data.remote.dto.WorkoutSessionSetDto.toDomain() = WorkoutSessionSet(
        id, version, setNumber, reps, holdDurationSeconds, weightKg, assistanceWeightKg, completed, completedAt, notes
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
