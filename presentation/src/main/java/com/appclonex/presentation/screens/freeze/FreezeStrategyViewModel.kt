package com.appclonex.presentation.screens.freeze

import com.appclonex.domain.model.FreezeStrategy
import com.appclonex.domain.model.FreezeTriggerType
import com.appclonex.domain.repository.FreezeStrategyRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 冻结策略界面 UI 状态
 * Freeze strategy screen UI state
 *
 * @property strategies 冻结策略列表 / List of freeze strategies
 * @property isLoading 是否正在加载 / Whether data is being loaded
 * @property executingStrategyId 正在执行中的策略ID / ID of the strategy currently being executed
 * @property successMessage 成功消息 / Success message
 */
data class FreezeStrategyUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val strategies: List<FreezeStrategy> = emptyList(),
    val executingStrategyId: Long? = null,
    val successMessage: String? = null
) : BaseUiState

/**
 * 智能冻结策略管理 ViewModel
 * Smart freeze strategy management ViewModel
 *
 * 负责冻结策略的加载、创建、更新、删除、启用/禁用切换和手动执行操作。
 * 继承 BaseViewModel 获得统一异常处理（R9-VM-007 修复）。
 */
@HiltViewModel
class FreezeStrategyViewModel @Inject constructor(
    private val freezeStrategyRepository: FreezeStrategyRepository
) : BaseViewModel<FreezeStrategyUiState>(FreezeStrategyUiState()) {

    init {
        loadStrategies()
    }

    /**
     * 加载所有冻结策略
     */
    fun loadStrategies() {
        launchWithErrorHandler {
            freezeStrategyRepository.getAllStrategies()
                .collect { strategies ->
                    updateState {
                        copy(
                            strategies = strategies,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * 切换策略的启用/禁用状态
     */
    fun toggleStrategy(id: Long, enabled: Boolean) {
        launchWithErrorHandler {
            val strategy = currentState.strategies.find { it.id == id } ?: return@launchWithErrorHandler
            val updatedStrategy = strategy.copy(
                isEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )

            freezeStrategyRepository.updateStrategy(updatedStrategy)
        }
    }

    /**
     * 创建新的冻结策略
     */
    fun createStrategy(name: String, triggerType: FreezeTriggerType) {
        launchWithErrorHandler {
            val newStrategy = FreezeStrategy(
                name = name,
                triggerType = triggerType,
                isEnabled = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            freezeStrategyRepository.insertStrategy(newStrategy)
            updateState {
                copy(successMessage = "策略 \"$name\" 已创建")
            }
        }
    }

    /**
     * 更新已有的冻结策略
     */
    fun updateStrategy(strategy: FreezeStrategy) {
        launchWithErrorHandler {
            val updatedStrategy = strategy.copy(
                updatedAt = System.currentTimeMillis()
            )

            freezeStrategyRepository.updateStrategy(updatedStrategy)
            updateState {
                copy(successMessage = "策略 \"${strategy.name}\" 已更新")
            }
        }
    }

    /**
     * 删除指定冻结策略
     */
    fun deleteStrategy(id: Long) {
        launchWithErrorHandler {
            val strategy = currentState.strategies.find { it.id == id }
            freezeStrategyRepository.deleteStrategy(id)
            updateState {
                copy(successMessage = "策略 \"${strategy?.name}\" 已删除")
            }
        }
    }

    /**
     * 手动执行指定冻结策略
     */
    fun executeStrategy(id: Long) {
        launchWithErrorHandler {
            updateState { copy(executingStrategyId = id, error = null) }

            freezeStrategyRepository.executeStrategy(id)
                .onSuccess { result ->
                    updateState {
                        copy(
                            executingStrategyId = null,
                            successMessage = "策略 \"${result.strategyName}\" 执行完成: " +
                                "冻结 ${result.frozenCount} 个, 解冻 ${result.unfrozenCount} 个"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            executingStrategyId = null,
                            error = throwable.message ?: "策略执行失败"
                        )
                    }
                }
        }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        updateState { copy(successMessage = null) }
    }
}
