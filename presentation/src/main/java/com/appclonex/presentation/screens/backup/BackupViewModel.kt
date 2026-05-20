package com.appclonex.presentation.screens.backup

import com.appclonex.domain.model.BackupMetadata
import com.appclonex.domain.usecase.backup.CreateBackupUseCase
import com.appclonex.domain.usecase.backup.DeleteBackupUseCase
import com.appclonex.domain.usecase.backup.ListBackupsUseCase
import com.appclonex.domain.usecase.backup.RestoreBackupUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 备份界面 UI 状态
 * Backup screen UI state
 *
 * @property backups 备份元数据列表 / List of backup metadata
 * @property isLoading 是否正在加载备份列表 / Whether backup list is being loaded
 * @property isCreating 是否正在创建备份 / Whether a backup is being created
 * @property isRestoring 是否正在恢复备份 / Whether a backup is being restored
 * @property error 错误消息 / Error message
 * @property message 提示消息 / Info message
 * @property successMessage 成功消息 / Success message
 */
data class BackupUiState(
    val backups: List<BackupMetadata> = emptyList(),
    override val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRestoring: Boolean = false,
    override val error: String? = null,
    override val message: String? = null,
    val successMessage: String? = null
) : BaseUiState

/**
 * 备份与恢复界面 ViewModel
 * ViewModel for the backup and restore screen
 *
 * 负责管理备份列表的加载、创建新备份、恢复备份和删除备份操作。
 * Manages loading backup list, creating new backups, restoring and deleting backups.
 *
 * @param createBackupUseCase 创建备份用例 / Create backup use case
 * @param restoreBackupUseCase 恢复备份用例 / Restore backup use case
 * @param listBackupsUseCase 列出备份用例 / List backups use case
 * @param deleteBackupUseCase 删除备份用例 / Delete backup use case
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val listBackupsUseCase: ListBackupsUseCase,
    private val deleteBackupUseCase: DeleteBackupUseCase
) : BaseViewModel<BackupUiState>(BackupUiState()) {

    init {
        loadBackups()
    }

    /**
     * 加载备份列表
     * Load the list of available backups
     */
    fun loadBackups() {
        launchWithErrorHandler {
            updateState { copy(isLoading = true, error = null) }

            listBackupsUseCase()
                .onSuccess { backups ->
                    updateState {
                        copy(
                            backups = backups,
                            isLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            isLoading = false,
                            error = throwable.message ?: "加载备份列表失败"
                        )
                    }
                }
        }
    }

    /**
     * 创建新备份
     * Create a new backup
     *
     * 创建完成后自动刷新备份列表。
     * Automatically refreshes the backup list after creation.
     */
    fun createBackup() {
        launchWithErrorHandler {
            updateState { copy(isCreating = true, error = null) }

            createBackupUseCase()
                .onSuccess { metadata ->
                    updateState {
                        copy(
                            isCreating = false,
                            successMessage = "备份创建成功: ${metadata.fileName}"
                        )
                    }
                    // 创建成功后刷新列表
                    loadBackups()
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            isCreating = false,
                            error = throwable.message ?: "创建备份失败"
                        )
                    }
                }
        }
    }

    /**
     * 恢复指定备份
     * Restore from the specified backup file
     *
     * @param filePath 备份文件路径 / Backup file path
     */
    fun restoreBackup(filePath: String) {
        launchWithErrorHandler {
            updateState { copy(isRestoring = true, error = null) }

            restoreBackupUseCase(filePath)
                .onSuccess {
                    updateState {
                        copy(
                            isRestoring = false,
                            successMessage = "备份恢复成功"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            isRestoring = false,
                            error = throwable.message ?: "恢复备份失败"
                        )
                    }
                }
        }
    }

    /**
     * 删除指定备份
     * Delete the specified backup
     *
     * @param backupId 备份唯一标识 / Unique backup identifier
     */
    fun deleteBackup(backupId: String) {
        launchWithErrorHandler {
            updateState { copy(error = null) }

            deleteBackupUseCase(backupId)
                .onSuccess {
                    updateState {
                        copy(
                            backups = backups.filter { backup -> backup.id != backupId },
                            successMessage = "备份已删除"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            error = throwable.message ?: "删除备份失败"
                        )
                    }
                }
        }
    }

    /**
     * 清除成功消息
     * Clear the success message
     */
    fun clearSuccessMessage() {
        updateState { copy(successMessage = null) }
    }
}
