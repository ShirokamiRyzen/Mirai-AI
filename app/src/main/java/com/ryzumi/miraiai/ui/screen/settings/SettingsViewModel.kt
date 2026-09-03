package com.ryzumi.miraiai.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.data.datastore.ThemeSettings
import com.ryzumi.miraiai.data.local.dao.InferenceConfigDao
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.data.network.HuggingFaceRepository
import com.ryzumi.miraiai.data.network.OpenAiRepository
import com.ryzumi.miraiai.data.network.DebugLogEntry
import com.ryzumi.miraiai.data.network.DebugLogManager
import com.ryzumi.miraiai.domain.backup.BackupRepository
import com.ryzumi.miraiai.domain.model.BackupStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SettingsUiState(
    val configs: List<InferenceConfigEntity> = emptyList(),
    val selectedConfig: InferenceConfigEntity? = null,
    val availableModels: List<String> = emptyList(),
    val visionModels: List<String> = emptyList(),
    val localModels: List<String> = emptyList(),
    val isFetchingModels: Boolean = false,
    val isTestingVision: Boolean = false,
    val visionTestResult: String? = null,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val themeSettings: ThemeSettings = ThemeSettings(),
    val isDebugLoggingEnabled: Boolean = true,
    val isShowThinkingProcess: Boolean = false,
    val isTokenCounterEnabled: Boolean = false,
    val isAllowDeviceContext: Boolean = false,
    val debugLogs: List<DebugLogEntry> = emptyList(),
    val backupStats: BackupStats = BackupStats(),
    val isExportingBackup: Boolean = false,
    val isImportingBackup: Boolean = false,
    val backupSuccessMessage: String? = null,
    val backupErrorMessage: String? = null
)

class SettingsViewModel(
    private val inferenceConfigDao: InferenceConfigDao,
    private val openAiRepository: OpenAiRepository,
    private val huggingFaceRepository: HuggingFaceRepository,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository? = null
) : ViewModel() {

    private val gson = Gson()
    private val _selectedConfigId = MutableStateFlow<String?>(null)
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    private val _visionModels = MutableStateFlow<List<String>>(emptyList())
    private val _localModels = MutableStateFlow<List<String>>(emptyList())
    private val _isFetchingModels = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isError = MutableStateFlow(false)

    private val _backupStats = MutableStateFlow(BackupStats())
    private val _isExportingBackup = MutableStateFlow(false)
    private val _isImportingBackup = MutableStateFlow(false)
    private val _backupSuccessMessage = MutableStateFlow<String?>(null)
    private val _backupErrorMessage = MutableStateFlow<String?>(null)

    init {
        loadLocalModels()
        refreshBackupStats()
        viewModelScope.launch {
            settingsRepository.debugLoggingEnabledFlow.collect { enabled ->
                DebugLogManager.setLoggingEnabled(enabled)
            }
        }
        viewModelScope.launch {
            val existing = inferenceConfigDao.getActiveConfigSync()
            if (existing == null) {
                val defaultConfig = InferenceConfigEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Default Profile",
                    baseUrl = "https://openrouter.ai/api/v1",
                    generateModelId = "gpt-3.5-turbo",
                    visionModelId = "gpt-4o",
                    isActive = true
                )
                inferenceConfigDao.insertConfig(defaultConfig)
                _selectedConfigId.value = defaultConfig.id
            } else {
                _selectedConfigId.value = existing.id
            }
        }
    }

    fun loadLocalModels() {
        val downloaded = huggingFaceRepository.getDownloadedModels().map { it.name }
        _localModels.value = downloaded.ifEmpty { listOf("None (Download via Model Hub)") }
    }

    private val _isTestingVision = MutableStateFlow(false)
    private val _visionTestResult = MutableStateFlow<String?>(null)

    private data class FiveNetwork(
        val models: List<String>,
        val visionModels: List<String>,
        val localModels: List<String>,
        val isFetching: Boolean,
        val status: String?
    )

    private val networkStateFlow = combine(
        _availableModels,
        _visionModels,
        _localModels,
        _isFetchingModels,
        _statusMessage
    ) { models, visionModels, localModels, isFetching, status ->
        FiveNetwork(models, visionModels, localModels, isFetching, status)
    }

    private data class ExtraState(
        val models: List<String>,
        val visionModels: List<String>,
        val localModels: List<String>,
        val isFetching: Boolean,
        val status: String?,
        val isError: Boolean,
        val theme: ThemeSettings,
        val isTestingVision: Boolean,
        val visionTestResult: String?,
        val isDebugEnabled: Boolean
    )

    private val extraStateFlow = combine(
        networkStateFlow,
        _isError,
        settingsRepository.themeSettingsFlow,
        _isTestingVision,
        _visionTestResult
    ) { net, isError, theme, isTesting, testResult ->
        Tuple5(net, isError, theme, isTesting, testResult)
    }

    private data class Tuple5(
        val net: FiveNetwork,
        val isError: Boolean,
        val theme: ThemeSettings,
        val isTesting: Boolean,
        val testResult: String?
    )

    private data class BackupState(
        val stats: BackupStats,
        val isExporting: Boolean,
        val isImporting: Boolean,
        val successMsg: String?,
        val errorMsg: String?
    )

    private val backupStateFlow = combine(
        _backupStats,
        _isExportingBackup,
        _isImportingBackup,
        _backupSuccessMessage,
        _backupErrorMessage
    ) { stats, isExporting, isImporting, successMsg, errorMsg ->
        BackupState(stats, isExporting, isImporting, successMsg, errorMsg)
    }

    private data class AdvanceAndBackupState(
        val isDebugEnabled: Boolean,
        val isShowThinking: Boolean,
        val isTokenCounter: Boolean,
        val isAllowDeviceContext: Boolean,
        val backup: BackupState
    )

    private val advanceAndBackupFlow = combine(
        combine(
            settingsRepository.debugLoggingEnabledFlow,
            settingsRepository.showThinkingProcessFlow,
            settingsRepository.tokenCounterEnabledFlow,
            settingsRepository.allowDeviceContextFlow
        ) { isDebug, isShowThinking, isTokenCounter, isAllowDeviceContext ->
            Quadruple(isDebug, isShowThinking, isTokenCounter, isAllowDeviceContext)
        },
        backupStateFlow
    ) { quad, backup ->
        AdvanceAndBackupState(quad.first, quad.second, quad.third, quad.fourth, backup)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    val uiState: StateFlow<SettingsUiState> = combine(
        inferenceConfigDao.getAllConfigs(),
        _selectedConfigId,
        extraStateFlow,
        advanceAndBackupFlow,
        DebugLogManager.logs
    ) { configs, selectedId, tuple, advanceState, logs ->
        val currentSelected = configs.find { it.id == selectedId }
            ?: configs.find { it.isActive }
            ?: configs.firstOrNull()

        val parsedModels = currentSelected?.let { cfg ->
            if (cfg.availableModelsJson.isNotBlank()) {
                try {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(cfg.availableModelsJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        } ?: emptyList()

        val modelsToShow = tuple.net.models.ifEmpty { parsedModels }

        SettingsUiState(
            configs = configs,
            selectedConfig = currentSelected,
            availableModels = modelsToShow,
            visionModels = tuple.net.visionModels.ifEmpty { modelsToShow },
            localModels = tuple.net.localModels,
            isFetchingModels = tuple.net.isFetching,
            isTestingVision = tuple.isTesting,
            visionTestResult = tuple.testResult,
            statusMessage = tuple.net.status,
            isError = tuple.isError,
            themeSettings = tuple.theme,
            isDebugLoggingEnabled = advanceState.isDebugEnabled,
            isShowThinkingProcess = advanceState.isShowThinking,
            isTokenCounterEnabled = advanceState.isTokenCounter,
            isAllowDeviceContext = advanceState.isAllowDeviceContext,
            debugLogs = logs,
            backupStats = advanceState.backup.stats,
            isExportingBackup = advanceState.backup.isExporting,
            isImportingBackup = advanceState.backup.isImporting,
            backupSuccessMessage = advanceState.backup.successMsg,
            backupErrorMessage = advanceState.backup.errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateTokenCounterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTokenCounterEnabled(enabled)
        }
    }

    fun updateAllowDeviceContext(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAllowDeviceContext(enabled)
        }
    }

    fun selectConfigProfile(configId: String) {
        _selectedConfigId.value = configId
        _availableModels.value = emptyList()
        _visionModels.value = emptyList()
        loadLocalModels()
    }

    fun setActiveProfile(configId: String) {
        viewModelScope.launch {
            inferenceConfigDao.switchActiveProfile(configId)
            _selectedConfigId.value = configId
            val config = inferenceConfigDao.getConfigByIdSync(configId)
            _statusMessage.value = "Active profile set to '${config?.name ?: "Selected Profile"}'"
            _isError.value = false
        }
    }

    fun saveConfigProfile(config: InferenceConfigEntity) {
        viewModelScope.launch {
            val modelsJson = if (_availableModels.value.isNotEmpty()) {
                gson.toJson(_availableModels.value)
            } else {
                config.availableModelsJson
            }
            val toSave = config.copy(availableModelsJson = modelsJson)
            inferenceConfigDao.insertConfig(toSave)
            _selectedConfigId.value = toSave.id
            _statusMessage.value = "Profile '${toSave.name}' saved successfully!"
            _isError.value = false
        }
    }

    fun deleteConfigProfile(config: InferenceConfigEntity) {
        viewModelScope.launch {
            inferenceConfigDao.deleteConfig(config)
            _statusMessage.value = "Profile deleted"
            _isError.value = false
            val remaining = uiState.value.configs.filter { it.id != config.id }
            if (remaining.isNotEmpty()) {
                val next = remaining.first()
                if (config.isActive) {
                    inferenceConfigDao.setActiveFlag(next.id)
                }
                _selectedConfigId.value = next.id
            }
        }
    }

    fun deleteConfigProfiles(configIds: Set<String>) {
        viewModelScope.launch {
            for (id in configIds) {
                val cfg = inferenceConfigDao.getConfigByIdSync(id)
                if (cfg != null) {
                    inferenceConfigDao.deleteConfig(cfg)
                }
            }
            _statusMessage.value = "${configIds.size} profile(s) deleted"
            _isError.value = false
            val remaining = uiState.value.configs.filter { !configIds.contains(it.id) }
            if (remaining.isNotEmpty()) {
                val next = remaining.first()
                inferenceConfigDao.setActiveFlag(next.id)
                _selectedConfigId.value = next.id
            }
        }
    }

    fun fetchAvailableModels(baseUrl: String, apiKey: String, customHeadersJson: String = "") {
        if (baseUrl.isBlank()) {
            _isError.value = true
            _statusMessage.value = "Base URL cannot be empty"
            return
        }

        viewModelScope.launch {
            _isFetchingModels.value = true
            _statusMessage.value = "Fetching models from endpoint..."
            _isError.value = false

            val result = openAiRepository.fetchAvailableModels(
                baseUrl = baseUrl,
                apiKey = apiKey,
                customHeadersJson = customHeadersJson
            )

            result.onSuccess { fetchResult ->
                val all = fetchResult.allModels
                val vision = fetchResult.visionModels

                if (all.isNotEmpty()) {
                    _availableModels.value = all
                    _visionModels.value = vision
                    _statusMessage.value = "Successfully fetched ${all.size} active models!"
                } else {
                    _statusMessage.value = "No active models found at endpoint."
                }
            }.onFailure { ex ->
                _isError.value = true
                _statusMessage.value = "Fetch failed: ${ex.message}"
            }

            _isFetchingModels.value = false
        }
    }

    fun testVisionCapability(config: InferenceConfigEntity) {
        val modelToTest = if (config.visionModelId.isNotBlank() && config.visionModelId != "auto-debug" && config.visionModelId != "none") {
            config.visionModelId
        } else {
            config.generateModelId
        }

        viewModelScope.launch {
            _isTestingVision.value = true
            _visionTestResult.value = "Sending 1x1 test image to $modelToTest..."
            val result = DebugLogManager.testVisionCapability(
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                modelId = modelToTest,
                customHeadersJson = config.customHeaders
            )
            result.onSuccess { responseText ->
                _visionTestResult.value = "Vision Test Success: $responseText"
            }.onFailure { ex ->
                _visionTestResult.value = "Vision Test Failed: ${ex.message}"
            }
            _isTestingVision.value = false
        }
    }

    fun clearDebugLogs() {
        DebugLogManager.clearLogs()
        _visionTestResult.value = null
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateThemeSettings(themeMode = mode)
        }
    }

    fun updateMonetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateThemeSettings(isMonetEnabled = enabled)
        }
    }

    fun updateDebugLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDebugLoggingEnabled(enabled)
            DebugLogManager.setLoggingEnabled(enabled)
        }
    }

    fun updateShowThinkingProcess(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowThinkingProcess(enabled)
        }
    }

    fun refreshBackupStats() {
        viewModelScope.launch {
            backupRepository?.let {
                _backupStats.value = it.getBackupStats()
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isExportingBackup.value = true
            _backupSuccessMessage.value = null
            _backupErrorMessage.value = null
            backupRepository?.let { repo ->
                val result = repo.writeBackupToUri(uri)
                result.onSuccess {
                    _backupSuccessMessage.value = "Backup successfully exported to .miraidb file!"
                    _backupStats.value = repo.getBackupStats()
                }.onFailure { ex ->
                    _backupErrorMessage.value = "Export failed: ${ex.message}"
                }
            } ?: run {
                _backupErrorMessage.value = "Backup service unavailable"
            }
            _isExportingBackup.value = false
        }
    }

    fun importBackup(uri: Uri, clearExisting: Boolean = false) {
        viewModelScope.launch {
            _isImportingBackup.value = true
            _backupSuccessMessage.value = null
            _backupErrorMessage.value = null
            backupRepository?.let { repo ->
                val readResult = repo.readBackupFromUri(uri)
                readResult.onSuccess { backupData ->
                    val restoreResult = repo.restoreBackup(backupData, clearExisting)
                    restoreResult.onSuccess { stats ->
                        _backupSuccessMessage.value = "Restored successfully: ${stats.characterCount} chars, ${stats.personaCount} personas, ${stats.sessionCount} sessions, ${stats.messageCount} messages, ${stats.assetCount} assets (${stats.formattedDataSize})."
                        _backupStats.value = repo.getBackupStats()
                    }.onFailure { ex ->
                        _backupErrorMessage.value = "Restore failed: ${ex.message}"
                    }
                }.onFailure { ex ->
                    _backupErrorMessage.value = "Import failed: ${ex.message}"
                }
            } ?: run {
                _backupErrorMessage.value = "Backup service unavailable"
            }
            _isImportingBackup.value = false
        }
    }

    fun clearBackupMessage() {
        _backupSuccessMessage.value = null
        _backupErrorMessage.value = null
    }
}
