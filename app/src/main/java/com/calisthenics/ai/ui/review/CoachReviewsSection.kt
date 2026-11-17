package com.calisthenics.ai.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MAX_REVIEW_COMMENT_LENGTH
import com.calisthenics.ai.domain.model.RatingSummary
import com.calisthenics.ai.ui.common.FeatureError
import com.calisthenics.ai.ui.common.toDisplayDate
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@Composable
fun featureErrorText(error: FeatureError): String =
    error.res?.let { stringResource(it) } ?: error.message.orEmpty()

/** Reviews section for a coach profile: summary, breakdown, review list, and the "Rate coach" entry. */
@Composable
fun CoachReviewsSection(
    coachId: Long,
    isOwnProfile: Boolean,
    modifier: Modifier = Modifier,
    viewModel: CoachReviewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(coachId) { viewModel.load(coachId) }
    LaunchedEffect(state.successMessageRes) {
        if (state.successMessageRes != null) {
            kotlinx.coroutines.delay(4_000)
            viewModel.consumeSuccess()
        }
    }

    DarkCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reviews_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    modifier = Modifier.weight(1f)
                )
                if (state.canRate && !isOwnProfile) {
                    Button(
                        onClick = viewModel::openSheet,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        Text(stringResource(if (state.hasMyReview) R.string.review_edit_my_review else R.string.review_rate_coach))
                    }
                }
            }

            state.successMessageRes?.let {
                Text(stringResource(it), color = Accent, style = MaterialTheme.typography.bodyMedium)
            }
            if (!state.canRate && !isOwnProfile) {
                state.myStatus?.reason?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
            }

            when {
                state.isSummaryLoading && state.summary == null -> LoadingBox()
                state.summaryError != null && state.summary == null -> ErrorBox(state.summaryError!!, viewModel::retry)
                state.summary != null -> {
                    val summary = state.summary!!
                    if (summary.totalReviews <= 0L) {
                        Text(stringResource(R.string.reviews_empty), color = SecondaryText)
                    } else {
                        RatingOverview(summary)
                    }
                }
            }

            if (state.reviews.isNotEmpty()) {
                HorizontalDivider(color = Border)
                state.reviews.forEachIndexed { index, review ->
                    ReviewItem(review)
                    if (index != state.reviews.lastIndex) HorizontalDivider(color = Border.copy(alpha = 0.5f))
                }
                if (state.hasNext) {
                    OutlinedButton(
                        onClick = viewModel::loadMore,
                        enabled = !state.isLoadingMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isLoadingMore) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.reviews_load_more))
                        }
                    }
                }
            } else if (state.isReviewsLoading && state.summary != null) {
                LoadingBox()
            }
            state.reviewsError?.let { if (state.reviews.isNotEmpty() || state.summary != null) ErrorBox(it, viewModel::retry) }
        }
    }

    if (state.isSheetOpen) {
        RateCoachSheet(viewModel)
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), color = Accent, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorBox(error: FeatureError, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(featureErrorText(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

@Composable
private fun RatingOverview(summary: RatingSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatRating(summary.averageRating),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            summary.averageRating?.let { StarRatingRow(it) }
            Text(
                stringResource(R.string.reviews_total_count, summary.totalReviews.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (5 downTo 1).forEach { star ->
                DistributionBar(star, summary.fractionFor(star), summary.distribution[star] ?: 0)
            }
        }
    }
    if (summary.hasCriteria) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.reviews_criteria_title),
                style = MaterialTheme.typography.labelLarge,
                color = SecondaryText
            )
            CriterionAverageRow(stringResource(R.string.review_criterion_coaching_quality), summary.coachingQualityAvg)
            CriterionAverageRow(stringResource(R.string.review_criterion_communication), summary.communicationAvg)
            CriterionAverageRow(stringResource(R.string.review_criterion_plan_effectiveness), summary.planEffectivenessAvg)
            CriterionAverageRow(stringResource(R.string.review_criterion_support), summary.supportResponsivenessAvg)
        }
    }
}

@Composable
private fun ReviewItem(review: CoachReview) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UserAvatar(review.reviewerAvatarUrl, modifier = Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    review.reviewerName.ifBlank { stringResource(R.string.review_anonymous) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StarRatingRow(review.overallRating.toDouble(), starSize = 13.dp)
                    Text(review.createdAt.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    if (review.edited) {
                        Text(stringResource(R.string.review_edited), style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    }
                }
            }
            if (review.mine) PillBadge(stringResource(R.string.review_yours), isAccent = true)
        }
        review.comment?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = PrimaryText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateCoachSheet(viewModel: CoachReviewsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = viewModel::closeSheet,
        sheetState = sheetState,
        containerColor = Ink
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(if (state.hasMyReview) R.string.review_sheet_title_edit else R.string.review_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Text(stringResource(R.string.review_overall_label), style = MaterialTheme.typography.labelLarge, color = SecondaryText)
            StarSelector(state.draft.overallRating, viewModel::setOverall, enabled = !state.isSubmitting)

            Text(stringResource(R.string.review_criteria_optional), style = MaterialTheme.typography.labelLarge, color = SecondaryText)
            ReviewCriterion.entries.forEach { criterion ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(criterionLabel(criterion), color = PrimaryText, modifier = Modifier.weight(1f))
                    StarSelector(
                        value = criterion.read(state.draft),
                        onValueChange = { viewModel.setCriterion(criterion, it) },
                        starSize = 28.dp,
                        enabled = !state.isSubmitting
                    )
                }
            }

            OutlinedTextField(
                value = state.draft.comment,
                onValueChange = viewModel::setComment,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.review_comment_label)) },
                supportingText = {
                    Text(
                        stringResource(R.string.review_comment_counter, state.draft.comment.length, MAX_REVIEW_COMMENT_LENGTH),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                minLines = 3,
                maxLines = 6,
                enabled = !state.isSubmitting
            )

            state.submitError?.let {
                Text(featureErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.draft.overallRating == 0) {
                Text(stringResource(R.string.review_overall_required), color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = viewModel::closeSheet,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(if (state.hasMyReview) R.string.review_save_changes else R.string.review_submit))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
