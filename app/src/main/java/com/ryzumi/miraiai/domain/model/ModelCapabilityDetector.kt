package com.ryzumi.miraiai.domain.model

data class ModelCapabilities(
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = false,
    val isReasoningModel: Boolean = false
)

/**
 * Purely visual display helper for showing capability badges in model selection UI.
 * DOES NOT affect runtime inference execution or vision sending logic.
 */
object ModelCapabilityDetector {
    fun detectCapabilities(modelId: String, knownVisionModels: List<String> = emptyList()): ModelCapabilities {
        if (modelId.isBlank()) return ModelCapabilities()

        val id = modelId.lowercase()

        val isVision = knownVisionModels.contains(modelId) ||
                id.contains("vision") || id.contains("vl") || id.contains("gpt-4o") ||
                id.contains("claude-3") || id.contains("gemini") || id.contains("pixtral") ||
                id.contains("qwen") || id.contains("llava") || id.contains("omni") ||
                id.contains("minicpm") || id.contains("molmo") || id.contains("internvl") ||
                id.contains("flash") || id.contains("mod") || id.contains("luna") ||
                id.contains("astra") || id.contains("exp") || id.contains("multimodal") ||
                id.contains("deepseek-v4") || id.contains("glm-5") || id.contains("gpt-5") ||
                id.contains("gpt-6")

        val isReasoning = id.contains("r1") || id.contains("reasoner") ||
                id.contains("o1") || id.contains("o3") || id.contains("qwq") ||
                id.contains("think") || id.contains("thought")

        val isTools = id.contains("gpt-") || id.contains("claude") || id.contains("gemini") ||
                id.contains("deepseek") || id.contains("glm") || id.contains("mistral") ||
                id.contains("qwen") || id.contains("llama") || id.contains("tools") ||
                id.contains("flash")

        return ModelCapabilities(
            supportsVision = isVision,
            supportsTools = isTools,
            isReasoningModel = isReasoning
        )
    }
}
