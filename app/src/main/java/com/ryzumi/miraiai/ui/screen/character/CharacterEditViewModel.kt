package com.ryzumi.miraiai.ui.screen.character

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryzumi.miraiai.data.local.dao.CharacterDao
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

import com.ryzumi.miraiai.domain.live2d.Live2dImportResult
import com.ryzumi.miraiai.domain.live2d.Live2dManager
import com.ryzumi.miraiai.domain.util.ImageUtils

import com.ryzumi.miraiai.data.local.dao.InferenceConfigDao
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity

data class CharacterEditUiState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val avatarUri: String? = null,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val impression: String = "",
    val tagsInput: String = "",
    val firstMessage: String = "",
    val isEditingExisting: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val live2dPath: String? = null,
    val live2dModelName: String? = null,
    val isImportingLive2d: Boolean = false,
    val live2dImportError: String? = null,
    val live2dImportSuccessMsg: String? = null,
    val voiceId: String = "id_kawaii",
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val isTestingVoice: Boolean = false,
    val configs: List<InferenceConfigEntity> = emptyList(),
    val selectedConfigId: String? = null
)

class CharacterEditViewModel(
    private val characterDao: CharacterDao,
    private val inferenceConfigDao: InferenceConfigDao,
    private val characterId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterEditUiState())
    val uiState: StateFlow<CharacterEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            inferenceConfigDao.getAllConfigs().collect { cfgList ->
                _uiState.value = _uiState.value.copy(
                    configs = cfgList,
                    selectedConfigId = _uiState.value.selectedConfigId ?: cfgList.find { it.isActive }?.id ?: cfgList.firstOrNull()?.id
                )
            }
        }
        if (!characterId.isNullOrBlank() && characterId != "new") {
            loadCharacter(characterId)
        }
    }

    private fun loadCharacter(id: String) {
        viewModelScope.launch {
            characterDao.getCharacterByIdSync(id)?.let { char ->
                _uiState.value = _uiState.value.copy(
                    id = char.id,
                    name = char.name,
                    avatarUri = char.avatarUri,
                    description = char.description,
                    personality = char.personality,
                    scenario = char.scenario,
                    impression = char.impression,
                    tagsInput = char.tags.joinToString(", "),
                    firstMessage = char.firstMessage,
                    isEditingExisting = true,
                    live2dPath = char.live2dPath,
                    live2dModelName = char.live2dPath?.substringAfterLast('/')?.substringBefore(".model"),
                    voiceId = char.voiceId,
                    voicePitch = char.voicePitch,
                    voiceSpeed = char.voiceSpeed
                )
            }
        }
    }

    fun onNameChanged(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun onAvatarUriChanged(v: String?) { _uiState.value = _uiState.value.copy(avatarUri = v) }
    fun onDescriptionChanged(v: String) { _uiState.value = _uiState.value.copy(description = v) }
    fun onPersonalityChanged(v: String) { _uiState.value = _uiState.value.copy(personality = v) }
    fun onScenarioChanged(v: String) { _uiState.value = _uiState.value.copy(scenario = v) }
    fun onImpressionChanged(v: String) { _uiState.value = _uiState.value.copy(impression = v) }
    fun onTagsInputChanged(v: String) { _uiState.value = _uiState.value.copy(tagsInput = v) }
    fun onFirstMessageChanged(v: String) { _uiState.value = _uiState.value.copy(firstMessage = v) }
    fun onVoiceIdChanged(v: String) { _uiState.value = _uiState.value.copy(voiceId = v) }
    fun onVoicePitchChanged(v: Float) { _uiState.value = _uiState.value.copy(voicePitch = v) }
    fun onVoiceSpeedChanged(v: Float) { _uiState.value = _uiState.value.copy(voiceSpeed = v) }
    fun onConfigSelected(id: String) { _uiState.value = _uiState.value.copy(selectedConfigId = id) }

    fun testVoice(context: Context) {
        val state = _uiState.value
        _uiState.value = state.copy(isTestingVoice = true, errorMessage = null)
        val chosenConfig = state.configs.find { it.id == state.selectedConfigId }
            ?: state.configs.find { it.isActive }
            ?: state.configs.firstOrNull()
        com.ryzumi.miraiai.domain.tts.TtsManager.testVoice(
            context = context,
            voiceId = state.voiceId,
            pitch = state.voicePitch,
            speed = state.voiceSpeed,
            config = chosenConfig,
            characterName = state.name.ifBlank { "Character" },
            onStart = {
                _uiState.value = _uiState.value.copy(isTestingVoice = true)
            },
            onDone = {
                _uiState.value = _uiState.value.copy(isTestingVoice = false)
            },
            onError = { errMsg ->
                _uiState.value = _uiState.value.copy(
                    isTestingVoice = false,
                    errorMessage = errMsg
                )
            }
        )
    }

    fun stopTestVoice() {
        com.ryzumi.miraiai.domain.tts.TtsManager.stop()
        _uiState.value = _uiState.value.copy(isTestingVoice = false)
    }

    fun importLive2dArchive(context: Context, zipUri: Uri) {
        val charId = _uiState.value.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingLive2d = true,
                live2dImportError = null,
                live2dImportSuccessMsg = null
            )
            when (val result = Live2dManager.validateAndImport(context, charId, zipUri)) {
                is Live2dImportResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isImportingLive2d = false,
                        live2dPath = result.relativeModelPath,
                        live2dModelName = result.modelName,
                        live2dImportSuccessMsg = "Live2D (${result.cubismVersion}) valid and saved! (${result.textureCount} textures)"
                    )
                }
                is Live2dImportResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isImportingLive2d = false,
                        live2dImportError = result.message
                    )
                }
            }
        }
    }

    fun removeLive2d(context: Context) {
        val charId = _uiState.value.id
        Live2dManager.deleteCharacterLive2d(context, charId)
        _uiState.value = _uiState.value.copy(
            live2dPath = null,
            live2dModelName = null,
            live2dImportError = null,
            live2dImportSuccessMsg = null
        )
    }

    fun dismissLive2dMessages() {
        _uiState.value = _uiState.value.copy(
            live2dImportError = null,
            live2dImportSuccessMsg = null
        )
    }

    fun saveCharacter(context: Context) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Character name cannot be empty")
            return
        }

        val parsedTags = state.tagsInput
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        viewModelScope.launch {
            val permanentAvatarUri = saveAvatarLocally(context, state.avatarUri, state.id)

            val entity = CharacterEntity(
                id = state.id,
                name = state.name.trim(),
                avatarUri = permanentAvatarUri,
                description = state.description.trim(),
                personality = state.personality.trim(),
                scenario = state.scenario.trim(),
                impression = state.impression.trim(),
                tags = parsedTags,
                firstMessage = state.firstMessage.trim(),
                live2dPath = state.live2dPath,
                voiceId = state.voiceId,
                voicePitch = state.voicePitch,
                voiceSpeed = state.voiceSpeed
            )

            characterDao.insertCharacter(entity)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    private suspend fun saveAvatarLocally(context: Context, uriString: String?, charId: String): String? {
        if (uriString.isNullOrBlank()) return null
        val avatarsDir = File(context.filesDir, "avatars")
        return if (uriString.startsWith("/") && uriString.startsWith(avatarsDir.absolutePath) && File(uriString).exists() && (uriString.endsWith(".webp", ignoreCase = true) || uriString.endsWith(".jpg", ignoreCase = true))) {
            uriString
        } else {
            ImageUtils.cropAndSaveAvatar(context, uriString, targetDimension = 720, quality = 85) ?: uriString
        }
    }
}
