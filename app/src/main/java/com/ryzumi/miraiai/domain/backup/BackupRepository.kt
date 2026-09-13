package com.ryzumi.miraiai.domain.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.data.local.MiraiDatabase
import com.ryzumi.miraiai.domain.model.BackupStats
import com.ryzumi.miraiai.domain.model.MiraiBackupData
import com.ryzumi.miraiai.domain.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val context: Context,
    private val database: MiraiDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun cleanupOrphanAssets(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        try {
            val allMessages = database.chatMessageDao().getAllMessagesSync()
            val validChatImagePaths = allMessages.mapNotNull { it.imageUri }
                .map { if (it.startsWith("file://")) Uri.parse(it).path ?: it else it }
                .toSet()

            val chatImagesDir = File(context.filesDir, "chat_images")
            if (chatImagesDir.exists() && chatImagesDir.isDirectory) {
                chatImagesDir.listFiles()?.forEach { file ->
                    if (file.isFile && !validChatImagePaths.contains(file.absolutePath)) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }

            val allCharacters = database.characterDao().getAllCharactersSync()
            val allPersonas = database.userPersonaDao().getAllPersonasSync()
            val validAvatarPaths = (allCharacters.mapNotNull { it.avatarUri } + allPersonas.mapNotNull { it.avatarUri })
                .map { if (it.startsWith("file://")) Uri.parse(it).path ?: it else it }
                .toSet()

            val avatarsDir = File(context.filesDir, "avatars")
            if (avatarsDir.exists() && avatarsDir.isDirectory) {
                avatarsDir.listFiles()?.forEach { file ->
                    if (file.isFile && !validAvatarPaths.contains(file.absolutePath)) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }

            val validCharacterIds = allCharacters.map { it.id }.toSet()
            val live2dDir = File(context.filesDir, "live2d")
            if (live2dDir.exists() && live2dDir.isDirectory) {
                live2dDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory && !validCharacterIds.contains(dir.name)) {
                        if (dir.deleteRecursively()) {
                            deletedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        deletedCount
    }

    suspend fun getBackupStats(): BackupStats = withContext(Dispatchers.IO) {
        cleanupOrphanAssets()

        val chars = database.characterDao().getAllCharactersSync().size
        val personas = database.userPersonaDao().getAllPersonasSync().size
        val sessions = database.chatSessionDao().getAllSessionsSync().size
        val messages = database.chatMessageDao().getAllMessagesSync().size
        val configs = database.inferenceConfigDao().getAllConfigsSync().size

        val avatarsDir = File(context.filesDir, "avatars")
        val avatarFiles = avatarsDir.listFiles()?.filter { it.isFile } ?: emptyList()

        val chatImagesDir = File(context.filesDir, "chat_images")
        val chatImageFiles = chatImagesDir.listFiles()?.filter { it.isFile } ?: emptyList()

        val live2dDir = File(context.filesDir, "live2d")
        val live2dFiles = if (live2dDir.exists() && live2dDir.isDirectory) {
            live2dDir.walkTopDown().filter { it.isFile }.toList()
        } else emptyList()
        val live2dModelCount = if (live2dDir.exists() && live2dDir.isDirectory) {
            live2dDir.listFiles()?.filter { it.isDirectory }?.size ?: 0
        } else 0

        val assetCount = avatarFiles.size + chatImageFiles.size + live2dFiles.size
        val assetSizeBytes = avatarFiles.sumOf { it.length() } + chatImageFiles.sumOf { it.length() } + live2dFiles.sumOf { it.length() }

        val dbFile = context.getDatabasePath("mirai_database")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        val dbSizeBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
                (if (walFile.exists()) walFile.length() else 0L) +
                (if (shmFile.exists()) shmFile.length() else 0L)

        val totalSizeBytes = assetSizeBytes + dbSizeBytes
        val formattedSize = formatFileSize(totalSizeBytes)

        BackupStats(
            characterCount = chars,
            personaCount = personas,
            sessionCount = sessions,
            messageCount = messages,
            configCount = configs,
            assetCount = assetCount,
            live2dModelCount = live2dModelCount,
            totalSizeBytes = totalSizeBytes,
            formattedDataSize = formattedSize
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return "%.1f %s".format(java.util.Locale.US, value, units[digitGroups])
    }

    private fun zipDirectoryToBytes(directory: File): ByteArray {
        if (!directory.exists() || !directory.isDirectory) return ByteArray(0)
        val baos = java.io.ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relPath = file.relativeTo(directory).path.replace('\\', '/')
                    val entry = ZipEntry(relPath)
                    zos.putNextEntry(entry)
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            zos.finish()
        }
        return baos.toByteArray()
    }

    private fun restoreLive2dFromZipBytes(charId: String, zipBytes: ByteArray) {
        if (zipBytes.isEmpty()) return
        val targetCharDir = File(context.filesDir, "live2d/$charId")
        targetCharDir.mkdirs()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zIn ->
            var entry = zIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.isNotBlank()) {
                    val file = File(targetCharDir, entry.name)
                    if (file.canonicalPath.startsWith(targetCharDir.canonicalPath)) {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { out ->
                            zIn.copyTo(out)
                        }
                    }
                }
                zIn.closeEntry()
                entry = zIn.nextEntry
            }
        }
    }

    suspend fun exportBackupToJson(): String = withContext(Dispatchers.IO) {
        val characters = database.characterDao().getAllCharactersSync()
        val personas = database.userPersonaDao().getAllPersonasSync()
        val sessions = database.chatSessionDao().getAllSessionsSync()
        val messages = database.chatMessageDao().getAllMessagesSync()
        val configs = database.inferenceConfigDao().getAllConfigsSync()
        val theme = settingsRepository.themeSettingsFlow.first()
        val showThinking = settingsRepository.showThinkingProcessFlow.first()
        val debugLogging = settingsRepository.debugLoggingEnabledFlow.first()
        val tokenCounter = settingsRepository.tokenCounterEnabledFlow.first()
        val allowDevice = settingsRepository.allowDeviceContextFlow.first()
        val uploadB64 = settingsRepository.uploadAsBase64Flow.first()

        val charBase64Map = mutableMapOf<String, String>()
        for (char in characters) {
            val uriStr = char.avatarUri
            if (!uriStr.isNullOrBlank()) {
                val rawBytes = ImageUtils.getImageBytesForUpload(context, uriStr)
                val webp = rawBytes?.let { ImageUtils.toWebpBytes(it, 720, 85) }
                if (webp != null && webp.isNotEmpty()) {
                    charBase64Map[char.id] = ImageUtils.safeBase64Encode(webp)
                }
            }
        }

        val personaBase64Map = mutableMapOf<String, String>()
        for (persona in personas) {
            val uriStr = persona.avatarUri
            if (!uriStr.isNullOrBlank()) {
                val rawBytes = ImageUtils.getImageBytesForUpload(context, uriStr)
                val webp = rawBytes?.let { ImageUtils.toWebpBytes(it, 720, 85) }
                if (webp != null && webp.isNotEmpty()) {
                    personaBase64Map[persona.id] = ImageUtils.safeBase64Encode(webp)
                }
            }
        }

        val chatImageBase64Map = mutableMapOf<String, String>()
        for (msg in messages) {
            val uriStr = msg.imageUri
            if (!uriStr.isNullOrBlank()) {
                val rawBytes = ImageUtils.getImageBytesForUpload(context, uriStr)
                val webp = rawBytes?.let { ImageUtils.toWebpBytes(it, 1024, 85) }
                if (webp != null && webp.isNotEmpty()) {
                    chatImageBase64Map[msg.id] = ImageUtils.safeBase64Encode(webp)
                }
            }
        }

        val live2dBase64Map = mutableMapOf<String, String>()
        val live2dBaseDir = File(context.filesDir, "live2d")
        if (live2dBaseDir.exists() && live2dBaseDir.isDirectory) {
            for (char in characters) {
                val charDir = File(live2dBaseDir, char.id)
                if (charDir.exists() && charDir.isDirectory) {
                    val zipBytes = zipDirectoryToBytes(charDir)
                    if (zipBytes.isNotEmpty()) {
                        live2dBase64Map[char.id] = ImageUtils.safeBase64Encode(zipBytes)
                    }
                }
            }
        }

        val backup = MiraiBackupData(
            version = 3,
            appName = "MiraiAI",
            exportedAt = System.currentTimeMillis(),
            characters = characters,
            personas = personas,
            sessions = sessions,
            messages = messages,
            configs = configs,
            themeSettings = theme,
            showThinkingProcess = showThinking,
            debugLoggingEnabled = debugLogging,
            tokenCounterEnabled = tokenCounter,
            allowDeviceContext = allowDevice,
            uploadAsBase64 = uploadB64,
            characterAvatars = charBase64Map.ifEmpty { null },
            personaAvatars = personaBase64Map.ifEmpty { null },
            messageImages = chatImageBase64Map.ifEmpty { null },
            live2dModels = live2dBase64Map.ifEmpty { null }
        )
        gson.toJson(backup)
    }

    suspend fun writeBackupToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val characters = database.characterDao().getAllCharactersSync()
            val personas = database.userPersonaDao().getAllPersonasSync()
            val sessions = database.chatSessionDao().getAllSessionsSync()
            val messages = database.chatMessageDao().getAllMessagesSync()
            val configs = database.inferenceConfigDao().getAllConfigsSync()
            val theme = settingsRepository.themeSettingsFlow.first()
            val showThinking = settingsRepository.showThinkingProcessFlow.first()
            val debugLogging = settingsRepository.debugLoggingEnabledFlow.first()
            val tokenCounter = settingsRepository.tokenCounterEnabledFlow.first()
            val allowDevice = settingsRepository.allowDeviceContextFlow.first()
            val uploadB64 = settingsRepository.uploadAsBase64Flow.first()

            val charWebpMap = mutableMapOf<String, ByteArray>()
            val charBase64Map = mutableMapOf<String, String>()
            for (char in characters) {
                val uriStr = char.avatarUri
                if (!uriStr.isNullOrBlank()) {
                    val rawBytes = ImageUtils.getImageBytesForUpload(context, uriStr)
                    val webp = rawBytes?.let { ImageUtils.toWebpBytes(it, 720, 85) }
                    if (webp != null && webp.isNotEmpty()) {
                        charWebpMap[char.id] = webp
                        charBase64Map[char.id] = ImageUtils.safeBase64Encode(webp)
                    }
                }
            }

            val personaWebpMap = mutableMapOf<String, ByteArray>()
            val personaBase64Map = mutableMapOf<String, String>()
            for (persona in personas) {
                val uriStr = persona.avatarUri
                if (!uriStr.isNullOrBlank()) {
                    val rawBytes = ImageUtils.getImageBytesForUpload(context, uriStr)
                    val webp = rawBytes?.let { ImageUtils.toWebpBytes(it, 720, 85) }
                    if (webp != null && webp.isNotEmpty()) {
                        personaWebpMap[persona.id] = webp
                        personaBase64Map[persona.id] = ImageUtils.safeBase64Encode(webp)
                    }
                }
            }

            val chatImageWebpMap = mutableMapOf<String, ByteArray>()
            val chatImageBase64Map = mutableMapOf<String, String>()
            for (msg in messages) {
                val uriStr = msg.imageUri
                if (!uriStr.isNullOrBlank()) {
                    val rawBytes = ImageUtils.getImageBytesForUpload(context, uriStr)
                    val webp = rawBytes?.let { ImageUtils.toWebpBytes(it, 1024, 85) }
                    if (webp != null && webp.isNotEmpty()) {
                        chatImageWebpMap[msg.id] = webp
                        chatImageBase64Map[msg.id] = ImageUtils.safeBase64Encode(webp)
                    }
                }
            }

            val backup = MiraiBackupData(
                version = 3,
                appName = "MiraiAI",
                exportedAt = System.currentTimeMillis(),
                characters = characters,
                personas = personas,
                sessions = sessions,
                messages = messages,
                configs = configs,
                themeSettings = theme,
                showThinkingProcess = showThinking,
                debugLoggingEnabled = debugLogging,
                tokenCounterEnabled = tokenCounter,
                allowDeviceContext = allowDevice,
                uploadAsBase64 = uploadB64,
                characterAvatars = charBase64Map.ifEmpty { null },
                personaAvatars = personaBase64Map.ifEmpty { null },
                messageImages = chatImageBase64Map.ifEmpty { null }
            )

            val jsonString = gson.toJson(backup)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // 1. Write backup.json
                    val jsonEntry = ZipEntry("backup.json")
                    zipOut.putNextEntry(jsonEntry)
                    zipOut.write(jsonBytes)
                    zipOut.closeEntry()

                    // 2. Write character avatars as WebP
                    for ((charId, bytes) in charWebpMap) {
                        val entry = ZipEntry("avatars/characters/$charId.webp")
                        zipOut.putNextEntry(entry)
                        zipOut.write(bytes)
                        zipOut.closeEntry()
                    }

                    // 3. Write persona avatars as WebP
                    for ((personaId, bytes) in personaWebpMap) {
                        val entry = ZipEntry("avatars/personas/$personaId.webp")
                        zipOut.putNextEntry(entry)
                        zipOut.write(bytes)
                        zipOut.closeEntry()
                    }

                    // 4. Write chat images as WebP
                    for ((msgId, bytes) in chatImageWebpMap) {
                        val entry = ZipEntry("chat_images/$msgId.webp")
                        zipOut.putNextEntry(entry)
                        zipOut.write(bytes)
                        zipOut.closeEntry()
                    }

                    // 5. Write Live2D model assets
                    val live2dDir = File(context.filesDir, "live2d")
                    if (live2dDir.exists() && live2dDir.isDirectory) {
                        live2dDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relPath = file.relativeTo(live2dDir).path.replace('\\', '/')
                                val entry = ZipEntry("live2d/$relPath")
                                zipOut.putNextEntry(entry)
                                file.inputStream().use { input ->
                                    input.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                        }
                    }

                    zipOut.finish()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open destination file"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBackupFromUri(uri: Uri): Result<MiraiBackupData> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Failed to read selected file"))

            val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

            if (isZip) {
                val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
                val chatImagesDir = File(context.filesDir, "chat_images").apply { if (!exists()) mkdirs() }
                val live2dBaseDir = File(context.filesDir, "live2d").apply { if (!exists()) mkdirs() }
                var jsonContent: String? = null
                val extractedCharAvatars = mutableMapOf<String, String>()
                val extractedPersonaAvatars = mutableMapOf<String, String>()
                val extractedChatImages = mutableMapOf<String, String>()

                ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name == "backup.json") {
                            jsonContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
                        } else if (name.startsWith("avatars/characters/")) {
                            val charId = name.removePrefix("avatars/characters/").substringBeforeLast(".")
                            val entryBytes = zipIn.readBytes()
                            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")
                            FileOutputStream(targetFile).use { it.write(entryBytes) }
                            extractedCharAvatars[charId] = targetFile.absolutePath
                        } else if (name.startsWith("avatars/personas/")) {
                            val personaId = name.removePrefix("avatars/personas/").substringBeforeLast(".")
                            val entryBytes = zipIn.readBytes()
                            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")
                            FileOutputStream(targetFile).use { it.write(entryBytes) }
                            extractedPersonaAvatars[personaId] = targetFile.absolutePath
                        } else if (name.startsWith("chat_images/")) {
                            val msgId = name.removePrefix("chat_images/").substringBeforeLast(".")
                            val entryBytes = zipIn.readBytes()
                            val targetFile = File(chatImagesDir, "chat_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")
                            FileOutputStream(targetFile).use { it.write(entryBytes) }
                            extractedChatImages[msgId] = targetFile.absolutePath
                        } else if (name.startsWith("live2d/") && !entry.isDirectory) {
                            val relPath = name.removePrefix("live2d/")
                            if (relPath.isNotBlank()) {
                                val targetFile = File(live2dBaseDir, relPath)
                                if (targetFile.canonicalPath.startsWith(live2dBaseDir.canonicalPath)) {
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { out ->
                                        zipIn.copyTo(out)
                                    }
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                if (jsonContent.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Invalid .miraidb backup archive (missing backup.json)"))
                }

                val rawBackup = gson.fromJson(jsonContent, MiraiBackupData::class.java)
                    ?: return@withContext Result.failure(Exception("Invalid or corrupted backup data"))

                val updatedCharacters = rawBackup.characters.map { char ->
                    val localPath = extractedCharAvatars[char.id]
                        ?: rawBackup.characterAvatars?.get(char.id)?.let { b64 ->
                            saveBase64Avatar(context, b64)
                        }
                    if (localPath != null) {
                        char.copy(avatarUri = localPath)
                    } else {
                        char
                    }
                }

                val updatedPersonas = rawBackup.personas.map { persona ->
                    val localPath = extractedPersonaAvatars[persona.id]
                        ?: rawBackup.personaAvatars?.get(persona.id)?.let { b64 ->
                            saveBase64Avatar(context, b64)
                        }
                    if (localPath != null) {
                        persona.copy(avatarUri = localPath)
                    } else {
                        persona
                    }
                }

                val updatedMessages = rawBackup.messages.map { msg ->
                    val localPath = extractedChatImages[msg.id]
                        ?: rawBackup.messageImages?.get(msg.id)?.let { b64 ->
                            saveBase64ChatImage(context, b64)
                        }
                    if (localPath != null) {
                        msg.copy(imageUri = localPath)
                    } else {
                        msg
                    }
                }

                // If backup also had live2dModels base64 map (e.g. from JSON), restore any missing ones
                rawBackup.live2dModels?.forEach { (charId, base64Zip) ->
                    try {
                        val zipBytes = ImageUtils.safeBase64Decode(base64Zip)
                        restoreLive2dFromZipBytes(charId, zipBytes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val finalBackup = rawBackup.copy(
                    characters = updatedCharacters,
                    personas = updatedPersonas,
                    messages = updatedMessages
                )
                Result.success(finalBackup)
            } else {
                // Fallback for legacy JSON backup or plain JSON
                val jsonString = String(bytes, Charsets.UTF_8)
                val rawBackup = gson.fromJson(jsonString, MiraiBackupData::class.java)
                    ?: return@withContext Result.failure(Exception("Invalid or empty backup file format"))

                val updatedCharacters = rawBackup.characters.map { char ->
                    val localPath = rawBackup.characterAvatars?.get(char.id)?.let { b64 ->
                        saveBase64Avatar(context, b64)
                    }
                    if (localPath != null) {
                        char.copy(avatarUri = localPath)
                    } else {
                        char
                    }
                }

                val updatedPersonas = rawBackup.personas.map { persona ->
                    val localPath = rawBackup.personaAvatars?.get(persona.id)?.let { b64 ->
                        saveBase64Avatar(context, b64)
                    }
                    if (localPath != null) {
                        persona.copy(avatarUri = localPath)
                    } else {
                        persona
                    }
                }

                val updatedMessages = rawBackup.messages.map { msg ->
                    val localPath = rawBackup.messageImages?.get(msg.id)?.let { b64 ->
                        saveBase64ChatImage(context, b64)
                    }
                    if (localPath != null) {
                        msg.copy(imageUri = localPath)
                    } else {
                        msg
                    }
                }

                // Restore Live2D models from JSON backup if present
                rawBackup.live2dModels?.forEach { (charId, base64Zip) ->
                    try {
                        val zipBytes = ImageUtils.safeBase64Decode(base64Zip)
                        restoreLive2dFromZipBytes(charId, zipBytes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val finalBackup = rawBackup.copy(
                    characters = updatedCharacters,
                    personas = updatedPersonas,
                    messages = updatedMessages
                )
                Result.success(finalBackup)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveBase64Avatar(context: Context, base64: String): String? {
        return try {
            val rawData = if (base64.startsWith("data:image/")) {
                base64.substringAfter("base64,")
            } else base64
            val bytes = ImageUtils.safeBase64Decode(rawData)
            val webpBytes = ImageUtils.toWebpBytes(bytes, 720, 85) ?: bytes
            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")
            FileOutputStream(targetFile).use { it.write(webpBytes) }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBase64ChatImage(context: Context, base64: String): String? {
        return try {
            val rawData = if (base64.startsWith("data:image/")) {
                base64.substringAfter("base64,")
            } else base64
            val bytes = ImageUtils.safeBase64Decode(rawData)
            val webpBytes = ImageUtils.toWebpBytes(bytes, 1024, 85) ?: bytes
            val chatImagesDir = File(context.filesDir, "chat_images").apply { if (!exists()) mkdirs() }
            val targetFile = File(chatImagesDir, "chat_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")
            FileOutputStream(targetFile).use { it.write(webpBytes) }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreBackup(backup: MiraiBackupData, clearExisting: Boolean = false): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            if (clearExisting) {
                database.chatMessageDao().deleteAllMessages()
                database.chatSessionDao().deleteAllSessions()
                database.characterDao().deleteAllCharacters()
                database.userPersonaDao().deleteAllPersonas()
                database.inferenceConfigDao().deleteAllConfigs()
                try {
                    File(context.filesDir, "live2d").deleteRecursively()
                    File(context.filesDir, "avatars").deleteRecursively()
                    File(context.filesDir, "chat_images").deleteRecursively()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (backup.characters.isNotEmpty()) {
                database.characterDao().insertCharacters(backup.characters)
            }
            if (backup.personas.isNotEmpty()) {
                database.userPersonaDao().insertPersonas(backup.personas)
            }
            if (backup.configs.isNotEmpty()) {
                database.inferenceConfigDao().insertConfigs(backup.configs)
            }
            if (backup.sessions.isNotEmpty()) {
                database.chatSessionDao().insertSessions(backup.sessions)
            }
            if (backup.messages.isNotEmpty()) {
                database.chatMessageDao().insertMessages(backup.messages)
            }

            // Restore settings if provided
            backup.themeSettings?.let {
                settingsRepository.updateThemeSettings(themeMode = it.themeMode, isMonetEnabled = it.isMonetEnabled)
            }
            backup.showThinkingProcess?.let {
                settingsRepository.updateShowThinkingProcess(it)
            }
            backup.debugLoggingEnabled?.let {
                settingsRepository.updateDebugLoggingEnabled(it)
            }
            backup.tokenCounterEnabled?.let {
                settingsRepository.updateTokenCounterEnabled(it)
            }
            backup.allowDeviceContext?.let {
                settingsRepository.updateAllowDeviceContext(it)
            }
            backup.uploadAsBase64?.let {
                settingsRepository.updateUploadAsBase64(it)
            }

            // Also restore live2d if in live2dModels map (for JSON backups)
            backup.live2dModels?.forEach { (charId, base64Zip) ->
                try {
                    val zipBytes = ImageUtils.safeBase64Decode(base64Zip)
                    restoreLive2dFromZipBytes(charId, zipBytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val stats = getBackupStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
