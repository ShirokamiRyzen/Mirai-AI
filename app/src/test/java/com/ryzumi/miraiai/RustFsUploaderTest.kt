package com.ryzumi.miraiai

import com.ryzumi.miraiai.data.network.RustFsUploader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RustFsUploaderTest {

    @Test
    fun testGeneratePresignedGetUrl() {
        val presignedUrl = RustFsUploader.generatePresignedGetUrl("uploads/test.jpg")
        println("Generated Presigned URL: $presignedUrl")
        assertTrue(presignedUrl.contains("https://r1.ryzumi.net/mirai-ai/uploads/test.jpg"))
        assertTrue(presignedUrl.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"))
        assertTrue(presignedUrl.contains("X-Amz-Credential="))
        assertTrue(presignedUrl.contains("X-Amz-Signature="))
        assertTrue(presignedUrl.contains("X-Amz-Expires=604800"))
    }

    @Test
    fun testSignUrlIfNeeded() {
        val rawPublicUrl = "https://r1.ryzumi.net/mirai-ai/uploads/test.png"
        val signedUrl = RustFsUploader.signUrlIfNeeded(rawPublicUrl)
        println("Signed URL: $signedUrl")
        assertTrue(signedUrl.contains("X-Amz-Signature="))
        assertTrue(signedUrl.contains("X-Amz-Credential="))

        // Already signed URL should not be double signed
        val alreadySigned = RustFsUploader.signUrlIfNeeded(signedUrl)
        assertTrue(alreadySigned == signedUrl)
    }

    @Test
    fun testUploadImageBytes() = runBlocking {
        // 1x1 dummy jpg/png test bytes
        val testBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val result = RustFsUploader.uploadImageBytes(testBytes, contentType = "image/jpeg", extension = "jpg")
        println("Upload result: $result")
        if (result.isSuccess) {
            val url = result.getOrThrow()
            assertTrue(url.contains("X-Amz-Signature="))
            assertTrue(url.contains(".jpg"))
        }
    }
}

