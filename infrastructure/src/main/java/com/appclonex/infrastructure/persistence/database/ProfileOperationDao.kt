package com.appclonex.infrastructure.persistence.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appclonex.domain.model.ProfileOperation
import com.appclonex.domain.model.ProfileOperationStatus
import com.appclonex.domain.model.ProfileOperationType

/**
 * 配置文件操作DAO
 *
 * 支持Saga模式的事务管理
 */
@Dao
interface ProfileOperationDao {

    /**
     * 插入新操作
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: ProfileOperation): Long

    /**
     * 更新操作
     */
    @Update
    suspend fun updateOperation(operation: ProfileOperation)

    /**
     * 根据ID获取操作
     */
    @Query("SELECT * FROM profile_operations WHERE operationId = :operationId")
    suspend fun getOperationById(operationId: String): ProfileOperation?

    /**
     * 根据用户ID获取操作
     */
    @Query("SELECT * FROM profile_operations WHERE userId = :userId")
    suspend fun getOperationByUserId(userId: Int): ProfileOperation?

    /**
     * 获取指定类型的操作
     */
    @Query("SELECT * FROM profile_operations WHERE operationType = :type")
    suspend fun getOperationsByType(type: ProfileOperationType): List<ProfileOperation>

    /**
     * 获取指定状态的操作
     */
    @Query("SELECT * FROM profile_operations WHERE status = :status")
    suspend fun getOperationsByStatus(status: ProfileOperationStatus): List<ProfileOperation>

    /**
     * 获取所有未完成的操作
     */
    @Query("""
        SELECT * FROM profile_operations 
        WHERE status NOT IN ('COMPLETED', 'COMPENSATED', 'FAILED', 'TIMEOUT')
    """)
    suspend fun getIncompleteOperations(): List<ProfileOperation>

    /**
     * 获取需要补偿的创建操作
     */
    @Query("""
        SELECT * FROM profile_operations 
        WHERE operationType = 'CREATE' 
        AND status IN ('CREATING_DB_RECORD_FAILED', 'CREATING_SYSTEM_USER_FAILED')
        AND userId >= 0
        AND compensationAttempts < 3
    """)
    suspend fun getPendingCompensationOperations(): List<ProfileOperation>

    /**
     * 获取超时的操作
     */
    @Query("""
        SELECT * FROM profile_operations 
        WHERE status NOT IN ('COMPLETED', 'COMPENSATED', 'FAILED', 'TIMEOUT')
        AND createdAt < :timeoutThreshold
    """)
    suspend fun getTimeoutOperations(timeoutThreshold: Long): List<ProfileOperation>

    /**
     * 更新操作状态
     */
    @Query("""
        UPDATE profile_operations 
        SET status = :status, updatedAt = :timestamp, errorMessage = :errorMessage 
        WHERE operationId = :operationId
    """)
    suspend fun updateStatus(
        operationId: String,
        status: ProfileOperationStatus,
        timestamp: Long = System.currentTimeMillis(),
        errorMessage: String? = null
    )

    /**
     * 更新用户ID（创建系统用户后）
     */
    @Query("""
        UPDATE profile_operations 
        SET userId = :userId, updatedAt = :timestamp 
        WHERE operationId = :operationId
    """)
    suspend fun updateUserId(operationId: String, userId: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * 增加重试次数
     */
    @Query("""
        UPDATE profile_operations 
        SET retryCount = retryCount + 1, updatedAt = :timestamp 
        WHERE operationId = :operationId
    """)
    suspend fun incrementRetryCount(operationId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * 增加补偿尝试次数
     */
    @Query("""
        UPDATE profile_operations 
        SET compensationAttempts = compensationAttempts + 1, updatedAt = :timestamp 
        WHERE operationId = :operationId
    """)
    suspend fun incrementCompensationAttempts(operationId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * 删除操作记录
     */
    @Query("DELETE FROM profile_operations WHERE operationId = :operationId")
    suspend fun deleteOperation(operationId: String)

    /**
     * 清理已完成的历史记录
     */
    @Query("""
        DELETE FROM profile_operations 
        WHERE status IN ('COMPLETED', 'COMPENSATED') 
        AND updatedAt < :cleanupThreshold
    """)
    suspend fun deleteCompletedOperations(cleanupThreshold: Long)

    /**
     * 检查是否存在未完成的操作
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM profile_operations 
        WHERE userId = :userId 
        AND status NOT IN ('COMPLETED', 'COMPENSATED', 'FAILED', 'TIMEOUT')
    """)
    suspend fun hasIncompleteOperation(userId: Int): Boolean

    /**
     * 获取所有操作（用于调试）
     */
    @Query("SELECT * FROM profile_operations ORDER BY createdAt DESC")
    suspend fun getAllOperations(): List<ProfileOperation>
}
