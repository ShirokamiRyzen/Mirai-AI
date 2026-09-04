package com.ryzumi.miraiai.domain.engine

import android.content.Context
import com.ryzumi.miraiai.data.local.MiraiDatabase
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.data.network.OpenAiRepository
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.domain.context.ContextBuilder
import com.ryzumi.miraiai.domain.macro.MacroEngine
import com.ryzumi.miraiai.domain.model.LocalModelManager
import com.ryzumi.miraiai.domain.model.LocalModelStatus
import com.ryzumi.miraiai.domain.util.ChatNotificationHelper
import com.ryzumi.miraiai.domain.util.DeviceContextManager
import com.ryzumi.miraiai.domain.util.MiraiToolManager
import com.ryzumi.miraiai.domain.util.TokenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class GenerationStreamState(
    val isStreaming: Boolean = false,
    val thinking: String = "",
    val text: String = "",
    val tokensCount: Int = 0,
    val speedTps: Double = 0.0,
    val modelName: String = "",
    val errorMessage: String? = null
)

object ChatGenerationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val streamStates = ConcurrentHashMap<String, MutableStateFlow<GenerationStreamState>>()

    val activeVisibleSessionId = MutableStateFlow<String?>(null)
    val isAppInForeground = MutableStateFlow(true)

    fun getStreamStateFlow(sessionId: String): StateFlow<GenerationStreamState> {
        return streamStates.getOrPut(sessionId) {
            MutableStateFlow(GenerationStreamState())
        }.asStateFlow()
    }

    fun setActiveVisibleSession(sessionId: String?) {
        activeVisibleSessionId.value = sessionId
    }

    fun clearActiveVisibleSession(sessionId: String) {
        if (activeVisibleSessionId.value == sessionId) {
            activeVisibleSessionId.value = null
        }
    }

    fun setAppForeground(isForeground: Boolean) {
        isAppInForeground.value = isForeground
    }

    fun startGeneration(
        context: Context,
        sessionId: String,
        character: CharacterEntity,
        persona: UserPersonaEntity?,
        config: InferenceConfigEntity,
        hasImage: Boolean,
        openAiRepository: OpenAiRepository,
        database: MiraiDatabase,
        isShowThinking: Boolean
    ) {
        // Cancel previous job for this session if running
        stopGeneration(sessionId, database)

        val stateFlow = streamStates.getOrPut(sessionId) { MutableStateFlow(GenerationStreamState()) }

        val job = scope.launch {
            val isLocalNeeded = if (hasImage) config.useLocalVisionModel else config.useLocalGenModel
            val chosenModel = if (hasImage) {
                if (config.visionModelId.isNotBlank() && config.visionModelId != "auto-debug" && config.visionModelId != "none") {
                    config.visionModelId
                } else {
                    config.generateModelId
                }
            } else {
                config.generateModelId
            }

            stateFlow.value = GenerationStreamState(
                isStreaming = true,
                thinking = "",
                text = "",
                tokensCount = 0,
                speedTps = 0.0,
                modelName = chosenModel,
                errorMessage = null
            )

            if (isLocalNeeded && chosenModel.isNotBlank()) {
                if (LocalModelManager.status.value != LocalModelStatus.LOADED || LocalModelManager.loadedModelName.value != chosenModel) {
                    val loadResult = LocalModelManager.loadModel(context, chosenModel, isVision = hasImage)
                    if (loadResult.isFailure) {
                        stateFlow.value = stateFlow.value.copy(
                            isStreaming = false,
                            errorMessage = "Failed to load local model into RAM: ${loadResult.exceptionOrNull()?.message}"
                        )
                        return@launch
                    }
                }
            }

            val thinkingSb = StringBuilder()
            val contentSb = StringBuilder()
            var firstTokenTime = 0L
            var contentFirstTokenTime = 0L
            var smoothedSpeed = 0.0

            val history = database.chatMessageDao().getMessagesForSessionSync(sessionId)

            val settingsRepo = SettingsRepository(context)
            val isAllowDeviceContext = try {
                settingsRepo.allowDeviceContextFlow.first()
            } catch (e: Exception) {
                false
            }

            val liveDeviceContext = if (isAllowDeviceContext) {
                try {
                    DeviceContextManager.getLiveDeviceContext(context)
                } catch (e: Exception) {
                    null
                }
            } else null

            val streamFlow = if (isLocalNeeded) {
                val (prunedHistory, _) = TokenUtils.trimHistoryToFitBudget(
                    chatHistory = history,
                    systemPromptTokens = 350,
                    maxContextTokens = config.maxTokens
                )
                LocalModelManager.streamLocalInference(
                    character = character,
                    persona = persona,
                    chatHistory = prunedHistory,
                    hasImage = hasImage,
                    modelName = chosenModel,
                    deviceContext = liveDeviceContext
                )
            } else {
                val openAiMessages = ContextBuilder.buildOpenAiMessages(
                    character = character,
                    persona = persona,
                    chatHistory = history,
                    context = context,
                    includeImages = hasImage,
                    deviceContext = liveDeviceContext,
                    maxContextTokens = config.maxTokens
                )

                val tools = if (isAllowDeviceContext) {
                    MiraiToolManager.getToolDefinitions()
                } else null

                val toolExecutor: (suspend (String, String) -> String)? = if (isAllowDeviceContext) {
                    { name, args ->
                        MiraiToolManager.executeTool(context, name, args)
                    }
                } else null

                openAiRepository.streamChatCompletions(
                    baseUrl = config.baseUrl,
                    apiKey = config.apiKey,
                    modelId = chosenModel,
                    messages = openAiMessages,
                    temperature = config.temperature,
                    topP = config.topP,
                    maxTokens = config.maxTokens,
                    customHeadersJson = config.customHeaders,
                    tools = tools,
                    toolExecutor = toolExecutor
                )
            }

            try {
                streamFlow.collect { chunk ->
                    var hasNewToken = false
                    if (chunk.thinking.isNotEmpty()) {
                        thinkingSb.append(chunk.thinking)
                        hasNewToken = true
                    }
                    if (chunk.content.isNotEmpty()) {
                        contentSb.append(chunk.content)
                        hasNewToken = true
                        if (contentFirstTokenTime == 0L) {
                            contentFirstTokenTime = System.currentTimeMillis()
                        }
                    }

                    if (hasNewToken && firstTokenTime == 0L) {
                        firstTokenTime = System.currentTimeMillis()
                    }

                    val thinkingTokens = TokenUtils.estimateTokenCount(thinkingSb.toString())
                    val contentTokens = TokenUtils.estimateTokenCount(contentSb.toString())

                    val (currentTokens, elapsedSec) = if (isShowThinking) {
                        val tokens = thinkingTokens + contentTokens
                        val elapsed = if (firstTokenTime > 0L) (System.currentTimeMillis() - firstTokenTime) / 1000.0 else 0.0
                        Pair(tokens, elapsed)
                    } else {
                        val tokens = contentTokens
                        val startTime = if (contentFirstTokenTime > 0L) contentFirstTokenTime else firstTokenTime
                        val elapsed = if (startTime > 0L) (System.currentTimeMillis() - startTime) / 1000.0 else 0.0
                        Pair(tokens, elapsed)
                    }

                    if (elapsedSec >= 0.25 && currentTokens >= 2) {
                        val instantSpeed = currentTokens / elapsedSec
                        smoothedSpeed = if (smoothedSpeed <= 0.0) {
                            instantSpeed
                        } else {
                            0.80 * smoothedSpeed + 0.20 * instantSpeed
                        }
                    }

                    stateFlow.value = stateFlow.value.copy(
                        thinking = thinkingSb.toString(),
                        text = contentSb.toString(),
                        tokensCount = currentTokens,
                        speedTps = smoothedSpeed
                    )
                }

                val cleanContent = MacroEngine.stripThinking(contentSb.toString()).trim()
                val cleanThinking = thinkingSb.toString().trim()

                val finalOutput = if (isShowThinking && cleanThinking.isNotBlank()) {
                    "<think>\n$cleanThinking\n</think>\n\n$cleanContent"
                } else {
                    cleanContent
                }

                val finalTokens = if (isShowThinking && cleanThinking.isNotBlank()) {
                    TokenUtils.estimateTokenCount(cleanThinking) + TokenUtils.estimateTokenCount(cleanContent)
                } else {
                    TokenUtils.estimateTokenCount(cleanContent)
                }

                val totalDurationSec = if (isShowThinking && cleanThinking.isNotBlank()) {
                    if (firstTokenTime > 0L) (System.currentTimeMillis() - firstTokenTime) / 1000.0 else 0.0
                } else {
                    val startTime = if (contentFirstTokenTime > 0L) contentFirstTokenTime else firstTokenTime
                    if (startTime > 0L) (System.currentTimeMillis() - startTime) / 1000.0 else 0.0
                }

                val finalSpeed = if (totalDurationSec > 0.1 && finalTokens > 0) {
                    finalTokens / totalDurationSec
                } else {
                    smoothedSpeed
                }

                if (finalOutput.isNotBlank()) {
                    val charMsg = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        sender = "CHARACTER",
                        content = finalOutput,
                        tokensCount = finalTokens,
                        generationSpeedTps = finalSpeed,
                        modelName = chosenModel
                    )
                    database.chatMessageDao().insertMessage(charMsg)

                    // Check if notification is needed (user closed chat or app in background)
                    val isViewingSession = isAppInForeground.value && (activeVisibleSessionId.value == sessionId)
                    if (!isViewingSession) {
                        ChatNotificationHelper.showResponseNotification(
                            context = context,
                            sessionId = sessionId,
                            characterName = character.name.ifBlank { "AI Character" },
                            messageContent = cleanContent,
                            avatarUri = character.avatarUri,
                            userName = persona?.name?.ifBlank { "You" } ?: "You",
                            userAvatarUri = persona?.avatarUri
                        )
                    }
                }
            } catch (e: Exception) {
                val rawMsg = e.message ?: "Unknown inference error"
                val cleanMsg = if (rawMsg.contains("<html", ignoreCase = true) || rawMsg.contains("<!DOCTYPE", ignoreCase = true)) {
                    val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(rawMsg)
                    val title = titleMatch?.groupValues?.get(1)?.trim()
                    if (!title.isNullOrBlank()) "Server Error: $title" else "Server Error: HTTP Bad Gateway (502)"
                } else {
                    rawMsg
                }
                stateFlow.value = stateFlow.value.copy(errorMessage = cleanMsg)
            } finally {
                stateFlow.value = stateFlow.value.copy(
                    isStreaming = false,
                    thinking = "",
                    text = "",
                    tokensCount = 0,
                    speedTps = 0.0,
                    modelName = ""
                )
                activeJobs.remove(sessionId)
            }
        }

        activeJobs[sessionId] = job
    }

    fun stopGeneration(sessionId: String, database: MiraiDatabase) {
        val job = activeJobs.remove(sessionId)
        job?.cancel()

        val stateFlow = streamStates[sessionId] ?: return
        val currentText = stateFlow.value.text.trim()
        val currentThinking = stateFlow.value.thinking.trim()
        val currentModel = stateFlow.value.modelName.ifBlank { null }
        val currentSpeed = stateFlow.value.speedTps
        val currentTokens = stateFlow.value.tokensCount

        val finalOutput = if (currentThinking.isNotBlank()) {
            "<think>\n$currentThinking\n</think>\n\n$currentText"
        } else {
            currentText
        }

        if (finalOutput.isNotBlank()) {
            scope.launch {
                val tokenCount = if (currentTokens > 0) currentTokens else TokenUtils.estimateTokenCount(finalOutput)
                val charMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    sender = "CHARACTER",
                    content = finalOutput,
                    tokensCount = tokenCount,
                    generationSpeedTps = currentSpeed,
                    modelName = currentModel
                )
                database.chatMessageDao().insertMessage(charMsg)
            }
        }

        stateFlow.value = GenerationStreamState()
    }

    fun dismissError(sessionId: String) {
        val stateFlow = streamStates[sessionId] ?: return
        stateFlow.value = stateFlow.value.copy(errorMessage = null)
    }
}
