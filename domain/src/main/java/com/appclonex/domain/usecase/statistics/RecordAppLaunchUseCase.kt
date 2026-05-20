package com.appclonex.domain.usecase.statistics

import com.appclonex.domain.repository.UsageStatisticsRepository
import javax.inject.Inject

/**
 * 记录应用启动用例
 * 当用户启动克隆应用时调用，将启动事件持久化到统计仓库
 *
 * Record app launch use case.
 * Called when a user launches a cloned app, persisting the launch event
 * to the statistics repository.
 *
 * @property usageStatisticsRepository 使用统计仓库 / Usage statistics repository
 */
class RecordAppLaunchUseCase @Inject constructor(
    private val usageStatisticsRepository: UsageStatisticsRepository
) {
    /**
     * 执行启动事件记录
     * Executes the launch event recording.
     *
     * @param packageName 应用包名 / Application package name
     * @param profileUserId 用户空间ID / Profile user ID
     */
    suspend operator fun invoke(packageName: String, profileUserId: Int) {
        usageStatisticsRepository.recordLaunch(packageName, profileUserId)
    }
}
