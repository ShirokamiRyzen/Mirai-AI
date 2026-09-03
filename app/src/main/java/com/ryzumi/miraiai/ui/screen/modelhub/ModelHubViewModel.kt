package com.ryzumi.miraiai.ui.screen.modelhub

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ryzumi.miraiai.data.network.HuggingFaceRepository
import com.ryzumi.miraiai.data.worker.ModelDownloadWorker
import com.ryzumi.miraiai.domain.model.HuggingFaceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class ModelHubFilter(val label: String) {
    ALL("All Models"),
    TEXT_GGUF("Text / GGUF"),
    VISION("Vision (VLM)"),
    IMAGE_GEN("Image Gen"),
    DOWNLOADED("Downloaded")
}

enum class ModelSizeFilter(val label: String) {
    ALL("All Sizes"),
    UNDER_1GB("< 1 GB"),
    FROM_1_TO_3GB("1 - 3 GB"),
    FROM_3_TO_6GB("3 - 6 GB"),
    ABOVE_6GB("> 6 GB")
}

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class ModelDownloadState(
    val modelId: String,
    val progress: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val workRequestId: UUID? = null
)

data class ModelHubUiState(
    val searchQuery: String = "",
    val selectedFilter: ModelHubFilter = ModelHubFilter.ALL,
    val selectedSizeFilter: ModelSizeFilter = ModelSizeFilter.ALL,
    val systemTotalRamGb: Double = 0.0,
    val systemAvailRamGb: Double = 0.0,
    val models: List<HuggingFaceModel> = emptyList(),
    val downloadedModels: List<File> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextPageUrl: String? = null,
    val downloadStateMap: Map<String, ModelDownloadState> = emptyMap(),
    val errorMessage: String? = null
)

class ModelHubViewModel(
    private val context: Context,
    private val repository: HuggingFaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelHubUiState())
    val uiState: StateFlow<ModelHubUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        refreshRamInfo()
        searchModels("")
        loadDownloadedModels()
    }

    fun refreshRamInfo() {
        val (total, avail) = repository.getSystemRamInfo()
        _uiState.value = _uiState.value.copy(
            systemTotalRamGb = total,
            systemAvailRamGb = avail
        )
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun selectFilter(filter: ModelHubFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        if (filter == ModelHubFilter.DOWNLOADED) {
            loadDownloadedModels()
        }
    }

    fun selectSizeFilter(sizeFilter: ModelSizeFilter) {
        _uiState.value = _uiState.value.copy(selectedSizeFilter = sizeFilter)
    }

    fun searchModels(query: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                errorMessage = null,
                nextPageUrl = null
            )
            val result = repository.searchModels(query)
            result.onSuccess { pageResult ->
                _uiState.value = _uiState.value.copy(
                    models = pageResult.models,
                    nextPageUrl = pageResult.nextPageUrl,
                    isSearching = false
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = "Failed to load models: ${ex.message}"
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isSearching || state.nextPageUrl == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val result = repository.searchModels(state.searchQuery, state.nextPageUrl)
            result.onSuccess { pageResult ->
                val currentIds = state.models.map { it.id }.toSet()
                val newUnique = pageResult.models.filter { it.id !in currentIds }

                _uiState.value = _uiState.value.copy(
                    models = state.models + newUnique,
                    nextPageUrl = pageResult.nextPageUrl,
                    isLoadingMore = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun downloadModel(model: HuggingFaceModel) {
        val downloadUrl = model.downloadUrl
            ?: "https://huggingface.co/${model.id}/resolve/main/${model.selectedFileName ?: "model.gguf"}"
        val fileName = model.selectedFileName ?: "model.gguf"

        val inputData = workDataOf(
            ModelDownloadWorker.KEY_MODEL_ID to model.id,
            ModelDownloadWorker.KEY_DOWNLOAD_URL to downloadUrl,
            ModelDownloadWorker.KEY_FILE_NAME to fileName
        )

        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .build()

        val currentState = _uiState.value.downloadStateMap[model.id]
        val initialProgress = currentState?.progress ?: 0

        val newMap = _uiState.value.downloadStateMap.toMutableMap()
        newMap[model.id] = ModelDownloadState(
            modelId = model.id,
            progress = initialProgress,
            status = DownloadStatus.DOWNLOADING,
            workRequestId = workRequest.id
        )
        _uiState.value = _uiState.value.copy(downloadStateMap = newMap)

        workManager.enqueue(workRequest)

        workManager.getWorkInfoByIdLiveData(workRequest.id).observeForever { workInfo ->
            if (workInfo != null) {
                val latestMap = _uiState.value.downloadStateMap.toMutableMap()
                val existing = latestMap[model.id]

                if (existing != null && existing.status == DownloadStatus.PAUSED) {
                    // Do not overwrite paused state
                    return@observeForever
                }

                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, existing?.progress ?: 0)
                        latestMap[model.id] = ModelDownloadState(
                            modelId = model.id,
                            progress = progress,
                            status = DownloadStatus.DOWNLOADING,
                            workRequestId = workRequest.id
                        )
                        _uiState.value = _uiState.value.copy(downloadStateMap = latestMap)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        latestMap[model.id] = ModelDownloadState(
                            modelId = model.id,
                            progress = 100,
                            status = DownloadStatus.COMPLETED,
                            workRequestId = null
                        )
                        _uiState.value = _uiState.value.copy(downloadStateMap = latestMap)
                        loadDownloadedModels()
                        searchModels()
                    }
                    WorkInfo.State.FAILED -> {
                        if (existing?.status != DownloadStatus.PAUSED) {
                            latestMap[model.id] = ModelDownloadState(
                                modelId = model.id,
                                progress = existing?.progress ?: 0,
                                status = DownloadStatus.FAILED,
                                workRequestId = null
                            )
                            _uiState.value = _uiState.value.copy(downloadStateMap = latestMap)
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        // Handled by pause/cancel methods
                    }
                    else -> Unit
                }
            }
        }
    }

    fun pauseDownload(model: HuggingFaceModel) {
        val existing = _uiState.value.downloadStateMap[model.id] ?: return
        existing.workRequestId?.let { id ->
            workManager.cancelWorkById(id)
        }
        val newMap = _uiState.value.downloadStateMap.toMutableMap()
        newMap[model.id] = existing.copy(
            status = DownloadStatus.PAUSED,
            workRequestId = null
        )
        _uiState.value = _uiState.value.copy(downloadStateMap = newMap)
    }

    fun cancelDownload(model: HuggingFaceModel) {
        val existing = _uiState.value.downloadStateMap[model.id]
        existing?.workRequestId?.let { id ->
            workManager.cancelWorkById(id)
        }

        // Delete temporary file
        val fileName = model.selectedFileName ?: "model.gguf"
        val safeFileName = "${model.id.replace("/", "_")}_$fileName.tmp"
        val tempFile = File(context.filesDir, "models/$safeFileName")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        val newMap = _uiState.value.downloadStateMap.toMutableMap()
        newMap.remove(model.id)
        _uiState.value = _uiState.value.copy(downloadStateMap = newMap)
    }

    fun loadDownloadedModels() {
        val list = repository.getDownloadedModels()
        _uiState.value = _uiState.value.copy(downloadedModels = list)
    }

    fun deleteDownloadedFile(file: File) {
        if (file.exists()) {
            file.delete()
            loadDownloadedModels()
            searchModels()
        }
    }
}
