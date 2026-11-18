package com.calisthenics.ai.ui.components.markdown

import java.util.regex.Pattern

object MarkdownParser {

    private val HEADING_REGEX = Pattern.compile("^(#{1,6})\\s+(.+)$")
    private val HORIZONTAL_RULE_REGEX = Pattern.compile("^[ ]{0,3}([-*_])(?:\\s*\\1){2,}\\s*$")
    private val UNORDERED_LIST_REGEX = Pattern.compile("^([ \\t]*)([-*+])\\s+(.*)$")
    private val CHECKLIST_REGEX = Pattern.compile("^([ \\t]*)([-*+])\\s+\\[([ xX])\\]\\s+(.*)$")
    private val ORDERED_LIST_REGEX = Pattern.compile("^([ \\t]*)(\\d+)\\.\\s+(.*)$")
    private val TABLE_SEPARATOR_REGEX = Pattern.compile("^\\s*\\|?\\s*(:?-+:?\\s*\\|\\s*)+:?-+:?\\s*\\|?\\s*$")

    fun parseBlocks(markdown: String): List<MarkdownBlock> = parse(markdown)

    /**
     * Parses the markdown string into a list of structured [MarkdownBlock] elements.
     * Safe for streaming: will not crash on incomplete tags, unclosed fences, or partial tables.
     */
    fun parse(markdown: String): List<MarkdownBlock> {
        if (markdown.isBlank()) return emptyList()

        val normalized = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split("\n")
        val blocks = mutableListOf<MarkdownBlock>()

        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            // 1. Check for fenced code block (e.g. ```kotlin ... ```)
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim().takeIf { it.isNotEmpty() }
                val codeLines = mutableListOf<String>()
                index++
                while (index < lines.size) {
                    val currentLine = lines[index]
                    if (currentLine.trim().startsWith("```")) {
                        index++
                        break
                    }
                    codeLines.add(currentLine)
                    index++
                }
                blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
                continue
            }

            // 2. Blank line -> skip
            if (trimmed.isEmpty()) {
                index++
                continue
            }

            // 3. Horizontal Rule (e.g. ---, ***, ___)
            if (HORIZONTAL_RULE_REGEX.matcher(trimmed).matches()) {
                blocks.add(MarkdownBlock.HorizontalRule)
                index++
                continue
            }

            // 4. Headings (# H1, ## H2, ### H3, etc.)
            val headingMatcher = HEADING_REGEX.matcher(trimmed)
            if (headingMatcher.matches()) {
                val level = headingMatcher.group(1)?.length ?: 1
                val content = headingMatcher.group(2) ?: ""
                blocks.add(
                    MarkdownBlock.Heading(
                        level = level,
                        text = content,
                        spans = parseInline(content)
                    )
                )
                index++
                continue
            }

            // 5. Blockquotes (> ...)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (index < lines.size) {
                    val currentLine = lines[index]
                    val currentTrimmed = currentLine.trim()
                    if (currentTrimmed.startsWith(">")) {
                        // strip single leading '>' and optional space
                        val stripped = currentTrimmed.substring(1).let {
                            if (it.startsWith(" ")) it.substring(1) else it
                        }
                        quoteLines.add(stripped)
                        index++
                    } else if (currentTrimmed.isEmpty()) {
                        break
                    } else {
                        // Continuation of blockquote paragraph
                        quoteLines.add(currentTrimmed)
                        index++
                    }
                }
                val nestedBlocks = parse(quoteLines.joinToString("\n"))
                blocks.add(MarkdownBlock.Blockquote(nestedBlocks))
                continue
            }

            // 6. Tables (| Col 1 | Col 2 |)
            if (line.contains("|") && index + 1 < lines.size && TABLE_SEPARATOR_REGEX.matcher(lines[index + 1]).matches()) {
                val headerLine = line
                val separatorLine = lines[index + 1]
                val alignments = parseTableAlignments(separatorLine)
                val headers = parseTableRow(headerLine)

                val rows = mutableListOf<List<TableCell>>()
                index += 2
                while (index < lines.size) {
                    val currentLine = lines[index]
                    val currentTrimmed = currentLine.trim()
                    if (currentTrimmed.isEmpty() || !currentTrimmed.contains("|")) {
                        break
                    }
                    rows.add(parseTableRow(currentLine))
                    index++
                }

                blocks.add(MarkdownBlock.Table(headers, alignments, rows))
                continue
            }

            // 7. Lists (Unordered, Checklists, Ordered)
            val checklistMatcher = CHECKLIST_REGEX.matcher(line)
            val unorderedMatcher = UNORDERED_LIST_REGEX.matcher(line)
            val orderedMatcher = ORDERED_LIST_REGEX.matcher(line)

            if (checklistMatcher.matches() || unorderedMatcher.matches() || orderedMatcher.matches()) {
                val listItems = mutableListOf<MarkdownListItem>()
                while (index < lines.size) {
                    val currentLine = lines[index]
                    val chkMatch = CHECKLIST_REGEX.matcher(currentLine)
                    val unordMatch = UNORDERED_LIST_REGEX.matcher(currentLine)
                    val ordMatch = ORDERED_LIST_REGEX.matcher(currentLine)

                    if (chkMatch.matches()) {
                        val indent = chkMatch.group(1) ?: ""
                        val depth = calculateIndentDepth(indent)
                        val checkChar = chkMatch.group(3) ?: " "
                        val isChecked = checkChar.equals("x", ignoreCase = true)
                        val content = chkMatch.group(4) ?: ""
                        listItems.add(
                            MarkdownListItem(
                                depth = depth,
                                isOrdered = false,
                                number = null,
                                isChecked = isChecked,
                                text = content,
                                spans = parseInline(content)
                            )
                        )
                        index++
                    } else if (unordMatch.matches()) {
                        val indent = unordMatch.group(1) ?: ""
                        val depth = calculateIndentDepth(indent)
                        val content = unordMatch.group(3) ?: ""
                        listItems.add(
                            MarkdownListItem(
                                depth = depth,
                                isOrdered = false,
                                number = null,
                                isChecked = null,
                                text = content,
                                spans = parseInline(content)
                            )
                        )
                        index++
                    } else if (ordMatch.matches()) {
                        val indent = ordMatch.group(1) ?: ""
                        val depth = calculateIndentDepth(indent)
                        val num = ordMatch.group(2)?.toIntOrNull() ?: (listItems.size + 1)
                        val content = ordMatch.group(3) ?: ""
                        listItems.add(
                            MarkdownListItem(
                                depth = depth,
                                isOrdered = true,
                                number = num,
                                isChecked = null,
                                text = content,
                                spans = parseInline(content)
                            )
                        )
                        index++
                    } else {
                        break
                    }
                }
                blocks.add(MarkdownBlock.ListGroup(listItems))
                continue
            }

            // 8. Paragraphs
            val paragraphLines = mutableListOf<String>()
            while (index < lines.size) {
                val currentLine = lines[index]
                val currentTrimmed = currentLine.trim()
                if (currentTrimmed.isEmpty() ||
                    currentTrimmed.startsWith("```") ||
                    currentTrimmed.startsWith(">") ||
                    HEADING_REGEX.matcher(currentTrimmed).matches() ||
                    HORIZONTAL_RULE_REGEX.matcher(currentTrimmed).matches() ||
                    CHECKLIST_REGEX.matcher(currentLine).matches() ||
                    UNORDERED_LIST_REGEX.matcher(currentLine).matches() ||
                    ORDERED_LIST_REGEX.matcher(currentLine).matches() ||
                    (currentLine.contains("|") && index + 1 < lines.size && TABLE_SEPARATOR_REGEX.matcher(lines[index + 1]).matches())
                ) {
                    break
                }
                paragraphLines.add(currentLine)
                index++
            }

            val paragraphText = paragraphLines.joinToString("\n")
            blocks.add(
                MarkdownBlock.Paragraph(
                    text = paragraphText,
                    spans = parseInline(paragraphText)
                )
            )
        }

        return blocks
    }

    private fun calculateIndentDepth(indent: String): Int {
        var spaces = 0
        for (c in indent) {
            when (c) {
                '\t' -> spaces += 4
                ' ' -> spaces += 1
            }
        }
        return (spaces / 2).coerceAtMost(5) // Every 2-4 spaces is a nesting depth level
    }

    private fun parseTableRow(line: String): List<TableCell> {
        val trimmed = line.trim()
        val stripped = trimmed.removePrefix("|").removeSuffix("|")
        return stripped.split("|").map { cellText ->
            val clean = cellText.trim()
            TableCell(clean, parseInline(clean))
        }
    }

    private fun parseTableAlignments(separatorLine: String): List<TableAlignment> {
        val trimmed = separatorLine.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split("|").map { col ->
            val clean = col.trim()
            val left = clean.startsWith(":")
            val right = clean.endsWith(":")
            when {
                left && right -> TableAlignment.CENTER
                right -> TableAlignment.RIGHT
                else -> TableAlignment.LEFT
            }
        }
    }

    /**
     * Parses inline markdown elements:
     * - Bold+Italic (*** or ___)
     * - Bold (** or __)
     * - Italic (* or _)
     * - Strikethrough (~~)
     * - Inline code (`)
     * - Links ([text](url))
     * - Auto URLs (https://... or http://...)
     */
    fun parseInline(text: String): List<MarkdownSpan> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<MarkdownSpan>()
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Find earliest marker
            val boldItalicIndex = findDelim(text, cursor, "***")
            val boldIndex = findDelim(text, cursor, "**")
            val strikeIndex = findDelim(text, cursor, "~~")
            val codeIndex = text.indexOf('`', cursor)
            val linkIndex = text.indexOf('[', cursor)
            val autoUrlIndex = findUrlStart(text, cursor)

            // Underscore bold/italic handled carefully to avoid snake_case
            val underBoldIndex = findUnderscoreDelim(text, cursor, "__")
            val starItalicIndex = findStarItalicDelim(text, cursor)
            val underItalicIndex = findUnderscoreDelim(text, cursor, "_")

            // Determine earliest trigger
            var nextIndex = len
            var markerType = MarkerType.NONE

            fun checkCandidate(candidateIndex: Int, type: MarkerType) {
                if (candidateIndex != -1 && candidateIndex < nextIndex) {
                    nextIndex = candidateIndex
                    markerType = type
                }
            }

            checkCandidate(boldItalicIndex, MarkerType.BOLD_ITALIC)
            checkCandidate(boldIndex, MarkerType.BOLD)
            checkCandidate(underBoldIndex, MarkerType.BOLD_UNDERSCORE)
            checkCandidate(strikeIndex, MarkerType.STRIKETHROUGH)
            checkCandidate(codeIndex, MarkerType.INLINE_CODE)
            checkCandidate(linkIndex, MarkerType.LINK)
            checkCandidate(autoUrlIndex, MarkerType.AUTO_URL)
            checkCandidate(starItalicIndex, MarkerType.ITALIC_STAR)
            checkCandidate(underItalicIndex, MarkerType.ITALIC_UNDERSCORE)

            if (markerType == MarkerType.NONE) {
                // No more markers
                result.add(MarkdownSpan.Text(text.substring(cursor)))
                break
            }

            if (nextIndex > cursor) {
                result.add(MarkdownSpan.Text(text.substring(cursor, nextIndex)))
                cursor = nextIndex
            }

            when (markerType) {
                MarkerType.BOLD_ITALIC -> {
                    val closing = findDelim(text, cursor + 3, "***")
                    if (closing != -1) {
                        val inner = text.substring(cursor + 3, closing)
                        result.add(MarkdownSpan.BoldItalic(parseInline(inner)))
                        cursor = closing + 3
                    } else {
                        // Unclosed streaming tag: render style to end
                        val inner = text.substring(cursor + 3)
                        result.add(MarkdownSpan.BoldItalic(parseInline(inner)))
                        cursor = len
                    }
                }
                MarkerType.BOLD -> {
                    val closing = findDelim(text, cursor + 2, "**")
                    if (closing != -1) {
                        val inner = text.substring(cursor + 2, closing)
                        result.add(MarkdownSpan.Bold(parseInline(inner)))
                        cursor = closing + 2
                    } else {
                        // Unclosed streaming tag
                        val inner = text.substring(cursor + 2)
                        result.add(MarkdownSpan.Bold(parseInline(inner)))
                        cursor = len
                    }
                }
                MarkerType.BOLD_UNDERSCORE -> {
                    val closing = findUnderscoreDelim(text, cursor + 2, "__")
                    if (closing != -1) {
                        val inner = text.substring(cursor + 2, closing)
                        result.add(MarkdownSpan.Bold(parseInline(inner)))
                        cursor = closing + 2
                    } else {
                        val inner = text.substring(cursor + 2)
                        result.add(MarkdownSpan.Bold(parseInline(inner)))
                        cursor = len
                    }
                }
                MarkerType.ITALIC_STAR -> {
                    val closing = findStarItalicClosing(text, cursor + 1)
                    if (closing != -1) {
                        val inner = text.substring(cursor + 1, closing)
                        result.add(MarkdownSpan.Italic(parseInline(inner)))
                        cursor = closing + 1
                    } else {
                        // If it wasn't a closed asterisk, treat as single text asterisk
                        result.add(MarkdownSpan.Text("*"))
                        cursor += 1
                    }
                }
                MarkerType.ITALIC_UNDERSCORE -> {
                    val closing = findUnderscoreDelim(text, cursor + 1, "_")
                    if (closing != -1) {
                        val inner = text.substring(cursor + 1, closing)
                        result.add(MarkdownSpan.Italic(parseInline(inner)))
                        cursor = closing + 1
                    } else {
                        result.add(MarkdownSpan.Text("_"))
                        cursor += 1
                    }
                }
                MarkerType.STRIKETHROUGH -> {
                    val closing = findDelim(text, cursor + 2, "~~")
                    if (closing != -1) {
                        val inner = text.substring(cursor + 2, closing)
                        result.add(MarkdownSpan.Strikethrough(parseInline(inner)))
                        cursor = closing + 2
                    } else {
                        val inner = text.substring(cursor + 2)
                        result.add(MarkdownSpan.Strikethrough(parseInline(inner)))
                        cursor = len
                    }
                }
                MarkerType.INLINE_CODE -> {
                    val closing = text.indexOf('`', cursor + 1)
                    if (closing != -1) {
                        val code = text.substring(cursor + 1, closing)
                        result.add(MarkdownSpan.InlineCode(code))
                        cursor = closing + 1
                    } else {
                        val code = text.substring(cursor + 1)
                        result.add(MarkdownSpan.InlineCode(code))
                        cursor = len
                    }
                }
                MarkerType.LINK -> {
                    val closeBracket = text.indexOf(']', cursor + 1)
                    if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val linkText = text.substring(cursor + 1, closeBracket)
                            val linkUrl = text.substring(closeBracket + 2, closeParen).trim()
                            result.add(MarkdownSpan.Link(linkText, linkUrl, parseInline(linkText)))
                            cursor = closeParen + 1
                        } else {
                            result.add(MarkdownSpan.Text("["))
                            cursor += 1
                        }
                    } else {
                        result.add(MarkdownSpan.Text("["))
                        cursor += 1
                    }
                }
                MarkerType.AUTO_URL -> {
                    var endUrl = cursor
                    while (endUrl < len && !text[endUrl].isWhitespace() && text[endUrl] !in ")],\"'<>") {
                        endUrl++
                    }
                    val url = text.substring(cursor, endUrl)
                    result.add(MarkdownSpan.Link(url, url, listOf(MarkdownSpan.Text(url))))
                    cursor = endUrl
                }
                MarkerType.NONE -> {
                    cursor++
                }
            }
        }

        return result
    }

    private enum class MarkerType {
        NONE,
        BOLD_ITALIC,
        BOLD,
        BOLD_UNDERSCORE,
        ITALIC_STAR,
        ITALIC_UNDERSCORE,
        STRIKETHROUGH,
        INLINE_CODE,
        LINK,
        AUTO_URL
    }

    private fun findDelim(text: String, start: Int, delim: String): Int {
        return text.indexOf(delim, start)
    }

    private fun findStarItalicDelim(text: String, start: Int): Int {
        var idx = start
        while (idx < text.length) {
            val i = text.indexOf('*', idx)
            if (i == -1) return -1
            // Ensure not part of ** or ***
            val prev = if (i > 0) text[i - 1] else ' '
            val next = if (i + 1 < text.length) text[i + 1] else ' '
            if (prev != '*' && next != '*' && !next.isWhitespace()) {
                return i
            }
            idx = i + 1
        }
        return -1
    }

    private fun findStarItalicClosing(text: String, start: Int): Int {
        var idx = start
        while (idx < text.length) {
            val i = text.indexOf('*', idx)
            if (i == -1) return -1
            val prev = if (i > 0) text[i - 1] else ' '
            val next = if (i + 1 < text.length) text[i + 1] else ' '
            if (prev != '*' && next != '*' && !prev.isWhitespace()) {
                return i
            }
            idx = i + 1
        }
        return -1
    }

    private fun findUnderscoreDelim(text: String, start: Int, delim: String): Int {
        val i = text.indexOf(delim, start)
        if (i == -1) return -1
        // Prevent mid-word match like snake_case
        val prev = if (i > 0) text[i - 1] else ' '
        val next = if (i + delim.length < text.length) text[i + delim.length] else ' '
        if (delim == "_" && (prev.isLetterOrDigit() && next.isLetterOrDigit())) {
            return -1
        }
        return i
    }

    private fun findUrlStart(text: String, start: Int): Int {
        val http = text.indexOf("http://", start)
        val https = text.indexOf("https://", start)
        return when {
            http != -1 && https != -1 -> minOf(http, https)
            http != -1 -> http
            else -> https
        }
    }

    /**
     * Converts Markdown text into clean, human-readable plain text suitable for clipboard copying,
     * preserving structure (headings, lists, paragraphs, tables, and code contents) without raw markers.
     */
    fun toPlainText(markdown: String): String {
        if (markdown.isBlank()) return markdown
        val blocks = parse(markdown)
        if (blocks.isEmpty()) return markdown.trim()
        val sb = StringBuilder()
        appendBlocksPlainText(blocks, sb)
        return sb.toString().trim()
    }

    private fun appendBlocksPlainText(blocks: List<MarkdownBlock>, sb: StringBuilder) {
        blocks.forEachIndexed { index, block ->
            if (index > 0 && sb.isNotEmpty() && !sb.endsWith("\n\n")) {
                if (!sb.endsWith("\n")) sb.append("\n")
                sb.append("\n")
            }
            when (block) {
                is MarkdownBlock.Heading -> {
                    sb.append(spansToPlainText(block.spans))
                }
                is MarkdownBlock.Paragraph -> {
                    sb.append(spansToPlainText(block.spans))
                }
                is MarkdownBlock.CodeBlock -> {
                    sb.append(block.code.trimEnd())
                }
                is MarkdownBlock.HorizontalRule -> {
                    sb.append("---")
                }
                is MarkdownBlock.Blockquote -> {
                    val nested = StringBuilder()
                    appendBlocksPlainText(block.blocks, nested)
                    val lines = nested.toString().trim().split("\n")
                    sb.append(lines.joinToString("\n") { line ->
                        if (line.isBlank()) "" else "> $line"
                    })
                }
                is MarkdownBlock.ListGroup -> {
                    block.items.forEachIndexed { itemIdx, item ->
                        if (itemIdx > 0) sb.append("\n")
                        val indent = "  ".repeat(item.depth.coerceAtLeast(0))
                        val prefix = when {
                            item.isChecked == true -> "[x] "
                            item.isChecked == false -> "[ ] "
                            item.isOrdered -> "${item.number ?: (itemIdx + 1)}. "
                            else -> "• "
                        }
                        sb.append(indent).append(prefix).append(spansToPlainText(item.spans))
                    }
                }
                is MarkdownBlock.Table -> {
                    if (block.headers.isNotEmpty()) {
                        sb.append(block.headers.joinToString(" | ") { spansToPlainText(it.spans) })
                    }
                    for (row in block.rows) {
                        if (sb.isNotEmpty()) sb.append("\n")
                        sb.append(row.joinToString(" | ") { spansToPlainText(it.spans) })
                    }
                }
            }
        }
    }

    private fun spansToPlainText(spans: List<MarkdownSpan>): String {
        val sb = StringBuilder()
        for (span in spans) {
            when (span) {
                is MarkdownSpan.Text -> sb.append(span.content)
                is MarkdownSpan.Bold -> sb.append(spansToPlainText(span.content))
                is MarkdownSpan.Italic -> sb.append(spansToPlainText(span.content))
                is MarkdownSpan.BoldItalic -> sb.append(spansToPlainText(span.content))
                is MarkdownSpan.Strikethrough -> sb.append(spansToPlainText(span.content))
                is MarkdownSpan.InlineCode -> sb.append(span.code)
                is MarkdownSpan.Link -> {
                    val label = spansToPlainText(span.content).ifBlank { span.text }
                    if (label == span.url || span.url.isBlank()) {
                        sb.append(label)
                    } else {
                        sb.append("$label (${span.url})")
                    }
                }
            }
        }
        return sb.toString()
    }
}
