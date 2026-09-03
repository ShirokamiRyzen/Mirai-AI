package com.ryzumi.miraiai.domain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

object MarkdownRenderer {

    fun parseMarkdown(
        text: String,
        actionColor: Color = Color(0xFFC3C7FF)
    ): AnnotatedString {
        if (text.isBlank()) return buildAnnotatedString { }

        val lines = text.split("\n")
        return buildAnnotatedString {
            var i = 0
            var inCodeBlock = false

            while (i < lines.size) {
                val rawLine = lines[i]
                val trimmed = rawLine.trim()

                // 1. Check for Code Block delimiter ```
                if (trimmed.startsWith("```")) {
                    inCodeBlock = !inCodeBlock
                    if (!inCodeBlock && i < lines.size - 1) {
                        append("\n")
                    }
                    i++
                    continue
                }

                if (inCodeBlock) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            background = Color.Black.copy(alpha = 0.35f),
                            color = Color(0xFFFFD580)
                        )
                    ) {
                        append(rawLine)
                    }
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 2. Check for Horizontal Rule: ---, ***, ___ (at least 3 characters on its own line)
                if (trimmed.length >= 3 && (trimmed.all { it == '-' } || trimmed.all { it == '*' } || trimmed.all { it == '_' })) {
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.25f), fontWeight = FontWeight.Light)) {
                        append("────────────────────────────────────────")
                    }
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 3. Check for Markdown Table (consecutive lines starting & ending with |)
                if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length > 2) {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|") && lines[i].trim().length > 2) {
                        tableLines.add(lines[i].trim())
                        i++
                    }

                    renderTable(tableLines, actionColor)
                    if (i < lines.size) append("\n")
                    continue
                }

                // 4. Check for Headings (#, ##, ###, ####, etc.)
                if (trimmed.startsWith("#")) {
                    val hashCount = trimmed.takeWhile { it == '#' }.length
                    if (hashCount in 1..6 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                        val headingContent = trimmed.substring(hashCount + 1).trim()
                        val (fontSize, fontWeight, color) = when (hashCount) {
                            1 -> Triple(19.sp, FontWeight.ExtraBold, Color(0xFFC7D2FE))
                            2 -> Triple(17.sp, FontWeight.Bold, Color(0xFFC7D2FE))
                            3 -> Triple(15.sp, FontWeight.Bold, Color(0xFFDDD6FE))
                            else -> Triple(14.sp, FontWeight.SemiBold, Color(0xFFE0E7FF))
                        }

                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight, color = color)) {
                            appendInlineMarkdown(headingContent, actionColor)
                        }
                        if (i < lines.size - 1) append("\n")
                        i++
                        continue
                    }
                }

                // 5. Check for Blockquote (> text)
                if (trimmed.startsWith(">")) {
                    val quoteContent = trimmed.removePrefix(">").trim()
                    withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)) {
                        append("▍ ")
                    }
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFE2E8F0))) {
                        appendInlineMarkdown(quoteContent, actionColor)
                    }
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 6. Check for Bullet List (- or *)
                if ((trimmed.startsWith("- ") || trimmed.startsWith("* ")) && !trimmed.startsWith("***")) {
                    val bulletContent = trimmed.substring(2).trim()
                    withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)) {
                        append("• ")
                    }
                    appendInlineMarkdown(bulletContent, actionColor)
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 7. Check for Numbered List (e.g. "1. ", "2. ")
                val numberedListMatch = Regex("^(\\d+\\.)\\s+(.*)$").find(trimmed)
                if (numberedListMatch != null) {
                    val numberPrefix = numberedListMatch.groupValues[1]
                    val itemContent = numberedListMatch.groupValues[2]
                    withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)) {
                        append("$numberPrefix ")
                    }
                    appendInlineMarkdown(itemContent, actionColor)
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 8. Standard paragraph line
                appendInlineMarkdown(rawLine, actionColor)
                if (i < lines.size - 1) append("\n")
                i++
            }
        }
    }

    private fun AnnotatedString.Builder.renderTable(
        tableLines: List<String>,
        actionColor: Color
    ) {
        if (tableLines.isEmpty()) return

        // Filter out delimiter rows (e.g. |---|---| or |:---|---:|)
        val delimiterRegex = Regex("^\\|[\\s\\-:\\|]+$")
        val dataLines = tableLines.filter { !delimiterRegex.matches(it) }

        if (dataLines.isEmpty()) return

        val headers = dataLines.first().split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val rows = if (dataLines.size > 1) {
            dataLines.drop(1).map { line ->
                line.split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        } else emptyList()

        if (headers.size == 2) {
            // Clean 2-column key-value / definition view (ideal for mobile chat bubbles)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE), fontSize = 14.sp)) {
                appendInlineMarkdown(headers[0], actionColor)
                append(" ➔ ")
                appendInlineMarkdown(headers[1], actionColor)
            }
            append("\n")

            for ((idx, row) in rows.withIndex()) {
                val key = row.getOrNull(0) ?: ""
                val value = row.getOrNull(1) ?: ""

                withStyle(SpanStyle(color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)) {
                    append("• ")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                    appendInlineMarkdown(key, actionColor)
                }
                if (value.isNotEmpty()) {
                    append(" — ")
                    appendInlineMarkdown(value, actionColor)
                }
                if (idx < rows.size - 1) append("\n")
            }
        } else {
            // General table representation for 1 or 3+ columns
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE))) {
                append(headers.joinToString(" │ "))
            }
            append("\n")
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.2f))) {
                append("────────────────────────────────────────\n")
            }
            for ((idx, row) in rows.withIndex()) {
                withStyle(SpanStyle(color = Color(0xFFA78BFA))) {
                    append("• ")
                }
                for ((cIdx, cell) in row.withIndex()) {
                    if (cIdx == 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            appendInlineMarkdown(cell, actionColor)
                        }
                    } else {
                        append(" │ ")
                        appendInlineMarkdown(cell, actionColor)
                    }
                }
                if (idx < rows.size - 1) append("\n")
            }
        }
    }

    fun AnnotatedString.Builder.appendInlineMarkdown(
        text: String,
        actionColor: Color
    ) {
        val length = text.length
        var i = 0

        while (i < length) {
            // 1. Triple Asterisk ***bold italic***
            if (i + 2 < length && text[i] == '*' && text[i + 1] == '*' && text[i + 2] == '*') {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    val inner = text.substring(i + 3, end)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = actionColor)) {
                        append(inner)
                    }
                    i = end + 3
                    continue
                }
            }

            // 2. Double Asterisk **bold**
            if (i + 1 < length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val inner = text.substring(i + 2, end)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        if (inner.startsWith("*") && inner.endsWith("*") && inner.length > 2) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = actionColor)) {
                                append(inner.substring(1, inner.length - 1))
                            }
                        } else {
                            append(inner)
                        }
                    }
                    i = end + 2
                    continue
                }
            }

            // 3. Single Asterisk *italic*
            if (text[i] == '*') {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && !text.substring(i + 1, end).contains("\n\n")) {
                    val inner = text.substring(i + 1, end)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = actionColor)) {
                        append(inner)
                    }
                    i = end + 1
                    continue
                }
            }

            // 4. Strikethrough ~~strikethrough~~
            if (i + 1 < length && text[i] == '~' && text[i + 1] == '~') {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    val inner = text.substring(i + 2, end)
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.White.copy(alpha = 0.6f))) {
                        append(inner)
                    }
                    i = end + 2
                    continue
                }
            }

            // 5. Backticks `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val inner = text.substring(i + 1, end)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            background = Color.Black.copy(alpha = 0.35f),
                            color = Color(0xFFFFD580)
                        )
                    ) {
                        append(inner)
                    }
                    i = end + 1
                    continue
                }
            }

            // 6. Markdown Link [text](url)
            if (text[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkLabel = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(color = Color(0xFF93C5FD), textDecoration = TextDecoration.Underline)) {
                            append(linkLabel)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Normal character
            append(text[i])
            i++
        }
    }
}
