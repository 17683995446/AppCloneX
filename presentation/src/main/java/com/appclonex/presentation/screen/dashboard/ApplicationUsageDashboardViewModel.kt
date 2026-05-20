package com.appclonex.presentation.screen.dashboard

import com.appclonex.domain.model.ApplicationUsageStatisticsModel
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.model.RecentlyUsedApp
import com.appclonex.domain.repository.UsageStatisticsRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for usage dashboard screen.
 */
data class DashboardUiState(
    override val isLoading: Boolean = true,
    override val message: String? = null,
    override val error: String? = null,
    val statistics: ApplicationUsageStatisticsModel = ApplicationUsageStatisticsModel(),
    // 从 UsageOverview 映射的字段
    val totalClonedApps: Int = 0,
    val activeApps: Int = 0,
    val totalLaunchesToday: Int = 0,
    val totalLaunchesThisWeek: Int = 0,
    val totalLaunchesAllTime: Long = 0,
    val totalUsageTimeToday: Long = 0,
    val topUsedApps: List<TopUsedApp> = emptyList(),
    val recentActivity: List<RecentlyUsedApp> = emptyList(),
    // 从 UserProfileRepository 补充的字段
    val totalProfiles: Int = 0,
    val activeProfiles: Int = 0,
    val totalFrozenApps: Int = 0,
    val storageUsedBytes: Long = 0L
) : BaseUiState

/**
 * ViewModel for the application usage dashboard.
 *
 * Responsibilities:
 * - Display usage statistics overview
 * - Show top used apps ranking
 * - Track launch counts
 * - Monitor storage usage
 */
@HiltViewModel
class ApplicationUsageDashboardViewModel @Inject constructor(
    private val usageStatisticsRepository: UsageStatisticsRepository
) : BaseViewModel<DashboardUiState>(DashboardUiState()) {

    private var loadJob: Job? = null

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        // 取消前一次加载，防止重复 Flow 收集（R9-VM-012 修复）
        loadJob?.cancel()
        loadJob = launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            // 使用 getOverview 获取统计概览
            val overview = usageStatisticsRepository.getOverview()

            // 从 overview 中提取最常使用和最近使用的应用
            val topApps = overview.mostUsedApps
            val recentApps = overview.recentlyUsedApps

            updateState {
                copy(
                    isLoading = false,
                    totalClonedApps = overview.totalClonedApps,
                    activeApps = overview.activeApps,
                    totalLaunchesToday = overview.totalLaunchesToday,
                    totalLaunchesThisWeek = overview.totalLaunchesThisWeek,
                    totalLaunchesAllTime = overview.totalLaunchesAllTime,
                    totalUsageTimeToday = overview.totalUsageTimeToday,
                    topUsedApps = topApps,
                    recentActivity = recentApps
                )
            }
        }
    }

    fun refresh() {
        updateState { copy(isLoading = true) }
        loadStatistics()
    }

    fun getStorageFormatted(): String {
        val bytes = currentState.storageUsedBytes
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
