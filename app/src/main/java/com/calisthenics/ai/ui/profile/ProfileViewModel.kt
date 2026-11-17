package com.calisthenics.ai.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.calisthenics.ai.R
import com.calisthenics.ai.data.local.AccountSession
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
import com.calisthenics.ai.ui.media.MediaUploadManager
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
    val gender: Gender? = null,
    val trainingExperience: String = "",
    val primaryGoals: String = "",
    val trainingInterests: String = "",
    val preferredFrequency: String = "",
    val coachingLanguage: String = "",
    val heightCm: String = "",
    val weightKg: String = ""
)

data class CoachFormState(
    val displayName: String = "",
    val bio: String = "",
    val gender: Gender? = null,
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
    @StringRes val feedbackResId: Int? = null,
    val isEditingClient: Boolean = false,
    val isEditingCoach: Boolean = false,
    val isEditingGender: Boolean = false,
    val isSavingGender: Boolean = false,
    val genderError: String? = null,
    val clientForm: ClientFormState = ClientFormState(),
    val coachForm: CoachFormState = CoachFormState(),
    val clientFormError: String? = null,
    val coachFormError: String? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarUploadError: String? = null,
    val avatarUploadSucceeded: Boolean = false,
    val athleteMedia: List<ProfileMediaItem> = emptyList(),
    val coachMedia: List<ProfileMediaItem> = emptyList(),
    val isMediaLoading: Boolean = false,
    val isMediaUploading: Boolean = false,
    val mediaError: String? = null,
    val userUsage: UserUsage? = null,
    val isUsageLoading: Boolean = false,
    val usageError: String? = null,
    val isUsageOffline: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val coachClientRepository: CoachClientRepository,
    private val chatRepository: ChatRepository,
    private val mediaUploadManager: MediaUploadManager,
    private val analysisRepository: AnalysisRepository,
    private val genderSessionStore: GenderSessionStore = GenderSessionStore(),
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    constructor(
        authRepository: AuthRepository,
        profileRepository: ProfileRepository,
        coachClientRepository: CoachClientRepository,
        chatRepository: ChatRepository,
        mediaUploadManager: MediaUploadManager,
        genderSessionStore: GenderSessionStore = GenderSessionStore(),
        accountSessionManager: AccountSessionManager = AccountSessionManager()
    ) : this(
        authRepository = authRepository,
        profileRepository = profileRepository,
        coachClientRepository = coachClientRepository,
        chatRepository = chatRepository,
        mediaUploadManager = mediaUploadManager,
        analysisRepository = object : AnalysisRepository {
            override suspend fun getUserUsage(): Result<UserUsage> = Result.success(UserUsage())
            override suspend fun uploadVideoForAnalysis(
                videoFile: java.io.File,
                exerciseId: Long?,
                exerciseType: String?,
                idempotencyKey: String?,
                sideView: Boolean
            ): Result<com.calisthenics.ai.domain.model.AnalysisResult> = Result.failure(UnsupportedOperationException())
            override suspend fun getVideoAnalysisById(id: Long): Result<com.calisthenics.ai.domain.model.AnalysisResult> = Result.failure(UnsupportedOperationException())
            override suspend fun getVideoAnalysisKeypoints(id: Long): Result<com.calisthenics.ai.domain.model.VideoKeypointsDataModel> = Result.failure(UnsupportedOperationException())
            override suspend fun getVideoAnalysisQuota(): Result<com.calisthenics.ai.domain.model.QuotaEntitlement> = Result.failure(UnsupportedOperationException())
            override suspend fun getUserEntitlements(): Result<com.calisthenics.ai.domain.model.QuotaEntitlement> = Result.failure(UnsupportedOperationException())
            override suspend fun updateMembershipTier(tier: String): Result<com.calisthenics.ai.domain.model.QuotaEntitlement> = Result.failure(UnsupportedOperationException())
            override suspend fun getVideoAnalysisHistory(): Result<List<com.calisthenics.ai.domain.model.AnalysisResult>> = Result.success(emptyList())
            override suspend fun analyzeVideo(videoFile: java.io.File, exerciseId: Long?): Result<com.calisthenics.ai.domain.model.AnalysisResult> = Result.failure(UnsupportedOperationException())
            override suspend fun analyzeImage(imageFile: java.io.File, exerciseId: Long?): Result<com.calisthenics.ai.domain.model.AnalysisResult> = Result.failure(UnsupportedOperationException())
            override suspend fun getAnalysisHistory(): Result<List<com.calisthenics.ai.domain.model.AnalysisResult>> = Result.success(emptyList())
            override suspend fun getAnalysisById(id: Long): Result<com.calisthenics.ai.domain.model.AnalysisResult> = Result.failure(UnsupportedOperationException())
        },
        genderSessionStore = genderSessionStore,
        accountSessionManager = accountSessionManager
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            _uiState.value = ProfileUiState()
            analysisRepository.invalidateUserUsageCache()
        },
        onSessionActive = { load() }
    )

    init {
        sessionGuard.startObserving()
        viewModelScope.launch {
            genderSessionStore.state.collect { sessionState ->
                if (sessionState == null) return@collect
                _uiState.update { state ->
                    val activeId = state.clientProfile?.userId
                        ?: state.coachProfile?.userId
                        ?: authRepository.getUserId()
                    if (activeId != null && activeId != sessionState.userId) {
                        state
                    } else {
                        state.copy(
                            clientProfile = state.clientProfile?.copy(gender = sessionState.gender),
                            coachProfile = state.coachProfile?.copy(gender = sessionState.gender),
                            clientForm = if (state.isEditingClient) state.clientForm else state.clientForm.copy(gender = sessionState.gender),
                            coachForm = if (state.isEditingCoach) state.coachForm else state.coachForm.copy(gender = sessionState.gender)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            profileRepository.profileUpdateEvents.collect { updatedProfile ->
                _uiState.update { state ->
                    val activeId = state.clientProfile?.userId ?: authRepository.getUserId()
                    if (activeId != null && activeId != updatedProfile.userId) {
                        state
                    } else {
                        state.copy(
                            clientProfile = updatedProfile,
                            clientForm = if (state.isEditingClient) state.clientForm else ClientFormState(
                                displayName = updatedProfile.displayName,
                                bio = updatedProfile.bio.orEmpty(),
                                experienceLevel = updatedProfile.experienceLevel ?: "INTERMEDIATE",
                                gender = updatedProfile.gender,
                                trainingExperience = updatedProfile.trainingExperience.orEmpty(),
                                primaryGoals = updatedProfile.primaryGoals.orEmpty(),
                                trainingInterests = updatedProfile.trainingInterests.orEmpty(),
                                preferredFrequency = updatedProfile.preferredFrequency.orEmpty(),
                                coachingLanguage = updatedProfile.coachingLanguage.orEmpty(),
                                heightCm = updatedProfile.heightCm?.toString().orEmpty(),
                                weightKg = updatedProfile.weightKg?.toString().orEmpty()
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            analysisRepository.usageUpdateEvents.collect { updatedUsage ->
                if (updatedUsage == null) {
                    loadUserUsage(forceRefresh = true)
                } else {
                    _uiState.update {
                        it.copy(
                            userUsage = updatedUsage,
                            isUsageLoading = false,
                            usageError = null,
                            isUsageOffline = false
                        )
                    }
                }
            }
        }
        load()
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(activeTab = tab, feedback = null, feedbackResId = null, error = null) }
        if (tab == ProfileTab.COACH && _uiState.value.coachProfile == null) {
            loadCoachProfile()
        }
    }

    fun load() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null, feedback = null) }
            loadClientProfileInternal(captured)
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped
            loadCoachProfileInternal(captured)
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped
            loadActiveClientsInternal(captured)
            if (sessionGuard.isCurrentSession(captured)) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        loadUserUsage()
    }

    fun loadUserUsage(forceRefresh: Boolean = false) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isUsageLoading = true, usageError = null) }
            val result = analysisRepository.getUserUsage()
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped

            result.fold(
                onSuccess = { usage ->
                    _uiState.update {
                        it.copy(
                            userUsage = usage,
                            isUsageLoading = false,
                            usageError = null,
                            isUsageOffline = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        val cached = current.userUsage ?: analysisRepository.getCachedUserUsage()
                        if (cached != null) {
                            current.copy(
                                userUsage = cached,
                                isUsageLoading = false,
                                isUsageOffline = true,
                                usageError = null
                            )
                        } else {
                            current.copy(
                                isUsageLoading = false,
                                isUsageOffline = false,
                                usageError = error.message ?: "Failed to load usage"
                            )
                        }
                    }
                }
            )
        }
    }

    private suspend fun loadClientProfileInternal(captured: AccountSession = sessionGuard.currentSession()) {
        profileRepository.getMyClientProfile()
            .onSuccess { profile ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                authRepository.updateAccountProfile(
                    userId = profile.userId,
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
                            gender = profile.gender,
                            trainingExperience = profile.trainingExperience.orEmpty(),
                            primaryGoals = profile.primaryGoals.orEmpty(),
                            trainingInterests = profile.trainingInterests.orEmpty(),
                            preferredFrequency = profile.preferredFrequency.orEmpty(),
                            coachingLanguage = profile.coachingLanguage.orEmpty(),
                            heightCm = profile.heightCm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty(),
                            weightKg = profile.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()
                        )
                    )
                }
                loadAthleteMediaInternal(profile.userId, captured)
            }
            .onFailure { throwable ->
                if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                _uiState.update { it.copy(error = throwable.localizedMessage ?: "Could not load client profile") }
            }
    }

    private suspend fun loadCoachProfileInternal(captured: AccountSession = sessionGuard.currentSession()) {
        profileRepository.getMyCoachProfile()
            .onSuccess { profile ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                authRepository.updateAccountProfile(
                    userId = profile.userId,
                    fullName = profile.displayName.ifBlank { profile.fullName },
                    avatarUrl = profile.avatarUrl
                )
                _uiState.update {
                    it.copy(
                        coachProfile = profile,
                        coachForm = CoachFormState(
                            displayName = profile.displayName,
                            bio = profile.bio.orEmpty(),
                            gender = profile.gender,
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
                loadCoachMediaInternal(profile.userId, captured)
            }
            .onFailure {
                // If coach profile fails or not set up, keep coachProfile null or state quiet
            }
    }

    private suspend fun loadActiveClientsInternal(captured: AccountSession = sessionGuard.currentSession()) {
        coachClientRepository.loadOverview()
            .onSuccess { overview ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                val clients = overview.relationships.filter {
                    it.status == "ACTIVE" && it.coachId == overview.currentUserId
                }
                _uiState.update { it.copy(activeClients = clients) }
            }
    }

    private suspend fun loadAthleteMediaInternal(userId: Long, captured: AccountSession = sessionGuard.currentSession()) {
        profileRepository.getProfileMedia(userId, ProfileType.ATHLETE)
            .onSuccess { list ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update { it.copy(athleteMedia = list) }
            }
    }

    private suspend fun loadCoachMediaInternal(userId: Long, captured: AccountSession = sessionGuard.currentSession()) {
        profileRepository.getProfileMedia(userId, ProfileType.COACH)
            .onSuccess { list ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update { it.copy(coachMedia = list) }
            }
    }

    fun loadMedia(profileType: ProfileType) {
        val userId = when (profileType) {
            ProfileType.ATHLETE -> _uiState.value.clientProfile?.userId
            ProfileType.COACH -> _uiState.value.coachProfile?.userId
        } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isMediaLoading = true) }
            profileRepository.getProfileMedia(userId, profileType)
                .onSuccess { list ->
                    _uiState.update { state ->
                        if (profileType == ProfileType.ATHLETE) {
                            state.copy(athleteMedia = list, isMediaLoading = false)
                        } else {
                            state.copy(coachMedia = list, isMediaLoading = false)
                        }
                    }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isMediaLoading = false, mediaError = t.localizedMessage) }
                }
        }
    }

    fun uploadMedia(uris: List<Uri>, profileType: ProfileType) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMediaUploading = true, mediaError = null) }
            for (uri in uris) {
                mediaUploadManager.uploadProfileMedia(uri, profileType)
                    .onFailure { t ->
                        _uiState.update { it.copy(mediaError = t.localizedMessage) }
                    }
            }
            loadMedia(profileType)
            _uiState.update { it.copy(isMediaUploading = false) }
        }
    }

    fun deleteMedia(mediaId: Long, profileType: ProfileType) {
        viewModelScope.launch {
            profileRepository.deleteProfileMedia(mediaId)
                .onSuccess {
                    loadMedia(profileType)
                }
                .onFailure { t ->
                    _uiState.update { it.copy(mediaError = t.localizedMessage) }
                }
        }
    }

    fun reorderMedia(mediaIds: List<Long>, profileType: ProfileType) {
        viewModelScope.launch {
            profileRepository.reorderProfileMedia(profileType, mediaIds)
                .onSuccess { list ->
                    _uiState.update { state ->
                        if (profileType == ProfileType.ATHLETE) {
                            state.copy(athleteMedia = list)
                        } else {
                            state.copy(coachMedia = list)
                        }
                    }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(mediaError = t.localizedMessage) }
                }
        }
    }

    fun setPrimaryCover(mediaId: Long, profileType: ProfileType) {
        viewModelScope.launch {
            profileRepository.setProfileMediaCover(mediaId)
                .onSuccess { list ->
                    _uiState.update { state ->
                        if (profileType == ProfileType.ATHLETE) {
                            state.copy(athleteMedia = list)
                        } else {
                            state.copy(coachMedia = list)
                        }
                    }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(mediaError = t.localizedMessage) }
                }
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
                        gender = current.gender,
                        trainingExperience = current.trainingExperience.orEmpty(),
                        primaryGoals = current.primaryGoals.orEmpty(),
                        trainingInterests = current.trainingInterests.orEmpty(),
                        preferredFrequency = current.preferredFrequency.orEmpty(),
                        coachingLanguage = current.coachingLanguage.orEmpty(),
                        heightCm = current.heightCm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty(),
                        weightKg = current.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()
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

        val heightVal = form.heightCm.trim().toDoubleOrNull()
        if (form.heightCm.isNotBlank() && (heightVal == null || heightVal < 50.0 || heightVal > 300.0)) {
            _uiState.update { it.copy(clientFormError = "Height must be between 50 and 300 cm") }
            return
        }

        val weightVal = form.weightKg.trim().toDoubleOrNull()
        if (form.weightKg.isNotBlank() && (weightVal == null || weightVal < 20.0 || weightVal > 500.0)) {
            _uiState.update { it.copy(clientFormError = "Weight must be between 20 and 500 kg") }
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
                coachingLanguage = form.coachingLanguage.ifBlank { null },
                heightCm = heightVal,
                weightKg = weightVal,
                gender = form.gender?.apiValue
            )

            profileRepository.updateMyClientProfile(request)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditingClient = false,
                            clientProfile = updated,
                            coachProfile = it.coachProfile?.copy(gender = updated.gender),
                            coachForm = it.coachForm.copy(gender = updated.gender),
                            feedback = "Client profile updated successfully.",
                            feedbackResId = null
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
                        gender = current.gender,
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
                isAcceptingClients = form.isAcceptingClients,
                gender = form.gender?.apiValue
            )

            profileRepository.updateMyCoachProfile(request)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditingCoach = false,
                            coachProfile = updated,
                            clientProfile = it.clientProfile?.copy(gender = updated.gender),
                            clientForm = it.clientForm.copy(gender = updated.gender),
                            feedback = "Coach profile updated successfully.",
                            feedbackResId = null
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

    fun startEditingGender() {
        _uiState.update { it.copy(isEditingGender = true, genderError = null) }
    }

    fun cancelEditingGender() {
        if (_uiState.value.isSavingGender) return
        _uiState.update { it.copy(isEditingGender = false, genderError = null) }
    }

    fun saveGender(gender: Gender) {
        if (_uiState.value.isSavingGender) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGender = true, genderError = null, feedback = null, feedbackResId = null) }
            authRepository.updateGender(gender)
                .onSuccess { updated ->
                    val confirmedGender = updated.gender ?: gender
                    _uiState.update { state ->
                        state.copy(
                            isSavingGender = false,
                            isEditingGender = false,
                            genderError = null,
                            clientProfile = state.clientProfile?.copy(gender = confirmedGender),
                            coachProfile = state.coachProfile?.copy(gender = confirmedGender),
                            clientForm = state.clientForm.copy(gender = confirmedGender),
                            coachForm = state.coachForm.copy(gender = confirmedGender),
                            feedback = null,
                            feedbackResId = R.string.gender_updated_success
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSavingGender = false,
                            genderError = NetworkErrorParser.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedback = null, feedbackResId = null) }
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

    fun logout() {
        sessionGuard.cancelActiveSessionWork()
        chatRepository.disconnectWebSocket()
        authRepository.clearAuth()
        _uiState.value = ProfileUiState()
    }

    fun switchAccount() {
        sessionGuard.cancelActiveSessionWork()
        chatRepository.disconnectWebSocket()
        authRepository.deactivateActiveSession()
        _uiState.value = ProfileUiState()
    }
}
