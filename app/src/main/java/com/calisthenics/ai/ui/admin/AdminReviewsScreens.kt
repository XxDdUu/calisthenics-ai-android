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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminReview
import com.calisthenics.ai.domain.model.AdminReviewHistoryEntry
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.ui.common.FeatureError
import com.calisthenics.ai.ui.common.toDisplayDateTime
import com.calisthenics.ai.ui.review.StarRatingRow
import com.calisthenics.ai.ui.review.featureErrorText
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminReviewsScreen(
    onBack: () -> Unit,
    onReviewClick: (Long) -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminReviewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadList(0) }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.REVIEWS,
        title = stringResource(R.string.admin_reviews),
        subtitle = "Coach review moderation queue",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = { viewModel.loadList() },
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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.statusFilter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text(stringResource(R.string.admin_review_all)) }
            )
            FilterChip(
                selected = state.statusFilter == ReviewModerationStatus.VISIBLE,
                onClick = { viewModel.setFilter(ReviewModerationStatus.VISIBLE) },
                label = { Text(stringResource(R.string.admin_review_status_visible)) }
            )
            FilterChip(
                selected = state.statusFilter == ReviewModerationStatus.HIDDEN,
                onClick = { viewModel.setFilter(ReviewModerationStatus.HIDDEN) },
                label = { Text(stringResource(R.string.admin_review_status_hidden)) }
            )
        }

        ModerationErrorBanner(state.error, onRetry = { viewModel.loadList() })

        if (state.isLoading && state.page == null) {
            BoxLoading()
        }

        state.page?.let { page ->
            if (page.content.isEmpty()) {
                Text(
                    stringResource(R.string.admin_reviews_empty),
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(page.content, key = { it.id }) { review ->
                        AdminReviewRowCard(review = review, onClick = { onReviewClick(review.id) })
                    }
                }
            }
            ModerationPageControls(page = page, onPageChange = viewModel::loadList)
        }
    }
    }
}

@Composable
private fun AdminReviewRowCard(review: AdminReview, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarRatingRow(rating = review.overallRating.toDouble(), starSize = 15.dp)
                ReviewStatusBadge(review.status)
            }
            Text(
                stringResource(R.string.admin_review_coach_label, review.coachName, review.coachId ?: 0L),
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.admin_review_student_label, review.studentName, review.studentId ?: 0L),
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
            review.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                review.createdAt.toDisplayDateTime(),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun AdminReviewDetailScreen(
    reviewId: Long,
    onBack: () -> Unit,
    viewModel: AdminReviewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showHideDialog by remember { mutableStateOf(false) }
    var hideReason by remember { mutableStateOf("") }

    LaunchedEffect(reviewId) {
        viewModel.clearMessage()
        viewModel.clearModerationError()
        viewModel.loadDetail(reviewId)
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
            title = stringResource(R.string.admin_review_detail),
            onBack = onBack,
            onRefresh = { viewModel.loadDetail(reviewId) }
        )

        ModerationErrorBanner(state.detailError, onRetry = { viewModel.loadDetail(reviewId) })

        state.messageRes?.let { res ->
            Text(stringResource(res), color = Accent, style = MaterialTheme.typography.bodyMedium)
        }
        state.moderationError?.let { err ->
            Text(featureErrorText(err), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.isDetailLoading && state.detail == null) {
            BoxLoading()
        }

        state.detail?.let { review ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Elevated)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StarRatingRow(rating = review.overallRating.toDouble(), starSize = 18.dp)
                        ReviewStatusBadge(review.status)
                    }
                    Text(
                        stringResource(R.string.admin_review_coach_label, review.coachName, review.coachId ?: 0L),
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.admin_review_student_label, review.studentName, review.studentId ?: 0L),
                        color = SecondaryText
                    )
                    CriterionLine(stringResource(R.string.review_criterion_coaching_quality), review.coachingQuality)
                    CriterionLine(stringResource(R.string.review_criterion_communication), review.communication)
                    CriterionLine(stringResource(R.string.review_criterion_plan_effectiveness), review.planEffectiveness)
                    CriterionLine(stringResource(R.string.review_criterion_support), review.supportResponsiveness)

                    review.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                        HorizontalDivider(color = Border)
                        Text(comment, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
                    }

                    Text(
                        review.createdAt.toDisplayDateTime(),
                        color = SecondaryText,
                        style = MaterialTheme.typography.labelSmall
                    )

                    review.moderationReason?.takeIf { it.isNotBlank() }?.let { reason ->
                        HorizontalDivider(color = Border)
                        Text(
                            "${stringResource(R.string.admin_review_reason_label)}: $reason",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    review.moderatedByAdminId?.let { adminId ->
                        Text(
                            "${stringResource(R.string.admin_review_moderated_by, adminId)} • ${review.moderatedAt.toDisplayDateTime()}",
                            color = SecondaryText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (review.status == ReviewModerationStatus.VISIBLE) {
                Button(
                    onClick = {
                        viewModel.clearModerationError()
                        hideReason = ""
                        showHideDialog = true
                    },
                    enabled = !state.isModerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isModerating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                    } else {
                        Text(stringResource(R.string.admin_review_hide))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.moderate(review.id, ReviewModerationStatus.VISIBLE, "") },
                    enabled = !state.isModerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isModerating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
                    } else {
                        Text(stringResource(R.string.admin_review_restore))
                    }
                }
            }

            Text(
                stringResource(R.string.admin_review_history),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText
            )
            if (review.history.isEmpty()) {
                Text("-", color = SecondaryText)
            } else {
                review.history.forEach { entry ->
                    ReviewHistoryRow(entry)
                }
            }
        }
    }

    if (showHideDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isModerating) showHideDialog = false },
            containerColor = Elevated,
            title = { Text(stringResource(R.string.admin_review_hide_title), color = PrimaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hideReason,
                        onValueChange = { hideReason = it.take(500) },
                        label = { Text(stringResource(R.string.admin_review_reason_label)) },
                        placeholder = { Text(stringResource(R.string.admin_review_reason_hint)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.moderationError?.let { err ->
                        Text(featureErrorText(err), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isModerating && hideReason.isNotBlank(),
                    onClick = {
                        viewModel.moderate(reviewId, ReviewModerationStatus.HIDDEN, hideReason)
                        showHideDialog = false
                    }
                ) {
                    Text(stringResource(R.string.admin_review_hide), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isModerating,
                    onClick = { showHideDialog = false }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ReviewStatusBadge(status: ReviewModerationStatus) {
    val label = stringResource(
        if (status == ReviewModerationStatus.HIDDEN) R.string.admin_review_status_hidden else R.string.admin_review_status_visible
    )
    val color = if (status == ReviewModerationStatus.HIDDEN) MaterialTheme.colorScheme.error else Accent
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun CriterionLine(label: String, value: Int?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        Text("$value / 5", color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReviewHistoryRow(entry: AdminReviewHistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.type.name, color = PrimaryText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(entry.createdAt.toDisplayDateTime(), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            }
            entry.overallRating?.let { rating ->
                Text("$rating ★", color = Accent, style = MaterialTheme.typography.bodySmall)
            }
            entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(note, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
            }
            entry.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(comment, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun ModerationHeader(title: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRefresh) { Text(stringResource(R.string.common_refresh)) }
    }
}

@Composable
internal fun ModerationPageControls(page: AdminPage<*>, onPageChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(enabled = page.page > 0, onClick = { onPageChange(page.page - 1) }) {
            Text(stringResource(R.string.common_previous))
        }
        Text(
            stringResource(R.string.loc_page_count, page.page + 1, page.totalPages.coerceAtLeast(1)),
            color = SecondaryText
        )
        TextButton(enabled = page.hasNext, onClick = { onPageChange(page.page + 1) }) {
            Text(stringResource(R.string.common_next))
        }
    }
}

@Composable
internal fun ModerationErrorBanner(error: FeatureError?, onRetry: () -> Unit) {
    if (error == null) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(featureErrorText(error), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

@Composable
internal fun BoxLoading() {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = Accent)
    }
}
