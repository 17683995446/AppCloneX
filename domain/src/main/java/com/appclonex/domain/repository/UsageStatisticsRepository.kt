package com.appclonex.domain.repository

import com.appclonex.domain.model.AppUsageStatistics
import com.appclonex.domain.model.RecentlyUsedApp
import com.appclonex.domain.model.StatisticsPeriod
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.model.UsageOverview
import com.appclonex.domain.model.UsageTrend
import kotlinx.coroutines.flow.Flow

/**
 * 使用统计仓库接口
 * 提供应用使用统计数据的记录、查询、趋势分析及管理操作
 *
 * Usage statistics repository interface.
 * Provides recording, querying, trend analysis, and management operations
 * for app usage statistics.
 */
interface UsageStatisticsRepository {

    // ==================== 记录使用事件 / Record Usage Events ====================

    /**
     * 记录应用启动事件
     * Records an app launch event.
     *
     * @param packageName 应用包名 / Application package name
     * @param profileUserId 用户空间ID / Profile user ID
     */
    suspend fun recordLaunch(packageName: String, profileUserId: Int)

    /**
     * 记录应用使用结束事件
     * Records an app usage end event.
     *
     * @param packageName 应用包名 / Application package name
     * @param profileUserId 用户空间ID / Profile user ID
     * @param durationMs 本次使用时长（毫秒）/ Usage duration in milliseconds
     */
    suspend fun recordUsageEnd(packageName: String, profileUserId: Int, durationMs: Long)

    // ==================== 查询统计 / Query Statistics ====================

    /**
     * 获取指定应用的详细使用统计
     * Gets detailed usage statistics for a specific app.
     *
     * @param packageName 应用包名 / Application package name
     * @param profileUserId 用户空间ID / Profile user ID
     * @return 应用使用统计，不存在时返回null / Usage statistics, or null if not found
     */
    suspend fun getStatistics(packageName: String, profileUserId: Int): AppUsageStatistics?

    /**
     * 获取所有应用的使用统计（Flow实时更新）
     * Gets usage statistics for all apps as a reactive Flow.
     *
     * @return 所有应用的使用统计列表 / List of all app usage statistics
     */
    fun getAllStatistics(): Flow<List<AppUsageStatistics>>

    /**
     * 获取使用统计概览
     * Gets the usage overview summary.
     *
     * @param period 统计时间范围 / Statistics period
     * @return 使用统计概览 / Usage overview
     */
    suspend fun getOverview(period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): UsageOverview

    /**
     * 获取最常使用的应用列表
     * Gets the list of most frequently used apps.
     *
     * @param limit 返回数量上限 / Maximum number of results
     * @param period 统计时间范围 / Statistics period
     * @return 最常使用的应用列表 / List of top used apps
     */
    suspend fun getTopUsedApps(
        limit: Int = 10,
        period: StatisticsPeriod = StatisticsPeriod.ALL_TIME
    ): List<TopUsedApp>

    /**
     * 获取最近使用的应用列表
     * Gets the list of recently used apps.
     *
     * @param limit 返回数量上限 / Maximum number of results
     * @return 最近使用的应用列表 / List of recently used apps
     */
    suspend fun getRecentlyUsedApps(limit: Int = 10): List<RecentlyUsedApp>

    // ==================== 趋势分析 / Trend Analysis ====================

    /**
     * 获取使用趋势数据
     * Gets usage trend data.
     *
     * @param days 统计天数 / Number of days to analyze
     * @return 使用趋势 / Usage trend
     */
    suspend fun getUsageTrend(days: Int = 30): UsageTrend

    /**
     * 获取每日启动次数列表
     * Gets the list of daily launch counts.
     *
     * @param days 统计天数 / Number of days to analyze
     * @return 每日启动次数列表 / List of daily launch counts
     */
    suspend fun getDailyLaunches(days: Int = 30): List<Int>

    // ==================== 管理 / Management ====================

    /**
     * 清除所有使用统计数据
     * Clears all usage statistics data.
     */
    suspend fun clearStatistics()

    /**
     * 清除指定应用的使用统计数据
     * Clears usage statistics for a specific app.
     *
     * @param packageName 应用包名 / Application package name
     * @param profileUserId 用户空间ID / Profile user ID
     */
    suspend fun clearStatisticsForApp(packageName: String, profileUserId: Int)

    /**
     * 导出使用统计数据为JSON字符串
     * Exports usage statistics data as a JSON string.
     *
     * @return JSON格式的统计数据 / Statistics data in JSON format
     */
    suspend fun exportStatistics(): String
}
