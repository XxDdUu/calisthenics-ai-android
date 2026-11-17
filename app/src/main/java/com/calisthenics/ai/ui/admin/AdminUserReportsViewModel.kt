package com.calisthenics.ai.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUserReport
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.model.UserReportSummary
import com.calisthenics.ai.domain.repository.ReportRepository
import com.calisthenics.ai.ui.common.FeatureError
import com.calisthenics.ai.ui.common.toFeatureError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class AdminUserReportsUiState(
    val summary: UserReportSummary? = null,
    val statusFilter: UserReportStatus? = null,
    val page: AdminPage<AdminUserReport>? = null,
    val isLoading: Boolean = false,
    val error: FeatureError? = null,
    val detail: AdminUserReport? = null,
    val isDetailLoading: Boolean = false,
    val detailError: FeatureError? = null,
    val isActing: Boolean = false,
    val actionError: FeatureError? = null,
    val messageRes: Int? = null
)

/** Returns the ISO-8601 instant [days] from [now], or null when [days] is not a positive number. */
fun suspensionUntil(days: String, now: Instant = Instant.now()): String? {
    val parsed = days.trim().toLongOrNull() ?: return null
    if (parsed <= 0L || parsed > MAX_SUSPENSION_DAYS) return null
    return now.plus(Duration.ofDays(parsed)).toString()
}

const val MAX_SUSPENSION_DAYS = 3650L

@HiltViewModel
class AdminUserReportsViewModel @Inject constructor(
    private val repository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserReportsUiState())
    val uiState: StateFlow<AdminUserReportsUiState> = _uiState.asStateFlow()

    fun loadSummary() {
        viewModelScope.launch {
            repository.getSummary().onSuccess { s -> _uiState.update { it.copy(summary = s) } }
        }
    }

    fun setFilter(status: UserReportStatus?) {
        if (_uiState.value.statusFilter == status && _uiState.value.page != null) return
        _uiState.update { it.copy(statusFilter = status) }
        loadList(0)
    }

    fun loadList(page: Int = _uiState.value.page?.page ?: 0) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getReports(_uiState.value.statusFilter, null, page.coerceAtLeast(0), PAGE_SIZE)
                .onSuccess { result -> _uiState.update { it.copy(isLoading = false, page = result) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toFeatureError()) } }
        }
    }

    fun refreshAll() {
        loadSummary()
        loadList()
    }

    fun loadDetail(id: Long) {
        _uiState.update { it.copy(isDetailLoading = true, detailError = null, detail = it.detail?.takeIf { d -> d.id == id }) }
        viewModelScope.launch {
            repository.getReport(id)
                .onSuccess { d -> _uiState.update { it.copy(isDetailLoading = false, detail = d) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isDetailLoading = false, detailError = e.toFeatureError(notFound = R.string.admin_report_not_found)) }
                }
        }
    }

    fun updateStatus(id: Long, status: UserReportStatus, note: String) =
        act(R.string.admin_report_status_updated) { repository.updateStatus(id, status, note) }

    fun addNote(id: Long, note: String) {
        if (note.isBlank()) {
            _uiState.update { it.copy(actionError = FeatureError(res = R.string.admin_report_note_required)) }
            return
        }
        act(R.string.admin_report_note_added) { repository.addNote(id, note) }
    }

    fun addAction(id: Long, action: ModerationAction, suspensionDays: String, note: String) {
        var until: String? = null
        if (action == ModerationAction.TEMPORARY_SUSPENSION) {
            until = suspensionUntil(suspensionDays)
            if (until == null) {
                _uiState.update { it.copy(actionError = FeatureError(res = R.string.admin_report_suspension_days_invalid)) }
                return
            }
        }
        act(R.string.admin_report_action_recorded) { repository.addAction(id, action, until, note) }
    }

    private fun act(successRes: Int, call: suspend () -> Result<AdminUserReport>) {
        if (_uiState.value.isActing) return
        _uiState.update { it.copy(isActing = true, actionError = null) }
        viewModelScope.launch {
            call()
                .onSuccess { d ->
                    _uiState.update { it.copy(isActing = false, detail = d, messageRes = successRes) }
                    loadSummary()
                }
                .onFailure { e -> _uiState.update { it.copy(isActing = false, actionError = e.toFeatureError()) } }
        }
    }

    fun clearActionError() = _uiState.update { it.copy(actionError = null) }
    fun clearMessage() = _uiState.update { it.copy(messageRes = null) }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
