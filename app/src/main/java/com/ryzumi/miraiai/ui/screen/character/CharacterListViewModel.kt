package com.ryzumi.miraiai.ui.screen.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.ryzumi.miraiai.domain.macro.MacroEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatSessionItem(
    val session: ChatSessionEntity,
    val character: CharacterEntity?,
    val lastMessage: String?,
    val timestamp: Long
)

data class CharacterListUiState(
    val characters: List<CharacterEntity> = emptyList(),
    val allCharacters: List<CharacterEntity> = emptyList(),
    val chatSessions: List<ChatSessionItem> = emptyList(),
    val configs: List<InferenceConfigEntity> = emptyList(),
    val personas: List<UserPersonaEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class CharacterListViewModel(
    private val characterDao: CharacterDao,
    private val chatSessionDao: ChatSessionDao,
    private val chatMessageDao: ChatMessageDao,
    private val userPersonaDao: UserPersonaDao,
    private val inferenceConfigDao: InferenceConfigDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CharacterListUiState> = combine(
        combine(
            chatSessionDao.getAllSessions(),
            characterDao.getAllCharacters(),
            inferenceConfigDao.getAllConfigs()
        ) { sessions, characters, configs ->
            Triple(sessions, characters, configs)
        },
        userPersonaDao.getAllPersonas(),
        chatMessageDao.getAllMessages(),
        _searchQuery
    ) { (sessions, characters, configs), personas, messages, query ->
        val trimmedQuery = query.trim()

        // 1. In-memory map of latest message per session
        val lastMessageMap = mutableMapOf<String, ChatMessageEntity>()
        for (msg in messages) {
            val existing = lastMessageMap[msg.sessionId]
            if (existing == null || msg.timestamp >= existing.timestamp) {
                lastMessageMap[msg.sessionId] = msg
            }
        }

        // 2. Character lookup map
        val characterMap = characters.associateBy { it.id }

        // 3. Filtered Chat Sessions
        val items = sessions.map { session ->
            val char = characterMap[session.characterId]
            val lastMsgEntity = lastMessageMap[session.id]
            val lastMsg = lastMsgEntity?.content ?: char?.firstMessage
            val time = lastMsgEntity?.timestamp ?: session.updatedAt
            ChatSessionItem(
                session = session,
                character = char,
                lastMessage = lastMsg,
                timestamp = time
            )
        }.filter { item ->
            if (trimmedQuery.isBlank()) true else {
                item.character?.name?.contains(trimmedQuery, ignoreCase = true) == true ||
                item.lastMessage?.contains(trimmedQuery, ignoreCase = true) == true ||
                item.character?.tags?.any { it.contains(trimmedQuery, ignoreCase = true) } == true
            }
        }

        // 4. Filtered Characters for search
        val filteredCharacters = characters.filter { char ->
            if (trimmedQuery.isBlank()) true else {
                char.name.contains(trimmedQuery, ignoreCase = true) ||
                char.description.contains(trimmedQuery, ignoreCase = true) ||
                char.personality.contains(trimmedQuery, ignoreCase = true) ||
                char.tags.any { it.contains(trimmedQuery, ignoreCase = true) }
            }
        }

        CharacterListUiState(
            characters = filteredCharacters,
            allCharacters = characters,
            chatSessions = items,
            configs = configs,
            personas = personas,
            searchQuery = query,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CharacterListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            characterDao.deleteCharacter(character)
        }
    }

    fun deleteCharacters(characterIds: Set<String>) {
        viewModelScope.launch {
            for (id in characterIds) {
                characterDao.deleteCharacterById(id)
            }
        }
    }

    fun deleteSession(session: ChatSessionEntity) {
        viewModelScope.launch {
            chatSessionDao.deleteSession(session)
            chatMessageDao.deleteMessagesForSession(session.id)
        }
    }

    fun deleteSessions(sessionIds: Set<String>) {
        viewModelScope.launch {
            for (id in sessionIds) {
                chatSessionDao.deleteSessionById(id)
                chatMessageDao.deleteMessagesForSession(id)
            }
        }
    }

    private suspend fun insertFirstMessageIfPresent(sessionId: String, characterId: String, personaId: String) {
        val character = characterDao.getCharacterByIdSync(characterId) ?: return
        if (character.firstMessage.isNotBlank()) {
            val persona = if (personaId.isNotBlank()) userPersonaDao.getPersonaByIdSync(personaId) else userPersonaDao.getDefaultPersonaSync()
            val charName = character.name.ifBlank { "Character" }
            val userName = persona?.name?.ifBlank { "User" } ?: "User"
            val processed = MacroEngine.processMacros(character.firstMessage, charName, userName)

            val greetingMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                sender = "CHARACTER",
                content = processed
            )
            chatMessageDao.insertMessage(greetingMsg)
        }
    }

    suspend fun createNewChatSession(characterId: String, configId: String = "", personaId: String = ""): String {
        val selectedConfig = configId.ifBlank { uiState.value.configs.firstOrNull()?.id ?: "" }
        val selectedPersona = personaId.ifBlank { uiState.value.personas.firstOrNull()?.id ?: "" }
        val newSessionId = UUID.randomUUID().toString()
        val newSession = ChatSessionEntity(
            id = newSessionId,
            characterId = characterId,
            personaId = selectedPersona,
            configId = selectedConfig,
            title = "Chat Session"
        )
        chatSessionDao.insertSession(newSession)
        insertFirstMessageIfPresent(newSessionId, characterId, selectedPersona)
        return newSessionId
    }
}
