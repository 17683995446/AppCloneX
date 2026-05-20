package com.appclonex.infrastructure.utility

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.appclonex.infrastructure.caching.ApplicationIconMemoryCacheManager

/**
 * 内存优化管理器
 * 用于监听内存状态并执行清理操作
 */
class ApplicationMemoryOptimizationManager(private val context: Context) : ComponentCallbacks2 {

    private var isRegistered = false

    init {
        register()
    }

    /**
     * 注册组件回调
     * 确保只注册一次，避免重复注册
     */
    private fun register() {
        if (!isRegistered) {
            context.registerComponentCallbacks(this)
            isRegistered = true
        }
    }

    /**
     * 内存紧张时回调
     */
    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // 运行时内存紧张，清理非必要缓存
                ApplicationIconMemoryCacheManager.clearCache(context)
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // UI 隐藏时，可以清理更多资源
                ApplicationIconMemoryCacheManager.clearCache(context)
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // 后台或内存严重不足，清理所有缓存
                ApplicationIconMemoryCacheManager.clearCache(context)
            }
        }
    }

    /**
     * 配置变化时回调
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        // 配置变化时不需要特殊处理
    }

    /**
     * 低内存时回调
     */
    override fun onLowMemory() {
        // 系统内存不足，清理所有缓存
        ApplicationIconMemoryCacheManager.clearCache(context)
    }

    /**
     * 注销回调
     * 确保只注销一次，避免重复注销导致的异常
     */
    fun unregister() {
        if (isRegistered) {
            context.unregisterComponentCallbacks(this)
            isRegistered = false
        }
    }

    companion object {
        @Volatile
        private var instance: ApplicationMemoryOptimizationManager? = null

        fun getInstance(context: Context): ApplicationMemoryOptimizationManager {
            return instance ?: synchronized(this) {
                instance ?: ApplicationMemoryOptimizationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
