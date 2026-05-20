/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件管理 ViewModel
 *
 * 管理插件管理界面的UI状态和业务逻辑，包括：
 * - 插件列表加载和搜索过滤
 * - 插件安装/卸载/启用/禁用操作
 * - 操作状态管理和错误处理
 * - 插件详情和权限信息获取
 */
package com.appclonex.presentation.screens.settings.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appclonex.domain.model.PluginConfig
import com.appclonex.domain.model.PluginId
import com.appclonex.domain.model.PluginManifest
import com.appclonex.domain.model.PluginMetadata
import com.appclonex.domain.model.PluginPermission
import com.appclonex.domain.model.PluginState
import com.appclonex.domain.repository.PluginRepository
import com.appclonex.domain.usecase.plugin.InstallPluginUseCase
import com.appclonex.domain.usecase.plugin.ManagePluginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 插件管理界面UI状态
 *
 * @property plugins 已安装插件列表
 * @property filteredPlugins 搜索过滤后的插件列表
 * @property searchQuery 当前搜索关键词
 * @property isLoading 是否正在加载
 * @property selectedPlugin 当前选中的插件（用于详情弹窗）
 * @property showInstallDialog 是否显示安装对话框
 * * @property showDetailDialog 是否显示详情弹窗
 * @property showPermissionDialog 是否显示权限管理对话框
 * @property pluginStates 插件状态映射
 * @property error 错误信息
 * @property message 提示信息
 */
data class PluginManagerUiState(
    val plugins: List<PluginMetadata> = emptyList(),
    val filteredPlugins: List<PluginMetadata> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedPlugin: PluginMetadata? = null,
    val showInstallDialog: Boolean = false,
    val showDetailDialog: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val pluginPermissions: Map<String, List<PluginPermission>> = emptyMap(),
    val error: String? = null,
    val message: String? = null
)

/**
 * 插件管理 ViewModel
 *
 * 使用 Hilt 注入依赖，管理插件管理界面的所有业务逻辑。
 * 通过 StateFlow 暴露UI状态，UI层通过 collectAsStateWithLifecycle 观察。
 *
 * @property repository 插件仓库，提供插件数据操作能力
 * @property installUseCase 安装插件用例
 * @property manageUseCase 插件管理用例
 */
@HiltViewModel
class PluginManagerViewModel @Inject constructor(
    private val repository: PluginRepository,
    private val installUseCase: InstallPluginUseCase,
    private val manageUseCase: ManagePluginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginManagerUiState())
    val uiState: StateFlow<PluginManagerUiState> = _uiState.asStateFlow()

    init {
        loadPlugins()
        observePluginStates()
    }

    // ============================================================================
    // 数据加载
    // ============================================================================

    /**
     * 加载已安装插件列表
     *
     * 从仓库获取所有已安装插件，并更新UI状态。
     */
    private fun loadPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getInstalledPlugins()
                .onSuccess { plugins ->
                    _uiState.update {
                        it.copy(
                            plugins = plugins,
                            filteredPlugins = plugins,
                            isLoading = false
                        )
                    }
                    // 加载每个插件的权限信息
                    loadPermissions(plugins)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "加载插件失败：${e.message}"
                        )
                    }
                }
        }
    }

    /**
     * 观察插件状态变化
     *
     * 通过 Flow 持续监听插件状态变化，实时更新UI。
     */
    private fun observePluginStates() {
        viewModelScope.launch {
            repository.observePluginStates().collect { states ->
                _uiState.update {
                    it.copy(
                        pluginStates = states.mapKeys { entry -> entry.key.value }
                    )
                }
            }
        }
    }

    /**
     * 加载插件权限信息
     *
     * @param plugins 插件列表
     */
    private suspend fun loadPermissions(plugins: List<PluginMetadata>) {
        val permissionsMap = mutableMapOf<String, List<PluginPermission>>()
        for (plugin in plugins) {
            repository.checkPermissions(plugin.id)
                .onSuccess { permissions ->
                    permissionsMap[plugin.id.value] = permissions
                }
        }
        _uiState.update { it.copy(pluginPermissions = permissionsMap) }
    }

    // ============================================================================
    // 搜索过滤
    // ============================================================================

    /**
     * 更新搜索关键词并过滤插件列表
     *
     * @param query 搜索关键词，按插件名称、描述、作者模糊匹配
     */
    fun updateSearchQuery(query: String) {
        val trimmedQuery = query.trim().lowercase()
        val filtered = if (trimmedQuery.isBlank()) {
            _uiState.value.plugins
        } else {
            _uiState.value.plugins.filter { plugin ->
                plugin.name.lowercase().contains(trimmedQuery) ||
                    plugin.description.lowercase().contains(trimmedQuery) ||
                    plugin.author.lowercase().contains(trimmedQuery)
            }
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredPlugins = filtered
            )
        }
    }

    // ============================================================================
    // 插件操作
    // ============================================================================

    /**
     * 安装插件
     *
     * 通过安装用例执行完整的安装流程，包括验证和持久化。
     *
     * @param manifest 插件清单
     */
    fun installPlugin(manifest: PluginManifest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showInstallDialog = false) }
            installUseCase(manifest)
                .onSuccess { result ->
                    if (result.success) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = "插件安装成功：${manifest.metadata.name}"
                            )
                        }
                        loadPlugins()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.errorMessage ?: "安装失败"
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "安装失败：${e.message}"
                        )
                    }
                }
        }
    }

    /**
     * 卸载插件
     *
     * @param pluginId 要卸载的插件ID
     */
    fun uninstallPlugin(pluginId: PluginId) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDetailDialog = false) }
            manageUseCase.uninstall(pluginId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedPlugin = null,
                            message = "插件已卸载"
                        )
                    }
                    loadPlugins()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "卸载失败：${e.message}"
                        )
                    }
                }
        }
    }

    /**
     * 启用插件
     *
     * @param pluginId 要启用的插件ID
     */
    fun enablePlugin(pluginId: PluginId) {
        viewModelScope.launch {
            manageUseCase.enable(pluginId)
                .onSuccess {
                    _uiState.update { it.copy(message = "插件已启用") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "启用失败：${e.message}") }
                }
        }
    }

    /**
     * 禁用插件
     *
     * @param pluginId 要禁用的插件ID
     */
    fun disablePlugin(pluginId: PluginId) {
        viewModelScope.launch {
            manageUseCase.disable(pluginId)
                .onSuccess {
                    _uiState.update { it.copy(message = "插件已禁用") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "禁用失败：${e.message}") }
                }
        }
    }

    /**
     * 更新插件
     *
     * @param pluginId 要更新的插件ID
     */
    fun updatePlugin(pluginId: PluginId) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            manageUseCase.update(pluginId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "插件已更新"
                        )
                    }
                    loadPlugins()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "更新失败：${e.message}"
                        )
                    }
                }
        }
    }

    // ============================================================================
    // 权限管理
    // ============================================================================

    /**
     * 授予插件权限
     *
     * @param pluginId 插件ID
     * @param permission 要授予的权限
     */
    fun grantPermission(pluginId: PluginId, permission: PluginPermission) {
        viewModelScope.launch {
            repository.grantPermission(pluginId, permission)
                .onSuccess {
                    _uiState.update { it.copy(message = "权限已授予") }
                    loadPlugins()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "授权失败：${e.message}") }
                }
        }
    }

    /**
     * 撤销插件权限
     *
     * @param pluginId 插件ID
     * @param permission 要撤销的权限
     */
    fun revokePermission(pluginId: PluginId, permission: PluginPermission) {
        viewModelScope.launch {
            repository.revokePermission(pluginId, permission)
                .onSuccess {
                    _uiState.update { it.copy(message = "权限已撤销") }
                    loadPlugins()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "撤销失败：${e.message}") }
                }
        }
    }

    // ============================================================================
    // UI 状态管理
    // ============================================================================

    /**
     * 选中插件并显示详情弹窗
     *
     * @param plugin 要查看详情的插件
     */
    fun selectPlugin(plugin: PluginMetadata) {
        _uiState.update {
            it.copy(
                selectedPlugin = plugin,
                showDetailDialog = true
            )
        }
    }

    /**
     * 显示安装插件对话框
     */
    fun showInstallDialog() {
        _uiState.update { it.copy(showInstallDialog = true) }
    }

    /**
     * 隐藏安装插件对话框
     */
    fun hideInstallDialog() {
        _uiState.update { it.copy(showInstallDialog = false) }
    }

    /**
     * 隐藏详情弹窗
     */
    fun hideDetailDialog() {
        _uiState.update {
            it.copy(
                showDetailDialog = false,
                selectedPlugin = null
            )
        }
    }

    /**
     * 显示权限管理对话框
     */
    fun showPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = true) }
    }

    /**
     * 隐藏权限管理对话框
     */
    fun hidePermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 清除提示信息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * 获取插件状态显示文本
     *
     * @param state 插件状态枚举
     * @return 状态的中文显示文本
     */
    fun getStateText(state: PluginState?): String {
        return when (state) {
            PluginState.ENABLED -> "已启用"
            PluginState.DISABLED -> "已禁用"
            PluginState.INSTALLED -> "已安装"
            PluginState.ERROR -> "错误"
            PluginState.UPDATING -> "更新中"
            null -> "未知"
        }
    }
}
