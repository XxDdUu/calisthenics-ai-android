package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionHistoryPage
import com.calisthenics.ai.domain.model.WorkoutSessionSetUpdate

interface WorkoutSessionRepository {
    suspend fun startOrResume(
        planId: Long,
        dayId: Long,
        date: String,
        assignmentId: Long?
    ): Result<WorkoutSession>
    suspend fun startScheduledSession(sessionId: Long): Result<WorkoutSession> =
        Result.failure(UnsupportedOperationException("Scheduled session start is not supported"))
    suspend fun getSession(sessionId: Long): Result<WorkoutSession>
    suspend fun getActiveSessions(): Result<List<WorkoutSession>>
    suspend fun getUpcomingSessions(): Result<List<WorkoutSession>> = Result.success(emptyList())
    suspend fun getHistory(page: Int, size: Int): Result<WorkoutSessionHistoryPage>
    fun hasPendingChanges(sessionId: Long): Boolean
    suspend fun reapplyConflictedChanges(sessionId: Long): Result<WorkoutSession>
    suspend fun saveSetDraft(sessionId: Long, exerciseId: Long, setId: Long, update: WorkoutSessionSetUpdate): Result<Unit>
    suspend fun updateSet(sessionId: Long, exerciseId: Long, setId: Long, update: WorkoutSessionSetUpdate): Result<WorkoutSession>
    suspend fun addSet(sessionId: Long, exerciseId: Long): Result<WorkoutSession>
    suspend fun deleteSet(sessionId: Long, exerciseId: Long, setId: Long): Result<WorkoutSession>
    fun observeActiveSessions(): kotlinx.coroutines.flow.Flow<List<WorkoutSession>> =
        kotlinx.coroutines.flow.emptyFlow()
    suspend fun updateExercise(
        sessionId: Long,
        exerciseId: Long,
        status: String,
        expectedVersion: Long
    ): Result<WorkoutSession>
    fun hasSyncConflict(sessionId: Long): Boolean
    suspend fun finish(sessionId: Long, confirmPartial: Boolean): Result<WorkoutSession>
}
