# Graph Report - MiraiAI  (2026-09-13)

## Corpus Check
- 77 files · ~88,130 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1493 nodes · 2607 edges · 90 communities (66 shown, 24 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 200 edges (avg confidence: 0.67)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6f8e3f05`
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
- SettingsScreen
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
- et
- AboutScreen.kt
- index.min.js
- da
- mt
- Yt
- c
- live2d.min.js
- live2dcubismcore.min.js
- x
- ns
- r
- q
- rt
- je
- ModelHubScreen
- ie
- ManagementScreen
- h
- pixi.min.js
- ChatScreen
- at
- HuggingFaceRepository
- ke
- pt
- PersonaViewModel
- DownloadStatus
- ModelHubFilter
- CharacterEntity
- lt
- pe
- ModelCompatibility
- TokenUtils
- tt
- Live2dViewerController
- Live2dViewer.kt
- MiraiNavGraph
- ChatUiState
- it
- _
- ce
- ut
- De
- Ae
- MiraiDatabase
- Live2dManager
- .updateParameters
- se

## God Nodes (most connected - your core abstractions)
1. `Ae` - 53 edges
2. `je` - 36 edges
3. `ie` - 35 edges
4. `ke` - 33 edges
5. `ChatViewModel` - 33 edges
6. `ChatMessageEntity` - 31 edges
7. `SettingsViewModel` - 31 edges
8. `UserPersonaEntity` - 30 edges
9. `at` - 27 edges
10. `DeviceContextManager` - 27 edges

## Surprising Connections (you probably didn't know these)
- `ja()` --indirect_call--> `L()`  [INFERRED]
  app/src/main/assets/live2d/live2dcubismcore.min.js → app/src/main/assets/live2d/index.min.js
- `createSettings()` --indirect_call--> `e()`  [INFERRED]
  app/src/main/assets/live2d/index.min.js → app/src/main/assets/live2d/live2d.min.js
- `unzip()` --indirect_call--> `e()`  [INFERRED]
  app/src/main/assets/live2d/index.min.js → app/src/main/assets/live2d/live2d.min.js
- `r()` --indirect_call--> `ti()`  [INFERRED]
  app/src/main/assets/live2d/pixi.min.js → app/src/main/assets/live2d/index.min.js
- `da()` --indirect_call--> `ii()`  [INFERRED]
  app/src/main/assets/live2d/live2dcubismcore.min.js → app/src/main/assets/live2d/index.min.js

## Import Cycles
- None detected.

## Communities (90 total, 24 thin omitted)

### Community 0 - "SettingsViewModel"
Cohesion: 0.08
Nodes (10): AdvanceAndBackupState, AdvanceToggles, BackupState, ExtraState, FiveNetwork, StateFlow, Uri, ViewModel (+2 more)

### Community 1 - "ChatViewModel"
Cohesion: 0.08
Nodes (12): ChatViewModel, CoreChatData, DbData, InputState, Context, StateFlow, ViewModel, LocalManagerState (+4 more)

### Community 2 - "UserPersonaEntity"
Cohesion: 0.17
Nodes (3): Flow, UserPersonaDao, UserPersonaEntity

### Community 3 - "ModelHubViewModel"
Cohesion: 0.23
Nodes (4): HuggingFaceModel, StateFlow, ViewModel, ModelHubViewModel

### Community 4 - "MiraiApplication"
Cohesion: 0.25
Nodes (5): Context, ImageLoader, MiraiApplication, Application, ImageLoaderFactory

### Community 5 - "CharacterEditViewModel"
Cohesion: 0.10
Nodes (8): AvatarCropDialog(), CharacterEditScreen(), CharacterEditUiState, CharacterEditViewModel, Context, StateFlow, Uri, ViewModel

### Community 6 - "OpenAiRepository"
Cohesion: 0.10
Nodes (20): AccumulatedToolCall, FilterResult, Flow, JsonObject, Result, ModelFetchResult, OpenAiRepository, SingleTurnResult (+12 more)

### Community 7 - "SettingsRepository"
Cohesion: 0.07
Nodes (13): AppSettings, Flow, SettingsRepository, ThemeSettings, Context, Intent, NotificationReplyReceiver, Intent (+5 more)

### Community 8 - "InferenceState"
Cohesion: 0.18
Nodes (10): Error, InferenceState, Flow, Result, StateFlow, Loading, LocalInferenceEngine, Ready (+2 more)

### Community 9 - "ChatSessionDao"
Cohesion: 0.08
Nodes (10): ChatSessionDao, Flow, ChatSessionEntity, CharacterListScreen(), ChatSessionCardItem(), CharacterListUiState, CharacterListViewModel, ChatSessionItem (+2 more)

### Community 10 - "DataUrlFetcher"
Cohesion: 0.31
Nodes (6): DataUrlFetcher, Factory, ImageLoader, Fetcher, FetchResult, Options

### Community 11 - "ChatMessageEntity"
Cohesion: 0.14
Nodes (4): ChatMessageDao, Flow, ChatMessageEntity, Flow

### Community 12 - "ImageUtils"
Cohesion: 0.23
Nodes (6): ImageUtils, Bitmap, ByteArray, Context, Uri, ProcessedImage

### Community 13 - "SettingsScreen"
Cohesion: 0.25
Nodes (14): androidx, AnnotatedString, AdvanceSettingsView(), BackupSettingsView(), ConfigCardItem(), ConfigEditorForm(), DebugLogCardItem(), com (+6 more)

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
Cohesion: 0.47
Nodes (3): ChatNotificationHelper, Bitmap, Context

### Community 35 - ".startGeneration"
Cohesion: 0.22
Nodes (4): ChatGenerationManager, GenerationStreamState, Context, StateFlow

### Community 36 - "DeviceContextManager"
Cohesion: 0.16
Nodes (6): DeviceContextManager, Context, PublicIpCache, ResolvedLocation, WeatherCache, Location

### Community 37 - "InferenceConfigEntity"
Cohesion: 0.15
Nodes (3): InferenceConfigDao, Flow, InferenceConfigEntity

### Community 38 - "MiraiToolManager"
Cohesion: 0.33
Nodes (3): Context, JsonObject, MiraiToolManager

### Community 42 - "AboutScreen.kt"
Cohesion: 0.38
Nodes (9): AboutAppView(), AppIconImage(), AppVersionInfo, ComponentLicenseCard(), getAppVersionInfo(), Context, Modifier, OpenSourceComponent (+1 more)

### Community 43 - "index.min.js"
Cohesion: 0.05
Nodes (24): ai(), cardanoAlgorithmForBezier(), cbrt(), createInternalModel(), directionToDegrees(), directionToRadian(), ge, Gt (+16 more)

### Community 44 - "da"
Cohesion: 0.14
Nodes (17): da(), bi(), ci(), di(), gi(), Ji(), Ki(), li() (+9 more)

### Community 46 - "Yt"
Cohesion: 0.06
Nodes (5): Ct, ee, wt(), Yt, se()

### Community 48 - "live2d.min.js"
Cohesion: 0.07
Nodes (20): b, a(), c(), d(), e(), f(), g(), h() (+12 more)

### Community 49 - "live2dcubismcore.min.js"
Cohesion: 0.15
Nodes (11): a(), assert(), B(), C(), d(), ia(), ja(), O() (+3 more)

### Community 50 - "x"
Cohesion: 0.18
Nodes (5): resolveURL(), unzip(), upload(), x, x()

### Community 51 - "ns"
Cohesion: 0.33
Nodes (7): k(), w(), b(), ns(), p(), rs(), X()

### Community 52 - "r"
Cohesion: 0.18
Nodes (23): ti(), Ue, a(), co(), g(), h(), i(), is() (+15 more)

### Community 53 - "q"
Cohesion: 0.19
Nodes (20): at(), ct(), dt(), et(), ft(), ht(), J(), K() (+12 more)

### Community 57 - "ModelHubScreen"
Cohesion: 0.31
Nodes (9): IndicatorBadge(), Color, LocalFileCardItem(), ModelCardItem(), ModelDetailDialog(), ModelHubScreen(), ModelDownloadState, ModelHubUiState (+1 more)

### Community 58 - "ie"
Cohesion: 0.08
Nodes (8): createXHR(), E, ie, L(), loadMotion(), setupLive2DModel(), v, warn()

### Community 59 - "ManagementScreen"
Cohesion: 0.39
Nodes (6): CharacterCardItem(), ManagementPersonaCardItem(), ManagementScreen(), PersonaCardItem(), PersonaEditDialog(), PersonaListScreen()

### Community 60 - "h"
Cohesion: 0.07
Nodes (6): f, g(), h(), j(), log(), m()

### Community 61 - "pixi.min.js"
Cohesion: 0.05
Nodes (20): Bo(), Ds(), E(), Ea(), ee(), Gs(), Hs(), ks() (+12 more)

### Community 62 - "ChatScreen"
Cohesion: 0.32
Nodes (11): cleanLive2dControlTags(), ChatBubbleItem(), ChatScreen(), FullScreenImagePreviewDialog(), Modifier, scrollToBottom(), StreamingBubbleItem(), TextSelectionDialog() (+3 more)

### Community 63 - "at"
Cohesion: 0.06
Nodes (6): at, Be, bt(), createCoreModel(), getEasingSine(), te()

### Community 64 - "HuggingFaceRepository"
Cohesion: 0.38
Nodes (3): HuggingFacePageResult, HuggingFaceRepository, Result

### Community 65 - "ke"
Cohesion: 0.13
Nodes (7): cancelXHRs(), dt, ht, ke, kt, p(), release()

### Community 67 - "PersonaViewModel"
Cohesion: 0.22
Nodes (3): StateFlow, ViewModel, PersonaViewModel

### Community 68 - "DownloadStatus"
Cohesion: 0.33
Nodes (6): DownloadStatus, COMPLETED, DOWNLOADING, FAILED, IDLE, PAUSED

### Community 69 - "ModelHubFilter"
Cohesion: 0.33
Nodes (6): ModelHubFilter, ALL, DOWNLOADED, IMAGE_GEN, TEXT_GGUF, VISION

### Community 70 - "CharacterEntity"
Cohesion: 0.13
Nodes (4): CharacterDao, Flow, CharacterEntity, ContextBuilderTest

### Community 72 - "pe"
Cohesion: 0.21
Nodes (3): pe, fe(), ve()

### Community 73 - "ModelCompatibility"
Cohesion: 0.40
Nodes (4): ModelCompatibility, LOW_MEMORY, MODERATE, OPTIMAL

### Community 76 - "Live2dViewerController"
Cohesion: 0.20
Nodes (5): com, Modifier, Live2dViewer(), Live2dViewerController, WebView

### Community 77 - "Live2dViewer.kt"
Cohesion: 0.28
Nodes (6): getMimeTypeForExtension(), getOptimizedImageStream(), handleLocalLive2dRequest(), Context, Live2dJsBridge, WebResourceResponse

### Community 78 - "MiraiNavGraph"
Cohesion: 0.40
Nodes (4): MiraiDestinations, MiraiNavGraph(), AboutScreen(), NavHostController

### Community 83 - "ce"
Cohesion: 0.08
Nodes (3): ce, le, me

### Community 84 - "ut"
Cohesion: 0.15
Nodes (6): cos(), degreesToRadian(), ii(), radianToDirection(), sin(), ut

### Community 86 - "De"
Cohesion: 0.20
Nodes (5): createSettings(), De, K, readText(), si()

### Community 104 - "Ae"
Cohesion: 0.09
Nodes (4): Ae, determinNextBlinkingTiming(), ft, updateParameters()

### Community 115 - "MiraiDatabase"
Cohesion: 0.18
Nodes (6): getInstance(), Context, migrate(), MiraiDatabase, RoomDatabase, SupportSQLiteDatabase

### Community 123 - "Live2dManager"
Cohesion: 0.16
Nodes (12): Error, Context, Uri, Live2dImportResult, Live2dManager, Success, Live2dExpressionInfo, Live2dModelCapabilities (+4 more)

## Knowledge Gaps
- **64 isolated node(s):** `AccumulatedToolCall`, `Unloaded`, `Error`, `Error`, `Live2dExpressionInfo` (+59 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MiraiNavGraph()` connect `MiraiNavGraph` to `HuggingFaceRepository`, `ChatViewModel`, `SettingsViewModel`, `ModelHubViewModel`, `PersonaViewModel`, `CharacterEditViewModel`, `OpenAiRepository`, `SettingsRepository`, `ChatSessionDao`, `SettingsScreen`, `ChatScreen`, `ModelHubScreen`, `ManagementScreen`, `BackupRepository`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `Ue` connect `r` to `ns`, `index.min.js`, `pixi.min.js`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **Why does `Ae` connect `Ae` to `.doDrawModel`, `index.min.js`, `.updateParameters`, `se`?**
  _High betweenness centrality (0.029) - this node is a cross-community bridge._
- **What connects `AccumulatedToolCall`, `Unloaded`, `Error` to the rest of the system?**
  _64 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SettingsViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07671957671957672 - nodes in this community are weakly interconnected._
- **Should `ChatViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07741935483870968 - nodes in this community are weakly interconnected._
- **Should `CharacterEditViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.10333333333333333 - nodes in this community are weakly interconnected._