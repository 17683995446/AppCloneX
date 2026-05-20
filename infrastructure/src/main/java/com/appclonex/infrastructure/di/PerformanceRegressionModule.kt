/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测模块 - Hilt 依赖注入配置
 *
 * 将 PerformanceRegressionRepositoryImpl 绑定到 PerformanceRegressionRepository 接口，
 * 使得上层（Domain/Presentation 层）可以通过接口引用获取实现实例。
 *
 * 安装在 SingletonComponent 中，确保全局单例生命周期。
 */

package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.PerformanceRegressionRepository
import com.appclonex.infrastructure.repository.PerformanceRegressionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 性能回归检测模块 - Hilt 依赖注入配置
 *
 * 提供 PerformanceRegressionRepository 的依赖绑定。
 * 使用 @Binds 将实现类绑定到接口，Hilt 在编译时生成依赖注入代码。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PerformanceRegressionModule {

    /**
     * 绑定 PerformanceRegressionRepositoryImpl 到 PerformanceRegressionRepository 接口
     *
     * @param impl PerformanceRegressionRepositoryImpl 实现实例
     * @return PerformanceRegressionRepository 接口实例
     */
    @Binds
    @Singleton
    abstract fun bindPerformanceRegressionRepository(
        impl: PerformanceRegressionRepositoryImpl
    ): PerformanceRegressionRepository
}
