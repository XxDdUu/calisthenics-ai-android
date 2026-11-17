package com.calisthenics.ai.ui.chat.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.MessageDeliveryStatus
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatConversationUiState(
    val conversationId: Long = 0L,
    val otherUserName: String = "Chat",
    val otherUserAvatarUrl: String? = null,
    val otherUserId: Long = 0L,
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val isSending: Boolean = false,
    val presence: UserPresence? = null,
    val isPresenceLoading: Boolean = false,
    val presenceError: String? = null,
    val webSocketState: WebSocketState = WebSocketState.DISCONNECTED,
    val messages: List<ChatMessage> = emptyList(),
    val currentPage: Int = 0,
    val hasMoreOlderMessages: Boolean = true,
    val inputText: String = "",
    val errorMessage: String? = null,
    val shouldScrollToBottom: Boolean = false
)

@HiltViewModel
class ChatConversationViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val conversationId: Long = savedStateHandle.get<Long>("conversationId")
        ?: savedStateHandle.get<String>("conversationId")?.toLongOrNull() ?: 0L
    private val otherUserName: String = savedStateHandle.get<String>("otherUserName") ?: "Chat"
    private val otherUserAvatarUrl: String? = savedStateHandle.get<String>("otherUserAvatarUrl")
    private val otherUserId: Long = savedStateHandle.get<Long>("otherUserId")
        ?: savedStateHandle.get<String>("otherUserId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(
        ChatConversationUiState(
            conversationId = conversationId,
            otherUserName = otherUserName,
            otherUserAvatarUrl = otherUserAvatarUrl,
            otherUserId = otherUserId
        )
    )
    val uiState: StateFlow<ChatConversationUiState> = _uiState.asStateFlow()

    private val pageSize = 40
    private var initialOwnerUserId: Long? = accountSessionManager.currentUserId()
        ?: chatRepository.getCurrentUserId()?.takeIf { it > 0L }
    private var isConversationSessionActive = true

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            isConversationSessionActive = false
            if (conversationId > 0) {
                chatRepository.unsubscribeFromConversation(conversationId)
            }
            _uiState.value = ChatConversationUiState(
                conversationId = 0L,
                otherUserName = "Chat",
                otherUserAvatarUrl = null,
                otherUserId = 0L,
                messages = emptyList(),
                currentPage = 0,
                hasMoreOlderMessages = false
            )
        },
        onSessionActive = { newSession ->
            // A conversation ID belongs strictly to a specific participant pair; never auto-reuse an
            // earlier account's conversation unless this ViewModel hadn't bound an owner yet.
            if (initialOwnerUserId == null || initialOwnerUserId == newSession.activeUserId) {
                initialOwnerUserId = newSession.activeUserId
                isConversationSessionActive = true
                if (conversationId > 0) {
                    _uiState.value = ChatConversationUiState(
                        conversationId = conversationId,
                        otherUserName = otherUserName,
                        otherUserAvatarUrl = otherUserAvatarUrl,
                        otherUserId = otherUserId
                    )
                    startConversationSession()
                }
            }
        }
    )

    init {
        sessionGuard.startObserving()
        if (conversationId > 0) {
            startConversationSession()
        }
    }

    private fun startConversationSession() {
        chatRepository.connectWebSocket()
        chatRepository.subscribeToConversation(conversationId)
        loadInitialMessages()
        observeIncomingMessages()
        loadPresence()
        observePresenceUpdates()
        observeWebSocketState()
    }

    override fun onCleared() {
        super.onCleared()
        if (conversationId > 0) {
            chatRepository.unsubscribeFromConversation(conversationId)
        }
    }

    fun loadInitialMessages() {
        if (!isConversationSessionActive || conversationId <= 0) return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            chatRepository.getMessages(conversationId, page = 0, size = pageSize)
                .onSuccess { msgs ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = msgs,
                            currentPage = 0,
                            hasMoreOlderMessages = msgs.size >= pageSize,
                            shouldScrollToBottom = true,
                            errorMessage = null
                        )
                    }
                    markAsRead()
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Could not load messages"
                        )
                    }
                }
        }
    }

    fun loadOlderMessages() {
        val state = _uiState.value
        if (!isConversationSessionActive || state.isLoadingOlder || !state.hasMoreOlderMessages || state.isLoading) {
            return
        }

        sessionGuard.launchScoped { captured ->
            val nextPage = state.currentPage + 1
            _uiState.update { it.copy(isLoadingOlder = true) }

            chatRepository.getMessages(conversationId, page = nextPage, size = pageSize)
                .onSuccess { olderMsgs ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onSuccess
                    _uiState.update { current ->
                        val existingIds = current.messages.map { it.id }.toSet()
                        val newOlder = olderMsgs.filter { it.id !in existingIds }
                        current.copy(
                            isLoadingOlder = false,
                            messages = newOlder + current.messages,
                            currentPage = nextPage,
                            hasMoreOlderMessages = olderMsgs.size >= pageSize,
                            shouldScrollToBottom = false
                        )
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onFailure
                    _uiState.update { it.copy(isLoadingOlder = false) }
                }
        }
    }

    fun updateInputText(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val trimmed = state.inputText.trim()
        if (trimmed.isEmpty()) return

        val clientMsgId = java.util.UUID.randomUUID().toString()
        val tempId = -System.currentTimeMillis()
        val currentUserId = chatRepository.getCurrentUserId() ?: 0L

        val optimisticMessage = ChatMessage(
            id = tempId,
            conversationId = conversationId,
            senderId = currentUserId,
            senderName = "Me",
            senderAvatarUrl = null,
            content = trimmed,
            messageType = "TEXT",
            createdAt = java.time.Instant.now().toString(),
            isFromCurrentUser = true,
            clientMessageId = clientMsgId,
            deliveryStatus = MessageDeliveryStatus.SENDING
        )

        // Optimistically update message list and clear input text
        _uiState.update { current ->
            current.copy(
                inputText = "",
                messages = current.messages + optimisticMessage,
                shouldScrollToBottom = true,
                errorMessage = null
            )
        }

        sessionGuard.launchScoped { captured ->
            chatRepository.sendMessage(conversationId, trimmed, clientMsgId)
                .onSuccess { sentMessage ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onSuccess
                    _uiState.update { current ->
                        val updatedList = current.messages.map { msg ->
                            if (msg.clientMessageId == clientMsgId || msg.id == tempId) {
                                sentMessage.copy(
                                    deliveryStatus = MessageDeliveryStatus.SENT,
                                    isFromCurrentUser = true
                                )
                            } else {
                                msg
                            }
                        }
                        current.copy(
                            messages = updatedList,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onFailure
                    _uiState.update { current ->
                        val updatedList = current.messages.map { msg ->
                            if (msg.clientMessageId == clientMsgId || msg.id == tempId) {
                                msg.copy(deliveryStatus = MessageDeliveryStatus.FAILED)
                            } else {
                                msg
                            }
                        }
                        current.copy(
                            messages = updatedList,
                            errorMessage = error.message ?: "Failed to send message"
                        )
                    }
                }
        }
    }

    fun retryMessage(clientMessageId: String) {
        if (!isConversationSessionActive) return
        val messageToRetry = _uiState.value.messages.firstOrNull {
            it.clientMessageId == clientMessageId && it.deliveryStatus == MessageDeliveryStatus.FAILED
        } ?: return

        _uiState.update { current ->
            val updatedList = current.messages.map { msg ->
                if (msg.clientMessageId == clientMessageId) {
                    msg.copy(deliveryStatus = MessageDeliveryStatus.SENDING)
                } else {
                    msg
                }
            }
            current.copy(messages = updatedList, errorMessage = null)
        }

        sessionGuard.launchScoped { captured ->
            chatRepository.sendMessage(conversationId, messageToRetry.content, clientMessageId)
                .onSuccess { sentMessage ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onSuccess
                    _uiState.update { current ->
                        val updatedList = current.messages.map { msg ->
                            if (msg.clientMessageId == clientMessageId) {
                                sentMessage.copy(
                                    deliveryStatus = MessageDeliveryStatus.SENT,
                                    isFromCurrentUser = true
                                )
                            } else {
                                msg
                            }
                        }
                        current.copy(
                            messages = updatedList,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onFailure
                    _uiState.update { current ->
                        val updatedList = current.messages.map { msg ->
                            if (msg.clientMessageId == clientMessageId) {
                                msg.copy(deliveryStatus = MessageDeliveryStatus.FAILED)
                            } else {
                                msg
                            }
                        }
                        current.copy(
                            messages = updatedList,
                            errorMessage = error.message ?: "Failed to resend message"
                        )
                    }
                }
        }
    }

    private fun observeIncomingMessages() {
        sessionGuard.launchScoped { captured ->
            chatRepository.getIncomingMessages().collect { incoming ->
                if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@collect
                if (incoming.conversationId == conversationId) {
                    _uiState.update { current ->
                        val existingIndex = current.messages.indexOfFirst {
                            (incoming.clientMessageId != null && it.clientMessageId == incoming.clientMessageId) ||
                                    (incoming.id > 0 && it.id == incoming.id)
                        }
                        val updatedMessages = if (existingIndex >= 0) {
                            current.messages.toMutableList().apply {
                                this[existingIndex] = incoming.copy(deliveryStatus = MessageDeliveryStatus.SENT)
                            }
                        } else {
                            current.messages + incoming.copy(deliveryStatus = MessageDeliveryStatus.SENT)
                        }
                        current.copy(
                            messages = updatedMessages,
                            shouldScrollToBottom = true
                        )
                    }
                    markAsRead()
                }
            }
        }
    }

    private fun loadPresence() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isPresenceLoading = true, presenceError = null) }
            chatRepository.getConversationPresence()
                .onSuccess { presences ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onSuccess
                    val targetPresence = presences.firstOrNull { it.userId == otherUserId }
                    _uiState.update { current ->
                        val existing = current.presence
                        if (targetPresence != null &&
                            (existing == null || targetPresence.version >= existing.version)
                        ) {
                            current.copy(
                                presence = targetPresence,
                                isPresenceLoading = false,
                                presenceError = null
                            )
                        } else {
                            current.copy(isPresenceLoading = false, presenceError = null)
                        }
                    }
                }
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@onFailure
                    _uiState.update {
                        it.copy(
                            isPresenceLoading = false,
                            presenceError = throwable.localizedMessage ?: "Presence is unavailable"
                        )
                    }
                }
        }
    }

    private fun observePresenceUpdates() {
        sessionGuard.launchScoped { captured ->
            chatRepository.getPresenceUpdates().collect { update ->
                if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@collect
                if (update.userId == otherUserId) {
                    _uiState.update { current ->
                        val existing = current.presence
                        if (existing == null || update.version >= existing.version) {
                            current.copy(presence = update, presenceError = null)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    private fun observeWebSocketState() {
        sessionGuard.launchScoped { captured ->
            var wasConnected = false
            chatRepository.getWebSocketState().collect { connectionState ->
                if (!sessionGuard.isCurrentSession(captured) || !isConversationSessionActive) return@collect
                _uiState.update { it.copy(webSocketState = connectionState) }
                if (connectionState == WebSocketState.CONNECTED && !wasConnected) {
                    loadPresence()
                }
                wasConnected = connectionState == WebSocketState.CONNECTED
            }
        }
    }

    fun markAsRead() {
        if (!isConversationSessionActive || conversationId <= 0) return
        sessionGuard.launchScoped {
            chatRepository.markAsRead(conversationId)
        }
    }

    fun resetScrollToBottom() {
        _uiState.update { it.copy(shouldScrollToBottom = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
