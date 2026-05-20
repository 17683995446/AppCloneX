package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 批量克隆配置
 * 支持一次选择多个应用克隆到指定配置文件
 */
@Serializable
data class BatchCloneConfig(
    val targetProfileId: Int = 0,
    val targetProfileName: String = "",
    val selectedPackages: List<String> = emptyList(),
    val options: BatchCloneOptions = BatchCloneOptions()
)

@Serializable
data class BatchCloneOptions(
    val skipExisting: Boolean = true,       // 跳过已克隆的应用
    val autoFreeze: Boolean = false,        // 克隆后自动冻结
    val copySettings: Boolean = false,      // 复制原应用设置
    val parallelClone: Boolean = true,      // 并行克隆
    val maxParallel: Int = 3,               // 最大并行数
    val showProgress: Boolean = true,       // 显示进度
    val notifyOnComplete: Boolean = true    // 完成后通知
)

/**
 * 批量克隆进度
 *
 * R12-002 修复：添加 skippedApps 计数器
 */
data class BatchCloneProgress(
    val totalApps: Int = 0,
    val completedApps: Int = 0,
    val failedApps: Int = 0,
    val skippedApps: Int = 0,  // R12-002: 新增跳过计数
    val currentApp: String = "",
    val currentStatus: CloneStatus = CloneStatus.IDLE,
    val results: List<SingleCloneResult> = emptyList(),
    val startTime: Long = 0,
    val estimatedTimeRemaining: Long = 0
)

enum class CloneStatus {
    IDLE, PREPARING, CLONING, INSTALLING, CONFIGURING, COMPLETED, FAILED, CANCELLED
}

/**
 * 单个克隆结果
 *
 * R12-002 修复：添加跳过标识和自动冻结状态
 */
data class SingleCloneResult(
    val packageName: String,
    val appName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val skipped: Boolean = false,  // R12-002: 标识是否被跳过
    val autoFreezeSuccess: Boolean = true,  // R12-002: 自动冻结是否成功
    val autoFreezeError: String? = null  // R12-002: 自动冻结错误信息
)

/**
 * 批量克隆结果摘要
 */
data class BatchCloneSummary(
    val totalApps: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
    val totalDurationMs: Long = 0,
    val results: List<SingleCloneResult> = emptyList()
)
