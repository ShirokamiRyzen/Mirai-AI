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
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    com.ryzumi.miraiai.domain.engine.ChatGenerationManager.setAppForeground(false)
                }
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })

        seedInitialDataIfNeeded()
    }

    private fun seedInitialDataIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
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

            // Seed default character if none exists
            val existingCharacters = db.characterDao().getAllCharacters().first()
            if (existingCharacters.isEmpty()) {
                val sampleChar = CharacterEntity(
                    name = "Mirai",
                    description = "Your friendly and intelligent AI companion from the future.",
                    personality = "Kind, witty, curious, and deeply knowledgeable about technology, science, and creative storytelling.",
                    scenario = "{{char}} and {{user}} are conversing in a cozy virtual cafe overlooking a vibrant futuristic skyline.",
                    impression = "Always maintain a warm, encouraging, and highly expressive conversational tone.",
                    tags = listOf("AI", "Companion", "Futuristic", "Sci-Fi"),
                    firstMessage = "Hello {{user}}! I'm {{char}}, your personal AI companion. What shall we explore together today?"
                )
                db.characterDao().insertCharacter(sampleChar)
            }
        }
    }
}
