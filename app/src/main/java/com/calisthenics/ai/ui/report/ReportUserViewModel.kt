package com.calisthenics.ai.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.MAX_REPORT_DESCRIPTION_LENGTH
import com.calisthenics.ai.domain.model.MAX_REPORT_EVIDENCE
import com.calisthenics.ai.domain.model.ReportReason
import com.calisthenics.ai.domain.model.UserReportDraft
import com.calisthenics.ai.domain.repository.ReportRepository
import com.calisthenics.ai.ui.common.FeatureError
import com.calisthenics.ai.ui.common.toFeatureError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EvidenceItem(
    val id: String,
    /** Local file path used for the thumbnail preview. */
    val previewPath: String,
    val url: String? = null,
    val isUploading: Boolean = false,
    val failed: Boolean = false
)

data class ReportUserUiState(
    val userId: Long? = null,
    val reason: ReportReason? = null,
    val description: String = "",
    val evidence: List<EvidenceItem> = emptyList(),
    val showConfirm: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: FeatureError? = null,
    val uploadError: FeatureError? = null,
    val submitted: Boolean = false
) {
    val isUploading: Boolean get() = evidence.any { it.isUploading }
    val uploadedUrls: List<String> get() = evidence.mapNotNull { it.url }
    val canAddEvidence: Boolean get() = evidence.size < MAX_REPORT_EVIDENCE
    val draft: UserReportDraft
        get() = UserReportDraft(reason, description, uploadedUrls)
    val canSubmit: Boolean get() = draft.isValid && !isUploading && !isSubmitting && userId != null
}

@HiltViewModel
class ReportUserViewModel @Inject constructor(
    private val repository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUserUiState())
    val uiState: StateFlow<ReportUserUiState> = _uiState.asStateFlow()

    /** Starts a fresh report form for [userId] (no-op if already started for the same user). */
    fun start(userId: Long) {
        if (_uiState.value.userId == userId && !_uiState.value.submitted) return
        _uiState.value = ReportUserUiState(userId = userId)
    }

    fun setReason(reason: ReportReason) = _uiState.update { it.copy(reason = reason, error = null) }

    fun setDescription(value: String) =
        _uiState.update { it.copy(description = value.take(MAX_REPORT_DESCRIPTION_LENGTH), error = null) }

    fun requestConfirm() {
        if (_uiState.value.canSubmit) _uiState.update { it.copy(showConfirm = true) }
    }

    fun dismissConfirm() = _uiState.update { it.copy(showConfirm = false) }

    fun removeEvidence(id: String) = _uiState.update { s ->
        s.copy(evidence = s.evidence.filterNot { it.id == id })
    }

    fun addEvidence(file: File) {
        val state = _uiState.value
        if (!state.canAddEvidence) {
            file.delete()
            return
        }
        val item = EvidenceItem(id = file.name, previewPath = file.absolutePath, isUploading = true)
        _uiState.update { it.copy(evidence = it.evidence + item, uploadError = null) }
        viewModelScope.launch {
            repository.uploadEvidence(file)
                .onSuccess { url ->
                    _uiState.update { s ->
                        s.copy(evidence = s.evidence.map { if (it.id == item.id) it.copy(url = url, isUploading = false) else it })
                    }
                }
                .onFailure { e ->
                    _uiState.update { s ->
                        s.copy(
                            evidence = s.evidence.filterNot { it.id == item.id },
                            uploadError = e.toFeatureError(tooManyRequests = R.string.report_error_too_many)
                        )
                    }
                }
        }
    }

    fun onEvidencePreparationFailed(message: String?) {
        _uiState.update { it.copy(uploadError = FeatureError(res = if (message == null) R.string.report_error_image else null, message = message)) }
    }

    fun submit() {
        val state = _uiState.value
        val userId = state.userId ?: return
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, showConfirm = false, error = null) }
        viewModelScope.launch {
            repository.reportUser(userId, state.draft)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, submitted = true) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.toFeatureError(
                                badRequest = null,
                                notFound = R.string.report_error_not_found,
                                conflict = R.string.report_error_duplicate,
                                tooManyRequests = R.string.report_error_too_many
                            )
                        )
                    }
                }
        }
    }

    fun reset() {
        _uiState.value = ReportUserUiState()
    }
}
