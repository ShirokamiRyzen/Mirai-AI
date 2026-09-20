package com.ryzumi.miraiai.domain.tts

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class TtsDownloadState {
    object Idle : TtsDownloadState()
    data class Downloading(val progress: Float, val downloadedMb: Double, val totalMb: Double) : TtsDownloadState()
    object Completed : TtsDownloadState()
    data class Error(val message: String) : TtsDownloadState()
}

data class VoicePreset(
    val id: String,
    val name: String,
    val language: String,
    val gender: String
)

object TtsManager {
    private const val TAG = "MiraiTTS"

    // Default download URL for Kokoro-82M ONNX model
    private const val KOKORO_82M_DOWNLOAD_URL =
        "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/model_quantized.onnx"
    private const val KOKORO_MODEL_FILENAME = "kokoro-82m.onnx"

    val KOKORO_VOICE_PRESETS = listOf(
        VoicePreset("id_kawaii", "Alya (Indonesian Anime Imut)", "id-ID", "Female"),
        VoicePreset("id_manis", "Nami (Indonesian Lembut / Manis)", "id-ID", "Female"),
        VoicePreset("id_ceria", "Lia (Indonesian Ceria / Riang)", "id-ID", "Female"),
        VoicePreset("id_putri", "Putri (Indonesian Alami / Santai)", "id-ID", "Female"),
        VoicePreset("id_bima", "Bima (Indonesian Cowok / Cool)", "id-ID", "Male"),
        VoicePreset("jf_alpha", "Alpha (Japanese Anime)", "ja-JP", "Female"),
        VoicePreset("jf_gongitsune", "Gongitsune (Japanese Soft)", "ja-JP", "Female"),
        VoicePreset("jm_kumo", "Kumo (Japanese Calm)", "ja-JP", "Male"),
        VoicePreset("af_heart", "Heart (Warm / Gentle)", "en-US", "Female"),
        VoicePreset("af_bella", "Bella (Soft / Cute)", "en-US", "Female"),
        VoicePreset("af_sarah", "Sarah (Casual / Natural)", "en-US", "Female"),
        VoicePreset("af_nicole", "Nicole (Whisper / Calm)", "en-US", "Female"),
        VoicePreset("af_sky", "Sky (Bright / Cheerful)", "en-US", "Female"),
        VoicePreset("am_adam", "Adam (Natural / Neutral)", "en-US", "Male"),
        VoicePreset("am_michael", "Michael (Deep / Authoritative)", "en-US", "Male"),
        VoicePreset("bf_emma", "Emma (British Accent)", "en-GB", "Female"),
        VoicePreset("bf_isabella", "Isabella (British Soft)", "en-GB", "Female"),
        VoicePreset("bm_george", "George (British Accent)", "en-GB", "Male"),
        VoicePreset("bm_lewis", "Lewis (British Natural)", "en-GB", "Male"),
        VoicePreset("system", "System Default Voice", "auto", "Any")
    )

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _downloadState = MutableStateFlow<TtsDownloadState>(TtsDownloadState.Idle)
    val downloadState: StateFlow<TtsDownloadState> = _downloadState.asStateFlow()

    private val _isKokoroLoaded = MutableStateFlow(false)
    val isKokoroLoaded: StateFlow<Boolean> = _isKokoroLoaded.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // System TextToSpeech instance
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isInitializing = false
    private val initCallbacks = mutableListOf<() -> Unit>()
    private val initErrorCallbacks = mutableListOf<(String) -> Unit>()
    private var mediaPlayer: MediaPlayer? = null
    private var activeSpeechCallback: (() -> Unit)? = null

    fun getTtsModelsDir(context: Context): File {
        val dir = File(context.filesDir, "models/tts/kokoro")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getKokoroModelFile(context: Context): File {
        val defaultFile = File(getTtsModelsDir(context), KOKORO_MODEL_FILENAME)
        if (defaultFile.exists() && defaultFile.length() > 10 * 1024 * 1024) {
            return defaultFile
        }
        val hubModelsDir = File(context.filesDir, "models")
        if (hubModelsDir.exists()) {
            val hubKokoroFile = hubModelsDir.listFiles()?.firstOrNull { file ->
                file.isFile && file.name.contains("kokoro", ignoreCase = true) &&
                        file.name.endsWith(".onnx", ignoreCase = true) &&
                        file.length() > 10 * 1024 * 1024
            }
            if (hubKokoroFile != null) {
                return hubKokoroFile
            }
        }
        return defaultFile
    }

    fun getSelectedVoiceModelFile(context: Context, modelName: String?): File? {
        if (modelName.isNullOrBlank() || modelName.equals("none", ignoreCase = true)) {
            return null
        }
        val modelsDir = File(context.filesDir, "models")
        if (modelsDir.exists()) {
            val exact = File(modelsDir, modelName)
            if (exact.exists() && exact.length() > 0) return exact
            val match = modelsDir.listFiles()?.firstOrNull { it.isFile && (it.name.equals(modelName, ignoreCase = true) || it.name.contains(modelName, ignoreCase = true)) }
            if (match != null) return match
        }
        val ttsDir = getTtsModelsDir(context)
        if (ttsDir.exists()) {
            val exactTts = File(ttsDir, modelName)
            if (exactTts.exists() && exactTts.length() > 0) return exactTts
            val matchTts = ttsDir.listFiles()?.firstOrNull { it.isFile && (it.name.equals(modelName, ignoreCase = true) || it.name.contains(modelName, ignoreCase = true)) }
            if (matchTts != null) return matchTts
        }
        return null
    }

    fun isKokoroModelDownloaded(context: Context): Boolean {
        val file = getKokoroModelFile(context)
        return file.exists() && file.length() > 10 * 1024 * 1024 // At least 10MB
    }

    fun getKokoroModelSizeText(context: Context): String {
        val file = getKokoroModelFile(context)
        if (!file.exists()) return "Not Downloaded"
        val mb = file.length() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun downloadKokoroModel(context: Context) {
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch {
            _downloadState.value = TtsDownloadState.Downloading(0f, 0.0, 82.0)
            try {
                val targetFile = getKokoroModelFile(context)
                val tempFile = File(targetFile.parentFile, "${KOKORO_MODEL_FILENAME}.tmp")

                val request = Request.Builder()
                    .url(KOKORO_82M_DOWNLOAD_URL)
                    .header("User-Agent", "Mozilla/5.0 (Android; MiraiAI)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _downloadState.value = TtsDownloadState.Error("Server error HTTP ${response.code}")
                    return@launch
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalLength = body.contentLength()
                val totalMb = if (totalLength > 0) totalLength / (1024.0 * 1024.0) else 82.0

                var bytesRead = 0L
                val buffer = ByteArray(8192)
                var read: Int

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                var lastUpdate = System.currentTimeMillis()
                try {
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 300) {
                            lastUpdate = now
                            val currentMb = bytesRead / (1024.0 * 1024.0)
                            val progress = if (totalLength > 0) (bytesRead.toFloat() / totalLength) else 0.5f
                            _downloadState.value = TtsDownloadState.Downloading(progress, currentMb, totalMb)
                        }
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                    inputStream.close()
                }

                if (tempFile.exists() && tempFile.length() > 1024 * 1024) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    _downloadState.value = TtsDownloadState.Completed
                    _isKokoroLoaded.value = true
                } else {
                    _downloadState.value = TtsDownloadState.Error("Downloaded file is incomplete")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download Kokoro-82M", e)
                _downloadState.value = TtsDownloadState.Error(e.localizedMessage ?: "Download failed")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.value = TtsDownloadState.Idle
    }

    fun deleteKokoroModel(context: Context) {
        val file = getKokoroModelFile(context)
        if (file.exists()) {
            file.delete()
        }
        _isKokoroLoaded.value = false
        _downloadState.value = TtsDownloadState.Idle
    }

    fun loadKokoroModel(context: Context): Boolean {
        if (!isKokoroModelDownloaded(context)) {
            _isKokoroLoaded.value = false
            return false
        }
        _isKokoroLoaded.value = true
        return true
    }

    fun unloadKokoroModel() {
        _isKokoroLoaded.value = false
    }

    /**
     * Initialize Android system TextToSpeech if not already created.
     */
    fun initSystemTts(
        context: Context,
        onReady: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        synchronized(this) {
            if (textToSpeech != null && isTtsInitialized) {
                mainHandler.post { onReady?.invoke() }
                return
            }

            if (onReady != null) initCallbacks.add(onReady)
            if (onError != null) initErrorCallbacks.add(onError)

            if (isInitializing) return
            isInitializing = true
        }

        val appContext = context.applicationContext
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                mainHandler.post {
                    synchronized(TtsManager) {
                        isInitializing = false
                        if (status == TextToSpeech.SUCCESS) {
                            isTtsInitialized = true
                            Log.d(TAG, "System TextToSpeech successfully initialized")
                            val callbacks = ArrayList(initCallbacks)
                            initCallbacks.clear()
                            initErrorCallbacks.clear()
                            callbacks.forEach { it.invoke() }
                        } else {
                            isTtsInitialized = false
                            textToSpeech = null
                            val errMsg = "TTS initialization failed (code $status). Ensure Speech Recognition & Synthesis is enabled."
                            Log.i(TAG, errMsg)
                            val errCallbacks = ArrayList(initErrorCallbacks)
                            initCallbacks.clear()
                            initErrorCallbacks.clear()
                            errCallbacks.forEach { it.invoke(errMsg) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating TextToSpeech", e)
            synchronized(this) {
                isInitializing = false
                isTtsInitialized = false
                textToSpeech = null
                val errMsg = e.localizedMessage ?: "Failed creating TextToSpeech"
                val errCallbacks = ArrayList(initErrorCallbacks)
                initCallbacks.clear()
                initErrorCallbacks.clear()
                mainHandler.post {
                    errCallbacks.forEach { it.invoke(errMsg) }
                }
            }
        }
    }

    /**
     * Clean message text to make it natural and pleasant for audio speech.
     */
    fun cleanTextForSpeech(raw: String): String {
        var clean = raw
        // 1. Remove think tags and contents
        clean = clean.replace(Regex("(?s)<think>.*?</think>"), "")
        // 2. Remove code blocks
        clean = clean.replace(Regex("(?s)```.*?```"), " code snippet omitted ")
        // 3. Remove inline code
        clean = clean.replace(Regex("`.*?`"), "")
        // 4. Remove Live2D tags [expression:...] [motion:...]
        clean = clean.replace(Regex("\\[(expression|motion|pose):[^\\]]+\\]"), "")
        // 5. Remove URLs
        clean = clean.replace(Regex("https?://\\S+"), " link ")
        // 6. Remove roleplay asterisks like *smiles warmly* if desired, or speak without asterisks
        clean = clean.replace(Regex("\\*([^*]+)\\*"), "$1")
        // 7. Remove zero-width characters (e.g. \u200B, \u2060, \uFEFF)
        clean = clean.replace(Regex("[\\u200B-\\u200D\\u2060\\uFEFF]"), "")
        // 8. Remove parenthesized kaomoji with optional arms and accessories (e.g. ٩(ˊᗜˋ*)و, (๑>ᴗ<๑), (´ω｀*), (*^▽^*), (¬‿¬), (^з^)-☆)
        clean = clean.replace(
            Regex("""[٩۶ᕗᕤᕙᕦงヽﾉノ凸\\/]*\s*[（(][^()（）]*[\^~_><*+xX•°º●・゜дДωᗜ๑ᴗ¬з☆★♥♡✿❀◕≧▽≦罒益﹏︿﹀T;´｀vVwW\u0250-\u02AF\u02B0-\u02FF\u0300-\u036F\u1500-\u154F\u2200-\u22FF\u2500-\u25FF\u2600-\u26FF\u2700-\u27BF\u3000-\u303F\uFF00-\uFFEF][^()（）]*[)）]\s*[-~]*[و٩۶ᕗᕤᕙᕦงヽﾉノ凸\\/★☆♥♡]*"""),
            " "
        )
        // 9. Remove standalone ASCII/Unicode kaomoji without parentheses (e.g. >_<, T_T, -_-, ^_^, >w<, ^^, UwU, OwO, :3, XD)
        clean = clean.replace(
            Regex("""\b[oO0]_[oO0]\b|\b[uU]_[uU]\b|\b[xX]_[xX]\b|\b[tT]_[tT]\b|\b[qQ]_[qQ]\b|\b[uU][wW][uU]\b|\b[oO][wW][oO]\b|[><][:;^~*_-]+[><]|>[_.-]<|[~^_-]{2,}|;\s*[-_]\s*;|\b[xX][dD]\b|;[;_-]+|:[3DPOpP)\(]\b"""),
            " "
        )
        // 10. Remove decorative symbols and arrows
        clean = clean.replace(Regex("""[★☆♥♡✿❀♪♫✧✦†‡✓✔✕✖~→←↑↓]"""), " ")
        // 11. Remove Unicode emojis
        clean = clean.replace(Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27BF]|[\u2300-\u23FF]|[\u2B50\u2B55]|[\uFE00-\uFE0F]"""), " ")
        // 12. Remove markdown headers, bold, italics markup
        clean = clean.replace(Regex("[#*_~>]+"), " ")
        // 13. Remove spaces before punctuation
        clean = clean.replace(Regex("\\s+([.,!?:;])"), "$1")
        // 14. Normalize multiple spaces and linebreaks
        clean = clean.replace(Regex("\\s+"), " ").trim()
        return clean
    }

    /**
     * Main entry point to speak text according to active config and character voice settings.
     */
    fun speak(
        context: Context,
        text: String,
        config: InferenceConfigEntity?,
        character: CharacterEntity?,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        stop()

        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) {
            onDone()
            return
        }

        val engine = config?.ttsEngine ?: "local"

        if (engine.equals("api", ignoreCase = true)) {
            speakViaApi(context, cleanText, config, character, onStart, onDone, onError)
        } else {
            speakViaLocal(context, cleanText, config, character, onStart, onDone, onError)
        }
    }

    private fun speakViaApi(
        context: Context,
        cleanText: String,
        config: InferenceConfigEntity?,
        character: CharacterEntity?,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val baseUrl = config?.baseUrl ?: "https://api.openai.com/v1"
                val endpoint = if (!config?.ttsApiEndpoint.isNullOrBlank()) {
                    config!!.ttsApiEndpoint.trim()
                } else {
                    "${baseUrl.trimEnd('/')}/audio/speech"
                }

                val apiKey = if (!config?.ttsApiKey.isNullOrBlank()) {
                    config!!.ttsApiKey.trim()
                } else {
                    config?.apiKey?.trim() ?: ""
                }

                val model = if (!config?.ttsApiModel.isNullOrBlank()) {
                    config!!.ttsApiModel.trim()
                } else {
                    "kokoro"
                }

                val voice = character?.voiceId?.trim()?.ifBlank { "af_heart" } ?: "af_heart"
                val speed = character?.voiceSpeed ?: 1.0f

                val isFishAudio = endpoint.contains("fish.audio", ignoreCase = true)

                val jsonBody = if (isFishAudio) {
                    JSONObject().apply {
                        put("text", cleanText)
                        // If voice is provided and not generic placeholder, use as reference_id
                        if (voice.isNotBlank() && voice != "system") {
                            put("reference_id", voice)
                        }
                        put("format", "mp3")
                        val prosody = JSONObject().apply {
                            put("speed", speed.toDouble())
                        }
                        put("prosody", prosody)
                    }
                } else {
                    JSONObject().apply {
                        put("model", model)
                        put("input", cleanText)
                        put("voice", voice)
                        put("speed", speed.toDouble())
                        put("response_format", "mp3")
                    }
                }

                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

                if (apiKey.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer $apiKey")
                    if (isFishAudio) {
                        reqBuilder.header("api-key", apiKey)
                    }
                }

                val response = httpClient.newCall(reqBuilder.build()).execute()
                if (!response.isSuccessful) {
                    val errBody = try { response.body?.string()?.take(300) } catch (_: Exception) { "" }
                    val err = "TTS API error HTTP ${response.code}${if (!errBody.isNullOrBlank()) ": $errBody" else ""}"
                    Log.e(TAG, err)
                    withContext(Dispatchers.Main) {
                        _isPlaying.value = false
                        onError(err)
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    val err = "Empty audio received from TTS API"
                    Log.e(TAG, err)
                    withContext(Dispatchers.Main) {
                        _isPlaying.value = false
                        onError(err)
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val cacheAudioFile = File(context.cacheDir, "tts_${UUID.randomUUID()}.mp3")
                cacheAudioFile.writeBytes(bytes)

                withContext(Dispatchers.Main) {
                    playAudioFile(context, cacheAudioFile, onStart, onDone, onError)
                }
            } catch (e: Exception) {
                val err = "TTS API error: ${e.localizedMessage ?: "Unknown network error"}"
                Log.e(TAG, err, e)
                withContext(Dispatchers.Main) {
                    _isPlaying.value = false
                    onError(err)
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun speakViaLocal(
        context: Context,
        cleanText: String,
        config: InferenceConfigEntity?,
        character: CharacterEntity?,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val voiceId = character?.voiceId ?: "af_heart"

        // If voiceId is not explicitly "system", or system TTS is disabled/uninitialized,
        // directly route to the neural speech synthesis engine to avoid Android OS TTS missing engine errors.
        if (voiceId != "system") {
            speakViaWebFallback(context, cleanText, character, onStart, onDone, onError)
            return
        }

        val mainHandler = Handler(Looper.getMainLooper())
        initSystemTts(
            context = context,
            onReady = {
                try {
                    val tts = textToSpeech
                    if (tts == null) {
                        speakViaWebFallback(context, cleanText, character, onStart, onDone, onError)
                        return@initSystemTts
                    }

                    val (basePitchMul, baseSpeedMul) = getPresetPitchAndSpeedMultipliers(voiceId)
                    val pitch = ((character?.voicePitch ?: 1.0f) * basePitchMul).coerceIn(0.5f, 2.0f)
                    val speed = ((character?.voiceSpeed ?: 1.0f) * baseSpeedMul).coerceIn(0.5f, 2.0f)

                    tts.setPitch(pitch)
                    tts.setSpeechRate(speed)

                    val utteranceId = "mirai_${System.currentTimeMillis()}"
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {
                            mainHandler.post {
                                _isPlaying.value = true
                                onStart()
                            }
                        }

                        override fun onDone(id: String?) {
                            mainHandler.post {
                                _isPlaying.value = false
                                onDone()
                            }
                        }

                        override fun onError(id: String?) {
                            mainHandler.post {
                                _isPlaying.value = false
                                onError("TTS playback error on device")
                            }
                        }
                    })

                    val params = Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    }

                    _isPlaying.value = true
                    val speakResult = tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                    if (speakResult != TextToSpeech.SUCCESS) {
                        speakViaWebFallback(context, cleanText, character, onStart, onDone, onError)
                    }
                } catch (e: Exception) {
                    speakViaWebFallback(context, cleanText, character, onStart, onDone, onError)
                }
            },
            onError = { _ ->
                speakViaWebFallback(context, cleanText, character, onStart, onDone, onError)
            }
        )
    }

    /**
     * Pitch and speed characteristics across all voice presets to ensure noticeable voice differentiation.
     */
    private fun getPresetPitchAndSpeedMultipliers(voiceId: String): Pair<Float, Float> {
        return when (voiceId.lowercase()) {
            // Indonesian
            "id_kawaii" -> Pair(1.45f, 1.08f) // Cute anime high-pitch
            "id_manis" -> Pair(1.22f, 0.95f)  // Sweet, gentle
            "id_ceria" -> Pair(1.35f, 1.15f)  // Cheerful, upbeat
            "id_putri" -> Pair(1.05f, 1.00f)  // Natural feminine
            "id_bima" -> Pair(0.72f, 0.92f)   // Deep cool male
            // Japanese
            "jf_alpha" -> Pair(1.42f, 1.10f)      // High anime female
            "jf_gongitsune" -> Pair(1.18f, 0.92f) // Soft gentle Japanese
            "jm_kumo" -> Pair(0.75f, 0.95f)       // Calm deep male
            // English (US)
            "af_heart" -> Pair(1.20f, 1.00f)  // Warm gentle
            "af_bella" -> Pair(1.40f, 1.10f)  // Cute high pitch
            "af_sarah" -> Pair(1.05f, 1.00f)  // Casual natural female
            "af_nicole" -> Pair(0.92f, 0.90f) // Whispery calm
            "af_sky" -> Pair(1.30f, 1.18f)    // Cheerful lively
            "am_adam" -> Pair(0.82f, 1.00f)   // Natural male
            "am_michael" -> Pair(0.68f, 0.90f) // Deep authoritative male
            // English (British)
            "bf_emma" -> Pair(1.18f, 1.02f)   // British clear female
            "bf_isabella" -> Pair(1.28f, 0.94f) // British soft female
            "bm_george" -> Pair(0.80f, 1.00f) // British male
            "bm_lewis" -> Pair(0.70f, 0.92f)  // British mature male
            else -> Pair(1.0f, 1.0f)
        }
    }

    private val activeAudioFiles = Collections.synchronizedList(mutableListOf<File>())

    private fun speakViaWebFallback(
        context: Context,
        cleanText: String,
        character: CharacterEntity?,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val voiceId = character?.voiceId ?: "af_heart"
                val preset = KOKORO_VOICE_PRESETS.find { it.id.equals(voiceId, ignoreCase = true) }
                val hasJapanese = cleanText.any { c ->
                    (c in '\u3040'..'\u309F') || (c in '\u30A0'..'\u30FF') || (c in '\u4E00'..'\u9FFF')
                }
                val langCode = when {
                    hasJapanese -> "ja"
                    preset != null && preset.language.startsWith("ja", ignoreCase = true) -> "ja"
                    preset != null && preset.language.startsWith("en-GB", ignoreCase = true) -> "en-GB"
                    preset != null && preset.language.startsWith("en", ignoreCase = true) -> "en-US"
                    preset != null && preset.language.startsWith("id", ignoreCase = true) -> "id"
                    else -> Locale.getDefault().language.ifBlank { "en" }
                }

                // Split into sentence chunks under 180 chars for translate_tts
                val sentences = cleanText.split(Regex("(?<=[.!?,\n])\\s+")).filter { it.isNotBlank() }
                val chunks = mutableListOf<String>()
                var current = StringBuilder()
                for (s in sentences) {
                    if (current.length + s.length > 180) {
                        if (current.isNotBlank()) chunks.add(current.toString().trim())
                        current = StringBuilder(s)
                    } else {
                        if (current.isNotBlank()) current.append(" ")
                        current.append(s)
                    }
                }
                if (current.isNotBlank()) chunks.add(current.toString().trim())
                if (chunks.isEmpty()) chunks.add(cleanText.take(180))

                val chunkFiles = mutableListOf<File>()
                for ((idx, chunk) in chunks.withIndex()) {
                    val encoded = java.net.URLEncoder.encode(chunk, "UTF-8")
                    val url = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=$langCode&q=$encoded"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .header("Referer", "https://translate.google.com/")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val chunkFile = File(context.cacheDir, "tts_web_${System.currentTimeMillis()}_$idx.mp3")
                            chunkFile.writeBytes(bytes)
                            chunkFiles.add(chunkFile)
                        }
                    }
                }

                if (chunkFiles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _isPlaying.value = false
                        onError("Online speech synthesis unavailable")
                    }
                    return@launch
                }

                val (basePitchMul, baseSpeedMul) = getPresetPitchAndSpeedMultipliers(voiceId)
                val pitch = ((character?.voicePitch ?: 1.0f) * basePitchMul).coerceIn(0.5f, 2.0f)
                val speed = ((character?.voiceSpeed ?: 1.0f) * baseSpeedMul).coerceIn(0.5f, 2.0f)

                withContext(Dispatchers.Main) {
                    playAudioPlaylist(chunkFiles, pitch, speed, onStart, onDone)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Web TTS fallback exception", e)
                withContext(Dispatchers.Main) {
                    _isPlaying.value = false
                    onError(e.localizedMessage ?: "Speech generation error")
                }
            }
        }
    }

    private fun playAudioPlaylist(
        files: List<File>,
        pitch: Float,
        speed: Float,
        onStart: () -> Unit,
        onDone: () -> Unit
    ) {
        if (files.isEmpty()) {
            onDone()
            return
        }

        stop()
        activeAudioFiles.addAll(files)

        var currentIndex = 0
        fun playNext() {
            if (currentIndex >= files.size) {
                _isPlaying.value = false
                onDone()
                return
            }

            val currentFile = files[currentIndex]
            try {
                mediaPlayer?.let {
                    try {
                        it.setOnCompletionListener(null)
                        it.setOnErrorListener(null)
                        if (it.isPlaying) it.stop()
                        it.reset()
                        it.release()
                    } catch (_: Exception) {}
                }
                val mp = MediaPlayer()
                mediaPlayer = mp
                mp.setDataSource(currentFile.absolutePath)
                mp.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mp.setOnCompletionListener {
                    try { currentFile.delete() } catch (_: Exception) {}
                    activeAudioFiles.remove(currentFile)
                    currentIndex++
                    playNext()
                }
                mp.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer chunk playback error: ($what, $extra)")
                    try { currentFile.delete() } catch (_: Exception) {}
                    activeAudioFiles.remove(currentFile)
                    currentIndex++
                    playNext()
                    true
                }
                mp.prepare()

                try {
                    val params = mp.playbackParams
                    params.pitch = pitch.coerceIn(0.5f, 2.0f)
                    params.speed = speed.coerceIn(0.5f, 2.0f)
                    mp.playbackParams = params
                } catch (e: Exception) {
                    Log.w(TAG, "MediaPlayer setPlaybackParams: ${e.message}")
                }

                if (currentIndex == 0) {
                    _isPlaying.value = true
                    onStart()
                }
                mp.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio chunk $currentIndex", e)
                try { currentFile.delete() } catch (_: Exception) {}
                activeAudioFiles.remove(currentFile)
                currentIndex++
                playNext()
            }
        }

        playNext()
    }

    private fun playAudioFile(
        context: Context,
        file: File,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            stop()
            activeAudioFiles.add(file)
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setDataSource(file.absolutePath)
            mp.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setOnCompletionListener {
                _isPlaying.value = false
                try { file.delete() } catch (_: Exception) {}
                activeAudioFiles.remove(file)
                onDone()
            }
            mp.setOnErrorListener { _, what, extra ->
                _isPlaying.value = false
                try { file.delete() } catch (_: Exception) {}
                activeAudioFiles.remove(file)
                val err = "Audio player error ($what, $extra)"
                onError(err)
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                true
            }
            mp.prepare()
            _isPlaying.value = true
            onStart()
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            _isPlaying.value = false
            try { file.delete() } catch (_: Exception) {}
            activeAudioFiles.remove(file)
            val err = "Failed to play audio: ${e.localizedMessage}"
            onError(err)
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
    }

    fun stop() {
        try {
            _isPlaying.value = false
            mediaPlayer?.let {
                try {
                    it.setOnCompletionListener(null)
                    it.setOnErrorListener(null)
                    if (it.isPlaying) it.stop()
                    it.reset()
                    it.release()
                } catch (_: Exception) {}
            }
            mediaPlayer = null

            synchronized(activeAudioFiles) {
                activeAudioFiles.forEach { file ->
                    try { if (file.exists()) file.delete() } catch (_: Exception) {}
                }
                activeAudioFiles.clear()
            }

            textToSpeech?.let {
                if (it.isSpeaking) it.stop()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun testVoice(
        context: Context,
        voiceId: String,
        pitch: Float,
        speed: Float,
        config: InferenceConfigEntity?,
        characterName: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val dummyChar = CharacterEntity(
            name = characterName,
            voiceId = voiceId,
            voicePitch = pitch,
            voiceSpeed = speed
        )
        val testText = if (voiceId.startsWith("id", ignoreCase = true)) {
            "Halo! Aku $characterName. Senang banget bisa ngobrol bareng kamu hari ini!"
        } else if (voiceId.startsWith("jf", ignoreCase = true) || voiceId.startsWith("jm", ignoreCase = true)) {
            "こんにちは！わたしは $characterName です。あなたとお話しできてとても嬉しいです！"
        } else {
            "Hello! I am $characterName. It's wonderful to talk with you!"
        }
        speak(context, testText, config, dummyChar, onStart, onDone, onError)
    }
}
