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

    private var _loadedModelFile: File? = null

    suspend fun loadModel(
        context: Context,
        modelFileName: String,
        isVision: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (_status.value == LocalModelStatus.LOADED && _loadedModelName.value == modelFileName && _loadedModelFile?.exists() == true) {
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

            // Execute model loading via LiteRT on-device engine
            val result = com.ryzumi.miraiai.domain.engine.LiteRtInferenceEngine.loadModel(
                context = context,
                modelFile = actualFile,
                isVision = isVision,
                onProgress = { p -> _loadingProgress.value = p }
            )

            if (result.isFailure) {
                throw result.exceptionOrNull() ?: Exception("Gagal memuat model via LiteRT")
            }

            val fileSizeMb = result.getOrNull() ?: (actualFile.length().toDouble() / (1024.0 * 1024.0))

            _loadedModelFile = actualFile
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
        _loadedModelFile = null
        _loadingProgress.value = 0f
        _allocatedMemoryMb.value = 0.0
        _errorMessage.value = null
        System.gc() // Hint garbage collection to release freed model memory from RAM
    }

    /**
     * Executes local in-memory model inference directly on device via LiteRT.
     */
    fun streamLocalInference(
        character: CharacterEntity?,
        persona: UserPersonaEntity?,
        chatHistory: List<ChatMessageEntity>,
        hasImage: Boolean = false,
        modelName: String,
        deviceContext: String? = null
    ): Flow<StreamChunk> {
        return com.ryzumi.miraiai.domain.engine.LiteRtInferenceEngine.streamInference(
            modelFile = _loadedModelFile,
            character = character,
            persona = persona,
            chatHistory = chatHistory,
            hasImage = hasImage,
            modelName = modelName,
            deviceContext = deviceContext
        )
    }
}
