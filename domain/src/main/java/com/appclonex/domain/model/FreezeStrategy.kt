package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 智能冻结策略模型
 * 支持多种冻结触发条件和规则
 */
@Serializable
data class FreezeStrategy(
    val id: Long = 0,
    val name: String = "",
    val isEnabled: Boolean = true,
    val triggerType: FreezeTriggerType = FreezeTriggerType.MANUAL,
    val conditions: List<FreezeCondition> = emptyList(),
    val targetApps: List<String> = emptyList(), // empty = all cloned apps
    val targetProfiles: List<Int> = emptyList(), // empty = all profiles
    val schedule: FreezeSchedule? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 冻结触发类型枚举
 * 定义策略被触发的各种场景
 */
@Serializable
enum class FreezeTriggerType {
    MANUAL,           // 手动触发
    SCHEDULE,         // 定时触发
    SCREEN_OFF,       // 熄屏触发
    INACTIVITY,       // 不活跃触发（N分钟未使用）
    BATTERY_LOW,      // 低电量触发
    WIFI_DISCONNECT,  // WiFi断开触发
    APP_SWITCH,       // 切换应用时冻结上一个
    COMPOUND          // 组合条件
}

/**
 * 冻结条件模型
 * 定义单个冻结条件的类型、值、阈值和比较运算符
 */
@Serializable
data class FreezeCondition(
    val type: ConditionType,
    val value: String = "",
    val threshold: Long = 0,
    val operator: ConditionOperator = ConditionOperator.EQUALS
)

/**
 * 条件类型枚举
 * 定义可用于冻结判断的各种系统状态条件
 */
@Serializable
enum class ConditionType {
    BATTERY_LEVEL,    // 电量百分比
    TIME_OF_DAY,      // 一天中的时间
    DAY_OF_WEEK,      // 星期几
    WIFI_CONNECTED,   // WiFi是否连接
    SCREEN_STATE,     // 屏幕状态
    APP_INACTIVITY,   // 应用不活跃时间
    MEMORY_USAGE,     // 内存使用率
    STORAGE_USAGE     // 存储使用率
}

/**
 * 条件运算符枚举
 * 定义条件判断时使用的比较运算方式
 */
@Serializable
enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    BETWEEN,
    CONTAINS
}

/**
 * 冻结调度配置
 * 定义定时冻结/解冻的时间段和重复规则
 */
@Serializable
data class FreezeSchedule(
    val startTimeHour: Int = 0,    // 0-23
    val startTimeMinute: Int = 0,  // 0-59
    val endTimeHour: Int = 23,
    val endTimeMinute: Int = 59,
    val repeatDays: List<Int> = listOf(1,2,3,4,5,6,7), // 1=Monday ... 7=Sunday
    val freezeOnStart: Boolean = true,
    val unfreezeOnEnd: Boolean = true
)

/**
 * 冻结执行结果
 * 记录单次策略执行的详细信息，包括冻结/解冻数量、失败列表和耗时
 */
data class FreezeExecutionResult(
    val strategyId: Long,
    val strategyName: String,
    val frozenCount: Int = 0,
    val unfrozenCount: Int = 0,
    val failedApps: List<String> = emptyList(),
    val executedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0
)
