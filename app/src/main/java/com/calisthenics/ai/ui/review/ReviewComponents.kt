package com.calisthenics.ai.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.ReviewDraft
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Warning
import java.util.Locale

fun formatRating(value: Double?): String = value?.let { String.format(Locale.US, "%.1f", it) } ?: "–"

/** Read-only row of 5 stars, rounded to the nearest whole star. */
@Composable
fun StarRatingRow(rating: Double, modifier: Modifier = Modifier, starSize: Dp = 16.dp) {
    val rounded = Math.round(rating).toInt().coerceIn(0, 5)
    Row(
        modifier = modifier.semantics {
            contentDescription = "${formatRating(rating)} / 5"
        },
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rounded) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (star <= rounded) Warning else SecondaryText,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

/** Interactive 1..5 star selector. [value] == null/0 means "unset". */
@Composable
fun StarSelector(
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp,
    enabled: Boolean = true
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { star ->
            val selected = star <= (value ?: 0)
            Icon(
                imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(R.string.review_star_content_description, star),
                tint = if (selected) Warning else SecondaryText,
                modifier = Modifier
                    .size(starSize)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.Button) { onValueChange(star) }
            )
        }
    }
}

/** Compact "★ 4.8 (12)" chip used in directory cards and headers. */
@Composable
fun RatingChip(averageRating: Double?, reviewCount: Long?, modifier: Modifier = Modifier) {
    if (averageRating == null || (reviewCount ?: 0L) <= 0L) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Warning.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(13.dp))
        Text(
            text = "${formatRating(averageRating)} (${reviewCount})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Warning
        )
    }
}

@Composable
fun DistributionBar(stars: Int, fraction: Float, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$stars", style = MaterialTheme.typography.labelMedium, color = SecondaryText, modifier = Modifier.width(14.dp))
        Icon(Icons.Filled.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Border)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Accent)
            )
        }
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = SecondaryText,
            modifier = Modifier.width(32.dp).padding(start = 8.dp)
        )
    }
}

@Composable
fun CriterionAverageRow(label: String, value: Double?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SecondaryText, modifier = Modifier.weight(1f))
        if (value == null) {
            Text("–", color = SecondaryText)
        } else {
            StarRatingRow(value, starSize = 13.dp)
            Text(
                formatRating(value),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp).width(28.dp)
            )
        }
    }
}

@Composable
fun criterionLabel(criterion: ReviewCriterion): String = stringResource(
    when (criterion) {
        ReviewCriterion.COACHING_QUALITY -> R.string.review_criterion_coaching_quality
        ReviewCriterion.COMMUNICATION -> R.string.review_criterion_communication
        ReviewCriterion.PLAN_EFFECTIVENESS -> R.string.review_criterion_plan_effectiveness
        ReviewCriterion.SUPPORT_RESPONSIVENESS -> R.string.review_criterion_support
    }
)

/** Short "criterion: n" summary lines for a review with criteria set. */
fun ReviewDraft.hasAnyCriterion(): Boolean =
    listOf(coachingQuality, communication, planEffectiveness, supportResponsiveness).any { it != null }
