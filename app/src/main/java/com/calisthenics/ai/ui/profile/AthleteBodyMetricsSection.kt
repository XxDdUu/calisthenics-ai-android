package com.calisthenics.ai.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.R
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

object BodyMetricFormatter {
    fun formatHeight(heightCm: Double?, fallback: String = "Not provided"): String {
        return heightCm?.let {
            if (it % 1.0 == 0.0) "${it.toInt()} cm" else String.format(java.util.Locale.US, "%.1f cm", it)
        } ?: fallback
    }

    fun formatWeight(weightKg: Double?, fallback: String = "Not provided"): String {
        return weightKg?.let {
            if (it % 1.0 == 0.0) "${it.toInt()} kg" else String.format(java.util.Locale.US, "%.1f kg", it)
        } ?: fallback
    }

    fun isBothMissing(heightCm: Double?, weightKg: Double?): Boolean =
        heightCm == null && weightKg == null
}

@Composable
fun AthleteBodyMetricsSection(
    heightCm: Double?,
    weightKg: Double?,
    modifier: Modifier = Modifier
) {
    val notProvidedText = stringResource(R.string.body_metrics_not_provided)

    val heightText = BodyMetricFormatter.formatHeight(heightCm, notProvidedText)
    val weightText = BodyMetricFormatter.formatWeight(weightKg, notProvidedText)
    val bothMissing = BodyMetricFormatter.isBothMissing(heightCm, weightKg)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MonitorWeight,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.body_metrics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BodyMetricCard(
                label = stringResource(R.string.body_metrics_height),
                value = heightText,
                isMissing = heightCm == null,
                icon = Icons.Default.Height,
                contentDescription = stringResource(R.string.body_metrics_height),
                modifier = Modifier.weight(1f)
            )
            BodyMetricCard(
                label = stringResource(R.string.body_metrics_weight),
                value = weightText,
                isMissing = weightKg == null,
                icon = Icons.Default.MonitorWeight,
                contentDescription = stringResource(R.string.body_metrics_weight),
                modifier = Modifier.weight(1f)
            )
        }

        if (bothMissing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Elevated)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.body_metrics_empty_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
fun BodyMetricCard(
    label: String,
    value: String,
    isMissing: Boolean,
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMissing) Border.copy(alpha = 0.4f) else Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = if (isMissing) SecondaryText else Accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = value,
                style = if (isMissing) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
                fontWeight = if (isMissing) FontWeight.Normal else FontWeight.Bold,
                color = if (isMissing) SecondaryText.copy(alpha = 0.8f) else PrimaryText
            )
        }
    }
}
