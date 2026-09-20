# Graph Report - MiraiAI  (2026-09-20)

## Corpus Check
- 86 files · ~105,735 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1631 nodes · 2860 edges · 96 communities (67 shown, 29 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 203 edges (avg confidence: 0.67)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `25ef00ac`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SettingsViewModel
- ChatViewModel
- UserPersonaEntity
- ModelHubViewModel
- ce
- CharacterEditViewModel
- OpenAiRepository
- SettingsRepository
- InferenceState
- ChatSessionDao
- DataUrlFetcher
- ChatMessageEntity
- ImageUtils
- ke
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
- mt
- .startGeneration
- DeviceContextManager
- Ct
- MiraiToolManager
- ee
- MarkdownRendererTest
- live2dcubismcore.min.js
- AboutScreen.kt
- index.min.js
- rt
- m
- et
- ns
- live2d.min.js
- Live2dViewerController
- TtsManager
- .generate
- r
- q
- SherpaOnnxTtsEngine
- je
- Ae
- MiraiDatabase
- c
- bt
- h
- pixi.min.js
- ChatScreen
- at
- ImageGenerationManager
- ut
- pt
- MiraiNavGraph
- da
- ie
- CharacterEntity
- InferenceConfigEntity
- pe
- ze
- .buildOpenAiMessages
- .updateParameters
- De
- LocalModelClassifier
- Be
- ImagePromptExtractor
- SettingsScreen.kt
- TtsDownloadState
- _
- .parseMarkdown
- Live2dViewer.kt
- ChatNotificationHelper
- MiraiApplication
- .streamInference
- TokenUtils
- DuckDuckGoScraper
- WebContentExtractor.kt
- .generateShaders
- Live2dManager
- se

## God Nodes (most connected - your core abstractions)
1. `Ae` - 53 edges
2. `je` - 36 edges
3. `ChatViewModel` - 36 edges
4. `ie` - 35 edges
5. `ke` - 33 edges
6. `ChatMessageEntity` - 32 edges
7. `CharacterEntity` - 31 edges
8. `UserPersonaEntity` - 31 edges
9. `SettingsViewModel` - 31 edges
10. `at` - 27 edges

## Surprising Connections (you probably didn't know these)
- `r()` --indirect_call--> `ti()`  [INFERRED]
  app/src/main/assets/live2d/pixi.min.js → app/src/main/assets/live2d/index.min.js
- `ja()` --indirect_call--> `L()`  [INFERRED]
  app/src/main/assets/live2d/live2dcubismcore.min.js → app/src/main/assets/live2d/index.min.js
- `createSettings()` --indirect_call--> `e()`  [INFERRED]
  app/src/main/assets/live2d/index.min.js → app/src/main/assets/live2d/live2d.min.js
- `unzip()` --indirect_call--> `e()`  [INFERRED]
  app/src/main/assets/live2d/index.min.js → app/src/main/assets/live2d/live2d.min.js
- `da()` --indirect_call--> `ii()`  [INFERRED]
  app/src/main/assets/live2d/live2dcubismcore.min.js → app/src/main/assets/live2d/index.min.js

## Import Cycles
- None detected.

## Communities (96 total, 29 thin omitted)

### Community 0 - "SettingsViewModel"
Cohesion: 0.07
Nodes (10): AdvanceAndBackupState, AdvanceToggles, BackupState, ExtraState, FiveNetwork, StateFlow, Uri, ViewModel (+2 more)

### Community 1 - "ChatViewModel"
Cohesion: 0.07
Nodes (13): ChatUiState, ChatViewModel, CoreChatData, DbData, InputState, Context, StateFlow, ViewModel (+5 more)

### Community 2 - "UserPersonaEntity"
Cohesion: 0.13
Nodes (9): Flow, UserPersonaDao, UserPersonaEntity, CharacterCardItem(), ManagementPersonaCardItem(), ManagementScreen(), PersonaCardItem(), PersonaEditDialog() (+1 more)

### Community 3 - "ModelHubViewModel"
Cohesion: 0.06
Nodes (34): HuggingFacePageResult, HuggingFaceRepository, com, Result, HuggingFaceModel, ModelCompatibility, LOW_MEMORY, MODERATE (+26 more)

### Community 4 - "ce"
Cohesion: 0.08
Nodes (3): ce, le, me

### Community 5 - "CharacterEditViewModel"
Cohesion: 0.09
Nodes (8): AvatarCropDialog(), CharacterEditScreen(), CharacterEditUiState, CharacterEditViewModel, Context, StateFlow, Uri, ViewModel

### Community 6 - "OpenAiRepository"
Cohesion: 0.14
Nodes (14): AccumulatedToolCall, FilterResult, Flow, JsonObject, Result, ModelFetchResult, OpenAiRepository, SingleTurnResult (+6 more)

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

### Community 13 - "ke"
Cohesion: 0.12
Nodes (10): cancelXHRs(), dt, ht, ke, p(), release(), resolveURL(), upload() (+2 more)

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
Cohesion: 0.06
Nodes (31): 1. Bring Your Own Key (BYOK) and Custom Inference Providers, 2. Deep Character and Persona Management, 3. Real-Time Streaming and Reasoning / Thinking Process, 4. Multimodal Vision Support, 5. Hugging Face Model Hub, 6. Full Data Backup and Restore, 7. Real-Time Web Search & Content Extractor (LLM Tool Calling), 8. Modern Material 3 Design (+23 more)

### Community 30 - "BackupRepository"
Cohesion: 0.19
Nodes (8): BackupRepository, ByteArray, Context, Gson, Result, Uri, BackupStats, MiraiBackupData

### Community 31 - "RustFsUploader"
Cohesion: 0.38
Nodes (3): ByteArray, Result, RustFsUploader

### Community 32 - "ModelDownloadWorker"
Cohesion: 0.40
Nodes (3): Result, ModelDownloadWorker, CoroutineWorker

### Community 35 - ".startGeneration"
Cohesion: 0.22
Nodes (4): ChatGenerationManager, GenerationStreamState, Context, StateFlow

### Community 36 - "DeviceContextManager"
Cohesion: 0.16
Nodes (6): DeviceContextManager, Context, PublicIpCache, ResolvedLocation, WeatherCache, Location

### Community 37 - "Ct"
Cohesion: 0.13
Nodes (3): Ct, lt, wt()

### Community 38 - "MiraiToolManager"
Cohesion: 0.36
Nodes (3): Context, JsonObject, MiraiToolManager

### Community 41 - "live2dcubismcore.min.js"
Cohesion: 0.15
Nodes (11): a(), assert(), B(), C(), d(), ia(), ja(), O() (+3 more)

### Community 42 - "AboutScreen.kt"
Cohesion: 0.35
Nodes (10): AboutAppView(), AboutScreen(), AppIconImage(), AppVersionInfo, ComponentLicenseCard(), getAppVersionInfo(), Context, Modifier (+2 more)

### Community 43 - "index.min.js"
Cohesion: 0.04
Nodes (25): ai(), cardanoAlgorithmForBezier(), cbrt(), createInternalModel(), directionToDegrees(), directionToRadian(), ge, Gt (+17 more)

### Community 45 - "m"
Cohesion: 0.23
Nodes (3): g(), log(), m()

### Community 46 - "et"
Cohesion: 0.06
Nodes (3): et, Yt, se()

### Community 47 - "ns"
Cohesion: 0.33
Nodes (7): k(), w(), b(), ns(), p(), rs(), X()

### Community 48 - "live2d.min.js"
Cohesion: 0.07
Nodes (19): b, a(), c(), d(), f(), g(), h(), i() (+11 more)

### Community 49 - "Live2dViewerController"
Cohesion: 0.20
Nodes (5): com, Modifier, Live2dViewer(), Live2dViewerController, WebView

### Community 50 - "TtsManager"
Cohesion: 0.19
Nodes (5): Context, MediaPlayer, StateFlow, TtsManager, Job

### Community 51 - ".generate"
Cohesion: 0.31
Nodes (5): Bitmap, Context, FloatArray, Result, MnnOnnxDiffusionEngine

### Community 52 - "r"
Cohesion: 0.17
Nodes (25): Ue, e(), a(), co(), Ds(), g(), h(), i() (+17 more)

### Community 53 - "q"
Cohesion: 0.19
Nodes (20): at(), ct(), dt(), et(), ft(), ht(), J(), K() (+12 more)

### Community 54 - "SherpaOnnxTtsEngine"
Cohesion: 0.14
Nodes (8): Context, FloatArray, MediaPlayer, Result, SherpaOnnxTtsEngine, LongArray, OrtEnvironment, ShortArray

### Community 56 - "Ae"
Cohesion: 0.09
Nodes (4): Ae, determinNextBlinkingTiming(), ft, updateParameters()

### Community 57 - "MiraiDatabase"
Cohesion: 0.18
Nodes (6): getInstance(), Context, migrate(), MiraiDatabase, RoomDatabase, SupportSQLiteDatabase

### Community 60 - "h"
Cohesion: 0.08
Nodes (5): d(), f, h(), it, j()

### Community 61 - "pixi.min.js"
Cohesion: 0.06
Nodes (18): Bo(), E(), Ea(), ee(), Gs(), Hs(), ks(), Lo() (+10 more)

### Community 62 - "ChatScreen"
Cohesion: 0.26
Nodes (12): cleanLive2dControlTags(), ChatBubbleItem(), ChatScreen(), FullScreenImagePreviewDialog(), com, Modifier, scrollToBottom(), StreamingBubbleItem() (+4 more)

### Community 64 - "ImageGenerationManager"
Cohesion: 0.24
Nodes (5): ImageGenerationManager, ImageGenProgress, Context, Result, StateFlow

### Community 65 - "ut"
Cohesion: 0.12
Nodes (8): cos(), degreesToRadian(), ii(), radianToDirection(), si(), sin(), te(), ut

### Community 67 - "MiraiNavGraph"
Cohesion: 0.15
Nodes (6): MiraiDestinations, MiraiNavGraph(), StateFlow, ViewModel, PersonaViewModel, NavHostController

### Community 68 - "da"
Cohesion: 0.14
Nodes (17): da(), bi(), ci(), di(), gi(), Ji(), Ki(), li() (+9 more)

### Community 69 - "ie"
Cohesion: 0.08
Nodes (8): createXHR(), E, ie, L(), loadMotion(), setupLive2DModel(), v, warn()

### Community 70 - "CharacterEntity"
Cohesion: 0.13
Nodes (4): CharacterDao, Flow, CharacterEntity, ContextBuilderTest

### Community 71 - "InferenceConfigEntity"
Cohesion: 0.18
Nodes (3): InferenceConfigDao, Flow, InferenceConfigEntity

### Community 74 - ".buildOpenAiMessages"
Cohesion: 0.24
Nodes (6): ContextBuilder, Context, OpenAiContentPart, OpenAiFunctionCall, OpenAiImageUrl, OpenAiToolCall

### Community 76 - "De"
Cohesion: 0.20
Nodes (5): createSettings(), De, K, readText(), unzip()

### Community 77 - "LocalModelClassifier"
Cohesion: 0.30
Nodes (6): LocalModelClassifier, LocalModelType, IMAGE_GEN, TEXT_LLM, VISION, VOICE_TTS

### Community 80 - "SettingsScreen.kt"
Cohesion: 0.24
Nodes (15): androidx, AnnotatedString, AdvanceSettingsView(), BackupSettingsView(), ConfigCardItem(), ConfigEditorForm(), DebugLogCardItem(), com (+7 more)

### Community 81 - "TtsDownloadState"
Cohesion: 0.29
Nodes (6): Completed, Downloading, Error, Idle, TtsDownloadState, VoicePreset

### Community 85 - "Live2dViewer.kt"
Cohesion: 0.28
Nodes (6): getMimeTypeForExtension(), getOptimizedImageStream(), handleLocalLive2dRequest(), Context, Live2dJsBridge, WebResourceResponse

### Community 90 - "ChatNotificationHelper"
Cohesion: 0.47
Nodes (3): ChatNotificationHelper, Bitmap, Context

### Community 91 - "MiraiApplication"
Cohesion: 0.25
Nodes (5): Context, ImageLoader, MiraiApplication, Application, ImageLoaderFactory

### Community 92 - ".streamInference"
Cohesion: 0.29
Nodes (4): Context, Flow, Result, LiteRtInferenceEngine

### Community 123 - "Live2dManager"
Cohesion: 0.16
Nodes (12): Error, Context, Uri, Live2dImportResult, Live2dManager, Success, Live2dExpressionInfo, Live2dModelCapabilities (+4 more)

## Knowledge Gaps
- **75 isolated node(s):** `AccumulatedToolCall`, `Unloaded`, `Error`, `Error`, `Live2dExpressionInfo` (+70 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **29 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MiraiNavGraph()` connect `MiraiNavGraph` to `SettingsViewModel`, `ChatViewModel`, `UserPersonaEntity`, `ModelHubViewModel`, `CharacterEditViewModel`, `OpenAiRepository`, `SettingsRepository`, `ChatSessionDao`, `AboutScreen.kt`, `SettingsScreen.kt`, `ChatScreen`, `BackupRepository`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Why does `Ue` connect `r` to `index.min.js`, `pixi.min.js`, `ns`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Why does `CharacterEntity` connect `CharacterEntity` to `UserPersonaEntity`, `.startGeneration`, `CharacterEditViewModel`, `ChatSessionDao`, `.buildOpenAiMessages`, `ChatMessageEntity`, `TtsManager`, `SherpaOnnxTtsEngine`, `MiraiApplication`, `.streamInference`?**
  _High betweenness centrality (0.031) - this node is a cross-community bridge._
- **What connects `AccumulatedToolCall`, `Unloaded`, `Error` to the rest of the system?**
  _75 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SettingsViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.06881720430107527 - nodes in this community are weakly interconnected._
- **Should `ChatViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07394957983193277 - nodes in this community are weakly interconnected._
- **Should `UserPersonaEntity` be split into smaller, more focused modules?**
  _Cohesion score 0.1282051282051282 - nodes in this community are weakly interconnected._