package com.appclonex.presentation.screen.sandbox

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UI state for sandbox management screen.
 */
data class SandboxUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val sandboxes: List<SandboxInfo> = emptyList(),
    val selectedSandbox: SandboxInfo? = null,
    val installedApps: List<String> = emptyList()
) : BaseUiState

/**
 * Information about a sandbox environment.
 */
data class SandboxInfo(
    val userId: Int,
    val name: String,
    val appCount: Int,
    val isActive: Boolean = false
)

/**
 * ViewModel for sandbox environment management.
 *
 * Responsibilities:
 * - List all sandbox environments
 * - Manage sandbox apps
 * - Configure sandbox settings
 */
@HiltViewModel
class SandboxManagementScreenViewModel @Inject constructor(
    private val clonedApplicationRepository: ClonedApplicationRepository
) : BaseViewModel<SandboxUiState>(SandboxUiState()) {

    init {
        loadSandboxes()
    }

    fun loadSandboxes() {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            clonedApplicationRepository.getAllClonedAppsFlow().collectLatest { apps ->
                val sandboxes = apps
                    .groupBy { it.profileUserId }
                    .map { (userId, appsInSandbox) ->
                        SandboxInfo(
                            userId = userId,
                            name = "Profile $userId",
                            appCount = appsInSandbox.size,
                            isActive = false
                        )
                    }

                updateState {
                    copy(
                        sandboxes = sandboxes,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectSandbox(sandbox: SandboxInfo) {
        launchWithErrorHandler {
            updateState { copy(selectedSandbox = sandbox, isLoading = true) }

            clonedApplicationRepository.getClonedAppsFlow(sandbox.userId).collectLatest { apps ->
                updateState {
                    copy(
                        installedApps = apps.map { it.packageName },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun installAppToSandbox(packageName: String, sandboxUserId: Int) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            clonedApplicationRepository.cloneApp(packageName, sandboxUserId)
                .onSuccess {
                    updateState {
                        copy(
                            message = "应用已安装到沙盒",
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "安装失败",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun removeAppFromSandbox(packageName: String, sandboxUserId: Int) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            // 通过 getClonedAppsByProfile 获取该沙盒下的所有应用，找到目标应用
            val apps = clonedApplicationRepository.getClonedAppsByProfile(sandboxUserId)
            val appToDelete = apps.find { it.packageName == packageName }

            if (appToDelete != null) {
                clonedApplicationRepository.deleteClonedApp(appToDelete)
                    .onSuccess {
                        updateState {
                            copy(
                                message = "应用已从沙盒移除",
                                isLoading = false
                            )
                        }
                    }
                    .onFailure { exception ->
                        updateState {
                            copy(
                                error = exception.message ?: "移除失败",
                                isLoading = false
                            )
                        }
                    }
            } else {
                updateState {
                    copy(
                        error = "未找到要移除的应用: $packageName",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearSandbox(userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            // 使用 first() 获取快照，避免在 collectLatest 中执行删除操作（R9-VM-006 修复）
            val apps = clonedApplicationRepository.getClonedAppsFlow(userId).first()
            var successCount = 0
            var failedCount = 0

            for (app in apps) {
                val result = clonedApplicationRepository.deleteClonedApp(app)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failedCount++
                }
            }

            val message = if (failedCount == 0) {
                "沙盒已清空（共 $successCount 个应用）"
            } else {
                "清空完成：成功 $successCount 个，失败 $failedCount 个"
            }

            updateState {
                copy(
                    message = message,
                    isLoading = false,
                    error = if (failedCount > 0) "部分应用删除失败" else null
                )
            }
        }
    }
}
