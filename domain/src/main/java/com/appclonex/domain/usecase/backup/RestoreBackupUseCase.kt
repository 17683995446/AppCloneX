package com.appclonex.domain.usecase.backup

import com.appclonex.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * 恢复备份用例
 * Restore backup use case
 *
 * 封装备份恢复逻辑，从指定文件路径读取备份数据并恢复到当前设备
 * Encapsulates backup restore logic, reading backup data from a specified file path and restoring to the current device
 *
 * @param backupRepository 备份仓库接口 / Backup repository interface
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    /**
     * 从文件恢复备份
     * Restore backup from a file
     *
     * 该方法会读取指定路径的备份文件，解析备份数据，并将其恢复到当前设备。
     * 恢复过程包括配置文件、克隆应用、设置和自动化规则等所有数据。
     *
     * This method reads the backup file at the specified path, parses the backup data,
     * and restores it to the current device. The restore process includes all data
     * such as profiles, cloned apps, settings, and automation rules.
     *
     * @param filePath 备份文件的完整路径 / Full path to the backup file
     * @return 恢复结果，成功时返回Unit / Restore result, Unit on success
     */
    suspend operator fun invoke(filePath: String): Result<Unit> =
        backupRepository.restoreFromFile(filePath)
}
