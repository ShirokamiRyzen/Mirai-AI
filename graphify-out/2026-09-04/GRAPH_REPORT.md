# Graph Report - MiraiAI  (2026-09-04)

## Corpus Check
- 69 files · ~55,275 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 626 nodes · 956 edges · 42 communities (32 shown, 10 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 64 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d1145a74`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SettingsViewModel
- ChatViewModel
- UserPersonaEntity
- ModelHubViewModel
- MiraiApplication
- CharacterEditViewModel
- OpenAiRepository
- SettingsRepository
- InferenceState
- ChatSessionDao
- DataUrlFetcher
- ChatMessageEntity
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
- .startGeneration
- DeviceContextManager
- InferenceConfigEntity
- MiraiToolManager
- .parseMarkdown
- MarkdownRendererTest
- HuggingFaceRepository

## God Nodes (most connected - your core abstractions)
1. `ChatMessageEntity` - 30 edges
2. `SettingsViewModel` - 30 edges
3. `UserPersonaEntity` - 29 edges
4. `ChatViewModel` - 27 edges
5. `CharacterEntity` - 23 edges
6. `InferenceConfigEntity` - 20 edges
7. `MiraiNavGraph()` - 20 edges
8. `ImageUtils` - 18 edges
9. `ChatMessageDao` - 17 edges
10. `ChatSessionDao` - 17 edges

## Surprising Connections (you probably didn't know these)
- `MiraiNavGraph()` --calls--> `SettingsRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/datastore/SettingsRepository.kt
- `MiraiNavGraph()` --calls--> `HuggingFaceRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/network/HuggingFaceRepository.kt
- `MiraiNavGraph()` --calls--> `OpenAiRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/network/OpenAiRepository.kt
- `MiraiNavGraph()` --calls--> `BackupRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/domain/backup/BackupRepository.kt
- `MiraiNavGraph()` --calls--> `CharacterEditScreen()`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/ui/screen/character/CharacterEditScreen.kt

## Import Cycles
- None detected.

## Communities (42 total, 10 thin omitted)

### Community 0 - "SettingsViewModel"
Cohesion: 0.08
Nodes (10): AdvanceAndBackupState, BackupState, ExtraState, FiveNetwork, StateFlow, Uri, ViewModel, Quadruple (+2 more)

### Community 1 - "ChatViewModel"
Cohesion: 0.08
Nodes (21): ChatBubbleItem(), ChatScreen(), FullScreenImagePreviewDialog(), Modifier, scrollToBottom(), StreamingBubbleItem(), TextSelectionDialog(), ThinkingProcessCard() (+13 more)

### Community 2 - "UserPersonaEntity"
Cohesion: 0.08
Nodes (15): Flow, UserPersonaDao, UserPersonaEntity, MiraiDestinations, MiraiNavGraph(), CharacterCardItem(), ManagementPersonaCardItem(), ManagementScreen() (+7 more)

### Community 3 - "ModelHubViewModel"
Cohesion: 0.06
Nodes (35): HuggingFaceModel, ModelCompatibility, LOW_MEMORY, MODERATE, OPTIMAL, IndicatorBadge(), Color, LocalFileCardItem() (+27 more)

### Community 4 - "MiraiApplication"
Cohesion: 0.24
Nodes (5): Context, ImageLoader, MiraiApplication, Application, ImageLoaderFactory

### Community 5 - "CharacterEditViewModel"
Cohesion: 0.12
Nodes (7): AvatarCropDialog(), CharacterEditScreen(), CharacterEditUiState, CharacterEditViewModel, Context, StateFlow, ViewModel

### Community 6 - "OpenAiRepository"
Cohesion: 0.08
Nodes (21): AccumulatedToolCall, FilterResult, Flow, JsonObject, Result, ModelFetchResult, OpenAiRepository, SingleTurnResult (+13 more)

### Community 7 - "SettingsRepository"
Cohesion: 0.07
Nodes (13): AppSettings, Flow, SettingsRepository, ThemeSettings, Context, Intent, NotificationReplyReceiver, Intent (+5 more)

### Community 8 - "InferenceState"
Cohesion: 0.18
Nodes (10): Error, InferenceState, Flow, Result, StateFlow, Loading, LocalInferenceEngine, Ready (+2 more)

### Community 9 - "ChatSessionDao"
Cohesion: 0.09
Nodes (10): ChatSessionDao, Flow, ChatSessionEntity, CharacterListScreen(), ChatSessionCardItem(), CharacterListUiState, CharacterListViewModel, ChatSessionItem (+2 more)

### Community 10 - "DataUrlFetcher"
Cohesion: 0.31
Nodes (6): DataUrlFetcher, Factory, ImageLoader, Fetcher, FetchResult, Options

### Community 11 - "ChatMessageEntity"
Cohesion: 0.07
Nodes (8): CharacterDao, Flow, ChatMessageDao, Flow, CharacterEntity, ChatMessageEntity, Flow, ContextBuilderTest

### Community 12 - "ImageUtils"
Cohesion: 0.23
Nodes (6): ImageUtils, Bitmap, ByteArray, Context, Uri, ProcessedImage

### Community 13 - "SettingsScreen.kt"
Cohesion: 0.23
Nodes (14): androidx, AnnotatedString, AdvanceSettingsView(), BackupSettingsView(), ConfigCardItem(), ConfigEditorForm(), DebugLogCardItem(), Modifier (+6 more)

### Community 14 - "DebugLogManager"
Cohesion: 0.23
Nodes (5): DebugLogEntry, DebugLogManager, Gson, Result, StateFlow

### Community 17 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 28 - "LocalModelManager"
Cohesion: 0.20
Nodes (9): Context, Result, StateFlow, LocalModelManager, LocalModelStatus, ERROR, LOADED, LOADING (+1 more)

### Community 29 - "Mirai AI"
Cohesion: 0.07
Nodes (29): 1. Bring Your Own Key (BYOK) and Custom Inference Providers, 2. Deep Character and Persona Management, 3. Real-Time Streaming and Reasoning / Thinking Process, 4. Multimodal Vision Support, 5. Hugging Face Model Hub, 6. Full Data Backup and Restore, 7. Modern Material 3 Design, Architecture and Tech Stack (+21 more)

### Community 30 - "BackupRepository"
Cohesion: 0.20
Nodes (7): BackupRepository, Context, Gson, Result, Uri, BackupStats, MiraiBackupData

### Community 31 - "RustFsUploader"
Cohesion: 0.38
Nodes (3): ByteArray, Result, RustFsUploader

### Community 32 - "ModelDownloadWorker"
Cohesion: 0.40
Nodes (3): Result, ModelDownloadWorker, CoroutineWorker

### Community 34 - "ChatNotificationHelper"
Cohesion: 0.44
Nodes (3): ChatNotificationHelper, Bitmap, Context

### Community 35 - ".startGeneration"
Cohesion: 0.11
Nodes (10): getInstance(), Context, migrate(), MiraiDatabase, ChatGenerationManager, GenerationStreamState, Context, StateFlow (+2 more)

### Community 36 - "DeviceContextManager"
Cohesion: 0.24
Nodes (5): DeviceContextManager, Context, ResolvedLocation, WeatherCache, Location

### Community 37 - "InferenceConfigEntity"
Cohesion: 0.15
Nodes (3): InferenceConfigDao, Flow, InferenceConfigEntity

### Community 38 - "MiraiToolManager"
Cohesion: 0.33
Nodes (3): Context, JsonObject, MiraiToolManager

### Community 41 - "HuggingFaceRepository"
Cohesion: 0.38
Nodes (3): HuggingFacePageResult, HuggingFaceRepository, Result

## Knowledge Gaps
- **62 isolated node(s):** `AccumulatedToolCall`, `Unloaded`, `Error`, `OPTIMAL`, `MODERATE` (+57 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MiraiNavGraph()` connect `UserPersonaEntity` to `SettingsViewModel`, `ChatViewModel`, `ModelHubViewModel`, `CharacterEditViewModel`, `OpenAiRepository`, `SettingsRepository`, `HuggingFaceRepository`, `ChatSessionDao`, `SettingsScreen.kt`, `BackupRepository`?**
  _High betweenness centrality (0.291) - this node is a cross-community bridge._
- **Why does `ChatMessageEntity` connect `ChatMessageEntity` to `ChatViewModel`, `.startGeneration`, `OpenAiRepository`, `SettingsRepository`, `ChatSessionDao`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._
- **Why does `UserPersonaEntity` connect `UserPersonaEntity` to `ChatMessageEntity`, `.startGeneration`, `MiraiApplication`, `OpenAiRepository`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `ChatMessageEntity` (e.g. with `.startGeneration()` and `.stopGeneration()`) actually correct?**
  _`ChatMessageEntity` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Are the 6 inferred relationships involving `UserPersonaEntity` (e.g. with `.seedInitialDataIfNeeded()` and `.testBuildOpenAiMessagesMixedHistoryTextTurn()`) actually correct?**
  _`UserPersonaEntity` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `CharacterEntity` (e.g. with `.seedInitialDataIfNeeded()` and `.saveCharacter()`) actually correct?**
  _`CharacterEntity` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AccumulatedToolCall`, `Unloaded`, `Error` to the rest of the system?**
  _62 weakly-connected nodes found - possible documentation gaps or missing edges._