package com.ryzumi.miraiai.domain.model

enum class ModelCompatibility {
    OPTIMAL,       // Runs very smoothly
    MODERATE,      // Playable / fits with moderate context
    LOW_MEMORY     // Heavy / likely to lag or exceed device RAM
}

data class HuggingFaceModel(
    val id: String,
    val modelName: String,
    val author: String,
    val downloads: Int = 0,
    val likes: Int = 0,
    val tags: List<String> = emptyList(),
    val pipelineTag: String? = null,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val estimatedSizeGb: Double = 0.0,
    val formattedSize: String = "Unknown",
    val hasVisionCapability: Boolean = false,
    val hasImageGenCapability: Boolean = false,
    val requiredRamGb: Double = 0.0,
    val compatibility: ModelCompatibility = ModelCompatibility.MODERATE,
    val downloadUrl: String? = null,
    val selectedFileName: String? = null
)
