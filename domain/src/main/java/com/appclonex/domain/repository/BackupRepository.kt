package com.appclonex.domain.repository

import com.appclonex.domain.model.BackupData
import com.appclonex.domain.model.BackupMetadata
import kotlinx.coroutines.flow.Flow

/**
 * 备份仓库接口
 * Backup repository interface
 *
 * 负责完整备份和恢复操作，包括创建备份、恢复备份、管理备份文件、
 * 导入导出以及自动备份等功能
 * Responsible for complete backup and restore operations, including creating backups,
 * restoring backups, managing backup files, import/export, and auto backup
 */
interface BackupRepository {

    // ==================== 创建备份 / Create Backup ====================

    /**
     * 创建完整备份
     * Create a full backup of all profiles and settings
     *
     * @return 备份元数据 / Backup metadata on success
     */
    suspend fun createBackup(): Result<BackupMetadata>

    /**
     * 创建指定配置文件的备份
     * Create a backup for the specified profiles only
     *
     * @param profileIds 需要备份的配置文件ID列表 / List of profile IDs to back up
     * @return 备份元数据 / Backup metadata on success
     */
    suspend fun createBackup(profileIds: List<Int>): Result<BackupMetadata>

    // ==================== 恢复备份 / Restore Backup ====================

    /**
     * 从备份数据对象恢复
     * Restore from a BackupData object
     *
     * @param backupData 备份数据 / The backup data to restore from
     * @return 恢复结果 / Restore result
     */
    suspend fun restoreBackup(backupData: BackupData): Result<Unit>

    /**
     * 从文件恢复备份
     * Restore backup from a file path
     *
     * @param filePath 备份文件路径 / Path to the backup file
     * @return 恢复结果 / Restore result
     */
    suspend fun restoreFromFile(filePath: String): Result<Unit>

    // ==================== 管理备份文件 / Manage Backup Files ====================

    /**
     * 列出所有可用备份
     * List all available backups
     *
     * @return 备份元数据列表 / List of backup metadata
     */
    suspend fun listBackups(): Result<List<BackupMetadata>>

    /**
     * 删除指定备份
     * Delete a specific backup by its ID
     *
     * @param backupId 备份唯一标识 / Unique backup identifier
     * @return 删除结果 / Deletion result
     */
    suspend fun deleteBackup(backupId: String): Result<Unit>

    /**
     * 获取备份详情
     * Get full backup data for a specific backup
     *
     * @param backupId 备份唯一标识 / Unique backup identifier
     * @return 完整备份数据 / Complete backup data
     */
    suspend fun getBackupDetails(backupId: String): Result<BackupData>

    // ==================== 导入导出 / Import & Export ====================

    /**
     * 将备份数据导出到文件
     * Export backup data to a file
     *
     * @param backupData 要导出的备份数据 / The backup data to export
     * @return 导出文件的路径 / Path of the exported file on success
     */
    suspend fun exportToFile(backupData: BackupData): Result<String>

    /**
     * 从文件导入备份数据
     * Import backup data from a file
     *
     * @param filePath 备份文件路径 / Path to the backup file
     * @return 解析后的备份数据 / Parsed backup data
     */
    suspend fun importFromFile(filePath: String): Result<BackupData>

    // ==================== 自动备份 / Auto Backup ====================

    /**
     * 执行自动备份
     * Perform an automatic backup
     *
     * 根据自动备份策略判断是否需要执行备份，如果不需要则返回null
     * Determines whether a backup is needed based on auto-backup policy; returns null if not needed
     *
     * @return 备份元数据，如果未执行备份则为null / Backup metadata, or null if backup was not performed
     */
    suspend fun performAutoBackup(): Result<BackupMetadata?>

    /**
     * 获取自动备份开关状态
     * Observe the auto-backup enabled status
     *
     * @return 自动备份是否启用的Flow / Flow of whether auto-backup is enabled
     */
    fun getAutoBackupStatus(): Flow<Boolean>

    // ==================== 备份大小 / Backup Size ====================

    /**
     * 获取当前备份占用的总存储空间
     * Get the total storage space used by backups
     *
     * @return 备份总大小（字节） / Total backup size in bytes
     */
    suspend fun getBackupSize(): Long

    /**
     * 获取最大备份文件数量
     * Get the maximum number of backup files allowed
     *
     * @return 最大备份数量 / Maximum backup count
     */
    suspend fun getMaxBackupCount(): Int
}
