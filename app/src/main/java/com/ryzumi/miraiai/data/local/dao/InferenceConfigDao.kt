package com.ryzumi.miraiai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryzumi.miraiai.data.local.entity.InferenceConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InferenceConfigDao {
    @Query("SELECT * FROM inference_configs ORDER BY name ASC")
    fun getAllConfigs(): Flow<List<InferenceConfigEntity>>

    @Query("SELECT * FROM inference_configs WHERE id = :id")
    fun getConfigById(id: String): Flow<InferenceConfigEntity?>

    @Query("SELECT * FROM inference_configs WHERE id = :id")
    suspend fun getConfigByIdSync(id: String): InferenceConfigEntity?

    @Query("SELECT * FROM inference_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveConfig(): Flow<InferenceConfigEntity?>

    @Query("SELECT * FROM inference_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfigSync(): InferenceConfigEntity?

    @Query("SELECT * FROM inference_configs")
    suspend fun getAllConfigsSync(): List<InferenceConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: InferenceConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<InferenceConfigEntity>)

    @Update
    suspend fun updateConfig(config: InferenceConfigEntity)

    @Delete
    suspend fun deleteConfig(config: InferenceConfigEntity)

    @Query("UPDATE inference_configs SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE inference_configs SET isActive = 1 WHERE id = :configId")
    suspend fun setActiveFlag(configId: String)

    @androidx.room.Transaction
    suspend fun switchActiveProfile(configId: String) {
        clearActiveFlags()
        setActiveFlag(configId)
    }

    @Query("DELETE FROM inference_configs")
    suspend fun deleteAllConfigs()
}
