package com.calisthenics.ai.ui.chat.conversation

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.MessageDeliveryStatus
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatConversationViewModelTest {

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

    private fun createViewModel(conversationId: Long = 10L): ChatConversationViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "conversationId" to conversationId,
                "otherUserName" to "Coach Alex",
                "otherUserAvatarUrl" to "https://example.com/avatar.jpg",
                "otherUserId" to 2L
            )
        )
        return ChatConversationViewModel(fakeRepository, savedStateHandle)
    }

    @Test
    fun loadInitialMessagesPopulatesStateAndMarksAsRead() = runTest {
        val msg1 = ChatMessage(
            id = 1L,
            conversationId = 10L,
            senderId = 2L,
            senderName = "Coach Alex",
            senderAvatarUrl = null,
            content = "How was your workout?",
            messageType = "TEXT",
            createdAt = "2026-10-26T10:00:00Z",
            isFromCurrentUser = false
        )
        fakeRepository.messagesMap[10L] = mutableListOf(msg1)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.messages.size)
        assertEquals("How was your workout?", state.messages[0].content)
        assertTrue(state.shouldScrollToBottom)
        assertEquals(1, fakeRepository.markAsReadCalls)
    }

    @Test
    fun sendMessageSuccessClearsInputAndAppendsMessage() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateInputText("Felt great, completed all sets!")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText) // Input cleared on success
        assertFalse(state.isSending)
        assertEquals(1, state.messages.size)
        assertEquals("Felt great, completed all sets!", state.messages[0].content)
        assertTrue(state.shouldScrollToBottom)
    }

    @Test
    fun sendMessageFailureUpdatesDeliveryStatusToFailedAndShowsError() = runTest {
        fakeRepository.sendShouldFail = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateInputText("This will fail")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText) // Input is cleared upon dispatch
        assertFalse(state.isSending)
        assertEquals("Failed to send message", state.errorMessage)
        assertEquals(1, state.messages.size)
        assertEquals(MessageDeliveryStatus.FAILED, state.messages[0].deliveryStatus)
    }

    @Test
    fun retryMessageResendsFailedMessageAndMarksSentOnSuccess() = runTest {
        fakeRepository.sendShouldFail = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateInputText("Message to retry")
        viewModel.sendMessage()
        advanceUntilIdle()

        val failedMessage = viewModel.uiState.value.messages[0]
        assertEquals(MessageDeliveryStatus.FAILED, failedMessage.deliveryStatus)
        val clientMessageId = failedMessage.clientMessageId!!

        // Now fix network condition and retry
        fakeRepository.sendShouldFail = false
        viewModel.retryMessage(clientMessageId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals(MessageDeliveryStatus.SENT, state.messages[0].deliveryStatus)
        assertEquals("Message to retry", state.messages[0].content)
    }

    @Test
    fun incomingMessageForCurrentConversationAppendsAndMarksRead() = runTest {
        val viewModel = createViewModel(conversationId = 10L)
        advanceUntilIdle()
        fakeRepository.markAsReadCalls = 0

        val incoming = ChatMessage(
            id = 55L,
            conversationId = 10L,
            senderId = 2L,
            senderName = "Coach Alex",
            senderAvatarUrl = null,
            content = "Great job today!",
            messageType = "TEXT",
            createdAt = "2026-10-26T11:00:00Z",
            isFromCurrentUser = false
        )

        fakeRepository.emitIncomingMessage(incoming)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Great job today!", state.messages[0].content)
        assertTrue(state.shouldScrollToBottom)
        assertEquals(1, fakeRepository.markAsReadCalls)
    }

    @Test
    fun incomingMessageForDifferentConversationIsIgnored() = runTest {
        val viewModel = createViewModel(conversationId = 10L)
        advanceUntilIdle()

        // Message intended for conversation 99, not 10
        val foreignMessage = ChatMessage(
            id = 77L,
            conversationId = 99L,
            senderId = 3L,
            senderName = "Another User",
            senderAvatarUrl = null,
            content = "Wrong conversation message",
            messageType = "TEXT",
            createdAt = "2026-10-26T11:00:00Z",
            isFromCurrentUser = false
        )

        fakeRepository.emitIncomingMessage(foreignMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.messages.size) // Must not leak into this conversation!
    }

    @Test
    fun recipientPresenceUpdatesInRealtimeAndIgnoresOlderEvents() = runTest {
        fakeRepository.presenceResult = Result.success(
            listOf(UserPresence(2L, UserPresenceStatus.ONLINE, version = 4L))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(UserPresenceStatus.ONLINE, viewModel.uiState.value.presence?.status)

        fakeRepository.emitPresenceUpdate(
            UserPresence(2L, UserPresenceStatus.OFFLINE, "2026-10-28T01:00:00Z", version = 5L)
        )
        fakeRepository.emitPresenceUpdate(UserPresence(2L, UserPresenceStatus.ONLINE, version = 4L))
        advanceUntilIdle()

        assertEquals(UserPresenceStatus.OFFLINE, viewModel.uiState.value.presence?.status)
        assertEquals(5L, viewModel.uiState.value.presence?.version)
    }

    @Test
    fun loadOlderMessagesPrependsMessagesToState() = runTest {
        // Page 0 returns 40 messages so hasMoreOlderMessages becomes true
        val page0Messages = (10L..49L).map { id ->
            ChatMessage(
                id = id,
                conversationId = 10L,
                senderId = 1L,
                senderName = "Me",
                senderAvatarUrl = null,
                content = "Message $id",
                messageType = "TEXT",
                createdAt = "2026-10-26T10:05:00Z",
                isFromCurrentUser = true
            )
        }.toMutableList()
        fakeRepository.messagesMap[10L] = page0Messages

        val viewModel = createViewModel(conversationId = 10L)
        advanceUntilIdle()
        assertEquals(40, viewModel.uiState.value.messages.size)
        assertTrue(viewModel.uiState.value.hasMoreOlderMessages)

        // When older page is fetched
        val olderMsg = ChatMessage(
            id = 1L,
            conversationId = 10L,
            senderId = 2L,
            senderName = "Coach Alex",
            senderAvatarUrl = null,
            content = "First message",
            messageType = "TEXT",
            createdAt = "2026-10-26T10:00:00Z",
            isFromCurrentUser = false
        )
        fakeRepository.olderMessagesToReturn = listOf(olderMsg)

        viewModel.loadOlderMessages()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(41, state.messages.size)
        assertEquals(1L, state.messages[0].id) // Older message at the beginning
        assertEquals(10L, state.messages[1].id)
    }

    private class FakeChatRepository : ChatRepository {
        var markAsReadCalls = 0
        var sendShouldFail = false
        var presenceResult: Result<List<UserPresence>> = Result.success(emptyList())
        val messagesMap = mutableMapOf<Long, MutableList<ChatMessage>>()
        var olderMessagesToReturn = emptyList<ChatMessage>()
        private val incomingFlow = MutableSharedFlow<ChatMessage>()
        private val presenceFlow = MutableSharedFlow<UserPresence>()

        suspend fun emitIncomingMessage(msg: ChatMessage) {
            incomingFlow.emit(msg)
        }

        suspend fun emitPresenceUpdate(update: UserPresence) {
            presenceFlow.emit(update)
        }

        override suspend fun getConversations(): Result<List<ChatConversation>> = Result.success(emptyList())
        override suspend fun getConversationPresence(): Result<List<UserPresence>> = presenceResult
        override suspend fun createOrGetConversation(otherUserId: Long): Result<ChatConversation> = Result.failure(UnsupportedOperationException())

        override suspend fun getMessages(conversationId: Long, page: Int, size: Int): Result<List<ChatMessage>> {
            return if (page == 0) {
                Result.success(messagesMap[conversationId] ?: emptyList())
            } else {
                Result.success(olderMessagesToReturn)
            }
        }

        override suspend fun sendMessage(
            conversationId: Long,
            content: String,
            clientMessageId: String?
        ): Result<ChatMessage> {
            if (sendShouldFail) {
                return Result.failure(RuntimeException("Failed to send message"))
            }
            val newMsg = ChatMessage(
                id = System.currentTimeMillis(),
                conversationId = conversationId,
                senderId = 1L,
                senderName = "Me",
                senderAvatarUrl = null,
                content = content,
                messageType = "TEXT",
                createdAt = "2026-10-26T12:00:00Z",
                isFromCurrentUser = true,
                clientMessageId = clientMessageId,
                deliveryStatus = MessageDeliveryStatus.SENT
            )
            return Result.success(newMsg)
        }

        override suspend fun markAsRead(conversationId: Long): Result<Unit> {
            markAsReadCalls++
            return Result.success(Unit)
        }

        override suspend fun getUnreadCount(): Result<Long> = Result.success(0L)
        override suspend fun getAvailableContacts(): Result<List<ChatContact>> = Result.success(emptyList())
        override fun getIncomingMessages(): Flow<ChatMessage> = incomingFlow
        override fun getPresenceUpdates(): Flow<UserPresence> = presenceFlow
        override fun getWebSocketState(): StateFlow<WebSocketState> = MutableStateFlow(WebSocketState.CONNECTED)
        override fun connectWebSocket() {}
        override fun disconnectWebSocket() {}
        override fun subscribeToConversation(conversationId: Long) {}
        override fun unsubscribeFromConversation(conversationId: Long) {}
        override fun getCurrentUserId(): Long? = 1L
    }
}
