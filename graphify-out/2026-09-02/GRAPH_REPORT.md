# Graph Report - MiraiAI  (2026-09-02)

## Corpus Check
- 68 files · ~43,803 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 582 nodes · 866 edges · 38 communities (30 shown, 8 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 60 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- SettingsViewModel
- ChatViewModel
- UserPersonaEntity
- ModelHubViewModel
- CharacterEntity
- CharacterEditViewModel
- OpenAiRepository
- MiraiNavGraph
- InferenceState
- ChatSessionDao
- DataUrlFetcher
- .startGeneration
- ImageUtils
- SettingsScreen.kt
- DebugLogManager
- MacroEngineTest
- Converters
- gradlew
- ExampleInstrumentedTest
- MacroEngine
- ExampleUnitTest
- rules/graphify.md
- workflows/graphify.md
- LocalModelManager
- Mirai AI
- BackupRepository
- RustFsUploader
- ModelDownloadWorker
- RustFsUploaderTest
- ChatNotificationHelper
- ChatMessageEntity
- DeviceContextManager
- MiraiToolManager

## God Nodes (most connected - your core abstractions)
1. `SettingsViewModel` - 29 edges
2. `UserPersonaEntity` - 28 edges
3. `ChatViewModel` - 27 edges
4. `ChatMessageEntity` - 23 edges
5. `CharacterEntity` - 22 edges
6. `InferenceConfigEntity` - 20 edges
7. `MiraiNavGraph()` - 20 edges
8. `SettingsRepository` - 16 edges
9. `ChatSessionDao` - 16 edges
10. `UserPersonaDao` - 16 edges

## Surprising Connections (you probably didn't know these)
- `MiraiNavGraph()` --calls--> `OpenAiRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/network/OpenAiRepository.kt
- `MiraiNavGraph()` --calls--> `BackupRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/domain/backup/BackupRepository.kt
- `MiraiNavGraph()` --calls--> `CharacterEditScreen()`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/ui/screen/character/CharacterEditScreen.kt
- `MiraiNavGraph()` --calls--> `CharacterEditViewModel`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/ui/screen/character/CharacterEditViewModel.kt
- `MiraiNavGraph()` --calls--> `CharacterListScreen()`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/ui/screen/character/CharacterListScreen.kt

## Import Cycles
- None detected.

## Communities (38 total, 8 thin omitted)

### Community 0 - "SettingsViewModel"
Cohesion: 0.06
Nodes (13): InferenceConfigDao, Flow, InferenceConfigEntity, AdvanceAndBackupState, BackupState, ExtraState, FiveNetwork, StateFlow (+5 more)

### Community 1 - "ChatViewModel"
Cohesion: 0.08
Nodes (17): ChatBubbleItem(), ChatScreen(), FullScreenImagePreviewDialog(), StreamingBubbleItem(), ThinkingProcessCard(), TypingDotsIndicator(), ChatUiState, ChatViewModel (+9 more)

### Community 2 - "UserPersonaEntity"
Cohesion: 0.09
Nodes (12): Flow, UserPersonaDao, UserPersonaEntity, CharacterCardItem(), ManagementPersonaCardItem(), ManagementScreen(), PersonaCardItem(), PersonaEditDialog() (+4 more)

### Community 3 - "ModelHubViewModel"
Cohesion: 0.06
Nodes (35): HuggingFaceModel, ModelCompatibility, LOW_MEMORY, MODERATE, OPTIMAL, IndicatorBadge(), Color, LocalFileCardItem() (+27 more)

### Community 4 - "CharacterEntity"
Cohesion: 0.10
Nodes (8): CharacterDao, Flow, CharacterEntity, ImageLoader, MiraiApplication, ContextBuilderTest, Application, ImageLoaderFactory

### Community 5 - "CharacterEditViewModel"
Cohesion: 0.12
Nodes (7): AvatarCropDialog(), CharacterEditScreen(), CharacterEditUiState, CharacterEditViewModel, Context, StateFlow, ViewModel

### Community 6 - "OpenAiRepository"
Cohesion: 0.08
Nodes (21): AccumulatedToolCall, FilterResult, Flow, JsonObject, Result, ModelFetchResult, OpenAiRepository, SingleTurnResult (+13 more)

### Community 7 - "MiraiNavGraph"
Cohesion: 0.07
Nodes (15): AppSettings, Flow, SettingsRepository, ThemeSettings, HuggingFacePageResult, HuggingFaceRepository, Result, Intent (+7 more)

### Community 8 - "InferenceState"
Cohesion: 0.18
Nodes (10): Error, InferenceState, Flow, Result, StateFlow, Loading, LocalInferenceEngine, Ready (+2 more)

### Community 9 - "ChatSessionDao"
Cohesion: 0.08
Nodes (10): ChatSessionDao, Flow, ChatSessionEntity, CharacterListScreen(), ChatSessionCardItem(), CharacterListUiState, CharacterListViewModel, ChatSessionItem (+2 more)

### Community 10 - "DataUrlFetcher"
Cohesion: 0.31
Nodes (6): DataUrlFetcher, Factory, ImageLoader, Fetcher, FetchResult, Options

### Community 11 - ".startGeneration"
Cohesion: 0.12
Nodes (10): getInstance(), Context, migrate(), MiraiDatabase, ChatGenerationManager, GenerationStreamState, Context, StateFlow (+2 more)

### Community 12 - "ImageUtils"
Cohesion: 0.27
Nodes (6): ImageUtils, Bitmap, ByteArray, Context, Uri, ProcessedImage

### Community 13 - "SettingsScreen.kt"
Cohesion: 0.18
Nodes (16): androidx, AnnotatedString, Color, MarkdownRenderer, AdvanceSettingsView(), BackupSettingsView(), ConfigCardItem(), ConfigEditorForm() (+8 more)

### Community 14 - "DebugLogManager"
Cohesion: 0.23
Nodes (5): DebugLogEntry, DebugLogManager, Gson, Result, StateFlow

### Community 17 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 28 - "LocalModelManager"
Cohesion: 0.16
Nodes (10): Context, Flow, Result, StateFlow, LocalModelManager, LocalModelStatus, ERROR, LOADED (+2 more)

### Community 29 - "Mirai AI"
Cohesion: 0.08
Nodes (23): 1. Bring Your Own Key (BYOK) and Custom Inference Providers, 2. Deep Character and Persona Management, 3. Real-Time Streaming and Reasoning / Thinking Process, 4. Multimodal Vision Support, 5. Hugging Face Model Hub, 6. Full Data Backup and Restore, 7. Modern Material 3 Design, Architecture and Tech Stack (+15 more)

### Community 30 - "BackupRepository"
Cohesion: 0.26
Nodes (6): BackupRepository, Gson, Result, Uri, BackupStats, MiraiBackupData

### Community 31 - "RustFsUploader"
Cohesion: 0.38
Nodes (3): ByteArray, Result, RustFsUploader

### Community 32 - "ModelDownloadWorker"
Cohesion: 0.40
Nodes (3): Result, ModelDownloadWorker, CoroutineWorker

### Community 34 - "ChatNotificationHelper"
Cohesion: 0.44
Nodes (3): ChatNotificationHelper, Bitmap, Context

### Community 35 - "ChatMessageEntity"
Cohesion: 0.12
Nodes (7): ChatMessageDao, Flow, ChatMessageEntity, Context, Intent, NotificationReplyReceiver, BroadcastReceiver

### Community 36 - "DeviceContextManager"
Cohesion: 0.30
Nodes (4): DeviceContextManager, Context, WeatherCache, Location

### Community 38 - "MiraiToolManager"
Cohesion: 0.33
Nodes (3): Context, JsonObject, MiraiToolManager

## Knowledge Gaps
- **59 isolated node(s):** `AccumulatedToolCall`, `Unloaded`, `Error`, `OPTIMAL`, `MODERATE` (+54 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MiraiNavGraph()` connect `MiraiNavGraph` to `SettingsViewModel`, `ChatViewModel`, `UserPersonaEntity`, `ModelHubViewModel`, `CharacterEditViewModel`, `OpenAiRepository`, `ChatSessionDao`, `SettingsScreen.kt`, `BackupRepository`?**
  _High betweenness centrality (0.308) - this node is a cross-community bridge._
- **Why does `UserPersonaEntity` connect `UserPersonaEntity` to `.startGeneration`, `LocalModelManager`, `CharacterEntity`, `OpenAiRepository`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Why does `ModelHubViewModel` connect `ModelHubViewModel` to `MiraiNavGraph`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `UserPersonaEntity` (e.g. with `.seedInitialDataIfNeeded()` and `.testBuildOpenAiMessagesMixedHistoryTextTurn()`) actually correct?**
  _`UserPersonaEntity` has 5 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `ChatMessageEntity` (e.g. with `.startGeneration()` and `.stopGeneration()`) actually correct?**
  _`ChatMessageEntity` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Are the 6 inferred relationships involving `CharacterEntity` (e.g. with `.seedInitialDataIfNeeded()` and `.saveCharacter()`) actually correct?**
  _`CharacterEntity` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AccumulatedToolCall`, `Unloaded`, `Error` to the rest of the system?**
  _59 weakly-connected nodes found - possible documentation gaps or missing edges._