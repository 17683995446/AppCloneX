package com.appclonex.application.di

import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块
 * 提供应用级别的全局依赖
 *
 * 注意：
 * - 数据库相关 Provider 已移至 infrastructure 模块的 DatabaseModule
 * - Repository 接口绑定已移至 infrastructure 模块的 InfrastructureModule
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationDependencyInjectionModule {

    /**
     * 提供 ShizukuServiceConnectionProvider (Singleton)
     * 初始化 Shizuku 服务连接
     */
    @Provides
    @Singleton
    fun provideShizukuServiceConnectionProvider(): ShizukuServiceConnectionProvider {
        return ShizukuServiceConnectionProvider
    }
}
