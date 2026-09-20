package com.ryzumi.miraiai.domain.engine

import android.content.Context
import android.util.Log
import com.ryzumi.miraiai.domain.model.LocalModelClassifier
import com.ryzumi.miraiai.domain.model.LocalModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class ImageGenProgress(
    val isGenerating: Boolean = false,
    val progressPercent: Int = 0,
    val statusText: String = "",
    val prompt: String = "",
    val modelName: String = "",
    val generatedImageUri: String? = null,
    val error: String? = null
)

/**
 * Image Generation Manager using MNN / ONNX Runtime local diffusion architecture.
 * Directly runs inference on local model weights without fake synthetic canvas fallbacks.
 */
object ImageGenerationManager {
    private const val TAG = "ImageGenManager"

    private val _progressFlow = MutableStateFlow(ImageGenProgress())
    val progressFlow: StateFlow<ImageGenProgress> = _progressFlow.asStateFlow()

    @Volatile
    private var lastGeneratedImageUri: String? = null

    fun isGenerating(): Boolean = _progressFlow.value.isGenerating

    fun getLastGeneratedImage(): String? = lastGeneratedImageUri

    fun consumeLastGeneratedImage(): String? {
        val uri = lastGeneratedImageUri
        lastGeneratedImageUri = null
        return uri
    }

    suspend fun generateImage(
        context: Context,
        prompt: String,
        modelName: String = "Stable Diffusion 3.5"
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Prompt tidak boleh kosong"))
        }

        val modelsDir = File(context.filesDir, "models")
        var targetModelFile: File? = null

        // 1. Check if modelName is specified and exists
        if (modelName.isNotBlank() && modelName != "none" && !modelName.startsWith("none (", ignoreCase = true)) {
            val direct = File(modelsDir, modelName)
            if (direct.exists() && direct.length() > 0) {
                targetModelFile = direct
            } else {
                targetModelFile = modelsDir.listFiles()?.firstOrNull {
                    it.isFile && (it.name.equals(modelName, ignoreCase = true) || it.name.contains(modelName, ignoreCase = true))
                }
            }
        }

        // 2. Check loaded local model in RAM
        if (targetModelFile == null) {
            val loadedName = LocalModelManager.loadedModelName.value
            if (!loadedName.isNullOrBlank()) {
                val direct = File(modelsDir, loadedName)
                if (direct.exists() && direct.length() > 0) {
                    targetModelFile = direct
                }
            }
        }

        // 3. Search modelsDir for any diffusion model (FLUX, SD, MNN, ONNX, GGUF)
        if (targetModelFile == null && modelsDir.exists()) {
            targetModelFile = modelsDir.listFiles()?.firstOrNull { file ->
                file.isFile && LocalModelClassifier.isImageGenModel(file) && file.length() > 10 * 1024 * 1024
            }
        }

        // Strictly require local model file: NO FAKE CANVAS FALLBACK
        if (targetModelFile == null || !targetModelFile.exists()) {
            val errMsg = "Tidak ditemukan file model Imagen (MNN / ONNX / FLUX / SD) lokal. Pastikan model telah diunduh di Model Hub dan dipilih di pengaturan!"
            Log.e(TAG, errMsg)
            _progressFlow.value = ImageGenProgress(
                isGenerating = false,
                error = errMsg
            )
            return@withContext Result.failure(IllegalStateException(errMsg))
        }

        val resolvedModelName = targetModelFile.name

        try {
            _progressFlow.value = ImageGenProgress(
                isGenerating = true,
                progressPercent = 5,
                statusText = "MNN / ONNX: Memuat model ${resolvedModelName}...",
                prompt = cleanPrompt,
                modelName = resolvedModelName
            )

            val imagesDir = File(context.filesDir, "generated_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val outputFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")

            val genResult = MnnOnnxDiffusionEngine.generate(
                context = context,
                modelFile = targetModelFile,
                prompt = cleanPrompt,
                outputFile = outputFile,
                onProgress = { percent, status ->
                    _progressFlow.value = ImageGenProgress(
                        isGenerating = true,
                        progressPercent = percent,
                        statusText = status,
                        prompt = cleanPrompt,
                        modelName = resolvedModelName
                    )
                }
            )

            if (genResult.isSuccess) {
                val finalPath = genResult.getOrThrow()
                lastGeneratedImageUri = finalPath
                _progressFlow.value = ImageGenProgress(
                    isGenerating = false,
                    progressPercent = 100,
                    statusText = "Selesai 100%",
                    prompt = cleanPrompt,
                    modelName = resolvedModelName,
                    generatedImageUri = finalPath
                )
                Result.success(finalPath)
            } else {
                val err = genResult.exceptionOrNull()?.localizedMessage ?: "Inference MNN / ONNX gagal"
                _progressFlow.value = ImageGenProgress(isGenerating = false, error = err)
                Result.failure(genResult.exceptionOrNull() ?: Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed on model $resolvedModelName", e)
            _progressFlow.value = ImageGenProgress(
                isGenerating = false,
                error = e.localizedMessage ?: "Gagal menjalankan model"
            )
            Result.failure(e)
        }
    }
}
