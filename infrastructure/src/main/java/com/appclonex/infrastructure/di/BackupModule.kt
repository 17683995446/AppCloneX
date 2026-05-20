package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.BackupRepository
import com.appclonex.infrastructure.repository.BackupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 备份模块 - Hilt依赖注入配置
 * Backup module - Hilt dependency injection configuration
 *
 * 将 BackupRepository 接口绑定到 BackupRepositoryImpl 实现
 * Binds the BackupRepository interface to BackupRepositoryImpl implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
