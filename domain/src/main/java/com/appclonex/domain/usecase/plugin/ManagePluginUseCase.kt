/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件管理用例
 *
 * 封装插件的启用、禁用、卸载和更新等管理操作的业务逻辑。
 * 遵循单一职责原则，将插件生命周期管理操作集中在一个用例中。
 */
package com.appclonex.domain.usecase.plugin

import com.appclonex.domain.model.PluginId
import com.appclonex.domain.repository.PluginRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 插件管理用例
 *
 * 提供插件生命周期管理的统一入口，包括：
 * - 启用插件
 * - 禁用插件
 * - 卸载插件
 * - 更新插件
 *
 * @property repository 插件仓库，提供插件持久化操作能力
 */
@Singleton
class ManagePluginUseCase @Inject constructor(
    private val repository: PluginRepository
) {
    /**
     * 启用插件
     *
     * 激活已安装的插件，使其开始正常运行。
     * 启用前会验证插件是否已安装。
     *
     * @param pluginId 要启用的插件ID
     * @return Result包装的启用结果，失败时包含错误信息
     */
    suspend fun enable(pluginId: PluginId): Result<Unit> {
        // 验证插件是否存在
        val pluginInfo = repository.getPluginInfo(pluginId)
        if (pluginInfo.isFailure || pluginInfo.getOrNull() == null) {
            return Result.failure(IllegalArgumentException("插件不存在：${pluginId.value}"))
        }

        return repository.enablePlugin(pluginId)
    }

    /**
     * 禁用插件
     *
     * 停用已启用的插件，暂停其运行但保留配置和数据。
     * 禁用前会验证插件是否已安装。
     *
     * @param pluginId 要禁用的插件ID
     * @return Result包装的禁用结果，失败时包含错误信息
     */
    suspend fun disable(pluginId: PluginId): Result<Unit> {
        // 验证插件是否存在
        val pluginInfo = repository.getPluginInfo(pluginId)
        if (pluginInfo.isFailure || pluginInfo.getOrNull() == null) {
            return Result.failure(IllegalArgumentException("插件不存在：${pluginId.value}"))
        }

        return repository.disablePlugin(pluginId)
    }

    /**
     * 卸载插件
     *
     * 从系统中完全移除插件，包括清理配置、权限和相关数据。
     * 卸载前会验证插件是否已安装。
     *
     * @param pluginId 要卸载的插件ID
     * @return Result包装的卸载结果，失败时包含错误信息
     */
    suspend fun uninstall(pluginId: PluginId): Result<Unit> {
        // 验证插件是否存在
        val pluginInfo = repository.getPluginInfo(pluginId)
        if (pluginInfo.isFailure || pluginInfo.getOrNull() == null) {
            return Result.failure(IllegalArgumentException("插件不存在：${pluginId.value}"))
        }

        // 先禁用插件，确保安全卸载
        repository.disablePlugin(pluginId)

        return repository.uninstallPlugin(pluginId)
    }

    /**
     * 更新插件
     *
     * 将插件更新到最新版本，包括验证兼容性和迁移配置。
     * 更新前会验证插件是否已安装。
     *
     * @param pluginId 要更新的插件ID
     * @return Result包装的更新结果，失败时包含错误信息
     */
    suspend fun update(pluginId: PluginId): Result<Unit> {
        // 验证插件是否存在
        val pluginInfo = repository.getPluginInfo(pluginId)
        if (pluginInfo.isFailure || pluginInfo.getOrNull() == null) {
            return Result.failure(IllegalArgumentException("插件不存在：${pluginId.value}"))
        }

        return repository.updatePlugin(pluginId)
    }
}
