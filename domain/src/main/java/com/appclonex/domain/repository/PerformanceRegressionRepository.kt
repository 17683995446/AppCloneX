/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测仓库接口
 * 定义性能回归检测、历史趋势分析、阈值告警等操作的抽象接口
 */

package com.appclonex.domain.repository

import com.appclonex.domain.model.PerformanceAlert
import com.appclonex.domain.model.PerformanceBaseline
import com.appclonex.domain.model.RegressionReport
import com.appclonex.domain.model.RegressionThreshold
import com.appclonex.domain.model.RegressionTrend
import com.appclonex.domain.model.RegressionType
import kotlinx.coroutines.flow.Flow

/**
 * 性能回归检测仓库接口
 *
 * 提供性能回归自动检测、历史趋势分析、阈值告警管理等功能。
 * 实现层负责具体的基准测试执行、数据持久化和告警生成逻辑。
 */
interface PerformanceRegressionRepository {

    // ==================== 基准测试执行 / Benchmark Execution ====================

    /**
     * 运行完整的性能基准测试并生成回归报告
     *
     * 依次执行所有类型的基准测试，与上次结果进行比较，
     * 生成包含回归项、评分和改善建议的完整报告。
     *
     * @return 回归报告，失败时返回异常
     */
    suspend fun runFullBenchmark(): Result<RegressionReport>

    /**
     * 运行指定类型的基准测试并返回回归项
     *
     * @param type 要测试的回归类型
     * @return 单个回归项，失败时返回异常
     */
    suspend fun runBenchmarkForType(type: RegressionType): Result<com.appclonex.domain.model.RegressionItem>

    // ==================== 历史数据查询 / History Query ====================

    /**
     * 获取回归报告历史记录
     *
     * @param limit 返回的最大记录数量
     * @return 回归报告列表，按时间倒序排列
     */
    suspend fun getRegressionHistory(limit: Int = 20): Result<List<RegressionReport>>

    /**
     * 获取指定类型的性能趋势分析
     *
     * 从历史报告中提取数据点，通过线性回归计算斜率，
     * 判断性能变化趋势方向并预测下一期值。
     *
     * @param type 回归类型
     * @return 趋势分析结果
     */
    suspend fun getRegressionTrend(type: RegressionType): Result<RegressionTrend>

    /**
     * 获取最新的回归报告
     *
     * @return 最新的回归报告，不存在时返回null
     */
    suspend fun getLatestReport(): Result<RegressionReport?>

    // ==================== 告警管理 / Alert Management ====================

    /**
     * 获取所有性能告警
     *
     * @return 告警列表，未读告警排在前面
     */
    suspend fun getAlerts(): Result<List<PerformanceAlert>>

    /**
     * 标记指定告警为已读
     *
     * @param alertId 告警唯一标识
     * @return 操作结果
     */
    suspend fun markAlertRead(alertId: String): Result<Unit>

    /**
     * 忽略/关闭指定告警
     *
     * @param alertId 告警唯一标识
     * @return 操作结果
     */
    suspend fun dismissAlert(alertId: String): Result<Unit>

    // ==================== 阈值管理 / Threshold Management ====================

    /**
     * 更新指定回归类型的阈值配置
     *
     * @param type 回归类型
     * @param warningThreshold 警告阈值
     * @param criticalThreshold 关键阈值
     * @return 操作结果
     */
    suspend fun updateThreshold(
        type: RegressionType,
        warningThreshold: Float,
        criticalThreshold: Float
    ): Result<Unit>

    /**
     * 获取所有回归类型的阈值配置
     *
     * @return 回归类型到阈值的映射
     */
    suspend fun getThresholds(): Result<Map<RegressionType, RegressionThreshold>>

    // ==================== 基线管理 / Baseline Management ====================

    /**
     * 获取所有性能基线
     *
     * @return 性能基线列表
     */
    suspend fun getBaselines(): Result<List<PerformanceBaseline>>

    /**
     * 设置性能基线
     *
     * 按设备型号和Android版本分组存储基线数据。
     *
     * @param baseline 性能基线数据
     * @return 操作结果
     */
    suspend fun setBaseline(baseline: PerformanceBaseline): Result<Unit>

    /**
     * 将回归报告与基线进行比较
     *
     * @param report 回归报告
     * @return 各指标与基线的偏差映射（指标名 -> 偏差百分比）
     */
    suspend fun compareWithBaseline(report: RegressionReport): Result<Map<String, Float>>

    // ==================== 实时数据流 / Reactive Streams ====================

    /**
     * 观察告警列表的实时变化
     *
     * @return 告警列表的Flow
     */
    fun observeAlerts(): Flow<List<PerformanceAlert>>

    /**
     * 观察回归报告列表的实时变化
     *
     * @return 回归报告列表的Flow
     */
    fun observeRegressionReports(): Flow<List<RegressionReport>>
}
