/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测仓库实现
 * 实现性能回归自动检测、历史趋势分析、阈值告警等核心功能
 * 使用JSON文件进行数据持久化，Mutex保证线程安全
 */

package com.appclonex.infrastructure.repository

import android.content.Context
import android.os.Build
import com.appclonex.domain.model.DeviceInfo
import com.appclonex.domain.model.PerformanceAlert
import com.appclonex.domain.model.PerformanceBaseline
import com.appclonex.domain.model.RegressionItem
import com.appclonex.domain.model.RegressionReport
import com.appclonex.domain.model.RegressionSeverity
import com.appclonex.domain.model.RegressionThreshold
import com.appclonex.domain.model.RegressionTrend
import com.appclonex.domain.model.RegressionType
import com.appclonex.domain.model.TrendDataPoint
import com.appclonex.domain.model.TrendDirection
import com.appclonex.domain.repository.PerformanceRegressionRepository
import com.appclonex.infrastructure.monitoring.PerformanceBenchmark
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 性能回归检测仓库实现
 *
 * 核心功能：
 * - 执行完整基准测试并与上次结果比较，自动检测性能回归
 * - 回归检测算法：计算变化百分比，根据阈值判断严重程度
 * - 历史趋势分析：线性回归计算斜率，判断趋势方向
 * - 阈值管理：默认阈值 + 用户自定义覆盖
 * - 基线管理：按设备型号 + Android版本分组
 * - 告警管理：自动生成CRITICAL和SEVERE级别告警
 *
 * @property context 应用上下文
 * @property performanceBenchmark 性能基准测试执行器
 */
@Singleton
class PerformanceRegressionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceBenchmark: PerformanceBenchmark
) : PerformanceRegressionRepository {

    // ==================== 序列化配置 ====================

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    // ==================== 线程安全 ====================

    /** 互斥锁，保证文件读写操作的线程安全 */
    private val mutex = Mutex()

    // ==================== 数据文件路径 ====================

    private val reportsFile: File
        get() = File(context.filesDir, "regression_reports.json")

    private val alertsFile: File
        get() = File(context.filesDir, "regression_alerts.json")

    private val thresholdsFile: File
        get() = File(context.filesDir, "regression_thresholds.json")

    private val baselinesFile: File
        get() = File(context.filesDir, "regression_baselines.json")

    // ==================== 实时数据流 ====================

    private val _alertsFlow = MutableStateFlow<List<PerformanceAlert>>(emptyList())
    private val _reportsFlow = MutableStateFlow<List<RegressionReport>>(emptyList())

    // ==================== 默认阈值 ====================

    /** 各回归类型的默认阈值配置 */
    private val defaultThresholds: Map<RegressionType, RegressionThreshold> = mapOf(
        RegressionType.COLD_START to RegressionThreshold(
            type = RegressionType.COLD_START,
            warningThreshold = 10f,
            criticalThreshold = 50f,
            maxAcceptableChange = 5f
        ),
        RegressionType.MEMORY_USAGE to RegressionThreshold(
            type = RegressionType.MEMORY_USAGE,
            warningThreshold = 10f,
            criticalThreshold = 50f,
            maxAcceptableChange = 5f
        ),
        RegressionType.DATABASE_QUERY to RegressionThreshold(
            type = RegressionType.DATABASE_QUERY,
            warningThreshold = 15f,
            criticalThreshold = 50f,
            maxAcceptableChange = 8f
        ),
        RegressionType.UI_RENDERING to RegressionThreshold(
            type = RegressionType.UI_RENDERING,
            warningThreshold = 10f,
            criticalThreshold = 40f,
            maxAcceptableChange = 5f
        ),
        RegressionType.BATTERY_USAGE to RegressionThreshold(
            type = RegressionType.BATTERY_USAGE,
            warningThreshold = 15f,
            criticalThreshold = 50f,
            maxAcceptableChange = 8f
        ),
        RegressionType.STORAGE_IO to RegressionThreshold(
            type = RegressionType.STORAGE_IO,
            warningThreshold = 15f,
            criticalThreshold = 50f,
            maxAcceptableChange = 8f
        )
    )

    // ==================== 指标名称映射 ====================

    /** 回归类型到可读指标名称的映射 */
    private val metricNameMap: Map<RegressionType, String> = mapOf(
        RegressionType.COLD_START to "冷启动时间",
        RegressionType.MEMORY_USAGE to "内存使用量",
        RegressionType.DATABASE_QUERY to "数据库查询",
        RegressionType.UI_RENDERING to "UI渲染帧时间",
        RegressionType.BATTERY_USAGE to "电池消耗率",
        RegressionType.STORAGE_IO to "存储IO延迟"
    )

    // ==================== 基准测试执行 / Benchmark Execution ====================

    /**
     * 运行完整的性能基准测试并生成回归报告
     */
    override suspend fun runFullBenchmark(): Result<RegressionReport> {
        return try {
            mutex.withLock {
                // 获取上次报告用于比较
                val previousReport = loadReports().firstOrNull()
                val previousScore = previousReport?.overallScore ?: 0f

                // 执行基准测试获取当前指标
                val suite = performanceBenchmark.runFullBenchmark()
                val metrics = performanceBenchmark.getPerformanceMetrics()

                // 构建当前指标映射
                val currentMetrics = buildCurrentMetrics(suite, metrics)

                // 获取上次指标（用于比较）
                val previousMetrics = previousReport?.regressions?.associateBy { it.type }

                // 获取自定义阈值
                val customThresholds = loadThresholds()

                // 逐项检测回归
                val regressions = mutableListOf<RegressionItem>()
                RegressionType.entries.forEach { type ->
                    val currentValue = currentMetrics[type] ?: 0f
                    val previousValue = previousMetrics?.get(type)?.currentValue ?: currentValue
                    val threshold = customThresholds[type] ?: defaultThresholds[type]
                        ?: throw IllegalStateException("无法获取类型 $type 的阈值配置")

                    val item = detectRegression(type, currentValue, previousValue, threshold)
                    regressions.add(item)
                }

                // 计算总体评分
                val overallScore = suite.overallScore
                val scoreChange = if (previousScore > 0) overallScore - previousScore else 0f

                // 生成改善建议
                val recommendations = generateRecommendations(regressions)

                // 获取设备信息
                val deviceInfo = getDeviceInfo()

                // 获取应用版本信息
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                // 构建报告
                val report = RegressionReport(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    },
                    versionName = packageInfo.versionName ?: "unknown",
                    regressions = regressions,
                    overallScore = overallScore,
                    previousScore = previousScore,
                    scoreChange = scoreChange,
                    deviceInfo = deviceInfo,
                    recommendations = recommendations
                )

                // 保存报告
                saveReport(report)

                // 自动生成告警（CRITICAL和SEVERE级别）
                val newAlerts = regressions
                    .filter { it.severity == RegressionSeverity.CRITICAL || it.severity == RegressionSeverity.SEVERE }
                    .map { item ->
                        PerformanceAlert(
                            id = UUID.randomUUID().toString(),
                            type = item.type,
                            severity = item.severity,
                            message = "${item.metricName}检测到${item.severity}回归: " +
                                    "变化 ${String.format("%.1f", item.changePercent)}%",
                            timestamp = System.currentTimeMillis(),
                            isRead = false
                        )
                    }
                if (newAlerts.isNotEmpty()) {
                    saveAlerts(newAlerts)
                }

                // 更新实时数据流
                _reportsFlow.value = loadReports()

                Result.success(report)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 运行指定类型的基准测试
     */
    override suspend fun runBenchmarkForType(type: RegressionType): Result<RegressionItem> {
        return try {
            val suite = performanceBenchmark.runFullBenchmark()
            val metrics = performanceBenchmark.getPerformanceMetrics()
            val currentMetrics = buildCurrentMetrics(suite, metrics)
            val currentValue = currentMetrics[type] ?: 0f

            val previousReport = loadReports().firstOrNull()
            val previousValue = previousReport?.regressions
                ?.firstOrNull { it.type == type }?.currentValue ?: currentValue

            val customThresholds = loadThresholds()
            val threshold = customThresholds[type] ?: defaultThresholds[type]
                ?: throw IllegalStateException("无法获取类型 $type 的阈值配置")

            val item = detectRegression(type, currentValue, previousValue, threshold)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 历史数据查询 / History Query ====================

    /**
     * 获取回归报告历史记录
     */
    override suspend fun getRegressionHistory(limit: Int): Result<List<RegressionReport>> {
        return try {
            val reports = loadReports().take(limit)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取指定类型的性能趋势分析
     *
     * 算法：
     * 1. 从历史报告中提取指定类型的数据点
     * 2. 使用最小二乘法进行线性回归计算斜率
     * 3. 根据斜率判断趋势方向
     * 4. 基于斜率预测下一期值
     */
    override suspend fun getRegressionTrend(type: RegressionType): Result<RegressionTrend> {
        return try {
            val reports = loadReports()

            // 从历史报告中提取数据点
            val dataPoints = reports.mapNotNull { report ->
                report.regressions
                    .firstOrNull { it.type == type }
                    ?.let { item ->
                        TrendDataPoint(
                            timestamp = report.timestamp,
                            value = item.currentValue,
                            versionCode = report.versionCode
                        )
                    }
            }.reversed() // 按时间正序排列

            if (dataPoints.size < 2) {
                return Result.success(
                    RegressionTrend(
                        type = type,
                        dataPoints = dataPoints,
                        slope = 0f,
                        direction = TrendDirection.UNKNOWN,
                        prediction = dataPoints.lastOrNull()?.value ?: 0f
                    )
                )
            }

            // 线性回归计算斜率
            val slope = calculateLinearRegressionSlope(dataPoints)

            // 判断趋势方向
            // 注意：对于性能指标（如启动时间、内存），斜率为正表示性能下降
            val direction = when {
                slope > 0.5f -> TrendDirection.DEGRADING   // 性能持续下降
                slope < -0.5f -> TrendDirection.IMPROVING   // 性能持续改善
                else -> TrendDirection.STABLE               // 性能保持稳定
            }

            // 预测下一期值
            val lastValue = dataPoints.last().value
            val prediction = lastValue + slope

            Result.success(
                RegressionTrend(
                    type = type,
                    dataPoints = dataPoints,
                    slope = slope,
                    direction = direction,
                    prediction = prediction
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取最新的回归报告
     */
    override suspend fun getLatestReport(): Result<RegressionReport?> {
        return try {
            val report = loadReports().firstOrNull()
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 告警管理 / Alert Management ====================

    /**
     * 获取所有性能告警
     */
    override suspend fun getAlerts(): Result<List<PerformanceAlert>> {
        return try {
            val alerts = loadAlerts()
            // 未读告警排在前面
            val sorted = alerts.sortedWith(
                compareByDescending<PerformanceAlert> { !it.isRead }
                    .thenByDescending { it.timestamp }
            )
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 标记指定告警为已读
     */
    override suspend fun markAlertRead(alertId: String): Result<Unit> {
        return try {
            mutex.withLock {
                val alerts = loadAlerts().map { alert ->
                    if (alert.id == alertId) alert.copy(isRead = true) else alert
                }
                saveAlertsToFile(alerts)
                _alertsFlow.value = alerts
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 忽略/关闭指定告警
     */
    override suspend fun dismissAlert(alertId: String): Result<Unit> {
        return try {
            mutex.withLock {
                val alerts = loadAlerts().filter { it.id != alertId }
                saveAlertsToFile(alerts)
                _alertsFlow.value = alerts
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 阈值管理 / Threshold Management ====================

    /**
     * 更新指定回归类型的阈值配置
     */
    override suspend fun updateThreshold(
        type: RegressionType,
        warningThreshold: Float,
        criticalThreshold: Float
    ): Result<Unit> {
        return try {
            mutex.withLock {
                val thresholds = loadThresholds().toMutableMap()
                thresholds[type] = RegressionThreshold(
                    type = type,
                    warningThreshold = warningThreshold,
                    criticalThreshold = criticalThreshold,
                    maxAcceptableChange = warningThreshold / 2f
                )
                saveThresholdsToFile(thresholds)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取所有回归类型的阈值配置
     */
    override suspend fun getThresholds(): Result<Map<RegressionType, RegressionThreshold>> {
        return try {
            val customThresholds = loadThresholds()
            // 合并默认阈值和自定义阈值
            val merged = defaultThresholds.toMutableMap()
            merged.putAll(customThresholds)
            Result.success(merged)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 基线管理 / Baseline Management ====================

    /**
     * 获取所有性能基线
     */
    override suspend fun getBaselines(): Result<List<PerformanceBaseline>> {
        return try {
            val baselines = loadBaselines()
            Result.success(baselines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设置性能基线
     */
    override suspend fun setBaseline(baseline: PerformanceBaseline): Result<Unit> {
        return try {
            mutex.withLock {
                val baselines = loadBaselines().toMutableList()
                // 按设备型号+Android版本查找并更新，或新增
                val existingIndex = baselines.indexOfFirst {
                    it.deviceModel == baseline.deviceModel &&
                    it.androidVersion == baseline.androidVersion
                }
                val updatedBaseline = baseline.copy(updatedAt = System.currentTimeMillis())
                if (existingIndex >= 0) {
                    baselines[existingIndex] = updatedBaseline
                } else {
                    baselines.add(updatedBaseline)
                }
                saveBaselinesToFile(baselines)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将回归报告与基线进行比较
     */
    override suspend fun compareWithBaseline(report: RegressionReport): Result<Map<String, Float>> {
        return try {
            val baselines = loadBaselines()
            val matchingBaseline = baselines.firstOrNull {
                it.deviceModel == report.deviceInfo.model &&
                it.androidVersion == report.deviceInfo.androidVersion
            } ?: return Result.success(emptyMap())

            // 计算各指标与基线的偏差
            val deviations = mutableMapOf<String, Float>()
            report.regressions.forEach { item ->
                val baselineValue = matchingBaseline.metrics[item.metricName]
                if (baselineValue != null && baselineValue > 0) {
                    val deviation = (item.currentValue - baselineValue) / baselineValue * 100f
                    deviations[item.metricName] = deviation
                }
            }
            Result.success(deviations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 实时数据流 / Reactive Streams ====================

    /**
     * 观察告警列表的实时变化
     */
    override fun observeAlerts(): Flow<List<PerformanceAlert>> {
        return _alertsFlow.asStateFlow()
    }

    /**
     * 观察回归报告列表的实时变化
     */
    override fun observeRegressionReports(): Flow<List<RegressionReport>> {
        return _reportsFlow.asStateFlow()
    }

    // ==================== 私有方法 - 回归检测算法 ====================

    /**
     * 检测单个指标的回归
     *
     * 算法：
     * 1. 计算变化百分比 = (current - previous) / previous * 100
     * 2. 对于性能指标（启动时间、内存等），增加是回归
     * 3. 根据变化百分比判断严重程度：
     *    - NONE: <5%
     *    - MINOR: 5%-10%
     *    - MODERATE: 10%-20%
     *    - SEVERE: 20%-50%
     *    - CRITICAL: >50%
     *
     * @param type 回归类型
     * @param currentValue 当前值
     * @param previousValue 上次值
     * @param threshold 阈值配置
     * @return 回归项
     */
    private fun detectRegression(
        type: RegressionType,
        currentValue: Float,
        previousValue: Float,
        threshold: RegressionThreshold
    ): RegressionItem {
        val metricName = metricNameMap[type] ?: type.name

        // 计算变化百分比
        val changePercent = if (previousValue > 0) {
            (currentValue - previousValue) / previousValue * 100f
        } else {
            0f
        }

        // 对于性能指标，正值（增加）表示性能下降（回归）
        val absoluteChange = abs(changePercent)

        // 根据阈值判断严重程度
        val severity = when {
            absoluteChange < threshold.maxAcceptableChange -> RegressionSeverity.NONE
            absoluteChange < threshold.warningThreshold -> RegressionSeverity.MINOR
            absoluteChange < threshold.warningThreshold * 2 -> RegressionSeverity.MODERATE
            absoluteChange < threshold.criticalThreshold -> RegressionSeverity.SEVERE
            else -> RegressionSeverity.CRITICAL
        }

        // 生成描述信息
        val description = when (severity) {
            RegressionSeverity.NONE -> "${metricName}表现正常，变化${String.format("%.1f", absoluteChange)}%"
            RegressionSeverity.MINOR -> "${metricName}轻微变化${String.format("%.1f", absoluteChange)}%，建议关注"
            RegressionSeverity.MODERATE -> "${metricName}中度回归${String.format("%.1f", absoluteChange)}%，需要优化"
            RegressionSeverity.SEVERE -> "${metricName}严重回归${String.format("%.1f", absoluteChange)}%，请立即处理"
            RegressionSeverity.CRITICAL -> "${metricName}关键回归${String.format("%.1f", absoluteChange)}%，严重影响用户体验"
        }

        return RegressionItem(
            type = type,
            metricName = metricName,
            currentValue = currentValue,
            previousValue = previousValue,
            changePercent = changePercent,
            severity = severity,
            threshold = threshold.warningThreshold,
            description = description
        )
    }

    /**
     * 生成改善建议
     *
     * 根据回归类型和严重程度生成针对性的优化建议
     *
     * @param regressions 回归项列表
     * @return 建议列表
     */
    private fun generateRecommendations(regressions: List<RegressionItem>): List<String> {
        val recommendations = mutableListOf<String>()

        regressions.filter { it.severity != RegressionSeverity.NONE }.forEach { item ->
            when (item.type) {
                RegressionType.COLD_START -> {
                    recommendations.add("优化冷启动: 检查Application.onCreate()中的初始化逻辑，延迟非必要组件加载")
                    if (item.severity.ordinal >= RegressionSeverity.MODERATE.ordinal) {
                        recommendations.add("考虑使用Baseline Profiles优化冷启动编译")
                    }
                }
                RegressionType.MEMORY_USAGE -> {
                    recommendations.add("优化内存使用: 检查内存泄漏，使用LeakCanary进行内存分析")
                    if (item.severity.ordinal >= RegressionSeverity.MODERATE.ordinal) {
                        recommendations.add("审查Bitmap缓存策略和大型对象的生命周期管理")
                    }
                }
                RegressionType.DATABASE_QUERY -> {
                    recommendations.add("优化数据库查询: 检查索引使用情况，避免N+1查询")
                    if (item.severity.ordinal >= RegressionSeverity.MODERATE.ordinal) {
                        recommendations.add("考虑使用Room的@Relation注解替代手动JOIN查询")
                    }
                }
                RegressionType.UI_RENDERING -> {
                    recommendations.add("优化UI渲染: 检查过度绘制和布局层级，使用Compose性能分析工具")
                    if (item.severity.ordinal >= RegressionSeverity.MODERATE.ordinal) {
                        recommendations.add("使用derivedStateOf和remember减少不必要的重组")
                    }
                }
                RegressionType.BATTERY_USAGE -> {
                    recommendations.add("优化电池使用: 检查后台任务和WakeLock使用情况")
                    if (item.severity.ordinal >= RegressionSeverity.MODERATE.ordinal) {
                        recommendations.add("使用WorkManager替代直接使用AlarmManager")
                    }
                }
                RegressionType.STORAGE_IO -> {
                    recommendations.add("优化存储IO: 减少主线程磁盘操作，使用DataStore替代SharedPreferences")
                    if (item.severity.ordinal >= RegressionSeverity.MODERATE.ordinal) {
                        recommendations.add("考虑使用内存缓存减少频繁的磁盘读写")
                    }
                }
            }
        }

        return recommendations.distinct()
    }

    // ==================== 私有方法 - 线性回归 ====================

    /**
     * 使用最小二乘法计算线性回归斜率
     *
     * 公式: slope = (n * Σ(x*y) - Σx * Σy) / (n * Σ(x^2) - (Σx)^2)
     * 其中 x 为数据点索引，y 为指标值
     *
     * @param dataPoints 趋势数据点列表
     * @return 线性回归斜率
     */
    private fun calculateLinearRegressionSlope(dataPoints: List<TrendDataPoint>): Float {
        val n = dataPoints.size.toFloat()
        if (n < 2) return 0f

        var sumX = 0f
        var sumY = 0f
        var sumXY = 0f
        var sumX2 = 0f

        dataPoints.forEachIndexed { index, point ->
            val x = index.toFloat()
            val y = point.value
            sumX += x
            sumY += y
            sumXY += x * y
            sumX2 += x * x
        }

        val denominator = n * sumX2 - sumX * sumX
        return if (abs(denominator) < 0.001f) {
            0f
        } else {
            (n * sumXY - sumX * sumY) / denominator
        }
    }

    // ==================== 私有方法 - 辅助工具 ====================

    /**
     * 从基准测试结果中构建当前指标映射
     */
    private fun buildCurrentMetrics(
        suite: com.appclonex.domain.model.BenchmarkSuite,
        metrics: com.appclonex.domain.model.PerformanceMetrics
    ): Map<RegressionType, Float> {
        return mapOf(
            RegressionType.COLD_START to metrics.coldStartMs.toFloat(),
            RegressionType.MEMORY_USAGE to metrics.memoryUsageMb,
            RegressionType.DATABASE_QUERY to metrics.dbQueryAvgMs,
            RegressionType.UI_RENDERING to metrics.uiFrameTimeMs,
            RegressionType.BATTERY_USAGE to 0f, // 暂无直接测量方式，使用估算值
            RegressionType.STORAGE_IO to 0f     // 暂无直接测量方式，使用估算值
        )
    }

    /**
     * 获取当前设备信息
     */
    private fun getDeviceInfo(): DeviceInfo {
        val runtime = Runtime.getRuntime()
        return DeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            sdkVersion = Build.VERSION.SDK_INT,
            totalMemory = runtime.maxMemory() / (1024 * 1024),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            screenDensity = context.resources.displayMetrics.densityDpi
        )
    }

    // ==================== 私有方法 - JSON持久化 ====================

    @Serializable
    private data class StoredReports(val reports: List<RegressionReport> = emptyList())

    @Serializable
    private data class StoredAlerts(val alerts: List<PerformanceAlert> = emptyList())

    @Serializable
    private data class StoredThresholds(
        val thresholds: Map<String, RegressionThreshold> = emptyMap()
    )

    @Serializable
    private data class StoredBaselines(val baselines: List<PerformanceBaseline> = emptyList())

    /**
     * 加载报告列表
     */
    private fun loadReports(): List<RegressionReport> {
        return try {
            if (!reportsFile.exists()) return emptyList()
            val content = reportsFile.readText()
            if (content.isBlank()) return emptyList()
            json.decodeFromString<StoredReports>(content).reports
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存单个报告（追加到列表头部）
     */
    private fun saveReport(report: RegressionReport) {
        try {
            val existing = loadReports().toMutableList()
            existing.add(0, report)
            // 保留最近50条记录
            val trimmed = existing.take(50)
            saveReportsToFile(trimmed)
        } catch (e: Exception) {
            // 基准测试失败不应导致应用崩溃，但应记录日志
            Log.w(TAG, "保存报告失败: ${e.message}")
        }
    }

    /**
     * 保存报告列表到文件
     */
    private fun saveReportsToFile(reports: List<RegressionReport>) {
        try {
            AtomicFileWriter.write(reportsFile, json.encodeToString(StoredReports(reports)))
        } catch (e: Exception) {
            Log.w(TAG, "保存报告到文件失败: ${e.message}")
        }
    }

    /**
     * 加载告警列表
     */
    private fun loadAlerts(): List<PerformanceAlert> {
        return try {
            if (!alertsFile.exists()) return emptyList()
            val content = alertsFile.readText()
            if (content.isBlank()) return emptyList()
            json.decodeFromString<StoredAlerts>(content).alerts
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存新告警（追加到现有列表）
     */
    private fun saveAlerts(newAlerts: List<PerformanceAlert>) {
        try {
            val existing = loadAlerts().toMutableList()
            existing.addAll(0, newAlerts)
            // 保留最近100条告警
            val trimmed = existing.take(100)
            saveAlertsToFile(trimmed)
            _alertsFlow.value = trimmed
        } catch (e: Exception) {
            Log.w(TAG, "保存告警失败: ${e.message}")
        }
    }

    /**
     * 保存告警列表到文件
     */
    private fun saveAlertsToFile(alerts: List<PerformanceAlert>) {
        try {
            AtomicFileWriter.write(alertsFile, json.encodeToString(StoredAlerts(alerts)))
        } catch (e: Exception) {
            Log.w(TAG, "保存告警到文件失败: ${e.message}")
        }
    }

    /**
     * 加载自定义阈值配置
     */
    private fun loadThresholds(): Map<RegressionType, RegressionThreshold> {
        return try {
            if (!thresholdsFile.exists()) return emptyMap()
            val content = thresholdsFile.readText()
            if (content.isBlank()) return emptyMap()
            val stored = json.decodeFromString<StoredThresholds>(content)
            stored.thresholds.mapKeys { (key, _) ->
                RegressionType.valueOf(key)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 保存阈值配置到文件
     */
    private fun saveThresholdsToFile(thresholds: Map<RegressionType, RegressionThreshold>) {
        try {
            val serialized = thresholds.mapKeys { (key, _) -> key.name }
            AtomicFileWriter.write(thresholdsFile, json.encodeToString(StoredThresholds(serialized)))
        } catch (_: Exception) {
            // 静默失败
        }
    }

    /**
     * 加载基线列表
     */
    private fun loadBaselines(): List<PerformanceBaseline> {
        return try {
            if (!baselinesFile.exists()) return emptyList()
            val content = baselinesFile.readText()
            if (content.isBlank()) return emptyList()
            json.decodeFromString<StoredBaselines>(content).baselines
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存基线列表到文件
     */
    private fun saveBaselinesToFile(baselines: List<PerformanceBaseline>) {
        try {
            AtomicFileWriter.write(baselinesFile, json.encodeToString(StoredBaselines(baselines)))
        } catch (_: Exception) {
            // 静默失败
        }
    }
}
