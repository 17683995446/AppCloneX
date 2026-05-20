package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.FreezeStrategyRepository
import com.appclonex.infrastructure.repository.FreezeStrategyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 冻结策略模块 - Hilt 依赖注入配置
 *
 * 将 [FreezeStrategyRepositoryImpl] 绑定到 [FreezeStrategyRepository] 接口，
 * 使上层（Domain/Presentation）可以通过接口依赖注入获取实现。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FreezeStrategyModule {

    @Binds
    @Singleton
    abstract fun bindFreezeStrategyRepository(
        impl: FreezeStrategyRepositoryImpl
    ): FreezeStrategyRepository
}
