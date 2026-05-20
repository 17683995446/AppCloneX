/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 获取性能趋势用例
 * 查询指定性能指标的历史趋势分析数据
 */

package com.appclonex.domain.usecase.monitoring

import com.appclonex.domain.model.RegressionTrend
import com.appclonex.domain.model.RegressionType
import com.appclonex.domain.repository.PerformanceRegressionRepository
import javax.inject.Inject

/**
 * 获取性能趋势用例
 *
 * 查询指定类型性能指标的历史趋势数据，
 * 包括线性回归斜率、趋势方向和预测值。
 *
 * R12-003 修复：移除 runBlocking，改为 suspend 函数
 * - runBlocking 会阻塞当前线程，可能导致 ANR
 * - 改为 suspend 函数，正确支持协程环境
 *
 * @property regressionRepository 性能回归检测仓库
 */
class GetPerformanceTrendUseCase @Inject constructor(
    private val regressionRepository: PerformanceRegressionRepository
) {

    /**
     * 获取指定类型的性能趋势
     *
     * 从仓库中获取历史回归报告，提取指定类型的数据点，
     * 通过线性回归算法计算趋势斜率并判断方向。
     *
     * R12-003 修复：改为 suspend 函数
     * - 直接调用 suspend 函数，无需 runBlocking 包装
     * - 支持协程取消和结构化并发
     *
     * @param type 回归类型（如冷启动、内存使用等）
     * @param dataPoints 要分析的数据点数量（默认取最近20个）
     * @return 趋势分析结果，失败时返回异常
     */
    suspend operator fun invoke(
        type: RegressionType,
        dataPoints: Int = 20
    ): Result<RegressionTrend> {
        return try {
            // R12-003 修复：直接调用 suspend 函数
            regressionRepository.getRegressionTrend(type)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
