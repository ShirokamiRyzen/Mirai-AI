package com.ryzumi.miraiai.domain.util

import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
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

    /**
     * Estimates tokens for a single chat message entity, including image overhead if present.
     */
    fun estimateMessageTokens(message: ChatMessageEntity): Int {
        val baseTokens = if (message.tokensCount > 0) message.tokensCount else estimateTokenCount(message.content)
        val imageTokens = if (!message.imageUri.isNullOrBlank()) 768 else 0
        return baseTokens + imageTokens + 4 // Overhead tokens for message role/framing
    }

    /**
     * Trims chat history from newest to oldest so that total context tokens (system prompt + history)
     * never exceeds maxTokens. Preserves the newest messages that fit within the remaining budget.
     * Always preserves at least the latest message if history is not empty.
     */
    fun trimHistoryToFitBudget(
        chatHistory: List<ChatMessageEntity>,
        systemPromptTokens: Int,
        maxContextTokens: Int
    ): Pair<List<ChatMessageEntity>, Int> {
        val safeMax = maxContextTokens.coerceAtLeast(1)
        val availableBudget = (safeMax - systemPromptTokens).coerceAtLeast(0)
        if (chatHistory.isEmpty()) {
            return Pair(emptyList(), minOf(systemPromptTokens, safeMax))
        }

        val reversed = chatHistory.reversed()
        val keptReversed = mutableListOf<ChatMessageEntity>()
        var accumulatedTokens = 0

        for ((index, msg) in reversed.withIndex()) {
            val msgTokens = estimateMessageTokens(msg)
            if (accumulatedTokens + msgTokens <= availableBudget) {
                keptReversed.add(msg)
                accumulatedTokens += msgTokens
            } else {
                // If even the latest message cannot fit within available budget, keep at least the latest message
                if (index == 0 && keptReversed.isEmpty()) {
                    keptReversed.add(msg)
                    accumulatedTokens += msgTokens
                }
                break
            }
        }

        val finalMessages = keptReversed.reversed()
        val totalTokens = (systemPromptTokens + accumulatedTokens).coerceAtMost(safeMax)
        return Pair(finalMessages, totalTokens)
    }
}
