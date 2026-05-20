package com.appclonex.domain.usecase.backup

import com.appclonex.domain.model.BackupMetadata
import com.appclonex.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * 创建备份用例
 * Create backup use case
 *
 * 封装备份创建逻辑，支持全量备份和按配置文件选择性备份
 * Encapsulates backup creation logic, supporting full backup and selective backup by profile IDs
 *
 * @param backupRepository 备份仓库接口 / Backup repository interface
 */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    /**
     * 创建完整备份（所有配置文件）
     * Create a full backup (all profiles)
     *
     * @return 备份元数据 / Backup metadata on success
     */
    suspend operator fun invoke(): Result<BackupMetadata> =
        backupRepository.createBackup()

    /**
     * 创建指定配置文件的备份
     * Create a backup for the specified profiles
     *
     * @param profileIds 需要备份的配置文件ID列表 / List of profile IDs to back up
     * @return 备份元数据 / Backup metadata on success
     */
    suspend operator fun invoke(profileIds: List<Int>): Result<BackupMetadata> =
        backupRepository.createBackup(profileIds)
}
