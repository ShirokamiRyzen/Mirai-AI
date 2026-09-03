# Graph Report - MiraiAI  (2026-09-01)

## Corpus Check
- 55 files · ~23,164 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 346 nodes · 501 edges · 29 communities (22 shown, 7 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 46 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- SettingsViewModel
- ChatMessageEntity
- UserPersonaEntity
- ModelHubViewModel
- CharacterEntity
- CharacterEditViewModel
- .buildOpenAiMessages
- SettingsRepository
- InferenceState
- ChatSessionDao
- DataUrlFetcher
- MiraiDatabase
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
- MiraiApplication

## God Nodes (most connected - your core abstractions)
1. `UserPersonaEntity` - 23 edges
2. `MiraiNavGraph()` - 19 edges
3. `CharacterEntity` - 18 edges
4. `SettingsViewModel` - 18 edges
5. `ChatMessageEntity` - 17 edges
6. `ChatViewModel` - 17 edges
7. `InferenceConfigEntity` - 16 edges
8. `CharacterEditViewModel` - 16 edges
9. `ChatSessionDao` - 12 edges
10. `InferenceConfigDao` - 12 edges

## Surprising Connections (you probably didn't know these)
- `MiraiNavGraph()` --calls--> `SettingsRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/datastore/SettingsRepository.kt
- `MiraiNavGraph()` --calls--> `HuggingFaceRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/network/HuggingFaceRepository.kt
- `MiraiNavGraph()` --calls--> `OpenAiRepository`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/data/network/OpenAiRepository.kt
- `MiraiNavGraph()` --calls--> `CharacterEditScreen()`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/ui/screen/character/CharacterEditScreen.kt
- `MiraiNavGraph()` --calls--> `CharacterEditViewModel`  [INFERRED]
  app/src/main/java/com/ryzumi/miraiai/ui/navigation/MiraiNavGraph.kt → app/src/main/java/com/ryzumi/miraiai/ui/screen/character/CharacterEditViewModel.kt

## Import Cycles
- None detected.

## Communities (29 total, 7 thin omitted)

### Community 0 - "SettingsViewModel"
Cohesion: 0.09
Nodes (9): InferenceConfigDao, Flow, InferenceConfigEntity, ExtraState, FiveNetwork, StateFlow, ViewModel, SettingsViewModel (+1 more)

### Community 1 - "ChatMessageEntity"
Cohesion: 0.09
Nodes (14): ChatMessageDao, Flow, ChatMessageEntity, ChatBubbleItem(), ChatScreen(), StreamingBubbleItem(), TypingDotsIndicator(), ChatUiState (+6 more)

### Community 2 - "UserPersonaEntity"
Cohesion: 0.10
Nodes (14): Flow, UserPersonaDao, UserPersonaEntity, MiraiDestinations, MiraiNavGraph(), CharacterCardItem(), ManagementScreen(), PersonaCardItem() (+6 more)

### Community 3 - "ModelHubViewModel"
Cohesion: 0.14
Nodes (10): HuggingFaceRepository, Result, HuggingFaceModel, LocalFileCardItem(), ModelCardItem(), ModelHubScreen(), StateFlow, ViewModel (+2 more)

### Community 4 - "CharacterEntity"
Cohesion: 0.18
Nodes (4): CharacterDao, Flow, CharacterEntity, ContextBuilderTest

### Community 5 - "CharacterEditViewModel"
Cohesion: 0.13
Nodes (6): CharacterEditScreen(), CharacterEditUiState, CharacterEditViewModel, Context, StateFlow, ViewModel

### Community 6 - ".buildOpenAiMessages"
Cohesion: 0.15
Nodes (10): Flow, Result, ModelFetchResult, OpenAiRepository, ContextBuilder, Context, OpenAiContentPart, OpenAiImageUrl (+2 more)

### Community 7 - "SettingsRepository"
Cohesion: 0.12
Nodes (8): AppSettings, Flow, SettingsRepository, ThemeSettings, MainActivity, MiraiAITheme(), Bundle, ComponentActivity

### Community 8 - "InferenceState"
Cohesion: 0.18
Nodes (10): Error, InferenceState, Flow, Result, StateFlow, Loading, LocalInferenceEngine, Ready (+2 more)

### Community 9 - "ChatSessionDao"
Cohesion: 0.10
Nodes (10): ChatSessionDao, Flow, ChatSessionEntity, CharacterListScreen(), ChatSessionCardItem(), CharacterListUiState, CharacterListViewModel, ChatSessionItem (+2 more)

### Community 10 - "DataUrlFetcher"
Cohesion: 0.31
Nodes (6): DataUrlFetcher, Factory, ImageLoader, Fetcher, FetchResult, Options

### Community 11 - "MiraiDatabase"
Cohesion: 0.18
Nodes (6): getInstance(), Context, migrate(), MiraiDatabase, RoomDatabase, SupportSQLiteDatabase

### Community 12 - "ImageUtils"
Cohesion: 0.19
Nodes (8): Result, ModelDownloadWorker, ImageUtils, Context, Bitmap, ByteArray, CoroutineWorker, Uri

### Community 13 - "SettingsScreen"
Cohesion: 0.20
Nodes (13): androidx, AnnotatedString, MarkdownRenderer, ConfigCardItem(), ConfigEditorForm(), DebugLogCardItem(), DebugTracingView(), SettingsScreen() (+5 more)

### Community 14 - "DebugLogManager"
Cohesion: 0.23
Nodes (5): DebugLogEntry, DebugLogManager, Result, StateFlow, Gson

### Community 17 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 28 - "MiraiApplication"
Cohesion: 0.29
Nodes (4): ImageLoader, MiraiApplication, Application, ImageLoaderFactory

## Knowledge Gaps
- **9 isolated node(s):** `Unloaded`, `Error`, `MiraiDestinations`, `Tuple5Chat`, `FiveNetwork` (+4 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MiraiNavGraph()` connect `UserPersonaEntity` to `SettingsViewModel`, `ChatMessageEntity`, `ModelHubViewModel`, `CharacterEditViewModel`, `.buildOpenAiMessages`, `SettingsRepository`, `ChatSessionDao`, `SettingsScreen`?**
  _High betweenness centrality (0.359) - this node is a cross-community bridge._
- **Why does `UserPersonaEntity` connect `UserPersonaEntity` to `ChatMessageEntity`, `MiraiApplication`, `CharacterEntity`, `.buildOpenAiMessages`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Why does `CharacterEntity` connect `CharacterEntity` to `ChatMessageEntity`, `UserPersonaEntity`, `CharacterEditViewModel`, `.buildOpenAiMessages`, `ChatSessionDao`, `MiraiApplication`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `UserPersonaEntity` (e.g. with `.seedInitialDataIfNeeded()` and `.testBuildOpenAiMessagesSequence()`) actually correct?**
  _`UserPersonaEntity` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Are the 17 inferred relationships involving `MiraiNavGraph()` (e.g. with `.onCreate()` and `SettingsRepository`) actually correct?**
  _`MiraiNavGraph()` has 17 INFERRED edges - model-reasoned connections that need verification._
- **Are the 5 inferred relationships involving `CharacterEntity` (e.g. with `.seedInitialDataIfNeeded()` and `.saveCharacter()`) actually correct?**
  _`CharacterEntity` has 5 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `ChatMessageEntity` (e.g. with `.insertFirstMessageIfPresent()` and `.testBuildOpenAiMessagesSequence()`) actually correct?**
  _`ChatMessageEntity` has 3 INFERRED edges - model-reasoned connections that need verification._