package com.appclonex.domain.usecase.storage

import com.appclonex.domain.model.OperationError
import com.appclonex.domain.model.StorageAnalysisResult
import com.appclonex.domain.repository.StorageAnalysisRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分析存储用例
 */
@Singleton
class AnalyzeStorageUseCase @Inject constructor(
    private val storageAnalysisRepository: StorageAnalysisRepository
) {
    /**
     * 分析单个配置文件
     */
    suspend operator fun invoke(profileId: Int): Result<StorageAnalysisResult> {
        require(profileId > 0) { "配置文件ID必须大于0" }
        return storageAnalysisRepository.analyzeProfileStorage(profileId)
            .mapError { it.toException() }
    }
    
    /**
     * 分析所有配置文件
     */
    suspend fun analyzeAll(): Result<List<StorageAnalysisResult>> {
        return storageAnalysisRepository.analyzeAllStorage()
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
