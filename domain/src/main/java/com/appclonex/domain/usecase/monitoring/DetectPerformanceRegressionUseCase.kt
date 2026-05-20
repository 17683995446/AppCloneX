/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测用例
 * 执行完整的性能基准测试并自动检测性能回归
 */

package com.appclonex.domain.usecase.monitoring

import com.appclonex.domain.model.RegressionReport
import com.appclonex.domain.model.RegressionType
import com.appclonex.domain.repository.PerformanceRegressionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 检测进度 - 密封接口
 * 表示性能回归检测过程中的各个阶段状态
 */
sealed interface DetectionProgress {

    /**
     * 正在初始化检测环境
     */
    data object Initializing : DetectionProgress

    /**
     * 正在运行指定类型的基准测试
     *
     * @property type 当前测试的回归类型
     * @property progress 测试进度（0.0-1.0）
     */
    data class RunningBenchmark(
        val type: RegressionType,
        val progress: Float
    ) : DetectionProgress

    /**
     * 正在分析测试结果
     */
    data object AnalyzingResults : DetectionProgress

    /**
     * 正在生成回归报告
     */
    data object GeneratingReport : DetectionProgress

    /**
     * 检测完成
     *
     * @property report 生成的回归报告
     */
    data class Completed(
        val report: RegressionReport
    ) : DetectionProgress

    /**
     * 检测过程中发生错误
     *
     * @property message 错误消息
     */
    data class Error(
        val message: String
    ) : DetectionProgress
}

/**
 * 性能回归检测用例
 *
 * 执行完整的性能基准测试流程，包括：
 * 1. 初始化检测环境
 * 2. 依次运行各类型基准测试
 * 3. 分析测试结果，检测性能回归
 * 4. 生成回归报告
 *
 * 通过 Flow 实时推送检测进度，供UI层展示进度条和状态。
 *
 * @property regressionRepository 性能回归检测仓库
 */
class DetectPerformanceRegressionUseCase @Inject constructor(
    private val regressionRepository: PerformanceRegressionRepository
) {

    /**
     * 执行性能回归检测
     *
     * 返回一个 Flow，依次推送各阶段的检测进度。
     * 最终推送 Completed（成功）或 Error（失败）状态。
     *
     * @return 检测进度 Flow
     */
    operator fun invoke(): Flow<DetectionProgress> = flow {
        try {
            // 阶段1: 初始化
            emit(DetectionProgress.Initializing)

            // 阶段2: 发射各类型基准测试进度（实际运行在阶段4的 runFullBenchmark 中完成）
            val benchmarkTypes = RegressionType.entries
            benchmarkTypes.forEachIndexed { index, type ->
                emit(
                    DetectionProgress.RunningBenchmark(
                        type = type,
                        progress = (index + 1).toFloat() / benchmarkTypes.size
                    )
                )
            }

            // 阶段3: 分析结果
            emit(DetectionProgress.AnalyzingResults)

            // 阶段4: 生成完整报告（包含所有类型的回归检测）
            emit(DetectionProgress.GeneratingReport)
            val result = regressionRepository.runFullBenchmark()

            result
                .onSuccess { report ->
                    emit(DetectionProgress.Completed(report))
                }
                .onFailure { throwable ->
                    emit(DetectionProgress.Error(throwable.message ?: "生成报告失败"))
                }
        } catch (e: Exception) {
            emit(DetectionProgress.Error(e.message ?: "检测过程中发生未知错误"))
        }
    }
}
