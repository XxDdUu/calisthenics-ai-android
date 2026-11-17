package com.calisthenics.ai.ui.components.markdown

/**
 * Representation of an alignment in a Markdown table column.
 */
enum class TableAlignment {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Represents inline Markdown formatting spans.
 */
sealed interface MarkdownSpan {
    data class Text(val content: String) : MarkdownSpan
    data class Bold(val content: List<MarkdownSpan>) : MarkdownSpan
    data class Italic(val content: List<MarkdownSpan>) : MarkdownSpan
    data class BoldItalic(val content: List<MarkdownSpan>) : MarkdownSpan
    data class Strikethrough(val content: List<MarkdownSpan>) : MarkdownSpan
    data class InlineCode(val code: String) : MarkdownSpan
    data class Link(val text: String, val url: String, val content: List<MarkdownSpan>) : MarkdownSpan
}

/**
 * A single cell in a Markdown table.
 */
data class TableCell(
    val rawText: String,
    val spans: List<MarkdownSpan>
)

/**
 * An item in an ordered, unordered, or checklist list.
 */
data class MarkdownListItem(
    val depth: Int,
    val isOrdered: Boolean,
    val number: Int?,
    val isChecked: Boolean?, // null for normal bullet/number, true for [x], false for [ ]
    val text: String,
    val spans: List<MarkdownSpan>
)

/**
 * Top-level block elements parsed from a Markdown document.
 */
sealed interface MarkdownBlock {
    data class Heading(
        val level: Int,
        val text: String,
        val spans: List<MarkdownSpan>
    ) : MarkdownBlock

    data class Paragraph(
        val text: String,
        val spans: List<MarkdownSpan>
    ) : MarkdownBlock

    data class CodeBlock(
        val language: String?,
        val code: String
    ) : MarkdownBlock

    data class Blockquote(
        val blocks: List<MarkdownBlock>
    ) : MarkdownBlock

    data class ListGroup(
        val items: List<MarkdownListItem>
    ) : MarkdownBlock

    data class Table(
        val headers: List<TableCell>,
        val alignments: List<TableAlignment>,
        val rows: List<List<TableCell>>
    ) : MarkdownBlock

    object HorizontalRule : MarkdownBlock
}
