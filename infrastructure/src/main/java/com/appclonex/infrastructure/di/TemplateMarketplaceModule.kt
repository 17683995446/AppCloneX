/*
 * Copyright (c) 2024 AppCloneX
 *
 * 模板市场依赖注入模块
 * 配置 Hilt DI，将模板市场仓库接口绑定到具体实现
 */

package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.TemplateMarketplaceRepository
import com.appclonex.infrastructure.repository.TemplateMarketplaceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 模板市场模块 - Hilt 依赖注入配置
 * Template marketplace module - Hilt dependency injection configuration
 *
 * 将 [TemplateMarketplaceRepository] 接口绑定到 [TemplateMarketplaceRepositoryImpl] 实现
 * Binds the TemplateMarketplaceRepository interface to TemplateMarketplaceRepositoryImpl implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TemplateMarketplaceModule {

    /**
     * 绑定模板市场仓库接口到实现类
     * 使用 @Singleton 确保全局单例，共享市场数据缓存
     *
     * @param impl 模板市场仓库实现
     * @return 模板市场仓库接口
     */
    @Binds
    @Singleton
    abstract fun bindTemplateMarketplaceRepository(
        impl: TemplateMarketplaceRepositoryImpl
    ): TemplateMarketplaceRepository
}
