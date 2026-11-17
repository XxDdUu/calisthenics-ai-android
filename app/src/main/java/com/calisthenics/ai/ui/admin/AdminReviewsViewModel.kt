package com.calisthenics.ai.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminReview
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.domain.repository.ReviewRepository
import com.calisthenics.ai.ui.common.FeatureError
import com.calisthenics.ai.ui.common.toFeatureError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminReviewsUiState(
    val statusFilter: ReviewModerationStatus? = null,
    val page: AdminPage<AdminReview>? = null,
    val isLoading: Boolean = false,
    val error: FeatureError? = null,
    val detail: AdminReview? = null,
    val isDetailLoading: Boolean = false,
    val detailError: FeatureError? = null,
    val isModerating: Boolean = false,
    val moderationError: FeatureError? = null,
    val messageRes: Int? = null
)

@HiltViewModel
class AdminReviewsViewModel @Inject constructor(
    private val repository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReviewsUiState())
    val uiState: StateFlow<AdminReviewsUiState> = _uiState.asStateFlow()

    fun setFilter(status: ReviewModerationStatus?) {
        if (_uiState.value.statusFilter == status && _uiState.value.page != null) return
        _uiState.update { it.copy(statusFilter = status) }
        loadList(0)
    }

    fun loadList(page: Int = _uiState.value.page?.page ?: 0) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getAdminReviews(_uiState.value.statusFilter, null, page.coerceAtLeast(0), PAGE_SIZE)
                .onSuccess { result -> _uiState.update { it.copy(isLoading = false, page = result) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toFeatureError()) } }
        }
    }

    fun loadDetail(id: Long) {
        _uiState.update { it.copy(isDetailLoading = true, detailError = null, detail = it.detail?.takeIf { d -> d.id == id }) }
        viewModelScope.launch {
            repository.getAdminReview(id)
                .onSuccess { detail -> _uiState.update { it.copy(isDetailLoading = false, detail = detail) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isDetailLoading = false, detailError = e.toFeatureError(notFound = R.string.admin_review_not_found)) }
                }
        }
    }

    fun moderate(id: Long, status: ReviewModerationStatus, reason: String) {
        if (_uiState.value.isModerating) return
        if (status == ReviewModerationStatus.HIDDEN && reason.isBlank()) {
            _uiState.update { it.copy(moderationError = FeatureError(res = R.string.admin_review_reason_required)) }
            return
        }
        _uiState.update { it.copy(isModerating = true, moderationError = null) }
        viewModelScope.launch {
            repository.moderateReview(id, status, reason.takeIf { status == ReviewModerationStatus.HIDDEN })
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isModerating = false,
                            detail = detail,
                            messageRes = if (status == ReviewModerationStatus.HIDDEN) R.string.admin_review_hidden_done else R.string.admin_review_restored_done
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isModerating = false, moderationError = e.toFeatureError()) } }
        }
    }

    fun clearModerationError() = _uiState.update { it.copy(moderationError = null) }
    fun clearMessage() = _uiState.update { it.copy(messageRes = null) }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
