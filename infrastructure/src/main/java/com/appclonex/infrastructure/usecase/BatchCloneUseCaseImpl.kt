package com.appclonex.infrastructure.usecase

import com.appclonex.domain.model.BatchCloneConfig
import com.appclonex.domain.model.BatchCloneProgress
import com.appclonex.domain.model.CloneStatus
import com.appclonex.domain.model.SingleCloneResult
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.domain.usecase.clone.BatchCloneUseCase
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量克隆用例实现
 *
 * 支持逐个克隆应用到指定配置文件，实时报告进度。
 * 可配置跳过已克隆应用、克隆后自动冻结等选项。
 *
 * ## R12-002 修复：异常处理改进
 * - 使用 getOrNull() 替代 getOrThrow() 避免异常崩溃
 * - 添加 skipped 计数器，区分成功、失败、跳过
 * - 自动冻结失败记录到结果中，不再静默吞没
 */
@Singleton
class BatchCloneUseCaseImpl @Inject constructor(
    private val clonedApplicationRepository: ClonedApplicationRepository,
    private val freezingService: FreezingService
) : BatchCloneUseCase {

    companion object {
        private const val TAG = "BatchCloneUseCase"
    }

    override fun invoke(config: BatchCloneConfig): Flow<BatchCloneProgress> = flow {
        val packages = config.selectedPackages
        if (packages.isEmpty()) {
            emit(
                BatchCloneProgress(
                    currentStatus = CloneStatus.COMPLETED,
                    startTime = System.currentTimeMillis()
                )
            )
            return@flow
        }

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<SingleCloneResult>()
        var completedCount = 0
        var failedCount = 0
        var skippedCount = 0  // R12-002: 新增跳过计数器

        // 发送准备阶段进度
        emit(
            BatchCloneProgress(
                totalApps = packages.size,
                currentStatus = CloneStatus.PREPARING,
                startTime = startTime
            )
        )

        for (packageName in packages) {
            // 检查是否已取消（通过检查 flow 的 isActive）
            val appStartTime = System.currentTimeMillis()

            // 发送当前正在克隆的应用进度
            emit(
                BatchCloneProgress(
                    totalApps = packages.size,
                    completedApps = completedCount,
                    failedApps = failedCount,
                    currentApp = packageName,
                    currentStatus = CloneStatus.CLONING,
                    results = results.toList(),
                    startTime = startTime
                )
            )

            val cloneResult = clonedApplicationRepository.cloneApp(
                packageName,
                config.targetProfileId
            )

            val durationMs = System.currentTimeMillis() - appStartTime

            // R12-002 修复：使用 getOrNull() 替代 getOrThrow()
            if (cloneResult.isSuccess) {
                val clonedApp = cloneResult.getOrNull()

                if (clonedApp != null) {
                    completedCount++

                    // R12-002 修复：自动冻结失败不再静默吞没
                    var autoFreezeSuccess = true
                    var autoFreezeError: String? = null

                    if (config.options.autoFreeze) {
                        try {
                            freezingService.freezeApp(packageName, config.targetProfileId)
                        } catch (e: Exception) {
                            autoFreezeSuccess = false
                            autoFreezeError = e.message
                            ApplicationLoggingServiceManager.w(
                                TAG,
                                "自动冻结失败: $packageName",
                                e
                            )
                        }
                    }

                    results.add(
                        SingleCloneResult(
                            packageName = packageName,
                            appName = clonedApp.appName,
                            success = true,
                            durationMs = durationMs,
                            autoFreezeSuccess = autoFreezeSuccess,
                            autoFreezeError = autoFreezeError
                        )
                    )
                } else {
                    // R12-002 修复：处理成功但返回null的异常情况
                    failedCount++
                    results.add(
                        SingleCloneResult(
                            packageName = packageName,
                            appName = packageName,
                            success = false,
                            errorMessage = "克隆成功但无法获取应用信息",
                            durationMs = durationMs
                        )
                    )
                }
            } else {
                val errorMessage = cloneResult.exceptionOrNull()?.message ?: "未知错误"

                // R12-002 修复：跳过已克隆应用时使用单独的计数器
                if (config.options.skipExisting &&
                    errorMessage.contains("已克隆", ignoreCase = true)) {
                    skippedCount++
                    results.add(
                        SingleCloneResult(
                            packageName = packageName,
                            appName = packageName,
                            success = true,
                            skipped = true,  // 新增字段标识跳过
                            durationMs = durationMs
                        )
                    )
                } else {
                    failedCount++
                    results.add(
                        SingleCloneResult(
                            packageName = packageName,
                            appName = packageName,
                            success = false,
                            errorMessage = errorMessage,
                            durationMs = durationMs
                        )
                    )
                }
            }

            // 发送更新后的进度
            val elapsed = System.currentTimeMillis() - startTime
            val processedCount = completedCount + skippedCount + failedCount
            val avgTimePerApp = if (processedCount > 0) {
                elapsed / processedCount
            } else {
                0L
            }
            val remainingApps = packages.size - processedCount
            val estimatedTimeRemaining = avgTimePerApp * remainingApps

            emit(
                BatchCloneProgress(
                    totalApps = packages.size,
                    completedApps = completedCount,
                    failedApps = failedCount,
                    skippedApps = skippedCount,  // R12-002: 包含跳过计数
                    currentApp = if (processedCount < packages.size) {
                        packages[processedCount]
                    } else "",
                    currentStatus = CloneStatus.CLONING,
                    results = results.toList(),
                    startTime = startTime,
                    estimatedTimeRemaining = estimatedTimeRemaining
                )
            )
        }

        // 发送完成进度
        emit(
            BatchCloneProgress(
                totalApps = packages.size,
                completedApps = completedCount,
                failedApps = failedCount,
                skippedApps = skippedCount,
                currentStatus = if (failedCount == 0) CloneStatus.COMPLETED else CloneStatus.COMPLETED,
                results = results.toList(),
                startTime = startTime
            )
        )

        ApplicationLoggingServiceManager.i(
            TAG,
            "批量克隆完成: 总计 ${packages.size}, 成功 $completedCount, 跳过 $skippedCount, 失败 $failedCount"
        )
    }.flowOn(Dispatchers.IO)
}
