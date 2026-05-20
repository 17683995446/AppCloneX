package com.appclonex.domain.repository

import com.appclonex.domain.model.StorageAnalysisResult
import com.appclonex.domain.model.CleanupSuggestion
import com.appclonex.domain.model.OperationResult
import kotlinx.coroutines.flow.Flow

/**
 * 存储分析仓库接口
 */
interface StorageAnalysisRepository {
    /**
     * 分析指定配置文件的存储使用情况
     */
    suspend fun analyzeProfileStorage(profileId: Int): OperationResult<StorageAnalysisResult>
    
    /**
     * 分析所有配置文件的存储使用情况
     */
    suspend fun analyzeAllStorage(): OperationResult<List<StorageAnalysisResult>>
    
    /**
     * 获取清理建议
     */
    suspend fun getCleanupSuggestions(profileId: Int): OperationResult<List<CleanupSuggestion>>
    
    /**
     * 执行清理
     * 
     * @param profileId 配置文件ID
     * @param types 要清理的类型
     * @return 清理的字节数
     */
    suspend fun performCleanup(
        profileId: Int,
        types: List<CleanupSuggestion.CleanupType>
    ): OperationResult<Long>
    
    /**
     * 获取存储使用趋势
     */
    fun getStorageTrend(): Flow<List<StorageAnalysisResult>>
}
