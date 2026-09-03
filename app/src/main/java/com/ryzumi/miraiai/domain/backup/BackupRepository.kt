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

    suspend fun getBackupStats(): BackupStats = withContext(Dispatchers.IO) {
        val chars = database.characterDao().getAllCharactersSync().size
        val personas = database.userPersonaDao().getAllPersonasSync().size
        val sessions = database.chatSessionDao().getAllSessionsSync().size
        val messages = database.chatMessageDao().getAllMessagesSync().size
        val configs = database.inferenceConfigDao().getAllConfigsSync().size

        val avatarsDir = File(context.filesDir, "avatars")
        val avatarFiles = avatarsDir.listFiles()?.filter { it.isFile } ?: emptyList()

        val chatImagesDir = File(context.filesDir, "chat_images")
        val chatImageFiles = chatImagesDir.listFiles()?.filter { it.isFile } ?: emptyList()

        val assetCount = avatarFiles.size + chatImageFiles.size
        val assetSizeBytes = avatarFiles.sumOf { it.length() } + chatImageFiles.sumOf { it.length() }

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

    suspend fun exportBackupToJson(): String = withContext(Dispatchers.IO) {
        val characters = database.characterDao().getAllCharactersSync()
        val personas = database.userPersonaDao().getAllPersonasSync()
        val sessions = database.chatSessionDao().getAllSessionsSync()
        val messages = database.chatMessageDao().getAllMessagesSync()
        val configs = database.inferenceConfigDao().getAllConfigsSync()
        val theme = settingsRepository.themeSettingsFlow.first()
        val showThinking = settingsRepository.showThinkingProcessFlow.first()
        val debugLogging = settingsRepository.debugLoggingEnabledFlow.first()

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

        val backup = MiraiBackupData(
            version = 2,
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
            characterAvatars = charBase64Map.ifEmpty { null },
            personaAvatars = personaBase64Map.ifEmpty { null }
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

            val backup = MiraiBackupData(
                version = 2,
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
                characterAvatars = charBase64Map.ifEmpty { null },
                personaAvatars = personaBase64Map.ifEmpty { null }
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
                var jsonContent: String? = null
                val extractedCharAvatars = mutableMapOf<String, String>()
                val extractedPersonaAvatars = mutableMapOf<String, String>()

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

                val finalBackup = rawBackup.copy(
                    characters = updatedCharacters,
                    personas = updatedPersonas
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

                val finalBackup = rawBackup.copy(
                    characters = updatedCharacters,
                    personas = updatedPersonas
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

    suspend fun restoreBackup(backup: MiraiBackupData, clearExisting: Boolean = false): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            if (clearExisting) {
                database.chatMessageDao().deleteAllMessages()
                database.chatSessionDao().deleteAllSessions()
                database.characterDao().deleteAllCharacters()
                database.userPersonaDao().deleteAllPersonas()
                database.inferenceConfigDao().deleteAllConfigs()
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

            val stats = getBackupStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
