package com.ryzumi.miraiai.domain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object MarkdownRenderer {

    fun parseMarkdown(
        text: String,
        actionColor: Color = Color(0xFFC3C7FF)
    ): AnnotatedString {
        if (text.isBlank()) return buildAnnotatedString { }

        return buildAnnotatedString {
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
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(inner)
                        }
                        i = end + 2
                        continue
                    }
                }

                // 3. Single Asterisk *italic roleplay action*
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

                // 4. Backticks `code`
                if (text[i] == '`') {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        val inner = text.substring(i + 1, end)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
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

                // Normal character
                append(text[i])
                i++
            }
        }
    }
}
