package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.CloneTemplateRepository
import com.appclonex.infrastructure.repository.CloneTemplateRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 模板模块 - Hilt依赖注入配置
 * Template module - Hilt dependency injection configuration
 *
 * 将 CloneTemplateRepository 接口绑定到 CloneTemplateRepositoryImpl 实现
 * Binds the CloneTemplateRepository interface to CloneTemplateRepositoryImpl implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TemplateModule {

    @Binds
    @Singleton
    abstract fun bindCloneTemplateRepository(
        impl: CloneTemplateRepositoryImpl
    ): CloneTemplateRepository
}
