package com.alpin.chat.data.repository

import com.alpin.chat.data.local.db.ModelConfigDao
import com.alpin.chat.domain.model.ModelConfig
import com.alpin.chat.domain.model.toDomain
import com.alpin.chat.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelConfigRepository @Inject constructor(
    private val modelConfigDao: ModelConfigDao
) {
    fun getAllModelConfigs(): Flow<List<ModelConfig>> =
        modelConfigDao.getAllModelConfigs().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getAllModelConfigsSync(): List<ModelConfig> =
        modelConfigDao.getAllModelConfigsSync().map { it.toDomain() }

    suspend fun getModelConfigById(id: String): ModelConfig? =
        modelConfigDao.getModelConfigById(id)?.toDomain()

    suspend fun getDefaultModelConfig(): ModelConfig? =
        modelConfigDao.getDefaultModelConfig()?.toDomain()

    fun getDefaultModelConfigFlow(): Flow<ModelConfig?> =
        modelConfigDao.getDefaultModelConfigFlow().map { it?.toDomain() }

    suspend fun insertModelConfig(modelConfig: ModelConfig) {
        modelConfigDao.insertModelConfig(modelConfig.toEntity())
    }

    suspend fun updateModelConfig(modelConfig: ModelConfig) {
        modelConfigDao.updateModelConfig(modelConfig.toEntity())
    }

    suspend fun deleteModelConfig(id: String) {
        modelConfigDao.deleteModelConfigById(id)
    }

    suspend fun setDefaultModel(id: String) {
        modelConfigDao.clearDefaultModel()
        modelConfigDao.setDefaultModel(id)
    }

    suspend fun clearDefaultModel() {
        modelConfigDao.clearDefaultModel()
    }
}
