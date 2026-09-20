package com.ryzumi.miraiai.domain.engine

import android.content.Context
import android.util.Base64
import android.util.Log
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.domain.model.LocalModelClassifier
import com.ryzumi.miraiai.domain.model.LocalModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

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
 * Image Generation Manager supporting both Local MNN/ONNX diffusion models AND Endpoint API (DALL-E, SD, FLUX API).
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
        modelName: String = "Stable Diffusion 3.5",
        config: InferenceConfigEntity? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Prompt tidak boleh kosong"))
        }

        val isApi = config?.imageGenEngine?.equals("api", ignoreCase = true) == true

        if (isApi && config != null) {
            return@withContext generateImageViaApi(context, cleanPrompt, config)
        }

        // Local Image Generation logic (MNN / ONNX / FLUX)
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

        if (targetModelFile == null || !targetModelFile.exists()) {
            val errMsg = "Tidak ditemukan file model Imagen (MNN / ONNX / FLUX / SD) lokal. Pastikan model telah diunduh di Model Hub atau ubah mode Imagen ke Endpoint API!"
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

    private suspend fun generateImageViaApi(
        context: Context,
        prompt: String,
        config: InferenceConfigEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = config.baseUrl.trim().trimEnd('/')
        val endpoint = if (config.imageGenApiEndpoint.isNotBlank()) {
            config.imageGenApiEndpoint.trim()
        } else {
            "$baseUrl/images/generations"
        }

        val apiKey = if (config.imageGenApiKey.isNotBlank()) {
            config.imageGenApiKey.trim()
        } else {
            config.apiKey.trim()
        }

        val modelId = if (config.imageGenApiModel.isNotBlank()) {
            config.imageGenApiModel.trim()
        } else {
            "dall-e-3"
        }

        val size = config.imageGenSize.ifBlank { "auto" }
        val sizeParam = if (size.equals("auto", ignoreCase = true)) "1024x1024" else size
        val sizeParts = sizeParam.split('x', 'X')
        val widthInt = sizeParts.firstOrNull()?.toIntOrNull() ?: 1024
        val heightInt = sizeParts.getOrNull(1)?.toIntOrNull() ?: 1024

        _progressFlow.value = ImageGenProgress(
            isGenerating = true,
            progressPercent = 15,
            statusText = "API: Menghubungi $modelId ($endpoint)...",
            prompt = prompt,
            modelName = modelId
        )

        try {
            val jsonBody = JSONObject().apply {
                put("model", modelId)
                put("prompt", prompt)
                put("n", 1)
                put("size", sizeParam)
                put("width", widthInt)
                put("height", heightInt)
                put("response_format", "b64_json")
                if (config.imageGenSteps > 0) {
                    put("steps", config.imageGenSteps)
                    put("num_inference_steps", config.imageGenSteps)
                }
                if (config.imageGenGuidanceScale > 0f) {
                    put("guidance_scale", config.imageGenGuidanceScale.toDouble())
                    put("cfg_scale", config.imageGenGuidanceScale.toDouble())
                }
                if (config.imageGenNegativePrompt.isNotBlank()) {
                    put("negative_prompt", config.imageGenNegativePrompt)
                }
            }

            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (apiKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $apiKey")
                reqBuilder.header("api-key", apiKey)
            }

            if (config.customHeaders.isNotBlank()) {
                try {
                    val headersObj = JSONObject(config.customHeaders)
                    headersObj.keys().forEach { k ->
                        reqBuilder.header(k, headersObj.getString(k))
                    }
                } catch (_: Exception) {}
            }

            _progressFlow.value = ImageGenProgress(
                isGenerating = true,
                progressPercent = 40,
                statusText = "API: Menunggu respon generasi gambar...",
                prompt = prompt,
                modelName = modelId
            )

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val response = client.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) {
                val errBody = try { response.body?.string()?.take(300) } catch (_: Exception) { "" }
                val err = "Image Gen API HTTP ${response.code}${if (!errBody.isNullOrBlank()) ": $errBody" else ""}"
                _progressFlow.value = ImageGenProgress(isGenerating = false, error = err)
                return@withContext Result.failure(Exception(err))
            }

            val contentType = response.header("Content-Type") ?: ""
            val imagesDir = File(context.filesDir, "generated_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val outputFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")

            if (contentType.startsWith("image/", ignoreCase = true)) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    outputFile.writeBytes(bytes)
                } else {
                    val err = "API mengembalikan respon gambar kosong"
                    _progressFlow.value = ImageGenProgress(isGenerating = false, error = err)
                    return@withContext Result.failure(Exception(err))
                }
            } else {
                val respStr = response.body?.string() ?: ""
                val json = JSONObject(respStr)

                var b64Data: String? = null
                var imageUrl: String? = null

                if (json.has("data")) {
                    val arr = json.getJSONArray("data")
                    if (arr.length() > 0) {
                        val first = arr.getJSONObject(0)
                        if (first.has("b64_json")) b64Data = first.getString("b64_json")
                        else if (first.has("url")) imageUrl = first.getString("url")
                        else if (first.has("image")) b64Data = first.getString("image")
                    }
                } else if (json.has("images")) {
                    val arr = json.getJSONArray("images")
                    if (arr.length() > 0) {
                        b64Data = arr.getString(0)
                    }
                } else if (json.has("b64_json")) {
                    b64Data = json.getString("b64_json")
                } else if (json.has("url")) {
                    imageUrl = json.getString("url")
                }

                if (!b64Data.isNullOrBlank()) {
                    val cleanB64 = b64Data.substringAfter("base64,")
                    val decoded = Base64.decode(cleanB64, Base64.DEFAULT)
                    outputFile.writeBytes(decoded)
                } else if (!imageUrl.isNullOrBlank()) {
                    val imgReq = Request.Builder().url(imageUrl).build()
                    val imgResp = client.newCall(imgReq).execute()
                    if (imgResp.isSuccessful && imgResp.body != null) {
                        outputFile.writeBytes(imgResp.body!!.bytes())
                    } else {
                        val err = "Gagal mengunduh gambar dari URL API: $imageUrl"
                        _progressFlow.value = ImageGenProgress(isGenerating = false, error = err)
                        return@withContext Result.failure(Exception(err))
                    }
                } else {
                    val err = "API tidak mengembalikan b64_json atau url gambar"
                    _progressFlow.value = ImageGenProgress(isGenerating = false, error = err)
                    return@withContext Result.failure(Exception(err))
                }
            }

            val finalPath = outputFile.absolutePath
            lastGeneratedImageUri = finalPath
            _progressFlow.value = ImageGenProgress(
                isGenerating = false,
                progressPercent = 100,
                statusText = "Selesai 100%",
                prompt = prompt,
                modelName = modelId,
                generatedImageUri = finalPath
            )
            Result.success(finalPath)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuat gambar via API", e)
            val err = "Gagal generasi gambar API: ${e.localizedMessage ?: "Unknown error"}"
            _progressFlow.value = ImageGenProgress(isGenerating = false, error = err)
            Result.failure(e)
        }
    }
}
