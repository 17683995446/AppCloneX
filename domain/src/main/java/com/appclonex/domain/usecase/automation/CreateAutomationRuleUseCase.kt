package com.appclonex.domain.usecase.automation

import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.repository.AutomationRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateAutomationRuleUseCase @Inject constructor(
    private val repository: AutomationRuleRepository
) {
    suspend operator fun invoke(rule: AutomationRule): Result<Long> {
        require(rule.profileId > 0) { "配置文件ID必须大于0" }
        require(rule.triggerValue.isNotBlank()) { "触发条件不能为空" }
        return repository.saveRule(rule)
    }
}
