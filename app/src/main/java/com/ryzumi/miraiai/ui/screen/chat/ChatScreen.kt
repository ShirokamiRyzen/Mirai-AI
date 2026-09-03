package com.ryzumi.miraiai.ui.screen.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.network.DebugLogManager
import com.ryzumi.miraiai.ui.screen.settings.DebugLogCardItem
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.ryzumi.miraiai.domain.model.LocalModelStatus

private suspend fun LazyListState.scrollToBottom() {
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems <= 0) return

    val lastIndex = totalItems - 1
    val visibleItems = layoutInfo.visibleItemsInfo
    val lastVisible = visibleItems.lastOrNull { it.index == lastIndex }

    if (lastVisible == null) {
        scrollToItem(lastIndex)
        val updatedLast = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex }
        if (updatedLast != null) {
            val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            val delta = (updatedLast.offset + updatedLast.size) - viewportBottom
            if (delta > 0) {
                scrollBy(delta.toFloat())
            }
        }
    } else {
        val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        val delta = (lastVisible.offset + lastVisible.size) - viewportBottom
        if (delta > 0) {
            scrollBy(delta.toFloat())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onInputTextChanged: (String) -> Unit,
    onImageSelected: (String?) -> Unit,
    onSelectModel: (String) -> Unit,
    onSendMessage: (Context) -> Unit,
    onRegenerateResponse: () -> Unit,
    onStopStreaming: () -> Unit,
    onLoadLocalModel: (Context) -> Unit = {},
    onUnloadLocalModel: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onToggleLiveThinkingExpanded: () -> Unit = {},
    onDeleteMessage: (ChatMessageEntity) -> Unit,
    onDeleteMessages: (Set<String>) -> Unit = {},
    onClearHistory: () -> Unit,
    onUpdateSessionSettings: (title: String, personaId: String, configId: String) -> Unit = { _, _, _ -> },
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var isModelPickerExpanded by remember { mutableStateOf(false) }
    var isTopMenuExpanded by remember { mutableStateOf(false) }
    var showDebugLogsDialog by remember { mutableStateOf(false) }
    var showEditChatDialog by remember { mutableStateOf(false) }
    var selectedMessageIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteMessagesDialog by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val debugLogs by DebugLogManager.logs.collectAsState()

    val isSelectionMode = selectedMessageIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedMessageIds = emptySet<String>()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it.toString()) }
    }

    val sessionId = uiState.session?.id
    androidx.compose.runtime.DisposableEffect(sessionId) {
        if (!sessionId.isNullOrBlank()) {
            com.ryzumi.miraiai.domain.engine.ChatGenerationManager.setActiveVisibleSession(sessionId)
            com.ryzumi.miraiai.domain.util.ChatNotificationHelper.cancelNotification(context, sessionId)
        }
        onDispose {
            com.ryzumi.miraiai.domain.engine.ChatGenerationManager.setActiveVisibleSession(null)
        }
    }

    // Auto-scroll to bottom on new messages or streaming status change
    LaunchedEffect(uiState.messages.size, uiState.isStreaming, uiState.streamingThinking) {
        listState.scrollToBottom()
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedMessageIds.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedMessageIds = emptySet<String>() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        if (selectedMessageIds.size == 1) {
                            val singleMsg = uiState.messages.find { it.id == selectedMessageIds.first() }
                            if (singleMsg != null) {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", singleMsg.content))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    selectedMessageIds = emptySet<String>()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Message")
                                }
                            }
                        }
                        IconButton(onClick = {
                            selectedMessageIds = if (selectedMessageIds.size == uiState.messages.size) emptySet<String>() else uiState.messages.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showBulkDeleteMessagesDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!uiState.character?.avatarUri.isNullOrBlank()) {
                            AsyncImage(
                                model = uiState.character?.avatarUri,
                                contentDescription = uiState.character?.name,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = uiState.character?.name ?: "Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Active Config / Model Chip Trigger
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                            .clickable { isModelPickerExpanded = true }
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.activeConfig?.name ?: "No Config",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Config",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isModelPickerExpanded,
                                        onDismissRequest = { isModelPickerExpanded = false }
                                    ) {
                                        uiState.configs.forEach { cfg ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = cfg.name,
                                                        fontWeight = if (cfg.id == uiState.activeConfig?.id) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    onSelectModel(cfg.id)
                                                    isModelPickerExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Token Counter Context Ratio Badge
                                if (uiState.isTokenCounterEnabled) {
                                    val maxTokens = uiState.activeConfig?.maxTokens ?: 2048
                                    val currentTokens = minOf(uiState.estimatedContextTokens, maxTokens)
                                    val ratio = (currentTokens.toFloat() / maxTokens.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val tokenColor = if (ratio > 0.9f) {
                                        MaterialTheme.colorScheme.error
                                    } else if (ratio > 0.75f) {
                                        Color(0xFFFFB74D)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tokenColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DataUsage,
                                            contentDescription = "Token Counter",
                                            tint = tokenColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$currentTokens / $maxTokens",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokenColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isTopMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options"
                        )
                    }

                    DropdownMenu(
                        expanded = isTopMenuExpanded,
                        onDismissRequest = { isTopMenuExpanded = false }
                    ) {
                        if (uiState.isUsingLocalModel) {
                            if (uiState.localModelStatus == LocalModelStatus.LOADED) {
                                DropdownMenuItem(
                                    text = { Text("Unload Local Model from RAM") },
                                    leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        isTopMenuExpanded = false
                                        onUnloadLocalModel()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Load Local Model into RAM") },
                                    leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        isTopMenuExpanded = false
                                        onLoadLocalModel(context)
                                    }
                                )
                            }
                        }

                        DropdownMenuItem(
                            text = { Text("Edit Chat Session") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                isTopMenuExpanded = false
                                showEditChatDialog = true
                            }
                        )

                        if (uiState.isDebugLoggingEnabled) {
                            DropdownMenuItem(
                                text = { Text("View Debug Request Logs") },
                                leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                                onClick = {
                                    isTopMenuExpanded = false
                                    showDebugLogsDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Clear Chat History") },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                            onClick = {
                                onClearHistory()
                                isTopMenuExpanded = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Local Model Active Status Banner
            if (uiState.isUsingLocalModel) {
                Surface(
                    color = when (uiState.localModelStatus) {
                        LocalModelStatus.LOADED -> Color(0xFF1B5E20).copy(alpha = 0.25f)
                        LocalModelStatus.LOADING -> Color(0xFFE65100).copy(alpha = 0.25f)
                        LocalModelStatus.UNLOADED, LocalModelStatus.ERROR -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = when (uiState.localModelStatus) {
                                    LocalModelStatus.LOADED -> Color(0xFF4CAF50)
                                    LocalModelStatus.LOADING -> Color(0xFFFF9800)
                                    LocalModelStatus.UNLOADED, LocalModelStatus.ERROR -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when (uiState.localModelStatus) {
                                        LocalModelStatus.LOADED -> "Local Model in RAM (~${String.format(Locale.US, "%.0f", uiState.localModelMemoryMb)} MB)"
                                        LocalModelStatus.LOADING -> "Loading into RAM (${(uiState.localModelLoadingProgress * 100).toInt()}%)..."
                                        LocalModelStatus.UNLOADED -> "Local Model Unloaded"
                                        LocalModelStatus.ERROR -> "Model Load Error"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (uiState.localModelStatus) {
                                        LocalModelStatus.LOADED -> Color(0xFF81C784)
                                        LocalModelStatus.LOADING -> Color(0xFFFFB74D)
                                        LocalModelStatus.UNLOADED, LocalModelStatus.ERROR -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (uiState.localModelStatus == LocalModelStatus.LOADED) {
                                    Text(
                                        text = uiState.loadedLocalModelName ?: "GGUF Model",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (uiState.localModelStatus == LocalModelStatus.LOADED) {
                            TextButton(
                                onClick = onUnloadLocalModel,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Unload", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        } else if (uiState.localModelStatus == LocalModelStatus.UNLOADED || uiState.localModelStatus == LocalModelStatus.ERROR) {
                            TextButton(
                                onClick = { onLoadLocalModel(context) },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Load Model", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (uiState.localModelStatus == LocalModelStatus.LOADING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // Error Message Banner (Dismissible & Compact)
            uiState.errorMessage?.let { errorMsg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Chat Messages History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { msg ->
                    val isMsgSelected = selectedMessageIds.contains(msg.id)
                    ChatBubbleItem(
                        message = msg,
                        characterName = uiState.character?.name ?: "AI",
                        isShowThinkingEnabled = uiState.isShowThinkingEnabled,
                        isTokenCounterEnabled = uiState.isTokenCounterEnabled,
                        isSelected = isMsgSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                selectedMessageIds = if (isMsgSelected) selectedMessageIds - msg.id else selectedMessageIds + msg.id
                            }
                        },
                        onLongClick = {
                            selectedMessageIds = if (isMsgSelected) selectedMessageIds - msg.id else selectedMessageIds + msg.id
                        },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Chat Message", msg.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { onDeleteMessage(msg) },
                        onImageClick = { previewImageUrl = it }
                    )
                }

                if (uiState.isStreaming) {
                    item(key = "streaming_bubble") {
                        StreamingBubbleItem(
                            streamingThinking = uiState.streamingThinking,
                            streamingText = uiState.streamingText,
                            streamingModelName = uiState.streamingModelName,
                            isThinkingExpanded = uiState.isLiveThinkingExpanded,
                            isShowThinkingEnabled = uiState.isShowThinkingEnabled,
                            isTokenCounterEnabled = uiState.isTokenCounterEnabled,
                            streamingTokensCount = uiState.streamingTokensCount,
                            streamingSpeedTps = uiState.streamingSpeedTps,
                            onToggleThinking = onToggleLiveThinkingExpanded,
                            characterName = uiState.character?.name ?: "AI",
                            listState = listState
                        )
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Selected Image Attachment Chip Preview with Blur & Loading Spinner
                    AnimatedVisibility(visible = !uiState.selectedImageUri.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                // 1. Base Image with blur effect while processing
                                AsyncImage(
                                    model = uiState.selectedImageUri,
                                    contentDescription = "Attached image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            if (!uiState.isProcessingImage) {
                                                previewImageUrl = uiState.selectedImageUri
                                            }
                                        }
                                        .then(if (uiState.isProcessingImage) Modifier.blur(10.dp) else Modifier),
                                    contentScale = ContentScale.Crop
                                )

                                // 2. Dark Scrim & Centered Loading Spinner while processing
                                if (uiState.isProcessingImage) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                } else {
                                    // 3. Remove Button when processing is complete
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .align(Alignment.TopEnd)
                                            .clickable { onImageSelected(null) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Image",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            if (uiState.isProcessingImage) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Memproses gambar...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    var localInputText by remember { mutableStateOf("") }

                    LaunchedEffect(uiState.inputText) {
                        if (uiState.inputText.isBlank() && localInputText.isNotBlank()) {
                            localInputText = ""
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Attach Vision Image",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = localInputText,
                            onValueChange = {
                                localInputText = it
                                onInputTextChanged(it)
                            },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        if (uiState.isStreaming) {
                            IconButton(
                                onClick = onStopStreaming,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Generating",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        } else {
                            val canSend = !uiState.isProcessingImage && (localInputText.isNotBlank() || !uiState.selectedImageUri.isNullOrBlank())
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        onSendMessage(context)
                                        localInputText = ""
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                if (uiState.isProcessingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDebugLogsDialog && uiState.isDebugLoggingEnabled) {
        AlertDialog(
            onDismissRequest = { showDebugLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("API Request Logs", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showDebugLogsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                if (debugLogs.isEmpty()) {
                    Text("No API request logs recorded yet. Send a message to see the request JSON payload.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(debugLogs, key = { it.id }) { log ->
                            DebugLogCardItem(log = log)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { DebugLogManager.clearLogs() }) {
                    Text("Clear Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDebugLogsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showEditChatDialog && uiState.session != null) {
        var editTitle by remember(uiState.session.title) { mutableStateOf(uiState.session.title) }
        var editPersonaId by remember(uiState.session.personaId, uiState.allPersonas) {
            mutableStateOf(uiState.session.personaId.ifBlank { uiState.allPersonas.firstOrNull()?.id ?: "" })
        }
        var editConfigId by remember(uiState.session.configId, uiState.configs) {
            mutableStateOf(uiState.session.configId.ifBlank { uiState.configs.firstOrNull()?.id ?: "" })
        }
        var isPersonaDropdownOpen by remember { mutableStateOf(false) }
        var isConfigDropdownOpen by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditChatDialog = false },
            title = { Text("Edit Chat Session") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Chat Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (uiState.allPersonas.isNotEmpty()) {
                        Column {
                            Text(
                                text = "User Persona:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isPersonaDropdownOpen,
                                onExpandedChange = { isPersonaDropdownOpen = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val currentPersona = uiState.allPersonas.find { it.id == editPersonaId }
                                    ?: uiState.allPersonas.first()
                                OutlinedTextField(
                                    value = currentPersona.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPersonaDropdownOpen) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isPersonaDropdownOpen,
                                    onDismissRequest = { isPersonaDropdownOpen = false }
                                ) {
                                    uiState.allPersonas.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(p.name, fontWeight = FontWeight.Bold)
                                                    if (p.personaDescription.isNotBlank()) {
                                                        Text(
                                                            p.personaDescription,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                editPersonaId = p.id
                                                isPersonaDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.configs.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Inference Config Profile:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isConfigDropdownOpen,
                                onExpandedChange = { isConfigDropdownOpen = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val currentConfig = uiState.configs.find { it.id == editConfigId }
                                    ?: uiState.configs.first()
                                OutlinedTextField(
                                    value = currentConfig.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isConfigDropdownOpen) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isConfigDropdownOpen,
                                    onDismissRequest = { isConfigDropdownOpen = false }
                                ) {
                                    uiState.configs.forEach { cfg ->
                                        DropdownMenuItem(
                                            text = { Text(cfg.name) },
                                            onClick = {
                                                editConfigId = cfg.id
                                                isConfigDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSessionSettings(editTitle, editPersonaId, editConfigId)
                        showEditChatDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkDeleteMessagesDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteMessagesDialog = false },
            title = { Text("Delete ${selectedMessageIds.size} Message${if (selectedMessageIds.size > 1) "s" else ""}?") },
            text = { Text("Are you sure you want to delete the selected messages?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selectedMessageIds
                        selectedMessageIds = emptySet<String>()
                        showBulkDeleteMessagesDialog = false
                        onDeleteMessages(ids)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteMessagesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (!previewImageUrl.isNullOrBlank()) {
        FullScreenImagePreviewDialog(
            imageUri = previewImageUrl!!,
            onDismiss = { previewImageUrl = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleItem(
    modifier: Modifier = Modifier,
    message: ChatMessageEntity,
    characterName: String,
    isShowThinkingEnabled: Boolean = false,
    isTokenCounterEnabled: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    val isUser = message.sender.equals("USER", ignoreCase = true)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    // Distinct Colors & Tail Shapes for User and Character
    val bubbleColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    } else if (isUser) {
        Color(0xFF3B3E70) // Soft deep indigo for user
    } else {
        Color(0xFF232638) // Dark slate container for character
    }

    // Tails: small radius on bottom-right for User, small radius on bottom-left for Character
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val thinkRegex = remember { Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE) }
    val matchResult = remember(message.content) { thinkRegex.find(message.content) }
    val thinkingText = remember(matchResult) { matchResult?.groupValues?.get(1)?.trim() }
    val cleanContentText = remember(message.content, matchResult) {
        if (matchResult != null) {
            message.content.replace(thinkRegex, "").trim()
        } else {
            message.content
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (!isUser) {
                    Text(
                        text = characterName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (!message.imageUri.isNullOrBlank()) {
                    val imageModel = remember(message.imageUri) {
                        val uriStr = message.imageUri
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

                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isSelectionMode) onClick() else onImageClick(message.imageUri)
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Expandable Thinking Process Accordion for Character Past Messages
                if (!isUser && isShowThinkingEnabled && !thinkingText.isNullOrBlank()) {
                    var isPastThinkingExpanded by remember { mutableStateOf(false) }
                    ThinkingProcessCard(
                        thinkingText = thinkingText,
                        isExpanded = isPastThinkingExpanded,
                        onToggle = {
                            if (isSelectionMode) onClick() else { isPastThinkingExpanded = !isPastThinkingExpanded }
                        },
                        isStreaming = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val actionColor = if (isUser) Color(0xFFD3CBFF) else Color(0xFFB4BEFF)
                val parsedContent = remember(cleanContentText, isUser) {
                    com.ryzumi.miraiai.domain.util.MarkdownRenderer.parseMarkdown(
                        text = cleanContentText,
                        actionColor = actionColor
                    )
                }

                Text(
                    text = parsedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser && isTokenCounterEnabled && (message.tokensCount > 0 || message.generationSpeedTps > 0 || !message.modelName.isNullOrBlank())) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(12.dp)
                            )
                            val modelPrefix = if (!message.modelName.isNullOrBlank()) "${message.modelName} • " else ""
                            Text(
                                text = "$modelPrefix${message.tokensCount} tokens • ${String.format(Locale.US, "%.1f", message.generationSpeedTps)} t/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingBubbleItem(
    modifier: Modifier = Modifier,
    streamingThinking: String = "",
    streamingText: String,
    streamingModelName: String = "",
    isThinkingExpanded: Boolean = true,
    isShowThinkingEnabled: Boolean = false,
    isTokenCounterEnabled: Boolean = false,
    streamingTokensCount: Int = 0,
    streamingSpeedTps: Double = 0.0,
    onToggleThinking: () -> Unit = {},
    characterName: String,
    listState: LazyListState? = null
) {
    // Smooth typewriter catch-up effect for streaming text
    var displayedLength by remember { mutableIntStateOf(if (streamingText.isNotEmpty()) 1 else 0) }

    LaunchedEffect(streamingText) {
        if (streamingText.isEmpty()) {
            displayedLength = 0
        } else {
            if (displayedLength == 0) {
                displayedLength = 1
            }
            while (displayedLength < streamingText.length) {
                val diff = streamingText.length - displayedLength
                val step = when {
                    diff > 80 -> 8
                    diff > 40 -> 4
                    diff > 15 -> 2
                    else -> 1
                }
                displayedLength = (displayedLength + step).coerceAtMost(streamingText.length)
                delay(if (diff > 25) 8L else 14L)
            }
        }
    }

    val visibleText = if (streamingText.isEmpty()) {
        ""
    } else {
        val len = displayedLength.coerceIn(1, streamingText.length)
        streamingText.substring(0, len)
    }

    // Auto-scroll to bottom as typewriter reveals newly typed lines to keep bottom/cursor in view
    LaunchedEffect(visibleText) {
        if (visibleText.isNotEmpty() && listState != null) {
            val totalCount = listState.layoutInfo.totalItemsCount
            val isNearBottom = listState.layoutInfo.visibleItemsInfo.any {
                it.index >= totalCount - 2
            }
            if (isNearBottom) {
                listState.scrollToBottom()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "streamingEffects")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            color = Color(0xFF232638),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = characterName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live Thinking Process Accordion while streaming
                if (isShowThinkingEnabled && streamingThinking.isNotBlank()) {
                    ThinkingProcessCard(
                        thinkingText = streamingThinking,
                        isExpanded = isThinkingExpanded,
                        onToggle = onToggleThinking,
                        isStreaming = visibleText.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (visibleText.isBlank() && (!isShowThinkingEnabled || streamingThinking.isBlank())) {
                    // Animated 3 Dots Typing Indicator
                    TypingDotsIndicator()
                } else if (visibleText.isNotBlank()) {
                    val parsedStreamingContent = remember(visibleText, cursorAlpha) {
                        val baseAnnotated = com.ryzumi.miraiai.domain.util.MarkdownRenderer.parseMarkdown(
                            text = visibleText,
                            actionColor = Color(0xFFB4BEFF)
                        )
                        buildAnnotatedString {
                            append(baseAnnotated)
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFFA5B4FC).copy(alpha = cursorAlpha),
                                    fontWeight = FontWeight.Black
                                )
                            ) {
                                append(" ▍")
                            }
                        }
                    }
                    Text(
                        text = parsedStreamingContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    if (isTokenCounterEnabled && (streamingTokensCount > 0 || streamingModelName.isNotBlank())) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(12.dp)
                            )
                            val modelPrefix = if (streamingModelName.isNotBlank()) "$streamingModelName • " else ""
                            Text(
                                text = "$modelPrefix$streamingTokensCount tokens • ${String.format(Locale.US, "%.1f", streamingSpeedTps)} t/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingProcessCard(
    thinkingText: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isStreaming: Boolean = false
) {
    val thinkingScrollState = rememberScrollState()

    // Auto-scroll to bottom of thinking process as new reasoning tokens arrive
    LaunchedEffect(thinkingText, isStreaming, isExpanded) {
        if (isStreaming && isExpanded) {
            thinkingScrollState.scrollTo(thinkingScrollState.maxValue)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF201B30),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "// THINKING PROCESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFC084FC)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isStreaming) {
                        Text(
                            text = "// reasoning...",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = if (isExpanded) "[hide]" else "[show]",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                    )
                    val preventParentScroll = remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                return available
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .nestedScroll(preventParentScroll)
                            .verticalScroll(thinkingScrollState)
                    ) {
                        Text(
                            text = thinkingText,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingDotsIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dotsTransition")

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )

    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha3"
    )

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha2))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha3))
        )
    }
}

@Composable
fun FullScreenImagePreviewDialog(
    imageUri: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val imageModel = remember(imageUri) {
        if (imageUri.startsWith("data:image/")) {
            try {
                val base64Part = imageUri.substringAfter("base64,")
                android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                imageUri
            }
        } else if (imageUri.startsWith("/")) {
            java.io.File(imageUri)
        } else {
            imageUri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Full Image Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1)) / 2f
                                val maxOffsetY = (size.height * (scale - 1)) / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit
            )

            // Top action bar with Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Preview",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
