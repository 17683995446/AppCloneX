/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测界面 ViewModel
 * 管理检测进度、报告列表、告警列表、趋势数据等UI状态
 */

package com.appclonex.presentation.screens.dashboard

import com.appclonex.domain.model.PerformanceAlert
import com.appclonex.domain.model.RegressionReport
import com.appclonex.domain.model.RegressionThreshold
import com.appclonex.domain.model.RegressionTrend
import com.appclonex.domain.model.RegressionType
import com.appclonex.domain.repository.PerformanceRegressionRepository
import com.appclonex.domain.usecase.monitoring.DetectPerformanceRegressionUseCase
import com.appclonex.domain.usecase.monitoring.DetectionProgress
import com.appclonex.domain.usecase.monitoring.ExportFormat
import com.appclonex.domain.usecase.monitoring.ExportPerformanceReportUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 性能回归检测界面 UI 状态
 *
 * @property latestReport 最新的回归报告
 * @property reportHistory 历史报告列表
 * @property alerts 告警列表
 * @property trendData 趋势数据映射
 * @property thresholds 阈值配置映射
 * @property detectionProgress 当前检测进度
 * @property isDetecting 是否正在执行检测
 * @property exportedContent 导出的报告内容
 * @property showThresholdDialog 是否显示阈值设置对话框
 * @property showExportDialog 是否显示导出格式选择对话框
 * @property selectedTrendType 当前选中的趋势类型
 * @property error 错误消息
 * @property message 提示消息
 */
data class PerformanceRegressionUiState(
    val latestReport: RegressionReport? = null,
    val reportHistory: List<RegressionReport> = emptyList(),
    val alerts: List<PerformanceAlert> = emptyList(),
    val trendData: Map<RegressionType, RegressionTrend> = emptyMap(),
    val thresholds: Map<RegressionType, RegressionThreshold> = emptyMap(),
    val detectionProgress: DetectionProgress? = null,
    val isDetecting: Boolean = false,
    val exportedContent: String? = null,
    val showThresholdDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val selectedTrendType: RegressionType = RegressionType.COLD_START,
    override val error: String? = null,
    override val message: String? = null
) : BaseUiState

/**
 * 性能回归检测界面 ViewModel
 *
 * 负责管理性能回归检测的完整UI流程：
 * - 运行性能回归检测并展示进度
 * - 加载和展示历史报告
 * - 管理告警列表（标记已读、忽略）
 * - 加载和展示趋势数据
 * - 导出报告（JSON/CSV/Markdown）
 * - 管理阈值配置
 *
 * @property detectRegressionUseCase 性能回归检测用例
 * @property exportReportUseCase 导出报告用例
 * @property regressionRepository 性能回归仓库
 */
@HiltViewModel
class PerformanceRegressionViewModel @Inject constructor(
    private val detectRegressionUseCase: DetectPerformanceRegressionUseCase,
    private val exportReportUseCase: ExportPerformanceReportUseCase,
    private val regressionRepository: PerformanceRegressionRepository
) : BaseViewModel<PerformanceRegressionUiState>(PerformanceRegressionUiState()) {

    init {
        loadInitialData()
    }

    /**
     * 加载初始数据
     *
     * 并行加载最新报告、历史记录、告警和阈值配置
     */
    private fun loadInitialData() {
        launchWithErrorHandler {
            updateState { copy(isDetecting = false) }

            // 并行加载各项数据
            val reportResult = regressionRepository.getLatestReport()
            val historyResult = regressionRepository.getRegressionHistory(limit = 20)
            val alertsResult = regressionRepository.getAlerts()
            val thresholdsResult = regressionRepository.getThresholds()

            reportResult.onSuccess { report ->
                updateState { copy(latestReport = report) }
            }

            historyResult.onSuccess { history ->
                updateState { copy(reportHistory = history) }
                // 加载趋势数据
                loadTrendData()
            }

            alertsResult.onSuccess { alerts ->
                updateState { copy(alerts = alerts) }
            }

            thresholdsResult.onSuccess { thresholds ->
                updateState { copy(thresholds = thresholds) }
            }

            // 合并错误处理
            val errors = listOfNotNull(
                reportResult.exceptionOrNull()?.message,
                historyResult.exceptionOrNull()?.message,
                alertsResult.exceptionOrNull()?.message,
                thresholdsResult.exceptionOrNull()?.message
            )
            if (errors.isNotEmpty()) {
                updateState { copy(error = errors.first()) }
            }
        }
    }

    /**
     * 加载趋势数据
     *
     * 为所有回归类型加载趋势分析数据
     */
    private fun loadTrendData() {
        launchWithErrorHandler {
            val trendMap = mutableMapOf<RegressionType, RegressionTrend>()
            RegressionType.entries.forEach { type ->
                regressionRepository.getRegressionTrend(type)
                    .onSuccess { trend ->
                        trendMap[type] = trend
                    }
            }
            updateState { copy(trendData = trendMap) }
        }
    }

    /**
     * 运行完整的性能回归检测
     *
     * 通过 Flow 接收检测进度并实时更新UI状态
     */
    fun runDetection() {
        launchWithErrorHandler {
            updateState {
                copy(
                    isDetecting = true,
                    detectionProgress = DetectionProgress.Initializing,
                    error = null
                )
            }

            detectRegressionUseCase().collect { progress ->
                updateState { copy(detectionProgress = progress) }

                when (progress) {
                    is DetectionProgress.Completed -> {
                        updateState {
                            copy(
                                isDetecting = false,
                                latestReport = progress.report,
                                message = "检测完成，总体评分: ${progress.report.overallScore}"
                            )
                        }
                        // 重新加载历史和告警
                        loadInitialData()
                    }
                    is DetectionProgress.Error -> {
                        updateState {
                            copy(
                                isDetecting = false,
                                error = progress.message
                            )
                        }
                    }
                    else -> {
                        // 其他进度状态已在上面更新
                    }
                }
            }
        }
    }

    /**
     * 导出报告
     *
     * @param format 导出格式（JSON/CSV/Markdown）
     */
    fun exportReport(format: ExportFormat) {
        launchWithErrorHandler {
            val report = currentState.latestReport
            if (report == null) {
                updateState { copy(error = "没有可导出的报告，请先运行检测") }
                return@launchWithErrorHandler
            }

            val result = exportReportUseCase(format, report)
            result
                .onSuccess { content ->
                    updateState {
                        copy(
                            exportedContent = content,
                            showExportDialog = false,
                            message = "报告已导出为${format.name}格式"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState { copy(error = "导出失败: ${throwable.message}") }
                }
        }
    }

    /**
     * 标记告警为已读
     *
     * @param alertId 告警ID
     */
    fun markAlertRead(alertId: String) {
        launchWithErrorHandler {
            regressionRepository.markAlertRead(alertId)
                .onSuccess {
                    // 重新加载告警列表
                    regressionRepository.getAlerts()
                        .onSuccess { alerts ->
                            updateState { copy(alerts = alerts) }
                        }
                }
                .onFailure { throwable ->
                    updateState { copy(error = throwable.message) }
                }
        }
    }

    /**
     * 忽略告警
     *
     * @param alertId 告警ID
     */
    fun dismissAlert(alertId: String) {
        launchWithErrorHandler {
            regressionRepository.dismissAlert(alertId)
                .onSuccess {
                    regressionRepository.getAlerts()
                        .onSuccess { alerts ->
                            updateState { copy(alerts = alerts) }
                        }
                }
                .onFailure { throwable ->
                    updateState { copy(error = throwable.message) }
                }
        }
    }

    /**
     * 更新阈值配置
     *
     * @param type 回归类型
     * @param warningThreshold 警告阈值
     * @param criticalThreshold 关键阈值
     */
    fun updateThreshold(
        type: RegressionType,
        warningThreshold: Float,
        criticalThreshold: Float
    ) {
        launchWithErrorHandler {
            regressionRepository.updateThreshold(type, warningThreshold, criticalThreshold)
                .onSuccess {
                    regressionRepository.getThresholds()
                        .onSuccess { thresholds ->
                            updateState {
                                copy(
                                    thresholds = thresholds,
                                    showThresholdDialog = false,
                                    message = "阈值已更新"
                                )
                            }
                        }
                }
                .onFailure { throwable ->
                    updateState { copy(error = throwable.message) }
                }
        }
    }

    /**
     * 选择趋势类型
     *
     * @param type 要查看的回归类型
     */
    fun selectTrendType(type: RegressionType) {
        updateState { copy(selectedTrendType = type) }
    }

    /**
     * 显示/隐藏阈值设置对话框
     */
    fun toggleThresholdDialog(show: Boolean) {
        updateState { copy(showThresholdDialog = show) }
    }

    /**
     * 显示/隐藏导出格式选择对话框
     */
    fun toggleExportDialog(show: Boolean) {
        updateState { copy(showExportDialog = show) }
    }

    /**
     * 清除导出内容
     */
    fun clearExportedContent() {
        updateState { copy(exportedContent = null) }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadInitialData()
    }
}
