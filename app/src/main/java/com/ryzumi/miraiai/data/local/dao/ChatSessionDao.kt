package com.ryzumi.miraiai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryzumi.miraiai.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId ORDER BY updatedAt DESC")
    fun getSessionsForCharacter(characterId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    fun getSessionById(id: String): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionByIdSync(id: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions")
    suspend fun getAllSessionsSync(): List<ChatSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ChatSessionEntity>)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("UPDATE chat_sessions SET activeModelId = :modelId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateActiveModel(id: String, modelId: String, updatedAt: Long)

    @Query("UPDATE chat_sessions SET configId = :configId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSessionConfig(id: String, configId: String, updatedAt: Long)

    @Query("UPDATE chat_sessions SET personaId = :personaId, configId = :configId, title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSessionSettings(id: String, personaId: String, configId: String, title: String, updatedAt: Long)
}
