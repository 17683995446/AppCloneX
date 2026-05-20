package com.appclonex.domain.usecase.backup

import com.appclonex.domain.model.BackupMetadata
import com.appclonex.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * 列出备份用例
 * List backups use case
 *
 * 封装备份列表查询逻辑，获取所有可用备份的元数据信息
 * Encapsulates backup list query logic, retrieving metadata for all available backups
 *
 * @param backupRepository 备份仓库接口 / Backup repository interface
 */
class ListBackupsUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    /**
     * 获取所有可用备份的元数据列表
     * Get the metadata list of all available backups
     *
     * 返回的备份按时间戳倒序排列（最新的在前），
     * 每条记录包含备份ID、文件名、大小、时间、配置数量等信息。
     *
     * Returned backups are sorted by timestamp in descending order (newest first),
     * each record contains backup ID, file name, size, timestamp, profile count, etc.
     *
     * @return 备份元数据列表 / List of backup metadata on success
     */
    suspend operator fun invoke(): Result<List<BackupMetadata>> =
        backupRepository.listBackups()
}
