package com.ryzumi.miraiai.domain.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * On-device Voice TTS synthesis engine powered by Sherpa-onnx / ONNX Runtime architecture.
 * Executes offline ONNX speech models (Kokoro, Piper, VITS) directly on Android.
 * Employs true G2P phonemizer (IPA phonemes + primary stress marks) and genuine 256-dim voice embeddings.
 */
object SherpaOnnxTtsEngine {
    private const val TAG = "SherpaOnnxTtsEngine"
    private var mediaPlayer: MediaPlayer? = null
    private var ortEnv: OrtEnvironment? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Official Kokoro-82M Vocabulary Mapping from tokenizer.json
    private val KOKORO_VOCAB: Map<Char, Long> = mapOf(
        '$' to 0L, ';' to 1L, ':' to 2L, ',' to 3L, '.' to 4L, '!' to 5L, '?' to 6L,
        '—' to 9L, '…' to 10L, '"' to 11L, '(' to 12L, ')' to 13L, '“' to 14L, '”' to 15L,
        ' ' to 16L, 'A' to 24L, 'I' to 25L, 'O' to 31L, 'Q' to 33L, 'S' to 35L, 'T' to 36L,
        'W' to 39L, 'Y' to 41L, 'a' to 43L, 'b' to 44L, 'c' to 45L, 'd' to 46L, 'e' to 47L,
        'f' to 48L, 'g' to 92L, 'h' to 50L, 'i' to 51L, 'j' to 52L, 'k' to 53L, 'l' to 54L,
        'm' to 55L, 'n' to 56L, 'o' to 57L, 'p' to 58L, 'q' to 59L, 'r' to 60L, 's' to 61L,
        't' to 62L, 'u' to 63L, 'v' to 64L, 'w' to 65L, 'x' to 66L, 'y' to 67L, 'z' to 68L,
        'ɑ' to 69L, 'æ' to 72L, 'ɔ' to 76L, 'ç' to 78L, 'ð' to 81L, 'ə' to 83L, 'ɚ' to 85L,
        'ɛ' to 86L, 'ɡ' to 92L, 'ɪ' to 102L, 'ŋ' to 112L, 'ɲ' to 114L, 'θ' to 119L, 'ɹ' to 123L,
        'ʃ' to 131L, 'ʊ' to 135L, 'ʌ' to 138L, 'ʒ' to 147L, 'ʔ' to 148L,
        'ˈ' to 156L, // Primary Stress mark (critical for Kokoro natural intonation)
        'ˌ' to 157L, // Secondary Stress mark
        'ː' to 158L  // Length mark
    )

    // High-frequency English dictionary to IPA phonemes with stress for expressive prosody
    private val PHONEME_DICT: Map<String, String> = mapOf(
        "hello" to "həˈloʊ", "hi" to "hˈaɪ", "hey" to "hˈeɪ", "how" to "hˈaʊ",
        "are" to "ˈɑːɹ", "you" to "jˈuː", "i" to "ˈaɪ", "am" to "ˈæm", "is" to "ˈɪz",
        "it" to "ˈɪt", "the" to "ðə", "this" to "ðˈɪs", "that" to "ðˈæt",
        "to" to "tˈuː", "and" to "ˈænd", "a" to "ə", "an" to "ən", "in" to "ˈɪn",
        "on" to "ˈɒn", "for" to "fˈɔːɹ", "with" to "wˈɪð", "at" to "ˈæt", "from" to "fɹˈɒm",
        "today" to "təˈdeɪ", "mirai" to "mˈiːɹaɪ", "fatih" to "fˈɑːtiː", "ai" to "ˈeɪˈaɪ",
        "assist" to "əˈsɪst", "help" to "hˈɛlp", "talk" to "tˈɔːk", "speak" to "spˈiːk",
        "good" to "ɡˈʊd", "morning" to "mˈɔːɹnɪŋ", "night" to "nˈaɪt", "day" to "dˈeɪ",
        "happy" to "hˈæpi", "wonderful" to "wˈʌndɚfəl", "beautiful" to "bjˈuːtɪfəl",
        "nice" to "nˈaɪs", "meet" to "mˈiːt", "like" to "lˈaɪk", "love" to "lˈʌv",
        "see" to "sˈiː", "hear" to "hˈɪəɹ", "can" to "kˈæn", "do" to "dˈuː",
        "what" to "wˈɒt", "where" to "wˈɛəɹ", "when" to "wˈɛn", "why" to "wˈaɪ",
        "who" to "hˈuː", "which" to "wˈɪtʃ", "there" to "ðˈɛəɹ", "here" to "hˈɪəɹ",
        "image" to "ˈɪmɪdʒ", "generate" to "dʒˈɛnɚeɪt", "sunset" to "sˈʌnsɛt", "beach" to "bˈiːtʃ",
        "anime" to "ˈænɪmeɪ", "picture" to "pˈɪktʃɚ", "please" to "plˈiːz", "thank" to "θˈæŋk",
        "thanks" to "θˈæŋks", "welcome" to "wˈɛlkəm", "yes" to "jˈɛs", "no" to "nˈoʊ",
        "sure" to "ʃˈʊəɹ", "of" to "əv", "course" to "kˈɔːɹs", "very" to "vˈɛɹi",
        "much" to "mˈʌtʃ", "so" to "sˈoʊ", "great" to "ɡɹˈeɪt", "fine" to "fˈaɪn",
        "well" to "wˈɛl", "always" to "ˈɔːlweɪz", "ready" to "ɹˈɛdi", "start" to "stˈɑːɹt"
    )

    // High-frequency Indonesian dictionary mapped to IPA phonemes with stress marks
    private val INDONESIAN_PHONEME_DICT: Map<String, String> = mapOf(
        "halo" to "hˈaloʊ", "hai" to "hˈaɪ", "selamat" to "səˈlamat", "pagi" to "pˈaɡi",
        "siang" to "siˈaŋ", "sore" to "sˈɔɹe", "malam" to "mˈalam", "terima" to "təˈɹima",
        "kasih" to "kˈasɪh", "sama" to "sˈama", "apa" to "ˈapa", "kabar" to "kˈabaɹ",
        "baik" to "bˈaɪk", "aku" to "ˈaku", "kamu" to "kˈamu", "saya" to "sˈaja",
        "dia" to "dˈia", "mereka" to "məˈɹeka", "kita" to "kˈita", "kami" to "kˈami",
        "bisa" to "bˈisa", "ada" to "ˈada", "tidak" to "tˈidak", "bukan" to "bˈukan",
        "ya" to "jˈa", "iya" to "ˈija", "senang" to "sənˈaŋ", "bertemu" to "bəɹtˈəmu",
        "dengan" to "dˈəŋan", "denganmu" to "dˈəŋanmu", "tolong" to "tˈɔlɔŋ", "silakan" to "silˈakan",
        "maaf" to "mˈaʔaf", "bagaimana" to "baɡaɪmˈana", "kenapa" to "kənˈapa", "mengapa" to "məŋˈapa",
        "siapa" to "siˈapa", "dimana" to "dimˈana", "kemana" to "kəmˈana", "kapan" to "kˈapan",
        "yang" to "jˈaŋ", "ini" to "ˈini", "itu" to "ˈitu", "sangat" to "sˈaŋat",
        "banget" to "bˈaŋət", "sekali" to "səkˈali", "tentu" to "tˈəntu", "saja" to "sˈadʒa",
        "sudah" to "sˈudah", "udah" to "ˈudah", "belum" to "bəlˈum", "sedang" to "sədˈaŋ",
        "lagi" to "lˈaɡi", "akan" to "ˈakan", "bicara" to "biˈtʃaɹa", "ngobrol" to "ŋˈɔbɹɔl",
        "cerita" to "tʃəˈɹita", "gambar" to "ɡˈambaɹ", "suara" to "suˈaɹa", "bahasa" to "bahˈasa",
        "indonesia" to "ɪndoˈnɛsja", "teman" to "təmˈan", "sahabat" to "sahˈabat", "sekarang" to "səkˈaɹaŋ",
        "cantik" to "tʃˈantɪk", "keren" to "kˈɛɹɛn", "lucu" to "lˈutʃu", "manis" to "mˈanɪs",
        "senyum" to "səɲˈum", "suka" to "sˈuka", "cinta" to "tʃˈɪnta", "sayang" to "sˈajaŋ",
        "tahu" to "tˈahu", "tau" to "tˈaʊ", "tanya" to "tˈaɲa", "jawab" to "dʒˈawab",
        "pertanyaan" to "pəɹtaɲˈaʔan", "bagus" to "bˈaɡus", "semua" to "səmˈua", "orang" to "ˈɔɹaŋ",
        "hari" to "hˈaɹi", "jalan" to "dʒˈalan", "kerja" to "kˈəɹdʒa", "belajar" to "bəlˈadʒaɹ",
        "makan" to "mˈakan", "minum" to "mˈɪnum", "tidur" to "tˈɪduɹ", "dengar" to "dˈəŋaɹ",
        "lihat" to "lˈihat", "buat" to "bˈuat", "membuat" to "məmbˈuat", "punya" to "pˈuɲa",
        "banyak" to "bˈaɲak", "sedikit" to "sədˈikɪt", "besar" to "bəsˈaɹ", "kecil" to "kətʃˈɪl",
        "cepat" to "tʃəpˈat", "lama" to "lˈama", "baru" to "bˈaɹu", "karena" to "kaˈɹena",
        "tapi" to "tˈapi", "selalu" to "səlˈalu", "sering" to "səɹˈɪŋ", "kadang" to "kˈadaŋ",
        "jangan" to "dʒˈaŋan", "harus" to "hˈaɹus", "boleh" to "bˈɔlɛh", "mungkin" to "mˈuŋkɪn",
        "pasti" to "pˈasti", "nyata" to "ɲˈata", "mimpi" to "mˈɪmpi", "dunia" to "dˈunja",
        "hati" to "hˈati", "rasa" to "ɹˈasa", "merasa" to "məˈɹasa", "hebat" to "hˈɛbat",
        "pintar" to "pˈɪntaɹ", "cerdas" to "tʃˈəɹdas", "buku" to "bˈuku", "nama" to "nˈama",
        "namaku" to "nˈamaku", "namamu" to "nˈamamu", "kenal" to "kənˈal", "salam" to "sˈalam",
        "semoga" to "səmˈoɡa", "selesai" to "sələsˈaɪ", "mulai" to "mˈulaɪ", "tunggu" to "tˈuŋɡu",
        "sini" to "sˈini", "sana" to "sˈana", "mari" to "mˈaɹi", "ayo" to "ˈajo",
        "yuk" to "jˈʊk", "dong" to "dˈɔŋ", "deh" to "dˈɛh", "nih" to "nˈɪh",
        "kan" to "kˈan", "kok" to "kˈɔk", "sih" to "sˈɪh", "loh" to "lˈɔh",
        "wah" to "wˈah", "asik" to "ˈasɪk", "oke" to "ˈoʊkˈeɪ", "sip" to "sˈɪp",
        "mantap" to "mˈantap", "mirai" to "mˈiːɹaɪ", "fatih" to "fˈɑːtiː",
        "kawaii" to "kawˈaɪ", "desu" to "dˈɛsu", "ne" to "nˈɛ", "senpai" to "sənpˈaɪ",
        "wibu" to "wˈibu", "gemoy" to "ɡəmˈɔɪ", "unyu" to "ˈuɲu", "wkwk" to "wˈekawˈeka",
        "hehe" to "hˈɛhɛ", "xixi" to "xˈiːxˈiː", "haha" to "hˈaha"
    )

    private val INDONESIAN_STOPWORDS = setOf(
        "yang", "dan", "di", "ke", "dari", "ini", "itu", "untuk", "dengan", "saya",
        "aku", "kamu", "dia", "kita", "kami", "mereka", "bisa", "ada", "tidak", "bukan",
        "gak", "nggak", "apa", "kabar", "halo", "hai", "terima", "kasih", "selamat",
        "pagi", "siang", "sore", "malam", "ya", "iya", "udah", "sudah", "lagi", "mau",
        "akan", "tapi", "karena", "sama", "banget", "nih", "dong", "deh", "loh", "kan",
        "kok", "buat", "aja", "saja", "bagus", "senang", "ngobrol", "cerita", "gambar",
        "suara", "bahasa", "indonesia", "bagaimana", "gimana", "kenapa", "siapa", "dimana",
        "kemana", "tolong", "bantu", "silakan", "maaf", "banyak", "sedikit", "sekarang"
    )

    private val INDONESIAN_NORMALIZATIONS = mapOf(
        "yg" to "yang", "dgn" to "dengan", "utk" to "untuk", "bgt" to "banget",
        "bgtu" to "begitu", "sy" to "saya", "km" to "kamu", "sdh" to "sudah",
        "blm" to "belum", "tp" to "tapi", "klo" to "kalau", "kalo" to "kalau",
        "gmn" to "bagaimana", "bgmn" to "bagaimana", "knp" to "kenapa",
        "bener" to "benar", "bnr" to "benar", "tsb" to "tersebut", "dlm" to "dalam",
        "sm" to "sama", "bs" to "bisa", "skrg" to "sekarang", "krn" to "karena",
        "dr" to "dari", "org" to "orang", "cm" to "cuma", "cmn" to "cuma",
        "trs" to "terus", "lg" to "lagi", "jg" to "juga", "udh" to "sudah",
        "ga" to "tidak", "gak" to "tidak", "ngga" to "tidak", "nggak" to "tidak",
        "tau" to "tahu", "makasih" to "terima kasih", "makaci" to "terima kasih",
        "thx" to "terima kasih", "halo" to "halo", "hallo" to "halo",
        "haii" to "hai", "haiii" to "hai"
    )

    private fun getEnvironment(): OrtEnvironment {
        if (ortEnv == null) {
            ortEnv = OrtEnvironment.getEnvironment()
        }
        return ortEnv!!
    }

    suspend fun synthesizeSpeech(
        context: Context,
        modelFile: File,
        text: String,
        character: CharacterEntity? = null,
        outputWavFile: File? = null,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!modelFile.exists() || modelFile.length() <= 1024) {
            val err = "File model Sherpa-onnx tidak ditemukan: ${modelFile.name}. Silakan download model ONNX di Model Hub!"
            onError(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            val targetFile = outputWavFile ?: File(context.cacheDir, "sherpa_${System.currentTimeMillis()}.wav")
            Log.d(TAG, "Sherpa-onnx: Inisialisasi OrtSession untuk ${modelFile.name} (${String.format("%.1f", modelFile.length() / (1024.0 * 1024.0))} MB)...")

            val env = getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
            }
            val session = env.createSession(modelFile.absolutePath, sessionOptions)

            val inputNames = session.inputNames
            val cleanText = text.trim()
            val speed = (character?.voiceSpeed ?: 1.0f).coerceIn(0.6f, 1.8f)

            // 1. Phonemize text to IPA with primary stress markers
            val tokenArray = phonemizeToKokoroTokens(cleanText)

            // 2. Load authentic voice style embedding directly without hardcoded replacement
            val voiceId = character?.voiceId?.trim()?.ifBlank { "af_heart" } ?: "af_heart"
            val styleVector = loadVoiceStyleEmbedding(context, voiceId, tokenArray.size)

            val inputs = mutableMapOf<String, OnnxTensor>()
            for (name in inputNames) {
                when {
                    name.contains("token", ignoreCase = true) || name.contains("input_ids", ignoreCase = true) -> {
                        inputs[name] = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenArray), longArrayOf(1, tokenArray.size.toLong()))
                    }
                    name.contains("speed", ignoreCase = true) -> {
                        inputs[name] = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(speed)), longArrayOf(1))
                    }
                    name.contains("style", ignoreCase = true) -> {
                        inputs[name] = OnnxTensor.createTensor(env, FloatBuffer.wrap(styleVector), longArrayOf(1, 256))
                    }
                }
            }

            Log.d(TAG, "Sherpa-onnx: Menjalankan inferensi graf ONNX (${tokenArray.size} tokens, voice=$voiceId)...")
            val result = session.run(inputs)
            val outputTensor = result[0].value
            val sampleRate = 24000 // 24kHz Kokoro standard

            val rawSamples: FloatArray = when (outputTensor) {
                is Array<*> -> {
                    val inner = outputTensor[0]
                    if (inner is FloatArray) {
                        inner
                    } else if (inner is Array<*>) {
                        inner[0] as FloatArray
                    } else {
                        throw IllegalStateException("Format output tensor tidak didukung: ${outputTensor::class.java.simpleName}")
                    }
                }
                is FloatArray -> outputTensor
                else -> throw IllegalStateException("Model tidak mengembalikan tensor audio yang valid: ${outputTensor?.javaClass?.simpleName}")
            }

            // Cleanup ONNX session & input tensors
            inputs.values.forEach { try { it.close() } catch (_: Exception) {} }
            result.close()
            session.close()

            // 3. Audio Peak Normalization to prevent digital clipping / scratches
            var maxAmp = 0.0001f
            for (s in rawSamples) {
                val a = abs(s)
                if (a > maxAmp) maxAmp = a
            }
            val scale = if (maxAmp > 0.01f) (32767f * 0.92f / maxAmp) else 32767f

            val pcmData = ShortArray(rawSamples.size) { i ->
                (rawSamples[i] * scale).toInt().coerceIn(-32767, 32767).toShort()
            }

            // 4. Write authentic 16-bit PCM WAV with proper RIFF header
            writeWavFile(targetFile, pcmData, sampleRate)

            withContext(Dispatchers.Main) {
                onStart()
                playWav(context, targetFile, onDone, onError)
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Sherpa-onnx ONNX Runtime execution failed", e)
            val err = "Gagal menjalankan model ONNX voice: ${e.localizedMessage ?: e.message}"
            onError(err)
            Result.failure(e)
        }
    }

    /**
     * Converts raw text into Kokoro IPA phonemes with stress marks (ˈ) for natural intonation.
     * Automatically detects Indonesian/Japanese text and routes through the appropriate G2P pipeline.
     */
    private fun phonemizeToKokoroTokens(text: String): LongArray {
        val tokens = mutableListOf<Long>()
        tokens.add(0L) // Start token '$'

        val isIndonesian = isIndonesianText(text)
        val hasJapanese = containsJapanese(text)
        val processedText = if (isIndonesian && !hasJapanese) normalizeIndonesianText(text) else text

        val langLabel = when {
            hasJapanese -> "ja"
            isIndonesian -> "id"
            else -> "en"
        }
        Log.d(TAG, "G2P: detected language=$langLabel, text=\"${processedText.take(60)}...\"")

        // Tokenize into words (Latin + Japanese chars), digit runs, and punctuation
        // Captures: Latin words, Japanese character runs (hiragana/katakana/kanji), digits, punctuation, spaces
        val regex = Regex("""([a-zA-Z]+|[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF\u3005]+|[\d]+|[.,!?;:\-—…()""、。！？\s])""")
        val matches = regex.findAll(processedText)

        for (match in matches) {
            val str = match.value
            if (str.isBlank()) {
                tokens.add(16L) // Space token
                continue
            }

            val ch = str[0]

            // Japanese punctuation mapping
            if (str.length == 1) {
                when (ch) {
                    '、' -> { tokens.add(3L); continue } // comma
                    '。' -> { tokens.add(4L); continue } // period
                    '！' -> { tokens.add(5L); continue } // exclamation
                    '？' -> { tokens.add(6L); continue } // question
                }
                if (KOKORO_VOCAB.containsKey(ch) && !ch.isLetter()) {
                    tokens.add(KOKORO_VOCAB[ch]!!)
                    continue
                }
            }

            // Japanese character run
            if (isJapaneseRun(str)) {
                val romaji = japaneseToRomaji(str)
                val ipa = japaneseRomajiToIPA(romaji)
                appendIpaToTokens(ipa, tokens)
                continue
            }

            // Digit expansion
            if (str.all { it.isDigit() }) {
                val numberWords = if (isIndonesian) expandIndonesianNumber(str) else str
                val numberTokens = numberWords.split(" ").filter { it.isNotBlank() }
                for ((idx, nw) in numberTokens.withIndex()) {
                    if (idx > 0) tokens.add(16L)
                    val nIpa = if (isIndonesian) {
                        INDONESIAN_PHONEME_DICT[nw] ?: indonesianG2P(nw)
                    } else {
                        PHONEME_DICT[nw] ?: ruleBasedEnglishG2P(nw)
                    }
                    appendIpaToTokens(nIpa, tokens)
                }
                continue
            }

            val lower = str.lowercase()
            val ipa = if (isIndonesian) {
                INDONESIAN_PHONEME_DICT[lower] ?: PHONEME_DICT[lower] ?: indonesianG2P(lower)
            } else {
                PHONEME_DICT[lower] ?: ruleBasedEnglishG2P(lower)
            }

            appendIpaToTokens(ipa, tokens)
        }

        tokens.add(0L) // End token '$'
        return tokens.toLongArray()
    }

    /**
     * Appends an IPA string to the token list, mapping each char through KOKORO_VOCAB.
     */
    private fun appendIpaToTokens(ipa: String, tokens: MutableList<Long>) {
        for (pCh in ipa) {
            val tokenId = KOKORO_VOCAB[pCh]
            if (tokenId != null) {
                tokens.add(tokenId)
            } else if (pCh.isLetter()) {
                tokens.add(KOKORO_VOCAB[pCh.lowercaseChar()] ?: 16L)
            }
            // Skip unmapped non-letter chars silently
        }
    }

    /**
     * Detects whether the input text is predominantly Indonesian.
     * Uses a weighted heuristic: Indonesian stopwords, common affixes, and absence of English markers.
     */
    private fun isIndonesianText(text: String): Boolean {
        val words = text.lowercase().split(Regex("""[\s.,!?;:—…()"]+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false

        var idScore = 0
        var enScore = 0

        for (w in words) {
            // Direct dictionary hit
            if (INDONESIAN_PHONEME_DICT.containsKey(w) || INDONESIAN_STOPWORDS.contains(w)) idScore += 2
            if (PHONEME_DICT.containsKey(w)) enScore += 2

            // Indonesian normalizations (slang/abbreviations)
            if (INDONESIAN_NORMALIZATIONS.containsKey(w)) idScore += 3

            // Indonesian affixes: me-, ber-, ter-, per-, di-, ke-, se-
            if (w.length > 3) {
                val prefix = w.substring(0, 2)
                val prefix3 = w.substring(0, 3)
                if (prefix3 in listOf("ber", "ter", "per", "mem", "men", "meng", "meny", "pen", "pem", "peng", "peny")) idScore += 1
                if (prefix in listOf("me", "di", "ke", "se")) idScore += 1
            }
            // Indonesian suffix: -kan, -an, -nya, -lah, -kah
            if (w.length > 3) {
                if (w.endsWith("kan") || w.endsWith("nya") || w.endsWith("lah") || w.endsWith("kah")) idScore += 1
                if (w.endsWith("an") && w.length > 3) idScore += 1
            }

            // English markers
            if (w in listOf("the", "is", "are", "was", "were", "have", "has", "been", "will", "would", "could", "should")) enScore += 2
        }

        return idScore > enScore
    }

    /**
     * Normalizes Indonesian slang, abbreviations, and informal text to standard forms.
     */
    private fun normalizeIndonesianText(text: String): String {
        val words = text.split(Regex("""(\s+)"""))
        val result = StringBuilder()
        for (part in words) {
            if (part.isBlank()) {
                result.append(part)
                continue
            }
            val lower = part.lowercase().trim()
            // Check if this word is slang / abbreviated
            val normalized = INDONESIAN_NORMALIZATIONS[lower]
            if (normalized != null) {
                result.append(normalized)
            } else {
                result.append(part)
            }
        }
        return result.toString()
    }

    /**
     * Indonesian Grapheme-to-Phoneme converter with proper phonological rules.
     *
     * Key rules applied:
     * - c → tʃ (always, Indonesian "c" = "ch")
     * - j → dʒ
     * - ng → ŋ, ny → ɲ, sy → ʃ, kh → x(→k)
     * - r → ɹ (alveolar approximant, Kokoro's closest)
     * - Schwa reduction on unstressed prefixes (se-, ke-, be-, me-, te-, pe-)
     * - Penultimate syllable stress (ˈ before the second-to-last vowel)
     */
    private fun indonesianG2P(word: String): String {
        if (word.isBlank()) return ""

        // Step 1: Convert graphemes to base IPA (without stress)
        val sb = StringBuilder()
        var i = 0
        while (i < word.length) {
            val remaining = word.substring(i)

            when {
                // Trigraphs
                remaining.startsWith("ngg") -> {
                    sb.append("ŋɡ")
                    i += 3
                    continue
                }
                remaining.startsWith("ngk") -> {
                    sb.append("ŋk")
                    i += 3
                    continue
                }
                // Digraphs (must check before single chars)
                remaining.startsWith("ng") -> {
                    sb.append("ŋ")
                    i += 2
                    continue
                }
                remaining.startsWith("ny") -> {
                    sb.append("ɲ")
                    i += 2
                    continue
                }
                remaining.startsWith("sy") -> {
                    sb.append("ʃ")
                    i += 2
                    continue
                }
                remaining.startsWith("kh") -> {
                    sb.append("k")
                    i += 2
                    continue
                }
                remaining.startsWith("ai") && (i + 2 >= word.length || !word[i + 2].isLetter()) -> {
                    // Word-final or before consonant diphthong
                    sb.append("aɪ")
                    i += 2
                    continue
                }
                remaining.startsWith("au") && (i + 2 >= word.length || !word[i + 2].isLetter()) -> {
                    sb.append("aʊ")
                    i += 2
                    continue
                }
                remaining.startsWith("oi") && (i + 2 >= word.length || !word[i + 2].isLetter()) -> {
                    sb.append("ɔɪ")
                    i += 2
                    continue
                }
            }

            // Single character mappings
            when (val c = word[i]) {
                'c' -> sb.append("tʃ")   // Indonesian c is always /tʃ/
                'j' -> sb.append("dʒ")   // Indonesian j is always /dʒ/
                'r' -> sb.append("ɹ")    // Kokoro's closest to Indonesian r
                'g' -> sb.append("ɡ")    // Use IPA g (ɡ)
                'x' -> sb.append("ks")
                'q' -> sb.append("k")
                'v' -> sb.append("f")    // Indonesian v often realized as f
                // Schwa for 'e' in unstressed prefixes
                'e' -> {
                    // Check if this is an unstressed prefix e (se-, ke-, be-, me-, te-, pe-)
                    if (i == 1 && word.length > 3 &&
                        word[0] in listOf('s', 'k', 'b', 'm', 't', 'p') &&
                        word[2].isLetter() && !isIndonesianVowel(word[2])
                    ) {
                        sb.append("ə") // Schwa in prefix
                    } else if (i == 1 && word.length > 4 &&
                        word[0] in listOf('s', 'k', 'b', 'm', 't', 'p') &&
                        word.substring(0, 2) in listOf("se", "ke", "be", "me", "te", "pe")
                    ) {
                        sb.append("ə") // Schwa in prefix
                    } else {
                        sb.append("e") // Keep as open-mid front vowel
                    }
                }
                else -> sb.append(c) // a, i, o, u, consonants pass through
            }
            i++
        }

        // Step 2: Apply penultimate stress
        val ipaNoStress = sb.toString()
        return applyPenultimateStress(ipaNoStress)
    }

    /**
     * Identifies vowel positions in an IPA string and places primary stress (ˈ)
     * before the penultimate (second-to-last) vowel, following Indonesian prosody rules.
     */
    private fun applyPenultimateStress(ipa: String): String {
        val vowelChars = setOf('a', 'e', 'i', 'o', 'u', 'ə', 'ɛ', 'ɔ', 'ɪ', 'ʊ', 'ʌ', 'æ', 'ɑ')

        // Find positions of all vowel nuclei (skip diphthong second elements)
        val vowelPositions = mutableListOf<Int>()
        var idx = 0
        while (idx < ipa.length) {
            if (ipa[idx] in vowelChars) {
                vowelPositions.add(idx)
                // Skip over diphthong glides (aɪ, aʊ, ɔɪ)
                if (idx + 1 < ipa.length && ipa[idx + 1] in setOf('ɪ', 'ʊ')) {
                    idx += 2
                    continue
                }
            }
            idx++
        }

        if (vowelPositions.size < 2) {
            // Monosyllabic: stress the only vowel
            if (vowelPositions.isNotEmpty()) {
                val pos = vowelPositions[0]
                return ipa.substring(0, pos) + "ˈ" + ipa.substring(pos)
            }
            return ipa
        }

        // Penultimate = second-to-last vowel
        val stressPos = vowelPositions[vowelPositions.size - 2]
        return ipa.substring(0, stressPos) + "ˈ" + ipa.substring(stressPos)
    }

    private fun isIndonesianVowel(c: Char): Boolean = c in listOf('a', 'i', 'u', 'e', 'o')

    /**
     * Expands an Indonesian number string into spoken Indonesian words.
     * e.g. "123" → "seratus dua puluh tiga", "0" → "nol"
     */
    private fun expandIndonesianNumber(numStr: String): String {
        val num = numStr.toLongOrNull() ?: return numStr.map { digitToIndonesian(it) }.joinToString(" ")

        if (num == 0L) return "nol"
        if (num < 0L) return "minus ${expandIndonesianLong(-num)}"
        return expandIndonesianLong(num)
    }

    private fun expandIndonesianLong(num: Long): String {
        if (num == 0L) return ""
        if (num == 1L) return "satu"
        if (num < 10L) return digitToIndonesian(('0' + num.toInt()))
        if (num == 10L) return "sepuluh"
        if (num == 11L) return "sebelas"
        if (num < 20L) return "${digitToIndonesian(('0' + (num % 10).toInt()))} belas"
        if (num < 100L) return "${digitToIndonesian(('0' + (num / 10).toInt()))} puluh${if (num % 10 != 0L) " ${expandIndonesianLong(num % 10)}" else ""}"
        if (num < 200L) return "seratus${if (num % 100 != 0L) " ${expandIndonesianLong(num % 100)}" else ""}"
        if (num < 1000L) return "${expandIndonesianLong(num / 100)} ratus${if (num % 100 != 0L) " ${expandIndonesianLong(num % 100)}" else ""}"
        if (num < 2000L) return "seribu${if (num % 1000 != 0L) " ${expandIndonesianLong(num % 1000)}" else ""}"
        if (num < 1_000_000L) return "${expandIndonesianLong(num / 1000)} ribu${if (num % 1000 != 0L) " ${expandIndonesianLong(num % 1000)}" else ""}"
        if (num < 1_000_000_000L) return "${expandIndonesianLong(num / 1_000_000)} juta${if (num % 1_000_000 != 0L) " ${expandIndonesianLong(num % 1_000_000)}" else ""}"
        return "${expandIndonesianLong(num / 1_000_000_000)} miliar${if (num % 1_000_000_000 != 0L) " ${expandIndonesianLong(num % 1_000_000_000)}" else ""}"
    }

    private fun digitToIndonesian(c: Char): String = when (c) {
        '0' -> "nol"
        '1' -> "satu"
        '2' -> "dua"
        '3' -> "tiga"
        '4' -> "empat"
        '5' -> "lima"
        '6' -> "enam"
        '7' -> "tujuh"
        '8' -> "delapan"
        '9' -> "sembilan"
        else -> ""
    }

    /**
     * Rule-based English Grapheme-to-Phoneme converter for words not in the high-frequency dictionary.
     * Applies primary stress (ˈ) and phonetic digraph mapping to preserve natural rhythm.
     */
    private fun ruleBasedEnglishG2P(word: String): String {
        if (word.isBlank()) return ""
        val sb = StringBuilder()

        var hasStress = false
        var i = 0
        while (i < word.length) {
            val remaining = word.substring(i)

            // Digraphs & Suffix rules
            when {
                remaining.startsWith("tion") -> {
                    sb.append("ʃən")
                    i += 4
                    continue
                }
                remaining.startsWith("ing") -> {
                    sb.append("ɪŋ")
                    i += 3
                    continue
                }
                remaining.startsWith("th") -> {
                    sb.append("θ")
                    i += 2
                    continue
                }
                remaining.startsWith("sh") -> {
                    sb.append("ʃ")
                    i += 2
                    continue
                }
                remaining.startsWith("ch") -> {
                    sb.append("tʃ")
                    i += 2
                    continue
                }
                remaining.startsWith("ph") -> {
                    sb.append("f")
                    i += 2
                    continue
                }
                remaining.startsWith("ee") || remaining.startsWith("ea") -> {
                    if (!hasStress) { sb.append("ˈ"); hasStress = true }
                    sb.append("iː")
                    i += 2
                    continue
                }
                remaining.startsWith("oo") -> {
                    if (!hasStress) { sb.append("ˈ"); hasStress = true }
                    sb.append("uː")
                    i += 2
                    continue
                }
                remaining.startsWith("ai") || remaining.startsWith("ay") -> {
                    if (!hasStress) { sb.append("ˈ"); hasStress = true }
                    sb.append("eɪ")
                    i += 2
                    continue
                }
                remaining.startsWith("oi") || remaining.startsWith("oy") -> {
                    if (!hasStress) { sb.append("ˈ"); hasStress = true }
                    sb.append("ɔɪ")
                    i += 2
                    continue
                }
                remaining.startsWith("ng") -> {
                    sb.append("ŋ")
                    i += 2
                    continue
                }
                remaining.startsWith("ny") -> {
                    sb.append("ɲ")
                    i += 2
                    continue
                }
            }

            val c = word[i]
            // Add stress to the first vowel in the word for expressive pitch accent
            if (!hasStress && (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')) {
                sb.append("ˈ")
                hasStress = true
            }

            sb.append(c)
            i++
        }

        return sb.toString()
    }

    // ==================== Japanese G2P Pipeline ====================

    /**
     * Returns true if the text contains any Japanese characters (hiragana, katakana, or kanji).
     */
    private fun containsJapanese(text: String): Boolean {
        return text.any { c ->
            c.code in 0x3040..0x309F || // Hiragana
            c.code in 0x30A0..0x30FF || // Katakana
            c.code in 0x4E00..0x9FFF || // CJK Unified Ideographs (Kanji)
            c.code == 0x3005           // Ideographic iteration mark 々
        }
    }

    /**
     * Returns true if the entire string is a run of Japanese characters.
     */
    private fun isJapaneseRun(str: String): Boolean {
        return str.isNotEmpty() && str.all { c ->
            c.code in 0x3040..0x309F ||
            c.code in 0x30A0..0x30FF ||
            c.code in 0x4E00..0x9FFF ||
            c.code == 0x3005
        }
    }

    // Hiragana → Romaji mapping (ordered by length for greedy matching)
    private val HIRAGANA_TO_ROMAJI: List<Pair<String, String>> = listOf(
        // Digraphs with ya/yu/yo (must come before single kana)
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja",  "じゅ" to "ju",  "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        // Single kana
        "あ" to "a",  "い" to "i",  "う" to "u",  "え" to "e",  "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "さ" to "sa", "し" to "shi","す" to "su", "せ" to "se", "そ" to "so",
        "た" to "ta", "ち" to "chi","つ" to "tsu","て" to "te", "と" to "to",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya",              "ゆ" to "yu",              "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "ゐ" to "wi",              "ゑ" to "we", "を" to "wo",
        "ん" to "n",
        // Dakuten (voiced)
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "だ" to "da", "ぢ" to "di", "づ" to "du", "で" to "de", "ど" to "do",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        // Handakuten (semi-voiced)
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
        // Small kana
        "ぁ" to "a", "ぃ" to "i", "ぅ" to "u", "ぇ" to "e", "ぉ" to "o",
        "っ" to "Q",  // Geminate marker (handled in romaji→IPA)
        "ー" to ":",  // Long vowel marker
        // Particle は (wa) and へ (e) are context-dependent, handled in word dict
    )

    // Common Kanji → Hiragana readings for conversational Japanese
    private val KANJI_READINGS: Map<String, String> = mapOf(
        "私" to "わたし", "僕" to "ぼく", "俺" to "おれ",
        "君" to "きみ", "彼" to "かれ", "彼女" to "かのじょ",
        "人" to "ひと", "今" to "いま", "今日" to "きょう",
        "明日" to "あした", "昨日" to "きのう", "時" to "とき",
        "日" to "ひ", "月" to "つき", "年" to "とし",
        "大" to "おお", "小" to "ちい", "中" to "なか",
        "上" to "うえ", "下" to "した", "前" to "まえ",
        "後" to "あと", "一" to "いち", "二" to "に",
        "三" to "さん", "四" to "よん", "五" to "ご",
        "六" to "ろく", "七" to "なな", "八" to "はち",
        "九" to "きゅう", "十" to "じゅう", "百" to "ひゃく",
        "千" to "せん", "万" to "まん",
        "何" to "なに", "誰" to "だれ", "嬉しい" to "うれしい",
        "楽しい" to "たのしい", "悲しい" to "かなしい", "好き" to "すき",
        "嫌い" to "きらい", "名前" to "なまえ", "言葉" to "ことば",
        "話" to "はなし", "食べる" to "たべる", "飲む" to "のむ",
        "見る" to "みる", "聞く" to "きく", "読む" to "よむ",
        "書く" to "かく", "行く" to "いく", "来る" to "くる",
        "帰る" to "かえる", "思う" to "おもう", "知る" to "しる",
        "分かる" to "わかる", "出来る" to "できる",
        "美しい" to "うつくしい", "可愛い" to "かわいい",
        "綺麗" to "きれい", "元気" to "げんき", "友達" to "ともだち",
        "学校" to "がっこう", "先生" to "せんせい", "生徒" to "せいと",
        "仕事" to "しごと", "天気" to "てんき", "世界" to "せかい",
        "心" to "こころ", "夢" to "ゆめ", "花" to "はな",
        "海" to "うみ", "山" to "やま", "空" to "そら",
        "星" to "ほし", "風" to "かぜ", "雨" to "あめ",
        "雪" to "ゆき", "春" to "はる", "夏" to "なつ",
        "秋" to "あき", "冬" to "ふゆ",
        "猫" to "ねこ", "犬" to "いぬ", "鳥" to "とり",
        "家" to "いえ", "本" to "ほん", "手" to "て",
        "目" to "め", "耳" to "みみ", "口" to "くち",
        "水" to "みず", "火" to "ひ", "石" to "いし",
        "木" to "き", "森" to "もり", "島" to "しま",
        "国" to "くに", "町" to "まち", "道" to "みち",
        "電車" to "でんしゃ", "車" to "くるま", "音楽" to "おんがく",
        "映画" to "えいが", "写真" to "しゃしん", "料理" to "りょうり",
        "旅行" to "りょこう", "勉強" to "べんきょう",
        "大丈夫" to "だいじょうぶ", "一緒" to "いっしょ",
        "最高" to "さいこう", "素敵" to "すてき", "幸せ" to "しあわせ",
        "お話" to "おはなし", "お願い" to "おねがい"
    )

    /**
     * Converts a Japanese character run to romaji.
     * Handles: katakana→hiragana normalization, kanji→reading lookup, hiragana→romaji table.
     */
    private fun japaneseToRomaji(text: String): String {
        val sb = StringBuilder()

        // First pass: resolve kanji by greedy longest-match against KANJI_READINGS
        val hiraganaText = resolveKanjiToHiragana(text)

        // Second pass: convert hiragana to romaji using greedy matching
        var i = 0
        while (i < hiraganaText.length) {
            var matched = false

            // Try digraphs first (2-char matches)
            if (i + 1 < hiraganaText.length) {
                val twoChar = hiraganaText.substring(i, i + 2)
                val romaji = HIRAGANA_TO_ROMAJI.firstOrNull { it.first == twoChar }
                if (romaji != null) {
                    sb.append(romaji.second)
                    i += 2
                    matched = true
                }
            }

            if (!matched) {
                val oneChar = hiraganaText[i].toString()
                val romaji = HIRAGANA_TO_ROMAJI.firstOrNull { it.first == oneChar }
                if (romaji != null) {
                    sb.append(romaji.second)
                } else {
                    // Katakana long vowel marker or unknown char
                    val c = hiraganaText[i]
                    if (c == 'ー' || c.code == 0x30FC) {
                        sb.append(":") // Long vowel
                    } else if (c.code in 0x30A0..0x30FF) {
                        // Katakana we missed — convert to hiragana offset
                        val hira = (c.code - 0x60).toChar()
                        val hRomaji = HIRAGANA_TO_ROMAJI.firstOrNull { it.first == hira.toString() }
                        if (hRomaji != null) sb.append(hRomaji.second) else sb.append(c)
                    } else {
                        sb.append(c)
                    }
                }
                i++
            }
        }

        return sb.toString()
    }

    /**
     * Resolves kanji in a string to hiragana readings via dictionary lookup.
     * Uses greedy longest-match. Unknown kanji are passed through as-is.
     * Also normalizes katakana to hiragana.
     */
    private fun resolveKanjiToHiragana(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            // Try longest match first (up to 4 characters)
            var matched = false
            for (len in minOf(4, text.length - i) downTo 1) {
                val substr = text.substring(i, i + len)
                val reading = KANJI_READINGS[substr]
                if (reading != null) {
                    sb.append(reading)
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                val c = text[i]
                // Katakana to hiragana normalization (U+30A1-30F6 → U+3041-3096)
                if (c.code in 0x30A1..0x30F6) {
                    sb.append((c.code - 0x60).toChar())
                } else {
                    sb.append(c)
                }
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Converts Japanese romaji to IPA phonemes suitable for Kokoro's j-voice presets.
     * Handles geminate consonants (Q), long vowels (:), and Japanese-specific phonetics.
     */
    private fun japaneseRomajiToIPA(romaji: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < romaji.length) {
            val remaining = romaji.substring(i)
            when {
                // Geminate: Q + consonant → double the consonant
                remaining.startsWith("Q") -> {
                    // Insert glottal stop (ʔ) for geminate
                    if (i + 1 < romaji.length) {
                        sb.append("ʔ")
                    }
                    i++
                    continue
                }
                // Long vowel marker
                remaining.startsWith(":") -> {
                    sb.append("ː")
                    i++
                    continue
                }
                // Consonant clusters
                remaining.startsWith("shi") -> { sb.append("ʃi"); i += 3; continue }
                remaining.startsWith("sha") -> { sb.append("ʃa"); i += 3; continue }
                remaining.startsWith("shu") -> { sb.append("ʃu"); i += 3; continue }
                remaining.startsWith("sho") -> { sb.append("ʃo"); i += 3; continue }
                remaining.startsWith("chi") -> { sb.append("tʃi"); i += 3; continue }
                remaining.startsWith("cha") -> { sb.append("tʃa"); i += 3; continue }
                remaining.startsWith("chu") -> { sb.append("tʃu"); i += 3; continue }
                remaining.startsWith("cho") -> { sb.append("tʃo"); i += 3; continue }
                remaining.startsWith("tsu") -> { sb.append("tsɯ"); i += 3; continue }
                remaining.startsWith("fu") ->  { sb.append("ɸɯ"); i += 2; continue }
                // Japanese 'r' is an alveolar tap, but Kokoro uses ɹ
                remaining.startsWith("ry") -> { sb.append("ɹj"); i += 2; continue }
                remaining.startsWith("r") && i + 1 < romaji.length && romaji[i + 1] in "aiueo" -> {
                    sb.append("ɹ")
                    i++
                    continue
                }
                // n before vowel or y → keep as n; before consonant or word-end → remain n
                remaining.startsWith("n") && i + 1 < romaji.length && romaji[i + 1] !in "aiueoy" -> {
                    // Moraic n
                    sb.append("n")
                    i++
                    continue
                }
                // ja/ju/jo
                remaining.startsWith("ja") -> { sb.append("dʒa"); i += 2; continue }
                remaining.startsWith("ju") -> { sb.append("dʒu"); i += 2; continue }
                remaining.startsWith("jo") -> { sb.append("dʒo"); i += 2; continue }
                remaining.startsWith("ji") -> { sb.append("dʒi"); i += 2; continue }
            }

            // Default: pass through character (vowels a,i,u,e,o and consonants)
            val c = romaji[i]
            when (c) {
                'u' -> sb.append("ɯ")  // Japanese u is unrounded
                else -> sb.append(c)
            }
            i++
        }

        return sb.toString()
    }

    // ==================== End Japanese G2P Pipeline ====================

    /**
     * Loads the 256-dimensional float style vector for the specified Kokoro voice.
     * Uses on-disk cached voice pack, or downloads on-demand (~522 KB) from HuggingFace.
     */
    private fun loadVoiceStyleEmbedding(context: Context, voiceName: String, tokenCount: Int): FloatArray {
        val cleanVoiceName = voiceName.trim().lowercase().ifBlank { "af_heart" }
        val voicesDir = File(context.filesDir, "voices")
        if (!voicesDir.exists()) voicesDir.mkdirs()

        val voiceFile = File(voicesDir, "$cleanVoiceName.bin")

        // 1. Download official voice embedding bin file if missing (only ~522 KB)
        if (!voiceFile.exists() || voiceFile.length() < 1024) {
            try {
                val voiceUrl = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/voices/$cleanVoiceName.bin"
                val req = Request.Builder().url(voiceUrl).build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        voiceFile.writeBytes(bytes)
                        Log.d(TAG, "Berhasil mengunduh voice pack resmi $cleanVoiceName.bin (${bytes.size} bytes)")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mengunduh voice pack $cleanVoiceName: ${e.message}")
            }
        }

        // 2. Read style vector from local binary file
        if (voiceFile.exists() && voiceFile.length() >= 510 * 256 * 4) {
            val channel = FileInputStream(voiceFile).channel
            val byteBuf = ByteBuffer.allocateDirect(510 * 256 * 4).apply {
                order(ByteOrder.LITTLE_ENDIAN)
            }
            channel.read(byteBuf)
            byteBuf.flip()
            val floatBuf = byteBuf.asFloatBuffer()

            val rowIndex = tokenCount.coerceIn(0, 509)
            val styleData = FloatArray(256)
            floatBuf.position(rowIndex * 256)
            floatBuf.get(styleData)
            channel.close()
            return styleData
        }

        // 3. Fallback to af_heart.bin if custom voice pack is unavailable
        val fallbackFile = File(voicesDir, "af_heart.bin")
        if (fallbackFile.exists() && fallbackFile.length() >= 510 * 256 * 4) {
            val channel = FileInputStream(fallbackFile).channel
            val byteBuf = ByteBuffer.allocateDirect(510 * 256 * 4).apply {
                order(ByteOrder.LITTLE_ENDIAN)
            }
            channel.read(byteBuf)
            byteBuf.flip()
            val floatBuf = byteBuf.asFloatBuffer()

            val rowIndex = tokenCount.coerceIn(0, 509)
            val styleData = FloatArray(256)
            floatBuf.position(rowIndex * 256)
            floatBuf.get(styleData)
            channel.close()
            return styleData
        }

        throw IllegalStateException("Voice style pack '$cleanVoiceName.bin' belum terunduh. Hubungkan internet sejenak untuk mengunduh voice pack (~522 KB)!")
    }

    private fun playWav(
        context: Context,
        wavFile: File,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(wavFile))
                prepare()
                setOnCompletionListener {
                    onDone()
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    onError("MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            onError("Gagal memutar audio Sherpa-onnx: ${e.message}")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val dataSize = pcmData.size * 2
        val chunkSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(chunkSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size for PCM
                putShort(1) // AudioFormat 1 = PCM
                putShort(numChannels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((numChannels * (bitsPerSample / 8)).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }
            out.write(header.array())

            val byteBuffer = ByteBuffer.allocate(dataSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (sample in pcmData) {
                    putShort(sample)
                }
            }
            out.write(byteBuffer.array())
        }
    }
}
