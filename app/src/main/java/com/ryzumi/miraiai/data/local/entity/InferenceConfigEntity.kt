package com.ryzumi.miraiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "inference_configs")
data class InferenceConfigEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "Default Profile",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val generateModelId: String = "auto",
    val useLocalGenModel: Boolean = false,
    val visionModelId: String = "",
    val useLocalVisionModel: Boolean = false,
    val imageGenModelId: String = "none",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val maxTokens: Int = 2048,
    val customHeaders: String = "",
    val availableModelsJson: String = "[]",
    val isActive: Boolean = false
)
