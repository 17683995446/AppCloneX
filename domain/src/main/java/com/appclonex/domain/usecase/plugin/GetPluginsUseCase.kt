/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 获取插件列表用例
 *
 * 封装获取已安装插件列表的业务逻辑，以响应式 Flow 形式
 * 持续推送插件列表更新，适合UI层直接观察。
 */
package com.appclonex.domain.usecase.plugin

import com.appclonex.domain.model.PluginMetadata
import com.appclonex.domain.repository.PluginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取插件列表用例
 *
 * 提供已安装插件列表的响应式数据流，当插件安装/卸载时
 * 自动推送更新，适合UI层直接观察和展示。
 *
 * @property repository 插件仓库，提供插件数据查询能力
 */
@Singleton
class GetPluginsUseCase @Inject constructor(
    private val repository: PluginRepository
) {
    /**
     * 获取已安装插件列表的响应式数据流
     *
     * 首次调用时获取当前已安装插件列表，之后当插件状态变化时
     * 自动重新获取并推送更新。
     *
     * @return Flow包装的已安装插件元数据列表
     */
    operator fun invoke(): Flow<List<PluginMetadata>> {
        return flow {
            // 初始加载
            val result = repository.getInstalledPlugins()
            if (result.isSuccess) {
                emit(result.getOrDefault(emptyList()))
            }

            // 观察插件状态变化，状态变化时重新获取列表
            repository.observePluginStates().collect {
                val updated = repository.getInstalledPlugins()
                if (updated.isSuccess) {
                    emit(updated.getOrDefault(emptyList()))
                }
            }
        }
    }
}
