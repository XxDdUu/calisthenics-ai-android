package com.calisthenics.ai.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MAX_REVIEW_COMMENT_LENGTH
import com.calisthenics.ai.domain.model.MyReviewStatus
import com.calisthenics.ai.domain.model.RatingSummary
import com.calisthenics.ai.domain.model.ReviewDraft
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

data class CoachReviewsUiState(
    val coachId: Long? = null,
    val summary: RatingSummary? = null,
    val isSummaryLoading: Boolean = false,
    val summaryError: FeatureError? = null,
    val reviews: List<CoachReview> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
    val isReviewsLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val reviewsError: FeatureError? = null,
    val myStatus: MyReviewStatus? = null,
    val isSheetOpen: Boolean = false,
    val draft: ReviewDraft = ReviewDraft(),
    val isSubmitting: Boolean = false,
    val submitError: FeatureError? = null,
    val successMessageRes: Int? = null
) {
    val canRate: Boolean get() = myStatus?.eligible == true
    val hasMyReview: Boolean get() = myStatus?.review != null
    val canSubmit: Boolean get() = draft.isValid && !isSubmitting
}

@HiltViewModel
class CoachReviewsViewModel @Inject constructor(
    private val repository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachReviewsUiState())
    val uiState: StateFlow<CoachReviewsUiState> = _uiState.asStateFlow()

    fun load(coachId: Long, force: Boolean = false) {
        val current = _uiState.value
        if (!force && current.coachId == coachId && (current.summary != null || current.isSummaryLoading)) return
        _uiState.value = CoachReviewsUiState(coachId = coachId)
        refreshAll(coachId)
    }

    fun retry() {
        _uiState.value.coachId?.let(::refreshAll)
    }

    private fun refreshAll(coachId: Long) {
        loadSummary(coachId)
        loadFirstPage(coachId)
        loadMine(coachId)
    }

    private fun loadSummary(coachId: Long) {
        _uiState.update { it.copy(isSummaryLoading = true, summaryError = null) }
        viewModelScope.launch {
            repository.getSummary(coachId)
                .onSuccess { summary ->
                    _uiState.update { if (it.coachId != coachId) it else it.copy(isSummaryLoading = false, summary = summary) }
                }
                .onFailure { e ->
                    _uiState.update {
                        if (it.coachId != coachId) it else it.copy(isSummaryLoading = false, summaryError = e.toFeatureError())
                    }
                }
        }
    }

    private fun loadFirstPage(coachId: Long) {
        _uiState.update { it.copy(isReviewsLoading = true, reviewsError = null) }
        viewModelScope.launch {
            repository.getReviews(coachId, 0, PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update {
                        if (it.coachId != coachId) it else it.copy(
                            isReviewsLoading = false,
                            reviews = page.content,
                            page = page.page,
                            hasNext = page.hasNext
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        if (it.coachId != coachId) it else it.copy(isReviewsLoading = false, reviewsError = e.toFeatureError())
                    }
                }
        }
    }

    private fun loadMine(coachId: Long) {
        viewModelScope.launch {
            // Failure to resolve eligibility simply hides the "Rate coach" button.
            repository.getMyReview(coachId).onSuccess { status ->
                _uiState.update { if (it.coachId != coachId) it else it.copy(myStatus = status) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val coachId = state.coachId ?: return
        if (!state.hasNext || state.isLoadingMore || state.isReviewsLoading) return
        _uiState.update { it.copy(isLoadingMore = true, reviewsError = null) }
        viewModelScope.launch {
            repository.getReviews(coachId, state.page + 1, PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update {
                        if (it.coachId != coachId) it else it.copy(
                            isLoadingMore = false,
                            reviews = (it.reviews + page.content).distinctBy(CoachReview::id),
                            page = page.page,
                            hasNext = page.hasNext
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        if (it.coachId != coachId) it else it.copy(isLoadingMore = false, reviewsError = e.toFeatureError())
                    }
                }
        }
    }

    fun openSheet() {
        _uiState.update {
            if (!it.canRate) it else it.copy(
                isSheetOpen = true,
                draft = ReviewDraft.from(it.myStatus?.review),
                submitError = null
            )
        }
    }

    fun closeSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSheetOpen = false, submitError = null) }
    }

    fun setOverall(value: Int) = updateDraft { it.copy(overallRating = value.coerceIn(1, 5)) }

    /** Tapping the same value again clears an optional criterion. */
    fun setCriterion(criterion: ReviewCriterion, value: Int) = updateDraft { draft ->
        val newValue = if (criterion.read(draft) == value) null else value.coerceIn(1, 5)
        criterion.write(draft, newValue)
    }

    fun setComment(value: String) =
        updateDraft { it.copy(comment = value.take(MAX_REVIEW_COMMENT_LENGTH)) }

    private fun updateDraft(transform: (ReviewDraft) -> ReviewDraft) {
        _uiState.update { it.copy(draft = transform(it.draft), submitError = null) }
    }

    fun submit() {
        val state = _uiState.value
        val coachId = state.coachId ?: return
        if (!state.canSubmit) return
        val editing = state.hasMyReview
        _uiState.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = if (editing) repository.updateMyReview(coachId, state.draft)
            else repository.createReview(coachId, state.draft)
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSheetOpen = false,
                            successMessageRes = if (editing) R.string.review_updated else R.string.review_submitted
                        )
                    }
                    refreshAll(coachId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = e.toFeatureError(
                                badRequest = null,
                                forbidden = R.string.review_error_not_eligible,
                                conflict = R.string.review_error_already_reviewed,
                                tooManyRequests = R.string.review_error_edit_too_fast
                            )
                        )
                    }
                }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(successMessageRes = null) }
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}

enum class ReviewCriterion(
    val read: (ReviewDraft) -> Int?,
    val write: (ReviewDraft, Int?) -> ReviewDraft
) {
    COACHING_QUALITY({ it.coachingQuality }, { d, v -> d.copy(coachingQuality = v) }),
    COMMUNICATION({ it.communication }, { d, v -> d.copy(communication = v) }),
    PLAN_EFFECTIVENESS({ it.planEffectiveness }, { d, v -> d.copy(planEffectiveness = v) }),
    SUPPORT_RESPONSIVENESS({ it.supportResponsiveness }, { d, v -> d.copy(supportResponsiveness = v) })
}
