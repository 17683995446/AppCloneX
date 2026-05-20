package com.appclonex.domain.repository

import com.appclonex.domain.model.CloudBackupConfig
import com.appclonex.domain.model.CloudBackupInfo
import com.appclonex.domain.model.CloudProvider
import com.appclonex.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

/**
 * 云备份仓库接口
 * 提供云备份配置管理、同步操作和提供商连接测试功能
 */
interface CloudBackupRepository {

    // ==================== 配置管理 / Configuration ====================

    /**
     * 获取云备份配置的响应式数据流
     *
     * @return 云备份配置的Flow
     */
    fun getConfig(): Flow<CloudBackupConfig>

    /**
     * 更新云备份配置
     *
     * @param config 新的云备份配置
     */
    suspend fun updateConfig(config: CloudBackupConfig)

    // ==================== 同步操作 / Sync Operations ====================

    /**
     * 同步备份到云端
     *
     * @return 同步结果
     */
    suspend fun syncToCloud(): Result<Unit>

    /**
     * 从云端同步备份
     *
     * @return 同步结果
     */
    suspend fun syncFromCloud(): Result<Unit>

    /**
     * 获取上次同步结果
     *
     * @return 上次同步结果状态
     */
    suspend fun getLastSyncResult(): SyncResult

    // ==================== 提供商管理 / Provider Management ====================

    /**
     * 测试与指定提供商的连接
     *
     * @param provider 云存储提供商
     * @return 连接测试结果，true表示连接成功
     */
    suspend fun testConnection(provider: CloudProvider): Result<Boolean>

    /**
     * 获取所有可用提供商的备份信息
     *
     * @return 可用提供商信息列表
     */
    suspend fun getAvailableProviders(): List<CloudBackupInfo>

    // ==================== 自动同步 / Auto Sync ====================

    /**
     * 获取自动同步开关状态
     *
     * @return 自动同步是否启用的Flow
     */
    fun getAutoSyncStatus(): Flow<Boolean>

    /**
     * 启用自动同步
     */
    suspend fun enableAutoSync()

    /**
     * 禁用自动同步
     */
    suspend fun disableAutoSync()
}
