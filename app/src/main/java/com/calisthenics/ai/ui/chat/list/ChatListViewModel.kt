package com.calisthenics.ai.ui.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val conversations: List<ChatConversation> = emptyList(),
    val presences: Map<Long, UserPresence> = emptyMap(),
    val isPresenceLoading: Boolean = false,
    val presenceError: String? = null,
    val webSocketState: WebSocketState = WebSocketState.DISCONNECTED,
    val availableContacts: List<ChatContact> = emptyList(),
    val totalUnreadCount: Long = 0,
    val error: String? = null,
    val isNewChatDialogVisible: Boolean = false,
    val isCreatingConversation: Boolean = false
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()
    private var presenceLoadJob: Job? = null

    init {
        chatRepository.connectWebSocket()
        loadConversations()
        observeIncomingMessages()
        observePresenceUpdates()
        observeWebSocketState()
    }

    fun loadConversations(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            chatRepository.getConversations()
                .onSuccess { conversations ->
                    val totalUnread = conversations.sumOf { it.unreadCount }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            conversations = conversations,
                            totalUnreadCount = totalUnread,
                            error = null
                        )
                    }
                    loadPresence()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.localizedMessage ?: "Failed to load conversations"
                        )
                    }
                }
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            chatRepository.getIncomingMessages().collect {
                // When an incoming message arrives, refresh conversations to update previews and unread badges
                chatRepository.getConversations().onSuccess { conversations ->
                    val totalUnread = conversations.sumOf { it.unreadCount }
                    _uiState.update {
                        it.copy(
                            conversations = conversations,
                            totalUnreadCount = totalUnread
                        )
                    }
                }
            }
        }
    }

    private fun observePresenceUpdates() {
        viewModelScope.launch {
            chatRepository.getPresenceUpdates().collect { presence ->
                _uiState.update { state ->
                    state.copy(presences = mergePresences(state.presences, listOf(presence)))
                }
            }
        }
    }

    private fun observeWebSocketState() {
        viewModelScope.launch {
            var wasConnected = false
            chatRepository.getWebSocketState().collect { connectionState ->
                _uiState.update { it.copy(webSocketState = connectionState) }
                if (connectionState == WebSocketState.CONNECTED && !wasConnected) {
                    loadPresence()
                }
                wasConnected = connectionState == WebSocketState.CONNECTED
            }
        }
    }

    private fun loadPresence() {
        if (presenceLoadJob?.isActive == true) return
        presenceLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isPresenceLoading = true, presenceError = null) }
            chatRepository.getConversationPresence()
                .onSuccess { presences ->
                    _uiState.update { state ->
                        state.copy(
                            isPresenceLoading = false,
                            presenceError = null,
                            presences = mergePresences(state.presences, presences)
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isPresenceLoading = false,
                            presenceError = throwable.localizedMessage ?: "Presence is unavailable"
                        )
                    }
                }
        }
    }

    private fun mergePresences(
        existing: Map<Long, UserPresence>,
        updates: List<UserPresence>
    ): Map<Long, UserPresence> = updates.fold(existing) { result, update ->
        val current = result[update.userId]
        if (current == null || update.version >= current.version) {
            result + (update.userId to update)
        } else {
            result
        }
    }

    fun showNewChatDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isNewChatDialogVisible = true) }
            chatRepository.getAvailableContacts().onSuccess { contacts ->
                _uiState.update { it.copy(availableContacts = contacts) }
            }
        }
    }

    fun dismissNewChatDialog() {
        _uiState.update { it.copy(isNewChatDialogVisible = false) }
    }

    fun startConversation(otherUserId: Long, onConversationReady: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingConversation = true) }
            chatRepository.createOrGetConversation(otherUserId)
                .onSuccess { conversation ->
                    _uiState.update {
                        it.copy(
                            isCreatingConversation = false,
                            isNewChatDialogVisible = false
                        )
                    }
                    onConversationReady(conversation.id)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingConversation = false,
                            error = throwable.localizedMessage ?: "Could not start conversation"
                        )
                    }
                }
        }
    }
}
