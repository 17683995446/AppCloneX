package com.appclonex.presentation.screen.automation

import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.model.AutoFreezeConfig
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.AutomationRuleRepository
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

data class AutomationRulesUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val rules: List<AutomationRule> = emptyList(),
    val autoFreezeConfig: AutoFreezeConfig = AutoFreezeConfig(),
    val availableApps: List<ClonedApplicationInformationModel> = emptyList(),
    val isCreatingRule: Boolean = false,
    val showCreateDialog: Boolean = false
) : BaseUiState

@HiltViewModel
class AutomationRulesScreenViewModel @Inject constructor(
    private val automationRuleRepository: AutomationRuleRepository,
    private val clonedApplicationRepository: ClonedApplicationRepository
) : BaseViewModel<AutomationRulesUiState>(AutomationRulesUiState()) {

    init {
        loadRules()
        loadAutoFreezeConfig()
        loadAvailableApps()
    }

    fun loadRules() {
        launchWithErrorHandler {
            automationRuleRepository.getAllRules().collectLatest { rules ->
                updateState { copy(rules = rules, isLoading = false) }
            }
        }
    }

    fun loadAutoFreezeConfig() {
        launchWithErrorHandler {
            val config = automationRuleRepository.getAutoFreezeConfig()
            updateState { copy(autoFreezeConfig = config) }
        }
    }

    fun loadAvailableApps() {
        launchWithErrorHandler {
            clonedApplicationRepository.getAllClonedAppsFlow().collectLatest { apps ->
                updateState { copy(availableApps = apps) }
            }
        }
    }

    fun createRule(rule: AutomationRule) {
        launchWithErrorHandler {
            updateState { copy(isCreatingRule = true) }
            val result = automationRuleRepository.saveRule(rule)
            result.onSuccess {
                updateState {
                    copy(
                        isCreatingRule = false,
                        showCreateDialog = false,
                        message = "规则创建成功"
                    )
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        isCreatingRule = false,
                        error = e.message ?: "创建规则失败"
                    )
                }
            }
        }
    }

    fun deleteRule(ruleId: Long) {
        launchWithErrorHandler {
            val result = automationRuleRepository.deleteRule(ruleId)
            result.onSuccess {
                updateState { copy(message = "规则已删除") }
            }.onFailure { e ->
                updateState { copy(error = e.message ?: "删除规则失败") }
            }
        }
    }

    fun toggleRule(ruleId: Long, enabled: Boolean) {
        launchWithErrorHandler {
            val currentRule = currentState.rules.find { it.id == ruleId } ?: return@launchWithErrorHandler
            val updatedRule = currentRule.copy(isEnabled = enabled)
            val result = automationRuleRepository.updateRule(updatedRule)
            result.onFailure { e ->
                updateState { copy(error = e.message ?: "更新规则失败") }
            }
        }
    }

    fun updateAutoFreezeConfig(config: AutoFreezeConfig) {
        launchWithErrorHandler {
            val result = automationRuleRepository.saveAutoFreezeConfig(config)
            result.onSuccess {
                updateState { copy(autoFreezeConfig = config) }
            }.onFailure { e ->
                updateState { copy(error = e.message ?: "更新配置失败") }
            }
        }
    }

    fun showCreateDialog() {
        updateState { copy(showCreateDialog = true) }
    }

    fun dismissCreateDialog() {
        updateState { copy(showCreateDialog = false) }
    }
}
