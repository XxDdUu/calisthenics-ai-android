package com.calisthenics.ai.ui.accountswitch

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.local.TodayWorkoutPreferences
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.domain.model.TodayWorkoutCandidate
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.domain.usecase.CreateWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.DeleteWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlansUseCase
import com.calisthenics.ai.ui.chat.conversation.ChatConversationViewModel
import com.calisthenics.ai.ui.chat.list.ChatListViewModel
import com.calisthenics.ai.ui.coach.CoachConnectionsViewModel
import com.calisthenics.ai.ui.media.MediaUploadManager
import com.calisthenics.ai.ui.profile.ProfileViewModel
import com.calisthenics.ai.ui.today.TodayWorkoutKind
import com.calisthenics.ai.ui.today.TodayWorkoutViewModel
import com.calisthenics.ai.ui.workoutplan.list.WorkoutPlanListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSwitchIsolationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefs: TestPrefs
    private lateinit var tokenManager: TokenManager
    private lateinit var sessionManager: AccountSessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        prefs = TestPrefs()
        val context = mockk<Context>(relaxed = true) {
            every { getSharedPreferences(any(), any()) } returns prefs
            every { applicationContext } returns this
        }
        tokenManager = TokenManager(context)
        sessionManager = AccountSessionManager(tokenManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Scenario A - WorkoutPlanListViewModel and TodayWorkoutViewModel clear A data immediately and auto-reload B without Reload press`() = runTest {
        tokenManager.saveAuthToken(jwtFor(1L), "ref-a", userId = 1L, role = "ROLE_USER")
        tokenManager.saveAuthToken(jwtFor(2L), "ref-b", userId = 2L, role = "ROLE_USER")
        assertTrue(tokenManager.switchToAccount(1L))

        val planA = samplePlan(id = 101L, name = "Account A Push Pull Legs")
        val planB = samplePlan(id = 202L, name = "Account B Planche Focus")

        val planRepo = mockk<WorkoutPlanRepository>(relaxed = true)
        val todayPrefs = mockk<TodayWorkoutPreferences> {
            every { priority } returns flowOf(WorkoutSource.COACH)
        }
        val sessionRepo = mockk<WorkoutSessionRepository>(relaxed = true) {
            coEvery { getActiveSessions() } returns Result.success(emptyList())
            coEvery { getUpcomingSessions() } returns Result.success(emptyList())
        }
        val connectivityMonitor = mockk<ConnectivityMonitor>(relaxed = true) {
            every { isConnected() } returns true
            every { isOnline } returns MutableStateFlow(true)
        }
        val coachClientRepo = mockk<CoachClientRepository>(relaxed = true) {
            coEvery { loadRelationships() } returns Result.success(emptyList())
        }

        val gateB = CompletableDeferred<List<UserWorkoutPlan>>()
        coEvery { planRepo.getPlans() } coAnswers {
            when (sessionManager.currentUserId()) {
                1L -> Result.success(listOf(planA))
                2L -> Result.success(gateB.await())
                else -> Result.success(emptyList())
            }
        }
        coEvery { planRepo.getMyAssignments() } coAnswers { Result.success(emptyList()) }
        coEvery { planRepo.getMyCoachAssignments() } coAnswers { Result.success(emptyList()) }
        coEvery { planRepo.getActivePlanId() } coAnswers {
            Result.success(if (sessionManager.currentUserId() == 1L) 101L else 202L)
        }
        coEvery { planRepo.resolveTodayWorkout(any(), any()) } coAnswers {
            val plan = if (sessionManager.currentUserId() == 1L) planA else planB
            Result.success(
                TodayWorkoutResolution(
                    workoutDate = "2026-11-10",
                    activeCoach = false,
                    source = WorkoutSource.PERSONAL,
                    selectionRequired = false,
                    selectedPlanId = plan.id,
                    candidates = listOf(TodayWorkoutCandidate(plan = plan, assignmentId = null, coachName = null))
                )
            )
        }
        coEvery { planRepo.getPlanById(101L) } returns Result.success(planA)
        coEvery { planRepo.getPlanById(202L) } returns Result.success(planB)

        val listViewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = GetWorkoutPlansUseCase(planRepo),
            createWorkoutPlanUseCase = CreateWorkoutPlanUseCase(planRepo),
            deleteWorkoutPlanUseCase = DeleteWorkoutPlanUseCase(planRepo),
            connectivityMonitor = connectivityMonitor,
            coachClientRepository = coachClientRepo,
            tokenManager = tokenManager,
            accountSessionManager = sessionManager
        )
        val todayViewModel = TodayWorkoutViewModel(
            getWorkoutPlanDetailsUseCase = GetWorkoutPlanDetailsUseCase(planRepo),
            workoutPlanRepository = planRepo,
            workoutSessionRepository = sessionRepo,
            connectivityMonitor = connectivityMonitor,
            todayWorkoutPreferences = todayPrefs,
            accountSessionManager = sessionManager
        )
        advanceUntilIdle()

        assertEquals(listOf("Account A Push Pull Legs"), listViewModel.uiState.value.plans.map { it.name })
        assertEquals("Account A Push Pull Legs", todayViewModel.uiState.value.selection.plan?.name)

        // Switch Account 1 -> 2 without pressing Reload
        assertTrue(tokenManager.switchToAccount(2L))
        advanceUntilIdle()

        // While B's plans call is still suspended on gateB, A's plans MUST ALREADY BE GONE
        assertTrue(listViewModel.uiState.value.plans.isEmpty())
        assertEquals("Account B Planche Focus", todayViewModel.uiState.value.selection.plan?.name)

        // Complete B's plans load automatically -> B's plan renders with zero manual Reload
        gateB.complete(listOf(planB))
        advanceUntilIdle()
        assertEquals(listOf("Account B Planche Focus"), listViewModel.uiState.value.plans.map { it.name })
    }

    @Test
    fun `Scenario B - ChatListViewModel and ChatConversationViewModel clear A messages, cursor, and draft and auto-load B`() = runTest {
        tokenManager.saveAuthToken(jwtFor(1L), "ref-a", userId = 1L, role = "ROLE_USER")
        tokenManager.saveAuthToken(jwtFor(2L), "ref-b", userId = 2L, role = "ROLE_USER")
        assertTrue(tokenManager.switchToAccount(1L))

        val incomingFlow = MutableSharedFlow<ChatMessage>()
        val wsState = MutableStateFlow(WebSocketState.CONNECTED)
        val unsubscribedIds = mutableListOf<Long>()

        val chatRepo = object : ChatRepository {
            override suspend fun getConversations(): Result<List<ChatConversation>> {
                return when (sessionManager.currentUserId()) {
                    1L -> Result.success(
                        listOf(
                            ChatConversation(
                                id = 11L,
                                otherUserId = 99L,
                                otherUserName = "Coach For A",
                                otherUserAvatarUrl = null,
                                lastMessagePreview = "Account A secret message",
                                unreadCount = 4,
                                lastMessageAt = "2026-11-10T10:00:00Z",
                                createdAt = "2026-11-10T09:00:00Z",
                                updatedAt = "2026-11-10T10:00:00Z"
                            )
                        )
                    )
                    2L -> Result.success(
                        listOf(
                            ChatConversation(
                                id = 22L,
                                otherUserId = 88L,
                                otherUserName = "Athlete For B",
                                otherUserAvatarUrl = null,
                                lastMessagePreview = "Hello Account B",
                                unreadCount = 1,
                                lastMessageAt = "2026-11-10T11:00:00Z",
                                createdAt = "2026-11-10T09:00:00Z",
                                updatedAt = "2026-11-10T11:00:00Z"
                            )
                        )
                    )
                    else -> Result.success(emptyList())
                }
            }

            override suspend fun getConversationPresence(): Result<List<UserPresence>> {
                val presence = when (sessionManager.currentUserId()) {
                    1L -> listOf(UserPresence(99L, com.calisthenics.ai.domain.model.UserPresenceStatus.ONLINE, version = 1L))
                    2L -> listOf(UserPresence(88L, com.calisthenics.ai.domain.model.UserPresenceStatus.ONLINE, version = 1L))
                    else -> emptyList()
                }
                return Result.success(presence)
            }
            override suspend fun createOrGetConversation(otherUserId: Long) = Result.failure<ChatConversation>(UnsupportedOperationException())
            override suspend fun getMessages(conversationId: Long, page: Int, size: Int): Result<List<ChatMessage>> {
                return if (sessionManager.currentUserId() == 1L && conversationId == 11L) {
                    Result.success(
                        listOf(
                            ChatMessage(
                                id = 501L,
                                conversationId = 11L,
                                senderId = 99L,
                                senderName = "Coach For A",
                                senderAvatarUrl = null,
                                content = "Private to A",
                                messageType = "TEXT",
                                createdAt = "2026-11-10T10:00:00Z",
                                isFromCurrentUser = false
                            )
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Forbidden"))
                }
            }
            override suspend fun sendMessage(conversationId: Long, content: String, clientMessageId: String?) = Result.failure<ChatMessage>(UnsupportedOperationException())
            override suspend fun markAsRead(conversationId: Long) = Result.success(Unit)
            override suspend fun getUnreadCount() = Result.success(0L)
            override suspend fun getAvailableContacts() = Result.success(emptyList<ChatContact>())
            override fun getIncomingMessages(): Flow<ChatMessage> = incomingFlow
            override fun getPresenceUpdates(): Flow<UserPresence> = MutableSharedFlow()
            override fun getWebSocketState(): StateFlow<WebSocketState> = wsState
            override fun connectWebSocket() {}
            override fun disconnectWebSocket() {}
            override fun subscribeToConversation(conversationId: Long) {}
            override fun unsubscribeFromConversation(conversationId: Long) { unsubscribedIds.add(conversationId) }
            override fun getCurrentUserId(): Long? = sessionManager.currentUserId()
        }

        val chatListVm = ChatListViewModel(chatRepo, sessionManager)
        val conversationVm = ChatConversationViewModel(
            chatRepository = chatRepo,
            savedStateHandle = SavedStateHandle(mapOf("conversationId" to 11L, "otherUserId" to 99L, "otherUserName" to "Coach For A")),
            accountSessionManager = sessionManager
        )
        conversationVm.updateInputText("Draft for A")
        advanceUntilIdle()

        assertEquals(listOf("Coach For A"), chatListVm.uiState.value.conversations.map { it.otherUserName })
        assertEquals(4L, chatListVm.uiState.value.totalUnreadCount)
        assertEquals(listOf("Private to A"), conversationVm.uiState.value.messages.map { it.content })

        // Switch Account 1 -> 2
        assertTrue(tokenManager.switchToAccount(2L))
        advanceUntilIdle()

        // ChatListViewModel automatically switched to B's conversation & unread count without manual Reload
        assertEquals(listOf("Athlete For B"), chatListVm.uiState.value.conversations.map { it.otherUserName })
        assertEquals(1L, chatListVm.uiState.value.totalUnreadCount)
        assertFalse(chatListVm.uiState.value.presences.containsKey(99L))
        assertTrue(chatListVm.uiState.value.presences.containsKey(88L))

        // ChatConversationViewModel from Account A is immediately wiped, unsubscribed, and ignores late WS events
        assertTrue(conversationVm.uiState.value.messages.isEmpty())
        assertEquals(0L, conversationVm.uiState.value.conversationId)
        assertEquals("", conversationVm.uiState.value.inputText)
        assertEquals(0, conversationVm.uiState.value.currentPage)
        assertFalse(conversationVm.uiState.value.hasMoreOlderMessages)
        assertTrue(11L in unsubscribedIds)

        incomingFlow.emit(
            ChatMessage(
                id = 502L,
                conversationId = 11L,
                senderId = 99L,
                senderName = "Coach For A",
                senderAvatarUrl = null,
                content = "Late message to A",
                messageType = "TEXT",
                createdAt = "2026-11-10T11:05:00Z",
                isFromCurrentUser = false
            )
        )
        advanceUntilIdle()
        assertTrue(conversationVm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `Scenario C - delayed response from Account A never overwrites Account B across TodayWorkout, Connections, and ProfileViewModel`() = runTest {
        tokenManager.saveAuthToken(jwtFor(1L), "ref-a", userId = 1L, role = "ROLE_USER")
        tokenManager.saveAuthToken(jwtFor(2L), "ref-b", userId = 2L, role = "ROLE_USER")
        assertTrue(tokenManager.switchToAccount(1L))

        val delayedResolutionA = CompletableDeferred<TodayWorkoutResolution>()
        val delayedOverviewA = CompletableDeferred<CoachConnectionOverview>()
        val delayedProfileA = CompletableDeferred<ClientProfile>()

        val planA = samplePlan(id = 101L, name = "Slow Account A Plan")
        val planB = samplePlan(id = 202L, name = "Fast Account B Plan")

        val planRepo = mockk<WorkoutPlanRepository>(relaxed = true) {
            coEvery { getActivePlanId() } returns Result.success(null)
            coEvery { getPlanById(202L) } returns Result.success(planB)
            coEvery { getPlanById(101L) } returns Result.success(planA)
            coEvery { resolveTodayWorkout(any(), any()) } coAnswers {
                when (sessionManager.currentUserId()) {
                    1L -> Result.success(delayedResolutionA.await())
                    else -> Result.success(
                        TodayWorkoutResolution(
                            workoutDate = "2026-11-10",
                            activeCoach = false,
                            source = WorkoutSource.PERSONAL,
                            selectionRequired = false,
                            selectedPlanId = 202L,
                            candidates = listOf(TodayWorkoutCandidate(planB, assignmentId = null, coachName = null))
                        )
                    )
                }
            }
        }
        val sessionRepo = mockk<WorkoutSessionRepository>(relaxed = true) {
            coEvery { getActiveSessions() } returns Result.success(emptyList())
            coEvery { getUpcomingSessions() } returns Result.success(emptyList())
        }
        val connectivityMonitor = mockk<ConnectivityMonitor>(relaxed = true) {
            every { isConnected() } returns true
            every { isOnline } returns MutableStateFlow(true)
        }
        val todayPrefs = mockk<TodayWorkoutPreferences> {
            every { priority } returns flowOf(WorkoutSource.PERSONAL)
        }

        val coachRepo = mockk<CoachClientRepository>(relaxed = true) {
            coEvery { loadOverview() } coAnswers {
                when (sessionManager.currentUserId()) {
                    1L -> Result.success(delayedOverviewA.await())
                    else -> Result.success(CoachConnectionOverview(currentUserId = 2L, coaches = emptyList(), relationships = emptyList()))
                }
            }
        }
        val chatRepo = mockk<ChatRepository>(relaxed = true)
        val analyticsRepo = mockk<com.calisthenics.ai.domain.repository.AnalyticsRepository>(relaxed = true) {
            coEvery { getClientFeedbacks() } returns Result.success(emptyList())
            coEvery { getClientSharedPRs() } returns Result.success(emptyList())
            coEvery { getCoachSharedPRs(any()) } returns Result.success(emptyList())
        }

        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { getUserId() } answers { sessionManager.currentUserId() }
        }
        val profileRepo = mockk<ProfileRepository>(relaxed = true) {
            every { profileUpdateEvents } returns kotlinx.coroutines.flow.MutableSharedFlow()
            coEvery { getMyClientProfile() } coAnswers {
                when (sessionManager.currentUserId()) {
                    1L -> Result.success(delayedProfileA.await())
                    else -> Result.success(ClientProfile(id = 900L, userId = 2L, fullName = "User Two", displayName = "Two"))
                }
            }
            coEvery { getMyCoachProfile() } returns Result.failure(NoSuchElementException())
            coEvery { getProfileMedia(any(), any()) } returns Result.success(emptyList<ProfileMediaItem>())
        }

        val todayVm = TodayWorkoutViewModel(
            GetWorkoutPlanDetailsUseCase(planRepo),
            planRepo,
            sessionRepo,
            connectivityMonitor,
            todayPrefs,
            sessionManager
        )
        val connectionsVm = CoachConnectionsViewModel(
            coachRepo,
            chatRepo,
            analyticsRepo,
            profileRepo,
            sessionManager
        )
        val profileVm = ProfileViewModel(
            authRepository = authRepo,
            profileRepository = profileRepo,
            coachClientRepository = coachRepo,
            chatRepository = chatRepo,
            mediaUploadManager = mockk(relaxed = true),
            genderSessionStore = GenderSessionStore(),
            accountSessionManager = sessionManager
        )

        advanceUntilIdle() // A's slow calls are all suspended in flight!

        // Switch rapidly to Account B before A finishes
        assertTrue(tokenManager.switchToAccount(2L))
        advanceUntilIdle()

        assertEquals("Fast Account B Plan", todayVm.uiState.value.selection.plan?.name)
        assertEquals(2L, connectionsVm.uiState.value.currentUserId)
        assertEquals(2L, profileVm.uiState.value.clientProfile?.userId)
        verify { authRepo.updateAccountProfile(userId = 2L, fullName = "Two", avatarUrl = null) }

        // Delayed Account A responses finish AFTER B is rendered
        delayedResolutionA.complete(
            TodayWorkoutResolution(
                workoutDate = "2026-11-10",
                activeCoach = false,
                source = WorkoutSource.PERSONAL,
                selectionRequired = false,
                selectedPlanId = 101L,
                candidates = listOf(TodayWorkoutCandidate(planA, assignmentId = null, coachName = null))
            )
        )
        delayedOverviewA.complete(
            CoachConnectionOverview(
                currentUserId = 1L,
                coaches = listOf(CoachProfile(id = 55L, userId = 55L, fullName = "Stale Coach A")),
                relationships = emptyList()
            )
        )
        delayedProfileA.complete(
            ClientProfile(id = 800L, userId = 1L, fullName = "User One", displayName = "One")
        )
        advanceUntilIdle()

        // B's UI state must stay completely clean
        assertEquals("Fast Account B Plan", todayVm.uiState.value.selection.plan?.name)
        assertEquals(2L, connectionsVm.uiState.value.currentUserId)
        assertTrue(connectionsVm.uiState.value.coaches.isEmpty())
        assertEquals(2L, profileVm.uiState.value.clientProfile?.userId)
        assertEquals("Two", profileVm.uiState.value.clientProfile?.displayName)
        coVerify(exactly = 0) { authRepo.updateAccountProfile(userId = 800L, fullName = any(), avatarUrl = any()) }
    }

    private fun samplePlan(id: Long, name: String) = UserWorkoutPlan(
        id = id,
        name = name,
        splitType = WorkoutSplitType.CUSTOM,
        createdAt = "2026-11-10T08:00:00Z",
        updatedAt = "2026-11-10T08:00:00Z",
        isActive = true,
        days = listOf(
            UserWorkoutPlanDay(
                id = id * 10,
                dayOfWeek = WorkoutDayOfWeek.TUESDAY,
                sessionType = WorkoutSessionType.PUSH,
                name = "$name Day",
                exercises = emptyList()
            )
        )
    )

    private fun jwtFor(userId: Long): String {
        val futureExp = (System.currentTimeMillis() / 1000) + 3600
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"sub\":\"$userId\",\"exp\":$futureExp}".toByteArray(StandardCharsets.UTF_8))
        return "hdr.$payload.sig"
    }

    private class TestPrefs : SharedPreferences {
        private val map = linkedMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
        override fun getString(key: String?, defValue: String?): String? = if (key != null && map.containsKey(key)) map[key] as? String else defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = key != null && map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val staged = linkedMapOf<String, Any?>()
            private var clearReq = false
            private val removals = linkedSetOf<String>()
            override fun putString(key: String?, value: String?) = apply { if (key != null) staged[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) staged[key] = values?.toSet() }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) staged[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) staged[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) staged[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) staged[key] = value }
            override fun remove(key: String?) = apply { if (key != null) removals.add(key) }
            override fun clear() = apply { clearReq = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearReq) map.clear()
                removals.forEach { map.remove(it) }
                map.putAll(staged)
            }
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }
}
