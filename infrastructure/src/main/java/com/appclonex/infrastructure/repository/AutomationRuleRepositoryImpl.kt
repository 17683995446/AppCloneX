package com.appclonex.infrastructure.repository

import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.model.AutoFreezeConfig
import com.appclonex.domain.repository.AutomationRuleRepository
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRuleRepositoryImpl @Inject constructor(
    private val dataStoreManager: ApplicationPreferencesDataStoreManager
) : AutomationRuleRepository {

    companion object {
        private const val KEY_AUTOMATION_RULES = "automation_rules"
        private const val KEY_AUTO_FREEZE_ENABLED = "auto_freeze_enabled"
        private const val KEY_AUTO_FREEZE_DELAY_MINUTES = "auto_freeze_delay_minutes"
        private const val KEY_AUTO_FREEZE_EXCLUDE_PACKAGES = "auto_freeze_exclude_packages"
        private const val KEY_AUTO_FREEZE_ON_SCREEN_OFF = "auto_freeze_on_screen_off"
        private const val KEY_AUTO_FREEZE_SCHEDULE_ENABLED = "auto_freeze_schedule_enabled"
        private const val KEY_AUTO_FREEZE_TIME_HOUR = "auto_freeze_time_hour"
        private const val KEY_AUTO_FREEZE_TIME_MINUTE = "auto_freeze_time_minute"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val rulesFlow = MutableStateFlow<List<AutomationRule>>(emptyList())

    /** 互斥锁，保证并发操作安全（R9-CONC-001 修复） */
    private val mutex = Mutex()

    init {
        // Load saved rules from DataStore into the in-memory list
        try {
            val savedRulesJson = dataStoreManager.getStringValue(KEY_AUTOMATION_RULES, "").first()
            if (savedRulesJson.isNotEmpty()) {
                val savedRules = json.decodeFromString<List<AutomationRule>>(savedRulesJson)
                rulesFlow.value = savedRules
                ApplicationLoggingServiceManager.d("AutomationRule", "从 DataStore 加载了 ${savedRules.size} 条规则")
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AutomationRule", "从 DataStore 加载规则失败", e)
        }
    }

    override fun getAllRules(): Flow<List<AutomationRule>> = rulesFlow.asStateFlow()

    override suspend fun getRule(id: Long): AutomationRule? {
        return rulesFlow.value.find { it.id == id }
    }

    override suspend fun saveRule(rule: AutomationRule): Result<Long> {
        return mutex.withLock {
            try {
                val newRules = rulesFlow.value.toMutableList()
                val newId = (newRules.maxOfOrNull { it.id } ?: 0) + 1
                val newRule = rule.copy(id = newId)
                newRules.add(newRule)
                rulesFlow.value = newRules
                persistRulesToDataStore(newRules)
                Result.success(newId)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("AutomationRule", "保存规则失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteRule(id: Long): Result<Boolean> {
        return mutex.withLock {
            try {
                val newRules = rulesFlow.value.filter { it.id != id }
                rulesFlow.value = newRules
                persistRulesToDataStore(newRules)
                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("AutomationRule", "删除规则失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateRule(rule: AutomationRule): Result<Boolean> {
        return mutex.withLock {
            try {
                val newRules = rulesFlow.value.map {
                    if (it.id == rule.id) rule else it
                }
                rulesFlow.value = newRules
                persistRulesToDataStore(newRules)
                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("AutomationRule", "更新规则失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getAutoFreezeConfig(): AutoFreezeConfig {
        return try {
            AutoFreezeConfig(
                enabled = dataStoreManager.getBooleanValue(KEY_AUTO_FREEZE_ENABLED, false).first(),
                delayMinutes = dataStoreManager.getIntValue(KEY_AUTO_FREEZE_DELAY_MINUTES, 5).first(),
                excludePackages = dataStoreManager.getStringListValue(KEY_AUTO_FREEZE_EXCLUDE_PACKAGES, emptyList()).first(),
                freezeOnScreenOff = dataStoreManager.getBooleanValue(KEY_AUTO_FREEZE_ON_SCREEN_OFF, true).first(),
                scheduleEnabled = dataStoreManager.getBooleanValue(KEY_AUTO_FREEZE_SCHEDULE_ENABLED, false).first(),
                freezeTimeHour = dataStoreManager.getIntValue(KEY_AUTO_FREEZE_TIME_HOUR, 23).first(),
                freezeTimeMinute = dataStoreManager.getIntValue(KEY_AUTO_FREEZE_TIME_MINUTE, 0).first()
            )
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AutomationRule", "读取自动冻结配置失败，使用默认值", e)
            AutoFreezeConfig()
        }
    }

    override suspend fun saveAutoFreezeConfig(config: AutoFreezeConfig): Result<Boolean> {
        return try {
            dataStoreManager.setBooleanValue(KEY_AUTO_FREEZE_ENABLED, config.enabled)
            dataStoreManager.setIntValue(KEY_AUTO_FREEZE_DELAY_MINUTES, config.delayMinutes)
            dataStoreManager.setStringListValue(KEY_AUTO_FREEZE_EXCLUDE_PACKAGES, config.excludePackages)
            dataStoreManager.setBooleanValue(KEY_AUTO_FREEZE_ON_SCREEN_OFF, config.freezeOnScreenOff)
            dataStoreManager.setBooleanValue(KEY_AUTO_FREEZE_SCHEDULE_ENABLED, config.scheduleEnabled)
            dataStoreManager.setIntValue(KEY_AUTO_FREEZE_TIME_HOUR, config.freezeTimeHour)
            dataStoreManager.setIntValue(KEY_AUTO_FREEZE_TIME_MINUTE, config.freezeTimeMinute)
            Result.success(true)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AutomationRule", "保存自动冻结配置失败", e)
            Result.failure(e)
        }
    }

    private suspend fun persistRulesToDataStore(rules: List<AutomationRule>) {
        try {
            val rulesJson = json.encodeToString(rules)
            dataStoreManager.setStringValue(KEY_AUTOMATION_RULES, rulesJson)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AutomationRule", "持久化规则到 DataStore 失败", e)
        }
    }

    override suspend fun deleteRulesByProfile(profileId: Int): Int {
        return mutex.withLock {
            try {
                val currentRules = rulesFlow.value
                val filteredRules = currentRules.filter { it.profileId != profileId }
                val deletedCount = currentRules.size - filteredRules.size
                if (deletedCount > 0) {
                    rulesFlow.value = filteredRules
                    persistRulesToDataStore(filteredRules)
                    ApplicationLoggingServiceManager.i(
                        "AutomationRule",
                        "已删除配置文件 $profileId 下的 $deletedCount 条自动化规则"
                    )
                }
                deletedCount
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("AutomationRule", "按配置文件删除规则失败", e)
                0
            }
        }
    }

    override suspend fun removePackageFromAllRules(packageName: String): Int {
        return mutex.withLock {
            try {
                val currentRules = rulesFlow.value
                var updatedCount = 0
                val updatedRules = currentRules.map { rule ->
                    if (packageName in rule.targetPackageNames) {
                        updatedCount++
                        rule.copy(targetPackageNames = rule.targetPackageNames - packageName)
                    } else {
                        rule
                    }
                }
                if (updatedCount > 0) {
                    rulesFlow.value = updatedRules
                    persistRulesToDataStore(updatedRules)
                    ApplicationLoggingServiceManager.i(
                        "AutomationRule",
                        "已从 $updatedCount 条规则中移除包名: $packageName"
                    )
                }
                updatedCount
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("AutomationRule", "从规则中移除包名失败", e)
                0
            }
        }
    }
}
