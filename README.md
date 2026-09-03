# Mirai AI

Mirai AI is an open-source, high-performance native Android application designed as a private, highly customizable alternative to proprietary character chat platforms like Character.AI, Jan, and SillyTavern. Built entirely with Kotlin and Jetpack Compose, Mirai AI gives users complete freedom to chat and roleplay with AI characters using their own API keys (BYOK) or self-hosted local LLM servers.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Supported Providers and Local LLMs](#supported-providers-and-local-llms)
- [Architecture and Tech Stack](#architecture-and-tech-stack)
- [Building from Source](#building-from-source)
- [Configuration and Usage](#configuration-and-usage)
  - [Character Macros](#character-macros)
  - [Connecting to Local LLMs (Ollama / LM Studio)](#connecting-to-local-llms-ollama--lm-studio)
- [Backup and Data Portability](#backup-and-data-portability)
- [SEO and Comparison Highlights](#seo-and-comparison-highlights)
- [License](#license)

---

## Overview

Traditional AI roleplay applications often restrict user customization, enforce rigid content filters, store conversations on remote proprietary servers, and lock users into expensive subscriptions.

Mirai AI provides a transparent, client-side, privacy-first native Android experience:
- **Zero Lock-In**: Connect to any OpenAI-compatible API endpoint.
- **Privacy-First**: All characters, user personas, chat histories, and configuration profiles are stored locally in an encrypted Room SQLite database on your device.
- **No Censorship Gateways**: You control the inference provider, temperature, top-p, system directives, and model parameters.
- **Full Offline Persistence**: Your data stays on your device with complete local JSON backup and restore capabilities.

---

## Key Features

### 1. Bring Your Own Key (BYOK) and Custom Inference Providers
- Multi-profile management for API endpoints, API keys, and model parameters.
- Seamless compatibility with OpenAI, OpenRouter, DeepSeek, Groq, Together AI, Mistral, Perplexity, and more.
- Native support for local inference backends such as Ollama, LM Studio, vLLM, LocalAI, and text-generation-webui via standard OpenAI REST endpoints.
- Custom HTTP header injection for routing, reseller gateways, or enterprise authentication.

### 2. Deep Character and Persona Management
- Create and edit detailed character profiles including Name, Description, Personality, Scenario, System Directives/Impression, and First Message (Greeting).
- Create multiple user personas to switch identities across different roleplays seamlessly.
- Context injection engine with dynamic macro replacements (`{{char}}`, `{{user}}`).

### 3. Real-Time Streaming and Reasoning / Thinking Process
- High-efficiency Server-Sent Events (SSE) streaming for instantaneous token rendering.
- Intelligent handling of reasoning tokens and `<think>` blocks (compatible with DeepSeek-R1, Qwen-2.5-Coder, and similar thinking models).
- Advance Setting option to toggle the thinking stream:
  - **OFF**: Strips thinking output completely from chat view.
  - **ON**: Displays live collapsible thinking stream during generation, then auto-collapses cleanly when answer output begins.

### 4. Multimodal Vision Support
- Attach images directly to chat messages.
- Automatic local image downscaling, orientation correction (EXIF), and local storage caching.
- Integrated cloud object storage offloading for API calls (offloading images to S3-compatible endpoints like RustFS to optimize API token payloads).
- Permanent local chat rendering ensuring attached images never disappear even after cloud retention policies expire.
- Interactive fullscreen image viewer with multi-touch pinch-to-zoom (up to 5x) and pan gestures.

### 5. Hugging Face Model Hub
- Explore trending open-source Large Language Models (LLMs) and GGUF quantized models directly within the application.
- Filter by model category, download statistics, and parameters to discover new models for your local or cloud setup.

### 6. Full Data Backup and Restore
- Single-click full backup export to standard JSON format using Android Storage Access Framework (SAF).
- Flexible restore options: Merge restore or clean overwrite restore.
- Real-time backup inventory overview showing total characters, personas, sessions, messages, and configs.

### 7. Modern Material 3 Design
- Native Android interface built with 100% Jetpack Compose.
- Dynamic Material You (Monet) color palette support for Android 12+.
- Tailored Dark Slate and Deep Indigo themes designed for prolonged reading comfort.

---

## Supported Providers and Local LLMs

Mirai AI works with any API that conforms to the standard OpenAI `v1/chat/completions` specification:

- **Commercial Cloud APIs**:
  - OpenAI (GPT-4o, GPT-4o-mini, o1, o3-mini)
  - OpenRouter (Access 200+ models including Claude, Gemini, Llama, Mistral)
  - DeepSeek (DeepSeek-V3, DeepSeek-R1)
  - Groq (Ultra-fast inference for Llama 3.3, Mixtral)
  - Together AI, Fireworks AI, Nebius, Novita AI
  - Google Gemini (via OpenAI compatibility endpoint)
  - Cloudflare Workers AI

- **Local and Self-Hosted LLMs**:
  - Ollama (`http://<ip>:11434/v1`)
  - LM Studio (`http://<ip>:1234/v1`)
  - vLLM / Aphrodite Engine (`http://<ip>:8000/v1`)
  - text-generation-webui / Kobold.cpp (`http://<ip>:5001/v1`)
  - LocalAI / Jan Desktop

---

## Architecture and Tech Stack

Mirai AI is built according to Modern Android Architecture guidelines (Clean Architecture + MVVM + Unidirectional Data Flow):

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3, Material You / Monet theming)
- **Local Database**: AndroidX Room SQLite with Flow support
- **State Management**: Kotlin Coroutines, StateFlow, SharedFlow
- **Preferences**: Jetpack DataStore Preferences
- **Networking**: OkHttp 4, Retrofit 2, OkHttp SSE (Server-Sent Events)
- **Image Pipeline**: Coil Compose, AndroidX ExifInterface
- **Storage / Cloud**: AWS S3 Signature Version 4 (SigV4) client for RustFS / S3 object storage
- **Build System**: Gradle with Kotlin DSL and Version Catalogs (`libs.versions.toml`)

---

## Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17 or JDK 21
- Android SDK with API Level 34 or 35 installed
- Git

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/ryzumi/MiraiAI.git
cd MiraiAI
```

2. Build debug APK using Gradle Wrapper:
```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

3. Run unit tests:
```bash
# Windows
.\gradlew.bat testDebugUnitTest

# Linux / macOS
./gradlew testDebugUnitTest
```

4. Install the APK to a connected device or emulator:
```bash
# Windows
.\gradlew.bat installDebug

# Linux / macOS
./gradlew installDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Configuration and Usage

### Character Macros

You can use the following dynamic macro placeholders in Character Description, Personality, Scenario, First Message, and User Message prompts:

| Macro | Replacement |
| :--- | :--- |
| `{{char}}` | The character's display name |
| `{{user}}` | The active user persona's display name |
| `<USER>` | Alias for `{{user}}` |
| `<BOT>` | Alias for `{{char}}` |

### Connecting to Local LLMs (Ollama / LM Studio)

To connect Mirai AI on an Android device or emulator to a local LLM running on your computer:

1. **Same Wi-Fi Network**:
   - Find your computer's local IP address (e.g., `192.168.1.100`).
   - Configure Ollama to bind to `0.0.0.0` (`OLLAMA_HOST=0.0.0.0:11434`).
   - In Mirai AI Settings -> Inference:
     - Base URL: `http://192.168.1.100:11434/v1`
     - API Key: `ollama` (or any non-empty string)
     - Model ID: `llama3.2` (or your loaded model name)

2. **Android Emulator**:
   - Use `http://10.0.2.2:11434/v1` for Ollama or `http://10.0.2.2:1234/v1` for LM Studio.

3. **Via USB Reverse Port Forwarding (ADB)**:
   ```bash
   adb reverse tcp:11434 tcp:11434
   ```
   Then set Base URL in Mirai AI to `http://localhost:11434/v1`.

---

## Backup and Data Portability

Mirai AI includes a complete backup engine located in Settings -> Backup:

- **Export Data**: Creates a timestamped `.miraidb` package containing all characters, user personas, WebP avatar images, chat sessions, message logs, inference configs, and application preferences.
- **Import Data**: Reads any exported `.miraidb` (or legacy `.json`) backup file, extracts and restores avatar images locally. You can choose between:
  - **Merge**: Appends imported characters and sessions without removing existing items.
  - **Clean Restore**: Wipes local data before restoring to match the exact snapshot of the backup file.

---

## SEO and Comparison Highlights

| Feature | Mirai AI | Character.AI | SillyTavern (Web) | Jan / LM Studio Mobile |
| :--- | :--- | :--- | :--- | :--- |
| **Native Android App** | Yes (Jetpack Compose) | Yes (Proprietary) | No (Web / Node.js wrapper) | Limited / Desktop focused |
| **Bring Your Own Key (BYOK)** | Yes | No | Yes | Yes |
| **Local LLM Support** | Yes (Ollama, LM Studio, etc.) | No | Yes | Yes |
| **Data Privacy** | 100% Local SQLite Database | Server-side logged | Local / Host-dependent | Local |
| **Reasoning / Thinking Support** | Live Stream + Auto-collapse | No | Partial | Partial |
| **Vision / Multimodal** | Yes (S3 offload + local view) | Limited | Extension-dependent | Varies |
| **Full Portable Backup (.miraidb)** | Yes (SAF + Avatar Images) | No | Yes | No |
| **Open Source** | Yes | No | Yes | Yes |

---

## Keywords

`character-ai-alternative` `sillytavern-android` `ai-roleplay-client` `byok-ai-chat` `local-llm-android` `ollama-android-client` `lm-studio-client` `deepseek-r1-android` `openrouter-chat-app` `jetpack-compose-ai` `private-ai-chat` `uncensored-roleplay-client` `android-ai-frontend`

---

## License

This project is released under the [MIT License](LICENSE).
