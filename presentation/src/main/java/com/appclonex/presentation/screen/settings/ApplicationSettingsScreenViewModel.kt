package com.appclonex.presentation.screen.settings

import com.appclonex.domain.model.ThemeMode
import com.appclonex.domain.repository.PreferencesRepository
import com.appclonex.domain.usecase.configuration.ExportConfigurationUseCase
import com.appclonex.domain.usecase.configuration.ImportConfigurationUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UI state for application settings screen.
 */
data class SettingsUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val excludeSystemApps: Boolean = true,
    val showNotifications: Boolean = true,
    val autoStartProfile: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkTheme: Boolean = false,
    val lastBackupTime: Long = 0L,
    val totalProfiles: Int = 0,
    val totalClonedApps: Int = 0,
    val isBackupLoading: Boolean = false,
    val isRestoreLoading: Boolean = false
) : BaseUiState

/**
 * ViewModel for application settings.
 *
 * Responsibilities:
 * - Manage app preferences
 * - Handle backup and restore
 * - Theme management
 *
 * R12-004 修复：Flow订阅管理改进
 * - 使用 combine() 组合多个 Flow，避免独立订阅
 * - 保存 Job 引用，支持取消和重新加载
 * - 统一状态更新，避免竞态条件
 */
@HiltViewModel
class ApplicationSettingsScreenViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val exportConfigurationUseCase: ExportConfigurationUseCase,
    private val importConfigurationUseCase: ImportConfigurationUseCase
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    // R12-004: 保存设置加载Job引用，支持取消
    private var loadSettingsJob: Job? = null

    init {
        loadSettings()
    }

    /**
     * 从 Repository 加载设置
     *
     * R12-004 修复：使用 combine 组合多个 Flow
     * - 取消之前的订阅，避免重复订阅
     * - 统一更新状态，避免竞态条件
     */
    private fun loadSettings() {
        // R12-004: 取消之前的订阅
        loadSettingsJob?.cancel()

        // R12-004: 使用 combine 组合多个 Flow
        loadSettingsJob = launchWithErrorHandler {
            combine(
                preferencesRepository.getExcludeSystemApps(),
                preferencesRepository.getThemeMode()
            ) { excludeSystemApps, themeMode ->
                // 组合结果
                SettingsUpdate(
                    excludeSystemApps = excludeSystemApps,
                    themeMode = themeMode
                )
            }.collect { update ->
                updateState {
                    copy(
                        excludeSystemApps = update.excludeSystemApps,
                        themeMode = update.themeMode
                    )
                }
            }
        }
    }

    /**
     * 设置更新数据类
     */
    private data class SettingsUpdate(
        val excludeSystemApps: Boolean,
        val themeMode: ThemeMode
    )

    /**
     * 刷新设置（取消旧订阅并重新加载）
     */
    fun refreshSettings() {
        loadSettings()
    }

    fun setExcludeSystemApps(exclude: Boolean) {
        launchWithErrorHandler {
            preferencesRepository.setExcludeSystemApps(exclude)
            updateState { copy(excludeSystemApps = exclude) }
        }
    }

    fun setShowNotifications(show: Boolean) {
        launchWithErrorHandler {
            preferencesRepository.setShowNotifications(show)
            updateState { copy(showNotifications = show) }
        }
    }

    fun setAutoStartProfile(autoStart: Boolean) {
        launchWithErrorHandler {
            preferencesRepository.setAutoStartProfile(autoStart)
            updateState { copy(autoStartProfile = autoStart) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        launchWithErrorHandler {
            preferencesRepository.setThemeMode(mode)
            updateState { copy(themeMode = mode) }
        }
    }

    fun setDarkTheme(dark: Boolean) {
        launchWithErrorHandler {
            preferencesRepository.setDarkTheme(dark)
            updateState { copy(darkTheme = dark) }
        }
    }

    fun performBackup(profileId: Int = 0) {
        launchWithErrorHandler {
            updateState { copy(isBackupLoading = true) }

            if (profileId <= 0) {
                updateState {
                    copy(
                        error = "请指定要备份的配置文件",
                        isBackupLoading = false
                    )
                }
                return@launchWithErrorHandler
            }

            val result = exportConfigurationUseCase(profileId)

            result.onSuccess { configuration ->
                updateState {
                    copy(
                        message = "备份成功: ${configuration.profile.profileName}",
                        lastBackupTime = System.currentTimeMillis(),
                        isBackupLoading = false
                    )
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        error = "备份失败: ${e.message}",
                        isBackupLoading = false
                    )
                }
            }
        }
    }

    fun performRestore(filePath: String) {
        launchWithErrorHandler {
            updateState { copy(isRestoreLoading = true) }

            if (filePath.isBlank()) {
                updateState {
                    copy(
                        error = "请指定备份文件路径",
                        isRestoreLoading = false
                    )
                }
                return@launchWithErrorHandler
            }

            val importResult = importConfigurationUseCase.fromFile(filePath)

            importResult.onSuccess { configuration ->
                val applyResult = importConfigurationUseCase(configuration)

                applyResult.onSuccess { newProfileId ->
                    updateState {
                        copy(
                            message = "恢复成功，配置文件ID: $newProfileId",
                            isRestoreLoading = false
                        )
                    }
                }.onFailure { e ->
                    updateState {
                        copy(
                            error = "应用配置失败: ${e.message}",
                            isRestoreLoading = false
                        )
                    }
                }
            }.onFailure { e ->
                updateState {
                    copy(
                        error = "读取备份文件失败: ${e.message}",
                        isRestoreLoading = false
                    )
                }
            }
        }
    }

    fun getFormattedLastBackupTime(): String {
        val timestamp = currentState.lastBackupTime
        return if (timestamp == 0L) {
            "从未备份"
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(timestamp))
        }
    }

    override fun onCleared() {
        super.onCleared()
        // R12-004: 清理时取消所有订阅
        loadSettingsJob?.cancel()
    }
}
