package com.ryzumi.miraiai.domain.model

import com.ryzumi.miraiai.data.datastore.ThemeSettings
import com.ryzumi.miraiai.data.local.entity.CharacterEntity
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.local.entity.ChatSessionEntity
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity

data class MiraiBackupData(
    val version: Int = 2,
    val appName: String = "MiraiAI",
    val exportedAt: Long = System.currentTimeMillis(),
    val characters: List<CharacterEntity> = emptyList(),
    val personas: List<UserPersonaEntity> = emptyList(),
    val sessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val configs: List<InferenceConfigEntity> = emptyList(),
    val themeSettings: ThemeSettings? = null,
    val showThinkingProcess: Boolean? = null,
    val debugLoggingEnabled: Boolean? = null,
    val characterAvatars: Map<String, String>? = null,
    val personaAvatars: Map<String, String>? = null
)

data class BackupStats(
    val characterCount: Int = 0,
    val personaCount: Int = 0,
    val sessionCount: Int = 0,
    val messageCount: Int = 0,
    val configCount: Int = 0,
    val assetCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val formattedDataSize: String = "0 B"
)
