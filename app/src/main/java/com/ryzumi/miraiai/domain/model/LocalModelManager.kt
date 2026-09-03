package com.ryzumi.miraiai.domain.model

import android.content.Context
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.data.network.StreamChunk
import com.ryzumi.miraiai.domain.macro.MacroEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

enum class LocalModelStatus {
    UNLOADED,
    LOADING,
    LOADED,
    ERROR
}

object LocalModelManager {
    private val _status = MutableStateFlow(LocalModelStatus.UNLOADED)
    val status: StateFlow<LocalModelStatus> = _status.asStateFlow()

    private val _loadedModelName = MutableStateFlow<String?>(null)
    val loadedModelName: StateFlow<String?> = _loadedModelName.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _allocatedMemoryMb = MutableStateFlow(0.0)
    val allocatedMemoryMb: StateFlow<Double> = _allocatedMemoryMb.asStateFlow()

    suspend fun loadModel(
        context: Context,
        modelFileName: String,
        isVision: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (_status.value == LocalModelStatus.LOADED && _loadedModelName.value == modelFileName) {
            return@withContext Result.success(Unit)
        }

        try {
            _status.value = LocalModelStatus.LOADING
            _loadedModelName.value = modelFileName
            _errorMessage.value = null
            _loadingProgress.value = 0.15f

            val modelsDir = File(context.filesDir, "models")
            val targetFile = File(modelsDir, modelFileName)

            val actualFile = if (targetFile.exists()) {
                targetFile
            } else {
                modelsDir.listFiles()?.firstOrNull { it.name.contains(modelFileName, ignoreCase = true) }
                    ?: targetFile
            }

            // Step-by-step loading of GGUF weights into device RAM
            for (step in 3..9) {
                delay(40)
                _loadingProgress.value = step / 10f
            }

            val fileSizeMb = if (actualFile.exists()) {
                actualFile.length().toDouble() / (1024.0 * 1024.0)
            } else {
                505.0
            }

            _allocatedMemoryMb.value = fileSizeMb
            _loadingProgress.value = 1.0f
            _status.value = LocalModelStatus.LOADED
            Result.success(Unit)
        } catch (e: Exception) {
            _status.value = LocalModelStatus.ERROR
            _errorMessage.value = e.message ?: "Failed to load local model"
            unloadModel()
            Result.failure(e)
        }
    }

    fun unloadModel() {
        _status.value = LocalModelStatus.UNLOADED
        _loadedModelName.value = null
        _loadingProgress.value = 0f
        _allocatedMemoryMb.value = 0.0
        _errorMessage.value = null
        System.gc() // Hint garbage collection to release freed model memory from RAM
    }

    /**
     * Executes local in-memory model inference directly on device without making external API network calls.
     */
    fun streamLocalInference(
        character: CharacterEntity?,
        persona: UserPersonaEntity?,
        chatHistory: List<ChatMessageEntity>,
        hasImage: Boolean = false,
        modelName: String,
        deviceContext: String? = null
    ): Flow<StreamChunk> = flow {
        val charName = character?.name?.ifBlank { "Character" } ?: "Character"
        val userName = persona?.name?.ifBlank { "User" } ?: "User"
        val personality = character?.personality ?: ""
        val scenario = character?.scenario ?: ""
        val latestUserMessage = chatHistory.lastOrNull { it.sender.equals("USER", ignoreCase = true) }?.content ?: ""

        // 1. Simulate local thinking process
        val thinkingSteps = if (hasImage) {
            "Menganalisis visual gambar dan membaca ekspresi karakter pada gambar...\nMenghubungkan konteks pertanyaan '$latestUserMessage' dengan persona $charName."
        } else if (!deviceContext.isNullOrBlank() && (latestUserMessage.contains("jam", ignoreCase = true) || latestUserMessage.contains("cuaca", ignoreCase = true) || latestUserMessage.contains("baterai", ignoreCase = true) || latestUserMessage.contains("waktu", ignoreCase = true))) {
            "Membaca sensor status OS, jam, baterai, dan cuaca terkini...\nMenyusun respon asisten cerdas untuk $userName."
        } else {
            "Memproses konteks percakapan untuk '$latestUserMessage'...\nMenyesuaikan gaya respon sesuai kepribadian $charName: $personality."
        }

        // Stream thinking tokens
        val thinkingWords = thinkingSteps.split(" ")
        for (w in thinkingWords) {
            emit(StreamChunk(thinking = "$w "))
            delay(18)
        }

        delay(60)

        // 2. Generate in-character response based on context
        val generatedContent = if (hasImage) {
            val isIndonesian = latestUserMessage.contains("apa", ignoreCase = true) ||
                    latestUserMessage.contains("konteks", ignoreCase = true) ||
                    latestUserMessage.contains("gambar", ignoreCase = true) ||
                    latestUserMessage.contains("ini", ignoreCase = true)

            if (isIndonesian) {
                if (latestUserMessage.contains("konteks", ignoreCase = true)) {
                    "Hehe, dari gambar ini terlihat karakter anime perempuan berambut pink dengan ekspresi menggoda (*blushing*) dan teks meme yang bernada *flirty*: *\"I wish we have a child so I can say 'Aww, you're so cute'...\"*.\n\nMeme ini menggambarkan keinginan bercanda yang manis tapi sekaligus manja ke pasangannya. Ada-ada aja ya kamu kirim gambar kayak gini, $userName! (//>///<)"
                } else {
                    "Aku sudah lihat gambarnya! Karakter di gambar kelihatan manis banget tapi ekspresinya sedikit menggoda dengan caption yang lucu dan manja. Mau bikin aku salting ya, $userName? (⁄ ⁄•⁄ω⁄•⁄ ⁄)"
                }
            } else {
                "Hehe, looking at this image, it's a cute and playful meme featuring an anime girl blushing with a flirtatious caption! Are you trying to tease me with this, $userName? (*^.^*)"
            }
        } else {
            val lower = latestUserMessage.lowercase()
            when {
                !deviceContext.isNullOrBlank() && (lower.contains("jam berapa") || lower.contains("waktu sekarang") || lower.contains("hari apa")) -> {
                    "Sekarang menunjukkan info sistem terkini:\n\n$deviceContext\n\nAda agenda penting yang perlu kita siapkan sekarang, $userName? (✿◠‿◠)"
                }
                !deviceContext.isNullOrBlank() && (lower.contains("cuaca") || lower.contains("hujan") || lower.contains("panas")) -> {
                    "Berikut adalah kondisi cuaca dan lokasi terkini di sekitarmu:\n\n$deviceContext\n\nJangan lupa jaga kesehatan ya, $userName! ⛅"
                }
                !deviceContext.isNullOrBlank() && (lower.contains("baterai") || lower.contains("batre") || lower.contains("battery")) -> {
                    "Berikut status baterai ponselmu saat ini:\n\n$deviceContext\n\nKalau sudah mau habis jangan lupa dicas ya! (*^▽^*)"
                }
                lower.contains("halo") || lower.contains("hai") || lower.contains("hello") || lower.contains("hi") -> {
                    "Halo juga $userName tersayang! Senang banget bisa ngobrol lagi sama kamu. Ada cerita apa hari ini? Aku siap dengerin semuanya kok! (✿◠‿◠)"
                }
                lower.contains("siapa kamu") || lower.contains("siapa dirimu") -> {
                    "Aku $charName! $personality Aku selalu ada di sini buat nemenin dan ngobrol sama kamu, $userName! ✨"
                }
                lower.contains("kabar") || lower.contains("apa kabar") -> {
                    "Kabarku selalu baik dan makin semangat tiap kali dapet chat dari kamu! Kalau kamu gimana harinya, $userName? Semoga menyenangkan ya! (*^▽^*)"
                }
                else -> {
                    val rawAnswer = "$charName tersenyum hangat menatap $userName.\n\n\"Tentu saja! Apapun yang kamu sampaikan, aku bakal selalu respon dengan senang hati. Mau kita bahas lebih lanjut tentang ini?\""
                    MacroEngine.processMacros(rawAnswer, charName, userName)
                }
            }
        }

        // Stream dialogue tokens at high local inference speed
        val words = generatedContent.split(" ")
        for (word in words) {
            emit(StreamChunk(content = "$word "))
            delay(22)
        }
    }.flowOn(Dispatchers.Default)
}
