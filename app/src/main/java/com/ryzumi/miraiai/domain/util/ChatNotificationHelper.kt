package com.ryzumi.miraiai.domain.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.ryzumi.miraiai.MainActivity
import com.ryzumi.miraiai.domain.macro.MacroEngine
import com.ryzumi.miraiai.domain.receiver.NotificationReplyReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChatNotificationHelper {
    const val CHANNEL_ID = "mirai_chat_responses"
    const val CHANNEL_NAME = "AI Character Responses"
    const val EXTRA_SESSION_ID = "extra_session_id"
    const val KEY_TEXT_REPLY = "key_text_reply"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when AI characters finish responding in background"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    suspend fun loadCircularBitmap(context: Context, uriString: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        try {
            val bytes = ImageUtils.getImageBytesForUpload(context, uriString) ?: return@withContext null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null

            val minDim = minOf(bitmap.width, bitmap.height)
            if (minDim <= 0) {
                bitmap.recycle()
                return@withContext null
            }
            val cropX = (bitmap.width - minDim) / 2
            val cropY = (bitmap.height - minDim) / 2
            val square = Bitmap.createBitmap(bitmap, cropX, cropY, minDim, minDim)
            if (square != bitmap) {
                bitmap.recycle()
            }

            val targetSize = minDim.coerceAtMost(256)
            val scaledSquare = if (square.width > targetSize) {
                val s = Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
                if (s != square) square.recycle()
                s
            } else {
                square
            }

            val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            val rect = Rect(0, 0, targetSize, targetSize)
            val rectF = RectF(rect)
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawOval(rectF, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaledSquare, rect, rect, paint)
            scaledSquare.recycle()

            output
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun showResponseNotification(
        context: Context,
        sessionId: String,
        characterName: String,
        messageContent: String,
        avatarUri: String? = null,
        userName: String = "You",
        userAvatarUri: String? = null
    ) = withContext(Dispatchers.IO) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanText = MacroEngine.stripThinking(messageContent).trim().ifBlank { "New response received." }

        val userPersonBuilder = Person.Builder()
            .setName(userName)
            .setKey("user_self")

        val userBitmap = loadCircularBitmap(context, userAvatarUri)
        if (userBitmap != null) {
            userPersonBuilder.setIcon(IconCompat.createWithBitmap(userBitmap))
        }
        val userPerson = userPersonBuilder.build()

        val charPersonBuilder = Person.Builder()
            .setName(characterName)
            .setKey(sessionId)

        val charBitmap = loadCircularBitmap(context, avatarUri)
        if (charBitmap != null) {
            charPersonBuilder.setIcon(IconCompat.createWithBitmap(charBitmap))
        }
        val charPerson = charPersonBuilder.build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(characterName)
            .setGroupConversation(false)
            .addMessage(cleanText, System.currentTimeMillis(), charPerson)

        // WhatsApp-style Inline Direct Reply Action
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to $characterName...")
            .build()

        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = NotificationReplyReceiver.ACTION_REPLY
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode() + 1000,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput)
        .setAllowGeneratedReplies(true)
        .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setStyle(messagingStyle)
            .setContentIntent(pendingIntent)
            .addAction(replyAction)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (charBitmap != null) {
            builder.setLargeIcon(charBitmap)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(sessionId.hashCode(), builder.build())
    }

    suspend fun showReplyingNotification(
        context: Context,
        sessionId: String,
        characterName: String,
        replyText: String,
        characterAvatarUri: String? = null,
        userName: String = "You",
        userAvatarUri: String? = null
    ) = withContext(Dispatchers.IO) {
        createNotificationChannel(context)

        val userPersonBuilder = Person.Builder()
            .setName(userName)
            .setKey("user_self")

        val userBitmap = loadCircularBitmap(context, userAvatarUri)
        if (userBitmap != null) {
            userPersonBuilder.setIcon(IconCompat.createWithBitmap(userBitmap))
        }
        val userPerson = userPersonBuilder.build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(characterName)
            .setGroupConversation(false)
            .addMessage(replyText, System.currentTimeMillis(), userPerson)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SESSION_ID, sessionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setStyle(messagingStyle)
            .setContentTitle(characterName)
            .setContentText("Sending reply...")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setNotificationSilent()
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setProgress(0, 0, true)

        if (userBitmap != null) {
            builder.setLargeIcon(userBitmap)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(sessionId.hashCode(), builder.build())
    }

    fun cancelNotification(context: Context, sessionId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(sessionId.hashCode())
    }
}
