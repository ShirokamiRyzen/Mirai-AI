package com.ryzumi.miraiai.domain.context

import android.content.Context
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.data.network.RustFsUploader
import com.ryzumi.miraiai.domain.macro.MacroEngine
import com.ryzumi.miraiai.domain.model.OpenAiContentPart
import com.ryzumi.miraiai.domain.model.OpenAiImageUrl
import com.ryzumi.miraiai.domain.model.OpenAiMessage
import com.ryzumi.miraiai.domain.util.ImageUtils
import java.util.concurrent.ConcurrentHashMap

object ContextBuilder {

    // In-memory cache for uploaded S3 image URLs to avoid repeated uploads across conversation turns
    private val s3UrlCache = ConcurrentHashMap<String, String>()

    fun buildSystemPrompt(
        character: CharacterEntity,
        persona: UserPersonaEntity?,
        deviceContext: String? = null
    ): String {
        val charName = character.name.ifBlank { "Character" }
        val userName = persona?.name?.ifBlank { "User" } ?: "User"

        val sb = StringBuilder()
        sb.append("You are $charName in an interactive roleplay and smart personal assistant conversation with $userName.\n")

        if (character.description.isNotBlank()) {
            sb.append("\n[Character Description]\n")
            sb.append(MacroEngine.processMacros(character.description, charName, userName))
        }

        if (character.personality.isNotBlank()) {
            sb.append("\n\n[Personality & Behavior]\n")
            sb.append(MacroEngine.processMacros(character.personality, charName, userName))
        }

        if (character.scenario.isNotBlank()) {
            sb.append("\n\n[Scenario & Setting]\n")
            sb.append(MacroEngine.processMacros(character.scenario, charName, userName))
        }

        if (character.impression.isNotBlank()) {
            sb.append("\n\n[System Directives / Impression]\n")
            sb.append(MacroEngine.processMacros(character.impression, charName, userName))
        }

        if (persona != null && persona.personaDescription.isNotBlank()) {
            sb.append("\n\n[User Persona Details ($userName)]\n")
            sb.append(MacroEngine.processMacros(persona.personaDescription, charName, userName))
        }

        if (!deviceContext.isNullOrBlank()) {
            sb.append("\n\n[SMART ASSISTANT & DEVICE INTEGRATION]")
            sb.append("\nYou are directly integrated into $userName's mobile device and have active access to read device telemetry, smartphone hardware info, OS version, battery status, network, clock/time, GPS location, and live weather.")
            sb.append("\n- If $userName asks whether you can access their phone info, battery, time, location, or weather, ALWAYS CONFIRM AFFIRMATIVELY that you have full access to their device system and provide the relevant information.")
            sb.append("\n- NEVER state \"I cannot access your phone\" or \"I am just an AI chat model\". You are their capable personal assistant as well as $charName.")
            sb.append("\n\n[Current Live Device & Environmental Telemetry]\n")
            sb.append(deviceContext)
            sb.append("\n(Seamlessly blend your character personality ($charName) with your smart personal assistant capabilities.)")
        }

        sb.append("\n\nStay strictly in character as $charName. Respond dynamically to $userName.")
        return sb.toString()
    }

    suspend fun buildOpenAiMessages(
        character: CharacterEntity,
        persona: UserPersonaEntity?,
        chatHistory: List<ChatMessageEntity>,
        context: Context? = null,
        includeImages: Boolean = true,
        deviceContext: String? = null
    ): List<OpenAiMessage> {
        val charName = character.name.ifBlank { "Character" }
        val userName = persona?.name?.ifBlank { "User" } ?: "User"

        val result = mutableListOf<OpenAiMessage>()

        // 1. System Prompt Message
        val systemPrompt = buildSystemPrompt(character, persona, deviceContext)
        result.add(OpenAiMessage(role = "system", content = systemPrompt))

        // 2. First Message (greeting) if configured and chat history doesn't already contain it
        if (character.firstMessage.isNotBlank()) {
            val processedFirstMsg = MacroEngine.processMacros(character.firstMessage, charName, userName)
            if (chatHistory.isEmpty() || chatHistory.firstOrNull()?.content != processedFirstMsg) {
                result.add(OpenAiMessage(role = "assistant", content = processedFirstMsg))
            }
        }

        // 3. Chat Messages History
        for (msg in chatHistory) {
            val role = when (msg.sender.uppercase()) {
                "USER" -> "user"
                "CHARACTER", "ASSISTANT" -> "assistant"
                else -> "system"
            }

            val processedText = MacroEngine.processMacros(msg.content, charName, userName)

            if (includeImages && !msg.imageUri.isNullOrBlank()) {
                val rawUri = msg.imageUri

                // Upload to RustFS S3 so AI receives presigned image URL instead of heavy base64
                val finalImageUrl = if (rawUri.startsWith("http://") || rawUri.startsWith("https://")) {
                    RustFsUploader.signUrlIfNeeded(rawUri)
                } else {
                    s3UrlCache[rawUri] ?: run {
                        val bytes = ImageUtils.getImageBytesForUpload(context, rawUri)
                        if (bytes != null && bytes.isNotEmpty()) {
                            val processed = ImageUtils.processImageBytes(bytes, maxDimension = 1024, quality = 85)
                            if (processed != null) {
                                val uploadResult = RustFsUploader.uploadImageBytes(
                                    imageBytes = processed.bytes,
                                    contentType = processed.contentType,
                                    extension = processed.extension
                                )
                                uploadResult.getOrNull()?.also { signedUrl ->
                                    s3UrlCache[rawUri] = signedUrl
                                }
                            } else null
                        } else null
                    } ?: if (rawUri.startsWith("data:image/")) {
                        rawUri
                    } else {
                        try {
                            ImageUtils.processAndEncodeImage(context, rawUri, maxDimension = 1024, quality = 85)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (!finalImageUrl.isNullOrBlank()) {
                    val parts = mutableListOf<OpenAiContentPart>()
                    val promptText = if (processedText.isNotBlank()) processedText else "Analyze this image"
                    parts.add(OpenAiContentPart(type = "text", text = promptText))
                    parts.add(
                        OpenAiContentPart(
                            type = "image_url",
                            image_url = OpenAiImageUrl(url = finalImageUrl)
                        )
                    )
                    result.add(OpenAiMessage(role = role, content = parts))
                } else {
                    result.add(OpenAiMessage(role = role, content = processedText.ifBlank { "Analyze this image" }))
                }
            } else {
                result.add(OpenAiMessage(role = role, content = processedText))
            }
        }

        return result
    }
}
