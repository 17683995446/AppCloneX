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

import com.appclonex.domain.repository.NextcloudRepository
import com.appclonex.domain.repository.WebDavRepository
import com.appclonex.infrastructure.network.WebDavApiService
import com.appclonex.infrastructure.repository.NextcloudRepositoryImpl
import com.appclonex.infrastructure.repository.WebDavRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WebDAV / Nextcloud 依赖注入模块
 * 配置 Hilt DI 容器中 WebDAV 和 Nextcloud 相关的依赖关系。
 *
 * 绑定关系：
 * - WebDavRepository 接口 -> WebDavRepositoryImpl 实现
 * - NextcloudRepository 接口 -> NextcloudRepositoryImpl 实现
 * - WebDavApiService 作为单例提供
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WebDavModule {

    /**
     * 将 WebDavRepository 接口绑定到 WebDavRepositoryImpl 实现
     *
     * @param impl WebDavRepositoryImpl 实例
     * @return WebDavRepository 接口实例
     */
    @Binds
    @Singleton
    abstract fun bindWebDavRepository(
        impl: WebDavRepositoryImpl
    ): WebDavRepository

    /**
     * 将 NextcloudRepository 接口绑定到 NextcloudRepositoryImpl 实现
     *
     * @param impl NextcloudRepositoryImpl 实例
     * @return NextcloudRepository 接口实例
     */
    @Binds
    @Singleton
    abstract fun bindNextcloudRepository(
        impl: NextcloudRepositoryImpl
    ): NextcloudRepository

    companion object {

        /**
         * 提供 WebDavApiService 单例实例
         * WebDavApiService 使用 @Inject 构造函数，由 Hilt 自动创建
         *
         * @return WebDavApiService 实例
         */
        @Provides
        @Singleton
        fun provideWebDavApiService(): WebDavApiService {
            return WebDavApiService()
        }
    }
}
