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
import com.ryzumi.miraiai.domain.util.TokenUtils
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
            sb.append("\nYou are directly integrated into $userName's mobile device and have active access to read device telemetry, smartphone hardware info, OS version, battery status, local network IP, public IP, connected Wi-Fi SSID, saved Wi-Fi networks, cellular network carrier, clock/time, GPS location, live weather, and environmental Air Quality / IKU (Indeks Kualitas Udara / AQI).")
            sb.append("\n- If $userName asks whether you can access their phone info, battery, network, local IP, public IP, Wi-Fi, cellular, time, location, weather, or air quality / IKU (Indeks Kualitas Udara / AQI / PM2.5 / UV), ALWAYS CONFIRM AFFIRMATIVELY that you have full access to their device and environmental data, and provide the relevant information.")
            sb.append("\n- NEVER state \"I cannot access your phone\" or \"I am just an AI chat model\". You are their capable personal assistant as well as $charName.")
            sb.append("\n\n[Current Live Device & Environmental Telemetry]\n")
            sb.append(deviceContext)
            sb.append("\n(Seamlessly blend your character personality ($charName) with your smart personal assistant capabilities.)")
        }

        sb.append("\n\n[Live2D Interactive Avatar Expressions & Motions]")
        sb.append("\nYour character possesses an animated Live2D avatar on screen. You can express emotions and perform body motions:")
        sb.append("\n- To trigger a facial expression: Use [expression:name] (e.g., [expression:happy], [expression:shy], [expression:love], [expression:angry], [expression:surprised], [expression:sad]).")
        sb.append("\n- To trigger a body motion / pose / gesture: Use [motion:name] (e.g., [motion:wave], [motion:dance], [motion:pose], [motion:jump], [motion:tap_body]).")
        sb.append("\n- IMPORTANT RULES FOR MOTIONS & EXPRESSIONS:")
        sb.append("\n  * If $userName specifically asks for a facial expression or emotion (e.g. smile, pout, act angry, blush), ONLY use [expression:name]. DO NOT add dancing or unrelated body motions!")
        sb.append("\n  * If $userName asks for a physical movement, dance, or pose, use [motion:name] along with a MATCHING facial expression (e.g. smile/happy with dance, shy with pose).")
        sb.append("\n  * NEVER contradict emotions and motions: NEVER dance, cheer, or jump while angry or sad!")

        sb.append("\n\nStay strictly in character as $charName. Respond dynamically to $userName.")
        return sb.toString()
    }

    suspend fun buildOpenAiMessages(
        character: CharacterEntity,
        persona: UserPersonaEntity?,
        chatHistory: List<ChatMessageEntity>,
        context: Context? = null,
        includeImages: Boolean = true,
        deviceContext: String? = null,
        maxContextTokens: Int? = null,
        uploadAsBase64: Boolean = true
    ): List<OpenAiMessage> {
        val charName = character.name.ifBlank { "Character" }
        val userName = persona?.name?.ifBlank { "User" } ?: "User"

        val result = mutableListOf<OpenAiMessage>()

        // 1. System Prompt Message
        val systemPrompt = buildSystemPrompt(character, persona, deviceContext)
        result.add(OpenAiMessage(role = "system", content = systemPrompt))

        // 2. First Message (greeting) if configured and chat history doesn't already contain it
        val hasGreeting = character.firstMessage.isNotBlank()
        val processedFirstMsg = if (hasGreeting) {
            MacroEngine.processMacros(character.firstMessage, charName, userName)
        } else ""

        val shouldAddGreeting = hasGreeting && (chatHistory.isEmpty() || chatHistory.firstOrNull()?.content != processedFirstMsg)
        if (shouldAddGreeting) {
            result.add(OpenAiMessage(role = "assistant", content = processedFirstMsg))
        }

        // 3. Chat Messages History - pruned to fit maxContextTokens budget if specified
        val effectiveHistory = if (maxContextTokens != null && maxContextTokens > 0) {
            var systemPromptTokens = TokenUtils.estimateTokenCount(systemPrompt) + 4
            if (shouldAddGreeting) {
                systemPromptTokens += TokenUtils.estimateTokenCount(processedFirstMsg) + 4
            }
            val (pruned, _) = TokenUtils.trimHistoryToFitBudget(
                chatHistory = chatHistory,
                systemPromptTokens = systemPromptTokens,
                maxContextTokens = maxContextTokens
            )
            pruned
        } else {
            chatHistory
        }

        for (msg in effectiveHistory) {
            val role = when (msg.sender.uppercase()) {
                "USER" -> "user"
                "CHARACTER", "ASSISTANT" -> "assistant"
                else -> "system"
            }

            val processedText = MacroEngine.processMacros(msg.content, charName, userName)

            if (includeImages && !msg.imageUri.isNullOrBlank()) {
                val rawUri = msg.imageUri

                // Determine image format based on uploadAsBase64 preference
                val finalImageUrl = if (uploadAsBase64) {
                    // Upload as Base64 (default ON): compress and send directly as data URI base64
                    if (rawUri.startsWith("data:image/")) {
                        rawUri
                    } else {
                        try {
                            ImageUtils.processAndEncodeImage(context, rawUri, maxDimension = 1024, quality = 85)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    // Upload to RustFS S3 storage so AI receives presigned image URL instead of base64
                    if (rawUri.startsWith("http://") || rawUri.startsWith("https://")) {
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
