package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.UsageStatisticsRepository
import com.appclonex.infrastructure.repository.UsageStatisticsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 使用统计模块 - Hilt 依赖注入配置
 *
 * 将 UsageStatisticsRepositoryImpl 绑定到 UsageStatisticsRepository 接口，
 * 使得上层（Domain/Presentation 层）可以通过接口引用获取实现实例。
 *
 * 安装在 SingletonComponent 中，确保全局单例生命周期。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UsageStatisticsModule {
    @Binds
    @Singleton
    abstract fun bindUsageStatisticsRepository(impl: UsageStatisticsRepositoryImpl): UsageStatisticsRepository
}
