package com.ryzumi.miraiai.data.network

import android.app.ActivityManager
import android.content.Context
import com.google.gson.JsonParser
import com.ryzumi.miraiai.domain.model.HuggingFaceModel
import com.ryzumi.miraiai.domain.model.ModelCompatibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class HuggingFacePageResult(
    val models: List<HuggingFaceModel>,
    val nextPageUrl: String?
)

class HuggingFaceRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getSystemRamInfo(): Pair<Double, Double> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalGb = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val availGb = memInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            Pair(totalGb, availGb)
        } catch (e: Exception) {
            Pair(6.0, 3.0)
        }
    }

    suspend fun searchModels(query: String, pageUrl: String? = null): Result<HuggingFacePageResult> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim()
            val url = pageUrl ?: if (cleanQuery.isEmpty()) {
                "https://huggingface.co/api/models?filter=gguf&sort=downloads&direction=-1&limit=40&full=true"
            } else {
                "https://huggingface.co/api/models?search=$cleanQuery&sort=downloads&direction=-1&limit=40&full=true"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val (systemTotalRamGb, _) = getSystemRamInfo()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${response.message}")
                    )
                }

                val linkHeader = response.header("Link")
                var nextPageUrl: String? = null
                if (linkHeader != null) {
                    val matcher = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"").matcher(linkHeader)
                    if (matcher.find()) {
                        nextPageUrl = matcher.group(1)
                    }
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty body"))

                val jsonArray = JsonParser.parseString(bodyString).asJsonArray
                val resultList = mutableListOf<HuggingFaceModel>()
                val localModelsDir = File(context.filesDir, "models")

                for (element in jsonArray) {
                    if (element.isJsonObject) {
                        val obj = element.asJsonObject
                        val id = obj.get("id")?.asString ?: continue
                        val downloads = obj.get("downloads")?.asInt ?: 0
                        val likes = obj.get("likes")?.asInt ?: 0
                        val pipelineTag = obj.get("pipeline_tag")?.asString

                        val parts = id.split("/")
                        val author = if (parts.size > 1) parts[0] else "HuggingFace"
                        val modelName = if (parts.size > 1) parts[1] else id

                        val tagsList = mutableListOf<String>()
                        obj.getAsJsonArray("tags")?.let { tagsArray ->
                            for (tag in tagsArray) {
                                if (tag.isJsonPrimitive) {
                                    tagsList.add(tag.asString)
                                }
                            }
                        }

                        // Collect all valid GGUF files in the repository siblings
                        val ggufFiles = mutableListOf<Pair<String, Long>>()
                        obj.getAsJsonArray("siblings")?.let { siblings ->
                            for (sib in siblings) {
                                if (sib.isJsonObject) {
                                    val sibObj = sib.asJsonObject
                                    val rfilename = sibObj.get("rfilename")?.asString ?: ""
                                    val sizeBytes = sibObj.get("size")?.asLong ?: 0L
                                    if (rfilename.endsWith(".gguf", ignoreCase = true) && !rfilename.contains("imatrix", ignoreCase = true)) {
                                        ggufFiles.add(Pair(rfilename, sizeBytes))
                                    }
                                }
                            }
                        }

                        // STRICT FILTER: Only include if the repo contains runnable GGUF files or is a known GGUF repo
                        val isGgufRepo = ggufFiles.isNotEmpty() || tagsList.any { it.equals("gguf", ignoreCase = true) } || id.contains("gguf", ignoreCase = true)
                        if (!isGgufRepo) {
                            continue
                        }

                        // Pick the best default quantization file (Priority: Q4_K_M > Q4_K_S > Q4_0 > Q5_K_M > Q8_0 > first .gguf)
                        val bestGgufPair = ggufFiles.firstOrNull { it.first.contains("q4_k_m", ignoreCase = true) }
                            ?: ggufFiles.firstOrNull { it.first.contains("q4_k_s", ignoreCase = true) || it.first.contains("q4_0", ignoreCase = true) }
                            ?: ggufFiles.firstOrNull { it.first.contains("q5_k_m", ignoreCase = true) || it.first.contains("q5_0", ignoreCase = true) }
                            ?: ggufFiles.firstOrNull { it.first.contains("q8_0", ignoreCase = true) }
                            ?: ggufFiles.firstOrNull()

                        val selectedFileName = bestGgufPair?.first ?: "model.gguf"
                        val directSizeBytes = bestGgufPair?.second ?: 0L

                        val downloadUrl = "https://huggingface.co/$id/resolve/main/$selectedFileName"

                        // 1. Detect Vision Capability
                        val hasVision = (pipelineTag in listOf("image-to-text", "visual-question-answering", "document-question-answering")) ||
                                tagsList.any { t ->
                                    t.contains("vision", ignoreCase = true) ||
                                            t.contains("multimodal", ignoreCase = true) ||
                                            t.contains("vlm", ignoreCase = true) ||
                                            t.contains("llava", ignoreCase = true)
                                } ||
                                id.contains("vl", ignoreCase = true) ||
                                id.contains("vision", ignoreCase = true) ||
                                id.contains("llava", ignoreCase = true)

                        // 2. Detect Image Gen Capability
                        val hasImageGen = (pipelineTag in listOf("text-to-image", "image-to-image")) ||
                                tagsList.any { t ->
                                    t.contains("diffusion", ignoreCase = true) ||
                                            t.contains("diffusers", ignoreCase = true) ||
                                            t.contains("text-to-image", ignoreCase = true) ||
                                            t.contains("flux", ignoreCase = true) ||
                                            t.contains("stable-diffusion", ignoreCase = true)
                                } ||
                                id.contains("diffusion", ignoreCase = true) ||
                                id.contains("flux", ignoreCase = true) ||
                                id.contains("sdxl", ignoreCase = true)

                        // 3. Size calculation
                        val estimatedSizeGb = if (directSizeBytes > 0L) {
                            directSizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                        } else {
                            val lowerName = "$id $modelName ${tagsList.joinToString(" ")}".lowercase(Locale.ROOT)
                            val pattern = Pattern.compile("(\\d+(\\.\\d+)?)[bB]")
                            val matcher = pattern.matcher(lowerName)
                            var paramB = 0.0
                            while (matcher.find()) {
                                val num = matcher.group(1)?.toDoubleOrNull() ?: 0.0
                                if (num in 0.1..150.0 && num > paramB) {
                                    paramB = num
                                }
                            }

                            when {
                                paramB in 0.1..0.8 -> 0.6
                                paramB in 0.8..1.8 -> 1.2
                                paramB in 1.8..3.8 -> 2.4
                                paramB in 3.8..6.5 -> 3.8
                                paramB in 6.5..8.5 -> 4.9
                                paramB in 8.5..12.0 -> 7.2
                                paramB in 12.0..16.0 -> 9.5
                                paramB in 16.0..34.0 -> 19.5
                                paramB > 34.0 -> 42.0
                                hasImageGen -> 3.5
                                hasVision -> 3.2
                                else -> 2.2
                            }
                        }

                        val formattedSize = if (estimatedSizeGb >= 1.0) {
                            String.format(Locale.US, "%.1f GB", estimatedSizeGb)
                        } else {
                            String.format(Locale.US, "%.0f MB", estimatedSizeGb * 1024.0)
                        }

                        // 4. Calculate RAM Requirement & Device Compatibility
                        val requiredRamGb = (estimatedSizeGb * 1.25) + 0.8
                        val compatibility = when {
                            systemTotalRamGb >= requiredRamGb + 1.5 -> ModelCompatibility.OPTIMAL
                            systemTotalRamGb >= requiredRamGb -> ModelCompatibility.MODERATE
                            else -> ModelCompatibility.LOW_MEMORY
                        }

                        val safeName = "${id.replace("/", "_")}_${selectedFileName}"
                        val targetFile = File(localModelsDir, safeName)
                        val isDownloaded = targetFile.exists() && targetFile.length() > 0

                        resultList.add(
                            HuggingFaceModel(
                                id = id,
                                modelName = modelName,
                                author = author,
                                downloads = downloads,
                                likes = likes,
                                tags = tagsList,
                                pipelineTag = pipelineTag,
                                isDownloaded = isDownloaded,
                                localFilePath = if (isDownloaded) targetFile.absolutePath else null,
                                estimatedSizeGb = estimatedSizeGb,
                                formattedSize = formattedSize,
                                hasVisionCapability = hasVision,
                                hasImageGenCapability = hasImageGen,
                                requiredRamGb = requiredRamGb,
                                compatibility = compatibility,
                                downloadUrl = downloadUrl,
                                selectedFileName = selectedFileName
                            )
                        )
                    }
                }

                Result.success(HuggingFacePageResult(resultList, nextPageUrl))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDownloadedModels(): List<File> {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return emptyList()
        return modelsDir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()
    }
}
