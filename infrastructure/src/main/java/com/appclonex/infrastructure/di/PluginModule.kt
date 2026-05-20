/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件系统依赖注入模块
 *
 * 使用 Hilt DI 框架配置插件系统的依赖关系，
 * 将 Domain 层的 PluginRepository 接口绑定到
 * Infrastructure 层的 PluginRepositoryImpl 实现。
 */
package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.PluginRepository
import com.appclonex.infrastructure.repository.PluginRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 插件系统 Hilt 依赖注入模块
 *
 * 负责将插件仓库接口绑定到具体实现，遵循依赖倒置原则。
 * 安装在 SingletonComponent 中，确保插件仓库为单例。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PluginModule {

    /**
     * 绑定 PluginRepository 接口到 PluginRepositoryImpl 实现
     *
     * 使用 @Binds 注解将抽象接口与具体实现关联，
     * Hilt 在编译时自动生成依赖注入代码。
     *
     * @param impl PluginRepositoryImpl 实现实例
     * @return PluginRepository 接口实例
     */
    @Binds
    @Singleton
    abstract fun bindPluginRepository(
        impl: PluginRepositoryImpl
    ): PluginRepository
}
