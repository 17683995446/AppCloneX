package com.appclonex.application.initializer

import android.content.Context
import androidx.startup.Initializer
import com.appclonex.infrastructure.caching.ApplicationIconMemoryCacheManager
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager

/**
 * 应用日志初始化器
 * 使用 AndroidX Startup 库在应用启动时自动初始化
 */
class LoggerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        ApplicationLoggingServiceManager.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/**
 * 图标缓存初始化器
 * 依赖日志初始化器
 */
class IconCacheInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        ApplicationIconMemoryCacheManager.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(LoggerInitializer::class.java)
}
