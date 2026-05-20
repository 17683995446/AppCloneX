package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 自动化规则
 */
@Serializable
data class AutomationRule(
    val id: Long = 0,
    val profileId: Int,
    val ruleType: RuleType,
    val triggerType: TriggerType,
    val triggerValue: String,
    val targetPackageNames: List<String> = emptyList(),
    val action: ActionType,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null,
    val description: String = ""
) {
    enum class RuleType {
        AUTO_FREEZE,       // 自动冻结
        AUTO_UNFREEZE,     // 自动解冻
        AUTO_SWITCH        // 自动切换配置
    }

    enum class TriggerType {
        TIME_BASED,        // 基于时间
        SCREEN_OFF,        // 屏幕关闭时
        APP_BACKGROUND,    // 应用进入后台N分钟后
        SCHEDULE           // 定时计划
    }

    enum class ActionType {
        FREEZE,            // 冻结
        UNFREEZE,          // 解冻
        FREEZE_ALL,        // 冻结全部
        UNFREEZE_ALL,      // 解冻全部
        SWITCH_PROFILE     // 切换配置
    }
}

/**
 * 自动冻结配置
 */
@Serializable
data class AutoFreezeConfig(
    val enabled: Boolean = false,
    val delayMinutes: Int = 5,          // 应用进入后台N分钟后自动冻结
    val excludePackages: List<String> = emptyList(),  // 排除的应用
    val freezeOnScreenOff: Boolean = true,  // 屏幕关闭时冻结
    val scheduleEnabled: Boolean = false,  // 定时冻结
    val freezeTimeHour: Int = 23,         // 定时冻结时间（小时）
    val freezeTimeMinute: Int = 0         // 定时冻结时间（分钟）
)
