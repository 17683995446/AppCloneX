package com.appclonex.presentation.screen.appdetail

import com.appclonex.domain.model.CleanupSuggestion
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.domain.repository.StorageAnalysisRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import com.appclonex.domain.model.ClonedApplicationInformationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetailUiState(
    override val isLoading: Boolean = true,
    override val message: String? = null,
    override val error: String? = null,
    val app: ClonedApplicationInformationModel? = null,
    val isFrozen: Boolean = false,
    val cacheSize: String = "--",
    val dataSize: String = "--",
    val totalSize: String = "--",
    val permissions: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val isDeleted: Boolean = false
) : BaseUiState

@HiltViewModel
class ApplicationDetailScreenViewModel @Inject constructor(
    private val clonedApplicationRepository: ClonedApplicationRepository,
    private val freezingService: FreezingService,
    private val storageAnalysisRepository: StorageAnalysisRepository
) : BaseViewModel<AppDetailUiState>(AppDetailUiState()) {

    private val _isFrozen = MutableStateFlow(false)
    val isFrozen: StateFlow<Boolean> = _isFrozen.asStateFlow()

    fun loadAppDetail(packageName: String, userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            // Load app info from cloned apps
            clonedApplicationRepository.getClonedAppsFlow(userId).collect { apps ->
                val targetApp = apps.find { it.packageName == packageName }
                updateState {
                    copy(
                        app = targetApp,
                        isLoading = false,
                        permissions = emptyList()
                    )
                }
            }
        }

        // Check frozen status
        launchWithErrorHandler {
            val frozen = freezingService.isAppFrozen(packageName, userId)
            _isFrozen.value = frozen
            updateState { copy(isFrozen = frozen) }
        }

        // Load storage info using analyzeProfileStorage
        launchWithErrorHandler {
            val result = storageAnalysisRepository.analyzeProfileStorage(userId)
            result.onSuccess { storageAnalysis ->
                updateState {
                    copy(
                        cacheSize = formatSize(storageAnalysis.cacheBytes),
                        dataSize = formatSize(storageAnalysis.appDataBytes),
                        totalSize = formatSize(storageAnalysis.totalBytes)
                    )
                }
            }.onFailure { e ->
                updateState { copy(error = e.message ?: "加载存储信息失败") }
            }
        }
    }

    fun toggleFreeze(packageName: String, userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }
            val currentlyFrozen = _isFrozen.value
            val result = if (currentlyFrozen) {
                freezingService.unfreezeApp(packageName, userId)
            } else {
                freezingService.freezeApp(packageName, userId)
            }

            result.onSuccess {
                _isFrozen.value = !currentlyFrozen
                updateState {
                    copy(
                        isFrozen = !currentlyFrozen,
                        isProcessing = false,
                        message = if (currentlyFrozen) "已解冻" else "已冻结"
                    )
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        isProcessing = false,
                        error = e.message ?: "操作失败"
                    )
                }
            }
        }
    }

    fun launchApp(packageName: String, userId: Int) {
        launchWithErrorHandler {
            val apps = clonedApplicationRepository.getClonedAppsByProfile(userId)
            val targetApp = apps.find { it.packageName == packageName }
            if (targetApp != null) {
                val result = clonedApplicationRepository.launchClonedApp(targetApp)
                result.onFailure { e ->
                    updateState { copy(error = e.message ?: "启动失败") }
                }
            } else {
                updateState { copy(error = "未找到克隆应用") }
            }
        }
    }

    fun deleteApp(packageName: String, userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }
            val apps = clonedApplicationRepository.getClonedAppsByProfile(userId)
            val targetApp = apps.find { it.packageName == packageName }
            if (targetApp != null) {
                val result = clonedApplicationRepository.deleteClonedApp(targetApp)
                result.onSuccess {
                    updateState {
                        copy(
                            isProcessing = false,
                            message = "已删除",
                            isDeleted = true
                        )
                    }
                }.onFailure { e ->
                    updateState {
                        copy(
                            isProcessing = false,
                            error = e.message ?: "删除失败"
                        )
                    }
                }
            } else {
                updateState {
                    copy(
                        isProcessing = false,
                        error = "未找到克隆应用"
                    )
                }
            }
        }
    }

    fun clearCache(packageName: String, userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }
            val result = storageAnalysisRepository.performCleanup(
                profileId = userId,
                types = listOf(CleanupSuggestion.CleanupType.CACHE)
            )
            result.onSuccess { bytesCleared ->
                updateState {
                    copy(
                        isProcessing = false,
                        message = "已清除 ${formatSize(bytesCleared)} 缓存"
                    )
                }
                // Reload storage info
                loadAppDetail(packageName, userId)
            }.onFailure { e ->
                updateState {
                    copy(isProcessing = false, error = e.message ?: "清除失败")
                }
            }
        }
    }

    fun clearAppData(packageName: String, userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }
            val result = storageAnalysisRepository.performCleanup(
                profileId = userId,
                types = listOf(
                    CleanupSuggestion.CleanupType.CACHE,
                    CleanupSuggestion.CleanupType.TEMP_FILES,
                    CleanupSuggestion.CleanupType.LOG_FILES
                )
            )
            result.onSuccess {
                updateState {
                    copy(isProcessing = false, message = "已清除应用数据")
                }
                loadAppDetail(packageName, userId)
            }.onFailure { e ->
                updateState {
                    copy(isProcessing = false, error = e.message ?: "清除失败")
                }
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups.coerceAtMost(units.size - 1)])
    }
}
