package com.ryzumi.miraiai.domain.model

import java.io.File

enum class LocalModelType(val displayName: String, val badgeLabel: String, val engineName: String) {
    TEXT_LLM("Text Generate Model", "Text (LiteRT)", "LiteRT"),
    VISION("Vision Model", "Vision (LiteRT)", "LiteRT"),
    IMAGE_GEN("Image Generation Model", "Imagen (MNN / ONNX)", "MNN / ONNX Runtime"),
    VOICE_TTS("Voice Model", "Voice (Sherpa-onnx)", "Sherpa-onnx")
}

object LocalModelClassifier {

    fun isVoiceModel(fileName: String, absolutePath: String = ""): Boolean {
        val name = fileName.lowercase()
        val path = absolutePath.lowercase()
        return name.endsWith(".onnx") ||
                path.contains("/models/tts/") ||
                path.contains("\\models\\tts\\") ||
                name.contains("kokoro") ||
                name.contains("piper") ||
                name.contains("tts") ||
                name.contains("voice") ||
                name.contains("speech") ||
                name.contains("audio") ||
                name.contains("vits") ||
                name.contains("silero")
    }

    fun isVoiceModel(file: File): Boolean = isVoiceModel(file.name, file.absolutePath)

    fun isImageGenModel(fileName: String, absolutePath: String = ""): Boolean {
        if (isVoiceModel(fileName, absolutePath)) return false
        val name = fileName.lowercase()
        return name.contains("diffusion") ||
                name.contains("diffusers") ||
                name.contains("stable-diffusion") ||
                name.contains("sdxl") ||
                name.contains("flux") ||
                name.contains("sd15") ||
                name.contains("sd21") ||
                name.contains("sd3") ||
                name.contains("pixart") ||
                name.contains("kolors") ||
                name.contains("cascade") ||
                name.contains("clip_g") ||
                name.contains("t5xxl") ||
                (name.contains("sd_") && name.endsWith(".gguf")) ||
                (name.contains("sd-") && name.endsWith(".gguf"))
    }

    fun isImageGenModel(file: File): Boolean = isImageGenModel(file.name, file.absolutePath)

    fun isVisionModel(fileName: String, absolutePath: String = ""): Boolean {
        if (isVoiceModel(fileName, absolutePath) || isImageGenModel(fileName, absolutePath)) return false
        val name = fileName.lowercase()
        return name.contains("vision") ||
                name.contains("llava") ||
                name.contains("moondream") ||
                name.contains("minicpm-v") ||
                name.contains("minicpm_v") ||
                name.contains("qwen-vl") ||
                name.contains("qwen_vl") ||
                name.contains("qwenvl") ||
                name.contains("internvl") ||
                name.contains("cogvlm") ||
                name.contains("multimodal") ||
                name.contains("bakllava") ||
                name.contains("deepseek-vl") ||
                name.contains("deepseek_vl") ||
                name.contains("omni") ||
                name.contains("vl-") ||
                name.contains("-vl")
    }

    fun isVisionModel(file: File): Boolean = isVisionModel(file.name, file.absolutePath)

    fun isTextModel(fileName: String, absolutePath: String = ""): Boolean {
        if (isVoiceModel(fileName, absolutePath) || isImageGenModel(fileName, absolutePath)) return false
        val name = fileName.lowercase()
        return name.endsWith(".gguf") || name.endsWith(".bin")
    }

    fun isTextModel(file: File): Boolean = isTextModel(file.name, file.absolutePath)

    fun classify(fileName: String, absolutePath: String = ""): LocalModelType {
        return when {
            isVoiceModel(fileName, absolutePath) -> LocalModelType.VOICE_TTS
            isImageGenModel(fileName, absolutePath) -> LocalModelType.IMAGE_GEN
            isVisionModel(fileName, absolutePath) -> LocalModelType.VISION
            else -> LocalModelType.TEXT_LLM
        }
    }

    fun classify(file: File): LocalModelType = classify(file.name, file.absolutePath)
}
