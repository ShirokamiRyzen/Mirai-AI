package com.ryzumi.miraiai.data.datastore

data class AppSettings(
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val selectedModelId: String = "gpt-3.5-turbo",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val maxTokens: Int = 2048,
    val customHeaders: String = "",
    val availableModels: List<String> = emptyList()
)
