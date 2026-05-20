package com.appclonex.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 待处理的克隆操作记录。
 *
 * 用于实现幂等性克隆操作，确保分布式事务（系统安装 + 数据库记录）的原子性。
 * 在克隆操作开始前创建此记录，标记操作状态，完成后更新或删除。
 *
 * ## 使用场景
 * 1. 克隆开始前：创建PENDING记录
 * 2. 系统安装成功后：更新为INSTALLING状态
 * 3. 数据库记录创建后：更新为COMPLETED或删除
 * 4. 失败时：更新为FAILED状态，便于后续补偿或重试
 *
 * ## 状态流转
 * ```
 * PENDING -> INSTALLING -> COMPLETED
 *    |           |
 *    v           v
 * FAILED      FAILED (需要补偿)
 * ```
 *
 * @property operationId 操作唯一标识符（UUID），幂等性键
 * @property packageName 要克隆的应用包名
 * @property profileUserId 目标配置文件用户ID
 * @property cloneIndex 分配的克隆序号
 * @property status 操作状态
 * @property createdAt 操作创建时间戳
 * @property updatedAt 最后更新时间戳
 * @property errorMessage 失败时的错误信息
 * @property retryCount 重试次数
 */
@Entity(
    tableName = "pending_clone_operations",
    indices = [
        Index(value = ["packageName", "profileUserId"], unique = true),
        Index(value = ["status"])
    ]
)
data class PendingCloneOperation(
    @PrimaryKey
    val operationId: String,
    
    val packageName: String,
    val profileUserId: Int,
    val cloneIndex: Int = 1,
    
    val status: OperationStatus = OperationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val retryCount: Int = 0
) {
    /**
     * 检查操作是否处于可重试状态
     */
    fun isRetryable(): Boolean {
        return status == OperationStatus.FAILED && retryCount < MAX_RETRY_COUNT
    }
    
    /**
     * 获取下一次重试的延迟时间（指数退避）
     */
    fun getRetryDelayMs(): Long {
        return MIN_RETRY_DELAY_MS * (1 shl retryCount.coerceAtMost(5))
    }
    
    companion object {
        const val MAX_RETRY_COUNT = 3
        const val MIN_RETRY_DELAY_MS = 1000L
        const val OPERATION_TIMEOUT_MS = 5 * 60 * 1000L // 5分钟超时
    }
}

/**
 * 克隆操作状态枚举
 */
enum class OperationStatus {
    /** 待处理：操作已创建，尚未开始系统安装 */
    PENDING,
    
    /** 安装中：系统安装已开始 */
    INSTALLING,
    
    /** 已完成：系统安装和数据库记录都成功 */
    COMPLETED,
    
    /** 失败：操作失败，可能需要补偿 */
    FAILED,
    
    /** 已补偿：失败后的补偿操作已完成 */
    COMPENSATED,
    
    /** 已超时：操作超过最大执行时间 */
    TIMEOUT
}

/**
 * 克隆操作结果封装
 */
sealed class CloneOperationResult {
    abstract val operationId: String
    
    data class Success(
        override val operationId: String,
        val clonedApp: ClonedApplicationInformationModel
    ) : CloneOperationResult()
    
    data class Failure(
        override val operationId: String,
        val error: Throwable,
        val needsCompensation: Boolean = false
    ) : CloneOperationResult()
    
    data class AlreadyExists(
        override val operationId: String,
        val existingApp: ClonedApplicationInformationModel
    ) : CloneOperationResult()
}
