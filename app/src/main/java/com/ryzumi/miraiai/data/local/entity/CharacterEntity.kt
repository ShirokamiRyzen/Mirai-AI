package com.ryzumi.miraiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarUri: String? = null,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val impression: String = "",
    val tags: List<String> = emptyList(),
    val firstMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
