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
    val localModelLoadingProgress: Float = 0f,
    val isLive2dMode: Boolean = false,
    val currentEmotion: String = "neutral",
    val currentMotion: String? = null,
    val motionTrigger: Long = 0L,
    val touchReactionText: String? = null,
    val touchReactionZone: String? = null
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
    private val _manualEmotion = MutableStateFlow<String?>(null)
    private val _currentMotion = MutableStateFlow<String?>(null)
    private val _motionTrigger = MutableStateFlow(0L)
    private val _touchReactionText = MutableStateFlow<String?>(null)
    private val _touchReactionZone = MutableStateFlow<String?>(null)
    private var touchReactionJob: kotlinx.coroutines.Job? = null

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

    private data class UiAuxState(
        val localError: String?,
        val manualEmotion: String?,
        val touchReactionText: String?,
        val touchReactionZone: String?,
        val motion: String?,
        val motionTrigger: Long
    )

    private val uiAuxStateFlow = combine(
        combine(_localError, _manualEmotion, _touchReactionText) { err, emo, txt -> Triple(err, emo, txt) },
        combine(_touchReactionZone, _currentMotion, _motionTrigger) { zone, mot, trg -> Triple(zone, mot, trg) }
    ) { (err, emo, txt), (zone, mot, trg) ->
        UiAuxState(err, emo, txt, zone, mot, trg)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        coreDataFlow,
        secondaryDataFlow,
        preferencesFlow,
        uiAuxStateFlow,
        localManagerStateFlow
    ) { core, (characters, personas), prefs, auxState, localState ->
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

        val resolvedEmotion = auxState.manualEmotion
            ?: if (core.streamState.isStreaming && core.streamState.text.isNotBlank()) {
                detectEmotionFromText(core.streamState.text)
            } else {
                val lastMsg = core.messages.lastOrNull { it.sender.equals("CHARACTER", ignoreCase = true) }
                if (lastMsg != null) detectEmotionFromText(lastMsg.content) else "neutral"
            }

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
            errorMessage = core.streamState.errorMessage ?: auxState.localError,
            localModelStatus = localState.status,
            loadedLocalModelName = localState.modelName,
            isUsingLocalModel = isUsingLocal,
            localModelMemoryMb = localState.memoryMb,
            localModelLoadingProgress = localState.progress,
            isLive2dMode = core.session?.isLive2dMode ?: false,
            currentEmotion = resolvedEmotion,
            currentMotion = auxState.motion,
            motionTrigger = auxState.motionTrigger,
            touchReactionText = auxState.touchReactionText,
            touchReactionZone = auxState.touchReactionZone
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        viewModelScope.launch {
            var lastEvaluatedMsgId: String? = null
            var currentStreamEvaluated = false

            coreDataFlow.collect { core ->
                if (core.streamState.isStreaming) {
                    val streamText = core.streamState.text
                    if (streamText.isNotBlank() && !currentStreamEvaluated) {
                        val motion = detectMotionFromText(streamText)
                        if (motion != null) {
                            currentStreamEvaluated = true
                            _currentMotion.value = motion
                            _motionTrigger.value = System.currentTimeMillis()
                        }
                    }
                } else {
                    val lastMsg = core.messages.lastOrNull { it.sender.equals("CHARACTER", ignoreCase = true) }
                    if (currentStreamEvaluated) {
                        currentStreamEvaluated = false
                        if (lastMsg != null) {
                            lastEvaluatedMsgId = lastMsg.id
                        }
                    } else if (lastMsg != null && lastMsg.id != lastEvaluatedMsgId) {
                        lastEvaluatedMsgId = lastMsg.id
                        val motion = detectMotionFromText(lastMsg.content)
                        if (motion != null) {
                            _currentMotion.value = motion
                            _motionTrigger.value = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    fun onLive2dTouched(zone: String) {
        val validZones = setOf("head", "chest", "groin", "hands", "legs")
        if (zone !in validZones) return

        viewModelScope.launch {
            val reactionText = when (zone) {
                "head" -> listOf(
                    "Hehe, patting my head feels so nice...",
                    "Being petted like this makes me feel so relaxed...",
                    "Please pet me more~ Hehe"
                ).random()
                "chest" -> listOf(
                    "Ah...! Touching there is embarrassing...",
                    "Uh... touching my chest... please be gentle...",
                    "My heart is beating so fast right now..."
                ).random()
                "groin" -> listOf(
                    "Ah...! P-please don't touch there, it's very sensitive...",
                    "You're being so naughty touching under my skirt...",
                    "P-please stop... or things might get out of hand..."
                ).random()
                "hands" -> listOf(
                    "Your hand is so warm... please don't let go~",
                    "I love it when you hold my hand like this...",
                    "Ehehe, I want to hold hands with you forever~"
                ).random()
                "legs" -> listOf(
                    "Ehh, are you trying to tickle my legs? Haha~",
                    "It tickles when you touch my legs~",
                    "Don't tease my legs like that~ hehe"
                ).random()
                else -> return@launch
            }

            val touchEmotion = when (zone) {
                "head" -> "happy"
                "chest" -> "shy"
                "groin" -> "love"
                "hands" -> "love"
                "legs" -> "playful"
                else -> "happy"
            }

            val touchMotion = when (zone) {
                "head" -> "happy"
                "chest" -> "shy"
                "groin" -> "special"
                "hands" -> "love"
                "legs" -> "playful"
                else -> "tap_body"
            }

            _manualEmotion.value = touchEmotion
            _currentMotion.value = touchMotion
            _motionTrigger.value = System.currentTimeMillis()
            _touchReactionText.value = reactionText
            _touchReactionZone.value = zone

            touchReactionJob?.cancel()
            touchReactionJob = launch {
                kotlinx.coroutines.delay(4000)
                _touchReactionText.value = null
                _touchReactionZone.value = null
                _manualEmotion.value = null
                _currentMotion.value = null
            }
        }
    }

    private fun detectEmotionFromText(text: String): String {
        // 0. Explicit expression tags: [expression:name] or <expression:name> or [emotion:name]
        val expTagRegex = Regex("""\[(?:expression|emotion)\s*[:=]\s*([a-zA-Z0-9_-]+)\s*\]|<(?:expression|emotion)\s*[:=]\s*([a-zA-Z0-9_-]+)\s*>""", RegexOption.IGNORE_CASE)
        val expMatch = expTagRegex.find(text)
        if (expMatch != null) {
            val tagVal = expMatch.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            if (!tagVal.isNullOrBlank()) return tagVal
        }

        // If an explicit motion is present without an explicit expression tag, harmonize facial emotion
        val motionTagRegex = Regex("""\[motion\s*[:=]\s*([a-zA-Z0-9_-]+)\s*\]|<motion\s*[:=]\s*([a-zA-Z0-9_-]+)\s*>""", RegexOption.IGNORE_CASE)
        val motionMatch = motionTagRegex.find(text)
        if (motionMatch != null) {
            val mVal = motionMatch.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.lowercase() ?: ""
            if (mVal.contains("dance") || mVal.contains("wave") || mVal.contains("jump")) {
                return "happy"
            }
            if (mVal.contains("pose")) {
                return "playful"
            }
        }

        val lower = text.lowercase()

        // 1. Angry / Frustrated
        if (lower.contains("marah") || lower.contains("kesal") || lower.contains("sebal") ||
            lower.contains("ngambek") || lower.contains("jahat") || lower.contains("benci") ||
            lower.contains("jangan begitu") || lower.contains("huff") || lower.contains("hmpf") ||
            lower.contains("angry") || lower.contains("annoyed") || lower.contains("baka") ||
            text.contains("😡") || text.contains("💢") || text.contains("😤") ||
            text.contains("(¬_¬)") || text.contains("(>_<)") || text.contains("（｀ー´）")
        ) {
            return "angry"
        }

        // 2. Sad / Crying
        if (lower.contains("sedih") || lower.contains("nangis") || lower.contains("menangis") ||
            lower.contains("kecewa") || lower.contains("maaf") || lower.contains("hiks") ||
            lower.contains("kasihan") || lower.contains("terluka") || lower.contains("sorry") ||
            lower.contains("sad") || lower.contains("cry") || lower.contains("tears") ||
            text.contains("😢") || text.contains("😭") || text.contains("🥺") ||
            text.contains("(T_T)") || text.contains("(つД`)") || text.contains("(；ω；)")
        ) {
            return "sad"
        }

        // 3. Shy / Embarrassed
        if (lower.contains("malu") || lower.contains("merona") || lower.contains("blush") ||
            lower.contains("deg-degan") || lower.contains("deg degan") || lower.contains("kya") ||
            lower.contains("shy") || lower.contains("embarrassed") ||
            text.contains("(///)") || text.contains("(//∇//)") || text.contains("(⁄ ⁄•⁄ω⁄•⁄ ⁄)") ||
            text.contains(">///<") || text.contains("😳") || text.contains("⁄(⁄ ⁄•⁄-⁄•⁄ ⁄)⁄")
        ) {
            return "shy"
        }

        // 4. Love / Affection
        if (lower.contains("sayang") || lower.contains("suamiku") || lower.contains("cinta") ||
            lower.contains("peluk") || lower.contains("cium") || lower.contains("muach") ||
            lower.contains("pacar") || lower.contains("manja") || lower.contains("love") ||
            text.contains("❤️") || text.contains("💕") || text.contains("💖") ||
            text.contains("🥰") || text.contains("😍") || text.contains("(*´▽`*)") ||
            text.contains("(♡)") || text.contains("(´∀｀)")
        ) {
            return "love"
        }

        // 5. Playful / Teasing
        if (lower.contains("bercanda") || lower.contains("iseng") || lower.contains("jahil") ||
            lower.contains("bleh") || lower.contains("bleeeh") || lower.contains("wkwk") ||
            lower.contains("tebak") || lower.contains("godain") || lower.contains("playful") ||
            text.contains("😜") || text.contains("😋") || text.contains("😏") ||
            text.contains("(¬‿¬)") || text.contains("(^з^)-☆")
        ) {
            return "playful"
        }

        // 6. Surprised / Shocked
        if (lower.contains("kaget") || lower.contains("apa?!") || lower.contains("hah?!") ||
            lower.contains("loh?!") || lower.contains("beneran?") || lower.contains("serius?") ||
            lower.contains("omg") || lower.contains("astaga") || lower.contains("wah") ||
            lower.contains("surprised") || lower.contains("shock") ||
            text.contains("😲") || text.contains("😱") || text.contains("!?!") ||
            text.contains("(・o・)") || text.contains("(゜o゜)")
        ) {
            return "surprised"
        }

        // 7. Happy / Cheerful
        if (lower.contains("senang") || lower.contains("bahagia") || lower.contains("gembira") ||
            lower.contains("hehe") || lower.contains("ehehe") || lower.contains("yay") ||
            lower.contains("terima kasih") || lower.contains("makasih") || lower.contains("suka") ||
            lower.contains("happy") || lower.contains("glad") ||
            text.contains("😊") || text.contains("😄") || text.contains("✨") ||
            text.contains("(^o^)") || text.contains("(ﾉ◕ヮ◕)ﾉ") || text.contains("(´ω｀*)")
        ) {
            return "happy"
        }

        return "neutral"
    }

    private fun detectMotionFromText(text: String): String? {
        if (text.isBlank()) return null

        // 1. Explicit motion tag: [motion:group] or <motion:group>
        val tagRegex = Regex("""\[motion\s*[:=]\s*([a-zA-Z0-9_-]+)\s*\]|<motion\s*[:=]\s*([a-zA-Z0-9_-]+)\s*>""", RegexOption.IGNORE_CASE)
        val match = tagRegex.find(text)
        if (match != null) {
            val group = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            if (!group.isNullOrBlank()) return group
        }

        // If an explicit expression tag is present without any explicit motion tag,
        // user/AI intended an expression only. DO NOT trigger motion!
        val expTagRegex = Regex("""\[(?:expression|emotion)\s*[:=]\s*[^\]]+\]|<(?:expression|emotion)\s*[:=]\s*[^>]+>""", RegexOption.IGNORE_CASE)
        if (expTagRegex.containsMatchIn(text)) {
            return null
        }

        val lower = text.lowercase()

        // 2. Action in asterisks (e.g. *melambaikan tangan*, *menari*, *dances*, *poses*)
        val actionRegex = Regex("""\*([^*]+)\*""")
        val actions = actionRegex.findAll(lower).map { it.groupValues[1] }.toList()
        for (action in actions) {
            if (action.contains("lamba") || action.contains("wave") || action.contains("sapa")) return "wave"
            if (action.contains("tari") || action.contains("dance") || action.contains("joget") || action.contains("goyang")) return "dance"
            if (action.contains("pose") || action.contains("gaya")) return "pose"
            if (action.contains("angguk") || action.contains("nod")) return "nod"
            if (action.contains("lompat") || action.contains("jump")) return "jump"
        }

        // 3. Natural conversational cues in character speech
        if (lower.contains("melambaikan tangan") || lower.contains("lambaikan tangan") || lower.contains("waving my hand") || lower.contains("waves at you")) {
            return "wave"
        }
        if (lower.contains("menari") || lower.contains("joget") || lower.contains("dancing for you") || lower.contains("goyang") || lower.contains("menari dengan")) {
            return "dance"
        }
        if (lower.contains("lihat pose") || lower.contains("bergaya") || lower.contains("strike a pose")) {
            return "pose"
        }
        if (lower.contains("mengangguk") || lower.contains("nodding")) {
            return "nod"
        }
        if (lower.contains("melompat") || lower.contains("jumping")) {
            return "jump"
        }
        if (lower.contains("ini gerakanku") || lower.contains("lihat gerakanku") || lower.contains("bergerak untukmu") || lower.contains("moving for you") || lower.contains("gerakan bebas")) {
            return "dance"
        }

        return null
    }

    fun toggleLive2dMode() {
        val currentSession = uiState.value.session ?: return
        val newMode = !currentSession.isLive2dMode
        viewModelScope.launch {
            chatSessionDao.updateLive2dMode(
                id = currentSession.id,
                isLive2dMode = newMode
            )
        }
    }

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
            val latestUserMsg = history.lastOrNull { it.sender.equals("USER", ignoreCase = true) } ?: return@launch
            val hasImage = !latestUserMsg.imageUri.isNullOrBlank()
            val config = currentState.activeConfig ?: InferenceConfigEntity()

            dismissError()

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
