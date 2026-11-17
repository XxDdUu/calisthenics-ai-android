package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminUserReport
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.PreviousUserReport
import com.calisthenics.ai.domain.model.UserReportAction
import com.calisthenics.ai.domain.model.UserReportActionType
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.model.UserReportSummary
import com.calisthenics.ai.ui.common.toDisplayDate
import com.calisthenics.ai.ui.common.toDisplayDateTime
import com.calisthenics.ai.ui.media.MediaUrlUtils
import com.calisthenics.ai.ui.report.reportReasonLabel
import com.calisthenics.ai.ui.review.featureErrorText
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Warning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUserReportsScreen(
    onBack: () -> Unit,
    onReportClick: (Long) -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminUserReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshAll() }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.USER_REPORTS,
        title = stringResource(R.string.admin_user_reports),
        subtitle = "Safety & user report moderation",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = viewModel::refreshAll,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_refresh),
                    tint = PrimaryText
                )
            }
        }
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        state.summary?.let { summary ->
            ReportsSummaryGrid(
                summary = summary,
                selectedStatus = state.statusFilter,
                onFilterClick = viewModel::setFilter
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.statusFilter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text(stringResource(R.string.admin_reports_all)) }
            )
            listOf(
                UserReportStatus.PENDING,
                UserReportStatus.UNDER_REVIEW,
                UserReportStatus.RESOLVED,
                UserReportStatus.DISMISSED
            ).forEach { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { viewModel.setFilter(status) },
                    label = { Text(reportStatusLabel(status)) }
                )
            }
        }

        ModerationErrorBanner(state.error, onRetry = { viewModel.loadList() })

        if (state.isLoading && state.page == null) {
            BoxLoading()
        }

        state.page?.let { page ->
            if (page.content.isEmpty()) {
                Text(
                    stringResource(R.string.admin_reports_empty),
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(page.content, key = { it.id }) { report ->
                        AdminUserReportCard(report = report, onClick = { onReportClick(report.id) })
                    }
                }
            }
            ModerationPageControls(page = page, onPageChange = viewModel::loadList)
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportsSummaryGrid(
    summary: UserReportSummary,
    selectedStatus: UserReportStatus?,
    onFilterClick: (UserReportStatus?) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportCounterChip(stringResource(R.string.admin_reports_total), summary.total, selectedStatus == null) {
            onFilterClick(null)
        }
        ReportCounterChip(stringResource(R.string.admin_reports_pending), summary.pending, selectedStatus == UserReportStatus.PENDING) {
            onFilterClick(UserReportStatus.PENDING)
        }
        ReportCounterChip(stringResource(R.string.admin_reports_under_review), summary.underReview, selectedStatus == UserReportStatus.UNDER_REVIEW) {
            onFilterClick(UserReportStatus.UNDER_REVIEW)
        }
        ReportCounterChip(stringResource(R.string.admin_reports_resolved), summary.resolved, selectedStatus == UserReportStatus.RESOLVED) {
            onFilterClick(UserReportStatus.RESOLVED)
        }
        ReportCounterChip(stringResource(R.string.admin_reports_dismissed), summary.dismissed, selectedStatus == UserReportStatus.DISMISSED) {
            onFilterClick(UserReportStatus.DISMISSED)
        }
    }
}

@Composable
private fun ReportCounterChip(
    label: String,
    value: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) Accent.copy(alpha = 0.16f) else Elevated)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
        }
    }
}

@Composable
private fun AdminUserReportCard(report: AdminUserReport, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    reportReasonLabel(report.reason),
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ReportStatusBadge(report.status)
            }
            Text(
                "${stringResource(R.string.admin_report_reported_label)}: ${report.reportedUser.fullName} (#${report.reportedUser.id ?: 0L})",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryText
            )
            Text(
                "${stringResource(R.string.admin_report_reporter_label)}: ${report.reporter.fullName} (#${report.reporter.id ?: 0L})",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
            Text(
                report.createdAt.toDisplayDateTime(),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun AdminUserReportDetailScreen(
    reportId: Long,
    onBack: () -> Unit,
    onReportClick: (Long) -> Unit,
    viewModel: AdminUserReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var noteInput by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf(ModerationAction.WARNING) }
    var suspensionDays by remember { mutableStateOf("7") }
    var actionNote by remember { mutableStateOf("") }
    var pendingStatusChange by remember { mutableStateOf<UserReportStatus?>(null) }
    var statusChangeNote by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reportId) {
        viewModel.clearMessage()
        viewModel.clearActionError()
        viewModel.loadDetail(reportId)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModerationHeader(
            title = stringResource(R.string.admin_report_detail),
            onBack = onBack,
            onRefresh = { viewModel.loadDetail(reportId) }
        )

        ModerationErrorBanner(state.detailError, onRetry = { viewModel.loadDetail(reportId) })

        state.messageRes?.let { res ->
            Text(stringResource(res), color = Accent, style = MaterialTheme.typography.bodyMedium)
        }
        state.actionError?.let { err ->
            Text(featureErrorText(err), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.isDetailLoading && state.detail == null) {
            BoxLoading()
        }

        state.detail?.let { report ->
            ReportOverviewCard(report = report, onEvidenceClick = { previewImageUrl = it })

            StatusModerationCard(
                status = report.status,
                busy = state.isActing,
                onChooseStatus = { target ->
                    viewModel.clearActionError()
                    statusChangeNote = ""
                    pendingStatusChange = target
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Elevated)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.admin_report_add_note), style = MaterialTheme.typography.titleSmall, color = PrimaryText)
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it.take(1000) },
                        label = { Text(stringResource(R.string.admin_report_note_label)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val trimmed = noteInput.trim()
                            viewModel.addNote(report.id, trimmed)
                            if (trimmed.isNotEmpty()) noteInput = ""
                        },
                        enabled = !state.isActing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.admin_report_add_note))
                    }
                }
            }

            ModerationActionCard(
                selectedAction = selectedAction,
                onSelectAction = { selectedAction = it },
                suspensionDays = suspensionDays,
                onSuspensionDaysChange = { suspensionDays = it.filter(Char::isDigit).take(4) },
                actionNote = actionNote,
                onActionNoteChange = { actionNote = it.take(1000) },
                busy = state.isActing,
                onSubmit = {
                    viewModel.addAction(report.id, selectedAction, suspensionDays, actionNote)
                    actionNote = ""
                }
            )

            Text(
                stringResource(R.string.admin_report_previous_reports, report.previousReports.size),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText
            )
            if (report.previousReports.isEmpty()) {
                Text("-", color = SecondaryText)
            } else {
                report.previousReports.forEach { prev ->
                    PreviousReportRow(prev = prev, onClick = { onReportClick(prev.id) })
                }
            }

            Text(
                stringResource(R.string.admin_report_timeline),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText
            )
            if (report.actions.isEmpty()) {
                Text("-", color = SecondaryText)
            } else {
                report.actions.forEach { action ->
                    ReportTimelineRow(action)
                }
            }
        }
    }

    pendingStatusChange?.let { targetStatus ->
        AlertDialog(
            onDismissRequest = { if (!state.isActing) pendingStatusChange = null },
            containerColor = Elevated,
            title = { Text(reportStatusActionLabel(targetStatus), color = PrimaryText) },
            text = {
                OutlinedTextField(
                    value = statusChangeNote,
                    onValueChange = { statusChangeNote = it.take(1000) },
                    label = { Text(stringResource(R.string.admin_report_note_optional_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isActing,
                    onClick = {
                        viewModel.updateStatus(reportId, targetStatus, statusChangeNote)
                        pendingStatusChange = null
                    }
                ) {
                    Text(reportStatusActionLabel(targetStatus), color = Accent)
                }
            },
            dismissButton = {
                TextButton(enabled = !state.isActing, onClick = { pendingStatusChange = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    previewImageUrl?.let { url ->
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Card(colors = CardDefaults.cardColors(containerColor = Elevated)) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
                    AsyncImage(
                        model = MediaUrlUtils.resolveMediaUrl(url) ?: url,
                        contentDescription = stringResource(R.string.report_evidence_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    TextButton(onClick = { previewImageUrl = null }) {
                        Text(stringResource(R.string.common_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportOverviewCard(report: AdminUserReport, onEvidenceClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(reportReasonLabel(report.reason), style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                ReportStatusBadge(report.status)
            }
            UserPartyLine(stringResource(R.string.admin_report_reported_label), report.reportedUser.fullName, report.reportedUser.email, report.reportedUser.id)
            UserPartyLine(stringResource(R.string.admin_report_reporter_label), report.reporter.fullName, report.reporter.email, report.reporter.id)

            report.description?.takeIf { it.isNotBlank() }?.let { desc ->
                HorizontalDivider(color = Border)
                Text(desc, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            }

            if (report.evidenceUrls.isNotEmpty()) {
                HorizontalDivider(color = Border)
                Text(stringResource(R.string.admin_report_evidence_label), style = MaterialTheme.typography.labelLarge, color = SecondaryText)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(report.evidenceUrls) { rawUrl ->
                        val resolved = MediaUrlUtils.resolveMediaUrl(rawUrl) ?: rawUrl
                        AsyncImage(
                            model = resolved,
                            contentDescription = stringResource(R.string.report_evidence_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Ink)
                                .clickable { onEvidenceClick(rawUrl) }
                        )
                    }
                }
            }

            HorizontalDivider(color = Border)
            Text(
                "${report.createdAt.toDisplayDateTime()}" +
                    (report.reviewedAt?.let { " • Reviewed ${it.toDisplayDateTime()}" }.orEmpty()) +
                    (report.resolvedAt?.let { " • Closed ${it.toDisplayDateTime()}" }.orEmpty()),
                color = SecondaryText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun UserPartyLine(roleLabel: String, name: String, email: String?, id: Long?) {
    val suffix = listOfNotNull(id?.let { "#$it" }, email?.takeIf(String::isNotBlank)).joinToString(" • ")
    Text(
        text = "$roleLabel: $name" + if (suffix.isNotEmpty()) " ($suffix)" else "",
        style = MaterialTheme.typography.bodySmall,
        color = PrimaryText
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusModerationCard(
    status: UserReportStatus,
    busy: Boolean,
    onChooseStatus: (UserReportStatus) -> Unit
) {
    if (status != UserReportStatus.PENDING && status != UserReportStatus.UNDER_REVIEW) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status == UserReportStatus.PENDING) {
                OutlinedButton(enabled = !busy, onClick = { onChooseStatus(UserReportStatus.UNDER_REVIEW) }) {
                    Text(stringResource(R.string.admin_report_mark_under_review))
                }
            }
            Button(
                enabled = !busy,
                onClick = { onChooseStatus(UserReportStatus.RESOLVED) },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
            ) {
                Text(stringResource(R.string.admin_report_resolve))
            }
            OutlinedButton(enabled = !busy, onClick = { onChooseStatus(UserReportStatus.DISMISSED) }) {
                Text(stringResource(R.string.admin_report_dismiss))
            }
        }
    }
}

@Composable
private fun ModerationActionCard(
    selectedAction: ModerationAction,
    onSelectAction: (ModerationAction) -> Unit,
    suspensionDays: String,
    onSuspensionDaysChange: (String) -> Unit,
    actionNote: String,
    onActionNoteChange: (String) -> Unit,
    busy: Boolean,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.admin_report_moderation_action), style = MaterialTheme.typography.titleSmall, color = PrimaryText)
            Text(stringResource(R.string.admin_report_action_notice), style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            ModerationAction.entries.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAction(action) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedAction == action, onClick = { onSelectAction(action) })
                    Text(moderationActionLabel(action), color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (selectedAction == ModerationAction.TEMPORARY_SUSPENSION) {
                OutlinedTextField(
                    value = suspensionDays,
                    onValueChange = onSuspensionDaysChange,
                    label = { Text(stringResource(R.string.admin_report_suspension_days_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = actionNote,
                onValueChange = onActionNoteChange,
                label = { Text(stringResource(R.string.admin_report_note_optional_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onSubmit,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                } else {
                    Text(stringResource(R.string.admin_report_take_action))
                }
            }
        }
    }
}

@Composable
private fun PreviousReportRow(prev: PreviousUserReport, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("#${prev.id} • ${reportReasonLabel(prev.reason)}", color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
                Text(prev.createdAt.toDisplayDate(), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
            ReportStatusBadge(prev.status)
        }
    }
}

@Composable
private fun ReportTimelineRow(action: UserReportAction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    when (action.type) {
                        UserReportActionType.STATUS_CHANGE ->
                            "${action.fromStatus?.name ?: "-"} → ${action.toStatus?.name ?: "-"}"
                        UserReportActionType.MODERATION_ACTION ->
                            action.moderationAction?.let { moderationActionLabel(it) } ?: action.type.name
                        UserReportActionType.NOTE ->
                            stringResource(R.string.admin_report_note_label)
                        UserReportActionType.UNKNOWN -> action.type.name
                    },
                    color = PrimaryText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(action.createdAt.toDisplayDateTime(), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
            action.suspendedUntil?.let { until ->
                Text("Until: ${until.toDisplayDateTime()}", color = Warning, style = MaterialTheme.typography.bodySmall)
            }
            action.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(note, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
            }
            action.adminName?.takeIf { it.isNotBlank() }?.let { admin ->
                Text("$admin (#${action.adminId ?: 0L})", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ReportStatusBadge(status: UserReportStatus) {
    val color = when (status) {
        UserReportStatus.PENDING -> Warning
        UserReportStatus.UNDER_REVIEW -> Accent
        UserReportStatus.RESOLVED -> Accent
        UserReportStatus.DISMISSED -> SecondaryText
        UserReportStatus.UNKNOWN -> SecondaryText
    }
    Text(
        text = reportStatusLabel(status),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun reportStatusLabel(status: UserReportStatus): String = when (status) {
    UserReportStatus.PENDING -> stringResource(R.string.admin_reports_pending)
    UserReportStatus.UNDER_REVIEW -> stringResource(R.string.admin_reports_under_review)
    UserReportStatus.RESOLVED -> stringResource(R.string.admin_reports_resolved)
    UserReportStatus.DISMISSED -> stringResource(R.string.admin_reports_dismissed)
    UserReportStatus.UNKNOWN -> "-"
}

@Composable
private fun reportStatusActionLabel(status: UserReportStatus): String = when (status) {
    UserReportStatus.UNDER_REVIEW -> stringResource(R.string.admin_report_mark_under_review)
    UserReportStatus.RESOLVED -> stringResource(R.string.admin_report_resolve)
    UserReportStatus.DISMISSED -> stringResource(R.string.admin_report_dismiss)
    else -> reportStatusLabel(status)
}

@Composable
private fun moderationActionLabel(action: ModerationAction): String = when (action) {
    ModerationAction.WARNING -> stringResource(R.string.admin_report_action_warning)
    ModerationAction.TEMPORARY_SUSPENSION -> stringResource(R.string.admin_report_action_temp_suspension)
    ModerationAction.PERMANENT_BAN -> stringResource(R.string.admin_report_action_permanent_ban)
}
