package com.appclonex.domain.usecase.statistics

import com.appclonex.domain.model.StatisticsPeriod
import com.appclonex.domain.model.UsageOverview
import com.appclonex.domain.repository.UsageStatisticsRepository
import javax.inject.Inject

/**
 * 获取使用统计概览用例
 * 从统计仓库中获取全局维度的使用统计汇总信息，支持按时间范围筛选
 *
 * Get usage overview use case.
 * Retrieves global usage statistics summary from the repository,
 * with support for filtering by time period.
 *
 * @property usageStatisticsRepository 使用统计仓库 / Usage statistics repository
 */
class GetUsageOverviewUseCase @Inject constructor(
    private val usageStatisticsRepository: UsageStatisticsRepository
) {
    /**
     * 执行概览查询
     * Executes the overview query.
     *
     * @param period 统计时间范围，默认为全部时间 / Statistics period, defaults to ALL_TIME
     * @return 使用统计概览 / Usage overview
     */
    suspend operator fun invoke(
        period: StatisticsPeriod = StatisticsPeriod.ALL_TIME
    ): UsageOverview = usageStatisticsRepository.getOverview(period)
}
