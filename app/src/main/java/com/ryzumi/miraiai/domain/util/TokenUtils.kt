package com.ryzumi.miraiai.domain.util

import com.ryzumi.miraiai.domain.model.OpenAiContentPart
import com.ryzumi.miraiai.domain.model.OpenAiMessage

object TokenUtils {

    /**
     * Estimates the number of tokens for a given string using standard BPE heuristic.
     */
    fun estimateTokenCount(text: String?): Int {
        if (text.isNullOrEmpty()) return 0
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0

        var tokenCount = 0.0
        var currentWordLen = 0

        for (char in trimmed) {
            when {
                char.isWhitespace() -> {
                    if (currentWordLen > 0) {
                        tokenCount += (currentWordLen / 3.8).coerceAtLeast(1.0)
                        currentWordLen = 0
                    }
                }
                char in ",.!?:;\"'()[]{}<>-/\\|`~@#$%^&*+=" -> {
                    if (currentWordLen > 0) {
                        tokenCount += (currentWordLen / 3.8).coerceAtLeast(1.0)
                        currentWordLen = 0
                    }
                    tokenCount += 1.0
                }
                char.code > 127 -> {
                    if (currentWordLen > 0) {
                        tokenCount += (currentWordLen / 3.8).coerceAtLeast(1.0)
                        currentWordLen = 0
                    }
                    tokenCount += 1.3
                }
                else -> {
                    currentWordLen++
                }
            }
        }

        if (currentWordLen > 0) {
            tokenCount += (currentWordLen / 3.8).coerceAtLeast(1.0)
        }

        return tokenCount.toInt().coerceAtLeast(1)
    }

    /**
     * Estimates the total context token count across all messages in context window.
     */
    fun estimateContextTokens(messages: List<OpenAiMessage>): Int {
        var total = 0
        for (msg in messages) {
            total += 4 // Overhead tokens for message role/framing
            when (val content = msg.content) {
                is String -> total += estimateTokenCount(content)
                is List<*> -> {
                    for (part in content) {
                        if (part is OpenAiContentPart) {
                            if (part.type == "text") {
                                total += estimateTokenCount(part.text)
                            } else if (part.type == "image_url") {
                                total += 768
                            }
                        }
                    }
                }
            }
        }
        return total
    }
}
