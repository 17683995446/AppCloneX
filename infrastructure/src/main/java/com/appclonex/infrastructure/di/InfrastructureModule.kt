package com.appclonex.infrastructure.di

import android.content.Context
import com.appclonex.domain.repository.AppLockRepository
import com.appclonex.domain.repository.ApplicationIconCacheRepository
import com.appclonex.domain.repository.AutomationRuleRepository
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.ConfigurationRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.domain.repository.PreferencesRepository
import com.appclonex.domain.repository.ShizukuStateRepository
import com.appclonex.domain.repository.StorageAnalysisRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.domain.service.CloneOperationNotificationService
import com.appclonex.domain.usecase.monitoring.CrashReportingService
import com.appclonex.domain.usecase.monitoring.UserEventTracker
import com.appclonex.domain.usecase.clone.BatchCloneUseCase
import com.appclonex.infrastructure.caching.ApplicationIconMemoryCacheManager
import com.appclonex.infrastructure.logging.CrashReportingServiceImpl
import com.appclonex.infrastructure.logging.UserEventTrackerImpl
import com.appclonex.infrastructure.repository.ApplicationIconCacheRepositoryImpl
import com.appclonex.infrastructure.repository.AutomationRuleRepositoryImpl
import com.appclonex.infrastructure.repository.ClonedApplicationRepositoryImpl
import com.appclonex.infrastructure.repository.ConfigurationRepositoryImpl
import com.appclonex.infrastructure.repository.PreferencesRepositoryImpl
import com.appclonex.infrastructure.repository.ShizukuStateRepositoryImpl
import com.appclonex.infrastructure.repository.UserProfileRepositoryImpl
import com.appclonex.infrastructure.security.AppLockRepositoryImpl
import com.appclonex.infrastructure.service.CloneOperationNotificationServiceImpl
import com.appclonex.infrastructure.storage.StorageAnalysisRepositoryImpl
import com.appclonex.infrastructure.system.ApplicationFreezingStateManager
import com.appclonex.infrastructure.usecase.BatchCloneUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Infrastructure 模块依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InfrastructureModule {

    @Binds
    @Singleton
    abstract fun bindClonedApplicationRepository(
        impl: ClonedApplicationRepositoryImpl
    ): ClonedApplicationRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindShizukuStateRepository(
        impl: ShizukuStateRepositoryImpl
    ): ShizukuStateRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindApplicationIconCacheRepository(
        impl: ApplicationIconCacheRepositoryImpl
    ): ApplicationIconCacheRepository

    @Binds
    @Singleton
    abstract fun bindCrashReportingService(
        impl: CrashReportingServiceImpl
    ): CrashReportingService

    @Binds
    @Singleton
    abstract fun bindUserEventTracker(
        impl: UserEventTrackerImpl
    ): UserEventTracker

    @Binds
    @Singleton
    abstract fun bindStorageAnalysisRepository(
        impl: StorageAnalysisRepositoryImpl
    ): StorageAnalysisRepository

    @Binds
    @Singleton
    abstract fun bindAppLockRepository(
        impl: AppLockRepositoryImpl
    ): AppLockRepository

    @Binds
    @Singleton
    abstract fun bindConfigurationRepository(
        impl: ConfigurationRepositoryImpl
    ): ConfigurationRepository

    @Binds
    @Singleton
    abstract fun bindAutomationRuleRepository(
        impl: AutomationRuleRepositoryImpl
    ): AutomationRuleRepository

    @Binds
    @Singleton
    abstract fun bindFreezingService(
        impl: ApplicationFreezingStateManager
    ): FreezingService

    @Binds
    @Singleton
    abstract fun bindCloneOperationNotificationService(
        impl: CloneOperationNotificationServiceImpl
    ): CloneOperationNotificationService

    @Binds
    @Singleton
    abstract fun bindBatchCloneUseCase(
        impl: BatchCloneUseCaseImpl
    ): BatchCloneUseCase
}

/**
 * Infrastructure 初始化模块
 * 
 * 注意：ApplicationIconCacheRepository 的初始化已移至
 * ApplicationIconCacheRepositoryImpl 的 @Inject constructor 中，
 * 避免与 InfrastructureModule 中的 @Binds 冲突。
 */
@Module
@InstallIn(SingletonComponent::class)
object InfrastructureInitModule {

    // ApplicationIconCacheRepository 的 @Provides 已移除，
    // 统一使用 InfrastructureModule 中的 @Binds 绑定。
    // ApplicationIconMemoryCacheManager.initialize(context) 已移至
    // ApplicationIconCacheRepositoryImpl 的构造函数中执行。
}
