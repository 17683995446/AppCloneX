package com.appclonex.infrastructure.persistence.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appclonex.domain.model.OperationStatus
import com.appclonex.domain.model.PendingCloneOperation

/**
 * 待处理克隆操作DAO
 *
 * 支持幂等性克隆操作的事务管理
 */
@Dao
interface PendingCloneOperationDao {

    /**
     * 插入新的待处理操作
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingCloneOperation): Long

    /**
     * 更新操作状态
     */
    @Update
    suspend fun updateOperation(operation: PendingCloneOperation)

    /**
     * 根据ID获取操作
     */
    @Query("SELECT * FROM pending_clone_operations WHERE operationId = :operationId")
    suspend fun getOperationById(operationId: String): PendingCloneOperation?

    /**
     * 根据包名和配置文件获取操作
     */
    @Query("SELECT * FROM pending_clone_operations WHERE packageName = :packageName AND profileUserId = :profileUserId")
    suspend fun getOperationByPackageAndProfile(packageName: String, profileUserId: Int): PendingCloneOperation?

    /**
     * 获取指定状态的所有操作
     */
    @Query("SELECT * FROM pending_clone_operations WHERE status = :status")
    suspend fun getOperationsByStatus(status: OperationStatus): List<PendingCloneOperation>

    /**
     * 获取所有待处理或安装中的操作（用于恢复检查）
     */
    @Query("SELECT * FROM pending_clone_operations WHERE status IN ('PENDING', 'INSTALLING')")
    suspend fun getIncompleteOperations(): List<PendingCloneOperation>

    /**
     * 获取所有失败的操作
     */
    @Query("SELECT * FROM pending_clone_operations WHERE status = 'FAILED'")
    suspend fun getFailedOperations(): List<PendingCloneOperation>

    /**
     * 获取超时的操作
     */
    @Query("SELECT * FROM pending_clone_operations WHERE status IN ('PENDING', 'INSTALLING') AND createdAt < :timeoutThreshold")
    suspend fun getTimeoutOperations(timeoutThreshold: Long): List<PendingCloneOperation>

    /**
     * 更新操作状态
     */
    @Query("UPDATE pending_clone_operations SET status = :status, updatedAt = :timestamp, errorMessage = :errorMessage WHERE operationId = :operationId")
    suspend fun updateStatus(operationId: String, status: OperationStatus, timestamp: Long = System.currentTimeMillis(), errorMessage: String? = null)

    /**
     * 更新重试次数
     */
    @Query("UPDATE pending_clone_operations SET retryCount = retryCount + 1, updatedAt = :timestamp WHERE operationId = :operationId")
    suspend fun incrementRetryCount(operationId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * 删除操作记录
     */
    @Query("DELETE FROM pending_clone_operations WHERE operationId = :operationId")
    suspend fun deleteOperation(operationId: String)

    /**
     * 删除已完成或已补偿的操作（清理）
     */
    @Query("DELETE FROM pending_clone_operations WHERE status IN ('COMPLETED', 'COMPENSATED') AND updatedAt < :cleanupThreshold")
    suspend fun deleteCompletedOperations(cleanupThreshold: Long)

    /**
     * 检查是否存在相同包名和配置文件的操作
     */
    @Query("SELECT COUNT(*) > 0 FROM pending_clone_operations WHERE packageName = :packageName AND profileUserId = :profileUserId AND status NOT IN ('COMPLETED', 'COMPENSATED')")
    suspend fun hasIncompleteOperation(packageName: String, profileUserId: Int): Boolean

    /**
     * 获取所有操作（用于调试）
     */
    @Query("SELECT * FROM pending_clone_operations ORDER BY createdAt DESC")
    suspend fun getAllOperations(): List<PendingCloneOperation>
}
