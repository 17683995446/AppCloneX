package com.appclonex.presentation.screen.home

import com.appclonex.domain.model.ApplicationUsageStatisticsModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * UI state for the home screen.
 */
data class HomeUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val totalProfiles: Int = 0,
    val totalClonedApps: Int = 0,
    val recentActivity: List<ApplicationUsageStatisticsModel> = emptyList()
) : BaseUiState

/**
 * ViewModel for the home screen.
 *
 * Responsibilities:
 * - Display overview of profiles and cloned apps
 * - Show recent activity
 * - Navigate to different sections
 */
@HiltViewModel
class ApplicationHomeScreenViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val clonedApplicationRepository: ClonedApplicationRepository
) : BaseViewModel<HomeUiState>(HomeUiState()) {

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        // 更新加载状态
        updateState { copy(isLoading = true) }

        // 加载配置文件数量（一次性操作）
        launchWithErrorHandler {
            val profileCount = userProfileRepository.getProfileCount()
            updateState { copy(totalProfiles = profileCount) }
        }

        // 使用单独的 launch 收集 Flow，避免阻塞
        launchWithErrorHandler {
            clonedApplicationRepository.getAllClonedAppsFlow().collectLatest { apps ->
                updateState {
                    copy(
                        totalClonedApps = apps.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}
