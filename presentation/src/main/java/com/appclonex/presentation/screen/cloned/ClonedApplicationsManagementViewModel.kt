package com.appclonex.presentation.screen.cloned

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * UI state for cloned applications management screen.
 */
data class ClonedApplicationsUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val clonedApps: List<ClonedApplicationInformationModel> = emptyList(),
    val selectedProfileId: Int = 0,
    val searchQuery: String = "",
    val isCloning: Boolean = false
) : BaseUiState

/**
 * ViewModel for managing cloned applications.
 *
 * Responsibilities:
 * - List all cloned applications
 * - Search and filter apps
 * - Delete cloned apps
 * - Clone new apps
 */
@HiltViewModel
class ClonedApplicationsManagementViewModel @Inject constructor(
    private val clonedApplicationRepository: ClonedApplicationRepository
) : BaseViewModel<ClonedApplicationsUiState>(ClonedApplicationsUiState()) {

    init {
        loadClonedApps()
    }

    fun loadClonedApps(profileUserId: Int = currentState.selectedProfileId) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            clonedApplicationRepository.getClonedAppsFlow(profileUserId).collectLatest { apps ->
                updateState {
                    copy(
                        clonedApps = apps.filter {
                            searchQuery.isEmpty() ||
                            it.appName.contains(searchQuery, ignoreCase = true) ||
                            it.packageName.contains(searchQuery, ignoreCase = true)
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setSelectedProfile(profileUserId: Int) {
        updateState { copy(selectedProfileId = profileUserId) }
        loadClonedApps(profileUserId)
    }

    fun setSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
        loadClonedApps()
    }

    fun deleteClonedApp(app: ClonedApplicationInformationModel) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            clonedApplicationRepository.deleteClonedApp(app)
                .onSuccess {
                    updateState { copy(message = "应用已删除", isLoading = false) }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "删除失败",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun cloneApp(packageName: String) {
        launchWithErrorHandler {
            updateState { copy(isCloning = true) }

            clonedApplicationRepository.cloneApp(packageName, currentState.selectedProfileId)
                .onSuccess {
                    updateState { copy(message = "克隆成功", isCloning = false) }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "克隆失败",
                            isCloning = false
                        )
                    }
                }
        }
    }

    fun launchApp(app: ClonedApplicationInformationModel) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            clonedApplicationRepository.launchClonedApp(app)
                .onSuccess {
                    updateState { copy(message = "正在启动...", isLoading = false) }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "启动失败",
                            isLoading = false
                        )
                    }
                }
        }
    }
}
