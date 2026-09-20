package com.ryzumi.miraiai.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.ryzumi.miraiai.domain.tts.TtsDownloadState
import com.ryzumi.miraiai.domain.tts.TtsManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.ui.screen.about.AboutAppView
import com.ryzumi.miraiai.data.network.DebugLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSelectConfigProfile: (String) -> Unit,
    onSaveConfigProfile: (InferenceConfigEntity) -> Unit,
    onDeleteConfigProfile: (InferenceConfigEntity) -> Unit,
    onDeleteConfigProfiles: (Set<String>) -> Unit = {},
    onFetchModelsClick: (baseUrl: String, apiKey: String, customHeaders: String) -> Unit,
    onTestVisionCapability: (InferenceConfigEntity) -> Unit = {},
    onClearDebugLogs: () -> Unit = {},
    onToggleDebugLogging: (Boolean) -> Unit = {},
    onToggleShowThinkingProcess: (Boolean) -> Unit = {},
    onToggleTokenCounter: (Boolean) -> Unit = {},
    onToggleAllowDeviceContext: (Boolean) -> Unit = {},
    onToggleUploadAsBase64: (Boolean) -> Unit = {},
    onSetActiveProfile: (String) -> Unit = {},
    onExportBackup: (Uri) -> Unit = {},
    onImportBackup: (Uri) -> Unit = {},
    onRefreshBackupStats: () -> Unit = {},
    onClearBackupMessage: () -> Unit = {},
    onUpdateThemeMode: (String) -> Unit,
    onUpdateMonetEnabled: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var editingConfig by remember { mutableStateOf<InferenceConfigEntity?>(null) }
    var configToDelete by remember { mutableStateOf<InferenceConfigEntity?>(null) }
    var selectedConfigIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkConfigDeleteDialog by remember { mutableStateOf(false) }

    val isConfigSelectionMode = pagerState.currentPage == 0 && editingConfig == null && selectedConfigIds.isNotEmpty()

    BackHandler(enabled = isConfigSelectionMode) {
        selectedConfigIds = emptySet<String>()
    }

    Scaffold(
        topBar = {
            if (isConfigSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedConfigIds.size} Selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedConfigIds = emptySet<String>() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedConfigIds = if (selectedConfigIds.size == uiState.configs.size) emptySet<String>() else uiState.configs.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showBulkConfigDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = if (editingConfig == null) "Settings" else "Edit Config",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (editingConfig != null) {
                                    editingConfig = null
                                } else {
                                    onBackClick()
                                }
                            }
                        ) {
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
            if (pagerState.currentPage == 0 && editingConfig == null && !isConfigSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        val count = uiState.configs.size + 1
                        editingConfig = InferenceConfigEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            name = "Profile #$count",
                            baseUrl = "https://openrouter.ai/api/v1",
                            generateModelId = "auto",
                            visionModelId = ""
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Config")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (editingConfig == null) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            selectedConfigIds = emptySet<String>()
                        },
                        text = { Text("Inference", maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.SettingsInputComponent, contentDescription = null) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            selectedConfigIds = emptySet<String>()
                        },
                        text = { Text("Themes", maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            selectedConfigIds = emptySet<String>()
                        },
                        text = { Text("Advance", maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 3,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            selectedConfigIds = emptySet<String>()
                        },
                        text = { Text("Backup", maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.Backup, contentDescription = null) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 4,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(4) }
                            selectedConfigIds = emptySet<String>()
                        },
                        text = { Text("About", maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }
            }

            if (editingConfig != null) {
                // Config Editor Form View
                ConfigEditorForm(
                    config = editingConfig!!,
                    uiState = uiState,
                    onSave = { updated ->
                        onSaveConfigProfile(updated)
                        editingConfig = null
                    },
                    onFetchModelsClick = onFetchModelsClick,
                    onCancel = { editingConfig = null }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> {
                            // Tab 0: Inference Configurations List
                            if (uiState.configs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No configs created yet. Tap + to add one!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uiState.configs, key = { it.id }) { config ->
                                        val isSelected = selectedConfigIds.contains(config.id)
                                        ConfigCardItem(
                                            config = config,
                                            isSelected = isSelected,
                                            isSelectionMode = isConfigSelectionMode,
                                            onClick = {
                                                if (isConfigSelectionMode) {
                                                    selectedConfigIds = if (isSelected) selectedConfigIds - config.id else selectedConfigIds + config.id
                                                } else {
                                                    onSelectConfigProfile(config.id)
                                                    editingConfig = config
                                                }
                                            },
                                            onLongClick = {
                                                selectedConfigIds = if (isSelected) selectedConfigIds - config.id else selectedConfigIds + config.id
                                            },
                                            onEdit = {
                                                onSelectConfigProfile(config.id)
                                                editingConfig = config
                                            },
                                            onDelete = { configToDelete = config }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Tab 1: Themes & Appearance
                            ThemeSettingsView(
                                themeSettings = uiState.themeSettings,
                                onUpdateThemeMode = onUpdateThemeMode,
                                onUpdateMonetEnabled = onUpdateMonetEnabled
                            )
                        }
                        2 -> {
                            // Tab 2: Advance Settings (Debug & Thinking)
                            AdvanceSettingsView(
                                uiState = uiState,
                                onTestVisionCapability = onTestVisionCapability,
                                onClearDebugLogs = onClearDebugLogs,
                                onToggleDebugLogging = onToggleDebugLogging,
                                onToggleShowThinkingProcess = onToggleShowThinkingProcess,
                                onToggleTokenCounter = onToggleTokenCounter,
                                onToggleAllowDeviceContext = onToggleAllowDeviceContext,
                                onToggleUploadAsBase64 = onToggleUploadAsBase64,
                                onSetActiveProfile = onSetActiveProfile
                            )
                        }
                        3 -> {
                            // Tab 3: Backup & Restore
                            BackupSettingsView(
                                uiState = uiState,
                                onExportBackup = onExportBackup,
                                onImportBackup = onImportBackup,
                                onRefreshStats = onRefreshBackupStats,
                                onClearBackupMessage = onClearBackupMessage
                            )
                        }
                        else -> {
                            // Tab 4: About App
                            AboutAppView()
                        }
                    }
                }
            }
        }
    }

    configToDelete?.let { cfg ->
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = { Text("Delete Config?") },
            text = { Text("Are you sure you want to delete '${cfg.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConfigProfile(cfg)
                        configToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkConfigDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkConfigDeleteDialog = false },
            title = { Text("Delete ${selectedConfigIds.size} Profile${if (selectedConfigIds.size > 1) "s" else ""}?") },
            text = { Text("Are you sure you want to delete the selected inference profiles?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selectedConfigIds
                        selectedConfigIds = emptySet<String>()
                        showBulkConfigDeleteDialog = false
                        onDeleteConfigProfiles(ids)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkConfigDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ThemeSettingsView(
    themeSettings: com.ryzumi.miraiai.data.datastore.ThemeSettings,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateMonetEnabled: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "App Theme Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // System Auto Card
        ThemeModeCardItem(
            title = "System Default (Auto)",
            subtitle = "Follow system dark/light mode setting automatically",
            icon = Icons.Default.AutoMode,
            isSelected = themeSettings.themeMode.equals("system", ignoreCase = true),
            onClick = { onUpdateThemeMode("system") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Light Mode Card
        ThemeModeCardItem(
            title = "Light Mode",
            subtitle = "Always use bright light theme",
            icon = Icons.Default.LightMode,
            isSelected = themeSettings.themeMode.equals("light", ignoreCase = true),
            onClick = { onUpdateThemeMode("light") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dark Mode Card
        ThemeModeCardItem(
            title = "Dark Mode",
            subtitle = "Always use dark background theme",
            icon = Icons.Default.DarkMode,
            isSelected = themeSettings.themeMode.equals("dark", ignoreCase = true),
            onClick = { onUpdateThemeMode("dark") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Monet / Dynamic Color Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monet / System Dynamic Color",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Follow system wallpaper color palette (Android 12+). When turned off, uses Mirai AI signature color palette.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = themeSettings.isMonetEnabled,
                    onCheckedChange = onUpdateMonetEnabled
                )
            }
        }
    }
}

@Composable
fun ThemeModeCardItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
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
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfigCardItem(
    config: InferenceConfigEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsInputComponent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = config.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gen: ${config.generateModelId} | Vis: ${config.visionModelId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorForm(
    config: InferenceConfigEntity,
    uiState: SettingsUiState,
    onSave: (InferenceConfigEntity) -> Unit,
    onFetchModelsClick: (baseUrl: String, apiKey: String, customHeaders: String) -> Unit,
    onCancel: () -> Unit
) {
    var isGenModelDropdownExpanded by remember { mutableStateOf(false) }
    var isVisionModelDropdownExpanded by remember { mutableStateOf(false) }
    var isImageGenModelDropdownExpanded by remember { mutableStateOf(false) }

    var profileName by remember(config.id) { mutableStateOf(config.name) }
    var baseUrl by remember(config.id) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.id) { mutableStateOf(config.apiKey) }
    var generateModelId by remember(config.id, config.generateModelId) { mutableStateOf(config.generateModelId) }
    var useLocalGenModel by remember(config.id) { mutableStateOf(config.useLocalGenModel) }
    var visionModelId by remember(config.id, config.visionModelId) { mutableStateOf(config.visionModelId) }
    var useLocalVisionModel by remember(config.id) { mutableStateOf(config.useLocalVisionModel) }
    var imageGenModelId by remember(config.id, config.imageGenModelId) { mutableStateOf(config.imageGenModelId) }
    var temperature by remember(config.id) { mutableFloatStateOf(config.temperature) }
    var topP by remember(config.id) { mutableFloatStateOf(config.topP) }
    var maxTokens by remember(config.id) { mutableIntStateOf(config.maxTokens) }
    var repetitionPenalty by remember(config.id) { mutableFloatStateOf(config.repetitionPenalty) }
    var customHeaders by remember(config.id) { mutableStateOf(config.customHeaders) }
    var ttsEngine by remember(config.id) { mutableStateOf(config.ttsEngine) }
    var ttsLocalModel by remember(config.id) { mutableStateOf(config.ttsLocalModel) }
    var isTtsLocalModelDropdownExpanded by remember { mutableStateOf(false) }
    var ttsApiEndpoint by remember(config.id) { mutableStateOf(config.ttsApiEndpoint) }
    var ttsApiKey by remember(config.id) { mutableStateOf(config.ttsApiKey) }
    var ttsApiModel by remember(config.id) { mutableStateOf(config.ttsApiModel) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = profileName,
            onValueChange = { profileName = it },
            label = { Text("Profile Name *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base Endpoint URL") },
            placeholder = { Text("https://openrouter.ai/api/v1") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key (Optional)") },
            placeholder = { Text("sk-...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )

        Button(
            onClick = { onFetchModelsClick(baseUrl, apiKey, customHeaders) },
            enabled = !uiState.isFetchingModels,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            if (uiState.isFetchingModels) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetch Models from Endpoint")
            }
        }

        uiState.statusMessage?.let { status ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isError)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = if (uiState.isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Models Section
        Text(
            text = "Model Configurations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Text Generate Model Header with Local Model Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Text Generate Model",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (useLocalGenModel) "Use Local Downloaded Model" else "Use Endpoint API Model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Local", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
            Switch(
                checked = useLocalGenModel,
                onCheckedChange = { useLocalGenModel = it }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Text Generate Model Dropdown Selector
        ExposedDropdownMenuBox(
            expanded = isGenModelDropdownExpanded,
            onExpandedChange = { isGenModelDropdownExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = generateModelId,
                onValueChange = { },
                readOnly = true,
                label = { Text(if (useLocalGenModel) "Local Text Model" else "API Text Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenModelDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            val genModels = if (useLocalGenModel) {
                uiState.localTextModels
            } else {
                uiState.availableModels.ifEmpty {
                    listOf("auto", "gpt-3.5-turbo", "deepseek-v4-flash", "glm-5.1")
                }
            }
            ExposedDropdownMenu(
                expanded = isGenModelDropdownExpanded,
                onDismissRequest = { isGenModelDropdownExpanded = false }
            ) {
                genModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            generateModelId = model
                            isGenModelDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // 2. Vision Model Header with Local Model Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vision Model (for images)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (useLocalVisionModel) "Use Local Downloaded Model" else "Use Endpoint API Model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Local", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
            Switch(
                checked = useLocalVisionModel,
                onCheckedChange = { useLocalVisionModel = it }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Vision Model Dropdown Selector
        ExposedDropdownMenuBox(
            expanded = isVisionModelDropdownExpanded,
            onExpandedChange = { isVisionModelDropdownExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = visionModelId,
                onValueChange = { },
                readOnly = true,
                label = { Text(if (useLocalVisionModel) "Local Vision Model" else "API Vision Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVisionModelDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            val visModels = if (useLocalVisionModel) {
                uiState.localVisionModels
            } else {
                uiState.visionModels.ifEmpty {
                    uiState.availableModels.ifEmpty {
                        listOf("auto-debug", "claude-opus-5-b", "deepseek-v4-flash-vision-exp", "gpt-5.6-luna-b")
                    }
                }
            }
            ExposedDropdownMenu(
                expanded = isVisionModelDropdownExpanded,
                onDismissRequest = { isVisionModelDropdownExpanded = false }
            ) {
                visModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            visionModelId = model
                            isVisionModelDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // 3. Image Generation Model (Local Model Only)
        Text(
            text = "Image Generation Model (Local Only)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Downloaded diffusion model for image generation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = isImageGenModelDropdownExpanded,
            onExpandedChange = { isImageGenModelDropdownExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            OutlinedTextField(
                value = imageGenModelId,
                onValueChange = { },
                readOnly = true,
                label = { Text("Local Image Gen Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isImageGenModelDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            val imgModels = uiState.localImageGenModels
            ExposedDropdownMenu(
                expanded = isImageGenModelDropdownExpanded,
                onDismissRequest = { isImageGenModelDropdownExpanded = false }
            ) {
                imgModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            imageGenModelId = model
                            isImageGenModelDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Hyperparameters Section
        Text(
            text = "Hyperparameters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Temperature Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Temperature: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format("%.2f", temperature),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = temperature,
            onValueChange = { temperature = Math.round(it * 100f) / 100f },
            valueRange = 0.0f..2.0f,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Top-P Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Top-P: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format("%.2f", topP),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = topP,
            onValueChange = { topP = Math.round(it * 100f) / 100f },
            valueRange = 0.0f..1.0f,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Max Tokens Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Max Tokens: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$maxTokens",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = maxTokens.toFloat(),
            onValueChange = { maxTokens = it.roundToInt() },
            valueRange = 128f..8192f,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Repetition Penalty Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Repetition Penalty: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format("%.2f", repetitionPenalty),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = repetitionPenalty,
            onValueChange = { repetitionPenalty = Math.round(it * 100f) / 100f },
            valueRange = 1.0f..2.0f,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom Headers
        OutlinedTextField(
            value = customHeaders,
            onValueChange = { customHeaders = it },
            label = { Text("Custom Request Headers (JSON)") },
            placeholder = { Text("{\"HTTP-Referer\": \"https://miraiai.org\"}") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            minLines = 2
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Text-to-Speech (TTS) Configuration
        Text(
            text = "Voice & Speech Synthesis (TTS)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure speech generation for characters when Voice Mode is active",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Engine Selector
        Text(
            text = "TTS Engine Source",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = ttsEngine == "local",
                onClick = { ttsEngine = "local" },
                label = { Text("Local Voice Model") },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = ttsEngine == "api",
                onClick = { ttsEngine = "api" },
                label = { Text("Endpoint API") },
                leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = ttsEngine == "system",
                onClick = { ttsEngine = "system" },
                label = { Text("System TTS") },
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (ttsEngine) {
            "system" -> {
                Text(
                    text = "System TTS (Lightweight)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Uses Android's built-in speech engine (Google TTS). No model download needed — ideal for low-end devices. Voice presets will adjust pitch & speed to match the selected character voice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            "local" -> {
                Text(
                    text = "Local Voice Model",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select a downloaded voice model for speech synthesis (Kokoro, Piper, ONNX)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = isTtsLocalModelDropdownExpanded,
                    onExpandedChange = { isTtsLocalModelDropdownExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    fun formatVoiceName(raw: String): Pair<String, String?> {
                        if (raw.isBlank() || raw.startsWith("none")) return Pair("none (System Default)", null)
                        val clean = raw.removeSuffix(".onnx").removeSuffix(".gguf")
                        val parts = clean.split("_")
                        return if (parts.size >= 3) {
                            val remainder = parts.drop(2).joinToString("_")
                            val title = if (remainder.equals("model", ignoreCase = true) ||
                                remainder.equals("model_quantized", ignoreCase = true) ||
                                remainder.equals("quantized", ignoreCase = true)
                            ) {
                                "${parts[1]} ($remainder)"
                            } else {
                                remainder
                            }
                            Pair(title, parts[0])
                        } else if (parts.size == 2) {
                            Pair(parts[1], parts[0])
                        } else {
                            Pair(clean, null)
                        }
                    }

                    val selectedDisplayName = remember(ttsLocalModel) {
                        val (title, author) = formatVoiceName(ttsLocalModel)
                        if (author != null) "$title (by $author)" else title
                    }

                    OutlinedTextField(
                        value = selectedDisplayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Local Voice Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTtsLocalModelDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    val downloadedVoices = remember(uiState.localVoiceModels) {
                        val list = mutableListOf("none (System Default)")
                        list.addAll(uiState.localVoiceModels.filter { it != "none (System Default)" && it != "None (Download via Model Hub)" })
                        list.distinct()
                    }

                    ExposedDropdownMenu(
                        expanded = isTtsLocalModelDropdownExpanded,
                        onDismissRequest = { isTtsLocalModelDropdownExpanded = false }
                    ) {
                        downloadedVoices.forEach { model ->
                            val (title, author) = formatVoiceName(model)
                            DropdownMenuItem(
                                text = {
                                    if (author != null) {
                                        Column {
                                            Text(title, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "by $author",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Text(title)
                                    }
                                },
                                onClick = {
                                    ttsLocalModel = if (model.startsWith("none")) "none" else model
                                    isTtsLocalModelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            "api" -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ttsApiEndpoint,
                        onValueChange = { ttsApiEndpoint = it },
                        label = { Text("TTS Endpoint URL") },
                        placeholder = { Text("http://localhost:8880/v1/audio/speech") },
                        supportingText = { Text("Leave blank to use Base Endpoint URL + /audio/speech") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ttsApiKey,
                        onValueChange = { ttsApiKey = it },
                        label = { Text("TTS API Key (Optional)") },
                        placeholder = { Text("Leave blank to use profile API key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ttsApiModel,
                        onValueChange = { ttsApiModel = it },
                        label = { Text("TTS Model Name") },
                        placeholder = { Text("kokoro, tts-1, or tts-1-hd") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val updated = config.copy(
                        name = profileName,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        generateModelId = generateModelId,
                        useLocalGenModel = useLocalGenModel,
                        visionModelId = visionModelId,
                        useLocalVisionModel = useLocalVisionModel,
                        imageGenModelId = imageGenModelId,
                        temperature = Math.round(temperature * 100f) / 100f,
                        topP = Math.round(topP * 100f) / 100f,
                        maxTokens = maxTokens,
                        repetitionPenalty = Math.round(repetitionPenalty * 100f) / 100f,
                        customHeaders = customHeaders,
                        ttsEngine = ttsEngine,
                        ttsLocalModel = ttsLocalModel,
                        ttsApiEndpoint = ttsApiEndpoint,
                        ttsApiKey = ttsApiKey,
                        ttsApiModel = ttsApiModel
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceSettingsView(
    uiState: SettingsUiState,
    onTestVisionCapability: (InferenceConfigEntity) -> Unit,
    onClearDebugLogs: () -> Unit,
    onToggleDebugLogging: (Boolean) -> Unit,
    onToggleShowThinkingProcess: (Boolean) -> Unit,
    onToggleTokenCounter: (Boolean) -> Unit,
    onToggleAllowDeviceContext: (Boolean) -> Unit = {},
    onToggleUploadAsBase64: (Boolean) -> Unit = {},
    onSetActiveProfile: (String) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val activeConfig = uiState.selectedConfig ?: uiState.configs.find { it.isActive } ?: uiState.configs.firstOrNull()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        if (isGranted) {
            onToggleAllowDeviceContext(true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Thinking Process Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Show Thinking Process",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isShowThinkingProcess)
                                "Real-time thinking reasoning is displayed in an expandable accordion."
                            else
                                "Thinking process is completely hidden from chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isShowThinkingProcess,
                        onCheckedChange = onToggleShowThinkingProcess
                    )
                }
            }
        }

        // 2. Token Counter Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Enable Token Counter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isTokenCounterEnabled)
                                "Context token ratio and token speed (t/s) are displayed in chat."
                            else
                                "Context token ratio (e.g. 1,250 / 4,096) is displayed in chat header, and token count with generation speed (t/s) is shown on AI bubbles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isTokenCounterEnabled,
                        onCheckedChange = onToggleTokenCounter
                    )
                }
            }
        }

        // 3. Allow Device Context Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Allow access OS information and weather",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isAllowDeviceContext)
                                "Device info (clock, battery, GPS, weather) is automatically provided to AI personal assistant."
                            else
                                "Real-time clock, battery percentage & charging status, device model, GPS location, and live weather conditions are provided to the AI agent as a personal assistant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isAllowDeviceContext,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                onToggleAllowDeviceContext(false)
                            }
                        }
                    )
                }
            }
        }

        // 4. Upload as Base64 Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Upload as Base64",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isUploadAsBase64)
                                "Vision images are compressed and sent directly to the AI model as Base64 data URLs."
                            else
                                "Vision images are compressed and uploaded to S3 storage service, sending presigned URLs to the AI model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isUploadAsBase64,
                        onCheckedChange = onToggleUploadAsBase64
                    )
                }
            }
        }

        // 5. Debug Logging Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Enable Debug Logging",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isDebugLoggingEnabled)
                                "API request payloads and responses are tracked in memory."
                            else
                                "Debug logging is off. No logs are saved and chat debug menu is hidden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isDebugLoggingEnabled,
                        onCheckedChange = onToggleDebugLogging
                    )
                }
            }
        }

        // 5. Active Profile & Quick Diagnostic Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var isProfilePickerExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Profile Diagnostic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.configs.isNotEmpty()) {
                            Box {
                                FilterChip(
                                    selected = true,
                                    onClick = { isProfilePickerExpanded = true },
                                    label = { Text(activeConfig?.name ?: "Select Profile", maxLines = 1) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Profile",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )

                                DropdownMenu(
                                    expanded = isProfilePickerExpanded,
                                    onDismissRequest = { isProfilePickerExpanded = false }
                                ) {
                                    uiState.configs.forEach { cfg ->
                                        val isSelected = cfg.id == activeConfig?.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Active",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = cfg.name,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                        if (cfg.baseUrl.isNotBlank()) {
                                                            Text(
                                                                text = cfg.baseUrl,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                isProfilePickerExpanded = false
                                                onSetActiveProfile(cfg.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeConfig != null) {
                        Text(
                            text = "Base URL: ${activeConfig.baseUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Generate Model: ${activeConfig.generateModelId.ifBlank { "Auto / Default" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Vision Model: ${activeConfig.visionModelId.ifBlank { "None (Using generate model)" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No active configuration profile found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { activeConfig?.let { onTestVisionCapability(it) } },
                            enabled = activeConfig != null && !uiState.isTestingVision,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isTestingVision) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Vision Ping")
                            }
                        }

                        OutlinedButton(
                            onClick = onClearDebugLogs,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }

                    if (!uiState.visionTestResult.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.visionTestResult.startsWith("Vision Test Success")) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vision Test Response",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.visionTestResult))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Test Response",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.visionTestResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Voice Model & TTS Debugger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var testText by remember { mutableStateOf("Hello! This is a voice model audio synthesis test.") }
                    var selectedPresetId by remember { mutableStateOf("af_heart") }
                    var voicePitch by remember { mutableFloatStateOf(1.0f) }
                    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }
                    var selectedConfigId by remember { mutableStateOf<String?>(activeConfig?.id) }
                    var isConfigDropdownExpanded by remember { mutableStateOf(false) }
                    var isVoiceTesting by remember { mutableStateOf(false) }
                    var voiceDebugStatus by remember { mutableStateOf<String?>(null) }

                    val activeTestConfig = uiState.configs.find { it.id == selectedConfigId } ?: activeConfig

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Model & TTS Debugger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Debug speech synthesis, test voice presets, pitch, speed, and inference config models directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Inference Config Profile Selector
                    if (uiState.configs.isNotEmpty()) {
                        Text(
                            text = "Inference Config (TTS Model & Engine)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = isConfigDropdownExpanded,
                            onExpandedChange = { isConfigDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = activeTestConfig?.let {
                                    "${it.name}${if (it.isActive) " (Active)" else ""} [${it.ttsEngine.uppercase()}${if (it.ttsEngine == "local") " • ${it.ttsLocalModel}" else if (it.ttsEngine == "api") " • ${it.ttsApiModel}" else ""}]"
                                } ?: "Select Profile",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Target Profile") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isConfigDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = isConfigDropdownExpanded,
                                onDismissRequest = { isConfigDropdownExpanded = false }
                            ) {
                                uiState.configs.forEach { cfg ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${cfg.name}${if (cfg.isActive) " (Active)" else ""}",
                                                    fontWeight = if (cfg.id == activeTestConfig?.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "Engine: ${cfg.ttsEngine.uppercase()}${if (cfg.ttsEngine == "local") " (Model: ${cfg.ttsLocalModel})" else " (Model: ${cfg.ttsApiModel})"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedConfigId = cfg.id
                                            isConfigDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 2. Voice Preset Quick Selection Chips
                    Text(
                        text = "Voice Preset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.ryzumi.miraiai.domain.tts.TtsManager.KOKORO_VOICE_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = selectedPresetId.equals(preset.id, ignoreCase = true),
                                onClick = { selectedPresetId = preset.id },
                                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Custom Voice Preset ID Text Field
                    OutlinedTextField(
                        value = selectedPresetId,
                        onValueChange = { selectedPresetId = it },
                        label = { Text("Voice Identifier / Custom Preset") },
                        placeholder = { Text("e.g. af_heart, af_bella, am_adam, or jf_alpha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Pitch: ${String.format(java.util.Locale.US, "%.2fx", voicePitch)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { voicePitch = 1.0f }) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Slider(
                        value = voicePitch,
                        onValueChange = { voicePitch = Math.round(it * 100f) / 100f },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 5. Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Speed: ${String.format(java.util.Locale.US, "%.2fx", voiceSpeed)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { voiceSpeed = 1.0f }) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Slider(
                        value = voiceSpeed,
                        onValueChange = { voiceSpeed = Math.round(it * 100f) / 100f },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 6. Input Text to Speak
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("Input Text to Speak") },
                        placeholder = { Text("Enter sentence to test...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7. Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isVoiceTesting) {
                                    com.ryzumi.miraiai.domain.tts.TtsManager.stop()
                                    isVoiceTesting = false
                                    voiceDebugStatus = "Playback stopped."
                                } else {
                                    isVoiceTesting = true
                                    val engineLabel = activeTestConfig?.ttsEngine?.uppercase() ?: "LOCAL"
                                    voiceDebugStatus = "[$engineLabel] Synthesizing voice '$selectedPresetId' (Pitch: ${voicePitch}x, Speed: ${voiceSpeed}x)..."
                                    val dummyChar = com.ryzumi.miraiai.data.local.entity.CharacterEntity(
                                        name = "Tester",
                                        voiceId = selectedPresetId,
                                        voicePitch = voicePitch,
                                        voiceSpeed = voiceSpeed
                                    )
                                    com.ryzumi.miraiai.domain.tts.TtsManager.speak(
                                        context = context,
                                        text = testText,
                                        config = activeTestConfig,
                                        character = dummyChar,
                                        onStart = {
                                            isVoiceTesting = true
                                            voiceDebugStatus = "[$engineLabel] Playing audio successfully..."
                                        },
                                        onDone = {
                                            isVoiceTesting = false
                                            voiceDebugStatus = "[$engineLabel] Completed playback."
                                        },
                                        onError = { err ->
                                            isVoiceTesting = false
                                            voiceDebugStatus = "Error: $err"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isVoiceTesting) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isVoiceTesting) "Stop Voice" else "Play / Test Voice")
                        }

                        OutlinedButton(
                            onClick = {
                                com.ryzumi.miraiai.domain.tts.TtsManager.stop()
                                isVoiceTesting = false
                                voiceDebugStatus = null
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset")
                        }
                    }

                    if (!voiceDebugStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (voiceDebugStatus!!.startsWith("Error")) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = voiceDebugStatus ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (voiceDebugStatus!!.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Request & Payload Logs Header & List
        if (uiState.isDebugLoggingEnabled) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Request & Payload Logs (${uiState.debugLogs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.debugLogs.isNotEmpty()) {
                        TextButton(onClick = onClearDebugLogs) {
                            Text("Clear All")
                        }
                    }
                }
            }

            if (uiState.debugLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No request logs recorded yet.\nSend a message in chat or click 'Test Vision Ping' above to inspect live payloads.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(uiState.debugLogs, key = { it.id }) { log ->
                    DebugLogCardItem(log = log)
                }
            }
        }
    }
}

@Composable
fun DebugLogCardItem(log: DebugLogEntry) {
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    val badgeColor = when (log.type) {
        "CHAT_VISION" -> MaterialTheme.colorScheme.primaryContainer
        "TEST_VISION" -> MaterialTheme.colorScheme.tertiaryContainer
        "ERROR" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val badgeTextColor = when (log.type) {
        "CHAT_VISION" -> MaterialTheme.colorScheme.onPrimaryContainer
        "TEST_VISION" -> MaterialTheme.colorScheme.onTertiaryContainer
        "ERROR" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = badgeColor),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = log.formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (log.isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (log.httpStatusCode != null) {
                        Text(
                            text = "${log.httpStatusCode} (${log.durationMs}ms)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (log.httpStatusCode in 200..299) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Model: ${log.modelId.ifBlank { "Auto" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = log.endpointUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 4 else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Collapsed preview of response if available
            if (!isExpanded && !log.responseBody.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Response: ${log.responseBody.take(120)}${if (log.responseBody.length > 120) "..." else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Error Message if present
                if (!log.error.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Error Details",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(log.error))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Error",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Text(
                                text = log.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 1. Response Box (Displayed on top for quick reading!)
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model Response",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!log.responseBody.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(log.responseBody))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Response",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = log.responseBody ?: if (log.isStreaming) "Streaming response tokens in real-time..." else "No response body returned",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                // 2. Request Payload Box
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Request Body (JSON Payload)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(log.requestPayloadRaw))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Request JSON",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = log.requestPayloadFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupSettingsView(
    uiState: SettingsUiState,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onRefreshStats: () -> Unit,
    onClearBackupMessage: () -> Unit
) {
    var showConfirmImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val defaultBackupFileName = remember {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        "mirai_backup_$dateStr.miraidb"
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { onExportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUri = it
            showConfirmImportDialog = true
        }
    }

    LaunchedEffect(Unit) {
        onRefreshStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Status Alerts (Success or Error)
        if (!uiState.backupSuccessMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B3828)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.backupSuccessMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearBackupMessage,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!uiState.backupErrorMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3B1818)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.backupErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearBackupMessage,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 1. Current Database Stats Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Storage Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onRefreshStats,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadgeItem(
                        label = "Characters",
                        value = "${uiState.backupStats.characterCount}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeItem(
                        label = "Personas",
                        value = "${uiState.backupStats.personaCount}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeItem(
                        label = "Chats",
                        value = "${uiState.backupStats.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeItem(
                        label = "Messages",
                        value = "${uiState.backupStats.messageCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadgeItem(
                        label = "Configs",
                        value = "${uiState.backupStats.configCount}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeItem(
                        label = "Assets/Files",
                        value = "${uiState.backupStats.assetCount}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeItem(
                        label = "Live2D Models",
                        value = "${uiState.backupStats.live2dModelCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total App Data Size:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.backupStats.formattedDataSize,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Export Data Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Export Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Save all your characters, user personas, avatar images, Live2D models, chat sessions, message histories, and inference configurations into a single portable .miraidb backup file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { exportLauncher.launch(defaultBackupFileName) },
                    enabled = !uiState.isExportingBackup && !uiState.isImportingBackup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isExportingBackup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting Backup...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup (.miraidb)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Import Data Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Import Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Restore chats, characters, persona images, Live2D models, and settings from a previously exported MiraiAI (.miraidb) backup file. Existing data will be cleanly wiped and replaced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { importLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json")) },
                    enabled = !uiState.isExportingBackup && !uiState.isImportingBackup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isImportingBackup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restoring Data...")
                    } else {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup File (.miraidb)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // TWRP Style Confirmation Dialog before restoring
    if (showConfirmImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmImportDialog = false
                pendingImportUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Restore", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "All current data (chats, characters, personas, Live2D models) will be completely wiped and replaced with the backup data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TwrpSwipeToConfirmSlider(
                        text = "Swipe to Restore",
                        onConfirmed = {
                            val uri = pendingImportUri
                            showConfirmImportDialog = false
                            pendingImportUri = null
                            uri?.let { onImportBackup(it) }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmImportDialog = false
                        pendingImportUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TwrpSwipeToConfirmSlider(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Swipe to Restore"
) {
    val sliderHeight = 52.dp
    val thumbWidth = 64.dp
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { thumbWidth.toPx() }
    val maxDragPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(0f)
    val progress = if (maxDragPx > 0f) (offsetX / maxDragPx).coerceIn(0f, 1f) else 0f

    // Authentic TWRP Colors
    val twrpCyan = Color(0xFF00A5FF)
    val twrpDarkBg = Color(0xFF181A1D)
    val twrpBorder = Color(0xFF2C3238)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(sliderHeight)
            .border(BorderStroke(1.dp, twrpBorder), RoundedCornerShape(2.dp))
            .background(twrpDarkBg)
            .onGloballyPositioned { coordinates ->
                trackWidthPx = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Cyan fill that follows the thumb
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (offsetX + thumbWidthPx).toDp() })
                .background(twrpCyan.copy(alpha = 0.22f))
        )

        // Centered TWRP label text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = (1f - progress * 0.75f).coerceAtLeast(0.2f)),
                letterSpacing = 0.5.sp
            )
        }

        // Rectangular TWRP Draggable Thumb Button with triple chevron (>>>)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(offsetX.toInt(), 0)
                }
                .width(thumbWidth)
                .fillMaxHeight()
                .background(twrpCyan)
                .pointerInput(maxDragPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (maxDragPx > 0f && offsetX >= maxDragPx * 0.85f) {
                                offsetX = maxDragPx
                                onConfirmed()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount: Float ->
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxDragPx)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Triple chevron arrows like real TWRP slider
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "▶▶▶",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }
        }
    }
}

@Composable
fun StatBadgeItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

