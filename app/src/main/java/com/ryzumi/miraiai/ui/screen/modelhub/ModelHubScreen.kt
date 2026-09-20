package com.ryzumi.miraiai.ui.screen.modelhub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ryzumi.miraiai.domain.model.LocalModelClassifier
import com.ryzumi.miraiai.domain.model.LocalModelType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryzumi.miraiai.domain.model.HuggingFaceModel
import com.ryzumi.miraiai.domain.model.ModelCompatibility
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModelHubScreen(
    uiState: ModelHubUiState,
    onFilterSelected: (ModelHubFilter) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onLoadMore: () -> Unit,
    onDownloadModelClick: (HuggingFaceModel) -> Unit,
    onPauseDownloadClick: (HuggingFaceModel) -> Unit,
    onCancelDownloadClick: (HuggingFaceModel) -> Unit,
    onDeleteFileClick: (File) -> Unit,
    onBackClick: () -> Unit
) {
    var localSearchText by rememberSaveable { mutableStateOf(uiState.searchQuery) }
    var selectedModelForDetails by remember { mutableStateOf<HuggingFaceModel?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery != localSearchText) {
            localSearchText = uiState.searchQuery
        }
    }

    // Infinite scroll detection
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.nextPageUrl != null) {
            onLoadMore()
        }
    }

    val displayModels = remember(uiState.models, uiState.selectedFilter, localSearchText) {
        val query = localSearchText.trim()
        uiState.models.filter { model ->
            val matchesQuery = query.isEmpty() ||
                    model.modelName.contains(query, ignoreCase = true) ||
                    model.author.contains(query, ignoreCase = true) ||
                    model.tags.any { it.contains(query, ignoreCase = true) }

            if (!matchesQuery) return@filter false

            when (uiState.selectedFilter) {
                ModelHubFilter.ALL -> true
                ModelHubFilter.TEXT_GGUF -> true
                ModelHubFilter.VISION -> true
                ModelHubFilter.IMAGE_GEN -> true
                ModelHubFilter.VOICE_TTS -> true
                ModelHubFilter.DOWNLOADED -> model.isDownloaded
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Hub", fontWeight = FontWeight.Bold) },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Device System RAM Card Indicator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "System RAM: %.1f GB (Available: %.1f GB)",
                                uiState.systemTotalRamGb,
                                uiState.systemAvailRamGb
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Model feasibility is estimated automatically based on this device's specs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Search Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = localSearchText,
                    onValueChange = {
                        localSearchText = it
                        onSearchQueryChanged(it)
                    },
                    placeholder = { Text("Search models") },
                    trailingIcon = {
                        if (localSearchText.isNotEmpty()) {
                            IconButton(onClick = {
                                localSearchText = ""
                                onSearchQueryChanged("")
                                keyboardController?.hide()
                                onSearchClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearchClick()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        onSearchClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            }

            // 3. Category & Size Filter Chips
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                // Category Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModelHubFilter.entries.forEach { filter ->
                        val label = if (filter == ModelHubFilter.DOWNLOADED) {
                            "Downloaded (${uiState.downloadedModels.size})"
                        } else filter.label

                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }


            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Model Listing
            if (uiState.selectedFilter == ModelHubFilter.DOWNLOADED && uiState.downloadedModels.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.downloadedModels,
                        key = { it.absolutePath }
                    ) { file ->
                        LocalFileCardItem(
                            file = file,
                            onDeleteClick = { fileToDelete = file }
                        )
                    }
                }
            } else if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (displayModels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (uiState.selectedFilter != ModelHubFilter.ALL) {
                                "No models match '${uiState.selectedFilter.label}'"
                            } else {
                                "No models found"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.selectedFilter != ModelHubFilter.ALL && uiState.models.isNotEmpty()) {
                            Button(
                                onClick = { onFilterSelected(ModelHubFilter.ALL) }
                            ) {
                                Text("View in All Models (${uiState.models.size})")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = displayModels,
                        key = { it.id }
                    ) { model ->
                        ModelCardItem(
                            model = model,
                            downloadState = uiState.downloadStateMap[model.id],
                            onCardClick = { selectedModelForDetails = model },
                            onDownloadClick = { onDownloadModelClick(model) },
                            onPauseClick = { onPauseDownloadClick(model) },
                            onCancelClick = { onCancelDownloadClick(model) }
                        )
                    }

                    // Infinite Scroll Loading Indicator
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Model Details Dialog
    selectedModelForDetails?.let { model ->
        ModelDetailDialog(
            model = model,
            systemTotalRamGb = uiState.systemTotalRamGb,
            systemAvailRamGb = uiState.systemAvailRamGb,
            downloadState = uiState.downloadStateMap[model.id],
            onDownloadClick = { onDownloadModelClick(model) },
            onPauseClick = { onPauseDownloadClick(model) },
            onCancelClick = { onCancelDownloadClick(model) },
            onDismiss = { selectedModelForDetails = null }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Downloaded Model?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${file.name}'? This will free up storage space, but you will need to download it again to use it offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFileClick(file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelCardItem(
    model: HuggingFaceModel,
    downloadState: ModelDownloadState?,
    onCardClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            // Header: Name & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.modelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by ${model.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (model.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (downloadState?.status == DownloadStatus.DOWNLOADING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledIconButton(
                            onClick = onPauseClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Download",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else if (downloadState?.status == DownloadStatus.PAUSED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledIconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume Download",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onCancelClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    FilledIconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Model",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Progress Bar Section
            if (downloadState != null && (downloadState.status == DownloadStatus.DOWNLOADING || downloadState.status == DownloadStatus.PAUSED)) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { downloadState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (downloadState.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (downloadState.status == DownloadStatus.PAUSED) "Paused" else "Downloading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (downloadState.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${downloadState.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Required Model Indicators: Size, Vision, Image Gen, RAM Compatibility
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Size Indicator
                IndicatorBadge(
                    icon = Icons.Default.Storage,
                    label = "Size: ${model.formattedSize}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // 2. Vision Capabilities Indicator
                if (model.hasVisionCapability) {
                    IndicatorBadge(
                        icon = Icons.Default.Visibility,
                        label = "Vision: Yes",
                        containerColor = Color(0xFF004D40),
                        contentColor = Color(0xFF80CBC4)
                    )
                } else {
                    IndicatorBadge(
                        icon = Icons.Default.VisibilityOff,
                        label = "Vision: No",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Image Gen Capabilities Indicator
                if (model.hasImageGenCapability) {
                    IndicatorBadge(
                        icon = Icons.Default.Image,
                        label = "Image Gen: Yes",
                        containerColor = Color(0xFF4A148C),
                        contentColor = Color(0xFFCE93D8)
                    )
                } else {
                    IndicatorBadge(
                        icon = Icons.Default.Image,
                        label = "Image Gen: No",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3b. Voice Capabilities Indicator
                if (model.hasVoiceCapability) {
                    IndicatorBadge(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Voice: Yes",
                        containerColor = Color(0xFF004D40),
                        contentColor = Color(0xFF80CBC4)
                    )
                }

                // 4. Device RAM & Compatibility Indicator
                when (model.compatibility) {
                    ModelCompatibility.OPTIMAL -> {
                        IndicatorBadge(
                            icon = Icons.Default.CheckCircle,
                            label = String.format(Locale.US, "Runs Smoothly (RAM ~%.1f GB)", model.requiredRamGb),
                            containerColor = Color(0xFF1B5E20),
                            contentColor = Color(0xFFA5D6A7)
                        )
                    }
                    ModelCompatibility.MODERATE -> {
                        IndicatorBadge(
                            icon = Icons.Default.AutoAwesome,
                            label = String.format(Locale.US, "Moderate (RAM ~%.1f GB)", model.requiredRamGb),
                            containerColor = Color(0xFFE65100),
                            contentColor = Color(0xFFFFCC80)
                        )
                    }
                    ModelCompatibility.LOW_MEMORY -> {
                        IndicatorBadge(
                            icon = Icons.Default.Warning,
                            label = String.format(Locale.US, "Heavy / High RAM (RAM ~%.1f GB)", model.requiredRamGb),
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color(0xFFFFCDD2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats: Downloads & Likes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${model.downloads}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 14.dp)
                )

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${model.likes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelDetailDialog(
    model: HuggingFaceModel,
    systemTotalRamGb: Double,
    systemAvailRamGb: Double,
    downloadState: ModelDownloadState?,
    onDownloadClick: () -> Unit,
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = model.modelName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "by ${model.author} • ${model.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Specifications Section
                Text(
                    text = "Specifications & Capabilities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IndicatorBadge(
                        icon = Icons.Default.Storage,
                        label = "Size: ${model.formattedSize}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    IndicatorBadge(
                        icon = if (model.hasVisionCapability) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        label = if (model.hasVisionCapability) "Vision: Yes" else "Vision: No",
                        containerColor = if (model.hasVisionCapability) Color(0xFF004D40) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (model.hasVisionCapability) Color(0xFF80CBC4) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IndicatorBadge(
                        icon = Icons.Default.Image,
                        label = if (model.hasImageGenCapability) "Image Gen: Yes" else "Image Gen: No",
                        containerColor = if (model.hasImageGenCapability) Color(0xFF4A148C) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (model.hasImageGenCapability) Color(0xFFCE93D8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (model.hasVoiceCapability) {
                        IndicatorBadge(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Voice: Yes",
                            containerColor = Color(0xFF004D40),
                            contentColor = Color(0xFF80CBC4)
                        )
                    }
                }

                // Device Feasibility Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (model.compatibility) {
                            ModelCompatibility.OPTIMAL -> Color(0xFF1B5E20).copy(alpha = 0.25f)
                            ModelCompatibility.MODERATE -> Color(0xFFE65100).copy(alpha = 0.25f)
                            ModelCompatibility.LOW_MEMORY -> Color(0xFFB71C1C).copy(alpha = 0.25f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (model.compatibility) {
                                    ModelCompatibility.OPTIMAL -> Icons.Default.CheckCircle
                                    ModelCompatibility.MODERATE -> Icons.Default.AutoAwesome
                                    ModelCompatibility.LOW_MEMORY -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = when (model.compatibility) {
                                    ModelCompatibility.OPTIMAL -> Color(0xFF4CAF50)
                                    ModelCompatibility.MODERATE -> Color(0xFFFF9800)
                                    ModelCompatibility.LOW_MEMORY -> Color(0xFFEF5350)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (model.compatibility) {
                                    ModelCompatibility.OPTIMAL -> "Smooth Performance"
                                    ModelCompatibility.MODERATE -> "Moderate Feasibility"
                                    ModelCompatibility.LOW_MEMORY -> "High RAM Demand / Heavy"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = String.format(
                                Locale.US,
                                "Estimated RAM Requirement: ~%.1f GB\nDevice Total RAM: %.1f GB (Available: %.1f GB)",
                                model.requiredRamGb,
                                systemTotalRamGb,
                                systemAvailRamGb
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (model.compatibility) {
                                ModelCompatibility.OPTIMAL -> "Your device has plenty of memory to run this model smoothly without lag or memory exhaustion."
                                ModelCompatibility.MODERATE -> "This model can run, but high context window usage or background apps may cause slowdowns."
                                ModelCompatibility.LOW_MEMORY -> "This model requires more RAM than your device provides and may cause slowdowns or out-of-memory errors."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tags Cloud
                if (model.tags.isNotEmpty()) {
                    Text(
                        text = "Model Tags",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        model.tags.take(10).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Progress Bar in Dialog
                if (downloadState != null && (downloadState.status == DownloadStatus.DOWNLOADING || downloadState.status == DownloadStatus.PAUSED)) {
                    Column {
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (downloadState.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (downloadState.status == DownloadStatus.PAUSED) "Download Paused" else "Downloading...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${downloadState.progress}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (model.isDownloaded) {
                TextButton(onClick = onDismiss) {
                    Text("Downloaded")
                }
            } else if (downloadState?.status == DownloadStatus.DOWNLOADING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onCancelClick()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel")
                    }
                    Button(onClick = onPauseClick) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                }
            } else if (downloadState?.status == DownloadStatus.PAUSED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onCancelClick()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel")
                    }
                    Button(onClick = onDownloadClick) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume")
                    }
                }
            } else {
                Button(
                    onClick = {
                        onDownloadClick()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download Model")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun IndicatorBadge(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalFileCardItem(
    file: File,
    onDeleteClick: () -> Unit
) {
    val fileLength = file.length()
    val formattedSize = remember(fileLength) {
        val sizeInGb = fileLength.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (sizeInGb >= 1.0) {
            String.format(Locale.US, "%.2f GB", sizeInGb)
        } else {
            String.format(Locale.US, "%.1f MB", fileLength.toDouble() / (1024.0 * 1024.0))
        }
    }

    val modelCategory = remember(file.name, file.absolutePath) {
        LocalModelClassifier.classify(file)
    }

    val modelInfo = remember(file.name, modelCategory) {
        val cleanName = file.name.removeSuffix(".onnx").removeSuffix(".gguf")
        val parts = cleanName.split("_")

        val author: String
        val repo: String
        val displayName: String

        if (parts.size >= 3) {
            author = parts[0]
            repo = parts[1]
            val remainder = parts.drop(2).joinToString("_")
            displayName = if (remainder.equals("model", ignoreCase = true) ||
                remainder.equals("model_quantized", ignoreCase = true) ||
                remainder.equals("quantized", ignoreCase = true)
            ) {
                "$repo (${remainder})"
            } else {
                remainder
            }
        } else if (parts.size == 2) {
            author = parts[0]
            repo = parts[1]
            displayName = parts[1]
        } else {
            author = if (modelCategory == LocalModelType.VOICE_TTS) "Local TTS" else "Local Storage"
            repo = cleanName
            displayName = cleanName
        }

        Pair(author, displayName)
    }

    val author = modelInfo.first
    val title = modelInfo.second

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            // Header: Model Title, Author & Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Model",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Indicators: Size, Type, Offline Ready Status
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Exact Size
                IndicatorBadge(
                    icon = Icons.Default.Storage,
                    label = "Size: $formattedSize",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // 2. Capability Badge
                when (modelCategory) {
                    LocalModelType.VOICE_TTS -> {
                        IndicatorBadge(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Voice (TTS / ONNX)",
                            containerColor = Color(0xFF004D40),
                            contentColor = Color(0xFF80CBC4)
                        )
                    }
                    LocalModelType.IMAGE_GEN -> {
                        IndicatorBadge(
                            icon = Icons.Default.Image,
                            label = "Image Gen (Diffusion)",
                            containerColor = Color(0xFF4A148C),
                            contentColor = Color(0xFFCE93D8)
                        )
                    }
                    LocalModelType.VISION -> {
                        IndicatorBadge(
                            icon = Icons.Default.Visibility,
                            label = "Vision (VLM / GGUF)",
                            containerColor = Color(0xFF004D40),
                            contentColor = Color(0xFF80CBC4)
                        )
                    }
                    LocalModelType.TEXT_LLM -> {
                        IndicatorBadge(
                            icon = Icons.Default.Memory,
                            label = "Text LLM (GGUF)",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. Offline Ready Status Badge
                IndicatorBadge(
                    icon = Icons.Default.CheckCircle,
                    label = "Ready for Offline Use",
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color(0xFFA5D6A7)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File Name details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
