package com.ryzumi.miraiai

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.ryzumi.miraiai.data.local.MiraiDatabase
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import com.ryzumi.miraiai.domain.util.DataUrlFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MiraiApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .components {
                add(object : coil.map.Mapper<String, File> {
                    override fun map(data: String, options: coil.request.Options): File? {
                        return if (data.startsWith("/")) File(data) else null
                    }
                })
                add(DataUrlFetcher.Factory())
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        com.ryzumi.miraiai.domain.util.ChatNotificationHelper.createNotificationChannel(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activityReferences = 0
            private var isActivityChangingConfigurations = false

            override fun onActivityStarted(activity: android.app.Activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    com.ryzumi.miraiai.domain.engine.ChatGenerationManager.setAppForeground(true)
                }
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    com.ryzumi.miraiai.domain.engine.ChatGenerationManager.setAppForeground(false)
                }
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
            }
        })

        val applicationScope = CoroutineScope(Dispatchers.IO)
        applicationScope.launch {
            val db = MiraiDatabase.getInstance(this@MiraiApplication)

            // Seed default persona if none exists
            val existingPersonas = db.userPersonaDao().getAllPersonas().first()
            if (existingPersonas.isEmpty()) {
                val defaultPersona = UserPersonaEntity(
                    name = "User",
                    personaDescription = "An inquisitive explorer chatting with AI characters.",
                    isDefault = true
                )
                db.userPersonaDao().insertPersona(defaultPersona)
            }

            val existingCharacters = db.characterDao().getAllCharacters().first()
            val avatarPath = saveAppIconAsDefaultAvatar(this@MiraiApplication)

            if (existingCharacters.isEmpty()) {
                val defaultChar = CharacterEntity(
                    name = "Mirai AI",
                    avatarUri = avatarPath,
                    description = "An intelligent, friendly, and helpful AI assistant ready to assist with daily tasks, answer questions, discuss ideas, and explore various topics objectively.",
                    personality = "Friendly, neutral, polite, objective, and supportive. Communicates in a courteous, clear, and warm manner without taking biased stances.",
                    scenario = "{{char}} is ready to chat, provide helpful information, and explore ideas with {{user}} across various everyday conversations.",
                    impression = "Maintains a polite, warm, objective, friendly, and articulate tone.",
                    tags = listOf("Assistant", "Friendly", "Neutral", "AI"),
                    firstMessage = "Hello {{user}}! I'm Mirai AI. How can I assist you or what would you like to talk about today?"
                )
                db.characterDao().insertCharacter(defaultChar)
            } else if (avatarPath != null) {
                val miraiWithoutAvatar = existingCharacters.find { it.name == "Mirai AI" && it.avatarUri.isNullOrBlank() }
                if (miraiWithoutAvatar != null) {
                    db.characterDao().updateCharacter(miraiWithoutAvatar.copy(avatarUri = avatarPath))
                }
            }
        }
    }

    private fun saveAppIconAsDefaultAvatar(context: Context): String? {
        return try {
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }
            val targetFile = File(avatarsDir, "mirai_default_avatar.png")
            if (targetFile.exists() && targetFile.length() > 500) {
                return targetFile.absolutePath
            }

            val drawable: android.graphics.drawable.Drawable = try {
                context.packageManager.getApplicationIcon(context.packageName)
            } catch (e: Exception) {
                null
            } ?: try {
                ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            } catch (e: Exception) {
                null
            } ?: ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
            ?: return null

            val targetSize = 512
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && drawable is android.graphics.drawable.AdaptiveIconDrawable) {
                drawable.background?.let { bg ->
                    bg.setBounds(0, 0, targetSize, targetSize)
                    bg.draw(canvas)
                }
                drawable.foreground?.let { fg ->
                    fg.setBounds(0, 0, targetSize, targetSize)
                    fg.draw(canvas)
                }
            } else if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
                val src = drawable.bitmap
                val scaled = Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
                canvas.drawBitmap(scaled, 0f, 0f, null)
                if (scaled != src) scaled.recycle()
            } else {
                drawable.setBounds(0, 0, targetSize, targetSize)
                drawable.draw(canvas)
            }

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            bitmap.recycle()
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
