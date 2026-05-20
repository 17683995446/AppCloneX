package com.appclonex.domain.usecase.storage

import com.appclonex.domain.model.CleanupSuggestion
import com.appclonex.domain.model.OperationError
import com.appclonex.domain.repository.StorageAnalysisRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 清理存储用例
 */
@Singleton
class CleanupStorageUseCase @Inject constructor(
    private val storageAnalysisRepository: StorageAnalysisRepository
) {
    /**
     * 获取清理建议
     */
    suspend fun getSuggestions(profileId: Int): Result<List<CleanupSuggestion>> {
        return storageAnalysisRepository.getCleanupSuggestions(profileId)
            .mapError { it.toException() }
    }
    
    /**
     * 执行清理
     * 
     * @param profileId 配置文件ID
     * @param types 要清理的类型
     * @return 清理的字节数
     */
    suspend operator fun invoke(
        profileId: Int,
        types: List<CleanupSuggestion.CleanupType>
    ): Result<Long> {
        require(profileId > 0) { "配置文件ID必须大于0" }
        require(types.isNotEmpty()) { "至少选择一种清理类型" }
        
        return storageAnalysisRepository.performCleanup(profileId, types)
            .mapError { it.toException() }
    }
    
    /**
     * 将 OperationError 转换为 Exception
     */
    private fun OperationError.toException(): Exception {
        return when (this) {
            is OperationError.ShizukuNotAvailable -> Exception(message)
            is OperationError.PermissionDenied -> Exception(message)
            is OperationError.ApplicationNotFound -> Exception("配置文件不存在: $packageName")
            is OperationError.CloneFailed -> Exception(reason)
            is OperationError.FreezeFailed -> Exception(reason)
            is OperationError.SandboxError -> Exception(reason)
            is OperationError.DatabaseError -> Exception(reason)
            is OperationError.NetworkError -> Exception(reason)
            is OperationError.UnknownError -> Exception(throwable)
        }
    }
}
