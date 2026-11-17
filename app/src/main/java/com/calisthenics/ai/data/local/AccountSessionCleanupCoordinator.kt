package com.calisthenics.ai.data.local

import com.calisthenics.ai.data.remote.websocket.ChatWebSocketClient
import com.calisthenics.ai.data.repository.WorkoutSessionRepositoryImpl
import com.calisthenics.ai.ui.liveanalysis.LiveSessionSnapshotCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates global cleanup of transport connections, in-memory caches, and in-flight HTTP calls
 * whenever the authenticated account session transitions (logout, sign-in, or account switch).
 */
@Singleton
class AccountSessionCleanupCoordinator @Inject constructor(
    private val tokenManager: TokenManager,
    private val chatWebSocketClient: ChatWebSocketClient,
    private val genderSessionStore: GenderSessionStore,
    private val onboardingSessionStore: OnboardingSessionStore = OnboardingSessionStore(),
    private val liveSessionSnapshotCache: LiveSessionSnapshotCache,
    private val workoutSessionRepository: WorkoutSessionRepositoryImpl,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            var lastSession = tokenManager.currentSessionSnapshot()
            tokenManager.sessionState
                .distinctUntilChangedBy { it.generation to it.activeUserId }
                .collect { nextSession ->
                    if (nextSession.generation != lastSession.generation ||
                        nextSession.activeUserId != lastSession.activeUserId
                    ) {
                        onSessionChanged(previous = lastSession, next = nextSession)
                        lastSession = nextSession
                    }
                }
        }
    }

    internal fun onSessionChanged(previous: AccountSession, next: AccountSession) {
        if (!next.isAuthenticated || previous.activeUserId != next.activeUserId || next.generation != previous.generation) {
            chatWebSocketClient.disconnect()
        }
        cancelStaleHttpCalls(next)
        if (!next.isAuthenticated) {
            genderSessionStore.clear()
            onboardingSessionStore.clear()
        } else if (genderSessionStore.state.value?.userId != next.activeUserId) {
            genderSessionStore.clear()
            onboardingSessionStore.clear()
        }
        liveSessionSnapshotCache.clearForOtherUsers(next.activeUserId)
        workoutSessionRepository.clearConflictedUpdatesForOtherUsers(next.activeUserId)
    }

    private fun cancelStaleHttpCalls(activeSession: AccountSession) {
        val dispatcher = okHttpClient.dispatcher
        val allCalls = dispatcher.queuedCalls() + dispatcher.runningCalls()
        allCalls.forEach { call ->
            val tag = call.request().tag(AuthRequestSession::class.java) ?: return@forEach
            if (tag.userId != activeSession.activeUserId || tag.generation != activeSession.generation) {
                call.cancel()
            }
        }
    }
}
