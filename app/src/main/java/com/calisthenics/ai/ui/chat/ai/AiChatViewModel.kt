package com.calisthenics.ai.ui.chat.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.AiActionCard
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage
import com.calisthenics.ai.domain.repository.AiChatRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AiChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentConversationId: Long? = null,
    val conversationTitle: String = "AI Assistant",
    val messages: List<AiMessage> = emptyList(),
    val allConversations: List<AiConversationSummary> = emptyList(),
    val isHistorySheetOpen: Boolean = false,
    val isDeleting: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    private val profileRepository: ProfileRepository? = null,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialConversationId: Long? = savedStateHandle.get<Long>("conversationId")?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(AiChatUiState(currentConversationId = initialConversationId))
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            _uiState.value = AiChatUiState()
        },
        onSessionActive = {
            initChat()
        }
    )

    init {
        sessionGuard.startObserving()
        initChat()
    }

    private fun initChat() {
        loadConversation(initialConversationId)
        loadAllConversations()
    }

    fun loadConversation(conversationId: Long? = null) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (conversationId != null && conversationId > 0L) {
                aiChatRepository.getConversation(conversationId)
                    .onSuccess { detail ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentConversationId = detail.id,
                                conversationTitle = detail.title,
                                messages = detail.messages,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                        _uiState.update {
                            it.copy(isLoading = false, error = error.localizedMessage ?: "Failed to load conversation")
                        }
                    }
            } else {
                // Fetch the latest conversation or ready for new messages
                aiChatRepository.getLatestConversationSummary()
                    .onSuccess { summary ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                        if (summary != null) {
                            loadConversation(summary.id)
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentConversationId = null,
                                    conversationTitle = "New Conversation",
                                    messages = emptyList(),
                                    error = null
                                )
                            }
                        }
                    }
                    .onFailure {
                        if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentConversationId = null,
                                conversationTitle = "New Conversation",
                                messages = emptyList(),
                                error = null
                            )
                        }
                    }
            }
        }
    }

    fun loadAllConversations() {
        sessionGuard.launchScoped { captured ->
            aiChatRepository.getConversations().onSuccess { list ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update { it.copy(allConversations = list) }
            }
        }
    }

    fun startNewConversation() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, isHistorySheetOpen = false) }
            aiChatRepository.createConversation("New Conversation")
                .onSuccess { summary ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentConversationId = summary.id,
                            conversationTitle = summary.title,
                            messages = emptyList(),
                            error = null
                        )
                    }
                    loadAllConversations()
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentConversationId = null,
                            conversationTitle = "New Conversation",
                            messages = emptyList()
                        )
                    }
                }
        }
    }

    fun selectConversation(conversationId: Long) {
        _uiState.update { it.copy(isHistorySheetOpen = false) }
        loadConversation(conversationId)
    }

    fun deleteConversation(conversationId: Long) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isDeleting = true) }
            aiChatRepository.deleteConversation(conversationId)
                .onSuccess {
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(isDeleting = false) }
                    loadAllConversations()
                    if (_uiState.value.currentConversationId == conversationId) {
                        loadConversation(null)
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isDeleting = false) }
                }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _uiState.value.isSending) return

        sessionGuard.launchScoped { captured ->
            // Optimistically add user's message
            val tempUserMessage = AiMessage(
                id = -System.currentTimeMillis(),
                conversationId = _uiState.value.currentConversationId ?: 0L,
                role = "user",
                content = trimmed,
                intent = null,
                metadata = null,
                createdAt = null
            )

            _uiState.update {
                it.copy(
                    isSending = true,
                    error = null,
                    messages = it.messages + tempUserMessage
                )
            }

            val conversationId = _uiState.value.currentConversationId
            val result = if (conversationId != null && conversationId > 0L) {
                aiChatRepository.sendMessage(conversationId = conversationId, message = trimmed)
            } else {
                aiChatRepository.sendQuickMessage(message = trimmed)
            }

            result.onSuccess { assistantMsg ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update {
                    it.copy(
                        isSending = false,
                        currentConversationId = assistantMsg.conversationId,
                        messages = it.messages.filter { m -> m.id != tempUserMessage.id } +
                                tempUserMessage.copy(id = assistantMsg.id - 1) +
                                assistantMsg,
                        error = null
                    )
                }
                loadAllConversations()

                // If profile was updated successfully, refresh user profile caches & notify observers
                if (assistantMsg.actionCard is AiActionCard.ProfileUpdate && assistantMsg.actionCard.success) {
                    profileRepository?.getMyClientProfile()
                }
            }.onFailure { error ->
                if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = error.localizedMessage ?: "Failed to receive AI response. Please try again."
                    )
                }
            }
        }
    }

    fun openHistorySheet() {
        loadAllConversations()
        _uiState.update { it.copy(isHistorySheetOpen = true) }
    }

    fun closeHistorySheet() {
        _uiState.update { it.copy(isHistorySheetOpen = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
