package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.CloudBackupRepository
import com.appclonex.infrastructure.repository.CloudBackupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 云备份模块 - Hilt依赖注入配置
 * Cloud backup module - Hilt dependency injection configuration
 *
 * 将 CloudBackupRepository 接口绑定到 CloudBackupRepositoryImpl 实现
 * Binds the CloudBackupRepository interface to CloudBackupRepositoryImpl implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CloudBackupModule {

    @Binds
    @Singleton
    abstract fun bindCloudBackupRepository(
        impl: CloudBackupRepositoryImpl
    ): CloudBackupRepository
}
