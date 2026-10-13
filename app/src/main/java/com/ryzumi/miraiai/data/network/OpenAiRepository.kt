package com.ryzumi.miraiai.data.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ryzumi.miraiai.domain.model.OpenAiContentPart
import com.ryzumi.miraiai.domain.model.OpenAiFunctionCall
import com.ryzumi.miraiai.domain.model.OpenAiMessage
import com.ryzumi.miraiai.domain.model.OpenAiToolCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

data class ModelFetchResult(
    val allModels: List<String>,
    val visionModels: List<String>
)

class OpenAiRepository {
    private val gson = Gson()
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .build()

    private val sseClient = EventSources.createFactory(client)

    suspend fun fetchAvailableModels(
        baseUrl: String,
        apiKey: String,
        customHeadersJson: String = ""
    ): Result<ModelFetchResult> = withContext(Dispatchers.IO) {
        try {
            val sanitizedBaseUrl = baseUrl.trim().removeSuffix("/")
            val url = if (sanitizedBaseUrl.endsWith("/v1")) {
                "$sanitizedBaseUrl/models"
            } else {
                "$sanitizedBaseUrl/v1/models"
            }

            val headersBuilder = Headers.Builder()
            if (apiKey.isNotBlank()) {
                headersBuilder.add("Authorization", "Bearer $apiKey")
            }
            parseAndAddCustomHeaders(headersBuilder, customHeadersJson)

            val request = Request.Builder()
                .url(url)
                .headers(headersBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to fetch models: HTTP ${response.code} ${response.message}")
                    )
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                val jsonObject = JsonParser.parseString(bodyString).asJsonObject
                val dataArray = jsonObject.getAsJsonArray("data")
                    ?: return@withContext Result.failure(Exception("No 'data' field found in response"))

                val allModels = mutableListOf<String>()
                val visionModels = mutableListOf<String>()

                for (element in dataArray) {
                    if (element.isJsonObject) {
                        val modelObj = element.asJsonObject
                        val id = modelObj.get("id")?.asString ?: continue

                        // Check if model is explicitly disabled
                        if (modelObj.has("enabled") && !modelObj.get("enabled").asBoolean) {
                            continue
                        }

                        allModels.add(id)

                        // Check vision capability
                        var isVision = false
                        if (modelObj.has("vision") && modelObj.get("vision").asBoolean) {
                            isVision = true
                        } else if (modelObj.has("modalities") && modelObj.get("modalities").isJsonObject) {
                            val modalitiesObj = modelObj.getAsJsonObject("modalities")
                            if (modalitiesObj.has("input") && modalitiesObj.get("input").isJsonArray) {
                                val inputArr = modalitiesObj.getAsJsonArray("input")
                                for (mod in inputArr) {
                                    if (mod.asString.equals("image", ignoreCase = true)) {
                                        isVision = true
                                        break
                                    }
                                }
                            }
                        } else if (com.ryzumi.miraiai.domain.model.ModelCapabilityDetector.detectCapabilities(id).supportsVision) {
                            isVision = true
                        }

                        if (isVision) {
                            visionModels.add(id)
                        }
                    }
                }

                allModels.sort()
                visionModels.sort()

                Result.success(
                    ModelFetchResult(
                        allModels = allModels,
                        visionModels = visionModels
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class SingleTurnResult(
        val accumulatedContent: String,
        val accumulatedReasoning: String,
        val toolCalls: List<AccumulatedToolCall>
    )

    data class AccumulatedToolCall(
        var id: String = "",
        var name: String = "",
        var arguments: String = ""
    )

    fun streamChatCompletions(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<OpenAiMessage>,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        maxTokens: Int = 2048,
        customHeadersJson: String = "",
        tools: List<JsonObject>? = null,
        toolExecutor: (suspend (name: String, arguments: String) -> String)? = null
    ): Flow<StreamChunk> = callbackFlow {
        val repoScope = this

        val job = repoScope.launch {
            try {
                var currentMessages = messages.toMutableList()
                var currentTools = tools
                var loopCount = 0
                val maxLoops = 5

                while (loopCount < maxLoops) {
                    loopCount++
                    val turnResult = try {
                        executeSingleTurn(
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            modelId = modelId,
                            messages = currentMessages,
                            temperature = temperature,
                            topP = topP,
                            maxTokens = maxTokens,
                            customHeadersJson = customHeadersJson,
                            tools = currentTools,
                            onChunk = { chunk ->
                                trySend(chunk)
                            }
                        )
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        val is400or422 = errMsg.contains("400") || errMsg.contains("422") || errMsg.contains("rejected", ignoreCase = true) || errMsg.contains("unsupported", ignoreCase = true) || errMsg.contains("schema", ignoreCase = true) || errMsg.contains("attachment", ignoreCase = true)

                        if (is400or422) {
                            if (currentTools != null) {
                                // Stage 1 Fallback: Retry without tools, but KEEP image_url attachments intact in currentMessages
                                android.util.Log.w("OpenAiRepository", "Request with tools failed ($errMsg). Stage 1 Retry: Dropping tools while preserving image attachments...", e)
                                currentTools = null
                                try {
                                    executeSingleTurn(
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        modelId = modelId,
                                        messages = currentMessages,
                                        temperature = temperature,
                                        topP = topP,
                                        maxTokens = maxTokens,
                                        customHeadersJson = customHeadersJson,
                                        tools = null,
                                        onChunk = { chunk -> trySend(chunk) }
                                    )
                                } catch (e2: Exception) {
                                    // Stage 2 Fallback: If model still rejects request (e.g. text-only model that cannot read attachments), sanitize messages to plain text
                                    val errMsg2 = e2.message ?: ""
                                    android.util.Log.w("OpenAiRepository", "Stage 1 Retry failed ($errMsg2). Stage 2 Retry: Sanitizing messages to plain text...", e2)
                                    currentMessages = sanitizeMessagesToPlainText(currentMessages).toMutableList()
                                    executeSingleTurn(
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        modelId = modelId,
                                        messages = currentMessages,
                                        temperature = temperature,
                                        topP = topP,
                                        maxTokens = maxTokens,
                                        customHeadersJson = customHeadersJson,
                                        tools = null,
                                        onChunk = { chunk -> trySend(chunk) }
                                    )
                                }
                            } else {
                                // Stage 2 Fallback: Tools were already null, sanitize messages to plain text
                                android.util.Log.w("OpenAiRepository", "Request failed ($errMsg). Stage 2 Retry: Sanitizing messages to plain text...", e)
                                currentMessages = sanitizeMessagesToPlainText(currentMessages).toMutableList()
                                executeSingleTurn(
                                    baseUrl = baseUrl,
                                    apiKey = apiKey,
                                    modelId = modelId,
                                    messages = currentMessages,
                                    temperature = temperature,
                                    topP = topP,
                                    maxTokens = maxTokens,
                                    customHeadersJson = customHeadersJson,
                                    tools = null,
                                    onChunk = { chunk -> trySend(chunk) }
                                )
                            }
                        } else {
                            throw e
                        }
                    }

                    if (turnResult.toolCalls.isNotEmpty() && toolExecutor != null) {
                        val isDsmlCall = turnResult.toolCalls.any { it.id.startsWith("call_dsml_") }

                        if (isDsmlCall) {
                            val rawAssistantContent = if (turnResult.accumulatedContent.isNotBlank()) {
                                turnResult.accumulatedContent
                            } else {
                                val sb = StringBuilder("< | DSML | calls>\n")
                                for (tc in turnResult.toolCalls) {
                                    sb.append("< | DSML | invoke name=\"${tc.name}\">\n")
                                    try {
                                        val argsObj = JsonParser.parseString(tc.arguments).asJsonObject
                                        for ((k, v) in argsObj.entrySet()) {
                                            sb.append("< | DSML | parameter name=\"$k\" string=\"true\">${v.asString}</ | DSML | parameter>\n")
                                        }
                                    } catch (e: Exception) { }
                                    sb.append("</ | DSML | invoke>\n")
                                }
                                sb.append("</ | DSML | calls>")
                                sb.toString()
                            }
                            currentMessages.add(
                                OpenAiMessage(
                                    role = "assistant",
                                    content = rawAssistantContent
                                )
                            )
                        } else {
                            currentMessages.add(
                                OpenAiMessage(
                                    role = "assistant",
                                    content = if (turnResult.accumulatedContent.isNotBlank()) turnResult.accumulatedContent else null,
                                    tool_calls = turnResult.toolCalls.mapIndexed { idx, tc ->
                                        OpenAiToolCall(
                                            id = tc.id.ifBlank { "call_${System.currentTimeMillis()}_$idx" },
                                            function = OpenAiFunctionCall(name = tc.name, arguments = tc.arguments)
                                        )
                                    }
                                )
                            )
                        }

                        for ((idx, tc) in turnResult.toolCalls.withIndex()) {
                            val callId = tc.id.ifBlank { "call_${System.currentTimeMillis()}_$idx" }
                            trySend(StreamChunk(thinking = "\n[Tool: ${tc.name}]\n"))
                            val output = toolExecutor(tc.name, tc.arguments)
                            if (isDsmlCall) {
                                currentMessages.add(
                                    OpenAiMessage(
                                        role = "user",
                                        content = "[Tool Output for ${tc.name}]:\n$output"
                                    )
                                )
                            } else {
                                currentMessages.add(
                                    OpenAiMessage(
                                        role = "tool",
                                        name = tc.name,
                                        tool_call_id = callId,
                                        content = output
                                    )
                                )
                            }
                        }
                        currentTools = null // Do not re-request tools on follow-up answer
                    } else {
                        break
                    }
                }
                close()
            } catch (e: Exception) {
                close(e)
            }
        }

        awaitClose {
            job.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun executeSingleTurn(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<OpenAiMessage>,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        customHeadersJson: String,
        tools: List<JsonObject>?,
        onChunk: (StreamChunk) -> Unit
    ): SingleTurnResult = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val startTime = System.currentTimeMillis()
        val sanitizedBaseUrl = baseUrl.trim().removeSuffix("/")
        val url = if (sanitizedBaseUrl.endsWith("/v1")) {
            "$sanitizedBaseUrl/chat/completions"
        } else {
            "$sanitizedBaseUrl/v1/chat/completions"
        }

        val hasVisionMessage = messages.any { it.content is List<*> }
        val isCloudOpenAi = baseUrl.contains("openrouter.ai", ignoreCase = true) || baseUrl.contains("api.openai.com", ignoreCase = true)

        val sanitizedTemperature = (Math.round(temperature * 100f) / 100f)
        val sanitizedTopP = (Math.round(topP * 100f) / 100f)

        val requestBodyJson = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", gson.toJsonTree(messages))
            addProperty("temperature", sanitizedTemperature)
            addProperty("top_p", sanitizedTopP)
            addProperty("max_tokens", maxTokens)
            addProperty("stream", true)
            if (!tools.isNullOrEmpty()) {
                add("tools", gson.toJsonTree(tools))
                addProperty("tool_choice", "auto")
            }
            if (isCloudOpenAi) {
                add("stream_options", JsonObject().apply { addProperty("include_usage", true) })
            }
        }

        val rawJsonPayload = requestBodyJson.toString()

        val headersBuilder = Headers.Builder()
            .add("Accept", "text/event-stream")
            .add("Content-Type", "application/json")

        val loggedHeaders = mutableMapOf<String, String>()
        loggedHeaders["Content-Type"] = "application/json"
        loggedHeaders["Accept"] = "text/event-stream"

        if (apiKey.isNotBlank()) {
            headersBuilder.add("Authorization", "Bearer $apiKey")
            loggedHeaders["Authorization"] = "Bearer " + apiKey.take(6) + "..." + apiKey.takeLast(4)
        }
        parseAndAddCustomHeaders(headersBuilder, customHeadersJson)

        val logId = DebugLogManager.logRequest(
            title = if (hasVisionMessage) "Chat Completion (Vision)" else "Chat Completion (Text)",
            type = if (hasVisionMessage) "CHAT_VISION" else "CHAT_TEXT",
            modelId = modelId,
            endpointUrl = url,
            headers = loggedHeaders,
            rawJsonPayload = rawJsonPayload
        )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .headers(headersBuilder.build())
            .post(rawJsonPayload.toRequestBody(mediaType))
            .build()

        val accumulatedResponse = StringBuilder()
        val accumulatedReasoning = StringBuilder()
        val thinkFilter = ThinkTagFilter()

        class ToolAcc(var id: String = "", var name: String = "", val args: StringBuilder = StringBuilder())
        val toolMap = mutableMapOf<Int, ToolAcc>()
        var isCompleted = false

        val eventSource = sseClient.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    val duration = System.currentTimeMillis() - startTime
                    val remaining = thinkFilter.flush()
                    if (remaining.thinking.isNotEmpty()) {
                        onChunk(StreamChunk(thinking = remaining.thinking))
                    }
                    if (remaining.content.isNotEmpty()) {
                        onChunk(StreamChunk(content = remaining.content))
                    }

                    val finalFullText = if (accumulatedReasoning.isNotEmpty()) {
                        "[Thinking Process]\n$accumulatedReasoning\n\n[Response]\n$accumulatedResponse"
                    } else {
                        accumulatedResponse.toString()
                    }
                    DebugLogManager.updateResponse(logId, finalFullText, 200, duration)

                    if (!isCompleted) {
                        isCompleted = true
                        val (cleanContent, dsmlCalls) = parseDsmlToolCalls(accumulatedResponse.toString())
                        val resultToolCalls = toolMap.values.map {
                            AccumulatedToolCall(id = it.id, name = it.name, arguments = it.args.toString())
                        } + dsmlCalls
                        continuation.resumeWith(Result.success(SingleTurnResult(cleanContent, accumulatedReasoning.toString(), resultToolCalls)))
                    }
                    return
                }

                try {
                    val jsonObject = JsonParser.parseString(data).asJsonObject
                    val choices = jsonObject.getAsJsonArray("choices")
                    if (choices != null && choices.size() > 0) {
                        val choice = choices[0].asJsonObject
                        val delta = choice.getAsJsonObject("delta")
                        if (delta != null) {
                            if (delta.has("tool_calls") && delta.get("tool_calls").isJsonArray) {
                                val tcArr = delta.getAsJsonArray("tool_calls")
                                for (elem in tcArr) {
                                    if (!elem.isJsonObject) continue
                                    val tcObj = elem.asJsonObject
                                    val index = if (tcObj.has("index")) tcObj.get("index").asInt else 0
                                    val acc = toolMap.getOrPut(index) { ToolAcc() }
                                    if (tcObj.has("id") && !tcObj.get("id").isJsonNull) {
                                        acc.id = tcObj.get("id").asString
                                    }
                                    if (tcObj.has("function") && tcObj.get("function").isJsonObject) {
                                        val fn = tcObj.getAsJsonObject("function")
                                        if (fn.has("name") && !fn.get("name").isJsonNull) {
                                            acc.name = fn.get("name").asString
                                        }
                                        if (fn.has("arguments") && !fn.get("arguments").isJsonNull) {
                                            acc.args.append(fn.get("arguments").asString)
                                        }
                                    }
                                }
                            }

                            val content = if (delta.has("content") && !delta.get("content").isJsonNull) {
                                delta.get("content").asString
                            } else null

                            val reasoning = if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull) {
                                delta.get("reasoning_content").asString
                            } else if (delta.has("reasoning") && !delta.get("reasoning").isJsonNull) {
                                delta.get("reasoning").asString
                            } else null

                            if (!reasoning.isNullOrEmpty()) {
                                accumulatedReasoning.append(reasoning)
                                onChunk(StreamChunk(thinking = reasoning))
                            }

                            if (!content.isNullOrEmpty()) {
                                accumulatedResponse.append(content)
                                val filterResult = thinkFilter.filter(content)
                                if (filterResult.thinking.isNotEmpty()) {
                                    onChunk(StreamChunk(thinking = filterResult.thinking))
                                }
                                if (filterResult.content.isNotEmpty()) {
                                    onChunk(StreamChunk(content = filterResult.content))
                                }
                            }

                            val fullLog = if (accumulatedReasoning.isNotEmpty()) {
                                "[Thinking Process]\n$accumulatedReasoning\n\n[Response]\n$accumulatedResponse"
                            } else {
                                accumulatedResponse.toString()
                            }
                            DebugLogManager.updateStreamingResponse(logId, fullLog)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val duration = System.currentTimeMillis() - startTime
                val responseBody = try { response?.body?.string() } catch (e: Exception) { null }
                val code = response?.code ?: 500
                val serverMsg = if (!responseBody.isNullOrBlank()) responseBody else (response?.message ?: "Unknown error")
                val errMsg = if (t != null) t.message ?: "Unknown network error" else "HTTP $code: $serverMsg"
                DebugLogManager.updateResponse(logId, accumulatedResponse.toString(), code, duration, errMsg)
                if (!isCompleted) {
                    isCompleted = true
                    continuation.resumeWith(Result.failure(t ?: Exception(errMsg)))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                val duration = System.currentTimeMillis() - startTime
                DebugLogManager.updateResponse(logId, accumulatedResponse.toString(), 200, duration)
                if (!isCompleted) {
                    isCompleted = true
                    val (cleanContent, dsmlCalls) = parseDsmlToolCalls(accumulatedResponse.toString())
                    val resultToolCalls = toolMap.values.map {
                        AccumulatedToolCall(id = it.id, name = it.name, arguments = it.args.toString())
                    } + dsmlCalls
                    continuation.resumeWith(Result.success(SingleTurnResult(cleanContent, accumulatedReasoning.toString(), resultToolCalls)))
                }
            }
        })

        continuation.invokeOnCancellation {
            eventSource.cancel()
        }
    }

    private fun parseDsmlToolCalls(rawContent: String): Pair<String, List<AccumulatedToolCall>> {
        val dsmlToolCalls = mutableListOf<AccumulatedToolCall>()

        val invokeRegex = Regex(
            """<[\s|]*DSML[\s|]*invoke\s+name=["']?([^"'>\s]+)["']?\s*>(.*?)(?:</[\s|]*DSML[\s|]*invoke>|$)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        val paramRegex = Regex(
            """<[\s|]*DSML[\s|]*parameter\s+name=["']?([^"'>\s]+)["']?[^>]*>(.*?)(?:</[\s|]*DSML[\s|]*parameter>|$)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        val matches = invokeRegex.findAll(rawContent)
        for ((idx, match) in matches.withIndex()) {
            val fnName = match.groupValues[1].trim()
            val body = match.groupValues[2]

            val argsObj = JsonObject()
            val paramMatches = paramRegex.findAll(body)
            for (pMatch in paramMatches) {
                val paramName = pMatch.groupValues[1].trim()
                val paramValue = pMatch.groupValues[2].trim()
                argsObj.addProperty(paramName, paramValue)
            }

            dsmlToolCalls.add(
                AccumulatedToolCall(
                    id = "call_dsml_${System.currentTimeMillis()}_$idx",
                    name = fnName,
                    arguments = argsObj.toString()
                )
            )
        }

        var cleanContent = rawContent
        cleanContent = cleanContent.replace(
            Regex("""<[\s|]*DSML[\s|]*calls\s*>(.*?)</[\s|]*DSML[\s|]*calls>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
            ""
        )
        cleanContent = cleanContent.replace(
            Regex("""<[\s|]*DSML[\s|]*invoke.*?(?:</[\s|]*DSML[\s|]*invoke>|$)""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
            ""
        )
        cleanContent = cleanContent.replace(
            Regex("""</?[\s|]*DSML[\s|]*[^>]*>""", RegexOption.IGNORE_CASE),
            ""
        ).trim()

        return Pair(cleanContent, dsmlToolCalls)
    }

    private fun sanitizeMessagesToPlainText(messages: List<OpenAiMessage>): List<OpenAiMessage> {
        return messages.map { msg ->
            val plainTextContent = when (val c = msg.content) {
                is String -> c
                is List<*> -> {
                    c.filterIsInstance<OpenAiContentPart>()
                        .mapNotNull { it.text }
                        .joinToString("\n")
                        .ifEmpty { "Analyze this image" }
                }
                else -> c?.toString() ?: ""
            }
            OpenAiMessage(
                role = msg.role,
                content = plainTextContent,
                name = msg.name,
                tool_call_id = msg.tool_call_id
            )
        }
    }

    private fun parseAndAddCustomHeaders(builder: Headers.Builder, customHeadersJson: String) {
        if (customHeadersJson.isBlank()) return
        try {
            val jsonObject = JsonParser.parseString(customHeadersJson).asJsonObject
            for ((key, value) in jsonObject.entrySet()) {
                if (value.isJsonPrimitive) {
                    builder.add(key, value.asString)
                }
            }
        } catch (e: Exception) {
            // Ignore custom header parsing issues
        }
    }
}

data class StreamChunk(
    val content: String = "",
    val thinking: String = ""
)

class ThinkTagFilter {
    private var insideThink = false
    private var insideDsml = false
    private val buffer = StringBuilder()

    data class FilterResult(
        val content: String,
        val thinking: String
    )

    fun filter(chunk: String): FilterResult {
        buffer.append(chunk)
        val contentResult = StringBuilder()
        val thinkingResult = StringBuilder()

        while (buffer.isNotEmpty()) {
            val text = buffer.toString()
            if (!insideThink && !insideDsml) {
                val thinkStart = text.indexOf("<think>")
                val dsmlMatch = findDsmlMatch(text)

                if (thinkStart != -1 && (dsmlMatch == null || thinkStart < dsmlMatch.range.first)) {
                    contentResult.append(text.substring(0, thinkStart))
                    buffer.delete(0, thinkStart + 7)
                    insideThink = true
                } else if (dsmlMatch != null) {
                    contentResult.append(text.substring(0, dsmlMatch.range.first))
                    buffer.delete(0, dsmlMatch.range.last + 1)
                    insideDsml = true
                } else {
                    val partialThink = findPartialTagStart(text, "<think>")
                    val partialDsml = findPartialDsmlStart(text)
                    val partialIndex = when {
                        partialThink != -1 && partialDsml != -1 -> minOf(partialThink, partialDsml)
                        partialThink != -1 -> partialThink
                        else -> partialDsml
                    }

                    if (partialIndex != -1) {
                        contentResult.append(text.substring(0, partialIndex))
                        val remaining = text.substring(partialIndex)
                        buffer.clear()
                        buffer.append(remaining)
                        break
                    } else {
                        contentResult.append(text)
                        buffer.clear()
                    }
                }
            } else if (insideThink) {
                val thinkEnd = text.indexOf("</think>")
                if (thinkEnd != -1) {
                    thinkingResult.append(text.substring(0, thinkEnd))
                    buffer.delete(0, thinkEnd + 8)
                    insideThink = false
                } else {
                    val partialIndex = findPartialTagStart(text, "</think>")
                    if (partialIndex != -1) {
                        thinkingResult.append(text.substring(0, partialIndex))
                        val remaining = text.substring(partialIndex)
                        buffer.clear()
                        buffer.append(remaining)
                        break
                    } else {
                        thinkingResult.append(text)
                        buffer.clear()
                    }
                }
            } else { // insideDsml
                val dsmlEndMatch = findDsmlEndMatch(text)
                if (dsmlEndMatch != null) {
                    buffer.delete(0, dsmlEndMatch.range.last + 1)
                    insideDsml = false
                } else {
                    buffer.clear()
                    break
                }
            }
        }
        return FilterResult(content = contentResult.toString(), thinking = thinkingResult.toString())
    }

    fun flush(): FilterResult {
        return if (insideThink) {
            val text = buffer.toString()
            buffer.clear()
            FilterResult(content = "", thinking = text)
        } else if (insideDsml) {
            buffer.clear()
            FilterResult(content = "", thinking = "")
        } else {
            val text = buffer.toString()
            buffer.clear()
            FilterResult(content = text, thinking = "")
        }
    }

    private fun findDsmlMatch(text: String): MatchResult? {
        val regexCalls = Regex("""<[\s|]*DSML[\s|]*calls\s*>""", RegexOption.IGNORE_CASE)
        val matchCalls = regexCalls.find(text)
        if (matchCalls != null) return matchCalls

        val regexInvoke = Regex("""<[\s|]*DSML[\s|]*invoke\b""", RegexOption.IGNORE_CASE)
        return regexInvoke.find(text)
    }

    private fun findDsmlEndMatch(text: String): MatchResult? {
        val regexCalls = Regex("""</[\s|]*DSML[\s|]*calls\s*>""", RegexOption.IGNORE_CASE)
        val matchCalls = regexCalls.find(text)
        if (matchCalls != null) return matchCalls

        val regexInvoke = Regex("""</[\s|]*DSML[\s|]*invoke\s*>""", RegexOption.IGNORE_CASE)
        return regexInvoke.find(text)
    }

    private fun findPartialDsmlStart(text: String): Int {
        val prefixes = listOf("<", "< ", "<|", "< |", "< | D", "< | DS", "< | DSM", "< | DSML")
        for (prefix in prefixes.asReversed()) {
            if (text.endsWith(prefix)) {
                return text.length - prefix.length
            }
        }
        return -1
    }

    private fun findPartialTagStart(text: String, tag: String): Int {
        for (i in 1 until tag.length) {
            val sub = tag.substring(0, i)
            if (text.endsWith(sub)) {
                return text.length - i
            }
        }
        return -1
    }
}
