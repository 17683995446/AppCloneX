package com.appclonex.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.workDataOf
import androidx.work.BackoffPolicy
import com.appclonex.domain.usecase.storage.CleanupStorageUseCase
import com.appclonex.domain.model.CleanupSuggestion
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 存储清理定时任务
 *
 * 定期清理缓存和临时文件
 * 建议每天执行一次
 *
 * ## R11-006 修复：Worker异常处理改进
 * - 区分可恢复错误和不可恢复错误
 * - 对可恢复错误返回Result.retry()，支持指数退避重试
 * - 对不可恢复错误返回Result.failure()，记录详细错误信息
 * - 添加最大重试次数限制
 */
@HiltWorker
class StorageCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cleanupStorageUseCase: CleanupStorageUseCase,
    private val userProfileRepository: UserProfileRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "StorageCleanupWorker"
        const val WORK_NAME = "storage_cleanup"
        private const val KEY_PROFILE_ID = "profile_id"
        
        // R11-006: 最大重试次数
        private const val MAX_RETRY_COUNT = 3
        
        // R11-006: 指数退避配置
        private const val BACKOFF_DELAY_MINUTES = 10L

        fun schedule(context: Context, profileId: Int? = null) {
            val inputData = profileId?.let {
                workDataOf(KEY_PROFILE_ID to it)
            } ?: workDataOf()

            // R11-006: 添加指数退避策略
            val workRequest = PeriodicWorkRequestBuilder<StorageCleanupWorker>(
                24, TimeUnit.HOURS
            )
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            ApplicationLoggingServiceManager.i(TAG, "开始执行存储清理 (attempt=${runAttemptCount + 1})")

            val profileId = inputData.getInt(KEY_PROFILE_ID, -1)
            
            return@withContext try {
                val totalBytesCleaned = if (profileId >= 0) {
                    // Clean specific profile
                    cleanProfile(profileId)
                } else {
                    // Clean all profiles
                    cleanAllProfiles()
                }
                
                ApplicationLoggingServiceManager.i(
                    TAG, 
                    "存储清理完成，共释放 ${totalBytesCleaned / 1024 / 1024} MB"
                )
                
                // R11-006: 成功时返回Result.success()
                Result.success(
                    workDataOf(
                        "bytes_cleaned" to totalBytesCleaned,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } catch (e: SecurityException) {
                // R11-006: 权限错误通常不会自行恢复，返回failure
                ApplicationLoggingServiceManager.e(TAG, "存储清理权限错误", e)
                Result.failure(
                    workDataOf(
                        "error_type" to "SECURITY",
                        "error_message" to (e.message ?: "权限错误")
                    )
                )
            } catch (e: OutOfMemoryError) {
                // R11-006: 内存不足可能是临时问题，尝试重试
                ApplicationLoggingServiceManager.e(TAG, "存储清理内存不足", e)
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(
                            "error_type" to "OUT_OF_MEMORY",
                            "error_message" to "内存不足，已达最大重试次数"
                        )
                    )
                }
            } catch (e: Exception) {
                // R11-006: 其他异常根据重试次数决定
                ApplicationLoggingServiceManager.e(TAG, "存储清理异常", e)
                
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    // 还有重试次数，返回retry
                    ApplicationLoggingServiceManager.i(TAG, "将在退避后重试 (${runAttemptCount + 1}/$MAX_RETRY_COUNT)")
                    Result.retry()
                } else {
                    // 已达最大重试次数，返回failure
                    ApplicationLoggingServiceManager.w(TAG, "已达最大重试次数，放弃重试")
                    Result.failure(
                        workDataOf(
                            "error_type" to "EXCEPTION",
                            "error_message" to (e.message ?: "未知错误"),
                            "retry_count" to runAttemptCount
                        )
                    )
                }
            }
        }
    }
    
    private suspend fun cleanProfile(profileId: Int): Long {
        var totalBytes = 0L
        val result = cleanupStorageUseCase(
            profileId = profileId,
            types = listOf(
                CleanupSuggestion.CleanupType.CACHE,
                CleanupSuggestion.CleanupType.TEMP_FILES,
                CleanupSuggestion.CleanupType.LOG_FILES
            )
        )
        result.onSuccess { bytes ->
            totalBytes += bytes
        }.onFailure { error ->
            // 记录单个配置文件的清理错误但不中断整体流程
            ApplicationLoggingServiceManager.w(TAG, "配置文件 $profileId 清理部分失败", error)
        }
        return totalBytes
    }
    
    private suspend fun cleanAllProfiles(): Long {
        var totalBytes = 0L
        try {
            val profiles = userProfileRepository.getAllProfilesWithInfo()
            for (profile in profiles) {
                totalBytes += cleanProfile(profile.profile.userId)
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "获取配置文件列表失败", e)
            // 继续清理默认配置
            totalBytes += cleanProfile(0)
        }
        return totalBytes
    }
}
