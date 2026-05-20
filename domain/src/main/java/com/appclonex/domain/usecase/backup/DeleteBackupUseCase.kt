package com.appclonex.domain.usecase.backup

import com.appclonex.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * 删除备份用例
 * Delete backup use case
 *
 * 封装备份删除逻辑，根据备份ID删除指定的备份文件及其关联数据
 * Encapsulates backup deletion logic, deleting a specific backup file and its associated data by backup ID
 *
 * @param backupRepository 备份仓库接口 / Backup repository interface
 */
class DeleteBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    /**
     * 删除指定备份
     * Delete a specific backup
     *
     * 根据备份ID查找并删除对应的备份文件。如果备份不存在，返回失败结果。
     * 删除操作不可逆，调用前应提示用户确认。
     *
     * Finds and deletes the backup file corresponding to the given backup ID.
     * Returns a failure result if the backup does not exist.
     * The deletion operation is irreversible; user confirmation should be obtained before calling.
     *
     * @param backupId 备份唯一标识 / Unique backup identifier
     * @return 删除结果，成功时返回Unit / Deletion result, Unit on success
     */
    suspend operator fun invoke(backupId: String): Result<Unit> =
        backupRepository.deleteBackup(backupId)
}
