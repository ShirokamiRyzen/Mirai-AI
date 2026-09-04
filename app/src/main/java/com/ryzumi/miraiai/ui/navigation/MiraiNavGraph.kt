package com.ryzumi.miraiai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.data.local.MiraiDatabase
import com.ryzumi.miraiai.data.network.HuggingFaceRepository
import com.ryzumi.miraiai.data.network.OpenAiRepository
import com.ryzumi.miraiai.domain.backup.BackupRepository
import com.ryzumi.miraiai.ui.screen.character.CharacterEditScreen
import com.ryzumi.miraiai.ui.screen.character.CharacterEditViewModel
import com.ryzumi.miraiai.ui.screen.character.CharacterListScreen
import com.ryzumi.miraiai.ui.screen.character.CharacterListViewModel
import com.ryzumi.miraiai.ui.screen.chat.ChatScreen
import com.ryzumi.miraiai.ui.screen.chat.ChatViewModel
import com.ryzumi.miraiai.ui.screen.management.ManagementScreen
import com.ryzumi.miraiai.ui.screen.modelhub.ModelHubScreen
import com.ryzumi.miraiai.ui.screen.modelhub.ModelHubViewModel
import com.ryzumi.miraiai.ui.screen.persona.PersonaListScreen
import com.ryzumi.miraiai.ui.screen.persona.PersonaViewModel
import com.ryzumi.miraiai.ui.screen.settings.SettingsScreen
import com.ryzumi.miraiai.ui.screen.settings.SettingsViewModel
import kotlinx.coroutines.launch

object MiraiDestinations {
    const val CHARACTER_LIST = "character_list"
    const val MANAGEMENT = "management"
    const val CHARACTER_EDIT = "character_edit/{characterId}"
    const val CHAT = "chat/{sessionId}"
    const val SETTINGS = "settings"
    const val MODEL_HUB = "model_hub"
    const val PERSONA_LIST = "persona_list"
}

@Composable
fun MiraiNavGraph(
    navController: NavHostController = rememberNavController(),
    initialSessionId: String? = null,
    onSessionHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val database = remember { MiraiDatabase.getInstance(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val openAiRepo = remember { OpenAiRepository() }
    val hfRepo = remember { HuggingFaceRepository(context) }
    val backupRepo = remember { BackupRepository(context, database, settingsRepo) }

    androidx.compose.runtime.LaunchedEffect(initialSessionId) {
        if (!initialSessionId.isNullOrBlank()) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            val currentSessionId = navController.currentBackStackEntry?.arguments?.getString("sessionId")

            // Always dismiss notification and ensure active visible session
            com.ryzumi.miraiai.domain.util.ChatNotificationHelper.cancelNotification(context, initialSessionId)
            com.ryzumi.miraiai.domain.engine.ChatGenerationManager.setActiveVisibleSession(initialSessionId)

            if (currentRoute == MiraiDestinations.CHAT && currentSessionId == initialSessionId) {
                // User is ALREADY in this exact chat session! Do NOT push a duplicate ChatScreen layer!
            } else {
                navController.navigate("chat/$initialSessionId") {
                    popUpTo(MiraiDestinations.CHARACTER_LIST) {
                        saveState = false
                    }
                    launchSingleTop = true
                }
            }
            onSessionHandled()
        }
    }

    NavHost(
        navController = navController,
        startDestination = MiraiDestinations.CHARACTER_LIST
    ) {
        // 1. Home / Character List Screen
        composable(MiraiDestinations.CHARACTER_LIST) {
            val viewModel: CharacterListViewModel = viewModel {
                CharacterListViewModel(
                    characterDao = database.characterDao(),
                    chatSessionDao = database.chatSessionDao(),
                    chatMessageDao = database.chatMessageDao(),
                    userPersonaDao = database.userPersonaDao(),
                    inferenceConfigDao = database.inferenceConfigDao()
                )
            }
            val uiState by viewModel.uiState.collectAsState()

            CharacterListScreen(
                uiState = uiState,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSessionClick = { sessionId ->
                    navController.navigate("chat/$sessionId") {
                        launchSingleTop = true
                    }
                },
                onDeleteSession = viewModel::deleteSession,
                onDeleteSessions = viewModel::deleteSessions,
                onStartNewChatClick = { charId, configId, personaId ->
                    scope.launch {
                        val sessionId = viewModel.createNewChatSession(charId, configId, personaId)
                        navController.navigate("chat/$sessionId") {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToManagement = { navController.navigate(MiraiDestinations.MANAGEMENT) },
                onNavigateToSettings = { navController.navigate(MiraiDestinations.SETTINGS) },
                onNavigateToModelHub = { navController.navigate(MiraiDestinations.MODEL_HUB) }
            )
        }

        // 2. Management Screen (Unified Characters & Personas CRUD in 2 tabs)
        composable(MiraiDestinations.MANAGEMENT) {
            val charViewModel: CharacterListViewModel = viewModel {
                CharacterListViewModel(
                    characterDao = database.characterDao(),
                    chatSessionDao = database.chatSessionDao(),
                    chatMessageDao = database.chatMessageDao(),
                    userPersonaDao = database.userPersonaDao(),
                    inferenceConfigDao = database.inferenceConfigDao()
                )
            }
            val personaViewModel: PersonaViewModel = viewModel {
                PersonaViewModel(personaDao = database.userPersonaDao())
            }

            val charUiState by charViewModel.uiState.collectAsState()
            val personas by personaViewModel.personas.collectAsState()

            ManagementScreen(
                characters = charUiState.characters,
                personas = personas,
                searchQuery = charUiState.searchQuery,
                onSearchQueryChanged = { query ->
                    charViewModel.onSearchQueryChanged(query)
                    personaViewModel.onSearchQueryChanged(query)
                },
                onEditCharacterClick = { charId ->
                    navController.navigate("character_edit/$charId")
                },
                onDeleteCharacter = charViewModel::deleteCharacter,
                onDeleteCharacters = charViewModel::deleteCharacters,
                onCreateCharacterClick = {
                    navController.navigate("character_edit/new")
                },
                onSavePersona = personaViewModel::savePersona,
                onSetDefaultPersona = personaViewModel::setDefault,
                onDeletePersona = personaViewModel::deletePersona,
                onDeletePersonas = personaViewModel::deletePersonas,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 3. Character Edit Screen
        composable(
            route = MiraiDestinations.CHARACTER_EDIT,
            arguments = listOf(navArgument("characterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val charId = backStackEntry.arguments?.getString("characterId") ?: "new"
            val viewModel: CharacterEditViewModel = viewModel {
                CharacterEditViewModel(
                    characterDao = database.characterDao(),
                    characterId = charId
                )
            }
            val uiState by viewModel.uiState.collectAsState()

            CharacterEditScreen(
                uiState = uiState,
                onNameChanged = viewModel::onNameChanged,
                onAvatarUriChanged = viewModel::onAvatarUriChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onPersonalityChanged = viewModel::onPersonalityChanged,
                onScenarioChanged = viewModel::onScenarioChanged,
                onImpressionChanged = viewModel::onImpressionChanged,
                onTagsInputChanged = viewModel::onTagsInputChanged,
                onFirstMessageChanged = viewModel::onFirstMessageChanged,
                onSaveClick = { viewModel.saveCharacter(context) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4. Chat Screen
        composable(
            route = MiraiDestinations.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val viewModel: ChatViewModel = viewModel {
                ChatViewModel(
                    sessionId = sessionId,
                    database = database,
                    chatSessionDao = database.chatSessionDao(),
                    chatMessageDao = database.chatMessageDao(),
                    characterDao = database.characterDao(),
                    userPersonaDao = database.userPersonaDao(),
                    inferenceConfigDao = database.inferenceConfigDao(),
                    openAiRepository = openAiRepo,
                    settingsRepository = settingsRepo
                )
            }
            val uiState by viewModel.uiState.collectAsState()

            ChatScreen(
                uiState = uiState,
                onInputTextChanged = viewModel::onInputTextChanged,
                onImageSelected = { uri -> viewModel.processImageAttachment(context, uri) },
                onSelectModel = viewModel::selectConfig,
                onSendMessage = viewModel::sendMessage,
                onRegenerateResponse = { viewModel.regenerateResponse(context) },
                onStopStreaming = viewModel::stopStreaming,
                onLoadLocalModel = viewModel::loadLocalModel,
                onUnloadLocalModel = viewModel::unloadLocalModel,
                onDismissError = viewModel::dismissError,
                onToggleLiveThinkingExpanded = viewModel::toggleLiveThinkingExpanded,
                onDeleteMessage = viewModel::deleteMessage,
                onDeleteMessages = viewModel::deleteMessages,
                onClearHistory = viewModel::clearHistory,
                onUpdateSessionSettings = viewModel::updateChatSessionSettings,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 5. Settings Screen (Inference, Themes, Advance & Backup Tabs)
        composable(MiraiDestinations.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel {
                SettingsViewModel(
                    inferenceConfigDao = database.inferenceConfigDao(),
                    openAiRepository = openAiRepo,
                    huggingFaceRepository = hfRepo,
                    settingsRepository = settingsRepo,
                    backupRepository = backupRepo
                )
            }
            val uiState by viewModel.uiState.collectAsState()

            SettingsScreen(
                uiState = uiState,
                onSelectConfigProfile = viewModel::selectConfigProfile,
                onSaveConfigProfile = viewModel::saveConfigProfile,
                onDeleteConfigProfile = viewModel::deleteConfigProfile,
                onDeleteConfigProfiles = viewModel::deleteConfigProfiles,
                onFetchModelsClick = viewModel::fetchAvailableModels,
                onTestVisionCapability = viewModel::testVisionCapability,
                onClearDebugLogs = viewModel::clearDebugLogs,
                onToggleDebugLogging = viewModel::updateDebugLoggingEnabled,
                onToggleShowThinkingProcess = viewModel::updateShowThinkingProcess,
                onToggleTokenCounter = viewModel::updateTokenCounterEnabled,
                onToggleAllowDeviceContext = viewModel::updateAllowDeviceContext,
                onSetActiveProfile = viewModel::setActiveProfile,
                onExportBackup = viewModel::exportBackup,
                onImportBackup = viewModel::importBackup,
                onRefreshBackupStats = viewModel::refreshBackupStats,
                onClearBackupMessage = viewModel::clearBackupMessage,
                onUpdateThemeMode = viewModel::updateThemeMode,
                onUpdateMonetEnabled = viewModel::updateMonetEnabled,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 6. Model Hub Screen
        composable(MiraiDestinations.MODEL_HUB) {
            val viewModel: ModelHubViewModel = viewModel {
                ModelHubViewModel(
                    context = context,
                    repository = hfRepo
                )
            }
            val uiState by viewModel.uiState.collectAsState()

            ModelHubScreen(
                uiState = uiState,
                onFilterSelected = viewModel::selectFilter,
                onSizeFilterSelected = viewModel::selectSizeFilter,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSearchClick = { viewModel.searchModels() },
                onLoadMore = viewModel::loadMore,
                onDownloadModelClick = viewModel::downloadModel,
                onPauseDownloadClick = viewModel::pauseDownload,
                onCancelDownloadClick = viewModel::cancelDownload,
                onDeleteFileClick = viewModel::deleteDownloadedFile,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 7. User Personas Screen
        composable(MiraiDestinations.PERSONA_LIST) {
            val viewModel: PersonaViewModel = viewModel {
                PersonaViewModel(personaDao = database.userPersonaDao())
            }
            val personas by viewModel.personas.collectAsState()

            PersonaListScreen(
                personas = personas,
                onSavePersona = viewModel::savePersona,
                onSetDefault = viewModel::setDefault,
                onDeletePersona = viewModel::deletePersona,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
