package com.calisthenics.ai.ui.components.aichat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.R
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.MutedText
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface

/**
 * Single source-of-truth AI Chat Prompt Composer.
 * Strictly uses the AI Workout Planner design, animated dual-arc glowing border,
 * keyboard positioning, and interaction patterns.
 */
@Composable
fun AiChatComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit = {},
    isGenerating: Boolean = false,
    placeholder: String = "Describe your fitness goals...",
    errorMessage: String? = null,
    leadingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = Accent
    val borderColor = Border

    // Single shared rotation with synchronized natural, irregular variable speed matching AI Workout Planner
    val infiniteTransition = rememberInfiniteTransition(label = "ai_copilot_composer")
    val sharedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4200
                0f at 0 using FastOutSlowInEasing
                0.38f at 1200 using FastOutSlowInEasing // swift burst
                0.55f at 2200 using FastOutSlowInEasing // gentle slower cruise
                0.82f at 3100 using FastOutSlowInEasing // acceleration sweep
                1f at 4200 using FastOutSlowInEasing   // smooth arrival into loop
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sharedRotation"
    )
    val fixedPhaseOffset = 0.5f // fixed 180° offset
    val arc1Phase = sharedRotation
    val arc2Phase = sharedRotation + fixedPhaseOffset

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Error banner
        if (!errorMessage.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Single stationary composer container with border and animation drawn on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Surface, shape = RoundedCornerShape(28.dp))
                .drawWithContent {
                    // 1. Draw child content (text field, icons, background)
                    drawContent()

                    // 2. Draw border layer on top so it is always visible and never obscured by background
                    val strokePx = if (isGenerating) 2.5.dp.toPx() else 1.dp.toPx()
                    val cornerPx = 28.dp.toPx()
                    val inset = strokePx / 2f

                    val rrPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = inset,
                                top = inset,
                                right = size.width - inset,
                                bottom = size.height - inset,
                                radiusX = cornerPx,
                                radiusY = cornerPx
                            )
                        )
                    }

                    if (!isGenerating) {
                        // Static idle border
                        drawPath(
                            path = rrPath,
                            color = borderColor,
                            style = Stroke(width = strokePx)
                        )
                    } else {
                        // Subtle base track while generating
                        drawPath(
                            path = rrPath,
                            color = borderColor.copy(alpha = 0.35f),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Trace two independent animated gradient arcs along the rounded-rect perimeter
                        val pm = PathMeasure()
                        pm.setPath(rrPath, forceClosed = true)
                        val totalLength = pm.length

                        if (totalLength > 0f) {
                            val arcLength = totalLength * 0.22f // each arc spans ~22% of perimeter
                            val slices = 20 // gradient segments for smooth fade from tail to head

                            fun drawGlowingArc(phase: Float) {
                                val normPhase = ((phase % 1f) + 1f) % 1f
                                val startDist = normPhase * totalLength
                                val sliceLength = arcLength / slices
                                val slicePath = Path()

                                for (s in 0 until slices) {
                                    val t = (s + 1).toFloat() / slices // 0..1 from tail to head
                                    val alpha = (t * t).coerceIn(0.02f, 1f) // smooth quadratic gradient leading to head

                                    val d1 = (startDist + s * sliceLength) % totalLength
                                    val d2 = (startDist + (s + 1) * sliceLength) % totalLength

                                    slicePath.reset()
                                    if (d1 < d2) {
                                        pm.getSegment(d1, d2, slicePath, true)
                                    } else {
                                        // Wraps around the path boundary
                                        pm.getSegment(d1, totalLength, slicePath, true)
                                        pm.getSegment(0f, d2, slicePath, false)
                                    }

                                    drawPath(
                                        path = slicePath,
                                        color = accentColor.copy(alpha = alpha),
                                        style = Stroke(
                                            width = strokePx,
                                            cap = if (s == slices - 1) StrokeCap.Round else StrokeCap.Butt
                                        )
                                    )
                                }
                            }

                            // Arc 1 & Arc 2: perfectly synchronized with fixed 180° offset
                            drawGlowingArc(arc1Phase)
                            drawGlowingArc(arc2Phase)
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (leadingAction != null) {
                    leadingAction()
                }

                BasicTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    enabled = !isGenerating,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = PrimaryText),
                    cursorBrush = SolidColor(Accent),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (prompt.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = SecondaryText.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Border.copy(alpha = 0.5f))
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val isEnabled = prompt.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) Accent else Elevated)
                            .then(
                                if (isEnabled) Modifier.clickable(onClick = onSend)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.ai_generate_button),
                            tint = if (isEnabled) Ink else MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
