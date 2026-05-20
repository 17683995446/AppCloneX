package com.appclonex.domain.repository

import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.model.AutoFreezeConfig
import kotlinx.coroutines.flow.Flow

/**
 * 自动化规则仓库接口
 */
interface AutomationRuleRepository {
    fun getAllRules(): Flow<List<AutomationRule>>
    suspend fun getRule(id: Long): AutomationRule?
    suspend fun saveRule(rule: AutomationRule): Result<Long>
    suspend fun deleteRule(id: Long): Result<Boolean>
    suspend fun updateRule(rule: AutomationRule): Result<Boolean>

    suspend fun getAutoFreezeConfig(): AutoFreezeConfig
    suspend fun saveAutoFreezeConfig(config: AutoFreezeConfig): Result<Boolean>

    /**
     * 删除指定配置文件下的所有自动化规则
     * @param profileId 配置文件ID
     * @return 被删除的规则数量
     */
    suspend fun deleteRulesByProfile(profileId: Int): Int

    /**
     * 从所有规则中移除指定的应用包名
     * @param packageName 要移除的应用包名
     * @return 被更新的规则数量
     */
    suspend fun removePackageFromAllRules(packageName: String): Int
}
