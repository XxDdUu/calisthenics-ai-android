package com.calisthenics.ai.ui.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.AiConversationDetail
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.repository.AiChatRepository
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
    val isCreatingConversation: Boolean = false,
    val aiSummary: AiConversationSummary? = null,
    val isAiSummaryLoading: Boolean = false
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiChatRepository: AiChatRepository,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    constructor(
        chatRepository: ChatRepository,
        accountSessionManager: AccountSessionManager = AccountSessionManager()
    ) : this(
        chatRepository = chatRepository,
        aiChatRepository = object : AiChatRepository {
            override suspend fun getConversations(): Result<List<AiConversationSummary>> = Result.success(emptyList())
            override suspend fun getLatestConversationSummary(): Result<AiConversationSummary?> = Result.success(null)
            override suspend fun createConversation(title: String?): Result<AiConversationSummary> = Result.failure(NotImplementedError())
            override suspend fun getConversation(id: Long): Result<AiConversationDetail> = Result.failure(NotImplementedError())
            override suspend fun deleteConversation(id: Long): Result<Unit> = Result.success(Unit)
            override suspend fun sendMessage(conversationId: Long, message: String, timezone: String?): Result<AiMessage> = Result.failure(NotImplementedError())
            override suspend fun sendQuickMessage(message: String, timezone: String?): Result<AiMessage> = Result.failure(NotImplementedError())
        },
        accountSessionManager = accountSessionManager
    )

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()
    private var presenceLoadJob: Job? = null

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            presenceLoadJob = null
            _uiState.value = ChatListUiState()
        },
        onSessionActive = {
            startChatSession()
        }
    )

    init {
        sessionGuard.startObserving()
        startChatSession()
    }

    private fun startChatSession() {
        chatRepository.connectWebSocket()
        loadConversations()
        loadAiSummary()
        observeIncomingMessages()
        observePresenceUpdates()
        observeWebSocketState()
    }

    fun loadAiSummary() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isAiSummaryLoading = true) }
            aiChatRepository.getLatestConversationSummary()
                .onSuccess { summary ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(aiSummary = summary, isAiSummaryLoading = false) }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isAiSummaryLoading = false) }
                }
        }
    }

    fun loadConversations(isRefresh: Boolean = false) {
        if (isRefresh) {
            loadAiSummary()
        }
        sessionGuard.launchScoped { captured ->
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            chatRepository.getConversations()
                .onSuccess { conversations ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
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
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
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
        sessionGuard.launchScoped { captured ->
            chatRepository.getIncomingMessages().collect {
                if (!sessionGuard.isCurrentSession(captured)) return@collect
                // When an incoming message arrives, refresh conversations to update previews and unread badges
                chatRepository.getConversations().onSuccess { conversations ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
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
        sessionGuard.launchScoped { captured ->
            chatRepository.getPresenceUpdates().collect { presence ->
                if (!sessionGuard.isCurrentSession(captured)) return@collect
                _uiState.update { state ->
                    state.copy(presences = mergePresences(state.presences, listOf(presence)))
                }
            }
        }
    }

    private fun observeWebSocketState() {
        sessionGuard.launchScoped { captured ->
            var wasConnected = false
            chatRepository.getWebSocketState().collect { connectionState ->
                if (!sessionGuard.isCurrentSession(captured)) return@collect
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
        presenceLoadJob = sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isPresenceLoading = true, presenceError = null) }
            chatRepository.getConversationPresence()
                .onSuccess { presences ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        state.copy(
                            isPresenceLoading = false,
                            presenceError = null,
                            presences = mergePresences(state.presences, presences)
                        )
                    }
                }
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
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
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isNewChatDialogVisible = true) }
            chatRepository.getAvailableContacts().onSuccess { contacts ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update { it.copy(availableContacts = contacts) }
            }
        }
    }

    fun dismissNewChatDialog() {
        _uiState.update { it.copy(isNewChatDialogVisible = false) }
    }

    fun startConversation(otherUserId: Long, onConversationReady: (Long) -> Unit) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isCreatingConversation = true) }
            chatRepository.createOrGetConversation(otherUserId)
                .onSuccess { conversation ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isCreatingConversation = false,
                            isNewChatDialogVisible = false
                        )
                    }
                    onConversationReady(conversation.id)
                }
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
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
