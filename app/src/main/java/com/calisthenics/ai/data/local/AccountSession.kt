package com.calisthenics.ai.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of the active authenticated account session.
 * [generation] increments monotonically on every account switch, logout, or sign-in transition
 * so async callers can cancel or discard work belonging to an earlier session.
 */
data class AccountSession(
    val activeUserId: Long? = null,
    val role: String? = null,
    val generation: Long = 0L
) {
    val isAuthenticated: Boolean
        get() = activeUserId != null && activeUserId > 0L
}

/**
 * Tag attached to authenticated OkHttp requests so [com.calisthenics.ai.data.remote.api.TokenAuthenticator]
 * and [AccountSessionCleanupCoordinator] can identify which session owns an in-flight call.
 */
data class AuthRequestSession(
    val userId: Long?,
    val generation: Long
)

/**
 * Reactive session source shared by repositories, interceptors, and ViewModels.
 */
@Singleton
open class AccountSessionManager private constructor(
    private val tokenManager: TokenManager?,
    @Suppress("UNUSED_PARAMETER") managed: Boolean
) {
    @Inject
    constructor(tokenManager: TokenManager) : this(tokenManager, true)

    constructor() : this(null, false)

    private val fallbackState = MutableStateFlow(AccountSession())

    open val sessionState: StateFlow<AccountSession>
        get() = tokenManager?.sessionState ?: fallbackState.asStateFlow()

    open fun currentSession(): AccountSession {
        val state = tokenManager?.currentSessionSnapshot() ?: fallbackState.value
        val activeUserId = state.activeUserId?.takeIf { it > 0L }
            ?: tokenManager?.getUserId()?.takeIf { it > 0L }
        return if (activeUserId != null && state.activeUserId?.takeIf { it > 0L } == null) {
            state.copy(activeUserId = activeUserId, role = tokenManager?.getUserRole()?.takeIf { it.isNotBlank() })
        } else {
            state.copy(activeUserId = activeUserId)
        }
    }

    open fun currentUserId(): Long? = currentSession().activeUserId?.takeIf { it > 0L }

    open fun isCurrentSession(snapshot: AccountSession): Boolean {
        val current = currentSession()
        return current.generation == snapshot.generation && current.activeUserId == snapshot.activeUserId
    }

    /**
     * Test/preview helper when [AccountSessionManager] is constructed without a [TokenManager].
     */
    fun updateTestSession(activeUserId: Long?, role: String? = null) {
        val previous = fallbackState.value
        val normalizedId = activeUserId?.takeIf { it > 0L }
        val nextGeneration = if (previous.activeUserId != normalizedId || previous.role != role) {
            previous.generation + 1L
        } else {
            previous.generation
        }
        fallbackState.value = AccountSession(
            activeUserId = normalizedId,
            role = if (normalizedId != null) role else null,
            generation = nextGeneration
        )
    }
}

/**
 * ViewModel helper that binds user-scoped coroutines to a replaceable child [SupervisorJob].
 * When the active account changes, all pending coroutines for the previous account are cancelled
 * immediately, [onSessionCleared] resets UI state synchronously, and [onSessionActive] reloads
 * data for the newly active account.
 */
class AccountSessionGuard(
    private val parentScope: CoroutineScope,
    private val sessionManager: AccountSessionManager?,
    private val onSessionCleared: () -> Unit = {},
    private val onSessionActive: (AccountSession) -> Unit = {}
) {
    private var sessionJob: Job = SupervisorJob(parentScope.coroutineContext[Job])
    private var initialSession: AccountSession = sessionManager?.currentSession() ?: AccountSession()
    private var hasStartedObserving = false

    fun currentSession(): AccountSession = sessionManager?.currentSession() ?: initialSession

    fun isCurrentSession(snapshot: AccountSession): Boolean {
        val manager = sessionManager ?: return true
        return manager.isCurrentSession(snapshot)
    }

    fun startObserving() {
        val manager = sessionManager ?: return
        if (hasStartedObserving) return
        hasStartedObserving = true

        parentScope.launch {
            var lastGeneration = initialSession.generation
            var lastUserId = initialSession.activeUserId
            manager.sessionState
                .distinctUntilChangedBy { it.generation to it.activeUserId }
                .collectLatest { session ->
                    if (session.generation == lastGeneration && session.activeUserId == lastUserId) {
                        return@collectLatest
                    }
                    lastGeneration = session.generation
                    lastUserId = session.activeUserId

                    sessionJob.cancel()
                    sessionJob = SupervisorJob(parentScope.coroutineContext[Job])
                    onSessionCleared()
                    if (session.isAuthenticated) {
                        onSessionActive(session)
                    }
                }
        }
    }

    fun cancelActiveSessionWork() {
        sessionJob.cancel()
        sessionJob = SupervisorJob(parentScope.coroutineContext[Job])
    }

    fun launchScoped(block: suspend CoroutineScope.(AccountSession) -> Unit): Job {
        val capturedSession = currentSession()
        return parentScope.launch(sessionJob) {
            if (!isCurrentSession(capturedSession)) return@launch
            block(capturedSession)
        }
    }
}
