package com.appclonex.domain.service

import kotlinx.coroutines.flow.StateFlow

/**
 * 克隆操作通知服务接口
 *
 * 定义前台服务在 Domain 层的抽象，
 * 使 Presentation 层可以通过接口控制前台服务，
 * 而不直接依赖 Infrastructure 层的具体实现。
 */
interface CloneOperationNotificationService {

    /**
     * 操作状态流
     */
    val operationState: StateFlow<CloneOperationNotificationState>

    /**
     * 启动前台服务
     *
     * @param operationType 操作类型 (clone, freeze, unfreeze, delete, batch_freeze, batch_unfreeze)
     * @param packageName 目标应用包名
     */
    fun startForegroundOperation(operationType: String, packageName: String = "")

    /**
     * 更新操作进度
     *
     * @param current 当前进度
     * @param total 总数
     */
    fun updateProgress(current: Int, total: Int)

    /**
     * 更新状态消息
     *
     * @param message 状态描述
     */
    fun updateStatusMessage(message: String)

    /**
     * 标记操作完成
     *
     * @param success 是否成功
     * @param message 完成消息
     */
    fun markCompleted(success: Boolean, message: String = "")

    /**
     * 停止前台服务
     */
    fun stopForegroundOperation()
}

/**
 * 克隆操作通知状态
 */
data class CloneOperationNotificationState(
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isSuccess: Boolean = false,
    val operationType: String = "",
    val packageName: String = "",
    val currentProgress: Int = 0,
    val totalProgress: Int = 0,
    val progressText: String = "",
    val statusMessage: String = ""
)
