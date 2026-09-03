package com.ryzumi.miraiai.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.ryzumi.miraiai.data.datastore.SettingsRepository
import com.ryzumi.miraiai.data.local.MiraiDatabase
import com.ryzumi.miraiai.data.local.entity.ChatMessageEntity
import com.ryzumi.miraiai.data.network.OpenAiRepository
import com.ryzumi.miraiai.domain.engine.ChatGenerationManager
import com.ryzumi.miraiai.domain.util.ChatNotificationHelper
import com.ryzumi.miraiai.domain.util.TokenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationReplyReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPLY = "com.ryzumi.miraiai.ACTION_NOTIFICATION_REPLY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = remoteInput.getCharSequence(ChatNotificationHelper.KEY_TEXT_REPLY)?.toString()?.trim() ?: return
        val sessionId = intent.getStringExtra(ChatNotificationHelper.EXTRA_SESSION_ID) ?: return

        if (replyText.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = MiraiDatabase.getInstance(context)
                val session = database.chatSessionDao().getSessionByIdSync(sessionId) ?: return@launch
                val character = database.characterDao().getCharacterByIdSync(session.characterId) ?: return@launch
                val persona = (if (session.personaId.isNotBlank()) {
                    database.userPersonaDao().getPersonaByIdSync(session.personaId)
                } else null) ?: database.userPersonaDao().getDefaultPersonaSync() ?: database.userPersonaDao().getAllPersonasSync().firstOrNull()
                val config = database.inferenceConfigDao().getConfigByIdSync(session.configId)
                    ?: database.inferenceConfigDao().getActiveConfigSync()
                    ?: database.inferenceConfigDao().getAllConfigsSync().firstOrNull()
                    ?: return@launch

                val settingsRepo = SettingsRepository(context)
                val isShowThinking = try {
                    settingsRepo.showThinkingProcessFlow.first()
                } catch (e: Exception) {
                    true
                }

                val userTokens = TokenUtils.estimateTokenCount(replyText)
                val userMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    sender = "USER",
                    content = replyText,
                    tokensCount = userTokens
                )
                database.chatMessageDao().insertMessage(userMsg)

                // Show WhatsApp-style pending reply progress state on the notification
                ChatNotificationHelper.showReplyingNotification(
                    context = context,
                    sessionId = sessionId,
                    characterName = character.name.ifBlank { "AI Character" },
                    replyText = replyText,
                    characterAvatarUri = character.avatarUri,
                    userName = persona?.name?.ifBlank { "You" } ?: "You",
                    userAvatarUri = persona?.avatarUri
                )

                val openAiRepo = OpenAiRepository()
                ChatGenerationManager.startGeneration(
                    context = context,
                    sessionId = sessionId,
                    character = character,
                    persona = persona,
                    config = config,
                    hasImage = false,
                    openAiRepository = openAiRepo,
                    database = database,
                    isShowThinking = isShowThinking
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
