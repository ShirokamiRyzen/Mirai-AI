package com.ryzumi.miraiai.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object RustFsUploader {
    const val ENDPOINT = "https://r1.ryzumi.net"
    const val HOST = "r1.ryzumi.net"
    const val BUCKET = "mirai-ai"
    const val ACCESS_KEY = "mirai-ai-public-up"
    const val SECRET_KEY = "VFTiWRWz0Vf4AogrfbdeEDPcawmV0AEPHdkZZzGX"
    const val REGION = "us-east-1"
    const val SERVICE = "s3"
    const val DEFAULT_EXPIRES_SECONDS = 604800L // 7 days (maximum allowed by S3 SigV4)

    private val client = OkHttpClient.Builder().build()

    fun generatePresignedGetUrl(
        objectKey: String,
        expiresInSeconds: Long = DEFAULT_EXPIRES_SECONDS
    ): String {
        val cleanKey = objectKey.trimStart('/')
        val canonicalUri = "/$BUCKET/$cleanKey"

        val dateUtc = Date()
        val amzDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val amzDate = amzDateFormat.format(dateUtc)
        val dateStamp = dateStampFormat.format(dateUtc)

        val credentialScope = "$dateStamp/$REGION/$SERVICE/aws4_request"
        val fullCredential = "$ACCESS_KEY/$credentialScope"
        val encodedCredential = rfc3986Encode(fullCredential)

        // Query parameters in alphabetical order
        val canonicalQueryString = "X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Credential=$encodedCredential" +
                "&X-Amz-Date=$amzDate" +
                "&X-Amz-Expires=$expiresInSeconds" +
                "&X-Amz-SignedHeaders=host"

        val canonicalHeaders = "host:$HOST\n"
        val signedHeaders = "host"
        val payloadHash = "UNSIGNED-PAYLOAD"

        val canonicalRequest = "GET\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(StandardCharsets.UTF_8))}"

        val signingKey = getSignatureKey(SECRET_KEY, dateStamp, REGION, SERVICE)
        val signature = hmacHex(signingKey, stringToSign)

        return "$ENDPOINT$canonicalUri?$canonicalQueryString&X-Amz-Signature=$signature"
    }

    fun signUrlIfNeeded(
        urlOrKey: String,
        expiresInSeconds: Long = DEFAULT_EXPIRES_SECONDS
    ): String {
        if (!urlOrKey.contains(HOST)) return urlOrKey
        if (urlOrKey.contains("X-Amz-Signature=")) return urlOrKey
        val objectKey = when {
            urlOrKey.startsWith("$ENDPOINT/$BUCKET/") -> urlOrKey.removePrefix("$ENDPOINT/$BUCKET/")
            urlOrKey.startsWith("https://$HOST/$BUCKET/") -> urlOrKey.removePrefix("https://$HOST/$BUCKET/")
            urlOrKey.startsWith("http://$HOST/$BUCKET/") -> urlOrKey.removePrefix("http://$HOST/$BUCKET/")
            else -> return urlOrKey
        }
        return generatePresignedGetUrl(objectKey, expiresInSeconds)
    }

    suspend fun uploadImageBytes(
        imageBytes: ByteArray,
        contentType: String = "image/jpeg",
        extension: String = "jpg"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val filename = "${UUID.randomUUID()}.$extension"
            val objectKey = "uploads/$filename"
            val canonicalUri = "/$BUCKET/$objectKey"
            val targetUrl = "$ENDPOINT$canonicalUri"

            val dateUtc = Date()
            val amzDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dateStampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val amzDate = amzDateFormat.format(dateUtc)
            val dateStamp = dateStampFormat.format(dateUtc)

            // 1. Hash Payload
            val payloadHash = sha256Hex(imageBytes)

            // 2. Canonical Request
            val canonicalHeaders = "content-type:$contentType\nhost:$HOST\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
            val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            // 3. String to Sign
            val credentialScope = "$dateStamp/$REGION/$SERVICE/aws4_request"
            val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(StandardCharsets.UTF_8))}"

            // 4. Signing Key & Signature
            val signingKey = getSignatureKey(SECRET_KEY, dateStamp, REGION, SERVICE)
            val signature = hmacHex(signingKey, stringToSign)

            // 5. Authorization Header
            val authHeader = "AWS4-HMAC-SHA256 Credential=$ACCESS_KEY/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val requestBody = imageBytes.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(targetUrl)
                .put(requestBody)
                .header("Host", HOST)
                .header("Content-Type", contentType)
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                // Generate and return presigned GET URL
                val presignedUrl = generatePresignedGetUrl(objectKey)
                Result.success(presignedUrl)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(Exception("RustFS S3 Upload failed with HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun rfc3986Encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacHex(key: ByteArray, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return rawHmac.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(StandardCharsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }
}
