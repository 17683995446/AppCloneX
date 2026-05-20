package com.appclonex.presentation.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Presentation 模块依赖注入配置
 * 
 * 注意：Presentation层只依赖domain层，不直接依赖infrastructure层
 * 所有基础设施相关的绑定都在infrastructure模块的InfrastructureModule中处理
 */
@Module
@InstallIn(SingletonComponent::class)
object PresentationModule {
    // Presentation层的特定依赖注入配置
    // 所有需要注入的接口都通过domain层的repository接口提供
}
