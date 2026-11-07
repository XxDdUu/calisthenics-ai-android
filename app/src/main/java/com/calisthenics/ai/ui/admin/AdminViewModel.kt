package com.calisthenics.ai.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminWorkoutPlan
import com.calisthenics.ai.domain.model.AdminWorkoutPlanDetail
import com.calisthenics.ai.domain.model.AdminWorkoutSession
import com.calisthenics.ai.domain.model.AdminWorkoutSessionDetail
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.repository.AdminRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AdminUiState(
    val isLoading: Boolean = false,
    val isUpdatingRole: Boolean = false,
    val isSavingExercise: Boolean = false,
    val isDeletingExercise: Boolean = false,
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
    val exercise: AdminExercise? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

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
    }
}
