package com.appclonex.infrastructure.di

import com.appclonex.infrastructure.logging.FirebaseCrashReportingService
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 崩溃报告模块
 *
 * 注意：FirebaseCrashReportingService 已通过 @Inject constructor 标注，
 * Hilt 可以自动创建实例。InfrastructureModule 中已通过 @Binds 将
 * CrashReportingServiceImpl 绑定到 CrashReportingService 接口。
 * 此处不再需要 @Provides，避免冗余提供。
 */
@Module
@InstallIn(SingletonComponent::class)
object CrashReportingModule {
    // FirebaseCrashReportingService 通过 @Inject constructor 自动管理，
    // 不再需要手动 @Provides。
}
