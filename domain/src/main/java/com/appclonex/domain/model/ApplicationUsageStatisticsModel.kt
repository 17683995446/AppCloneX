package com.appclonex.domain.model

/**
 * 使用统计数据模型
 * 用于仪表盘展示聚合后的使用信息
 */
data class ApplicationUsageStatisticsModel(
    val totalProfiles: Int = 0,
    val activeProfiles: Int = 0,
    val totalClonedApps: Int = 0,
    val totalLaunches: Int = 0,
    val totalFrozenApps: Int = 0,
    val mostUsedApp: String? = null,
    val mostUsedAppLaunches: Int = 0,
    val topUsedApps: List<TopUsedApp> = emptyList(),
    val recentActivity: List<ActivityRecord> = emptyList(),
    val storageUsedBytes: Long = 0L
)

/**
 * 活动记录
 * 代表一次用户操作事件
 */
data class ActivityRecord(
    val packageName: String,
    val appName: String,
    val action: String,  // "launch" / "clone" / "freeze" / "unfreeze" / "delete"
    val timestamp: Long
) {
    /**
     * 获取操作的中文描述
     */
    fun getActionLabel(): String {
        return when (action) {
            "launch" -> "启动了"
            "clone" -> "克隆了"
            "freeze" -> "冻结了"
            "unfreeze" -> "解冻了"
            "delete" -> "删除了"
            else -> action
        }
    }

    /**
     * 获取操作对应的图标名称
     */
    fun getActionIconName(): String {
        return when (action) {
            "launch" -> "TouchApp"
            "clone" -> "ContentCopy"
            "freeze" -> "AcUnit"
            "unfreeze" -> "PlayArrow"
            "delete" -> "Delete"
            else -> "Info"
        }
    }
}
