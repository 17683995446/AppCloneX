/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件仓库接口
 *
 * 定义插件系统数据操作的契约，遵循依赖倒置原则（DIP）：
 * - Domain层定义接口契约
 * - Infrastructure层提供具体实现（如 PluginRepositoryImpl）
 *
 * ## 职责范围
 * - 插件的安装、卸载、启用、禁用、更新
 * - 插件元数据和配置的持久化
 * - 插件权限管理
 * - 插件搜索和状态观察
 */
package com.appclonex.domain.repository

import com.appclonex.domain.model.PluginConfig
import com.appclonex.domain.model.PluginId
import com.appclonex.domain.model.PluginInstallResult
import com.appclonex.domain.model.PluginManifest
import com.appclonex.domain.model.PluginMetadata
import com.appclonex.domain.model.PluginPermission
import com.appclonex.domain.model.PluginRuntimeInfo
import com.appclonex.domain.model.PluginSearchResult
import com.appclonex.domain.model.PluginState
import kotlinx.coroutines.flow.Flow

/**
 * 插件仓库接口
 *
 * 提供插件全生命周期管理的数据操作能力，包括安装、卸载、
 * 配置、权限管理和状态观察等功能。
 *
 * ## 使用示例
 * ```kotlin
 * class PluginViewModel(
 *     private val pluginRepository: PluginRepository
 * ) : ViewModel() {
 *
 *     // 观察所有插件状态变化
 *     val pluginStates = pluginRepository.observePluginStates()
 *         .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
 *
 *     // 安装插件
 *     fun installPlugin(manifest: PluginManifest) {
 *         viewModelScope.launch {
 *             pluginRepository.installPlugin(manifest)
 *                 .onSuccess { showSuccess() }
 *                 .onFailure { showError(it.message) }
 *         }
 *     }
 * }
 * ```
 *
 * @see PluginManifest
 * @see PluginMetadata
 * @see PluginConfig
 */
interface PluginRepository {

    /**
     * 安装插件
     *
     * 根据插件清单安装插件，包括验证依赖关系、检查版本兼容性、
     * 保存元数据和配置等步骤。
     *
     * @param manifest 插件清单，包含完整的插件描述信息
     * @return Result包装的安装结果，包含成功/失败状态和冲突信息
     */
    suspend fun installPlugin(manifest: PluginManifest): Result<PluginInstallResult>

    /**
     * 卸载插件
     *
     * 从系统中移除已安装的插件，包括禁用插件、清理配置和
     * 释放相关资源。
     *
     * @param pluginId 要卸载的插件ID
     * @return Result包装的卸载结果
     */
    suspend fun uninstallPlugin(pluginId: PluginId): Result<Unit>

    /**
     * 启用插件
     *
     * 激活已安装的插件，使其开始正常运行。
     *
     * @param pluginId 要启用的插件ID
     * @return Result包装的启用结果
     */
    suspend fun enablePlugin(pluginId: PluginId): Result<Unit>

    /**
     * 禁用插件
     *
     * 停用已启用的插件，暂停其运行但保留配置和数据。
     *
     * @param pluginId 要禁用的插件ID
     * @return Result包装的禁用结果
     */
    suspend fun disablePlugin(pluginId: PluginId): Result<Unit>

    /**
     * 更新插件
     *
     * 将插件更新到最新版本，包括下载新版本、验证兼容性、
     * 迁移配置等步骤。
     *
     * @param pluginId 要更新的插件ID
     * @return Result包装的更新结果
     */
    suspend fun updatePlugin(pluginId: PluginId): Result<Unit>

    /**
     * 获取所有已安装插件列表
     *
     * 返回系统中所有已安装插件的元数据列表。
     *
     * @return Result包装的已安装插件元数据列表
     */
    suspend fun getInstalledPlugins(): Result<List<PluginMetadata>>

    /**
     * 获取指定插件的详细信息
     *
     * @param pluginId 插件ID
     * @return Result包装的插件元数据，插件不存在时返回null
     */
    suspend fun getPluginInfo(pluginId: PluginId): Result<PluginMetadata?>

    /**
     * 获取指定插件的当前状态
     *
     * @param pluginId 插件ID
     * @return Result包装的插件状态
     */
    suspend fun getPluginState(pluginId: PluginId): Result<PluginState>

    /**
     * 获取指定插件的运行时信息
     *
     * 包含内存使用、CPU时间、运行状态等性能数据。
     *
     * @param pluginId 插件ID
     * @return Result包装的运行时信息，插件未运行时返回null
     */
    suspend fun getPluginRuntimeInfo(pluginId: PluginId): Result<PluginRuntimeInfo?>

    /**
     * 获取所有插件的运行时信息
     *
     * 批量获取所有已安装插件的运行时性能数据。
     *
     * @return Result包装的运行时信息列表
     */
    suspend fun getAllRuntimeInfo(): Result<List<PluginRuntimeInfo>>

    /**
     * 更新插件配置
     *
     * 保存用户对插件的配置修改，会验证配置字段的合法性。
     *
     * @param pluginId 插件ID
     * @param config 新的插件配置
     * @return Result包装的更新结果
     */
    suspend fun updatePluginConfig(pluginId: PluginId, config: PluginConfig): Result<Unit>

    /**
     * 获取插件当前配置
     *
     * @param pluginId 插件ID
     * @return Result包装的插件配置
     */
    suspend fun getPluginConfig(pluginId: PluginId): Result<PluginConfig>

    /**
     * 检查插件的已授权权限
     *
     * @param pluginId 插件ID
     * @return Result包装的已授权权限列表
     */
    suspend fun checkPermissions(pluginId: PluginId): Result<List<PluginPermission>>

    /**
     * 授予插件权限
     *
     * 为插件授予指定的系统权限。
     *
     * @param pluginId 插件ID
     * @param permission 要授予的权限
     * @return Result包装的授权结果
     */
    suspend fun grantPermission(pluginId: PluginId, permission: PluginPermission): Result<Unit>

    /**
     * 撤销插件权限
     *
     * 撤销插件已获得的指定权限。
     *
     * @param pluginId 插件ID
     * @param permission 要撤销的权限
     * @return Result包装的撤销结果
     */
    suspend fun revokePermission(pluginId: PluginId, permission: PluginPermission): Result<Unit>

    /**
     * 搜索插件
     *
     * 根据关键词搜索已安装的插件，支持按名称和描述模糊匹配。
     *
     * @param query 搜索关键词
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @return Result包装的分页搜索结果
     */
    suspend fun searchPlugins(query: String, page: Int, pageSize: Int): Result<PluginSearchResult>

    /**
     * 观察所有插件的状态变化
     *
     * 返回一个Flow，当任何插件的状态发生变化时自动发出更新。
     * 适合用于UI层的响应式状态展示。
     *
     * @return 插件ID到状态的映射Flow
     */
    fun observePluginStates(): Flow<Map<PluginId, PluginState>>

    /**
     * 观察所有插件的错误信息
     *
     * 返回一个Flow，当任何插件发生错误时自动发出更新。
     * 适合用于错误通知和监控。
     *
     * @return 插件ID到错误信息的映射Flow
     */
    fun observePluginErrors(): Flow<Map<PluginId, String>>
}
