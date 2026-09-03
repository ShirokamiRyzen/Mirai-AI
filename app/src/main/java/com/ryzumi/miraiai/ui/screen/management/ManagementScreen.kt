package com.ryzumi.miraiai.ui.screen.management

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.ui.screen.persona.PersonaEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    characters: List<CharacterEntity>,
    personas: List<UserPersonaEntity>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onEditCharacterClick: (String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit = {},
    onDeleteCharacters: (Set<String>) -> Unit = {},
    onCreateCharacterClick: () -> Unit,
    onSavePersona: (id: String?, name: String, desc: String, avatar: String?, isDefault: Boolean) -> Unit,
    onSetDefaultPersona: (String) -> Unit,
    onDeletePersona: (UserPersonaEntity) -> Unit = {},
    onDeletePersonas: (Set<String>) -> Unit = {},
    onBackClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCharIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedPersonaIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showBulkCharDeleteDialog by remember { mutableStateOf(false) }
    var showBulkPersonaDeleteDialog by remember { mutableStateOf(false) }

    var personaToEdit by remember { mutableStateOf<UserPersonaEntity?>(null) }
    var isCreatingPersona by remember { mutableStateOf(false) }
    var localSearchText by rememberSaveable { mutableStateOf(searchQuery) }

    val isCharSelectionMode = selectedTabIndex == 0 && selectedCharIds.isNotEmpty()
    val isPersonaSelectionMode = selectedTabIndex == 1 && selectedPersonaIds.isNotEmpty()

    BackHandler(enabled = isCharSelectionMode || isPersonaSelectionMode) {
        if (isCharSelectionMode) selectedCharIds = emptySet()
        if (isPersonaSelectionMode) selectedPersonaIds = emptySet()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != localSearchText) {
            localSearchText = searchQuery
        }
    }

    Scaffold(
        topBar = {
            if (isCharSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedCharIds.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedCharIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedCharIds = if (selectedCharIds.size == characters.size) emptySet() else characters.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showBulkCharDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else if (isPersonaSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedPersonaIds.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedPersonaIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedPersonaIds = if (selectedPersonaIds.size == personas.size) emptySet() else personas.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showBulkPersonaDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                TopAppBar(
                    title = { Text("Management", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isCharSelectionMode && !isPersonaSelectionMode) {
                if (selectedTabIndex == 0) {
                    FloatingActionButton(
                        onClick = onCreateCharacterClick,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Character")
                    }
                } else {
                    FloatingActionButton(
                        onClick = { isCreatingPersona = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Persona")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        selectedPersonaIds = emptySet()
                    },
                    text = { Text("Characters (${characters.size})") },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        selectedCharIds = emptySet()
                    },
                    text = { Text("User Personas (${personas.size})") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }

            if (selectedTabIndex == 0) {
                // Characters Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = localSearchText,
                        onValueChange = {
                            localSearchText = it
                            onSearchQueryChanged(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        placeholder = { Text("Search characters...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Characters"
                            )
                        },
                        trailingIcon = {
                            if (localSearchText.isNotEmpty()) {
                                IconButton(onClick = {
                                    localSearchText = ""
                                    onSearchQueryChanged("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp)
                    )

                    if (characters.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (localSearchText.isBlank()) "No characters found. Tap + to create one!" else "No characters match '$localSearchText'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = characters,
                                key = { it.id },
                                contentType = { "character" }
                            ) { character ->
                                val isSelected = selectedCharIds.contains(character.id)
                                CharacterCardItem(
                                    character = character,
                                    isSelected = isSelected,
                                    isSelectionMode = isCharSelectionMode,
                                    onClick = {
                                        if (isCharSelectionMode) {
                                            selectedCharIds = if (isSelected) selectedCharIds - character.id else selectedCharIds + character.id
                                        } else {
                                            onEditCharacterClick(character.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectedCharIds = if (isSelected) selectedCharIds - character.id else selectedCharIds + character.id
                                    },
                                    onEditClick = { onEditCharacterClick(character.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Personas Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = localSearchText,
                        onValueChange = {
                            localSearchText = it
                            onSearchQueryChanged(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        placeholder = { Text("Search user personas...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Personas"
                            )
                        },
                        trailingIcon = {
                            if (localSearchText.isNotEmpty()) {
                                IconButton(onClick = {
                                    localSearchText = ""
                                    onSearchQueryChanged("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp)
                    )

                    if (personas.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (localSearchText.isBlank()) "No user personas found. Tap + to create one!" else "No personas match '$localSearchText'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = personas,
                                key = { it.id },
                                contentType = { "persona" }
                            ) { persona ->
                                val isSelected = selectedPersonaIds.contains(persona.id)
                                ManagementPersonaCardItem(
                                    persona = persona,
                                    isSelected = isSelected,
                                    isSelectionMode = isPersonaSelectionMode,
                                    onClick = {
                                        if (isPersonaSelectionMode) {
                                            selectedPersonaIds = if (isSelected) selectedPersonaIds - persona.id else selectedPersonaIds + persona.id
                                        } else {
                                            personaToEdit = persona
                                        }
                                    },
                                    onLongClick = {
                                        selectedPersonaIds = if (isSelected) selectedPersonaIds - persona.id else selectedPersonaIds + persona.id
                                    },
                                    onSetDefault = { onSetDefaultPersona(persona.id) },
                                    onEdit = { personaToEdit = persona }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreatingPersona || personaToEdit != null) {
        PersonaEditDialog(
            persona = personaToEdit,
            onDismiss = {
                isCreatingPersona = false
                personaToEdit = null
            },
            onSave = { name, desc, avatar, isDefault ->
                onSavePersona(personaToEdit?.id, name, desc, avatar, isDefault)
                isCreatingPersona = false
                personaToEdit = null
            }
        )
    }

    if (showBulkCharDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkCharDeleteDialog = false },
            title = { Text("Delete ${selectedCharIds.size} Character${if (selectedCharIds.size > 1) "s" else ""}?") },
            text = { Text("Are you sure you want to delete the selected character(s)? This will also remove their associated chats.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selectedCharIds
                        selectedCharIds = emptySet()
                        showBulkCharDeleteDialog = false
                        onDeleteCharacters(ids)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkCharDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBulkPersonaDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkPersonaDeleteDialog = false },
            title = { Text("Delete ${selectedPersonaIds.size} Persona${if (selectedPersonaIds.size > 1) "s" else ""}?") },
            text = { Text("Are you sure you want to delete the selected user persona(s)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selectedPersonaIds
                        selectedPersonaIds = emptySet()
                        showBulkPersonaDeleteDialog = false
                        onDeletePersonas(ids)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkPersonaDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterCardItem(
    character: CharacterEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val avatarModel = remember(character.avatarUri) {
        val uriStr = character.avatarUri
        if (uriStr.isNullOrBlank()) null else {
            if (uriStr.startsWith("data:image/")) {
                try {
                    val base64Part = uriStr.substringAfter("base64,")
                    android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    uriStr
                }
            } else if (uriStr.startsWith("/")) {
                java.io.File(uriStr)
            } else {
                uriStr
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarModel != null) {
                AsyncImage(
                    model = avatarModel,
                    contentDescription = character.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = character.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (character.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = character.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (character.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        character.tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Character"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManagementPersonaCardItem(
    persona: UserPersonaEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit
) {
    val avatarModel = remember(persona.avatarUri) {
        val uriStr = persona.avatarUri
        if (uriStr.isNullOrBlank()) null else {
            if (uriStr.startsWith("data:image/")) {
                try {
                    val base64Part = uriStr.substringAfter("base64,")
                    android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    uriStr
                }
            } else if (uriStr.startsWith("/")) {
                java.io.File(uriStr)
            } else {
                uriStr
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarModel != null) {
                AsyncImage(
                    model = avatarModel,
                    contentDescription = persona.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = persona.name,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = persona.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (persona.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(Default)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (persona.personaDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = persona.personaDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Persona")
                }
            }
        }
    }
}
