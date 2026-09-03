package com.ryzumi.miraiai.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DebugLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val type: String, // "CHAT_VISION", "CHAT_TEXT", "TEST_VISION", "MODELS_FETCH", "ERROR"
    val modelId: String,
    val endpointUrl: String,
    val requestHeaders: Map<String, String>,
    val requestPayloadFormatted: String,
    val requestPayloadRaw: String,
    val responseBody: String? = null,
    val isStreaming: Boolean = false,
    val durationMs: Long = 0,
    val error: String? = null,
    val httpStatusCode: Int? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

object DebugLogManager {
    private const val MAX_LOGS = 50
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    private val _isLoggingEnabled = MutableStateFlow(false)
    val isLoggingEnabled: StateFlow<Boolean> = _isLoggingEnabled.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setLoggingEnabled(enabled: Boolean) {
        _isLoggingEnabled.value = enabled
        if (!enabled) {
            _logs.value = emptyList()
        }
    }

    fun logRequest(
        title: String,
        type: String,
        modelId: String,
        endpointUrl: String,
        headers: Map<String, String>,
        rawJsonPayload: String
    ): String {
        if (!_isLoggingEnabled.value && type != "TEST_VISION") {
            return ""
        }

        val logId = UUID.randomUUID().toString()
        val formattedJson = try {
            val jsonElement = JsonParser.parseString(rawJsonPayload)
            gson.toJson(jsonElement)
        } catch (e: Exception) {
            rawJsonPayload
        }

        val entry = DebugLogEntry(
            id = logId,
            timestamp = System.currentTimeMillis(),
            title = title,
            type = type,
            modelId = modelId,
            endpointUrl = endpointUrl,
            requestHeaders = headers,
            requestPayloadFormatted = formattedJson,
            requestPayloadRaw = rawJsonPayload,
            isStreaming = true
        )

        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > MAX_LOGS) {
            _logs.value = currentList.take(MAX_LOGS)
        } else {
            _logs.value = currentList
        }

        return logId
    }

    fun updateStreamingResponse(logId: String, currentText: String) {
        if (!_isLoggingEnabled.value || logId.isBlank()) return
        val currentList = _logs.value.map { entry ->
            if (entry.id == logId) {
                entry.copy(responseBody = currentText)
            } else {
                entry
            }
        }
        _logs.value = currentList
    }

    fun updateResponse(
        logId: String,
        response: String,
        statusCode: Int = 200,
        durationMs: Long = 0,
        error: String? = null
    ) {
        if (!_isLoggingEnabled.value || logId.isBlank()) return
        val currentList = _logs.value.map { entry ->
            if (entry.id == logId) {
                entry.copy(
                    responseBody = response,
                    isStreaming = false,
                    httpStatusCode = statusCode,
                    durationMs = durationMs,
                    error = error
                )
            } else {
                entry
            }
        }
        _logs.value = currentList
    }

    fun logError(
        title: String,
        type: String,
        modelId: String,
        endpointUrl: String,
        errorMsg: String
    ) {
        if (!_isLoggingEnabled.value && type != "TEST_VISION") return
        val entry = DebugLogEntry(
            title = title,
            type = type,
            modelId = modelId,
            endpointUrl = endpointUrl,
            requestHeaders = emptyMap(),
            requestPayloadFormatted = "{}",
            requestPayloadRaw = "{}",
            error = errorMsg,
            isStreaming = false
        )
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        _logs.value = currentList.take(MAX_LOGS)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Executes a self-contained Vision Test to verify whether the configured model
     * and API endpoint successfully parse and respond to image payloads.
     */
    suspend fun testVisionCapability(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        customHeadersJson: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val sanitizedBaseUrl = baseUrl.trim().removeSuffix("/")
            val url = if (sanitizedBaseUrl.endsWith("/v1")) {
                "$sanitizedBaseUrl/chat/completions"
            } else {
                "$sanitizedBaseUrl/v1/chat/completions"
            }

            // 1x1 red PNG data URL
            val testImageDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

            val requestBodyJson = """
            {
              "model": "$modelId",
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "text",
                      "text": "What is in this image? Respond with a single short sentence confirming you see a red pixel/image."
                    },
                    {
                      "type": "image_url",
                      "image_url": {
                        "url": "$testImageDataUrl"
                      }
                    }
                  ]
                }
              ],
              "max_tokens": 150,
              "temperature": 0.2
            }
            """.trimIndent()

            val headersMap = mutableMapOf<String, String>()
            headersMap["Content-Type"] = "application/json"
            if (apiKey.isNotBlank()) {
                headersMap["Authorization"] = "Bearer $apiKey"
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            if (customHeadersJson.isNotBlank()) {
                try {
                    val customHeadersObj = JsonParser.parseString(customHeadersJson).asJsonObject
                    for ((k, v) in customHeadersObj.entrySet()) {
                        if (v.isJsonPrimitive) {
                            headersMap[k] = v.asString
                            requestBuilder.addHeader(k, v.asString)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore header parsing errors
                }
            }

            val logId = logRequest(
                title = "Vision Capability Test",
                type = "TEST_VISION",
                modelId = modelId,
                endpointUrl = url,
                headers = headersMap,
                rawJsonPayload = requestBodyJson
            )

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = requestBuilder
                .post(requestBodyJson.toRequestBody(mediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}\n$bodyStr"
                    updateResponse(logId, bodyStr, response.code, duration, errMsg)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val parsedContent = try {
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    val choices = json.getAsJsonArray("choices")
                    val message = choices[0].asJsonObject.getAsJsonObject("message")
                    message.get("content").asString
                } catch (e: Exception) {
                    bodyStr
                }

                updateResponse(logId, parsedContent, response.code, duration)
                Result.success(parsedContent)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logError("Vision Test Error", "ERROR", modelId, baseUrl, e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
}
