package com.appclonex.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.BackoffPolicy
import com.appclonex.domain.usecase.configuration.ExportConfigurationUseCase
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 配置自动备份Worker
 *
 * 定期自动备份用户配置到本地文件
 *
 * ## R11-006 修复：Worker异常处理改进
 * - 区分可恢复错误（IO异常）和不可恢复错误
 * - 对可恢复错误返回Result.retry()，支持指数退避重试
 * - 对不可恢复错误返回Result.failure()，记录详细错误信息
 * - 添加最大重试次数限制
 */
@HiltWorker
class ConfigurationAutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val exportConfigurationUseCase: ExportConfigurationUseCase
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ConfigAutoBackupWorker"
        const val WORK_NAME = "config_auto_backup"
        private const val MAX_BACKUP_FILES = 5
        
        // R11-006: 最大重试次数
        private const val MAX_RETRY_COUNT = 3
        
        // R11-006: 指数退避配置
        private const val BACKOFF_DELAY_MINUTES = 15L

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ConfigurationAutoBackupWorker>(
                24, TimeUnit.HOURS
            )
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
            try {
                ApplicationLoggingServiceManager.i(TAG, "开始自动备份配置 (attempt=${runAttemptCount + 1})")

                val backupDir = File(applicationContext.filesDir, "backups")
                if (!backupDir.exists()) backupDir.mkdirs()

                // Clean old backups (keep only MAX_BACKUP_FILES)
                val existingBackups = backupDir.listFiles()
                    ?.filter { it.extension == "json" }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

                if (existingBackups.size >= MAX_BACKUP_FILES) {
                    existingBackups.drop(MAX_BACKUP_FILES - 1).forEach { it.delete() }
                }

                // Export current configuration
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "auto_backup_$timestamp.json")

                // R9-EDGE-005: 遍历所有可用配置文件进行备份
                val profileIds = try {
                    exportConfigurationUseCase.getAvailableProfileIds()
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.w(TAG, "获取配置文件列表失败，尝试默认导出", e)
                    listOf(1)
                }

                var backupSuccess = false
                var lastError: Throwable? = null
                
                for (profileId in profileIds) {
                    val result = exportConfigurationUseCase(profileId)
                    result.onSuccess { config ->
                        val json = kotlinx.serialization.json.Json.encodeToString(
                            com.appclonex.domain.model.ProfileConfiguration.serializer(), config
                        )
                        // R9-EDGE-003: 使用 AtomicFileWriter 进行原子写入
                        AtomicFileWriter.write(backupFile, json)
                        ApplicationLoggingServiceManager.i(TAG, "自动备份成功: ${backupFile.name} (profileId=$profileId)")
                        backupSuccess = true
                    }.onFailure { e ->
                        ApplicationLoggingServiceManager.e(TAG, "自动备份失败 (profileId=$profileId)", e)
                        lastError = e
                    }
                }

                return@withContext when {
                    backupSuccess -> {
                        Result.success(
                            workDataOf(
                                "backup_file" to backupFile.absolutePath,
                                "timestamp" to timestamp,
                                "profiles_backed_up" to profileIds.size
                            )
                        )
                    }
                    runAttemptCount < MAX_RETRY_COUNT -> {
                        // R11-006: 有重试次数时返回retry
                        ApplicationLoggingServiceManager.w(TAG, "备份失败，将重试 (${runAttemptCount + 1}/$MAX_RETRY_COUNT)")
                        Result.retry()
                    }
                    else -> {
                        // R11-006: 无重试次数时返回failure
                        ApplicationLoggingServiceManager.e(TAG, "备份失败，已达最大重试次数")
                        Result.failure(
                            workDataOf(
                                "error_type" to "BACKUP_FAILED",
                                "error_message" to (lastError?.message ?: "所有配置文件备份均失败"),
                                "retry_count" to runAttemptCount
                            )
                        )
                    }
                }
            } catch (e: IOException) {
                // R11-006: IO错误通常是可恢复的（存储空间不足、文件被占用等）
                ApplicationLoggingServiceManager.e(TAG, "自动备份 IO 异常", e)
                
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(
                            "error_type" to "IO_EXCEPTION",
                            "error_message" to (e.message ?: "IO错误"),
                            "retry_count" to runAttemptCount
                        )
                    )
                }
            } catch (e: SecurityException) {
                // R11-006: 权限错误通常不会自行恢复
                ApplicationLoggingServiceManager.e(TAG, "自动备份权限错误", e)
                Result.failure(
                    workDataOf(
                        "error_type" to "SECURITY",
                        "error_message" to (e.message ?: "权限错误")
                    )
                )
            } catch (e: OutOfMemoryError) {
                // R11-006: 内存不足可能是临时问题
                ApplicationLoggingServiceManager.e(TAG, "自动备份内存不足", e)
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
                // R11-006: 其他异常
                ApplicationLoggingServiceManager.e(TAG, "自动备份异常", e)
                
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
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
}
