package com.calisthenics.ai.ui.components.aichat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.ui.theme.Accent

/**
 * Standardized AI thinking/generating/tool-execution status row.
 * Reusable across all AI interfaces with the pulsating accent dot matching AI Workout Planner.
 */
@Composable
fun AiStatusRow(
    statusText: String?,
    modifier: Modifier = Modifier
) {
    if (statusText.isNullOrBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = alpha))
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Accent,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun AiStatusRow(
    state: AIChatState,
    modifier: Modifier = Modifier
) {
    AiStatusRow(
        statusText = state.statusText,
        modifier = modifier
    )
}
