package com.appclonex.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 配置文件操作记录。
 *
 * 用于实现Saga模式，确保配置文件创建/删除的分布式事务一致性。
 * 记录操作状态，支持补偿和重试。
 *
 * ## 使用场景
 * 1. 创建配置文件：系统用户创建 → 数据库记录 → 完成
 * 2. 删除配置文件：系统用户删除 → 数据库删除 → 级联清理 → 完成
 *
 * ## Saga模式状态流转（创建）
 * ```
 * CREATING_SYSTEM_USER -> CREATING_DB_RECORD -> COMPLETED
 *         |                      |
 *         v                      v
 *    COMPENSATING         COMPENSATING
 *         |
 *         v
 *   COMPENSATED
 * ```
 *
 * ## Saga模式状态流转（删除）
 * ```
 * DELETING_SYSTEM_USER -> DELETING_DB_RECORD -> CLEANING_ASSOCIATED -> COMPLETED
 *         |                      |                      |
 *         v                      v                      v
 *      FAILED                 FAILED                 FAILED
 * ```
 *
 * @property operationId 操作唯一标识符（UUID）
 * @property operationType 操作类型（CREATE/DELETE）
 * @property userId 配置文件用户ID（创建后获得）
 * @property profileName 配置文件名称
 * @property status 操作状态
 * @property createdAt 操作创建时间戳
 * @property updatedAt 最后更新时间戳
 * @property errorMessage 失败时的错误信息
 * @property retryCount 重试次数
 * @property compensationAttempts 补偿尝试次数
 */
@Entity(
    tableName = "profile_operations",
    indices = [
        Index(value = ["userId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["operationType"])
    ]
)
data class ProfileOperation(
    @PrimaryKey
    val operationId: String,

    val operationType: ProfileOperationType,
    val userId: Int = -1,
    val profileName: String? = null,

    val status: ProfileOperationStatus = ProfileOperationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val compensationAttempts: Int = 0
) {
    /**
     * 检查操作是否处于可重试状态
     */
    fun isRetryable(): Boolean {
        return status in setOf(
            ProfileOperationStatus.FAILED,
            ProfileOperationStatus.COMPENSATION_FAILED
        ) && retryCount < MAX_RETRY_COUNT
    }

    /**
     * 检查是否需要补偿
     */
    fun needsCompensation(): Boolean {
        return operationType == ProfileOperationType.CREATE &&
               status in setOf(
                   ProfileOperationStatus.CREATING_DB_RECORD_FAILED,
                   ProfileOperationStatus.CREATING_SYSTEM_USER_FAILED
               ) &&
               userId >= 0
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
 * 配置文件操作类型
 */
enum class ProfileOperationType {
    CREATE,
    DELETE
}

/**
 * 配置文件操作状态
 */
enum class ProfileOperationStatus {
    // 通用状态
    PENDING,
    COMPLETED,
    FAILED,

    // 创建操作状态
    CREATING_SYSTEM_USER,
    CREATING_DB_RECORD,
    CREATE_ROLLING_BACK,

    // 删除操作状态
    DELETING_SYSTEM_USER,
    DELETING_DB_RECORD,
    CLEANING_ASSOCIATED_DATA,

    // 补偿状态
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED,

    // 具体失败状态
    CREATING_SYSTEM_USER_FAILED,
    CREATING_DB_RECORD_FAILED,
    DELETING_SYSTEM_USER_FAILED,
    CLEANING_ASSOCIATED_FAILED,

    // 超时
    TIMEOUT
}

/**
 * 配置文件操作结果
 */
sealed class ProfileOperationResult {
    abstract val operationId: String

    data class Success(
        override val operationId: String,
        val userId: Int
    ) : ProfileOperationResult()

    data class Failure(
        override val operationId: String,
        val error: Throwable,
        val compensable: Boolean = false,
        val userId: Int = -1
    ) : ProfileOperationResult()

    data class Compensated(
        override val operationId: String,
        val originalError: Throwable
    ) : ProfileOperationResult()
}
