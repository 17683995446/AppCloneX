package com.appclonex.presentation.screen.freeze

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * UI state for application freezing screen.
 */
data class FreezerUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val clonedApps: List<ClonedApplicationInformationModel> = emptyList(),
    val frozenApps: Map<String, Boolean> = emptyMap(),
    val frozenCount: Int = 0,
    val totalCount: Int = 0,
    val currentUserId: Int = 0,
    val isProcessing: Boolean = false
) : BaseUiState

/**
 * ViewModel for managing frozen applications.
 *
 * Responsibilities:
 * - List applications eligible for freezing
 * - Toggle freeze state for apps
 * - Show freeze statistics
 */
@HiltViewModel
class ApplicationFreezingScreenViewModel @Inject constructor(
    private val clonedApplicationRepository: ClonedApplicationRepository,
    private val freezingService: FreezingService
) : BaseViewModel<FreezerUiState>(FreezerUiState()) {

    init {
        loadApps()
    }

    fun loadApps(userId: Int = currentState.currentUserId) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true, currentUserId = userId) }

            clonedApplicationRepository.getClonedAppsFlow(userId).collectLatest { apps ->
                updateState {
                    copy(
                        clonedApps = apps,
                        totalCount = apps.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleFreeze(packageName: String) {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }

            val userId = currentState.currentUserId
            val isCurrentlyFrozen = currentState.frozenApps[packageName] ?: false

            val result = if (isCurrentlyFrozen) {
                freezingService.unfreezeApp(packageName, userId)
            } else {
                freezingService.freezeApp(packageName, userId)
            }

            result.onSuccess {
                updateState {
                    val newFrozenApps = frozenApps.toMutableMap()
                    newFrozenApps[packageName] = !isCurrentlyFrozen
                    val newCount = newFrozenApps.values.count { it }

                    copy(
                        frozenApps = newFrozenApps,
                        frozenCount = newCount,
                        message = if (!isCurrentlyFrozen) "应用已冻结" else "应用已解冻",
                        isProcessing = false
                    )
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        error = e.message ?: "操作失败",
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun freezeAll() {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }

            val userId = currentState.currentUserId
            val result = freezingService.freezeAllApps(userId)

            result.onSuccess { count ->
                if (count > 0) {
                    val appsToFreeze = currentState.clonedApps.map { it.packageName }
                    val newFrozenApps = appsToFreeze.associateWith { true }
                    updateState {
                        copy(
                            frozenApps = newFrozenApps,
                            frozenCount = appsToFreeze.size,
                            message = "已冻结 ${appsToFreeze.size} 个应用",
                            isProcessing = false
                        )
                    }
                } else {
                    updateState {
                        copy(
                            error = "批量冻结失败",
                            isProcessing = false
                        )
                    }
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        error = e.message ?: "批量冻结失败",
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun unfreezeAll() {
        launchWithErrorHandler {
            updateState { copy(isProcessing = true) }

            val userId = currentState.currentUserId
            val result = freezingService.unfreezeAllApps(userId)

            result.onSuccess { count ->
                if (count > 0) {
                    updateState {
                        copy(
                            frozenApps = emptyMap(),
                            frozenCount = 0,
                            message = "已解冻所有应用",
                            isProcessing = false
                        )
                    }
                } else {
                    updateState {
                        copy(
                            error = "批量解冻失败",
                            isProcessing = false
                        )
                    }
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        error = e.message ?: "批量解冻失败",
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun refresh() {
        loadApps()
    }
}
