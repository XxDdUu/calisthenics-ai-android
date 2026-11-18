package com.calisthenics.ai.ui.components.aichat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.ui.components.markdown.AiMarkdownText
import com.calisthenics.ai.ui.components.markdown.MarkdownParser
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.MutedText
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.Success
import kotlinx.coroutines.delay

/**
 * Compact copy button that copies raw or Markdown-cleaned text to the Android clipboard
 * and temporarily shows a checkmark indicator for visual confirmation.
 */
@Composable
fun MessageCopyButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    cleanMarkdown: Boolean = true,
    tint: Color = MutedText,
    copiedTint: Color = Success
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = textToCopy.isNotBlank()
            ) {
                val processedText = if (cleanMarkdown) {
                    MarkdownParser.toPlainText(textToCopy)
                } else {
                    textToCopy
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Chat message", processedText))
                copied = true
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = if (copied) "Copied" else "Copy message",
            tint = if (copied) copiedTint else tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Single source-of-truth User message bubble.
 * Standardized across all AI chat interfaces, matching AI Workout Planner.
 */
@Composable
fun AiUserMessageBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentWidth(Alignment.End)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(Accent)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ink,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageCopyButton(
                        textToCopy = text,
                        cleanMarkdown = false,
                        tint = Ink.copy(alpha = 0.65f),
                        copiedTint = Ink
                    )
                }
            }
        }
    }
}

/**
 * Single source-of-truth AI Assistant message bubble.
 * Standardized across all AI chat interfaces, matching AI Workout Planner with full rich Markdown support.
 */
@Composable
fun AiAssistantMessageBubble(
    markdown: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    trailingHeaderContent: (@Composable () -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentWidth(Alignment.Start)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(Elevated)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!badgeText.isNullOrBlank() || trailingHeaderContent != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!badgeText.isNullOrBlank()) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        trailingHeaderContent?.invoke()
                    }
                }

                if (markdown.isNotBlank()) {
                    AiMarkdownText(
                        markdown = markdown,
                        color = PrimaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MessageCopyButton(
                            textToCopy = markdown,
                            cleanMarkdown = true,
                            tint = MutedText
                        )
                    }
                }

                extraContent?.invoke()
            }
        }
    }
}
