package com.ryzumi.miraiai.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModelDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MODEL_ID = "KEY_MODEL_ID"
        const val KEY_DOWNLOAD_URL = "KEY_DOWNLOAD_URL"
        const val KEY_FILE_NAME = "KEY_FILE_NAME"
        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_LOCAL_PATH = "KEY_LOCAL_PATH"
        const val KEY_ERROR = "KEY_ERROR"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return@withContext Result.failure(
            workDataOf(KEY_ERROR to "Missing model ID")
        )

        val fileName = inputData.getString(KEY_FILE_NAME) ?: "model.gguf"
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
            ?: "https://huggingface.co/$modelId/resolve/main/$fileName"

        try {
            val modelsDir = File(applicationContext.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val safeFileName = "${modelId.replace("/", "_")}_$fileName"
            val targetFile = File(modelsDir, safeFileName)
            val tempFile = File(modelsDir, "$safeFileName.tmp")

            var existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            val requestBuilder = Request.Builder().url(downloadUrl)
            if (existingBytes > 0L) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Response body is null")
            )

            val isPartial = response.code == 206
            if (!isPartial) {
                // Server does not support range, restart from beginning
                existingBytes = 0L
                if (tempFile.exists()) tempFile.delete()
            }

            val responseContentLength = body.contentLength()
            val totalBytes = if (responseContentLength > 0L) existingBytes + responseContentLength else -1L

            var downloadedBytes = existingBytes
            val appendMode = isPartial && existingBytes > 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile, appendMode).use { outputStream ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var lastProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            return@withContext Result.failure(workDataOf(KEY_ERROR to "Download stopped"))
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0L) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                setProgress(workDataOf(KEY_PROGRESS to progress))
                            }
                        }
                    }
                }
            }

            if (isStopped) {
                return@withContext Result.failure(workDataOf(KEY_ERROR to "Download stopped"))
            }

            if (tempFile.exists()) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
            }

            Result.success(
                workDataOf(
                    KEY_PROGRESS to 100,
                    KEY_LOCAL_PATH to targetFile.absolutePath
                )
            )
        } catch (e: Exception) {
            if (isStopped) {
                Result.failure(workDataOf(KEY_ERROR to "Download stopped"))
            } else {
                Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
            }
        }
    }
}
