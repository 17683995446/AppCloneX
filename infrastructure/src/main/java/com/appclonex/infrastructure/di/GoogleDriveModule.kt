/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appclonex.infrastructure.di

import com.appclonex.domain.repository.GoogleDriveRepository
import com.appclonex.infrastructure.network.GoogleDriveApiService
import com.appclonex.infrastructure.repository.GoogleDriveRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Google Drive 模块 - Hilt 依赖注入配置
 * 提供 Google Drive API 服务和仓库绑定的依赖注入配置。
 *
 * 绑定关系：
 * - GoogleDriveRepository -> GoogleDriveRepositoryImpl
 * - GoogleDriveApiService 通过 @Provides 提供（需要 Context）
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GoogleDriveModule {

    /**
     * 将 GoogleDriveRepository 接口绑定到 GoogleDriveRepositoryImpl 实现
     * Hilt 在需要注入 GoogleDriveRepository 时会自动提供 GoogleDriveRepositoryImpl 实例
     */
    @Binds
    @Singleton
    abstract fun bindGoogleDriveRepository(
        impl: GoogleDriveRepositoryImpl
    ): GoogleDriveRepository

    companion object {

        /**
         * 提供 GoogleDriveApiService 实例
         * GoogleDriveApiService 需要 Application Context 来访问网络和文件系统
         *
         * 注意：GoogleDriveApiService 已使用 @Inject 和 @Singleton 注解，
         * Hilt 可以自动创建实例。此方法作为显式提供方式的备选。
         * 如果 GoogleDriveApiService 的构造函数已有 @Inject 注解，
         * 可以移除此方法，让 Hilt 自动处理。
         */
        // GoogleDriveApiService 已通过 @Inject @Singleton 注解标记，
        // Hilt 会自动管理其生命周期，无需显式 @Provides
    }
}
