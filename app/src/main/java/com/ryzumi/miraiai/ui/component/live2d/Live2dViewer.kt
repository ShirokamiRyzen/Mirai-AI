package com.ryzumi.miraiai.ui.component.live2d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ryzumi.miraiai.domain.live2d.Live2dManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder

private fun getMimeTypeForExtension(ext: String): String {
    return when (ext.lowercase()) {
        "html", "htm" -> "text/html"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "wav" -> "audio/wav"
        "mp3" -> "audio/mpeg"
        "ogg" -> "audio/ogg"
        "moc", "moc3" -> "application/octet-stream"
        else -> "application/octet-stream"
    }
}

private fun getOptimizedImageStream(file: File): InputStream {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val maxDim = maxOf(options.outWidth, options.outHeight)
        if (maxDim <= 2048) {
            return FileInputStream(file)
        }

        var inSampleSize = 1
        while ((maxDim / (inSampleSize * 2)) >= 2048) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: return FileInputStream(file)

        val targetMax = 2048f
        val currentMax = maxOf(decoded.width, decoded.height).toFloat()
        val finalBitmap = if (currentMax > targetMax) {
            val ratio = targetMax / currentMax
            val scaled = Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != decoded) decoded.recycle()
            scaled
        } else {
            decoded
        }

        try {
            FileOutputStream(file).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            finalBitmap.recycle()
            return FileInputStream(file)
        } catch (e: Exception) {
            val bos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, bos)
            finalBitmap.recycle()
            return ByteArrayInputStream(bos.toByteArray())
        }
    } catch (e: Exception) {
        android.util.Log.e("MiraiLive2D", "Failed to optimize image: ${e.message}", e)
        return FileInputStream(file)
    }
}

class Live2dViewerController {
    internal var webView: WebView? = null

    fun setExpression(name: String) {
        val safe = name.replace("'", "\\'")
        webView?.evaluateJavascript("if (window.setExpression) { window.setExpression('$safe'); }", null)
    }

    fun setPartOpacity(partId: String, opacity: Float) {
        val safe = partId.replace("'", "\\'")
        webView?.evaluateJavascript("if (window.setPartOpacity) { window.setPartOpacity('$safe', $opacity); }", null)
    }

    fun setParamValue(paramId: String, value: Float) {
        val safe = paramId.replace("'", "\\'")
        webView?.evaluateJavascript("if (window.setCostumeParam) { window.setCostumeParam('$safe', $value); }", null)
    }

    fun playMotion(group: String, index: Int? = null) {
        val safe = group.replace("'", "\\'")
        val idxArg = if (index != null && index >= 0) "$index" else "null"
        webView?.evaluateJavascript("if (window.playMotion) { window.playMotion('$safe', $idxArg); }", null)
    }

    fun resetView() {
        webView?.evaluateJavascript("if (window.resetView) { window.resetView(); }", null)
    }

    fun loadOutfit(charId: String, modelRelPath: String) {
        val safeRelPath = modelRelPath.split('/').joinToString("/") { Uri.encode(it) }
        val url = "/model/" + Uri.encode(charId) + "/" + safeRelPath
        webView?.evaluateJavascript("if (window.loadOutfit) { window.loadOutfit('$url'); }", null)
    }
}

class Live2dJsBridge(
    private val onTouched: (String) -> Unit,
    private val onCapabilities: (String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onModelTouched(zone: String) {
        onTouched(zone)
    }

    @android.webkit.JavascriptInterface
    fun onModelCapabilitiesLoaded(json: String) {
        onCapabilities(json)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Live2dViewer(
    characterId: String,
    modelRelativePath: String,
    isSpeaking: Boolean = false,
    emotion: String = "neutral",
    motion: String? = null,
    motionTrigger: Long = 0L,
    controller: Live2dViewerController = remember { Live2dViewerController() },
    onCapabilitiesLoaded: ((com.ryzumi.miraiai.domain.live2d.Live2dModelCapabilities) -> Unit)? = null,
    onModelTouched: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef: WebView? = remember { null }
    val currentOnTouched = androidx.compose.runtime.rememberUpdatedState(onModelTouched)
    val currentOnCapabilities = androidx.compose.runtime.rememberUpdatedState(onCapabilitiesLoaded)

    androidx.compose.runtime.LaunchedEffect(motionTrigger) {
        if (motionTrigger > 0L && !motion.isNullOrBlank()) {
            controller.playMotion(motion)
        }
    }

    val bridge = remember {
        Live2dJsBridge(
            onTouched = { zone ->
                currentOnTouched.value?.invoke(zone)
            },
            onCapabilities = { jsonStr ->
                try {
                    val gson = com.google.gson.Gson()
                    val root = gson.fromJson(jsonStr, com.google.gson.JsonObject::class.java)

                    // 1. Models available on disk
                    val models = Live2dManager.getAvailableModelFiles(context, characterId, modelRelativePath)

                    // 2. Display names from cdi3.json
                    val (partNamesMap, paramNamesMap) = Live2dManager.getDisplayInfoNames(context, characterId, modelRelativePath)

                    // 3. Expressions (combine detected + standard presets)
                    val expressions = mutableListOf<com.ryzumi.miraiai.domain.live2d.Live2dExpressionInfo>()
                    root.getAsJsonArray("expressions")?.forEach { elem ->
                        val obj = elem.asJsonObject
                        val id = obj.get("id")?.asString ?: ""
                        val name = obj.get("name")?.asString ?: id
                        if (id.isNotBlank()) {
                            expressions.add(com.ryzumi.miraiai.domain.live2d.Live2dExpressionInfo(id, name, false))
                        }
                    }


                    // 4. Parts (with display names)
                    val parts = mutableListOf<com.ryzumi.miraiai.domain.live2d.Live2dPartInfo>()
                    root.getAsJsonArray("parts")?.forEach { elem ->
                        val obj = elem.asJsonObject
                        val id = obj.get("id")?.asString ?: ""
                        val op = obj.get("opacity")?.asFloat ?: 1f
                        if (id.isNotBlank()) {
                            val cdiName = partNamesMap[id]
                            val disp = if (!cdiName.isNullOrBlank()) "$cdiName ($id)" else id
                            parts.add(com.ryzumi.miraiai.domain.live2d.Live2dPartInfo(id, disp, op))
                        }
                    }

                    // 5. Parameters
                    val params = mutableListOf<com.ryzumi.miraiai.domain.live2d.Live2dParamInfo>()
                    root.getAsJsonArray("parameters")?.forEach { elem ->
                        val obj = elem.asJsonObject
                        val id = obj.get("id")?.asString ?: ""
                        val min = obj.get("min")?.asFloat ?: 0f
                        val max = obj.get("max")?.asFloat ?: 1f
                        val def = obj.get("defaultValue")?.asFloat ?: 0f
                        val cur = obj.get("currentValue")?.asFloat ?: def
                        if (id.isNotBlank()) {
                            val cdiName = paramNamesMap[id]
                            val disp = if (!cdiName.isNullOrBlank()) "$cdiName ($id)" else id
                            params.add(com.ryzumi.miraiai.domain.live2d.Live2dParamInfo(id, disp, min, max, def, cur))
                        }
                    }

                    // 6. Motions
                    val motions = mutableListOf<com.ryzumi.miraiai.domain.live2d.Live2dMotionInfo>()
                    root.getAsJsonArray("motions")?.forEach { elem ->
                        val obj = elem.asJsonObject
                        val group = obj.get("group")?.asString ?: ""
                        val index = obj.get("index")?.asInt ?: 0
                        val name = obj.get("name")?.asString ?: group
                        motions.add(com.ryzumi.miraiai.domain.live2d.Live2dMotionInfo(group, index, name))
                    }

                    // Scan folder for extra motion files (e.g. chibang.motion3.json)
                    val baseDir = Live2dManager.getCharacterLive2dDir(context, characterId)
                    baseDir.walkTopDown().forEach { f ->
                        if (f.isFile && (f.name.endsWith(".motion3.json", ignoreCase = true) || f.name.endsWith(".motion.json", ignoreCase = true))) {
                            val clean = f.name.removeSuffix(".motion3.json").removeSuffix(".motion.json")
                            if (motions.none { it.name.equals(clean, ignoreCase = true) || it.group.equals(clean, ignoreCase = true) }) {
                                motions.add(com.ryzumi.miraiai.domain.live2d.Live2dMotionInfo(clean, 0, clean))
                            }
                        }
                    }

                    val caps = com.ryzumi.miraiai.domain.live2d.Live2dModelCapabilities(
                        models = models,
                        expressions = expressions,
                        parts = parts,
                        parameters = params,
                        motions = motions
                    )

                    currentOnCapabilities.value?.invoke(caps)
                } catch (e: Exception) {
                    android.util.Log.e("MiraiLive2D", "Failed to parse model capabilities", e)
                }
            }
        )
    }

    LaunchedEffect(isSpeaking, emotion) {
        webViewRef?.evaluateJavascript(
            "if (window.setSpeaking) { window.setSpeaking($isSpeaking); } if (window.setEmotion) { window.setEmotion('$emotion'); }",
            null
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.webView = null
            webViewRef?.let { wv ->
                wv.onPause()
                wv.stopLoading()
                wv.destroy()
            }
            webViewRef = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView.setWebContentsDebuggingEnabled(true)
            WebView(ctx).apply {
                webViewRef = this
                controller.webView = this
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_NONE, null)
                onResume()

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                addJavascriptInterface(bridge, "AndroidBridge")

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        android.util.Log.d(
                            "MiraiLive2D",
                            "[JS] ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()}) [${consoleMessage?.sourceId()}]"
                        )
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val requestUrl = request?.url ?: return null
                        if (requestUrl.host.equals("live2d.local", ignoreCase = true)) {
                            val path = requestUrl.path ?: ""
                            return handleLocalLive2dRequest(ctx, path)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                val targetUrl = "https://live2d.local/viewer.html?charId=" +
                        Uri.encode(characterId) +
                        "&model=" +
                        Uri.encode(modelRelativePath)
                loadUrl(targetUrl)
            }
        },
        update = { wv ->
            webViewRef = wv
            controller.webView = wv
            wv.evaluateJavascript(
                "if (window.setSpeaking) { window.setSpeaking($isSpeaking); } if (window.setEmotion) { window.setEmotion('$emotion'); }",
                null
            )
        }
    )
}

private fun handleLocalLive2dRequest(context: Context, path: String): WebResourceResponse? {
    try {
        // 1. Static viewer assets
        if (path == "/" || path == "/viewer.html") {
            val input = context.assets.open("live2d/viewer.html")
            return WebResourceResponse("text/html", "UTF-8", input)
        }
        if (path == "/live2d.min.js") {
            val input = context.assets.open("live2d/live2d.min.js")
            return WebResourceResponse("application/javascript", "UTF-8", input)
        }
        if (path == "/live2dcubismcore.min.js") {
            val input = context.assets.open("live2d/live2dcubismcore.min.js")
            return WebResourceResponse("application/javascript", "UTF-8", input)
        }
        if (path == "/pixi.min.js") {
            val input = context.assets.open("live2d/pixi.min.js")
            return WebResourceResponse("application/javascript", "UTF-8", input)
        }
        if (path == "/index.min.js") {
            val input = context.assets.open("live2d/index.min.js")
            return WebResourceResponse("application/javascript", "UTF-8", input)
        }

        // 2. Character dynamic model files: /model/<charId>/...
        if (path.startsWith("/model/")) {
            val rawSub = path.removePrefix("/model/")
            val decodedFull = try {
                URLDecoder.decode(rawSub, "UTF-8")
            } catch (e: Exception) {
                rawSub
            }
            val firstSlash = decodedFull.indexOf('/')
            if (firstSlash != -1) {
                val cid = decodedFull.substring(0, firstSlash)
                val decodedSubPath = decodedFull.substring(firstSlash + 1)

                val baseDir = Live2dManager.getCharacterLive2dDir(context, cid)
                val targetFile = File(baseDir, decodedSubPath)
                val exists = targetFile.exists() && targetFile.isFile
                val canRead = targetFile.canRead()
                val size = if (exists) targetFile.length() else -1L

                android.util.Log.d(
                    "MiraiLive2D",
                    "Intercepted model file: path='$path', cid='$cid', decodedSubPath='$decodedSubPath', exists=$exists, canRead=$canRead, size=$size, abs='${targetFile.absolutePath}'"
                )

                if (targetFile.canonicalPath.startsWith(baseDir.canonicalPath) && exists) {
                    val mimeType = getMimeTypeForExtension(targetFile.extension)
                    val responseHeaders = mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Cache-Control" to "no-cache"
                    )
                    val stream = if (targetFile.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")) {
                        getOptimizedImageStream(targetFile)
                    } else {
                        FileInputStream(targetFile)
                    }
                    return WebResourceResponse(
                        mimeType,
                        null,
                        200,
                        "OK",
                        responseHeaders,
                        stream
                    )
                } else {
                    android.util.Log.w(
                        "MiraiLive2D",
                        "File not found or outside baseDir! target='${targetFile.canonicalPath}', base='${baseDir.canonicalPath}'"
                    )
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MiraiLive2D", "Error handling request for path '$path'", e)
    }
    android.util.Log.w("MiraiLive2D", "Unhandled or missing request for path '$path'")
    return null
}
