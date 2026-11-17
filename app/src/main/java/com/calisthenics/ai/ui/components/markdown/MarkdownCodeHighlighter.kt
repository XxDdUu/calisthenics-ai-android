package com.calisthenics.ai.ui.components.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Lightweight, zero-dependency syntax highlighter for Markdown code blocks.
 * Supports Kotlin/Java, Python, SQL, JSON, Bash/Shell, and general code.
 */
object MarkdownCodeHighlighter {

    data class SyntaxColors(
        val keyword: Color = Color(0xFFFF7B72),      // Coral / Red-orange
        val string: Color = Color(0xFFA5D6FF),       // Light Blue
        val number: Color = Color(0xFF79C0FF),       // Cyan-Blue
        val comment: Color = Color(0xFF8B949E),      // Muted Gray
        val booleanOrNull: Color = Color(0xFFFF9E64),// Amber
        val punctuation: Color = Color(0xFFC9D1D9),  // Light Gray
        val plainText: Color = Color(0xFFF0F6FC)     // Off-white
    )

    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
        "in", "interface", "is", "null", "object", "package", "return", "super", "this",
        "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
        "private", "protected", "public", "internal", "data", "sealed", "suspend", "override"
    )

    private val PYTHON_KEYWORDS = setOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
        "raise", "return", "True", "try", "while", "with", "yield"
    )

    private val SQL_KEYWORDS = setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "JOIN", "LEFT",
        "RIGHT", "INNER", "OUTER", "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT",
        "OFFSET", "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "AS", "AND", "OR",
        "NOT", "NULL", "IS", "IN", "BETWEEN", "LIKE", "CASE", "WHEN", "THEN", "ELSE", "END",
        "COUNT", "SUM", "AVG", "MIN", "MAX", "DISTINCT", "UNION", "ALL", "EXISTS"
    )

    private val BASH_KEYWORDS = setOf(
        "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "until",
        "do", "done", "in", "function", "select", "time", "export", "source", "alias",
        "echo", "cd", "ls", "grep", "cat", "mkdir", "rm", "cp", "mv", "sudo", "chmod"
    )

    /**
     * Highlights code text according to detected or specified language.
     */
    fun highlight(
        code: String,
        language: String?,
        isDarkTheme: Boolean = true
    ): AnnotatedString {
        val lang = language?.trim()?.lowercase() ?: ""
        val colors = if (isDarkTheme) {
            SyntaxColors()
        } else {
            SyntaxColors(
                keyword = Color(0xFFD73A49),
                string = Color(0xFF032F62),
                number = Color(0xFF005CC5),
                comment = Color(0xFF6A737D),
                booleanOrNull = Color(0xFFE36209),
                punctuation = Color(0xFF24292E),
                plainText = Color(0xFF24292E)
            )
        }

        return when (lang) {
            "json" -> highlightJson(code, colors)
            "sql" -> highlightSql(code, colors)
            "python", "py" -> highlightKeywordLanguage(code, PYTHON_KEYWORDS, commentPrefix = "#", colors)
            "kotlin", "kt", "java" -> highlightKeywordLanguage(code, KOTLIN_KEYWORDS, commentPrefix = "//", colors)
            "bash", "sh", "shell", "zsh" -> highlightKeywordLanguage(code, BASH_KEYWORDS, commentPrefix = "#", colors)
            else -> highlightGeneric(code, colors)
        }
    }

    private fun highlightJson(code: String, colors: SyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = code.length

            while (i < len) {
                val c = code[i]
                when {
                    c == '"' -> {
                        val start = i
                        i++
                        var escaped = false
                        while (i < len) {
                            val curr = code[i]
                            if (curr == '\\' && !escaped) {
                                escaped = true
                            } else if (curr == '"' && !escaped) {
                                i++
                                break
                            } else {
                                escaped = false
                            }
                            i++
                        }
                        val strToken = code.substring(start, i)
                        // Check if it's a key or a value
                        val rest = code.substring(i).trimStart()
                        val isKey = rest.startsWith(":")
                        val color = if (isKey) colors.keyword else colors.string
                        pushStyle(SpanStyle(color = color, fontWeight = if (isKey) FontWeight.SemiBold else FontWeight.Normal))
                        append(strToken)
                        pop()
                    }
                    c.isDigit() || (c == '-' && i + 1 < len && code[i + 1].isDigit()) -> {
                        val start = i
                        i++
                        while (i < len && (code[i].isDigit() || code[i] == '.' || code[i] == 'e' || code[i] == 'E' || code[i] == '+' || code[i] == '-')) {
                            i++
                        }
                        pushStyle(SpanStyle(color = colors.number))
                        append(code.substring(start, i))
                        pop()
                    }
                    code.startsWith("true", i) -> {
                        pushStyle(SpanStyle(color = colors.booleanOrNull, fontWeight = FontWeight.Bold))
                        append("true")
                        pop()
                        i += 4
                    }
                    code.startsWith("false", i) -> {
                        pushStyle(SpanStyle(color = colors.booleanOrNull, fontWeight = FontWeight.Bold))
                        append("false")
                        pop()
                        i += 5
                    }
                    code.startsWith("null", i) -> {
                        pushStyle(SpanStyle(color = colors.booleanOrNull, fontWeight = FontWeight.Bold))
                        append("null")
                        pop()
                        i += 4
                    }
                    c in "{}[],:" -> {
                        pushStyle(SpanStyle(color = colors.punctuation))
                        append(c)
                        pop()
                        i++
                    }
                    else -> {
                        pushStyle(SpanStyle(color = colors.plainText))
                        append(c)
                        pop()
                        i++
                    }
                }
            }
        }
    }

    private fun highlightSql(code: String, colors: SyntaxColors): AnnotatedString {
        val lines = code.split("\n")
        return buildAnnotatedString {
            lines.forEachIndexed { lineIdx, line ->
                val commentIdx = line.indexOf("--")
                val activePart = if (commentIdx != -1) line.substring(0, commentIdx) else line
                val commentPart = if (commentIdx != -1) line.substring(commentIdx) else ""

                var i = 0
                while (i < activePart.length) {
                    val c = activePart[i]
                    when {
                        c == '\'' || c == '"' -> {
                            val start = i
                            val quote = c
                            i++
                            while (i < activePart.length && activePart[i] != quote) {
                                i++
                            }
                            if (i < activePart.length) i++ // include closing quote
                            pushStyle(SpanStyle(color = colors.string))
                            append(activePart.substring(start, i))
                            pop()
                        }
                        c.isLetter() || c == '_' -> {
                            val start = i
                            while (i < activePart.length && (activePart[i].isLetterOrDigit() || activePart[i] == '_')) {
                                i++
                            }
                            val word = activePart.substring(start, i)
                            if (SQL_KEYWORDS.contains(word.uppercase())) {
                                pushStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold))
                                append(word)
                                pop()
                            } else {
                                pushStyle(SpanStyle(color = colors.plainText))
                                append(word)
                                pop()
                            }
                        }
                        c.isDigit() -> {
                            val start = i
                            while (i < activePart.length && (activePart[i].isDigit() || activePart[i] == '.')) {
                                i++
                            }
                            pushStyle(SpanStyle(color = colors.number))
                            append(activePart.substring(start, i))
                            pop()
                        }
                        else -> {
                            pushStyle(SpanStyle(color = colors.plainText))
                            append(c)
                            pop()
                            i++
                        }
                    }
                }

                if (commentPart.isNotEmpty()) {
                    pushStyle(SpanStyle(color = colors.comment))
                    append(commentPart)
                    pop()
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun highlightKeywordLanguage(
        code: String,
        keywords: Set<String>,
        commentPrefix: String,
        colors: SyntaxColors
    ): AnnotatedString {
        val lines = code.split("\n")
        return buildAnnotatedString {
            lines.forEachIndexed { lineIdx, line ->
                val commentIdx = line.indexOf(commentPrefix)
                val activePart = if (commentIdx != -1) line.substring(0, commentIdx) else line
                val commentPart = if (commentIdx != -1) line.substring(commentIdx) else ""

                var i = 0
                while (i < activePart.length) {
                    val c = activePart[i]
                    when {
                        c == '"' || c == '\'' -> {
                            val quote = c
                            val start = i
                            i++
                            var escaped = false
                            while (i < activePart.length) {
                                val curr = activePart[i]
                                if (curr == '\\' && !escaped) {
                                    escaped = true
                                } else if (curr == quote && !escaped) {
                                    i++
                                    break
                                } else {
                                    escaped = false
                                }
                                i++
                            }
                            pushStyle(SpanStyle(color = colors.string))
                            append(activePart.substring(start, i))
                            pop()
                        }
                        c.isLetter() || c == '_' -> {
                            val start = i
                            while (i < activePart.length && (activePart[i].isLetterOrDigit() || activePart[i] == '_')) {
                                i++
                            }
                            val word = activePart.substring(start, i)
                            if (keywords.contains(word)) {
                                pushStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold))
                                append(word)
                                pop()
                            } else if (word == "true" || word == "false" || word == "null" || word == "True" || word == "False" || word == "None") {
                                pushStyle(SpanStyle(color = colors.booleanOrNull, fontWeight = FontWeight.Bold))
                                append(word)
                                pop()
                            } else {
                                pushStyle(SpanStyle(color = colors.plainText))
                                append(word)
                                pop()
                            }
                        }
                        c.isDigit() -> {
                            val start = i
                            while (i < activePart.length && (activePart[i].isDigit() || activePart[i] == '.' || activePart[i] == 'f' || activePart[i] == 'L')) {
                                i++
                            }
                            pushStyle(SpanStyle(color = colors.number))
                            append(activePart.substring(start, i))
                            pop()
                        }
                        else -> {
                            pushStyle(SpanStyle(color = colors.plainText))
                            append(c)
                            pop()
                            i++
                        }
                    }
                }

                if (commentPart.isNotEmpty()) {
                    pushStyle(SpanStyle(color = colors.comment))
                    append(commentPart)
                    pop()
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun highlightGeneric(code: String, colors: SyntaxColors): AnnotatedString {
        return buildAnnotatedString {
            pushStyle(SpanStyle(color = colors.plainText))
            append(code)
            pop()
        }
    }
}
