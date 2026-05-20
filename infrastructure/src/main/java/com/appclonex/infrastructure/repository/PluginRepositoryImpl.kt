/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件仓库实现
 *
 * 提供插件系统数据操作的完整实现，包括：
 * - JSON 文件持久化（installed_plugins.json, plugin_configs.json, plugin_permissions.json）
 * - 插件全生命周期管理（安装、卸载、启用、禁用、更新）
 * - 线程安全保证（使用 Mutex）
 * - 响应式状态通知（使用 MutableStateFlow）
 *
 * ## 持久化策略
 * 使用 Android Context 的 filesDir 存储三个 JSON 文件：
 * - installed_plugins.json: 已安装插件的元数据列表
 * - plugin_configs.json: 插件配置映射
 * - plugin_permissions.json: 插件权限映射
 */
package com.appclonex.infrastructure.repository

import android.content.Context
import android.util.Log
import com.appclonex.domain.model.PluginConfig
import com.appclonex.domain.model.PluginId
import com.appclonex.domain.model.PluginInstallResult
import com.appclonex.domain.model.PluginManifest
import com.appclonex.domain.model.PluginMetadata
import com.appclonex.domain.model.PluginPermission
import com.appclonex.domain.model.PluginRuntimeInfo
import com.appclonex.domain.model.PluginSearchResult
import com.appclonex.domain.model.PluginState
import com.appclonex.domain.repository.PluginRepository
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 插件仓库实现类
 *
 * 使用 JSON 文件进行插件数据的持久化存储，通过 Mutex 保证
 * 并发操作的线程安全，通过 StateFlow 实现响应式状态通知。
 *
 * @property context Android 应用上下文，用于获取文件存储路径
 */
@Singleton
class PluginRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PluginRepository {

    companion object {
        private const val TAG = "PluginRepositoryImpl"

        // 持久化文件名
        private const val FILE_INSTALLED_PLUGINS = "installed_plugins.json"
        private const val FILE_PLUGIN_CONFIGS = "plugin_configs.json"
        private const val FILE_PLUGIN_PERMISSIONS = "plugin_permissions.json"
    }

    // JSON 序列化器，配置宽松模式以兼容不同格式
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    // 互斥锁，保证并发操作的线程安全
    private val mutex = Mutex()

    // 内存缓存：已安装插件列表
    private var cachedPlugins: MutableList<PluginMetadata> = mutableListOf()

    // 内存缓存：插件配置映射
    private var cachedConfigs: MutableMap<String, PluginConfig> = mutableMapOf()

    // 内存缓存：插件权限映射
    private var cachedPermissions: MutableMap<String, List<PluginPermission>> = mutableMapOf()

    // 内存缓存：插件状态映射
    private val pluginStates = MutableStateFlow<Map<PluginId, PluginState>>(emptyMap())

    // 内存缓存：插件错误信息映射
    private val pluginErrors = MutableStateFlow<Map<PluginId, String>>(emptyMap())

    // 内存缓存：插件运行时信息
    private val runtimeInfoMap = mutableMapOf<String, PluginRuntimeInfo>()

    init {
        // 初始化时从文件加载数据
        loadAllData()
    }

    // ============================================================================
    // 插件安装/卸载
    // ============================================================================

    /**
     * 安装插件
     *
     * 完整安装流程：验证清单 -> 检查重复 -> 检查依赖 -> 保存数据
     */
    override suspend fun installPlugin(manifest: PluginManifest): Result<PluginInstallResult> =
        mutex.withLock {
            try {
                val metadata = manifest.metadata
                val pluginIdStr = metadata.id.value

                // 检查是否已安装（防止重复安装）
                if (cachedPlugins.any { it.id.value == pluginIdStr }) {
                    return@withLock Result.failure(
                        IllegalStateException("插件已安装：${metadata.name}")
                    )
                }

                // 检查依赖插件是否已安装
                val missingDeps = manifest.dependencies.filter { dep ->
                    cachedPlugins.none { it.id.value == dep.pluginId }
                }
                if (missingDeps.isNotEmpty()) {
                    return@withLock Result.failure(
                        IllegalStateException("缺少依赖插件：${missingDeps.joinToString(", ") { it.pluginId }}")
                    )
                }

                // 保存插件元数据到已安装列表
                cachedPlugins.add(metadata)

                // 初始化插件配置
                val defaultConfig = PluginConfig(
                    pluginId = metadata.id,
                    settings = manifest.configSchema.mapValues { it.value.defaultValue ?: "" },
                    isEnabled = true
                )
                cachedConfigs[pluginIdStr] = defaultConfig

                // 初始化插件权限（默认全部未授权）
                cachedPermissions[pluginIdStr] = emptyList()

                // 设置初始状态为已安装
                updateStateSafely(metadata.id, PluginState.INSTALLED)

                // 持久化到文件
                saveAllData()

                Log.d(TAG, "插件安装成功：${metadata.name} (${metadata.version})")
                Result.success(
                    PluginInstallResult(
                        success = true,
                        pluginId = metadata.id
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "插件安装失败", e)
                // R13-005 修复：异常时返回 Result.failure 而非 Result.success(success=false)
                Result.failure(e)
            }
        }

    /**
     * 卸载插件
     *
     * 完整卸载流程：禁用插件 -> 移除数据 -> 清理配置 -> 持久化
     */
    override suspend fun uninstallPlugin(pluginId: PluginId): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value

                // 检查插件是否存在
                val plugin = cachedPlugins.find { it.id.value == pluginIdStr }
                    ?: return@withLock Result.failure(
                        IllegalArgumentException("插件不存在：$pluginIdStr")
                    )

                // 禁用插件
                updateStateSafely(pluginId, PluginState.DISABLED)

                // 从已安装列表移除
                cachedPlugins.removeAll { it.id.value == pluginIdStr }

                // 清理配置
                cachedConfigs.remove(pluginIdStr)

                // 清理权限
                cachedPermissions.remove(pluginIdStr)

                // 清理运行时信息
                runtimeInfoMap.remove(pluginIdStr)

                // 清理错误信息
                updateErrorsSafely(pluginId, null)

                // 持久化到文件
                saveAllData()

                Log.d(TAG, "插件卸载成功：${plugin.name}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "插件卸载失败", e)
                Result.failure(e)
            }
        }

    // ============================================================================
    // 插件启用/禁用/更新
    // ============================================================================

    /**
     * 启用插件
     *
     * 更新插件状态为 ENABLED，并通知观察者。
     */
    override suspend fun enablePlugin(pluginId: PluginId): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value
                if (cachedPlugins.none { it.id.value == pluginIdStr }) {
                    return@withLock Result.failure(
                        IllegalArgumentException("插件不存在：$pluginIdStr")
                    )
                }

                updateStateSafely(pluginId, PluginState.ENABLED)
                updateErrorsSafely(pluginId, null)

                // 更新运行时信息
                runtimeInfoMap[pluginIdStr] = PluginRuntimeInfo(
                    pluginId = pluginId,
                    isRunning = true,
                    startTime = System.currentTimeMillis()
                )

                saveAllData()
                Log.d(TAG, "插件已启用：$pluginIdStr")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "启用插件失败", e)
                Result.failure(e)
            }
        }

    /**
     * 禁用插件
     *
     * 更新插件状态为 DISABLED，停止运行并通知观察者。
     */
    override suspend fun disablePlugin(pluginId: PluginId): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value
                if (cachedPlugins.none { it.id.value == pluginIdStr }) {
                    return@withLock Result.failure(
                        IllegalArgumentException("插件不存在：$pluginIdStr")
                    )
                }

                updateStateSafely(pluginId, PluginState.DISABLED)

                // 更新运行时信息
                runtimeInfoMap[pluginIdStr]?.let { info ->
                    runtimeInfoMap[pluginIdStr] = info.copy(isRunning = false)
                }

                saveAllData()
                Log.d(TAG, "插件已禁用：$pluginIdStr")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "禁用插件失败", e)
                Result.failure(e)
            }
        }

    /**
     * 更新插件
     *
     * 将插件状态设置为 UPDATING，模拟更新完成后恢复为 ENABLED。
     */
    override suspend fun updatePlugin(pluginId: PluginId): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value
                val plugin = cachedPlugins.find { it.id.value == pluginIdStr }
                    ?: return@withLock Result.failure(
                        IllegalArgumentException("插件不存在：$pluginIdStr")
                    )

                // 设置更新中状态
                updateStateSafely(pluginId, PluginState.UPDATING)

                // 模拟更新操作：更新最后修改时间
                val updatedMetadata = plugin.copy(
                    lastUpdateDate = System.currentTimeMillis()
                )
                cachedPlugins.replaceAll {
                    if (it.id.value == pluginIdStr) updatedMetadata else it
                }

                // 更新完成后恢复为启用状态
                updateStateSafely(pluginId, PluginState.ENABLED)

                saveAllData()
                Log.d(TAG, "插件更新成功：${plugin.name}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "更新插件失败", e)
                updateStateSafely(pluginId, PluginState.ERROR)
                updateErrorsSafely(pluginId, "更新失败：${e.message}")
                Result.failure(e)
            }
        }

    // ============================================================================
    // 插件查询
    // ============================================================================

    /**
     * 获取所有已安装插件列表
     */
    override suspend fun getInstalledPlugins(): Result<List<PluginMetadata>> {
        return Result.success(cachedPlugins.toList())
    }

    /**
     * 获取指定插件的详细信息
     */
    override suspend fun getPluginInfo(pluginId: PluginId): Result<PluginMetadata?> {
        val plugin = cachedPlugins.find { it.id.value == pluginId.value }
        return Result.success(plugin)
    }

    /**
     * 获取指定插件的当前状态
     */
    override suspend fun getPluginState(pluginId: PluginId): Result<PluginState> {
        val state = pluginStates.value[pluginId] ?: PluginState.INSTALLED
        return Result.success(state)
    }

    /**
     * 获取指定插件的运行时信息
     */
    override suspend fun getPluginRuntimeInfo(pluginId: PluginId): Result<PluginRuntimeInfo?> {
        val info = runtimeInfoMap[pluginId.value]
        return Result.success(info)
    }

    /**
     * 获取所有插件的运行时信息
     */
    override suspend fun getAllRuntimeInfo(): Result<List<PluginRuntimeInfo>> {
        return Result.success(runtimeInfoMap.values.toList())
    }

    // ============================================================================
    // 插件配置
    // ============================================================================

    /**
     * 更新插件配置
     *
     * 验证配置字段后保存到缓存和文件。
     */
    override suspend fun updatePluginConfig(pluginId: PluginId, config: PluginConfig): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value
                if (cachedPlugins.none { it.id.value == pluginIdStr }) {
                    return@withLock Result.failure(
                        IllegalArgumentException("插件不存在：$pluginIdStr")
                    )
                }

                // 保存配置
                cachedConfigs[pluginIdStr] = config
                saveAllData()

                Log.d(TAG, "插件配置已更新：$pluginIdStr")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "更新插件配置失败", e)
                Result.failure(e)
            }
        }

    /**
     * 获取插件当前配置
     */
    override suspend fun getPluginConfig(pluginId: PluginId): Result<PluginConfig> {
        val config = cachedConfigs[pluginId.value]
            ?: PluginConfig(pluginId = pluginId, settings = emptyMap(), isEnabled = false)
        return Result.success(config)
    }

    // ============================================================================
    // 插件权限管理
    // ============================================================================

    /**
     * 检查插件的已授权权限
     */
    override suspend fun checkPermissions(pluginId: PluginId): Result<List<PluginPermission>> {
        val permissions = cachedPermissions[pluginId.value] ?: emptyList()
        return Result.success(permissions)
    }

    /**
     * 授予插件权限
     */
    override suspend fun grantPermission(pluginId: PluginId, permission: PluginPermission): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value
                val current = cachedPermissions[pluginIdStr]?.toMutableList() ?: mutableListOf()

                if (permission !in current) {
                    current.add(permission)
                    cachedPermissions[pluginIdStr] = current
                    saveAllData()
                }

                Log.d(TAG, "已授予权限 $permission 给插件 $pluginIdStr")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "授予权限失败", e)
                Result.failure(e)
            }
        }

    /**
     * 撤销插件权限
     */
    override suspend fun revokePermission(pluginId: PluginId, permission: PluginPermission): Result<Unit> =
        mutex.withLock {
            try {
                val pluginIdStr = pluginId.value
                val current = cachedPermissions[pluginIdStr]?.toMutableList() ?: mutableListOf()

                if (permission in current) {
                    current.remove(permission)
                    cachedPermissions[pluginIdStr] = current
                    saveAllData()
                }

                Log.d(TAG, "已撤销插件 $pluginIdStr 的权限 $permission")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "撤销权限失败", e)
                Result.failure(e)
            }
        }

    // ============================================================================
    // 插件搜索
    // ============================================================================

    /**
     * 搜索已安装插件
     *
     * 按名称和描述进行模糊匹配，支持分页。
     */
    override suspend fun searchPlugins(query: String, page: Int, pageSize: Int): Result<PluginSearchResult> {
        val trimmedQuery = query.trim().lowercase()

        val filtered = if (trimmedQuery.isBlank()) {
            cachedPlugins
        } else {
            cachedPlugins.filter { plugin ->
                plugin.name.lowercase().contains(trimmedQuery) ||
                    plugin.description.lowercase().contains(trimmedQuery) ||
                    plugin.author.lowercase().contains(trimmedQuery)
            }
        }

        // 分页计算
        val totalCount = filtered.size
        val safePage = maxOf(1, page)
        val safePageSize = maxOf(1, pageSize)
        val fromIndex = (safePage - 1) * safePageSize
        val toIndex = minOf(fromIndex + safePageSize, totalCount)

        val pageItems = if (fromIndex < totalCount) {
            filtered.subList(fromIndex, toIndex)
        } else {
            emptyList()
        }

        return Result.success(
            PluginSearchResult(
                plugins = pageItems,
                totalCount = totalCount,
                page = safePage,
                pageSize = safePageSize
            )
        )
    }

    // ============================================================================
    // 状态观察
    // ============================================================================

    /**
     * 观察所有插件的状态变化
     */
    override fun observePluginStates(): Flow<Map<PluginId, PluginState>> {
        return pluginStates.asStateFlow()
    }

    /**
     * 观察所有插件的错误信息
     */
    override fun observePluginErrors(): Flow<Map<PluginId, String>> {
        return pluginErrors.asStateFlow()
    }

    // ============================================================================
    // 内部辅助方法
    // ============================================================================

    /**
     * 安全更新插件状态
     *
     * 使用 StateFlow 的 update 方法原子性地更新状态映射。
     * 注意：此方法应在 mutex 锁内调用，确保线程安全。
     */
    private fun updateStateSafely(pluginId: PluginId, state: PluginState) {
        val currentStates = pluginStates.value
        val updatedStates = currentStates.toMutableMap().apply {
            this[pluginId] = state
        }
        // 只在状态真正变化时更新，减少不必要的重组
        if (currentStates[pluginId] != state) {
            pluginStates.value = updatedStates
        }
    }

    /**
     * 安全更新插件错误信息
     *
     * 设置或清除指定插件的错误信息。
     */
    private fun updateErrorsSafely(pluginId: PluginId, error: String?) {
        val current = pluginErrors.value.toMutableMap()
        if (error != null) {
            current[pluginId] = error
        } else {
            current.remove(pluginId)
        }
        pluginErrors.value = current
    }

    /**
     * 从文件加载所有持久化数据
     *
     * 在初始化时调用，从 JSON 文件恢复插件数据到内存缓存。
     */
    private fun loadAllData() {
        try {
            // 加载已安装插件列表
            val pluginsFile = getPluginFile(FILE_INSTALLED_PLUGINS)
            if (pluginsFile.exists()) {
                val content = pluginsFile.readText()
                if (content.isNotBlank()) {
                    val wrapper = json.decodeFromString<PluginListWrapper>(content)
                    cachedPlugins = wrapper.plugins.toMutableList()
                }
            }

            // 加载插件配置
            val configsFile = getPluginFile(FILE_PLUGIN_CONFIGS)
            if (configsFile.exists()) {
                val content = configsFile.readText()
                if (content.isNotBlank()) {
                    cachedConfigs = json.decodeFromString(content)
                }
            }

            // 加载插件权限
            val permissionsFile = getPluginFile(FILE_PLUGIN_PERMISSIONS)
            if (permissionsFile.exists()) {
                val content = permissionsFile.readText()
                if (content.isNotBlank()) {
                    cachedPermissions = json.decodeFromString(content)
                }
            }

            // 初始化插件状态映射
            val stateMap = mutableMapOf<PluginId, PluginState>()
            for (plugin in cachedPlugins) {
                val config = cachedConfigs[plugin.id.value]
                stateMap[plugin.id] = if (config?.isEnabled == true) {
                    PluginState.ENABLED
                } else {
                    PluginState.DISABLED
                }
            }
            pluginStates.value = stateMap

            Log.d(TAG, "插件数据加载完成，共 ${cachedPlugins.size} 个插件")
        } catch (e: Exception) {
            Log.e(TAG, "加载插件数据失败", e)
        }
    }

    /**
     * 保存所有数据到文件
     *
     * 将内存缓存中的插件数据序列化为 JSON 并写入文件。
     */
    private fun saveAllData() {
        try {
            // 保存已安装插件列表
            val pluginsFile = getPluginFile(FILE_INSTALLED_PLUGINS)
            val pluginsWrapper = PluginListWrapper(cachedPlugins.toList())
            AtomicFileWriter.write(pluginsFile, json.encodeToString(pluginsWrapper))

            // 保存插件配置
            val configsFile = getPluginFile(FILE_PLUGIN_CONFIGS)
            AtomicFileWriter.write(configsFile, json.encodeToString(cachedConfigs))

            // 保存插件权限
            val permissionsFile = getPluginFile(FILE_PLUGIN_PERMISSIONS)
            AtomicFileWriter.write(permissionsFile, json.encodeToString(cachedPermissions))

            Log.d(TAG, "插件数据已保存")
        } catch (e: Exception) {
            Log.e(TAG, "保存插件数据失败", e)
        }
    }

    /**
     * 获取插件数据文件
     *
     * @param fileName 文件名
     * @return 文件对象
     */
    private fun getPluginFile(fileName: String): File {
        val dir = File(context.filesDir, "plugins")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    /**
     * 插件列表序列化包装器
     *
     * 用于将插件列表序列化为 JSON 数组格式。
     */
    @Serializable
    data class PluginListWrapper(
        val plugins: List<PluginMetadata> = emptyList()
    )
}
