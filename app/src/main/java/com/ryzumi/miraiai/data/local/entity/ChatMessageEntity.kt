package com.ryzumi.miraiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sender: String, // "USER", "CHARACTER", "SYSTEM"
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensCount: Int = 0,
    val generationSpeedTps: Double = 0.0,
    val modelName: String? = null
)
