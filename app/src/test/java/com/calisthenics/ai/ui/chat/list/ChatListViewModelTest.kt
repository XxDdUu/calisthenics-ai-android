package com.calisthenics.ai.ui.chat.list

import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.model.UserPresenceStatus
import com.calisthenics.ai.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadConversationsPopulatesStateOnSuccess() = runTest {
        val conversations = listOf(
            ChatConversation(
                id = 1L,
                otherUserId = 2L,
                otherUserName = "Coach Alex",
                otherUserAvatarUrl = null,
                lastMessagePreview = "Keep up the great work!",
                unreadCount = 2,
                lastMessageAt = "2026-10-26T10:00:00Z",
                createdAt = "2026-10-25T10:00:00Z",
                updatedAt = "2026-10-26T10:00:00Z"
            )
        )
        fakeRepository.conversationsResult = Result.success(conversations)

        val viewModel = ChatListViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.conversations.size)
        assertEquals(2L, state.totalUnreadCount)
        assertEquals("Coach Alex", state.conversations[0].otherUserName)
        assertTrue(fakeRepository.isWebSocketConnected)
    }

    @Test
    fun loadConversationsSetsErrorOnFailure() = runTest {
        fakeRepository.conversationsResult = Result.failure(RuntimeException("Network error"))

        val viewModel = ChatListViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
        assertTrue(state.conversations.isEmpty())
    }

    @Test
    fun incomingMessageRefreshesConversationList() = runTest {
        val initialConversations = listOf(
            ChatConversation(
                id = 1L,
                otherUserId = 2L,
                otherUserName = "Coach Alex",
                otherUserAvatarUrl = null,
                lastMessagePreview = "Old message",
                unreadCount = 0,
                lastMessageAt = "2026-10-26T09:00:00Z",
                createdAt = "2026-10-25T10:00:00Z",
                updatedAt = "2026-10-26T09:00:00Z"
            )
        )
        fakeRepository.conversationsResult = Result.success(initialConversations)

        val viewModel = ChatListViewModel(fakeRepository)
        advanceUntilIdle()
        assertEquals("Old message", viewModel.uiState.value.conversations[0].lastMessagePreview)

        // When a new message arrives via Flow
        val updatedConversations = listOf(
            ChatConversation(
                id = 1L,
                otherUserId = 2L,
                otherUserName = "Coach Alex",
                otherUserAvatarUrl = null,
                lastMessagePreview = "New real-time message",
                unreadCount = 1,
                lastMessageAt = "2026-10-26T10:30:00Z",
                createdAt = "2026-10-25T10:00:00Z",
                updatedAt = "2026-10-26T10:30:00Z"
            )
        )
        fakeRepository.conversationsResult = Result.success(updatedConversations)

        fakeRepository.emitIncomingMessage(
            ChatMessage(
                id = 99L,
                conversationId = 1L,
                senderId = 2L,
                senderName = "Coach Alex",
                senderAvatarUrl = null,
                content = "New real-time message",
                messageType = "TEXT",
                createdAt = "2026-10-26T10:30:00Z",
                isFromCurrentUser = false
            )
        )
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals("New real-time message", updatedState.conversations[0].lastMessagePreview)
        assertEquals(1L, updatedState.totalUnreadCount)
    }

    @Test
    fun presenceSnapshotAndRealtimeUpdatesAreVersionedInConversationList() = runTest {
        fakeRepository.presenceResult = Result.success(
            listOf(UserPresence(2L, UserPresenceStatus.ONLINE, version = 1L))
        )
        val viewModel = ChatListViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(UserPresenceStatus.ONLINE, viewModel.uiState.value.presences[2L]?.status)
        fakeRepository.emitPresenceUpdate(
            UserPresence(2L, UserPresenceStatus.OFFLINE, "2026-10-28T01:00:00Z", version = 2L)
        )
        fakeRepository.emitPresenceUpdate(
            UserPresence(2L, UserPresenceStatus.ONLINE, version = 1L)
        )
        advanceUntilIdle()

        assertEquals(UserPresenceStatus.OFFLINE, viewModel.uiState.value.presences[2L]?.status)
        assertEquals(2L, viewModel.uiState.value.presences[2L]?.version)
    }

    @Test
    fun startConversationCreatesAndNavigates() = runTest {
        fakeRepository.createConversationResult = Result.success(
            ChatConversation(
                id = 42L,
                otherUserId = 5L,
                otherUserName = "User 5",
                otherUserAvatarUrl = null,
                lastMessagePreview = null,
                unreadCount = 0,
                lastMessageAt = null,
                createdAt = "2026-10-26T10:00:00Z",
                updatedAt = "2026-10-26T10:00:00Z"
            )
        )

        val viewModel = ChatListViewModel(fakeRepository)
        advanceUntilIdle()

        var createdId: Long? = null
        viewModel.startConversation(5L) { id ->
            createdId = id
        }
        advanceUntilIdle()

        assertEquals(42L, createdId)
        assertFalse(viewModel.uiState.value.isNewChatDialogVisible)
    }

    private class FakeChatRepository : ChatRepository {
        var isWebSocketConnected = false
        var conversationsResult: Result<List<ChatConversation>> = Result.success(emptyList())
        var createConversationResult: Result<ChatConversation> = Result.failure(UnsupportedOperationException())
        var presenceResult: Result<List<UserPresence>> = Result.success(emptyList())
        private val incomingFlow = MutableSharedFlow<ChatMessage>()
        private val presenceFlow = MutableSharedFlow<UserPresence>()

        suspend fun emitIncomingMessage(msg: ChatMessage) {
            incomingFlow.emit(msg)
        }

        suspend fun emitPresenceUpdate(update: UserPresence) {
            presenceFlow.emit(update)
        }

        override suspend fun getConversations(): Result<List<ChatConversation>> = conversationsResult
        override suspend fun getConversationPresence(): Result<List<UserPresence>> = presenceResult
        override suspend fun createOrGetConversation(otherUserId: Long): Result<ChatConversation> = createConversationResult
        override suspend fun getMessages(conversationId: Long, page: Int, size: Int): Result<List<ChatMessage>> = Result.success(emptyList())
        override suspend fun sendMessage(conversationId: Long, content: String, clientMessageId: String?): Result<ChatMessage> = Result.failure(UnsupportedOperationException())
        override suspend fun markAsRead(conversationId: Long): Result<Unit> = Result.success(Unit)
        override suspend fun getUnreadCount(): Result<Long> = Result.success(0L)
        override suspend fun getAvailableContacts(): Result<List<ChatContact>> = Result.success(emptyList())
        override fun getIncomingMessages(): Flow<ChatMessage> = incomingFlow
        override fun getPresenceUpdates(): Flow<UserPresence> = presenceFlow
        override fun getWebSocketState(): StateFlow<WebSocketState> = MutableStateFlow(WebSocketState.CONNECTED)
        override fun connectWebSocket() { isWebSocketConnected = true }
        override fun disconnectWebSocket() { isWebSocketConnected = false }
        override fun subscribeToConversation(conversationId: Long) {}
        override fun unsubscribeFromConversation(conversationId: Long) {}
        override fun getCurrentUserId(): Long? = 1L
    }
}
