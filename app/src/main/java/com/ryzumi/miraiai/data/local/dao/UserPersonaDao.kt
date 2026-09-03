package com.ryzumi.miraiai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryzumi.miraiai.data.local.entity.UserPersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPersonaDao {
    @Query("SELECT * FROM user_personas")
    fun getAllPersonas(): Flow<List<UserPersonaEntity>>

    @Query("SELECT * FROM user_personas WHERE id = :id")
    fun getPersonaById(id: String): Flow<UserPersonaEntity?>

    @Query("SELECT * FROM user_personas WHERE id = :id")
    suspend fun getPersonaByIdSync(id: String): UserPersonaEntity?

    @Query("SELECT * FROM user_personas WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPersona(): Flow<UserPersonaEntity?>

    @Query("SELECT * FROM user_personas WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPersonaSync(): UserPersonaEntity?

    @Query("SELECT * FROM user_personas")
    suspend fun getAllPersonasSync(): List<UserPersonaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: UserPersonaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonas(personas: List<UserPersonaEntity>)

    @Update
    suspend fun updatePersona(persona: UserPersonaEntity)

    @Delete
    suspend fun deletePersona(persona: UserPersonaEntity)

    @Query("DELETE FROM user_personas WHERE id = :id")
    suspend fun deletePersonaById(id: String)

    @Query("UPDATE user_personas SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE user_personas SET isDefault = 1 WHERE id = :personaId")
    suspend fun setDefaultFlag(personaId: String)

    @Query("DELETE FROM user_personas")
    suspend fun deleteAllPersonas()
}
