package com.calisthenics.ai.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ClientDetailUiState(
    val isLoading: Boolean = false,
    val isStartingChat: Boolean = false,
    val isTerminating: Boolean = false,
    val clientProfile: ClientProfile? = null,
    val trainingOverview: ClientTrainingOverview? = null,
    val clientMedia: List<ProfileMediaItem> = emptyList(),
    val activeRelationshipId: Long? = null,
    val error: String? = null,
    val isForbidden: Boolean = false
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val coachClientRepository: CoachClientRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val clientId: Long = checkNotNull(savedStateHandle.get<Long>("clientId"))

    private val _uiState = MutableStateFlow(ClientDetailUiState(isLoading = true))
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = ClientDetailUiState(isLoading = false) },
        onSessionActive = { load() }
    )

    init {
        sessionGuard.startObserving()
        load()
    }

    fun load() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null, isForbidden = false) }

            val profileResult = profileRepository.getClientProfile(clientId)
            val overviewResult = profileRepository.getClientOverview(clientId)
            val mediaResult = profileRepository.getProfileMedia(clientId, ProfileType.ATHLETE)
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped

            if (profileResult.isSuccess) {
                val profile = profileResult.getOrNull()
                val relId = profile?.connectedCoach?.relationshipId
                    ?: coachClientRepository.loadRelationships().getOrNull()
                        ?.firstOrNull { it.status == "ACTIVE" && it.clientId == clientId }?.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        clientProfile = profile,
                        trainingOverview = overviewResult.getOrNull(),
                        clientMedia = mediaResult.getOrDefault(emptyList()),
                        activeRelationshipId = relId,
                        error = null,
                        isForbidden = false
                    )
                }
            } else {
                val exception = profileResult.exceptionOrNull()
                val is403 = (exception as? HttpException)?.code() == 403 ||
                    exception?.message?.contains("403") == true ||
                    exception?.message?.contains("Forbidden") == true

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        clientProfile = null,
                        trainingOverview = null,
                        isForbidden = is403,
                        error = if (is403) {
                            "Access Restricted: Only active coaches can view their clients' training profiles."
                        } else {
                            exception?.localizedMessage ?: "Failed to load client profile"
                        }
                    )
                }
            }
        }
    }

    fun startChat(onReady: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingChat = true) }
            chatRepository.createOrGetConversation(clientId)
                .onSuccess { conversation ->
                    _uiState.update { it.copy(isStartingChat = false) }
                    onReady(conversation.id)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isStartingChat = false,
                            error = throwable.localizedMessage ?: "Failed to start conversation"
                        )
                    }
                }
        }
    }

    fun terminateRelationship(onSuccess: () -> Unit) {
        val relId = _uiState.value.activeRelationshipId
            ?: _uiState.value.clientProfile?.connectedCoach?.relationshipId
        if (relId == null) {
            _uiState.update { it.copy(error = "Active relationship not found") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTerminating = true) }
            coachClientRepository.terminateRelationship(relId)
                .onSuccess {
                    _uiState.update { it.copy(isTerminating = false) }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isTerminating = false,
                            error = throwable.localizedMessage ?: "Failed to end relationship"
                        )
                    }
                }
        }
    }
}
