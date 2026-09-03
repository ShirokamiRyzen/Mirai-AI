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

import com.ryzumi.miraiai.domain.util.ImageUtils

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
    val errorMessage: String? = null
)

class CharacterEditViewModel(
    private val characterDao: CharacterDao,
    private val characterId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterEditUiState())
    val uiState: StateFlow<CharacterEditUiState> = _uiState.asStateFlow()

    init {
        if (!characterId.isNullOrBlank() && characterId != "new") {
            loadCharacter(characterId)
        }
    }

    private fun loadCharacter(id: String) {
        viewModelScope.launch {
            characterDao.getCharacterByIdSync(id)?.let { char ->
                _uiState.value = CharacterEditUiState(
                    id = char.id,
                    name = char.name,
                    avatarUri = char.avatarUri,
                    description = char.description,
                    personality = char.personality,
                    scenario = char.scenario,
                    impression = char.impression,
                    tagsInput = char.tags.joinToString(", "),
                    firstMessage = char.firstMessage,
                    isEditingExisting = true
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
                firstMessage = state.firstMessage.trim()
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
