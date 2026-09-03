package com.ryzumi.miraiai.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ProcessedImage(
    val bytes: ByteArray,
    val contentType: String,
    val extension: String
)

object ImageUtils {

    fun processImageBytes(
        bytes: ByteArray,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): ProcessedImage? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            val mime = options.outMimeType?.lowercase() ?: ""
            val isPng = mime == "image/png"

            val extension = if (isPng) "png" else "jpg"
            val contentType = if (isPng) "image/png" else "image/jpeg"
            val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            val bitmap = decodeAndCorrectBitmap(bytes, maxDimension) ?: return null

            val outStream = ByteArrayOutputStream()
            bitmap.compress(compressFormat, if (isPng) 100 else quality, outStream)
            bitmap.recycle()

            val processedBytes = outStream.toByteArray()
            ProcessedImage(
                bytes = processedBytes,
                contentType = contentType,
                extension = extension
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveChatImageLocally(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            val uriString = imageUri.toString()
            val chatImagesDir = File(context.filesDir, "chat_images")
            if (uriString.startsWith("/") && uriString.startsWith(chatImagesDir.absolutePath)) {
                return@withContext uriString
            }
            if (uriString.startsWith("data:image/")) {
                val base64Data = uriString.substringAfter("base64,")
                val bytes = safeBase64Decode(base64Data)
                return@withContext saveBytesToFile(context, bytes, maxDimension, quality)
            }

            val bytes = try {
                if (uriString.startsWith("/")) {
                    val file = File(uriString)
                    if (file.exists()) file.readBytes() else null
                } else if (uriString.startsWith("file://")) {
                    val path = imageUri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists()) file.readBytes() else null
                    } else null
                } else {
                    context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: return@withContext null

            saveBytesToFile(context, bytes, maxDimension, quality)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBytesToFile(
        context: Context,
        bytes: ByteArray,
        maxDimension: Int,
        quality: Int
    ): String? {
        return try {
            val webpBytes = toWebpBytes(bytes, maxDimension, quality) ?: return null
            val dir = File(context.filesDir, "chat_images").apply { if (!exists()) mkdirs() }
            val targetFile = File(dir, "chat_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")

            val fos = FileOutputStream(targetFile)
            fos.write(webpBytes)
            fos.flush()
            fos.close()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun safeBase64Decode(str: String): ByteArray {
        return try {
            Base64.decode(str, Base64.DEFAULT)
        } catch (e: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    fun safeBase64Encode(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    suspend fun getImageBytesForUpload(
        context: Context?,
        imageSource: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (imageSource.startsWith("data:image/")) {
                val base64Data = imageSource.substringAfter("base64,")
                return@withContext safeBase64Decode(base64Data)
            }
            if (imageSource.startsWith("/")) {
                val file = File(imageSource)
                if (file.exists()) return@withContext file.readBytes()
            }
            if (imageSource.startsWith("file://")) {
                val path = Uri.parse(imageSource).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) return@withContext file.readBytes()
                }
            }
            if (context != null) {
                context.contentResolver.openInputStream(Uri.parse(imageSource))?.use { it.readBytes() }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun processAndEncodeImage(
        context: Context?,
        imageSource: String,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (imageSource.startsWith("data:image/")) {
                return@withContext imageSource
            }

            val bytes = getImageBytesForUpload(context, imageSource) ?: return@withContext null
            val processed = processImageBytes(bytes, maxDimension, quality) ?: return@withContext null
            val base64String = safeBase64Encode(processed.bytes)
            "data:${processed.contentType};base64,$base64String"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeAndCorrectBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
        return try {
            // 1. Check dimensions with inJustDecodeBounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 2. Calculate inSampleSize
            var inSampleSize = 1
            while (origWidth / (inSampleSize * 2) >= maxDimension || origHeight / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            // 4. Handle EXIF Orientation rotation
            bitmap = correctExifOrientation(bytes, bitmap)

            // 5. Resize to maxDimension if needed
            val currentWidth = bitmap.width
            val currentHeight = bitmap.height
            if (currentWidth > maxDimension || currentHeight > maxDimension) {
                val (targetW, targetH) = if (currentWidth >= currentHeight) {
                    val ratio = maxDimension.toFloat() / currentWidth
                    Pair(maxDimension, (currentHeight * ratio).toInt().coerceAtLeast(1))
                } else {
                    val ratio = maxDimension.toFloat() / currentHeight
                    Pair((currentWidth * ratio).toInt().coerceAtLeast(1), maxDimension)
                }
                val resized = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (resized != bitmap) {
                    bitmap.recycle()
                    bitmap = resized
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Suppress("DEPRECATION")
    val webpCompressFormat: Bitmap.CompressFormat
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    suspend fun cropAndSaveAvatar(
        context: Context,
        imageSource: String,
        targetDimension: Int = 720,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = getImageBytesForUpload(context, imageSource) ?: return@withContext null
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val orientedBitmap = correctExifOrientation(bytes, rawBitmap)

            val width = orientedBitmap.width
            val height = orientedBitmap.height
            val squareSize = minOf(width, height)
            val cropX = (width - squareSize) / 2
            val cropY = (height - squareSize) / 2

            val squareBitmap = Bitmap.createBitmap(orientedBitmap, cropX, cropY, squareSize, squareSize)
            if (squareBitmap != orientedBitmap) {
                orientedBitmap.recycle()
            }

            // Downscale to targetDimension (720x720) if larger
            val finalBitmap = if (squareBitmap.width > targetDimension || squareBitmap.height > targetDimension) {
                val scaled = Bitmap.createScaledBitmap(squareBitmap, targetDimension, targetDimension, true)
                if (scaled != squareBitmap) {
                    squareBitmap.recycle()
                }
                scaled
            } else {
                squareBitmap
            }

            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")

            val fos = FileOutputStream(targetFile)
            finalBitmap.compress(webpCompressFormat, quality, fos)
            fos.flush()
            fos.close()
            finalBitmap.recycle()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun cropAndSaveAvatarFromWindow(
        context: Context,
        imageSource: String,
        panX: Float,
        panY: Float,
        zoomScale: Float,
        viewportDisplaySize: Float,
        targetDimension: Int = 720,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = getImageBytesForUpload(context, imageSource) ?: return@withContext null
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val orientedBitmap = correctExifOrientation(bytes, rawBitmap)

            val imgW = orientedBitmap.width.toFloat()
            val imgH = orientedBitmap.height.toFloat()

            // Calculate base scale to fill square viewport
            val baseScale = maxOf(viewportDisplaySize / imgW, viewportDisplaySize / imgH)
            val effectiveScale = maxOf(1f, zoomScale) * baseScale

            // Viewport center relative to original image coordinate space
            val viewCenterX = (imgW / 2f) - (panX / effectiveScale)
            val viewCenterY = (imgH / 2f) - (panY / effectiveScale)

            // Crop window size in original image coordinate space
            val cropSize = viewportDisplaySize / effectiveScale
            val cropSizeInt = cropSize.toInt().coerceIn(1, minOf(orientedBitmap.width, orientedBitmap.height))

            val left = (viewCenterX - (cropSizeInt / 2f)).toInt().coerceIn(0, orientedBitmap.width - cropSizeInt)
            val top = (viewCenterY - (cropSizeInt / 2f)).toInt().coerceIn(0, orientedBitmap.height - cropSizeInt)

            val croppedBitmap = Bitmap.createBitmap(orientedBitmap, left, top, cropSizeInt, cropSizeInt)
            if (croppedBitmap != orientedBitmap) {
                orientedBitmap.recycle()
            }

            // Downscale to targetDimension (720x720) if larger
            val finalBitmap = if (croppedBitmap.width > targetDimension || croppedBitmap.height > targetDimension) {
                val scaled = Bitmap.createScaledBitmap(croppedBitmap, targetDimension, targetDimension, true)
                if (scaled != croppedBitmap) {
                    croppedBitmap.recycle()
                }
                scaled
            } else {
                croppedBitmap
            }

            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")

            val fos = FileOutputStream(targetFile)
            finalBitmap.compress(webpCompressFormat, quality, fos)
            fos.flush()
            fos.close()
            finalBitmap.recycle()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun cropAndSaveAvatarFromFitWindow(
        context: Context,
        imageSource: String,
        panX: Float,
        panY: Float,
        zoomScale: Float,
        viewportW: Float,
        viewportH: Float,
        cropBoxSize: Float,
        targetDimension: Int = 720,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = getImageBytesForUpload(context, imageSource) ?: return@withContext null
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val orientedBitmap = correctExifOrientation(bytes, rawBitmap)

            val imgW = orientedBitmap.width.toFloat()
            val imgH = orientedBitmap.height.toFloat()

            // Display fit scale factor when using ContentScale.Fit
            val fitScale = minOf(viewportW / imgW, viewportH / imgH)
            val effectiveScale = maxOf(0.1f, zoomScale) * fitScale

            // Viewport center relative to original image coordinate space
            val viewCenterX = (imgW / 2f) - (panX / effectiveScale)
            val viewCenterY = (imgH / 2f) - (panY / effectiveScale)

            // Crop window size in original image coordinate space
            val cropSize = cropBoxSize / effectiveScale
            val cropSizeInt = cropSize.toInt().coerceIn(1, minOf(orientedBitmap.width, orientedBitmap.height))

            val left = (viewCenterX - (cropSizeInt / 2f)).toInt().coerceIn(0, orientedBitmap.width - cropSizeInt)
            val top = (viewCenterY - (cropSizeInt / 2f)).toInt().coerceIn(0, orientedBitmap.height - cropSizeInt)

            val croppedBitmap = Bitmap.createBitmap(orientedBitmap, left, top, cropSizeInt, cropSizeInt)
            if (croppedBitmap != orientedBitmap) {
                orientedBitmap.recycle()
            }

            // Downscale to targetDimension (720x720) if larger
            val finalBitmap = if (croppedBitmap.width > targetDimension || croppedBitmap.height > targetDimension) {
                val scaled = Bitmap.createScaledBitmap(croppedBitmap, targetDimension, targetDimension, true)
                if (scaled != croppedBitmap) {
                    croppedBitmap.recycle()
                }
                scaled
            } else {
                croppedBitmap
            }

            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")

            val fos = FileOutputStream(targetFile)
            finalBitmap.compress(webpCompressFormat, quality, fos)
            fos.flush()
            fos.close()
            finalBitmap.recycle()

            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun correctExifOrientation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        return try {
            ByteArrayInputStream(bytes).use { input ->
                val exif = ExifInterface(input)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (degrees != 0f) {
                    val matrix = Matrix().apply { postRotate(degrees) }
                    val rotated = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    bitmap.recycle()
                    rotated
                } else {
                    bitmap
                }
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    fun toWebpBytes(
        bytes: ByteArray,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): ByteArray? {
        return try {
            val bitmap = decodeAndCorrectBitmap(bytes, maxDimension) ?: return null
            val outStream = ByteArrayOutputStream()
            bitmap.compress(webpCompressFormat, quality, outStream)
            bitmap.recycle()
            outStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toJpegBytes(
        bytes: ByteArray,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): ByteArray? {
        return try {
            val bitmap = decodeAndCorrectBitmap(bytes, maxDimension) ?: return null
            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            bitmap.recycle()
            outStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveAvatarAsWebp(
        context: Context,
        imageSource: String,
        targetDimension: Int = 720,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = getImageBytesForUpload(context, imageSource) ?: return@withContext null
            val webpBytes = toWebpBytes(bytes, targetDimension, quality) ?: return@withContext null
            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp")
            FileOutputStream(targetFile).use { it.write(webpBytes) }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteLocalFile(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        try {
            val filePath = when {
                uriString.startsWith("file://") -> Uri.parse(uriString).path
                uriString.startsWith("/") -> uriString
                else -> null
            }
            if (!filePath.isNullOrBlank()) {
                val file = File(filePath)
                if (file.exists() && file.isFile) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
