package com.ryzumi.miraiai.domain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

object MarkdownRenderer {

    fun parseMarkdown(
        text: String,
        actionColor: Color = Color(0xFFC3C7FF),
        isDark: Boolean = true,
        linkColor: Color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
        onLinkClick: ((String) -> Unit)? = null
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
                            background = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF1E1E2E),
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
                    val hrColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFCBD5E1)
                    withStyle(SpanStyle(color = hrColor, fontWeight = FontWeight.Light)) {
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

                    renderTable(tableLines, actionColor, isDark, linkColor, onLinkClick)
                    if (i < lines.size) append("\n")
                    continue
                }

                // 4. Check for Headings (#, ##, ###, ####, etc.)
                if (trimmed.startsWith("#")) {
                    val hashCount = trimmed.takeWhile { it == '#' }.length
                    if (hashCount in 1..6 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                        val headingContent = trimmed.substring(hashCount + 1).trim()
                        val (fontSize, fontWeight, color) = when (hashCount) {
                            1 -> Triple(19.sp, FontWeight.ExtraBold, if (isDark) Color(0xFFC7D2FE) else Color(0xFF312E81))
                            2 -> Triple(17.sp, FontWeight.Bold, if (isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3))
                            3 -> Triple(15.sp, FontWeight.Bold, if (isDark) Color(0xFFDDD6FE) else Color(0xFF4338CA))
                            else -> Triple(14.sp, FontWeight.SemiBold, if (isDark) Color(0xFFE0E7FF) else Color(0xFF4F46E5))
                        }

                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight, color = color)) {
                            appendInlineMarkdown(headingContent, actionColor, isDark, linkColor, onLinkClick)
                        }
                        if (i < lines.size - 1) append("\n")
                        i++
                        continue
                    }
                }

                // 5. Check for Blockquote (> text)
                if (trimmed.startsWith(">")) {
                    val quoteContent = trimmed.removePrefix(">").trim()
                    val barColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                    val quoteColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                    withStyle(SpanStyle(color = barColor, fontWeight = FontWeight.Bold)) {
                        append("▍ ")
                    }
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor)) {
                        appendInlineMarkdown(quoteContent, actionColor, isDark, linkColor, onLinkClick)
                    }
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 6. Check for Bullet List (- or *)
                if ((trimmed.startsWith("- ") || trimmed.startsWith("* ")) && !trimmed.startsWith("***")) {
                    val bulletContent = trimmed.substring(2).trim()
                    val bulletColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                    withStyle(SpanStyle(color = bulletColor, fontWeight = FontWeight.Bold)) {
                        append("• ")
                    }
                    appendInlineMarkdown(bulletContent, actionColor, isDark, linkColor, onLinkClick)
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 7. Check for Numbered List (e.g. "1. ", "2. ")
                val numberedListMatch = Regex("^(\\d+\\.)\\s+(.*)$").find(trimmed)
                if (numberedListMatch != null) {
                    val numberPrefix = numberedListMatch.groupValues[1]
                    val itemContent = numberedListMatch.groupValues[2]
                    val numberColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                    withStyle(SpanStyle(color = numberColor, fontWeight = FontWeight.Bold)) {
                        append("$numberPrefix ")
                    }
                    appendInlineMarkdown(itemContent, actionColor, isDark, linkColor, onLinkClick)
                    if (i < lines.size - 1) append("\n")
                    i++
                    continue
                }

                // 8. Standard paragraph line
                appendInlineMarkdown(rawLine, actionColor, isDark, linkColor, onLinkClick)
                if (i < lines.size - 1) append("\n")
                i++
            }
        }
    }

    private fun AnnotatedString.Builder.renderTable(
        tableLines: List<String>,
        actionColor: Color,
        isDark: Boolean,
        linkColor: Color,
        onLinkClick: ((String) -> Unit)?
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

        val headerColor = if (isDark) Color(0xFFC7D2FE) else Color(0xFF312E81)
        val bulletColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
        val rowKeyColor = if (isDark) Color.White else Color(0xFF0F172A)
        val dividerColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)

        if (headers.size == 2) {
            // Clean 2-column key-value / definition view (ideal for mobile chat bubbles)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = headerColor, fontSize = 14.sp)) {
                appendInlineMarkdown(headers[0], actionColor, isDark, linkColor, onLinkClick)
                append(" ➔ ")
                appendInlineMarkdown(headers[1], actionColor, isDark, linkColor, onLinkClick)
            }
            append("\n")

            for ((idx, row) in rows.withIndex()) {
                val key = row.getOrNull(0) ?: ""
                val value = row.getOrNull(1) ?: ""

                withStyle(SpanStyle(color = bulletColor, fontWeight = FontWeight.Bold)) {
                    append("• ")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = rowKeyColor)) {
                    appendInlineMarkdown(key, actionColor, isDark, linkColor, onLinkClick)
                }
                if (value.isNotEmpty()) {
                    append(" — ")
                    appendInlineMarkdown(value, actionColor, isDark, linkColor, onLinkClick)
                }
                if (idx < rows.size - 1) append("\n")
            }
        } else {
            // General table representation for 1 or 3+ columns
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = headerColor)) {
                append(headers.joinToString(" │ "))
            }
            append("\n")
            withStyle(SpanStyle(color = dividerColor)) {
                append("────────────────────────────────────────\n")
            }
            for ((idx, row) in rows.withIndex()) {
                withStyle(SpanStyle(color = bulletColor)) {
                    append("• ")
                }
                for ((cIdx, cell) in row.withIndex()) {
                    if (cIdx == 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = rowKeyColor)) {
                            appendInlineMarkdown(cell, actionColor, isDark, linkColor, onLinkClick)
                        }
                    } else {
                        append(" │ ")
                        appendInlineMarkdown(cell, actionColor, isDark, linkColor, onLinkClick)
                    }
                }
                if (idx < rows.size - 1) append("\n")
            }
        }
    }

    fun AnnotatedString.Builder.appendInlineMarkdown(
        text: String,
        actionColor: Color,
        isDark: Boolean = true,
        linkColor: Color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
        onLinkClick: ((String) -> Unit)? = null
    ) {
        val length = text.length
        var i = 0

        val boldColor = if (isDark) Color.White else Color(0xFF0F172A)
        val strikeColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
        val inlineCodeBg = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
        val inlineCodeColor = if (isDark) Color(0xFFFFD580) else Color(0xFF9A3412)

        fun createLinkAnnotation(url: String): LinkAnnotation.Url {
            val finalUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                "https://$url"
            } else {
                url
            }
            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            )
            return if (onLinkClick != null) {
                LinkAnnotation.Url(
                    url = finalUrl,
                    styles = linkStyles,
                    linkInteractionListener = { ann ->
                        if (ann is LinkAnnotation.Url) onLinkClick(ann.url)
                    }
                )
            } else {
                LinkAnnotation.Url(url = finalUrl, styles = linkStyles)
            }
        }

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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
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
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = strikeColor)) {
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
                            background = inlineCodeBg,
                            color = inlineCodeColor
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
                        val rawUrl = text.substring(closeBracket + 2, closeParen).trim()
                        val annotation = createLinkAnnotation(rawUrl)
                        withLink(annotation) {
                            append(linkLabel)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // 7. Plain URL detection (http://, https://, or www.)
            val isHttp = text.startsWith("http://", i, ignoreCase = true)
            val isHttps = text.startsWith("https://", i, ignoreCase = true)
            val isWww = text.startsWith("www.", i, ignoreCase = true)
            if (isHttp || isHttps || isWww) {
                var end = i
                while (end < length && !text[end].isWhitespace()) {
                    end++
                }
                var urlEnd = end
                while (urlEnd > i && text[urlEnd - 1] in ".,;:!?)]}") {
                    if (text[urlEnd - 1] == ')' && text.substring(i, urlEnd - 1).contains('(')) {
                        break
                    }
                    urlEnd--
                }
                if (urlEnd > i) {
                    val rawUrl = text.substring(i, urlEnd)
                    val annotation = createLinkAnnotation(rawUrl)
                    withLink(annotation) {
                        append(rawUrl)
                    }
                    if (urlEnd < end) {
                        append(text.substring(urlEnd, end))
                    }
                    i = end
                    continue
                }
            }

            // Normal character
            append(text[i])
            i++
        }
    }
}

