package com.ryzumi.miraiai.ui.screen.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryzumi.miraiai.data.local.dao.UserPersonaDao
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.ryzumi.miraiai.domain.util.ImageUtils
import kotlinx.coroutines.launch
import java.util.UUID

class PersonaViewModel(
    private val personaDao: UserPersonaDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val personas: StateFlow<List<UserPersonaEntity>> = combine(
        personaDao.getAllPersonas(),
        _searchQuery
    ) { list, query ->
        val trimmed = query.trim()
        if (trimmed.isBlank()) list else {
            list.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.personaDescription.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun savePersona(
        id: String?,
        name: String,
        description: String,
        avatarUri: String?,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            val personaId = if (id.isNullOrBlank()) UUID.randomUUID().toString() else id
            val entity = UserPersonaEntity(
                id = personaId,
                name = name.trim(),
                avatarUri = avatarUri,
                personaDescription = description.trim(),
                isDefault = isDefault
            )
            personaDao.insertPersona(entity)
            if (isDefault) {
                personaDao.clearDefaultFlags()
                personaDao.setDefaultFlag(personaId)
            }
        }
    }

    fun setDefault(personaId: String) {
        viewModelScope.launch {
            personaDao.clearDefaultFlags()
            personaDao.setDefaultFlag(personaId)
        }
    }

    fun deletePersona(persona: UserPersonaEntity) {
        viewModelScope.launch {
            ImageUtils.deleteLocalFile(persona.avatarUri)
            personaDao.deletePersona(persona)
        }
    }

    fun deletePersonas(personaIds: Set<String>) {
        viewModelScope.launch {
            for (id in personaIds) {
                val persona = personaDao.getPersonaByIdSync(id)
                if (persona != null) {
                    ImageUtils.deleteLocalFile(persona.avatarUri)
                }
                personaDao.deletePersonaById(id)
            }
        }
    }
}
