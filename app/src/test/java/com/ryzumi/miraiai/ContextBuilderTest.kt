package com.ryzumi.miraiai

import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.domain.context.ContextBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBuilderTest {

    @Test
    fun testBuildSystemPromptMacroReplacement() {
        val character = CharacterEntity(
            name = "Aria",
            description = "A friendly AI companion chatting with {{user}}.",
            personality = "Cheerful and witty.",
            scenario = "Meeting {{user}} at a cafe."
        )
        val persona = UserPersonaEntity(
            name = "Fatih",
            personaDescription = "An Android engineer."
        )

        val prompt = ContextBuilder.buildSystemPrompt(character, persona)

        assertTrue(prompt.contains("A friendly AI companion chatting with Fatih."))
        assertTrue(prompt.contains("Meeting Fatih at a cafe."))
        assertTrue(prompt.contains("An Android engineer."))
    }

    @Test
    fun testBuildOpenAiMessagesSequence() {
        val character = CharacterEntity(
            name = "Aria",
            firstMessage = "Greetings {{user}}!"
        )
        val persona = UserPersonaEntity(name = "Fatih")

        val history = listOf(
            ChatMessageEntity(
                sessionId = "sess1",
                sender = "USER",
                content = "How are you {{char}}?"
            )
        )

        val messages = kotlinx.coroutines.runBlocking {
            ContextBuilder.buildOpenAiMessages(character, persona, history)
        }

        assertEquals(3, messages.size)
        assertEquals("system", messages[0].role)
        assertEquals("assistant", messages[1].role)
        assertEquals("Greetings Fatih!", messages[1].content)
        assertEquals("user", messages[2].role)
        assertEquals("How are you Aria?", messages[2].content)
    }

    @Test
    fun testBuildOpenAiMessagesWithVisionImage() {
        val character = CharacterEntity(name = "Aria")
        val persona = UserPersonaEntity(name = "Fatih")

        val dummyDataUrl = "data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        val history = listOf(
            ChatMessageEntity(
                sessionId = "sess1",
                sender = "USER",
                content = "What is in this image?",
                imageUri = dummyDataUrl
            )
        )

        val messages = kotlinx.coroutines.runBlocking {
            ContextBuilder.buildOpenAiMessages(character, persona, history)
        }

        assertEquals(2, messages.size) // system + user
        assertEquals("user", messages[1].role)
        assertTrue(messages[1].content is List<*>)

        val parts = messages[1].content as List<com.ryzumi.miraiai.domain.model.OpenAiContentPart>
        assertEquals(2, parts.size)
        assertEquals("text", parts[0].type)
        assertEquals("What is in this image?", parts[0].text)
        assertEquals("image_url", parts[1].type)
        assertEquals(dummyDataUrl, parts[1].image_url?.url)
    }

    @Test
    fun testBuildOpenAiMessagesMixedHistoryTextTurn() {
        val character = CharacterEntity(name = "Aria")
        val persona = UserPersonaEntity(name = "Fatih")

        val dummyDataUrl = "data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        val history = listOf(
            ChatMessageEntity(
                sessionId = "sess1",
                sender = "USER",
                content = "What is in this image?",
                imageUri = dummyDataUrl
            ),
            ChatMessageEntity(
                sessionId = "sess1",
                sender = "CHARACTER",
                content = "I see a cute picture."
            ),
            ChatMessageEntity(
                sessionId = "sess1",
                sender = "USER",
                content = "Really?"
            )
        )

        // Text-only turn (includeImages = false)
        val messages = kotlinx.coroutines.runBlocking {
            ContextBuilder.buildOpenAiMessages(character, persona, history, includeImages = false)
        }

        assertEquals(4, messages.size) // system + user(image msg as text) + assistant + user(text)
        assertEquals("system", messages[0].role)
        assertEquals("user", messages[1].role)
        assertEquals("What is in this image?", messages[1].content) // Plain string, NO List/OpenAiContentPart
        assertEquals("assistant", messages[2].role)
        assertEquals("I see a cute picture.", messages[2].content)
        assertEquals("user", messages[3].role)
        assertEquals("Really?", messages[3].content)

        // Ensure none of the messages have List/image_url content
        for (msg in messages) {
            assertTrue(msg.content is String)
        }
    }

    @Test
    fun testTrimHistoryToFitBudgetDoesNotExceedLimit() {
        val history = (1..20).map { i ->
            ChatMessageEntity(
                sessionId = "sess1",
                sender = if (i % 2 == 0) "CHARACTER" else "USER",
                content = "Message turn $i with detailed explanations and long context text to take up token space.",
                tokensCount = 200
            )
        }
        val systemPromptTokens = 300
        val maxContextTokens = 1000

        val (pruned, totalTokens) = com.ryzumi.miraiai.domain.util.TokenUtils.trimHistoryToFitBudget(
            chatHistory = history,
            systemPromptTokens = systemPromptTokens,
            maxContextTokens = maxContextTokens
        )

        // 300 system prompt tokens + 204 msg tokens (200 + 4 overhead) * 3 = 912 tokens <= 1000
        assertTrue("Total tokens ($totalTokens) must be <= maxContextTokens ($maxContextTokens)", totalTokens <= maxContextTokens)
        assertTrue("Pruned messages count should be less than original history", pruned.size < history.size)
        // Must preserve the latest message
        assertEquals("Message turn 20 with detailed explanations and long context text to take up token space.", pruned.last().content)
    }

    @Test
    fun testBuildOpenAiMessagesWithMaxContextTokensPruning() {
        val character = CharacterEntity(name = "Aria")
        val persona = UserPersonaEntity(name = "Fatih")

        val history = (1..10).map { i ->
            ChatMessageEntity(
                sessionId = "sess1",
                sender = if (i % 2 == 0) "CHARACTER" else "USER",
                content = "Message turn $i content with multiple sentences to test context budget trimming.",
                tokensCount = 300
            )
        }

        // Limit context to 800 tokens. System prompt is ~70 tokens.
        // Each msg is ~304 tokens. Only the 2 most recent messages should fit.
        val messages = kotlinx.coroutines.runBlocking {
            ContextBuilder.buildOpenAiMessages(
                character = character,
                persona = persona,
                chatHistory = history,
                maxContextTokens = 800
            )
        }

        // 1 system message + at most 2 pruned messages = 3 messages
        assertTrue("Expected pruned messages to fit budget, got ${messages.size}", messages.size <= 3)
        assertEquals("system", messages.first().role)
        assertEquals(history.last().content, messages.last().content)
    }
}
