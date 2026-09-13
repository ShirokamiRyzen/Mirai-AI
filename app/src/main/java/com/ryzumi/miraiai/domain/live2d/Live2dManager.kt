package com.ryzumi.miraiai.domain.live2d

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class Live2dImportResult {
    data class Success(
        val relativeModelPath: String,
        val modelName: String,
        val cubismVersion: String,
        val textureCount: Int
    ) : Live2dImportResult()

    data class Error(val message: String) : Live2dImportResult()
}

object Live2dManager {

    private val gson = Gson()

    fun getCharacterLive2dDir(context: Context, characterId: String): File {
        return File(context.filesDir, "live2d/$characterId")
    }

    fun isModelValid(context: Context, characterId: String, live2dPath: String?): Boolean {
        if (live2dPath.isNullOrBlank()) return false
        val baseDir = getCharacterLive2dDir(context, characterId)
        val modelFile = File(baseDir, live2dPath)
        return modelFile.exists() && modelFile.isFile
    }

    fun deleteCharacterLive2d(context: Context, characterId: String) {
        val baseDir = getCharacterLive2dDir(context, characterId)
        if (baseDir.exists()) {
            baseDir.deleteRecursively()
        }
    }

    suspend fun validateAndImport(
        context: Context,
        characterId: String,
        zipUri: Uri
    ): Live2dImportResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "live2d_temp_${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            val tempZipFile = File(tempDir, "model.zip")

            // 1. Copy uri to temp file
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Live2dImportResult.Error("Failed to read file from storage.")

            if (tempZipFile.length() < 10) {
                return@withContext Live2dImportResult.Error("Archive file is empty or corrupted.")
            }

            // 2. Read zip entries and detect model file
            val entryNames = mutableSetOf<String>()
            var modelEntryName: String? = null
            var isCubism3or4 = false

            ZipInputStream(FileInputStream(tempZipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val normalizedName = entry.name.replace('\\', '/').trimStart('/')
                    if (!entry.isDirectory && normalizedName.isNotBlank()) {
                        entryNames.add(normalizedName)
                        if (normalizedName.endsWith(".model3.json", ignoreCase = true)) {
                            if (modelEntryName == null || !isCubism3or4) {
                                modelEntryName = normalizedName
                                isCubism3or4 = true
                            }
                        } else if (normalizedName.endsWith(".model.json", ignoreCase = true)) {
                            if (modelEntryName == null) {
                                modelEntryName = normalizedName
                                isCubism3or4 = false
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (modelEntryName == null) {
                return@withContext Live2dImportResult.Error(
                    "The ZIP archive is not a valid Live2D model. Configuration file .model3.json (Cubism 3/4/5) or .model.json (Cubism 2) was not found."
                )
            }

            // 3. Read and parse model json configuration
            var jsonContent: String? = null
            ZipInputStream(FileInputStream(tempZipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val normalizedName = entry.name.replace('\\', '/').trimStart('/')
                    if (normalizedName.equals(modelEntryName, ignoreCase = true)) {
                        jsonContent = zis.bufferedReader().use { it.readText() }
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (jsonContent.isNullOrBlank()) {
                return@withContext Live2dImportResult.Error("File $modelEntryName is empty or unreadable.")
            }

            val jsonObject = try {
                gson.fromJson(jsonContent, JsonObject::class.java)
            } catch (e: Exception) {
                return@withContext Live2dImportResult.Error("Invalid JSON in $modelEntryName: ${e.localizedMessage}")
            }

            val parentDir = if (modelEntryName.contains('/')) {
                modelEntryName.substringBeforeLast('/') + "/"
            } else {
                ""
            }

            var cubismVersion = "Live2D Cubism"
            var textureCount = 0

            // 4. Validate specific Cubism fields and dependencies
            if (isCubism3or4) {
                cubismVersion = "Cubism 3/4/5"
                val fileReferences = jsonObject.getAsJsonObject("FileReferences")
                    ?: return@withContext Live2dImportResult.Error(
                        "File $modelEntryName does not have a valid 'FileReferences' structure for Live2D Cubism 3/4/5."
                    )

                val mocRelativePath = fileReferences.get("Moc")?.asString
                if (mocRelativePath.isNullOrBlank()) {
                    return@withContext Live2dImportResult.Error("File $modelEntryName does not specify a Moc binary model file (.moc3).")
                }

                val fullMocPath = (parentDir + mocRelativePath).replace('\\', '/').trimStart('/')
                val mocExists = entryNames.any { it.equals(fullMocPath, ignoreCase = true) }
                if (!mocExists) {
                    return@withContext Live2dImportResult.Error(
                        "Moc binary model file ($mocRelativePath) was not found in the ZIP archive."
                    )
                }

                val texturesArray = fileReferences.getAsJsonArray("Textures")
                if (texturesArray != null && texturesArray.size() > 0) {
                    textureCount = texturesArray.size()
                    val firstTexture = texturesArray.get(0).asString
                    val fullTexturePath = (parentDir + firstTexture).replace('\\', '/').trimStart('/')
                    val textureExists = entryNames.any { it.equals(fullTexturePath, ignoreCase = true) }
                    if (!textureExists) {
                        return@withContext Live2dImportResult.Error(
                            "Texture file ($firstTexture) was not found in the ZIP archive."
                        )
                    }
                }
            } else {
                cubismVersion = "Cubism 2.1"
                val mocRelativePath = jsonObject.get("model")?.asString
                if (mocRelativePath.isNullOrBlank()) {
                    return@withContext Live2dImportResult.Error("File $modelEntryName does not specify a .moc model file.")
                }

                val fullMocPath = (parentDir + mocRelativePath).replace('\\', '/').trimStart('/')
                val mocExists = entryNames.any { it.equals(fullMocPath, ignoreCase = true) }
                if (!mocExists) {
                    return@withContext Live2dImportResult.Error(
                        "Moc binary model file ($mocRelativePath) was not found in the ZIP archive."
                    )
                }

                val texturesArray = jsonObject.getAsJsonArray("textures")
                if (texturesArray != null && texturesArray.size() > 0) {
                    textureCount = texturesArray.size()
                }
            }

            // 5. Validation passed! Extract to target character directory
            val targetDir = getCharacterLive2dDir(context, characterId)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            ZipInputStream(FileInputStream(tempZipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val normalizedName = entry.name.replace('\\', '/').trimStart('/')
                    if (normalizedName.isNotBlank()) {
                        val outFile = File(targetDir, normalizedName)
                        // Zip Slip validation
                        if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                            throw SecurityException("Zip entry is trying to escape target directory: $normalizedName")
                        }

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }

                            val ext = outFile.extension.lowercase()
                            if (ext in listOf("png", "jpg", "jpeg", "webp")) {
                                optimizeImageFile(outFile)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val modelName = modelEntryName.substringAfterLast('/').substringBefore(".model")

            Live2dImportResult.Success(
                relativeModelPath = modelEntryName,
                modelName = modelName,
                cubismVersion = cubismVersion,
                textureCount = textureCount
            )
        } catch (e: Exception) {
            Live2dImportResult.Error("An error occurred while processing the Live2D archive: ${e.localizedMessage}")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun optimizeImageFile(file: File) {
        try {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
            val maxDim = maxOf(options.outWidth, options.outHeight)
            if (maxDim <= 2048) return

            var inSampleSize = 1
            while ((maxDim / (inSampleSize * 2)) >= 2048) {
                inSampleSize *= 2
            }
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decoded = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return

            val targetMax = 2048f
            val currentMax = maxOf(decoded.width, decoded.height).toFloat()
            val finalBitmap = if (currentMax > targetMax) {
                val ratio = targetMax / currentMax
                val scaled = android.graphics.Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
                if (scaled != decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }

            FileOutputStream(file).use { fos ->
                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, fos)
            }
            finalBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAvailableModelFiles(
        context: Context,
        characterId: String,
        currentModelPath: String? = null
    ): List<Live2dModelFile> {
        val baseDir = getCharacterLive2dDir(context, characterId)
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()

        val results = mutableListOf<Live2dModelFile>()
        baseDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val name = file.name
                val isModel = name.endsWith(".model3.json", ignoreCase = true) ||
                        name.endsWith(".model.json", ignoreCase = true)
                if (isModel) {
                    val relPath = file.relativeTo(baseDir).path.replace('\\', '/')
                    val cleanName = name
                        .removeSuffix(".model3.json")
                        .removeSuffix(".model.json")
                    val parentName = file.parentFile?.name
                    val displayName = if (parentName != null && parentName != characterId && !parentName.equals("live2d", ignoreCase = true)) {
                        "$parentName ($cleanName)"
                    } else {
                        cleanName
                    }
                    val isDefault = currentModelPath != null && relPath.equals(currentModelPath, ignoreCase = true)
                    results.add(
                        Live2dModelFile(
                            name = displayName,
                            relativePath = relPath,
                            isDefault = isDefault
                        )
                    )
                }
            }
        }
        return results
    }

    fun getDisplayInfoNames(context: Context, characterId: String, modelRelPath: String): Pair<Map<String, String>, Map<String, String>> {
        val baseDir = getCharacterLive2dDir(context, characterId)
        val modelFile = File(baseDir, modelRelPath)
        if (!modelFile.exists()) return Pair(emptyMap(), emptyMap())

        val paramNames = mutableMapOf<String, String>()
        val partNames = mutableMapOf<String, String>()

        try {
            val modelContent = modelFile.readText()
            val json = gson.fromJson(modelContent, JsonObject::class.java)
            val fileReferences = json.getAsJsonObject("FileReferences")
            val cdiRel = fileReferences?.get("DisplayInfo")?.asString

            val cdiFile = if (!cdiRel.isNullOrBlank()) {
                File(modelFile.parentFile, cdiRel)
            } else {
                modelFile.parentFile?.listFiles()?.firstOrNull { it.name.endsWith(".cdi3.json", ignoreCase = true) }
            }

            if (cdiFile != null && cdiFile.exists()) {
                val cdiJson = gson.fromJson(cdiFile.readText(), JsonObject::class.java)
                cdiJson.getAsJsonArray("Parameters")?.forEach { elem ->
                    val obj = elem.asJsonObject
                    val id = obj.get("Id")?.asString
                    val name = obj.get("Name")?.asString
                    if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                        paramNames[id] = name
                    }
                }
                cdiJson.getAsJsonArray("Parts")?.forEach { elem ->
                    val obj = elem.asJsonObject
                    val id = obj.get("Id")?.asString
                    val name = obj.get("Name")?.asString
                    if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                        partNames[id] = name
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MiraiLive2D", "Failed to parse DisplayInfo: ${e.message}")
        }

        return Pair(partNames, paramNames)
    }
}

/**
 * Strips Live2D control tags such as [motion:dance], [expression:happy], etc., from user-facing text.
 * When isStreaming is true, it also suppresses incomplete opening tags at the tail of the stream.
 */
fun cleanLive2dControlTags(text: String, isStreaming: Boolean = false): String {
    if (text.isBlank()) return text
    var result = text.replace(Regex("""\[(?:motion|expression|emotion)[^\]]*\]|<(?:motion|expression|emotion)[^>]*>""", RegexOption.IGNORE_CASE), "")
    if (isStreaming) {
        result = result.replace(Regex("""\[(?:motion|expression|emotion)[^\]]*$|<(?:motion|expression|emotion)[^>]*$""", RegexOption.IGNORE_CASE), "")
    }
    return result.replace(Regex("""[ \t]{2,}"""), " ").trimStart()
}

