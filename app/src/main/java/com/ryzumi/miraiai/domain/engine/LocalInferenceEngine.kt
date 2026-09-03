package com.ryzumi.miraiai.domain.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

sealed interface InferenceState {
    object Unloaded : InferenceState
    data class Loading(val progress: Float) : InferenceState
    data class Ready(val modelPath: String) : InferenceState
    data class Error(val message: String) : InferenceState
}

interface LocalInferenceEngine {
    val state: StateFlow<InferenceState>
    suspend fun loadModel(modelPath: String): Result<Unit>
    suspend fun unloadModel()
    fun generateStream(prompt: String): Flow<String>
}

class StubLocalInferenceEngine : LocalInferenceEngine {
    private val _state = MutableStateFlow<InferenceState>(InferenceState.Unloaded)
    override val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var currentPath: String? = null

    override suspend fun loadModel(modelPath: String): Result<Unit> {
        _state.value = InferenceState.Loading(0.1f)
        delay(500)
        _state.value = InferenceState.Loading(0.7f)
        delay(500)
        currentPath = modelPath
        _state.value = InferenceState.Ready(modelPath)
        return Result.success(Unit)
    }

    override suspend fun unloadModel() {
        currentPath = null
        _state.value = InferenceState.Unloaded
    }

    override fun generateStream(prompt: String): Flow<String> = flow {
        val responseText = " [Local Inference Engine Output]: Processed input prompt ($prompt) locally."
        val tokens = responseText.split(" ")
        for (token in tokens) {
            emit("$token ")
            delay(50)
        }
    }
}
