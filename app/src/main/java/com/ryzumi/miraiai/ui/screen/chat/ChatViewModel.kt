package com.ryzumi.miraiai.ui.screen.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.data.local.MiraiDatabase
import com.ryzumi.miraiai.data.local.dao.CharacterDao
import com.ryzumi.miraiai.data.local.dao.ChatMessageDao
import com.ryzumi.miraiai.data.local.dao.ChatSessionDao
import com.ryzumi.miraiai.data.local.dao.InferenceConfigDao
import com.ryzumi.miraiai.data.local.dao.UserPersonaDao
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.ChatSessionEntity
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.data.network.DebugLogManager
import com.ryzumi.miraiai.data.network.OpenAiRepository
import com.ryzumi.miraiai.domain.engine.ChatGenerationManager
import com.ryzumi.miraiai.domain.model.LocalModelManager
import com.ryzumi.miraiai.domain.model.LocalModelStatus
import com.ryzumi.miraiai.domain.util.ImageUtils
import com.ryzumi.miraiai.domain.util.TokenUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val session: ChatSessionEntity? = null,
    val character: CharacterEntity? = null,
    val persona: UserPersonaEntity? = null,
    val allPersonas: List<UserPersonaEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val configs: List<InferenceConfigEntity> = emptyList(),
    val activeConfig: InferenceConfigEntity? = null,
    val inputText: String = "",
    val selectedImageUri: String? = null,
    val isProcessingImage: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingThinking: String = "",
    val streamingText: String = "",
    val isLiveThinkingExpanded: Boolean = true,
    val isShowThinkingEnabled: Boolean = false,
    val isTokenCounterEnabled: Boolean = false,
    val isDebugLoggingEnabled: Boolean = false,
    val estimatedContextTokens: Int = 0,
    val streamingTokensCount: Int = 0,
    val streamingSpeedTps: Double = 0.0,
    val streamingModelName: String = "",
    val errorMessage: String? = null,
    val localModelStatus: LocalModelStatus = LocalModelStatus.UNLOADED,
    val loadedLocalModelName: String? = null,
    val isUsingLocalModel: Boolean = false,
    val localModelMemoryMb: Double = 0.0,
    val localModelLoadingProgress: Float = 0f
)

class ChatViewModel(
    private val sessionId: String,
    private val database: MiraiDatabase,
    private val chatSessionDao: ChatSessionDao = database.chatSessionDao(),
    private val chatMessageDao: ChatMessageDao = database.chatMessageDao(),
    private val characterDao: CharacterDao = database.characterDao(),
    private val userPersonaDao: UserPersonaDao = database.userPersonaDao(),
    private val inferenceConfigDao: InferenceConfigDao = database.inferenceConfigDao(),
    private val openAiRepository: OpenAiRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _selectedImageUri = MutableStateFlow<String?>(null)
    private val _isProcessingImage = MutableStateFlow(false)
    private val _isLiveThinkingExpanded = MutableStateFlow(true)
    private val _localError = MutableStateFlow<String?>(null)

    private val generationStreamState = ChatGenerationManager.getStreamStateFlow(sessionId)

    init {
        ChatGenerationManager.setActiveVisibleSession(sessionId)
    }

    override fun onCleared() {
        super.onCleared()
        ChatGenerationManager.clearActiveVisibleSession(sessionId)
    }

    private data class DbData(
        val session: ChatSessionEntity?,
        val messages: List<ChatMessageEntity>,
        val configs: List<InferenceConfigEntity>
    )

    private val dbDataFlow = combine(
        chatSessionDao.getSessionById(sessionId),
        chatMessageDao.getMessagesForSession(sessionId),
        inferenceConfigDao.getAllConfigs()
    ) { session, messages, configs ->
        DbData(session, messages, configs)
    }

    private data class InputState(
        val input: String,
        val imageUri: String?,
        val isProcessing: Boolean
    )

    private val inputStateFlow = combine(
        _inputText,
        _selectedImageUri,
        _isProcessingImage
    ) { input, imageUri, isProcessing ->
        InputState(input, imageUri, isProcessing)
    }

    private data class StreamState(
        val isStreaming: Boolean,
        val thinking: String,
        val text: String,
        val tokensCount: Int,
        val speedTps: Double,
        val modelName: String,
        val errorMessage: String?,
        val isExpanded: Boolean
    )

    private val fullStreamStateFlow = combine(
        generationStreamState,
        _isLiveThinkingExpanded
    ) { stream, isExpanded ->
        StreamState(
            isStreaming = stream.isStreaming,
            thinking = stream.thinking,
            text = stream.text,
            tokensCount = stream.tokensCount,
            speedTps = stream.speedTps,
            modelName = stream.modelName,
            errorMessage = stream.errorMessage,
            isExpanded = isExpanded
        )
    }

    private data class CoreChatData(
        val session: ChatSessionEntity?,
        val messages: List<ChatMessageEntity>,
        val configs: List<InferenceConfigEntity>,
        val inputState: InputState,
        val streamState: StreamState
    )

    private val coreDataFlow = combine(
        dbDataFlow,
        inputStateFlow,
        fullStreamStateFlow
    ) { dbData, inputState, streamState ->
        CoreChatData(dbData.session, dbData.messages, dbData.configs, inputState, streamState)
    }

    private val secondaryDataFlow = combine(
        characterDao.getAllCharacters(),
        userPersonaDao.getAllPersonas()
    ) { characters, personas ->
        Pair(characters, personas)
    }

    private data class PreferenceState(
        val showThinking: Boolean,
        val tokenCounter: Boolean,
        val isDebugEnabled: Boolean
    )

    private val preferencesFlow = combine(
        settingsRepository.showThinkingProcessFlow,
        settingsRepository.tokenCounterEnabledFlow,
        settingsRepository.debugLoggingEnabledFlow
    ) { showThinking, tokenCounter, isDebugEnabled ->
        DebugLogManager.setLoggingEnabled(isDebugEnabled)
        PreferenceState(showThinking, tokenCounter, isDebugEnabled)
    }

    private data class LocalManagerState(
        val status: LocalModelStatus,
        val modelName: String?,
        val memoryMb: Double,
        val progress: Float
    )

    private val localManagerStateFlow = combine(
        LocalModelManager.status,
        LocalModelManager.loadedModelName,
        LocalModelManager.allocatedMemoryMb,
        LocalModelManager.loadingProgress
    ) { status, modelName, memoryMb, progress ->
        LocalManagerState(status, modelName, memoryMb, progress)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        coreDataFlow,
        secondaryDataFlow,
        preferencesFlow,
        _localError,
        localManagerStateFlow
    ) { core, (characters, personas), prefs, localError, localState ->
        val character = characters.find { it.id == core.session?.characterId }
        val persona = if (!core.session?.personaId.isNullOrBlank()) {
            personas.find { it.id == core.session?.personaId } ?: personas.find { it.isDefault } ?: personas.firstOrNull()
        } else {
            personas.find { it.isDefault } ?: personas.firstOrNull()
        }

        val currentConfig = if (!core.session?.configId.isNullOrBlank()) {
            core.configs.find { it.id == core.session?.configId }
                ?: core.configs.find { it.isActive }
                ?: core.configs.firstOrNull()
        } else {
            core.configs.find { it.isActive } ?: core.configs.firstOrNull()
        }

        val isUsingLocal = (currentConfig?.useLocalGenModel == true) || (currentConfig?.useLocalVisionModel == true)
        val maxTokens = currentConfig?.maxTokens ?: 2048

        // Calculate estimated context tokens based on active context budget
        val systemPromptTokens = TokenUtils.estimateTokenCount(character?.description ?: "") +
                TokenUtils.estimateTokenCount(character?.personality ?: "") +
                TokenUtils.estimateTokenCount(character?.scenario ?: "") +
                TokenUtils.estimateTokenCount(character?.impression ?: "") +
                TokenUtils.estimateTokenCount(persona?.personaDescription ?: "") + 35

        val (_, totalContextTokens) = TokenUtils.trimHistoryToFitBudget(
            chatHistory = core.messages,
            systemPromptTokens = systemPromptTokens,
            maxContextTokens = maxTokens
        )

        ChatUiState(
            session = core.session,
            character = character,
            persona = persona,
            allPersonas = personas,
            messages = core.messages,
            configs = core.configs,
            activeConfig = currentConfig,
            inputText = core.inputState.input,
            selectedImageUri = core.inputState.imageUri,
            isProcessingImage = core.inputState.isProcessing,
            isStreaming = core.streamState.isStreaming,
            streamingThinking = core.streamState.thinking,
            streamingText = core.streamState.text,
            isLiveThinkingExpanded = core.streamState.isExpanded,
            isShowThinkingEnabled = prefs.showThinking,
            isTokenCounterEnabled = prefs.tokenCounter,
            isDebugLoggingEnabled = prefs.isDebugEnabled,
            estimatedContextTokens = totalContextTokens,
            streamingTokensCount = core.streamState.tokensCount,
            streamingSpeedTps = core.streamState.speedTps,
            streamingModelName = core.streamState.modelName,
            errorMessage = core.streamState.errorMessage ?: localError,
            localModelStatus = localState.status,
            loadedLocalModelName = localState.modelName,
            isUsingLocalModel = isUsingLocal,
            localModelMemoryMb = localState.memoryMb,
            localModelLoadingProgress = localState.progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun updateChatSessionSettings(title: String, personaId: String, configId: String) {
        viewModelScope.launch {
            chatSessionDao.updateSessionSettings(
                id = sessionId,
                personaId = personaId,
                configId = configId,
                title = title.trim().ifBlank { "Chat Session" },
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun toggleLiveThinkingExpanded() {
        _isLiveThinkingExpanded.value = !_isLiveThinkingExpanded.value
    }

    fun onInputTextChanged(text: String) { _inputText.value = text }

    fun onImageSelected(uri: String?) {
        _selectedImageUri.value = uri
        _isProcessingImage.value = false
    }

    fun processImageAttachment(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) {
            _selectedImageUri.value = null
            _isProcessingImage.value = false
            return
        }

        _selectedImageUri.value = uriString
        _isProcessingImage.value = true

        if (uriString.startsWith("data:image/")) {
            _isProcessingImage.value = false
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val processed = ImageUtils.saveChatImageLocally(context, Uri.parse(uriString))
                _selectedImageUri.value = processed ?: uriString
            } catch (e: Exception) {
                _selectedImageUri.value = uriString
            } finally {
                _isProcessingImage.value = false
            }
        }
    }

    fun selectConfig(configId: String) {
        viewModelScope.launch {
            chatSessionDao.updateSessionConfig(sessionId, configId, System.currentTimeMillis())
        }
    }

    fun loadLocalModel(context: Context) {
        val config = uiState.value.activeConfig ?: return
        val chosenModel = if (config.useLocalGenModel && config.generateModelId.isNotBlank()) {
            config.generateModelId
        } else if (config.useLocalVisionModel && config.visionModelId.isNotBlank()) {
            config.visionModelId
        } else {
            config.generateModelId
        }
        if (chosenModel.isNotBlank()) {
            viewModelScope.launch {
                LocalModelManager.loadModel(context, chosenModel)
            }
        }
    }

    fun unloadLocalModel() {
        LocalModelManager.unloadModel()
    }

    fun dismissError() {
        _localError.value = null
        ChatGenerationManager.dismissError(sessionId)
    }

    fun sendMessage(context: Context) {
        val currentState = uiState.value
        val text = _inputText.value.trim()
        val rawImageUri = _selectedImageUri.value
        val character = currentState.character ?: return

        if (text.isBlank() && rawImageUri.isNullOrBlank()) return

        _inputText.value = ""
        _selectedImageUri.value = null
        _isProcessingImage.value = false

        viewModelScope.launch {
            val localImagePath = if (!rawImageUri.isNullOrBlank()) {
                if (rawImageUri.startsWith("/")) {
                    rawImageUri
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            ImageUtils.saveChatImageLocally(context, Uri.parse(rawImageUri)) ?: rawImageUri
                        } catch (e: Exception) {
                            rawImageUri
                        }
                    }
                }
            } else null

            val userTokens = TokenUtils.estimateTokenCount(text) + if (localImagePath != null) 768 else 0

            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                sender = "USER",
                content = text,
                imageUri = localImagePath,
                tokensCount = userTokens
            )

            chatMessageDao.insertMessage(userMsg)

            val config = currentState.activeConfig ?: InferenceConfigEntity()
            ChatGenerationManager.startGeneration(
                context = context.applicationContext,
                sessionId = sessionId,
                character = character,
                persona = currentState.persona,
                config = config,
                hasImage = !localImagePath.isNullOrBlank(),
                openAiRepository = openAiRepository,
                database = database,
                isShowThinking = currentState.isShowThinkingEnabled
            )
        }
    }

    fun regenerateResponse(context: Context) {
        val currentState = uiState.value
        val character = currentState.character ?: return

        viewModelScope.launch {
            val lastMsg = currentState.messages.lastOrNull()
            if (lastMsg != null && lastMsg.sender.equals("CHARACTER", ignoreCase = true)) {
                ImageUtils.deleteLocalFile(lastMsg.imageUri)
                chatMessageDao.deleteMessage(lastMsg)
            }

            val history = chatMessageDao.getMessagesForSessionSync(sessionId)
            val latestUserMsg = history.lastOrNull { it.sender.equals("USER", ignoreCase = true) }
            val hasImage = !latestUserMsg?.imageUri.isNullOrBlank()
            val config = currentState.activeConfig ?: InferenceConfigEntity()

            ChatGenerationManager.startGeneration(
                context = context.applicationContext,
                sessionId = sessionId,
                character = character,
                persona = currentState.persona,
                config = config,
                hasImage = hasImage,
                openAiRepository = openAiRepository,
                database = database,
                isShowThinking = currentState.isShowThinkingEnabled
            )
        }
    }

    fun stopStreaming() {
        ChatGenerationManager.stopGeneration(sessionId, database)
    }

    fun deleteMessage(msg: ChatMessageEntity) {
        viewModelScope.launch {
            ImageUtils.deleteLocalFile(msg.imageUri)
            chatMessageDao.deleteMessage(msg)
        }
    }

    fun deleteMessages(messageIds: Set<String>) {
        viewModelScope.launch {
            val msgs = chatMessageDao.getMessagesByIdsSync(messageIds.toList())
            msgs.forEach { ImageUtils.deleteLocalFile(it.imageUri) }
            chatMessageDao.deleteMessagesByIds(messageIds.toList())
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val msgs = chatMessageDao.getMessagesForSessionSync(sessionId)
            msgs.forEach {
                ImageUtils.deleteLocalFile(it.imageUri)
                chatMessageDao.deleteMessage(it)
            }

            // Re-seed character's firstMessage greeting if available
            val char = characterDao.getCharacterByIdSync(uiState.value.character?.id ?: "")
            val defaultGreeting = char?.firstMessage
            if (!defaultGreeting.isNullOrBlank()) {
                val greetingMsg = ChatMessageEntity(
                    sessionId = sessionId,
                    sender = "CHARACTER",
                    content = defaultGreeting
                )
                chatMessageDao.insertMessage(greetingMsg)
            }
        }
    }
}
