# Graph Report - MiraiAI  (2026-09-09)

## Corpus Check
- 69 files · ~59,024 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 641 nodes · 986 edges · 45 communities (34 shown, 11 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 67 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `57a9d301`
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
- MiraiNavGraph
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
- CharacterEntity
- PersonaViewModel
- ManagementScreen
- ContextBuilderTest

## God Nodes (most connected - your core abstractions)
1. `ChatMessageEntity` - 31 edges
2. `SettingsViewModel` - 31 edges
3. `UserPersonaEntity` - 30 edges
4. `DeviceContextManager` - 27 edges
5. `ChatViewModel` - 27 edges
6. `CharacterEntity` - 24 edges
7. `InferenceConfigEntity` - 20 edges
8. `MiraiNavGraph()` - 20 edges
9. `ImageUtils` - 18 edges
10. `SettingsRepository` - 17 edges

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

## Communities (45 total, 11 thin omitted)

### Community 0 - "SettingsViewModel"
Cohesion: 0.08
Nodes (10): AdvanceAndBackupState, AdvanceToggles, BackupState, ExtraState, FiveNetwork, StateFlow, Uri, ViewModel (+2 more)

### Community 1 - "ChatViewModel"
Cohesion: 0.08
Nodes (21): ChatBubbleItem(), ChatScreen(), FullScreenImagePreviewDialog(), Modifier, scrollToBottom(), StreamingBubbleItem(), TextSelectionDialog(), ThinkingProcessCard() (+13 more)

### Community 2 - "UserPersonaEntity"
Cohesion: 0.17
Nodes (3): Flow, UserPersonaDao, UserPersonaEntity

### Community 3 - "ModelHubViewModel"
Cohesion: 0.06
Nodes (35): HuggingFaceModel, ModelCompatibility, LOW_MEMORY, MODERATE, OPTIMAL, IndicatorBadge(), Color, LocalFileCardItem() (+27 more)

### Community 4 - "MiraiApplication"
Cohesion: 0.25
Nodes (5): Context, ImageLoader, MiraiApplication, Application, ImageLoaderFactory

### Community 5 - "CharacterEditViewModel"
Cohesion: 0.12
Nodes (7): AvatarCropDialog(), CharacterEditScreen(), CharacterEditUiState, CharacterEditViewModel, Context, StateFlow, ViewModel

### Community 6 - "OpenAiRepository"
Cohesion: 0.08
Nodes (21): AccumulatedToolCall, FilterResult, Flow, JsonObject, Result, ModelFetchResult, OpenAiRepository, SingleTurnResult (+13 more)

### Community 7 - "MiraiNavGraph"
Cohesion: 0.05
Nodes (19): AppSettings, Flow, SettingsRepository, ThemeSettings, HuggingFacePageResult, HuggingFaceRepository, Result, Context (+11 more)

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
Cohesion: 0.15
Nodes (3): ChatMessageDao, Flow, ChatMessageEntity

### Community 12 - "ImageUtils"
Cohesion: 0.23
Nodes (6): ImageUtils, Bitmap, ByteArray, Context, Uri, ProcessedImage

### Community 13 - "SettingsScreen.kt"
Cohesion: 0.25
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
Cohesion: 0.16
Nodes (6): DeviceContextManager, Context, PublicIpCache, ResolvedLocation, WeatherCache, Location

### Community 37 - "InferenceConfigEntity"
Cohesion: 0.15
Nodes (3): InferenceConfigDao, Flow, InferenceConfigEntity

### Community 38 - "MiraiToolManager"
Cohesion: 0.33
Nodes (3): Context, JsonObject, MiraiToolManager

### Community 41 - "CharacterEntity"
Cohesion: 0.18
Nodes (4): CharacterDao, Flow, CharacterEntity, Flow

### Community 42 - "PersonaViewModel"
Cohesion: 0.22
Nodes (3): StateFlow, ViewModel, PersonaViewModel

### Community 43 - "ManagementScreen"
Cohesion: 0.39
Nodes (6): CharacterCardItem(), ManagementPersonaCardItem(), ManagementScreen(), PersonaCardItem(), PersonaEditDialog(), PersonaListScreen()

## Knowledge Gaps
- **62 isolated node(s):** `AccumulatedToolCall`, `Unloaded`, `Error`, `OPTIMAL`, `MODERATE` (+57 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MiraiNavGraph()` connect `MiraiNavGraph` to `SettingsViewModel`, `ChatViewModel`, `ModelHubViewModel`, `CharacterEditViewModel`, `OpenAiRepository`, `ChatSessionDao`, `PersonaViewModel`, `ManagementScreen`, `SettingsScreen.kt`, `BackupRepository`?**
  _High betweenness centrality (0.280) - this node is a cross-community bridge._
- **Why does `ChatMessageEntity` connect `ChatMessageEntity` to `ChatViewModel`, `.startGeneration`, `OpenAiRepository`, `MiraiNavGraph`, `CharacterEntity`, `ChatSessionDao`, `ContextBuilderTest`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Why does `UserPersonaEntity` connect `UserPersonaEntity` to `.startGeneration`, `MiraiApplication`, `OpenAiRepository`, `CharacterEntity`, `PersonaViewModel`, `ManagementScreen`, `ContextBuilderTest`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Are the 10 inferred relationships involving `ChatMessageEntity` (e.g. with `.startGeneration()` and `.stopGeneration()`) actually correct?**
  _`ChatMessageEntity` has 10 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `UserPersonaEntity` (e.g. with `.onCreate()` and `.testBuildOpenAiMessagesMixedHistoryTextTurn()`) actually correct?**
  _`UserPersonaEntity` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `AccumulatedToolCall`, `Unloaded`, `Error` to the rest of the system?**
  _62 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SettingsViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07671957671957672 - nodes in this community are weakly interconnected._