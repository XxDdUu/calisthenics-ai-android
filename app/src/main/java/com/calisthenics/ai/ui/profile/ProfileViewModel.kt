package com.calisthenics.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileTab {
    CLIENT,
    COACH
}

data class ClientFormState(
    val displayName: String = "",
    val bio: String = "",
    val experienceLevel: String = "INTERMEDIATE",
    val trainingExperience: String = "",
    val primaryGoals: String = "",
    val trainingInterests: String = "",
    val preferredFrequency: String = "",
    val coachingLanguage: String = ""
)

data class CoachFormState(
    val displayName: String = "",
    val bio: String = "",
    val specialties: String = "",
    val yearsExperience: String = "",
    val trainingPhilosophy: String = "",
    val coachingDescription: String = "",
    val languages: String = "",
    val certifications: String = "",
    val socialLinks: String = "",
    val isPublic: Boolean = true,
    val isAcceptingClients: Boolean = true
)

data class ProfileUiState(
    val activeTab: ProfileTab = ProfileTab.CLIENT,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val clientProfile: ClientProfile? = null,
    val coachProfile: CoachProfile? = null,
    val activeClients: List<CoachClientRelationship> = emptyList(),
    val busyChatUserIds: Set<Long> = emptySet(),
    val busyRelationshipIds: Set<Long> = emptySet(),
    val error: String? = null,
    val feedback: String? = null,
    val isEditingClient: Boolean = false,
    val isEditingCoach: Boolean = false,
    val clientForm: ClientFormState = ClientFormState(),
    val coachForm: CoachFormState = CoachFormState(),
    val clientFormError: String? = null,
    val coachFormError: String? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarUploadError: String? = null,
    val avatarUploadSucceeded: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val coachClientRepository: CoachClientRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(activeTab = tab, feedback = null, error = null) }
        if (tab == ProfileTab.COACH && _uiState.value.coachProfile == null) {
            loadCoachProfile()
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, feedback = null) }
            loadClientProfileInternal()
            loadCoachProfileInternal()
            loadActiveClientsInternal()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadClientProfileInternal() {
        profileRepository.getMyClientProfile()
            .onSuccess { profile ->
                authRepository.updateAccountProfile(
                    userId = profile.id,
                    fullName = profile.displayName.ifBlank { profile.fullName },
                    avatarUrl = profile.avatarUrl
                )
                _uiState.update {
                    it.copy(
                        clientProfile = profile,
                        clientForm = ClientFormState(
                            displayName = profile.displayName,
                            bio = profile.bio.orEmpty(),
                            experienceLevel = profile.experienceLevel ?: "INTERMEDIATE",
                            trainingExperience = profile.trainingExperience.orEmpty(),
                            primaryGoals = profile.primaryGoals.orEmpty(),
                            trainingInterests = profile.trainingInterests.orEmpty(),
                            preferredFrequency = profile.preferredFrequency.orEmpty(),
                            coachingLanguage = profile.coachingLanguage.orEmpty()
                        )
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update { it.copy(error = throwable.localizedMessage ?: "Could not load client profile") }
            }
    }

    private suspend fun loadCoachProfileInternal() {
        profileRepository.getMyCoachProfile()
            .onSuccess { profile ->
                authRepository.updateAccountProfile(
                    userId = profile.id,
                    fullName = profile.displayName.ifBlank { profile.fullName },
                    avatarUrl = profile.avatarUrl
                )
                _uiState.update {
                    it.copy(
                        coachProfile = profile,
                        coachForm = CoachFormState(
                            displayName = profile.displayName,
                            bio = profile.bio.orEmpty(),
                            specialties = profile.specialties.orEmpty(),
                            yearsExperience = profile.yearsExperience?.toString().orEmpty(),
                            trainingPhilosophy = profile.trainingPhilosophy.orEmpty(),
                            coachingDescription = profile.coachingDescription.orEmpty(),
                            languages = profile.languages.orEmpty(),
                            certifications = profile.certifications.orEmpty(),
                            socialLinks = profile.socialLinks.orEmpty(),
                            isPublic = profile.isPublic,
                            isAcceptingClients = profile.isAcceptingClients
                        )
                    )
                }
            }
            .onFailure {
                // If coach profile fails or not set up, keep coachProfile null or state quiet
            }
    }

    private suspend fun loadActiveClientsInternal() {
        coachClientRepository.loadOverview()
            .onSuccess { overview ->
                val clients = overview.relationships.filter {
                    it.status == "ACTIVE" && it.coachId == overview.currentUserId
                }
                _uiState.update { it.copy(activeClients = clients) }
            }
    }

    fun loadCoachProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadCoachProfileInternal()
            loadActiveClientsInternal()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun terminateRelationship(relationshipId: Long, onCompleted: ((Boolean) -> Unit)? = null) {
        if (relationshipId in _uiState.value.busyRelationshipIds) return
        _uiState.update {
            it.copy(
                busyRelationshipIds = it.busyRelationshipIds + relationshipId,
                feedback = null
            )
        }
        viewModelScope.launch {
            coachClientRepository.terminateRelationship(relationshipId)
                .onSuccess {
                    loadClientProfileInternal()
                    loadCoachProfileInternal()
                    loadActiveClientsInternal()
                    _uiState.update { it.copy(feedback = "Relationship ended.") }
                    onCompleted?.invoke(true)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.localizedMessage ?: "Failed to end relationship") }
                    onCompleted?.invoke(false)
                }
            _uiState.update {
                it.copy(busyRelationshipIds = it.busyRelationshipIds - relationshipId)
            }
        }
    }

    // Client Profile Editing
    fun startEditingClient() {
        val current = _uiState.value.clientProfile
        _uiState.update {
            it.copy(
                isEditingClient = true,
                clientFormError = null,
                clientForm = if (current != null) {
                    ClientFormState(
                        displayName = current.displayName,
                        bio = current.bio.orEmpty(),
                        experienceLevel = current.experienceLevel ?: "INTERMEDIATE",
                        trainingExperience = current.trainingExperience.orEmpty(),
                        primaryGoals = current.primaryGoals.orEmpty(),
                        trainingInterests = current.trainingInterests.orEmpty(),
                        preferredFrequency = current.preferredFrequency.orEmpty(),
                        coachingLanguage = current.coachingLanguage.orEmpty()
                    )
                } else it.clientForm
            )
        }
    }

    fun cancelEditingClient() {
        _uiState.update { it.copy(isEditingClient = false, clientFormError = null) }
    }

    fun updateClientFormField(updater: (ClientFormState) -> ClientFormState) {
        _uiState.update { it.copy(clientForm = updater(it.clientForm), clientFormError = null) }
    }

    fun saveClientProfile() {
        val form = _uiState.value.clientForm
        if (form.displayName.length > 100) {
            _uiState.update { it.copy(clientFormError = "Display name cannot exceed 100 characters") }
            return
        }
        if (form.bio.length > 500) {
            _uiState.update { it.copy(clientFormError = "Bio cannot exceed 500 characters") }
            return
        }

        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, clientFormError = null, feedback = null) }
            val request = ClientProfileRequestDto(
                displayName = form.displayName.ifBlank { null },
                bio = form.bio.ifBlank { null },
                experienceLevel = form.experienceLevel.ifBlank { null },
                trainingExperience = form.trainingExperience.ifBlank { null },
                primaryGoals = form.primaryGoals.ifBlank { null },
                trainingInterests = form.trainingInterests.ifBlank { null },
                preferredFrequency = form.preferredFrequency.ifBlank { null },
                coachingLanguage = form.coachingLanguage.ifBlank { null }
            )

            profileRepository.updateMyClientProfile(request)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditingClient = false,
                            clientProfile = updated,
                            feedback = "Client profile updated successfully."
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            clientFormError = throwable.localizedMessage ?: "Failed to save client profile"
                        )
                    }
                }
        }
    }

    // Coach Profile Editing
    fun startEditingCoach() {
        val current = _uiState.value.coachProfile
        _uiState.update {
            it.copy(
                isEditingCoach = true,
                coachFormError = null,
                coachForm = if (current != null) {
                    CoachFormState(
                        displayName = current.displayName,
                        bio = current.bio.orEmpty(),
                        specialties = current.specialties.orEmpty(),
                        yearsExperience = current.yearsExperience?.toString().orEmpty(),
                        trainingPhilosophy = current.trainingPhilosophy.orEmpty(),
                        coachingDescription = current.coachingDescription.orEmpty(),
                        languages = current.languages.orEmpty(),
                        certifications = current.certifications.orEmpty(),
                        socialLinks = current.socialLinks.orEmpty(),
                        isPublic = current.isPublic,
                        isAcceptingClients = current.isAcceptingClients
                    )
                } else it.coachForm
            )
        }
    }

    fun cancelEditingCoach() {
        _uiState.update { it.copy(isEditingCoach = false, coachFormError = null) }
    }

    fun updateCoachFormField(updater: (CoachFormState) -> CoachFormState) {
        _uiState.update { it.copy(coachForm = updater(it.coachForm), coachFormError = null) }
    }

    fun saveCoachProfile() {
        val form = _uiState.value.coachForm
        if (form.displayName.length > 100) {
            _uiState.update { it.copy(coachFormError = "Display name cannot exceed 100 characters") }
            return
        }
        if (form.bio.length > 500) {
            _uiState.update { it.copy(coachFormError = "Bio cannot exceed 500 characters") }
            return
        }
        val parsedYears = if (form.yearsExperience.isNotBlank()) {
            form.yearsExperience.toIntOrNull().takeIf { it != null && it >= 0 } ?: run {
                _uiState.update { it.copy(coachFormError = "Years of experience must be a non-negative number") }
                return
            }
        } else null

        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, coachFormError = null, feedback = null) }
            val request = CoachProfileRequestDto(
                displayName = form.displayName.ifBlank { null },
                bio = form.bio.ifBlank { null },
                specialties = form.specialties.ifBlank { null },
                yearsExperience = parsedYears,
                trainingPhilosophy = form.trainingPhilosophy.ifBlank { null },
                coachingDescription = form.coachingDescription.ifBlank { null },
                languages = form.languages.ifBlank { null },
                certifications = form.certifications.ifBlank { null },
                socialLinks = form.socialLinks.ifBlank { null },
                isPublic = form.isPublic,
                isAcceptingClients = form.isAcceptingClients
            )

            profileRepository.updateMyCoachProfile(request)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditingCoach = false,
                            coachProfile = updated,
                            feedback = "Coach profile updated successfully."
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            coachFormError = throwable.localizedMessage ?: "Failed to save coach profile"
                        )
                    }
                }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }

    fun clearAvatarUploadState() {
        _uiState.update {
            it.copy(avatarUploadError = null, avatarUploadSucceeded = false)
        }
    }

    fun uploadAvatar(file: java.io.File) {
        if (_uiState.value.isUploadingAvatar) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingAvatar = true,
                    avatarUploadError = null,
                    avatarUploadSucceeded = false,
                    feedback = null
                )
            }
            try {
                val result = profileRepository.uploadAvatar(file)
                result.onSuccess { avatarUrl ->
                    val current = _uiState.value
                    val client = current.clientProfile?.copy(avatarUrl = avatarUrl)
                    val coach = current.coachProfile?.copy(avatarUrl = avatarUrl)
                    client?.let { profile ->
                        authRepository.updateAccountProfile(
                            userId = profile.userId,
                            fullName = profile.displayName.ifBlank { profile.fullName },
                            avatarUrl = avatarUrl
                        )
                    } ?: coach?.let { profile ->
                        authRepository.updateAccountProfile(
                            userId = profile.userId,
                            fullName = profile.displayName.ifBlank { profile.fullName },
                            avatarUrl = avatarUrl
                        )
                    }
                    _uiState.update {
                        it.copy(
                            clientProfile = client,
                            coachProfile = coach,
                            isUploadingAvatar = false,
                            avatarUploadSucceeded = true,
                            feedback = "Profile avatar updated."
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            avatarUploadError = throwable.localizedMessage ?: "Avatar upload failed."
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        avatarUploadError = exception.localizedMessage ?: "Avatar upload failed."
                    )
                }
            } finally {
                file.delete()
            }
        }
    }

    fun startChat(otherUserId: Long, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyChatUserIds = it.busyChatUserIds + otherUserId) }
            chatRepository.createOrGetConversation(otherUserId)
                .onSuccess { conversation ->
                    _uiState.update { it.copy(busyChatUserIds = it.busyChatUserIds - otherUserId) }
                    onReady(conversation.id)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            busyChatUserIds = it.busyChatUserIds - otherUserId,
                            error = throwable.localizedMessage ?: "Failed to start conversation"
                        )
                    }
                }
        }
    }

    fun logout() = authRepository.clearAuth()

    fun switchAccount() {
        authRepository.deactivateActiveSession()
        _uiState.value = ProfileUiState()
    }
}
