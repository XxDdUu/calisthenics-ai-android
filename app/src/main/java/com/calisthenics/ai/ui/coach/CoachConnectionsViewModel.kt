package com.calisthenics.ai.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

enum class CoachSection {
    ALL,
    DISCOVER,
    MY_COACHES,
    SENT,
    INCOMING,
    MY_CLIENTS
}

data class CoachConnectionsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshingSections: Set<CoachSection> = emptySet(),
    val isProfileLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val currentUserId: Long? = null,
    val coaches: List<CoachProfile> = emptyList(),
    val relationships: List<CoachClientRelationship> = emptyList(),
    val selectedProfile: CoachProfile? = null,
    val selectedProfileId: Long? = null,
    val requestingCoachIds: Set<Long> = emptySet(),
    val busyRelationshipIds: Set<Long> = emptySet(),
    val busyChatUserIds: Set<Long> = emptySet(),
    val error: String? = null,
    val profileError: String? = null,
    val feedback: String? = null
) {
    fun isSectionRefreshing(section: CoachSection): Boolean =
        isRefreshing || section in refreshingSections

    val myCoaches: List<CoachClientRelationship>
        get() = relationships.filter { it.status == "ACTIVE" && it.clientId == currentUserId }

    val myClients: List<CoachClientRelationship>
        get() = relationships.filter { it.status == "ACTIVE" && it.coachId == currentUserId }

    val sentRequests: List<CoachClientRelationship>
        get() = relationships.filter { it.status == "PENDING" && it.requestedBy == currentUserId }

    val incomingRequests: List<CoachClientRelationship>
        get() = relationships.filter {
            it.status == "PENDING" && currentUserId != null && it.requestedBy != currentUserId
        }.distinctBy { it.id }

    val incomingRequestCount: Int
        get() = incomingRequests.size
}

@HiltViewModel
class CoachConnectionsViewModel @Inject constructor(
    private val repository: CoachClientRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachConnectionsUiState())
    val uiState: StateFlow<CoachConnectionsUiState> = _uiState.asStateFlow()
    private var overviewRefreshJob: Job? = null
    private var relationshipMutationVersion = 0

    init {
        refresh()
    }

    fun refresh() {
        if (overviewRefreshJob?.isActive == true ||
            _uiState.value.busyRelationshipIds.isNotEmpty() ||
            _uiState.value.requestingCoachIds.isNotEmpty()
        ) return

        overviewRefreshJob = viewModelScope.launch {
            val requestedMutationVersion = relationshipMutationVersion
            val isRefresh = _uiState.value.hasLoaded
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    refreshingSections = emptySet(),
                    error = null,
                    feedback = null
                )
            }
            repository.loadOverview()
                .onSuccess { overview -> applyOverview(overview, requestedMutationVersion) }
                .onFailure { throwable ->
                    if (requestedMutationVersion != relationshipMutationVersion) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            refreshingSections = emptySet(),
                            hasLoaded = true,
                            error = if (it.coaches.isEmpty() && it.relationships.isEmpty()) {
                                userMessage(throwable)
                            } else {
                                null
                            },
                            feedback = userMessage(throwable)
                        )
                    }
                }
        }
    }

    fun refreshAll() = refresh()

    fun refreshDiscover() {
        if (!_uiState.value.hasLoaded || _uiState.value.currentUserId == null) {
            refresh()
            return
        }
        if (CoachSection.DISCOVER in _uiState.value.refreshingSections || overviewRefreshJob?.isActive == true) return

        _uiState.update {
            it.copy(
                refreshingSections = it.refreshingSections + CoachSection.DISCOVER,
                feedback = null
            )
        }
        viewModelScope.launch {
            repository.loadCoaches()
                .onSuccess { coaches ->
                    _uiState.update {
                        it.copy(
                            coaches = coaches,
                            refreshingSections = it.refreshingSections - CoachSection.DISCOVER,
                            selectedProfile = it.selectedProfile?.let { profile ->
                                coaches.firstOrNull { coach -> coach.id == profile.id } ?: profile
                            }
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            refreshingSections = it.refreshingSections - CoachSection.DISCOVER,
                            feedback = userMessage(throwable)
                        )
                    }
                }
        }
    }

    fun refreshMyCoaches() = refreshRelationships(CoachSection.MY_COACHES)

    fun refreshSentRequests() = refreshRelationships(CoachSection.SENT)

    fun refreshIncomingRequests() = refreshRelationships(CoachSection.INCOMING)

    fun refreshMyClients() = refreshRelationships(CoachSection.MY_CLIENTS)

    fun refreshRelationships(section: CoachSection = CoachSection.ALL) {
        if (!_uiState.value.hasLoaded || _uiState.value.currentUserId == null) {
            refresh()
            return
        }
        if (section in _uiState.value.refreshingSections || overviewRefreshJob?.isActive == true) return
        if (_uiState.value.busyRelationshipIds.isNotEmpty() ||
            _uiState.value.requestingCoachIds.isNotEmpty()
        ) return

        val requestedMutationVersion = relationshipMutationVersion
        _uiState.update {
            it.copy(
                refreshingSections = it.refreshingSections + section,
                feedback = null
            )
        }
        viewModelScope.launch {
            repository.loadRelationships()
                .onSuccess { relationships ->
                    if (requestedMutationVersion != relationshipMutationVersion) return@onSuccess
                    _uiState.update {
                        it.copy(
                            relationships = relationships.distinctBy { r -> r.id },
                            refreshingSections = it.refreshingSections - section
                        )
                    }
                }
                .onFailure { throwable ->
                    if (requestedMutationVersion != relationshipMutationVersion) return@onFailure
                    _uiState.update {
                        it.copy(
                            refreshingSections = it.refreshingSections - section,
                            feedback = userMessage(throwable)
                        )
                    }
                }
        }
    }

    fun refreshSection(section: CoachSection) {
        when (section) {
            CoachSection.ALL -> refresh()
            CoachSection.DISCOVER -> refreshDiscover()
            CoachSection.MY_COACHES -> refreshMyCoaches()
            CoachSection.SENT -> refreshSentRequests()
            CoachSection.INCOMING -> refreshIncomingRequests()
            CoachSection.MY_CLIENTS -> refreshMyClients()
        }
    }

    fun loadCoachProfile(id: Long) {
        val state = _uiState.value
        val loadedProfile = state.coaches.firstOrNull { it.id == id }
        _uiState.update {
            it.copy(
                selectedProfile = loadedProfile ?: it.selectedProfile,
                selectedProfileId = id,
                isProfileLoading = loadedProfile == null,
                profileError = null
            )
        }

        viewModelScope.launch {
            repository.getCoachProfile(id)
                .onSuccess { profile ->
                    _uiState.update {
                        if (it.selectedProfileId == id) {
                            it.copy(selectedProfile = profile, isProfileLoading = false, profileError = null)
                        } else {
                            it
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        if (it.selectedProfileId == id && it.selectedProfile == null) {
                            it.copy(
                                selectedProfile = null,
                                isProfileLoading = false,
                                profileError = userMessage(throwable)
                            )
                        } else {
                            it.copy(isProfileLoading = false)
                        }
                    }
                }
        }
    }

    fun requestConnection(coachId: Long) {
        val state = _uiState.value
        val currentUserId = state.currentUserId ?: return
        if (coachId in state.requestingCoachIds || state.relationships.any {
                it.coachId == coachId && it.clientId == currentUserId &&
                    it.status in setOf("PENDING", "ACTIVE")
            }
        ) return

        _uiState.update {
            it.copy(requestingCoachIds = it.requestingCoachIds + coachId, feedback = null)
        }
        viewModelScope.launch {
            repository.requestConnection(coachId)
                .onSuccess { relationship ->
                    addOrReplace(relationship)
                    _uiState.update { it.copy(feedback = "Connection request sent.") }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(feedback = userMessage(throwable)) }
                }
            _uiState.update {
                it.copy(requestingCoachIds = it.requestingCoachIds - coachId)
            }
        }
    }

    fun acceptRequest(relationshipId: Long) = updateRequest(
        relationshipId,
        "Connection accepted."
    ) { repository.acceptRequest(relationshipId) }

    fun rejectRequest(relationshipId: Long) = updateRequest(
        relationshipId,
        "Request declined."
    ) { repository.rejectRequest(relationshipId) }

    fun cancelRequest(relationshipId: Long) = updateRequest(
        relationshipId,
        "Request canceled."
    ) { repository.cancelRequest(relationshipId) }

    fun startChat(userId: Long, onConversationReady: (Long) -> Unit) {
        if (userId in _uiState.value.busyChatUserIds) return
        _uiState.update {
            it.copy(busyChatUserIds = it.busyChatUserIds + userId, feedback = null)
        }
        viewModelScope.launch {
            chatRepository.createOrGetConversation(userId)
                .onSuccess { conversation ->
                    onConversationReady(conversation.id)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(feedback = userMessage(throwable)) }
                }
            _uiState.update {
                it.copy(busyChatUserIds = it.busyChatUserIds - userId)
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }

    private fun updateRequest(
        relationshipId: Long,
        successMessage: String,
        request: suspend () -> Result<CoachClientRelationship>
    ) {
        if (relationshipId in _uiState.value.busyRelationshipIds) return
        _uiState.update {
            it.copy(
                busyRelationshipIds = it.busyRelationshipIds + relationshipId,
                feedback = null
            )
        }
        viewModelScope.launch {
            request()
                .onSuccess { relationship ->
                    addOrReplace(relationship)
                    _uiState.update { it.copy(feedback = successMessage) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(feedback = userMessage(throwable)) }
                }
            _uiState.update {
                it.copy(busyRelationshipIds = it.busyRelationshipIds - relationshipId)
            }
        }
    }

    private fun addOrReplace(relationship: CoachClientRelationship) {
        relationshipMutationVersion++
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                isRefreshing = false,
                relationships = state.relationships
                    .filterNot { it.id == relationship.id }
                    .plus(relationship)
                    .sortedByDescending { it.createdAt.orEmpty() }
            )
        }
    }

    private fun applyOverview(
        overview: CoachConnectionOverview,
        requestedMutationVersion: Int
    ) {
        if (requestedMutationVersion != relationshipMutationVersion) return
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                refreshingSections = emptySet(),
                hasLoaded = true,
                currentUserId = overview.currentUserId,
                coaches = overview.coaches,
                relationships = overview.relationships.distinctBy { relationship -> relationship.id },
                selectedProfile = it.selectedProfile?.let { profile ->
                    overview.coaches.firstOrNull { coach -> coach.id == profile.id } ?: profile
                },
                error = null,
                feedback = null
            )
        }
    }

    private fun userMessage(throwable: Throwable): String = when (throwable) {
        is HttpException -> when (throwable.code()) {
            401 -> "Your session has expired. Sign in again to continue."
            403 -> "You are not allowed to perform this action."
            404 -> "This profile or request is no longer available."
            409 -> "This request has already changed. Refresh and try again."
            else -> "The server could not complete this request. Please try again."
        }
        is IOException -> "Check your internet connection and try again."
        else -> "Something went wrong. Please try again."
    }
}
