/*
 * AppCloneX - 通知模块 Hilt DI 配置
 * AppCloneX - Notification Module Hilt DI Configuration
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 将通知仓库接口绑定到具体实现类。
 * Binds the notification repository interface to its concrete implementation.
 */
package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.NotificationRepository
import com.appclonex.infrastructure.repository.NotificationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 通知模块 - Hilt依赖注入配置
 * Notification module - Hilt dependency injection configuration
 *
 * 将 NotificationRepository 接口绑定到 NotificationRepositoryImpl 实现，
 * 使用 Singleton 作用域确保全局单例。
 * Binds the NotificationRepository interface to NotificationRepositoryImpl implementation,
 * using Singleton scope to ensure a global singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    /**
     * 绑定通知仓库接口到实现类
     * Bind notification repository interface to implementation
     *
     * @param impl 通知仓库实现类 / Notification repository implementation
     * @return 通知仓库接口 / Notification repository interface
     */
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository
}
