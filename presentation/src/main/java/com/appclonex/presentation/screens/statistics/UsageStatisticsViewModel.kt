package com.appclonex.presentation.screens.statistics

import com.appclonex.domain.model.StatisticsPeriod
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.model.UsageOverview
import com.appclonex.domain.usecase.statistics.GetTopUsedAppsUseCase
import com.appclonex.domain.usecase.statistics.GetUsageOverviewUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 使用统计界面 UI 状态
 * Usage statistics screen UI state
 *
 * @property overview 使用统计概览 / Usage statistics overview
 * @property topApps 最常使用应用列表 / Top used apps list
 * @property selectedPeriod 当前选中的统计时间段 / Currently selected statistics period
 * @property isLoading 是否正在加载 / Whether data is being loaded
 * @property error 错误消息 / Error message
 * @property message 提示消息 / Info message
 */
data class UsageStatisticsUiState(
    val overview: UsageOverview = UsageOverview(),
    val topApps: List<TopUsedApp> = emptyList(),
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.ALL_TIME,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    override val message: String? = null
) : BaseUiState

/**
 * 使用统计界面 ViewModel
 * ViewModel for the usage statistics screen
 *
 * 负责加载和展示应用使用统计概览、最常使用应用排行和使用趋势数据。
 * Manages loading and displaying app usage overview, top used app ranking and usage trend data.
 *
 * @param getUsageOverviewUseCase 获取使用统计概览用例 / Get usage overview use case
 * @param getTopUsedAppsUseCase 获取最常使用应用用例 / Get top used apps use case
 */
@HiltViewModel
class UsageStatisticsViewModel @Inject constructor(
    private val getUsageOverviewUseCase: GetUsageOverviewUseCase,
    private val getTopUsedAppsUseCase: GetTopUsedAppsUseCase
) : BaseViewModel<UsageStatisticsUiState>(UsageStatisticsUiState()) {

    init {
        loadStatistics(currentState.selectedPeriod)
    }

    /**
     * 加载指定时间段的使用统计数据
     * Load usage statistics for the specified period
     *
     * 并行加载概览数据和最常使用应用列表。
     * Loads overview data and top used apps list in parallel.
     *
     * @param period 统计时间段 / Statistics period
     */
    fun loadStatistics(period: StatisticsPeriod) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true, error = null) }

            // 并行加载概览和排行数据
            val overviewDeferred = kotlinx.coroutines.async {
                getUsageOverviewUseCase(period)
            }
            val topAppsDeferred = kotlinx.coroutines.async {
                getTopUsedAppsUseCase(limit = 10, period = period)
            }

            val overview = overviewDeferred.await()
            val topApps = topAppsDeferred.await()

            updateState {
                copy(
                    overview = overview,
                    topApps = topApps,
                    selectedPeriod = period,
                    isLoading = false
                )
            }
        }
    }

    /**
     * 选择统计时间段
     * Select a statistics period
     *
     * 切换时间段后自动重新加载统计数据。
     * Automatically reloads statistics data after switching period.
     *
     * @param period 要切换到的统计时间段 / Statistics period to switch to
     */
    fun selectPeriod(period: StatisticsPeriod) {
        if (period == currentState.selectedPeriod) return
        loadStatistics(period)
    }
}
