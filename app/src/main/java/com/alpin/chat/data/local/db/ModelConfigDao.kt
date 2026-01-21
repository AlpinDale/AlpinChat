package com.alpin.chat.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alpin.chat.data.local.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Query("SELECT * FROM model_configs ORDER BY displayName ASC")
    fun getAllModelConfigs(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs ORDER BY displayName ASC")
    suspend fun getAllModelConfigsSync(): List<ModelConfigEntity>

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getModelConfigById(id: String): ModelConfigEntity?

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultModelConfig(): ModelConfigEntity?

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    fun getDefaultModelConfigFlow(): Flow<ModelConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelConfig(modelConfig: ModelConfigEntity)

    @Update
    suspend fun updateModelConfig(modelConfig: ModelConfigEntity)

    @Delete
    suspend fun deleteModelConfig(modelConfig: ModelConfigEntity)

    @Query("DELETE FROM model_configs WHERE id = :id")
    suspend fun deleteModelConfigById(id: String)

    @Query("UPDATE model_configs SET isDefault = 0")
    suspend fun clearDefaultModel()

    @Query("UPDATE model_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultModel(id: String)
}
