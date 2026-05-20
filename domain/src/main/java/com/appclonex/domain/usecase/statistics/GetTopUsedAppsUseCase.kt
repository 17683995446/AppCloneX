package com.appclonex.domain.usecase.statistics

import com.appclonex.domain.model.StatisticsPeriod
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.repository.UsageStatisticsRepository
import javax.inject.Inject

/**
 * 获取最常使用应用列表用例
 * 根据使用频率和使用时长，返回排名靠前的克隆应用列表
 *
 * Get top used apps use case.
 * Returns a ranked list of cloned apps based on usage frequency and duration.
 *
 * @property usageStatisticsRepository 使用统计仓库 / Usage statistics repository
 */
class GetTopUsedAppsUseCase @Inject constructor(
    private val usageStatisticsRepository: UsageStatisticsRepository
) {
    /**
     * 执行查询
     * Executes the query.
     *
     * @param limit 返回数量上限，默认10 / Maximum number of results, defaults to 10
     * @param period 统计时间范围，默认为全部时间 / Statistics period, defaults to ALL_TIME
     * @return 最常使用的应用列表（按排名排序）/ List of top used apps sorted by rank
     */
    suspend operator fun invoke(
        limit: Int = 10,
        period: StatisticsPeriod = StatisticsPeriod.ALL_TIME
    ): List<TopUsedApp> = usageStatisticsRepository.getTopUsedApps(limit, period)
}
