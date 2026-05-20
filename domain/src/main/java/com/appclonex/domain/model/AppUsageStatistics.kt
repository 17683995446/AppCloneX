package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用使用统计模型
 * 跟踪克隆应用的使用频率、时长、趋势
 *
 * App usage statistics model.
 * Tracks cloned app usage frequency, duration, and trends.
 */
@Serializable
data class AppUsageStatistics(
    /** 应用包名 / Application package name */
    val packageName: String = "",
    /** 应用名称 / Application display name */
    val appName: String = "",
    /** 用户空间ID / Profile user ID */
    val profileUserId: Int = 0,
    /** 总启动次数 / Total launch count */
    val totalLaunchCount: Int = 0,
    /** 总使用时长（毫秒）/ Total usage time in milliseconds */
    val totalUsageTimeMs: Long = 0,
    /** 最后启动时间戳 / Last launch timestamp */
    val lastLaunchTime: Long = 0,
    /** 最近一次使用时长（毫秒）/ Last usage duration in milliseconds */
    val lastUsageDurationMs: Long = 0,
    /** 平均使用时长（毫秒）/ Average usage duration in milliseconds */
    val averageUsageDurationMs: Long = 0,
    /** 每日使用明细，键为日期(yyyy-MM-dd) / Daily usage breakdown, keyed by date */
    val dailyUsage: Map<String, DailyUsage> = emptyMap(),
    /** 每周使用统计列表 / Weekly usage statistics list */
    val weeklyUsage: List<WeeklyUsage> = emptyList(),
    /** 每月使用统计列表 / Monthly usage statistics list */
    val monthlyUsage: List<MonthlyUsage> = emptyList(),
    /** 最近7天每日启动次数趋势 / Launch trend for the last 7 days */
    val launchTrend: List<Int> = emptyList()
)

/**
 * 每日使用明细
 *
 * Daily usage breakdown.
 */
@Serializable
data class DailyUsage(
    /** 日期，格式 yyyy-MM-dd / Date in yyyy-MM-dd format */
    val date: String = "",
    /** 当日启动次数 / Launch count for the day */
    val launchCount: Int = 0,
    /** 当日使用时长（毫秒）/ Usage time in milliseconds for the day */
    val usageTimeMs: Long = 0,
    /** 当日首次启动时间戳 / First launch timestamp of the day */
    val firstLaunchTime: Long = 0,
    /** 当日最后启动时间戳 / Last launch timestamp of the day */
    val lastLaunchTime: Long = 0
)

/**
 * 每周使用统计
 *
 * Weekly usage statistics.
 */
@Serializable
data class WeeklyUsage(
    /** 周起始日期，格式 yyyy-MM-dd / Week start date in yyyy-MM-dd format */
    val weekStart: String = "",
    /** 当周总启动次数 / Total launch count for the week */
    val totalLaunchCount: Int = 0,
    /** 当周总使用时长（毫秒）/ Total usage time in milliseconds for the week */
    val totalUsageTimeMs: Long = 0,
    /** 每日启动次数明细，键为星期几 / Daily launch breakdown, keyed by day of week */
    val dayBreakdown: Map<String, Int> = emptyMap()
)

/**
 * 每月使用统计
 *
 * Monthly usage statistics.
 */
@Serializable
data class MonthlyUsage(
    /** 月份，格式 yyyy-MM / Month in yyyy-MM format */
    val month: String = "",
    /** 当月总启动次数 / Total launch count for the month */
    val totalLaunchCount: Int = 0,
    /** 当月总使用时长（毫秒）/ Total usage time in milliseconds for the month */
    val totalUsageTimeMs: Long = 0,
    /** 当月活跃天数 / Number of active days in the month */
    val activeDays: Int = 0,
    /** 当月使用峰值日期 / Peak usage day in the month */
    val peakDay: String = ""
)

/**
 * 统计概览
 * 提供全局维度的使用统计汇总信息
 *
 * Usage overview.
 * Provides aggregated usage statistics at a global level.
 */
@Serializable
data class UsageOverview(
    /** 克隆应用总数 / Total number of cloned apps */
    val totalClonedApps: Int = 0,
    /** 近7天有使用的应用数 / Number of apps used in the last 7 days */
    val activeApps: Int = 0,
    /** 今日总启动次数 / Total launches today */
    val totalLaunchesToday: Int = 0,
    /** 本周总启动次数 / Total launches this week */
    val totalLaunchesThisWeek: Int = 0,
    /** 历史总启动次数 / Total launches all time */
    val totalLaunchesAllTime: Long = 0,
    /** 今日总使用时长（毫秒）/ Total usage time today in milliseconds */
    val totalUsageTimeToday: Long = 0,
    /** 最常使用的应用列表 / Most frequently used apps */
    val mostUsedApps: List<TopUsedApp> = emptyList(),
    /** 最近使用的应用列表 / Recently used apps */
    val recentlyUsedApps: List<RecentlyUsedApp> = emptyList(),
    /** 使用趋势数据 / Usage trend data */
    val usageTrend: UsageTrend = UsageTrend()
)

/**
 * 最常使用的应用
 *
 * Top used app entry.
 */
@Serializable
data class TopUsedApp(
    /** 应用包名 / Application package name */
    val packageName: String = "",
    /** 应用名称 / Application display name */
    val appName: String = "",
    /** 启动次数 / Launch count */
    val launchCount: Int = 0,
    /** 使用时长（毫秒）/ Usage time in milliseconds */
    val usageTimeMs: Long = 0,
    /** 排名 / Rank position */
    val rank: Int = 0
)

/**
 * 最近使用的应用
 *
 * Recently used app entry.
 */
@Serializable
data class RecentlyUsedApp(
    /** 应用包名 / Application package name */
    val packageName: String = "",
    /** 应用名称 / Application display name */
    val appName: String = "",
    /** 最后启动时间戳 / Last launch timestamp */
    val lastLaunchTime: Long = 0,
    /** 所属用户空间名称 / Profile name */
    val profileName: String = ""
)

/**
 * 使用趋势数据
 * 包含日维度、周维度和月维度的趋势指标
 *
 * Usage trend data.
 * Contains daily, weekly, and monthly trend indicators.
 */
@Serializable
data class UsageTrend(
    /** 最近30天每日启动次数 / Daily launch counts for the last 30 days */
    val dailyLaunches: List<Int> = emptyList(),
    /** 最近30天每日使用时长（分钟）/ Daily usage time in minutes for the last 30 days */
    val dailyUsageTime: List<Long> = emptyList(),
    /** 周环比增长率 / Week-over-week growth rate */
    val weeklyGrowthRate: Float = 0f,
    /** 月环比增长率 / Month-over-month growth rate */
    val monthlyGrowthRate: Float = 0f
)

/**
 * 使用统计时间范围
 *
 * Statistics period enum.
 */
enum class StatisticsPeriod {
    /** 今天 / Today */
    TODAY,
    /** 本周 / This week */
    THIS_WEEK,
    /** 本月 / This month */
    THIS_MONTH,
    /** 全部时间 / All time */
    ALL_TIME,
    /** 自定义范围 / Custom range */
    CUSTOM
}
