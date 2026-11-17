package com.calisthenics.ai.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.AdminAiConversationSummary
import com.calisthenics.ai.domain.model.AdminAiMessage
import com.calisthenics.ai.domain.model.AdminAiRecommendation
import com.calisthenics.ai.domain.model.AdminCoachClientStatistics
import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminGlobalUserStatistics
import com.calisthenics.ai.domain.model.AdminGlobalUserSummary
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminPendingConfirmation
import com.calisthenics.ai.domain.model.AdminPlatformEngagement
import com.calisthenics.ai.domain.model.GettingStartedCampaignAnalytics
import com.calisthenics.ai.domain.model.GettingStartedCampaignSummary
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.GettingStartedProgramPreview
import com.calisthenics.ai.domain.model.AdminPlatformOverview
import com.calisthenics.ai.domain.model.AdminTimeRange
import com.calisthenics.ai.domain.model.AdminToolExecution
import com.calisthenics.ai.domain.model.AdminTrainingAnalytics
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminUserDetailDossier
import com.calisthenics.ai.domain.model.AdminUserSegmentFilter
import com.calisthenics.ai.domain.model.AdminUserSegments
import com.calisthenics.ai.domain.model.AdminWorkoutPlan
import com.calisthenics.ai.domain.model.AdminWorkoutPlanDetail
import com.calisthenics.ai.domain.model.AdminWorkoutSession
import com.calisthenics.ai.domain.model.AdminWorkoutSessionDetail
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.repository.AdminRepository
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AdminUiState(
    val isLoading: Boolean = false,
    val isUpdatingRole: Boolean = false,
    val isSavingExercise: Boolean = false,
    val isDeletingExercise: Boolean = false,
    val isSendingAiMessage: Boolean = false,
    val isResolvingConfirmation: Boolean = false,
    val isGeneratingRecommendation: Boolean = false,
    val isReviewingRecommendation: Boolean = false,
    val isLoadingGlobalUsers: Boolean = false,
    val isLoadingUserDetail: Boolean = false,
    val aiGlobalUserStatistics: AdminGlobalUserStatistics? = null,
    val aiCoachClientStatistics: AdminCoachClientStatistics? = null,
    val aiPlatformEngagement: AdminPlatformEngagement? = null,
    val aiUserSegments: AdminUserSegments? = null,
    val aiMatchedChatUsers: List<AdminGlobalUserSummary> = emptyList(),
    val aiLastExecutedTools: List<AdminToolExecution> = emptyList(),
    val aiLastTimeRangeUsed: AdminTimeRange? = null,
    val aiMissingDataNotes: List<String> = emptyList(),
    val aiSegmentFilter: AdminUserSegmentFilter = AdminUserSegmentFilter(),
    val inspectedUserDetail: AdminUserDetailDossier? = null,
    val error: String? = null,
    val authorizationErrorCode: Int? = null,
    val message: String? = null,
    val dashboard: AdminDashboard? = null,
    val userPage: AdminPage<AdminUser>? = null,
    val user: AdminUser? = null,
    val planPage: AdminPage<AdminWorkoutPlan>? = null,
    val plan: AdminWorkoutPlanDetail? = null,
    val sessionPage: AdminPage<AdminWorkoutSession>? = null,
    val session: AdminWorkoutSessionDetail? = null,
    val exercisePage: AdminPage<AdminExercise>? = null,
    val exercise: AdminExercise? = null,
    val aiConversations: List<AdminAiConversationSummary> = emptyList(),
    val activeAiConversationId: Long? = null,
    val aiMessages: List<AdminAiMessage> = emptyList(),
    val aiSuggestedFollowUps: List<String> = DEFAULT_ADMIN_AI_PROMPTS,
    val aiAnalyticsRange: String = "LAST_30_DAYS",
    val aiPlatformOverview: AdminPlatformOverview? = null,
    val aiTrainingAnalytics: AdminTrainingAnalytics? = null,
    val aiRecommendations: List<AdminAiRecommendation> = emptyList(),
    val aiGettingStartedProgram: GettingStartedProgram? = null,
    val aiGettingStartedPreview: GettingStartedProgramPreview? = null,
    val aiActiveCampaigns: List<GettingStartedCampaignSummary> = emptyList(),
    val aiCampaignAnalytics: GettingStartedCampaignAnalytics? = null,
    val aiPendingConfirmation: AdminPendingConfirmation? = null,
    val proSearchQuery: String = "",
    val proUsersPage: AdminPage<AdminUser>? = null,
    val isSearchingProUsers: Boolean = false,
    val proSearchError: String? = null,
    val updatingProUserId: Long? = null,
    val proActionMessage: String? = null,
    val proActionError: String? = null
) {
    companion object {
        val DEFAULT_ADMIN_AI_PROMPTS = listOf(
            "Grant PRO to user@example.com",
            "Revoke PRO from user@example.com",
            "Create a beginner-friendly 3-day full-body Getting Started program.",
            "Show me the currently active Getting Started campaign.",
            "Show Getting Started campaign analytics and completion rates.",
            "Summarize platform activity this month.",
            "Analyze user training trends."
        )
    }
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    constructor(
        adminRepository: AdminRepository,
        authRepository: AuthRepository
    ) : this(adminRepository, authRepository, NoOpAnalysisRepository)

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    private var currentUserSearch: String? = null
    private var currentUserRole: String? = null
    private var currentUserPage = 0

    private var currentExerciseSearch: String? = null
    private var currentExerciseCategory: String? = null
    private var currentExerciseDifficulty: String? = null
    private var currentExerciseActive: Boolean? = null
    private var currentExercisePage = 0

    fun loadDashboard() {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getDashboard()
                .onSuccess { dashboard ->
                    _uiState.update { it.copy(isLoading = false, dashboard = dashboard, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadUsers(page: Int = currentUserPage, search: String? = currentUserSearch, role: String? = currentUserRole) {
        currentUserPage = page
        currentUserSearch = search
        currentUserRole = role
        viewModelScope.launch {
            beginLoading()
            adminRepository.getUsers(page, PAGE_SIZE, search?.takeIf(String::isNotBlank), role)
                .onSuccess { users ->
                    _uiState.update { it.copy(isLoading = false, userPage = users, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getUser(userId)
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, user = user, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun updateUserRole(userId: Long, role: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingRole = true, error = null, message = null) }
            adminRepository.updateUserRole(userId, role)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isUpdatingRole = false,
                            user = user,
                            message = "User role updated.",
                            error = null,
                            authorizationErrorCode = null
                        )
                    }
                    if (_uiState.value.userPage != null) loadUsers()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingRole = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun loadWorkoutPlans(page: Int = 0) {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getWorkoutPlans(page, PAGE_SIZE)
                .onSuccess { plans ->
                    _uiState.update { it.copy(isLoading = false, planPage = plans, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadWorkoutPlan(planId: Long) {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getWorkoutPlan(planId)
                .onSuccess { plan ->
                    _uiState.update { it.copy(isLoading = false, plan = plan, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadWorkoutSessions(page: Int = 0) {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getWorkoutSessions(page, PAGE_SIZE)
                .onSuccess { sessions ->
                    _uiState.update { it.copy(isLoading = false, sessionPage = sessions, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadWorkoutSession(sessionId: Long) {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getWorkoutSession(sessionId)
                .onSuccess { session ->
                    _uiState.update { it.copy(isLoading = false, session = session, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadExercises(
        page: Int = currentExercisePage,
        search: String? = currentExerciseSearch,
        category: String? = currentExerciseCategory,
        difficulty: String? = currentExerciseDifficulty,
        active: Boolean? = currentExerciseActive
    ) {
        currentExercisePage = page
        currentExerciseSearch = search
        currentExerciseCategory = category
        currentExerciseDifficulty = difficulty
        currentExerciseActive = active
        viewModelScope.launch {
            beginLoading()
            adminRepository.getExercises(
                page = page,
                size = PAGE_SIZE,
                search = search?.takeIf(String::isNotBlank),
                category = category,
                difficulty = difficulty,
                active = active
            )
                .onSuccess { exercises ->
                    _uiState.update { it.copy(isLoading = false, exercisePage = exercises, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun loadExercise(exerciseId: Long) {
        viewModelScope.launch {
            beginLoading()
            adminRepository.getExercise(exerciseId)
                .onSuccess { exercise ->
                    _uiState.update { it.copy(isLoading = false, exercise = exercise, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun createExercise(
        name: String,
        code: String?,
        category: String,
        difficulty: String,
        description: String?,
        targetMuscles: String?,
        instructions: String?,
        imageUrl: String?,
        videoUrl: String?,
        equipment: String?,
        active: Boolean,
        mediaItems: List<ExerciseMedia> = emptyList(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingExercise = true, error = null, message = null) }
            adminRepository.createExercise(
                name = name,
                code = code,
                category = category,
                difficulty = difficulty,
                description = description,
                targetMuscles = targetMuscles,
                instructions = instructions,
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                equipment = equipment,
                active = active,
                mediaItems = mediaItems
            )
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSavingExercise = false,
                            message = "Exercise created successfully."
                        )
                    }
                    loadExercises(page = 0)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingExercise = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun updateExercise(
        exerciseId: Long,
        name: String,
        code: String?,
        category: String,
        difficulty: String,
        description: String?,
        targetMuscles: String?,
        instructions: String?,
        imageUrl: String?,
        videoUrl: String?,
        equipment: String?,
        active: Boolean?,
        mediaItems: List<ExerciseMedia> = emptyList(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingExercise = true, error = null, message = null) }
            adminRepository.updateExercise(
                exerciseId = exerciseId,
                name = name,
                code = code,
                category = category,
                difficulty = difficulty,
                description = description,
                targetMuscles = targetMuscles,
                instructions = instructions,
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                equipment = equipment,
                active = active,
                mediaItems = mediaItems
            )
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isSavingExercise = false,
                            exercise = updated,
                            message = "Exercise updated successfully."
                        )
                    }
                    loadExercises()
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingExercise = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun updateExerciseStatus(exerciseId: Long, active: Boolean) {
        viewModelScope.launch {
            adminRepository.updateExerciseStatus(exerciseId, active)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        val updatedPage = state.exercisePage?.let { page ->
                            page.copy(content = page.content.map { if (it.id == exerciseId) updated else it })
                        }
                        state.copy(
                            exercise = if (state.exercise?.id == exerciseId) updated else state.exercise,
                            exercisePage = updatedPage,
                            message = "Exercise status updated to ${if (active) "Active" else "Inactive"}."
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun deleteExercise(
        exerciseId: Long,
        softDelete: Boolean,
        onSuccess: (AdminExerciseDeleteResult) -> Unit,
        onConflict: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingExercise = true, error = null, message = null) }
            adminRepository.deleteExercise(exerciseId, softDelete)
                .onSuccess { result ->
                    _uiState.update { state ->
                        val updatedPage = state.exercisePage?.let { page ->
                            if (result.softDeleted) {
                                page.copy(content = page.content.map {
                                    if (it.id == exerciseId) it.copy(active = false) else it
                                })
                            } else {
                                page.copy(
                                    content = page.content.filter { it.id != exerciseId },
                                    totalElements = page.totalElements - 1
                                )
                            }
                        }
                        state.copy(
                            isDeletingExercise = false,
                            exercisePage = updatedPage,
                            message = result.message
                        )
                    }
                    onSuccess(result)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isDeletingExercise = false) }
                    val message = NetworkErrorParser.toUserMessage(error)
                    if ((error as? HttpException)?.code() == 409) {
                        onConflict(message)
                    } else {
                        showError(error)
                    }
                }
        }
    }

    fun logout() {
        authRepository.clearAuth()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun loadAdminAiWorkspace(range: String = _uiState.value.aiAnalyticsRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, aiAnalyticsRange = range, error = null) }
            val conversationsResult = adminRepository.getAdminAiConversations()
            val overviewResult = adminRepository.getAdminAiPlatformOverview(range = range)
            val trainingResult = adminRepository.getAdminAiTrainingAnalytics(range = range)
            val recommendationsResult = adminRepository.getAdminAiRecommendations()

            val conversations = conversationsResult.getOrDefault(emptyList())
            val selectedId = _uiState.value.activeAiConversationId ?: conversations.firstOrNull()?.id
            val messages = if (selectedId != null) {
                adminRepository.getAdminAiConversationMessages(selectedId).getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val firstError = listOf(conversationsResult, overviewResult, trainingResult, recommendationsResult)
                .firstNotNullOfOrNull { it.exceptionOrNull() }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    aiConversations = conversations,
                    activeAiConversationId = selectedId,
                    aiMessages = messages,
                    aiPlatformOverview = overviewResult.getOrNull() ?: state.aiPlatformOverview,
                    aiTrainingAnalytics = trainingResult.getOrNull() ?: state.aiTrainingAnalytics,
                    aiRecommendations = recommendationsResult.getOrNull() ?: state.aiRecommendations,
                    error = firstError?.let(NetworkErrorParser::toUserMessage),
                    authorizationErrorCode = firstError?.let(::authorizationErrorCode)
                )
            }
        }
    }

    fun selectAdminAiConversation(conversationId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeAiConversationId = conversationId,
                    isLoading = true,
                    error = null,
                    aiGettingStartedPreview = null,
                    aiCampaignAnalytics = null,
                    aiPendingConfirmation = null
                )
            }
            adminRepository.getAdminAiConversationMessages(conversationId)
                .onSuccess { messages ->
                    _uiState.update { it.copy(isLoading = false, aiMessages = messages, error = null) }
                }
                .onFailure(::showError)
        }
    }

    fun createNewAdminAiConversation(title: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            adminRepository.createAdminAiConversation(title)
                .onSuccess { created ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            aiConversations = listOf(created) + state.aiConversations.filterNot { it.id == created.id },
                            activeAiConversationId = created.id,
                            aiMessages = emptyList(),
                            aiGettingStartedProgram = null,
                            aiGettingStartedPreview = null,
                            aiCampaignAnalytics = null,
                            aiPendingConfirmation = null,
                            message = "Started new Admin AI session."
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun sendAdminAiMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val currentConvId = _uiState.value.activeAiConversationId
        val currentRange = _uiState.value.aiAnalyticsRange

        val optimisticUserMsg = AdminAiMessage(
            id = -System.currentTimeMillis(),
            conversationId = currentConvId ?: 0L,
            role = "USER",
            content = trimmed,
            intent = null,
            createdAt = null
        )

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isSendingAiMessage = true,
                    error = null,
                    message = null,
                    aiMessages = state.aiMessages + optimisticUserMsg
                )
            }
            adminRepository.sendAdminAiChat(
                conversationId = currentConvId,
                message = trimmed,
                range = currentRange
            )
                .onSuccess { response ->
                    val conversations = adminRepository.getAdminAiConversations()
                        .getOrDefault(_uiState.value.aiConversations)
                    val allRecommendations = adminRepository.getAdminAiRecommendations()
                        .getOrDefault((response.recommendations + _uiState.value.aiRecommendations).distinctBy { it.id })
                    _uiState.update { state ->
                        val updatedMessages = state.aiMessages
                            .filterNot { it.id == optimisticUserMsg.id } +
                            listOfNotNull(response.userMessage, response.assistantMessage)
                        state.copy(
                            isSendingAiMessage = false,
                            activeAiConversationId = response.conversationId,
                            aiConversations = conversations,
                            aiMessages = updatedMessages,
                            aiSuggestedFollowUps = response.suggestedFollowUps.ifEmpty {
                                AdminUiState.DEFAULT_ADMIN_AI_PROMPTS
                            },
                            aiPlatformOverview = response.platformOverview ?: state.aiPlatformOverview,
                            aiTrainingAnalytics = response.trainingAnalytics ?: state.aiTrainingAnalytics,
                            aiRecommendations = allRecommendations,
                            aiGlobalUserStatistics = response.userStatistics ?: state.aiGlobalUserStatistics,
                            aiCoachClientStatistics = response.coachClientStatistics ?: state.aiCoachClientStatistics,
                            aiPlatformEngagement = response.platformEngagement ?: state.aiPlatformEngagement,
                            aiUserSegments = response.segmentAnalysis ?: state.aiUserSegments,
                            aiMatchedChatUsers = response.matchedUsers,
                            aiLastExecutedTools = response.executedTools,
                            aiLastTimeRangeUsed = response.timeRangeUsed,
                            aiMissingDataNotes = response.missingDataNotes,
                            inspectedUserDetail = response.inspectedUserDetail ?: state.inspectedUserDetail,
                            aiGettingStartedProgram = response.gettingStartedProgram ?: state.aiGettingStartedProgram,
                            aiGettingStartedPreview = response.gettingStartedPreview,
                            aiActiveCampaigns = if (response.activeCampaigns.isNotEmpty() ||
                                response.executedTools.any {
                                    it.toolName == "getActiveCampaigns" ||
                                        it.toolName == "publishProgram" ||
                                        it.toolName == "unpublishProgram"
                                }
                            ) {
                                response.activeCampaigns
                            } else {
                                state.aiActiveCampaigns
                            },
                            aiCampaignAnalytics = response.campaignAnalytics,
                            aiPendingConfirmation = response.pendingConfirmation
                        )
                    }
                    val updatedPro = response.updatedProUser
                    val proMutated = updatedPro != null && response.executedTools.any {
                        (it.toolName == "grantPro" || it.toolName == "revokePro") &&
                            it.status.equals("EXECUTED", ignoreCase = true)
                    }
                    if (proMutated) {
                        val safeUpdatedPro = requireNotNull(updatedPro)
                        analysisRepository.invalidateUserUsageCache()
                        val msg = if (safeUpdatedPro.adminGrantedPro) {
                            "Granted unlimited PRO access to ${safeUpdatedPro.fullName}."
                        } else if (safeUpdatedPro.paidPro) {
                            "Revoked admin-granted PRO for ${safeUpdatedPro.fullName}. Paid subscription remains active."
                        } else {
                            "Revoked admin-granted PRO for ${safeUpdatedPro.fullName}."
                        }
                        applyUpdatedProUser(safeUpdatedPro, msg)
                        refreshProSearchAndDetailAfterChange(safeUpdatedPro.id)
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSendingAiMessage = false,
                            aiMessages = state.aiMessages.filterNot { it.id == optimisticUserMsg.id },
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    /** Approves or cancels the pending Admin AI Getting Started publish/unpublish confirmation. */
    fun resolvePendingConfirmation(approved: Boolean) {
        val pending = _uiState.value.aiPendingConfirmation ?: return
        val currentConvId = _uiState.value.activeAiConversationId

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingConfirmation = true, error = null, message = null) }
            adminRepository.resolveAdminAiConfirmation(
                confirmationToken = pending.confirmationToken,
                approved = approved,
                conversationId = currentConvId
            )
                .onSuccess { decision ->
                    val targetConvId = decision.conversationId ?: currentConvId
                    val refreshedMessages = if (targetConvId != null) {
                        adminRepository.getAdminAiConversationMessages(targetConvId)
                            .getOrNull()
                            ?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                    _uiState.update { state ->
                        val fallbackMessages = if (!decision.reply.isNullOrBlank()) {
                            state.aiMessages + AdminAiMessage(
                                id = -System.currentTimeMillis(),
                                conversationId = targetConvId ?: 0L,
                                role = "ASSISTANT",
                                content = decision.reply,
                                intent = decision.actionType,
                                createdAt = null
                            )
                        } else {
                            state.aiMessages
                        }
                        val isCampaignAction = decision.actionType.equals("PUBLISH_PROGRAM", ignoreCase = true) ||
                            decision.actionType.equals("UNPUBLISH_PROGRAM", ignoreCase = true)
                        val campaignsUpdated = isCampaignAction && (
                            decision.status.equals("CONFIRMED", ignoreCase = true) ||
                                decision.status.equals("EXECUTED", ignoreCase = true) ||
                                decision.execution?.status.equals("EXECUTED", ignoreCase = true) ||
                                decision.activeCampaigns.isNotEmpty()
                            )
                        state.copy(
                            isResolvingConfirmation = false,
                            activeAiConversationId = targetConvId ?: state.activeAiConversationId,
                            aiMessages = refreshedMessages ?: fallbackMessages,
                            aiPendingConfirmation = null,
                            aiGettingStartedProgram = decision.gettingStartedProgram ?: state.aiGettingStartedProgram,
                            aiActiveCampaigns = if (campaignsUpdated) decision.activeCampaigns else state.aiActiveCampaigns,
                            aiLastExecutedTools = listOfNotNull(decision.execution).ifEmpty { state.aiLastExecutedTools },
                            message = decision.reply
                        )
                    }
                    val updatedUser = decision.updatedUser
                    if (updatedUser != null && decision.status.equals("CONFIRMED", ignoreCase = true)) {
                        analysisRepository.invalidateUserUsageCache()
                        applyUpdatedProUser(
                            updatedUser = updatedUser,
                            successMessage = decision.reply ?: "Updated PRO access for ${updatedUser.fullName}."
                        )
                        refreshProSearchAndDetailAfterChange(updatedUser.id)
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isResolvingConfirmation = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun generateAdminRecommendations(
        type: String,
        targetSkill: String? = null,
        trainingLevel: String? = null,
        prompt: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingRecommendation = true, error = null, message = null) }
            adminRepository.createAdminAiRecommendations(
                type = type,
                targetSkill = targetSkill,
                trainingLevel = trainingLevel,
                prompt = prompt
            )
                .onSuccess { created ->
                    val refreshed = adminRepository.getAdminAiRecommendations().getOrDefault(
                        (created + _uiState.value.aiRecommendations).distinctBy { it.id }
                    )
                    _uiState.update { state ->
                        state.copy(
                            isGeneratingRecommendation = false,
                            aiRecommendations = refreshed,
                            message = "Generated ${created.size} recommendation(s) for administrator review."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingRecommendation = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun approveAdminRecommendation(
        id: Long,
        reviewNotes: String? = null,
        publishExercise: Boolean? = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewingRecommendation = true, error = null, message = null) }
            adminRepository.approveAdminAiRecommendation(id, reviewNotes, publishExercise)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isReviewingRecommendation = false,
                            aiRecommendations = state.aiRecommendations.map { if (it.id == id) updated else it },
                            message = "Recommendation approved (${updated.status})."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isReviewingRecommendation = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun rejectAdminRecommendation(id: Long, reviewNotes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewingRecommendation = true, error = null, message = null) }
            adminRepository.rejectAdminAiRecommendation(id, reviewNotes)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isReviewingRecommendation = false,
                            aiRecommendations = state.aiRecommendations.map { if (it.id == id) updated else it },
                            message = "Recommendation rejected."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isReviewingRecommendation = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun implementAdminRecommendation(id: Long, reviewNotes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewingRecommendation = true, error = null, message = null) }
            adminRepository.implementAdminAiRecommendation(id, reviewNotes)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isReviewingRecommendation = false,
                            aiRecommendations = state.aiRecommendations.map { if (it.id == id) updated else it },
                            message = "Recommendation implemented and confirmed."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isReviewingRecommendation = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun loadGlobalUserAnalytics(range: String = _uiState.value.aiAnalyticsRange) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingGlobalUsers = true,
                    aiAnalyticsRange = range,
                    error = null
                )
            }
            val statsResult = adminRepository.getGlobalUserStatistics(range = range)
            val coachingResult = adminRepository.getCoachClientStatistics(range = range)
            val engagementResult = adminRepository.getPlatformEngagement(range = range)
            val currentFilter = _uiState.value.aiSegmentFilter.copy(range = range)
            val segmentsResult = adminRepository.analyzeUserSegments(currentFilter)

            val firstFailure = listOf(statsResult, coachingResult, engagementResult, segmentsResult)
                .firstNotNullOfOrNull { it.exceptionOrNull() }

            _uiState.update { state ->
                state.copy(
                    isLoadingGlobalUsers = false,
                    aiGlobalUserStatistics = statsResult.getOrNull() ?: state.aiGlobalUserStatistics,
                    aiCoachClientStatistics = coachingResult.getOrNull() ?: state.aiCoachClientStatistics,
                    aiPlatformEngagement = engagementResult.getOrNull() ?: state.aiPlatformEngagement,
                    aiUserSegments = segmentsResult.getOrNull() ?: state.aiUserSegments,
                    aiSegmentFilter = currentFilter,
                    error = firstFailure?.let { NetworkErrorParser.toUserMessage(it) } ?: state.error,
                    authorizationErrorCode = firstFailure?.let { authorizationErrorCode(it) }
                )
            }
        }
    }

    fun analyzeUserSegment(filter: AdminUserSegmentFilter) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingGlobalUsers = true,
                    aiSegmentFilter = filter,
                    error = null
                )
            }
            adminRepository.analyzeUserSegments(filter)
                .onSuccess { segments ->
                    _uiState.update {
                        it.copy(
                            isLoadingGlobalUsers = false,
                            aiUserSegments = segments
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingGlobalUsers = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun inspectGlobalUser(userId: Long, includeEmail: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUserDetail = true, error = null) }
            adminRepository.getGlobalUserDetail(
                userId = userId,
                includeEmail = includeEmail,
                range = _uiState.value.aiAnalyticsRange
            )
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isLoadingUserDetail = false,
                            inspectedUserDetail = detail
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingUserDetail = false,
                            error = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun dismissInspectedUser() {
        _uiState.update { it.copy(inspectedUserDetail = null) }
    }

    fun updateProSearchQuery(query: String) {
        _uiState.update { it.copy(proSearchQuery = query) }
    }

    fun searchProUsers(
        query: String = _uiState.value.proSearchQuery,
        page: Int = 0
    ) {
        val normalizedQuery = query.trim().takeIf { it.isNotBlank() }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    proSearchQuery = query,
                    isSearchingProUsers = true,
                    proSearchError = null,
                    authorizationErrorCode = null
                )
            }
            adminRepository.getUsers(
                page = page,
                size = PRO_PAGE_SIZE,
                search = normalizedQuery,
                role = null
            )
                .onSuccess { resultPage ->
                    _uiState.update {
                        it.copy(
                            isSearchingProUsers = false,
                            proUsersPage = resultPage,
                            proSearchError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSearchingProUsers = false,
                            proSearchError = NetworkErrorParser.toUserMessage(error),
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun grantAdminPro(userId: Long, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingProUserId = userId,
                    proActionMessage = null,
                    proActionError = null,
                    error = null,
                    message = null,
                    authorizationErrorCode = null
                )
            }
            adminRepository.grantAdminPro(userId, reason)
                .onSuccess { updatedUser ->
                    analysisRepository.invalidateUserUsageCache()
                    applyUpdatedProUser(
                        updatedUser = updatedUser,
                        successMessage = "Granted unlimited PRO access to ${updatedUser.fullName}."
                    )
                    refreshProSearchAndDetailAfterChange(updatedUser.id)
                }
                .onFailure { error ->
                    val message = NetworkErrorParser.toUserMessage(error)
                    _uiState.update {
                        it.copy(
                            updatingProUserId = null,
                            proActionError = message,
                            error = if (it.user?.id == userId) message else it.error,
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun revokeAdminPro(userId: Long, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingProUserId = userId,
                    proActionMessage = null,
                    proActionError = null,
                    error = null,
                    message = null,
                    authorizationErrorCode = null
                )
            }
            adminRepository.revokeAdminPro(userId, reason)
                .onSuccess { updatedUser ->
                    analysisRepository.invalidateUserUsageCache()
                    val detailMessage = if (updatedUser.paidPro) {
                        "Revoked admin-granted PRO for ${updatedUser.fullName}. Paid subscription remains active."
                    } else {
                        "Revoked admin-granted PRO for ${updatedUser.fullName}."
                    }
                    applyUpdatedProUser(
                        updatedUser = updatedUser,
                        successMessage = detailMessage
                    )
                    refreshProSearchAndDetailAfterChange(updatedUser.id)
                }
                .onFailure { error ->
                    val message = NetworkErrorParser.toUserMessage(error)
                    _uiState.update {
                        it.copy(
                            updatingProUserId = null,
                            proActionError = message,
                            error = if (it.user?.id == userId) message else it.error,
                            authorizationErrorCode = authorizationErrorCode(error)
                        )
                    }
                }
        }
    }

    fun clearProActionFeedback() {
        _uiState.update { it.copy(proActionMessage = null, proActionError = null) }
    }

    private fun applyUpdatedProUser(updatedUser: AdminUser, successMessage: String) {
        _uiState.update { state ->
            state.copy(
                updatingProUserId = null,
                proActionMessage = successMessage,
                proActionError = null,
                message = if (state.user?.id == updatedUser.id) successMessage else state.message,
                user = if (state.user?.id == updatedUser.id) updatedUser else state.user,
                proUsersPage = state.proUsersPage?.let { page ->
                    page.copy(content = page.content.map { existing ->
                        if (existing.id == updatedUser.id) updatedUser else existing
                    })
                },
                userPage = state.userPage?.let { page ->
                    page.copy(content = page.content.map { existing ->
                        if (existing.id == updatedUser.id) updatedUser else existing
                    })
                },
                authorizationErrorCode = null
            )
        }
    }

    private suspend fun refreshProSearchAndDetailAfterChange(userId: Long) {
        val currentState = _uiState.value
        val currentPage = currentState.proUsersPage
        if (currentPage != null) {
            adminRepository.getUsers(
                page = currentPage.page,
                size = currentPage.size,
                search = currentState.proSearchQuery.trim().takeIf { it.isNotBlank() },
                role = null
            ).onSuccess { refreshedPage ->
                _uiState.update { it.copy(proUsersPage = refreshedPage) }
            }
        }
        if (currentState.user?.id == userId) {
            adminRepository.getUser(userId).onSuccess { refreshedUser ->
                _uiState.update { it.copy(user = refreshedUser) }
            }
        }
        if (currentState.userPage != null) {
            adminRepository.getUsers(
                page = currentUserPage,
                size = PAGE_SIZE,
                search = currentUserSearch?.takeIf(String::isNotBlank),
                role = currentUserRole
            ).onSuccess { refreshedUsers ->
                _uiState.update { it.copy(userPage = refreshedUsers) }
            }
        }
    }

    private fun beginLoading() {
        _uiState.update { it.copy(isLoading = true, error = null, authorizationErrorCode = null) }
    }

    private fun showError(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = NetworkErrorParser.toUserMessage(error),
                authorizationErrorCode = authorizationErrorCode(error)
            )
        }
    }

    private fun authorizationErrorCode(error: Throwable): Int? =
        (error as? HttpException)?.code()?.takeIf { it == 401 || it == 403 }

    private companion object {
        const val PAGE_SIZE = 20
        const val PRO_PAGE_SIZE = 8
    }

    private object NoOpAnalysisRepository : AnalysisRepository {
        private val _events = MutableSharedFlow<com.calisthenics.ai.domain.model.UserUsage?>(extraBufferCapacity = 1)
        override val usageUpdateEvents: SharedFlow<com.calisthenics.ai.domain.model.UserUsage?> = _events.asSharedFlow()
        override fun getCachedUserUsage(): com.calisthenics.ai.domain.model.UserUsage? = null
        override fun invalidateUserUsageCache() {}
        override suspend fun getUserUsage(): Result<com.calisthenics.ai.domain.model.UserUsage> =
            Result.failure(UnsupportedOperationException())
        override suspend fun uploadVideoForAnalysis(
            videoFile: java.io.File,
            exerciseId: Long?,
            exerciseType: String?,
            idempotencyKey: String?,
            sideView: Boolean
        ): Result<com.calisthenics.ai.domain.model.AnalysisResult> = Result.failure(UnsupportedOperationException())
        override suspend fun getVideoAnalysisById(id: Long): Result<com.calisthenics.ai.domain.model.AnalysisResult> =
            Result.failure(UnsupportedOperationException())
        override suspend fun getVideoAnalysisKeypoints(id: Long): Result<com.calisthenics.ai.domain.model.VideoKeypointsDataModel> =
            Result.failure(UnsupportedOperationException())
        override suspend fun getVideoAnalysisQuota(): Result<com.calisthenics.ai.domain.model.QuotaEntitlement> =
            Result.failure(UnsupportedOperationException())
        override suspend fun getUserEntitlements(): Result<com.calisthenics.ai.domain.model.QuotaEntitlement> =
            Result.failure(UnsupportedOperationException())
        override suspend fun updateMembershipTier(tier: String): Result<com.calisthenics.ai.domain.model.QuotaEntitlement> =
            Result.failure(UnsupportedOperationException())
        override suspend fun getVideoAnalysisHistory(): Result<List<com.calisthenics.ai.domain.model.AnalysisResult>> =
            Result.success(emptyList())
        override suspend fun analyzeVideo(videoFile: java.io.File, exerciseId: Long?): Result<com.calisthenics.ai.domain.model.AnalysisResult> =
            Result.failure(UnsupportedOperationException())
        override suspend fun analyzeImage(imageFile: java.io.File, exerciseId: Long?): Result<com.calisthenics.ai.domain.model.AnalysisResult> =
            Result.failure(UnsupportedOperationException())
        override suspend fun getAnalysisHistory(): Result<List<com.calisthenics.ai.domain.model.AnalysisResult>> =
            Result.success(emptyList())
        override suspend fun getAnalysisById(id: Long): Result<com.calisthenics.ai.domain.model.AnalysisResult> =
            Result.failure(UnsupportedOperationException())
    }
}
