package com.calisthenics.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.calisthenics.ai.data.remote.api.AuthInterceptor
import com.calisthenics.ai.data.remote.api.TokenAuthenticator
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.websocket.ChatWebSocketClient
import com.calisthenics.ai.data.repository.WorkoutSessionRepositoryImpl
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.ui.liveanalysis.LiveSessionSnapshotCache
import com.calisthenics.ai.ui.workoutsession.timer.ExerciseTimerConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionIsolationTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveAuthToken prefers JWT sub claim over stale userId and switchToAccount bumps generation cleanly`() {
        val prefs = InMemorySharedPreferences()
        val tokenManager = createTokenManager(prefs)

        val jwtA = createJwt(userId = 10L)
        val jwtB = createJwt(userId = 20L)

        tokenManager.saveAuthToken(accessToken = jwtA, refreshToken = "ref-a", userId = 10L, role = "ROLE_USER")
        val sessionA = tokenManager.sessionState.value
        assertEquals(10L, sessionA.activeUserId)
        assertTrue(sessionA.generation >= 1L)

        // Same-user token refresh must keep generation stable
        val jwtARefreshed = createJwt(userId = 10L, suffix = "2")
        tokenManager.saveAuthToken(accessToken = jwtARefreshed, refreshToken = "ref-a2", userId = 10L, role = "ROLE_USER")
        assertEquals(sessionA.generation, tokenManager.sessionState.value.generation)

        // Logging in via JWT for B with null explicit userId must resolve B from JWT 'sub' instead of sticking to A (10L)
        tokenManager.saveAuthToken(accessToken = jwtB, refreshToken = "ref-b", userId = null, role = "ROLE_USER")
        val sessionB = tokenManager.sessionState.value
        assertEquals(20L, tokenManager.getUserId())
        assertEquals(20L, sessionB.activeUserId)
        assertTrue(sessionB.generation > sessionA.generation)

        // Switch back to A -> bumps generation again and restores A's token
        assertTrue(tokenManager.switchToAccount(10L))
        val backToA = tokenManager.sessionState.value
        assertEquals(10L, backToA.activeUserId)
        assertTrue(backToA.generation > sessionB.generation)
        assertEquals(jwtARefreshed, tokenManager.getAccessToken())

        // Scenario E: fresh TokenManager after app restart restores A cleanly, never B
        val restartedTokenManager = createTokenManager(prefs)
        assertEquals(10L, restartedTokenManager.getActiveUserId())
        assertEquals(jwtARefreshed, restartedTokenManager.getAccessToken())
        assertNotEquals(jwtB, restartedTokenManager.getAccessToken())
    }

    @Test
    fun `TokenAuthenticator ignores 401 from Account A after user switched to Account B`() {
        val prefs = InMemorySharedPreferences()
        val tokenManager = createTokenManager(prefs)
        val sessionManager = SessionManager()
        val jwtA = createJwt(userId = 1L)
        val jwtB = createJwt(userId = 2L)

        tokenManager.saveAuthToken(accessToken = jwtA, refreshToken = "ref-a", userId = 1L, role = "ROLE_USER")
        val tagForA = AuthRequestSession(userId = 1L, generation = tokenManager.currentSessionSnapshot().generation)

        tokenManager.saveAuthToken(accessToken = jwtB, refreshToken = "ref-b", userId = 2L, role = "ROLE_USER")

        val authenticator = spyk(TokenAuthenticator(tokenManager, sessionManager))
        val staleRequestFromA = Request.Builder()
            .url("https://api.test.local/api/v1/workout-plans")
            .header("Authorization", "Bearer $jwtA")
            .tag(AuthRequestSession::class.java, tagForA)
            .build()
        val staleResponse = Response.Builder()
            .request(staleRequestFromA)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        val retry = authenticator.authenticate(null, staleResponse)

        // Must neither retry with B's token nor wipe B's active session
        assertNull(retry)
        assertEquals(2L, tokenManager.getUserId())
        assertEquals(jwtB, tokenManager.getAccessToken())
        verify(exactly = 0) { authenticator.performRefresh(any()) }
    }

    @Test
    fun `AuthInterceptor tags requests with active user session and selects owner token`() {
        val prefs = InMemorySharedPreferences()
        val tokenManager = createTokenManager(prefs)
        val jwtA = createJwt(userId = 11L)
        val jwtB = createJwt(userId = 22L)
        tokenManager.saveAuthToken(jwtA, "ref-a", userId = 11L, role = "ROLE_USER")
        tokenManager.saveAuthToken(jwtB, "ref-b", userId = 22L, role = "ROLE_USER")

        val interceptor = AuthInterceptor(tokenManager)
        var capturedRequest: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns Request.Builder().url("https://api.test.local/api/v1/chat/conversations").build()
        every { chain.proceed(any()) } answers {
            capturedRequest = firstArg()
            Response.Builder()
                .request(capturedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("[]".toResponseBody("application/json".toMediaType()))
                .build()
        }

        interceptor.intercept(chain)

        assertEquals("Bearer $jwtB", capturedRequest?.header("Authorization"))
        val sessionTag = capturedRequest?.tag(AuthRequestSession::class.java)
        assertNotNull(sessionTag)
        assertEquals(22L, sessionTag?.userId)
        assertEquals(tokenManager.currentSessionSnapshot().generation, sessionTag?.generation)
    }

    @Test
    fun `AccountSessionCleanupCoordinator cancels stale calls, disconnects WebSocket, and isolates caches on switch`() = runTest {
        val prefs = InMemorySharedPreferences()
        val tokenManager = createTokenManager(prefs)
        val sessionManager = AccountSessionManager(tokenManager)
        val genderStore = GenderSessionStore()
        val snapshotCache = LiveSessionSnapshotCache(sessionManager)
        val chatWs = mockk<ChatWebSocketClient>(relaxed = true)
        val sessionRepo = mockk<WorkoutSessionRepositoryImpl>(relaxed = true)

        val staleCallA = mockk<Call>(relaxed = true)
        val activeCallB = mockk<Call>(relaxed = true)
        val httpDispatcher = mockk<Dispatcher>(relaxed = true)
        every { httpDispatcher.runningCalls() } returns emptyList()
        every { httpDispatcher.queuedCalls() } returns emptyList()
        val okHttp = mockk<OkHttpClient>(relaxed = true)
        every { okHttp.dispatcher } returns httpDispatcher

        val coordinator = AccountSessionCleanupCoordinator(
            tokenManager = tokenManager,
            chatWebSocketClient = chatWs,
            genderSessionStore = genderStore,
            onboardingSessionStore = com.calisthenics.ai.data.local.OnboardingSessionStore(),
            liveSessionSnapshotCache = snapshotCache,
            workoutSessionRepository = sessionRepo,
            okHttpClient = okHttp
        )
        coordinator.start()
        advanceUntilIdle()

        tokenManager.saveAuthToken(createJwt(1L), "ref-1", userId = 1L, role = "ROLE_USER")
        advanceUntilIdle()
        val gen1 = tokenManager.currentSessionSnapshot().generation

        every { staleCallA.request() } returns Request.Builder()
            .url("https://api.test.local/api/v1/chat/conversations")
            .tag(AuthRequestSession::class.java, AuthRequestSession(userId = 1L, generation = gen1))
            .build()
        every { activeCallB.request() } returns Request.Builder()
            .url("https://api.test.local/api/v1/workout-plans")
            .tag(AuthRequestSession::class.java, AuthRequestSession(userId = 2L, generation = gen1 + 1L))
            .build()
        every { httpDispatcher.runningCalls() } returns listOf(staleCallA, activeCallB)

        genderStore.publish(1L, Gender.MALE)
        snapshotCache.put(LiveSessionRecord(id = "session-a", userId = 1L, exerciseType = "PUSH_UP"), ownerUserId = 1L)
        assertNotNull(snapshotCache.get("session-a", ownerUserId = 1L))

        // Switch to User 2
        tokenManager.saveAuthToken(createJwt(2L), "ref-2", userId = 2L, role = "ROLE_USER")
        advanceUntilIdle()

        verify { chatWs.disconnect() }
        verify { staleCallA.cancel() }
        verify(exactly = 0) { activeCallB.cancel() }
        assertNull(genderStore.state.value)
        assertNull(snapshotCache.get("session-a", ownerUserId = 2L))
        assertNull(snapshotCache.get("session-a", ownerUserId = 1L))
        verify { sessionRepo.clearConflictedUpdatesForOtherUsers(2L) }
    }

    @Test
    fun `WorkoutTimerPreferences isolates per-user timer configurations`() {
        val prefs = InMemorySharedPreferences()
        val context = mockContext(prefs)
        val tokenManager = createTokenManager(prefs)
        val timerPrefs = WorkoutTimerPreferences(context, tokenManager)

        tokenManager.saveAuthToken(createJwt(1L), "ref-1", userId = 1L, role = "ROLE_USER")
        timerPrefs.saveTimerConfig(
            "Planche Lean",
            ExerciseTimerConfig(countdownEnabled = false, countdownDurationSeconds = 5, restTimerEnabled = true, restDurationSeconds = 180)
        )

        tokenManager.saveAuthToken(createJwt(2L), "ref-2", userId = 2L, role = "ROLE_USER")
        val configForUser2 = timerPrefs.getTimerConfig("Planche Lean", fallbackRestSeconds = 90)
        assertTrue(configForUser2.countdownEnabled)
        assertEquals(90, configForUser2.restDurationSeconds)

        tokenManager.switchToAccount(1L)
        val configForUser1 = timerPrefs.getTimerConfig("Planche Lean", fallbackRestSeconds = 90)
        assertFalse(configForUser1.countdownEnabled)
        assertEquals(180, configForUser1.restDurationSeconds)
    }

    private fun createTokenManager(prefs: InMemorySharedPreferences): TokenManager {
        val context = mockContext(prefs)
        return TokenManager(context)
    }

    private fun mockContext(prefs: SharedPreferences): Context {
        return mockk(relaxed = true) {
            every { getSharedPreferences(any(), any()) } returns prefs
            every { applicationContext } returns this
        }
    }

    private fun createJwt(userId: Long, suffix: String = "1"): String {
        val futureExp = (System.currentTimeMillis() / 1000) + 3600
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payloadJson = "{\"sub\":\"$userId\",\"exp\":$futureExp,\"role\":\"ROLE_USER\"}"
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray(StandardCharsets.UTF_8))
        return "$header.$payload.sig$suffix"
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val map = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
        override fun getString(key: String?, defValue: String?): String? =
            if (key != null && map.containsKey(key)) map[key] as? String else defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = key != null && map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class EditorImpl(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val staged = linkedMapOf<String, Any?>()
            private var clearRequested = false
            private val removals = linkedSetOf<String>()

            override fun putString(key: String?, value: String?) = apply { if (key != null) staged[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) staged[key] = values?.toSet() }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) staged[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) staged[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) staged[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) staged[key] = value }
            override fun remove(key: String?) = apply { if (key != null) { removals.add(key); staged.remove(key) } }
            override fun clear() = apply { clearRequested = true; staged.clear(); removals.clear() }
            override fun commit(): Boolean { applyChanges(); return true }
            override fun apply() { applyChanges() }
            private fun applyChanges() {
                if (clearRequested) target.clear()
                removals.forEach { target.remove(it) }
                target.putAll(staged)
            }
        }
    }
}
