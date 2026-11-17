package com.calisthenics.ai.ui.liveanalysis

import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.LiveSessionRecord
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache bridging just-completed live workout sessions to [WorkoutAnalysisSummaryScreen]
 * so the summary renders immediately without waiting on backend completion or insight generation.
 * Also coordinates in-flight backend `completeSession` requests to eliminate races before requesting insights.
 */
@Singleton
class LiveSessionSnapshotCache private constructor(
    private val accountSessionManager: AccountSessionManager?,
    @Suppress("UNUSED_PARAMETER") managed: Boolean
) {
    @Inject
    constructor(accountSessionManager: AccountSessionManager) : this(accountSessionManager, true)

    constructor() : this(null, false)

    private data class OwnedRecord(
        val ownerUserId: Long?,
        val record: LiveSessionRecord
    )

    private data class OwnedSignal(
        val ownerUserId: Long?,
        val signal: CompletableDeferred<Boolean>
    )

    private val cache = ConcurrentHashMap<String, OwnedRecord>()
    private val completionSignals = ConcurrentHashMap<String, OwnedSignal>()

    fun put(record: LiveSessionRecord, ownerUserId: Long? = accountSessionManager?.currentUserId()) {
        cache[record.id] = OwnedRecord(ownerUserId = ownerUserId, record = record)
    }

    fun get(sessionId: String, ownerUserId: Long? = accountSessionManager?.currentUserId()): LiveSessionRecord? {
        val entry = cache[sessionId] ?: return null
        if (entry.ownerUserId != null && ownerUserId != null && entry.ownerUserId != ownerUserId) {
            return null
        }
        if (entry.ownerUserId != null && ownerUserId == null) {
            return null
        }
        return entry.record
    }

    fun markCompletionStarted(sessionId: String, ownerUserId: Long? = accountSessionManager?.currentUserId()) {
        completionSignals[sessionId] = OwnedSignal(
            ownerUserId = ownerUserId,
            signal = CompletableDeferred()
        )
    }

    fun markCompletionFinished(sessionId: String, success: Boolean) {
        completionSignals[sessionId]?.signal?.complete(success)
    }

    suspend fun awaitCompletionIfInFlight(
        sessionId: String,
        ownerUserId: Long? = accountSessionManager?.currentUserId()
    ): Boolean {
        val entry = completionSignals[sessionId] ?: return true
        if (entry.ownerUserId != null && ownerUserId != entry.ownerUserId) {
            return false
        }
        return try {
            entry.signal.await()
        } catch (_: Exception) {
            false
        }
    }

    fun clear(sessionId: String) {
        cache.remove(sessionId)
        completionSignals.remove(sessionId)
    }

    fun clearForOtherUsers(activeUserId: Long?) {
        cache.entries.removeIf { (_, value) ->
            activeUserId == null || (value.ownerUserId != null && value.ownerUserId != activeUserId)
        }
        completionSignals.entries.removeIf { (_, value) ->
            val shouldRemove = activeUserId == null || (value.ownerUserId != null && value.ownerUserId != activeUserId)
            if (shouldRemove) {
                value.signal.cancel()
            }
            shouldRemove
        }
    }
}
