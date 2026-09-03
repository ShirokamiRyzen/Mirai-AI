package com.ryzumi.miraiai.ui.screen.character

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.ryzumi.miraiai.data.local.entity.ChatSessionEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    uiState: CharacterListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (ChatSessionEntity) -> Unit = {},
    onDeleteSessions: (Set<String>) -> Unit = {},
    onStartNewChatClick: (characterId: String, configId: String, personaId: String) -> Unit,
    onNavigateToManagement: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToModelHub: () -> Unit
) {
    var selectedSessionIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var isStartNewChatDialogOpen by remember { mutableStateOf(false) }
    var localSearchText by rememberSaveable { mutableStateOf(uiState.searchQuery) }

    val isSelectionMode = selectedSessionIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedSessionIds = emptySet()
    }

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != localSearchText) {
            localSearchText = uiState.searchQuery
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedSessionIds.size} Selected",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedSessionIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedSessionIds.size == uiState.chatSessions.size) {
                                selectedSessionIds = emptySet()
                            } else {
                                selectedSessionIds = uiState.chatSessions.map { it.session.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All"
                            )
                        }
                        IconButton(onClick = { showBulkDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Mirai AI",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToManagement) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = "Manage Characters & Personas"
                            )
                        }
                        IconButton(onClick = onNavigateToModelHub) {
                            Icon(
                                imageVector = Icons.Default.ModelTraining,
                                contentDescription = "Model Hub"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
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
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { isStartNewChatDialogOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat Session"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search chats...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
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

            if (uiState.chatSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No active chat sessions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to start a new chat with a character!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.characters.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = onNavigateToManagement) {
                                Text("Open Management to Create Characters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.chatSessions,
                        key = { it.session.id },
                        contentType = { "session" }
                    ) { item ->
                        val isSelected = selectedSessionIds.contains(item.session.id)
                        ChatSessionCardItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedSessionIds = if (isSelected) {
                                        selectedSessionIds - item.session.id
                                    } else {
                                        selectedSessionIds + item.session.id
                                    }
                                } else {
                                    onSessionClick(item.session.id)
                                }
                            },
                            onLongClick = {
                                selectedSessionIds = if (isSelected) {
                                    selectedSessionIds - item.session.id
                                } else {
                                    selectedSessionIds + item.session.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (isStartNewChatDialogOpen) {
        var selectedConfigId by remember(uiState.configs) {
            mutableStateOf(uiState.configs.firstOrNull()?.id ?: "")
        }
        var selectedPersonaId by remember(uiState.personas) {
            mutableStateOf(uiState.personas.firstOrNull()?.id ?: "")
        }
        var isConfigDropdownExpanded by remember { mutableStateOf(false) }
        var isPersonaDropdownExpanded by remember { mutableStateOf(false) }
        var newChatCharSearchQuery by remember { mutableStateOf("") }

        val availableChars = if (uiState.allCharacters.isNotEmpty()) uiState.allCharacters else uiState.characters
        val filteredDialogChars = remember(availableChars, newChatCharSearchQuery) {
            val q = newChatCharSearchQuery.trim()
            if (q.isBlank()) availableChars else {
                availableChars.filter {
                    it.name.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(q, ignoreCase = true) }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { isStartNewChatDialogOpen = false },
            title = { Text("Start New Chat Session") },
            text = {
                if (availableChars.isEmpty()) {
                    Text("No characters available. Please create one in Management first.")
                } else {
                    Column {
                        if (uiState.configs.isNotEmpty()) {
                            Text(
                                text = "Inference Config Profile:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isConfigDropdownExpanded,
                                onExpandedChange = { isConfigDropdownExpanded = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                val currentConfig = uiState.configs.find { it.id == selectedConfigId } ?: uiState.configs.first()
                                OutlinedTextField(
                                    value = currentConfig.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isConfigDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isConfigDropdownExpanded,
                                    onDismissRequest = { isConfigDropdownExpanded = false }
                                ) {
                                    uiState.configs.forEach { cfg ->
                                        DropdownMenuItem(
                                            text = { Text(cfg.name) },
                                            onClick = {
                                                selectedConfigId = cfg.id
                                                isConfigDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.personas.isNotEmpty()) {
                            Text(
                                text = "User Persona:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isPersonaDropdownExpanded,
                                onExpandedChange = { isPersonaDropdownExpanded = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                val currentPersona = uiState.personas.find { it.id == selectedPersonaId } ?: uiState.personas.first()
                                OutlinedTextField(
                                    value = currentPersona.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPersonaDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isPersonaDropdownExpanded,
                                    onDismissRequest = { isPersonaDropdownExpanded = false }
                                ) {
                                    uiState.personas.forEach { persona ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(persona.name, fontWeight = FontWeight.Bold)
                                                    if (persona.personaDescription.isNotBlank()) {
                                                        Text(
                                                            persona.personaDescription,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPersonaId = persona.id
                                                isPersonaDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Select Character:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = newChatCharSearchQuery,
                            onValueChange = { newChatCharSearchQuery = it },
                            placeholder = { Text("Search character...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )

                        if (filteredDialogChars.isEmpty()) {
                            Text(
                                text = "No characters found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(220.dp)
                            ) {
                                items(
                                    items = filteredDialogChars,
                                    key = { it.id },
                                    contentType = { "new_chat_char" }
                                ) { char ->
                                    val charAvatarModel = remember(char.avatarUri) {
                                        val uriStr = char.avatarUri
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
                                            .clickable {
                                                isStartNewChatDialogOpen = false
                                                onStartNewChatClick(char.id, selectedConfigId, selectedPersonaId)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (charAvatarModel != null) {
                                                AsyncImage(
                                                    model = charAvatarModel,
                                                    contentDescription = char.name,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = char.name,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = char.name,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                if (char.description.isNotBlank()) {
                                                    Text(
                                                        text = char.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (availableChars.isEmpty()) {
                    TextButton(
                        onClick = {
                            isStartNewChatDialogOpen = false
                            onNavigateToManagement()
                        }
                    ) {
                        Text("Open Management")
                    }
                } else {
                    TextButton(onClick = { isStartNewChatDialogOpen = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete ${selectedSessionIds.size} Chat Session${if (selectedSessionIds.size > 1) "s" else ""}?") },
            text = {
                Text("Are you sure you want to delete the selected chat sessions and their messages? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = selectedSessionIds
                        selectedSessionIds = emptySet()
                        showBulkDeleteDialog = false
                        onDeleteSessions(idsToDelete)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatSessionCardItem(
    item: ChatSessionItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val charName = item.character?.name ?: "Unknown Character"
    val avatarUri = item.character?.avatarUri
    val formattedTime = remember(item.timestamp) {
        SimpleDateFormat("HH:mm, MMM dd", Locale.getDefault()).format(Date(item.timestamp))
    }
    val avatarModel = remember(avatarUri) {
        if (avatarUri.isNullOrBlank()) null else {
            if (avatarUri.startsWith("data:image/")) {
                try {
                    val base64Part = avatarUri.substringAfter("base64,")
                    android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    avatarUri
                }
            } else if (avatarUri.startsWith("/")) {
                java.io.File(avatarUri)
            } else {
                avatarUri
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
                    contentDescription = charName,
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
                        contentDescription = charName,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = charName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.lastMessage ?: "Tap to start chatting...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }
        }
    }
}
