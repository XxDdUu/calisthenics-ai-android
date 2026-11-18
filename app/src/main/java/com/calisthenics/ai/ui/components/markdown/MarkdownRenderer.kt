package com.calisthenics.ai.ui.components.markdown

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Rich, performant Jetpack Compose Markdown renderer for AI Assistant responses across SKY.
 * Handles headings, tables, syntax-highlighted code blocks with copy-to-clipboard, nested lists,
 * checklists, blockquotes, horizontal dividers, links, and text formatting.
 */
@Composable
fun AiMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalAppColors.current.primaryText
) {
    if (markdown.isBlank()) return

    val blocks = remember(markdown) {
        MarkdownParser.parseBlocks(markdown)
    }

    val appColors = LocalAppColors.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Heading -> {
                    HeadingBlock(block = block, appColors = appColors)
                }
                is MarkdownBlock.Paragraph -> {
                    MarkdownInlineText(
                        spans = block.spans,
                        style = textStyle,
                        color = color
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        block = block,
                        isDarkTheme = appColors.isDark
                    )
                }
                is MarkdownBlock.Table -> {
                    TableBlockView(
                        block = block,
                        appColors = appColors
                    )
                }
                is MarkdownBlock.ListGroup -> {
                    ListGroupView(
                        block = block,
                        style = textStyle,
                        color = color,
                        accentColor = appColors.accent
                    )
                }
                is MarkdownBlock.Blockquote -> {
                    BlockquoteView(
                        block = block,
                        style = textStyle,
                        color = color,
                        accentColor = appColors.accent
                    )
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = appColors.subtleBorder,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadingBlock(
    block: MarkdownBlock.Heading,
    appColors: com.calisthenics.ai.ui.theme.AppColors
) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = appColors.primaryText
        )
        2 -> MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = appColors.accent
        )
        3 -> MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = appColors.primaryText
        )
        else -> MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = appColors.primaryText
        )
    }

    val topPadding = when (block.level) {
        1 -> 8.dp
        2 -> 6.dp
        3 -> 4.dp
        else -> 2.dp
    }

    Box(modifier = Modifier.padding(top = topPadding)) {
        MarkdownInlineText(
            spans = block.spans,
            style = style,
            color = style.color
        )
    }
}

@Composable
private fun ListGroupView(
    block: MarkdownBlock.ListGroup,
    style: TextStyle,
    color: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (item in block.items) {
            val startPadding = (item.depth * 14).dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.isChecked != null) {
                    // Checklist item
                    Icon(
                        imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (item.isChecked) "Checked" else "Unchecked",
                        tint = if (item.isChecked) accentColor else color.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    MarkdownInlineText(
                        spans = item.spans,
                        style = if (item.isChecked) {
                            style.copy(textDecoration = TextDecoration.LineThrough, color = color.copy(alpha = 0.7f))
                        } else {
                            style
                        },
                        color = if (item.isChecked) color.copy(alpha = 0.7f) else color
                    )
                } else if (item.isOrdered) {
                    val num = item.number ?: 1
                    Text(
                        text = "$num.",
                        style = style.copy(fontWeight = FontWeight.Bold, color = accentColor),
                        modifier = Modifier.widthIn(min = 18.dp)
                    )
                    MarkdownInlineText(
                        spans = item.spans,
                        style = style,
                        color = color
                    )
                } else {
                    // Unordered bullet
                    val bulletChar = when (item.depth % 3) {
                        0 -> "•"
                        1 -> "◦"
                        else -> "▪"
                    }
                    Text(
                        text = bulletChar,
                        style = style.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = 15.sp
                        )
                    )
                    MarkdownInlineText(
                        spans = item.spans,
                        style = style,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockquoteView(
    block: MarkdownBlock.Blockquote,
    style: TextStyle,
    color: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor.copy(alpha = 0.7f))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (innerBlock in block.blocks) {
                when (innerBlock) {
                    is MarkdownBlock.Paragraph -> {
                        MarkdownInlineText(
                            spans = innerBlock.spans,
                            style = style.copy(fontStyle = FontStyle.Italic),
                            color = color.copy(alpha = 0.9f)
                        )
                    }
                    else -> {
                        // Render other block types within blockquote if any
                        AiMarkdownText(
                            markdown = when (innerBlock) {
                                is MarkdownBlock.Heading -> "#".repeat(innerBlock.level) + " " + innerBlock.text
                                else -> ""
                            },
                            textStyle = style.copy(fontStyle = FontStyle.Italic),
                            color = color.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(
    block: MarkdownBlock.CodeBlock,
    isDarkTheme: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    val highlightedCode = remember(block.code, block.language, isDarkTheme) {
        MarkdownCodeHighlighter.highlight(block.code, block.language, isDarkTheme)
    }

    val containerBg = if (isDarkTheme) Color(0xFF16181A) else Color(0xFFF6F8FA)
    val headerBg = if (isDarkTheme) Color(0xFF21252B) else Color(0xFFEAECEF)
    val borderColor = if (isDarkTheme) Color(0xFF30363D) else Color(0xFFD0D7DE)
    val textColor = if (isDarkTheme) Color(0xFF8B949E) else Color(0xFF57606A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(containerBg)
    ) {
        Column {
            // Header bar with language badge and copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = (block.language?.takeIf { it.isNotBlank() } ?: "CODE").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedVisibility(visible = copied, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = "Copied!",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) Color(0xFF7EE787) else Color(0xFF1A7F37),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(block.code))
                            copied = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (copied) {
                                if (isDarkTheme) Color(0xFF7EE787) else Color(0xFF1A7F37)
                            } else {
                                textColor
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Scrollable code area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = highlightedCode,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TableBlockView(
    block: MarkdownBlock.Table,
    appColors: com.calisthenics.ai.ui.theme.AppColors
) {
    if (block.headers.isEmpty()) return

    val borderColor = appColors.subtleBorder
    val headerBg = appColors.surfaceVariant.copy(alpha = 0.7f)
    val alternateRowBg = appColors.surfaceVariant.copy(alpha = 0.25f)
    val baseRowBg = Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(headerBg)
                    .padding(vertical = 8.dp)
            ) {
                block.headers.forEachIndexed { colIdx, headerCell ->
                    val alignment = block.alignments.getOrElse(colIdx) { TableAlignment.LEFT }
                    TableCellView(
                        cell = headerCell,
                        alignment = alignment,
                        isHeader = true,
                        appColors = appColors
                    )
                }
            }

            HorizontalDivider(color = borderColor, thickness = 1.dp)

            // Data Rows
            block.rows.forEachIndexed { rowIdx, rowCells ->
                val bg = if (rowIdx % 2 == 1) alternateRowBg else baseRowBg
                Row(
                    modifier = Modifier
                        .background(bg)
                        .padding(vertical = 7.dp)
                ) {
                    rowCells.forEachIndexed { colIdx, cell ->
                        val alignment = block.alignments.getOrElse(colIdx) { TableAlignment.LEFT }
                        TableCellView(
                            cell = cell,
                            alignment = alignment,
                            isHeader = false,
                            appColors = appColors
                        )
                    }
                }
                if (rowIdx < block.rows.size - 1) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TableCellView(
    cell: TableCell,
    alignment: TableAlignment,
    isHeader: Boolean,
    appColors: com.calisthenics.ai.ui.theme.AppColors
) {
    val textAlign = when (alignment) {
        TableAlignment.LEFT -> TextAlign.Start
        TableAlignment.CENTER -> TextAlign.Center
        TableAlignment.RIGHT -> TextAlign.End
    }

    val style = if (isHeader) {
        MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            color = appColors.primaryText,
            textAlign = textAlign
        )
    } else {
        MaterialTheme.typography.bodySmall.copy(
            color = appColors.primaryText,
            textAlign = textAlign
        )
    }

    Box(
        modifier = Modifier
            .widthIn(min = 100.dp, max = 220.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = when (alignment) {
            TableAlignment.LEFT -> Alignment.CenterStart
            TableAlignment.CENTER -> Alignment.Center
            TableAlignment.RIGHT -> Alignment.CenterEnd
        }
    ) {
        MarkdownInlineText(
            spans = cell.spans,
            style = style,
            color = appColors.primaryText
        )
    }
}

@Composable
private fun MarkdownInlineText(
    spans: List<MarkdownSpan>,
    style: TextStyle,
    color: Color
) {
    val appColors = LocalAppColors.current
    val annotated = remember(spans, color, appColors.accent, appColors.isDark) {
        buildAnnotatedString {
            appendSpans(
                spans = spans,
                accentColor = appColors.accent,
                codeBgColor = if (appColors.isDark) Color(0xFF24292E) else Color(0xFFE1E4E8)
            )
        }
    }

    Text(
        text = annotated,
        style = style,
        color = color
    )
}

private fun AnnotatedString.Builder.appendSpans(
    spans: List<MarkdownSpan>,
    accentColor: Color,
    codeBgColor: Color
) {
    for (span in spans) {
        when (span) {
            is MarkdownSpan.Text -> {
                append(span.content)
            }
            is MarkdownSpan.Bold -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendSpans(span.content, accentColor, codeBgColor)
                }
            }
            is MarkdownSpan.Italic -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendSpans(span.content, accentColor, codeBgColor)
                }
            }
            is MarkdownSpan.BoldItalic -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    appendSpans(span.content, accentColor, codeBgColor)
                }
            }
            is MarkdownSpan.Strikethrough -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendSpans(span.content, accentColor, codeBgColor)
                }
            }
            is MarkdownSpan.InlineCode -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(" ${span.code} ")
                }
            }
            is MarkdownSpan.Link -> {
                val linkStyle = TextLinkStyles(
                    style = SpanStyle(
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )
                )
                withLink(LinkAnnotation.Url(url = span.url, styles = linkStyle)) {
                    if (span.content.isNotEmpty()) {
                        appendSpans(span.content, accentColor, codeBgColor)
                    } else {
                        append(span.text)
                    }
                }
            }
        }
    }
}
