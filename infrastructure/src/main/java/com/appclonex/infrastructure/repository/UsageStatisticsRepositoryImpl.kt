package com.appclonex.infrastructure.repository

import android.content.Context
import com.appclonex.domain.model.AppUsageStatistics
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.DailyUsage
import com.appclonex.domain.model.RecentlyUsedApp
import com.appclonex.domain.model.StatisticsPeriod
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.model.UsageOverview
import com.appclonex.domain.model.UsageTrend
import com.appclonex.domain.model.UserProfileConfigurationModel
import com.appclonex.domain.repository.UsageStatisticsRepository
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.UserProfileEntityDataAccessObject
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 使用统计仓库实现 - AppCloneX V22
 *
 * 基于 JSON 文件持久化的使用统计数据管理。
 * 以 "${packageName}_${profileUserId}" 为键存储每个克隆应用的统计数据，
 * 提供启动记录、使用时长追踪、趋势分析等完整功能。
 *
 * @property clonedAppDao 克隆应用数据访问对象，用于获取应用信息
 * @property userProfileDao 用户配置文件数据访问对象，用于获取用户空间信息
 * @property context Android 应用上下文，用于文件存储
 */
@Singleton
class UsageStatisticsRepositoryImpl @Inject constructor(
    private val clonedAppDao: ClonedApplicationDataAccessObject,
    private val userProfileDao: UserProfileEntityDataAccessObject,
    @ApplicationContext private val context: Context
) : UsageStatisticsRepository {

    companion object {
        private const val STATISTICS_FILE_NAME = "usage_stats.json"
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd"
        private const val MONTH_FORMAT_PATTERN = "yyyy-MM"
        private const val WEEK_START_FORMAT = "yyyy-MM-dd"
        private const val MILLISECONDS_PER_MINUTE = 60_000L
        private const val DEFAULT_TREND_DAYS = 7
        private const val DEFAULT_TOP_LIMIT = 10
        private const val DEFAULT_RECENT_LIMIT = 10
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US)
    private val monthFormat = SimpleDateFormat(MONTH_FORMAT_PATTERN, Locale.US)

    /** 内存缓存：键为 "${packageName}_${profileUserId}"，值为统计数据 */
    private val statisticsCache = MutableStateFlow<Map<String, AppUsageStatistics>>(emptyMap())

    /** 统计数据文件 */
    private val statisticsFile: File
        get() = File(context.filesDir, STATISTICS_FILE_NAME)

    // ==================== 初始化 / Initialization ====================

    /**
     * 初始化时从文件加载缓存数据
     */
    init {
        loadStatisticsFromFile()
    }

    /**
     * 从 JSON 文件加载统计数据到内存缓存
     */
    @Synchronized
    private fun loadStatisticsFromFile() {
        try {
            if (statisticsFile.exists()) {
                val content = statisticsFile.readText()
                if (content.isNotBlank()) {
                    val serializedMap = json.decodeFromString<SerializedStatisticsMap>(content)
                    statisticsCache.value = serializedMap.statistics
                }
            }
        } catch (e: Exception) {
            // 文件损坏时静默处理，从空缓存开始
            statisticsCache.value = emptyMap()
        }
    }

    /**
     * 将内存缓存持久化到 JSON 文件
     */
    @Synchronized
    private fun saveStatisticsToFile() {
        try {
            val serialized = SerializedStatisticsMap(statisticsCache.value)
            val content = json.encodeToString(serialized)
            AtomicFileWriter.write(statisticsFile, content)
        } catch (e: Exception) {
            // 写入失败时静默处理，数据仍保留在内存中
        }
    }

    /**
     * 生成统计数据的唯一键
     */
    private fun buildKey(packageName: String, profileUserId: Int): String {
        return "${packageName}_$profileUserId"
    }

    /**
     * 获取今天的日期字符串 (yyyy-MM-dd)
     */
    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * 获取指定时间戳的日期字符串
     */
    private fun getDateForTimestamp(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 获取指定天数前的日期字符串
     */
    private fun getDateDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取本周一的日期字符串
     */
    private fun getWeekStartDate(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取本月第一天的日期字符串
     */
    private fun getMonthStartDate(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取本周的起始日期（周一）
     */
    private fun getThisWeekStart(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取上周的起始日期（周一）
     */
    private fun getLastWeekStart(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, -1)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 获取上个月的起始日期
     */
    private fun getLastMonthStart(): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 判断日期是否在指定时间范围内
     */
    private fun isDateInRange(date: String, startDate: String): Boolean {
        return date >= startDate
    }

    // ==================== 记录使用事件 / Record Usage Events ====================

    /**
     * 记录应用启动事件
     *
     * 查找或创建统计数据条目，递增 totalLaunchCount，
     * 更新 lastLaunchTime，添加到当天的 DailyUsage，更新 launchTrend。
     */
    override suspend fun recordLaunch(packageName: String, profileUserId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val key = buildKey(packageName, profileUserId)
                val now = System.currentTimeMillis()
                val today = getTodayDate()

                // 获取应用名称
                val appName = getAppName(packageName, profileUserId)

                val currentStats = statisticsCache.value[key]
                val updatedStats = if (currentStats != null) {
                    // 更新现有统计
                    val updatedDailyUsage = currentStats.dailyUsage.toMutableMap()
                    val todayUsage = updatedDailyUsage[today]
                    updatedDailyUsage[today] = if (todayUsage != null) {
                        todayUsage.copy(
                            launchCount = todayUsage.launchCount + 1,
                            lastLaunchTime = now,
                            firstLaunchTime = if (todayUsage.firstLaunchTime == 0L) now else todayUsage.firstLaunchTime
                        )
                    } else {
                        DailyUsage(
                            date = today,
                            launchCount = 1,
                            usageTimeMs = 0,
                            firstLaunchTime = now,
                            lastLaunchTime = now
                        )
                    }

                    // 更新 launchTrend：保留最近7天的每日启动次数
                    val updatedLaunchTrend = updateLaunchTrend(currentStats.launchTrend, today)

                    currentStats.copy(
                        totalLaunchCount = currentStats.totalLaunchCount + 1,
                        lastLaunchTime = now,
                        dailyUsage = updatedDailyUsage,
                        launchTrend = updatedLaunchTrend,
                        appName = if (appName.isNotEmpty()) appName else currentStats.appName
                    )
                } else {
                    // 创建新统计
                    val dailyUsage = mapOf(
                        today to DailyUsage(
                            date = today,
                            launchCount = 1,
                            usageTimeMs = 0,
                            firstLaunchTime = now,
                            lastLaunchTime = now
                        )
                    )

                    AppUsageStatistics(
                        packageName = packageName,
                        appName = appName,
                        profileUserId = profileUserId,
                        totalLaunchCount = 1,
                        totalUsageTimeMs = 0,
                        lastLaunchTime = now,
                        lastUsageDurationMs = 0,
                        averageUsageDurationMs = 0,
                        dailyUsage = dailyUsage,
                        weeklyUsage = emptyList(),
                        monthlyUsage = emptyList(),
                        launchTrend = listOf(1)
                    )
                }

                // 更新缓存并持久化
                val updatedCache = statisticsCache.value.toMutableMap()
                updatedCache[key] = updatedStats
                statisticsCache.value = updatedCache
                saveStatisticsToFile()
            } catch (e: Exception) {
                // 记录失败时静默处理，不影响用户体验
            }
        }
    }

    /**
     * 记录应用使用结束事件
     *
     * 更新 lastUsageDurationMs、totalUsageTimeMs、averageUsageDurationMs，
     * 更新 DailyUsage 中的 usageTimeMs。
     */
    override suspend fun recordUsageEnd(packageName: String, profileUserId: Int, durationMs: Long) {
        withContext(Dispatchers.IO) {
            try {
                if (durationMs <= 0) return@withContext

                val key = buildKey(packageName, profileUserId)
                val today = getTodayDate()

                val currentStats = statisticsCache.value[key] ?: return@withContext

                // 更新每日使用时长
                val updatedDailyUsage = currentStats.dailyUsage.toMutableMap()
                val todayUsage = updatedDailyUsage[today]
                updatedDailyUsage[today] = if (todayUsage != null) {
                    todayUsage.copy(usageTimeMs = todayUsage.usageTimeMs + durationMs)
                } else {
                    DailyUsage(
                        date = today,
                        launchCount = 0,
                        usageTimeMs = durationMs,
                        firstLaunchTime = 0,
                        lastLaunchTime = 0
                    )
                }

                // 计算新的平均使用时长
                val newTotalUsageTimeMs = currentStats.totalUsageTimeMs + durationMs
                val newAverageUsageDurationMs = if (currentStats.totalLaunchCount > 0) {
                    newTotalUsageTimeMs / currentStats.totalLaunchCount
                } else {
                    durationMs
                }

                val updatedStats = currentStats.copy(
                    lastUsageDurationMs = durationMs,
                    totalUsageTimeMs = newTotalUsageTimeMs,
                    averageUsageDurationMs = newAverageUsageDurationMs,
                    dailyUsage = updatedDailyUsage
                )

                // 更新缓存并持久化
                val updatedCache = statisticsCache.value.toMutableMap()
                updatedCache[key] = updatedStats
                statisticsCache.value = updatedCache
                saveStatisticsToFile()
            } catch (e: Exception) {
                // 记录失败时静默处理
            }
        }
    }

    /**
     * 更新 launchTrend，保持最近7天的数据
     */
    private fun updateLaunchTrend(currentTrend: List<Int>, today: String): List<Int> {
        val updatedTrend = currentTrend.toMutableList()
        return if (updatedTrend.size >= DEFAULT_TREND_DAYS) {
            // 移除最早的，添加今天的
            updatedTrend.drop(1) + (updatedTrend.last() + 1)
        } else {
            if (updatedTrend.isEmpty()) {
                listOf(1)
            } else {
                updatedTrend + (updatedTrend.last() + 1)
            }
        }
    }

    /**
     * 从数据库获取应用名称
     */
    private suspend fun getAppName(packageName: String, profileUserId: Int): String {
        return try {
            val app = clonedAppDao.getAppByPackageAndUser(packageName, profileUserId)
            app?.getDisplayName() ?: packageName
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * 从数据库获取用户空间名称
     */
    private suspend fun getProfileName(profileUserId: Int): String {
        return try {
            val profile = userProfileDao.getProfileById(profileUserId)
            profile?.profileName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // ==================== 查询统计 / Query Statistics ====================

    /**
     * 获取指定应用的详细使用统计
     */
    override suspend fun getStatistics(packageName: String, profileUserId: Int): AppUsageStatistics? {
        return withContext(Dispatchers.IO) {
            val key = buildKey(packageName, profileUserId)
            statisticsCache.value[key]
        }
    }

    /**
     * 获取所有应用的使用统计（Flow 实时更新）
     */
    override fun getAllStatistics(): Flow<List<AppUsageStatistics>> {
        return statisticsCache.asStateFlow().map { it.values.toList() }
    }

    /**
     * 获取使用统计概览
     *
     * 聚合所有统计数据，计算总启动次数、活跃应用数、
     * 最常使用应用、最近使用应用、使用趋势等。
     */
    override suspend fun getOverview(period: StatisticsPeriod): UsageOverview {
        return withContext(Dispatchers.IO) {
            try {
                val allStats = statisticsCache.values.toList()
                if (allStats.isEmpty()) {
                    return@withContext UsageOverview()
                }

                val today = getTodayDate()
                val weekStart = getThisWeekStart()
                val monthStart = getMonthStartDate()

                // 计算时间范围起始日期
                val periodStart = when (period) {
                    StatisticsPeriod.TODAY -> today
                    StatisticsPeriod.THIS_WEEK -> weekStart
                    StatisticsPeriod.THIS_MONTH -> monthStart
                    StatisticsPeriod.ALL_TIME -> ""
                    StatisticsPeriod.CUSTOM -> ""
                }

                // 获取克隆应用总数
                val totalClonedApps = try {
                    clonedAppDao.getTotalCount()
                } catch (e: Exception) {
                    allStats.size
                }

                // 计算活跃应用数（近7天有使用记录）
                val sevenDaysAgo = getDateDaysAgo(7)
                val activeApps = allStats.count { stats ->
                    stats.dailyUsage.keys.any { date -> date >= sevenDaysAgo }
                }

                // 计算今日总启动次数和今日总使用时长
                val todayStats = allStats.mapNotNull { it.dailyUsage[today] }
                val totalLaunchesToday = todayStats.sumOf { it.launchCount }
                val totalUsageTimeToday = todayStats.sumOf { it.usageTimeMs }

                // 计算本周总启动次数
                val weeklyLaunches = allStats.sumOf { stats ->
                    stats.dailyUsage.entries
                        .filter { (date, _) -> date >= weekStart }
                        .sumOf { (_, usage) -> usage.launchCount }
                }

                // 计算历史总启动次数
                val totalLaunchesAllTime = allStats.sumOf { it.totalLaunchCount.toLong() }

                // 获取最常使用的应用（按启动次数排序）
                val mostUsedApps = buildTopUsedAppsList(allStats, DEFAULT_TOP_LIMIT, periodStart)

                // 获取最近使用的应用
                val recentlyUsedApps = buildRecentlyUsedAppsList(allStats, DEFAULT_RECENT_LIMIT)

                // 获取使用趋势
                val usageTrend = computeUsageTrendInternal(30)

                UsageOverview(
                    totalClonedApps = totalClonedApps,
                    activeApps = activeApps,
                    totalLaunchesToday = totalLaunchesToday,
                    totalLaunchesThisWeek = weeklyLaunches,
                    totalLaunchesAllTime = totalLaunchesAllTime,
                    totalUsageTimeToday = totalUsageTimeToday,
                    mostUsedApps = mostUsedApps,
                    recentlyUsedApps = recentlyUsedApps,
                    usageTrend = usageTrend
                )
            } catch (e: Exception) {
                UsageOverview()
            }
        }
    }

    /**
     * 获取最常使用的应用列表
     *
     * 默认按启动次数排序，返回前 N 个应用。
     */
    override suspend fun getTopUsedApps(limit: Int, period: StatisticsPeriod): List<TopUsedApp> {
        return withContext(Dispatchers.IO) {
            try {
                val allStats = statisticsCache.values.toList()
                if (allStats.isEmpty()) {
                    return@withContext emptyList()
                }

                val periodStart = when (period) {
                    StatisticsPeriod.TODAY -> getTodayDate()
                    StatisticsPeriod.THIS_WEEK -> getThisWeekStart()
                    StatisticsPeriod.THIS_MONTH -> getMonthStartDate()
                    StatisticsPeriod.ALL_TIME -> ""
                    StatisticsPeriod.CUSTOM -> ""
                }

                buildTopUsedAppsList(allStats, limit, periodStart)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * 获取最近使用的应用列表
     *
     * 按最后启动时间降序排列。
     */
    override suspend fun getRecentlyUsedApps(limit: Int): List<RecentlyUsedApp> {
        return withContext(Dispatchers.IO) {
            try {
                val allStats = statisticsCache.values.toList()
                if (allStats.isEmpty()) {
                    return@withContext emptyList()
                }

                buildRecentlyUsedAppsList(allStats, limit)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ==================== 趋势分析 / Trend Analysis ====================

    /**
     * 获取使用趋势数据
     *
     * 计算最近 N 天的每日启动次数和使用时长，
     * 以及周环比和月环比增长率。
     */
    override suspend fun getUsageTrend(days: Int): UsageTrend {
        return withContext(Dispatchers.IO) {
            computeUsageTrendInternal(days)
        }
    }

    /**
     * 获取每日启动次数列表
     */
    override suspend fun getDailyLaunches(days: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val allStats = statisticsCache.values.toList()
                val dailyLaunches = mutableListOf<Int>()

                for (i in (days - 1) downTo 0) {
                    val date = getDateDaysAgo(i)
                    val dayLaunches = allStats.sumOf { stats ->
                        stats.dailyUsage[date]?.launchCount ?: 0
                    }
                    dailyLaunches.add(dayLaunches)
                }

                dailyLaunches
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * 内部方法：计算使用趋势
     */
    private fun computeUsageTrendInternal(days: Int): UsageTrend {
        return try {
            val allStats = statisticsCache.values.toList()
            if (allStats.isEmpty()) {
                return UsageTrend()
            }

            // 计算每日启动次数
            val dailyLaunches = mutableListOf<Int>()
            val dailyUsageTime = mutableListOf<Long>()

            for (i in (days - 1) downTo 0) {
                val date = getDateDaysAgo(i)
                val dayLaunches = allStats.sumOf { stats ->
                    stats.dailyUsage[date]?.launchCount ?: 0
                }
                val dayUsageTime = allStats.sumOf { stats ->
                    (stats.dailyUsage[date]?.usageTimeMs ?: 0) / MILLISECONDS_PER_MINUTE
                }
                dailyLaunches.add(dayLaunches)
                dailyUsageTime.add(dayUsageTime)
            }

            // 计算周环比增长率
            val weeklyGrowthRate = calculateWeeklyGrowthRate(allStats)

            // 计算月环比增长率
            val monthlyGrowthRate = calculateMonthlyGrowthRate(allStats)

            UsageTrend(
                dailyLaunches = dailyLaunches,
                dailyUsageTime = dailyUsageTime,
                weeklyGrowthRate = weeklyGrowthRate,
                monthlyGrowthRate = monthlyGrowthRate
            )
        } catch (e: Exception) {
            UsageTrend()
        }
    }

    /**
     * 计算周环比增长率
     *
     * (本周启动次数 - 上周启动次数) / 上周启动次数 * 100%
     */
    private fun calculateWeeklyGrowthRate(allStats: List<AppUsageStatistics>): Float {
        val thisWeekStart = getThisWeekStart()
        val lastWeekStart = getLastWeekStart()

        val thisWeekLaunches = allStats.sumOf { stats ->
            stats.dailyUsage.entries
                .filter { (date, _) -> date >= thisWeekStart }
                .sumOf { (_, usage) -> usage.launchCount }
        }

        val lastWeekLaunches = allStats.sumOf { stats ->
            stats.dailyUsage.entries
                .filter { (date, _) -> date >= lastWeekStart && date < thisWeekStart }
                .sumOf { (_, usage) -> usage.launchCount }
        }

        return if (lastWeekLaunches > 0) {
            ((thisWeekLaunches - lastWeekLaunches).toFloat() / lastWeekLaunches.toFloat()) * 100f
        } else {
            if (thisWeekLaunches > 0) 100f else 0f
        }
    }

    /**
     * 计算月环比增长率
     *
     * (本月启动次数 - 上月启动次数) / 上月启动次数 * 100%
     */
    private fun calculateMonthlyGrowthRate(allStats: List<AppUsageStatistics>): Float {
        val thisMonthStart = getMonthStartDate()
        val lastMonthStart = getLastMonthStart()

        val thisMonthLaunches = allStats.sumOf { stats ->
            stats.dailyUsage.entries
                .filter { (date, _) -> date >= thisMonthStart }
                .sumOf { (_, usage) -> usage.launchCount }
        }

        val lastMonthLaunches = allStats.sumOf { stats ->
            stats.dailyUsage.entries
                .filter { (date, _) -> date >= lastMonthStart && date < thisMonthStart }
                .sumOf { (_, usage) -> usage.launchCount }
        }

        return if (lastMonthLaunches > 0) {
            ((thisMonthLaunches - lastMonthLaunches).toFloat() / lastMonthLaunches.toFloat()) * 100f
        } else {
            if (thisMonthLaunches > 0) 100f else 0f
        }
    }

    // ==================== 辅助构建方法 / Helper Builder Methods ====================

    /**
     * 构建最常使用的应用列表
     *
     * @param allStats 所有统计数据
     * @param limit 返回数量上限
     * @param periodStart 时间范围起始日期，空字符串表示全部时间
     */
    private fun buildTopUsedAppsList(
        allStats: List<AppUsageStatistics>,
        limit: Int,
        periodStart: String
    ): List<TopUsedApp> {
        return allStats
            .map { stats ->
                val (launchCount, usageTimeMs) = if (periodStart.isEmpty()) {
                    // 全部时间
                    Pair(stats.totalLaunchCount, stats.totalUsageTimeMs)
                } else {
                    // 指定时间范围
                    val filteredLaunches = stats.dailyUsage.entries
                        .filter { (date, _) -> date >= periodStart }
                        .sumOf { (_, usage) -> usage.launchCount }
                    val filteredUsageTime = stats.dailyUsage.entries
                        .filter { (date, _) -> date >= periodStart }
                        .sumOf { (_, usage) -> usage.usageTimeMs }
                    Pair(filteredLaunches, filteredUsageTime)
                }

                TopUsedApp(
                    packageName = stats.packageName,
                    appName = stats.appName,
                    launchCount = launchCount,
                    usageTimeMs = usageTimeMs,
                    rank = 0
                )
            }
            .filter { it.launchCount > 0 || it.usageTimeMs > 0 }
            .sortedWith(compareByDescending<TopUsedApp> { it.launchCount }.thenByDescending { it.usageTimeMs })
            .take(limit)
            .mapIndexed { index, app -> app.copy(rank = index + 1) }
    }

    /**
     * 构建最近使用的应用列表
     *
     * @param allStats 所有统计数据
     * @param limit 返回数量上限
     */
    private suspend fun buildRecentlyUsedAppsList(
        allStats: List<AppUsageStatistics>,
        limit: Int
    ): List<RecentlyUsedApp> {
        return allStats
            .filter { it.lastLaunchTime > 0 }
            .sortedByDescending { it.lastLaunchTime }
            .take(limit)
            .map { stats ->
                val profileName = getProfileName(stats.profileUserId)
                RecentlyUsedApp(
                    packageName = stats.packageName,
                    appName = stats.appName,
                    lastLaunchTime = stats.lastLaunchTime,
                    profileName = profileName
                )
            }
    }

    // ==================== 管理 / Management ====================

    /**
     * 清除所有使用统计数据
     *
     * 删除 JSON 文件并清空内存缓存。
     */
    override suspend fun clearStatistics() {
        withContext(Dispatchers.IO) {
            try {
                statisticsCache.value = emptyMap()
                if (statisticsFile.exists()) {
                    statisticsFile.delete()
                }
            } catch (e: Exception) {
                // 清除失败时静默处理
            }
        }
    }

    /**
     * 清除指定应用的使用统计数据
     */
    override suspend fun clearStatisticsForApp(packageName: String, profileUserId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val key = buildKey(packageName, profileUserId)
                val updatedCache = statisticsCache.value.toMutableMap()
                updatedCache.remove(key)
                statisticsCache.value = updatedCache
                saveStatisticsToFile()
            } catch (e: Exception) {
                // 清除失败时静默处理
            }
        }
    }

    /**
     * 导出使用统计数据为 JSON 字符串
     */
    override suspend fun exportStatistics(): String {
        return withContext(Dispatchers.IO) {
            try {
                val serialized = SerializedStatisticsMap(statisticsCache.value)
                json.encodeToString(serialized)
            } catch (e: Exception) {
                "{}"
            }
        }
    }
}

/**
 * 用于 JSON 序列化的统计数据 Map 包装类
 *
 * kotlinx.serialization 需要一个明确的序列化容器来处理 Map<String, AppUsageStatistics>。
 */
@Serializable
internal data class SerializedStatisticsMap(
    val statistics: Map<String, AppUsageStatistics> = emptyMap()
)
